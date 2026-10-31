package com.mariolagger.simulator.service.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.function.Predicate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mariolagger.simulator.dto.AssetId;
import com.mariolagger.simulator.dto.TelemetryMessage;
import com.mariolagger.simulator.dto.VibrationLevel;

/**
 * No mocks and no randomness: the schedule makes every message a function of asset and
 * timestamp, so each test can name the exact instant it checks.
 */
class TelemetryGeneratorTest {

	/** A multiple of both cycle lengths (120 and 420 minutes), so every asset starts a cycle here. */
	private static final Instant CYCLE_START = Instant.ofEpochSecond(1_764_000_000L);
	private static final long STEP_SECONDS = 60;
	private static final int MINUTES_PER_DAY = 24 * 60;

	// REQ-FLAGS thresholds for the rolling 24 h scope, in hours.
	private static final double HEAVY_RECENT_USE_THRESHOLD = 1.5;
	private static final double THERMAL_LOAD_THRESHOLD = 0.8;
	private static final double MECHANICAL_ATTENTION_THRESHOLD = 1.0;

	private final TelemetryGenerator telemetryGenerator = new TelemetryGenerator();

	@Test
	@DisplayName("REQ-TELEMETRY-SHAPE: every message stays within the field constraints and carries its asset and timestamp")
	void everyMessageStaysWithinTheFieldConstraintsAndCarriesItsAssetAndTimestamp() {
		for (AssetId assetId : AssetId.values()) {
			for (int minute = 0; minute < 2 * MINUTES_PER_DAY; minute++) {
				Instant timestamp = minutesAfterCycleStart(minute);
				TelemetryMessage message = telemetryGenerator.generate(assetId, timestamp);
				assertTrue(message.getGroundSpeedKmh() >= 0.0);
				assertTrue(message.getLoadFactor() >= 0.0);
				assertTrue(message.getLoadFactor() <= 1.0);
				assertEquals(assetId, message.getAssetId());
				assertEquals(timestamp, message.getTimestamp());
			}
		}
	}

	@Test
	@DisplayName("REQ-SIM-SCHEDULE: the same asset and instant always produce the same message")
	void theSameAssetAndInstantAlwaysProduceTheSameMessage() {
		Instant timestamp = Instant.parse("2026-08-13T09:41:07Z");
		for (AssetId assetId : AssetId.values()) {
			TelemetryMessage first = telemetryGenerator.generate(assetId, timestamp);
			TelemetryMessage second = telemetryGenerator.generate(assetId, timestamp);
			assertEquals(first.isPtoEngaged(), second.isPtoEngaged());
			assertEquals(first.getGroundSpeedKmh(), second.getGroundSpeedKmh());
			assertEquals(first.getLoadFactor(), second.getLoadFactor());
			assertEquals(first.getTemperatureC(), second.getTemperatureC());
			assertEquals(first.getVibrationLevel(), second.getVibrationLevel());
		}
	}

	@Test
	@DisplayName("REQ-SIM-SCHEDULE: each asset reports the values of the segment its timestamp falls in")
	void eachAssetReportsTheValuesOfTheSegmentItsTimestampFallsIn() {
		TelemetryMessage tractorWorking = telemetryGenerator.generate(AssetId.TRACTOR, minutesAfterCycleStart(30));
		assertTrue(tractorWorking.isPtoEngaged());
		assertEquals(0.80, tractorWorking.getLoadFactor());
		assertEquals(80.0, tractorWorking.getTemperatureC());

		TelemetryMessage tractorHot = telemetryGenerator.generate(AssetId.TRACTOR, minutesAfterCycleStart(65));
		assertEquals(94.0, tractorHot.getTemperatureC());

		TelemetryMessage tractorTransport = telemetryGenerator.generate(AssetId.TRACTOR, minutesAfterCycleStart(75));
		assertEquals(false, tractorTransport.isPtoEngaged());
		assertEquals(20.0, tractorTransport.getGroundSpeedKmh());

		TelemetryMessage tractorIdle = telemetryGenerator.generate(AssetId.TRACTOR, minutesAfterCycleStart(100));
		assertEquals(0.0, tractorIdle.getGroundSpeedKmh());

		TelemetryMessage tractorNextCycle = telemetryGenerator.generate(AssetId.TRACTOR, minutesAfterCycleStart(120 + 65));
		assertEquals(94.0, tractorNextCycle.getTemperatureC());

		TelemetryMessage mowerVibrating = telemetryGenerator.generate(AssetId.MOWER, minutesAfterCycleStart(190));
		assertEquals(VibrationLevel.HIGH, mowerVibrating.getVibrationLevel());
	}

