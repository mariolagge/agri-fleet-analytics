package com.mariolagger.operatorapi.dto;

/** REQ-FLAGS. Only active flags appear in a response (GLOSSARY §5). */
public enum Flag {
	HEAVY_RECENT_USE,
	THERMAL_LOAD,
	MECHANICAL_ATTENTION,
	HIGH_CUMULATIVE_LOAD
}
