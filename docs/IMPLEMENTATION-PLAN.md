# docs\IMPLEMENTATION-PLAN.md

> **Non-normative.** This document is a build plan, not a source of truth. It sits *outside* the `SPEC.md` §0 hierarchy and is deliberately self-contained: it restates thresholds, ranges and names so it can be executed without holding four documents open at once. Where this file and `SPEC.md` / `ARCHITECTURE.md` / `GLOSSARY.md` disagree, **the governed document wins** and this file is wrong and must be corrected. It is not registered in `SPEC.md` §0 for exactly that reason — a document that restates rules cannot be part of a hierarchy that forbids restating rules.
>
> **Scope.** Backend only: the three Java modules, the SQL schema, and the docker wiring. The frontend rebuild and per-service unit tests are out of scope — see §7.

---

## 1. Resolved ambiguities

The three governed documents do not decide the following five points. Each is recorded as *what the documents say → why that is under-determined → the decision → what it costs*. §1.6 is an open finding that still needs a SPEC decision.

### 1.1 Demo pacing — startup backfill

**Documents.** `SPEC.md` §3.1: one message per asset every 5–10 seconds. §4.2 step 3: a Δt above 60 s is discarded entirely. §5.1: the weakest rolling flag needs `highLoadHours` ≥ 1.5 h. §3.2: the simulator "must produce enough variation for the §5 flags to be reachable within a demo session".

**Why under-determined.** Counters accumulate wall-clock time at 1:1, and the 60 s discard rule caps how fast simulated time can advance. No rolling flag can appear in under ~1.5 hours of runtime, which contradicts §3.2's own requirement.

**Decision.** On startup the simulator publishes a **backfill burst**: for each asset, ~1440 messages stepping Δt = exactly 60 s across the last 24 hours, ending at `now`. 60 s is credited, not discarded (§4.2 discards Δt **greater than** 60 s). It then switches to the normal 5–10 s live cadence, which continues from the last backfill timestamp with a Δt of 5–10 s.

**Cost.** ~5760 messages at boot (see §1.5). The rolling window, the utilization percentages and 24 hours of `hourly_aggregate` history are all populated within seconds, and message timestamps never drift from wall-clock.

### 1.2 Excursion rate — probability scaled to Δt

**Documents.** `SPEC.md` §3.2: a thermal excursion starts "with roughly a 5 % chance per working interval, lasting 2–15 minutes"; a vibration excursion likewise at 3 %.

**Why under-determined.** "Working interval" reads either as *per emitted message while working* or *per WORKING dwell period*, and the two differ by two orders of magnitude. The backfill makes it worse: backfill steps 60 s per message and the live phase ~7.5 s, so a flat per-message probability would fire 8× more often once live.

**Decision.** Per-message trigger, probability scaled to the message's own Δt:

```
thermalChance   = 0.05 * deltaSeconds / 60.0
vibrationChance = 0.03 * deltaSeconds / 60.0
```

Evaluated only while the asset is `WORKING` and not already inside an excursion of that kind. An excursion that has started runs to its 2–15 minute end regardless of state changes ("independently of the state cycle").

**Cost.** Identical duty cycle in both phases: mean 20 minutes of work to trigger against a mean 8.5 minute excursion ≈ 30 % of working time. See §1.6 — this lands `tempExcursionHours` far above its threshold.

### 1.3 Flag storage — three columns per flag

**Documents.** `ARCHITECTURE.md` §4: `asset_state` holds "the four flags each with the severity, triggering counter, scope, value and threshold behind it". §2: `OperatorApi` "derives no product rule".

**Why under-determined.** `counter` and `scope` are constant per flag — `HEAVY_RECENT_USE` is always `highLoadHours` / `ROLLING_24H`. Storing them per row repeats the same string in every row forever.

**Decision.** Three columns per flag — `*_severity` (NULL = inactive), `*_value`, `*_threshold` — twelve columns total. `OperatorApi` fills `counter` and `scope` from a constant lookup:

| Flag | `counter` | `scope` |
|------|-----------|---------|
| `HEAVY_RECENT_USE` | `highLoadHours` | `ROLLING_24H` |
| `THERMAL_LOAD` | `tempExcursionHours` | `ROLLING_24H` |
| `MECHANICAL_ATTENTION` | `vibrationAlertHours` | `ROLLING_24H` |
| `HIGH_CUMULATIVE_LOAD` | `loadedHours` | `LIFETIME` |

