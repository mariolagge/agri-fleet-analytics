# docs\IMPLEMENTATION-PROMPTS.md

> **Non-normative.** Paste-ready prompts for driving an AI coding agent through `IMPLEMENTATION-PLAN.md`. Like that document, this one sits outside the `SPEC.md` §0 hierarchy — where a prompt and a governed document disagree, the governed document wins and the prompt is wrong.

---

## How to use this file

**One prompt per session.** Each prompt is self-contained and produces a unit you can verify before starting the next. Do not paste two at once — a long agent session drifts, and a failed Phase 3B is far easier to diagnose when Phase 3A is already known-good.

**Run the "Done when" check before moving on.** Every prompt ends with a command or query that either passes or does not. If it does not pass, fix it in the same session rather than carrying the problem forward.

**Order matters.** 1 → 2A → 2B → 3A → 3B → 3C → 4A → 4B → C → 5. Prompt R can run at any point after 4B.

**The `Stop and ask if` list is the important part.** `CLAUDE.md` requires clarifying questions over assumptions. Each prompt names the specific things worth stopping for in that phase; an agent that guesses instead of asking has failed the prompt even if the code compiles.

**Windows.** Commands below use `mvnw.cmd`. Each module is built and run from its own directory.

---

## Prompt 1 — Schema and docker

````
Implement Phase 1 of docs/IMPLEMENTATION-PLAN.md: the database schema and its docker wiring.

READ FIRST
- docs/IMPLEMENTATION-PLAN.md §2 (the full DDL is written out there — use it)
- docs/ARCHITECTURE.md §4 (persistence)
- docs/GLOSSARY.md §1 (database columns are snake_case)
- docker/docker-compose.yml as it exists today

CREATE
- docker/database-script.sql — tables asset_state and hourly_aggregate, exactly as
  specified in IMPLEMENTATION-PLAN.md §2.1

EDIT
- docker/docker-compose.yml — add this mount to the `database` service's volumes list,
  alongside the existing database-data volume:
      - ./database-script.sql:/docker-entrypoint-initdb.d/database-script.sql:ro
  Change nothing else in this file.

DO NOT
- Do not write any Java. This phase is SQL and YAML only.
- Do not add Flyway, Liquibase, or any migration tool.
- Do not enable Hibernate auto-DDL anywhere; ddl-auto=none stays as it is.
- Do not add tables beyond the two named. ARCHITECTURE.md §4 lists exactly two.

KEY POINTS
- asset_state: asset_id is the primary key. 8 lifetime_* + 8 rolling_24h_* counter
  columns, all numeric NOT NULL. window_coverage_hours numeric NOT NULL;
  utilization_percent and productive_share_percent numeric NULL (SPEC §4.5 makes them
  null when their denominator is 0). status / live_condition / recommendation text NOT
  NULL, priority_rank integer NOT NULL. Four flags x 3 columns (*_severity text NULL,
  *_value numeric NULL, *_threshold numeric NULL) — NULL severity means inactive.
  Seven nullable latest_* telemetry columns. updated_at timestamptz NOT NULL.
- hourly_aggregate: PRIMARY KEY (asset_id, hour_start), 8 numeric NOT NULL counter
  columns, plus an index on hour_start.
- hour_start is timestamptz holding the UTC instant at which the Europe/Vienna local
  hour began (SPEC §4.3).
- Counter columns are numeric, never float or double precision — values are exact in
  the database and only rounded at the API boundary.

STOP AND ASK IF
- The DDL in IMPLEMENTATION-PLAN.md §2.1 contradicts ARCHITECTURE.md §4 in any way.
- You believe a column is missing for something SPEC §6.1 needs in its response.

DONE WHEN
  cd docker
  docker compose down -v
  docker compose up -d
then, in psql against agri-fleet-analytics-db, both `\d asset_state` and
`\d hourly_aggregate` resolve with the column sets above.

Note: `down -v` is required. The database-data volume outlives a plain `down`, and an
init script only runs when the database is first created.
````

---

## Prompt 2A — Simulator: domain and generator

````
Implement the first half of Phase 2 of docs/IMPLEMENTATION-PLAN.md: the Simulator's
telemetry domain types and its state machine. No messaging, no scheduling yet.

READ FIRST
- docs/SPEC.md §2 (assets), §3.1 (message shape), §3.2 (simulator behaviour)
- docs/GLOSSARY.md §2, §3 (names and values)
- docs/IMPLEMENTATION-PLAN.md §3.1-§3.3 and §1.2 (the excursion-rate decision)
- CLAUDE.md (coding style — it is binding)

CREATE under Simulator/src/main/java/com/mariolagger/simulator/
- dto/TelemetryMessage.java  — assetId, timestamp (Instant), ptoEngaged,
  groundSpeedKmh, loadFactor, temperatureC, vibrationLevel, workingStatus
- dto/AssetId.java           — TRACTOR, MOWER, BALER, MIXER
- dto/VibrationLevel.java    — NORMAL, ELEVATED, HIGH
- dto/WorkingStatus.java     — WORKING, TRANSPORT, IDLE
- service/business/AssetSimulationState.java
- service/business/TelemetryGenerator.java

DO NOT
- Do not add Lombok. Hand-write the no-arg constructor, getters and setters.
- Do not add SLF4J or any logging framework; use System.out.println if you log at all.
- Do not create a RabbitTemplate, a publisher, or a @Scheduled method yet.
- Do not build a physics model. SPEC §3.2's ranges are the contract, nothing more.
- Do not vary any range or probability that SPEC §3.2 fixes.

