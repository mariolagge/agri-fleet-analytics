package com.mariolagger.simulator.service.business;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import org.springframework.stereotype.Component;

import com.mariolagger.simulator.dto.AssetId;
import com.mariolagger.simulator.dto.TelemetryMessage;
import com.mariolagger.simulator.dto.VibrationLevel;
import com.mariolagger.simulator.dto.WorkingStatus;

@Component
public class TelemetryGenerator {

	// Not fixed by SPEC §3.2: how fast temperatureC closes the gap to its target band.
	private static final double TEMPERATURE_DRIFT_C_PER_SECOND = 0.4;

	private final Random random = new Random();

	/**
	 * The one entry point. The message timestamp is always the clock: the state machine
	 * advances against it and the excursion probabilities scale with the gap to the
	 * previous message, so the backfill and the live scheduler share this single path.
	 */
	public TelemetryMessage generate(AssetSimulationState state, Instant timestamp) {
		double deltaSeconds = secondsSinceLastMessage(state, timestamp);

		advanceStateCycle(state, timestamp);
		expireFinishedExcursions(state, timestamp);
		maybeStartExcursions(state, timestamp, deltaSeconds);

		WorkingStatus workingStatus = state.getWorkingStatus();
		double groundSpeedKmh = groundSpeedKmh(state.getAssetId(), workingStatus);
		double loadFactor = loadFactor(state.getAssetId(), workingStatus);
		double temperatureC = driftedTemperatureC(state, deltaSeconds);

		state.setTemperatureC(temperatureC);
		state.setLastMessageTimestamp(timestamp);

		TelemetryMessage message = new TelemetryMessage();
		message.setAssetId(state.getAssetId());
		message.setTimestamp(timestamp);
		// SPEC §3.1: WORKING exactly when ptoEngaged and groundSpeedKmh > 2 — every
		// WORKING speed range below starts at 3 or above, every other state has pto off.
		message.setPtoEngaged(workingStatus == WorkingStatus.WORKING);
		message.setGroundSpeedKmh(groundSpeedKmh);
		message.setLoadFactor(loadFactor);
		message.setTemperatureC(temperatureC);
		message.setVibrationLevel(vibrationLevel(state, loadFactor));
		message.setWorkingStatus(workingStatus);
		return message;
	}

	private double secondsSinceLastMessage(AssetSimulationState state, Instant timestamp) {
		if (state.getLastMessageTimestamp() == null) {
			return 0.0;
		}
		return Duration.between(state.getLastMessageTimestamp(), timestamp).toMillis() / 1000.0;
	}

	private void advanceStateCycle(AssetSimulationState state, Instant timestamp) {
		if (state.getStateEndsAt() == null) {
			state.setStateEndsAt(timestamp.plusSeconds(dwellSeconds(state.getWorkingStatus())));
			return;
		}
		while (!timestamp.isBefore(state.getStateEndsAt())) {
			WorkingStatus nextWorkingStatus = nextWorkingStatus(state.getWorkingStatus());
			state.setWorkingStatus(nextWorkingStatus);
			state.setStateEndsAt(state.getStateEndsAt().plusSeconds(dwellSeconds(nextWorkingStatus)));
		}
	}

	private long dwellSeconds(WorkingStatus workingStatus) {
		// SPEC §3.2
		return switch (workingStatus) {
			case WORKING -> randomMinutesAsSeconds(20, 60);
			case TRANSPORT -> randomMinutesAsSeconds(3, 10);
			case IDLE -> randomMinutesAsSeconds(5, 30);
		};
	}

	private WorkingStatus nextWorkingStatus(WorkingStatus workingStatus) {
		// SPEC §3.2
		return switch (workingStatus) {
			case WORKING -> random.nextDouble() < 0.60 ? WorkingStatus.TRANSPORT : WorkingStatus.IDLE;
			case TRANSPORT -> random.nextDouble() < 0.70 ? WorkingStatus.WORKING : WorkingStatus.IDLE;
			case IDLE -> random.nextDouble() < 0.80 ? WorkingStatus.WORKING : WorkingStatus.TRANSPORT;
		};
	}

	private double groundSpeedKmh(AssetId assetId, WorkingStatus workingStatus) {
		// SPEC §3.2
		return switch (workingStatus) {
			case WORKING -> switch (assetId) {
				case TRACTOR -> randomBetween(6, 14);
				case MOWER -> randomBetween(8, 13);
				case BALER -> randomBetween(6, 10);
				case MIXER -> randomBetween(3, 5);
			};
			case TRANSPORT -> assetId == AssetId.MIXER ? randomBetween(8, 18) : randomBetween(12, 30);
			case IDLE -> 0.0;
		};
	}

