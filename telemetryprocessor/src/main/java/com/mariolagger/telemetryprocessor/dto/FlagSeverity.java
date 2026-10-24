package com.mariolagger.telemetryprocessor.dto;

/** REQ-FLAGS. There is no INACTIVE value (GLOSSARY §5): an inactive flag has no severity. */
public enum FlagSeverity {
	MODERATE,
	SEVERE
}