RULES TO ENCODE (SPEC §3.2)
State machine, per asset, independently:
  WORKING   dwell 20-60 min -> TRANSPORT 60% / IDLE 40%
  TRANSPORT dwell 3-10 min  -> WORKING 70%  / IDLE 30%
  IDLE      dwell 5-30 min  -> WORKING 80%  / TRANSPORT 20%
Field values per state:
  WORKING    ptoEngaged true,  speed TRACTOR 6-14 / MOWER 8-13 / BALER 6-10 / MIXER 3-5,
             loadFactor 0.45-0.95
  TRANSPORT  ptoEngaged false, speed 12-30 (MIXER 8-18), loadFactor 0.15-0.35
  IDLE       ptoEngaged false, speed 0,                  loadFactor 0.05-0.15
temperatureC drifts smoothly towards 70-85 while WORKING and towards 45-60 while IDLE;
during a thermal excursion it is 91-98. Carry the value between messages so the drift
is actually smooth.
vibrationLevel is NORMAL by default, ELEVATED while loadFactor >= 0.85, HIGH during a
vibration excursion.
Excursions last 2-15 minutes and, once started, run to their end regardless of state
changes ("independently of the state cycle").

EXCURSION TRIGGER — this is a resolved ambiguity, see IMPLEMENTATION-PLAN.md §1.2.
Evaluate only while the asset is WORKING and not already inside an excursion of that
kind, with the probability scaled to the message's own delta:
    thermalChance   = 0.05 * deltaSeconds / 60.0
    vibrationChance = 0.03 * deltaSeconds / 60.0
This keeps the rate identical in the backfill (delta 60 s) and live (delta ~7.5 s)
phases. Do not use a flat per-message probability.

CRITICAL DESIGN POINT
TelemetryGenerator must expose exactly one entry point:
    TelemetryMessage generate(AssetSimulationState state, Instant timestamp)
The message timestamp is always the clock — the state machine advances against it, and
deltaSeconds comes from it. This single path is what lets the backfill and the live
scheduler share all the logic in Prompt 2B. Do not add a second generate method or a
mode flag.

CONSISTENCY REQUIREMENT
SPEC §3.1 guarantees workingStatus is WORKING exactly when ptoEngaged is true and
groundSpeedKmh > 2. Emit workingStatus straight from the state machine and verify by
inspection that every speed range above keeps that guarantee true.

STOP AND ASK IF
- A SPEC §3.2 range is ambiguous for a specific asset.
- You are unsure whether an excursion should be able to start while an asset is
  TRANSPORT or IDLE.
- Smooth temperature drift seems to require state SPEC does not describe.

DONE WHEN
  cd Simulator
  mvnw.cmd test
compiles and the existing contextLoads() test still passes. No behaviour is observable
yet — that arrives in Prompt 2B.
````

---

## Prompt 2B — Simulator: publishing, backfill, scheduling

````
Implement the second half of Phase 2 of docs/IMPLEMENTATION-PLAN.md: publish the
generated telemetry to RabbitMQ, including the startup backfill burst.

READ FIRST
- docs/IMPLEMENTATION-PLAN.md §1.1 (why the backfill exists) and §3.4-§3.8
- docs/ARCHITECTURE.md §3 (messaging)
- docs/SPEC.md §3.1 (cadence), §4.2 (why 60 s exactly, not 61)
- The code you wrote in Prompt 2A

CREATE under Simulator/src/main/java/com/mariolagger/simulator/
- service/messaging/TelemetryPublisher.java
- service/business/BackfillService.java
- scheduler/TelemetryScheduler.java

EDIT
- SimulatorApplication.java — add a JSON MessageConverter bean. Keep the existing
  telemetryQueue() bean and @EnableScheduling exactly as they are.

EDIT
- Simulator/src/main/resources/application.properties — add simulator.backfill.hours=24

DO NOT
- Do not declare an exchange or a binding. ARCHITECTURE.md §3: one queue, default
  exchange, one consumer.
- Do not add a second queue, and do not add any consumer to this module.
- Do not persist simulator state. SPEC §3.2: in-memory only, fresh on every run.
- Do not use field injection. Constructor injection only.

TELEMETRY PUBLISHER
Constructor-injected RabbitTemplate, one method publish(TelemetryMessage) calling
convertAndSend("telemetry", message) against the default exchange.

MESSAGE CONVERTER — verify before you write it
Spring Boot 4 / Spring AMQP 4 moved to Jackson 3 and renamed the JSON message
converter. Check the actual class name on the classpath rather than assuming
Jackson2JsonMessageConverter. Whichever it is, the converter builds its own ObjectMapper,
so you must register the Java-time module and disable write-dates-as-timestamps
explicitly — otherwise Instant serializes as a numeric epoch and breaks SPEC §3.1's
"ISO-8601 instant in UTC".
Verify by inspecting one message body in the RabbitMQ UI, not by assuming.

BACKFILL SERVICE
On ApplicationReadyEvent, for each of the four assets, step from
(now - simulator.backfill.hours) to now in increments of EXACTLY 60 seconds, calling
TelemetryGenerator.generate and publishing each message.
Why 60 and not more: SPEC §4.2 step 3 discards a delta GREATER than 60 s. 60 s exactly
is credited. This is the entire reason the backfill works — do not round it up.
simulator.backfill.hours defaults to 24; a value of 0 disables the burst.

