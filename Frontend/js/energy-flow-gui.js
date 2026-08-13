// Empty base → relative /energy/* (Nginx on :8080 proxies to OperatorApi :8083).
const API_BASE = '';
const AUTO_REFRESH_MS = 12_000;

const TABLER_PATHS = {
  bolt: ['M13 3l0 7l6 0l-8 11l0 -7l-6 0l8 -11'],
  sparkles: ['M16 18a2 2 0 0 1 2 2a2 2 0 0 1 2 -2a2 2 0 0 1 -2 -2a2 2 0 0 1 -2 2zm0 -12a2 2 0 0 1 2 2a2 2 0 0 1 2 -2a2 2 0 0 1 -2 -2a2 2 0 0 1 -2 2zm-7 12a6 6 0 0 1 6 -6a6 6 0 0 1 -6 -6a6 6 0 0 1 -6 6a6 6 0 0 1 6 6z'],
  'temperature-snow': [
    'M4 13.5a4 4 0 1 0 4 0v-8.5a2 2 0 1 0 -4 0v8.5',
    'M4 9h4',
    'M14.75 4l1 2h2.25',
    'M17 4l-3 5l2 3',
    'M20.25 10l-1.25 2l1.25 2',
    'M22 12h-6l-2 3',
    'M18 18h-2.25l-1 2',
    'M17 20l-3 -5h-1',
    'M12 9l2.088 .008',
  ],
  'car-fan': [
    'M12 12v-9l4.912 1.914a1.7 1.7 0 0 1 .428 2.925z',
    'M12 12h9l-1.914 4.912a1.7 1.7 0 0 1 -2.925 .428z',
    'M12 12h-9l1.914 -4.912a1.7 1.7 0 0 1 2.925 -.428z',
    'M12 12v9l-4.912 -1.914a1.7 1.7 0 0 1 -.428 -2.925z',
  ],
  robot: [
    'M6 4m0 2a2 2 0 0 1 2 -2h8a2 2 0 0 1 2 2v4a2 2 0 0 1 -2 2h-8a2 2 0 0 1 -2 -2z',
    'M12 2v2',
    'M9 12v9',
    'M15 12v9',
    'M5 16l4 -2',
    'M15 14l4 2',
    'M9 18h6',
    'M10 8v.01',
    'M14 8v.01',
  ],
  wind: [
    'M5 8h8.5a2.5 2.5 0 1 0 -2.34 -3.24',
    'M3 12h15.5a2.5 2.5 0 1 1 -2.34 3.24',
    'M4 16h5.5a2.5 2.5 0 1 1 -2.34 3.24',
  ],
  'solar-panel': [
    'M4.28 14h15.44a1 1 0 0 0 .97 -1.243l-1.5 -6a1 1 0 0 0 -.97 -.757h-12.44a1 1 0 0 0 -.97 .757l-1.5 6a1 1 0 0 0 .97 1.243z',
    'M4 10h16',
    'M10 6l-1 8',
    'M14 6l1 8',
    'M12 14v4',
    'M7 18h10',
  ],
  battery: ['M6 7h11a2 2 0 0 1 2 2v.5a.5 .5 0 0 0 .5 .5a.5 .5 0 0 1 .5 .5v3a.5 .5 0 0 1 -.5 .5a.5 .5 0 0 0 -.5 .5v.5a2 2 0 0 1 -2 2h-11a2 2 0 0 1 -2 -2v-6a2 2 0 0 1 2 -2'],
  'battery-charging': [
    'M16 7h1a2 2 0 0 1 2 2v.5a.5 .5 0 0 0 .5 .5a.5 .5 0 0 1 .5 .5v3a.5 .5 0 0 1 -.5 .5a.5 .5 0 0 0 -.5 .5v.5a2 2 0 0 1 -2 2h-2',
    'M8 7h-2a2 2 0 0 0 -2 2v6a2 2 0 0 0 2 2h1',
    'M12 8l-2 4h3l-2 4',
  ],
  'plug-connected': [
    'M7 12l5 5l-1.5 1.5a3.536 3.536 0 1 1 -5 -5l1.5 -1.5z',
    'M17 12l-5 -5l1.5 -1.5a3.536 3.536 0 1 1 5 5l-1.5 1.5z',
    'M3 21l2.5 -2.5',
    'M18.5 5.5l2.5 -2.5',
    'M10 11l-2 2',
    'M13 14l-2 2',
  ],
  send: ['M10 14l11 -11', 'M21 3l-6.5 18a.55 .55 0 0 1 -1 0l-3.5 -7l-7 -3.5a.55 .55 0 0 1 0 -1l18 -6.5'],
  shield: ['M12 3a12 12 0 0 0 8.5 3a12 12 0 0 1 -8.5 15a12 12 0 0 1 -8.5 -15a12 12 0 0 0 8.5 -3'],
  'alert-octagon': [
    'M12.802 2.165l5.575 2.389c.48 .206 .863 .589 1.07 1.07l2.388 5.574c.22 .512 .22 1.092 0 1.604l-2.389 5.575c-.206 .48 -.589 .863 -1.07 1.07l-5.574 2.388c-.512 .22 -1.092 .22 -1.604 0l-5.575 -2.389a2.036 2.036 0 0 1 -1.07 -1.07l-2.388 -5.574a2.036 2.036 0 0 1 0 -1.604l2.389 -5.575c.206 -.48 .589 -.863 1.07 -1.07l5.574 -2.388a2.036 2.036 0 0 1 1.604 0z',
    'M12 8v4',
    'M12 16h.01',
  ],
  components: [
    'M3 12l3 3l3 -3l-3 -3z',
    'M15 12l3 3l3 -3l-3 -3z',
    'M9 6l3 3l3 -3l-3 -3z',
    'M9 18l3 3l3 -3l-3 -3z',
  ],
  users: [
    'M9 7m-4 0a4 4 0 1 0 8 0a4 4 0 1 0 -8 0',
    'M3 21v-2a4 4 0 0 1 4 -4h4a4 4 0 0 1 4 4v2',
    'M16 3.13a4 4 0 0 1 0 7.75',
    'M21 21v-2a4 4 0 0 0 -3 -3.85',
  ],
  bulb: [
    'M3 12h1m8 -9v1m8 8h1m-15.4 -6.4l.7 .7m12.1 -.7l-.7 .7',
    'M9 16a5 5 0 1 1 6 0a3.5 3.5 0 0 0 -1 3a2 2 0 0 1 -4 0a3.5 3.5 0 0 0 -1 -3',
    'M9.7 17l4.6 0',
  ],
  'chart-line': ['M4 19l16 0', 'M4 15l4 -6l4 2l4 -5l4 4'],
  history: ['M12 8l0 4l2 2', 'M3.05 11a9 9 0 1 1 .5 4m-.5 5v-5h5'],
  'player-play': ['M7 4v16l13 -8z'],
  'player-stop': ['M5 5m0 2a2 2 0 0 1 2 -2h10a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2z'],
  download: ['M4 17v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2 -2v-2', 'M7 11l5 5l5 -5', 'M12 4l0 12'],
  check: ['M5 12l5 5l10 -10'],
  x: ['M18 6l-12 12', 'M6 6l12 12'],
  'arrow-up': ['M12 5l0 14', 'M18 11l-6 -6', 'M6 11l6 -6'],
  'arrow-down': ['M12 5l0 14', 'M18 13l-6 6', 'M6 13l6 6'],
  clock: ['M12 12m-9 0a9 9 0 1 0 18 0a9 9 0 1 0 -18 0', 'M12 7v5l3 3'],
  home: [
    'M5 12l-2 0l9 -9l9 9l-2 0',
    'M5 12v7a2 2 0 0 0 2 2h10a2 2 0 0 0 2 -2v-7',
    'M9 21v-6a2 2 0 0 1 2 -2h2a2 2 0 0 1 2 2v6',
  ],
};

