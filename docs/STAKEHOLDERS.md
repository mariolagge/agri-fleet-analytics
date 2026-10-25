# docs\STAKEHOLDERS.md

This document owns the stakeholders and their needs. Every requirement in `SPEC.md` names the needs it serves in its *Source* attribute.

---

## 1. Stakeholders

| ID | Stakeholder | Relationship to the system | Main interest |
|----|-------------|----------------------------|---------------|
| `STK-FARMER` | Farmer (owner and manager of the farm) | Primary user; checks the dashboard every morning | Decide which machine to service and how hard to work each machine |
| `STK-WORKSHOP` | Independent workshop technician | Indirect; never uses the system, receives flag reasons from the farmer | Understand why a machine was brought in |
| `STK-ADMIN` | System administrator | Installs, starts and configures the system | Rules that are traceable, testable and reproducible |

---

## 2. Stakeholder needs

| ID | Stakeholder | Need |
|----|-------------|------|
| `NEED-MONITOR-FLEET` | `STK-FARMER` | The farmer needs to see the condition of all four machines of the farm in one place. |
| `NEED-PRIORITISE-MAINTENANCE` | `STK-FARMER` | The farmer needs to know which machine to look at first and how urgently. |
| `NEED-OPERATIONAL-RISK` | `STK-FARMER` | The farmer needs to know whether it is advisable to keep working a machine hard today. |
| `NEED-UTILIZATION-AWARENESS` | `STK-FARMER` | The farmer needs to see which machines do productive work and which mostly idle or only transport. |
| `NEED-FLAG-REASON` | `STK-WORKSHOP` | The workshop technician needs the measured reason behind a warning — which counter, its value and the threshold it crossed — not only that the machine is critical. |
| `NEED-HISTORY-REVIEW` | `STK-WORKSHOP` | The workshop technician needs to review how a machine was used hour by hour over the past days. |
| `NEED-LIFETIME-HOURS` | `STK-WORKSHOP` | The workshop technician needs the accumulated lifetime hours of a machine, including its hours under load. |
| `NEED-TRACEABLE-RULES` | `STK-ADMIN` | The admin needs every number the system shows to be traceable to counters and telemetry, with no hidden scoring. |
| `NEED-REPRODUCIBLE-DEMO` | `STK-ADMIN` | The admin needs every run to start from the same known state and to show every status within one demo session. |
| `NEED-STABLE-INTERFACE` | `STK-ADMIN` | The admin needs a fixed telemetry message format and a documented, read-only API contract. |

---

## 3. Elicitation

The needs were derived from the author's own experience of how machine maintenance is planned on a farm; that as-is process is modeled in `PROCESS.md`. They have not been validated in interviews with other farmers or workshops.
