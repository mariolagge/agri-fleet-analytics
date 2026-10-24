package com.mariolagger.simulator.scheduler;

import java.time.Instant;
import java.util.Random;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.mariolagger.simulator.service.business.AssetSimulationState;
import com.mariolagger.simulator.service.business.SimulatedFleet;
import com.mariolagger.simulator.service.business.TelemetryGenerator;
import com.mariolagger.simulator.service.messaging.TelemetryPublisher;

@Component
public class TelemetryScheduler {

	// REQ-TELEMETRY-CADENCE: one message per asset every 5-10 s. Each asset carries its own due
	// time, which is what makes the four independent of one another; a single tick
	// then serves all of them.
	private static final int MINIMUM_INTERVAL_SECONDS = 5;
	private static final int MAXIMUM_INTERVAL_SECONDS = 10;

	private final SimulatedFleet fleet;
	private final TelemetryGenerator telemetryGenerator;
	private final TelemetryPublisher telemetryPublisher;
	private final Random random = new Random();

	public TelemetryScheduler(SimulatedFleet fleet, TelemetryGenerator telemetryGenerator,
			TelemetryPublisher telemetryPublisher) {
		this.fleet = fleet;
		this.telemetryGenerator = telemetryGenerator;
		this.telemetryPublisher = telemetryPublisher;
	}

	@Scheduled(fixedDelay = 1000)
	public void publishDueMessages() {
		if (!fleet.isLiveCadenceStarted()) {
			return;
		}
		Instant now = Instant.now();
		for (AssetSimulationState state : fleet.getAssets()) {
			if (state.getNextDueAt() != null && state.getNextDueAt().isAfter(now)) {
				continue;
			}
			telemetryPublisher.publish(telemetryGenerator.generate(state, now));
			state.setNextDueAt(now.plusSeconds(nextIntervalSeconds()));
		}
	}

	private long nextIntervalSeconds() {
		return MINIMUM_INTERVAL_SECONDS
				+ random.nextInt(MAXIMUM_INTERVAL_SECONDS - MINIMUM_INTERVAL_SECONDS + 1);
	}
}
