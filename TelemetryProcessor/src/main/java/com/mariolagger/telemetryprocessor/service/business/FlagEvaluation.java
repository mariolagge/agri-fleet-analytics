package com.mariolagger.telemetryprocessor.service.business;

import java.util.List;

import com.mariolagger.telemetryprocessor.dto.FlagSeverity;

/**
 * One SPEC §5.1 flag as it stands right now: its severity, the counter value behind it and
 * the threshold that value crossed, so the reason survives all the way into the response.
 * An inactive flag carries no severity, no value and no threshold — GLOSSARY §5 gives it
 * no representation at all.
 */
public class FlagEvaluation {

	private final FlagSeverity severity;
	private final Double value;
	private final Double threshold;

	private FlagEvaluation(FlagSeverity severity, Double value, Double threshold) {
		this.severity = severity;
		this.value = value;
		this.threshold = threshold;
	}

	public static FlagEvaluation inactive() {
		return new FlagEvaluation(null, null, null);
	}

	public static FlagEvaluation active(FlagSeverity severity, double value, double threshold) {
		return new FlagEvaluation(severity, value, threshold);
	}

	/** How many of these flags currently sit at one severity — SPEC §5.2 and §5.4 both count. */
	public static int countAtSeverity(List<FlagEvaluation> flags, FlagSeverity severity) {
		int count = 0;
		for (FlagEvaluation flag : flags) {
			if (flag.getSeverity() == severity) {
				count++;
			}
		}
		return count;
	}

	public FlagSeverity getSeverity() {
		return severity;
	}

	public Double getValue() {
		return value;
	}

	public Double getThreshold() {
		return threshold;
	}
}
