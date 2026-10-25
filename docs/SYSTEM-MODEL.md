# docs\SYSTEM-MODEL.md

This document holds SysML-style views of the system, drawn in Mermaid so that they render on GitHub. Mermaid is not a SysML tool: the views borrow SysML diagram kinds and notation but are not a SysML model. They are views of `SPEC.md` and `ARCHITECTURE.md`; where a view and those documents disagree, the documents win.

| SysML diagram | Mermaid diagram type | Section |
|---------------|----------------------|---------|
| System context | `flowchart` | §1 |
| Block definition diagram | `classDiagram` | §2 |
| Internal block diagram | `flowchart` | §3 |
| State machine diagram | `stateDiagram-v2` | §4 |
| Requirement diagram | `requirementDiagram` | §5 |

---

## 1. System context

The system of interest, the machines that feed it telemetry, and the stakeholders who receive its results.

```mermaid
flowchart LR
    farmer["Farmer<br/>(STK-FARMER)"]
    workshop["Independent workshop technician<br/>(STK-WORKSHOP)"]
    machines["Tractor, mower, baler, feed mixer<br/>(telematics simulated by the Simulator module)"]
    system["AgriFleet Analytics<br/>(system of interest)"]
    machines -- "telemetry messages" --> system
    farmer -- "opens dashboard" --> system
    system -- "fleet ranking, flags with reasons,<br/>live condition, hourly history" --> farmer
    farmer -. "flag reasons (counter, value, threshold)" .-> workshop
```

The admin (`STK-ADMIN`) has no runtime interface and is therefore not shown.

Source: `SPEC.md` §1–§2, `STAKEHOLDERS.md` §1.

---

## 2. Block definition diagram

The blocks the system is composed of and the telemetry signal that passes between them.

```mermaid
classDiagram
    class AgriFleetAnalytics {
        <<block>>
    }
    class Simulator {
        <<block>>
    }
    class TelemetryProcessor {
        <<block>>
    }
    class OperatorApi {
        <<block>>
    }
    class Frontend {
        <<block>>
    }
    class WebServer {
        <<block>>
    }
    class MessageBroker {
        <<block>>
    }
    class Database {
        <<block>>
    }
    class TelemetryMessage {
        <<signal>>
    }
    AgriFleetAnalytics *-- Simulator
    AgriFleetAnalytics *-- TelemetryProcessor
    AgriFleetAnalytics *-- OperatorApi
    AgriFleetAnalytics *-- Frontend
    AgriFleetAnalytics *-- WebServer
    AgriFleetAnalytics *-- MessageBroker
    AgriFleetAnalytics *-- Database
    Simulator ..> TelemetryMessage : sends
    TelemetryProcessor ..> TelemetryMessage : receives
    note for MessageBroker "RabbitMQ, one durable queue: telemetry"
    note for Database "PostgreSQL, tables asset_state and hourly_aggregate"
    note for WebServer "nginx, serves Frontend and proxies /api/"
    note for OperatorApi "Port 8083, the only HTTP port"
```

Source: `ARCHITECTURE.md` §1–§3.

---

## 3. Internal block diagram

How the parts of the system are connected and what flows over each connection.

```mermaid
flowchart LR
    subgraph agrifleet["AgriFleet Analytics"]
        simulator[":Simulator"]
        broker[(":MessageBroker<br/>queue telemetry")]
        processor[":TelemetryProcessor"]
        database[(":Database<br/>asset_state, hourly_aggregate")]
        api[":OperatorApi<br/>port 8083"]
        web[":WebServer<br/>nginx + Frontend"]
    end
    browser["Farmer's browser"]
    simulator -- "TelemetryMessage" --> broker
    broker -- "TelemetryMessage" --> processor
    processor -- "upsert asset_state,<br/>insert hourly_aggregate" --> database
    database -- "read-only queries" --> api
    api -- "JSON /api/*" --> web
    web -- "HTML, CSS, JS and proxied /api/*" --> browser
```

Source: `ARCHITECTURE.md` §1, §3–§5.

---

## 4. State machine: simulator schedule (TRACTOR)

