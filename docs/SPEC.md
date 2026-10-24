## 1. Goal

**REQ-GOAL-SCOPE.** The system shall monitor utilization of one tractor and three typical Austrian hay/dairy-farm implements — the fleet fixed by §2 — and shall support the decisions in §7 and no others.

**REQ-GOAL-TRACE.** Every number the system shows shall be traceable to a counter, and every counter shall be traceable to telemetry. The system shall not use hidden scoring, machine learning, or opaque weighting.

---

## 2. Assets

**REQ-FLEET-FIXED.** The fleet shall be exactly the four assets in the table below. The system shall not register or discover assets.

| Asset | Role |
|-------|------|
| `TRACTOR` | Primary power unit; pulls or drives the three implements. |
| `MOWER` | Rotary/disc mower. |
| `BALER` | Round baler. |
| `MIXER` | Feed mixer wagon. |

---

## 3. Telemetry

### 3.1 Message

**REQ-TELEMETRY-CADENCE.** Outside the §3.2 startup burst, each asset shall publish one telemetry message every 5–10 seconds, independently of the other assets.

```json
{
  "assetId": "BALER",
  "timestamp": "2026-07-31T01:15:00Z",
  "ptoEngaged": true,
  "groundSpeedKmh": 8.4,
  "loadFactor": 0.78,
  "temperatureC": 87.2,
  "vibrationLevel": "ELEVATED",
  "workingStatus": "WORKING"
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
| `workingStatus` | string | `WORKING`, `TRANSPORT` or `IDLE` |

**REQ-TELEMETRY-SHAPE.** A telemetry message shall have exactly the fields and constraints in the table above.

**REQ-TELEMETRY-WORKING-STATUS.** The simulator shall set `workingStatus` to `WORKING` exactly when `ptoEngaged` is true and `groundSpeedKmh > 2`.

**REQ-TELEMETRY-PRODUCTIVE.** Consumers shall derive productive work from `ptoEngaged` and `groundSpeedKmh > 2`, and shall use `workingStatus` only to separate `TRANSPORT` from `IDLE` (§4.1). Consumers shall not treat `workingStatus` as proof of productive work.

### 3.2 Simulator behaviour

**REQ-SIM-ONLY-SOURCE.** The simulator shall be the only source of telemetry. It is not a physics model. The ranges in this section are the contract.

**REQ-SIM-CYCLE.** Each asset shall independently follow the state cycle, the field values, and the per-asset rates in the tables below. The working `loadFactor` band and the two excursion rates shall be the only per-asset differences a §4.1 counter reads. No counter or threshold shall use the per-asset `groundSpeedKmh` difference.

Each asset walks this cycle:

| State | Dwell | Next state |
|-------|-------|------------|
| `WORKING` | 20–60 min | `TRANSPORT` (60 %) or `IDLE` (40 %) |
| `TRANSPORT` | 3–10 min | `WORKING` (70 %) or `IDLE` (30 %) |
| `IDLE` | 5–30 min | `WORKING` (80 %) or `TRANSPORT` (20 %) |

Field values per state:

| State | `ptoEngaged` | `groundSpeedKmh` | `loadFactor` |
|-------|--------------|------------------|--------------|
| `WORKING` | `true` | `TRACTOR` 6–14, `MOWER` 8–13, `BALER` 6–10, `MIXER` 3–5 | per asset, see below |
| `TRANSPORT` | `false` | 12–30 (`MIXER` 8–18) | 0.15–0.35 |
| `IDLE` | `false` | 0 | 0.05–0.15 |

Per-asset working `loadFactor` and excursion rates:

| Asset | working `loadFactor` | thermal excursion chance | vibration excursion chance |
|-------|----------------------|--------------------------|----------------------------|
| `TRACTOR` | 0.30–0.90 | 1.5 % | 1.0 % |
| `BALER` | 0.25–0.81 | 0.15 % | 0.15 % |
| `MOWER` | 0.25–0.78 | 0.15 % | 0.8 % |
| `MIXER` | 0.25–0.72 | 0.15 % | 0.15 % |

**REQ-SIM-DEMO.** The per-asset rates and the default burst window shall be aimed at these anchors over a full window: `TRACTOR` at `CRITICAL` on the rolling flags, `BALER` at `WATCH` on `HEAVY_RECENT_USE` alone, `MIXER` at `GOOD`, and `MOWER` able to move between `GOOD`, `WATCH`, and `CRITICAL` as `MECHANICAL_ATTENTION` comes and goes. A single random run is not required to land on every anchor. The §5 flags shall be reachable within a demo session.

`MOWER` moves because its only flag driver is the vibration excursion, and a window holds fewer than ten excursions of 2–15 minutes each — too few to average out. The `ELEVATED`-from-load path is the only low-variance route to `vibrationAlertHours`, and REQ-SIM-ELEVATED keeps that path accruing `highLoadHours` on the same messages. The three anchors are what make the §5.4 ranking meaningful; `MOWER` is what makes it visibly live.

**REQ-SIM-TEMPERATURE.** Outside a thermal excursion, `temperatureC` shall drift smoothly within 70–85 °C while `WORKING`, 55–70 °C while `TRANSPORT`, and towards 45–60 °C while `IDLE`. Independently of the state cycle, each asset shall enter a thermal excursion at 91–98 °C at its rate in the per-asset table, for 2–15 minutes.

**REQ-SIM-VIBRATION.** `vibrationLevel` shall be `NORMAL` by default, `ELEVATED` while `loadFactor ≥ 0.85`, and `HIGH` during a vibration excursion. Each asset shall enter a vibration excursion at its rate in the per-asset table, for 2–15 minutes.

**REQ-SIM-EXCURSION-RATE.** Both excursion rates shall be applied per minute of working time, not per emitted message. The startup burst and the live cadence shall therefore wear a machine at the same speed.

**REQ-SIM-ELEVATED.** `ELEVATED` from load alone shall be possible only for an asset whose working `loadFactor` band can reach the `ELEVATED` condition in REQ-SIM-VIBRATION. That condition shall stay strictly above the `highLoadHours` condition in §4.1, so every such message also accrues `highLoadHours`. The bands and thresholds shall not be retuned so that `ELEVATED` from load can occur without accruing `highLoadHours`.

**REQ-SIM-BURST.** Each asset shall open its run by publishing one message per simulated minute across a configurable window of the immediately preceding hours, ending at the present, and only then switch to the §3.1 cadence. The step shall be exactly 60 seconds, which §4.2 credits, so the burst can fill the window in the time it takes to publish. Timestamps shall be true wall-clock instants. Nothing shall be backdated beyond the window. The window shall default to 24 hours. It is a calibration knob, not a threshold: shortening it only moves a run's starting point along the same ramp.

**REQ-SIM-MEMORY.** Simulator state shall be in memory only and shall start fresh on every run, the burst included.

---

## 4. Counters

### 4.1 Counter set

**REQ-COUNTER-SET.** Each asset shall maintain the eight hour-counters in the table below, and shall accrue each counter only on the condition in that table. Productive work shall mean the `totalWorkingHours` condition.

Temperature and vibration stay ungated because an idle machine can still overheat or vibrate. Load stays gated because it has meaning only during productive work.

| Counter | Accumulates on a message when |
|---------|-------------------------------|
| `engineHours` | always (the asset is switched on and reporting) |
| `totalWorkingHours` | `ptoEngaged` **and** `groundSpeedKmh > 2` |
| `transportHours` | not productive work **and** `workingStatus` = `TRANSPORT` |
| `idleHours` | not productive work **and** `workingStatus` ≠ `TRANSPORT` |
| `loadedHours` | productive work **and** `loadFactor ≥ 0.55` |
| `highLoadHours` | productive work **and** `loadFactor ≥ 0.75` |
| `tempExcursionHours` | `temperatureC > 90` (regardless of working state) |
| `vibrationAlertHours` | `vibrationLevel` is `ELEVATED` or `HIGH` (regardless of working state) |

**REQ-COUNTER-INVARIANT.** `engineHours` shall equal `totalWorkingHours` + `transportHours` + `idleHours`. `totalWorkingHours` shall be greater than or equal to `loadedHours`, which shall be greater than or equal to `highLoadHours`. Both relations follow from the conditions in the table above.

### 4.2 Accumulation rule

**REQ-ACCUMULATE.** On each telemetry message for an asset, the processor shall apply the four steps below in order.

1. If the message `timestamp` is less than or equal to the last processed timestamp for that asset, discard the message entirely (out-of-order or duplicate delivery). It updates nothing.
2. Compute Δt = `timestamp` − last processed timestamp, in seconds. For the first message of an asset, Δt is undefined.
3. If Δt is undefined or Δt > 60 s, **discard Δt**: add nothing to any counter, including `engineHours`. Store the timestamp as the new last-processed value and continue. A Δt above 60 s is roughly six times the maximum cadence and therefore means the simulator or a consumer was down; crediting that time would invent hours the machine never ran.
4. Otherwise add Δt / 3600 to every counter whose §4.1 condition is true for **this** message, and store the timestamp as the new last-processed value. The elapsed interval is attributed to the state reported at its end. With Δt ≤ 60 s the resulting error is bounded by one minute per state change.

### 4.3 Hourly buckets and the rolling 24-hour window

**REQ-BUCKETS.** Counter contributions shall be accumulated into hourly buckets aligned to whole hours in `Europe/Vienna`. A bucket holds the amount each of the eight counters accrued within that hour. A message's whole Δt shall be credited to the bucket containing its own timestamp. Because credited Δt is at most 60 seconds, at most one minute of a message can fall in the neighbouring hour.

**REQ-SCOPES.** Two scopes are derived from the buckets:

- **Rolling 24 h** — the current, still-open bucket plus the 23 completed buckets before it. Coverage is therefore between 23 and 24 hours depending on the position within the current hour. This is deliberate: the window reacts to load within the current hour.
- **Lifetime** — the §4.4 baseline plus everything accrued since the process started.

**REQ-BUCKET-EXACT.** Bucket contents shall be exact seconds, so threshold comparisons are exact. Only the edge of the rolling window shall be quantised to the hour.

DST transitions (23- and 25-hour local days) are not handled specially — out of scope (§8).

### 4.4 Seeded lifetime baselines

**REQ-BASELINES.** The lifetime flag thresholds in §5.1 are unreachable in a demo that starts from zero. Each asset shall therefore start from the fixed lifetime baseline in the table below, loaded at startup. Baselines shall apply to the lifetime scope only. The rolling 24-hour window shall start empty. Lifetime counters shall not be carried across restarts; every run shall start from this table, so a demo run is reproducible.

| Asset | `engineHours` | `totalWorkingHours` | `transportHours` | `idleHours` | `loadedHours` | `highLoadHours` | `tempExcursionHours` | `vibrationAlertHours` |
|-------|---------------|---------------------|------------------|-------------|---------------|-----------------|----------------------|------------------------|
| `TRACTOR` | 6200 | 3400 | 1500 | 1300 | 980 | 410 | 26 | 58 |
| `MOWER` | 950 | 640 | 210 | 100 | 310 | 95 | 4 | 12 |
| `BALER` | 1480 | 1010 | 300 | 170 | 620 | 240 | 11 | 46 |
| `MIXER` | 1120 | 860 | 60 | 200 | 415 | 130 | 7 | 19 |

Every row satisfies REQ-COUNTER-INVARIANT. On the baselines alone, before any telemetry, §5 puts `TRACTOR` at `WATCH` on its single lifetime flag and the other three at `GOOD`, with no asset at `CRITICAL` from its history. The §3.2 burst then fills the rolling window. These baselines make the lifetime flag reachable; they are not the whole of the first screen.

### 4.5 Utilization

**REQ-UTILIZATION.** The system shall publish the two percentages below, both over the rolling 24-hour window, and shall publish `null` when the denominator is 0, including for any asset that has not reported in this run. It shall not publish a lifetime utilization percentage: the §4.4 baselines carry no wall-clock span, so that percentage would be meaningless.

- `utilizationPercent` = `totalWorkingHours` (24 h) / `windowCoverageHours` × 100 — how much of the recent day the machine spent working.
- `productiveSharePercent` = `totalWorkingHours` (24 h) / `engineHours` (24 h) × 100 — of the time it was actually running, how much was productive.

`windowCoverageHours` = the time from the later of (start of the oldest included bucket) and (the asset's first message in this run) up to now.

---

## 5. Flags, status and ranking

### 5.1 Flag definitions

**REQ-FLAGS.** The system shall evaluate the flags in the tables below. Each named flag shall be inactive, or active at the highest threshold it currently meets, so the eight rows produce at most four active flags. Every active flag shall be published with the counter, the scope, the value, and the threshold that triggered it. An inactive flag shall be absent.

Evaluated on the rolling 24-hour window:

| Flag | Condition | Severity |
|------|-----------|----------|
| `HEAVY_RECENT_USE` | `highLoadHours` (24 h) ≥ 1.5 h | `MODERATE` |
| `HEAVY_RECENT_USE` | `highLoadHours` (24 h) ≥ 2.5 h | `SEVERE` |
| `THERMAL_LOAD` | `tempExcursionHours` (24 h) ≥ 0.8 h | `MODERATE` |
| `THERMAL_LOAD` | `tempExcursionHours` (24 h) ≥ 1.5 h | `SEVERE` |
| `MECHANICAL_ATTENTION` | `vibrationAlertHours` (24 h) ≥ 1.0 h | `MODERATE` |
| `MECHANICAL_ATTENTION` | `vibrationAlertHours` (24 h) ≥ 2.0 h | `SEVERE` |

Evaluated on the lifetime scope:

| Flag | Condition | Severity |
|------|-----------|----------|
| `HIGH_CUMULATIVE_LOAD` | `loadedHours` (lifetime) ≥ 850 h | `MODERATE` |
| `HIGH_CUMULATIVE_LOAD` | `loadedHours` (lifetime) ≥ 1200 h | `SEVERE` |

### 5.2 Status

**REQ-STATUS.** Counting each active flag once, at its current severity, status shall be exactly one of the three cases below. The cases are exhaustive and mutually exclusive.

- `CRITICAL` — at least one `SEVERE` flag **or** two or more `MODERATE` flags
- `WATCH` — exactly one `MODERATE` flag and no `SEVERE` flag
- `GOOD` — no active flag

### 5.3 Live condition

Status is an integral over 24 hours and therefore cannot answer whether the machine is in trouble right now.

**REQ-LIVE.** `liveCondition` shall be derived from the most recent message only, using the first matching row of the table below. It shall not influence flags, status, ranking, or recommendations.

| Value | Condition |
|-------|-----------|
| `STALE` | the most recent message for this asset is more than 60 s old, or the asset has not reported at all in this run |
| `ALERT` | `vibrationLevel` = `HIGH` **or** `temperatureC > 95` |
| `CAUTION` | `vibrationLevel` = `ELEVATED` **or** `temperatureC > 90` **or** `loadFactor ≥ 0.9` |
| `NORMAL` | otherwise |

### 5.4 Priority ranking

**REQ-RANK.** Whenever ranks are assigned, all four assets shall be ranked together. Sort by the criteria below, in order. An asset with no flags sorts to the bottom by those criteria, not by an extra rule. `priorityRank` shall be 1-based in that order.

1. number of `SEVERE` flags, descending
2. number of `MODERATE` flags, descending
3. `highLoadHours` (24 h) + `tempExcursionHours` (24 h) + `vibrationAlertHours` (24 h), descending
4. `assetId`, ascending

Criterion 3 is a deliberate heuristic: the three counters can overlap on the same message, so the sum is a tie-breaking magnitude, not a physical quantity. Criterion 4 guarantees a fully deterministic order.

### 5.5 Service recommendation

**REQ-RECOMMEND.** The recommendation shall be the sentence for that status in the table below. "Engine hours" in those sentences means the `engineHours` counter (§4.1) of that machine.

| Status | Recommendation |
|--------|----------------|
| `CRITICAL` | Priority check recommended within the next 5 engine hours. |
| `WATCH` | Plan inspection within the next 15–20 engine hours. |
| `GOOD` | No inspection recommended at this time. |

---

## 6. REST API

Exposed by OperatorApi (see `ARCHITECTURE.md` §2 for the module and port).

**REQ-API-COMMON.** The API shall expose only the read-only endpoints in the table below. There shall be no authentication. No endpoint shall change system state. Timestamps in requests and responses shall be ISO-8601 instants in UTC. Errors shall use Spring's default error body with the status code and message given in this section. There shall be no custom error DTO.

| Method & Path | Description |
|---------------|-------------|
| `GET /api/assets` | Current counters, flags, status and priority ranking for all four assets. |
| `GET /api/assets/{assetId}` | The same view for a single asset. |
| `GET /api/history` | Hourly aggregates for a requested period. |
| `GET /api/telemetry/latest` | The most recent telemetry message per asset. |

### 6.1 `GET /api/assets`

**REQ-API-ASSETS.** `GET /api/assets` shall return `200` with `assets` ordered by `priorityRank`, in the shape of the example below. An asset that has not reported in this run shall still be listed, with its §4.4 baselines as `lifetimeCounters`, zeroed `rolling24hCounters`, `liveCondition` `STALE`, and both percentages `null`. The response shall be `503` when no state has been published yet. That `503` is a startup condition, not an empty fleet (§2).

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
          "severity": "MODERATE",
          "counter": "loadedHours",
          "scope": "LIFETIME",
          "value": 981.4,
          "threshold": 850.0
        }
      ],
      "lifetimeCounters": {
        "engineHours": 6203.1,
        "totalWorkingHours": 3401.8,
        "transportHours": 1500.6,
        "idleHours": 1300.7,
        "loadedHours": 981.4,
        "highLoadHours": 411.2,
        "tempExcursionHours": 26.0,
        "vibrationAlertHours": 58.3
      },
      "rolling24hCounters": {
        "engineHours": 3.1,
        "totalWorkingHours": 1.8,
        "transportHours": 0.6,
        "idleHours": 0.7,
        "loadedHours": 1.4,
        "highLoadHours": 1.2,
        "tempExcursionHours": 0.0,
        "vibrationAlertHours": 0.3
      },
      "windowCoverageHours": 3.1,
      "utilizationPercent": 58.1,
      "productiveSharePercent": 58.1
    }
  ]
}
```

