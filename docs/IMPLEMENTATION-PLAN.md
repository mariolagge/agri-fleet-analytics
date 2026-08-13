# docs\IMPLEMENTATION-PLAN.md

Work order for fixing bugs 1–13 from the 2026-08-13 repo review. Written to be executed by
a coding agent, top to bottom.

**This file is a transient work order, not a governed document.** It defines no rule of its
own. Every threshold, name and contract it mentions belongs to `SPEC.md`, `ARCHITECTURE.md`
or `GLOSSARY.md` and is *referenced* here, never restated as an independent source. It sits
outside the `SPEC.md` §0 hierarchy and is deleted once the work lands.

Line numbers below are anchors as of commit `a51b96c` and will drift as steps are applied.
**Method and class names are the real anchors** — if a line number no longer matches, find
the named method.

---

## 0. Scope guard

### In scope — bugs 1 to 13

| # | Bug | Phase |
|---|-----|-------|
| 1 | Buckets sealed while still filling; `hourly_aggregate` loses hours | A3 |
| 2 | Live `CounterSet` objects read outside the accumulator lock | A2, A3 |
| 3 | Hours marked sealed before the write commits | A4 |
| 4 | `synchronized` released before the transaction commits | A4 |
| 5 | `@Transactional` bypassed by self-invocation from `@PostConstruct` | A4 |
| 6 | Percentages and counters computed from different clock reads | A2 |
| 7 | `/api/assets` and `/api/assets/{id}` non-conforming when rows are missing | B1 |
| 8 | Simulator invents a `TRANSPORT` temperature band SPEC never defines | C1, E1 |
| 9 | Burst shows a 20-hour-old reading with a live `NORMAL` condition | A1 |
| 10 | `classList.add('')` throws and aborts the whole fleet render | D1 |
| 11 | Diagram and metric strip keep stale data after a failed fetch | D2 |
| 12 | Unescaped `assetId` interpolated into a class attribute | D3 |
| 13 | Nullable columns unbox into primitives — latent NPE | B2 |

### Out of scope — do not touch

Bugs 14–18 and every item from the review's "Minor" list are **explicitly excluded**. Do not:

