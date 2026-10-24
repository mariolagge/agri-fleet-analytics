package com.mariolagger.operatorapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mariolagger.operatorapi.dto.AssetView;
import com.mariolagger.operatorapi.dto.FleetResponse;
import com.mariolagger.operatorapi.service.business.AssetViewService;

/**
 * REQ-API-ASSETS and REQ-API-ASSET. The full /api path is mapped here: ARCHITECTURE.md §5 passes the
 * prefix through nginx unchanged, so a servlet context path would produce /api/api/assets.
 */
@RestController
@RequestMapping("/api/assets")
public class AssetController {

	private final AssetViewService assetViewService;

	public AssetController(AssetViewService assetViewService) {
		this.assetViewService = assetViewService;
	}

	@GetMapping
	public FleetResponse getFleet() {
		return assetViewService.getFleet();
	}

	@GetMapping("/{assetId}")
	public AssetView getAsset(@PathVariable String assetId) {
		return assetViewService.getAsset(assetId);
	}
}