**Cost.** A naming lookup lives in `OperatorApi`. It is a label table, not a product rule — no threshold, percentage or status is computed there.

### 1.4 History and the open hour — completed hours only

**Documents.** `ARCHITECTURE.md` §4: buckets "reach the database only once the hour completes". `SPEC.md` §6.3: history returns "one row per asset per hour that overlaps the requested period".

**Why under-determined.** A request with `to = now` therefore never includes the hour in progress.

**Decision.** Completed hours only. `hourly_aggregate` rows are written once, when their hour seals, and never updated.

**Cost.** The newest history row is 0–59 minutes behind. Accepted: rows are immutable, which makes the table trivial to reason about and impossible to disagree with the sealed buckets.

### 1.5 Burst writes — the catch-up rule

**Documents.** `ARCHITECTURE.md` §4: `asset_state` is "upserted on every processed message", and all four rows are rewritten in one transaction because `priorityRank` is cross-asset.

**Why under-determined.** Taken literally, the backfill's ~5760 messages produce ~5760 four-row transactions (~23 000 row writes) before the API can serve a full window.

**Decision.** A general catch-up rule, not a backfill special case:

```
if (Duration.between(message.timestamp, Instant.now()) > 2 minutes)
    accumulate into memory only          // catch-up
else
    accumulate and upsert asset_state
```

The `@Scheduled(fixedRate = 5000)` refresh publishes whatever is in memory regardless, so nothing is lost — the burst is simply not written 1440 times per asset.

**Cost.** One extra condition in the consumer, and a wording change in `ARCHITECTURE.md` §4 (Phase 5). Boot reaches a fully queryable state in seconds instead of a minute or more.

### 1.6 OPEN — the §3.2 ranges saturate the §5.1 rolling thresholds

This is a finding, not a decision. It needs a SPEC answer before or during Phase 2.

The §3.2 state machine has a stationary time split of roughly **72 % `WORKING`**, 8 % `TRANSPORT`, 20 % `IDLE` — about 17.3 working hours in any full 24-hour window. Combined with the §3.2 field ranges, the rolling counters over a *full* window come out as:

| Counter | Driver | Expected in 24 h | `MODERATE` | `SEVERE` |
|---------|--------|------------------|------------|----------|
| `highLoadHours` | `loadFactor ≥ 0.75`, i.e. 40 % of the uniform 0.45–0.95 working range | ≈ 6.9 h | 1.5 h | 2.5 h |
| `vibrationAlertHours` | `ELEVATED` at `loadFactor ≥ 0.85` (20 % of working) ∪ `HIGH` excursions (≈ 20 %) | ≈ 6 h | 1.0 h | 2.0 h |
| `tempExcursionHours` | §1.2 excursion duty ≈ 30 % of working | ≈ 5 h | 0.8 h | 1.5 h |

All three sit far above their `SEVERE` bars. Two of the three follow from §3.2's own ranges and are independent of the §1.2 decision. The consequence is that once the backfill fills a full window, **all four assets report three `SEVERE` flags, `CRITICAL` status, and an identical flag profile** — so §5.4's ranking is decided entirely by criterion 3 (the counter sum) and criterion 4 (`assetId`), and §4.4's stated intent that "the priority ranking is meaningful on first load without any asset being permanently `CRITICAL`" does not hold.

Three ways out, in increasing order of SPEC surgery:

1. **Shorten the backfill** to e.g. 6 hours. The window fills partially, counters land nearer the thresholds, and flags climb during the session. Cheapest — one property — but the window then reports a coverage well under 24 h until the session catches up.
2. **Lower the working `loadFactor` distribution** in §3.2 (e.g. 0.35–0.85, and differentiate per asset) so `highLoadHours` and `vibrationAlertHours` land in the 1–3 h band and the four assets differ from each other.
3. **Raise the §5.1 rolling thresholds** to match the simulator the SPEC already describes.

Option 1 is reversible and touches no governed rule, so it is the recommended first move: implement Phase 2 as specified, run it, then set `simulator.backfill.hours` from the observed numbers. Options 2 and 3 are SPEC changes and belong in Phase 5 if chosen.

---

## 2. Phase 1 — Schema and docker

### 2.1 `docker/database-script.sql` (new file)

Hand-written DDL; `spring.jpa.hibernate.ddl-auto=none` stays as it is in both datasource modules.

