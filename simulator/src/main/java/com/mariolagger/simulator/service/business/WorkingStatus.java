package com.mariolagger.simulator.service.business;

/** The state of a schedule segment (REQ-SIM-SCHEDULE). Internal to the simulator; not sent in telemetry. */
public enum WorkingStatus {
	WORKING,
	TRANSPORT,
	IDLE
}
