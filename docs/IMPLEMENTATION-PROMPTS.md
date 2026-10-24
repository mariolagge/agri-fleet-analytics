# docs\IMPLEMENTATION-PROMPTS.md

Copy-paste prompts for the work order in `docs/IMPLEMENTATION-PLAN.md`. One prompt per plan
phase (P3 has two), run in order, each ending in a state the owner can check before the next.

**This file is a transient work order, like the plan.** It defines no rule of its own. Where
a prompt and the plan disagree, the plan wins and the agent says so. Both files are deleted
in P10.

---

## 1. How to use this file

- **Run the prompts in the order of §3.** Each one names its precondition; do not start a
  prompt whose precondition is not met.
- **Start a fresh agent session for every prompt.** Every prompt names the files to read,
  so no session needs the previous one's context.
- **Copy everything inside the four-backtick fence**, nothing outside it. The table above
  each fence is for the owner, not the agent.
- **Replace the placeholders** written as `<…>` before sending. Only P8 and P10 have them.
- **After each prompt:** read the agent's report, check the diff, do the owner tasks, then
  commit with the suggested message. The agent has already set the phase's status in plan
  §5.1; the status table lives only there.
- **If the agent asks a question,** answer it in the same session, and let the same agent
  finish the phase.

---

## 2. Standing rules

Every prompt starts with "Apply the standing rules". They are:

1. Follow `docs/IMPLEMENTATION-PLAN.md` §0 (agent operating rules) completely. In short:
   one phase only, never commit, push or create issues, no production behaviour change,
   ask multiple-choice questions when something is not pinned down, report in the §0 rule 7
   format, set the phase status in §5.1 on success.
2. The plan is the authority. This prompt only adds focus, pitfalls and exact verification
   commands. If the prompt and the plan disagree, follow the plan and report the conflict.
3. Read only what *READ FIRST* lists, plus files you must edit. Do not survey the repository.
4. Read every file completely before editing it.
5. Commands marked **(Bash)** run in the Bash tool (Git Bash). Commands marked
   **(PowerShell)** run in the PowerShell tool. Run Maven only through the module's
   `.\mvnw.cmd` wrapper, from inside the module directory.
6. If a verification command fails, report the failure with its output. Never describe
   unverified work as done.
7. If you notice an unrelated defect, write one line about it in the report and change
   nothing.

---

## 3. Prompt index

| Prompt | Plan phase | Precondition | Owner before | Owner after |
|--------|-----------|--------------|--------------|-------------|
| P0-H | P0 (helper, optional) | — | — | Create milestone and issues; fill plan §5.1 *GitHub #* |
| P1 | P1 | P0 done | — | Review, commit |
| P2 | P2 | P1 committed | — | Review, commit |
| P3a | P3, draft | P2 committed | — | Correct and tidy both BPMN files in the modeler, export SVGs |
| P3b | P3, finish | P3a done, owner modeler work done | — | Review, commit |
| P4 | P4 | P3b committed | — | Review, commit |
| P5 | P5 | P4 committed | — | Review, commit |
| P6 | P6 | P5 committed | — | Review, commit |
| P7 | P7 | P6 committed | — | Review, commit |
| P8 | P8 | P7 committed | Decide the cut (plan §5.3) | Check diagrams on mermaid.live, review, commit |
| P9 | P9 | P8 committed | Take the dashboard screenshot | Review, commit, push `develop` |
| P10 | P10 | P9 committed and pushed | Have the three employer names ready | Close P10 issue and milestone, commit |

P4 depends only on P2, so it can run before P3 is finished if the modeler work is delayed.
Every other prompt runs strictly in order.

---

## 4. The prompts

### P0-H — Issue texts ready to paste (optional)

| | |
|---|---|
| **Plan** | §5.2, §7 P0 |
| **Precondition** | — |
| **Writes files** | No |

````text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2. Exception to plan §0 rule 2
and rule 8: this prompt executes no phase and edits no file.

GOAL
Print the milestone and the ten GitHub issues of plan §5.2 as text I can paste into GitHub.

READ FIRST
- docs/IMPLEMENTATION-PLAN.md §5.1, §5.2 and §7 (the Goal and Acceptance criteria of P1-P10)

DO
1. Print the milestone title, due date and description exactly as in §5.2.
2. For each phase P1-P10, print one block:
   - Title: P<n>: <Title from §5.1>
   - Label: enhancement (P5, P6) or documentation (all others)
   - Body: the phase's Goal paragraph and Acceptance criteria list, verbatim from §7,
     then the line "Milestone M1."
   Put each body inside its own fenced Markdown code block so I can copy it.

DO NOT
- Do not create issues, milestones or labels (no gh commands).
- Do not write any file.

DONE WHEN
- Eleven blocks are printed: one milestone, ten issues.
````

---

### P1 — Fix stale references, add RUNNING.md


| | |
|---|---|
| **Plan** | §7 P1 |
| **Precondition** | P0 done (issue numbers in plan §5.1) |
| **Files** | `README.md`, `docs/ARCHITECTURE.md`, `docs/GLOSSARY.md`, `docs/SPEC.md`, new `docs/RUNNING.md` |