const FLOW_ICON_MAP = {
  pvToLoadsKwh: 'solar-panel',
  pvToBatteryKwh: 'battery-charging',
  batteryToLoadsKwh: 'battery',
  gridToLoadsKwh: 'plug-connected',
  pvToGridExportKwh: 'arrow-up',
};

const USER_ICON_MAP = {
  'milk-cooling': 'temperature-snow',
  'milking-robot': 'robot',
  'hay-dryer': 'car-fan',
  'barn-ventilation': 'wind',
};

const USER_ICON_TONE = {
  'milk-cooling': 'cooling',
  'milking-robot': 'robot',
  'hay-dryer': 'dryer',
  'barn-ventilation': 'ventilation',
};

function tablerIcon(name, { size = 24, className = '', stroke = 'currentColor', strokeWidth = 2 } = {}) {
  const paths = TABLER_PATHS[name];
  if (!paths) {
    return '';
  }
  const pathEls = paths.map((d) => `<path d="${d}" />`).join('');
  const extraClass = className ? ` ${className}` : '';
  return `<svg class="tabler-icon${extraClass}" xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${stroke}" stroke-width="${strokeWidth}" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${pathEls}</svg>`;
}

function mountTablerIcon(el, name, { size = 24, className = '', stroke = 'currentColor', strokeWidth = 2 } = {}) {
  const paths = TABLER_PATHS[name];
  if (!paths) {
    return;
  }

  if (el.namespaceURI === 'http://www.w3.org/2000/svg') {
    while (el.firstChild) {
      el.removeChild(el.firstChild);
    }

    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('width', String(size));
    svg.setAttribute('height', String(size));
    svg.setAttribute('viewBox', '0 0 24 24');
    svg.setAttribute('fill', 'none');
    svg.setAttribute('stroke', stroke);
    svg.setAttribute('stroke-width', String(strokeWidth));
    svg.setAttribute('stroke-linecap', 'round');
    svg.setAttribute('stroke-linejoin', 'round');
    svg.classList.add('tabler-icon');
    if (className) {
      className.split(' ').filter(Boolean).forEach((cls) => svg.classList.add(cls));
    }

    paths.forEach((d) => {
      const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
      path.setAttribute('d', d);
      svg.appendChild(path);
    });
    el.appendChild(svg);
    return;
  }

  el.innerHTML = tablerIcon(name, { size, stroke, strokeWidth, className });
}

