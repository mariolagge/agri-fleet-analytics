# docs\IMPLEMENTATION-PLAN.md

Governed by `SPEC.md` §0 (document hierarchy, anti-duplication, change protocol). This is a **working checklist for the frontend rewrite**, not a source of truth: it owns no rule, no threshold and no name. Where a step needs one, it points at the file that owns it. If this document and `SPEC.md`, `ARCHITECTURE.md`, `GLOSSARY.md` or `CLAUDE.md` disagree, they win and this file is wrong.

Scope: `frontend/` only. The backend is complete (`CLAUDE.md`, Repo status).

---

## 0. Ground rules for every step

- **Do not invent visual design.** The palette, fonts, icon pack, type scale, radii and shadows are fixed — `CLAUDE.md`, "Frontend design system". New markup composes existing tokens and classes in `frontend/css/app.css`. Adding a colour is a defect.
- **Do not invent display text.** Every enum reaching the screen is renamed through a map in `frontend/js/format.js`, whose values come from `GLOSSARY.md` §2, §3, §5, §6. Raw `UPPER_SNAKE_CASE` never appears in the UI.
- **Do not re-derive product rules.** No threshold, status rule, ranking rule or recommendation sentence is computed in JavaScript. All of it arrives in the response (`SPEC.md` §5, §6.1). The frontend renders and orders what it is given.
- **No dependencies, no build step.** Plain `<script>` tags, globals, no bundler, no package manager (`ARCHITECTURE.md` §7).
- **`null` renders as an em dash, never `0`.** `SPEC.md` §4.5 publishes `null` percentages for machines that have not reported.
- **Escape before interpolating.** Anything from the API goes through `escapeHtml` before it lands in an HTML string.

---

## 1. File inventory

| File | Owns |
|------|------|
| `frontend/index.html` | The full static skeleton: header, metric strip, `dashboard-top`, `dashboard-details`, history section, script order. |
| `frontend/css/app.css` | The whole design system and every component style. |
| `frontend/js/icons.js` | Tabler outline path data plus `tablerIcon` / `mountTablerIcon` / `initTablerIcons`. |
| `frontend/js/format.js` | Display-name maps, `COUNTER_ORDER`, formatters, `escapeHtml`, and the shared fragments `statusPillHtml` / `liveConditionHtml` / `assetChipHtml` / `flagChipsHtml`. |
| `frontend/js/api.js` | `fetchJson`, `fetchFleet`, `fetchLatestTelemetry`, `fetchHistory`, `showAlert`, `clearAlerts`. |
| `frontend/js/fleet.js` | Fleet ranking (`GLOSSARY.md` §8): SVG diagram, ranked list, fleet metric strip, diagram selection and tooltip. |
| `frontend/js/asset.js` | Asset detail (`GLOSSARY.md` §8) and the two counter cards. |
| `frontend/js/telemetry.js` | Live telemetry strip (`GLOSSARY.md` §8). |
| `frontend/js/history.js` | History (`GLOSSARY.md` §8): the period form and the table. |
| `frontend/js/app.js` | Selected-machine state, the single refresh timer, component wiring. |

Script order in `index.html` is load-bearing, because these are globals and not modules: `icons.js`, `format.js`, `api.js`, `fleet.js`, `asset.js`, `telemetry.js`, `history.js`, `app.js`.

Infrastructure touched once: `docker/docker-compose.yml` bind-mounts `../frontend` (`ARCHITECTURE.md` §5).

---

## 2. Steps

### Step 1 — Remove the previous project's GUI

Delete `index.html`, `energy-flow-gui.html`, `css/energy-flow-gui.css`, `js/app.js` and `js/energy-flow-gui.js`. Repoint the `web` service volume in `docker/docker-compose.yml` to `../frontend`.

*Done when* `git status` shows the five deletions and nothing under `frontend/` still mentions `/energy/`.

### Step 2 — Design-system notes in `CLAUDE.md`

