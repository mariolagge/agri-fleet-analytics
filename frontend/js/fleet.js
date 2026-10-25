// Fleet ranking: the SVG diagram and the ranked list under it.
// The assets array is rendered in the order the API returned it — that order IS the
// priority ranking (REQ-RANK), so it is never re-sorted here.

const NODE_STATUS_CLASSES = ['status-good', 'status-watch', 'status-critical'];
const NODE_LIVE_CLASSES = ['live-normal', 'live-caution', 'live-alert', 'live-stale'];

// The diagram loop runs over the four static node groups, not over the response, so a
// machine the response does not mention is reset instead of keeping its previous values.
function renderFleetDiagram(assets, selectedAssetId) {
  document.querySelectorAll('#fleet-svg .node-group').forEach((node) => {
    const asset = assets.find((candidate) => candidate.assetId === node.dataset.assetId);

    NODE_STATUS_CLASSES.forEach((cls) => node.classList.remove(cls));
    NODE_LIVE_CLASSES.forEach((cls) => node.classList.remove(cls));
    node.classList.toggle('is-selected', Boolean(asset) && asset.assetId === selectedAssetId);

    if (!asset) {
      node.querySelector('.node-status').textContent = EM_DASH;
      node.querySelector('.node-value').textContent = EM_DASH;
      node.querySelector('.node-rank').textContent = EM_DASH;
      return;
    }

    // statusClass() returns '' for an unknown status, and classList.add('') throws.
    const nodeStatusClass = statusClass(asset.status);
    if (nodeStatusClass) {
      node.classList.add(nodeStatusClass);
    }
    const nodeLiveClass = liveConditionClass(asset.liveCondition);
    if (nodeLiveClass) {
      node.classList.add(nodeLiveClass);
    }

    node.querySelector('.node-status').textContent = statusDisplayName(asset.status).toUpperCase();
    node.querySelector('.node-value').textContent = asset.utilizationPercent === null
      || asset.utilizationPercent === undefined
      ? `${EM_DASH} util`
      : `${formatNumber(asset.utilizationPercent, 0)}% util`;
    node.querySelector('.node-rank').textContent = asset.priorityRank ?? EM_DASH;
  });
}

function rankRowHtml(asset, selectedAssetId) {
  const selected = asset.assetId === selectedAssetId ? ' is-selected' : '';
  return `
    <button type="button" class="rank-row${selected}" data-asset-id="${escapeHtml(asset.assetId)}">
      <span class="rank-badge ${statusClass(asset.status)}">${escapeHtml(String(asset.priorityRank ?? EM_DASH))}</span>
      ${assetChipHtml(asset.assetId)}
      <span class="rank-info">
        <span class="rank-name-row">
          <span class="rank-name">${escapeHtml(assetDisplayName(asset.assetId))}</span>
          ${statusPillHtml(asset.status)}
        </span>
        ${flagChipsHtml(asset.flags)}
      </span>
      <span class="rank-chevron">${tablerIcon('chevron-right', { size: 14 })}</span>
    </button>`;
}

function renderFleetRanking(assets, selectedAssetId, onSelect) {
  const container = document.getElementById('fleet-ranking');
  container.innerHTML = assets.map((asset) => rankRowHtml(asset, selectedAssetId)).join('');
  container.querySelectorAll('.rank-row').forEach((row) => {
    row.addEventListener('click', () => onSelect(row.dataset.assetId));
  });
}

function renderFleet(assets, selectedAssetId, onSelect) {
  renderFleetDiagram(assets, selectedAssetId);
  renderFleetRanking(assets, selectedAssetId, onSelect);
}

function showFleetRankingError(message) {
  document.getElementById('fleet-ranking').innerHTML =
    `<div class="section-loading">${escapeHtml(message)}</div>`;
}

// Clicking a machine in the diagram selects it, exactly like clicking its ranking row.
// The four node groups are static markup, so these handlers are attached once.
function initFleetDiagram(onSelect, describeAsset) {
  const tooltip = document.getElementById('tooltip');

  document.querySelectorAll('#fleet-svg .node-group').forEach((node) => {
    const assetId = node.dataset.assetId;
    node.addEventListener('click', () => onSelect(assetId));
    node.addEventListener('mouseenter', () => {
      tooltip.textContent = describeAsset(assetId);
      tooltip.classList.add('visible');
    });
    node.addEventListener('mousemove', (event) => {
      tooltip.style.left = `${event.clientX + 12}px`;
      tooltip.style.top = `${event.clientY + 12}px`;
    });
    node.addEventListener('mouseleave', () => tooltip.classList.remove('visible'));
  });
}