function initTablerIcons() {
  document.querySelectorAll('[data-tabler-icon]').forEach((el) => {
    const name = el.dataset.tablerIcon;
    const size = Number(el.dataset.tablerSize) || 24;
    const stroke = el.dataset.tablerStroke || 'currentColor';
    const strokeWidth = Number(el.dataset.tablerStrokeWidth) || 2;
    const className = el.dataset.tablerClass || '';
    mountTablerIcon(el, name, { size, stroke, strokeWidth, className });
  });
}

function compIconHtml(iconName) {
  return `<div class="comp-icon">${tablerIcon(iconName, { size: 54 })}</div>`;
}

function userIconHtml(iconName, userId) {
  const tone = USER_ICON_TONE[userId] || 'default';
  return `<div class="user-icon user-icon-${tone}">${tablerIcon(iconName, { size: 12, stroke: '#fdfcf9' })}</div>`;
}

const ENERGY_USER_IDS = ['milk-cooling', 'milking-robot', 'hay-dryer', 'barn-ventilation'];

const LIVE_FLOW_KEYS = [
  'pvToLoadsKwh',
  'pvToBatteryKwh',
  'batteryToLoadsKwh',
  'gridToLoadsKwh',
  'pvToGridExportKwh',
];

const LIVE_FLOW_LABELS = [
  { key: 'pvToLoadsKwh', label: 'PV → loads' },
  { key: 'pvToBatteryKwh', label: 'PV → battery' },
  { key: 'batteryToLoadsKwh', label: 'Battery → loads' },
  { key: 'gridToLoadsKwh', label: 'Grid → loads' },
  { key: 'pvToGridExportKwh', label: 'PV → grid export' },
];

const LOAD_SPLIT_AGGREGATE_KEYS = ['pvToLoadsKwh', 'batteryToLoadsKwh', 'gridToLoadsKwh'];

const FLOW_PATH_IDS = [
  'flow-pvToBatteryKwh',
  'flow-pvToGridExportKwh',
  ...LOAD_SPLIT_AGGREGATE_KEYS.flatMap((aggregateKey) =>
    ENERGY_USER_IDS.map((userId) => `flow-${aggregateKey}--${userId}`)
  ),
];

const SOURCE_NODE_TOOLTIPS = {
  pv: 'Farm production (PV)',
  battery: 'Battery storage',
  grid: 'Public grid connection',
};

let userNameById = {};
let cachedEnergyUsers = [];
let cachedFlexibleLoads = [];

const uiState = {
  aiActive: true,
  manualOverride: false,
  islandMode: false,
};

function usedKwhElementId(userId) {
  return `usedKwh-${userId}`;
}

let currentUsageByUserId = {};
let currentVisualFlowKwh = {};
let dashLoopsStarted = false;
let particleGeneration = 0;

function getFlowKwh(liveFlows, key) {
  const value = Number(liveFlows?.[key]);
  return Number.isFinite(value) && value > 0 ? value : 0;
}

function getMaxVisualFlowKwh(visualFlows) {
  return Math.max(...FLOW_PATH_IDS.map((pathId) => getFlowKwh(visualFlows, pathId)), 0);
}

function visualFlowIntensity(visualFlows, pathId) {
  const max = getMaxVisualFlowKwh(visualFlows);
  if (max <= 0) {
    return 0;
  }
  return getFlowKwh(visualFlows, pathId) / max;
}

function computePerLoadShares(usageByUserId) {
  const shares = {};
  let total = 0;
  ENERGY_USER_IDS.forEach((userId) => {
    total += Number(usageByUserId[userId]) || 0;
  });
  ENERGY_USER_IDS.forEach((userId) => {
    shares[userId] = total <= 0 ? 0 : (Number(usageByUserId[userId]) || 0) / total;
  });
  return shares;
}

function buildVisualFlowKwh(liveFlows, usageByUserId) {
  const visual = {};
  const shares = computePerLoadShares(usageByUserId || {});

  visual['flow-pvToBatteryKwh'] = getFlowKwh(liveFlows, 'pvToBatteryKwh');
  visual['flow-pvToGridExportKwh'] = getFlowKwh(liveFlows, 'pvToGridExportKwh');

  LOAD_SPLIT_AGGREGATE_KEYS.forEach((aggregateKey) => {
    const aggregateKwh = getFlowKwh(liveFlows, aggregateKey);
    ENERGY_USER_IDS.forEach((userId) => {
      visual[`flow-${aggregateKey}--${userId}`] = aggregateKwh * shares[userId];
    });
  });

  return visual;
}

