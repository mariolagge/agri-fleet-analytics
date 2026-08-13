package com.mariolagger.operatorapi.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mariolagger.operatorapi.entity.AssetState;

public interface AssetStateRepository extends JpaRepository<AssetState, String> {

	/** SPEC §6.1: assets are ordered by priorityRank. */
	List<AssetState> findAllByOrderByPriorityRankAsc();
}
