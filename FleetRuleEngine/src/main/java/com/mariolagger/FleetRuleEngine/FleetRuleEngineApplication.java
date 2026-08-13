package com.mariolagger.FleetRuleEngine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FleetRuleEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(FleetRuleEngineApplication.class, args);
    }
}