```sql
CREATE TABLE asset_state (
    asset_id                          text PRIMARY KEY,

    -- SPEC §4.1 counters, lifetime scope (§4.4 baselines + this run)
    lifetime_engine_hours             numeric NOT NULL,
    lifetime_total_working_hours      numeric NOT NULL,
    lifetime_transport_hours          numeric NOT NULL,
    lifetime_idle_hours               numeric NOT NULL,
    lifetime_loaded_hours             numeric NOT NULL,
    lifetime_high_load_hours          numeric NOT NULL,
    lifetime_temp_excursion_hours     numeric NOT NULL,
    lifetime_vibration_alert_hours    numeric NOT NULL,

    -- SPEC §4.1 counters, rolling 24 h scope (§4.3)
    rolling_24h_engine_hours          numeric NOT NULL,
    rolling_24h_total_working_hours   numeric NOT NULL,
    rolling_24h_transport_hours       numeric NOT NULL,
    rolling_24h_idle_hours            numeric NOT NULL,
    rolling_24h_loaded_hours          numeric NOT NULL,
    rolling_24h_high_load_hours       numeric NOT NULL,
    rolling_24h_temp_excursion_hours  numeric NOT NULL,
    rolling_24h_vibration_alert_hours numeric NOT NULL,

    -- SPEC §4.5
    window_coverage_hours             numeric NOT NULL,
    utilization_percent               numeric NULL,
    productive_share_percent          numeric NULL,

    -- SPEC §5.2, §5.3, §5.4, §5.5
    status                            text NOT NULL,
    live_condition                    text NOT NULL,
    recommendation                    text NOT NULL,
    priority_rank                     integer NOT NULL,

    -- SPEC §5.1 flags; NULL severity means the flag is inactive
    heavy_recent_use_severity         text NULL,
    heavy_recent_use_value            numeric NULL,
    heavy_recent_use_threshold        numeric NULL,
    thermal_load_severity             text NULL,
    thermal_load_value                numeric NULL,
    thermal_load_threshold            numeric NULL,
    mechanical_attention_severity     text NULL,
    mechanical_attention_value        numeric NULL,
    mechanical_attention_threshold    numeric NULL,
    high_cumulative_load_severity     text NULL,
    high_cumulative_load_value        numeric NULL,
    high_cumulative_load_threshold    numeric NULL,

    -- SPEC §3.1 most recent message; all NULL until the asset reports
    latest_timestamp                  timestamptz NULL,
    latest_pto_engaged                boolean NULL,
    latest_ground_speed_kmh           numeric NULL,
    latest_load_factor                numeric NULL,
    latest_temperature_c              numeric NULL,
    latest_vibration_level            text NULL,
    latest_working_status             text NULL,

    updated_at                        timestamptz NOT NULL
);

CREATE TABLE hourly_aggregate (
    asset_id             text NOT NULL,
    hour_start           timestamptz NOT NULL,
    engine_hours         numeric NOT NULL,
    total_working_hours  numeric NOT NULL,
    transport_hours      numeric NOT NULL,
    idle_hours           numeric NOT NULL,
    loaded_hours         numeric NOT NULL,
    high_load_hours      numeric NOT NULL,
    temp_excursion_hours numeric NOT NULL,
    vibration_alert_hours numeric NOT NULL,
    PRIMARY KEY (asset_id, hour_start)
);

CREATE INDEX idx_hourly_aggregate_hour_start ON hourly_aggregate (hour_start);
```

`hour_start` is the UTC instant at which the `Europe/Vienna` local hour began (`SPEC.md` §4.3, §6.3). The composite primary key is the natural one and makes the "one row per asset per hour" rule a database guarantee rather than a convention.

### 2.2 `docker/docker-compose.yml`

Add one mount to the `database` service, next to the existing `database-data` volume:

```yaml
      - ./database-script.sql:/docker-entrypoint-initdb.d/database-script.sql:ro
```

### 2.3 Done when

```
cd docker
docker compose down -v          # -v is required: database-data outlives a plain down
docker compose up -d
```

`\d asset_state` and `\d hourly_aggregate` both resolve in psql.

---

## 3. Phase 2 — Simulator

Package `com.mariolagger.simulator`. No web server (`spring.main.web-application-type=none`, already set).

### 3.1 `dto/`

