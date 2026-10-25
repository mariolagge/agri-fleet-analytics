package com.mariolagger.telemetryprocessor.service.business;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mariolagger.telemetryprocessor.dto.AssetId;
import com.mariolagger.telemetryprocessor.dto.AssetStatus;
import com.mariolagger.telemetryprocessor.dto.LiveCondition;
import com.mariolagger.telemetryprocessor.dto.TelemetryMessage;
import com.mariolagger.telemetryprocessor.dto.VibrationLevel;

/**
 * No mocks and no clock: liveCondition is handed the now it should measure against, so every
 * staleness case is a pair of fixed instants.
 */
class AssetAssessmentServiceTest {

	private static final Instant NOW = Instant.parse("2026-08-13T08:00:00Z");
	private static final Instant TEN_SECONDS_AGO = NOW.minusSeconds(10);
	private static final Instant TWENTY_HOURS_AGO = NOW.minusSeconds(20 * 3600);
	private static final Instant FIVE_SECONDS_AHEAD = NOW.plusSeconds(5);

	private final AssetAssessmentService assetAssessmentService = new AssetAssessmentService();

	/** REQ-LIVE: staleness is the age of the reading itself, not of its delivery. */
	@Test
	@DisplayName("REQ-LIVE: a message twenty hours old is stale whatever its readings say")
	void messageTwentyHoursOldIsStaleWhateverItsReadingsSay() {
		TelemetryMessage backfilledAlarmingMessage = message(TWENTY_HOURS_AGO, 0.95, 99.0, VibrationLevel.HIGH);

		assertEquals(LiveCondition.STALE, assetAssessmentService.liveCondition(backfilledAlarmingMessage, NOW));
	}

	@Test
	@DisplayName("REQ-LIVE: an asset that has never reported is stale")
	void assetThatHasNeverReportedIsStale() {
		assertEquals(LiveCondition.STALE, assetAssessmentService.liveCondition(null, NOW));
	}

	/** A timestamp slightly ahead of now is a clock skew, not sixty seconds of silence. */
	@Test
	@DisplayName("REQ-LIVE: a message timestamped in the future is not stale")
	void messageTimestampedInTheFutureIsNotStale() {
		TelemetryMessage message = message(FIVE_SECONDS_AHEAD, 0.4, 70.0, VibrationLevel.NORMAL);

		assertEquals(LiveCondition.NORMAL, assetAssessmentService.liveCondition(message, NOW));
	}

	@Test
	@DisplayName("REQ-LIVE: fresh high vibration is alert")
	void freshHighVibrationIsAlert() {
		TelemetryMessage message = message(TEN_SECONDS_AGO, 0.4, 70.0, VibrationLevel.HIGH);

		assertEquals(LiveCondition.ALERT, assetAssessmentService.liveCondition(message, NOW));
	}

	@Test
	@DisplayName("REQ-LIVE: a fresh temperature above the alert bar is alert")
	void freshTemperatureAboveTheAlertBarIsAlert() {
		TelemetryMessage message = message(TEN_SECONDS_AGO, 0.4, 95.1, VibrationLevel.NORMAL);

		assertEquals(LiveCondition.ALERT, assetAssessmentService.liveCondition(message, NOW));
	}

	@Test
	@DisplayName("REQ-LIVE: fresh elevated vibration is caution")
	void freshElevatedVibrationIsCaution() {
		TelemetryMessage message = message(TEN_SECONDS_AGO, 0.4, 70.0, VibrationLevel.ELEVATED);

		assertEquals(LiveCondition.CAUTION, assetAssessmentService.liveCondition(message, NOW));
	}

	/** REQ-LIVE: the caution load factor row is inclusive. */
	@Test
	@DisplayName("REQ-LIVE: a load factor exactly at the caution bar is caution")
	void loadFactorExactlyAtTheCautionBarIsCaution() {
		TelemetryMessage message = message(TEN_SECONDS_AGO, 0.9, 70.0, VibrationLevel.NORMAL);

		assertEquals(LiveCondition.CAUTION, assetAssessmentService.liveCondition(message, NOW));
	}

	@Test
	@DisplayName("REQ-LIVE: fresh unremarkable readings are normal")
	void freshUnremarkableReadingsAreNormal() {
		TelemetryMessage message = message(TEN_SECONDS_AGO, 0.4, 70.0, VibrationLevel.NORMAL);

		assertEquals(LiveCondition.NORMAL, assetAssessmentService.liveCondition(message, NOW));
	}

	/** REQ-STATUS. */
	@Test
	@DisplayName("REQ-STATUS: four active flags are critical")
	void fourActiveFlagsAreCritical() {
		assertEquals(AssetStatus.CRITICAL, assetAssessmentService.status(flags(true, true, true, true)));
	}

	/** REQ-STATUS. */
	@Test
	@DisplayName("REQ-STATUS: two active flags are critical")
	void twoActiveFlagsAreCritical() {
		assertEquals(AssetStatus.CRITICAL, assetAssessmentService.status(flags(false, true, false, true)));
	}

	/** REQ-STATUS. */
	@Test
	@DisplayName("REQ-STATUS: one active flag is watch")
	void oneActiveFlagIsWatch() {
		assertEquals(AssetStatus.WATCH, assetAssessmentService.status(flags(false, false, true, false)));
	}

	/** REQ-STATUS. */
	@Test
	@DisplayName("REQ-STATUS: no active flag is good")
	void noActiveFlagIsGood() {
		assertEquals(AssetStatus.GOOD, assetAssessmentService.status(flags(false, false, false, false)));
	}

	/** REQ-RECOMMEND, verbatim — en dash included. */
	@Test
	@DisplayName("REQ-RECOMMEND: each status maps to its recommendation")
	void eachStatusMapsToItsRecommendation() {
		assertEquals("Priority check recommended within the next 5 engine hours.",
				assetAssessmentService.recommendation(AssetStatus.CRITICAL));
		assertEquals("Plan inspection within the next 15–20 engine hours.",
				assetAssessmentService.recommendation(AssetStatus.WATCH));
		assertEquals("No inspection recommended at this time.",
				assetAssessmentService.recommendation(AssetStatus.GOOD));
	}

	private List<FlagEvaluation> flags(boolean heavyRecentUse, boolean thermalLoad, boolean mechanicalAttention,
			boolean highCumulativeLoad) {
		return List.of(flag(heavyRecentUse), flag(thermalLoad), flag(mechanicalAttention), flag(highCumulativeLoad));
	}

	private FlagEvaluation flag(boolean active) {
		if (!active) {
			return FlagEvaluation.inactive();
		}
		return FlagEvaluation.active(3.0, 2.0);
	}

	private TelemetryMessage message(Instant timestamp, double loadFactor, double temperatureC,
			VibrationLevel vibrationLevel) {
		TelemetryMessage message = new TelemetryMessage();
		message.setAssetId(AssetId.TRACTOR);
		message.setTimestamp(timestamp);
		message.setPtoEngaged(true);
		message.setGroundSpeedKmh(8.0);
		message.setLoadFactor(loadFactor);
		message.setTemperatureC(temperatureC);
		message.setVibrationLevel(vibrationLevel);
		return message;
	}
}
