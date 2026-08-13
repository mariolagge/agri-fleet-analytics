# docs\IMPLEMENTATION-PROMPTS.md

Executable prompts for the bug 1–13 work order in `docs/IMPLEMENTATION-PLAN.md`. Fourteen
prompts, run in order, each ending in a state you can verify before starting the next.

Like the plan, **this file is a transient work order, not a governed document.** It defines no
rule of its own and sits outside the `SPEC.md` §0 hierarchy. Delete both files once the work
lands.

---

## 1. How to use this file

- **Run the prompts in the listed order.** P1–P4 in particular are order-dependent; the plan's
  §2 explains why.
- **Copy the fenced block verbatim.** Everything inside the fence is the prompt. The table
  above each fence is for you, not the agent.
- **One prompt per agent turn.** A fresh session per prompt is fine and often better — every
  prompt names the files to read, so no session needs the previous one's context.
- **Do not skip the VERIFY block.** Each prompt's verification is cheap (compile plus a grep)
  except P12–P14, which are the real gates.
- **Tick the status table in §3** as you go, so a resumed session knows where it is.
- If an agent reports a blocked precondition or a disagreement, resolve it before continuing
  rather than re-running the prompt.

**Inner-loop cost.** P1–P10 verify with `mvnw.cmd -q compile` and greps only — no Docker, no
broker, seconds per prompt. P11–P13 run their own new test classes. P14 is the only prompt
that needs the full infrastructure and a running fleet.

---

## 2. Standing rules

Every prompt below says "apply the standing rules". They are:

1. **Repository.** `agri-fleet-analytics`, branch `develop`. Windows: use `mvnw.cmd`, not
   `./mvnw`.
2. **Authority.** `docs/IMPLEMENTATION-PLAN.md` is the work order. `CLAUDE.md` §"Coding style"
   governs every line you write. `SPEC.md` §0 governs the documents. Where the prompt and the
   plan disagree, the plan wins — and tell the human.
3. **Scope.** Do exactly what the prompt says. Bugs 14–18 and the review's "Minor" list are
   out of scope — see plan §0 for the explicit do-not-touch list. If you notice an unrelated
   defect, write one line about it in your report and change nothing.
4. **Style.** Plain Java: no Lombok, no SLF4J, `System.out.println` for logging, constructor
   injection only, verbose names, no new interfaces or factories. Comments are rare and cite a
   SPEC section rather than narrating the code.
5. **No commits.** Leave changes in the working tree. Do not `git commit`, `git add`,
   `git stash` or create a branch unless the human asks.
6. **Disagree out loud.** If a step looks wrong, would break something, or cannot be done as
   written — stop, explain, and propose. Never silently substitute a different approach.
7. **Report honestly.** If a verification command fails, say so and paste the output. Do not
   describe work as done that you did not verify.
8. **Read before you edit**, and read only what the prompt lists. Do not survey the repo.

---

## 3. Prompt index

| # | Prompt | Bugs | Plan § | Files touched | Done |
|---|--------|------|--------|---------------|------|
| P1 | Live condition from the message timestamp | 9 | A1 | 4 java | ☐ |
| P2 | Watermark sealing; split read from mark | 1, 2 | A3 | 3 java | ☐ |
| P3 | `AssetSnapshot`; one lock, one clock | 6, 2 | A2 | 3 java | ☐ |
| P4 | Drop `@Transactional`; seal after the write | 3, 4, 5 | A4 | 1 java | ☐ |
| P5 | 503 for missing state, 404 for unknown id | 7 | B1 | 1 java | ☐ |
| P6 | Nullable DTO fields | 13 | B2 | 3 java | ☐ |
| P7 | Remove the apologetic simulator comment | 8 | C1 | 1 java | ☐ |
| P8 | Frontend guards and reset | 10, 11, 12 | D1–D3 | 3 js | ☐ |
| P9 | `SPEC.md` and `ARCHITECTURE.md` edits | 7–9 | E1–E6 | 2 md | ☐ |
| P10 | `CLAUDE.md` wording — prepare, do not commit † | — | E7 | 0 | ☐ |
| P11 | `CounterAccumulationServiceTest` | 1, 2, 6 | F1 | 1 test | ☐ |
| P12 | `AssetStatePublisherTest` + `AssetAssessmentServiceTest` | 3–5, 9 | F2, F3 | 2 tests | ☐ |
| P13 | `AssetViewServiceTest` | 7, 13 | F4 | 1 test | ☐ |
| P14 | Full verification runbook | all | G | 0 | ☐ |

P5–P8 are independent of P1–P4 and of each other; run them in any order, or in parallel
sessions, as long as they all land before P9.

† **P10 is listed in plan order but is best run after P13.** Its whole output is proposed
wording about which test classes now exist, so running it before P11–P13 produces a proposal
about files that are not there yet.

---

## 4. The prompts

### P1 — Live condition from the message timestamp

| | |
|---|---|
| **Bug** | 9 |
| **Plan** | §3, step A1 |
| **Precondition** | none |

```text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Make SPEC §5.3 STALE depend on the age of the most recent telemetry message instead of on
when that message happened to arrive, and delete latestArrivalRealTime entirely.

WHY
CounterAccumulationService.accumulate stamps latestArrivalRealTime = Instant.now() for every
accepted message, including the thousands replayed by the SPEC §3.2 startup burst. Because
liveCondition measures staleness against that stamp, a reading from twenty hours ago is
published as a live NORMAL for the duration of the burst.

READ FIRST
- docs/IMPLEMENTATION-PLAN.md, sections 0, 1 and step A1 in section 3
- telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/service/business/AssetAccumulator.java
- telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/service/business/AssetAssessmentService.java
- telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/service/business/CounterAccumulationService.java  (lines 62-90 and 185-191 only)
- telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/service/business/AssetStatePublisher.java  (lines 72-110 only)

CHANGE
1. AssetAccumulator: delete the latestArrivalRealTime field (line 35), its javadoc comment
   (line 34), its getter and its setter (lines 86-92).
2. CounterAccumulationService: delete the setLatestArrivalRealTime call (line 88) and the
   public latestArrivalRealTime(AssetId) accessor (lines 189-191). Keep latestMessage() and
   keep setting it for every accepted message — a stale reading is now correctly reported as
   STALE by its own timestamp, so there is no reason to withhold it.
3. AssetAssessmentService.liveCondition: change the signature to
   (TelemetryMessage latestMessage, Instant now). Return STALE when latestMessage is null or
   when the message's own timestamp is more than STALE_AFTER before now. Leave STALE_AFTER,
   the ALERT row, the CAUTION row and their top-to-bottom order exactly as they are.
4. A message timestamped in the future relative to now must NOT read as STALE. Duration.between
   returns a negative duration there, so compareTo(STALE_AFTER) > 0 already handles it — just
   do not "helpfully" wrap it in Math.abs.
5. AssetStatePublisher: update the call site at lines 90-92. Pass Instant.now() for now at
   this point; P4 hoists it to a single read per publish.
6. Update the javadoc on liveCondition so it explains the timestamp basis. The existing text
   argues for arrival time and is now wrong.

DO NOT
- Do not introduce a Clock bean, a time provider, or a test-only clock seam.
- Do not use TelemetryConsumer's CATCH_UP_BEHIND here. This fix does not need it, and
  TelemetryConsumer itself must not be modified.
- Do not change AssetViewService's rule that telemetry is null only when latestTimestamp is
  null. That is correct and P9 documents it.

VERIFY
  grep -rn "latestArrivalRealTime" telemetryprocessor/                # expect: no output
  grep -n  "Instant.now()" telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/service/business/AssetAssessmentService.java   # expect: no output
  cd telemetryprocessor && mvnw.cmd -q compile

DONE WHEN
- latestArrivalRealTime appears nowhere in the repository.
- AssetAssessmentService contains no Instant.now().
- The module compiles.

REPORT
The new liveCondition signature and body, and confirmation that the three ordered SPEC §5.3
rows below STALE are unchanged.
```

