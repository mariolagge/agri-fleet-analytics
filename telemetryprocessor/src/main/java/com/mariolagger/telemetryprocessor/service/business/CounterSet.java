package com.mariolagger.telemetryprocessor.service.business;

/**
 * The eight SPEC §4.1 counters as one value, in hours. The same type carries an hourly
 * bucket (SPEC §4.3), a lifetime total (§4.4) and the contribution of a single message,
 * because all three are the same eight numbers added the same way.
 */
public class CounterSet {

	private double engineHours;
	private double totalWorkingHours;
	private double transportHours;
	private double idleHours;
	private double loadedHours;
	private double highLoadHours;
	private double tempExcursionHours;
	private double vibrationAlertHours;

	public CounterSet() {
	}

	public CounterSet(double engineHours, double totalWorkingHours, double transportHours, double idleHours,
			double loadedHours, double highLoadHours, double tempExcursionHours, double vibrationAlertHours) {
		this.engineHours = engineHours;
		this.totalWorkingHours = totalWorkingHours;
		this.transportHours = transportHours;
		this.idleHours = idleHours;
		this.loadedHours = loadedHours;
		this.highLoadHours = highLoadHours;
		this.tempExcursionHours = tempExcursionHours;
		this.vibrationAlertHours = vibrationAlertHours;
	}

	public CounterSet copy() {
		return new CounterSet(engineHours, totalWorkingHours, transportHours, idleHours, loadedHours, highLoadHours,
				tempExcursionHours, vibrationAlertHours);
	}

	public void add(CounterSet other) {
		this.engineHours += other.engineHours;
		this.totalWorkingHours += other.totalWorkingHours;
		this.transportHours += other.transportHours;
		this.idleHours += other.idleHours;
		this.loadedHours += other.loadedHours;
		this.highLoadHours += other.highLoadHours;
		this.tempExcursionHours += other.tempExcursionHours;
		this.vibrationAlertHours += other.vibrationAlertHours;
	}

	public void addEngineHours(double hours) {
		this.engineHours += hours;
	}

	public void addTotalWorkingHours(double hours) {
		this.totalWorkingHours += hours;
	}

	public void addTransportHours(double hours) {
		this.transportHours += hours;
	}

	public void addIdleHours(double hours) {
		this.idleHours += hours;
	}

	public void addLoadedHours(double hours) {
		this.loadedHours += hours;
	}

	public void addHighLoadHours(double hours) {
		this.highLoadHours += hours;
	}

	public void addTempExcursionHours(double hours) {
		this.tempExcursionHours += hours;
	}

	public void addVibrationAlertHours(double hours) {
		this.vibrationAlertHours += hours;
	}

	public double getEngineHours() {
		return engineHours;
	}

	public double getTotalWorkingHours() {
		return totalWorkingHours;
	}

	public double getTransportHours() {
		return transportHours;
	}

	public double getIdleHours() {
		return idleHours;
	}

	public double getLoadedHours() {
		return loadedHours;
	}

	public double getHighLoadHours() {
		return highLoadHours;
	}

	public double getTempExcursionHours() {
		return tempExcursionHours;
	}

	public double getVibrationAlertHours() {
		return vibrationAlertHours;
	}
}
