package com.mariolagger.telemetryprocessor.service.business;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import com.mariolagger.telemetryprocessor.dto.AssetStatus;
import com.mariolagger.telemetryprocessor.dto.FlagSeverity;
import com.mariolagger.telemetryprocessor.dto.LiveCondition;
import com.mariolagger.telemetryprocessor.dto.TelemetryMessage;
import com.mariolagger.telemetryprocessor.dto.VibrationLevel;

/**
 * SPEC §5.2, §5.3 and §5.5. Status is an integral over 24 hours; liveCondition is the
 * current instant. They are computed side by side here and never fed into each other —
 * §5.3 is explicit that liveCondition influences nothing else.
 */
@Component
public class AssetAssessmentService {

	// SPEC §5.3.
	private static final Duration STALE_AFTER = Duration.ofSeconds(60);
	private static final double ALERT_TEMPERATURE_C = 95.0;
	private static final double CAUTION_TEMPERATURE_C = 90.0;
	private static final double CAUTION_LOAD_FACTOR = 0.9;

	// SPEC §5.5, verbatim.
	private static final String CRITICAL_RECOMMENDATION = "Priority check recommended within the next 5 engine hours.";
	private static final String WATCH_RECOMMENDATION = "Plan inspection within the next 15–20 engine hours.";
	private static final String GOOD_RECOMMENDATION = "No inspection recommended at this time.";

	/** SPEC §5.2 — each active flag counted once, at its current severity. */
	public AssetStatus status(List<FlagEvaluation> flags) {
		int severeCount = FlagEvaluation.countAtSeverity(flags, FlagSeverity.SEVERE);
		int moderateCount = FlagEvaluation.countAtSeverity(flags, FlagSeverity.MODERATE);
		if (severeCount >= 1 || moderateCount >= 2) {
			return AssetStatus.CRITICAL;
		}
		if (moderateCount == 1) {
			return AssetStatus.WATCH;
		}
		return AssetStatus.GOOD;
	}

	/**
	 * SPEC §5.3, evaluated top to bottom — the first matching row wins. Staleness is the age
	 * of the message's own timestamp, not of its arrival: a backfilled reading from twenty
	 * hours ago describes an asset that has been silent for twenty hours.
	 */
	public LiveCondition liveCondition(TelemetryMessage latestMessage, Instant now) {
		if (latestMessage == null
				|| Duration.between(latestMessage.getTimestamp(), now).compareTo(STALE_AFTER) > 0) {
			return LiveCondition.STALE;
		}
		if (latestMessage.getVibrationLevel() == VibrationLevel.HIGH
				|| latestMessage.getTemperatureC() > ALERT_TEMPERATURE_C) {
			return LiveCondition.ALERT;
		}
		if (latestMessage.getVibrationLevel() == VibrationLevel.ELEVATED
				|| latestMessage.getTemperatureC() > CAUTION_TEMPERATURE_C
				|| latestMessage.getLoadFactor() >= CAUTION_LOAD_FACTOR) {
			return LiveCondition.CAUTION;
		}
		return LiveCondition.NORMAL;
	}

	/** SPEC §5.5 — driven by status alone. */
	public String recommendation(AssetStatus status) {
		return switch (status) {
			case CRITICAL -> CRITICAL_RECOMMENDATION;
			case WATCH -> WATCH_RECOMMENDATION;
			case GOOD -> GOOD_RECOMMENDATION;
		};
	}
}
