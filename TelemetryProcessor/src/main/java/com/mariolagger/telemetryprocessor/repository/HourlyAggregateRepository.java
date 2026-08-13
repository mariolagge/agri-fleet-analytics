package com.mariolagger.telemetryprocessor.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mariolagger.telemetryprocessor.entity.HourlyAggregate;
import com.mariolagger.telemetryprocessor.entity.HourlyAggregateId;

public interface HourlyAggregateRepository extends JpaRepository<HourlyAggregate, HourlyAggregateId> {
}
