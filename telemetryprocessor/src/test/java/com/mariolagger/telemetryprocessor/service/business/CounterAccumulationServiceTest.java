package com.mariolagger.telemetryprocessor.service.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mariolagger.telemetryprocessor.dto.AssetId;
import com.mariolagger.telemetryprocessor.dto.TelemetryMessage;
import com.mariolagger.telemetryprocessor.dto.VibrationLevel;

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
	@DisplayName("ARCHITECTURE §4: sealable buckets stay empty while the stream is still inside the hour")
	void sealableBucketsIsEmptyWhileTheStreamIsStillInsideTheHour() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_30));
		counterAccumulationService.accumulate(productiveMessage(AT_06_31_00));

		assertTrue(counterAccumulationService.sealableBuckets(ASSET).isEmpty());
	}

	@Test
	@DisplayName("ARCHITECTURE §4: an hour becomes sealable once a message of the next hour arrives")
	void hourBecomesSealableOnceAMessageOfTheNextHourArrives() {
		accumulateAcrossTheHourBoundary();

		Map<Instant, CounterSet> sealable = counterAccumulationService.sealableBuckets(ASSET);

		assertEquals(1, sealable.size());
		assertEquals(Set.of(HOUR_06), sealable.keySet());
		assertEquals(SIXTY_SECONDS_IN_HOURS, sealable.get(HOUR_06).getEngineHours(), TOLERANCE);
	}

	@Test
	@DisplayName("ARCHITECTURE §4: sealable buckets are returned as copies")
	void sealableBucketsReturnsCopies() {
		accumulateAcrossTheHourBoundary();

		counterAccumulationService.sealableBuckets(ASSET).get(HOUR_06).addEngineHours(100.0);

		Map<Instant, CounterSet> sealable = counterAccumulationService.sealableBuckets(ASSET);
		assertEquals(SIXTY_SECONDS_IN_HOURS, sealable.get(HOUR_06).getEngineHours(), TOLERANCE);
	}

	@Test
	@DisplayName("ARCHITECTURE §4: sealable buckets are offered again until they are marked sealed")
	void sealableBucketsIsRepeatableUntilMarkSealed() {
		accumulateAcrossTheHourBoundary();

		assertEquals(Set.of(HOUR_06), counterAccumulationService.sealableBuckets(ASSET).keySet());
		assertEquals(Set.of(HOUR_06), counterAccumulationService.sealableBuckets(ASSET).keySet());

		counterAccumulationService.markSealed(ASSET, Set.of(HOUR_06));

		assertTrue(counterAccumulationService.sealableBuckets(ASSET).isEmpty());
	}

	/** REQ-ACCUMULATE step 3. */
	@Test
	@DisplayName("REQ-ACCUMULATE: a gap above 60 s credits nothing but advances the last processed timestamp")
	void deltaAboveSixtySecondsCreditsNothingButAdvancesTheLastProcessedTimestamp() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));
		counterAccumulationService.accumulate(productiveMessage(AT_06_32_00));
		counterAccumulationService.accumulate(productiveMessage(AT_06_32_30));

		// 30 s, measured from the discarded message; measured from 06:30:00 it would be 150 s
		// and nothing would have been credited at all.
		assertEquals(THIRTY_SECONDS_IN_HOURS, rolling24hCounters().getEngineHours(), TOLERANCE);
	}

	/** REQ-ACCUMULATE step 3: above 60 s, not 60 s itself. */
	@Test
	@DisplayName("REQ-ACCUMULATE: a gap of exactly 60 s is credited")
	void deltaOfExactlySixtySecondsIsCredited() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));
		counterAccumulationService.accumulate(productiveMessage(AT_06_31_00));

		assertEquals(SIXTY_SECONDS_IN_HOURS, rolling24hCounters().getEngineHours(), TOLERANCE);
	}

	/** REQ-ACCUMULATE step 1. */
	@Test
	@DisplayName("REQ-ACCUMULATE: an out-of-order or duplicate timestamp updates nothing")
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

	/** REQ-ACCUMULATE step 2: Δt is undefined for the first message of an asset. */
	@Test
	@DisplayName("REQ-ACCUMULATE: the first message of an asset credits nothing")
	void firstMessageOfAnAssetCreditsNothing() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));

		assertEquals(0.0, rolling24hCounters().getEngineHours(), TOLERANCE);
	}

	/** REQ-COUNTER-SET: pto alone or speed alone is not productive work; speed must be above 2, not 2. */
	@Test
	@DisplayName("REQ-COUNTER-SET: productive work needs both pto engaged and ground speed above 2")
	void productiveWorkNeedsBothPtoEngagedAndGroundSpeedAboveTwo() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));
		counterAccumulationService.accumulate(message(AT_06_30_30, false, 8.0, 0.6, 70.0, VibrationLevel.NORMAL));
		counterAccumulationService.accumulate(message(AT_06_31_00, true, 2.0, 0.6, 70.0, VibrationLevel.NORMAL));
		counterAccumulationService.accumulate(productiveMessage(AT_06_31_30));

		CounterSet rolling24hCounters = rolling24hCounters();
		assertEquals(3 * THIRTY_SECONDS_IN_HOURS, rolling24hCounters.getEngineHours(), TOLERANCE);
		assertEquals(THIRTY_SECONDS_IN_HOURS, rolling24hCounters.getTotalWorkingHours(), TOLERANCE);
	}

	/** REQ-COUNTER-SET: load counts only while working, and the 0.75 bar is inclusive. */
	@Test
	@DisplayName("REQ-COUNTER-SET: high load hours accrue only during productive work at a load factor of 0.75 or more")
	void highLoadHoursAccrueOnlyDuringProductiveWorkAtALoadFactorOfPointSevenFiveOrMore() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));
		counterAccumulationService.accumulate(message(AT_06_30_30, true, 8.0, 0.75, 70.0, VibrationLevel.NORMAL));
		counterAccumulationService.accumulate(message(AT_06_31_00, false, 0.0, 0.9, 70.0, VibrationLevel.NORMAL));
		counterAccumulationService.accumulate(message(AT_06_31_30, true, 8.0, 0.74, 70.0, VibrationLevel.NORMAL));

		assertEquals(THIRTY_SECONDS_IN_HOURS, rolling24hCounters().getHighLoadHours(), TOLERANCE);
	}

	/** REQ-COUNTER-SET: an idle machine can still overheat or vibrate. */
	@Test
	@DisplayName("REQ-COUNTER-SET: temperature and vibration hours accrue regardless of working state")
	void temperatureAndVibrationHoursAccrueRegardlessOfWorkingState() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));
		counterAccumulationService.accumulate(message(AT_06_30_30, false, 0.0, 0.1, 90.1, VibrationLevel.ELEVATED));
		counterAccumulationService.accumulate(message(AT_06_31_00, false, 0.0, 0.1, 90.0, VibrationLevel.HIGH));

		CounterSet rolling24hCounters = rolling24hCounters();
		assertEquals(THIRTY_SECONDS_IN_HOURS, rolling24hCounters.getTempExcursionHours(), TOLERANCE);
		assertEquals(SIXTY_SECONDS_IN_HOURS, rolling24hCounters.getVibrationAlertHours(), TOLERANCE);
	}

	@Test
	@DisplayName("REQ-COUNTER-SET: engine hours are at least working hours, which are at least high load hours")
	void engineHoursAreAtLeastWorkingHoursWhichAreAtLeastHighLoadHours() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));
		counterAccumulationService.accumulate(message(AT_06_30_30, true, 8.0, 0.9, 70.0, VibrationLevel.NORMAL));
		counterAccumulationService.accumulate(productiveMessage(AT_06_31_00));
		counterAccumulationService.accumulate(message(AT_06_31_30, false, 20.0, 0.2, 65.0, VibrationLevel.NORMAL));

		CounterSet rolling24hCounters = rolling24hCounters();
		assertTrue(rolling24hCounters.getEngineHours() >= rolling24hCounters.getTotalWorkingHours());
		assertTrue(rolling24hCounters.getTotalWorkingHours() >= rolling24hCounters.getHighLoadHours());
		assertEquals(3 * THIRTY_SECONDS_IN_HOURS, rolling24hCounters.getEngineHours(), TOLERANCE);
		assertEquals(2 * THIRTY_SECONDS_IN_HOURS, rolling24hCounters.getTotalWorkingHours(), TOLERANCE);
		assertEquals(THIRTY_SECONDS_IN_HOURS, rolling24hCounters.getHighLoadHours(), TOLERANCE);
	}

	/** REQ-SCOPES and REQ-BASELINES: one message lands in both scopes, but only lifetime starts from the baseline. */
	@Test
	@DisplayName("REQ-SCOPES: lifetime is the baseline plus this run, while the rolling window starts empty")
	void lifetimeIsTheBaselinePlusThisRunWhileTheRollingWindowStartsEmpty() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_30));

		AssetSnapshot snapshot = snapshot();
		double baselineEngineHours = AssetBaselines.baselineFor(ASSET).getEngineHours();
		assertEquals(THIRTY_SECONDS_IN_HOURS, snapshot.getRolling24hCounters().getEngineHours(), TOLERANCE);
		assertEquals(baselineEngineHours + THIRTY_SECONDS_IN_HOURS, snapshot.getLifetimeCounters().getEngineHours(),
				TOLERANCE);
	}

	/**
	 * REQ-SCOPES: the window is the open bucket plus the 23 completed before it. At 05:30Z the
	 * next day the 06:00Z bucket is the oldest one still inside; at 06:30Z it has dropped out.
	 */
	@Test
	@DisplayName("REQ-SCOPES: an hour leaves the rolling window after 23 completed hours but stays in lifetime")
	void anHourLeavesTheRollingWindowAfterTwentyThreeCompletedHoursButStaysInLifetime() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_30));

		AssetSnapshot lastHourInside = counterAccumulationService.snapshotOf(ASSET,
				Instant.parse("2026-08-14T05:30:00Z"));
		AssetSnapshot firstHourOutside = counterAccumulationService.snapshotOf(ASSET,
				Instant.parse("2026-08-14T06:30:00Z"));

		assertEquals(THIRTY_SECONDS_IN_HOURS, lastHourInside.getRolling24hCounters().getEngineHours(), TOLERANCE);
		assertEquals(0.0, firstHourOutside.getRolling24hCounters().getEngineHours(), TOLERANCE);
		assertEquals(lastHourInside.getLifetimeCounters().getEngineHours(),
				firstHourOutside.getLifetimeCounters().getEngineHours(), TOLERANCE);
	}

	/** REQ-UTILIZATION: the percentage is the quotient of the counters published beside it. */
	@Test
	@DisplayName("REQ-UTILIZATION: utilization percent is the quotient of the snapshot's own counters and coverage")
	void utilizationPercentIsTheQuotientOfTheSnapshotsOwnCountersAndCoverage() {
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_00));
		counterAccumulationService.accumulate(productiveMessage(AT_06_30_30));

		AssetSnapshot snapshot = snapshot();

		assertEquals(0.5, snapshot.getWindowCoverageHours(), TOLERANCE);
		assertNotNull(snapshot.getUtilizationPercent());
		assertEquals(snapshot.getRolling24hCounters().getTotalWorkingHours() / snapshot.getWindowCoverageHours() * 100.0,
				snapshot.getUtilizationPercent(), TOLERANCE);
	}

	/** REQ-UTILIZATION: null, not 0.0, when the denominator is 0. */
	@Test
	@DisplayName("REQ-UTILIZATION: an asset that never reported has no coverage and null percentages")
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
		return message(timestamp, true, 8.0, 0.6, 70.0, VibrationLevel.NORMAL);
	}

	private TelemetryMessage message(Instant timestamp, boolean ptoEngaged, double groundSpeedKmh, double loadFactor,
			double temperatureC, VibrationLevel vibrationLevel) {
		TelemetryMessage message = new TelemetryMessage();
		message.setAssetId(ASSET);
		message.setTimestamp(timestamp);
		message.setPtoEngaged(ptoEngaged);
		message.setGroundSpeedKmh(groundSpeedKmh);
		message.setLoadFactor(loadFactor);
		message.setTemperatureC(temperatureC);
		message.setVibrationLevel(vibrationLevel);
		return message;
	}
}
