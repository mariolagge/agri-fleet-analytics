package com.mariolagger.telemetryprocessor.entity;

import java.time.Instant;

import com.mariolagger.telemetryprocessor.dto.AssetStatus;
import com.mariolagger.telemetryprocessor.dto.LiveCondition;
import com.mariolagger.telemetryprocessor.dto.VibrationLevel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "asset_state")
public class AssetState {

	@Id
	@Column(name = "asset_id")
	private String assetId;

	// REQ-COUNTER-SET counters, lifetime scope
	@Column(name = "lifetime_engine_hours")
	private double lifetimeEngineHours;

	@Column(name = "lifetime_total_working_hours")
	private double lifetimeTotalWorkingHours;

	@Column(name = "lifetime_high_load_hours")
	private double lifetimeHighLoadHours;

	@Column(name = "lifetime_temp_excursion_hours")
	private double lifetimeTempExcursionHours;

	@Column(name = "lifetime_vibration_alert_hours")
	private double lifetimeVibrationAlertHours;

	// REQ-COUNTER-SET counters, rolling 24 h scope (REQ-SCOPES)
	@Column(name = "rolling_24h_engine_hours")
	private double rolling24hEngineHours;

	@Column(name = "rolling_24h_total_working_hours")
	private double rolling24hTotalWorkingHours;

	@Column(name = "rolling_24h_high_load_hours")
	private double rolling24hHighLoadHours;

	@Column(name = "rolling_24h_temp_excursion_hours")
	private double rolling24hTempExcursionHours;

	@Column(name = "rolling_24h_vibration_alert_hours")
	private double rolling24hVibrationAlertHours;

	// REQ-UTILIZATION
	@Column(name = "window_coverage_hours")
	private double windowCoverageHours;

	@Column(name = "utilization_percent")
	private Double utilizationPercent;

	@Column(name = "productive_share_percent")
	private Double productiveSharePercent;

