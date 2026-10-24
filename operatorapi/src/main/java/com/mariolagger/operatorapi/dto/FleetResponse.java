package com.mariolagger.operatorapi.dto;

import java.time.Instant;
import java.util.List;

/** REQ-API-ASSETS: the body of GET /api/assets. */
public class FleetResponse {

	private Instant generatedAt;
	private List<AssetView> assets;

	public FleetResponse() {
	}

	public Instant getGeneratedAt() {
		return generatedAt;
	}

	public void setGeneratedAt(Instant generatedAt) {
		this.generatedAt = generatedAt;
	}

	public List<AssetView> getAssets() {
		return assets;
	}

	public void setAssets(List<AssetView> assets) {
		this.assets = assets;
	}
}
