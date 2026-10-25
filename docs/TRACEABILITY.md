# docs\TRACEABILITY.md

This document is a derived view, built by hand from the *Source* attributes in `SPEC.md`, the user stories in `USER-STORY-MAP.md`, the `REQ-` comments in the code and the `DisplayName` of the unit tests. Where it disagrees with those, they win. Update it in the same change as any of them.

---

## 1. Stakeholder needs → user stories → requirements

| Need | Stakeholder | User stories | Requirements |
|------|-------------|--------------|--------------|
| `NEED-MONITOR-FLEET` | `STK-FARMER` | `US-1.1` | `REQ-GOAL-SCOPE` |
| `NEED-PRIORITISE-MAINTENANCE` | `STK-FARMER` | `US-1.1`<br>`US-2.1`<br>`US-2.2`<br>`US-3.2` | `REQ-FLAGS`<br>`REQ-STATUS`<br>`REQ-RANK`<br>`REQ-RECOMMEND`<br>`REQ-API-ASSETS`<br>`REQ-DECISIONS` |
| `NEED-OPERATIONAL-RISK` | `STK-FARMER` | `US-1.2`<br>`US-4.1`<br>`US-4.2` | `REQ-SCOPES`<br>`REQ-LIVE`<br>`REQ-API-LATEST`<br>`REQ-DECISIONS` |
| `NEED-UTILIZATION-AWARENESS` | `STK-FARMER` | `US-5.1` | `REQ-COUNTER-SET`<br>`REQ-UTILIZATION`<br>`REQ-DECISIONS` |
| `NEED-FLAG-REASON` | `STK-WORKSHOP` | `US-3.1`<br>`US-3.2`<br>`US-6.1` | `REQ-GOAL-TRACE`<br>`REQ-FLAGS`<br>`REQ-API-ASSETS` |
| `NEED-HISTORY-REVIEW` | `STK-WORKSHOP` | `US-6.3`<br>`US-6.4` | `REQ-BUCKETS`<br>`REQ-API-HISTORY` |
| `NEED-LIFETIME-HOURS` | `STK-WORKSHOP` | `US-6.2`<br>`US-7.3` | `REQ-SCOPES`<br>`REQ-BASELINES` |
| `NEED-TRACEABLE-RULES` | `STK-ADMIN` | `US-7.4` | `REQ-GOAL-TRACE`<br>`REQ-COUNTER-SET`<br>`REQ-ACCUMULATE`<br>`REQ-BUCKETS` |
| `NEED-REPRODUCIBLE-DEMO` | `STK-ADMIN` | `US-7.1`<br>`US-7.2`<br>`US-7.3` | `REQ-SIM-SCHEDULE`<br>`REQ-SIM-DEMO`<br>`REQ-SIM-BURST`<br>`REQ-BASELINES` |
| `NEED-STABLE-INTERFACE` | `STK-ADMIN` | `US-7.5` | `REQ-TELEMETRY-SHAPE`<br>`REQ-API-COMMON` |

---

## 2. Requirements → implementation → verification

**Verification method** says how the requirement is checked:

- `Test` — automated unit test; the *Verified by* column names the test classes and how many tests in each start their `DisplayName` with the REQ ID.
- `Inspection` — review of code or documents against the requirement; §3 says what is inspected.
- `Demonstration` — shown on the running system (`RUNNING.md`); §3 says what is shown.

