package com.mariolagger.operatorapi.dto;

import java.util.List;

/** SPEC §6.1 and §6.2: one asset with its counters, flags, status and ranking. */
public class AssetView {

	private AssetId assetId;
	private int priorityRank;
	private AssetStatus status;
	private LiveCondition liveCondition;
	private String recommendation;
	private List<FlagView> flags;
	private CounterSetView lifetimeCounters;
	private CounterSetView rolling24hCounters;
	private double windowCoverageHours;

	// SPEC §4.5: both are null when their denominator is 0
	private Double utilizationPercent;
	private Double productiveSharePercent;

	public AssetView() {
	}

	public AssetId getAssetId() {
		return assetId;
	}

	public void setAssetId(AssetId assetId) {
		this.assetId = assetId;
	}

	public int getPriorityRank() {
		return priorityRank;
	}

	public void setPriorityRank(int priorityRank) {
		this.priorityRank = priorityRank;
	}

	public AssetStatus getStatus() {
		return status;
	}

	public void setStatus(AssetStatus status) {
		this.status = status;
	}

	public LiveCondition getLiveCondition() {
		return liveCondition;
	}

	public void setLiveCondition(LiveCondition liveCondition) {
		this.liveCondition = liveCondition;
	}

	public String getRecommendation() {
		return recommendation;
	}

	public void setRecommendation(String recommendation) {
		this.recommendation = recommendation;
	}

	public List<FlagView> getFlags() {
		return flags;
	}

	public void setFlags(List<FlagView> flags) {
		this.flags = flags;
	}

	public CounterSetView getLifetimeCounters() {
		return lifetimeCounters;
	}

	public void setLifetimeCounters(CounterSetView lifetimeCounters) {
		this.lifetimeCounters = lifetimeCounters;
	}

	public CounterSetView getRolling24hCounters() {
		return rolling24hCounters;
	}

	public void setRolling24hCounters(CounterSetView rolling24hCounters) {
		this.rolling24hCounters = rolling24hCounters;
	}

	public double getWindowCoverageHours() {
		return windowCoverageHours;
	}

	public void setWindowCoverageHours(double windowCoverageHours) {
		this.windowCoverageHours = windowCoverageHours;
	}

	public Double getUtilizationPercent() {
		return utilizationPercent;
	}

	public void setUtilizationPercent(Double utilizationPercent) {
		this.utilizationPercent = utilizationPercent;
	}

	public Double getProductiveSharePercent() {
		return productiveSharePercent;
	}

	public void setProductiveSharePercent(Double productiveSharePercent) {
		this.productiveSharePercent = productiveSharePercent;
	}
}
