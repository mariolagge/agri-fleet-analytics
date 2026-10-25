package com.mariolagger.telemetryprocessor.service.business;

/**
 * The five REQ-COUNTER-SET counters as one value, in hours. The same type carries an hourly
 * bucket (REQ-BUCKETS), a lifetime total (REQ-BASELINES) and the contribution of a single message,
 * because all three are the same five numbers added the same way.
 */
public class CounterSet {

	private double engineHours;
	private double totalWorkingHours;
	private double highLoadHours;
	private double tempExcursionHours;
	private double vibrationAlertHours;

	public CounterSet() {
	}

	public CounterSet(double engineHours, double totalWorkingHours, double highLoadHours, double tempExcursionHours,
			double vibrationAlertHours) {
		this.engineHours = engineHours;
		this.totalWorkingHours = totalWorkingHours;
		this.highLoadHours = highLoadHours;
		this.tempExcursionHours = tempExcursionHours;
		this.vibrationAlertHours = vibrationAlertHours;
	}

	public CounterSet copy() {
		return new CounterSet(engineHours, totalWorkingHours, highLoadHours, tempExcursionHours, vibrationAlertHours);
	}

	public void add(CounterSet other) {
		this.engineHours += other.engineHours;
		this.totalWorkingHours += other.totalWorkingHours;
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
