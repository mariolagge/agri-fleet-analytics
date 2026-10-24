package com.mariolagger.telemetryprocessor.service.business;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.mariolagger.telemetryprocessor.dto.AssetId;
import com.mariolagger.telemetryprocessor.dto.TelemetryMessage;
import com.mariolagger.telemetryprocessor.dto.VibrationLevel;
import com.mariolagger.telemetryprocessor.dto.WorkingStatus;

/**
 * The one place REQ-ACCUMULATE exists. Owns the four accumulators, credits every accepted
 * message into the hourly bucket containing its own timestamp and into the lifetime
 * total, and derives the rolling 24-hour window (REQ-SCOPES) and the REQ-UTILIZATION figures from them.
 *
 * Every public method is synchronized: the Rabbit listener thread and the 5 s refresh
 * scheduler both reach this state.
 */
@Component
public class CounterAccumulationService {

	private static final ZoneId FARM_ZONE = ZoneId.of("Europe/Vienna");

	/** REQ-ACCUMULATE step 3: a Δt above — not equal to — 60 s credits nothing. */
	private static final double MAXIMUM_CREDITED_DELTA_SECONDS = 60.0;

	// REQ-COUNTER-SET thresholds.
	private static final double PRODUCTIVE_GROUND_SPEED_KMH = 2.0;
	private static final double LOADED_LOAD_FACTOR = 0.55;
	private static final double HIGH_LOAD_FACTOR = 0.75;
	private static final double TEMP_EXCURSION_C = 90.0;

	/** REQ-SCOPES: the open bucket plus the 23 completed buckets before it. */
	private static final int ROLLING_WINDOW_HOURS = 24;

	/** The open bucket plus the last 24; older buckets are dropped once they are flushed. */
	private static final int RETAINED_BUCKETS = 25;

	private static final double SECONDS_PER_HOUR = 3600.0;

	private final Map<AssetId, AssetAccumulator> accumulators = new EnumMap<>(AssetId.class);

	public CounterAccumulationService() {
		for (AssetId assetId : AssetId.values()) {
			accumulators.put(assetId, new AssetAccumulator(assetId, AssetBaselines.baselineFor(assetId)));
		}
	}

	/**
	 * REQ-ACCUMULATE, in order. The four steps are deliberately written as one straight path:
	 * step 1 returns, step 3 falls through the crediting block, and every accepted message
	 * ends by storing its timestamp.
	 */
	public synchronized void accumulate(TelemetryMessage message) {
		AssetAccumulator accumulator = accumulators.get(message.getAssetId());
		Instant timestamp = message.getTimestamp();
		Instant lastProcessedTimestamp = accumulator.getLastProcessedTimestamp();

		// Step 1: out-of-order or duplicate delivery updates nothing at all.
		if (lastProcessedTimestamp != null && !timestamp.isAfter(lastProcessedTimestamp)) {
			return;
		}

		// Steps 2-4: Δt is undefined for the first message of an asset, and a Δt above 60 s
		// means something was down — both store the timestamp but credit no hours.
		if (lastProcessedTimestamp != null) {
			double deltaSeconds = secondsBetween(lastProcessedTimestamp, timestamp);
			if (deltaSeconds <= MAXIMUM_CREDITED_DELTA_SECONDS) {
				CounterSet contribution = contributionOf(message, deltaSeconds / SECONDS_PER_HOUR);
				accumulator.getLifetime().add(contribution);
				accumulator.bucketFor(hourStartOf(timestamp)).add(contribution);
			}
		}

		accumulator.setLastProcessedTimestamp(timestamp);
		if (accumulator.getFirstMessageTimestamp() == null) {
			accumulator.setFirstMessageTimestamp(timestamp);
		}
		accumulator.setLatestMessage(message);
	}

