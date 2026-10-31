// Display text and value formatting. The API returns raw enum values; every display
// name comes from i18n.js, which follows GLOSSARY.md (§2 assets, §3 telemetry, §5 flags,
// §6 status and live condition). Nothing here derives a product rule — it only renames
// and formats.

const ASSET_ICONS = {
  TRACTOR: 'tractor',
  MOWER: 'blade',
  BALER: 'barrel',
  MIXER: 'blender',
};

const STATUS_ICONS = {
  GOOD: 'circle-check',
  WATCH: 'alert-triangle',
  CRITICAL: 'alert-octagon',
};

const FLAG_ICONS = {
  HEAVY_RECENT_USE: 'weight',
  THERMAL_LOAD: 'flame',
  MECHANICAL_ATTENTION: 'activity',
  HIGH_CUMULATIVE_LOAD: 'hourglass',
};

// Fixed order for the counter panels, matching REQ-COUNTER-SET.
const COUNTER_ORDER = [
  'engineHours',
  'totalWorkingHours',
  'highLoadHours',
  'tempExcursionHours',
  'vibrationAlertHours',
];

const EM_DASH = '—';

// The display name for an enum value, e.g. displayName('status', 'GOOD') -> t('status.GOOD').
// A value i18n.js does not know gets the fallback instead of its raw key.
function displayName(prefix, value, fallback) {
  const key = `${prefix}.${value}`;
  return TRANSLATIONS.en[key] ? t(key) : fallback;
}

function assetDisplayName(assetId) {
  return displayName('asset', assetId, assetId || EM_DASH);
}

function assetIcon(assetId) {
  return ASSET_ICONS[assetId] || 'tool';
}

function statusDisplayName(status) {
  return displayName('status', status, EM_DASH);
}

// The backend sends one fixed English sentence per status, so the translation is looked
// up by status. An unknown status keeps whatever text the API sent.
function recommendationText(status, apiRecommendation) {
  return displayName('recommendation', status, apiRecommendation || EM_DASH);
}

function statusIcon(status) {
  return STATUS_ICONS[status] || 'circle-check';
}

// Maps a status onto the CSS modifier that carries its colour.
function statusClass(status) {
  if (status === 'GOOD' || status === 'WATCH' || status === 'CRITICAL') {
    return `status-${status.toLowerCase()}`;
  }
  return '';
}

function liveConditionDisplayName(liveCondition) {
  return displayName('live', liveCondition, EM_DASH);
}

function liveConditionClass(liveCondition) {
  if (!liveCondition) {
    return 'live-stale';
  }
  return `live-${liveCondition.toLowerCase()}`;
}

function flagDisplayName(flag) {
  return displayName('flag', flag, flag || EM_DASH);
}

function flagIcon(flag) {
  return FLAG_ICONS[flag] || 'alert-triangle';
}

function scopeDisplayName(scope) {
  return displayName('scope', scope, scope || EM_DASH);
}

function counterDisplayName(counter) {
  return displayName('counter', counter, counter || EM_DASH);
}

function vibrationLevelDisplayName(vibrationLevel) {
  return displayName('vibration', vibrationLevel, EM_DASH);
}

// A missing value is an em dash, never a zero: REQ-UTILIZATION publishes null percentages
// for assets that have not reported, and rendering those as 0 would be a lie.
function formatNumber(value, fractionDigits = 1) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return EM_DASH;
  }
  return Number(value).toLocaleString(currentLocale(), {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  });
}

function formatHours(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return EM_DASH;
  }
  return `${formatNumber(value, 2)} h`;
}

function formatPercent(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return EM_DASH;
  }
  return `${formatNumber(value, 1)} %`;
}

// The API speaks UTC instants (REQ-API-COMMON); the farmer reads farm-local time. That is the
// farm's zone, not the browser's — REQ-BUCKETS aligns the hourly buckets to Europe/Vienna,
// so on a machine set to any other zone the hourStart column would stop landing on :00.
const FARM_ZONE = 'Europe/Vienna';

function formatInstant(value) {
  if (!value) {
    return EM_DASH;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return EM_DASH;
  }
  return date.toLocaleString(currentLocale(), {
    timeZone: FARM_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function formatTimeOfDay(value) {
  if (!value) {
    return EM_DASH;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return EM_DASH;
  }
  return date.toLocaleTimeString(currentLocale(), {
    timeZone: FARM_ZONE,
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

function formatBoolean(value) {
  if (value === null || value === undefined) {
    return EM_DASH;
  }
  return value ? t('common.yes') : t('common.no');
}

function escapeHtml(text) {
  const element = document.createElement('div');
  element.textContent = text ?? '';
  return element.innerHTML;
}

// Small display fragments shared by the fleet ranking, the asset detail and the
// telemetry strip. Status and live condition stay in separate elements on purpose —
// REQ-LIVE keeps them apart.

function statusPillHtml(status) {
  return `<span class="status-pill ${statusClass(status)}">`
    + `${tablerIcon(statusIcon(status), { size: 11 })}${escapeHtml(statusDisplayName(status))}</span>`;
}

function liveConditionHtml(liveCondition) {
  return `<span class="live-condition"><span class="live-dot ${liveConditionClass(liveCondition)}"></span>`
    + `${escapeHtml(liveConditionDisplayName(liveCondition))}</span>`;
}

function assetChipHtml(assetId) {
  const tone = String(assetId || '').toLowerCase();
  return `<span class="asset-chip asset-chip-${escapeHtml(tone)}">`
    + `${tablerIcon(assetIcon(assetId), { size: 24, stroke: 'var(--on-color)' })}</span>`;
}

function flagChipsHtml(flags) {
  if (!Array.isArray(flags) || flags.length === 0) {
    return `<span class="flag-chips-empty">${escapeHtml(t('flag.none'))}</span>`;
  }
  const chips = flags.map((flag) => `<span class="flag-chip">`
    + `${tablerIcon(flagIcon(flag.flag), { size: 11 })}${escapeHtml(flagDisplayName(flag.flag))}</span>`);
  return `<span class="flag-chips">${chips.join('')}</span>`;
}

// How far the farm's wall clock runs ahead of UTC at this instant. Reading both zones
// through the same formatter keeps this to two lines and no offset table.
function farmZoneOffsetMs(instant) {
  const farmWallClock = new Date(instant.toLocaleString('en-US', { timeZone: FARM_ZONE }));
  const utcWallClock = new Date(instant.toLocaleString('en-US', { timeZone: 'UTC' }));
  return farmWallClock.getTime() - utcWallClock.getTime();
}

// The value a <input type="datetime-local"> wants, as the farm's wall clock rather than
// the browser's — the period the farmer picks must mean the same thing as the hours the
// table shows back.
function toDatetimeLocalValue(date) {
  const parts = {};
  new Intl.DateTimeFormat('en-CA', {
    timeZone: FARM_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(date).forEach((part) => {
    parts[part.type] = part.value;
  });
  return `${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}`;
}

// The UTC ISO-8601 instant the API wants, from a farm-local datetime-local value. The
// offset is read once, at the naive instant: a period that straddles a DST transition is
// off by an hour, which SPEC §4.3 puts out of scope.
function toUtcInstant(localValue) {
  if (!localValue) {
    return '';
  }
  const asIfUtc = new Date(`${localValue}:00Z`);
  if (Number.isNaN(asIfUtc.getTime())) {
    return '';
  }
  return new Date(asIfUtc.getTime() - farmZoneOffsetMs(asIfUtc)).toISOString();
}
