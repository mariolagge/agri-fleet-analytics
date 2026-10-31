# docs\USER-STORY-MAP.md

This document owns the epics (`E…` IDs), the user stories (`US-…` IDs) and their release slices. Where a story and `SPEC.md` disagree, `SPEC.md` wins.

---

## 1. How to read this map

- **Personas** are the stakeholders in `STAKEHOLDERS.md` §1: the farmer (`STK-FARMER`) and the machinery repair shop technician (`STK-WORKSHOP`). The machinery repair shop never uses the system; its stories describe what it receives through the farmer.
- **Epics.** Epics E1–E5 are the activities of the to-be BPMN model in `PROCESS.md` §3. They are grouped by persona: the farmer (E1–E4), then the machinery repair shop technician (E5). Within each group, epics follow the order of the to-be process. Each epic names its BPMN element and the decision in REQ-DECISIONS it supports.
- **Numbering.** A story's ID is `US-<epic>.<story>`, so `US-2.1` is the first story of epic E2. Within an epic, stories are ordered by slice (Must, then Should, then Could) and then by the order of the to-be process.
- **Slices.** *Must* is the implemented release; it supports all three decisions in REQ-DECISIONS. A Must story's slice is the MoSCoW priority of its requirements in `SPEC.md`, and all requirements of one story share the same priority. *Should* and *Could* stories form the backlog: they are not implemented and have no requirements in `SPEC.md` yet, so their slice is set in this map. *Should* makes the system more useful in daily work, but every decision still works without it. *Could* is a nice-to-have. *Won't* marks deliberate scope boundaries, not planned work (§4).
- **Story points** are relative estimates on the Fibonacci scale 1, 2, 3, 5, 8. Must stories were estimated in retrospect from the implemented scope; backlog stories were estimated up front. Points compare stories with each other; they are not hours.
- **Traceability.** Each story names the stakeholder needs it serves (*Source*) and the requirements that realise it. For a Must story, every need it names is a *Source* of at least one of those requirements. A backlog story lists its requirements as *not yet specified* and is added to `TRACEABILITY.md` once they are written. Code and tests are reached through `TRACEABILITY.md`.
- **Acceptance.** A story is accepted through its requirements: the *shall* statements in `SPEC.md` are the acceptance criteria, and `TRACEABILITY.md` §2 names the test or other method that verifies each one.

---

## 2. Slice summary

| Slice | Stories | Points |
|-------|---------|--------|
| Must | 11 | 33 |
| Should | 4 | 9 |
| Could | 4 | 13 |
| **Total** | **19** | **55** |

---

## 3. Epics and user stories

### E1 Check the fleet

The farmer sees the state of the whole fleet in one place every morning.

To-be BPMN: *Check fleet status* · Decision: 1 Maintenance prioritisation

**US-1.1** As a farmer, I want to see all machines on one screen, so that I do not have to check each machine on its own.

- Slice: Must · Points: 3
- Source: NEED-MONITOR-FLEET, NEED-PRIORITISE-MAINTENANCE
- Requirements: REQ-GOAL-SCOPE, REQ-API-ASSETS

**US-1.2** As a farmer, I want a live strip with each machine's newest reading, so that I see at a glance which machine is under strain right now.

- Slice: Must · Points: 3
- Source: NEED-OPERATIONAL-RISK
- Requirements: REQ-API-LATEST

**US-1.3** As a farmer, I want to see when the dashboard last received new data, so that I know I am not planning from an old picture.

- Slice: Should · Points: 2
- Source: NEED-MONITOR-FLEET
- Requirements: not yet specified

**US-1.4** As a farmer, I want a high-contrast view, so that I can read the dashboard on a tablet in bright sunlight.

- Slice: Could · Points: 2
- Source: NEED-MONITOR-FLEET
- Requirements: not yet specified

### E2 Prioritise by status

The farmer knows which machine needs attention first and what kind of attention it needs.

To-be BPMN: *Asset status?*, *Schedule inspection* · Decision: 1 Maintenance prioritisation

**US-2.1** As a farmer, I want the fleet ranked by urgency, so that I look at the most urgent machine first.

- Slice: Must · Points: 3
- Source: NEED-PRIORITISE-MAINTENANCE
- Requirements: REQ-RANK, REQ-DECISIONS

**US-2.2** As a farmer, I want a status and a service recommendation for each machine, so that I know whether to act now, plan an inspection or do nothing.

- Slice: Must · Points: 2
- Source: NEED-PRIORITISE-MAINTENANCE
- Requirements: REQ-STATUS, REQ-RECOMMEND

**US-2.3** As a farmer, I want to see why a machine ranks above the next one, so that I can trust the order and not second-guess it.

- Slice: Should · Points: 2
- Source: NEED-PRIORITISE-MAINTENANCE
- Requirements: not yet specified

**US-2.4** As a farmer, I want to see how many engine hours are left in a WATCH machine's 15–20 hour inspection window, so that I can schedule the inspection before it runs out.

- Slice: Could · Points: 3
- Source: NEED-PRIORITISE-MAINTENANCE
- Requirements: not yet specified

