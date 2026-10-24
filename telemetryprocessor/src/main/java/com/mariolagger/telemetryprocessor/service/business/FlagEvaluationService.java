package com.mariolagger.telemetryprocessor.service.business;

import org.springframework.stereotype.Component;

import com.mariolagger.telemetryprocessor.dto.FlagSeverity;

/**
 * REQ-FLAGS. The eight threshold rows collapse into at most four flags: one per counter,
 * each at the highest severity it currently meets.
 */
@Component
public class FlagEvaluationService {

	// REQ-FLAGS, rolling 24 h scope.
	private static final double HEAVY_RECENT_USE_MODERATE_HOURS = 1.5;
	private static final double HEAVY_RECENT_USE_SEVERE_HOURS = 2.5;
	private static final double THERMAL_LOAD_MODERATE_HOURS = 0.8;
	private static final double THERMAL_LOAD_SEVERE_HOURS = 1.5;
	private static final double MECHANICAL_ATTENTION_MODERATE_HOURS = 1.0;
	private static final double MECHANICAL_ATTENTION_SEVERE_HOURS = 2.0;

	// REQ-FLAGS, lifetime scope.
	private static final double HIGH_CUMULATIVE_LOAD_MODERATE_HOURS = 850.0;
	private static final double HIGH_CUMULATIVE_LOAD_SEVERE_HOURS = 1200.0;

	/** HEAVY_RECENT_USE — highLoadHours (24 h). */
	public FlagEvaluation heavyRecentUse(CounterSet rolling24hCounters) {
		return evaluate(rolling24hCounters.getHighLoadHours(), HEAVY_RECENT_USE_MODERATE_HOURS,
				HEAVY_RECENT_USE_SEVERE_HOURS);
	}

	/** THERMAL_LOAD — tempExcursionHours (24 h). */
	public FlagEvaluation thermalLoad(CounterSet rolling24hCounters) {
		return evaluate(rolling24hCounters.getTempExcursionHours(), THERMAL_LOAD_MODERATE_HOURS,
				THERMAL_LOAD_SEVERE_HOURS);
	}

	/** MECHANICAL_ATTENTION — vibrationAlertHours (24 h). */
	public FlagEvaluation mechanicalAttention(CounterSet rolling24hCounters) {
		return evaluate(rolling24hCounters.getVibrationAlertHours(), MECHANICAL_ATTENTION_MODERATE_HOURS,
				MECHANICAL_ATTENTION_SEVERE_HOURS);
	}

	/** HIGH_CUMULATIVE_LOAD — loadedHours (lifetime). */
	public FlagEvaluation highCumulativeLoad(CounterSet lifetimeCounters) {
		return evaluate(lifetimeCounters.getLoadedHours(), HIGH_CUMULATIVE_LOAD_MODERATE_HOURS,
				HIGH_CUMULATIVE_LOAD_SEVERE_HOURS);
	}

	/**
	 * SEVERE is checked first, so a counter that meets both rows produces one flag at its
	 * highest severity and the threshold reported is the one that actually fired.
	 */
	private FlagEvaluation evaluate(double value, double moderateThreshold, double severeThreshold) {
		if (value >= severeThreshold) {
			return FlagEvaluation.active(FlagSeverity.SEVERE, value, severeThreshold);
		}
		if (value >= moderateThreshold) {
			return FlagEvaluation.active(FlagSeverity.MODERATE, value, moderateThreshold);
		}
		return FlagEvaluation.inactive();
	}
}
