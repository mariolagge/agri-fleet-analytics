package com.mariolagger.operatorapi.service.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mariolagger.operatorapi.dto.AssetId;
import com.mariolagger.operatorapi.dto.AssetStatus;
import com.mariolagger.operatorapi.dto.AssetView;
import com.mariolagger.operatorapi.dto.Flag;
import com.mariolagger.operatorapi.dto.FlagScope;
import com.mariolagger.operatorapi.dto.FlagSeverity;
import com.mariolagger.operatorapi.dto.FlagView;
import com.mariolagger.operatorapi.dto.LatestTelemetryEntry;
import com.mariolagger.operatorapi.dto.LatestTelemetryResponse;
import com.mariolagger.operatorapi.dto.LiveCondition;
import com.mariolagger.operatorapi.dto.TelemetryView;
import com.mariolagger.operatorapi.dto.VibrationLevel;
import com.mariolagger.operatorapi.dto.WorkingStatus;
import com.mariolagger.operatorapi.entity.AssetState;
import com.mariolagger.operatorapi.repository.AssetStateRepository;

/**
 * The repository is the only collaborator, so it is the only mock: the service maps rows onto
 * response shapes and derives nothing. Each test builds its rows through assetStateRow() and
 * then sets only the columns it makes an assertion about.
 */
@ExtendWith(MockitoExtension.class)
class AssetViewServiceTest {

	private static final Instant AT_06_30 = Instant.parse("2026-08-13T06:30:00Z");

	@Mock
	private AssetStateRepository assetStateRepository;

	private AssetViewService assetViewService;

	@BeforeEach
	void createService() {
		assetViewService = new AssetViewService(assetStateRepository);
	}