### 6.2 `GET /api/assets/{assetId}`

**REQ-API-ASSET.** `GET /api/assets/{assetId}` shall return `200` with one asset object in the same shape as one element of §6.1 `assets`. It shall return `404` only when `assetId` is not one of the §2 assets. It shall return `503` when `assetId` is one of those assets but no state has been published for it. A known asset with missing state is `503`, not `404`.

### 6.3 `GET /api/history`

**REQ-API-HISTORY.** `GET /api/history` shall accept the parameters in the table below. It shall return one row per asset per hour that overlaps the requested period, ordered by `hourStart` ascending then `assetId` ascending. Counter values shall be the amount accrued within that hour, not running totals. Hours shall be aligned to `Europe/Vienna` local hours (§4.3) and reported as the UTC instant at which the local hour began. It shall return `400` when `from` or `to` is missing or unparseable, when `from` is not before `to`, or when the range exceeds 7 days. A range of exactly 7 days shall be accepted. It shall return `404` when `assetId` is given but is not one of the §2 assets. An empty `rows` array shall be `200`, not an error.

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
      "totalWorkingHours": 0.82,
      "transportHours": 0.11,
      "idleHours": 0.07,
      "loadedHours": 0.74,
      "highLoadHours": 0.51,
      "tempExcursionHours": 0.0,
      "vibrationAlertHours": 0.19
    }
  ]
}
```

### 6.4 `GET /api/telemetry/latest`

The live telemetry strip is intended to be polled every few seconds. That interval is not a requirement.

**REQ-API-LATEST.** `GET /api/telemetry/latest` shall return `200` in the shape below, including before any state has been published. It shall not return `503`. `telemetry` shall be `null` only for an asset that has never reported in this run, and that asset's `liveCondition` shall be `STALE`. An asset that reported and then fell silent shall be `STALE` and shall keep its last reading, so the last activity stays visible. All four assets shall be present, ordered by `assetId` ascending. Before any state is published, that `200` body is all four assets at `STALE` with `telemetry` `null`.

`200 OK`

```json
{
  "generatedAt": "2026-08-13T09:41:12Z",
  "assets": [
    {
      "assetId": "BALER",
      "liveCondition": "CAUTION",
      "telemetry": {
        "assetId": "BALER",
        "timestamp": "2026-07-31T01:15:00Z",
        "ptoEngaged": true,
        "groundSpeedKmh": 8.4,
        "loadFactor": 0.78,
        "temperatureC": 87.2,
        "vibrationLevel": "ELEVATED",
        "workingStatus": "WORKING"
      }
    }
  ]
}
```

---

## 7. Decision support provided to the farmer

**REQ-DECISIONS.** The system shall support exactly the three decisions below, each backed only by the counters and indicators named there, and no other decision.

1. **Maintenance prioritisation** — what should be looked at first, and how urgent is it?
   Backed by: priority ranking (§5.4), status (§5.2), the flags and their reasons (§5.1), and the recommendation (§5.5).

2. **Short-term operational risk** — is it advisable to keep working this machine hard?
   Backed by: `liveCondition` (§5.3) for the current instant, and the rolling 24-hour flags (§5.1) for sustained recent load.

3. **Basic utilization awareness** — which machines are doing productive work versus mostly idling or only transporting?
   Backed by: `utilizationPercent` and `productiveSharePercent` (§4.5), and the `totalWorkingHours` / `transportHours` / `idleHours` split (§4.1).