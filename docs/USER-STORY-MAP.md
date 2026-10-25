# docs\USER-STORY-MAP.md

This document owns the epics (`E…` IDs), the user stories (`US-…` IDs) and their release slices. Where a story and `SPEC.md` disagree, `SPEC.md` wins.

---

## 1. How to read this map

- **Personas** are the stakeholders in `STAKEHOLDERS.md` §1: the farmer (`STK-FARMER`), the workshop technician (`STK-WORKSHOP`) and the system administrator (`STK-ADMIN`). The workshop never uses the system; its stories describe what it receives through the farmer.
- **Epics.** Epics E1–E6 are the farmer's activities, taken from the to-be BPMN model in `PROCESS.md` §3, in the order the farmer performs them. Epic E7 holds the system administrator's stories: they make the other epics possible but are not steps of the farmer's process. Each epic names its BPMN element and the decision in REQ-DECISIONS it supports.
- **Numbering.** A story's ID is `US-<epic>.<story>`, so `US-2.1` is the first story of epic E2. Within an epic, stories are ordered by slice (Must, then Should, then Could) and then by process order.
- **Slices.** A story's slice is the MoSCoW priority of its requirements in `SPEC.md`. All requirements of one story share the same priority. *Must* is the smallest release that supports all three decisions in REQ-DECISIONS. *Should* makes the results correct and robust. *Could* is demo tuning only. *Won't* marks deliberate scope boundaries, not planned work (§4).
- **Story points** are relative estimates on the Fibonacci scale 1, 2, 3, 5, 8. They were made in retrospect from the implemented scope and compare stories with each other; they are not hours.
- **Traceability.** Each story names the stakeholder needs it serves (*Source*) and the requirements that realise it. Every need it names is a *Source* of at least one of those requirements. Code and tests are reached through `TRACEABILITY.md`.
- **Acceptance.** A story is accepted through its requirements: the *shall* statements in `SPEC.md` are the acceptance criteria, and `TRACEABILITY.md` §2 names the test or other method that verifies each one.

---

## 2. Slice summary

| Slice | Stories | Points |
|-------|---------|--------|
| Must | 14 | 50 |
| Should | 2 | 6 |
| Could | 2 | 7 |
| **Total** | **18** | **63** |

---

## 3. Epics and user stories

### E1 Check the fleet

The farmer sees the state of the whole fleet in one place every morning.

To-be BPMN: *Check fleet ranking and live telemetry strip* · Decision: 1 Maintenance prioritisation

**US-1.1** As a farmer, I want to see all machines on one screen, so that I do not have to check each machine on its own.

- Slice: Must · Points: 3
- Source: NEED-MONITOR-FLEET, NEED-PRIORITISE-MAINTENANCE
- Requirements: REQ-GOAL-SCOPE, REQ-API-ASSETS

**US-1.2** As a farmer, I want a live strip with each machine's newest reading, so that I see at a glance which machine is under strain right now.

- Slice: Should · Points: 3
- Source: NEED-OPERATIONAL-RISK
- Requirements: REQ-API-LATEST

### E2 Prioritise by status

The farmer knows which machine needs attention first and what kind of attention it needs.

To-be BPMN: *Asset status?*, *Plan inspection within the next 15–20 engine hours* · Decision: 1 Maintenance prioritisation

**US-2.1** As a farmer, I want the fleet ranked by urgency, so that I look at the most urgent machine first.

- Slice: Must · Points: 3
- Source: NEED-PRIORITISE-MAINTENANCE
- Requirements: REQ-RANK, REQ-DECISIONS

**US-2.2** As a farmer, I want a status and a service recommendation for each machine, so that I know whether to act now, plan an inspection or do nothing.

- Slice: Must · Points: 2
- Source: NEED-PRIORITISE-MAINTENANCE
- Requirements: REQ-STATUS, REQ-RECOMMEND

### E3 Inspect asset detail

The farmer understands why a machine is flagged and can decide who repairs it.

To-be BPMN: *Open asset detail: flags with reasons, live condition* · Decision: 1 Maintenance prioritisation

**US-3.1** As a farmer, I want to open one machine on its own, so that I can read its flags, counters and live condition before deciding who repairs it.

- Slice: Must · Points: 2
- Source: NEED-FLAG-REASON
- Requirements: REQ-API-ASSETS

**US-3.2** As a farmer, I want every flag to show the counter, its value and the threshold, so that I can trust the warning and decide whether I can fix the problem myself.

- Slice: Must · Points: 5
- Source: NEED-PRIORITISE-MAINTENANCE, NEED-FLAG-REASON
- Requirements: REQ-FLAGS, REQ-GOAL-TRACE

### E4 Hand over to the workshop

The workshop technician receives the facts behind a repair without relying on the farmer's memory.

To-be BPMN: *Send flag reasons to workshop* · Decision: 1 Maintenance prioritisation

**US-4.1** As a workshop technician, I want to receive the counter, value and threshold behind each flag, so that I know why the machine was brought in without relying on a verbal description.

- Slice: Must · Points: 1
- Source: NEED-FLAG-REASON
- Requirements: REQ-FLAGS, REQ-GOAL-TRACE

**US-4.2** As a workshop technician, I want the lifetime hours of a machine, including its hours under heavy load, so that I can judge its wear.

