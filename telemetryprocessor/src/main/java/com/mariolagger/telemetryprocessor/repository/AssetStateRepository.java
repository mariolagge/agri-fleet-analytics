package com.mariolagger.telemetryprocessor.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mariolagger.telemetryprocessor.entity.AssetState;

public interface AssetStateRepository extends JpaRepository<AssetState, String> {
}
