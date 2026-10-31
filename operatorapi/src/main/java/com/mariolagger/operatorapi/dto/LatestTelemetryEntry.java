package com.mariolagger.operatorapi.dto;

/**
 * REQ-API-LATEST: one asset in the live telemetry strip. An asset that has not reported in this
 * run carries liveCondition STALE and a null `telemetry`.
 */
public class LatestTelemetryEntry {

	private AssetId assetId;
	private LiveCondition liveCondition;
	private TelemetryView telemetry;

	public LatestTelemetryEntry() {
	}

	public AssetId getAssetId() {
		return assetId;
	}

	public void setAssetId(AssetId assetId) {
		this.assetId = assetId;
	}

	public LiveCondition getLiveCondition() {
		return liveCondition;
	}

	public void setLiveCondition(LiveCondition liveCondition) {
		this.liveCondition = liveCondition;
	}

	public TelemetryView getTelemetry() {
		return telemetry;
	}

	public void setTelemetry(TelemetryView telemetry) {
		this.telemetry = telemetry;
	}
}
