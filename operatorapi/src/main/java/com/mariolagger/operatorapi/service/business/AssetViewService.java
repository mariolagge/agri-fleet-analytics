package com.mariolagger.operatorapi.service.business;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.mariolagger.operatorapi.dto.AssetId;
import com.mariolagger.operatorapi.dto.AssetView;
import com.mariolagger.operatorapi.dto.CounterSetView;
import com.mariolagger.operatorapi.dto.Flag;
import com.mariolagger.operatorapi.dto.FlagScope;
import com.mariolagger.operatorapi.dto.FlagView;
import com.mariolagger.operatorapi.dto.FleetResponse;
import com.mariolagger.operatorapi.dto.LatestTelemetryEntry;
import com.mariolagger.operatorapi.dto.LatestTelemetryResponse;
import com.mariolagger.operatorapi.dto.LiveCondition;
import com.mariolagger.operatorapi.dto.TelemetryView;
import com.mariolagger.operatorapi.entity.AssetState;
import com.mariolagger.operatorapi.repository.AssetStateRepository;

/**
 * Maps asset_state rows onto the REQ-API-ASSETS and REQ-API-LATEST response shapes. ARCHITECTURE.md
 * §2: no threshold, percentage, status, recommendation or rank is derived here — every one
 * of them is read from the row TelemetryProcessor wrote.
 */
@Component
public class AssetViewService {

	private final AssetStateRepository assetStateRepository;

	public AssetViewService(AssetStateRepository assetStateRepository) {
		this.assetStateRepository = assetStateRepository;
	}

	/** REQ-API-ASSETS. */
	public FleetResponse getFleet() {
		List<AssetView> assets = new ArrayList<>();
		for (AssetState assetState : assetStateRepository.findAllByOrderByPriorityRankAsc()) {
			assets.add(toAssetView(assetState));
		}

		if (assets.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"No stored state for any asset — TelemetryProcessor has not published its state yet");
		}