	/**
	 * REQ-COUNTER-SET — the hours this one message credits. The same contribution goes into the
	 * bucket and into the lifetime total, so the two scopes can never disagree about a
	 * message.
	 */
	private CounterSet contributionOf(TelemetryMessage message, double hours) {
		CounterSet contribution = new CounterSet();
		contribution.addEngineHours(hours);

		// REQ-TELEMETRY-PRODUCTIVE: productive work is derived from the condition itself, never from the
		// simulator's workingStatus, which only separates TRANSPORT from IDLE.
		boolean productiveWork = message.isPtoEngaged() && message.getGroundSpeedKmh() > PRODUCTIVE_GROUND_SPEED_KMH;

		// One if/else-if/else, so the §4.1 invariant engineHours = working + transport +
		// idle holds by construction rather than by arithmetic luck.
		if (productiveWork) {
			contribution.addTotalWorkingHours(hours);
		} else if (message.getWorkingStatus() == WorkingStatus.TRANSPORT) {
			contribution.addTransportHours(hours);
		} else {
			contribution.addIdleHours(hours);
		}

		if (productiveWork && message.getLoadFactor() >= LOADED_LOAD_FACTOR) {
			contribution.addLoadedHours(hours);
		}
		if (productiveWork && message.getLoadFactor() >= HIGH_LOAD_FACTOR) {
			contribution.addHighLoadHours(hours);
		}
		// Temperature and vibration are not gated on productive work (§4.1).
		if (message.getTemperatureC() > TEMP_EXCURSION_C) {
			contribution.addTempExcursionHours(hours);
		}
		if (message.getVibrationLevel() == VibrationLevel.ELEVATED
				|| message.getVibrationLevel() == VibrationLevel.HIGH) {
			contribution.addVibrationAlertHours(hours);
		}
		return contribution;
	}

	/**
	 * Everything one asset_state row needs, computed from the single now it is handed under a
	 * single lock acquisition. Reading the six values one call at a time let an hour boundary
	 * fall between them, so a published percentage need not have been the quotient of the
	 * counters published beside it.
	 */
	public synchronized AssetSnapshot snapshotOf(AssetId assetId, Instant now) {
		CounterSet rolling24hCounters = rolling24hCounters(assetId, now);
		double windowCoverageHours = windowCoverageHours(assetId, now);
		return new AssetSnapshot(lifetimeCounters(assetId), rolling24hCounters, windowCoverageHours,
				utilizationPercent(rolling24hCounters, windowCoverageHours),
				productiveSharePercent(rolling24hCounters), latestMessage(assetId));
	}

	/** REQ-BASELINES baseline plus this run. */
	private CounterSet lifetimeCounters(AssetId assetId) {
		CounterSet copy = new CounterSet();
		copy.add(accumulators.get(assetId).getLifetime());
		return copy;
	}

	/** REQ-SCOPES — the open bucket plus the 23 completed before it, summed. */
	private CounterSet rolling24hCounters(AssetId assetId, Instant now) {
		Instant windowStart = rollingWindowStart(now);
		CounterSet rolling = new CounterSet();
		for (Map.Entry<Instant, CounterSet> bucket : accumulators.get(assetId).getBuckets().entrySet()) {
			if (!bucket.getKey().isBefore(windowStart)) {
				rolling.add(bucket.getValue());
			}
		}
		return rolling;
	}

	/**
	 * REQ-UTILIZATION — from the later of (start of the oldest included bucket) and (the asset's
	 * first message in this run) up to now. 0 for an asset that has not reported.
	 */
	private double windowCoverageHours(AssetId assetId, Instant now) {
		AssetAccumulator accumulator = accumulators.get(assetId);
		Instant oldestIncludedBucketStart = oldestIncludedBucketStart(accumulator, rollingWindowStart(now));
		if (oldestIncludedBucketStart == null || accumulator.getFirstMessageTimestamp() == null) {
			return 0.0;
		}
		Instant coverageStart = oldestIncludedBucketStart.isAfter(accumulator.getFirstMessageTimestamp())
				? oldestIncludedBucketStart
				: accumulator.getFirstMessageTimestamp();
		return Math.max(secondsBetween(coverageStart, now) / SECONDS_PER_HOUR, 0.0);
	}

	/**
	 * REQ-UTILIZATION — null, not 0.0, when the denominator is 0. Both values are handed in rather
	 * than recomputed, so the published percentage is exactly the quotient of the counters
	 * published beside it.
	 */
	private Double utilizationPercent(CounterSet rolling24hCounters, double windowCoverageHours) {
		if (windowCoverageHours == 0.0) {
			return null;
		}
		return rolling24hCounters.getTotalWorkingHours() / windowCoverageHours * 100.0;
	}

