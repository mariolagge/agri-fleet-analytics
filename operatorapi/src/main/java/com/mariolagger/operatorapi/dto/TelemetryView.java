package com.mariolagger.operatorapi.dto;

import java.time.Instant;

/** REQ-TELEMETRY-SHAPE: the most recent telemetry message of one asset, as embedded by REQ-API-LATEST. */
public class TelemetryView {

	private AssetId assetId;
	private Instant timestamp;
	private Boolean ptoEngaged;
	private Double groundSpeedKmh;
	private Double loadFactor;
	private Double temperatureC;
	private VibrationLevel vibrationLevel;

	public TelemetryView() {
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

	public Boolean getPtoEngaged() {
		return ptoEngaged;
	}

	public void setPtoEngaged(Boolean ptoEngaged) {
		this.ptoEngaged = ptoEngaged;
	}

	public Double getGroundSpeedKmh() {
		return groundSpeedKmh;
	}

	public void setGroundSpeedKmh(Double groundSpeedKmh) {
		this.groundSpeedKmh = groundSpeedKmh;
	}

	public Double getLoadFactor() {
		return loadFactor;
	}

	public void setLoadFactor(Double loadFactor) {
		this.loadFactor = loadFactor;
	}

	public Double getTemperatureC() {
		return temperatureC;
	}

	public void setTemperatureC(Double temperatureC) {
		this.temperatureC = temperatureC;
	}

	public VibrationLevel getVibrationLevel() {
		return vibrationLevel;
	}

	public void setVibrationLevel(VibrationLevel vibrationLevel) {
		this.vibrationLevel = vibrationLevel;
	}
}
