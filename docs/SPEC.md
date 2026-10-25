# docs\SPEC.md

This document owns the product rules: requirements, domain logic, API contract and scope.

Each requirement (`REQ-…`) is followed by two attributes. **Priority** uses MoSCoW:

- `Must` — part of the implemented release.
- `Should` — not implemented; makes results correct or robust, but every decision still works without it.
- `Could` — not implemented; a nice-to-have.

**Source** lists the stakeholder needs (`NEED-…`, `STAKEHOLDERS.md` §2) the requirement serves.

---

## 1. Goal

**REQ-GOAL-SCOPE.** The system shall monitor utilization of exactly the four assets in §2 — one tractor and three typical Austrian hay/dairy-farm implements — and shall support the decisions in §7 and no others. The system shall not register or discover assets.

*Priority: Must · Source: NEED-MONITOR-FLEET*

**REQ-GOAL-TRACE.** Every number the system shows shall be traceable to a counter, and every counter shall be traceable to telemetry. The system shall not use hidden scoring, machine learning, or opaque weighting.

*Priority: Must · Source: NEED-TRACEABLE-RULES, NEED-FLAG-REASON*

---

## 2. Assets

| Asset | Role |
|-------|------|
| `TRACTOR` | Primary power unit; pulls or drives the three implements. |
| `MOWER` | Rotary/disc mower. |
| `BALER` | Round baler. |
| `MIXER` | Feed mixer wagon. |

---

## 3. Telemetry

### 3.1 Message

```json
{
  "assetId": "TRACTOR",
  "timestamp": "2026-07-31T01:15:00Z",
  "ptoEngaged": true,
  "groundSpeedKmh": 10.0,
  "loadFactor": 0.80,
  "temperatureC": 94.0,
  "vibrationLevel": "NORMAL"
}
```

| Field | Type | Constraint |
|-------|------|------------|
| `assetId` | string | One of the four §2 assets. |
| `timestamp` | string | ISO-8601 instant in UTC. |
| `ptoEngaged` | boolean | — |
| `groundSpeedKmh` | number | ≥ 0 |
| `loadFactor` | number | 0.0–1.0 |
| `temperatureC` | number | — |
| `vibrationLevel` | string | `NORMAL`, `ELEVATED` or `HIGH` |

**REQ-TELEMETRY-SHAPE.** A telemetry message shall have exactly the fields and constraints in the table above. Outside the §3.2 startup burst, each asset shall publish one message every 5–10 seconds, independently of the other assets.

*Priority: Must · Source: NEED-STABLE-INTERFACE*

### 3.2 Simulator behaviour

**REQ-SIM-SCHEDULE.** The simulator shall be the only source of telemetry. Each asset shall repeat the fixed schedule in the table below, without randomness. The position in the schedule shall be the message timestamp modulo the cycle length, counted from the Unix epoch, so the same instant always produces the same message. Field values shall be constant within a segment. Simulator state shall be in memory only.

*Priority: Must · Source: NEED-REPRODUCIBLE-DEMO*

Field values per state:

| State | `ptoEngaged` | `groundSpeedKmh` | `loadFactor` | `temperatureC` | `vibrationLevel` |
|-------|--------------|------------------|--------------|----------------|------------------|
| `WORKING` | `true` | `TRACTOR` 10, `MOWER` 11, `BALER` 8, `MIXER` 4 | per segment | 80 | `NORMAL` |
| `TRANSPORT` | `false` | 20 | 0.25 | 65 | `NORMAL` |
| `IDLE` | `false` | 0 | 0.10 | 50 | `NORMAL` |

Schedule per asset. A segment overrides a state value only where the table says so.

| Asset | Cycle | Segments in order |
|-------|-------|-------------------|
| `TRACTOR` | 120 min | `WORKING` 60 min, load 0.80 · `WORKING` 10 min, load 0.80, **94 °C** · `TRANSPORT` 20 min · `IDLE` 30 min |
| `BALER` | 120 min | `WORKING` 15 min, load 0.80 · `WORKING` 45 min, load 0.60 · `TRANSPORT` 20 min · `IDLE` 40 min |
| `MOWER` | 420 min | `WORKING` 180 min, load 0.60 · `WORKING` 18 min, load 0.60, **vibration `HIGH`** · `TRANSPORT` 42 min · `IDLE` 180 min |
| `MIXER` | 120 min | `WORKING` 40 min, load 0.50 · `IDLE` 80 min |