	/** REQ-UTILIZATION — null, not 0.0, when the denominator is 0. */
	private Double productiveSharePercent(CounterSet rolling24hCounters) {
		if (rolling24hCounters.getEngineHours() == 0.0) {
			return null;
		}
		return rolling24hCounters.getTotalWorkingHours() / rolling24hCounters.getEngineHours() * 100.0;
	}

	private TelemetryMessage latestMessage(AssetId assetId) {
		return accumulators.get(assetId).getLatestMessage();
	}

	/**
	 * The buckets that can no longer grow and have not been handed over yet, oldest first, as
	 * copies. A pure read: the caller decides when the hand-over actually happened by calling
	 * markSealed afterwards.
	 *
	 * REQ-ACCUMULATE: message timestamps are strictly increasing per asset — step 1 discards
	 * anything else — so once a message for a later hour has been processed, no later message
	 * can land in an earlier hour. The asset's own stream, not wall-clock, is therefore the
	 * boundary between "may still grow" and "final".
	 */
	public synchronized Map<Instant, CounterSet> sealableBuckets(AssetId assetId) {
		AssetAccumulator accumulator = accumulators.get(assetId);
		Map<Instant, CounterSet> sealable = new LinkedHashMap<>();
		if (accumulator.getLastProcessedTimestamp() == null) {
			return sealable;
		}
		Instant openHourStart = hourStartOf(accumulator.getLastProcessedTimestamp());
		for (Map.Entry<Instant, CounterSet> bucket : accumulator.getBuckets().entrySet()) {
			if (bucket.getKey().isBefore(openHourStart) && !accumulator.getSealedHours().contains(bucket.getKey())) {
				sealable.put(bucket.getKey(), bucket.getValue().copy());
			}
		}
		return sealable;
	}

	/**
	 * Records that these hours reached hourly_aggregate, so they are never handed over again.
	 * The only place sealedHours grows and the only place buckets are pruned.
	 */
	public synchronized void markSealed(AssetId assetId, Set<Instant> hourStarts) {
		AssetAccumulator accumulator = accumulators.get(assetId);
		accumulator.getSealedHours().addAll(hourStarts);
		pruneBuckets(accumulator);
	}

	/**
	 * Drops the oldest buckets once more than RETAINED_BUCKETS exist. Only sealed hours are
	 * dropped: an hour that has never been handed over would otherwise be lost before it
	 * reached hourly_aggregate.
	 */
	private void pruneBuckets(AssetAccumulator accumulator) {
		while (accumulator.getBuckets().size() > RETAINED_BUCKETS) {
			Instant oldestHourStart = accumulator.getBuckets().firstKey();
			if (!accumulator.getSealedHours().contains(oldestHourStart)) {
				return;
			}
			accumulator.getBuckets().remove(oldestHourStart);
			accumulator.getSealedHours().remove(oldestHourStart);
		}
	}

	private Instant oldestIncludedBucketStart(AssetAccumulator accumulator, Instant windowStart) {
		for (Instant hourStart : accumulator.getBuckets().keySet()) {
			if (!hourStart.isBefore(windowStart)) {
				return hourStart;
			}
		}
		return null;
	}

	/** The whole Europe/Vienna hour containing this instant, as the UTC instant it began. */
	private Instant hourStartOf(Instant instant) {
		return ZonedDateTime.ofInstant(instant, FARM_ZONE).truncatedTo(ChronoUnit.HOURS).toInstant();
	}

	private Instant rollingWindowStart(Instant now) {
		return ZonedDateTime.ofInstant(now, FARM_ZONE)
				.truncatedTo(ChronoUnit.HOURS)
				.minusHours(ROLLING_WINDOW_HOURS - 1L)
				.toInstant();
	}

	/** Exact fractional seconds: truncating to whole seconds would lose most of a live Δt. */
	private double secondsBetween(Instant start, Instant end) {
		Duration elapsed = Duration.between(start, end);
		return elapsed.getSeconds() + elapsed.getNano() / 1_000_000_000.0;
	}
}
