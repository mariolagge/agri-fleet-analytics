package com.mariolagger.telemetryprocessor.service.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.mariolagger.telemetryprocessor.dto.AssetId;
import com.mariolagger.telemetryprocessor.dto.TelemetryMessage;
import com.mariolagger.telemetryprocessor.dto.VibrationLevel;
import com.mariolagger.telemetryprocessor.dto.WorkingStatus;

/**
 * Every time-dependent value is handed to the service, so these cases are expressed with
 * fixed instants and no clock at all. Europe/Vienna is UTC+2 in August 2026, so
 * 2026-08-13T06:30:00Z is local 08:30 and belongs to the bucket starting 06:00Z.
 */
class CounterAccumulationServiceTest {

	private static final AssetId ASSET = AssetId.TRACTOR;

	private static final Instant HOUR_06 = Instant.parse("2026-08-13T06:00:00Z");
	private static final Instant HOUR_07 = Instant.parse("2026-08-13T07:00:00Z");

	private static final Instant AT_06_30_00 = Instant.parse("2026-08-13T06:30:00Z");
	private static final Instant AT_06_30_15 = Instant.parse("2026-08-13T06:30:15Z");
	private static final Instant AT_06_30_30 = Instant.parse("2026-08-13T06:30:30Z");
	private static final Instant AT_06_31_00 = Instant.parse("2026-08-13T06:31:00Z");
	private static final Instant AT_06_31_30 = Instant.parse("2026-08-13T06:31:30Z");
	private static final Instant AT_06_32_00 = Instant.parse("2026-08-13T06:32:00Z");
	private static final Instant AT_06_32_30 = Instant.parse("2026-08-13T06:32:30Z");
	private static final Instant AT_06_59_30 = Instant.parse("2026-08-13T06:59:30Z");
	private static final Instant AT_07_00_10 = Instant.parse("2026-08-13T07:00:10Z");

	private static final double SECONDS_PER_HOUR = 3600.0;
	private static final double THIRTY_SECONDS_IN_HOURS = 30.0 / SECONDS_PER_HOUR;
	private static final double SIXTY_SECONDS_IN_HOURS = 60.0 / SECONDS_PER_HOUR;
	private static final double TOLERANCE = 1e-9;

	private final CounterAccumulationService counterAccumulationService = new CounterAccumulationService();