TELEMETRY SCHEDULER
@Scheduled(fixedDelay = 1000) tick that iterates the four assets. Each asset carries its
own nextDueAt; when it passes, generate with timestamp = Instant.now(), publish, and set
nextDueAt 5-10 seconds ahead. One tick, not four scheduled methods — SPEC §3.1's
"independently of the other assets" is satisfied by the per-asset due time.

STOP AND ASK IF
- The message converter class name cannot be resolved on the classpath. Report what you
  found before choosing a fallback; carrying timestamp as an ISO-8601 String in the DTO
  is the recorded fallback but it is a change worth confirming.
- The backfill's last timestamp and the first live timestamp could be more than 60 s
  apart, which would cost one discarded interval.

DONE WHEN
  cd Simulator
  mvnw.cmd spring-boot:run
The RabbitMQ management UI at localhost:15672 (admin/admin) shows roughly 5760 messages
on queue `telemetry` within seconds of boot (4 assets x 1440), then a steady trickle of
about 0.5 msg/s. Open one message in the UI and confirm it matches the SPEC §3.1 shape
with an ISO-8601 string timestamp.
````

---

## Prompt 3A — TelemetryProcessor: persistence layer

````
Implement the persistence layer of Phase 3 of docs/IMPLEMENTATION-PLAN.md. Entities and
repositories only — no accumulation logic, no consumer.

READ FIRST
- docs/IMPLEMENTATION-PLAN.md §4.1-§4.3 and §2.1 (the DDL you are mapping)
- docs/ARCHITECTURE.md §2 (why the DTO is duplicated), §4 (tables)
- docs/GLOSSARY.md §1 (snake_case columns, camelCase Java fields)
- CLAUDE.md (JPA conventions — explicit mappings, ddl-auto=none)

CREATE under TelemetryProcessor/src/main/java/com/mariolagger/telemetryprocessor/
- dto/TelemetryMessage.java, dto/AssetId.java, dto/VibrationLevel.java,
  dto/WorkingStatus.java  — this module's OWN copy
- entity/AssetState.java
- entity/HourlyAggregate.java
- entity/HourlyAggregateId.java
- repository/AssetStateRepository.java
- repository/HourlyAggregateRepository.java

ON THE DUPLICATED DTO
Yes, this duplicates the Simulator's types. ARCHITECTURE.md §2 states there is no shared
library module and that this duplication is accepted. Do not create one, and do not add
a Maven dependency from this module to Simulator.

ENTITY RULES (CLAUDE.md)
- Explicit @Entity, @Table(name = "..."), @Column(name = "...") on every field.
- Hand-written POJOs: no-arg constructor, explicit getters and setters, no Lombok.
- Enum-backed columns use @Enumerated(EnumType.STRING).
- ddl-auto stays none. The schema is Prompt 1's SQL; the entity maps it, never creates it.

AssetState maps all 44 columns of asset_state, @Id on assetId (String).
HourlyAggregate uses the composite natural key:
    @Entity @Table(name = "hourly_aggregate")
    @IdClass(HourlyAggregateId.class)
    @Id private String assetId;
    @Id private Instant hourStart;
HourlyAggregateId implements Serializable with those two fields, a no-arg constructor,
and equals/hashCode. @IdClass will not work without them.

REPOSITORIES
    AssetStateRepository extends JpaRepository<AssetState, String>
    HourlyAggregateRepository extends JpaRepository<HourlyAggregate, HourlyAggregateId>
No query methods are needed in this module yet — add none until something calls them.

DO NOT
- Do not add a @Repository implementation class or a custom interface layer.
- Do not add JPA relationships between the two entities. They are keyed independently.
- Do not add a converter, an @Embeddable, or a JSON column.

STOP AND ASK IF
- A column in the DDL has no obvious Java type, or a name would violate GLOSSARY §1.
- The 44-column entity seems to want splitting. It does not — flag it, do not do it.

DONE WHEN
  cd TelemetryProcessor
  mvnw.cmd test
compiles, and the contextLoads() test passes with the docker infra running. A Hibernate
mapping error here means a column name disagrees with Prompt 1's DDL — fix the entity,
not the SQL.
````

---

## Prompt 3B — TelemetryProcessor: counter accumulation

````
Implement the counter accumulation of Phase 3 of docs/IMPLEMENTATION-PLAN.md. This is the
heart of the system and the one place SPEC §4.2 exists. Nothing persists yet.

READ FIRST
- docs/SPEC.md §4 in full — §4.1 counters, §4.2 accumulation rule, §4.3 hourly buckets
  and the rolling window, §4.4 baselines, §4.5 utilization
- docs/IMPLEMENTATION-PLAN.md §4.4 (first four classes)
- docs/GLOSSARY.md §4 (counter names — use them exactly, invent no variants)

CREATE under TelemetryProcessor/src/main/java/com/mariolagger/telemetryprocessor/service/business/
- CounterSet.java
- AssetAccumulator.java
- AssetBaselines.java
- CounterAccumulationService.java

DO NOT
- Do not write to the database in this prompt. In-memory only.
- Do not evaluate flags, status, live condition or ranking here.
- Do not abbreviate counter names. GLOSSARY §4 owns them: totalWorkingHours, not
  workHrs.

