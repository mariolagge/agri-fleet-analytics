package com.mariolagger.telemetryprocessor.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mariolagger.telemetryprocessor.service.business.AssetStatePublisher;

/**
 * ARCHITECTURE.md §4 write cadence. Three groups of values move with wall-clock rather
 * than with message arrival — the rolling window edge (SPEC §4.3), window coverage and the
 * percentages (§4.5), and staleness (§5.3) — and an asset that stops reporting would
 * otherwise stay frozen at its last message, with its final hour never sealed.
 */
@Component
public class AssetStateRefreshScheduler {

	private final AssetStatePublisher assetStatePublisher;

	public AssetStateRefreshScheduler(AssetStatePublisher assetStatePublisher) {
		this.assetStatePublisher = assetStatePublisher;
	}

	@Scheduled(fixedRate = 5000)
	public void refreshAssetState() {
		assetStatePublisher.publish();
	}
}
