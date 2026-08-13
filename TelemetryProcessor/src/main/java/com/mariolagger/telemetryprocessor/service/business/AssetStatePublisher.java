package com.mariolagger.telemetryprocessor.service.business;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mariolagger.telemetryprocessor.dto.AssetId;
import com.mariolagger.telemetryprocessor.dto.AssetStatus;
import com.mariolagger.telemetryprocessor.dto.LiveCondition;
import com.mariolagger.telemetryprocessor.dto.TelemetryMessage;
import com.mariolagger.telemetryprocessor.entity.AssetState;
import com.mariolagger.telemetryprocessor.entity.HourlyAggregate;
import com.mariolagger.telemetryprocessor.repository.AssetStateRepository;
import com.mariolagger.telemetryprocessor.repository.HourlyAggregateRepository;

import jakarta.annotation.PostConstruct;

/**
 * Assembles the four asset_state rows from the in-memory accumulators and writes them in
 * one transaction, and flushes sealed hourly buckets to hourly_aggregate. Values are
 * stored exactly as computed — rounding is the API's job, not the database's.
 */
@Component
public class AssetStatePublisher {

	private final CounterAccumulationService counterAccumulationService;
	private final FlagEvaluationService flagEvaluationService;
	private final AssetAssessmentService assetAssessmentService;
	private final FleetRankingService fleetRankingService;
	private final AssetStateRepository assetStateRepository;
	private final HourlyAggregateRepository hourlyAggregateRepository;

	public AssetStatePublisher(CounterAccumulationService counterAccumulationService,
			FlagEvaluationService flagEvaluationService, AssetAssessmentService assetAssessmentService,
			FleetRankingService fleetRankingService, AssetStateRepository assetStateRepository,
			HourlyAggregateRepository hourlyAggregateRepository) {
		this.counterAccumulationService = counterAccumulationService;
		this.flagEvaluationService = flagEvaluationService;
		this.assetAssessmentService = assetAssessmentService;
		this.fleetRankingService = fleetRankingService;
		this.assetStateRepository = assetStateRepository;
		this.hourlyAggregateRepository = hourlyAggregateRepository;
	}

	/**
	 * ARCHITECTURE.md §4 startup reset: a stale Postgres volume must not leak state into a
	 * new run. Publishing straight afterwards writes the SPEC §4.4 baselines, because that
	 * is exactly what the freshly constructed accumulators hold.
	 *
	 * This must stay a @PostConstruct. Rabbit listener containers are SmartLifecycle beans
	 * started inside finishRefresh(), which runs before any CommandLineRunner or
	 * ApplicationRunner — a runner-based reset could delete messages already accumulated.
	 */
	@PostConstruct
	public void resetOnStartup() {
		assetStateRepository.deleteAll();
		hourlyAggregateRepository.deleteAll();
		publish();
		System.out.println("TelemetryProcessor: both tables cleared, SPEC §4.4 baselines written.");
	}

	/**
	 * Synchronized because the Rabbit listener thread and the 5 s refresh scheduler both
	 * call this; serialising them keeps the four rows internally consistent and keeps two
	 * transactions from updating the same rows at once.
	 */
	@Transactional
	public synchronized void publish() {
		Map<AssetId, AssetState> rowsByAsset = new EnumMap<>(AssetId.class);
		Map<AssetId, List<FlagEvaluation>> flagsByAsset = new EnumMap<>(AssetId.class);
		Map<AssetId, CounterSet> rolling24hCountersByAsset = new EnumMap<>(AssetId.class);

		for (AssetId assetId : AssetId.values()) {
			CounterSet lifetimeCounters = counterAccumulationService.lifetimeCounters(assetId);
			CounterSet rolling24hCounters = counterAccumulationService.rolling24hCounters(assetId);

			FlagEvaluation heavyRecentUse = flagEvaluationService.heavyRecentUse(rolling24hCounters);
			FlagEvaluation thermalLoad = flagEvaluationService.thermalLoad(rolling24hCounters);
			FlagEvaluation mechanicalAttention = flagEvaluationService.mechanicalAttention(rolling24hCounters);
			FlagEvaluation highCumulativeLoad = flagEvaluationService.highCumulativeLoad(lifetimeCounters);
			List<FlagEvaluation> flags = List.of(heavyRecentUse, thermalLoad, mechanicalAttention,
					highCumulativeLoad);

			AssetStatus status = assetAssessmentService.status(flags);
			LiveCondition liveCondition = assetAssessmentService.liveCondition(
					counterAccumulationService.latestMessage(assetId),
					counterAccumulationService.latestArrivalRealTime(assetId));

			AssetState row = new AssetState();
			row.setAssetId(assetId.name());
			applyLifetimeCounters(row, lifetimeCounters);
			applyRolling24hCounters(row, rolling24hCounters);
			row.setWindowCoverageHours(counterAccumulationService.windowCoverageHours(assetId));
			row.setUtilizationPercent(counterAccumulationService.utilizationPercent(assetId));
			row.setProductiveSharePercent(counterAccumulationService.productiveSharePercent(assetId));
			row.setStatus(status);
			row.setLiveCondition(liveCondition);
			row.setRecommendation(assetAssessmentService.recommendation(status));
			applyFlags(row, heavyRecentUse, thermalLoad, mechanicalAttention, highCumulativeLoad);
			applyLatestMessage(row, counterAccumulationService.latestMessage(assetId));
			row.setUpdatedAt(Instant.now());

			rowsByAsset.put(assetId, row);
			flagsByAsset.put(assetId, flags);
			rolling24hCountersByAsset.put(assetId, rolling24hCounters);
		}

		// SPEC §5.4 is cross-asset, so the rank is only known once all four rows exist —
		// and all four are then written together (ARCHITECTURE.md §4).
		Map<AssetId, Integer> priorityRanks = fleetRankingService.assignPriorityRanks(flagsByAsset,
				rolling24hCountersByAsset);
		for (Map.Entry<AssetId, AssetState> row : rowsByAsset.entrySet()) {
			row.getValue().setPriorityRank(priorityRanks.get(row.getKey()));
		}
		assetStateRepository.saveAll(rowsByAsset.values());

		flushSealedBuckets();
	}

