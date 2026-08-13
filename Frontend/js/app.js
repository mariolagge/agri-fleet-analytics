// Empty base → relative /energy/* (Nginx on :8080 proxies to OperatorApi :8083).
const API_BASE = '';
const CURRENT_AUTO_REFRESH_MS = 12_000;

const STATIC_ENERGY_USERS = [
  { userId: 'milk-cooling', name: 'Milk cooling', flexible: false },
  { userId: 'milking-robot', name: 'Milking robot', flexible: false },
  { userId: 'hay-dryer', name: 'Hay dryer', flexible: true },
  { userId: 'barn-ventilation', name: 'Barn ventilation', flexible: true },
];

const FLEXIBLE_LOAD_LABELS = {
  'hay-dryer': 'Hay dryer',
  'barn-ventilation': 'Barn ventilation',
};

const LIVE_FLOW_LABELS = [
  { key: 'pvToLoadsKwh', label: 'PV → loads' },
  { key: 'pvToBatteryKwh', label: 'PV → battery' },
  { key: 'batteryToLoadsKwh', label: 'Battery → loads' },
  { key: 'gridToLoadsKwh', label: 'Grid → loads' },
  { key: 'pvToGridExportKwh', label: 'PV → grid export' },
];

let userNameById = {};

function showAlert(message, type = 'danger') {
  const placeholder = document.getElementById('alert-placeholder');
  placeholder.innerHTML = `
    <div class="alert alert-${type} alert-dismissible fade show" role="alert">
      ${message}
      <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    </div>`;
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

function flexibleLoadDisplayName(userId) {
  return userNameById[userId] || FLEXIBLE_LOAD_LABELS[userId] || userId;
}

function renderEnergyUsers(users) {
  const list = document.getElementById('users-list');
  const loading = document.getElementById('users-loading');

  userNameById = {};
  users.forEach((user) => {
    userNameById[user.userId] = user.name;
  });

  list.innerHTML = users
    .map((user) => {
      const flexibleBadge = user.flexible
        ? '<span class="badge bg-warning text-dark">Flexible</span>'
        : '<span class="badge bg-secondary">Must-run</span>';
      return `
    <li class="list-group-item d-flex justify-content-between align-items-center">
      <span>${escapeHtml(user.name)} ${flexibleBadge}</span>
      <span class="text-muted small font-monospace">${escapeHtml(user.userId)}</span>
    </li>`;
    })
    .join('');

  loading.classList.add('d-none');
  list.classList.remove('d-none');
}

async function loadEnergyUsers() {
  try {
    const response = await fetch(`${API_BASE}/energy/users`);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const users = await response.json();
    if (!Array.isArray(users) || users.length === 0) {
      throw new Error('Empty user list');
    }
    renderEnergyUsers(users);
  } catch {
    renderEnergyUsers(STATIC_ENERGY_USERS);
  }
}

function renderBatteryFromCurrent(data) {
  const loading = document.getElementById('battery-loading');
  const dataBlock = document.getElementById('battery-data');

  const soc = data.batterySocPercent;
  document.getElementById('battery-soc').textContent =
    soc === null || soc === undefined ? '—' : `${formatNumber(soc)}%`;

  const charged = Number(data.batteryChargedKwh) || 0;
  const discharged = Number(data.batteryDischargedKwh) || 0;
  let activity = 'Idle';
  if (charged > 0 && discharged > 0) {
    activity = 'Charging and discharging (net flows this hour)';
  } else if (charged > 0) {
    activity = 'Charging';
  } else if (discharged > 0) {
    activity = 'Discharging';
  }
  document.getElementById('battery-activity').textContent = activity;
  document.getElementById('battery-charged').textContent = formatNumber(data.batteryChargedKwh);
  document.getElementById('battery-discharged').textContent = formatNumber(data.batteryDischargedKwh);

  loading.classList.add('d-none');
  dataBlock.classList.remove('d-none');
}

function renderLiveFlows(liveFlows) {
  const list = document.getElementById('flows-list');
  const loading = document.getElementById('flows-loading');
  const flows = liveFlows || {};

  list.innerHTML = LIVE_FLOW_LABELS
    .map(({ key, label }) => {
      const kwh = flows[key];
      return `
    <li class="list-group-item d-flex justify-content-between align-items-center">
      <span><span class="flow-arrow">→</span> ${escapeHtml(label)}</span>
      <span class="font-monospace">${formatNumber(kwh)} kWh</span>
    </li>`;
    })
    .join('');

  loading.classList.add('d-none');
  list.classList.remove('d-none');
}

function renderFlexibleLoads(flexibleLoads) {
  const list = document.getElementById('flexible-list');
  const loading = document.getElementById('flexible-loading');
  const loads = Array.isArray(flexibleLoads) ? flexibleLoads : [];

  if (loads.length === 0) {
    list.innerHTML =
      '<li class="list-group-item text-muted">No flexible load state returned from the API.</li>';
  } else {
    list.innerHTML = loads
      .map((load) => {
        const percent = Number(load.percent) || 0;
        const on = percent > 0;
        const badgeClass = on ? 'badge-on' : 'badge-off';
        const statusText = on ? `ON (${formatNumber(percent)}%)` : 'OFF (0%)';
        return `
    <li class="list-group-item d-flex justify-content-between align-items-center">
      <span>${escapeHtml(flexibleLoadDisplayName(load.userId))}</span>
      <span class="badge ${badgeClass}">${statusText}</span>
    </li>`;
      })
      .join('');
  }

  loading.classList.add('d-none');
  list.classList.remove('d-none');
}

function renderHaySession(session) {
  const loading = document.getElementById('hay-session-loading');
  const dataBlock = document.getElementById('hay-session-data');

  const active = session && session.active;
  document.getElementById('hay-active').textContent = active ? 'Yes' : 'No';
  document.getElementById('hay-moisture').textContent = formatNumber(session?.moisturePercent);
  document.getElementById('hay-started').textContent = formatHour(session?.startedAt);
  document.getElementById('hay-stopped').textContent = formatHour(session?.stoppedAt);

  document.getElementById('hay-start-btn').disabled = active;
  document.getElementById('hay-stop-btn').disabled = !active;

  loading.classList.add('d-none');
  dataBlock.classList.remove('d-none');
}

async function loadHayDryingSession() {
  try {
    const response = await fetch(`${API_BASE}/energy/hay-drying/session`);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const session = await response.json();
    renderHaySession(session);
  } catch (err) {
    document.getElementById('hay-session-loading').classList.add('d-none');
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

async function loadCurrentEnergy() {
  const loading = document.getElementById('current-loading');
  const dataBlock = document.getElementById('current-data');

  loading.classList.remove('d-none');
  dataBlock.classList.add('d-none');
  document.getElementById('battery-loading').classList.remove('d-none');
  document.getElementById('battery-data').classList.add('d-none');
  document.getElementById('flows-loading').classList.remove('d-none');
  document.getElementById('flows-list').classList.add('d-none');
  document.getElementById('flexible-loading').classList.remove('d-none');
  document.getElementById('flexible-list').classList.add('d-none');

  try {
    const response = await fetch(`${API_BASE}/energy/current`);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const data = await response.json();

    document.getElementById('current-hour').textContent = formatHour(data.hour);
    document.getElementById('current-solar-depleted').textContent = formatNumber(data.solarDepletedPercent);
    document.getElementById('current-grid-portion').textContent = formatNumber(data.gridPortionPercent);
    document.getElementById('current-farm-produced').textContent = formatNumber(data.farmProducedKwh);
    document.getElementById('current-farm-used').textContent = formatNumber(data.farmUsedKwh);
    document.getElementById('current-grid-imported').textContent = formatNumber(data.gridUsedKwh);
    document.getElementById('current-grid-exported').textContent = formatNumber(data.gridExportedKwh);

    loading.classList.add('d-none');
    dataBlock.classList.remove('d-none');

    renderBatteryFromCurrent(data);
    renderLiveFlows(data.liveFlows);
    renderFlexibleLoads(data.flexibleLoads);

    clearAlerts();
  } catch (err) {
    loading.classList.add('d-none');
    showAlert(
      `Could not load current status. ${err.message}. Check that OperatorApi is running and reachable through the /energy proxy.`
    );
  }
}

async function refreshDashboard() {
  await loadCurrentEnergy();
  await loadHayDryingSession();
}

async function loadHistoricalEnergy(start, end) {
  const tbody = document.getElementById('historical-body');
  tbody.innerHTML =
    '<tr><td colspan="7" class="text-muted text-center">Loading historical data…</td></tr>';

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
        '<tr><td colspan="7" class="text-muted text-center">No hourly data for this date range.</td></tr>';
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
      '<tr><td colspan="7" class="text-danger text-center">Could not load historical data.</td></tr>';
    showAlert(
      `Could not load historical energy. ${err.message}. Check that OperatorApi is running and reachable through the /energy proxy.`
    );
  }
}

function initDefaultHistoricalRange() {
  const end = new Date();
  const start = new Date(end.getTime() - 7 * 24 * 60 * 60 * 1000);
  document.getElementById('start').value = toDatetimeLocalValue(start);
  document.getElementById('end').value = toDatetimeLocalValue(end);
}

document.getElementById('refresh-current').addEventListener('click', () => refreshDashboard());

document.getElementById('historical-form').addEventListener('submit', (event) => {
  event.preventDefault();
  const start = document.getElementById('start').value;
  const end = document.getElementById('end').value;
  loadHistoricalEnergy(start, end);
});

document.getElementById('hay-start-form').addEventListener('submit', async (event) => {
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

document.getElementById('hay-stop-btn').addEventListener('click', async () => {
  try {
    const session = await stopHaySession();
    renderHaySession(session);
    clearAlerts();
    showAlert('Hay-drying session stopped.', 'success');
  } catch (err) {
    showAlert(`Could not stop hay-drying session. ${err.message}`);
  }
});

initDefaultHistoricalRange();
loadEnergyUsers();
refreshDashboard();
setInterval(() => refreshDashboard(), CURRENT_AUTO_REFRESH_MS);