| File | Contents |
|------|----------|
| `TelemetryMessage.java` | `assetId`, `timestamp` (`Instant`), `ptoEngaged`, `groundSpeedKmh`, `loadFactor`, `temperatureC`, `vibrationLevel`, `workingStatus`. Hand-written POJO: no-arg constructor, explicit getters/setters. |
| `AssetId.java` | `TRACTOR`, `MOWER`, `BALER`, `MIXER` |
| `VibrationLevel.java` | `NORMAL`, `ELEVATED`, `HIGH` |
| `WorkingStatus.java` | `WORKING`, `TRANSPORT`, `IDLE` |

### 3.2 `service/business/AssetSimulationState.java`

Per-asset mutable state, one instance per asset, in memory only:

- `assetId`, current `WorkingStatus`, `stateEndsAt` (`Instant`, in message-timestamp time)
- `temperatureC` (carried between messages so it can drift smoothly)
- `thermalExcursionEndsAt`, `vibrationExcursionEndsAt` (`Instant`, null when not in one)
- `lastMessageTimestamp` (`Instant`, for Δt)
- `nextDueAt` (`Instant`, real wall-clock, used only by the live scheduler)

### 3.3 `service/business/TelemetryGenerator.java`

One entry point, `TelemetryMessage generate(AssetSimulationState state, Instant timestamp)`, used by **both** the backfill and the live scheduler. The message timestamp is always the clock, so one code path covers both phases.

State machine (`SPEC.md` §3.2):

| State | Dwell | Next state |
|-------|-------|------------|
| `WORKING` | 20–60 min | `TRANSPORT` 60 % / `IDLE` 40 % |
| `TRANSPORT` | 3–10 min | `WORKING` 70 % / `IDLE` 30 % |
| `IDLE` | 5–30 min | `WORKING` 80 % / `TRANSPORT` 20 % |

Field values:

| State | `ptoEngaged` | `groundSpeedKmh` | `loadFactor` |
|-------|--------------|------------------|--------------|
| `WORKING` | `true` | `TRACTOR` 6–14, `MOWER` 8–13, `BALER` 6–10, `MIXER` 3–5 | 0.45–0.95 |
| `TRANSPORT` | `false` | 12–30 (`MIXER` 8–18) | 0.15–0.35 |
| `IDLE` | `false` | 0 | 0.05–0.15 |

Temperature drifts towards 70–85 °C while `WORKING` and towards 45–60 °C while `IDLE`; during a thermal excursion it is 91–98 °C. Vibration is `NORMAL` by default, `ELEVATED` while `loadFactor ≥ 0.85`, `HIGH` during a vibration excursion. Excursions start per §1.2 and last 2–15 minutes.

`workingStatus` is emitted straight from the state machine, which keeps `SPEC.md` §3.1's guarantee (`WORKING` exactly when `ptoEngaged` and `groundSpeedKmh > 2`) true for every listed range.

### 3.4 `service/messaging/TelemetryPublisher.java`

Constructor-injected `RabbitTemplate`; one method `publish(TelemetryMessage)` calling `convertAndSend("telemetry", message)` against the default exchange.

### 3.5 `service/business/BackfillService.java`

On `ApplicationReadyEvent`: for each asset, step from `now − backfillHours` to `now` in exact 60 s increments, calling `TelemetryGenerator.generate` and publishing each message. Property `simulator.backfill.hours`, default `24`, `0` disables the burst.

The first message of each asset credits nothing (§4.2: Δt undefined for the first message), so a 24-hour backfill credits 1439 × 60 s ≈ 23.98 h.

### 3.6 `scheduler/TelemetryScheduler.java`

`@Scheduled(fixedDelay = 1000)` tick over the four assets. Each asset carries its own `nextDueAt`; when it passes, generate with `timestamp = Instant.now()`, publish, and set `nextDueAt` 5–10 s ahead. A single tick keeps the independence required by §3.1 without four separate scheduled methods.

### 3.7 `SimulatorApplication.java`

Add a JSON `MessageConverter` bean. The converter builds its own `ObjectMapper`, so the Java-time module must be registered explicitly and date-as-timestamp disabled — otherwise `Instant` serializes as a numeric epoch and breaks the §3.1 contract. The existing `telemetryQueue()` bean and `@EnableScheduling` stay.

### 3.8 Done when