	@Test
	void sealableBucketsIsEmptyWhileTheStreamIsStillInsideTheHour() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_30));
		counterAccumulationService.accumulate(productiveMessage(AT_06_31_00));

		assertTrue(counterAccumulationService.sealableBuckets(ASSET).isEmpty());
	}

	@Test
	void hourBecomesSealableOnceAMessageOfTheNextHourArrives() {
		accumulateAcrossTheHourBoundary();

		Map<Instant, CounterSet> sealable = counterAccumulationService.sealableBuckets(ASSET);

		assertEquals(1, sealable.size());
		assertEquals(Set.of(HOUR_06), sealable.keySet());
		assertEquals(SIXTY_SECONDS_IN_HOURS, sealable.get(HOUR_06).getEngineHours(), TOLERANCE);
	}

	@Test
	void sealableBucketsReturnsCopies() {
		accumulateAcrossTheHourBoundary();

		counterAccumulationService.sealableBuckets(ASSET).get(HOUR_06).addEngineHours(100.0);

		Map<Instant, CounterSet> sealable = counterAccumulationService.sealableBuckets(ASSET);
		assertEquals(SIXTY_SECONDS_IN_HOURS, sealable.get(HOUR_06).getEngineHours(), TOLERANCE);
	}

	@Test
	void sealableBucketsIsRepeatableUntilMarkSealed() {
		accumulateAcrossTheHourBoundary();

		assertEquals(Set.of(HOUR_06), counterAccumulationService.sealableBuckets(ASSET).keySet());
		assertEquals(Set.of(HOUR_06), counterAccumulationService.sealableBuckets(ASSET).keySet());

		counterAccumulationService.markSealed(ASSET, Set.of(HOUR_06));

		assertTrue(counterAccumulationService.sealableBuckets(ASSET).isEmpty());
	}

	/** SPEC §4.2 step 3. */
	@Test
	void deltaAboveSixtySecondsCreditsNothingButAdvancesTheLastProcessedTimestamp() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));
		counterAccumulationService.accumulate(productiveMessage(AT_06_32_00));
		counterAccumulationService.accumulate(productiveMessage(AT_06_32_30));

		// 30 s, measured from the discarded message; measured from 06:30:00 it would be 150 s
		// and nothing would have been credited at all.
		assertEquals(THIRTY_SECONDS_IN_HOURS, rolling24hCounters().getEngineHours(), TOLERANCE);
	}

	/** SPEC §4.2 step 3: above 60 s, not 60 s itself. */
	@Test
	void deltaOfExactlySixtySecondsIsCredited() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));
		counterAccumulationService.accumulate(productiveMessage(AT_06_31_00));

		assertEquals(SIXTY_SECONDS_IN_HOURS, rolling24hCounters().getEngineHours(), TOLERANCE);
	}

	/** SPEC §4.2 step 1. */
	@Test
	void outOfOrderOrDuplicateTimestampUpdatesNothing() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));
		TelemetryMessage lastAcceptedMessage = productiveMessage(AT_06_30_30);
		counterAccumulationService.accumulate(lastAcceptedMessage);

		counterAccumulationService.accumulate(productiveMessage(AT_06_30_30));
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_15));

		assertEquals(THIRTY_SECONDS_IN_HOURS, rolling24hCounters().getEngineHours(), TOLERANCE);
		assertSame(lastAcceptedMessage, snapshot().getLatestMessage());

		// The last-processed timestamp is still 06:30:30, so this credits 30 s and not 45 s.
		counterAccumulationService.accumulate(productiveMessage(AT_06_31_00));
		assertEquals(SIXTY_SECONDS_IN_HOURS, rolling24hCounters().getEngineHours(), TOLERANCE);
	}

	/** SPEC §4.2 step 2: Δt is undefined for the first message of an asset. */
	@Test
	void firstMessageOfAnAssetCreditsNothing() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));

		assertEquals(0.0, rolling24hCounters().getEngineHours(), TOLERANCE);
	}

	/** SPEC §4.1: the three buckets partition engine hours. */
	@Test
	void engineHoursEqualsWorkingPlusTransportPlusIdle() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_30));
		counterAccumulationService.accumulate(transportMessage(AT_06_31_00));
		counterAccumulationService.accumulate(idleMessage(AT_06_31_30));

		CounterSet rolling24hCounters = rolling24hCounters();
		assertEquals(THIRTY_SECONDS_IN_HOURS, rolling24hCounters.getTotalWorkingHours(), TOLERANCE);
		assertEquals(THIRTY_SECONDS_IN_HOURS, rolling24hCounters.getTransportHours(), TOLERANCE);
		assertEquals(THIRTY_SECONDS_IN_HOURS, rolling24hCounters.getIdleHours(), TOLERANCE);
		assertEquals(rolling24hCounters.getEngineHours(), rolling24hCounters.getTotalWorkingHours()
				+ rolling24hCounters.getTransportHours() + rolling24hCounters.getIdleHours(), TOLERANCE);
	}

	/** SPEC §4.1: productive work is pto plus ground speed, never the reported status. */
	@Test
	void productiveWorkIsDerivedFromPtoAndGroundSpeedNotFromWorkingStatus() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));
		counterAccumulationService.accumulate(
				message(AT_06_30_30, false, 8.0, 0.6, 70.0, VibrationLevel.NORMAL, WorkingStatus.WORKING));

		CounterSet rolling24hCounters = rolling24hCounters();
		assertEquals(0.0, rolling24hCounters.getTotalWorkingHours(), TOLERANCE);
		assertEquals(THIRTY_SECONDS_IN_HOURS, rolling24hCounters.getIdleHours(), TOLERANCE);
	}

	/** SPEC §4.5: the percentage is the quotient of the counters published beside it. */
	@Test
	void utilizationPercentIsTheQuotientOfTheSnapshotsOwnCountersAndCoverage() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_30));

		AssetSnapshot snapshot = snapshot();

		assertEquals(0.5, snapshot.getWindowCoverageHours(), TOLERANCE);
		assertNotNull(snapshot.getUtilizationPercent());
		assertEquals(snapshot.getRolling24hCounters().getTotalWorkingHours() / snapshot.getWindowCoverageHours() * 100.0,
				snapshot.getUtilizationPercent(), TOLERANCE);
	}

	/** SPEC §4.5: null, not 0.0, when the denominator is 0. */
	@Test
	void assetThatNeverReportedHasNoCoverageAndNullPercentages() {
		AssetSnapshot snapshot = counterAccumulationService.snapshotOf(AssetId.MOWER, HOUR_07);

		assertEquals(0.0, snapshot.getWindowCoverageHours(), TOLERANCE);
		assertNull(snapshot.getUtilizationPercent());
		assertNull(snapshot.getProductiveSharePercent());
	}

	/**
	 * Leaves the 06:00Z bucket holding exactly 60 s of engine hours and the asset's stream
	 * inside the 07:00Z hour: 06:30:00 is the first message and credits nothing, the two
	 * following credit 30 s each, 06:59:30 is more than 60 s later and credits nothing, and
	 * 07:00:10 credits its 40 s into the next hour.
	 */
	private void accumulateAcrossTheHourBoundary() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_30));
		counterAccumulationService.accumulate(productiveMessage(AT_06_31_00));
		counterAccumulationService.accumulate(productiveMessage(AT_06_59_30));
		counterAccumulationService.accumulate(productiveMessage(AT_07_00_10));
	}

	private AssetSnapshot snapshot() {
		return counterAccumulationService.snapshotOf(ASSET, HOUR_07);
	}

	private CounterSet rolling24hCounters() {
		return snapshot().getRolling24hCounters();
	}

	private TelemetryMessage productiveMessage(Instant timestamp) {
		return message(timestamp, true, 8.0, 0.6, 70.0, VibrationLevel.NORMAL, WorkingStatus.WORKING);
	}

	private TelemetryMessage transportMessage(Instant timestamp) {
		return message(timestamp, false, 22.0, 0.2, 70.0, VibrationLevel.NORMAL, WorkingStatus.TRANSPORT);
	}

	private TelemetryMessage idleMessage(Instant timestamp) {
		return message(timestamp, false, 0.0, 0.1, 70.0, VibrationLevel.NORMAL, WorkingStatus.IDLE);
	}

	private TelemetryMessage message(Instant timestamp, boolean ptoEngaged, double groundSpeedKmh, double loadFactor,
			double temperatureC, VibrationLevel vibrationLevel, WorkingStatus workingStatus) {
		TelemetryMessage message = new TelemetryMessage();
		message.setAssetId(ASSET);
		message.setTimestamp(timestamp);
		message.setPtoEngaged(ptoEngaged);
		message.setGroundSpeedKmh(groundSpeedKmh);
		message.setLoadFactor(loadFactor);
		message.setTemperatureC(temperatureC);
		message.setVibrationLevel(vibrationLevel);
		message.setWorkingStatus(workingStatus);
		return message;
	}
}