````text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Execute plan phase P1: remove every reference to the deleted SPEC.md §0 and §8, stop README
from claiming that CLAUDE.md holds run commands, and write docs/RUNNING.md.

READ FIRST
- CLAUDE.md
- docs/IMPLEMENTATION-PLAN.md §0-§4 and §7 P1
- README.md, docs/ARCHITECTURE.md, docs/GLOSSARY.md (whole files)
- docs/SPEC.md lines 140-150
- The old run instructions: (Bash) git show 3430dc2^:CLAUDE.md
  Use only its "Common commands" section.
- docker/docker-compose.yml and simulator/src/main/resources/application.properties
  (to confirm ports and the backfill property before writing them down)

DO
Plan P1 steps 1-7, in order.

WATCH OUT
- Every fact in RUNNING.md must be checked against the repository, not copied blindly
  from the old CLAUDE.md: nginx port 8080 (NGINX_PORT default in docker-compose.yml),
  RabbitMQ management port 15672, module directories in lowercase (simulator,
  telemetryprocessor, operatorapi), property simulator.backfill.hours.
- -Pdev works from a module directory because each module's pom inherits the root pom,
  which defines the dev profile.
- Write RUNNING.md for a reader who has never seen the project: short numbered steps,
  each command in its own code block, Windows command first, macOS/Linux second.
- Start RUNNING.md with the heading "# docs\RUNNING.md" and one sentence saying what it
  owns (plan §4.1).
- In GLOSSARY.md and ARCHITECTURE.md, change only the lines plan P1 names.

DO NOT
- Do not rewrite README.md beyond the edits in step 6. The full rewrite is P9.
- Do not touch CLAUDE.md.

VERIFY
1. (Bash) git grep -nE '§0|SPEC\.md.{0,2} §8|\(§8\)' -- README.md docs ':!docs/IMPLEMENTATION-PLAN.md' ':!docs/IMPLEMENTATION-PROMPTS.md'
   -> no output
2. (Bash) git grep -n "Common commands" -- README.md docs ':!docs/IMPLEMENTATION-PLAN.md' ':!docs/IMPLEMENTATION-PROMPTS.md'
   -> no output
3. For every relative link in README.md: (PowerShell) Test-Path <target> -> True
4. RUNNING.md contains all seven parts of plan P1 step 7 (check one by one).
5. (Bash) git diff --stat -> only the five files listed above.

DONE WHEN
All P1 acceptance criteria in the plan are met.

REPORT
In the plan §0 rule 7 format.
````

---

### P2 — Stakeholders and needs

| | |
|---|---|
| **Plan** | §7 P2, §6.1 |
| **Precondition** | P1 committed |
| **Files** | new `docs/STAKEHOLDERS.md`, `docs/GLOSSARY.md`, `README.md` |

````text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Execute plan phase P2: create the stakeholder analysis with three stakeholders and ten
stakeholder needs, and register the two new ID kinds in GLOSSARY.md.

READ FIRST
- CLAUDE.md
- docs/IMPLEMENTATION-PLAN.md §0-§4, §6.1 and §7 P2
- docs/GLOSSARY.md §1
- docs/SPEC.md §1 and §7
- README.md

DO
Plan P2 steps 1-3.

WATCH OUT
- Copy both tables of plan §6.1 verbatim: same IDs, same wording, same order. Do not
  improve the wording.
- The §3 Elicitation text is fixed in plan P2 step 1. Use it word for word.
- The GLOSSARY.md naming table has three columns: Kind | Convention | Example. The two new
  rows go directly below the "Requirement IDs" row.
- STAKEHOLDERS.md links to PROCESS.md, which does not exist until P3. That is expected;
  keep the reference as plain text (`PROCESS.md`), as other docs do, not as a link.

DO NOT
- Do not add NEED- or STK- IDs to any other file in this phase.
- Do not touch SPEC.md (attributes come in P4).

VERIFY
1. (Bash) grep -oE '^\| `STK-[A-Z]+`' docs/STAKEHOLDERS.md | wc -l -> 3
2. (Bash) grep -oE '^\| `NEED-[A-Z]+(-[A-Z]+)*`' docs/STAKEHOLDERS.md | wc -l -> 10
3. (Bash) git grep -nE 'NEED-|STK-' -- ':!docs/STAKEHOLDERS.md' ':!docs/GLOSSARY.md' ':!docs/IMPLEMENTATION-PLAN.md' ':!docs/IMPLEMENTATION-PROMPTS.md'
   -> no output
4. Compare the two tables with plan §6.1 line by line.

DONE WHEN
All P2 acceptance criteria in the plan are met.

REPORT
In the plan §0 rule 7 format.
````

---

### P3a — BPMN models and PROCESS.md, draft

| | |
|---|---|
| **Plan** | §7 P3 |
| **Precondition** | P2 committed |
| **Files** | new `docs/process/maintenance-planning-as-is.bpmn`, `docs/process/maintenance-planning-to-be.bpmn`, `docs/PROCESS.md`; `README.md` |
| **Owner after** | Plan P3 owner tasks 1–2 (correct, tidy, save, export SVG) |

