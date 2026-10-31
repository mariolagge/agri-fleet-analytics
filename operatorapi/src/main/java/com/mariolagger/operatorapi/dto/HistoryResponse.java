package com.mariolagger.operatorapi.dto;

import java.time.Instant;
import java.util.List;

/** REQ-API-HISTORY: the body of GET /api/history. An empty `rows` array is a valid 200. */
public class HistoryResponse {

	private Instant from;
	private Instant to;
	private List<HistoryRow> rows;

	public HistoryResponse() {
	}

	public Instant getFrom() {
		return from;
	}

	public void setFrom(Instant from) {
		this.from = from;
	}

	public Instant getTo() {
		return to;
	}

	public void setTo(Instant to) {
		this.to = to;
	}

	public List<HistoryRow> getRows() {
		return rows;
	}

	public void setRows(List<HistoryRow> rows) {
		this.rows = rows;
	}
}
