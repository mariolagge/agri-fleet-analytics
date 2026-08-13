package com.mariolagger.operatorapi.dto;

/** SPEC §5.3. Derived from the latest message only and never merged into the status. */
public enum LiveCondition {
	NORMAL,
	CAUTION,
	ALERT,
	STALE
}