- rename or move any module directory (bug 14 — `OperatorApi/` vs `operatorapi/` casing);
- modify, guard or annotate the three existing `contextLoads()` tests (bug 15);
- edit or remove the four `IMPLEMENTATION-PLAN.md` citations in
  [AssetViewService.java:120](../operatorapi/src/main/java/com/mariolagger/operatorapi/service/business/AssetViewService.java#L120),
  [AssetStatePublisher.java:125](../telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/service/business/AssetStatePublisher.java#L125),
  [TelemetryConsumer.java:20](../telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/service/messaging/TelemetryConsumer.java#L20)
  and [simulator application.properties:13](../simulator/src/main/resources/application.properties#L13)
  (bug 16). Their `§1.3`–`§1.6` numbers point at the *old*, deleted plan and do not match this
  file's sections. Leave them exactly as they are — including when a step rewrites the
  surrounding comment;
- touch `.cursorrules` (bug 17) or `README.md` (bug 18);
- touch `docker-compose.yml`, `nginx.conf`, `database-script.sql`, `icons.js`'s unused icons,
  `spring.jpa.show-sql`, `.claude/settings.local.json`, or add a poll-overlap guard to
  `setInterval` (all "Minor").

**No schema change is required by any step.** `docker compose down -v` is therefore *not*
needed except for the one optional cold-start check in Phase G.

### Working conventions

`CLAUDE.md` "Coding style" governs every line written here and is not restated. The points
most likely to be violated by these particular changes:

- plain Java, no Lombok, no SLF4J, `System.out.println` for logging;
- constructor injection only;
- verbose names (`sealableBuckets`, not `sealed`); field names come from `GLOSSARY.md`;
- comments are rare and cite a SPEC section rather than narrating the code;
- no new frameworks, no new Spring beans beyond what a step names, no extra interfaces or
  factories.

---

## 1. Decisions already made

Do not re-open these. They were settled before the plan was written.

| Decision | Choice |
|----------|--------|
| Doc editing authority | `SPEC.md` and `ARCHITECTURE.md` may be edited (Phase E). `GLOSSARY.md` needs no change. `CLAUDE.md` edits need sign-off — see E7. |
| Bug 1 seal rule | Per-asset **timestamp watermark**: an hour seals when that asset's own `lastProcessedTimestamp` has moved past it. Accepted trade-off: a permanently silent asset's final partial hour never reaches `hourly_aggregate`. No wall-clock backstop. |
| Bugs 3/4/5 mechanism | **Drop `@Transactional` entirely.** `SimpleJpaRepository.saveAll` is itself transactional, so the four `asset_state` rows stay atomic; `asset_state` and `hourly_aggregate` become two transactions instead of one. |
| Bug 7 behaviour | **503** when state is missing, **404** reserved strictly for an id outside the four §2 assets. Requires a `SPEC.md` §6 addition. |
| Bug 8 | Add the `TRANSPORT` band to `SPEC.md` §3.2; simulator behaviour unchanged. |
| Bug 9 | Derive `STALE` from the **message timestamp**, not from arrival wall-clock. `latestArrivalRealTime` is deleted. |
| §6.4 knock-on | Widen `SPEC.md` §6.4: `telemetry` is `null` only for an asset that never reported; one that reported and fell silent is `STALE` **and keeps its last reading**. |
| Bug 6 scope | One `snapshotOf(assetId, now)` under a single lock acquisition and a single clock read. |
| Bug 13 | Nullable DTO fields; `null` reaches the JSON. The frontend already renders `null` as an em dash. |
| Tests | A focused regression test per behavioural fix. Pure JUnit 5 + Mockito, no Spring context, no database. |
| Verification | Unit tests **and** a full manual run (Phase G). |

---

## 2. Execution order

```
A1 ─┐
    ├─▶ A2 ──▶ A4 ──▶ F1..F3 ──▶ G
A3 ─┘         │
              └─▶ E5, E6
B1 ──▶ E4          B2 ──▶ E3
C1 ──▶ E1                A1 ──▶ E2
D1, D2, D3 (independent)
```

Phases A → B → C → D → E → F → G. Within Phase A the order **A1, A3, A2, A4** is mandatory:
A1 and A3 are local edits, A2 restructures the accumulator's public surface around them, and
A4 depends on the shape A2 and A3 leave behind.

Phases B, C and D are independent of A and of each other. Phase E is pure documentation and
must land in the same change set as the code it describes (`SPEC.md` §0 change protocol).

---

## 3. Phase A — TelemetryProcessor

All paths relative to
`telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/`.

A property worth aiming at deliberately: **after A1–A3, `CounterAccumulationService` and
`AssetAssessmentService` never call `Instant.now()`.** Every time-dependent value is passed
in by `AssetStatePublisher`, which reads the clock exactly once per `publish()`. That is what
makes Phase F's tests deterministic without a clock abstraction, and it is the single most
useful outcome of this phase — do not undo it by leaving a stray `Instant.now()` behind.

### A1 — Live condition from the message timestamp (bug 9)

**Files**

- `service/business/AssetAccumulator.java` — fields at :34-35, accessors at :86-92
- `service/business/CounterAccumulationService.java` — `accumulate` :88, `latestArrivalRealTime` :189-191
- `service/business/AssetAssessmentService.java` — `liveCondition` :52-67
- `service/business/AssetStatePublisher.java` — call site :90-92

**What is wrong.** `accumulate` stamps `latestArrivalRealTime = Instant.now()` for every
accepted message, including the thousands of backfilled ones. `liveCondition` measures
staleness against that stamp, so during the `SPEC.md` §3.2 burst a reading from twenty hours
ago is reported as a live `NORMAL`.

**Required behaviour**

1. Delete `latestArrivalRealTime` from `AssetAccumulator` — field, getter and setter.
2. Delete the `setLatestArrivalRealTime` call from `accumulate` and the
   `latestArrivalRealTime(AssetId)` accessor from `CounterAccumulationService`.
3. `liveCondition` takes `(TelemetryMessage latestMessage, Instant now)` and returns `STALE`
   when `latestMessage` is `null` **or** the message's own timestamp is more than
   `STALE_AFTER` old relative to `now`. Keep `STALE_AFTER` at its current value and keep the
   `SPEC.md` §5.3 row order below it untouched.
4. A message timestamped in the future (clock skew) must not read as `STALE`.

**Acceptance criteria**

- `grep -r latestArrivalRealTime telemetryprocessor/` returns nothing.
- `AssetAssessmentService` contains no `Instant.now()`.
- Given a message timestamped 20 hours before `now`, `liveCondition` returns `STALE`
  regardless of its vibration, temperature or load values.
- Given a message timestamped 5 seconds before `now` with `vibrationLevel` `HIGH`,
  `liveCondition` returns `ALERT`.

### A3 — Watermark sealing, and sealing split from marking (bugs 1, 2)

Apply before A2.

**File** — `service/business/CounterAccumulationService.java`, `takeSealedBuckets` :198-210
and `pruneBuckets` :217-226. Supporting change in `service/business/CounterSet.java`.

**What is wrong.** `takeSealedBuckets` seals every bucket whose key precedes the *wall-clock*
current hour. During the burst the consumer is replaying hours-old timestamps, so a bucket
that is still receiving messages qualifies, gets written as a partial snapshot, and — because
`sealedHours` guarantees one hand-over — is never written again. It also hands back the live
`CounterSet` objects and marks them sealed in the same call.

**Required behaviour**

1. Add a copy facility to `CounterSet` — a copy constructor or a `copy()` method returning a
   new instance with the same eight values. Nothing else about `CounterSet` changes.
2. Replace `takeSealedBuckets` with two methods:
   - `sealableBuckets(AssetId)` — a **pure read**. Returns, oldest first, the buckets whose
     hour is strictly before `hourStartOf(lastProcessedTimestamp)` for that asset and that are
     not already in `sealedHours`. Returns an empty map when `lastProcessedTimestamp` is
     `null`. **Returns copies**, never the live `CounterSet` instances. Mutates nothing —
     no `sealedHours` insertion, no pruning.
   - `markSealed(AssetId, Set<Instant> hourStarts)` — adds those hours to `sealedHours` and
     then runs the existing `pruneBuckets` logic. `synchronized`, like everything else here.
3. `pruneBuckets` keeps its current rule unchanged: drop the oldest bucket only when the map
   exceeds `RETAINED_BUCKETS` **and** that oldest hour is already sealed.

**Why a watermark is correct.** Message timestamps are strictly increasing per asset
(`SPEC.md` §4.2 step 1 discards anything else), so once a message for hour *H+1* has been
processed, no later message can land in hour *H*. `hourStartOf(lastProcessedTimestamp)` is
therefore the exact boundary between "may still grow" and "final".

**Acceptance criteria**

- With wall-clock hours ahead of the stream, an hour is returned only after a message
  belonging to a later hour has been accumulated for that asset.
- Mutating a returned `CounterSet` leaves the accumulator's own bucket unchanged.
- Calling `sealableBuckets` twice in a row without an intervening `markSealed` returns the
  same hours both times.
- After `markSealed`, those hours are not returned again.

### A2 — One snapshot, one lock, one clock read (bugs 6, 2)

**File** — `service/business/CounterAccumulationService.java`, plus a new
`service/business/AssetSnapshot.java`.

**What is wrong.** `AssetStatePublisher` takes the lock six times per asset and
`utilizationPercent`/`productiveSharePercent` each re-read `Instant.now()` and recompute the
counters internally. A sequence straddling an hour boundary can store a percentage that is
not the quotient of the counters stored beside it — which breaks `SPEC.md` §1's "every number
the system shows must be traceable to a counter". Every one of those calls also reaches live
accumulator state from the scheduler thread.

**Required behaviour**

1. New plain POJO `AssetSnapshot` in `service/business/` — constructor plus getters, no
   setters, no annotations. It carries everything one `asset_state` row needs from the
   accumulator:
   - `CounterSet lifetimeCounters`
   - `CounterSet rolling24hCounters`
   - `double windowCoverageHours`
   - `Double utilizationPercent`
   - `Double productiveSharePercent`
   - `TelemetryMessage latestMessage`
2. `CounterAccumulationService` gains `public synchronized AssetSnapshot snapshotOf(AssetId
   assetId, Instant now)` which computes all six from the single `now` it is handed, under a
   single lock acquisition.
3. `rolling24hCounters`, `lifetimeCounters`, `windowCoverageHours`, `utilizationPercent`,
   `productiveSharePercent` and `latestMessage` become **private** helpers. The time-dependent
   ones take `now` as a parameter instead of calling `Instant.now()`. The two percentage
   helpers take the already-computed rolling counters and coverage rather than recomputing
   them — this is what makes the published quotient exact.
4. After this step `CounterAccumulationService` exposes exactly four public methods:
   `accumulate`, `snapshotOf`, `sealableBuckets`, `markSealed`.
5. The `SPEC.md` §4.5 null rule is unchanged: a percentage is `null`, never `0.0`, when its
   denominator is `0`.
6. `latestMessage` is handed out by reference. It is never mutated after
   `TelemetryGenerator` builds it — do not add a setter call anywhere downstream.

**Acceptance criteria**

- `CounterAccumulationService` contains no `Instant.now()`.
- For any snapshot with a non-null `utilizationPercent`, that value equals
  `rolling24hCounters.totalWorkingHours / windowCoverageHours × 100` exactly.
- For any snapshot with a non-null `productiveSharePercent`, that value equals
  `rolling24hCounters.totalWorkingHours / rolling24hCounters.engineHours × 100` exactly.
- An asset that has not reported yields `windowCoverageHours` `0.0` and both percentages
  `null`.

### A4 — Lock outside the write, seal after the write (bugs 3, 4, 5)

**File** — `service/business/AssetStatePublisher.java`, `resetOnStartup` :59-65,
`publish` :72-123, `flushSealedBuckets` :126-137.

**What is wrong.** Three faults in one method. `@Transactional` is applied by a proxy *around*
`publish()`, so the `synchronized` monitor is released before commit and a second thread can
start rewriting the same four rows against an uncommitted transaction. `resetOnStartup` calls
`this.publish()` from `@PostConstruct`, where the proxy does not exist at all, so the
annotation does nothing there. And `flushSealedBuckets` marks hours sealed as an in-memory
side effect that no rollback undoes, so a failed write loses those hours permanently.

**Required behaviour**

1. Remove `@Transactional` from `publish()` and remove the now-unused import. `publish()`
   stays `synchronized`.
2. `publish()` reads the clock **once** into a local `Instant now` and threads it into
   `snapshotOf` and `liveCondition`. `row.setUpdatedAt` uses the same value.
3. Rewrite the body in this strict order:
   1. read `now`;
   2. per asset: `snapshotOf(assetId, now)` and `sealableBuckets(assetId)`, keeping the hour
      sets per asset for step 6;
   3. evaluate flags, status, recommendation and live condition from the snapshot;
   4. assign priority ranks across all four rows (unchanged — `FleetRankingService`);
   5. `assetStateRepository.saveAll(rows)`, then `hourlyAggregateRepository.saveAll(hourRows)`;
   6. **only now** call `markSealed(assetId, hours)` for each asset.
4. Steps 5 and 6 must not be reordered or interleaved. If either `saveAll` throws, no hour is
   marked sealed and the next tick retries it.
5. `resetOnStartup` keeps `@PostConstruct` and keeps calling `publish()` directly — with the
   annotation gone, that is no longer a bypass.
6. `flushSealedBuckets` either disappears into `publish()` or keeps its name and returns the
   per-asset hour sets. Do not leave it able to mark hours sealed on its own.

**What is deliberately given up.** `asset_state` and `hourly_aggregate` are now written in two
transactions rather than one. Both remain internally atomic — `SimpleJpaRepository.saveAll` is
`@Transactional`, so all four `asset_state` rows still land together and
`ARCHITECTURE.md` §4's split-rank concern is unaffected. `resetOnStartup`'s
delete-then-write also remains two transactions; the window where the table is empty is
covered by B1's 503.

**Acceptance criteria**

- No `@Transactional` anywhere in the module.
- `publish()` calls `Instant.now()` exactly once.
- With a repository mock that throws on `saveAll`, a following `sealableBuckets` call still
  returns the same hours.
- With repository mocks that succeed, `markSealed` is invoked after both `saveAll` calls.
- All four `asset_state` rows go out in a single `saveAll` invocation.

---

## 4. Phase B — OperatorApi

Paths relative to `operatorapi/src/main/java/com/mariolagger/operatorapi/`.

### B1 — 503 for missing state, 404 only for an unknown id (bug 7)

**File** — `service/business/AssetViewService.java`, `getFleet` :43-53, `getAsset` :56-63,
`getLatestTelemetry` :66-84.

**What is wrong.** `SPEC.md` §6.1 requires all four assets to be listed on every response and
§6.2 restricts `404` to an id outside the four §2 assets. When `asset_state` is empty — before
`TelemetryProcessor` has ever run, or inside its reset window — `getFleet` returns an empty
array and `getAsset` returns `404` with a message about baselines, so a transient
infrastructure state is reported as "this machine does not exist".

**Required behaviour**

1. `getFleet` throws `ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, …)` when the
   repository returns no rows. The message must name the cause (no state published yet), in
   the style of the existing messages in this class.
2. `getAsset` keeps `requireKnownAsset` and its `404` for an id outside the four. The
   `orElseThrow` for a *known* id with no row becomes `SERVICE_UNAVAILABLE`.
3. `getLatestTelemetry` is **unchanged** and keeps returning `200`. Four entries with
   `liveCondition` `STALE` is exactly the `SPEC.md` §6.4 shape for assets that have not
   reported, it is truthful, and it is the endpoint the strip polls every ten seconds — a 503
   there would blank the strip for no gain. Record this asymmetry in the §6 doc edit (E4).
4. `spring.web.error.include-message=always` is already set, so the message reaches the body.
   Change nothing in `application.properties`.

**Acceptance criteria**

- Empty repository → `GET /api/assets` is `503` with a non-empty `message`.
- Empty repository → `GET /api/assets/TRACTOR` is `503`.
- Any repository state → `GET /api/assets/BULLDOZER` is `404`.
- Empty repository → `GET /api/telemetry/latest` is `200` with four `STALE` entries.

### B2 — Nullable DTO fields (bug 13)

**Files** — `dto/TelemetryView.java`, `dto/FlagView.java`,
`service/business/AssetViewService.java` (`toTelemetryView` :183-194, `addFlagIfActive`
:123-138).

**What is wrong.** `latest_pto_engaged`, `latest_ground_speed_kmh`, `latest_load_factor`,
`latest_temperature_c` and the six flag `value`/`threshold` columns are all nullable, but the
DTO fields and `round2(double)` are primitives. A null unboxes to an NPE.

**Required behaviour**

1. `TelemetryView`: `ptoEngaged` becomes `Boolean`; `groundSpeedKmh`, `loadFactor` and
   `temperatureC` become `Double`.
2. **`isPtoEngaged()` must be renamed to `getPtoEngaged()`.** Jackson recognises `isX()` as a
   getter only for the primitive `boolean`; leaving it as `isPtoEngaged()` on a `Boolean`
   silently drops the field from the JSON.
3. `FlagView`: `value` and `threshold` become `Double`.
4. `AssetViewService` uses the existing `round2OrNull` (:210) for all five nullable numeric
   mappings and assigns the `Boolean` straight through.
5. **Do not** change `CounterSetView` or `HistoryRow` — every column behind them is `NOT NULL`
   in `docker/database-script.sql`, and primitives are correct there.
6. No frontend change is needed: `formatBoolean(null)` and `formatNumber(null)` already return
   an em dash.

**Acceptance criteria**

- A row with all `latest_*` columns null produces a `telemetry` object of nulls rather than
  throwing, and no field renders as `0`.
- A reporting asset's `/api/telemetry/latest` response still contains a `ptoEngaged` key.
- `grep -n "isPtoEngaged" operatorapi/` returns nothing.

---

## 5. Phase C — Simulator

### C1 — Remove the apologetic comment (bug 8)

**File** —
`simulator/src/main/java/com/mariolagger/simulator/service/business/TelemetryGenerator.java`,
`targetTemperatureC` :166-167.

**What is wrong.** The comment concedes that `SPEC.md` §3.2 gives no `TRANSPORT` band and that
the code invented `55–70` to fill the gap. `SPEC.md` §0 forbids inventing rules in code.

**Required behaviour.** The generator's numbers do not change. Replace the two-line
explanation with a bare `// SPEC §3.2` citation, matching the style of the other comments in
the class. E1 puts the band into `SPEC.md` in the same change set.

**Acceptance criteria** — the `TRANSPORT` band still reads `randomBetween(55, 70)`; no comment
in the file describes a value as absent from SPEC.

---

## 6. Phase D — Frontend

Plain HTML/CSS/vanilla JS, no bundler, no dependencies. **No CSS change is required by any of
these steps** — do not add a colour, class or token to `app.css`.

### D1 — Guard the empty class token (bug 10)

**File** — `frontend/js/fleet.js`, `renderFleetDiagram` :17-18.

**What is wrong.** `statusClass()` returns `''` for any status outside the three known values
([format.js:116-121](../frontend/js/format.js#L116-L121)) and `DOMTokenList.add('')` raises a
`SyntaxError`. That aborts `renderFleetDiagram` mid-loop, so `renderFleetRanking` never runs
and the whole screen goes blank.

**Required behaviour.** Add the class only when it is non-empty. Keep `statusClass`'s contract
as it is — it is used elsewhere inside `class="…"` strings where `''` is harmless. Do not
substitute a placeholder class name; an unknown status should simply leave the node
unstyled.

**Acceptance criteria** — rendering a fleet whose asset has `status: null` throws nothing and
still produces the ranking list.

### D2 — Reset the diagram and metrics on a failed fetch (bug 11)

**Files** — `frontend/js/fleet.js` `renderFleetDiagram` :8-28 and `renderFleetMetrics` :74-94;
`frontend/js/app.js` `refreshFleet` error branch :49-55.

**What is wrong.** `renderFleetDiagram` iterates the *response*, so a node with no matching
asset keeps the previous status, rank and utilization. On error `app.js` sets `fleetAssets`
to `[]` but never re-renders the diagram or the metric tiles, so both keep showing the last
good values beside an error message.

**Required behaviour**

1. `renderFleetDiagram` iterates the four `.node-group` elements in the DOM and looks each one
   up in `assets`. A node with no match is reset: status and live classes removed, and the
   status, value and rank texts set to the em dash. This fixes the short-response case and
   the error case in one place.
2. `renderFleetMetrics` renders the em dash rather than `0` for the three status counts and
   for `reporting` when `assets` is empty. An unknown fleet is not a fleet of zero criticals —
   this is the same rule `CLAUDE.md` states for the rest of the frontend.
3. `refreshFleet`'s catch branch calls `renderFleet([], null, selectAsset)` before
   `showFleetRankingError`, so the reset actually runs.

**Acceptance criteria** — with `OperatorApi` stopped, pressing Refresh leaves no node showing a
status, rank or utilization figure, and all nine metric values read the em dash.

### D3 — Escape the asset id in the chip class (bug 12)

**File** — `frontend/js/format.js`, `assetChipHtml` :251-255.

**What is wrong.** `asset-chip-${tone}` is the one value in the frontend interpolated into
markup without `escapeHtml`. Not reachable today — `assetId` is a Java enum — but it is the
single exception to the rule the rest of the file follows.

**Required behaviour.** Escape it, exactly as every other interpolation in the file does.

**Acceptance criteria** — no unescaped `${…}` remains inside a quoted attribute anywhere in
`frontend/js/`.

---

## 7. Phase E — Documents

Follow `SPEC.md` §0's Document Change Protocol: edit only the authoritative file, then scan
the others for anything the edit made stale. These edits must land in the same change set as
the code they describe.

**`GLOSSARY.md` needs no change.** No identifier that it owns is added, removed or renamed by
this work — `latestArrivalRealTime` was never a glossary name, and a status code is not a name.
Do not go looking for one.

| Step | File | Change |
|------|------|--------|
| E1 | `SPEC.md` §3.2 | Add the `TRANSPORT` band to the `temperatureC` sentence, beside the existing `WORKING` and `IDLE` bands. The value is the one the simulator already uses. Pairs with C1. |
| E2 | `SPEC.md` §5.3 | Restate the `STALE` row in terms of the age of the most recent **message**, not of when one was "received". This aligns the row with §5.3's own opening sentence — "derived from the most recent message only" — and with A1. |
| E3 | `SPEC.md` §6.4 | Widen the closing paragraph: `telemetry` is `null` only for an asset that has never reported in this run; one that reported and then fell silent is `STALE` and keeps its last reading. Pairs with A1. |
| E4 | `SPEC.md` §6.1, §6.2 | Add `503 Service Unavailable` — no state has been published yet — to both. State in §6.2 that `404` is reserved for an id outside the four §2 assets. Note that §6.4 deliberately stays `200` with four `STALE` entries. Pairs with B1. |
| E5 | `ARCHITECTURE.md` §4 | Rewrite the sealing sentence: an hour reaches `hourly_aggregate` once that asset's **own stream** has moved past it, not once wall-clock has. Pairs with A3. |
| E6 | `ARCHITECTURE.md` §4 | Update the write-cadence and startup-reset paragraphs: `asset_state` and `hourly_aggregate` are two transactions; the four `asset_state` rows remain atomic; an hour is marked flushed only after its write returns; the API answers `503` during the reset window. Pairs with A4 and B1. |
| E7 | `CLAUDE.md` "Repo status" | **Needs sign-off before editing.** Phase F makes the "only the three `contextLoads()` smoke tests exist" sentence false. `CLAUDE.md` is rank 0 and was not covered by the doc-authority decision. Prepare the wording, flag it, and do not commit the edit unauthorised. |

---

## 8. Phase F — Regression tests

One test class per service touched, `JUnit 5 + Mockito`, matching the conventions
`CLAUDE.md` already sets: `@ExtendWith(MockitoExtension.class)`, `@Mock`, `ArgumentCaptor`,
plain `assertEquals`/`assertTrue`.

**These are pure unit tests. No `@SpringBootTest`, no Spring context, no database, no broker.**
Phase A leaves `CounterAccumulationService` and `AssetAssessmentService` free of
`Instant.now()`, so every time-dependent case is expressed by passing a fixed `Instant` — do
not introduce a `Clock` bean or a time-mocking library.

Do not modify the three existing `contextLoads()` classes (bug 15, out of scope).

### F1 — `CounterAccumulationServiceTest` (telemetryprocessor)

Covers bugs 1, 2 and 6.

- an hour is not sealable while the asset's stream is still inside it, even with `now` hours
  ahead — the direct bug 1 regression;
- an hour becomes sealable once a message belonging to a later hour is accumulated;
- `sealableBuckets` returns copies: mutating the result does not change the accumulator;
- `sealableBuckets` is repeatable until `markSealed`, and silent afterwards;
- `SPEC.md` §4.2: a Δt above 60 s credits nothing but still advances the last-processed
  timestamp; a Δt of exactly 60 s is credited; an out-of-order or duplicate timestamp updates
  nothing;
- `SPEC.md` §4.1 invariant: `engineHours` equals `totalWorkingHours + transportHours +
  idleHours` after a mixed sequence;
- a snapshot's `utilizationPercent` is exactly its own counters over its own coverage;
- an asset that never reported yields `null` for both percentages.

### F2 — `AssetStatePublisherTest` (telemetryprocessor)

Covers bugs 3, 4 and 5.

- `markSealed` is invoked only after both `saveAll` calls, verified by `InOrder`;
- when `assetStateRepository.saveAll` throws, `markSealed` is never invoked;
- all four rows reach `assetStateRepository` in a single `saveAll`, captured with
  `ArgumentCaptor`;
- `resetOnStartup` deletes both tables and then writes four rows carrying the `SPEC.md` §4.4
  baselines.

### F3 — `AssetAssessmentServiceTest` (telemetryprocessor)

Covers bug 9 and pins `SPEC.md` §5.2/§5.3/§5.5.

- a message 20 hours old is `STALE` whatever its readings say;
- the §5.3 rows are evaluated top to bottom — a fresh `HIGH` vibration is `ALERT`, a fresh
  `ELEVATED` is `CAUTION`, `loadFactor` exactly `0.9` is `CAUTION`, otherwise `NORMAL`;
- §5.2: one `SEVERE` is `CRITICAL`; two `MODERATE` is `CRITICAL`; one `MODERATE` is `WATCH`;
  none is `GOOD`.

### F4 — `AssetViewServiceTest` (operatorapi)

Covers bugs 7 and 13.

- empty repository → `getFleet` throws with status `503`;
- unknown id → `404`; known id with no row → `503`;
- a row with null `latest_*` columns maps to a `telemetry` object of nulls without throwing;
- an inactive flag is absent from the response, an active one carries its counter, scope,
  value and threshold.

---

## 9. Phase G — Verification runbook

Run in order. Windows: `mvnw.cmd`, not `./mvnw`.

**1. Compile and unit-test**

```
mvn -q compile                       # from the repo root
```
Then per module, from `simulator/`, `telemetryprocessor/`, `operatorapi/`:
```
mvnw.cmd test
```
`TelemetryProcessor` and `OperatorApi` need Postgres reachable for their `contextLoads()`
test; `TelemetryProcessor` also needs RabbitMQ. Start the infra first (step 2). This is
expected, not a bug — `CLAUDE.md` records it.

**2. Infrastructure**

From `docker/`:
```
docker compose up -d
```
No schema change was made, so `down -v` is not required.

**3. Cold-start 503 check (optional, bug 7)** — this one *does* need a wiped volume:
```
docker compose down -v && docker compose up -d
```
Start **only** `OperatorApi`. Expect `503` from `/api/assets` and from
`/api/assets/TRACTOR`, `404` from `/api/assets/BULLDOZER`, and `200` with four `STALE`
entries from `/api/telemetry/latest`. Then stop it and continue.

**4. Full run**

Start `TelemetryProcessor`, then `Simulator`, then `OperatorApi`.

**5. Checks**

| # | Check | Bug |
|---|-------|-----|
| 5.1 | Between `TelemetryProcessor` starting and `Simulator` starting, `/api/assets` returns `200` with four assets at their §4.4 baselines, `TRACTOR` at `WATCH` and the rest `GOOD`. | 5, 7 |
| 5.2 | **During** the burst, `/api/telemetry/latest` shows `STALE` for all four assets. Before the fix it showed `NORMAL` beside a twenty-hour-old timestamp. Shorten `simulator.backfill.hours` if the window is too brief to catch. | 9 |
| 5.3 | After the burst, all four read `NORMAL`/`CAUTION`/`ALERT` with timestamps within seconds of now. | 9 |
| 5.4 | **The bug 1 regression check.** `GET /api/history?from=<24 h ago>&to=<now>` — every *completed* hour must read `engineHours` ≈ `1.00` (±0.02). The burst steps 60 s and credits 60/3600 h per step, so a full hour is exactly 1.0; before the fix one or two hours per run read visibly less. Only the first and last hours of the range may be partial. | 1, 3 |
| 5.5 | Each asset has ~24 rows over the window, and `totalWorkingHours + transportHours + idleHours` equals `engineHours` per row. | 1 |
| 5.6 | For each asset in `/api/assets`, `utilizationPercent` equals `rolling24hCounters.totalWorkingHours / windowCoverageHours × 100` to the published precision. | 6 |
| 5.7 | Let it run across a whole-hour boundary in `Europe/Vienna`. The newly completed hour appears in `/api/history` within ~5 s and also reads ≈ `1.00`. | 1 |
| 5.8 | Open `http://localhost:8080`, stop `OperatorApi`, press Refresh: every diagram node and every metric tile resets to an em dash, and the ranking shows the error. Restart `OperatorApi` and confirm it recovers. | 11 |

---

## 10. Definition of done

- [ ] A1 — `latestArrivalRealTime` gone; `liveCondition` takes `now` and reads the message timestamp
- [ ] A3 — `sealableBuckets` (pure, copies, watermark) and `markSealed` replace `takeSealedBuckets`
- [ ] A2 — `AssetSnapshot` added; `CounterAccumulationService` exposes four public methods and no `Instant.now()`
- [ ] A4 — `@Transactional` gone; one clock read per `publish()`; `markSealed` strictly after both `saveAll`
- [ ] B1 — `503` for missing state, `404` only for an unknown id, `/api/telemetry/latest` still `200`
- [ ] B2 — `TelemetryView` and `FlagView` nullable; `getPtoEngaged()` renamed
- [ ] C1 — apologetic comment replaced by a `SPEC §3.2` citation
- [ ] D1 — no `classList.add('')`
- [ ] D2 — diagram and metrics reset on a failed fetch
- [ ] D3 — asset id escaped in `assetChipHtml`
- [ ] E1–E6 — `SPEC.md` and `ARCHITECTURE.md` updated in the same change set
- [ ] E7 — `CLAUDE.md` wording prepared and flagged, **not** committed without sign-off
- [ ] F1–F4 — four new unit test classes, all green, none requiring infrastructure
- [ ] G — full runbook executed, checks 5.1–5.8 pass
- [ ] Nothing from bugs 14–18 or the "Minor" list was touched
- [ ] `git diff` reviewed: no Lombok, no SLF4J, no new bean beyond `AssetSnapshot`, no CSS change

---

## 11. Pitfalls

Specific to these changes, in rough order of how likely they are to bite:

1. **Leaving an `Instant.now()` behind** in `CounterAccumulationService` or
   `AssetAssessmentService` after A1/A2. It compiles, it runs, and it quietly makes F1 and F3
   non-deterministic.
2. **Returning live `CounterSet` references** from `sealableBuckets`. The code works; the race
   in bug 2 survives untouched.
3. **Marking hours sealed inside the read.** The whole point of the A3/A4 split is that the
   read is pure. If `sealableBuckets` mutates, bug 3 comes straight back.
4. **`isPtoEngaged()` on a `Boolean`.** Jackson drops the field silently — no error, just a
   missing key and an em dash on the strip.
5. **Re-adding `@Transactional` "for safety"** on `publish()` or on the new writer path. That
   reinstates bugs 4 and 5.
6. **A wall-clock backstop in the seal rule.** It was considered and rejected; adding one
   reintroduces bug 1 during the burst.
7. **Touching the module directory casing** because the root reactor looks wrong. Bug 14 is
   out of scope and the working tree is correct for this machine.
8. **Renumbering or "fixing" the `IMPLEMENTATION-PLAN.md §1.x` citations** in the four files
   listed in §0. They refer to the deleted plan, not to this one. Bug 16, out of scope.
