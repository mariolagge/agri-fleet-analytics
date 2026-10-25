package com.mariolagger.telemetryprocessor.service.business;

import org.springframework.stereotype.Component;

/**
 * REQ-FLAGS. Four flags, one threshold each: a flag is active when its counter has reached
 * the threshold.
 */
@Component
public class FlagEvaluationService {

	// REQ-FLAGS, rolling 24 h scope.
	private static final double HEAVY_RECENT_USE_THRESHOLD_HOURS = 1.5;
	private static final double THERMAL_LOAD_THRESHOLD_HOURS = 0.8;
	private static final double MECHANICAL_ATTENTION_THRESHOLD_HOURS = 1.0;

	// REQ-FLAGS, lifetime scope.
	private static final double HIGH_CUMULATIVE_LOAD_THRESHOLD_HOURS = 400.0;

	/** HEAVY_RECENT_USE — highLoadHours (24 h). */
	public FlagEvaluation heavyRecentUse(CounterSet rolling24hCounters) {
		return evaluate(rolling24hCounters.getHighLoadHours(), HEAVY_RECENT_USE_THRESHOLD_HOURS);
	}

	/** THERMAL_LOAD — tempExcursionHours (24 h). */
	public FlagEvaluation thermalLoad(CounterSet rolling24hCounters) {
		return evaluate(rolling24hCounters.getTempExcursionHours(), THERMAL_LOAD_THRESHOLD_HOURS);
	}

	/** MECHANICAL_ATTENTION — vibrationAlertHours (24 h). */
	public FlagEvaluation mechanicalAttention(CounterSet rolling24hCounters) {
		return evaluate(rolling24hCounters.getVibrationAlertHours(), MECHANICAL_ATTENTION_THRESHOLD_HOURS);
	}

	/** HIGH_CUMULATIVE_LOAD — highLoadHours (lifetime). */
	public FlagEvaluation highCumulativeLoad(CounterSet lifetimeCounters) {
		return evaluate(lifetimeCounters.getHighLoadHours(), HIGH_CUMULATIVE_LOAD_THRESHOLD_HOURS);
	}

	private FlagEvaluation evaluate(double value, double threshold) {
		if (value >= threshold) {
			return FlagEvaluation.active(value, threshold);
		}
		return FlagEvaluation.inactive();
	}
}
