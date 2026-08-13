package com.mariolagger.operatorapi.dto;

/** SPEC §5.1. Only active flags appear in a response (GLOSSARY §5). */
public enum Flag {
	HEAVY_RECENT_USE,
	THERMAL_LOAD,
	MECHANICAL_ATTENTION,
	HIGH_CUMULATIVE_LOAD
}
