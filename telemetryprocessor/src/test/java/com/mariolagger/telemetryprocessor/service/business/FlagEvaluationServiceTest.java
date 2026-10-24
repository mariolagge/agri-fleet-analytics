package com.mariolagger.telemetryprocessor.service.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mariolagger.telemetryprocessor.dto.FlagSeverity;

/**
 * No mocks: the service only compares counters with thresholds. The thresholds below are the
 * literal numbers from the REQ-FLAGS tables, not the service's constants, so a changed
 * constant fails here instead of being copied into the test.
 */
class FlagEvaluationServiceTest {

	private final FlagEvaluationService flagEvaluationService = new FlagEvaluationService();

	@Test
	@DisplayName("REQ-FLAGS: high load hours just below the moderate threshold leave heavy recent use inactive")
	void highLoadHoursJustBelowTheModerateThresholdLeaveHeavyRecentUseInactive() {
		FlagEvaluation heavyRecentUse = flagEvaluationService.heavyRecentUse(rolling24hCounters(1.4, 0.0, 0.0));

		assertInactive(heavyRecentUse);
	}

	@Test
	@DisplayName("REQ-FLAGS: high load hours exactly at the moderate threshold make heavy recent use moderate")
	void highLoadHoursExactlyAtTheModerateThresholdMakeHeavyRecentUseModerate() {
		FlagEvaluation heavyRecentUse = flagEvaluationService.heavyRecentUse(rolling24hCounters(1.5, 0.0, 0.0));

		assertEquals(FlagSeverity.MODERATE, heavyRecentUse.getSeverity());
		assertEquals(1.5, heavyRecentUse.getValue());
		assertEquals(1.5, heavyRecentUse.getThreshold());
	}

	/** Both rows are met, but the flag is reported once, at the threshold that actually fired. */
	@Test
	@DisplayName("REQ-FLAGS: high load hours above both thresholds give one severe flag with the severe threshold")
	void highLoadHoursAboveBothThresholdsGiveOneSevereFlagWithTheSevereThreshold() {
		FlagEvaluation heavyRecentUse = flagEvaluationService.heavyRecentUse(rolling24hCounters(3.0, 0.0, 0.0));

		assertEquals(FlagSeverity.SEVERE, heavyRecentUse.getSeverity());
		assertEquals(3.0, heavyRecentUse.getValue());
		assertEquals(2.5, heavyRecentUse.getThreshold());
	}

	@Test
	@DisplayName("REQ-FLAGS: temp excursion hours alone raise thermal load and no other rolling flag")
	void tempExcursionHoursAloneRaiseThermalLoadAndNoOtherRollingFlag() {
		CounterSet rolling24hCounters = rolling24hCounters(0.0, 0.8, 0.0);

		FlagEvaluation thermalLoad = flagEvaluationService.thermalLoad(rolling24hCounters);

		assertEquals(FlagSeverity.MODERATE, thermalLoad.getSeverity());
		assertEquals(0.8, thermalLoad.getThreshold());
		assertInactive(flagEvaluationService.heavyRecentUse(rolling24hCounters));
		assertInactive(flagEvaluationService.mechanicalAttention(rolling24hCounters));
	}

	@Test
	@DisplayName("REQ-FLAGS: vibration alert hours exactly at the severe threshold make mechanical attention severe")
	void vibrationAlertHoursExactlyAtTheSevereThresholdMakeMechanicalAttentionSevere() {
		FlagEvaluation mechanicalAttention = flagEvaluationService.mechanicalAttention(rolling24hCounters(0.0, 0.0, 2.0));

		assertEquals(FlagSeverity.SEVERE, mechanicalAttention.getSeverity());
		assertEquals(2.0, mechanicalAttention.getValue());
		assertEquals(2.0, mechanicalAttention.getThreshold());
	}

	@Test
	@DisplayName("REQ-FLAGS: lifetime loaded hours raise high cumulative load to moderate and then to severe")
	void lifetimeLoadedHoursRaiseHighCumulativeLoadToModerateAndThenToSevere() {
		FlagEvaluation moderate = flagEvaluationService.highCumulativeLoad(lifetimeCounters(850.0));
		FlagEvaluation severe = flagEvaluationService.highCumulativeLoad(lifetimeCounters(1200.0));

		assertEquals(FlagSeverity.MODERATE, moderate.getSeverity());
		assertEquals(850.0, moderate.getThreshold());
		assertEquals(FlagSeverity.SEVERE, severe.getSeverity());
		assertEquals(1200.0, severe.getThreshold());
	}

	/** GLOSSARY §5: an inactive flag has no severity, no value and no threshold. */
	private void assertInactive(FlagEvaluation flag) {
		assertNull(flag.getSeverity());
		assertNull(flag.getValue());
		assertNull(flag.getThreshold());
	}

	/** Only the three counters the rolling flags read are set; the other five stay 0. */
	private CounterSet rolling24hCounters(double highLoadHours, double tempExcursionHours,
			double vibrationAlertHours) {
		return new CounterSet(0.0, 0.0, 0.0, 0.0, 0.0, highLoadHours, tempExcursionHours, vibrationAlertHours);
	}

	/** Only loadedHours is set, because it is the one counter the lifetime flag reads. */
	private CounterSet lifetimeCounters(double loadedHours) {
		return new CounterSet(0.0, 0.0, 0.0, 0.0, loadedHours, 0.0, 0.0, 0.0);
	}
}
