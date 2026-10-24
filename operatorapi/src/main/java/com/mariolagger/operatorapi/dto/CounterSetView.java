package com.mariolagger.operatorapi.dto;

/** REQ-COUNTER-SET: the eight counters of one scope, in hours. */
public class CounterSetView {

	private double engineHours;
	private double totalWorkingHours;
	private double transportHours;
	private double idleHours;
	private double loadedHours;
	private double highLoadHours;
	private double tempExcursionHours;
	private double vibrationAlertHours;

	public CounterSetView() {
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
