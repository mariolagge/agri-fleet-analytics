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


---

---


---


---



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
