package com.mariolagger.telemetryprocessor.service.business;

import com.mariolagger.telemetryprocessor.dto.TelemetryMessage;

/**
 * Everything one asset_state row needs from the accumulator, taken under a single lock
 * acquisition from a single clock read. The two percentages are the exact quotients of the
 * counters beside them, which is what SPEC §1 asks for: every published number traceable to
 * a counter.
 *
 * The counters are copies; latestMessage is handed out by reference and is never mutated
 * after TelemetryGenerator builds it.
 */
public class AssetSnapshot {

	private final CounterSet lifetimeCounters;
	private final CounterSet rolling24hCounters;
	private final double windowCoverageHours;
	private final Double utilizationPercent;
	private final Double productiveSharePercent;
	private final TelemetryMessage latestMessage;

	public AssetSnapshot(CounterSet lifetimeCounters, CounterSet rolling24hCounters, double windowCoverageHours,
			Double utilizationPercent, Double productiveSharePercent, TelemetryMessage latestMessage) {
		this.lifetimeCounters = lifetimeCounters;
		this.rolling24hCounters = rolling24hCounters;
		this.windowCoverageHours = windowCoverageHours;
		this.utilizationPercent = utilizationPercent;
		this.productiveSharePercent = productiveSharePercent;
		this.latestMessage = latestMessage;
	}

	public CounterSet getLifetimeCounters() {
		return lifetimeCounters;
	}

	public CounterSet getRolling24hCounters() {
		return rolling24hCounters;
	}

	public double getWindowCoverageHours() {
		return windowCoverageHours;
	}

	public Double getUtilizationPercent() {
		return utilizationPercent;
	}

	public Double getProductiveSharePercent() {
		return productiveSharePercent;
	}

	public TelemetryMessage getLatestMessage() {
		return latestMessage;
	}
}
