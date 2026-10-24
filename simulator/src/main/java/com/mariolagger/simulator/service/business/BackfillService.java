package com.mariolagger.simulator.service.business;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.mariolagger.simulator.service.messaging.TelemetryPublisher;

@Component
public class BackfillService {

	// REQ-ACCUMULATE step 3 discards a delta GREATER than 60 s, so 60 s exactly is the
	// largest step the consumer still credits. This is the whole reason a 24 h window
	// can be filled in seconds; rounding it up to 61 would credit nothing at all.
	private static final long BACKFILL_STEP_SECONDS = 60;

	private final SimulatedFleet fleet;
	private final TelemetryGenerator telemetryGenerator;
	private final TelemetryPublisher telemetryPublisher;
	private final int backfillHours;

	public BackfillService(SimulatedFleet fleet, TelemetryGenerator telemetryGenerator,
			TelemetryPublisher telemetryPublisher,
			@Value("${simulator.backfill.hours:24}") int backfillHours) {
		this.fleet = fleet;
		this.telemetryGenerator = telemetryGenerator;
		this.telemetryPublisher = telemetryPublisher;
		this.backfillHours = backfillHours;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void publishBackfill() {
		Instant startedAt = Instant.now();
		int publishedMessages = 0;

		if (backfillHours > 0) {
			Instant backfillStart = startedAt.minus(Duration.ofHours(backfillHours));
			for (AssetSimulationState state : fleet.getAssets()) {
				Instant timestamp = backfillStart;
				while (!timestamp.isAfter(startedAt)) {
					telemetryPublisher.publish(telemetryGenerator.generate(state, timestamp));
					publishedMessages++;
					timestamp = timestamp.plusSeconds(BACKFILL_STEP_SECONDS);
				}
			}
		}

		long elapsedMillis = Duration.between(startedAt, Instant.now()).toMillis();
		fleet.startLiveCadence();

		// The elapsed time is also the gap between the last backfill timestamp and the
		// first live one. It has to stay under 60 s or that one interval is discarded.
		System.out.println("Backfill: published " + publishedMessages + " messages covering "
				+ backfillHours + " h in " + elapsedMillis + " ms; live cadence started");
	}
}