---

### P2 — Watermark sealing; split the read from the mark

| | |
|---|---|
| **Bugs** | 1, 2 |
| **Plan** | §3, step A3 |
| **Precondition** | P1 green |

```text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Stop flushing an hourly bucket to hourly_aggregate while it is still receiving messages, and
separate "which hours are ready" (a pure read) from "these hours were written" (a mutation).

WHY
takeSealedBuckets seals every bucket whose hour precedes the WALL-CLOCK current hour. During
the SPEC §3.2 burst the consumer is replaying hours-old timestamps, so a bucket that is still
filling qualifies, gets written as a partial snapshot, and — because sealedHours guarantees a
single hand-over — is never written again. GET /api/history then under-reports those hours
permanently. The same method also hands back the live CounterSet objects and marks them
sealed in the same call, which is bugs 2 and 3.

READ FIRST
- docs/IMPLEMENTATION-PLAN.md, step A3 in section 3
- telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/service/business/CounterAccumulationService.java
- telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/service/business/CounterSet.java
- telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/service/business/AssetAccumulator.java

CHANGE
1. CounterSet: add a copy facility — a copy constructor or a copy() method returning a new
   instance holding the same eight values. Change nothing else about the class.
2. Replace takeSealedBuckets (lines 198-210) with two synchronized methods:

   sealableBuckets(AssetId assetId)
     A PURE READ. Returns, oldest first, the buckets whose hour is strictly before
     hourStartOf(lastProcessedTimestamp) for that asset and that are not already in
     sealedHours. Returns an empty map when lastProcessedTimestamp is null. Returns COPIES of
     each CounterSet. Mutates nothing: no sealedHours insertion, no pruning, no field writes.

   markSealed(AssetId assetId, Set<Instant> hourStarts)
     Adds those hours to sealedHours, then runs the existing pruneBuckets logic for that
     accumulator.

3. pruneBuckets keeps its current rule unchanged: drop the oldest bucket only when the map
   exceeds RETAINED_BUCKETS and that oldest hour is already sealed. Move the call site into
   markSealed; do not change the logic.
4. AssetStatePublisher.flushSealedBuckets currently calls takeSealedBuckets, which both read
   and marked. Update it to: call sealableBuckets, build the rows, call
   hourlyAggregateRepository.saveAll, and THEN call markSealed for those hours. Keep it a
   per-asset loop for now — P4 restructures the whole of publish().

   This ordering is not optional bookkeeping. If you call sealableBuckets without ever
   calling markSealed, every retained hour is rewritten on every 5-second tick forever; if
   you call markSealed before the write, you have reinstated bug 3. Leave this prompt in a
   correct state, not a placeholder one.

5. Add one comment above sealableBuckets citing SPEC §4.2 and stating the watermark argument
   in a sentence: message timestamps are strictly increasing per asset, so once a message for
   a later hour has been processed, no later message can land in an earlier hour.

DO NOT
- Do not add a wall-clock backstop for silent assets. It was considered and rejected in plan
  §1; adding one reintroduces bug 1 during the burst.
- Do not return the live CounterSet instances. Copies are the point.
- Do not let sealableBuckets write to any field.
- Do not change RETAINED_BUCKETS, ROLLING_WINDOW_HOURS, hourStartOf or rollingWindowStart.

VERIFY
  grep -rn "takeSealedBuckets" telemetryprocessor/                    # expect: no output
  grep -c "Instant.now()" telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/service/business/CounterAccumulationService.java
      # expect: 2  - the two remaining are inside rolling24hCounters and windowCoverageHours,
      #              which P3 removes. Do NOT count on line numbers here; P1 shifted them.
  cd telemetryprocessor && mvnw.cmd -q compile

DONE WHEN
- sealableBuckets is a pure read returning copies, driven by the asset's own
  lastProcessedTimestamp and not by wall-clock.
- markSealed is the only place sealedHours grows and the only place pruning happens.
- flushSealedBuckets writes first and marks second, so the module is in a correct state even
  before P4 restructures it.
- The module compiles.

REPORT
The two new method bodies, and the copy mechanism you added to CounterSet.
```

---

### P3 — `AssetSnapshot`: one lock, one clock read

| | |
|---|---|
| **Bugs** | 6, 2 |
| **Plan** | §3, step A2 |
| **Precondition** | P1 and P2 green |