SPEC §4.2 — implement these four steps in this order, exactly
1. If message timestamp <= lastProcessedTimestamp for that asset, discard the message
   entirely. It updates nothing — not even lastProcessedTimestamp.
2. delta = timestamp - lastProcessedTimestamp, in seconds. Undefined for an asset's
   first message.
3. If delta is undefined OR delta > 60 s, credit NOTHING (including engineHours), store
   the timestamp as the new last-processed value, and continue.
4. Otherwise add delta / 3600 to every counter whose §4.1 condition is true for THIS
   message, then store the timestamp.
Step 3 is the one most often got wrong: a too-large delta still advances the timestamp,
it just credits no hours.

SPEC §4.1 conditions, evaluated on the current message only
  engineHours          always
  totalWorkingHours    ptoEngaged && groundSpeedKmh > 2      ("productive work")
  transportHours       NOT productive && workingStatus == TRANSPORT
  idleHours            NOT productive && workingStatus != TRANSPORT
  loadedHours          productive && loadFactor >= 0.55
  highLoadHours        productive && loadFactor >= 0.75
  tempExcursionHours   temperatureC > 90        (NOT gated on productive work)
  vibrationAlertHours  vibrationLevel is ELEVATED or HIGH  (NOT gated)

Derive productive work from the condition itself, never from workingStatus. SPEC §3.1
is explicit that consumers must not trust the simulator's workingStatus for this;
workingStatus is used only to separate TRANSPORT from IDLE.

INVARIANT — write the three-way branch as one if / else-if / else
  engineHours == totalWorkingHours + transportHours + idleHours, exactly.
Three separate if statements will eventually break this. One branch makes it structural.

SPEC §4.3 — hourly buckets
Bucket key is the whole hour in Europe/Vienna. Declare on the class that needs it:
    private static final ZoneId FARM_ZONE = ZoneId.of("Europe/Vienna");
A message's whole delta goes to the bucket containing its own timestamp — never split
across two buckets.
Rolling 24 h = the current still-open bucket plus the 23 completed buckets before it.
Coverage is deliberately 23-24 h; that is not a bug to correct.
Seal a bucket once the current Vienna hour has moved past it, and expose sealed buckets
for Prompt 3C to flush. Prune to the open bucket plus the last 24.
DST is explicitly out of scope (SPEC §8) — do not special-case 23- or 25-hour days.

SPEC §4.4 — baselines, in AssetBaselines as constants, lifetime scope only
  asset    engine working transport idle loaded highLoad tempExc vibAlert
  TRACTOR   6200   3400     1500    1300   980     410      26      58
  MOWER      950    640      210     100   310      95       4      12
  BALER     1480   1010      300     170   620     240      11      46
  MIXER     1120    860        60    200   415     130       7      19
The rolling window always starts empty. Baselines are never carried across restarts.

