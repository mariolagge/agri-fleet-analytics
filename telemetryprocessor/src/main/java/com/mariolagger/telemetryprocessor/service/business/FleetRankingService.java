package com.mariolagger.telemetryprocessor.service.business;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.mariolagger.telemetryprocessor.dto.AssetId;

/**
 * REQ-RANK. All four assets are ranked every time; an asset with no flags simply sorts to
 * the bottom.
 */
@Component
public class FleetRankingService {

	/** 1-based ranks for the whole fleet, keyed by asset. */
	public Map<AssetId, Integer> assignPriorityRanks(Map<AssetId, List<FlagEvaluation>> flagsByAsset,
			Map<AssetId, CounterSet> rolling24hCountersByAsset) {
		List<AssetId> ordered = new ArrayList<>(List.of(AssetId.values()));
		ordered.sort((left, right) -> compare(left, right, flagsByAsset, rolling24hCountersByAsset));

		Map<AssetId, Integer> priorityRanks = new EnumMap<>(AssetId.class);
		for (int position = 0; position < ordered.size(); position++) {
			priorityRanks.put(ordered.get(position), position + 1);
		}
		return priorityRanks;
	}

	/** The three REQ-RANK criteria, in order. Criterion 3 makes the order fully deterministic. */
	private int compare(AssetId left, AssetId right, Map<AssetId, List<FlagEvaluation>> flagsByAsset,
			Map<AssetId, CounterSet> rolling24hCountersByAsset) {
		int byActiveCount = Integer.compare(FlagEvaluation.countActive(flagsByAsset.get(right)),
				FlagEvaluation.countActive(flagsByAsset.get(left)));
		if (byActiveCount != 0) {
			return byActiveCount;
		}
		int byCounterSum = Double.compare(counterSum(rolling24hCountersByAsset.get(right)),
				counterSum(rolling24hCountersByAsset.get(left)));
		if (byCounterSum != 0) {
			return byCounterSum;
		}
		return left.name().compareTo(right.name());
	}

	/**
	 * REQ-RANK criterion 2: a deliberate tie-breaking magnitude, not a physical quantity —
	 * the three counters can all be credited by the same message.
	 */
	private double counterSum(CounterSet rolling24hCounters) {
		return rolling24hCounters.getHighLoadHours() + rolling24hCounters.getTempExcursionHours()
				+ rolling24hCounters.getVibrationAlertHours();
	}
}
