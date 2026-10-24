package com.mariolagger.operatorapi.dto;

import java.time.Instant;

/**
 * REQ-API-HISTORY: one asset for one hour. Counter values are the amount accrued within that
 * hour, not running totals. `hourStart` is the UTC instant at which the Europe/Vienna
 * local hour began (REQ-BUCKETS).
 */
public class HistoryRow {

	private AssetId assetId;
	private Instant hourStart;
	private double engineHours;
	private double totalWorkingHours;
	private double transportHours;
	private double idleHours;
	private double loadedHours;
	private double highLoadHours;
	private double tempExcursionHours;
	private double vibrationAlertHours;

	public HistoryRow() {
	}

	public AssetId getAssetId() {
		return assetId;
	}

	public void setAssetId(AssetId assetId) {
		this.assetId = assetId;
	}

	public Instant getHourStart() {
		return hourStart;
	}

	public void setHourStart(Instant hourStart) {
		this.hourStart = hourStart;
	}

	public double getEngineHours() {
		return engineHours;
	}

	public void setEngineHours(double engineHours) {
		this.engineHours = engineHours;
	}

	public double getTotalWorkingHours() {
		return totalWorkingHours;
	}

	public void setTotalWorkingHours(double totalWorkingHours) {
		this.totalWorkingHours = totalWorkingHours;
	}

	public double getTransportHours() {
		return transportHours;
	}

	public void setTransportHours(double transportHours) {
		this.transportHours = transportHours;
	}

	public double getIdleHours() {
		return idleHours;
	}

	public void setIdleHours(double idleHours) {
		this.idleHours = idleHours;
	}

	public double getLoadedHours() {
		return loadedHours;
	}

	public void setLoadedHours(double loadedHours) {
		this.loadedHours = loadedHours;
	}

	public double getHighLoadHours() {
		return highLoadHours;
	}

	public void setHighLoadHours(double highLoadHours) {
		this.highLoadHours = highLoadHours;
	}

	public double getTempExcursionHours() {
		return tempExcursionHours;
	}

	public void setTempExcursionHours(double tempExcursionHours) {
		this.tempExcursionHours = tempExcursionHours;
	}

	public double getVibrationAlertHours() {
		return vibrationAlertHours;
	}

	public void setVibrationAlertHours(double vibrationAlertHours) {
		this.vibrationAlertHours = vibrationAlertHours;
	}
}
