package com.mariolagger.operatorapi.service.business;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.mariolagger.operatorapi.dto.AssetId;
import com.mariolagger.operatorapi.dto.HistoryResponse;
import com.mariolagger.operatorapi.dto.HistoryRow;
import com.mariolagger.operatorapi.entity.HourlyAggregate;
import com.mariolagger.operatorapi.repository.HourlyAggregateRepository;

/**
 * REQ-API-HISTORY. Counter values are the amount accrued within that hour, exactly as
 * TelemetryProcessor sealed it — nothing is summed, scaled or re-derived here.
 */
@Component
public class HistoryService {

	private static final Duration MAXIMUM_RANGE = Duration.ofDays(7);

	private final HourlyAggregateRepository hourlyAggregateRepository;

	public HistoryService(HourlyAggregateRepository hourlyAggregateRepository) {
		this.hourlyAggregateRepository = hourlyAggregateRepository;
	}

	public HistoryResponse getHistory(String from, String to, String assetId) {
		Instant fromInstant = parseRequiredInstant(from, "from");
		Instant toInstant = parseRequiredInstant(to, "to");

		if (!fromInstant.isBefore(toInstant)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must be before to");
		}
		if (Duration.between(fromInstant, toInstant).compareTo(MAXIMUM_RANGE) > 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "the requested range must not exceed 7 days");
		}

		// REQ-API-HISTORY: a row covering [hourStart, hourStart + 1h) overlaps [from, to) exactly
		// when hourStart < to and hourStart > from − 1h
		Instant fromMinusOneHour = fromInstant.minus(1, ChronoUnit.HOURS);

		List<HourlyAggregate> hourlyAggregates;
		if (isBlank(assetId)) {
			hourlyAggregates = hourlyAggregateRepository
					.findByHourStartLessThanAndHourStartGreaterThanOrderByHourStartAscAssetIdAsc(
							toInstant, fromMinusOneHour);
		} else {
			hourlyAggregates = hourlyAggregateRepository
					.findByAssetIdAndHourStartLessThanAndHourStartGreaterThanOrderByHourStartAscAssetIdAsc(
							requireKnownAsset(assetId).name(), toInstant, fromMinusOneHour);
		}

		List<HistoryRow> rows = new ArrayList<>();
		for (HourlyAggregate hourlyAggregate : hourlyAggregates) {
			rows.add(toHistoryRow(hourlyAggregate));
		}

		HistoryResponse response = new HistoryResponse();
		response.setFrom(fromInstant);
		response.setTo(toInstant);
		response.setRows(rows);
		return response;
	}

	private HistoryRow toHistoryRow(HourlyAggregate hourlyAggregate) {
		HistoryRow row = new HistoryRow();
		row.setAssetId(AssetId.valueOf(hourlyAggregate.getAssetId()));
		row.setHourStart(hourlyAggregate.getHourStart());
		row.setEngineHours(round2(hourlyAggregate.getEngineHours()));
		row.setTotalWorkingHours(round2(hourlyAggregate.getTotalWorkingHours()));
		row.setHighLoadHours(round2(hourlyAggregate.getHighLoadHours()));
		row.setTempExcursionHours(round2(hourlyAggregate.getTempExcursionHours()));
		row.setVibrationAlertHours(round2(hourlyAggregate.getVibrationAlertHours()));
		return row;
	}

	/** REQ-API-HISTORY: 400 when the parameter is missing or unparseable. */
	private Instant parseRequiredInstant(String value, String parameterName) {
		if (isBlank(value)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, parameterName + " is required");
		}
		try {
			return Instant.parse(value);
		} catch (DateTimeParseException unparseable) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					parameterName + " must be an ISO-8601 instant, for example 2026-08-13T06:00:00Z");
		}
	}

	/** REQ-API-HISTORY: 404 when assetId is given but is not one of the four REQ-GOAL-SCOPE assets. */
	private AssetId requireKnownAsset(String assetId) {
		try {
			return AssetId.valueOf(assetId);
		} catch (IllegalArgumentException unknownAssetId) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown asset: " + assetId);
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	/** Exact numeric stays in the database; the response is rounded to 2 decimals. */
	private static double round2(double value) {
		return Math.round(value * 100.0) / 100.0;
	}
}