function applyFlowPathStyles(visualFlows) {
  FLOW_PATH_IDS.forEach((pathId) => {
    const path = document.getElementById(pathId);
    if (!path) {
      return;
    }

    const kwh = getFlowKwh(visualFlows, pathId);
    const intensity = visualFlowIntensity(visualFlows, pathId);
    const baseWidth = Number(path.dataset.baseWidth || path.getAttribute('stroke-width') || 2);

    path.style.strokeWidth = String(baseWidth + intensity * 3.5);
    path.style.opacity = kwh > 0
      ? String(0.3 + intensity * 0.55)
      : '0.1';
    path.dataset.flowKwh = String(kwh);
  });
}

function animateParticleAlongPath(circle, path, pathId, generation) {
  const length = path.getTotalLength();
  const phase = Number(circle.dataset.phase) || 0;

  function frame(timestamp) {
    if (generation !== particleGeneration) {
      return;
    }

    const kwh = getFlowKwh(currentVisualFlowKwh, pathId);
    const intensity = visualFlowIntensity(currentVisualFlowKwh, pathId);
    if (kwh <= 0) {
      circle.style.opacity = '0';
      requestAnimationFrame(frame);
      return;
    }

    const duration = Math.max(1800, 5200 - intensity * 3200);
    const progress = ((timestamp / duration) + phase) % 1;
    const point = path.getPointAtLength(progress * length);
    circle.setAttribute('cx', String(point.x));
    circle.setAttribute('cy', String(point.y));
    circle.setAttribute('r', String(1.5 + intensity * 2.5));
    circle.style.opacity = String(0.35 + intensity * 0.55);
    requestAnimationFrame(frame);
  }

  requestAnimationFrame(frame);
}

function createParticles(visualFlows = currentVisualFlowKwh) {
  currentVisualFlowKwh = visualFlows || {};
  particleGeneration += 1;
  const generation = particleGeneration;

  const container = document.getElementById('particles');
  container.innerHTML = '';

  FLOW_PATH_IDS.forEach((pathId) => {
    const path = document.getElementById(pathId);
    if (!path) {
      return;
    }

    const kwh = getFlowKwh(currentVisualFlowKwh, pathId);
    if (kwh <= 0) {
      return;
    }

    const intensity = visualFlowIntensity(currentVisualFlowKwh, pathId);
    const count = Math.max(1, Math.round(1 + intensity * 7));

    for (let i = 0; i < count; i += 1) {
      const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
      circle.setAttribute('fill', path.getAttribute('stroke') || '#0d6b66');
      circle.classList.add('flow-particle');
      circle.dataset.pathId = pathId;
      circle.dataset.phase = String(i / count);
      container.appendChild(circle);
      animateParticleAlongPath(circle, path, pathId, generation);
    }
  });
}

function startDashLoops() {
  FLOW_PATH_IDS.forEach((pathId) => {
    const path = document.getElementById(pathId);
    if (!path) {
      return;
    }

    let offset = 0;
    function tick() {
      const kwh = getFlowKwh(currentVisualFlowKwh, pathId);
      const intensity = visualFlowIntensity(currentVisualFlowKwh, pathId);

      const dashLen = 5 + intensity * 9;
      const gapLen = Math.max(4, 11 - intensity * 5);
      path.style.strokeDasharray = `${dashLen} ${gapLen}`;

      const speed = kwh > 0 ? 0.06 + intensity * 0.75 : 0.015;
      offset -= speed;
      path.style.strokeDashoffset = String(offset);
      requestAnimationFrame(tick);
    }

    requestAnimationFrame(tick);
  });
}

function animateDashes(visualFlows) {
  if (visualFlows !== undefined) {
    currentVisualFlowKwh = visualFlows || {};
    applyFlowPathStyles(currentVisualFlowKwh);
  }

  if (!dashLoopsStarted) {
    dashLoopsStarted = true;
    startDashLoops();
  }
}

function updateFlowVisualization() {
  currentVisualFlowKwh = buildVisualFlowKwh(current.liveFlows, currentUsageByUserId);
  applyFlowPathStyles(currentVisualFlowKwh);
  createParticles(currentVisualFlowKwh);
}

function showAlert(message, type = 'danger') {
  const placeholder = document.getElementById('alert-placeholder');
  placeholder.innerHTML = `
    <div class="alert alert-${type} alert-dismissible fade show" role="alert">
      ${message}
      <button type="button" class="btn-close" aria-label="Close">${tablerIcon('x', { size: 14 })}</button>
    </div>`;
  placeholder.querySelector('.btn-close')?.addEventListener('click', clearAlerts);
}

function clearAlerts() {
  document.getElementById('alert-placeholder').innerHTML = '';
}

function formatNumber(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '—';
  }
  return Number(value).toLocaleString(undefined, { maximumFractionDigits: 2 });
}