````text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Execute the drafting part of plan phase P3: write both BPMN 2.0 files so they open in
demo.bpmn.io, and write docs/PROCESS.md. The owner will then correct and tidy the models
in a modeler and export the SVGs; P3b finishes the phase.

READ FIRST
- CLAUDE.md
- docs/IMPLEMENTATION-PLAN.md §0-§4, §2 (rows "As-is source" and "To-be trigger"), §6.1
  and §7 P3 (whole section)
- docs/SPEC.md §5.5 and §7
- docs/GLOSSARY.md §6 and §8
- docs/STAKEHOLDERS.md

DO
1. Write docs/process/maintenance-planning-as-is.bpmn from the as-is element table and
   flow lists in plan P3.
2. Write docs/process/maintenance-planning-to-be.bpmn from the to-be element table and
   flow lists.
3. Write docs/PROCESS.md with sections 1-4 as specified in plan P3. Embed the two SVGs with
   relative paths (process/maintenance-planning-as-is.svg, …) even though they do not
   exist yet.
4. Add the PROCESS.md row to the README.md documents table (plan P3 acceptance criteria).

BPMN FILE STRUCTURE (both files)
- Root: bpmn:definitions with xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL",
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI",
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC",
  xmlns:di="http://www.omg.org/spec/DD/20100524/DI",
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance",
  id="AsIs_Definitions" / "ToBe_Definitions", targetNamespace="http://bpmn.io/schema/bpmn".
- One bpmn:collaboration holding the participants and the message flows.
- The expanded Farmer participant has processRef to the process. Collapsed participants
  have no processRef.
- One bpmn:process (isExecutable="false") holding all flow nodes, sequence flows, text
  annotations and associations.
- Every flow node lists its bpmn:incoming and bpmn:outgoing sequence flow IDs.
- One bpmndi:BPMNDiagram with one bpmndi:BPMNPlane whose bpmnElement is the
  collaboration ID. Inside it: a BPMNShape (with dc:Bounds) for every participant, flow
  node and text annotation; a BPMNEdge (with at least two di:waypoint) for every sequence
  flow, message flow and association. Participant shapes have isHorizontal="true".

LAYOUT (the owner tidies it later; aim for readable, not perfect)
- Farmer pool: x=150, y=60, width as needed (about 1900), height 440. Main path on
  y≈200 (element centre), the second start event and the workshop branch on y≈380.
- Collapsed pools below the Farmer pool, height 60, same x and width: as-is Workshop at
  y=560; to-be AgriFleet at y=560 and Workshop at y=660.
- Message flows: vertical edges from the bottom of the task/event to the top of the
  collapsed pool, or the reverse.
- Element sizes and spacing: plan P3 modeling rules.

