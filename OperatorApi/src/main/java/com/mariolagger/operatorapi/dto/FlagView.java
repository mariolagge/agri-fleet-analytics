package com.mariolagger.operatorapi.dto;

/**
 * SPEC §5.1: an active flag published together with the counter value and the threshold
 * that triggered it, so the farmer can always see the underlying reason.
 */
public class FlagView {

	private Flag flag;
	private FlagSeverity severity;
	private String counter;
	private FlagScope scope;
	private double value;
	private double threshold;

	public FlagView() {
	}

	public Flag getFlag() {
		return flag;
	}

	public void setFlag(Flag flag) {
		this.flag = flag;
	}

	public FlagSeverity getSeverity() {
		return severity;
	}

	public void setSeverity(FlagSeverity severity) {
		this.severity = severity;
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

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}
}