RabbitMQ management UI (`localhost:15672`, admin/admin) shows ~5760 messages queued within seconds of boot, then a steady ~0.5 msg/s. One message inspected in the UI matches the §3.1 shape with an ISO-8601 `timestamp`.

---

## 4. Phase 3 — TelemetryProcessor

Package `com.mariolagger.telemetryprocessor`. The only consumer, and the only place the §4.2 accumulation rule exists.

### 4.1 `dto/`

Its own copy of `TelemetryMessage` and the three enums. `ARCHITECTURE.md` §2 accepts this duplication explicitly — there is no shared library module.

### 4.2 `entity/`

| File | Notes |
|------|-------|
| `AssetState.java` | `@Entity @Table(name = "asset_state")`, explicit `@Column(name = ...)` on all 44 columns, `@Id` on `assetId`. Enum-backed columns use `@Enumerated(EnumType.STRING)`. |
| `HourlyAggregate.java` | `@Entity @Table(name = "hourly_aggregate")`, `@IdClass(HourlyAggregateId.class)`, `@Id` on both `assetId` and `hourStart`. |
| `HourlyAggregateId.java` | `Serializable`, two fields, no-arg constructor, `equals`/`hashCode`. Required by `@IdClass`. |

### 4.3 `repository/`

`AssetStateRepository extends JpaRepository<AssetState, String>` and `HourlyAggregateRepository extends JpaRepository<HourlyAggregate, HourlyAggregateId>`.

### 4.4 `service/business/`

**`CounterSet.java`** — the eight counters as doubles, plus `addSeconds(double seconds, boolean[] conditions)` or eight explicit adders, plus `add(CounterSet other)`. Used for both hourly buckets and the lifetime total.

**`AssetAccumulator.java`** — everything one asset needs in memory:
- `lastProcessedTimestamp` (`Instant`)
- `lifetime` (`CounterSet`, seeded from `AssetBaselines`)
- `buckets` (`Map<Instant, CounterSet>` keyed by Vienna hour start)
- `firstMessageTimestamp` (for §4.5 `windowCoverageHours`)
- `latestMessage` and `latestArrivalRealTime` (arrival wall-clock, for §5.3 `STALE`)

**`AssetBaselines.java`** — the `SPEC.md` §4.4 table as constants:

| Asset | engine | working | transport | idle | loaded | highLoad | tempExc | vibAlert |
|-------|--------|---------|-----------|------|--------|----------|---------|----------|
| `TRACTOR` | 6200 | 3400 | 1500 | 1300 | 980 | 410 | 26 | 58 |
| `MOWER` | 950 | 640 | 210 | 100 | 310 | 95 | 4 | 12 |
| `BALER` | 1480 | 1010 | 300 | 170 | 620 | 240 | 11 | 46 |
| `MIXER` | 1120 | 860 | 60 | 200 | 415 | 130 | 7 | 19 |

Lifetime scope only; the rolling window always starts empty.

**`CounterAccumulationService.java`** — owns the four accumulators and `private static final ZoneId FARM_ZONE = ZoneId.of("Europe/Vienna");`.

`SPEC.md` §4.2, in order:
1. `timestamp <= lastProcessedTimestamp` → discard the message entirely, update nothing.
2. Δt = `timestamp − lastProcessedTimestamp` in seconds; undefined for the first message.
3. Δt undefined or Δt > 60 s → credit nothing, store the timestamp, continue.
4. Otherwise add Δt / 3600 to every counter whose §4.1 condition holds **for this message**, into the bucket containing this message's timestamp.

§4.1 conditions, evaluated on the current message only:

| Counter | Condition |
|---------|-----------|
| `engineHours` | always |
| `totalWorkingHours` | `ptoEngaged && groundSpeedKmh > 2` ("productive work") |
| `transportHours` | not productive **and** `workingStatus == TRANSPORT` |
| `idleHours` | not productive **and** `workingStatus != TRANSPORT` |
| `loadedHours` | productive **and** `loadFactor >= 0.55` |
| `highLoadHours` | productive **and** `loadFactor >= 0.75` |
| `tempExcursionHours` | `temperatureC > 90` (not gated on productive work) |
| `vibrationAlertHours` | `vibrationLevel` is `ELEVATED` or `HIGH` (not gated) |

The three-way `totalWorkingHours` / `transportHours` / `idleHours` branch must be written as one if/else-if/else so the §4.1 invariant `engineHours = working + transport + idle` holds by construction.