	/** Bug 7: no published state is a server-side gap, not a missing resource. */
	@Test
	void getFleetThrows503WhenNoStateHasBeenPublishedYet() {
		when(assetStateRepository.findAllByOrderByPriorityRankAsc()).thenReturn(List.of());

		ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
				() -> assetViewService.getFleet());

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, thrown.getStatusCode());
		assertReasonIsNotEmpty(thrown);
	}

	/** Bug 7: 404 is reserved for an id outside the four SPEC §2 assets. */
	@Test
	void getAssetThrows404ForAnUnknownAssetIdWithoutQueryingTheRepository() {
		ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
				() -> assetViewService.getAsset("BULLDOZER"));

		assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());
		assertReasonIsNotEmpty(thrown);
		verifyNoInteractions(assetStateRepository);
	}

	/** Bug 7: a known asset with no row is the same server-side gap as an empty fleet. */
	@Test
	void getAssetThrows503WhenTheKnownAssetHasNoStoredRow() {
		when(assetStateRepository.findById(AssetId.TRACTOR.name())).thenReturn(Optional.empty());

		ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
				() -> assetViewService.getAsset("TRACTOR"));

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, thrown.getStatusCode());
		assertReasonIsNotEmpty(thrown);
	}

	/**
	 * ARCHITECTURE.md §2: the ranking is TelemetryProcessor's, so the service hands the rows
	 * back in the order the repository produced them. The ranks below are deliberately out of
	 * order — a service that re-sorted would reorder this list.
	 */
	@Test
	void getFleetKeepsTheRepositoryOrderAndDoesNotReSortByPriorityRank() {
		AssetState tractor = assetStateRow(AssetId.TRACTOR);
		tractor.setPriorityRank(2);
		AssetState mower = assetStateRow(AssetId.MOWER);
		mower.setPriorityRank(4);
		AssetState baler = assetStateRow(AssetId.BALER);
		baler.setPriorityRank(1);
		AssetState mixer = assetStateRow(AssetId.MIXER);
		mixer.setPriorityRank(3);
		when(assetStateRepository.findAllByOrderByPriorityRankAsc())
				.thenReturn(List.of(tractor, mower, baler, mixer));

		List<AssetView> assets = assetViewService.getFleet().getAssets();

		assertEquals(4, assets.size());
		assertEquals(AssetId.TRACTOR, assets.get(0).getAssetId());
		assertEquals(AssetId.MOWER, assets.get(1).getAssetId());
		assertEquals(AssetId.BALER, assets.get(2).getAssetId());
		assertEquals(AssetId.MIXER, assets.get(3).getAssetId());
		assertEquals(2, assets.get(0).getPriorityRank());
	}

	/** Bug 13: an asset that never reported has no telemetry object at all. */
	@Test
	void getLatestTelemetryLeavesTelemetryNullWhenEveryLatestColumnIsNull() {
		AssetState neverReported = assetStateRow(AssetId.TRACTOR);
		neverReported.setLiveCondition(LiveCondition.STALE);
		when(assetStateRepository.findAll()).thenReturn(List.of(neverReported));

		LatestTelemetryEntry entry = entryOf(assetViewService.getLatestTelemetry(), AssetId.TRACTOR);

		assertEquals(LiveCondition.STALE, entry.getLiveCondition());
		assertNull(entry.getTelemetry());
	}

	/** Bug 13: a null reading stays null in the response — it must not become 0.0. */
	@Test
	void getLatestTelemetryMapsANullReadingToNullAndNotToZero() {
		AssetState reported = assetStateRow(AssetId.MOWER);
		reported.setLiveCondition(LiveCondition.NORMAL);
		reported.setLatestTimestamp(AT_06_30);
		reported.setLatestGroundSpeedKmh(null);
		reported.setLatestPtoEngaged(Boolean.TRUE);
		reported.setLatestLoadFactor(0.5);
		reported.setLatestTemperatureC(70.0);
		reported.setLatestVibrationLevel(VibrationLevel.NORMAL);
		reported.setLatestWorkingStatus(WorkingStatus.WORKING);
		when(assetStateRepository.findAll()).thenReturn(List.of(reported));

		TelemetryView telemetry = entryOf(assetViewService.getLatestTelemetry(), AssetId.MOWER).getTelemetry();

		assertNotNull(telemetry);
		assertNull(telemetry.getGroundSpeedKmh());
		assertEquals(AT_06_30, telemetry.getTimestamp());
		assertEquals(0.5, telemetry.getLoadFactor());
	}

	/** GLOSSARY §5: an inactive flag has no severity and is absent from the response. */
	@Test
	void anAssetWithNoFlagSeveritySetProducesAnEmptyFlagsList() {
		AssetState unflagged = assetStateRow(AssetId.MIXER);
		when(assetStateRepository.findById(AssetId.MIXER.name())).thenReturn(Optional.of(unflagged));

		AssetView assetView = assetViewService.getAsset("MIXER");

		assertNotNull(assetView.getFlags());
		assertTrue(assetView.getFlags().isEmpty());
	}

	/** SPEC §5.1: an active flag is published with the counter, scope, value and threshold. */
	@Test
	void anActiveFlagCarriesItsCounterScopeValueAndThresholdRoundedToTwoDecimals() {
		AssetState flagged = assetStateRow(AssetId.TRACTOR);
		flagged.setStatus(AssetStatus.WATCH);
		flagged.setHeavyRecentUseSeverity(FlagSeverity.MODERATE);
		flagged.setHeavyRecentUseValue(7.126666);
		flagged.setHeavyRecentUseThreshold(6.0);
		flagged.setHighCumulativeLoadSeverity(FlagSeverity.SEVERE);
		flagged.setHighCumulativeLoadValue(4801.994);
		flagged.setHighCumulativeLoadThreshold(4500.0);
		when(assetStateRepository.findById(AssetId.TRACTOR.name())).thenReturn(Optional.of(flagged));

		List<FlagView> flags = assetViewService.getAsset("TRACTOR").getFlags();

		assertEquals(2, flags.size());

		FlagView heavyRecentUse = flagOf(flags, Flag.HEAVY_RECENT_USE);
		assertEquals(FlagSeverity.MODERATE, heavyRecentUse.getSeverity());
		assertEquals("highLoadHours", heavyRecentUse.getCounter());
		assertEquals(FlagScope.ROLLING_24H, heavyRecentUse.getScope());
		assertEquals(7.13, heavyRecentUse.getValue());
		assertEquals(6.0, heavyRecentUse.getThreshold());

		FlagView highCumulativeLoad = flagOf(flags, Flag.HIGH_CUMULATIVE_LOAD);
		assertEquals(FlagSeverity.SEVERE, highCumulativeLoad.getSeverity());
		assertEquals("loadedHours", highCumulativeLoad.getCounter());
		assertEquals(FlagScope.LIFETIME, highCumulativeLoad.getScope());
		assertEquals(4801.99, highCumulativeLoad.getValue());
		assertEquals(4500.0, highCumulativeLoad.getThreshold());
	}

	/** SPEC §6.4: all four assets are always present, ordered by assetId ascending. */
	@Test
	void getLatestTelemetryReturnsAllFourAssetsInAscendingIdOrderWithTheMissingOnesStale() {
		AssetState tractor = assetStateRow(AssetId.TRACTOR);
		tractor.setLiveCondition(LiveCondition.NORMAL);
		tractor.setLatestTimestamp(AT_06_30);
		AssetState baler = assetStateRow(AssetId.BALER);
		baler.setLiveCondition(LiveCondition.ALERT);
		baler.setLatestTimestamp(AT_06_30);
		when(assetStateRepository.findAll()).thenReturn(List.of(tractor, baler));

		LatestTelemetryResponse response = assetViewService.getLatestTelemetry();
		List<LatestTelemetryEntry> assets = response.getAssets();

		assertEquals(4, assets.size());
		assertEquals(AssetId.BALER, assets.get(0).getAssetId());
		assertEquals(AssetId.MIXER, assets.get(1).getAssetId());
		assertEquals(AssetId.MOWER, assets.get(2).getAssetId());
		assertEquals(AssetId.TRACTOR, assets.get(3).getAssetId());

		assertEquals(LiveCondition.ALERT, assets.get(0).getLiveCondition());
		assertNotNull(assets.get(0).getTelemetry());

		assertEquals(LiveCondition.STALE, assets.get(1).getLiveCondition());
		assertNull(assets.get(1).getTelemetry());
		assertEquals(LiveCondition.STALE, assets.get(2).getLiveCondition());
		assertNull(assets.get(2).getTelemetry());
	}

	private AssetState assetStateRow(AssetId assetId) {
		AssetState assetState = new AssetState();
		assetState.setAssetId(assetId.name());
		return assetState;
	}

	private LatestTelemetryEntry entryOf(LatestTelemetryResponse response, AssetId assetId) {
		for (LatestTelemetryEntry entry : response.getAssets()) {
			if (entry.getAssetId() == assetId) {
				return entry;
			}
		}
		throw new IllegalStateException("no entry returned for " + assetId);
	}

	private FlagView flagOf(List<FlagView> flags, Flag flag) {
		for (FlagView flagView : flags) {
			if (flagView.getFlag() == flag) {
				return flagView;
			}
		}
		throw new IllegalStateException("no flag returned for " + flag);
	}

	private void assertReasonIsNotEmpty(ResponseStatusException thrown) {
		assertNotNull(thrown.getReason());
		assertFalse(thrown.getReason().isBlank());
	}
}
