package com.mariolagger.simulator.service.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.mariolagger.simulator.dto.TelemetryMessage;

@Component
public class TelemetryPublisher {

	// ARCHITECTURE.md §3: one durable queue, reached through the default exchange, so
	// the queue name is the routing key and there is no exchange or binding to declare.
	private static final String TELEMETRY_QUEUE = "telemetry";

	private final RabbitTemplate rabbitTemplate;

	public TelemetryPublisher(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	public void publish(TelemetryMessage message) {
		rabbitTemplate.convertAndSend(TELEMETRY_QUEUE, message);
	}
}
