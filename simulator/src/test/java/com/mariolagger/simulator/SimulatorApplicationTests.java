package com.mariolagger.simulator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// The burst is switched off here on purpose: a context-load test must not need a broker,
// and must not push 24 h of telemetry into the queue of a running demo.
@SpringBootTest(properties = "simulator.backfill.hours=0")
class SimulatorApplicationTests {

	@Test
	void contextLoads() {
	}

}
