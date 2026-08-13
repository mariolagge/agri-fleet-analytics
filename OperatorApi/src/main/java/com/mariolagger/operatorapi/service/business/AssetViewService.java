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
import com.mariolagger.operatorapi.dto.FlagSeverity;
import com.mariolagger.operatorapi.dto.FlagView;
import com.mariolagger.operatorapi.dto.FleetResponse;
import com.mariolagger.operatorapi.dto.LatestTelemetryEntry;
import com.mariolagger.operatorapi.dto.LatestTelemetryResponse;
import com.mariolagger.operatorapi.dto.LiveCondition;
import com.mariolagger.operatorapi.dto.TelemetryView;
import com.mariolagger.operatorapi.entity.AssetState;
import com.mariolagger.operatorapi.repository.AssetStateRepository;

/**
 * Maps asset_state rows onto the SPEC §6.1, §6.2 and §6.4 response shapes. ARCHITECTURE.md
 * §2: no threshold, percentage, status, recommendation or rank is derived here — every one
 * of them is read from the row TelemetryProcessor wrote.
 */
@Component
public class AssetViewService {

	private final AssetStateRepository assetStateRepository;

	public AssetViewService(AssetStateRepository assetStateRepository) {
		this.assetStateRepository = assetStateRepository;
	}

	/** SPEC §6.1. */
	public FleetResponse getFleet() {
		List<AssetView> assets = new ArrayList<>();
		for (AssetState assetState : assetStateRepository.findAllByOrderByPriorityRankAsc()) {
			assets.add(toAssetView(assetState));
		}

		FleetResponse response = new FleetResponse();
		response.setGeneratedAt(Instant.now());
		response.setAssets(assets);
		return response;
	}

	/** SPEC §6.2. */
	public AssetView getAsset(String assetId) {
		AssetId knownAssetId = requireKnownAsset(assetId);
		AssetState assetState = assetStateRepository.findById(knownAssetId.name())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"No stored state for asset " + knownAssetId.name()
								+ " — TelemetryProcessor has not written its baselines yet"));
		return toAssetView(assetState);
	}

	/** SPEC §6.4. All four assets are always present, ordered by assetId ascending. */
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
				assetState.getHeavyRecentUseSeverity(), assetState.getHeavyRecentUseValue(),
				assetState.getHeavyRecentUseThreshold());
		addFlagIfActive(flags, Flag.THERMAL_LOAD, "tempExcursionHours", FlagScope.ROLLING_24H,
				assetState.getThermalLoadSeverity(), assetState.getThermalLoadValue(),
				assetState.getThermalLoadThreshold());
		addFlagIfActive(flags, Flag.MECHANICAL_ATTENTION, "vibrationAlertHours", FlagScope.ROLLING_24H,
				assetState.getMechanicalAttentionSeverity(), assetState.getMechanicalAttentionValue(),
				assetState.getMechanicalAttentionThreshold());
		addFlagIfActive(flags, Flag.HIGH_CUMULATIVE_LOAD, "loadedHours", FlagScope.LIFETIME,
				assetState.getHighCumulativeLoadSeverity(), assetState.getHighCumulativeLoadValue(),
				assetState.getHighCumulativeLoadThreshold());
		return flags;
	}

	/**
	 * IMPLEMENTATION-PLAN §1.3: the counter and the scope are constant per flag, so they are
	 * labels filled in here rather than three more columns repeated in every row.
	 */
	private void addFlagIfActive(List<FlagView> flags, Flag flag, String counter, FlagScope scope,
			FlagSeverity severity, Double value, Double threshold) {
		// GLOSSARY §5: an inactive flag has no severity and is absent from the response
		if (severity == null) {
			return;
		}

		FlagView flagView = new FlagView();
		flagView.setFlag(flag);
		flagView.setSeverity(severity);
		flagView.setCounter(counter);
		flagView.setScope(scope);
		flagView.setValue(round2(value));
		flagView.setThreshold(round2(threshold));
		flags.add(flagView);
	}

	private CounterSetView toLifetimeCounters(AssetState assetState) {
		CounterSetView counters = new CounterSetView();
		counters.setEngineHours(round2(assetState.getLifetimeEngineHours()));
		counters.setTotalWorkingHours(round2(assetState.getLifetimeTotalWorkingHours()));
		counters.setTransportHours(round2(assetState.getLifetimeTransportHours()));
		counters.setIdleHours(round2(assetState.getLifetimeIdleHours()));
		counters.setLoadedHours(round2(assetState.getLifetimeLoadedHours()));
		counters.setHighLoadHours(round2(assetState.getLifetimeHighLoadHours()));
		counters.setTempExcursionHours(round2(assetState.getLifetimeTempExcursionHours()));
		counters.setVibrationAlertHours(round2(assetState.getLifetimeVibrationAlertHours()));
		return counters;
	}

	private CounterSetView toRolling24hCounters(AssetState assetState) {
		CounterSetView counters = new CounterSetView();
		counters.setEngineHours(round2(assetState.getRolling24hEngineHours()));
		counters.setTotalWorkingHours(round2(assetState.getRolling24hTotalWorkingHours()));
		counters.setTransportHours(round2(assetState.getRolling24hTransportHours()));
		counters.setIdleHours(round2(assetState.getRolling24hIdleHours()));
		counters.setLoadedHours(round2(assetState.getRolling24hLoadedHours()));
		counters.setHighLoadHours(round2(assetState.getRolling24hHighLoadHours()));
		counters.setTempExcursionHours(round2(assetState.getRolling24hTempExcursionHours()));
		counters.setVibrationAlertHours(round2(assetState.getRolling24hVibrationAlertHours()));
		return counters;
	}

	private LatestTelemetryEntry toLatestTelemetryEntry(AssetId assetId, AssetState assetState) {
		LatestTelemetryEntry entry = new LatestTelemetryEntry();
		entry.setAssetId(assetId);

		// SPEC §6.4: an asset that has not reported in this run is STALE with no telemetry
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
		telemetry.setGroundSpeedKmh(round2(assetState.getLatestGroundSpeedKmh()));
		telemetry.setLoadFactor(round2(assetState.getLatestLoadFactor()));
		telemetry.setTemperatureC(round2(assetState.getLatestTemperatureC()));
		telemetry.setVibrationLevel(assetState.getLatestVibrationLevel());
		telemetry.setWorkingStatus(assetState.getLatestWorkingStatus());
		return telemetry;
	}

	/** SPEC §6.2: 404 when the id is not one of the four SPEC §2 assets. */
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
