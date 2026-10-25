package com.mariolagger.simulator.service.business;

import java.time.Instant;

import com.mariolagger.simulator.dto.AssetId;

/**
 * What the live scheduler remembers about one asset: when its next message is due.
 * In memory only, one instance per asset, fresh on every run (REQ-SIM-SCHEDULE).
 */
public class AssetSimulationState {

	private final AssetId assetId;

	private Instant nextDueAt;

	public AssetSimulationState(AssetId assetId) {
		this.assetId = assetId;
	}

	public AssetId getAssetId() {
		return assetId;
	}

	public Instant getNextDueAt() {
		return nextDueAt;
	}

	public void setNextDueAt(Instant nextDueAt) {
		this.nextDueAt = nextDueAt;
	}
}