function formatHour(value) {
  if (value === null || value === undefined || value === '') {
    return '—';
  }
  return String(value);
}

function escapeHtml(text) {
  const element = document.createElement('div');
  element.textContent = text ?? '';
  return element.innerHTML;
}

function toApiDateTime(localValue) {
  if (!localValue) {
    return '';
  }
  const normalized = localValue.length === 16 ? `${localValue}:00` : localValue;
  return normalized;
}

function toDatetimeLocalValue(date) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

let current = {};

function setMetric(elementId, value) {
  const element = document.getElementById(elementId);
  if (!element) {
    return;
  }
  const unit = element.querySelector('.metric-unit');
  element.textContent = formatNumber(value);
  if (unit) {
    element.appendChild(unit);
  }
}

function bindMetricField(field, value) {
  document.querySelectorAll(`[data-bind="${field}"]`).forEach((node) => {
    const unit = node.querySelector('.metric-unit');
    node.textContent = formatNumber(value);
    if (unit) {
      node.appendChild(unit);
    }
  });
}

function renderBatteryFromCurrent(data) {
  const soc = data.batterySocPercent;
  const socText = soc === null || soc === undefined ? '—' : `${formatNumber(soc)}%`;

  const diagramSoc = document.getElementById('batterySocPercent');
  if (diagramSoc) {
    diagramSoc.textContent = socText;
  }
  setMetric('batterySocPercent-metric', soc);

  bindMetricField('batteryChargedKwh', data.batteryChargedKwh);
  bindMetricField('batteryDischargedKwh', data.batteryDischargedKwh);
}

function renderGridNode(data) {
  const imported = document.getElementById('gridUsedKwh');
  if (imported) {
    imported.textContent = `Imported ${formatNumber(data.gridUsedKwh)} kWh`;
  }
  bindMetricField('gridUsedKwh', data.gridUsedKwh);
  bindMetricField('gridExportedKwh', data.gridExportedKwh);
}

function renderFarmProducedNode(data) {
  const diagram = document.getElementById('farmProducedKwh');
  if (diagram) {
    diagram.textContent = `${formatNumber(data.farmProducedKwh)} kWh`;
  }
  bindMetricField('farmProducedKwh', data.farmProducedKwh);
}

function renderComponentList(liveFlows) {
  const list = document.getElementById('comp-list');
  if (!list) {
    return;
  }

  const flows = liveFlows || {};

  list.innerHTML = LIVE_FLOW_LABELS.map(({ key, label }) => `
    <div class="comp-row">
      ${compIconHtml(FLOW_ICON_MAP[key])}
      <div class="comp-info">
        <div class="comp-name">${escapeHtml(label)}</div>
        <div class="comp-meta">Live flow</div>
      </div>
      <div class="comp-power neutral">${formatNumber(flows[key])} kWh</div>
    </div>`).join('');
}

function flexibleLoadStatusHtml(user, flexibleByUserId) {
  if (!user.flexible) {
    return '';
  }

  const load = flexibleByUserId[user.userId];
  const percent = load ? Number(load.percent) || 0 : 0;
  const on = percent > 0;
  const statusText = on ? `ON (${formatNumber(percent)}%)` : 'OFF (0%)';
  return `<div class="comp-power ${on ? 'positive' : 'neutral'}">${escapeHtml(statusText)}</div>`;
}

function refreshEnergyUsersList() {
  if (cachedEnergyUsers.length === 0) {
    return;
  }
  renderEnergyUsers(cachedEnergyUsers, cachedFlexibleLoads);
}

function resetCurrentEnergyDisplay() {
  setMetric('solarDepletedPercent', null);
  setMetric('gridPortionPercent', null);
  setMetric('batterySocPercent-metric', null);
  bindMetricField('farmProducedKwh', null);
  bindMetricField('farmUsedKwh', null);
  bindMetricField('gridUsedKwh', null);
  bindMetricField('gridExportedKwh', null);
  bindMetricField('batteryChargedKwh', null);
  bindMetricField('batteryDischargedKwh', null);

  const diagramSoc = document.getElementById('batterySocPercent');
  if (diagramSoc) {
    diagramSoc.textContent = '—';
  }

  const farmProduced = document.getElementById('farmProducedKwh');
  if (farmProduced) {
    farmProduced.textContent = '—';
  }

  const imported = document.getElementById('gridUsedKwh');
  if (imported) {
    imported.textContent = 'Imported —';
  }

  const list = document.getElementById('comp-list');
  if (list) {
    list.innerHTML = '';
  }

  cachedFlexibleLoads = [];
  refreshEnergyUsersList();

  currentUsageByUserId = {};
  updateFlowVisualization();
}

function resetPerLoadUsageDisplay() {
  ENERGY_USER_IDS.forEach((userId) => {
    const element = document.getElementById(usedKwhElementId(userId));
    if (element) {
      element.textContent = '—';
    }
  });
  currentUsageByUserId = {};
  updateFlowVisualization();
}