EXPECTED COUNTS (check them; they follow from the plan's lists)
- As-is: 15 flow nodes, 15 sequence flows (AsIs_Flow_1..15), 2 message flows
  (AsIs_Message_1..2), 1 association, 1 text annotation, 2 participants.
- To-be: 17 flow nodes, 19 sequence flows (ToBe_Flow_1..19), 4 message flows
  (ToBe_Message_1..4), 3 associations, 3 text annotations, 3 participants.
- A bullet like "A → B → C" in the plan is two sequence flows, numbered left to right.

WATCH OUT
- Names in the XML are exactly the names in the element tables, including en dashes
  ("15–20") and colons.
- Gateway answers ("Yes", "No", "CRITICAL", "WATCH", "GOOD") are the name attribute of the
  outgoing sequence flow.
- Merge gateways have no name attribute.
- PROCESS.md narrative paragraphs must describe only elements that exist in the model and
  only system features that exist in SPEC.md. No notifications, no maintenance log, no
  workshop access to the system.

DO NOT
- Do not add elements, lanes, data objects or extra annotations beyond the plan's tables.
- Do not create the .svg files.

VERIFY (PowerShell), for each of the two files as $f:
1. $x = [xml](Get-Content $f -Raw)
   -> no error
2. $ids = $x.SelectNodes('//@id') | ForEach-Object { $_.Value }
   $refs = $x.SelectNodes('//@bpmnElement') | ForEach-Object { $_.Value }
   $refs | Where-Object { $ids -notcontains $_ }
   -> no output (every diagram element points to an existing model element)
3. $kinds = 'task','startEvent','endEvent','intermediateCatchEvent','exclusiveGateway',
   'sequenceFlow','messageFlow','participant','textAnnotation','association'
   $x.SelectNodes('//*') | Where-Object { $kinds -contains $_.LocalName } |
     ForEach-Object { $_.id } | Where-Object { $refs -notcontains $_ }
   -> no output (every model element has a shape or edge)
4. Count the elements per kind and compare with EXPECTED COUNTS.
5. Every ID in the plan's element tables appears exactly once as an id attribute.

DONE WHEN
- Both .bpmn files pass VERIFY 1-5.
- PROCESS.md has sections 1-4 with the content plan P3 specifies.
- Do NOT set the P3 status in plan §5.1 yet; P3b does that.

REPORT
In the plan §0 rule 7 format. Under "Owner tasks", list plan P3 owner tasks 1 and 2, and
add: "Then run prompt P3b."
````

---

### P3b — BPMN models and PROCESS.md, finish

| | |
|---|---|
| **Plan** | §7 P3 |
| **Precondition** | P3a done; owner has saved both corrected `.bpmn` files and exported both `.svg` files |
| **Files** | `docs/PROCESS.md`, plan §7 P3 lists (only if the owner changed elements), the two `.bpmn` files (read only) |

````text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Finish plan phase P3 after the owner's modeler work: bring PROCESS.md and the plan's P3
element lists in line with the saved models, and check all P3 acceptance criteria.

READ FIRST
- CLAUDE.md
- docs/IMPLEMENTATION-PLAN.md §0-§4 and §7 P3
- docs/process/maintenance-planning-as-is.bpmn and maintenance-planning-to-be.bpmn
  (the owner's saved versions)
- docs/PROCESS.md

DO
1. Confirm that both .svg files exist next to their .bpmn files. If one is missing, stop
   and ask the owner.
2. Compare each saved .bpmn with the plan's element tables and flow lists. List every
   element or flow the owner added, removed or renamed.
3. If there are differences: the owner's version wins. Update the plan's P3 element tables
   and flow lists to match the files, and update the PROCESS.md text (walk-through
   paragraphs, pain-point table, decision table) so it describes the saved models.
   Owner-created elements keep the IDs the modeler generated; do not rename them.
4. Run every P3 acceptance criterion in the plan.

WATCH OUT
- Do not edit the .bpmn or .svg files. If a file fails to parse, stop and report.
- The owner's corrections come from real farm experience. Never revert them to the plan's
  original wording.

VERIFY
- (PowerShell) VERIFY 1-3 from prompt P3a, re-run on both saved files -> no output.
- (PowerShell) Test-Path for both .svg files -> True.
- Every image path in PROCESS.md resolves to an existing .svg.

DONE WHEN
All P3 acceptance criteria in the plan are met (the "opens in demo.bpmn.io" criterion is
confirmed by the owner's modeler work). Set the P3 status in plan §5.1.

REPORT
In the plan §0 rule 7 format, including the list from DO step 2 (or "no differences").
````

---

### P4 — REQ attributes in SPEC

| | |
|---|---|
| **Plan** | §7 P4, §6.2 |
| **Precondition** | P2 committed (P3 may still be open) |
| **Files** | `docs/SPEC.md` |

````text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Execute plan phase P4: add a short preface to SPEC.md and one Priority/Source line under
each of its 35 requirements.

READ FIRST
- CLAUDE.md
- docs/IMPLEMENTATION-PLAN.md §0-§4, §6.2 and §7 P4
- docs/SPEC.md (whole file)
- docs/STAKEHOLDERS.md

DO
Plan P4 steps 1-3. Work through SPEC.md top to bottom, one REQ at a time, taking the values
from the §6.2 table row with the same REQ ID.

WATCH OUT
- The attribute line is its own paragraph: one blank line before it, one blank line after.
  It goes directly after the paragraph that starts with **REQ-…**, even when explanatory
  prose, a table or a code block follows that paragraph.
- Exact format: *Priority: Must · Source: NEED-A, NEED-B*
  The separator is a middle dot (U+00B7) with one space on each side; needs are separated
  by comma + space, in the order of §6.2.
- Do not reflow, re-wrap or reword any existing line.

DO NOT
- Do not change any REQ text, table, heading or example.
- Do not add Won't items or any other attribute.

VERIFY (Grep tool on docs/SPEC.md, output_mode count)
1. ^\*Priority: (Must|Should|Could) · Source: NEED-   -> 35
2. ^\*\*REQ-[A-Z-]+\.\*\*                             -> 35
3. ^\*Priority: Must ·    -> 21
   ^\*Priority: Should ·  -> 11
   ^\*Priority: Could ·   -> 3
4. (Bash) comm -23 <(grep -oE 'NEED-[A-Z]+(-[A-Z]+)*' docs/SPEC.md | sort -u) <(grep -oE '^\| `NEED-[A-Z]+(-[A-Z]+)*' docs/STAKEHOLDERS.md | sed 's/^| `//' | sort -u)
   -> no output (every need used is declared)
5. (Bash) git diff --numstat docs/SPEC.md -> the deleted-lines count is 0
6. Spot check five REQs against §6.2: REQ-GOAL-TRACE, REQ-SIM-BURST, REQ-SCOPES,
   REQ-API-ASSET, REQ-DECISIONS.

DONE WHEN
All P4 acceptance criteria in the plan are met.

REPORT
In the plan §0 rule 7 format.
````

---

### P5 — REQ IDs in code comments

| | |
|---|---|
| **Plan** | §7 P5, §6.3 |
| **Precondition** | P4 committed |
| **Files** | every tracked non-Markdown file containing `SPEC` (about 60 files) |

````text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Execute plan phase P5: replace every SPEC section citation in code comments with the REQ ID
it refers to.

READ FIRST
- CLAUDE.md
- docs/IMPLEMENTATION-PLAN.md §0-§4, §6.3 and §7 P5
- docs/SPEC.md (whole file: you must know each REQ's text to choose between REQs of one
  section)
- The work list: (Bash) git grep -n "SPEC" -- ':!*.md'   (expected: 150 lines)

DO
1. Process the work list module by module, in this order: simulator, telemetryprocessor,
   operatorapi, frontend, docker. For each file: read it, apply plan P5 rules 1-7, move on.
2. After each Java module: (PowerShell) .\mvnw.cmd -q compile in that module.
3. Keep a mapping table while you work: file:line | old reference | new reference. It goes
   into the report so the owner can review every choice.

WATCH OUT
- Use git grep, never a plain recursive grep: the untracked target/ folders contain old
  copies of application.properties.
- Section with several REQs (§3.1, §3.2, §4.1, §4.3): read the commented code and choose
  the REQ whose shall it implements. Examples: a dwell-time switch -> REQ-SIM-CYCLE; a
  vibration level rule -> REQ-SIM-VIBRATION; a comment about the backfill window ->
  REQ-SIM-BURST; rolling window vs lifetime -> REQ-SCOPES.
- A comment can cite several sections ("SPEC §5.2, §5.3, §5.4, §5.5"): replace each one
  ("REQ-STATUS, REQ-LIVE, REQ-RANK, REQ-RECOMMEND").
- The only known rule-5 case is frontend/js/format.js citing SPEC.md §8 for DST; it becomes
  "SPEC §4.3".
- Comment-only changes to docker/database-script.sql need no database reset.

DO NOT
- Do not change any code token, string literal, identifier or blank line.
- Do not touch references to ARCHITECTURE.md or GLOSSARY.
- Do not touch Markdown files.

VERIFY
1. (Bash) git grep -n "SPEC" -- ':!*.md'
   -> only lines allowed by plan P5 rules 5 and 7; name each one in the report.
2. (Bash) git diff -U0 -- ':!*.md' | grep -E '^[+-]' | grep -vE '^(\+\+\+|---)' | grep -vE '^[+-]\s*(//|\*|/\*|#|--|<!--)'
   -> no output, or only lines whose change is a trailing comment on unchanged code.
3. (Bash) comm -23 <(git grep -ohE 'REQ-[A-Z]+(-[A-Z]+)*' -- ':!*.md' | sort -u) <(grep -oE '^\*\*REQ-[A-Z]+(-[A-Z]+)*' docs/SPEC.md | sed 's/^\*\*//' | sort -u)
   -> no output (every REQ ID in code is declared in SPEC.md).
4. (PowerShell) .\mvnw.cmd -q compile in simulator, telemetryprocessor and operatorapi
   -> success.
5. Baseline tests (plan §4.4):
   in telemetryprocessor: .\mvnw.cmd test "-Dtest=CounterAccumulationServiceTest,AssetAssessmentServiceTest,AssetStatePublisherTest" -> 31 tests, 0 failures
   in operatorapi:        .\mvnw.cmd test "-Dtest=AssetViewServiceTest" -> 9 tests, 0 failures

DONE WHEN
All P5 acceptance criteria in the plan are met.

REPORT
In the plan §0 rule 7 format, plus the mapping table from DO step 3.
````

---

### P6 — REQ-tagged unit tests

| | |
|---|---|
| **Plan** | §7 P6, §4.3, §6.4 |
| **Precondition** | P5 committed |
| **Files** | 4 existing test classes, 4 new test classes (paths in plan P6) |

````text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Execute plan phase P6: give every existing service test a @DisplayName that starts with the
requirement it verifies, and add the four new test classes.

READ FIRST
- CLAUDE.md
- docs/IMPLEMENTATION-PLAN.md §0-§4, §6.4 and §7 P6
- docs/SPEC.md: REQ-FLAGS, REQ-RANK, REQ-API-HISTORY, REQ-TELEMETRY-WORKING-STATUS,
  REQ-TELEMETRY-SHAPE
- Style reference: operatorapi/src/test/java/com/mariolagger/operatorapi/service/business/AssetViewServiceTest.java
- Classes under test and what they need:
  - telemetryprocessor/.../service/business/FlagEvaluationService.java, FlagEvaluation.java,
    CounterSet.java (constructor argument order)
  - telemetryprocessor/.../service/business/FleetRankingService.java
  - operatorapi/.../service/business/HistoryService.java,
    operatorapi/.../repository/HourlyAggregateRepository.java,
    operatorapi/.../entity/HourlyAggregate.java (how to build a row)
  - simulator/.../service/business/TelemetryGenerator.java, AssetSimulationState.java,
    simulator/.../dto/TelemetryMessage.java (getter names)
- The four existing test classes in telemetryprocessor and operatorapi

DO
Plan P6 steps 1-5, in order. Run each module's tests as soon as its classes are done.

WATCH OUT
- @DisplayName format: "<ID>: <sentence>", for example
  @DisplayName("REQ-ACCUMULATE: a gap above 60 s credits nothing but advances the last processed timestamp")
  The sentence restates the method name in plain English, lower case after the colon.
- Assert HTTP status the way AssetViewServiceTest does (getStatusCode() against HttpStatus).
- HistoryServiceTest runs with Mockito strict stubs: an unused stub fails the test. In the
  400 and 404 cases the repository is never called; stub nothing there.
- HistoryServiceTest test 8 verifies the call to
  findByHourStartLessThanAndHourStartGreaterThanOrderByHourStartAscAssetIdAsc(to, from minus 1 h).
- FleetRankingServiceTest: both maps need all four AssetId keys, or the service throws a
  NullPointerException that is not a REQ-RANK failure.
- TelemetryGeneratorTest: the generator's Random is unseeded. Do not seed it, do not add a
  constructor, do not change the generator. Both assertions hold on every random path.
- If a new test fails, first check the test against the REQ text. If the test is right and
  the production code is wrong, stop and report (plan §0 rule 5). Do not fix production
  code.

DO NOT
- Do not rename or change the body of any existing test method.
- Do not touch the *ApplicationTests classes.
- Do not add dependencies or test libraries.

VERIFY (PowerShell, from each module directory)
1. telemetryprocessor:
   .\mvnw.cmd test "-Dtest=CounterAccumulationServiceTest,AssetAssessmentServiceTest,AssetStatePublisherTest,FlagEvaluationServiceTest,FleetRankingServiceTest"
   -> Tests run: 42, Failures: 0, Errors: 0
2. operatorapi: .\mvnw.cmd test "-Dtest=AssetViewServiceTest,HistoryServiceTest"
   -> Tests run: 17, Failures: 0, Errors: 0
3. simulator: .\mvnw.cmd test "-Dtest=TelemetryGeneratorTest"
   -> Tests run: 2, Failures: 0, Errors: 0
4. (Bash) git grep -c "@Test" -- '*Test.java' ':!*ApplicationTests.java'
   (Bash) git grep -c "@DisplayName" -- '*Test.java' ':!*ApplicationTests.java'
   -> identical counts per file
5. (Bash) git grep -hoE '@DisplayName\("[^:]+:' -- '*Test.java' | sort | uniq -c
   -> every ID is a declared REQ- ID or "ARCHITECTURE §4"
6. (Bash) git diff --stat -- '*/src/main/*' -> no output

DONE WHEN
All P6 acceptance criteria in the plan are met.

REPORT
In the plan §0 rule 7 format, plus the output of VERIFY 5.
````

---

### P7 — Traceability matrix

| | |
|---|---|
| **Plan** | §7 P7 |
| **Precondition** | P6 committed |
| **Files** | new `docs/TRACEABILITY.md`, `README.md` |

````text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Execute plan phase P7: write docs/TRACEABILITY.md, tracing every need to its requirements
and every requirement to its implementing files and verifying tests.

READ FIRST
- CLAUDE.md
- docs/IMPLEMENTATION-PLAN.md §0-§4 and §7 P7
- docs/SPEC.md (REQ order and the Priority/Source lines)
- docs/STAKEHOLDERS.md (NEED order)
- README.md

COLLECT THE DATA (Bash) — build the tables only from these outputs, never from memory
1. Requirement order, priority and source:
   grep -nE '^\*\*REQ-|^\*Priority:' docs/SPEC.md
2. Implementing files (one line per file and REQ):
   git grep -oE 'REQ-[A-Z]+(-[A-Z]+)*' -- '*/src/main/*' frontend docker | sort -u
3. Verifying tests (count, REQ, test class):
   git grep -oE '@DisplayName\("REQ-[A-Z]+(-[A-Z]+)*:' -- '*/src/test/*' | sed -E 's#^.*/([A-Za-z]+)\.java:@DisplayName\("(REQ-[A-Z-]+):#\2 \1#' | sort | uniq -c

DO
Write TRACEABILITY.md with the heading, sentence and sections 1-3 exactly as plan P7
specifies, then add the README.md documents row.

WATCH OUT
- REQ-API-ASSET is a prefix of REQ-API-ASSETS. The commands above match whole IDs (and the
  DisplayName command includes the colon), so use their output, not a substring search.
- Module prefix from the path: simulator/ -> Simulator, telemetryprocessor/ ->
  TelemetryProcessor, operatorapi/ -> OperatorApi, frontend/ -> frontend, docker/ -> docker.
- Java files are listed by class name (no .java); other files by file name.
- List entries within a cell separated by "<br>", sorted by module then name.
- Section 3 must name every Must REQ with "—" in Verified by.

DO NOT
- Do not add a verification-method column or any column the plan does not list.
- Do not edit SPEC.md, code or tests to make the matrix look better.

VERIFY
1. Table 1 has 10 data rows; table 2 has 35 data rows (count them).
2. (Bash) grep -oE '^\| `?REQ-[A-Z]+(-[A-Z]+)*' docs/TRACEABILITY.md | sort | uniq -d
   -> no output (no REQ twice)
3. Spot check five REQs (REQ-SIM-CYCLE, REQ-ACCUMULATE, REQ-FLAGS, REQ-API-ASSET,
   REQ-API-HISTORY) against outputs 2 and 3 above.

DONE WHEN
All P7 acceptance criteria in the plan are met.

REPORT
In the plan §0 rule 7 format, including the coverage numbers from section 3.
````

---

### P8 — SysML-style system model

| | |
|---|---|
| **Plan** | §7 P8, §5.3 |
| **Precondition** | P7 committed |
| **Owner before** | Replace `<CUT>` with `none`, `cut 1` or `cut 2` (plan §5.3) |
| **Files** | new `docs/SYSTEM-MODEL.md`, `README.md` |

````text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Execute plan phase P8: write docs/SYSTEM-MODEL.md with the SysML-style Mermaid views.

CUT FOR THIS RUN: <CUT>
- none  -> all five sections
- cut 1 -> omit §2 (block definition) and §3 (internal block); keep §1, §4, §5
- cut 2 -> omit §2, §3 and §4; keep §1 and §5
Omitted sections are left out entirely. The remaining sections keep their numbers (so §5
stays §5), and the omitted sections' rows are dropped from the diagram-kind table.

READ FIRST
- CLAUDE.md
- docs/IMPLEMENTATION-PLAN.md §0-§4, §5.3 and §7 P8
- docs/ARCHITECTURE.md §1-§5
- docs/SPEC.md: REQ-SIM-CYCLE, REQ-FLAGS, REQ-STATUS, REQ-RANK, REQ-RECOMMEND
- docs/STAKEHOLDERS.md
- README.md

DO
1. Write SYSTEM-MODEL.md: the frame from plan P8 "Document frame", then each section with
   its one-sentence description, the Mermaid block copied verbatim from the plan, the
   "Source:" line, and any sentence the plan places below the diagram.
2. Add the SYSTEM-MODEL.md row to the README.md documents table.

WATCH OUT
- Copy the Mermaid blocks character for character, including en dashes, <br/> and quotes.
  Use ```mermaid fences.
- You cannot render Mermaid. Do not install anything to try (no npm, no mermaid-cli).
  Rendering is checked by the owner on mermaid.live.
- If the owner reports a rendering error, change only syntax, never content, and list the
  change in the report.

DO NOT
- Do not add diagrams, elements or relations beyond the plan.
- Do not edit ARCHITECTURE.md or SPEC.md.

VERIFY
1. Every class and test named in §5 exists:
   (Bash) for n in FlagEvaluationService AssetAssessmentService FleetRankingService FlagEvaluationServiceTest AssetAssessmentServiceTest FleetRankingServiceTest; do git ls-files "*/$n.java" | grep -q . && echo "ok $n" || echo "MISSING $n"; done
   -> six "ok" lines
2. Each Mermaid block in SYSTEM-MODEL.md is identical to the plan's block for that section
   (compare them one by one).

DONE WHEN
All P8 acceptance criteria in the plan are met, except the rendering check, which stays an
owner task.

REPORT
In the plan §0 rule 7 format. Under "Owner tasks": "Paste each Mermaid block into
https://mermaid.live and confirm it renders." Record the cut in the plan §5.1 status as
the plan §5.3 requires.
````

---

### P9 — README front door

| | |
|---|---|
| **Plan** | §7 P9 |
| **Precondition** | P8 committed; `docs/images/dashboard.png` exists (plan P9 owner task) |
| **Files** | `README.md` |

````text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Execute plan phase P9: rewrite README.md as the portfolio's front door.

READ FIRST
- CLAUDE.md
- docs/IMPLEMENTATION-PLAN.md §0-§4 and §7 P9
- README.md (current)
- docs/PROCESS.md §1-§4 (the story must match it, including any owner corrections)
- docs/SYSTEM-MODEL.md §1
- docs/STAKEHOLDERS.md

DO
1. Check that docs/images/dashboard.png exists. If not, stop and ask the owner to take the
   screenshot (plan P9 owner task).
2. Rewrite README.md with the seven parts in the order plan P9 lists.

WATCH OUT
- Audience: a recruiter reads it in two minutes; an engineer uses the artifacts table.
  Plain, concrete sentences. No buzzwords, no claims the repository cannot back up.
- "The problem" and "The solution" may only state what PROCESS.md and SPEC.md support.
  The system sends no notifications, keeps no maintenance log, and the workshop does not
  use it.
- The engineering artifacts table replaces the old documents table completely; every
  document that table listed must still appear in the new one.
- Part 5 copies SYSTEM-MODEL.md §1 character for character, preceded by the HTML comment
  the plan gives.
- No company name of a target employer.

DO NOT
- Do not change any file other than README.md.

VERIFY
1. (Bash) diff <(awk '/^```mermaid/{n++; f=(n==1); next} /^```/{f=0} f' README.md) <(awk '/^```mermaid/{n++; f=(n==1); next} /^```/{f=0} f' docs/SYSTEM-MODEL.md)
   -> no output
2. (PowerShell) (Get-Content README.md).Count -> at most 90
3. For every relative link and the image path: (PowerShell) Test-Path <target> -> True
4. The seven parts appear in the plan's order (check the headings).

DONE WHEN
All P9 acceptance criteria in the plan are met.

REPORT
In the plan §0 rule 7 format. Under "Owner tasks": "Commit, push develop, and open the
README on GitHub to check that the image and the Mermaid diagram render."
````

---

### P10 — Final consistency review

| | |
|---|---|
| **Plan** | §7 P10 |
| **Precondition** | P9 committed and pushed |
| **Owner before** | Replace `<NAMES>` with the three employer and group names, typed only into the chat (never saved in a file) |
| **Files** | any P1–P9 file that needs a consistency fix; deletes `docs/IMPLEMENTATION-PLAN.md` and `docs/IMPLEMENTATION-PROMPTS.md` |

````text
Apply the standing rules in docs/IMPLEMENTATION-PROMPTS.md §2.

GOAL
Execute plan phase P10: check that all M1 artifacts agree with each other, fix drift, and
delete the two work-order files once the owner confirms.

NAMES TO CHECK FOR (from the owner, case-insensitive): <NAMES>

READ FIRST
- CLAUDE.md
- docs/IMPLEMENTATION-PLAN.md (whole file)
- Every file changed since commit 3430dc2: (Bash) git diff --name-only 3430dc2 HEAD

DO
1. Re-run the VERIFY blocks of prompts P1, P4, P5, P6 and P7 from
   docs/IMPLEMENTATION-PROMPTS.md. Fix any drift in the artifact that is wrong (never in
   SPEC.md REQ texts), and report each fix.
2. (Bash) for each name: git grep -niF "<name>" -> no output anywhere in the repository.
3. ID declarations (each must print nothing) (Bash):
   - REQ IDs used anywhere but not declared in SPEC.md:
     comm -23 <(git grep -ohE 'REQ-[A-Z]+(-[A-Z]+)*' -- ':!docs/IMPLEMENTATION-PLAN.md' ':!docs/IMPLEMENTATION-PROMPTS.md' | sort -u) <(grep -oE '^\*\*REQ-[A-Z]+(-[A-Z]+)*' docs/SPEC.md | sed 's/^\*\*//' | sort -u)
   - NEED IDs used anywhere but not declared in STAKEHOLDERS.md:
     comm -23 <(git grep -ohE 'NEED-[A-Z]+(-[A-Z]+)*' -- ':!docs/IMPLEMENTATION-PLAN.md' ':!docs/IMPLEMENTATION-PROMPTS.md' | sort -u) <(grep -oE '^\| `NEED-[A-Z]+(-[A-Z]+)*' docs/STAKEHOLDERS.md | sed 's/^| `//' | sort -u)
   - STK IDs, same pattern with 'STK-[A-Z]+'.
   - No REQ declared twice: grep -oE '^\*\*REQ-[A-Z]+(-[A-Z]+)*' docs/SPEC.md | sort | uniq -d
4. Every relative link and image path in README.md and in every Markdown file under docs/
   (the two work-order files excepted) resolves: (PowerShell) Test-Path.
5. Unit tests: re-run P6 VERIFY 1-3 -> 42, 17 and 2 tests, 0 failures.
6. Ask the owner, as a multiple-choice question: "Are P1-P9 committed and their issues
   closed?" Only on "yes": delete docs/IMPLEMENTATION-PLAN.md and
   docs/IMPLEMENTATION-PROMPTS.md. On anything else: stop before deleting.

WATCH OUT
- Do not write the names into any file, command history file or report. In the report,
  say "employer-name check: no matches" without repeating the names.
- Fixes stay within the scope of the phase that owns the file. Anything larger is a
  question to the owner, not a fix.

DO NOT
- Do not change production code.
- Do not commit.

DONE WHEN
All P10 acceptance criteria in the plan are met, the owner has confirmed, and both
work-order files are deleted. (The plan file is gone after deletion, so the status update
of plan §0 rule 8 does not apply to P10.)

REPORT
In the plan §0 rule 7 format, with one line per check in DO steps 1-5.
````

---

## 5. If a prompt goes wrong

- **A verification fails.** Do not run the next prompt. Let the same agent fix forward
  against the plan, then re-run the VERIFY block.
- **The agent deviated from the plan.** Inspect `git diff`, revert the affected files with
  `git checkout -- <path>`, and re-run the prompt in a fresh session with the deviation
  added as an extra line under DO NOT.
- **A precondition is not met.** Check plan §5.1 *Status* and `git log` before starting.
  Most prompts read files that an earlier phase creates.
- **A new test exposes a production bug (P6).** This is a finding, not a failure of the
  phase. Decide with the agent whether to fix the code in a separate, explicitly scoped
  change; the plan itself never changes production behaviour.
- **The plan itself looks wrong.** Stop and decide. Plan §2 lists the decisions settled
  with the owner; reversing one is the owner's call, and the plan is edited before the
  prompt is re-run.
