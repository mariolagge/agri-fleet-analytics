package com.mariolagger.telemetryprocessor.service.business;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.mariolagger.telemetryprocessor.dto.AssetId;
import com.mariolagger.telemetryprocessor.dto.TelemetryMessage;

/**
 * Everything one asset needs in memory. The hourly buckets (REQ-BUCKETS) are derived state,
 * rebuilt from the stream on every run, so they live here and not in the database until
 * their hour completes.
 */
public class AssetAccumulator {

	private final AssetId assetId;

	/** REQ-BASELINES baseline plus everything accrued in this run. */
	private final CounterSet lifetime;

	/** Keyed by the UTC instant at which the Europe/Vienna hour began, oldest first. */
	private final SortedMap<Instant, CounterSet> buckets = new TreeMap<>();

	/** Hours already handed to the publisher, so a sealed hour is flushed exactly once. */
	private final Set<Instant> sealedHours = new HashSet<>();

	private Instant lastProcessedTimestamp;
	private Instant firstMessageTimestamp;
	private TelemetryMessage latestMessage;

	public AssetAccumulator(AssetId assetId, CounterSet lifetime) {
		this.assetId = assetId;
		this.lifetime = lifetime;
	}

	public CounterSet bucketFor(Instant hourStart) {
		return buckets.computeIfAbsent(hourStart, hour -> new CounterSet());
	}

	public AssetId getAssetId() {
		return assetId;
	}

	public CounterSet getLifetime() {
		return lifetime;
	}

	public SortedMap<Instant, CounterSet> getBuckets() {
		return buckets;
	}

	public Set<Instant> getSealedHours() {
		return sealedHours;
	}

	public Instant getLastProcessedTimestamp() {
		return lastProcessedTimestamp;
	}

	public void setLastProcessedTimestamp(Instant lastProcessedTimestamp) {
		this.lastProcessedTimestamp = lastProcessedTimestamp;
	}

	public Instant getFirstMessageTimestamp() {
		return firstMessageTimestamp;
	}

	public void setFirstMessageTimestamp(Instant firstMessageTimestamp) {
		this.firstMessageTimestamp = firstMessageTimestamp;
	}

	public TelemetryMessage getLatestMessage() {
		return latestMessage;
	}

	public void setLatestMessage(TelemetryMessage latestMessage) {
		this.latestMessage = latestMessage;
	}
}
