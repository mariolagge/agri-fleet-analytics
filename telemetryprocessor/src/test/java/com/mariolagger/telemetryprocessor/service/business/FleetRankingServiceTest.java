package com.mariolagger.telemetryprocessor.service.business;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mariolagger.telemetryprocessor.dto.AssetId;
import com.mariolagger.telemetryprocessor.dto.FlagSeverity;

/**
 * No mocks. REQ-RANK ranks all four assets together, so every test starts from a fleet with
 * no flags and zero counters for all four assets and then changes only the assets it is about.
 * Each case is built so that the criteria below the one under test would give the opposite
 * order: alphabetically BALER comes before MIXER, MOWER and TRACTOR.
 */
class FleetRankingServiceTest {

	private final FleetRankingService fleetRankingService = new FleetRankingService();

	private final Map<AssetId, List<FlagEvaluation>> flagsByAsset = new EnumMap<>(AssetId.class);
	private final Map<AssetId, CounterSet> rolling24hCountersByAsset = new EnumMap<>(AssetId.class);

	@BeforeEach
	void createFleetWithNoFlagsAndZeroCounters() {
		for (AssetId assetId : AssetId.values()) {
			flagsByAsset.put(assetId, flags(null, null, null, null));
			rolling24hCountersByAsset.put(assetId, rolling24hCounters(0.0, 0.0, 0.0));
		}
	}

	@Test
	@DisplayName("REQ-RANK: one severe flag outranks two moderate flags")
	void oneSevereFlagOutranksTwoModerateFlags() {
		flagsByAsset.put(AssetId.TRACTOR, flags(FlagSeverity.SEVERE, null, null, null));
		flagsByAsset.put(AssetId.MOWER, flags(FlagSeverity.MODERATE, FlagSeverity.MODERATE, null, null));

		Map<AssetId, Integer> priorityRanks = assignPriorityRanks();

		assertEquals(1, priorityRanks.get(AssetId.TRACTOR));
		assertEquals(2, priorityRanks.get(AssetId.MOWER));
	}

	@Test
	@DisplayName("REQ-RANK: with equal severe counts, more moderate flags rank higher")
	void withEqualSevereCountsMoreModerateFlagsRankHigher() {
		flagsByAsset.put(AssetId.TRACTOR, flags(FlagSeverity.SEVERE, FlagSeverity.MODERATE, null, null));
		flagsByAsset.put(AssetId.BALER, flags(FlagSeverity.SEVERE, null, null, null));

		Map<AssetId, Integer> priorityRanks = assignPriorityRanks();

		assertEquals(1, priorityRanks.get(AssetId.TRACTOR));
		assertEquals(2, priorityRanks.get(AssetId.BALER));
	}

	/** BALER has the largest single counter, MIXER the larger sum of the three. */
	@Test
	@DisplayName("REQ-RANK: with equal flag counts, the larger sum of the three rolling counters ranks higher")
	void withEqualFlagCountsTheLargerSumOfTheThreeRollingCountersRanksHigher() {
		rolling24hCountersByAsset.put(AssetId.MIXER, rolling24hCounters(0.2, 0.2, 0.2));
		rolling24hCountersByAsset.put(AssetId.BALER, rolling24hCounters(0.5, 0.0, 0.0));

		Map<AssetId, Integer> priorityRanks = assignPriorityRanks();

		assertEquals(1, priorityRanks.get(AssetId.MIXER));
		assertEquals(2, priorityRanks.get(AssetId.BALER));
	}

	@Test
	@DisplayName("REQ-RANK: with everything equal, ranks follow asset id ascending")
	void withEverythingEqualRanksFollowAssetIdAscending() {
		Map<AssetId, Integer> priorityRanks = assignPriorityRanks();

		assertEquals(1, priorityRanks.get(AssetId.BALER));
		assertEquals(2, priorityRanks.get(AssetId.MIXER));
		assertEquals(3, priorityRanks.get(AssetId.MOWER));
		assertEquals(4, priorityRanks.get(AssetId.TRACTOR));
	}

	/** MOWER and MIXER are tied on every criterion except the last. */
	@Test
	@DisplayName("REQ-RANK: the result holds the ranks 1 to 4, each exactly once")
	void theResultHoldsTheRanksOneToFourEachExactlyOnce() {
		flagsByAsset.put(AssetId.TRACTOR, flags(FlagSeverity.SEVERE, null, null, null));
		flagsByAsset.put(AssetId.MOWER, flags(FlagSeverity.MODERATE, null, null, null));
		flagsByAsset.put(AssetId.MIXER, flags(FlagSeverity.MODERATE, null, null, null));

		Map<AssetId, Integer> priorityRanks = assignPriorityRanks();

		assertEquals(AssetId.values().length, priorityRanks.size());
		List<Integer> ranks = new ArrayList<>(priorityRanks.values());
		ranks.sort(null);
		assertEquals(List.of(1, 2, 3, 4), ranks);
	}

	private Map<AssetId, Integer> assignPriorityRanks() {
		return fleetRankingService.assignPriorityRanks(flagsByAsset, rolling24hCountersByAsset);
	}

	private List<FlagEvaluation> flags(FlagSeverity heavyRecentUse, FlagSeverity thermalLoad,
			FlagSeverity mechanicalAttention, FlagSeverity highCumulativeLoad) {
		return List.of(flag(heavyRecentUse), flag(thermalLoad), flag(mechanicalAttention), flag(highCumulativeLoad));
	}

	private FlagEvaluation flag(FlagSeverity severity) {
		if (severity == null) {
			return FlagEvaluation.inactive();
		}
		return FlagEvaluation.active(severity, 3.0, 2.0);
	}

	/** Only the three counters REQ-RANK criterion 3 adds up are set; the other five stay 0. */
	private CounterSet rolling24hCounters(double highLoadHours, double tempExcursionHours,
			double vibrationAlertHours) {
		return new CounterSet(0.0, 0.0, 0.0, 0.0, 0.0, highLoadHours, tempExcursionHours, vibrationAlertHours);
	}
}
