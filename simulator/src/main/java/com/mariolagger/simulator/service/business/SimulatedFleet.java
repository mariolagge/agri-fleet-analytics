package com.mariolagger.simulator.service.business;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.mariolagger.simulator.dto.AssetId;

/**
 * The four simulated assets, shared by the startup backfill and the live scheduler so
 * that the live cadence continues the same state machines the burst left behind.
 * In memory only, fresh on every run (REQ-SIM-MEMORY).
 */
@Component
public class SimulatedFleet {

	private final List<AssetSimulationState> assets = new ArrayList<>();

	/**
	 * The scheduler's tick starts with the context, before the backfill runs on
	 * ApplicationReadyEvent. Without this gate a live message timestamped now would
	 * overtake the burst, and REQ-ACCUMULATE step 1 would make the consumer discard every
	 * backfill message that arrived after it.
	 */
	private volatile boolean liveCadenceStarted;

	public SimulatedFleet() {
		for (AssetId assetId : AssetId.values()) {
			assets.add(new AssetSimulationState(assetId));
		}
	}

	public List<AssetSimulationState> getAssets() {
		return assets;
	}

	public boolean isLiveCadenceStarted() {
		return liveCadenceStarted;
	}

	public void startLiveCadence() {
		this.liveCadenceStarted = true;
	}
}