async function loadCurrentEnergy() {
  try {
    const response = await fetch(`${API_BASE}/energy/current`);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const data = await response.json();
    current = data;

    setMetric('solarDepletedPercent', data.solarDepletedPercent);
    setMetric('gridPortionPercent', data.gridPortionPercent);

    renderFarmProducedNode(data);
    bindMetricField('farmUsedKwh', data.farmUsedKwh);
    renderBatteryFromCurrent(data);
    renderGridNode(data);
    cachedFlexibleLoads = Array.isArray(data.flexibleLoads) ? data.flexibleLoads : [];
    renderComponentList(data.liveFlows);
    refreshEnergyUsersList();

    clearAlerts();
    return data;
  } catch (err) {
    current = {};
    resetCurrentEnergyDisplay();
    showAlert(
      `Could not load current status. ${err.message}. Check that OperatorApi is running and reachable through the /energy proxy.`
    );
    return null;
  }
}

function renderEnergyUsers(users, flexibleLoads) {
  const list = document.getElementById('users-list');
  const loading = document.getElementById('users-loading');
  if (!list || !loading) {
    return;
  }

  userNameById = {};
  users.forEach((user) => {
    userNameById[user.userId] = user.name;
  });

  const flexibleByUserId = {};
  (Array.isArray(flexibleLoads) ? flexibleLoads : []).forEach((load) => {
    flexibleByUserId[load.userId] = load;
  });

  list.innerHTML = users
    .map((user) => `
    <div class="comp-row">
      ${userIconHtml(USER_ICON_MAP[user.userId] || 'bolt', user.userId)}
      <div class="comp-info">
        <div class="comp-name">${escapeHtml(user.name)}</div>
        <div class="comp-meta">${escapeHtml(user.userId)}</div>
      </div>
      ${flexibleLoadStatusHtml(user, flexibleByUserId)}
    </div>`)
    .join('');

  loading.classList.add('d-none');
  list.classList.remove('d-none');
}

async function loadEnergyUsers() {
  const loading = document.getElementById('users-loading');
  const list = document.getElementById('users-list');
  if (!loading || !list) {
    return;
  }

  try {
    const response = await fetch(`${API_BASE}/energy/users`);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const users = await response.json();
    if (!Array.isArray(users) || users.length === 0) {
      throw new Error('Empty user list');
    }
    cachedEnergyUsers = users;
    refreshEnergyUsersList();
  } catch (err) {
    loading.textContent = 'Could not load energy users.';
    list.classList.add('d-none');
    showAlert(
      `Could not load energy users. ${err.message}. Check that OperatorApi is running and reachable through the /energy proxy.`
    );
  }
}

async function loadPerLoadUsage(hour) {
  if (!hour) {
    resetPerLoadUsageDisplay();
    return;
  }

  try {
    const params = new URLSearchParams({ hour: String(hour) });
    const response = await fetch(`${API_BASE}/energy/users/usage?${params}`);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const rows = await response.json();

    const usageByUserId = {};
    if (Array.isArray(rows)) {
      rows.forEach((row) => {
        usageByUserId[row.userId] = row.usedKwh;
      });
    }

    currentUsageByUserId = {};
    ENERGY_USER_IDS.forEach((userId) => {
      const usedKwh = usageByUserId[userId];
      currentUsageByUserId[userId] = usedKwh === null || usedKwh === undefined
        ? 0
        : Number(usedKwh) || 0;
    });

    ENERGY_USER_IDS.forEach((userId) => {
      const element = document.getElementById(usedKwhElementId(userId));
      const usedKwh = usageByUserId[userId];
      if (!element) {
        return;
      }
      if (usedKwh === null || usedKwh === undefined) {
        element.textContent = '—';
        return;
      }
      element.textContent = `${formatNumber(usedKwh)} kWh`;
    });
  } catch (err) {
    resetPerLoadUsageDisplay();
    showAlert(
      `Could not load per-load usage. ${err.message}. Check that OperatorApi is running and reachable through the /energy proxy.`
    );
  }
}

function renderHaySession(session) {
  const active = session && session.active;
  const activeEl = document.getElementById('hay-active');
  if (!activeEl) {
    return;
  }

  activeEl.textContent = active ? 'Yes' : 'No';
  document.getElementById('hay-moisture').textContent = formatNumber(session?.moisturePercent);
  document.getElementById('hay-started').textContent = formatHour(session?.startedAt);
  document.getElementById('hay-stopped').textContent = formatHour(session?.stoppedAt);

  const startBtn = document.getElementById('hay-start-btn');
  const stopBtn = document.getElementById('hay-stop-btn');
  if (startBtn) {
    startBtn.disabled = active;
  }
  if (stopBtn) {
    stopBtn.disabled = !active;
  }

  const loading = document.getElementById('hay-session-loading');
  const dataBlock = document.getElementById('hay-session-data');
  if (loading) {
    loading.classList.add('d-none');
  }
  if (dataBlock) {
    dataBlock.classList.remove('d-none');
  }
}