Record the visual conventions under a **Frontend design system** heading and refresh the **Repo status** section. Conventions only — no naming (owned by `GLOSSARY.md`), no structure (owned by `ARCHITECTURE.md`).

*Done when* a fresh agent can restyle nothing by accident: the fonts, icon pack, palette, type scale, depth, layout and the no-build constraint are all written down.

### Step 3 — `frontend/css/app.css`

Keep every existing token and component. Add, using existing variables only: `.status-pill` (+ `.status-good` / `.status-watch` / `.status-critical`), `.live-condition` and `.live-dot` (+ the four live modifiers), `.flag-chip` (+ `.severity-moderate` / `.severity-severe`), `.asset-chip` (+ one modifier per machine), `.fleet-ranking` / `.rank-row` / `.rank-badge` / `.rank-info` / `.rank-chevron`, the SVG node rules, `.card-body`, `.detail-*`, `.data-list` / `.data-row`, `.flag-reason*`, `.telemetry-*`, `.card-meta`.

The SVG node rules are the subtle part. Identity colour goes on `.node-icon-bg` only, keyed off the per-machine class. Status colour goes on `.node-bg` stroke, `.node-accent`, `.node-rank-bg` and `.node-status`, keyed off a status class the script sets on the group. Live condition goes on `.node-live-dot` alone — that separation is `SPEC.md` §5.3 expressed in CSS, and collapsing it is the easiest way to get this screen wrong.

Delete rules the new markup no longer references.

*Done when* the file has no selector without markup and no markup without a selector.

### Step 4 — `frontend/js/icons.js`

Keep the mount runtime as-is. Keep only the icons the markup references; path data is copied verbatim from `@tabler/icons` 3.31.0 outline, never hand-drawn.

*Done when* every `data-tabler-icon` value and every `tablerIcon(...)` name resolves to a key in `TABLER_PATHS`.

### Step 5 — `frontend/index.html`

Build the skeleton. Element ids other files depend on:

| Id | Read/written by |
|----|-----------------|
| `alert-placeholder`, `tooltip` | `api.js`, `fleet.js` |
| `generated-at`, `refresh-btn` | `app.js` |
| `metric-urgent-name`, `metric-urgent-status`, `metric-urgent-flags`, `metric-count-critical`, `metric-count-watch`, `metric-count-good`, `metric-avg-utilization`, `metric-avg-productive`, `metric-reporting` | `fleet.js` |
| `fleet-svg` and its four `.node-group[data-asset-id]`, `fleet-ranking` | `fleet.js` |
| `asset-detail` | `asset.js` |
| `rolling-counters`, `rolling-counters-asset`, `lifetime-counters`, `lifetime-counters-asset` | `asset.js` |
| `telemetry-strip` | `telemetry.js` |
| `history-form`, `history-from`, `history-to`, `history-asset`, `history-body` | `history.js` |

The four `.node-group`s are static markup, because the fleet is fixed at four machines (`SPEC.md` §2) — only their text and classes change at runtime. Geometry: `viewBox="0 0 1100 560"`, the tractor at `translate(550, 130)` on the 230×108 pill, the three implements at `translate(200|550|900, 420)` on the 210×96 pill, joined by three static `.link-path`s. Each group carries `.node-select-ring`, `.node-bg`, `.node-icon-bg`, `.node-accent`, the icon `<g>`, `.node-label`, `.node-status`, `.node-value`, `.node-rank-bg`, `.node-rank` and `.node-live-dot`.

*Done when* the page loads with no console error and every panel shows its placeholder text.

### Step 6 — `frontend/js/format.js` and `frontend/js/api.js`

`format.js`: one map per enum family, `COUNTER_ORDER` in `SPEC.md` §4.1 order, the formatters (`formatNumber`, `formatHours`, `formatPercent`, `formatInstant`, `formatTimeOfDay`, `formatBoolean`), `escapeHtml`, the two `datetime-local` ↔ UTC-instant converters, and the four shared HTML fragments.

