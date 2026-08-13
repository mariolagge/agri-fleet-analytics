package com.mariolagger.FleetDataService;

import org.springframework.amqp.core.Queue;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FleetDataServiceApplication {

	@Bean
	public Queue telemetryQueue(){
		return new Queue("telemetry", true);
	}

	public static void main(String[] args) {
		SpringApplication.run(FleetDataServiceApplication.class, args);
	}

}
