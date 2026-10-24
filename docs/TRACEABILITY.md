# docs\TRACEABILITY.md

This document is a derived view, built by hand from the *Source* attributes in `SPEC.md`, the `REQ-` comments in the code and the `@DisplayName` of the unit tests. Where it disagrees with those, they win. Update it in the same change as any of them.

---

## 1. Stakeholder needs → requirements

| Need | Stakeholder | Requirements |
|------|-------------|--------------|
| `NEED-MONITOR-FLEET` | `STK-FARMER` | `REQ-GOAL-SCOPE`<br>`REQ-FLEET-FIXED` |
| `NEED-PRIORITISE-MAINTENANCE` | `STK-FARMER` | `REQ-FLAGS`<br>`REQ-STATUS`<br>`REQ-RANK`<br>`REQ-RECOMMEND`<br>`REQ-API-ASSETS`<br>`REQ-DECISIONS` |
| `NEED-OPERATIONAL-RISK` | `STK-FARMER` | `REQ-SCOPES`<br>`REQ-LIVE`<br>`REQ-API-LATEST`<br>`REQ-DECISIONS` |
| `NEED-UTILIZATION-AWARENESS` | `STK-FARMER` | `REQ-TELEMETRY-WORKING-STATUS`<br>`REQ-TELEMETRY-PRODUCTIVE`<br>`REQ-COUNTER-SET`<br>`REQ-UTILIZATION`<br>`REQ-DECISIONS` |
| `NEED-FLAG-REASON` | `STK-WORKSHOP` | `REQ-GOAL-TRACE`<br>`REQ-FLAGS`<br>`REQ-API-ASSET` |
| `NEED-HISTORY-REVIEW` | `STK-WORKSHOP` | `REQ-BUCKETS`<br>`REQ-API-HISTORY` |
| `NEED-LIFETIME-HOURS` | `STK-WORKSHOP` | `REQ-SCOPES`<br>`REQ-BASELINES` |
| `NEED-TRACEABLE-RULES` | `STK-DEVELOPER` | `REQ-GOAL-TRACE`<br>`REQ-COUNTER-SET`<br>`REQ-COUNTER-INVARIANT`<br>`REQ-ACCUMULATE`<br>`REQ-BUCKET-EXACT` |
| `NEED-REPRODUCIBLE-DEMO` | `STK-DEVELOPER` | `REQ-SIM-ONLY-SOURCE`<br>`REQ-SIM-CYCLE`<br>`REQ-SIM-DEMO`<br>`REQ-SIM-TEMPERATURE`<br>`REQ-SIM-VIBRATION`<br>`REQ-SIM-EXCURSION-RATE`<br>`REQ-SIM-ELEVATED`<br>`REQ-SIM-BURST`<br>`REQ-SIM-MEMORY`<br>`REQ-BASELINES` |
| `NEED-STABLE-INTERFACE` | `STK-DEVELOPER` | `REQ-TELEMETRY-CADENCE`<br>`REQ-TELEMETRY-SHAPE`<br>`REQ-API-COMMON` |

---

## 2. Requirements → implementation → verification

