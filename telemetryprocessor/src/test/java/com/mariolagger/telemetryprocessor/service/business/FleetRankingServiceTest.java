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
			flagsByAsset.put(assetId, flags(0));
			rolling24hCountersByAsset.put(assetId, rolling24hCounters(0.0, 0.0, 0.0));
		}
	}

	/** MOWER has the larger counter sum, so only criterion 1 can put TRACTOR first. */
	@Test
	@DisplayName("REQ-RANK: more active flags rank higher, whatever the counter sum")
	void moreActiveFlagsRankHigherWhateverTheCounterSum() {
		flagsByAsset.put(AssetId.TRACTOR, flags(2));
		flagsByAsset.put(AssetId.MOWER, flags(1));
		rolling24hCountersByAsset.put(AssetId.MOWER, rolling24hCounters(5.0, 5.0, 5.0));

		Map<AssetId, Integer> priorityRanks = assignPriorityRanks();

		assertEquals(1, priorityRanks.get(AssetId.TRACTOR));
		assertEquals(2, priorityRanks.get(AssetId.MOWER));
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
		flagsByAsset.put(AssetId.TRACTOR, flags(2));
		flagsByAsset.put(AssetId.MOWER, flags(1));
		flagsByAsset.put(AssetId.MIXER, flags(1));

		Map<AssetId, Integer> priorityRanks = assignPriorityRanks();

		assertEquals(AssetId.values().length, priorityRanks.size());
		List<Integer> ranks = new ArrayList<>(priorityRanks.values());
		ranks.sort(null);
		assertEquals(List.of(1, 2, 3, 4), ranks);
	}

	private Map<AssetId, Integer> assignPriorityRanks() {
		return fleetRankingService.assignPriorityRanks(flagsByAsset, rolling24hCountersByAsset);
	}

	/** The four flags of one asset, the first activeCount of them active. */
	private List<FlagEvaluation> flags(int activeCount) {
		List<FlagEvaluation> flags = new ArrayList<>();
		for (int index = 0; index < 4; index++) {
			flags.add(index < activeCount ? FlagEvaluation.active(3.0, 2.0) : FlagEvaluation.inactive());
		}
		return flags;
	}

	/** Only the three counters REQ-RANK criterion 2 adds up are set; the other two stay 0. */
	private CounterSet rolling24hCounters(double highLoadHours, double tempExcursionHours,
			double vibrationAlertHours) {
		return new CounterSet(0.0, 0.0, highLoadHours, tempExcursionHours, vibrationAlertHours);
	}
}
