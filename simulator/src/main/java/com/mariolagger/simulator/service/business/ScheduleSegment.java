package com.mariolagger.simulator.service.business;

import com.mariolagger.simulator.dto.VibrationLevel;

/**
 * One fixed stretch of an asset's REQ-SIM-SCHEDULE cycle. The factory methods hold the
 * field values per state; a segment only differs from them where the schedule says so.
 */
public class ScheduleSegment {

	// REQ-SIM-SCHEDULE: field values per state.
	private static final double WORKING_TEMPERATURE_C = 80.0;
	private static final double TRANSPORT_LOAD_FACTOR = 0.25;
	private static final double TRANSPORT_TEMPERATURE_C = 65.0;
	private static final double IDLE_LOAD_FACTOR = 0.10;
	private static final double IDLE_TEMPERATURE_C = 50.0;

	private final WorkingStatus workingStatus;
	private final int minutes;
	private final double loadFactor;
	private final double temperatureC;
	private final VibrationLevel vibrationLevel;

	private ScheduleSegment(WorkingStatus workingStatus, int minutes, double loadFactor, double temperatureC,
			VibrationLevel vibrationLevel) {
		this.workingStatus = workingStatus;
		this.minutes = minutes;
		this.loadFactor = loadFactor;
		this.temperatureC = temperatureC;
		this.vibrationLevel = vibrationLevel;
	}

	public static ScheduleSegment working(int minutes, double loadFactor) {
		return new ScheduleSegment(WorkingStatus.WORKING, minutes, loadFactor, WORKING_TEMPERATURE_C,
				VibrationLevel.NORMAL);
	}

	public static ScheduleSegment workingHot(int minutes, double loadFactor, double temperatureC) {
		return new ScheduleSegment(WorkingStatus.WORKING, minutes, loadFactor, temperatureC, VibrationLevel.NORMAL);
	}

	public static ScheduleSegment workingVibrating(int minutes, double loadFactor) {
		return new ScheduleSegment(WorkingStatus.WORKING, minutes, loadFactor, WORKING_TEMPERATURE_C,
				VibrationLevel.HIGH);
	}

	public static ScheduleSegment transport(int minutes) {
		return new ScheduleSegment(WorkingStatus.TRANSPORT, minutes, TRANSPORT_LOAD_FACTOR, TRANSPORT_TEMPERATURE_C,
				VibrationLevel.NORMAL);
	}

	public static ScheduleSegment idle(int minutes) {
		return new ScheduleSegment(WorkingStatus.IDLE, minutes, IDLE_LOAD_FACTOR, IDLE_TEMPERATURE_C,
				VibrationLevel.NORMAL);
	}

	public WorkingStatus getWorkingStatus() {
		return workingStatus;
	}

	public int getMinutes() {
		return minutes;
	}

	public double getLoadFactor() {
		return loadFactor;
	}

	public double getTemperatureC() {
		return temperatureC;
	}

	public VibrationLevel getVibrationLevel() {
		return vibrationLevel;
	}
}