| Requirement | Priority | Implemented in | Verified by |
|-------------|----------|----------------|-------------|
| `REQ-GOAL-SCOPE` | Must | — | — |
| `REQ-GOAL-TRACE` | Must | TelemetryProcessor: AssetSnapshot | — |
| `REQ-FLEET-FIXED` | Must | OperatorApi: AssetViewService<br>OperatorApi: HistoryService<br>frontend: index.html | — |
| `REQ-TELEMETRY-CADENCE` | Should | Simulator: TelemetryScheduler | — |
| `REQ-TELEMETRY-SHAPE` | Must | Simulator: SimulatorApplication<br>TelemetryProcessor: AssetState<br>OperatorApi: AssetState<br>OperatorApi: TelemetryView<br>docker: database-script.sql | TelemetryGeneratorTest (1) |
| `REQ-TELEMETRY-WORKING-STATUS` | Should | Simulator: TelemetryGenerator | TelemetryGeneratorTest (1) |
| `REQ-TELEMETRY-PRODUCTIVE` | Must | TelemetryProcessor: CounterAccumulationService | CounterAccumulationServiceTest (1) |
| `REQ-SIM-ONLY-SOURCE` | Must | — | — |
| `REQ-SIM-CYCLE` | Must | Simulator: TelemetryGenerator<br>Simulator: application.properties | — |
| `REQ-SIM-DEMO` | Could | Simulator: application.properties | — |
| `REQ-SIM-TEMPERATURE` | Should | Simulator: TelemetryGenerator | — |
| `REQ-SIM-VIBRATION` | Should | Simulator: TelemetryGenerator | — |
| `REQ-SIM-EXCURSION-RATE` | Should | Simulator: TelemetryGenerator | — |
| `REQ-SIM-ELEVATED` | Should | — | — |
| `REQ-SIM-BURST` | Could | Simulator: application.properties | — |
| `REQ-SIM-MEMORY` | Should | Simulator: AssetSimulationState<br>Simulator: SimulatedFleet | — |
| `REQ-COUNTER-SET` | Must | Simulator: TelemetryGenerator<br>TelemetryProcessor: AssetState<br>TelemetryProcessor: CounterAccumulationService<br>TelemetryProcessor: CounterSet<br>OperatorApi: AssetState<br>OperatorApi: CounterSetView<br>frontend: format.js<br>docker: database-script.sql | — |
| `REQ-COUNTER-INVARIANT` | Must | — | CounterAccumulationServiceTest (1) |
| `REQ-ACCUMULATE` | Must | Simulator: BackfillService<br>Simulator: SimulatedFleet<br>TelemetryProcessor: CounterAccumulationService | CounterAccumulationServiceTest (4) |
| `REQ-BUCKETS` | Must | TelemetryProcessor: AssetAccumulator<br>TelemetryProcessor: CounterSet<br>OperatorApi: HistoryRow<br>frontend: format.js | AssetStatePublisherTest (1) |
| `REQ-SCOPES` | Must | TelemetryProcessor: AssetState<br>TelemetryProcessor: AssetStateRefreshScheduler<br>TelemetryProcessor: CounterAccumulationService<br>OperatorApi: AssetState<br>OperatorApi: FlagScope<br>docker: database-script.sql | — |
| `REQ-BUCKET-EXACT` | Should | — | — |
| `REQ-BASELINES` | Could | TelemetryProcessor: AssetAccumulator<br>TelemetryProcessor: AssetBaselines<br>TelemetryProcessor: AssetStatePublisher<br>TelemetryProcessor: CounterAccumulationService<br>TelemetryProcessor: CounterSet<br>docker: database-script.sql | AssetStatePublisherTest (1) |
| `REQ-UTILIZATION` | Must | TelemetryProcessor: AssetState<br>TelemetryProcessor: AssetStateRefreshScheduler<br>TelemetryProcessor: CounterAccumulationService<br>OperatorApi: AssetState<br>OperatorApi: AssetView<br>frontend: format.js<br>docker: database-script.sql | CounterAccumulationServiceTest (2) |
| `REQ-FLAGS` | Must | TelemetryProcessor: AssetState<br>TelemetryProcessor: FlagEvaluation<br>TelemetryProcessor: FlagEvaluationService<br>TelemetryProcessor: FlagSeverity<br>OperatorApi: AssetState<br>OperatorApi: Flag<br>OperatorApi: FlagSeverity<br>OperatorApi: FlagView<br>docker: database-script.sql | FlagEvaluationServiceTest (6)<br>AssetViewServiceTest (2) |
| `REQ-STATUS` | Must | TelemetryProcessor: AssetAssessmentService<br>TelemetryProcessor: AssetState<br>TelemetryProcessor: AssetStatus<br>TelemetryProcessor: FlagEvaluation<br>OperatorApi: AssetState<br>OperatorApi: AssetStatus<br>docker: database-script.sql | AssetAssessmentServiceTest (4) |
| `REQ-LIVE` | Must | TelemetryProcessor: AssetAssessmentService<br>TelemetryProcessor: AssetState<br>TelemetryProcessor: AssetStateRefreshScheduler<br>TelemetryProcessor: LiveCondition<br>OperatorApi: AssetState<br>OperatorApi: LiveCondition<br>frontend: app.css<br>frontend: format.js<br>docker: database-script.sql | AssetAssessmentServiceTest (8) |
| `REQ-RANK` | Must | TelemetryProcessor: AssetState<br>TelemetryProcessor: AssetStatePublisher<br>TelemetryProcessor: FlagEvaluation<br>TelemetryProcessor: FleetRankingService<br>OperatorApi: AssetState<br>frontend: fleet.js<br>docker: database-script.sql | AssetStatePublisherTest (1)<br>FleetRankingServiceTest (5) |
| `REQ-RECOMMEND` | Must | TelemetryProcessor: AssetAssessmentService<br>TelemetryProcessor: AssetState<br>OperatorApi: AssetState<br>docker: database-script.sql | AssetAssessmentServiceTest (1) |
| `REQ-API-COMMON` | Must | OperatorApi: application.properties<br>frontend: api.js<br>frontend: format.js | — |
| `REQ-API-ASSETS` | Must | OperatorApi: AssetController<br>OperatorApi: AssetStateRepository<br>OperatorApi: AssetView<br>OperatorApi: AssetViewService<br>OperatorApi: FleetResponse | AssetViewServiceTest (2) |
| `REQ-API-ASSET` | Should | OperatorApi: AssetController<br>OperatorApi: AssetView<br>OperatorApi: AssetViewService<br>frontend: api.js | AssetViewServiceTest (2) |
| `REQ-API-HISTORY` | Should | OperatorApi: HistoryController<br>OperatorApi: HistoryResponse<br>OperatorApi: HistoryRow<br>OperatorApi: HistoryService<br>OperatorApi: HourlyAggregateRepository<br>OperatorApi: application.properties<br>frontend: history.js | HistoryServiceTest (8) |
| `REQ-API-LATEST` | Should | TelemetryProcessor: AssetStatePublisher<br>OperatorApi: AssetViewService<br>OperatorApi: LatestTelemetryEntry<br>OperatorApi: LatestTelemetryResponse<br>OperatorApi: TelemetryController<br>OperatorApi: TelemetryView<br>frontend: telemetry.js | AssetViewServiceTest (3) |
| `REQ-DECISIONS` | Must | frontend: asset.js | — |

---

## 3. Coverage

- 17 of 35 REQs have at least one test.
- `Must` REQs with no test: `REQ-GOAL-SCOPE`, `REQ-GOAL-TRACE`, `REQ-FLEET-FIXED`, `REQ-SIM-ONLY-SOURCE`, `REQ-SIM-CYCLE`, `REQ-COUNTER-SET`, `REQ-SCOPES`, `REQ-API-COMMON`, `REQ-DECISIONS`.
- Tests labelled `ARCHITECTURE §4` verify `ARCHITECTURE.md` rules and are not listed above.