```text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Collapse the accumulator's six per-value readers into one snapshotOf(assetId, now) taken under
a single lock acquisition from a single clock read, and make the class free of Instant.now().

WHY
AssetStatePublisher.publish currently calls counterAccumulationService eight times per asset
(lines 79, 80, 91, 92, 98, 99, 100, 105 — latestMessage twice), so 32 lock acquisitions per
cycle. utilizationPercent and productiveSharePercent each re-read Instant.now() and recompute
the counters internally, so a sequence straddling an hour boundary can store a percentage that
is not the quotient of the counters stored beside it. SPEC §1 requires every published number
to be traceable to a counter.

READ FIRST
- docs/IMPLEMENTATION-PLAN.md, step A2 in section 3
- telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/service/business/CounterAccumulationService.java
- telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/service/business/AssetStatePublisher.java

CHANGE
1. New file AssetSnapshot.java in the same service/business package. A plain POJO: final
   fields, one constructor, getters, no setters, no annotations, no Spring. It carries
   everything one asset_state row needs from the accumulator:
     CounterSet lifetimeCounters
     CounterSet rolling24hCounters
     double windowCoverageHours
     Double utilizationPercent
     Double productiveSharePercent
     TelemetryMessage latestMessage
2. CounterAccumulationService gains:
     public synchronized AssetSnapshot snapshotOf(AssetId assetId, Instant now)
   It computes all six values from the single now it is handed, inside one lock acquisition.
3. Demote lifetimeCounters, rolling24hCounters, windowCoverageHours, utilizationPercent,
   productiveSharePercent and latestMessage to PRIVATE helpers. The time-dependent ones take
   now as a parameter instead of calling Instant.now().
4. The two percentage helpers must take the already-computed values rather than recomputing:
     utilizationPercent(CounterSet rolling24hCounters, double windowCoverageHours)
     productiveSharePercent(CounterSet rolling24hCounters)
   This is what makes the published quotient exact rather than approximately right.
5. Keep the SPEC §4.5 rule intact: a percentage is null, never 0.0, when its denominator is 0.
6. Update AssetStatePublisher to call snapshotOf once per asset and read every value off the
   snapshot. Pass Instant.now() at the call site for now; P4 hoists it.
7. After this step CounterAccumulationService must expose exactly four public methods:
   accumulate, snapshotOf, sealableBuckets, markSealed.

DO NOT
- Do not add setters to AssetSnapshot, and do not make it a Spring bean, a record, or an
  interface. A plain POJO with a constructor is what the rest of this codebase looks like.
- Do not mutate latestMessage anywhere. It is handed out by reference and is treated as
  immutable once TelemetryGenerator has built it.
- Do not change the arithmetic of windowCoverageHours, rolling24hCounters or lifetimeCounters.
  This step moves the clock read out; it does not touch the formulas.

VERIFY
  grep -n "public synchronized" telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/service/business/CounterAccumulationService.java
      # expect exactly four: accumulate, snapshotOf, sealableBuckets, markSealed
  grep -n "Instant.now()" telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/service/business/CounterAccumulationService.java
      # expect: no output
  cd telemetryprocessor && mvnw.cmd -q compile

DONE WHEN
- CounterAccumulationService contains no Instant.now() and has four public methods.
- For any snapshot, utilizationPercent equals
  rolling24hCounters.totalWorkingHours / windowCoverageHours * 100 exactly, and
  productiveSharePercent equals
  rolling24hCounters.totalWorkingHours / rolling24hCounters.engineHours * 100 exactly.
- An asset that has not reported yields windowCoverageHours 0.0 and both percentages null.
- The module compiles.

REPORT
The AssetSnapshot field list, the snapshotOf body, and the count of accumulator calls per
asset in publish() before and after.
```

---

### P4 — Drop `@Transactional`; seal after the write

| | |
|---|---|
| **Bugs** | 3, 4, 5 |
| **Plan** | §3, step A4 |
| **Precondition** | P1, P2 and P3 green |

```text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Hold the lock across the whole write, make @PostConstruct's call path honest, and mark hours
sealed only after their rows have actually been written.

WHY
Three faults in one method. @Transactional is applied by a proxy AROUND publish(), so the
synchronized monitor is released before commit and a second thread can start rewriting the
same four rows against an uncommitted transaction. resetOnStartup calls this.publish() from
@PostConstruct, where no proxy exists, so the annotation does nothing there at all.
flushSealedBuckets marks hours sealed as an in-memory side effect that no rollback undoes, so
a failed write loses those hours permanently.

READ FIRST
- docs/IMPLEMENTATION-PLAN.md, step A4 in section 3
- telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/service/business/AssetStatePublisher.java

CHANGE
1. Remove @Transactional from publish() (line 72) and remove the now-unused import
   org.springframework.transaction.annotation.Transactional (line 10). publish() stays
   synchronized.
2. Read the clock ONCE at the top of publish() into a local Instant now, and use it for
   snapshotOf, liveCondition and row.setUpdatedAt. Delete the Instant.now() at line 106.
3. Rewrite the body in this strict order:
     a. read now
     b. per asset: snapshotOf(assetId, now) and sealableBuckets(assetId); keep each asset's
        hour set for step (f)
     c. evaluate flags, status, recommendation and live condition from the snapshot
     d. assign priority ranks across all four rows (FleetRankingService, unchanged)
     e. assetStateRepository.saveAll(rows), then hourlyAggregateRepository.saveAll(hourRows)
     f. only now: markSealed(assetId, hours) for each asset
4. Steps (e) and (f) must not be reordered or interleaved. If either saveAll throws, no hour
   is marked sealed and the next tick retries it.
5. resetOnStartup keeps @PostConstruct and keeps calling publish() directly. With the
   annotation gone that is no longer a bypass — update its javadoc, which currently explains
   why the call is safe in terms that no longer apply.
6. flushSealedBuckets either folds into publish() or keeps its name and RETURNS the per-asset
   hour sets. It must no longer be able to mark hours sealed on its own.

WHAT IS DELIBERATELY GIVEN UP
asset_state and hourly_aggregate are now written in two transactions rather than one. Both
remain internally atomic: SimpleJpaRepository.saveAll is itself @Transactional, so all four
asset_state rows still land together and ARCHITECTURE §4's split-rank concern is unaffected.
resetOnStartup's delete-then-write also stays two transactions; P5's 503 covers the window
where the table is empty. P9 updates ARCHITECTURE §4 to say all this. Do not try to restore
cross-table atomicity.

DO NOT
- Do not re-add @Transactional anywhere in this module "for safety". That is exactly bugs 4
  and 5.
- Do not introduce TransactionTemplate, a TransactionSynchronization, a new writer bean, or
  self-injection. Plan §1 settled on plain repository transactions.
- Do not make publish() non-synchronized.

VERIFY
  grep -rn "Transactional" telemetryprocessor/src/main/java                  # expect: no output
  grep -c "Instant.now()" telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/service/business/AssetStatePublisher.java
      # expect: 1
  grep -rn "Instant.now()" telemetryprocessor/src/main/java | wc -l
      # expect: 2  (AssetStatePublisher once, TelemetryConsumer once - the latter is out of scope)
  cd telemetryprocessor && mvnw.cmd -q compile

DONE WHEN
- No @Transactional in the module; publish() reads the clock exactly once.
- markSealed is called strictly after both saveAll calls, for every asset.
- All four asset_state rows go out in a single saveAll invocation.
- The module compiles.

REPORT
The rewritten publish() body, and confirm that a throw from either saveAll leaves sealedHours
untouched.
```

