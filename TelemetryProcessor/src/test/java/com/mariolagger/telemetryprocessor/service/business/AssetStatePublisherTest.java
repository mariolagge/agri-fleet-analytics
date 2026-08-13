package com.mariolagger.telemetryprocessor.service.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mariolagger.telemetryprocessor.dto.AssetId;
import com.mariolagger.telemetryprocessor.dto.TelemetryMessage;
import com.mariolagger.telemetryprocessor.dto.VibrationLevel;
import com.mariolagger.telemetryprocessor.dto.WorkingStatus;
import com.mariolagger.telemetryprocessor.entity.AssetState;
import com.mariolagger.telemetryprocessor.entity.HourlyAggregate;
import com.mariolagger.telemetryprocessor.repository.AssetStateRepository;
import com.mariolagger.telemetryprocessor.repository.HourlyAggregateRepository;

/**
 * Only the two repositories are mocked. The four collaborators are real instances: they are
 * deterministic, and stubbing them would test the stubs rather than the publisher. That mix
 * rules out @InjectMocks, so the publisher is constructed by hand and the accumulator is kept
 * as a field, because the sealing assertions read it back after publish() has run.
 */
@ExtendWith(MockitoExtension.class)
class AssetStatePublisherTest {

	private static final AssetId ASSET = AssetId.TRACTOR;

	private static final Instant HOUR_06 = Instant.parse("2026-08-13T06:00:00Z");

	private static final Instant AT_06_30_00 = Instant.parse("2026-08-13T06:30:00Z");
	private static final Instant AT_06_30_30 = Instant.parse("2026-08-13T06:30:30Z");
	private static final Instant AT_06_31_00 = Instant.parse("2026-08-13T06:31:00Z");
	private static final Instant AT_07_00_10 = Instant.parse("2026-08-13T07:00:10Z");

	/** SPEC §4.4: the TRACTOR baseline engine hours. */
	private static final double TRACTOR_BASELINE_ENGINE_HOURS = 6200.0;

	private static final double TOLERANCE = 1e-9;

	@Mock
	private AssetStateRepository assetStateRepository;

	@Mock
	private HourlyAggregateRepository hourlyAggregateRepository;

	@Captor
	private ArgumentCaptor<Iterable<AssetState>> assetStateRows;

	private CounterAccumulationService counterAccumulationService;
	private AssetStatePublisher assetStatePublisher;

	@BeforeEach
	void createPublisherWithRealCollaborators() {
		counterAccumulationService = new CounterAccumulationService();
		assetStatePublisher = new AssetStatePublisher(counterAccumulationService, new FlagEvaluationService(),
				new AssetAssessmentService(), new FleetRankingService(), assetStateRepository,
				hourlyAggregateRepository);
	}

	/** The hour reached hourly_aggregate, so it is never handed over again. */
	@Test
	void publishSealsTheHourOnceBothWritesHaveReturned() {
		accumulateAcrossTheHourBoundary();
		assertTrue(counterAccumulationService.sealableBuckets(ASSET).containsKey(HOUR_06));

		assetStatePublisher.publish();

		assertTrue(counterAccumulationService.sealableBuckets(ASSET).isEmpty());
	}

	/** A failed write must leave the hour sealable, so the next cycle retries it. */
	@Test
	void publishLeavesTheHourSealableWhenTheAssetStateWriteThrows() {
		accumulateAcrossTheHourBoundary();
		when(assetStateRepository.saveAll(anyIterable())).thenThrow(new RuntimeException("asset_state write failed"));

		assertThrows(RuntimeException.class, () -> assetStatePublisher.publish());

		assertTrue(counterAccumulationService.sealableBuckets(ASSET).containsKey(HOUR_06));
	}