**REQ-SIM-DEMO.** Over a full rolling window, the schedule shall produce these outcomes: `TRACTOR` at `CRITICAL`, `BALER` at `WATCH` on `HEAVY_RECENT_USE` alone, `MIXER` at `GOOD`, and `MOWER` moving between `GOOD` and `WATCH` as `MECHANICAL_ATTENTION` comes and goes.

*Priority: Must · Source: NEED-REPRODUCIBLE-DEMO*

Why the schedule lands on these outcomes, in a window of 23–24 h (§4.3):

| Asset | `highLoadHours` | `tempExcursionHours` | `vibrationAlertHours` | Active flags |
|-------|-----------------|----------------------|------------------------|--------------|
| `TRACTOR` | ≈ 13–14 h | ≈ 1.8–2 h | 0 | `HEAVY_RECENT_USE`, `THERMAL_LOAD`, `HIGH_CUMULATIVE_LOAD` |
| `BALER` | ≈ 2.8–3 h | 0 | 0 | `HEAVY_RECENT_USE` |
| `MOWER` | 0 | 0 | 0.9–1.2 h | `MECHANICAL_ATTENTION` part of the time |
| `MIXER` | 0 | 0 | 0 | none |

`MOWER` moves because its 420-minute cycle does not divide 24 hours: the window holds three or four of its 18-minute vibration segments depending on the time of day, which is 0.9 h or 1.2 h around the 1.0 h threshold.

**REQ-SIM-BURST.** Each asset shall open its run by publishing one message per simulated minute across a configurable window of the immediately preceding hours, ending at the present, and only then switch to the §3.1 cadence. The step shall be exactly 60 seconds, which §4.2 credits, so the burst can fill the window in the time it takes to publish. Timestamps shall be true wall-clock instants. The window shall default to 24 hours.

*Priority: Must · Source: NEED-REPRODUCIBLE-DEMO*

---

## 4. Counters

### 4.1 Counter set

**REQ-COUNTER-SET.** Each asset shall maintain the five hour-counters in the table below, and shall accrue each counter only on the condition in that table. Productive work shall mean `ptoEngaged` **and** `groundSpeedKmh > 2`. As a consequence, `engineHours` ≥ `totalWorkingHours` ≥ `highLoadHours` shall always hold.

*Priority: Must · Source: NEED-UTILIZATION-AWARENESS, NEED-TRACEABLE-RULES*

Temperature and vibration stay ungated because an idle machine can still overheat or vibrate. Load stays gated because it has meaning only during productive work.

| Counter | Accumulates on a message when |
|---------|-------------------------------|
| `engineHours` | always (the asset is switched on and reporting) |
| `totalWorkingHours` | productive work |
| `highLoadHours` | productive work **and** `loadFactor ≥ 0.75` |
| `tempExcursionHours` | `temperatureC > 90` (regardless of working state) |
| `vibrationAlertHours` | `vibrationLevel` is `ELEVATED` or `HIGH` (regardless of working state) |

### 4.2 Accumulation rule

**REQ-ACCUMULATE.** On each telemetry message for an asset, the processor shall apply the four steps below in order.

*Priority: Must · Source: NEED-TRACEABLE-RULES*

1. If the message `timestamp` is less than or equal to the last processed timestamp for that asset, discard the message entirely (out-of-order or duplicate delivery). It updates nothing.
2. Compute Δt = `timestamp` − last processed timestamp, in seconds. For the first message of an asset, Δt is undefined.
3. If Δt is undefined or Δt > 60 s, **discard Δt**: add nothing to any counter, including `engineHours`. Store the timestamp as the new last-processed value and continue. A Δt above 60 s is roughly six times the maximum cadence and therefore means the simulator or a consumer was down; crediting that time would invent hours the machine never ran.
4. Otherwise add Δt / 3600 to every counter whose §4.1 condition is true for **this** message, and store the timestamp as the new last-processed value. The elapsed interval is attributed to the state reported at its end. With Δt ≤ 60 s the resulting error is bounded by one minute per state change.

### 4.3 Hourly buckets and the rolling 24-hour window

