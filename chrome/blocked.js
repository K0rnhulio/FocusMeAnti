/**
 * FocusMe - Blocked Screen Logic
 * Live countdown and automatic reset detection with 1PM - 9PM Schedule
 */

const ALLOWED_START_HOUR = 13; // 1:00 PM
const ALLOWED_END_HOUR = 21;   // 9:00 PM

const FOCUS_QUOTES = [
  "Focus is a muscle. Every time you step away from distraction, it grows stronger.",
  "Deep work produces extraordinary results. Enjoy the calm of this hour.",
  "The secret of getting ahead is getting started on what truly matters.",
  "Distraction is the enemy of depth. Reclaim your attention.",
  "Small daily disciplines repeated consistently lead to massive achievements."
];

function getQueryParams() {
  const params = new URLSearchParams(window.location.search);
  return {
    originalUrl: params.get('url') || ''
  };
}

function isWithinPermittedHours(date = new Date()) {
  const currentHour = date.getHours();
  return currentHour >= ALLOWED_START_HOUR && currentHour < ALLOWED_END_HOUR;
}

function formatCountdown(totalSeconds, showHours = false) {
  const hours = Math.floor(totalSeconds / 3600);
  const mins = Math.floor((totalSeconds % 3600) / 60);
  const secs = totalSeconds % 60;

  if (showHours || hours > 0) {
    return `${String(hours).padStart(2, '0')}:${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

function getNextUnlockInfo(date = new Date()) {
  const currentHour = date.getHours();

  if (!isWithinPermittedHours(date)) {
    const target = new Date(date.getFullYear(), date.getMonth(), date.getDate(), ALLOWED_START_HOUR, 0, 0, 0);
    if (currentHour >= ALLOWED_END_HOUR) {
      target.setDate(target.getDate() + 1); // Tomorrow at 13:00
    }
    const secondsRemaining = Math.max(0, Math.floor((target.getTime() - date.getTime()) / 1000));
    
    // Total duration from previous 21:00 to 13:00 is 16 hours (57600s)
    const totalOutsideSeconds = 16 * 3600;
    const progressPercent = Math.min(100, Math.max(0, ((totalOutsideSeconds - secondsRemaining) / totalOutsideSeconds) * 100));

    return {
      isPermitted: false,
      secondsRemaining,
      targetTimeStr: '1:00 PM (13:00)',
      progressPercent
    };
  } else {
    const nextHour = new Date(date.getFullYear(), date.getMonth(), date.getDate(), currentHour + 1, 0, 0, 0);
    const secondsRemaining = Math.max(0, Math.floor((nextHour.getTime() - date.getTime()) / 1000));
    const targetHourStr = (currentHour + 1 === ALLOWED_END_HOUR)
      ? '21:00 (Lock time)' 
      : `${String(currentHour + 1).padStart(2, '0')}:00`;

    const elapsedInHour = 3600 - secondsRemaining;
    const progressPercent = Math.min(100, Math.max(0, (elapsedInHour / 3600) * 100));

    return {
      isPermitted: true,
      secondsRemaining,
      targetTimeStr: targetHourStr,
      progressPercent
    };
  }
}

document.addEventListener('DOMContentLoaded', () => {
  const mainTitleEl = document.getElementById('main-title');
  const subtitleEl = document.getElementById('subtitle-text');
  const timerLabelEl = document.getElementById('timer-label');
  const countdownEl = document.getElementById('countdown-text');
  const targetTimeEl = document.getElementById('target-time-text');
  const progressFillEl = document.getElementById('progress-fill');
  const quoteTextEl = document.getElementById('quote-text');
  const autoReturnCheckbox = document.getElementById('auto-return-checkbox');
  const targetSiteContainer = document.getElementById('target-site-container');
  const targetSiteName = document.getElementById('target-site-name');

  const { originalUrl } = getQueryParams();

  // Show destination if available
  if (originalUrl) {
    try {
      const parsedUrl = new URL(originalUrl);
      targetSiteName.textContent = parsedUrl.hostname;
      targetSiteContainer.style.display = 'block';
    } catch (e) {
      targetSiteName.textContent = originalUrl;
      targetSiteContainer.style.display = 'block';
    }
  }

  // Set random inspirational quote
  const randomQuote = FOCUS_QUOTES[Math.floor(Math.random() * FOCUS_QUOTES.length)];
  quoteTextEl.textContent = randomQuote;

  // Load auto-return preference
  chrome.storage.local.get({ autoRedirectOnReset: true }, (data) => {
    autoReturnCheckbox.checked = data.autoRedirectOnReset !== false;
  });

  autoReturnCheckbox.addEventListener('change', () => {
    chrome.storage.local.set({ autoRedirectOnReset: autoReturnCheckbox.checked });
  });

  let isRedirecting = false;

  function handleResetUnlocked() {
    if (isRedirecting) return;
    isRedirecting = true;

    countdownEl.textContent = "00:00:00";
    countdownEl.style.color = "#10b981"; // Emerald
    targetTimeEl.textContent = "Allowance unlocked!";

    if (autoReturnCheckbox.checked && originalUrl) {
      targetTimeEl.textContent = "Redirecting back to site...";
      setTimeout(() => {
        window.location.href = originalUrl;
      }, 1200);
    }
  }

  function updateDisplay() {
    const unlockInfo = getNextUnlockInfo();

    if (!unlockInfo.isPermitted) {
      mainTitleEl.textContent = "Outside Allowed Hours";
      subtitleEl.innerHTML = "Access is completely locked before <strong class='highlight'>1:00 PM</strong> and after <strong class='highlight'>9:00 PM</strong>.";
      timerLabelEl.textContent = "Daily Window Opens In";
      countdownEl.textContent = formatCountdown(unlockInfo.secondsRemaining, true);
      targetTimeEl.textContent = `at ${unlockInfo.targetTimeStr}`;
      progressFillEl.style.width = `${unlockInfo.progressPercent}%`;
    } else {
      mainTitleEl.textContent = "Hourly Quota Used";
      subtitleEl.innerHTML = "You have used your combined <strong class='highlight'>5 minutes</strong> for this clock hour.";
      timerLabelEl.textContent = "Next Allowance Unlocks In";
      countdownEl.textContent = formatCountdown(unlockInfo.secondsRemaining, false);
      targetTimeEl.textContent = `at ${unlockInfo.targetTimeStr}`;
      progressFillEl.style.width = `${unlockInfo.progressPercent}%`;
    }

    if (unlockInfo.secondsRemaining <= 0) {
      handleResetUnlocked();
    }
  }

  updateDisplay();
  const timerInterval = setInterval(() => {
    updateDisplay();

    chrome.runtime.sendMessage({ type: 'GET_STATUS' }, (res) => {
      if (chrome.runtime.lastError || !res) return;
      if (res.isPermitted && res.remaining > 0) {
        clearInterval(timerInterval);
        handleResetUnlocked();
      }
    });
  }, 1000);

  chrome.runtime.onMessage.addListener((msg) => {
    if (msg.type === 'HOUR_RESET') {
      if (msg.isPermitted && msg.remaining > 0) {
        clearInterval(timerInterval);
        handleResetUnlocked();
      } else {
        updateDisplay();
      }
    }
  });
});