	/** SPEC §5.4 is cross-asset, so all four rows are written together and ranked 1..4. */
	@Test
	void allFourRowsReachTheRepositoryInOneSaveAllWithDistinctRanks() {
		assetStatePublisher.publish();

		verify(assetStateRepository).saveAll(assetStateRows.capture());
		List<AssetState> rows = capturedRows();
		assertEquals(AssetId.values().length, rows.size());

		List<Integer> priorityRanks = new ArrayList<>();
		for (AssetState row : rows) {
			priorityRanks.add(row.getPriorityRank());
		}
		priorityRanks.sort(null);
		assertEquals(List.of(1, 2, 3, 4), priorityRanks);
	}

	/** ARCHITECTURE.md §4 startup reset, then the SPEC §4.4 baselines. */
	@Test
	void resetOnStartupClearsBothTablesAndThenWritesTheBaselines() {
		assetStatePublisher.resetOnStartup();

		InOrder bothTables = inOrder(assetStateRepository, hourlyAggregateRepository);
		bothTables.verify(assetStateRepository).deleteAll();
		bothTables.verify(hourlyAggregateRepository).deleteAll();
		bothTables.verify(assetStateRepository).saveAll(assetStateRows.capture());

		List<AssetState> rows = capturedRows();
		assertEquals(AssetId.values().length, rows.size());
		assertEquals(TRACTOR_BASELINE_ENGINE_HOURS, rowOf(rows, ASSET).getLifetimeEngineHours(), TOLERANCE);
	}

	/** One clock read per publish, so the four rows describe the same instant. */
	@Test
	void everyRowOfOnePublishCarriesTheSameUpdatedAt() {
		assetStatePublisher.publish();

		verify(assetStateRepository).saveAll(assetStateRows.capture());
		List<AssetState> rows = capturedRows();
		Instant updatedAt = rows.get(0).getUpdatedAt();
		assertNotNull(updatedAt);
		for (AssetState row : rows) {
			assertEquals(updatedAt, row.getUpdatedAt());
		}
	}

	/** The sealed hour is written to hourly_aggregate with the counters it accumulated. */
	@Test
	void theSealedHourIsWrittenToHourlyAggregate() {
		accumulateAcrossTheHourBoundary();

		assetStatePublisher.publish();

		ArgumentCaptor<Iterable<HourlyAggregate>> hourRows = ArgumentCaptor.captor();
		verify(hourlyAggregateRepository).saveAll(hourRows.capture());
		List<HourlyAggregate> rows = new ArrayList<>();
		hourRows.getValue().forEach(rows::add);
		assertEquals(1, rows.size());
		assertEquals(ASSET.name(), rows.get(0).getAssetId());
		assertEquals(HOUR_06, rows.get(0).getHourStart());
		assertTrue(rows.get(0).getEngineHours() > 0.0);
	}

	private List<AssetState> capturedRows() {
		List<AssetState> rows = new ArrayList<>();
		assetStateRows.getValue().forEach(rows::add);
		return rows;
	}

	private AssetState rowOf(List<AssetState> rows, AssetId assetId) {
		for (AssetState row : rows) {
			if (row.getAssetId().equals(assetId.name())) {
				return row;
			}
		}
		throw new IllegalStateException("no row written for " + assetId);
	}

	/**
	 * Leaves the 06:00Z bucket final — the asset's stream has moved into the 07:00Z hour — so
	 * sealableBuckets offers exactly that one hour.
	 */
	private void accumulateAcrossTheHourBoundary() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_30));
		counterAccumulationService.accumulate(productiveMessage(AT_06_31_00));
		counterAccumulationService.accumulate(productiveMessage(AT_07_00_10));
	}

	private TelemetryMessage productiveMessage(Instant timestamp) {
		TelemetryMessage message = new TelemetryMessage();
		message.setAssetId(ASSET);
		message.setTimestamp(timestamp);
		message.setPtoEngaged(true);
		message.setGroundSpeedKmh(8.0);
		message.setLoadFactor(0.6);
		message.setTemperatureC(70.0);
		message.setVibrationLevel(VibrationLevel.NORMAL);
		message.setWorkingStatus(WorkingStatus.WORKING);
		return message;
	}
}