**REQ-BUCKETS.** Counter contributions shall be accumulated into hourly buckets aligned to whole hours in `Europe/Vienna`. A bucket holds the exact seconds each of the five counters accrued within that hour, so threshold comparisons are exact. A message's whole Δt shall be credited to the bucket containing its own timestamp. Because credited Δt is at most 60 seconds, at most one minute of a message can fall in the neighbouring hour.

*Priority: Must · Source: NEED-HISTORY-REVIEW, NEED-TRACEABLE-RULES*

**REQ-SCOPES.** Two scopes are derived from the buckets:

*Priority: Must · Source: NEED-OPERATIONAL-RISK, NEED-LIFETIME-HOURS*

- **Rolling 24 h** — the current, still-open bucket plus the 23 completed buckets before it. Coverage is therefore between 23 and 24 hours depending on the position within the current hour. This is deliberate: the window reacts to load within the current hour.
- **Lifetime** — the §4.4 baseline plus everything accrued since the process started.

DST transitions (23- and 25-hour local days) are not handled specially — out of scope.

### 4.4 Seeded lifetime baselines

**REQ-BASELINES.** The lifetime flag threshold in §5.1 is unreachable in a demo that starts from zero. Each asset shall therefore start from the fixed lifetime baseline in the table below, loaded at startup. Baselines shall apply to the lifetime scope only. The rolling 24-hour window shall start empty. Lifetime counters shall not be carried across restarts; every run shall start from this table, so a demo run is reproducible.

*Priority: Must · Source: NEED-LIFETIME-HOURS, NEED-REPRODUCIBLE-DEMO*

| Asset | `engineHours` | `totalWorkingHours` | `highLoadHours` | `tempExcursionHours` | `vibrationAlertHours` |
|-------|---------------|---------------------|-----------------|----------------------|------------------------|
| `TRACTOR` | 6200 | 3400 | 410 | 26 | 58 |
| `MOWER` | 950 | 640 | 95 | 4 | 12 |
| `BALER` | 1480 | 1010 | 240 | 11 | 46 |
| `MIXER` | 1120 | 860 | 130 | 7 | 19 |

Every row satisfies the order in REQ-COUNTER-SET. On the baselines alone, before any telemetry, §5 puts `TRACTOR` at `WATCH` on its lifetime flag and the other three at `GOOD`. The §3.2 burst then fills the rolling window.

### 4.5 Utilization

**REQ-UTILIZATION.** The system shall publish the two percentages below, both over the rolling 24-hour window, and shall publish `null` when the denominator is 0, including for any asset that has not reported in this run. It shall not publish a lifetime utilization percentage: the §4.4 baselines carry no wall-clock span, so that percentage would be meaningless.

*Priority: Must · Source: NEED-UTILIZATION-AWARENESS*

- `utilizationPercent` = `totalWorkingHours` (24 h) / `windowCoverageHours` × 100 — how much of the recent day the machine spent working.
- `productiveSharePercent` = `totalWorkingHours` (24 h) / `engineHours` (24 h) × 100 — of the time it was actually running, how much was productive.