	/** A sealed hour is written once and never updated (IMPLEMENTATION-PLAN.md §1.4). */
	private void flushSealedBuckets() {
		List<HourlyAggregate> rows = new ArrayList<>();
		for (AssetId assetId : AssetId.values()) {
			for (Map.Entry<Instant, CounterSet> sealedBucket : counterAccumulationService.takeSealedBuckets(assetId)
					.entrySet()) {
				rows.add(toHourlyAggregate(assetId, sealedBucket.getKey(), sealedBucket.getValue()));
			}
		}
		if (!rows.isEmpty()) {
			hourlyAggregateRepository.saveAll(rows);
		}
	}

	private HourlyAggregate toHourlyAggregate(AssetId assetId, Instant hourStart, CounterSet counters) {
		HourlyAggregate row = new HourlyAggregate();
		row.setAssetId(assetId.name());
		row.setHourStart(hourStart);
		row.setEngineHours(counters.getEngineHours());
		row.setTotalWorkingHours(counters.getTotalWorkingHours());
		row.setTransportHours(counters.getTransportHours());
		row.setIdleHours(counters.getIdleHours());
		row.setLoadedHours(counters.getLoadedHours());
		row.setHighLoadHours(counters.getHighLoadHours());
		row.setTempExcursionHours(counters.getTempExcursionHours());
		row.setVibrationAlertHours(counters.getVibrationAlertHours());
		return row;
	}

	private void applyLifetimeCounters(AssetState row, CounterSet counters) {
		row.setLifetimeEngineHours(counters.getEngineHours());
		row.setLifetimeTotalWorkingHours(counters.getTotalWorkingHours());
		row.setLifetimeTransportHours(counters.getTransportHours());
		row.setLifetimeIdleHours(counters.getIdleHours());
		row.setLifetimeLoadedHours(counters.getLoadedHours());
		row.setLifetimeHighLoadHours(counters.getHighLoadHours());
		row.setLifetimeTempExcursionHours(counters.getTempExcursionHours());
		row.setLifetimeVibrationAlertHours(counters.getVibrationAlertHours());
	}

	private void applyRolling24hCounters(AssetState row, CounterSet counters) {
		row.setRolling24hEngineHours(counters.getEngineHours());
		row.setRolling24hTotalWorkingHours(counters.getTotalWorkingHours());
		row.setRolling24hTransportHours(counters.getTransportHours());
		row.setRolling24hIdleHours(counters.getIdleHours());
		row.setRolling24hLoadedHours(counters.getLoadedHours());
		row.setRolling24hHighLoadHours(counters.getHighLoadHours());
		row.setRolling24hTempExcursionHours(counters.getTempExcursionHours());
		row.setRolling24hVibrationAlertHours(counters.getVibrationAlertHours());
	}

	/** An inactive flag leaves all three of its columns NULL — it has no representation. */
	private void applyFlags(AssetState row, FlagEvaluation heavyRecentUse, FlagEvaluation thermalLoad,
			FlagEvaluation mechanicalAttention, FlagEvaluation highCumulativeLoad) {
		row.setHeavyRecentUseSeverity(heavyRecentUse.getSeverity());
		row.setHeavyRecentUseValue(heavyRecentUse.getValue());
		row.setHeavyRecentUseThreshold(heavyRecentUse.getThreshold());
		row.setThermalLoadSeverity(thermalLoad.getSeverity());
		row.setThermalLoadValue(thermalLoad.getValue());
		row.setThermalLoadThreshold(thermalLoad.getThreshold());
		row.setMechanicalAttentionSeverity(mechanicalAttention.getSeverity());
		row.setMechanicalAttentionValue(mechanicalAttention.getValue());
		row.setMechanicalAttentionThreshold(mechanicalAttention.getThreshold());
		row.setHighCumulativeLoadSeverity(highCumulativeLoad.getSeverity());
		row.setHighCumulativeLoadValue(highCumulativeLoad.getValue());
		row.setHighCumulativeLoadThreshold(highCumulativeLoad.getThreshold());
	}

	/** All seven columns stay NULL until the asset reports (SPEC §6.4). */
	private void applyLatestMessage(AssetState row, TelemetryMessage latestMessage) {
		if (latestMessage == null) {
			return;
		}
		row.setLatestTimestamp(latestMessage.getTimestamp());
		row.setLatestPtoEngaged(latestMessage.isPtoEngaged());
		row.setLatestGroundSpeedKmh(latestMessage.getGroundSpeedKmh());
		row.setLatestLoadFactor(latestMessage.getLoadFactor());
		row.setLatestTemperatureC(latestMessage.getTemperatureC());
		row.setLatestVibrationLevel(latestMessage.getVibrationLevel());
		row.setLatestWorkingStatus(latestMessage.getWorkingStatus());
	}
}