| Requirement | Priority | Implemented in | Verification method | Verified by |
|-------------|----------|----------------|---------------------|-------------|
| `REQ-GOAL-SCOPE` | Must | OperatorApi: AssetViewService<br>OperatorApi: HistoryService<br>frontend: index.html | Test | AssetViewServiceTest (2) |
| `REQ-GOAL-TRACE` | Must | TelemetryProcessor: AssetSnapshot | Inspection | §3 |
| `REQ-TELEMETRY-SHAPE` | Must | Simulator: SimulatorApplication<br>Simulator: TelemetryScheduler<br>TelemetryProcessor: AssetState<br>OperatorApi: AssetState<br>OperatorApi: TelemetryView<br>docker: database-script.sql | Test | TelemetryGeneratorTest (1) |
| `REQ-SIM-SCHEDULE` | Must | Simulator: AssetSimulationState<br>Simulator: ScheduleSegment<br>Simulator: SimulatedFleet<br>Simulator: TelemetryGenerator<br>Simulator: WorkingStatus | Test | TelemetryGeneratorTest (2) |
| `REQ-SIM-BURST` | Must | Simulator: application.properties | Demonstration | §3 |
| `REQ-SIM-DEMO` | Must | Simulator: application.properties | Test | TelemetryGeneratorTest (2) |
| `REQ-COUNTER-SET` | Must | TelemetryProcessor: AssetState<br>TelemetryProcessor: CounterAccumulationService<br>TelemetryProcessor: CounterSet<br>OperatorApi: AssetState<br>OperatorApi: CounterSetView<br>frontend: format.js<br>docker: database-script.sql | Test | CounterAccumulationServiceTest (4) |
| `REQ-ACCUMULATE` | Must | Simulator: BackfillService<br>Simulator: SimulatedFleet<br>TelemetryProcessor: CounterAccumulationService | Test | CounterAccumulationServiceTest (4) |
| `REQ-BUCKETS` | Must | TelemetryProcessor: AssetAccumulator<br>TelemetryProcessor: CounterSet<br>OperatorApi: HistoryRow<br>frontend: format.js | Test | AssetStatePublisherTest (1) |
| `REQ-SCOPES` | Must | TelemetryProcessor: AssetState<br>TelemetryProcessor: AssetStateRefreshScheduler<br>TelemetryProcessor: CounterAccumulationService<br>OperatorApi: AssetState<br>OperatorApi: FlagScope<br>docker: database-script.sql | Test | CounterAccumulationServiceTest (2) |
| `REQ-BASELINES` | Must | TelemetryProcessor: AssetAccumulator<br>TelemetryProcessor: AssetBaselines<br>TelemetryProcessor: AssetStatePublisher<br>TelemetryProcessor: CounterAccumulationService<br>TelemetryProcessor: CounterSet<br>docker: database-script.sql | Test | AssetStatePublisherTest (1) |
| `REQ-UTILIZATION` | Must | TelemetryProcessor: AssetState<br>TelemetryProcessor: AssetStateRefreshScheduler<br>TelemetryProcessor: CounterAccumulationService<br>OperatorApi: AssetState<br>OperatorApi: AssetView<br>frontend: format.js<br>docker: database-script.sql | Test | CounterAccumulationServiceTest (2) |
| `REQ-FLAGS` | Must | TelemetryProcessor: AssetState<br>TelemetryProcessor: FlagEvaluation<br>TelemetryProcessor: FlagEvaluationService<br>OperatorApi: AssetState<br>OperatorApi: Flag<br>OperatorApi: FlagView<br>docker: database-script.sql | Test | FlagEvaluationServiceTest (6)<br>AssetStatePublisherTest (1)<br>AssetViewServiceTest (2) |
| `REQ-STATUS` | Must | TelemetryProcessor: AssetAssessmentService<br>TelemetryProcessor: AssetState<br>TelemetryProcessor: AssetStatus<br>TelemetryProcessor: FlagEvaluation<br>OperatorApi: AssetState<br>OperatorApi: AssetStatus<br>docker: database-script.sql | Test | AssetAssessmentServiceTest (4) |
| `REQ-LIVE` | Must | TelemetryProcessor: AssetAssessmentService<br>TelemetryProcessor: AssetState<br>TelemetryProcessor: AssetStateRefreshScheduler<br>TelemetryProcessor: LiveCondition<br>OperatorApi: AssetState<br>OperatorApi: LiveCondition<br>frontend: app.css<br>frontend: format.js<br>docker: database-script.sql | Test | AssetAssessmentServiceTest (8) |
| `REQ-RANK` | Must | TelemetryProcessor: AssetState<br>TelemetryProcessor: AssetStatePublisher<br>TelemetryProcessor: FlagEvaluation<br>TelemetryProcessor: FleetRankingService<br>OperatorApi: AssetState<br>frontend: fleet.js<br>docker: database-script.sql | Test | FleetRankingServiceTest (4)<br>AssetStatePublisherTest (1) |
| `REQ-RECOMMEND` | Must | TelemetryProcessor: AssetAssessmentService<br>TelemetryProcessor: AssetState<br>OperatorApi: AssetState<br>docker: database-script.sql | Test | AssetAssessmentServiceTest (1) |
| `REQ-API-COMMON` | Must | OperatorApi: application.properties<br>frontend: api.js<br>frontend: format.js | Inspection | §3 |
| `REQ-API-ASSETS` | Must | OperatorApi: AssetController<br>OperatorApi: AssetStateRepository<br>OperatorApi: AssetView<br>OperatorApi: AssetViewService<br>OperatorApi: FleetResponse<br>frontend: api.js | Test | AssetViewServiceTest (3) |
| `REQ-API-HISTORY` | Must | OperatorApi: HistoryController<br>OperatorApi: HistoryResponse<br>OperatorApi: HistoryRow<br>OperatorApi: HistoryService<br>OperatorApi: HourlyAggregateRepository<br>OperatorApi: application.properties<br>frontend: history.js | Test | HistoryServiceTest (8) |
| `REQ-API-LATEST` | Must | TelemetryProcessor: AssetStatePublisher<br>OperatorApi: AssetViewService<br>OperatorApi: LatestTelemetryEntry<br>OperatorApi: LatestTelemetryResponse<br>OperatorApi: TelemetryController<br>OperatorApi: TelemetryView<br>frontend: telemetry.js | Test | AssetViewServiceTest (3) |
| `REQ-DECISIONS` | Must | frontend: asset.js | Demonstration | §3 |

---

## 3. Coverage

- All 22 REQs have a verification method. 18 are verified by unit tests; 4 by inspection or demonstration.
- Every `Must` REQ whose rule can be checked in a unit test has at least one test.
- Inspections and demonstrations:

| Requirement | Method | What is checked |
|-------------|--------|-----------------|
| `REQ-GOAL-TRACE` | Inspection | Every value in the `SPEC.md` §6 responses is a counter, a quotient of counters (§4.5) or a value derived from counters by a §5 rule. No module contains a score, weight or model. |
| `REQ-API-COMMON` | Inspection | The controllers map only `GET` endpoints, there is no security configuration, and errors use `ResponseStatusException` with Spring's default body. |
| `REQ-SIM-BURST` | Demonstration | On startup the Simulator logs `Backfill: published 5764 messages covering 24 h` (one per asset per minute, ends included) and then starts the live cadence; the dashboard shows a full rolling window right away. |
| `REQ-DECISIONS` | Demonstration | The dashboard shows, for each machine, the inputs of the three decisions and nothing else: rank, status, flags with reasons and recommendation; live condition and 24 h flags; the two utilization percentages. |

- Tests labelled `ARCHITECTURE §4` verify `ARCHITECTURE.md` rules and are not listed above.
