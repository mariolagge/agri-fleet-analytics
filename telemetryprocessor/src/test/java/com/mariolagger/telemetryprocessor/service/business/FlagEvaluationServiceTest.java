package com.mariolagger.telemetryprocessor.service.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * No mocks: the service only compares counters with thresholds. The thresholds below are the
 * literal numbers from the REQ-FLAGS table, not the service's constants, so a changed
 * constant fails here instead of being copied into the test.
 */
class FlagEvaluationServiceTest {

	private final FlagEvaluationService flagEvaluationService = new FlagEvaluationService();

	@Test
	@DisplayName("REQ-FLAGS: high load hours just below the threshold leave heavy recent use inactive")
	void highLoadHoursJustBelowTheThresholdLeaveHeavyRecentUseInactive() {
		FlagEvaluation heavyRecentUse = flagEvaluationService.heavyRecentUse(rolling24hCounters(1.4, 0.0, 0.0));

		assertInactive(heavyRecentUse);
	}

	@Test
	@DisplayName("REQ-FLAGS: high load hours exactly at the threshold make heavy recent use active with its reason")
	void highLoadHoursExactlyAtTheThresholdMakeHeavyRecentUseActiveWithItsReason() {
		FlagEvaluation heavyRecentUse = flagEvaluationService.heavyRecentUse(rolling24hCounters(1.5, 0.0, 0.0));

		assertTrue(heavyRecentUse.isActive());
		assertEquals(1.5, heavyRecentUse.getValue());
		assertEquals(1.5, heavyRecentUse.getThreshold());
	}

	@Test
	@DisplayName("REQ-FLAGS: an active flag reports its actual value, not the threshold")
	void anActiveFlagReportsItsActualValueNotTheThreshold() {
		FlagEvaluation heavyRecentUse = flagEvaluationService.heavyRecentUse(rolling24hCounters(3.0, 0.0, 0.0));

		assertEquals(3.0, heavyRecentUse.getValue());
		assertEquals(1.5, heavyRecentUse.getThreshold());
	}

	@Test
	@DisplayName("REQ-FLAGS: temp excursion hours alone raise thermal load and no other rolling flag")
	void tempExcursionHoursAloneRaiseThermalLoadAndNoOtherRollingFlag() {
		CounterSet rolling24hCounters = rolling24hCounters(0.0, 0.8, 0.0);

		FlagEvaluation thermalLoad = flagEvaluationService.thermalLoad(rolling24hCounters);

		assertTrue(thermalLoad.isActive());
		assertEquals(0.8, thermalLoad.getThreshold());
		assertInactive(flagEvaluationService.heavyRecentUse(rolling24hCounters));
		assertInactive(flagEvaluationService.mechanicalAttention(rolling24hCounters));
	}

	@Test
	@DisplayName("REQ-FLAGS: vibration alert hours exactly at the threshold make mechanical attention active")
	void vibrationAlertHoursExactlyAtTheThresholdMakeMechanicalAttentionActive() {
		FlagEvaluation mechanicalAttention = flagEvaluationService.mechanicalAttention(rolling24hCounters(0.0, 0.0, 1.0));

		assertTrue(mechanicalAttention.isActive());
		assertEquals(1.0, mechanicalAttention.getThreshold());
	}

	@Test
	@DisplayName("REQ-FLAGS: lifetime high load hours make high cumulative load active from the threshold on")
	void lifetimeHighLoadHoursMakeHighCumulativeLoadActiveFromTheThresholdOn() {
		FlagEvaluation below = flagEvaluationService.highCumulativeLoad(lifetimeCounters(399.9));
		FlagEvaluation atThreshold = flagEvaluationService.highCumulativeLoad(lifetimeCounters(400.0));

		assertInactive(below);
		assertTrue(atThreshold.isActive());
		assertEquals(400.0, atThreshold.getThreshold());
	}

	/** GLOSSARY §5: an inactive flag has no value and no threshold. */
	private void assertInactive(FlagEvaluation flag) {
		assertFalse(flag.isActive());
		assertNull(flag.getValue());
		assertNull(flag.getThreshold());
	}

	/** Only the three counters the rolling flags read are set; the other two stay 0. */
	private CounterSet rolling24hCounters(double highLoadHours, double tempExcursionHours,
			double vibrationAlertHours) {
		return new CounterSet(0.0, 0.0, highLoadHours, tempExcursionHours, vibrationAlertHours);
	}

	/** Only highLoadHours is set, because it is the one counter the lifetime flag reads. */
	private CounterSet lifetimeCounters(double highLoadHours) {
		return new CounterSet(0.0, 0.0, highLoadHours, 0.0, 0.0);
	}
}