### E3 Inspect asset detail

The farmer understands why a machine is flagged and can decide who repairs it.

To-be BPMN: *Review asset details* · Decision: 1 Maintenance prioritisation

**US-3.1** As a farmer, I want to open one machine on its own, so that I can read its flags, counters and live condition before deciding who repairs it.

- Slice: Must · Points: 2
- Source: NEED-FLAG-REASON
- Requirements: REQ-API-ASSETS

**US-3.2** As a farmer, I want every flag to show the counter, its value and the threshold, so that I can trust the warning and decide whether I can fix the problem myself.

- Slice: Must · Points: 5
- Source: NEED-PRIORITISE-MAINTENANCE, NEED-FLAG-REASON
- Requirements: REQ-FLAGS, REQ-GOAL-TRACE

**US-3.3** As a farmer, I want to see since when each flag has been active, so that I can tell a new warning from one I have ignored for days.

- Slice: Should · Points: 3
- Source: NEED-PRIORITISE-MAINTENANCE, NEED-FLAG-REASON
- Requirements: not yet specified

### E4 Plan machine use

The farmer decides which machines to work today and how hard.

To-be BPMN: *Plan machine use* · Decision: 2 Short-term operational risk

**US-4.1** As a farmer, I want each machine's condition from its latest reading, so that I stop working a machine that is under strain right now.

- Slice: Must · Points: 3
- Source: NEED-OPERATIONAL-RISK
- Requirements: REQ-LIVE

**US-4.2** As a farmer, I want to see the sustained load of the last 24 hours, so that I can decide whether to keep working a machine hard today.

- Slice: Must · Points: 3
- Source: NEED-OPERATIONAL-RISK
- Requirements: REQ-SCOPES, REQ-DECISIONS

**US-4.3** As a farmer, I want a chart of a machine's load over the past days, so that I can see whether the strain is rising or falling.

- Slice: Could · Points: 3
- Source: NEED-OPERATIONAL-RISK
- Requirements: not yet specified

### E5 Hand over to the machinery repair shop

The machinery repair shop technician receives the facts behind a repair without relying on the farmer's memory.

To-be BPMN: *Send flag reasons* · Decision: 1 Maintenance prioritisation

**US-5.1** As a machinery repair shop technician, I want to receive the counter, value and threshold behind each flag, so that I know why the machine was brought in without relying on a verbal description.

- Slice: Must · Points: 1
- Source: NEED-FLAG-REASON
- Requirements: REQ-FLAGS, REQ-GOAL-TRACE

**US-5.2** As a machinery repair shop technician, I want a machine's usage recorded automatically hour by hour, so that I can see how it was used without depending on the farmer's memory.

- Slice: Must · Points: 5
- Source: NEED-HISTORY-REVIEW
- Requirements: REQ-BUCKETS

**US-5.3** As a machinery repair shop technician, I want to review a machine's usage hour by hour over the past days, so that I understand how it was worked before it came in.

- Slice: Must · Points: 3
- Source: NEED-HISTORY-REVIEW
- Requirements: REQ-API-HISTORY

**US-5.4** As a machinery repair shop technician, I want a printable sheet with a machine's flags, values, thresholds and lifetime hours, so that I get the facts on paper when the machine arrives.

- Slice: Should · Points: 2
- Source: NEED-FLAG-REASON, NEED-LIFETIME-HOURS
- Requirements: not yet specified

**US-5.5** As a machinery repair shop technician, I want to see which flags were active over the past days, so that I can spot a problem that comes and goes.

- Slice: Could · Points: 5
- Source: NEED-HISTORY-REVIEW, NEED-FLAG-REASON
- Requirements: not yet specified

---

## 4. Won't have (scope boundary)

These are deliberate limits of the system, not planned work.

| Won't have | Epic | Why | Source |
|------------|------|-----|--------|
| Notifications | E1 | The farmer checks the dashboard every morning; the system sends nothing. | `PROCESS.md` §4 |
| Manufacturer service intervals | E2 | The hour-meter check of the as-is process continues unchanged. | `PROCESS.md` §4 |
| Maintenance log | — | Usage is recorded; performed maintenance is not. | `PROCESS.md` §4 |
| Machinery repair shop access | E5 | The machinery repair shop never uses the system and gets the flag reasons only through the farmer. | `PROCESS.md` §4 |
| Asset registration | — | The fleet is fixed to four machines. | REQ-GOAL-SCOPE |
| Write access, authentication | — | The API is read-only and open. | REQ-API-COMMON |

---

## 5. Definition of Done

A story is done when all of the following are true for each of its requirements:

1. The code that implements the requirement cites its `REQ-` ID in a comment.
2. The requirement has a row in `TRACEABILITY.md` §2, and the story appears in `TRACEABILITY.md` §1.
3. The requirement is verified by the method named in `TRACEABILITY.md` §2: every unit test whose `DisplayName` starts with the `REQ-` ID passes, and an inspection or demonstration has been carried out.
4. The story can be shown on the running system (`RUNNING.md`).
5. Every new identifier is named in `GLOSSARY.md`.
