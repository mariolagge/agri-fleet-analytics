package com.mariolagger.operatorapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mariolagger.operatorapi.dto.LatestTelemetryResponse;
import com.mariolagger.operatorapi.service.business.AssetViewService;

/** SPEC §6.4: the lightweight endpoint behind the live telemetry strip. */
@RestController
@RequestMapping("/api/telemetry")
public class TelemetryController {

	private final AssetViewService assetViewService;

	public TelemetryController(AssetViewService assetViewService) {
		this.assetViewService = assetViewService;
	}

	@GetMapping("/latest")
	public LatestTelemetryResponse getLatestTelemetry() {
		return assetViewService.getLatestTelemetry();
	}
}