	/**
	 * SPEC §3.2. The working band is per asset — it is the only per-asset difference any
	 * §4.1 counter reads, and therefore the whole reason the four machines rank differently.
	 * Only TRACTOR reaches 0.85, so only TRACTOR reports ELEVATED from load alone.
	 */
	private double loadFactor(AssetId assetId, WorkingStatus workingStatus) {
		return switch (workingStatus) {
			case WORKING -> switch (assetId) {
				case TRACTOR -> randomBetween(0.30, 0.90);
				case BALER -> randomBetween(0.25, 0.81);
				case MOWER -> randomBetween(0.25, 0.78);
				case MIXER -> randomBetween(0.25, 0.72);
			};
			case TRANSPORT -> randomBetween(0.15, 0.35);
			case IDLE -> randomBetween(0.05, 0.15);
		};
	}

	/** SPEC §3.2, per asset, per minute of working time. */
	private double thermalExcursionChancePerMinute(AssetId assetId) {
		return switch (assetId) {
			case TRACTOR -> 0.015;
			case BALER -> 0.0015;
			case MOWER -> 0.0015;
			case MIXER -> 0.0015;
		};
	}

	/** SPEC §3.2, per asset, per minute of working time. */
	private double vibrationExcursionChancePerMinute(AssetId assetId) {
		return switch (assetId) {
			case TRACTOR -> 0.010;
			case BALER -> 0.0015;
			case MOWER -> 0.008;
			case MIXER -> 0.0015;
		};
	}

	/**
	 * Moves the carried temperature at most TEMPERATURE_DRIFT_C_PER_SECOND towards a fresh
	 * random point inside the band the asset currently belongs in. Far from the band that
	 * is a steady climb or fall; inside it, every target is also inside, so the value
	 * jitters within the band instead of leaving it.
	 */
	private double driftedTemperatureC(AssetSimulationState state, double deltaSeconds) {
		double currentTemperatureC = state.getTemperatureC();
		double gap = targetTemperatureC(state) - currentTemperatureC;
		double maximumChange = TEMPERATURE_DRIFT_C_PER_SECOND * deltaSeconds;
		if (Math.abs(gap) <= maximumChange) {
			return currentTemperatureC + gap;
		}
		return currentTemperatureC + Math.signum(gap) * maximumChange;
	}

	private double targetTemperatureC(AssetSimulationState state) {
		// SPEC §3.2: an excursion overrides the state bands and runs to its own end.
		if (state.getThermalExcursionEndsAt() != null) {
			return randomBetween(91, 98);
		}
		// SPEC §3.2
		return switch (state.getWorkingStatus()) {
			case WORKING -> randomBetween(70, 85);
			case TRANSPORT -> randomBetween(55, 70);
			case IDLE -> randomBetween(45, 60);
		};
	}

	private VibrationLevel vibrationLevel(AssetSimulationState state, double loadFactor) {
		// SPEC §3.2
		if (state.getVibrationExcursionEndsAt() != null) {
			return VibrationLevel.HIGH;
		}
		if (loadFactor >= 0.85) {
			return VibrationLevel.ELEVATED;
		}
		return VibrationLevel.NORMAL;
	}

	private void maybeStartExcursions(AssetSimulationState state, Instant timestamp, double deltaSeconds) {
		// SPEC §3.2: only while WORKING and not already inside one of that kind. The rate is
		// per minute of working time, so it is scaled to this message's own delta — the burst
		// steps 60 s and the live phase ~7.5 s, and both must wear the machine at one speed.
		if (state.getWorkingStatus() != WorkingStatus.WORKING) {
			return;
		}
		AssetId assetId = state.getAssetId();
		if (state.getThermalExcursionEndsAt() == null
				&& random.nextDouble() < thermalExcursionChancePerMinute(assetId) * deltaSeconds / 60.0) {
			state.setThermalExcursionEndsAt(timestamp.plusSeconds(excursionSeconds()));
		}
		if (state.getVibrationExcursionEndsAt() == null
				&& random.nextDouble() < vibrationExcursionChancePerMinute(assetId) * deltaSeconds / 60.0) {
			state.setVibrationExcursionEndsAt(timestamp.plusSeconds(excursionSeconds()));
		}
	}

	private void expireFinishedExcursions(AssetSimulationState state, Instant timestamp) {
		// SPEC §3.2: an excursion runs to its end independently of the state cycle, so it
		// is cleared by the clock and never by a state change. A non-null end therefore
		// means "currently inside one" everywhere below.
		if (state.getThermalExcursionEndsAt() != null && !timestamp.isBefore(state.getThermalExcursionEndsAt())) {
			state.setThermalExcursionEndsAt(null);
		}
		if (state.getVibrationExcursionEndsAt() != null && !timestamp.isBefore(state.getVibrationExcursionEndsAt())) {
			state.setVibrationExcursionEndsAt(null);
		}
	}

	private long excursionSeconds() {
		// SPEC §3.2: excursions last 2-15 minutes
		return randomMinutesAsSeconds(2, 15);
	}

	private long randomMinutesAsSeconds(int minimumMinutes, int maximumMinutes) {
		return Math.round(randomBetween(minimumMinutes, maximumMinutes) * 60.0);
	}

	private double randomBetween(double minimum, double maximum) {
		return minimum + random.nextDouble() * (maximum - minimum);
	}
}
