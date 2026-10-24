package com.mariolagger.operatorapi.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mariolagger.operatorapi.entity.HourlyAggregate;
import com.mariolagger.operatorapi.entity.HourlyAggregateId;

/**
 * REQ-API-HISTORY: one row per asset per hour that overlaps the requested period, ordered by
 * hourStart ascending then assetId ascending. A row covering [hourStart, hourStart + 1h)
 * overlaps [from, to) exactly when hourStart &lt; to and hourStart &gt; from − 1h, so both
 * finders take `to` and `from` minus one hour as their bounds.
 */
public interface HourlyAggregateRepository extends JpaRepository<HourlyAggregate, HourlyAggregateId> {

	List<HourlyAggregate> findByHourStartLessThanAndHourStartGreaterThanOrderByHourStartAscAssetIdAsc(
			Instant to, Instant fromMinusOneHour);

	List<HourlyAggregate> findByAssetIdAndHourStartLessThanAndHourStartGreaterThanOrderByHourStartAscAssetIdAsc(
			String assetId, Instant to, Instant fromMinusOneHour);
}