---

### P5 — 503 for missing state, 404 only for an unknown id

| | |
|---|---|
| **Bug** | 7 |
| **Plan** | §4, step B1 |
| **Precondition** | none |

```text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Stop reporting "TelemetryProcessor has not written its baselines yet" as 404 Not Found.

WHY
SPEC §6.1 requires all four assets on every response and §6.2 restricts 404 to an id outside
the four §2 assets. When asset_state is empty — before TelemetryProcessor has ever run, or
inside its reset window — getFleet returns an empty array and getAsset returns 404, so a
transient infrastructure state is reported as "this machine does not exist".

READ FIRST
- docs/IMPLEMENTATION-PLAN.md, step B1 in section 4
- operatorapi/src/main/java/com/mariolagger/operatorapi/service/business/AssetViewService.java

CHANGE
1. getFleet (lines 43-53): when the repository returns no rows, throw
   ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ...). The message must name the
   cause — no state has been published yet — in the style of the existing messages in this
   class.
2. getAsset (lines 56-63): keep requireKnownAsset and its 404 for an id outside the four.
   Change only the orElseThrow for a KNOWN id with no row, from NOT_FOUND to
   SERVICE_UNAVAILABLE.
3. getLatestTelemetry (lines 66-84) is UNCHANGED and keeps returning 200.

WHY §6.4 STAYS 200
Four entries with liveCondition STALE is exactly the SPEC §6.4 shape for assets that have not
reported, it is truthful, and it is the endpoint the live strip polls every ten seconds. A 503
there would blank the strip for no gain. P9 records this asymmetry in the SPEC edit. Do not
"make it consistent".

DO NOT
- Do not add a @ControllerAdvice or a custom error DTO. CLAUDE.md mandates
  ResponseStatusException thrown from the service layer.
- Do not touch application.properties. spring.web.error.include-message=always is already set,
  so the message reaches the body.
- Do not give OperatorApi any knowledge of the SPEC §4.4 baselines. ARCHITECTURE §2 keeps this
  module free of product rules, which is why the answer is 503 and not a synthesized row.

VERIFY
  cd operatorapi && mvnw.cmd -q compile
  grep -n "SERVICE_UNAVAILABLE\|NOT_FOUND" src/main/java/com/mariolagger/operatorapi/service/business/AssetViewService.java
      # expect: two SERVICE_UNAVAILABLE (getFleet, getAsset) and one NOT_FOUND (requireKnownAsset)

DONE WHEN
- Empty repository: /api/assets is 503, /api/assets/TRACTOR is 503.
- Any state: /api/assets/BULLDOZER is 404.
- Empty repository: /api/telemetry/latest is still 200 with four STALE entries.

REPORT
The three call sites and the exact message text you chose for each 503.
```

---

### P6 — Nullable DTO fields

| | |
|---|---|
| **Bug** | 13 |
| **Plan** | §4, step B2 |
| **Precondition** | none |

```text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Let a NULL database column reach the JSON as null instead of unboxing into a
NullPointerException or being fabricated as 0.

WHY
latest_pto_engaged, latest_ground_speed_kmh, latest_load_factor, latest_temperature_c and the
six flag value/threshold columns are all nullable in docker/database-script.sql, but the DTO
fields and round2(double) are primitives. A null unboxes and throws.

READ FIRST
- docs/IMPLEMENTATION-PLAN.md, step B2 in section 4
- operatorapi/src/main/java/com/mariolagger/operatorapi/dto/TelemetryView.java
- operatorapi/src/main/java/com/mariolagger/operatorapi/dto/FlagView.java
- operatorapi/src/main/java/com/mariolagger/operatorapi/service/business/AssetViewService.java  (lines 102-138 and 183-212)

CHANGE
1. TelemetryView: ptoEngaged becomes Boolean; groundSpeedKmh, loadFactor and temperatureC
   become Double. Update the setters' parameter types to match.
2. RENAME isPtoEngaged() to getPtoEngaged(). This is not cosmetic: Jackson recognises isX() as
   a getter only for the primitive boolean, so leaving the name alone silently drops the field
   from the JSON with no error anywhere.
3. FlagView: value and threshold become Double.
4. AssetViewService: use the existing round2OrNull (line 210) for all five nullable numeric
   mappings in toTelemetryView and addFlagIfActive, and assign the Boolean straight through.

DO NOT
- Do not change CounterSetView or HistoryRow. Every column behind them is NOT NULL in the
  schema, and primitives are correct there.
- Do not change anything in frontend/. formatBoolean(null) and formatNumber(null) already
  return an em dash, which is precisely the behaviour CLAUDE.md's frontend rule asks for.
- Do not substitute 0.0 or false for a null. Rendering a missing reading as a real-looking
  zero is the failure mode this fix exists to prevent.

VERIFY
  grep -rn "isPtoEngaged" operatorapi/                                # expect: no output
  grep -n "private Double\|private Boolean" operatorapi/src/main/java/com/mariolagger/operatorapi/dto/TelemetryView.java
      # expect: 4 fields
  cd operatorapi && mvnw.cmd -q compile

DONE WHEN
- A row with all latest_* columns null maps to a telemetry object of nulls without throwing.
- A reporting asset's /api/telemetry/latest response still contains a ptoEngaged key.

REPORT
The changed field/getter signatures, and confirmation that CounterSetView and HistoryRow were
left as primitives.
```

---

### P7 — Remove the apologetic simulator comment

| | |
|---|---|
| **Bug** | 8 |
| **Plan** | §5, step C1 |
| **Precondition** | none |

```text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Stop the simulator from documenting an invented rule in a code comment. The band itself moves
into SPEC §3.2 in P9.

WHY
targetTemperatureC carries a comment conceding that SPEC §3.2 names bands for WORKING and IDLE
only, and that 55-70 for TRANSPORT was chosen in code to fill the gap. SPEC §0 forbids
inventing rules outside the governed documents.

READ FIRST
- docs/IMPLEMENTATION-PLAN.md, step C1 in section 5
- simulator/src/main/java/com/mariolagger/simulator/service/business/TelemetryGenerator.java  (lines 145-185 only)

CHANGE
Replace the two-line comment at lines 166-167 with a bare "// SPEC §3.2" citation, matching
the style of the other comments in the class.

DO NOT
- Do not change any temperature number. randomBetween(55, 70) for TRANSPORT stays exactly as
  it is; P9 writes that band into SPEC so the code becomes conformant without moving.
- Do not touch the excursion logic, the drift constant, or any other comment in the file.

VERIFY
  cd simulator && mvnw.cmd -q compile
  grep -n "randomBetween(55, 70)" src/main/java/com/mariolagger/simulator/service/business/TelemetryGenerator.java
      # expect: still present

DONE WHEN
- The TRANSPORT band still reads randomBetween(55, 70).
- No comment in the file describes a value as absent from SPEC.

REPORT
One line confirming the comment replaced and the value unchanged.
```

