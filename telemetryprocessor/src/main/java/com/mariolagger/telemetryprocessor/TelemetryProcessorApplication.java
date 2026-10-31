package com.mariolagger.telemetryprocessor;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TelemetryProcessorApplication {

	@Bean
	public Queue telemetryQueue() {
		return new Queue("telemetry", true);
	}

	@Bean
	public MessageConverter jsonMessageConverter() {
		// The Simulator publishes JSON with its own DTO class name in the __TypeId__ header.
		// The listener's type mapper prefers the type inferred from the @RabbitListener
		// method parameter, so this module deserializes into its own copy of the DTO
		// (ARCHITECTURE.md §2) without the two packages ever having to agree.
		return new JacksonJsonMessageConverter();
	}

	public static void main(String[] args) {
		SpringApplication.run(TelemetryProcessorApplication.class, args);
	}

}