SPEC §4.5 — derived figures
  windowCoverageHours = from the LATER of (start of the oldest included bucket) and
                        (the asset's first message in this run), up to now
  utilizationPercent      = totalWorkingHours(24h) / windowCoverageHours   * 100
  productiveSharePercent  = totalWorkingHours(24h) / engineHours(24h)      * 100
Both are null when their denominator is 0. Return null, not 0.0 — SPEC §6.1 distinguishes
them, and an asset that never reported must show null.
No lifetime utilization percentage exists. Do not add one.

ALSO TRACK per asset, for later prompts
- latestMessage, and latestArrivalRealTime (arrival wall-clock, NOT the message
  timestamp) — SPEC §5.3's STALE rule needs arrival time
- firstMessageTimestamp — for windowCoverageHours

STOP AND ASK IF
- You are unsure whether a delta of exactly 60 s is credited. (It is. Only > 60 s is
  discarded.)
- "The oldest included bucket" is ambiguous when fewer than 24 buckets exist.
- A rounding choice would make the §4.1 invariant fail.

DONE WHEN
  cd TelemetryProcessor
  mvnw.cmd test
compiles and contextLoads() passes. Verify the invariant by hand on paper for three
messages: one productive, one TRANSPORT, one with delta > 60 s.
````

---

## Prompt 3C — TelemetryProcessor: evaluation, persistence, wiring

````
Complete Phase 3 of docs/IMPLEMENTATION-PLAN.md: evaluate flags/status/ranking, persist
both tables, and wire the consumer and scheduler. After this prompt the system produces
observable output.

READ FIRST
- docs/SPEC.md §5 in full — §5.1 flags, §5.2 status, §5.3 live condition, §5.4 ranking,
  §5.5 recommendation
- docs/IMPLEMENTATION-PLAN.md §4.4 (last four classes), §4.5-§4.7, and §1.5 (catch-up)
- docs/ARCHITECTURE.md §4 (write cadence, startup reset)

CREATE under TelemetryProcessor/src/main/java/com/mariolagger/telemetryprocessor/
- service/business/FlagEvaluationService.java
- service/business/AssetAssessmentService.java
- service/business/FleetRankingService.java
- service/business/AssetStatePublisher.java
- service/messaging/TelemetryConsumer.java
- scheduler/AssetStateRefreshScheduler.java

EDIT
- TelemetryProcessorApplication.java — add the same JSON MessageConverter bean as the
  Simulator. Keep telemetryQueue() and @EnableScheduling.

SPEC §5.1 — flags. Eight threshold rows collapse to AT MOST FOUR flags, each at the
highest severity currently met. Publish the counter value and the threshold that fired.
  HEAVY_RECENT_USE      highLoadHours       (24h)  MODERATE >= 1.5   SEVERE >= 2.5
  THERMAL_LOAD          tempExcursionHours  (24h)  MODERATE >= 0.8   SEVERE >= 1.5
  MECHANICAL_ATTENTION  vibrationAlertHours (24h)  MODERATE >= 1.0   SEVERE >= 2.0
  HIGH_CUMULATIVE_LOAD  loadedHours    (LIFETIME)  MODERATE >= 850   SEVERE >= 1200
An inactive flag has no representation — no INACTIVE value exists (GLOSSARY §5).

SPEC §5.2 — status, counting each active flag ONCE at its current severity
  CRITICAL  at least one SEVERE, OR two or more MODERATE
  WATCH     exactly one MODERATE and no SEVERE
  GOOD      no active flag

SPEC §5.3 — live condition, from the latest message only, first match wins
  STALE    no message received for this asset in the last 60 s
  ALERT    vibrationLevel == HIGH  OR temperatureC > 95
  CAUTION  vibrationLevel == ELEVATED OR temperatureC > 90 OR loadFactor >= 0.9
  NORMAL   otherwise
Measure staleness on ARRIVAL wall-clock, not on the message timestamp.
liveCondition never influences flags, status, ranking or recommendation. Do not let it
leak into any of them.

SPEC §5.4 — ranking. All four assets, every time. Sort by:
  1. number of SEVERE flags, descending
  2. number of MODERATE flags, descending
  3. highLoadHours + tempExcursionHours + vibrationAlertHours (all 24h), descending
  4. assetId, ascending
priorityRank is 1-based. Criterion 4 makes the order fully deterministic — never omit it.

SPEC §5.5 — recommendation strings, verbatim, including the en-dash in the WATCH line
  CRITICAL  Priority check recommended within the next 5 engine hours.
  WATCH     Plan inspection within the next 15–20 engine hours.
  GOOD      No inspection recommended at this time.

ASSET STATE PUBLISHER
- Writes all four asset_state rows in ONE transaction. priorityRank is a cross-asset
  value; upserting one row alone would transiently leave two rows claiming the same rank
  (ARCHITECTURE §4).
- Flushes sealed hourly buckets to hourly_aggregate. Rows are write-once — a sealed hour
  is never updated (IMPLEMENTATION-PLAN.md §1.4).
- Startup reset in a @PostConstruct: delete both tables, then write the SPEC §4.4
  baselines with zeroed rolling counters, liveCondition STALE, and both percentages null.

  ORDERING GOTCHA — this MUST be @PostConstruct, not CommandLineRunner or
  ApplicationRunner. Rabbit listener containers are SmartLifecycle beans started inside
  finishRefresh(), which runs BEFORE runners. A runner-based reset can delete state the
  consumer has already accumulated. Do not "improve" this to a runner.

TELEMETRY CONSUMER
@RabbitListener(queues = "telemetry"). Hand the message to CounterAccumulationService,
then apply the catch-up rule from IMPLEMENTATION-PLAN.md §1.5:
    if (message timestamp is more than 2 minutes behind Instant.now())
        accumulate only          // catch-up: the backfill burst
    else
        accumulate and upsert asset_state
Nothing is lost — the 5 s refresh publishes whatever is in memory. Without this, the
~5760-message backfill produces ~5760 four-row transactions before the API is usable.

REFRESH SCHEDULER
@Scheduled(fixedRate = 5000) calling AssetStatePublisher. Required because three groups
of values move with wall-clock rather than message arrival: the rolling window edge, the
coverage and percentages, and STALE. It must also seal buckets, so an asset that stops
reporting still gets its final hour written.

DO NOT
- Do not compute anything in this module that OperatorApi will recompute. This module is
  where every product rule lives; OperatorApi only reads.
- Do not add @ControllerAdvice, controllers, or a web server. This module runs with
  spring.main.web-application-type=none.
- Do not round values before persisting. Exact numeric in the database.

STOP AND ASK IF
- Two flags at MODERATE and one at SEVERE produce a status you cannot derive from §5.2
  unambiguously.
- The ranking's criterion 3 sum seems wrong to you. It is a deliberate tie-breaking
  heuristic (SPEC §5.4) — flag it, do not redesign it.
- The startup reset appears to race the listener despite @PostConstruct.

DONE WHEN
With docker infra up, the Simulator running, and this module running:
  SELECT asset_id, status, priority_rank, rolling_24h_high_load_hours
    FROM asset_state ORDER BY priority_rank;
returns four rows with distinct ranks 1-4 and values that change between polls, and
  SELECT count(*) FROM hourly_aggregate;
returns roughly 96 after a 24 h backfill. Also confirm per asset that
engineHours == totalWorkingHours + transportHours + idleHours.
````

---

## Prompt 4A — OperatorApi: read layer

````
Implement the read layer of Phase 4 of docs/IMPLEMENTATION-PLAN.md: entities,
repositories and response DTOs for OperatorApi. No controllers, no services yet.

READ FIRST
- docs/SPEC.md §6 (all four endpoints and their exact response shapes)
- docs/IMPLEMENTATION-PLAN.md §5.1-§5.2 and §1.3 (the flag lookup table)
- docs/GLOSSARY.md §7 (response field names)
- docs/ARCHITECTURE.md §2 (OperatorApi derives no product rule)

CREATE under OperatorApi/src/main/java/com/mariolagger/operatorapi/
- entity/AssetState.java, entity/HourlyAggregate.java, entity/HourlyAggregateId.java
  (read-side copies, mapped exactly as in TelemetryProcessor)
- repository/AssetStateRepository.java
- repository/HourlyAggregateRepository.java
- dto/ — FleetResponse, AssetView, FlagView, CounterSetView, HistoryResponse,
  HistoryRow, LatestTelemetryResponse, LatestTelemetryEntry, TelemetryView, plus the
  enums these serialize

REPOSITORY QUERIES
  AssetStateRepository.findAllByOrderByPriorityRankAsc()

For history, a row covering [hourStart, hourStart + 1h) overlaps [from, to) exactly when
    hourStart < to AND hourStart > from - 1h
so the derived queries are:
  findByHourStartLessThanAndHourStartGreaterThanOrderByHourStartAscAssetIdAsc(
      Instant to, Instant fromMinusOneHour)
  findByAssetIdAndHourStartLessThanAndHourStartGreaterThanOrderByHourStartAscAssetIdAsc(
      String assetId, Instant to, Instant fromMinusOneHour)
Ordering is hourStart ascending then assetId ascending (SPEC §6.3).

DTO FIELD NAMES — must match SPEC §6 exactly
  generatedAt, assets, assetId, priorityRank, status, liveCondition, recommendation,
  flags, lifetimeCounters, rolling24hCounters, windowCoverageHours, utilizationPercent,
  productiveSharePercent, rows, hourStart, telemetry
Flag reason fields: flag, severity, counter, scope, value, threshold.
utilizationPercent and productiveSharePercent must be able to serialize as null.

DO NOT
- Do not add validation annotations or spring-boot-starter-validation. SPEC §6 errors are
  thrown as ResponseStatusException from the service layer.
- Do not add a custom error DTO. SPEC §6: Spring's default error body.
- Do not add an AMQP dependency. This module consumes no queue.
- Do not add write methods to the repositories. This module writes no table.

STOP AND ASK IF
- A SPEC §6 response field has no column behind it in asset_state.
- The history overlap condition looks wrong to you for an hour that starts exactly at
  `from` or exactly at `to`.

DONE WHEN
  cd OperatorApi
  mvnw.cmd test
compiles and contextLoads() passes with the docker infra running.
````

---

## Prompt 4B — OperatorApi: services and controllers

````
Complete Phase 4 of docs/IMPLEMENTATION-PLAN.md: the four REST endpoints of SPEC §6.

READ FIRST
- docs/SPEC.md §6.1-§6.4 — copy the response shapes from the examples there
- docs/IMPLEMENTATION-PLAN.md §5.3-§5.6 and §1.3
- docs/ARCHITECTURE.md §5 (the /api prefix), §2 (no product rules here)
- CLAUDE.md (ResponseStatusException from the service layer, no @ControllerAdvice)

CREATE under OperatorApi/src/main/java/com/mariolagger/operatorapi/
- service/business/AssetViewService.java
- service/business/HistoryService.java
- controller/AssetController.java      — GET /api/assets, GET /api/assets/{assetId}
- controller/HistoryController.java    — GET /api/history
- controller/TelemetryController.java  — GET /api/telemetry/latest

THE /api PREFIX — read this before mapping anything
docker/nginx-conf/nginx.conf proxies /api/ through WITHOUT stripping it, and no module
sets a servlet context path. Map the full paths verbatim: @RequestMapping("/api/assets").
Adding server.servlet.context-path=/api would turn that into /api/api/assets. Do not
add one.

FLAG LOOKUP (IMPLEMENTATION-PLAN.md §1.3)
asset_state stores only severity/value/threshold per flag. Fill counter and scope from
this constant table — it is a label lookup, not a product rule:
  HEAVY_RECENT_USE      -> highLoadHours       / ROLLING_24H
  THERMAL_LOAD          -> tempExcursionHours  / ROLLING_24H
  MECHANICAL_ATTENTION  -> vibrationAlertHours / ROLLING_24H
  HIGH_CUMULATIVE_LOAD  -> loadedHours         / LIFETIME
Omit any flag whose severity column is null — an inactive flag is absent from the
response (GLOSSARY §5).

ROUNDING (IMPLEMENTATION-PLAN.md §1.1 decision 11)
Exact numeric stays in the database; round to 2 decimals at the API boundary only:
    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
This reproduces every SPEC §6 example: 981.4, 26.0, 0.82. Never round before persisting.

GET /api/assets (SPEC §6.1)
200, assets ordered by priorityRank. Assets that never reported are still listed: SPEC
§4.4 baselines as lifetimeCounters, zeroed rolling24hCounters, liveCondition STALE, both
percentages null.

GET /api/assets/{assetId} (SPEC §6.2)
200 with one asset object, identical in shape to one element of the array above.
404 when assetId is not one of TRACTOR / MOWER / BALER / MIXER.

GET /api/history (SPEC §6.3)
Params: from (required), to (required), assetId (optional).
One row per asset per hour that overlaps the period, ordered by hourStart then assetId.
Counter values are the amount accrued WITHIN that hour, not running totals.
400 when from or to is missing or unparseable, from is not before to, or the range
    exceeds 7 days.
404 when assetId is given but is not one of the four assets.
An empty rows array is a valid 200 — not an error, not a 404.

GET /api/telemetry/latest (SPEC §6.4)
All four assets always present, ordered by assetId ascending. An asset that has not
reported gets liveCondition STALE and telemetry null.

DO NOT
- Do not compute a threshold, a percentage, a status, a recommendation or a rank here.
  Every one of those is read from asset_state. If you find yourself writing a comparison
  against 1.5 or 850, stop — that belongs in TelemetryProcessor.
- Do not add @ControllerAdvice or a custom error body.
- Do not add authentication. SPEC §6: no auth, demo system.
- Do not add pagination. 7 days x 24 h x 4 assets is 672 rows maximum.

STOP AND ASK IF
- A response field cannot be produced without recomputing a product rule. That means
  either the schema or TelemetryProcessor is missing something — report it rather than
  computing it here.
- "Range exceeds 7 days" is ambiguous at exactly 7 days.

DONE WHEN
  cd OperatorApi
  mvnw.cmd spring-boot:run
then all of these behave as specified:
  curl localhost:8083/api/assets
  curl localhost:8083/api/assets/TRACTOR
  curl -i localhost:8083/api/assets/NOPE                                     # 404
  curl localhost:8083/api/telemetry/latest
  curl "localhost:8083/api/history?from=2026-08-13T06:00:00Z&to=2026-08-13T09:00:00Z"
  curl -i "localhost:8083/api/history?to=2026-08-13T09:00:00Z"               # 400
  curl -i "localhost:8083/api/history?from=2026-08-13T09:00:00Z&to=2026-08-13T06:00:00Z"  # 400
  curl -i "localhost:8083/api/history?from=2026-08-01T00:00:00Z&to=2026-08-13T00:00:00Z"  # 400
And through nginx: curl localhost:8080/api/assets — confirming the prefix is not doubled.
````

---

## Prompt C — Calibration (resolves IMPLEMENTATION-PLAN.md §1.6)

Run this once the full stack is working. It closes the one open finding in the plan.

````
Measure and calibrate the simulator against the SPEC §5.1 thresholds. This resolves the
open finding in docs/IMPLEMENTATION-PLAN.md §1.6.

READ FIRST
- docs/IMPLEMENTATION-PLAN.md §1.6 in full — the arithmetic and the three options
- docs/SPEC.md §4.4 (the intent this is testing), §5.1 (thresholds), §5.4 (ranking)

THE PROBLEM
The SPEC §3.2 state machine spends about 72% of its time WORKING, roughly 17.3 working
hours per 24 h window. Against §3.2's own field ranges that predicts:
  highLoadHours       ~6.9 h  vs SEVERE bar 2.5 h
  vibrationAlertHours ~6   h  vs SEVERE bar 2.0 h
  tempExcursionHours  ~5   h  vs SEVERE bar 1.5 h
If that holds, all four assets show three SEVERE flags and identical CRITICAL status, so
§5.4's ranking is decided entirely by criteria 3 and 4 — and SPEC §4.4's stated intent
that no asset is permanently CRITICAL does not hold.

MEASURE FIRST, DO NOT ASSUME
With the full stack running and a 24 h backfill completed, record for all four assets:
  SELECT asset_id, status,
         rolling_24h_high_load_hours,
         rolling_24h_temp_excursion_hours,
         rolling_24h_vibration_alert_hours,
         rolling_24h_total_working_hours,
         rolling_24h_engine_hours
    FROM asset_state ORDER BY priority_rank;
Then repeat with simulator.backfill.hours set to 12, 6 and 3, restarting both the
Simulator and TelemetryProcessor between runs (TelemetryProcessor resets both tables on
startup, so each run is clean).

REPORT
A table of measured counters per backfill setting against each threshold, and which
status and flag set each setting produces. State plainly whether the predicted
saturation actually occurred.

THEN RECOMMEND, DO NOT UNILATERALLY CHANGE
Option 1 — shorten simulator.backfill.hours. Touches no governed rule, reversible,
  costs a window coverage below 24 h until the session catches up.
Option 2 — lower the working loadFactor range in SPEC §3.2 (e.g. 0.35-0.85) and
  differentiate it per asset so the four machines differ. A SPEC change.
Option 3 — raise the SPEC §5.1 rolling thresholds to match the simulator the SPEC
  already describes. A SPEC change.

Option 1 is a property change and you may apply it. Options 2 and 3 edit SPEC.md and
require explicit approval first — propose, then stop and ask.

DONE WHEN
The measurements are reported, a backfill value is chosen with the numbers behind it,
and any SPEC change is either approved and applied or recorded as still open.
````

---

## Prompt 5 — Documents

````
Implement Phase 5 of docs/IMPLEMENTATION-PLAN.md: bring the governed documents back in
line with the code that now exists.

READ FIRST
- docs/SPEC.md §0 in full — the document hierarchy, anti-duplication rules and change
  protocol. It governs this entire task.
- docs/IMPLEMENTATION-PLAN.md §6, and §1.1/§1.2/§1.5 for the deviations being recorded
- CLAUDE.md, particularly the "Repo status: skeleton mid-rewrite" section

EDITS

1. docs/SPEC.md §3.1 / §3.2
   - Add the startup backfill: on start, each asset publishes messages stepping 60 s
     across a configurable preceding window before switching to the live 5-10 s cadence.
   - Replace "roughly a 5 % chance per working interval" and the 3 % vibration equivalent
     with the delta-scaled rate actually implemented (0.05 and 0.03 per 60 s of work).
   - If Prompt C produced an approved change to the loadFactor range or the §5.1
     thresholds, apply it here too.

2. docs/ARCHITECTURE.md §4
   - Qualify "asset_state is upserted on every processed message" with the catch-up rule:
     a message whose timestamp is well behind wall-clock updates the in-memory buckets
     only, and the fixed-interval refresh publishes the result.

3. CLAUDE.md
   - Rewrite the "Repo status: skeleton mid-rewrite" section to describe what now exists.
   - Delete the now-false notes: that docker/database-script.sql does not exist, that
     docker-compose mounts no init script, and that OperatorApi has no controllers.
   - Keep the "/api prefix" warning and the queue-wiring note — both are still true.
   - Move the "database-data volume outlives docker compose down, so run down -v once"
     warning into the commands section, where someone hitting it will actually look.

CONSTRAINTS FROM SPEC §0 — these govern the edit, not just the content
- Edit ONLY the authoritative file for each rule. Product rules -> SPEC. Structure ->
  ARCHITECTURE. Naming -> GLOSSARY. Style, commands and repo status -> CLAUDE.md.
- Do not copy a rule, threshold, flag, status rule, name or architecture decision into a
  second file.
- Lower documents may reference higher ones ("see SPEC.md §4") but never restate them.
- After each edit, scan the other three for content the edit just made stale, and fix it
  in the same pass.

DO NOT
- Do not register docs/IMPLEMENTATION-PLAN.md or docs/IMPLEMENTATION-PROMPTS.md in the
  SPEC §0 hierarchy. Both restate rules deliberately and both carry a header saying they
  are non-normative and outside it. Adding a rule-restating document to a hierarchy that
  forbids restating rules would be self-contradictory.
- Do not add anything from SPEC §8 to any document as though it were in scope.
- Do not rewrite sections that are still accurate.

PRE-COMPLETION CHECK (SPEC §0)
- No rule or name is defined in more than one governed file.
- SPEC.md, ARCHITECTURE.md, GLOSSARY.md and CLAUDE.md are mutually consistent.
- Report any place where IMPLEMENTATION-PLAN.md or IMPLEMENTATION-PROMPTS.md now
  contradicts a governed document, and correct the non-normative file.

DONE WHEN
All four governed documents describe the running system, and the pre-completion check
above passes with the findings stated explicitly.
````

---

## Prompt R — Spec-conformance review

Run after Prompt 4B, and again after any significant change. This is a read-and-report task, not an edit task.

````
Review the implementation against docs/SPEC.md. Report findings; change nothing unless
asked.

READ
- docs/SPEC.md §3, §4, §5, §6 in full
- The implemented code in Simulator/, TelemetryProcessor/ and OperatorApi/

CHECK EACH OF THESE AND STATE PASS OR FAIL WITH THE EVIDENCE

Counters (SPEC §4)
1. engineHours == totalWorkingHours + transportHours + idleHours, exactly, in both
   scopes, for all four assets. Query asset_state and show the arithmetic.
2. totalWorkingHours >= loadedHours >= highLoadHours in both scopes.
3. A message with timestamp <= the last processed one updates nothing at all.
4. A delta > 60 s credits no counter but DOES advance the last-processed timestamp.
5. A delta of exactly 60 s IS credited.
6. Productive work is derived from ptoEngaged && groundSpeedKmh > 2, never from
   workingStatus.
7. tempExcursionHours and vibrationAlertHours are NOT gated on productive work;
   loadedHours and highLoadHours ARE.

Buckets and window (SPEC §4.3, §4.5)
8. Buckets are aligned to Europe/Vienna hours via a FARM_ZONE constant.
9. The rolling window is the open bucket plus the 23 before it.
10. Both percentages are null, not 0.0, when their denominator is 0.
11. No lifetime utilization percentage is published anywhere.

Flags, status, ranking (SPEC §5)
12. At most four flags appear per asset, each at its highest met severity.
13. Every active flag carries its counter value and the threshold that fired.
14. Status matches §5.2 for the two-MODERATE and one-SEVERE cases.
15. liveCondition is evaluated top to bottom, first match wins, and staleness uses
    arrival wall-clock rather than the message timestamp.
16. liveCondition influences nothing else.
17. Ranking applies all four criteria in order, including assetId ascending last.
18. Recommendation strings match §5.5 character for character, including the en-dash.

API (SPEC §6)
19. Controllers map the full /api/... paths and no servlet context path is set anywhere.
20. Response field names match §6 and GLOSSARY §7 exactly.
21. The documented 400 and 404 cases return those statuses with Spring's default body.
22. An empty history result is a 200, not a 404.
23. Assets that never reported appear with baselines, zeroed rolling counters, STALE and
    null percentages.
24. OperatorApi computes no threshold, percentage, status, recommendation or rank.

Scope (SPEC §8)
25. Nothing from §8 has been implemented: no AI layer, no POST /api/scenario, no
    per-asset productive-work definition, no separate HIGH vibration counter, no
    persistence across restarts, no DST handling, no auth.

Style (CLAUDE.md)
26. No Lombok, no SLF4J, constructor injection only, no @ControllerAdvice, explicit JPA
    column mappings, ddl-auto=none.

OUTPUT
A numbered list of findings, most severe first. For each: what the document requires,
what the code does, and the file and line. Distinguish confirmed defects from things you
could not verify statically. Propose no fixes unless asked.
````