---

### P8 — Frontend guards and reset

| | |
|---|---|
| **Bugs** | 10, 11, 12 |
| **Plan** | §6, steps D1–D3 |
| **Precondition** | none |

```text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Three independent frontend fixes: an exception that blanks the screen, stale data surviving a
failed fetch, and the one unescaped interpolation in the codebase.

WHY
(10) statusClass() returns '' for any status outside the three known values, and
     DOMTokenList.add('') raises a SyntaxError. That aborts renderFleetDiagram mid-loop, so
     renderFleetRanking never runs and the whole screen goes blank.
(11) renderFleetDiagram iterates the RESPONSE, so a node with no matching asset keeps the
     previous status, rank and utilization. On error app.js sets fleetAssets to [] but never
     re-renders the diagram or the metric tiles, so both keep showing the last good values
     beside an error message.
(12) assetChipHtml interpolates assetId into a class attribute without escapeHtml. Not
     reachable today, but it is the single exception to the rule the rest of the file follows.

READ FIRST
- docs/IMPLEMENTATION-PLAN.md, section 6
- frontend/js/fleet.js
- frontend/js/format.js  (lines 107-135 and 237-265 only)
- frontend/js/app.js     (lines 35-56 only)

CHANGE
1. fleet.js renderFleetDiagram (lines 17-18): add a class only when it is non-empty. Leave
   statusClass's contract as it is — it is used elsewhere inside class="..." strings where ''
   is harmless. Do not substitute a placeholder class name; an unknown status should simply
   leave the node unstyled.
2. fleet.js renderFleetDiagram: iterate the four .node-group elements in the DOM and look each
   one up in assets, instead of iterating assets. A node with no match is reset — status and
   live classes removed, and the .node-status, .node-value and .node-rank texts set to
   EM_DASH. This fixes the error case and the short-response case in one place.
3. fleet.js renderFleetMetrics (lines 74-94): when assets is empty, render EM_DASH rather than
   0 for the three status counts and for the reporting tile. An unknown fleet is not a fleet
   of zero criticals, and CLAUDE.md's frontend rule already forbids rendering a missing value
   as 0.
4. app.js refreshFleet catch branch (lines 49-55): call renderFleet([], null, selectAsset)
   before showFleetRankingError, so the reset in (2) and (3) actually runs.
5. format.js assetChipHtml (lines 251-255): escape the interpolated asset id.

DO NOT
- Do not touch frontend/css/app.css. No step here needs a colour, class or token, and
  CLAUDE.md treats the design system as a hard constraint.
- Do not add a framework, a bundler, a dependency, or a build step.
- Do not add a poll-overlap guard to setInterval. That is a "Minor" item, out of scope.
- Do not reorder the <script> tags in index.html; they load in dependency order.

VERIFY
  node --check frontend/js/fleet.js
  node --check frontend/js/format.js
  node --check frontend/js/app.js
      # node v24 is installed on this machine. --check parses only, it does not execute, so
      # browser globals like document are irrelevant. All three must print nothing.

  Then confirm by reading — there is no JS test harness in this project, and the behavioural
  check is P14 step 5.8:
  - no classList.add() call in fleet.js can receive an empty string
  - renderFleetDiagram's loop is over the four DOM nodes, not over assets
  - renderFleet([], null, fn) leaves no stale text or status class in any node
  - every ${...} inside a quoted attribute in frontend/js/ passes through escapeHtml

DONE WHEN
- All five changes are in place and all three files pass node --check.

REPORT
The rewritten renderFleetDiagram, and the em-dash handling you chose for the metric tiles.
```

---

### P9 — `SPEC.md` and `ARCHITECTURE.md` edits

| | |
|---|---|
| **Bugs** | 7, 8, 9 |
| **Plan** | §7, steps E1–E6 |
| **Precondition** | P1–P8 all green |

```text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Bring the two governed documents in line with the code that P1-P8 just landed, in the same
change set, as SPEC §0's Document Change Protocol requires.

READ FIRST
- docs/SPEC.md, section 0 (the change protocol itself), then 3.2, 5.3, 6.1, 6.2, 6.4
- docs/ARCHITECTURE.md, section 4
- docs/IMPLEMENTATION-PLAN.md, section 7

CHANGE
E1  SPEC §3.2 — add the TRANSPORT band to the temperatureC sentence, beside the existing
    WORKING and IDLE bands. The value is the one the simulator already uses. Pairs with P7.
E2  SPEC §5.3 — restate the STALE row in terms of the age of the most recent MESSAGE rather
    than when one was "received". This aligns the row with §5.3's own opening sentence,
    "derived from the most recent message only", and with P1.
E3  SPEC §6.4 — widen the closing paragraph: telemetry is null only for an asset that has
    never reported in this run; one that reported and then fell silent is STALE and KEEPS its
    last reading, so the operator can see what it was doing when the signal stopped. Pairs
    with P1.
E4  SPEC §6.1 and §6.2 — add 503 Service Unavailable (no state has been published yet) to
    both. State in §6.2 that 404 is reserved for an id outside the four §2 assets. Note that
    §6.4 deliberately stays 200 with four STALE entries. Pairs with P5.
E5  ARCHITECTURE §4 — rewrite the sealing sentence: an hour reaches hourly_aggregate once that
    asset's OWN STREAM has moved past it, not once wall-clock has. Pairs with P2.
E6  ARCHITECTURE §4 — update the write-cadence and startup-reset paragraphs: asset_state and
    hourly_aggregate are two transactions; the four asset_state rows remain atomic because
    saveAll is itself transactional; an hour is marked flushed only after its write returns;
    the API answers 503 during the reset window. Pairs with P4 and P5.

THEN run SPEC §0's protocol step 2: scan the other governed files for anything these edits
made stale, and report what you find without fixing anything outside this prompt's list.

DO NOT
- GLOSSARY.md needs NO change. No identifier it owns is added, removed or renamed by this
  work — latestArrivalRealTime was never a glossary name, and a status code is not a name. Do
  not go looking for one.
- Do not edit CLAUDE.md. That is P10, and it needs human sign-off first.
- Do not restate a rule in more than one file. SPEC §0 forbids it: ARCHITECTURE may reference
  a SPEC section but never repeat its content.
- Do not renumber any existing section.
- Keep the edits minimal. These are surgical sentence changes, not rewrites.

VERIFY
  Re-read each edited section and confirm:
  - every changed rule appears in exactly one file
  - ARCHITECTURE cites SPEC rather than repeating it
  - the four documents are mutually consistent (SPEC §0 Pre-Completion Check)

DONE WHEN
- E1 through E6 are applied and the pre-completion check passes.

REPORT
A diff-style summary of each of the six edits, plus anything the §0 scan flagged as stale
that you deliberately left alone.
```