	@Test
	@DisplayName("REQ-SIM-DEMO: over a rolling window TRACTOR meets two flags, BALER only HEAVY_RECENT_USE and MIXER none")
	void overARollingWindowTractorMeetsTwoFlagsBalerOnlyHeavyRecentUseAndMixerNone() {
		// 23 h is the shortest rolling window (REQ-SCOPES), so it is the hardest case for an active flag.
		Instant windowEnd = minutesAfterCycleStart(10 * MINUTES_PER_DAY);
		int windowMinutes = 23 * 60;

		assertTrue(highLoadHours(AssetId.TRACTOR, windowEnd, windowMinutes) >= HEAVY_RECENT_USE_THRESHOLD);
		assertTrue(tempExcursionHours(AssetId.TRACTOR, windowEnd, windowMinutes) >= THERMAL_LOAD_THRESHOLD);

		assertTrue(highLoadHours(AssetId.BALER, windowEnd, windowMinutes) >= HEAVY_RECENT_USE_THRESHOLD);
		assertTrue(tempExcursionHours(AssetId.BALER, windowEnd, windowMinutes) < THERMAL_LOAD_THRESHOLD);
		assertTrue(vibrationAlertHours(AssetId.BALER, windowEnd, windowMinutes) < MECHANICAL_ATTENTION_THRESHOLD);

		assertEquals(0.0, highLoadHours(AssetId.MIXER, windowEnd, MINUTES_PER_DAY));
		assertEquals(0.0, tempExcursionHours(AssetId.MIXER, windowEnd, MINUTES_PER_DAY));
		assertEquals(0.0, vibrationAlertHours(AssetId.MIXER, windowEnd, MINUTES_PER_DAY));
	}

	@Test
	@DisplayName("REQ-SIM-DEMO: MOWER's 24 h vibration hours fall below and rise above the MECHANICAL_ATTENTION threshold")
	void mowerVibrationHoursFallBelowAndRiseAboveTheMechanicalAttentionThreshold() {
		// Just before a vibration segment the window holds three of them, just after it four.
		double beforeSegment = vibrationAlertHours(AssetId.MOWER, minutesAfterCycleStart(179), MINUTES_PER_DAY);
		double afterSegment = vibrationAlertHours(AssetId.MOWER, minutesAfterCycleStart(198), MINUTES_PER_DAY);

		assertEquals(0.9, beforeSegment, 1e-9);
		assertEquals(1.2, afterSegment, 1e-9);
	}

	private double highLoadHours(AssetId assetId, Instant windowEnd, int windowMinutes) {
		return hoursMatching(assetId, windowEnd, windowMinutes,
				message -> message.isPtoEngaged() && message.getGroundSpeedKmh() > 2 && message.getLoadFactor() >= 0.75);
	}

	private double tempExcursionHours(AssetId assetId, Instant windowEnd, int windowMinutes) {
		return hoursMatching(assetId, windowEnd, windowMinutes, message -> message.getTemperatureC() > 90);
	}

	private double vibrationAlertHours(AssetId assetId, Instant windowEnd, int windowMinutes) {
		return hoursMatching(assetId, windowEnd, windowMinutes,
				message -> message.getVibrationLevel() != VibrationLevel.NORMAL);
	}

	/**
	 * Steps through the window one minute at a time, as the startup burst does, and credits each
	 * message's minute when it matches — the interval belongs to the message at its end (REQ-ACCUMULATE).
	 */
	private double hoursMatching(AssetId assetId, Instant windowEnd, int windowMinutes,
			Predicate<TelemetryMessage> condition) {
		int matchingMinutes = 0;
		for (int minute = windowMinutes - 1; minute >= 0; minute--) {
			Instant timestamp = windowEnd.minusSeconds(minute * STEP_SECONDS);
			if (condition.test(telemetryGenerator.generate(assetId, timestamp))) {
				matchingMinutes++;
			}
		}
		return matchingMinutes / 60.0;
	}

	private Instant minutesAfterCycleStart(int minutes) {
		return CYCLE_START.plusSeconds(minutes * STEP_SECONDS);
	}
}
