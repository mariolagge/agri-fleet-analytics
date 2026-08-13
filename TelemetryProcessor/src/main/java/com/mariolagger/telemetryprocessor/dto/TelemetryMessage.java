package com.mariolagger.telemetryprocessor.dto;

import java.time.Instant;

public class TelemetryMessage {

	private AssetId assetId;
	private Instant timestamp;
	private boolean ptoEngaged;
	private double groundSpeedKmh;
	private double loadFactor;
	private double temperatureC;
	private VibrationLevel vibrationLevel;
	private WorkingStatus workingStatus;

	public TelemetryMessage() {
	}

	public AssetId getAssetId() {
		return assetId;
	}

	public void setAssetId(AssetId assetId) {
		this.assetId = assetId;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public boolean isPtoEngaged() {
		return ptoEngaged;
	}

	public void setPtoEngaged(boolean ptoEngaged) {
		this.ptoEngaged = ptoEngaged;
	}

	public double getGroundSpeedKmh() {
		return groundSpeedKmh;
	}

	public void setGroundSpeedKmh(double groundSpeedKmh) {
		this.groundSpeedKmh = groundSpeedKmh;
	}

	public double getLoadFactor() {
		return loadFactor;
	}

	public void setLoadFactor(double loadFactor) {
		this.loadFactor = loadFactor;
	}

	public double getTemperatureC() {
		return temperatureC;
	}

	public void setTemperatureC(double temperatureC) {
		this.temperatureC = temperatureC;
	}

	public VibrationLevel getVibrationLevel() {
		return vibrationLevel;
	}

	public void setVibrationLevel(VibrationLevel vibrationLevel) {
		this.vibrationLevel = vibrationLevel;
	}

	public WorkingStatus getWorkingStatus() {
		return workingStatus;
	}

	public void setWorkingStatus(WorkingStatus workingStatus) {
		this.workingStatus = workingStatus;
	}
}
