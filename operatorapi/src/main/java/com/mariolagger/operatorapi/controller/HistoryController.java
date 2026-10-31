package com.mariolagger.operatorapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mariolagger.operatorapi.dto.HistoryResponse;
import com.mariolagger.operatorapi.service.business.HistoryService;

/**
 * REQ-API-HISTORY. The three parameters arrive as raw strings so that "missing" and
 * "unparseable" both reach HistoryService and come back as the REQ-API-HISTORY 400.
 */
@RestController
@RequestMapping("/api/history")
public class HistoryController {

	private final HistoryService historyService;

	public HistoryController(HistoryService historyService) {
		this.historyService = historyService;
	}

	@GetMapping
	public HistoryResponse getHistory(@RequestParam(required = false) String from,
			@RequestParam(required = false) String to,
			@RequestParam(required = false) String assetId) {
		return historyService.getHistory(from, to, assetId);
	}
}