		FleetResponse response = new FleetResponse();
		response.setGeneratedAt(Instant.now());
		response.setAssets(assets);
		return response;
	}

	/** REQ-API-ASSETS, single asset. */
	public AssetView getAsset(String assetId) {
		AssetId knownAssetId = requireKnownAsset(assetId);
		AssetState assetState = assetStateRepository.findById(knownAssetId.name())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
						"No stored state for asset " + knownAssetId.name()
								+ " — TelemetryProcessor has not published its state yet"));
		return toAssetView(assetState);
	}

	/** REQ-API-LATEST. All four assets are always present, ordered by assetId ascending. */
	public LatestTelemetryResponse getLatestTelemetry() {
		Map<String, AssetState> assetStatesByAssetId = new HashMap<>();
		for (AssetState assetState : assetStateRepository.findAll()) {
			assetStatesByAssetId.put(assetState.getAssetId(), assetState);
		}

		List<AssetId> orderedAssetIds = new ArrayList<>(List.of(AssetId.values()));
		orderedAssetIds.sort((left, right) -> left.name().compareTo(right.name()));

		List<LatestTelemetryEntry> assets = new ArrayList<>();
		for (AssetId assetId : orderedAssetIds) {
			assets.add(toLatestTelemetryEntry(assetId, assetStatesByAssetId.get(assetId.name())));
		}

		LatestTelemetryResponse response = new LatestTelemetryResponse();
		response.setGeneratedAt(Instant.now());
		response.setAssets(assets);
		return response;
	}

	private AssetView toAssetView(AssetState assetState) {
		AssetView assetView = new AssetView();
		assetView.setAssetId(AssetId.valueOf(assetState.getAssetId()));
		assetView.setPriorityRank(assetState.getPriorityRank());
		assetView.setStatus(assetState.getStatus());
		assetView.setLiveCondition(assetState.getLiveCondition());
		assetView.setRecommendation(assetState.getRecommendation());
		assetView.setFlags(toFlagViews(assetState));
		assetView.setLifetimeCounters(toLifetimeCounters(assetState));
		assetView.setRolling24hCounters(toRolling24hCounters(assetState));
		assetView.setWindowCoverageHours(round2(assetState.getWindowCoverageHours()));
		assetView.setUtilizationPercent(round2OrNull(assetState.getUtilizationPercent()));
		assetView.setProductiveSharePercent(round2OrNull(assetState.getProductiveSharePercent()));
		return assetView;
	}

	private List<FlagView> toFlagViews(AssetState assetState) {
		List<FlagView> flags = new ArrayList<>();
		addFlagIfActive(flags, Flag.HEAVY_RECENT_USE, "highLoadHours", FlagScope.ROLLING_24H,
				assetState.getHeavyRecentUseValue(), assetState.getHeavyRecentUseThreshold());
		addFlagIfActive(flags, Flag.THERMAL_LOAD, "tempExcursionHours", FlagScope.ROLLING_24H,
				assetState.getThermalLoadValue(), assetState.getThermalLoadThreshold());
		addFlagIfActive(flags, Flag.MECHANICAL_ATTENTION, "vibrationAlertHours", FlagScope.ROLLING_24H,
				assetState.getMechanicalAttentionValue(), assetState.getMechanicalAttentionThreshold());
		addFlagIfActive(flags, Flag.HIGH_CUMULATIVE_LOAD, "highLoadHours", FlagScope.LIFETIME,
				assetState.getHighCumulativeLoadValue(), assetState.getHighCumulativeLoadThreshold());
		return flags;
	}

	/**
	 * GLOSSARY §5: the counter and the scope are constant per flag, so they are labels
	 * filled in here rather than three more columns repeated in every row.
	 */
	private void addFlagIfActive(List<FlagView> flags, Flag flag, String counter, FlagScope scope,
			Double value, Double threshold) {
		// GLOSSARY §5: an inactive flag has no value and is absent from the response
		if (value == null) {
			return;
		}

		FlagView flagView = new FlagView();
		flagView.setFlag(flag);
		flagView.setCounter(counter);
		flagView.setScope(scope);
		flagView.setValue(round2OrNull(value));
		flagView.setThreshold(round2OrNull(threshold));
		flags.add(flagView);
	}

	private CounterSetView toLifetimeCounters(AssetState assetState) {
		CounterSetView counters = new CounterSetView();
		counters.setEngineHours(round2(assetState.getLifetimeEngineHours()));
		counters.setTotalWorkingHours(round2(assetState.getLifetimeTotalWorkingHours()));
		counters.setHighLoadHours(round2(assetState.getLifetimeHighLoadHours()));
		counters.setTempExcursionHours(round2(assetState.getLifetimeTempExcursionHours()));
		counters.setVibrationAlertHours(round2(assetState.getLifetimeVibrationAlertHours()));
		return counters;
	}

	private CounterSetView toRolling24hCounters(AssetState assetState) {
		CounterSetView counters = new CounterSetView();
		counters.setEngineHours(round2(assetState.getRolling24hEngineHours()));
		counters.setTotalWorkingHours(round2(assetState.getRolling24hTotalWorkingHours()));
		counters.setHighLoadHours(round2(assetState.getRolling24hHighLoadHours()));
		counters.setTempExcursionHours(round2(assetState.getRolling24hTempExcursionHours()));
		counters.setVibrationAlertHours(round2(assetState.getRolling24hVibrationAlertHours()));
		return counters;
	}

	private LatestTelemetryEntry toLatestTelemetryEntry(AssetId assetId, AssetState assetState) {
		LatestTelemetryEntry entry = new LatestTelemetryEntry();
		entry.setAssetId(assetId);

		// REQ-API-LATEST: an asset that has not reported in this run is STALE with no telemetry
		if (assetState == null) {
			entry.setLiveCondition(LiveCondition.STALE);
			return entry;
		}

		entry.setLiveCondition(assetState.getLiveCondition());
		if (assetState.getLatestTimestamp() != null) {
			entry.setTelemetry(toTelemetryView(assetId, assetState));
		}
		return entry;
	}

	private TelemetryView toTelemetryView(AssetId assetId, AssetState assetState) {
		TelemetryView telemetry = new TelemetryView();
		telemetry.setAssetId(assetId);
		telemetry.setTimestamp(assetState.getLatestTimestamp());
		telemetry.setPtoEngaged(assetState.getLatestPtoEngaged());
		telemetry.setGroundSpeedKmh(round2OrNull(assetState.getLatestGroundSpeedKmh()));
		telemetry.setLoadFactor(round2OrNull(assetState.getLatestLoadFactor()));
		telemetry.setTemperatureC(round2OrNull(assetState.getLatestTemperatureC()));
		telemetry.setVibrationLevel(assetState.getLatestVibrationLevel());
		return telemetry;
	}

	/** REQ-API-ASSETS: 404 when the id is not one of the four REQ-GOAL-SCOPE assets. */
	private AssetId requireKnownAsset(String assetId) {
		try {
			return AssetId.valueOf(assetId);
		} catch (IllegalArgumentException unknownAssetId) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown asset: " + assetId);
		}
	}

	/** Exact numeric stays in the database; the response is rounded to 2 decimals. */
	private static double round2(double value) {
		return Math.round(value * 100.0) / 100.0;
	}

	private static Double round2OrNull(Double value) {
		return value == null ? null : round2(value);
	}
}
