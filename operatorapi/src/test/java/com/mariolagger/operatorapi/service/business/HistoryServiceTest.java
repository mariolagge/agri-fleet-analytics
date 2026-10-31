package com.mariolagger.operatorapi.service.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mariolagger.operatorapi.dto.AssetId;
import com.mariolagger.operatorapi.dto.HistoryResponse;
import com.mariolagger.operatorapi.dto.HistoryRow;
import com.mariolagger.operatorapi.entity.HourlyAggregate;
import com.mariolagger.operatorapi.repository.HourlyAggregateRepository;

/**
 * The repository is the only collaborator, so it is the only mock. Every 400 and 404 case is
 * decided before the repository is asked, so those tests stub nothing: Mockito's strict stubs
 * would fail a test on a stub it never uses.
 */
@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

	private static final Instant FROM = Instant.parse("2026-08-13T06:00:00Z");
	private static final Instant TO = Instant.parse("2026-08-13T09:00:00Z");
	private static final Instant HOUR_BEFORE_FROM = Instant.parse("2026-08-13T05:00:00Z");
	private static final Instant HOUR_07 = Instant.parse("2026-08-13T07:00:00Z");

	private static final long SEVEN_DAYS_IN_SECONDS = 7 * 24 * 3600;

	@Mock
	private HourlyAggregateRepository hourlyAggregateRepository;

	private HistoryService historyService;

	@BeforeEach
	void createService() {
		historyService = new HistoryService(hourlyAggregateRepository);
	}

	@Test
	@DisplayName("REQ-API-HISTORY: a missing from is a 400")
	void missingFromIsABadRequest() {
		ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
				() -> historyService.getHistory(null, TO.toString(), null));

		assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
	}

	@Test
	@DisplayName("REQ-API-HISTORY: an unparseable to is a 400")
	void unparseableToIsABadRequest() {
		ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
				() -> historyService.getHistory(FROM.toString(), "yesterday", null));

		assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
	}

	@Test
	@DisplayName("REQ-API-HISTORY: from equal to to is a 400")
	void fromEqualToToIsABadRequest() {
		ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
				() -> historyService.getHistory(FROM.toString(), FROM.toString(), null));

		assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
	}

	@Test
	@DisplayName("REQ-API-HISTORY: a range of seven days and one second is a 400")
	void rangeOfSevenDaysAndOneSecondIsABadRequest() {
		Instant sevenDaysAndOneSecondLater = FROM.plusSeconds(SEVEN_DAYS_IN_SECONDS + 1);

		ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
				() -> historyService.getHistory(FROM.toString(), sevenDaysAndOneSecondLater.toString(), null));

		assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
	}

	/** Nothing is stubbed: an unstubbed finder returns an empty list, which is all this needs. */
	@Test
	@DisplayName("REQ-API-HISTORY: a range of exactly seven days is accepted")
	void rangeOfExactlySevenDaysIsAccepted() {
		Instant sevenDaysLater = FROM.plusSeconds(SEVEN_DAYS_IN_SECONDS);

		HistoryResponse response = historyService.getHistory(FROM.toString(), sevenDaysLater.toString(), null);

		assertEquals(FROM, response.getFrom());
		assertEquals(sevenDaysLater, response.getTo());
	}

	@Test
	@DisplayName("REQ-API-HISTORY: an unknown asset id in a valid range is a 404")
	void unknownAssetIdInAValidRangeIsNotFound() {
		ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
				() -> historyService.getHistory(FROM.toString(), TO.toString(), "PLOUGH"));

		assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());
		verifyNoInteractions(hourlyAggregateRepository);
	}

	@Test
	@DisplayName("REQ-API-HISTORY: no stored rows give an empty rows list and not an error")
	void noStoredRowsGiveAnEmptyRowsListAndNotAnError() {
		when(hourlyAggregateRepository.findByHourStartLessThanAndHourStartGreaterThanOrderByHourStartAscAssetIdAsc(
				TO, HOUR_BEFORE_FROM)).thenReturn(List.of());

		HistoryResponse response = historyService.getHistory(FROM.toString(), TO.toString(), null);

		assertTrue(response.getRows().isEmpty());
	}

	/** A row overlaps [from, to) exactly when hourStart < to and hourStart > from minus one hour. */
	@Test
	@DisplayName("REQ-API-HISTORY: without an asset id the all-assets finder gets to and from minus one hour and its order is kept")
	void withoutAnAssetIdTheAllAssetsFinderGetsToAndFromMinusOneHourAndItsOrderIsKept() {
		when(hourlyAggregateRepository.findByHourStartLessThanAndHourStartGreaterThanOrderByHourStartAscAssetIdAsc(
				TO, HOUR_BEFORE_FROM)).thenReturn(List.of(hourlyAggregateRow(AssetId.BALER, FROM),
						hourlyAggregateRow(AssetId.MIXER, FROM), hourlyAggregateRow(AssetId.BALER, HOUR_07)));

		List<HistoryRow> rows = historyService.getHistory(FROM.toString(), TO.toString(), null).getRows();

		verify(hourlyAggregateRepository).findByHourStartLessThanAndHourStartGreaterThanOrderByHourStartAscAssetIdAsc(
				TO, HOUR_BEFORE_FROM);
		assertEquals(3, rows.size());
		assertEquals(AssetId.BALER, rows.get(0).getAssetId());
		assertEquals(FROM, rows.get(0).getHourStart());
		assertEquals(AssetId.MIXER, rows.get(1).getAssetId());
		assertEquals(FROM, rows.get(1).getHourStart());
		assertEquals(AssetId.BALER, rows.get(2).getAssetId());
		assertEquals(HOUR_07, rows.get(2).getHourStart());
	}

	private HourlyAggregate hourlyAggregateRow(AssetId assetId, Instant hourStart) {
		HourlyAggregate hourlyAggregate = new HourlyAggregate();
		hourlyAggregate.setAssetId(assetId.name());
		hourlyAggregate.setHourStart(hourStart);
		return hourlyAggregate;
	}
}