`windowCoverageHours` = the time from the later of (start of the oldest included bucket) and (the asset's first message in this run) up to now.

---

## 5. Flags, status and ranking

### 5.1 Flag definitions

**REQ-FLAGS.** The system shall evaluate the four flags in the table below. A flag shall be active when its counter, in its scope, is greater than or equal to its threshold, and inactive otherwise. Every active flag shall be published with the counter, the scope, the value, and the threshold. An inactive flag shall be absent.

*Priority: Must · Source: NEED-PRIORITISE-MAINTENANCE, NEED-FLAG-REASON*

| Flag | Counter | Scope | Threshold |
|------|---------|-------|-----------|
| `HEAVY_RECENT_USE` | `highLoadHours` | rolling 24 h | 1.5 h |
| `THERMAL_LOAD` | `tempExcursionHours` | rolling 24 h | 0.8 h |
| `MECHANICAL_ATTENTION` | `vibrationAlertHours` | rolling 24 h | 1.0 h |
| `HIGH_CUMULATIVE_LOAD` | `highLoadHours` | lifetime | 400 h |

### 5.2 Status

**REQ-STATUS.** Status shall be exactly one of the three cases below, by the number of active flags.

*Priority: Must · Source: NEED-PRIORITISE-MAINTENANCE*

- `CRITICAL` — two or more active flags
- `WATCH` — exactly one active flag
- `GOOD` — no active flag

### 5.3 Live condition

Status is an integral over 24 hours and therefore cannot answer whether the machine is in trouble right now.

**REQ-LIVE.** `liveCondition` shall be derived from the most recent message only, using the first matching row of the table below. It shall not influence flags, status, ranking, or recommendations.

*Priority: Must · Source: NEED-OPERATIONAL-RISK*

| Value | Condition |
|-------|-----------|
| `STALE` | the most recent message for this asset is more than 60 s old, or the asset has not reported at all in this run |
| `ALERT` | `vibrationLevel` = `HIGH` **or** `temperatureC > 95` |
| `CAUTION` | `vibrationLevel` = `ELEVATED` **or** `temperatureC > 90` **or** `loadFactor ≥ 0.9` |
| `NORMAL` | otherwise |

### 5.4 Priority ranking

**REQ-RANK.** Whenever ranks are assigned, all four assets shall be ranked together. Sort by the criteria below, in order. `priorityRank` shall be 1-based in that order.

*Priority: Must · Source: NEED-PRIORITISE-MAINTENANCE*

1. number of active flags, descending
2. `highLoadHours` (24 h) + `tempExcursionHours` (24 h) + `vibrationAlertHours` (24 h), descending
3. `assetId`, ascending

Criterion 2 is a deliberate heuristic: the three counters can overlap on the same message, so the sum is a tie-breaking magnitude, not a physical quantity. Criterion 3 guarantees a fully deterministic order.

### 5.5 Service recommendation

**REQ-RECOMMEND.** The recommendation shall be the sentence for that status in the table below. "Engine hours" in those sentences means the `engineHours` counter (§4.1) of that machine.

*Priority: Must · Source: NEED-PRIORITISE-MAINTENANCE*

| Status | Recommendation |
|--------|----------------|
| `CRITICAL` | Priority check recommended within the next 5 engine hours. |
| `WATCH` | Plan inspection within the next 15–20 engine hours. |
| `GOOD` | No inspection recommended at this time. |

---

## 6. REST API

Exposed by OperatorApi (see `ARCHITECTURE.md` §2 for the module and port).

**REQ-API-COMMON.** The API shall expose only the read-only endpoints in the table below. There shall be no authentication. No endpoint shall change system state. Timestamps in requests and responses shall be ISO-8601 instants in UTC. Errors shall use Spring's default error body with the status code and message given in this section. There shall be no custom error DTO.

*Priority: Must · Source: NEED-STABLE-INTERFACE*

| Method & Path | Description |
|---------------|-------------|
| `GET /api/assets` | Current counters, flags, status and priority ranking for all four assets. |
| `GET /api/assets/{assetId}` | The same view for a single asset. |
| `GET /api/history` | Hourly aggregates for a requested period. |
| `GET /api/telemetry/latest` | The most recent telemetry message per asset. |

### 6.1 `GET /api/assets` and `GET /api/assets/{assetId}`

**REQ-API-ASSETS.** `GET /api/assets` shall return `200` with `assets` ordered by `priorityRank`, in the shape of the example below. An asset that has not reported in this run shall still be listed, with its §4.4 baselines as `lifetimeCounters`, zeroed `rolling24hCounters`, `liveCondition` `STALE`, and both percentages `null`. The response shall be `503` when no state has been published yet; that is a startup condition, not an empty fleet. `GET /api/assets/{assetId}` shall return `200` with one asset object in the same shape, `404` when `assetId` is not one of the §2 assets, and `503` when the asset is known but no state has been published for it.

*Priority: Must · Source: NEED-PRIORITISE-MAINTENANCE, NEED-FLAG-REASON*

```json
{
  "generatedAt": "2026-08-13T09:41:12Z",
  "assets": [
    {
      "assetId": "TRACTOR",
      "priorityRank": 1,
      "status": "WATCH",
      "liveCondition": "NORMAL",
      "recommendation": "Plan inspection within the next 15–20 engine hours.",
      "flags": [
        {
          "flag": "HIGH_CUMULATIVE_LOAD",
          "counter": "highLoadHours",
          "scope": "LIFETIME",
          "value": 411.2,
          "threshold": 400.0
        }
      ],
      "lifetimeCounters": {
        "engineHours": 6203.1,
        "totalWorkingHours": 3401.8,
        "highLoadHours": 411.2,
        "tempExcursionHours": 26.0,
        "vibrationAlertHours": 58.0
      },
      "rolling24hCounters": {
        "engineHours": 3.1,
        "totalWorkingHours": 1.8,
        "highLoadHours": 1.2,
        "tempExcursionHours": 0.0,
        "vibrationAlertHours": 0.0
      },
      "windowCoverageHours": 3.1,
      "utilizationPercent": 58.1,
      "productiveSharePercent": 58.1
    }
  ]
}
```

### 6.2 `GET /api/history`

**REQ-API-HISTORY.** `GET /api/history` shall accept the parameters in the table below. It shall return one row per asset per hour that overlaps the requested period, ordered by `hourStart` ascending then `assetId` ascending. Counter values shall be the amount accrued within that hour, not running totals. Hours shall be aligned to `Europe/Vienna` local hours (§4.3) and reported as the UTC instant at which the local hour began. It shall return `400` when `from` or `to` is missing or unparseable, when `from` is not before `to`, or when the range exceeds 7 days. A range of exactly 7 days shall be accepted. It shall return `404` when `assetId` is given but is not one of the §2 assets. An empty `rows` array shall be `200`, not an error.

*Priority: Must · Source: NEED-HISTORY-REVIEW*

| Parameter | Required | Format |
|-----------|----------|--------|
| `from` | yes | ISO-8601 instant |
| `to` | yes | ISO-8601 instant |
| `assetId` | no | one of the four §2 assets; omitted means all |

`200 OK`

```json
{
  "from": "2026-08-13T06:00:00Z",
  "to": "2026-08-13T09:00:00Z",
  "rows": [
    {
      "assetId": "BALER",
      "hourStart": "2026-08-13T06:00:00Z",
      "engineHours": 1.0,
      "totalWorkingHours": 0.5,
      "highLoadHours": 0.25,
      "tempExcursionHours": 0.0,
      "vibrationAlertHours": 0.0
    }
  ]
}
```

### 6.3 `GET /api/telemetry/latest`

The live telemetry strip is intended to be polled every few seconds. That interval is not a requirement.

**REQ-API-LATEST.** `GET /api/telemetry/latest` shall return `200` in the shape below, including before any state has been published. It shall not return `503`. `telemetry` shall be `null` only for an asset that has never reported in this run, and that asset's `liveCondition` shall be `STALE`. An asset that reported and then fell silent shall be `STALE` and shall keep its last reading, so the last activity stays visible. All four assets shall be present, ordered by `assetId` ascending.

*Priority: Must · Source: NEED-OPERATIONAL-RISK*

`200 OK`

```json
{
  "generatedAt": "2026-08-13T09:41:12Z",
  "assets": [
    {
      "assetId": "TRACTOR",
      "liveCondition": "CAUTION",
      "telemetry": {
        "assetId": "TRACTOR",
        "timestamp": "2026-08-13T09:41:07Z",
        "ptoEngaged": true,
        "groundSpeedKmh": 10.0,
        "loadFactor": 0.80,
        "temperatureC": 94.0,
        "vibrationLevel": "NORMAL"
      }
    }
  ]
}
```

---

## 7. Decision support provided to the farmer

**REQ-DECISIONS.** The system shall support exactly the three decisions below, each backed only by the counters and indicators named there, and no other decision.

*Priority: Must · Source: NEED-PRIORITISE-MAINTENANCE, NEED-OPERATIONAL-RISK, NEED-UTILIZATION-AWARENESS*

1. **Maintenance prioritisation** — what should be looked at first, and how urgent is it?
   Backed by: priority ranking (§5.4), status (§5.2), the flags and their reasons (§5.1), and the recommendation (§5.5).

2. **Short-term operational risk** — is it advisable to keep working this machine hard?
   Backed by: `liveCondition` (§5.3) for the current instant, and the rolling 24-hour flags (§5.1) for sustained recent load.

3. **Basic utilization awareness** — which machines are doing productive work versus mostly running without working?
   Backed by: `utilizationPercent` and `productiveSharePercent` (§4.5), and `totalWorkingHours` against `engineHours` (§4.1).
