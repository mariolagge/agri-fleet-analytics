package com.mariolagger.simulator.service.business;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import com.mariolagger.simulator.dto.AssetId;
import com.mariolagger.simulator.dto.TelemetryMessage;

/**
 * REQ-SIM-SCHEDULE. Every asset repeats a fixed schedule, so a message depends only on the
 * asset and the timestamp. The startup burst and the live scheduler therefore share this
 * one path, and every run produces the same telemetry for the same instant.
 */
@Component
public class TelemetryGenerator {

	private static final double TRANSPORT_SPEED_KMH = 20.0;

	public TelemetryMessage generate(AssetId assetId, Instant timestamp) {
		ScheduleSegment segment = segmentAt(assetId, timestamp);

		TelemetryMessage message = new TelemetryMessage();
		message.setAssetId(assetId);
		message.setTimestamp(timestamp);
		message.setPtoEngaged(segment.getWorkingStatus() == WorkingStatus.WORKING);
		message.setGroundSpeedKmh(groundSpeedKmh(assetId, segment.getWorkingStatus()));
		message.setLoadFactor(segment.getLoadFactor());
		message.setTemperatureC(segment.getTemperatureC());
		message.setVibrationLevel(segment.getVibrationLevel());
		return message;
	}

	/** REQ-SIM-SCHEDULE: the position in the cycle is the timestamp modulo the cycle length. */
	ScheduleSegment segmentAt(AssetId assetId, Instant timestamp) {
		List<ScheduleSegment> schedule = scheduleFor(assetId);
		long secondsIntoCycle = Math.floorMod(timestamp.getEpochSecond(), cycleSeconds(schedule));
		for (ScheduleSegment segment : schedule) {
			long segmentSeconds = segment.getMinutes() * 60L;
			if (secondsIntoCycle < segmentSeconds) {
				return segment;
			}
			secondsIntoCycle -= segmentSeconds;
		}
		throw new IllegalStateException("No segment found for " + assetId + " at " + timestamp);
	}

	/** REQ-SIM-SCHEDULE: one fixed cycle per asset, segments in order. */
	private List<ScheduleSegment> scheduleFor(AssetId assetId) {
		return switch (assetId) {
			case TRACTOR -> List.of(
					ScheduleSegment.working(60, 0.80),
					ScheduleSegment.workingHot(10, 0.80, 94.0),
					ScheduleSegment.transport(20),
					ScheduleSegment.idle(30));
			case BALER -> List.of(
					ScheduleSegment.working(15, 0.80),
					ScheduleSegment.working(45, 0.60),
					ScheduleSegment.transport(20),
					ScheduleSegment.idle(40));
			case MOWER -> List.of(
					ScheduleSegment.working(180, 0.60),
					ScheduleSegment.workingVibrating(18, 0.60),
					ScheduleSegment.transport(42),
					ScheduleSegment.idle(180));
			case MIXER -> List.of(
					ScheduleSegment.working(40, 0.50),
					ScheduleSegment.idle(80));
		};
	}

	private long cycleSeconds(List<ScheduleSegment> schedule) {
		long cycleMinutes = 0;
		for (ScheduleSegment segment : schedule) {
			cycleMinutes += segment.getMinutes();
		}
		return cycleMinutes * 60L;
	}

	private double groundSpeedKmh(AssetId assetId, WorkingStatus workingStatus) {
		// REQ-SIM-SCHEDULE: every WORKING speed is above 2 km/h, so WORKING is exactly productive work.
		return switch (workingStatus) {
			case WORKING -> switch (assetId) {
				case TRACTOR -> 10.0;
				case MOWER -> 11.0;
				case BALER -> 8.0;
				case MIXER -> 4.0;
			};
			case TRANSPORT -> TRANSPORT_SPEED_KMH;
			case IDLE -> 0.0;
		};
	}
}
