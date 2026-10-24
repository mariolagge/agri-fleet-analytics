package com.mariolagger.simulator.service.business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mariolagger.simulator.dto.AssetId;
import com.mariolagger.simulator.dto.TelemetryMessage;
import com.mariolagger.simulator.dto.WorkingStatus;

/**
 * No mocks, and no seed: the generator's Random is unseeded, so every run takes a different
 * path through the state cycle. Both assertions hold on every message of every path, so the
 * outcome is still deterministic. 2,000 messages 60 s apart are about 33 simulated hours per
 * asset — long enough to pass through every working status many times.
 */
class TelemetryGeneratorTest {

	private static final Instant START = Instant.parse("2026-08-13T00:00:00Z");
	private static final int MESSAGES_PER_ASSET = 2000;
	private static final long STEP_SECONDS = 60;

	private final TelemetryGenerator telemetryGenerator = new TelemetryGenerator();

	@Test
	@DisplayName("REQ-TELEMETRY-WORKING-STATUS: working status is WORKING exactly when pto is engaged and ground speed is above 2")
	void workingStatusIsWorkingExactlyWhenPtoIsEngagedAndGroundSpeedIsAboveTwo() {
		for (AssetId assetId : AssetId.values()) {
			for (TelemetryMessage message : generateMessages(assetId)) {
				boolean productive = message.isPtoEngaged() && message.getGroundSpeedKmh() > 2;
				assertEquals(productive, message.getWorkingStatus() == WorkingStatus.WORKING);
			}
		}
	}

	@Test
	@DisplayName("REQ-TELEMETRY-SHAPE: every message stays within the field constraints and carries its asset and timestamp")
	void everyMessageStaysWithinTheFieldConstraintsAndCarriesItsAssetAndTimestamp() {
		for (AssetId assetId : AssetId.values()) {
			List<TelemetryMessage> messages = generateMessages(assetId);
			for (int index = 0; index < messages.size(); index++) {
				TelemetryMessage message = messages.get(index);
				assertTrue(message.getGroundSpeedKmh() >= 0.0);
				assertTrue(message.getLoadFactor() >= 0.0);
				assertTrue(message.getLoadFactor() <= 1.0);
				assertEquals(assetId, message.getAssetId());
				assertEquals(timestampOf(index), message.getTimestamp());
			}
		}
	}

	/** One fresh state per asset, as on every simulator run (REQ-SIM-MEMORY). */
	private List<TelemetryMessage> generateMessages(AssetId assetId) {
		AssetSimulationState state = new AssetSimulationState(assetId);
		List<TelemetryMessage> messages = new ArrayList<>();
		for (int index = 0; index < MESSAGES_PER_ASSET; index++) {
			messages.add(telemetryGenerator.generate(state, timestampOf(index)));
		}
		return messages;
	}

	private Instant timestampOf(int index) {
		return START.plusSeconds(index * STEP_SECONDS);
	}
}
