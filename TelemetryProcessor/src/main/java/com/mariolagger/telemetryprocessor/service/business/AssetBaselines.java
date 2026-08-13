package com.mariolagger.telemetryprocessor.service.business;

import com.mariolagger.telemetryprocessor.dto.AssetId;

/**
 * The SPEC §4.4 seeded service history. Lifetime scope only — the rolling 24-hour window
 * always starts empty — and never carried across restarts, so every run is reproducible.
 */
public final class AssetBaselines {

	private AssetBaselines() {
	}

	/** A fresh copy per call: the caller adds this run's hours on top of it. */
	public static CounterSet baselineFor(AssetId assetId) {
		// SPEC §4.4, column order: engine, working, transport, idle, loaded, highLoad,
		// tempExcursion, vibrationAlert.
		return switch (assetId) {
			case TRACTOR -> new CounterSet(6200, 3400, 1500, 1300, 980, 410, 26, 58);
			case MOWER -> new CounterSet(950, 640, 210, 100, 310, 95, 4, 12);
			case BALER -> new CounterSet(1480, 1010, 300, 170, 620, 240, 11, 46);
			case MIXER -> new CounterSet(1120, 860, 60, 200, 415, 130, 7, 19);
		};
	}
}