async function loadHayDryingSession() {
  if (!document.getElementById('hay-active')) {
    return;
  }

  try {
    const response = await fetch(`${API_BASE}/energy/hay-drying/session`);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const session = await response.json();
    renderHaySession(session);
  } catch (err) {
    const loading = document.getElementById('hay-session-loading');
    if (loading) {
      loading.classList.add('d-none');
    }
    showAlert(
      `Could not load hay-drying session. ${err.message}. Check that OperatorApi is running and reachable through the /energy proxy.`
    );
  }
}

async function startHaySession(moisturePercent) {
  const response = await fetch(`${API_BASE}/energy/hay-drying/session/start`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ moisturePercent }),
  });
  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `HTTP ${response.status}`);
  }
  return response.json();
}

async function stopHaySession() {
  const response = await fetch(`${API_BASE}/energy/hay-drying/session/stop`, {
    method: 'POST',
  });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return response.json();
}

async function loadHistoricalEnergy(start, end) {
  const tbody = document.getElementById('historical-body');
  if (!tbody) {
    return;
  }

  tbody.innerHTML =
    '<tr><td colspan="7" class="historical-empty">Loading historical data…</td></tr>';

  const params = new URLSearchParams({
    start: toApiDateTime(start),
    end: toApiDateTime(end),
  });

  try {
    const response = await fetch(`${API_BASE}/energy/historical?${params}`);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const rows = await response.json();

    if (!Array.isArray(rows) || rows.length === 0) {
      tbody.innerHTML =
        '<tr><td colspan="7" class="historical-empty">No hourly data for this date range.</td></tr>';
      return;
    }

    tbody.innerHTML = rows
      .map(
        (row) => `
      <tr>
        <td>${formatHour(row.hour)}</td>
        <td>${formatNumber(row.farmProducedKwh)}</td>
        <td>${formatNumber(row.farmUsedKwh)}</td>
        <td>${formatNumber(row.gridUsedKwh)}</td>
        <td>${formatNumber(row.gridExportedKwh)}</td>
        <td>${formatNumber(row.batteryChargedKwh)}</td>
        <td>${formatNumber(row.batteryDischargedKwh)}</td>
      </tr>`
      )
      .join('');
  } catch (err) {
    tbody.innerHTML =
      '<tr><td colspan="7" class="historical-error">Could not load historical data.</td></tr>';
    showAlert(
      `Could not load historical energy. ${err.message}. Check that OperatorApi is running and reachable through the /energy proxy.`
    );
  }
}

function initDefaultHistoricalRange() {
  const startInput = document.getElementById('start');
  const endInput = document.getElementById('end');
  if (!startInput || !endInput) {
    return;
  }
  const end = new Date();
  const start = new Date(end.getTime() - 7 * 24 * 60 * 60 * 1000);
  startInput.value = toDatetimeLocalValue(start);
  endInput.value = toDatetimeLocalValue(end);
}

async function refreshDashboard() {
  await loadCurrentEnergy();
  await loadPerLoadUsage(current.hour);
  await loadHayDryingSession();
  updateFlowVisualization();
}

const insights = [];

function renderInsights() {
  const container = document.getElementById('insights-container');
  const countEl = document.getElementById('insight-count');
  if (!container) {
    return;
  }

  if (insights.length === 0) {
    container.innerHTML = '<div class="insights-empty">No insights — AI not connected.</div>';
    if (countEl) {
      countEl.textContent = '—';
    }
    return;
  }

  container.innerHTML = insights.map((ins) => `
    <div class="insight-card ${ins.actionable ? 'actionable' : ''} ${ins.applied ? 'applied' : ''}" data-id="${ins.id}">
      <div class="insight-type ${ins.type}">${ins.typeLabel}</div>
      ${ins.query ? `<div class="insight-query">“${ins.query}”</div>` : ''}
      <div class="insight-body">${ins.body}</div>
      <div class="confidence">
        <div class="confidence-bar"><div class="confidence-fill" style="width:${ins.confidence}%"></div></div>
        ${ins.confidence}% confidence
      </div>
      ${ins.actionable && !ins.applied ? `
        <div class="insight-action">
          <button class="btn btn-primary apply-btn" data-id="${ins.id}">${tablerIcon('check', { size: 13 })} Apply</button>
          <button class="btn dismiss-btn" data-id="${ins.id}">${tablerIcon('x', { size: 13 })} Dismiss</button>
        </div>
      ` : ins.applied ? `<div class="insight-applied">${tablerIcon('check', { size: 12 })} Applied by AI</div>` : ''}
    </div>
  `).join('');

  if (countEl) {
    countEl.textContent = `${insights.length} active`;
  }

  container.querySelectorAll('.apply-btn').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const id = Number(btn.dataset.id);
      const ins = insights.find((i) => i.id === id);
      if (ins) {
        ins.applied = true;
        renderInsights();
      }
    });
  });
  container.querySelectorAll('.dismiss-btn').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const id = Number(btn.dataset.id);
      const idx = insights.findIndex((i) => i.id === id);
      if (idx > -1) {
        insights.splice(idx, 1);
        renderInsights();
      }
    });
  });
}

