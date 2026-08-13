package com.mariolagger.operatorapi.dto;

import java.time.Instant;
import java.util.List;

/** SPEC §6.4: the body of GET /api/telemetry/latest, all four assets by assetId ascending. */
public class LatestTelemetryResponse {

	private Instant generatedAt;
	private List<LatestTelemetryEntry> assets;

	public LatestTelemetryResponse() {
	}

	public Instant getGeneratedAt() {
		return generatedAt;
	}

	public void setGeneratedAt(Instant generatedAt) {
		this.generatedAt = generatedAt;
	}

	public List<LatestTelemetryEntry> getAssets() {
		return assets;
	}

	public void setAssets(List<LatestTelemetryEntry> assets) {
		this.assets = assets;
	}
}
