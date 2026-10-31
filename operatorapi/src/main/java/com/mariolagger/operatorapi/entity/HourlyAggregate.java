package com.mariolagger.operatorapi.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "hourly_aggregate")
@IdClass(HourlyAggregateId.class)
public class HourlyAggregate {

	@Id
	@Column(name = "asset_id")
	private String assetId;

	@Id
	@Column(name = "hour_start")
	private Instant hourStart;

	@Column(name = "engine_hours")
	private double engineHours;

	@Column(name = "total_working_hours")
	private double totalWorkingHours;

	@Column(name = "high_load_hours")
	private double highLoadHours;

	@Column(name = "temp_excursion_hours")
	private double tempExcursionHours;

	@Column(name = "vibration_alert_hours")
	private double vibrationAlertHours;

	public HourlyAggregate() {
	}

	public String getAssetId() {
		return assetId;
	}

	public void setAssetId(String assetId) {
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