function initAiShell() {
  document.getElementById('btn-estop')?.addEventListener('click', () => {
    if (!confirm('EMERGENCY STOP will immediately disconnect all controllable loads and isolate battery. Confirm?')) {
      return;
    }
    alert('Emergency stop engaged. All non-critical loads offline. Manual reset required.');
    uiState.aiActive = false;
    document.getElementById('ai-chat-input').disabled = true;
    document.getElementById('ai-chat-send').disabled = true;
    document.getElementById('ai-chat-input').placeholder = 'AI suspended — emergency stop active';
    document.getElementById('ai-strategy-text').textContent = 'Emergency stop engaged. Deterministic safety layer only. Manual reset required.';
  });

  document.getElementById('toggle-manual')?.addEventListener('change', (e) => {
    uiState.manualOverride = e.target.checked;
    const chatDisabled = e.target.checked || !uiState.aiActive;
    document.getElementById('ai-chat-input').disabled = chatDisabled;
    document.getElementById('ai-chat-send').disabled = chatDisabled;
    if (uiState.manualOverride) {
      document.getElementById('ai-strategy-text').textContent = 'Manual override active. AI recommendations paused. Deterministic control only.';
      document.getElementById('ai-chat-input').placeholder = 'AI paused — manual override active';
    } else if (uiState.aiActive) {
      document.getElementById('ai-strategy-text').textContent = '—';
      document.getElementById('ai-chat-input').placeholder = 'Ask Aether about strategy, loads, or forecast…';
    }
  });

  document.getElementById('toggle-island')?.addEventListener('change', (e) => {
    uiState.islandMode = e.target.checked;
    const safetyBar = document.querySelector('.safety-bar');
    if (safetyBar) {
      safetyBar.classList.toggle('island-active', uiState.islandMode);
    }
  });

  const tooltip = document.getElementById('tooltip');
  document.querySelectorAll('.node-group').forEach((g) => {
    g.addEventListener('mouseenter', () => {
      const id = g.dataset.id;
      tooltip.textContent = SOURCE_NODE_TOOLTIPS[id] || userNameById[id] || id;
      tooltip.classList.add('visible');
    });
    g.addEventListener('mousemove', (e) => {
      tooltip.style.left = `${e.clientX + 12}px`;
      tooltip.style.top = `${e.clientY + 12}px`;
    });
    g.addEventListener('mouseleave', () => tooltip.classList.remove('visible'));
  });

  function submitChat() {
    if (uiState.manualOverride || !uiState.aiActive) {
      return;
    }
    const input = document.getElementById('ai-chat-input');
    const query = input.value.trim();
    if (!query) {
      return;
    }
    input.value = '';
    document.getElementById('ai-strategy-text').textContent = 'AI chat is not connected.';
    input.focus();
  }

  document.getElementById('ai-chat-send')?.addEventListener('click', submitChat);
  document.getElementById('ai-chat-input')?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      submitChat();
    }
  });
}

function initHayAndHistoricalHandlers() {
  const hayStartForm = document.getElementById('hay-start-form');
  if (hayStartForm) {
    hayStartForm.addEventListener('submit', async (event) => {
      event.preventDefault();
      const input = document.getElementById('hay-moisture-input');
      const moisture = Number(input.value);
      if (Number.isNaN(moisture) || moisture < 0 || moisture > 100) {
        showAlert('Enter a hay moisture percentage between 0 and 100.');
        return;
      }
      try {
        const session = await startHaySession(moisture);
        renderHaySession(session);
        clearAlerts();
        showAlert('Hay-drying session started.', 'success');
      } catch (err) {
        showAlert(`Could not start hay-drying session. ${err.message}`);
      }
    });
  }

  const hayStopBtn = document.getElementById('hay-stop-btn');
  if (hayStopBtn) {
    hayStopBtn.addEventListener('click', async () => {
      try {
        const session = await stopHaySession();
        renderHaySession(session);
        clearAlerts();
        showAlert('Hay-drying session stopped.', 'success');
      } catch (err) {
        showAlert(`Could not stop hay-drying session. ${err.message}`);
      }
    });
  }

  const historicalForm = document.getElementById('historical-form');
  if (historicalForm) {
    historicalForm.addEventListener('submit', (event) => {
      event.preventDefault();
      const start = document.getElementById('start').value;
      const end = document.getElementById('end').value;
      loadHistoricalEnergy(start, end);
    });
  }
}

initTablerIcons();
initDefaultHistoricalRange();
initAiShell();
initHayAndHistoricalHandlers();
renderInsights();
animateDashes();
updateFlowVisualization();
loadEnergyUsers();
refreshDashboard();
setInterval(refreshDashboard, AUTO_REFRESH_MS);
