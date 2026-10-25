package com.mariolagger.operatorapi.dto;

/**
 * REQ-FLAGS: an active flag published together with the counter value and the threshold
 * that triggered it, so the farmer can always see the underlying reason.
 */
public class FlagView {

	private Flag flag;
	private String counter;
	private FlagScope scope;
	private Double value;
	private Double threshold;

	public FlagView() {
	}

	public Flag getFlag() {
		return flag;
	}

	public void setFlag(Flag flag) {
		this.flag = flag;
	}

	public String getCounter() {
		return counter;
	}

	public void setCounter(String counter) {
		this.counter = counter;
	}

	public FlagScope getScope() {
		return scope;
	}

	public void setScope(FlagScope scope) {
		this.scope = scope;
	}

	public Double getValue() {
		return value;
	}

	public void setValue(Double value) {
		this.value = value;
	}

	public Double getThreshold() {
		return threshold;
	}

	public void setThreshold(Double threshold) {
		this.threshold = threshold;
	}
}
