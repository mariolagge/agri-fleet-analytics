package com.mariolagger.telemetryprocessor.dto;

/** REQ-LIVE. Derived from the latest message only and never merged into the status. */
public enum LiveCondition {
	NORMAL,
	CAUTION,
	ALERT,
	STALE
}
