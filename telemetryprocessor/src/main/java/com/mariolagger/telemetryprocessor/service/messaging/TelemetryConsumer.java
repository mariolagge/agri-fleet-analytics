package com.mariolagger.telemetryprocessor.service.messaging;

import java.time.Duration;
import java.time.Instant;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.mariolagger.telemetryprocessor.dto.TelemetryMessage;
import com.mariolagger.telemetryprocessor.service.business.AssetStatePublisher;
import com.mariolagger.telemetryprocessor.service.business.CounterAccumulationService;

/**
 * The only consumer of the telemetry queue (ARCHITECTURE.md §3).
 */
@Component
public class TelemetryConsumer {

	/**
	 * ARCHITECTURE.md §4 catch-up: a message this far behind wall-clock belongs to the
	 * startup backfill, so it is accumulated but not written. Otherwise the ~5760-message
	 * burst would produce ~5760 four-row transactions before the API is usable. Nothing is
	 * lost — the 5 s refresh publishes whatever is in memory.
	 */
	private static final Duration CATCH_UP_BEHIND = Duration.ofMinutes(2);

	private final CounterAccumulationService counterAccumulationService;
	private final AssetStatePublisher assetStatePublisher;

	public TelemetryConsumer(CounterAccumulationService counterAccumulationService,
			AssetStatePublisher assetStatePublisher) {
		this.counterAccumulationService = counterAccumulationService;
		this.assetStatePublisher = assetStatePublisher;
	}

	@RabbitListener(queues = "telemetry")
	public void onTelemetryMessage(TelemetryMessage message) {
		counterAccumulationService.accumulate(message);
		if (Duration.between(message.getTimestamp(), Instant.now()).compareTo(CATCH_UP_BEHIND) > 0) {
			return;
		}
		assetStatePublisher.publish();
	}
}
