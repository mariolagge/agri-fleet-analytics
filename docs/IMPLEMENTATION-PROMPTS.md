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



---



---



---



---



---



---



---



---



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
