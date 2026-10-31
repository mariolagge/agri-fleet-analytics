// Demo replacement for frontend/js/api.js, used only in the GitHub Pages build.
// Same three functions, same response shapes, but the data comes from static JSON files
// in data/ instead of OperatorApi. The JSON was derived once from the Simulator's fixed
// schedules and the TelemetryProcessor rules for the snapshot instant below.

const SNAPSHOT_TIME = '2026-10-30T07:06:00Z';
const MAXIMUM_HISTORY_RANGE_MS = 7 * 24 * 60 * 60 * 1000;
const ONE_HOUR_MS = 60 * 60 * 1000;
const REPOSITORY_URL = 'https://github.com/mariolagge/agri-fleet-analytics';

async function loadJson(fileName) {
  const response = await fetch(`data/${fileName}`);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return response.json();
}

function startOfHourMs(timeMs) {
  return Math.floor(timeMs / ONE_HOUR_MS) * ONE_HOUR_MS;
}

// Moves the recorded hours forward so the newest history row is always the previous hour.
function historyShiftMs() {
  return startOfHourMs(Date.now()) - startOfHourMs(Date.parse(SNAPSHOT_TIME));
}

function shiftInstant(instant, shiftMs) {
  return new Date(Date.parse(instant) + shiftMs).toISOString();
}

// The live endpoints show the snapshot as if it was read just now.
async function fetchFleet() {
  const response = await loadJson('fleet.json');
  response.generatedAt = new Date().toISOString();
  return response;
}

async function fetchLatestTelemetry() {
  const response = await loadJson('telemetry-latest.json');
  const now = new Date().toISOString();
  response.generatedAt = now;
  response.assets.forEach((entry) => {
    if (entry.telemetry) {
      entry.telemetry.timestamp = now;
    }
  });
  return response;
}

// Same validation messages as HistoryService, so history.js translates them as usual.
async function fetchHistory(fromInstant, toInstant, assetId) {
  const fromMs = Date.parse(fromInstant);
  const toMs = Date.parse(toInstant);
  if (Number.isNaN(fromMs) || Number.isNaN(toMs)) {
    throw new Error('from and to must be ISO-8601 instants');
  }
  if (fromMs >= toMs) {
    throw new Error('from must be before to');
  }
  if (toMs - fromMs > MAXIMUM_HISTORY_RANGE_MS) {
    throw new Error('the requested range must not exceed 7 days');
  }

  const recorded = await loadJson('history.json');
  const shiftMs = historyShiftMs();
  const rows = recorded.rows
    .map((row) => ({ ...row, hourStart: shiftInstant(row.hourStart, shiftMs) }))
    .filter((row) => !assetId || row.assetId === assetId)
    .filter((row) => {
      // A row covers [hourStart, hourStart + 1 h) and is kept when it overlaps [from, to).
      const hourStartMs = Date.parse(row.hourStart);
      return hourStartMs < toMs && hourStartMs + ONE_HOUR_MS > fromMs;
    });

  return { from: fromInstant, to: toInstant, rows };
}

// A one-line notice in the top bar, left of the language toggle. Its texts are translated
// by i18n.js like any other static markup, because the elements carry data-i18n.
function addDemoBanner() {
  const banner = document.createElement('div');
  banner.className = 'demo-banner';
  banner.innerHTML = `
    ${tablerIcon('info-circle', { size: 14 })}
    <span><strong data-i18n="demo.banner"></strong>
    <a href="${REPOSITORY_URL}" target="_blank" rel="noopener" data-i18n="demo.sourceLink"></a></span>`;
  const topBar = document.querySelector('.top-bar');
  topBar.insertBefore(banner, topBar.firstChild);
}

addDemoBanner();

// Visitors see a filled history table right away instead of an empty prompt.
window.addEventListener('load', () => loadHistory());
