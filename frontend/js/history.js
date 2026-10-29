// Hourly history table. The period is validated by the API (REQ-API-HISTORY) — a bad or
// too-long range comes back as 400 and its message is shown in the table itself.

const HISTORY_COLUMN_COUNT = 7;

// The fixed validation messages HistoryService sends, mapped to translation keys. Any
// other message is shown as the API wrote it.
const HISTORY_ERROR_KEYS = {
  'from must be before to': 'history.error.fromBeforeTo',
  'the requested range must not exceed 7 days': 'history.error.rangeTooLong',
};

let historyLoadedOnce = false;

function historyErrorText(message) {
  const key = HISTORY_ERROR_KEYS[message];
  return key ? t(key) : message;
}

function historyRowHtml(row) {
  const counterCells = COUNTER_ORDER
    .map((counter) => `<td>${escapeHtml(formatNumber(row[counter], 2))}</td>`)
    .join('');
  return `
    <tr>
      <td>${escapeHtml(assetDisplayName(row.assetId))}</td>
      <td>${escapeHtml(formatInstant(row.hourStart))}</td>
      ${counterCells}
    </tr>`;
}

function showHistoryMessage(message, className) {
  document.getElementById('history-body').innerHTML =
    `<tr><td colspan="${HISTORY_COLUMN_COUNT}" class="${className}">${escapeHtml(message)}</td></tr>`;
}

async function loadHistory() {
  const fromValue = document.getElementById('history-from').value;
  const toValue = document.getElementById('history-to').value;
  const assetId = document.getElementById('history-asset').value;

  historyLoadedOnce = true;
  showHistoryMessage(t('history.loading'), 'history-empty');

  try {
    const response = await fetchHistory(toUtcInstant(fromValue), toUtcInstant(toValue), assetId);
    const rows = Array.isArray(response.rows) ? response.rows : [];

    if (rows.length === 0) {
      showHistoryMessage(t('history.empty'), 'history-empty');
      return;
    }

    document.getElementById('history-body').innerHTML = rows.map(historyRowHtml).join('');
  } catch (error) {
    showHistoryMessage(historyErrorText(error.message), 'history-error');
  }
}

// After a language switch: reload the table if the farmer already loaded a period,
// otherwise show the prompt in the new language.
function rerenderHistory() {
  if (historyLoadedOnce) {
    loadHistory();
  } else {
    showHistoryMessage(t('history.prompt'), 'history-empty');
  }
}

function initHistory() {
  const now = new Date();
  const dayEarlier = new Date(now.getTime() - 24 * 60 * 60 * 1000);
  document.getElementById('history-from').value = toDatetimeLocalValue(dayEarlier);
  document.getElementById('history-to').value = toDatetimeLocalValue(now);

  document.getElementById('history-form').addEventListener('submit', (event) => {
    event.preventDefault();
    loadHistory();
  });
}
