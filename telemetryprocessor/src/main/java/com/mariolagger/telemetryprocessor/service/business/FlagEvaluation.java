package com.mariolagger.telemetryprocessor.service.business;

import java.util.List;

/**
 * One REQ-FLAGS flag as it stands right now: whether it is active, the counter value behind
 * it and the threshold that value reached, so the reason survives all the way into the
 * response. An inactive flag carries no value and no threshold — GLOSSARY §5 gives it no
 * representation at all.
 */
public class FlagEvaluation {

	private final boolean active;
	private final Double value;
	private final Double threshold;

	private FlagEvaluation(boolean active, Double value, Double threshold) {
		this.active = active;
		this.value = value;
		this.threshold = threshold;
	}

	public static FlagEvaluation inactive() {
		return new FlagEvaluation(false, null, null);
	}

	public static FlagEvaluation active(double value, double threshold) {
		return new FlagEvaluation(true, value, threshold);
	}

	/** How many of these flags are active — REQ-STATUS and REQ-RANK both count. */
	public static int countActive(List<FlagEvaluation> flags) {
		int count = 0;
		for (FlagEvaluation flag : flags) {
			if (flag.isActive()) {
				count++;
			}
		}
		return count;
	}

	public boolean isActive() {
		return active;
	}

	public Double getValue() {
		return value;
	}

	public Double getThreshold() {
		return threshold;
	}
}
