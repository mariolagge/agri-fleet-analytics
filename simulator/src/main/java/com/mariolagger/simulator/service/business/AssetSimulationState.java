package com.mariolagger.simulator.service.business;

import java.time.Instant;

import com.mariolagger.simulator.dto.AssetId;
import com.mariolagger.simulator.dto.WorkingStatus;

/**
 * Everything one asset remembers between two telemetry messages. In memory only,
 * one instance per asset, fresh on every run (SPEC §3.2).
 */
public class AssetSimulationState {

	/** Cold-start temperature: parked in the shed, at the bottom of the IDLE band. */
	private static final double COLD_START_TEMPERATURE_C = 45.0;

	private final AssetId assetId;

	private WorkingStatus workingStatus = WorkingStatus.IDLE;
	private Instant stateEndsAt;
	private double temperatureC = COLD_START_TEMPERATURE_C;
	private Instant thermalExcursionEndsAt;
	private Instant vibrationExcursionEndsAt;
	private Instant lastMessageTimestamp;
	private Instant nextDueAt;

	public AssetSimulationState(AssetId assetId) {
		this.assetId = assetId;
	}

	public AssetId getAssetId() {
		return assetId;
	}

	public WorkingStatus getWorkingStatus() {
		return workingStatus;
	}

	public void setWorkingStatus(WorkingStatus workingStatus) {
		this.workingStatus = workingStatus;
	}

	public Instant getStateEndsAt() {
		return stateEndsAt;
	}

	public void setStateEndsAt(Instant stateEndsAt) {
		this.stateEndsAt = stateEndsAt;
	}

	public double getTemperatureC() {
		return temperatureC;
	}

	public void setTemperatureC(double temperatureC) {
		this.temperatureC = temperatureC;
	}

	public Instant getThermalExcursionEndsAt() {
		return thermalExcursionEndsAt;
	}

	public void setThermalExcursionEndsAt(Instant thermalExcursionEndsAt) {
		this.thermalExcursionEndsAt = thermalExcursionEndsAt;
	}

	public Instant getVibrationExcursionEndsAt() {
		return vibrationExcursionEndsAt;
	}

	public void setVibrationExcursionEndsAt(Instant vibrationExcursionEndsAt) {
		this.vibrationExcursionEndsAt = vibrationExcursionEndsAt;
	}

	public Instant getLastMessageTimestamp() {
		return lastMessageTimestamp;
	}

	public void setLastMessageTimestamp(Instant lastMessageTimestamp) {
		this.lastMessageTimestamp = lastMessageTimestamp;
	}

	public Instant getNextDueAt() {
		return nextDueAt;
	}

	public void setNextDueAt(Instant nextDueAt) {
		this.nextDueAt = nextDueAt;
	}
}