	// REQ-STATUS, REQ-LIVE, REQ-RANK, REQ-RECOMMEND
	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private AssetStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "live_condition")
	private LiveCondition liveCondition;

	@Column(name = "recommendation")
	private String recommendation;

	@Column(name = "priority_rank")
	private int priorityRank;

	// REQ-FLAGS flags; NULL value means the flag is inactive
	@Column(name = "heavy_recent_use_value")
	private Double heavyRecentUseValue;

	@Column(name = "heavy_recent_use_threshold")
	private Double heavyRecentUseThreshold;

	@Column(name = "thermal_load_value")
	private Double thermalLoadValue;

	@Column(name = "thermal_load_threshold")
	private Double thermalLoadThreshold;

	@Column(name = "mechanical_attention_value")
	private Double mechanicalAttentionValue;

	@Column(name = "mechanical_attention_threshold")
	private Double mechanicalAttentionThreshold;

	@Column(name = "high_cumulative_load_value")
	private Double highCumulativeLoadValue;

	@Column(name = "high_cumulative_load_threshold")
	private Double highCumulativeLoadThreshold;

	// REQ-TELEMETRY-SHAPE most recent message; all NULL until the asset reports
	@Column(name = "latest_timestamp")
	private Instant latestTimestamp;

	@Column(name = "latest_pto_engaged")
	private Boolean latestPtoEngaged;

	@Column(name = "latest_ground_speed_kmh")
	private Double latestGroundSpeedKmh;

	@Column(name = "latest_load_factor")
	private Double latestLoadFactor;

	@Column(name = "latest_temperature_c")
	private Double latestTemperatureC;

	@Enumerated(EnumType.STRING)
	@Column(name = "latest_vibration_level")
	private VibrationLevel latestVibrationLevel;

	@Column(name = "updated_at")
	private Instant updatedAt;

	public AssetState() {
	}

	public String getAssetId() {
		return assetId;
	}

	public void setAssetId(String assetId) {
		this.assetId = assetId;
	}

	public double getLifetimeEngineHours() {
		return lifetimeEngineHours;
	}

	public void setLifetimeEngineHours(double lifetimeEngineHours) {
		this.lifetimeEngineHours = lifetimeEngineHours;
	}

	public double getLifetimeTotalWorkingHours() {
		return lifetimeTotalWorkingHours;
	}

	public void setLifetimeTotalWorkingHours(double lifetimeTotalWorkingHours) {
		this.lifetimeTotalWorkingHours = lifetimeTotalWorkingHours;
	}

	public double getLifetimeHighLoadHours() {
		return lifetimeHighLoadHours;
	}

	public void setLifetimeHighLoadHours(double lifetimeHighLoadHours) {
		this.lifetimeHighLoadHours = lifetimeHighLoadHours;
	}

	public double getLifetimeTempExcursionHours() {
		return lifetimeTempExcursionHours;
	}

	public void setLifetimeTempExcursionHours(double lifetimeTempExcursionHours) {
		this.lifetimeTempExcursionHours = lifetimeTempExcursionHours;
	}

	public double getLifetimeVibrationAlertHours() {
		return lifetimeVibrationAlertHours;
	}

	public void setLifetimeVibrationAlertHours(double lifetimeVibrationAlertHours) {
		this.lifetimeVibrationAlertHours = lifetimeVibrationAlertHours;
	}

	public double getRolling24hEngineHours() {
		return rolling24hEngineHours;
	}

	public void setRolling24hEngineHours(double rolling24hEngineHours) {
		this.rolling24hEngineHours = rolling24hEngineHours;
	}

	public double getRolling24hTotalWorkingHours() {
		return rolling24hTotalWorkingHours;
	}

	public void setRolling24hTotalWorkingHours(double rolling24hTotalWorkingHours) {
		this.rolling24hTotalWorkingHours = rolling24hTotalWorkingHours;
	}

	public double getRolling24hHighLoadHours() {
		return rolling24hHighLoadHours;
	}

	public void setRolling24hHighLoadHours(double rolling24hHighLoadHours) {
		this.rolling24hHighLoadHours = rolling24hHighLoadHours;
	}

	public double getRolling24hTempExcursionHours() {
		return rolling24hTempExcursionHours;
	}

	public void setRolling24hTempExcursionHours(double rolling24hTempExcursionHours) {
		this.rolling24hTempExcursionHours = rolling24hTempExcursionHours;
	}

	public double getRolling24hVibrationAlertHours() {
		return rolling24hVibrationAlertHours;
	}

	public void setRolling24hVibrationAlertHours(double rolling24hVibrationAlertHours) {
		this.rolling24hVibrationAlertHours = rolling24hVibrationAlertHours;
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

	public int getPriorityRank() {
		return priorityRank;
	}

	public void setPriorityRank(int priorityRank) {
		this.priorityRank = priorityRank;
	}

	public Double getHeavyRecentUseValue() {
		return heavyRecentUseValue;
	}

	public void setHeavyRecentUseValue(Double heavyRecentUseValue) {
		this.heavyRecentUseValue = heavyRecentUseValue;
	}

	public Double getHeavyRecentUseThreshold() {
		return heavyRecentUseThreshold;
	}

	public void setHeavyRecentUseThreshold(Double heavyRecentUseThreshold) {
		this.heavyRecentUseThreshold = heavyRecentUseThreshold;
	}

	public Double getThermalLoadValue() {
		return thermalLoadValue;
	}

	public void setThermalLoadValue(Double thermalLoadValue) {
		this.thermalLoadValue = thermalLoadValue;
	}

	public Double getThermalLoadThreshold() {
		return thermalLoadThreshold;
	}

	public void setThermalLoadThreshold(Double thermalLoadThreshold) {
		this.thermalLoadThreshold = thermalLoadThreshold;
	}

	public Double getMechanicalAttentionValue() {
		return mechanicalAttentionValue;
	}

	public void setMechanicalAttentionValue(Double mechanicalAttentionValue) {
		this.mechanicalAttentionValue = mechanicalAttentionValue;
	}

	public Double getMechanicalAttentionThreshold() {
		return mechanicalAttentionThreshold;
	}

	public void setMechanicalAttentionThreshold(Double mechanicalAttentionThreshold) {
		this.mechanicalAttentionThreshold = mechanicalAttentionThreshold;
	}

	public Double getHighCumulativeLoadValue() {
		return highCumulativeLoadValue;
	}

	public void setHighCumulativeLoadValue(Double highCumulativeLoadValue) {
		this.highCumulativeLoadValue = highCumulativeLoadValue;
	}

	public Double getHighCumulativeLoadThreshold() {
		return highCumulativeLoadThreshold;
	}

	public void setHighCumulativeLoadThreshold(Double highCumulativeLoadThreshold) {
		this.highCumulativeLoadThreshold = highCumulativeLoadThreshold;
	}

	public Instant getLatestTimestamp() {
		return latestTimestamp;
	}

	public void setLatestTimestamp(Instant latestTimestamp) {
		this.latestTimestamp = latestTimestamp;
	}

	public Boolean getLatestPtoEngaged() {
		return latestPtoEngaged;
	}

	public void setLatestPtoEngaged(Boolean latestPtoEngaged) {
		this.latestPtoEngaged = latestPtoEngaged;
	}

	public Double getLatestGroundSpeedKmh() {
		return latestGroundSpeedKmh;
	}

	public void setLatestGroundSpeedKmh(Double latestGroundSpeedKmh) {
		this.latestGroundSpeedKmh = latestGroundSpeedKmh;
	}

	public Double getLatestLoadFactor() {
		return latestLoadFactor;
	}

	public void setLatestLoadFactor(Double latestLoadFactor) {
		this.latestLoadFactor = latestLoadFactor;
	}

	public Double getLatestTemperatureC() {
		return latestTemperatureC;
	}

	public void setLatestTemperatureC(Double latestTemperatureC) {
		this.latestTemperatureC = latestTemperatureC;
	}

	public VibrationLevel getLatestVibrationLevel() {
		return latestVibrationLevel;
	}

	public void setLatestVibrationLevel(VibrationLevel latestVibrationLevel) {
		this.latestVibrationLevel = latestVibrationLevel;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