---

### P10 — `CLAUDE.md` wording: prepare, do not commit

| | |
|---|---|
| **Bug** | — (plan step E7) |
| **Plan** | §7, step E7 |
| **Precondition** | P9 green; run after P11–P13 for accurate wording |

```text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Prepare — and do NOT apply — the CLAUDE.md "Repo status" wording that the new test classes
make necessary.

WHY
CLAUDE.md currently says "only the three contextLoads() smoke tests exist so far". P11-P13 add
four real unit test classes, so that sentence becomes false. CLAUDE.md is rank 0 in the SPEC §0
hierarchy and was NOT covered by the doc-editing authority granted for this work, which
extended to SPEC.md and ARCHITECTURE.md only.

READ FIRST
- CLAUDE.md, the "Repo status" section
- docs/IMPLEMENTATION-PLAN.md, step E7 in section 7

CHANGE
Nothing on disk. Produce, in your reply only:
1. The exact current sentence(s) that became false.
2. Proposed replacement wording — factual, same voice, naming which service classes now have
   unit tests and which still do not.
3. Any other statement in CLAUDE.md that P1-P13 made stale, with proposed wording.

DO NOT
- Do not write to CLAUDE.md. Not even a comment. Not even if it looks obviously correct.
- Do not create a patch file or a scratch copy in the repository.

DONE WHEN
- The proposed wording is in your reply and CLAUDE.md is byte-identical to before.

REPORT
The before/after wording, and an explicit note that you did not modify the file.
```

---

### P11 — `CounterAccumulationServiceTest`

| | |
|---|---|
| **Bugs** | 1, 2, 6 |
| **Plan** | §8, step F1 |
| **Precondition** | P1–P4 green |

```text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Pin the accumulator's behaviour so bugs 1, 2 and 6 cannot come back, and pin the SPEC §4.1 and
§4.2 rules that were previously untested.

WHY THIS IS EASY NOW
After P1-P3, CounterAccumulationService contains no Instant.now(): every time-dependent value
is passed in. Express each case by handing it a fixed Instant. Do NOT introduce a Clock bean,
a time-mocking library, or a test-only seam — the class is already deterministic and keeping
it that way is the point.

READ FIRST
- docs/IMPLEMENTATION-PLAN.md, step F1 in section 8
- telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/service/business/CounterAccumulationService.java
- telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/dto/TelemetryMessage.java
- CLAUDE.md, the "Tests" bullet under Coding style

CREATE
telemetryprocessor/src/test/java/com/mariolagger/telemetryprocessor/service/business/CounterAccumulationServiceTest.java

Plain JUnit 5. No @SpringBootTest, no Spring context, no database, no broker, no mocks needed
— construct CounterAccumulationService directly. Add a small private helper that builds a
TelemetryMessage from its seven fields; the DTO has setters and building them inline is
unreadable.

A USEFUL FIXTURE
Europe/Vienna is UTC+2 in August 2026, so:
  2026-08-13T06:30:00Z  is local 08:30, bucket hourStart 06:00Z
  2026-08-13T06:59:30Z  is local 08:59:30, bucket hourStart 06:00Z
  2026-08-13T07:00:10Z  is local 09:00:10, bucket hourStart 07:00Z  (delta 40s, credited)

CASES
Bug 1 regression — the important one:
  1. Accumulate several messages all inside 06:00Z-06:59Z. Assert sealableBuckets returns
     EMPTY, even though real wall-clock is far past that hour. Before the fix this returned
     the still-filling bucket.
  2. Accumulate one more at 07:00:10Z. Assert sealableBuckets now returns exactly the 06:00Z
     hour, and that its engineHours equals the sum credited inside that hour.
Bug 2:
  3. Mutate a CounterSet returned by sealableBuckets. Assert a second call returns the
     original, unmutated values.
Seal lifecycle:
  4. sealableBuckets is repeatable until markSealed, and returns nothing for those hours
     afterwards.
SPEC §4.2:
  5. A delta above 60 s credits nothing but still advances the last-processed timestamp
     (assert via the next message's delta being measured from it).
  6. A delta of exactly 60 s IS credited.
  7. An out-of-order or duplicate timestamp updates nothing at all — counters, latest message
     and last-processed timestamp all unchanged.
  8. The first message of an asset credits nothing (delta undefined).
SPEC §4.1:
  9. After a mixed WORKING / TRANSPORT / IDLE sequence, engineHours equals
     totalWorkingHours + transportHours + idleHours exactly.
 10. Productive work is derived from ptoEngaged && groundSpeedKmh > 2, NOT from the message's
     workingStatus — send a message with workingStatus WORKING but pto false and assert it
     lands in idleHours.
SPEC §4.5, bug 6:
 11. For a snapshot with a non-null utilizationPercent, assert it equals
     totalWorkingHours / windowCoverageHours * 100 for that same snapshot.
 12. An asset that never reported: windowCoverageHours 0.0, both percentages null.

DO NOT
- Do not modify any production file. If a test cannot be written without one, stop and report.
- Do not modify TelemetryProcessorApplicationTests (bug 15, out of scope).
- Do not use Thread.sleep or the real clock anywhere.

VERIFY
  cd telemetryprocessor && mvnw.cmd test -Dtest=CounterAccumulationServiceTest

DONE WHEN
- The class runs green with no infrastructure running.

REPORT
The test-method names and the run output.
```

---

### P12 — `AssetStatePublisherTest` and `AssetAssessmentServiceTest`

| | |
|---|---|
| **Bugs** | 3, 4, 5, 9 |
| **Plan** | §8, steps F2 and F3 |
| **Precondition** | P1–P4 and P11 green |