Also provides:
- **Bucket sealing** — a bucket is complete once the current Vienna hour has moved past it. Sealed buckets are handed to `AssetStatePublisher` for flushing. The 5 s refresh must also seal, so an asset that stops reporting still gets its final hour written.
- **Bucket pruning** — keep the open bucket plus the last 24; older ones are dropped after sealing.
- **Rolling 24 h** (§4.3) — the open bucket plus the 23 completed buckets before it, summed. Coverage is deliberately 23–24 h.
- **§4.5 derived figures** — `windowCoverageHours` = from the later of (start of the oldest included bucket) and (the asset's first message in this run) up to now; `utilizationPercent` = `totalWorkingHours`(24 h) / `windowCoverageHours` × 100; `productiveSharePercent` = `totalWorkingHours`(24 h) / `engineHours`(24 h) × 100. Both `null` when their denominator is 0.

**`FlagEvaluationService.java`** — `SPEC.md` §5.1. Eight threshold rows collapse into at most four flags, each at its highest met severity:

| Flag | Scope | `MODERATE` | `SEVERE` |
|------|-------|------------|----------|
| `HEAVY_RECENT_USE` (`highLoadHours`) | rolling 24 h | ≥ 1.5 h | ≥ 2.5 h |
| `THERMAL_LOAD` (`tempExcursionHours`) | rolling 24 h | ≥ 0.8 h | ≥ 1.5 h |
| `MECHANICAL_ATTENTION` (`vibrationAlertHours`) | rolling 24 h | ≥ 1.0 h | ≥ 2.0 h |
| `HIGH_CUMULATIVE_LOAD` (`loadedHours`) | lifetime | ≥ 850 h | ≥ 1200 h |

Returns the counter value and the threshold actually crossed, so the reason survives into the response.

**`AssetAssessmentService.java`** — `SPEC.md` §5.2, §5.3, §5.5.

Status, counting each active flag once at its current severity:
- `CRITICAL` — at least one `SEVERE`, or two or more `MODERATE`
- `WATCH` — exactly one `MODERATE` and no `SEVERE`
- `GOOD` — no active flag

Live condition, from the latest message only, first match wins:
- `STALE` — nothing received for this asset in the last 60 s (measured on **arrival wall-clock**, not on the message timestamp)
- `ALERT` — `vibrationLevel == HIGH` or `temperatureC > 95`
- `CAUTION` — `vibrationLevel == ELEVATED` or `temperatureC > 90` or `loadFactor >= 0.9`
- `NORMAL` — otherwise

Recommendation strings, verbatim:
- `CRITICAL` → `Priority check recommended within the next 5 engine hours.`
- `WATCH` → `Plan inspection within the next 15–20 engine hours.`
- `GOOD` → `No inspection recommended at this time.`

`liveCondition` never feeds flags, status, ranking or recommendation.

**`FleetRankingService.java`** — `SPEC.md` §5.4. Sort all four assets by, in order: `SEVERE` count descending, `MODERATE` count descending, `highLoadHours + tempExcursionHours + vibrationAlertHours` (all 24 h) descending, `assetId` ascending. Assign 1-based `priorityRank`.

**`AssetStatePublisher.java`** — assembles the four `AssetState` rows from the services above and saves them in **one transaction** (`priorityRank` is cross-asset; upserting one row alone would transiently leave two rows claiming the same rank). Also flushes sealed buckets to `hourly_aggregate`.

Startup reset lives here, in a `@PostConstruct`: delete both tables, then write the §4.4 baselines with zeroed rolling counters, `liveCondition` `STALE` and both percentages `null`.

> **Ordering gotcha.** This must be `@PostConstruct`, not a `CommandLineRunner` or `ApplicationRunner`. Rabbit listener containers are `SmartLifecycle` beans started inside `finishRefresh()`, which happens *before* runners execute — a runner-based reset could delete state the consumer has already accumulated.

### 4.5 `service/messaging/TelemetryConsumer.java`

`@RabbitListener(queues = "telemetry")`. Hands the message to `CounterAccumulationService`, then applies the §1.5 catch-up rule: if the message timestamp is more than 2 minutes behind wall-clock, skip the `asset_state` upsert and let the 5 s refresh publish.

### 4.6 `scheduler/AssetStateRefreshScheduler.java`

`@Scheduled(fixedRate = 5000)` calling `AssetStatePublisher`. Required because three groups of values move with wall-clock rather than with message arrival: the rolling window edge, `windowCoverageHours` and the percentages, and `STALE`.

### 4.7 `TelemetryProcessorApplication.java`

Same JSON `MessageConverter` bean as the Simulator. Existing `telemetryQueue()` bean and `@EnableScheduling` stay.

### 4.8 Done when

```sql
SELECT asset_id, status, priority_rank, rolling_24h_high_load_hours FROM asset_state ORDER BY priority_rank;
SELECT count(*) FROM hourly_aggregate;    -- ≈ 96 after a 24 h backfill (4 assets × 24 h)
SELECT asset_id, sum(engine_hours) FROM hourly_aggregate GROUP BY asset_id;
```

Four rows with distinct ranks 1–4, values that change between polls, and per-asset hourly sums close to 24.

---

## 5. Phase 4 — OperatorApi

Package `com.mariolagger.operatorapi`, port 8083. Reads two tables and maps rows onto the §6 shapes. It derives no product rule: no threshold, no percentage, no status, no recommendation is computed here.

### 5.1 `entity/` and `repository/`

Read-side copies of `AssetState`, `HourlyAggregate` and `HourlyAggregateId`.

- `AssetStateRepository.findAllByOrderByPriorityRankAsc()`
- History overlap — a row covering `[hourStart, hourStart + 1h)` overlaps `[from, to)` exactly when `hourStart < to && hourStart > from − 1h`:
  - `findByHourStartLessThanAndHourStartGreaterThanOrderByHourStartAscAssetIdAsc(Instant to, Instant fromMinusOneHour)`
  - `findByAssetIdAndHourStartLessThanAndHourStartGreaterThanOrderByHourStartAscAssetIdAsc(...)`

### 5.2 `dto/`

`FleetResponse`, `AssetView`, `FlagView`, `CounterSetView`, `HistoryResponse`, `HistoryRow`, `LatestTelemetryResponse`, `LatestTelemetryEntry`, `TelemetryView`, plus the enums it serializes. Field names exactly as `GLOSSARY.md` and the §6 examples: `lifetimeCounters`, `rolling24hCounters`, `windowCoverageHours`, `utilizationPercent`, `productiveSharePercent`, `priorityRank`, `liveCondition`, `generatedAt`, `hourStart`.

### 5.3 `service/business/AssetViewService.java`

Row → DTO mapping. Applies the §1.3 flag lookup, omits flags whose severity column is NULL (there is no `INACTIVE` value), and rounds to 2 decimals at the boundary:

```java
private static double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
}
```

Exact `numeric` stays in the database so hourly rows sum correctly; only the response is rounded. `Math.round` on a `double` reproduces every §6 example (`981.4`, `26.0`, `0.82`).

`getAsset(String assetId)` throws `ResponseStatusException(HttpStatus.NOT_FOUND, ...)` when the id is not one of the four assets — thrown from the service, no `@ControllerAdvice`.

Assets that never reported are still listed: §4.4 baselines as `lifetimeCounters`, zeroed `rolling24hCounters`, `liveCondition` `STALE`, both percentages `null`.

### 5.4 `service/business/HistoryService.java`

`SPEC.md` §6.3 validation, all via `ResponseStatusException`:
- `400` — `from` or `to` missing or unparseable, `from` not before `to`, or the range exceeds 7 days
- `404` — `assetId` given but not one of the four assets
- an empty `rows` array is a valid `200`

Counter values are the amount accrued **within** that hour, not running totals. Rows ordered by `hourStart` ascending then `assetId` ascending.

### 5.5 `controller/`

| File | Mappings |
|------|----------|
| `AssetController.java` | `GET /api/assets`, `GET /api/assets/{assetId}` |
| `HistoryController.java` | `GET /api/history` |
| `TelemetryController.java` | `GET /api/telemetry/latest` |

Full `/api/...` paths and **no servlet context path** — nginx passes `/api/` through unchanged, so a context path would produce `/api/api/assets`.

`GET /api/telemetry/latest` returns all four assets ordered by `assetId` ascending, with `telemetry` `null` and `liveCondition` `STALE` for any asset that has not reported.

### 5.6 Done when

All four endpoints match the §6 example shapes, and `/api/assets/NOPE`, a missing `from`, a reversed range and an 8-day range each return Spring's default error body with the right status.

---

## 6. Phase 5 — Documents

`SPEC.md` §0's change protocol: edit the owning file, then scan the others for stale content in the same pass.

1. **`SPEC.md` §3.1 / §3.2** — add the startup backfill (§1.1) and replace "roughly a 5 % chance per working interval" / "roughly a 3 % chance" with the Δt-scaled rate (§1.2). If §1.6 is resolved by changing the ranges or thresholds, that lands here and in §5.1 too.
2. **`ARCHITECTURE.md` §4** — qualify "upserted on every processed message" with the catch-up rule (§1.5).
3. **`CLAUDE.md`** — rewrite the "Repo status: skeleton mid-rewrite" section; drop the notes that `docker/database-script.sql` does not exist, that no init script is mounted, and that `OperatorApi` has no controllers. Keep the `docker compose down -v` warning and move it into the commands section, since a pre-existing `database-data` volume will not run the new init script.
4. **Pre-completion check** — no rule or name defined in two governed files; all four consistent. This document is exempt by its own header but must be corrected wherever it now contradicts them.

---

## 7. Out of scope

- **Per-service unit tests.** `CLAUDE.md`'s coding-style section mandates JUnit 5 + Mockito test classes per service and consumer. None are planned here; this is a known, deliberate gap to be planned separately. The three existing `contextLoads()` smoke tests must stay green throughout.
- **Frontend.** `frontend/` still contains the previous project's solar GUI calling `/energy/*`. It is rebuilt separately against `/api/*` using the `GLOSSARY.md` §8 component names.
- Everything under `SPEC.md` §8.

---

## 8. Known risks

- **Message converter class name.** Spring Boot 4 / Spring AMQP 4 moved to Jackson 3, which renamed the JSON message converter. Verify the class on the classpath before writing the bean in §3.7 and §4.7. If the Java-time wiring proves awkward, the fallback is to carry `timestamp` as an ISO-8601 `String` in the DTO and parse it in the processor — §3.1 types it as a string anyway.
- **Directory casing.** The working tree has lowercase module directories (`simulator/`, `frontend/`) while git and the build files use PascalCase (`Simulator`, `../Frontend` in `docker-compose.yml`). Windows resolves either; a Linux checkout would not. Worth a `git mv` pass, but it is not part of this plan.
- **`@SpringBootTest` needs infra.** `TelemetryProcessor` and `OperatorApi` context-load tests require Postgres at `localhost:5432`, and `TelemetryProcessor` additionally needs RabbitMQ, because its listener container starts with the context. Start the docker infra first — this is expected, not a bug.
- **`hourly_aggregate` growth.** Nothing prunes the table during a long run; ~96 rows per day is harmless for a demo, but it grows without bound.

---

## 9. Verification

1. `cd docker && docker compose down -v && docker compose up -d`; in psql, `\d asset_state` and `\d hourly_aggregate`.
2. Run `Simulator` — RabbitMQ UI at `localhost:15672` shows the backfill burst, then a steady trickle. Inspect one message against the §3.1 shape.
3. Run `TelemetryProcessor` — the §4.8 queries: four ranked rows that move between polls, ~96 hourly rows, per-asset hourly sums near 24.
4. Run `OperatorApi` and check each endpoint plus its error cases:
   ```
   curl localhost:8083/api/assets
   curl localhost:8083/api/assets/TRACTOR
   curl -i localhost:8083/api/assets/NOPE                       # 404
   curl localhost:8083/api/telemetry/latest
   curl "localhost:8083/api/history?from=2026-08-13T06:00:00Z&to=2026-08-13T09:00:00Z"
   curl -i "localhost:8083/api/history?to=2026-08-13T09:00:00Z"  # 400 missing from
   curl -i "localhost:8083/api/history?from=2026-08-13T09:00:00Z&to=2026-08-13T06:00:00Z"  # 400 reversed
   curl -i "localhost:8083/api/history?from=2026-08-01T00:00:00Z&to=2026-08-13T00:00:00Z"  # 400 over 7 days
   ```
5. Through nginx: `curl localhost:8080/api/assets` — confirms the `/api` prefix is proxied without being doubled.
6. `mvn test` from the repo root — the three `contextLoads()` tests stay green.
7. Invariant spot-check on any asset: `engineHours == totalWorkingHours + transportHours + idleHours`, and `totalWorkingHours >= loadedHours >= highLoadHours`, in both scopes.
