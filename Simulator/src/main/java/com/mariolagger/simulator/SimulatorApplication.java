package com.mariolagger.simulator;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SimulatorApplication {

	@Bean
	public Queue telemetryQueue() {
		return new Queue("telemetry", true);
	}

	@Bean
	public MessageConverter jsonMessageConverter() {
		// Without this the RabbitTemplate falls back to Java serialization.
		// SPEC §3.1: Jackson 3 writes Instant as an ISO-8601 UTC string by default,
		// so no date configuration is needed here.
		return new JacksonJsonMessageConverter();
	}

	public static void main(String[] args) {
		SpringApplication.run(SimulatorApplication.class, args);
	}
}