- Slice: Must · Points: 2
- Source: NEED-LIFETIME-HOURS
- Requirements: REQ-SCOPES

**US-4.3** As a workshop technician, I want a machine's usage recorded automatically hour by hour, so that I can see how it was used without depending on the farmer's memory.

- Slice: Must · Points: 5
- Source: NEED-HISTORY-REVIEW
- Requirements: REQ-BUCKETS

**US-4.4** As a workshop technician, I want to review a machine's usage hour by hour over the past days, so that I understand how it was worked before it came in.

- Slice: Should · Points: 3
- Source: NEED-HISTORY-REVIEW
- Requirements: REQ-API-HISTORY

### E5 Plan machine use

The farmer decides which machines to work today and how hard.

To-be BPMN: *Plan machine use from live condition and 24 h flags* · Decision: 2 Short-term operational risk

**US-5.1** As a farmer, I want each machine's condition from its latest reading, so that I stop working a machine that is under strain right now.

- Slice: Must · Points: 3
- Source: NEED-OPERATIONAL-RISK
- Requirements: REQ-LIVE

**US-5.2** As a farmer, I want to see the sustained load of the last 24 hours, so that I can decide whether to keep working a machine hard today.

- Slice: Must · Points: 3
- Source: NEED-OPERATIONAL-RISK
- Requirements: REQ-SCOPES, REQ-DECISIONS

### E6 Review utilization

The farmer knows how the fleet is used over the longer term.

To-be BPMN: annotation *Usage is recorded automatically (counters, hourly history)*; not a task of the daily process · Decision: 3 Basic utilization awareness

**US-6.1** As a farmer, I want to see which machines do productive work and which mostly run without working, so that I can plan how the fleet is used.

- Slice: Must · Points: 5
- Source: NEED-UTILIZATION-AWARENESS
- Requirements: REQ-COUNTER-SET, REQ-UTILIZATION, REQ-DECISIONS

### E7 Run and trace the system

The system administrator can run, demonstrate and explain the system without real machines.

To-be BPMN: none (the system administrator has no runtime interface) · Decision: none (enables E1–E6)

**US-7.1** As a system administrator, I want every number on the dashboard to come from a counter accrued by one fixed, exact rule, so that I can explain and check any result by hand.

- Slice: Must · Points: 8
- Source: NEED-TRACEABLE-RULES
- Requirements: REQ-GOAL-TRACE, REQ-COUNTER-SET, REQ-ACCUMULATE, REQ-BUCKETS

**US-7.2** As a system administrator, I want a simulator that is the only telemetry source and repeats a fixed schedule per machine, so that the system can be run and shown without real machines.

- Slice: Must · Points: 5
- Source: NEED-REPRODUCIBLE-DEMO
- Requirements: REQ-SIM-SCHEDULE

**US-7.3** As a system administrator, I want a fixed telemetry message format and a read-only REST API, so that the modules and the frontend can be changed without breaking each other.

- Slice: Must · Points: 3
- Source: NEED-STABLE-INTERFACE
- Requirements: REQ-TELEMETRY-SHAPE, REQ-API-COMMON

**US-7.4** As a system administrator, I want every run to start from a fixed service history per machine, so that the lifetime flag is reachable in a demo and every run starts the same way.

- Slice: Could · Points: 2
- Source: NEED-LIFETIME-HOURS, NEED-REPRODUCIBLE-DEMO
- Requirements: REQ-BASELINES

**US-7.5** As a system administrator, I want the simulator to fill the last 24 hours at startup and land each machine on a known status, so that one demo session shows GOOD, WATCH and CRITICAL.

- Slice: Could · Points: 5
- Source: NEED-REPRODUCIBLE-DEMO
- Requirements: REQ-SIM-DEMO, REQ-SIM-BURST

---

## 4. Won't have (scope boundary)

These are deliberate limits of the system, not planned work.

| Won't have | Epic | Why | Source |
|------------|------|-----|--------|
| Notifications | E1 | The farmer checks the dashboard every morning; the system sends nothing. | `PROCESS.md` §4 |
| Manufacturer service intervals | E2 | The hour-meter check of the as-is process continues unchanged. | `PROCESS.md` §4 |
| Workshop access | E4 | The workshop never uses the system and gets the flag reasons only through the farmer. | `PROCESS.md` §4 |
| Maintenance log | E6 | Usage is recorded; performed maintenance is not. | `PROCESS.md` §4 |
| Asset registration | E7 | The fleet is fixed to four machines. | REQ-GOAL-SCOPE |
| Write access, authentication | E7 | The API is read-only and open. | REQ-API-COMMON |

---

## 5. Definition of Done

A story is done when all of the following are true for each of its requirements:

1. The code that implements the requirement cites its `REQ-` ID in a comment.
2. The requirement has a row in `TRACEABILITY.md` §2, and the story appears in `TRACEABILITY.md` §1.
3. The requirement is verified by the method named in `TRACEABILITY.md` §2: every unit test whose `@DisplayName` starts with the `REQ-` ID passes, and an inspection or demonstration has been carried out.
4. The story can be shown on the running system (`RUNNING.md`).
5. Every new identifier is named in `GLOSSARY.md`.
