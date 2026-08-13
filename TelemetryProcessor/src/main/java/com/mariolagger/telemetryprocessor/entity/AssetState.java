package com.mariolagger.telemetryprocessor.entity;

import java.time.Instant;

import com.mariolagger.telemetryprocessor.dto.VibrationLevel;
import com.mariolagger.telemetryprocessor.dto.WorkingStatus;

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

	// SPEC §4.1 counters, lifetime scope
	@Column(name = "lifetime_engine_hours")
	private double lifetimeEngineHours;

	@Column(name = "lifetime_total_working_hours")
	private double lifetimeTotalWorkingHours;

	@Column(name = "lifetime_transport_hours")
	private double lifetimeTransportHours;

	@Column(name = "lifetime_idle_hours")
	private double lifetimeIdleHours;

	@Column(name = "lifetime_loaded_hours")
	private double lifetimeLoadedHours;

	@Column(name = "lifetime_high_load_hours")
	private double lifetimeHighLoadHours;

	@Column(name = "lifetime_temp_excursion_hours")
	private double lifetimeTempExcursionHours;

	@Column(name = "lifetime_vibration_alert_hours")
	private double lifetimeVibrationAlertHours;

	// SPEC §4.1 counters, rolling 24 h scope (§4.3)
	@Column(name = "rolling_24h_engine_hours")
	private double rolling24hEngineHours;

	@Column(name = "rolling_24h_total_working_hours")
	private double rolling24hTotalWorkingHours;

	@Column(name = "rolling_24h_transport_hours")
	private double rolling24hTransportHours;

	@Column(name = "rolling_24h_idle_hours")
	private double rolling24hIdleHours;

	@Column(name = "rolling_24h_loaded_hours")
	private double rolling24hLoadedHours;

	@Column(name = "rolling_24h_high_load_hours")
	private double rolling24hHighLoadHours;

	@Column(name = "rolling_24h_temp_excursion_hours")
	private double rolling24hTempExcursionHours;

	@Column(name = "rolling_24h_vibration_alert_hours")
	private double rolling24hVibrationAlertHours;

	// SPEC §4.5
	@Column(name = "window_coverage_hours")
	private double windowCoverageHours;

	@Column(name = "utilization_percent")
	private Double utilizationPercent;

	@Column(name = "productive_share_percent")
	private Double productiveSharePercent;

	// SPEC §5.2, §5.3, §5.4, §5.5
	@Column(name = "status")
	private String status;

	@Column(name = "live_condition")
	private String liveCondition;

	@Column(name = "recommendation")
	private String recommendation;

	@Column(name = "priority_rank")
	private int priorityRank;

	// SPEC §5.1 flags; NULL severity means the flag is inactive
	@Column(name = "heavy_recent_use_severity")
	private String heavyRecentUseSeverity;

	@Column(name = "heavy_recent_use_value")
	private Double heavyRecentUseValue;

	@Column(name = "heavy_recent_use_threshold")
	private Double heavyRecentUseThreshold;

	@Column(name = "thermal_load_severity")
	private String thermalLoadSeverity;

	@Column(name = "thermal_load_value")
	private Double thermalLoadValue;

	@Column(name = "thermal_load_threshold")
	private Double thermalLoadThreshold;

	@Column(name = "mechanical_attention_severity")
	private String mechanicalAttentionSeverity;

	@Column(name = "mechanical_attention_value")
	private Double mechanicalAttentionValue;

	@Column(name = "mechanical_attention_threshold")
	private Double mechanicalAttentionThreshold;

	@Column(name = "high_cumulative_load_severity")
	private String highCumulativeLoadSeverity;

	@Column(name = "high_cumulative_load_value")
	private Double highCumulativeLoadValue;

	@Column(name = "high_cumulative_load_threshold")
	private Double highCumulativeLoadThreshold;

	// SPEC §3.1 most recent message; all NULL until the asset reports
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

	@Enumerated(EnumType.STRING)
	@Column(name = "latest_working_status")
	private WorkingStatus latestWorkingStatus;

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

	public double getLifetimeTransportHours() {
		return lifetimeTransportHours;
	}

	public void setLifetimeTransportHours(double lifetimeTransportHours) {
		this.lifetimeTransportHours = lifetimeTransportHours;
	}

	public double getLifetimeIdleHours() {
		return lifetimeIdleHours;
	}

	public void setLifetimeIdleHours(double lifetimeIdleHours) {
		this.lifetimeIdleHours = lifetimeIdleHours;
	}

	public double getLifetimeLoadedHours() {
		return lifetimeLoadedHours;
	}

	public void setLifetimeLoadedHours(double lifetimeLoadedHours) {
		this.lifetimeLoadedHours = lifetimeLoadedHours;
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

	public double getRolling24hTransportHours() {
		return rolling24hTransportHours;
	}

	public void setRolling24hTransportHours(double rolling24hTransportHours) {
		this.rolling24hTransportHours = rolling24hTransportHours;
	}

	public double getRolling24hIdleHours() {
		return rolling24hIdleHours;
	}

	public void setRolling24hIdleHours(double rolling24hIdleHours) {
		this.rolling24hIdleHours = rolling24hIdleHours;
	}

	public double getRolling24hLoadedHours() {
		return rolling24hLoadedHours;
	}

	public void setRolling24hLoadedHours(double rolling24hLoadedHours) {
		this.rolling24hLoadedHours = rolling24hLoadedHours;
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getLiveCondition() {
		return liveCondition;
	}

	public void setLiveCondition(String liveCondition) {
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

	public String getHeavyRecentUseSeverity() {
		return heavyRecentUseSeverity;
	}

	public void setHeavyRecentUseSeverity(String heavyRecentUseSeverity) {
		this.heavyRecentUseSeverity = heavyRecentUseSeverity;
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

	public String getThermalLoadSeverity() {
		return thermalLoadSeverity;
	}

	public void setThermalLoadSeverity(String thermalLoadSeverity) {
		this.thermalLoadSeverity = thermalLoadSeverity;
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

	public String getMechanicalAttentionSeverity() {
		return mechanicalAttentionSeverity;
	}

	public void setMechanicalAttentionSeverity(String mechanicalAttentionSeverity) {
		this.mechanicalAttentionSeverity = mechanicalAttentionSeverity;
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

	public String getHighCumulativeLoadSeverity() {
		return highCumulativeLoadSeverity;
	}

	public void setHighCumulativeLoadSeverity(String highCumulativeLoadSeverity) {
		this.highCumulativeLoadSeverity = highCumulativeLoadSeverity;
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

	public WorkingStatus getLatestWorkingStatus() {
		return latestWorkingStatus;
	}

	public void setLatestWorkingStatus(WorkingStatus latestWorkingStatus) {
		this.latestWorkingStatus = latestWorkingStatus;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