```text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Two test classes: one pinning the write-then-seal ordering, one pinning the live condition and
the status rules.

READ FIRST
- docs/IMPLEMENTATION-PLAN.md, steps F2 and F3 in section 8
- telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/service/business/AssetStatePublisher.java
- telemetryprocessor/src/main/java/com/mariolagger/telemetryprocessor/service/business/AssetAssessmentService.java

CREATE 1 — AssetStatePublisherTest (bugs 3, 4, 5)
telemetryprocessor/src/test/java/com/mariolagger/telemetryprocessor/service/business/AssetStatePublisherTest.java

@ExtendWith(MockitoExtension.class). Mock ONLY the two repositories. Use REAL instances of
CounterAccumulationService, FlagEvaluationService, AssetAssessmentService and
FleetRankingService — they are deterministic after P1-P3 and stubbing them would test the
mocks instead of the code.

Because the six constructor arguments are a mix of mocks and real objects, @InjectMocks will
not do what you want. Construct AssetStatePublisher by hand in a @BeforeEach, keeping a
reference to the real CounterAccumulationService so the tests can feed it messages and query
sealableBuckets afterwards.

  1. Prefer a BEHAVIOURAL assertion over a call-order one. Feed the real accumulator messages
     that seal an hour, then:
       - happy path: call publish(), then assert sealableBuckets returns nothing for that hour
       - failure path: make assetStateRepository.saveAll throw, call publish() expecting the
         throw, then assert sealableBuckets STILL returns that hour
     That second case is bug 3 exactly, and it tests the property that matters rather than the
     sequence of calls.
  2. ArgumentCaptor on assetStateRepository.saveAll: assert all four rows arrive in ONE
     invocation, each with a distinct priorityRank of 1..4.
  3. resetOnStartup deletes both tables and then writes four rows carrying the SPEC §4.4
     baselines — check one asset's lifetimeEngineHours against AssetBaselines.
  4. Every row in one publish() carries the same updatedAt value, proving the clock is read
     once.

CREATE 2 — AssetAssessmentServiceTest (bug 9)
telemetryprocessor/src/test/java/com/mariolagger/telemetryprocessor/service/business/AssetAssessmentServiceTest.java

Plain JUnit 5, no mocks. Pass fixed Instants.

  5. A message timestamped 20 hours before now is STALE whatever its readings say — the bug 9
     regression.
  6. A null message is STALE.
  7. A message timestamped slightly in the FUTURE is not STALE.
  8. SPEC §5.3 top-to-bottom: fresh HIGH vibration is ALERT; fresh temperature above the alert
     bar is ALERT; fresh ELEVATED is CAUTION; loadFactor exactly at the caution bar is
     CAUTION; otherwise NORMAL.
  9. SPEC §5.2: one SEVERE is CRITICAL; two MODERATE is CRITICAL; one MODERATE is WATCH; none
     is GOOD.
 10. SPEC §5.5: each status maps to its exact recommendation string, en dash included.

DO NOT
- Do not modify any production file.
- Do not stub CounterAccumulationService. If publish() is hard to test with a real one, that
  is a signal worth reporting, not a reason to mock.
- Do not assert on System.out output.

VERIFY
  cd telemetryprocessor && mvnw.cmd test -Dtest=AssetStatePublisherTest
  cd telemetryprocessor && mvnw.cmd test -Dtest=AssetAssessmentServiceTest

DONE WHEN
- Both classes run green with no infrastructure running.

REPORT
Test-method names, both run outputs, and whether the failure-path test needed any production
change (it should not).
```

---

### P13 — `AssetViewServiceTest`

| | |
|---|---|
| **Bugs** | 7, 13 |
| **Plan** | §8, step F4 |
| **Precondition** | P5 and P6 green |

```text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Pin the OperatorApi status codes and the null-column mapping.

READ FIRST
- docs/IMPLEMENTATION-PLAN.md, step F4 in section 8
- operatorapi/src/main/java/com/mariolagger/operatorapi/service/business/AssetViewService.java
- operatorapi/src/main/java/com/mariolagger/operatorapi/entity/AssetState.java

CREATE
operatorapi/src/test/java/com/mariolagger/operatorapi/service/business/AssetViewServiceTest.java

@ExtendWith(MockitoExtension.class), @Mock AssetStateRepository. No Spring context, no
database. Add a private helper that builds an AssetState entity so each test states only the
fields it cares about.

CASES
Bug 7:
  1. findAllByOrderByPriorityRankAsc returns an empty list -> getFleet throws
     ResponseStatusException with status 503.
  2. getAsset("BULLDOZER") throws 404, and the repository is never touched.
  3. getAsset("TRACTOR") with findById empty throws 503.
  4. getFleet with four rows returns them in the repository's order, untouched — the service
     must not re-sort.
Bug 13:
  5. A row with every latest_* column null: getLatestTelemetry does not throw, and the entry's
     telemetry is null (latestTimestamp was null).
  6. A row with latestTimestamp set but latest_ground_speed_kmh null: the telemetry object is
     present and groundSpeedKmh is null, NOT 0.0.
Flags and shape:
  7. A row with all four flag severities null produces an empty flags list.
  8. An active flag carries its flag name, severity, counter name, scope, value and threshold,
     each rounded to two decimals.
  9. getLatestTelemetry returns all four assets ordered by assetId ascending even when the
     repository returns fewer rows, with the missing ones STALE and telemetry null.

DO NOT
- Do not modify any production file.
- Do not modify OperatorApiApplicationTests (bug 15, out of scope).
- Do not assert on exact 503/404 message text beyond it being non-empty; the wording is not
  contractual.

VERIFY
  cd operatorapi && mvnw.cmd test -Dtest=AssetViewServiceTest

DONE WHEN
- The class runs green with no infrastructure running.

REPORT
Test-method names and the run output.
```

---

### P14 — Full verification runbook

| | |
|---|---|
| **Bugs** | all |
| **Plan** | §9 |
| **Precondition** | P1–P13 green |