`api.js`: `fetchJson` unwraps Spring's default error body and throws its `message`; one function per endpoint this screen uses. `GET /api/assets/{assetId}` (`SPEC.md` §6.2) is deliberately not called — it returns an object identical to one element of the fleet response, which the poll already holds. Say so in a comment so the omission reads as a decision.

*Done when* an unreachable API produces the server's own message in the alert banner rather than `HTTP 500`.

### Step 7 — `frontend/js/fleet.js`

`renderFleet(assets, selectedAssetId, onSelect)` drives three things from one array:

1. `renderFleetMetrics` — `assets[0]` is the most urgent machine, because the response is already ordered by `priorityRank` (`SPEC.md` §6.1). Status counts, and the averages of the two percentages over machines that report one.
2. `renderFleetDiagram` — per machine, swap the status and live-condition classes on the group, toggle `is-selected`, and write status, utilization and rank into the three text nodes.
3. `renderFleetRanking` — one `.rank-row` button per machine **in array order**. Never sort. `SPEC.md` §5.4 already did, and re-sorting here would silently invent a second ranking.

`initFleetDiagram(onSelect, describeAsset)` attaches click and tooltip handlers to the four static groups once, at startup.

*Done when* clicking a node and clicking its row select the same machine, and the ranking order matches `priorityRank` exactly.

### Step 8 — `frontend/js/asset.js`

`renderAssetDetail(asset)` shows the identity chip, name, status pill, the `recommendation` sentence verbatim, then `priorityRank`, `liveCondition`, `utilizationPercent`, `productiveSharePercent`, `windowCoverageHours`, then one `.flag-reason` per entry of `flags` printing `counter`, `scope`, `value` and `threshold`. That evidence block is the point of the screen (`SPEC.md` §7) — never reduce a flag to its name alone.

`renderCounterCards(asset)` fills both counter cards from `rolling24hCounters` and `lifetimeCounters`, iterating `COUNTER_ORDER` so the two cards line up row for row.

Both render an empty state when no machine is selected.

*Done when* a machine with no flags shows "No active flags" and a flagged one shows a number and a threshold for each.

### Step 9 — `frontend/js/telemetry.js` and `frontend/js/history.js`

`renderTelemetryStrip(entries)` renders all four entries of `GET /api/telemetry/latest`; an entry with `telemetry: null` shows its live condition and a "no reading" line rather than being skipped (`SPEC.md` §6.4).

`history.js` defaults the period to the last 24 hours, converts both inputs to UTC instants, passes `assetId` only when one is chosen, and renders errors into the table with `.history-error`. Range validation belongs to the API (`SPEC.md` §6.3) — do not re-implement the 7-day check client-side; show what the server says.

*Done when* an out-of-range period puts the server's 400 message in the table body and a valid one fills it with rows.

### Step 10 — `frontend/js/app.js`

Hold `fleetAssets` and `selectedAssetId`. On first successful load, select `fleetAssets[0]` so the detail panel is never empty. One `setInterval` refreshes both live endpoints together and re-renders; a failure shows the banner, and the next success clears it.

*Done when* the page updates on its own every 10 s and the selected machine survives each refresh.

---

## 3. Acceptance walk-through

Start the infra and the three modules (`CLAUDE.md`, Common commands), open `http://localhost:8080` and check:

1. Four machines in the ranking, ordered by `priorityRank`, none missing.
2. The diagram frames carry the status colour, the icon discs the identity colour, the dot the live condition — three separate channels.
3. Selecting a machine updates the detail panel and both counter cards together.
4. Every active flag shows its counter, scope, value and threshold.
5. The telemetry strip changes between polls; a machine that never reported reads "No signal" and shows no invented numbers.
6. The history table fills for the default period, and reports the server's message for a bad one.
7. No `UPPER_SNAKE_CASE` and no bare `0` standing in for a missing value anywhere on screen.

---

## 4. Not in this plan

The unit tests listed under `CLAUDE.md` Repo status, and everything in `SPEC.md` §8.