The fixed cycle the simulated TRACTOR repeats every 120 minutes. All transitions are time-triggered and deterministic; there are no probabilities.

```mermaid
stateDiagram-v2
    direction LR
    state "WORKING<br/>load 0.80, 80 °C" as WorkingHeavy
    state "WORKING<br/>load 0.80, 94 °C" as WorkingHot
    state "TRANSPORT<br/>20 km/h, 65 °C" as Transport
    state "IDLE<br/>0 km/h, 50 °C" as Idle
    [*] --> WorkingHeavy
    WorkingHeavy --> WorkingHot : after 60 min
    WorkingHot --> Transport : after 10 min
    Transport --> Idle : after 20 min
    Idle --> WorkingHeavy : after 30 min
    note right of WorkingHot
        temperatureC > 90 accrues tempExcursionHours
        and makes liveCondition CAUTION
    end note
```

Each of the four assets runs its own instance with its own segments and cycle length (`SPEC.md` §3.2). The machine stores no current state: `TelemetryGenerator` computes the segment from the message timestamp modulo the cycle length, so the startup burst and the live phase are always in the same state at the same instant. The initial transition therefore enters whichever segment contains the start time.

Source: REQ-SIM-SCHEDULE.

---

## 5. Requirement diagram: maintenance prioritisation slice

How one stakeholder need is refined into requirements, which classes satisfy them and which tests verify them.

Arrows follow SysML direction: `derives` points from the derived requirement to its source, `satisfies` from the implementing class to the requirement, `verifies` from the test to the requirement.

```mermaid
requirementDiagram

    requirement NEED_PRIORITISE_MAINTENANCE {
        id: "NEED-PRIORITISE-MAINTENANCE"
        text: "Know which machine to look at first and how urgently."
    }

    functionalRequirement REQ_FLAGS {
        id: "REQ-FLAGS"
        text: "Evaluate four flags, one threshold each, published with counter, scope, value and threshold."
    }

    functionalRequirement REQ_STATUS {
        id: "REQ-STATUS"
        text: "Derive GOOD, WATCH or CRITICAL from the number of active flags."
    }

    functionalRequirement REQ_RANK {
        id: "REQ-RANK"
        text: "Rank all four assets by active flag count, counter sum, assetId."
    }

    functionalRequirement REQ_RECOMMEND {
        id: "REQ-RECOMMEND"
        text: "Publish the fixed service sentence for the status."
    }

    element FlagEvaluationService {
        type: "Java class"
    }

    element AssetAssessmentService {
        type: "Java class"
    }

    element FleetRankingService {
        type: "Java class"
    }

    element FlagEvaluationServiceTest {
        type: "JUnit test"
    }

    element AssetAssessmentServiceTest {
        type: "JUnit test"
    }

    element FleetRankingServiceTest {
        type: "JUnit test"
    }

    REQ_FLAGS - derives -> NEED_PRIORITISE_MAINTENANCE
    REQ_STATUS - derives -> NEED_PRIORITISE_MAINTENANCE
    REQ_RANK - derives -> NEED_PRIORITISE_MAINTENANCE
    REQ_RECOMMEND - derives -> NEED_PRIORITISE_MAINTENANCE
    FlagEvaluationService - satisfies -> REQ_FLAGS
    AssetAssessmentService - satisfies -> REQ_STATUS
    AssetAssessmentService - satisfies -> REQ_RECOMMEND
    FleetRankingService - satisfies -> REQ_RANK
    FlagEvaluationServiceTest - verifies -> REQ_FLAGS
    AssetAssessmentServiceTest - verifies -> REQ_STATUS
    AssetAssessmentServiceTest - verifies -> REQ_RECOMMEND
    FleetRankingServiceTest - verifies -> REQ_RANK
```

This is one slice of the full trace in `TRACEABILITY.md`. REQ-FLAGS also serves NEED-FLAG-REASON, which this slice does not show.

Source: `STAKEHOLDERS.md`, REQ-FLAGS, REQ-STATUS, REQ-RANK, REQ-RECOMMEND, `TRACEABILITY.md`.