```text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Prove the fixes hold in the running system, not just in unit tests. This is the only prompt
that needs Docker and three JVMs.

READ FIRST
- docs/IMPLEMENTATION-PLAN.md, sections 9 and 10

STEP 1 - build and test
  mvn -q compile                          # repo root
  then in each of simulator/, telemetryprocessor/, operatorapi/:  mvnw.cmd test
TelemetryProcessor and OperatorApi need Postgres reachable for their contextLoads() test, and
TelemetryProcessor also needs RabbitMQ. Start the infra (step 2) first. This is expected and
CLAUDE.md records it — it is not a bug to fix.

STEP 2 - infrastructure
  cd docker && docker compose up -d
No schema change was made by this work, so `down -v` is NOT required here.

STEP 3 - cold-start 503 check (bug 7). This one DOES need a wiped volume:
  cd docker && docker compose down -v && docker compose up -d
Start ONLY OperatorApi, then (curl.exe 8.21 ships with Windows at C:\WINDOWS\system32; note
the null sink is NUL, not /dev/null, and always spell it curl.exe so PowerShell does not
resolve `curl` to Invoke-WebRequest, which takes different flags and throws on 4xx/5xx):

  curl.exe -s -o NUL -w "%{http_code}\n" http://localhost:8083/api/assets           # expect 503
  curl.exe -s -o NUL -w "%{http_code}\n" http://localhost:8083/api/assets/TRACTOR   # expect 503
  curl.exe -s -o NUL -w "%{http_code}\n" http://localhost:8083/api/assets/BULLDOZER # expect 404
  curl.exe -s -o NUL -w "%{http_code}\n" http://localhost:8083/api/telemetry/latest # expect 200

Stop OperatorApi.

STEP 4 - full run
Start TelemetryProcessor, then Simulator, then OperatorApi.

STEP 5 - checks. Record PASS/FAIL and the evidence for each.
 5.1 (bugs 5, 7) Between TelemetryProcessor starting and Simulator starting, /api/assets is
     200 with four assets at their SPEC §4.4 baselines, TRACTOR at WATCH, the rest GOOD.
 5.2 (bug 9) DURING the burst, /api/telemetry/latest shows STALE for all four assets. Before
     the fix it showed NORMAL beside a twenty-hour-old timestamp. If the window is too brief
     to catch, lower simulator.backfill.hours temporarily and restart the Simulator.
 5.3 (bug 9) After the burst, all four read NORMAL / CAUTION / ALERT with timestamps within
     seconds of now.
 5.4 (bugs 1, 3) THE KEY REGRESSION CHECK. Every COMPLETED hour must read engineHours ~ 1.00.
     The burst steps 60 s and credits 60/3600 h per step, so a full hour is exactly 1.0. Before
     the fix, one or two hours per run read visibly less. Only the first and last hours of the
     range may legitimately be partial. In PowerShell:

       $from = (Get-Date).ToUniversalTime().AddHours(-24).ToString("yyyy-MM-ddTHH:mm:ssZ")
       $to   = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
       foreach ($id in 'TRACTOR','MOWER','BALER','MIXER') {
         $rows = (Invoke-RestMethod "http://localhost:8083/api/history?from=$from&to=$to&assetId=$id").rows
         "$id : $($rows.Count) rows"
         if ($rows.Count -ge 3) {
           $bad = $rows[1..($rows.Count-2)] | Where-Object { $_.engineHours -lt 0.98 }
           if ($bad) { "  TRUNCATED HOURS:"; $bad | Format-Table hourStart, engineHours }
           else { "  all complete hours >= 0.98" }
         } else { "  too few rows to judge - let it run longer" }
       }

     Expect four lines of "all complete hours >= 0.98". The Count -ge 3 guard matters: with
     fewer than three rows the 1..(Count-2) range runs backwards in PowerShell and silently
     re-orders the slice instead of erroring.
 5.5 (bug 1) Each asset has about 24 rows over the window, and every row satisfies
     engineHours = totalWorkingHours + transportHours + idleHours.
 5.6 (bug 6) For each asset in /api/assets, utilizationPercent equals
     rolling24hCounters.totalWorkingHours / windowCoverageHours * 100 to the published
     precision.
 5.7 (bug 1) Let it run across a whole-hour boundary in Europe/Vienna. The newly completed
     hour appears in /api/history within about five seconds and also reads ~1.00.
 5.8 (bug 11) Open http://localhost:8080. Stop OperatorApi and press Refresh: every diagram
     node and every metric tile resets to an em dash and the ranking shows the error. Restart
     OperatorApi and confirm the screen recovers on the next poll.

STEP 6 - scope audit
  git status
  git diff --stat
Confirm nothing outside the plan's scope was touched: no module directory renamed, no
contextLoads() test modified, no .cursorrules, no README.md, no docker-compose.yml, no
database-script.sql, no app.css, and no edit to the four IMPLEMENTATION-PLAN.md citations
listed in plan §0.

Two additions to docs/ ARE expected and are not scope creep: IMPLEMENTATION-PLAN.md and
IMPLEMENTATION-PROMPTS.md themselves. Both are transient work orders. Leave them in place —
deleting them is the human's call once the change set is merged.

DO NOT
- Do not commit anything.
- Do not "fix" a failing check by changing the check. Report the failure.
- Do not run docker compose down -v after step 3; the later checks need the running data.

REPORT
The plan §10 Definition of done checklist with each box ticked or explicitly failed, the
5.4 output for all four assets, and the git diff --stat.
```

---

## 5. If a prompt goes wrong

- **A verification fails.** Do not run the next prompt. Re-read the step in
  `docs/IMPLEMENTATION-PLAN.md`, fix forward, re-verify. The plan is the authority, not the
  prompt.
- **An agent deviated from the plan.** `git diff` the affected files, revert with
  `git checkout -- <path>`, and re-run the prompt with the deviation named explicitly in the
  DO NOT block.
- **A precondition is not actually met.** Prompts P1–P4 are order-dependent and will produce
  confusing compile errors out of order. Check the §3 status table before starting.
- **The plan itself looks wrong.** Stop and raise it. Plan §1 lists twelve decisions that were
  settled deliberately; several have rejected alternatives that look attractive in isolation —
  a wall-clock seal backstop, `TransactionTemplate`, synthesizing baselines in OperatorApi.
  Reversing one of those is a human decision.

---

## 6. Closing prompt

Run once, after P14.

```text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Final review of the whole change set before it goes to the human.

DO
1. git diff — read every hunk. For each, name the bug number from
   docs/IMPLEMENTATION-PLAN.md §0 that it serves. Any hunk you cannot attribute to a bug in
   1-13 is scope creep: report it and propose reverting it.
2. Confirm the plan §10 Definition of done list, box by box, from the diff rather than from
   memory.
3. Confirm the four documents pass SPEC §0's Pre-Completion Check: no rule defined in more
   than one file, all four mutually consistent.
4. Confirm CLAUDE.md is unmodified (P10 was preparation only).
5. List anything you noticed but deliberately did not fix, with one line each.

DO NOT
- Do not make any further code change during this review. If something needs fixing, report it
  and stop.
- Do not commit.

REPORT
The attribution table (hunk -> bug), the completed checklist, and the deliberately-not-fixed
list.
```
