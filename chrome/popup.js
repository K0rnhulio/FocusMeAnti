/**
 * FocusMe - Popup Dashboard Controller
 * Active Window: 1:00 PM (13:00) - 9:00 PM (21:00)
 * 30-Minute Productivity Reflection Tracker
 */

const RING_CIRCUMFERENCE = 339.292; // 2 * Math.PI * 54

function formatTime(seconds) {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

function formatMinutesAndSeconds(seconds) {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins}m ${String(secs).padStart(2, '0')}s`;
}

function formatDuration(totalSeconds) {
  const hours = Math.floor(totalSeconds / 3600);
  const mins = Math.floor((totalSeconds % 3600) / 60);
  const secs = totalSeconds % 60;
  if (hours > 0) {
    return `${hours}h ${mins}m ${secs}s`;
  }
  return `${mins}m ${secs}s`;
}

function formatClockTime(timestamp) {
  const d = new Date(timestamp);
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function getCleanDomain(input) {
  let cleaned = input.trim().toLowerCase();
  cleaned = cleaned.replace(/^https?:\/\//, '').replace(/^www\./, '');
  cleaned = cleaned.split('/')[0].split('?')[0].split('#')[0];
  return cleaned;
}

document.addEventListener('DOMContentLoaded', () => {
  const hourWindowText = document.getElementById('hour-window-text');
  const pulseDot = document.getElementById('pulse-dot');
  const ringFill = document.getElementById('ring-fill');
  const remainingTimeText = document.getElementById('remaining-time-text');
  const remainingTimeSub = document.getElementById('remaining-time-sub');
  const statusBadge = document.getElementById('status-badge');
  const statusText = document.getElementById('status-text');
  const resetHint = document.getElementById('reset-hint');

  const tabButtons = document.querySelectorAll('.tab-btn');
  const tabContents = document.querySelectorAll('.tab-content');

  const domainsList = document.getElementById('domains-list');
  const newDomainInput = document.getElementById('new-domain-input');
  const addDomainBtn = document.getElementById('add-domain-btn');

  const statUsedHour = document.getElementById('stat-used-hour');
  const statUsedToday = document.getElementById('stat-used-today');
  const hourlyBars = document.getElementById('hourly-bars');

  const reflectionCountBadge = document.getElementById('reflection-count-badge');
  const reflectionsList = document.getElementById('reflections-list');

  const settingShowPill = document.getElementById('setting-show-pill');
  const settingAutoRedirect = document.getElementById('setting-auto-redirect');
  const resetCurrentHourBtn = document.getElementById('reset-current-hour-btn');

  let currentStatus = null;

  // Tabs Navigation
  tabButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      tabButtons.forEach(b => b.classList.remove('active'));
      tabContents.forEach(c => c.classList.remove('active'));

      btn.classList.add('active');
      const targetId = `tab-${btn.dataset.tab}`;
      document.getElementById(targetId).classList.add('active');
    });
  });

  // Render Status
  function render(status) {
    currentStatus = status;

    const isPermitted = status.isPermitted;
    const remaining = isPermitted ? (status.remaining || 0) : 0;
    const quota = status.quota || 300;
    const used = status.used || 0;
    const secondsUntilReset = status.secondsUntilReset || 0;
    const hasReflected = status.hasReflected;
    const reflectionHistory = status.reflectionHistory || [];

    const now = new Date();
    const curHour = now.getHours();

    // Status Badge & Circular Progress
    statusBadge.classList.remove('status-active', 'status-warning', 'status-blocked');

    if (!isPermitted) {
      // Outside 1PM - 9PM
      pulseDot.style.background = '#64748b';
      pulseDot.style.boxShadow = 'none';
      hourWindowText.textContent = 'Window: 1PM – 9PM (Locked)';

      remainingTimeText.textContent = '00:00';
      remainingTimeSub.textContent = 'Locked until 1:00 PM';

      statusBadge.classList.add('status-blocked');
      statusText.textContent = 'Outside Permitted Hours';
      ringFill.style.stroke = '#64748b'; // Slate
      ringFill.style.strokeDashoffset = RING_CIRCUMFERENCE; // Empty ring

      resetHint.textContent = `Daily window opens in ${formatDuration(secondsUntilReset)} at 1:00 PM`;
    } else {
      // Inside 1PM - 9PM
      pulseDot.style.background = '#10b981';
      pulseDot.style.boxShadow = '0 0 8px #10b981';
      const nextHour = (curHour + 1) % 24;
      hourWindowText.textContent = `${String(curHour).padStart(2, '0')}:00 - ${String(nextHour).padStart(2, '0')}:00`;

      remainingTimeText.textContent = formatTime(remaining);
      remainingTimeSub.textContent = '/ 5m allowance';

      const progressFraction = Math.max(0, Math.min(1, remaining / quota));
      const dashOffset = RING_CIRCUMFERENCE * (1 - progressFraction);
      ringFill.style.strokeDashoffset = dashOffset;

      if (!hasReflected) {
        statusBadge.classList.add('status-warning');
        statusText.textContent = 'Check-in Required';
        ringFill.style.stroke = '#f59e0b'; // Amber
      } else if (remaining <= 0) {
        statusBadge.classList.add('status-blocked');
        statusText.textContent = 'Quota Exhausted';
        ringFill.style.stroke = '#ef4444'; // Red
      } else if (remaining <= 60) {
        statusBadge.classList.add('status-warning');
        statusText.textContent = 'Final Minute';
        ringFill.style.stroke = '#f59e0b'; // Amber
      } else {
        statusBadge.classList.add('status-active');
        statusText.textContent = `${Math.ceil(remaining / 60)}m Remaining`;
        ringFill.style.stroke = '#38bdf8'; // Cyan
      }

      const minsUntilReset = Math.floor(secondsUntilReset / 60);
      const secsUntilReset = secondsUntilReset % 60;
      resetHint.textContent = `Resets in ${minsUntilReset}m ${String(secsUntilReset).padStart(2, '0')}s (no rollover)`;
    }

    // Stats tab
    statUsedHour.textContent = formatMinutesAndSeconds(used);

    // Calculate today total & render 24h chart
    const d = new Date();
    const todayPrefix = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    let todayTotalSecs = 0;
    const usage = status.hourlyUsage || {};

    hourlyBars.innerHTML = '';
    for (let h = 0; h < 24; h++) {
      const hourKey = `${todayPrefix}-${String(h).padStart(2, '0')}`;
      const hourSecs = usage[hourKey] || 0;
      todayTotalSecs += hourSecs;

      const col = document.createElement('div');
      col.className = 'hourly-bar-col';
      
      const isAllowedHour = (h >= 13 && h < 21);
      col.title = `Hour ${String(h).padStart(2, '0')}:00 - ${formatMinutesAndSeconds(hourSecs)} ${isAllowedHour ? '(Active Window)' : '(Locked Window)'}`;

      const bar = document.createElement('div');
      bar.className = 'hourly-bar';
      const heightPercent = Math.min(100, (hourSecs / quota) * 100);
      bar.style.height = `${Math.max(4, heightPercent)}%`;

      if (!isAllowedHour) {
        bar.style.opacity = '0.35';
      }

      if (h === curHour) {
        bar.classList.add('active-hour');
      } else if (hourSecs >= quota) {
        bar.classList.add('blocked-hour');
      } else if (hourSecs > 0) {
        bar.classList.add('filled-hour');
      }

      col.appendChild(bar);
      hourlyBars.appendChild(col);
    }

    statUsedToday.textContent = formatMinutesAndSeconds(todayTotalSecs);

    // Render Reflection History
    reflectionCountBadge.textContent = `${reflectionHistory.length} entries`;
    if (reflectionHistory.length === 0) {
      reflectionsList.innerHTML = '<div class="reflection-empty">No check-ins logged yet today.</div>';
    } else {
      reflectionsList.innerHTML = '';
      reflectionHistory.slice(0, 10).forEach(entry => {
        const item = document.createElement('div');
        item.className = 'reflection-item';
        item.innerHTML = `
          <div class="reflection-item-header">
            <span>${formatClockTime(entry.timestamp)}</span>
            <span>Check-in</span>
          </div>
          <p class="reflection-item-text">"${entry.answer}"</p>
        `;
        reflectionsList.appendChild(item);
      });
    }

    // Settings fields
    settingShowPill.checked = status.showPill !== false;
    settingAutoRedirect.checked = status.autoRedirectOnReset !== false;

    // Render domains list
    renderDomains(status.blockedDomains || []);
  }

  // Render Blocked Domains
  function renderDomains(domains) {
    domainsList.innerHTML = '';
    domains.forEach(domain => {
      const chip = document.createElement('div');
      chip.className = 'domain-chip';

      chip.innerHTML = `
        <span class="domain-name">${domain}</span>
        <div style="display: flex; align-items: center; gap: 8px;">
          <span class="domain-tag">Blocked</span>
          <button class="delete-domain-btn" title="Remove" data-domain="${domain}">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
      `;

      chip.querySelector('.delete-domain-btn').addEventListener('click', (e) => {
        const toDelete = e.currentTarget.dataset.domain;
        const updated = domains.filter(d => d !== toDelete);
        chrome.runtime.sendMessage({
          type: 'UPDATE_SETTINGS',
          settings: { blockedDomains: updated }
        }, fetchStatus);
      });

      domainsList.appendChild(chip);
    });
  }

  // Add Domain
  function handleAddDomain() {
    const rawVal = newDomainInput.value;
    const clean = getCleanDomain(rawVal);
    if (!clean || !clean.includes('.')) {
      newDomainInput.focus();
      return;
    }

    const currentList = (currentStatus && currentStatus.blockedDomains) || [];
    if (!currentList.includes(clean)) {
      const updated = [...currentList, clean];
      chrome.runtime.sendMessage({
        type: 'UPDATE_SETTINGS',
        settings: { blockedDomains: updated }
      }, () => {
        newDomainInput.value = '';
        fetchStatus();
      });
    }
  }

  addDomainBtn.addEventListener('click', handleAddDomain);
  newDomainInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') handleAddDomain();
  });

  // Settings change listeners
  settingShowPill.addEventListener('change', () => {
    chrome.runtime.sendMessage({
      type: 'UPDATE_SETTINGS',
      settings: { showPill: settingShowPill.checked }
    });
  });

  settingAutoRedirect.addEventListener('change', () => {
    chrome.runtime.sendMessage({
      type: 'UPDATE_SETTINGS',
      settings: { autoRedirectOnReset: settingAutoRedirect.checked }
    });
  });

  resetCurrentHourBtn.addEventListener('click', () => {
    chrome.runtime.sendMessage({ type: 'RESET_USAGE_DEBUG' }, () => {
      resetCurrentHourBtn.textContent = '✓ Reset Applied!';
      setTimeout(() => {
        resetCurrentHourBtn.textContent = 'Reset Current Hour Allowance (Test)';
      }, 1500);
      fetchStatus();
    });
  });

  // Fetch status from background
  function fetchStatus() {
    chrome.runtime.sendMessage({ type: 'GET_STATUS' }, (res) => {
      if (chrome.runtime.lastError || !res) return;
      render(res);
    });
  }

  fetchStatus();
  setInterval(fetchStatus, 1000);
});
