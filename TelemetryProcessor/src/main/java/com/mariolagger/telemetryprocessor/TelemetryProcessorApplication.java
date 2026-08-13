package com.mariolagger.telemetryprocessor;

import org.springframework.amqp.core.Queue;
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

	public static void main(String[] args) {
		SpringApplication.run(TelemetryProcessorApplication.class, args);
	}

}
