package com.mariolagger.telemetryprocessor.entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class HourlyAggregateId implements Serializable {

	private String assetId;
	private Instant hourStart;

	public HourlyAggregateId() {
	}

	public HourlyAggregateId(String assetId, Instant hourStart) {
		this.assetId = assetId;
		this.hourStart = hourStart;
	}

	public String getAssetId() {
		return assetId;
	}

	public void setAssetId(String assetId) {
		this.assetId = assetId;
	}

	public Instant getHourStart() {
		return hourStart;
	}

	public void setHourStart(Instant hourStart) {
		this.hourStart = hourStart;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof HourlyAggregateId)) {
			return false;
		}
		HourlyAggregateId otherId = (HourlyAggregateId) other;
		return Objects.equals(assetId, otherId.assetId) && Objects.equals(hourStart, otherId.hourStart);
	}

	@Override
	public int hashCode() {
		return Objects.hash(assetId, hourStart);
	}
}
