/**
 * FocusMe - Cross-Browser Hourly Site Limiter
 * Background Engine (Chrome MV3 & Firefox/Zen MV3)
 * Active Window: 1:00 PM (13:00) - 9:00 PM (21:00)
 * Mindful 30-min Reflection Prompt Requirement
 */

const DEFAULT_QUOTA_SECONDS = 300; // 5 minutes per hour
const ALLOWED_START_HOUR = 13; // 1:00 PM (13:00)
const ALLOWED_END_HOUR = 21;   // 9:00 PM (21:00)

const DEFAULT_BLOCKED_DOMAINS = [
  'twitter.com',
  'x.com',
  'reddit.com',
  'facebook.com',
  'fb.com'
];

// Helper to format local hour key: "YYYY-MM-DD-HH"
function getCurrentHourKey() {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hour = String(d.getHours()).padStart(2, '0');
  return `${year}-${month}-${day}-${hour}`;
}

// Check if current local time is within the allowed 1PM - 9PM daily window
function isWithinPermittedHours(date = new Date()) {
  const currentHour = date.getHours();
  return currentHour >= ALLOWED_START_HOUR && currentHour < ALLOWED_END_HOUR;
}

// Calculate the next unlock target and remaining seconds
function getNextUnlockInfo(date = new Date()) {
  const currentHour = date.getHours();

  if (!isWithinPermittedHours(date)) {
    const target = new Date(date.getFullYear(), date.getMonth(), date.getDate(), ALLOWED_START_HOUR, 0, 0, 0);
    if (currentHour >= ALLOWED_END_HOUR) {
      target.setDate(target.getDate() + 1); // Tomorrow at 13:00
    }
    const secondsRemaining = Math.max(0, Math.floor((target.getTime() - date.getTime()) / 1000));
    return {
      isPermitted: false,
      secondsRemaining,
      targetTimeStr: '1:00 PM (13:00)',
      targetTimestamp: target.getTime(),
      reason: 'outside_window'
    };
  } else {
    const nextHour = new Date(date.getFullYear(), date.getMonth(), date.getDate(), currentHour + 1, 0, 0, 0);
    const secondsRemaining = Math.max(0, Math.floor((nextHour.getTime() - date.getTime()) / 1000));
    const targetHourStr = (currentHour + 1 === ALLOWED_END_HOUR)
      ? '21:00 (Lock time)' 
      : `${String(currentHour + 1).padStart(2, '0')}:00`;

    return {
      isPermitted: true,
      secondsRemaining,
      targetTimeStr: targetHourStr,
      targetTimestamp: nextHour.getTime(),
      reason: 'hourly_reset'
    };
  }
}

// Robust Domain matching helper
function isBlockedUrl(urlStr, blockedList) {
  if (!urlStr || typeof urlStr !== 'string') return false;
  
  if (
    urlStr.startsWith('chrome://') ||
    urlStr.startsWith('chrome-extension://') ||
    urlStr.startsWith('moz-extension://') ||
    urlStr.startsWith('about:') ||
    urlStr.startsWith('edge://') ||
    urlStr.startsWith('view-source:')
  ) {
    return false;
  }

  try {
    const url = new URL(urlStr);
    const hostname = url.hostname.toLowerCase();
    const list = blockedList || DEFAULT_BLOCKED_DOMAINS;

    return list.some(domain => {
      const cleanDomain = domain.trim().toLowerCase().replace(/^www\./, '');
      if (!cleanDomain) return false;
      return (
        hostname === cleanDomain ||
        hostname === 'www.' + cleanDomain ||
        hostname.endsWith('.' + cleanDomain)
      );
    });
  } catch (e) {
    return false;
  }
}

// Facebook Reels URL checker
function isFacebookReelsUrl(urlStr) {
  if (!urlStr || typeof urlStr !== 'string') return false;
  try {
    const url = new URL(urlStr);
    const host = url.hostname.toLowerCase();
    if (host.includes('facebook.com') || host.includes('fb.com')) {
      return (
        url.pathname.startsWith('/reel') ||
        url.pathname.startsWith('/reels') ||
        url.pathname.includes('/watch/reels')
      );
    }
  } catch (e) {}
  return false;
}

// Storage helpers
async function getState() {
  return new Promise((resolve) => {
    chrome.storage.local.get({
      quotaSeconds: DEFAULT_QUOTA_SECONDS,
      blockedDomains: DEFAULT_BLOCKED_DOMAINS,
      hourlyUsage: {},
      hourlyReflections: {},
      reflectionHistory: [],
      showPill: true,
      autoRedirectOnReset: true,
      soundAlert: true
    }, resolve);
  });
}

async function saveState(data) {
  return new Promise((resolve) => {
    chrome.storage.local.set(data, resolve);
  });
}

// Update extension badge
function updateBadge(remainingSeconds, isPermitted) {
  try {
    if (!isPermitted) {
      chrome.action.setBadgeText({ text: 'OFF' });
      chrome.action.setBadgeBackgroundColor({ color: '#64748b' }); // Slate gray
    } else if (remainingSeconds <= 0) {
      chrome.action.setBadgeText({ text: '0m' });
      chrome.action.setBadgeBackgroundColor({ color: '#dc2626' }); // Red
    } else if (remainingSeconds <= 60) {
      chrome.action.setBadgeText({ text: `${remainingSeconds}s` });
      chrome.action.setBadgeBackgroundColor({ color: '#ef4444' }); // Alert red
    } else {
      const mins = Math.ceil(remainingSeconds / 60);
      chrome.action.setBadgeText({ text: `${mins}m` });
      chrome.action.setBadgeBackgroundColor({
        color: mins <= 2 ? '#f59e0b' : '#10b981' // Amber or Green
      });
    }
  } catch (err) {}
}

// Redirect all matching tabs to blocked.html
async function blockAllMatchingTabs(blockedDomains) {
  try {
    const tabs = await chrome.tabs.query({});
    for (const tab of tabs) {
      if (tab.url && isBlockedUrl(tab.url, blockedDomains)) {
        const blockedPageUrl = chrome.runtime.getURL(`blocked.html?url=${encodeURIComponent(tab.url)}`);
        chrome.tabs.update(tab.id, { url: blockedPageUrl }).catch(() => {});
      }
    }
  } catch (e) {}
}

// Check single tab and redirect if outside allowed window, reflection needed, or quota exhausted
async function checkAndBlockTab(tabId, url) {
  if (!url || typeof url !== 'string') return;

  // Intercept Facebook Reels directly
  if (isFacebookReelsUrl(url)) {
    chrome.tabs.update(tabId, { url: 'https://www.facebook.com/' }).catch(() => {});
    return;
  }

  const state = await getState();
  if (isBlockedUrl(url, state.blockedDomains)) {
    // 1. Outside 1PM - 9PM: completely blocked
    if (!isWithinPermittedHours()) {
      const blockedPageUrl = chrome.runtime.getURL(`blocked.html?url=${encodeURIComponent(url)}`);
      chrome.tabs.update(tabId, { url: blockedPageUrl }).catch(() => {});
      return;
    }

    const hourKey = getCurrentHourKey();
    const used = (state.hourlyUsage && state.hourlyUsage[hourKey]) || 0;
    const remaining = (state.quotaSeconds || DEFAULT_QUOTA_SECONDS) - used;

    // 2. Quota is exhausted for this hour
    if (remaining <= 0) {
      const blockedPageUrl = chrome.runtime.getURL(`blocked.html?url=${encodeURIComponent(url)}`);
      chrome.tabs.update(tabId, { url: blockedPageUrl }).catch(() => {});
      return;
    }

    // 3. Check if user has answered the 30-min reflection for this hour
    const reflections = state.hourlyReflections || {};
    const hasReflected = Boolean(reflections[hourKey]);

    if (!hasReflected) {
      const reflectionPageUrl = chrome.runtime.getURL(`reflection.html?url=${encodeURIComponent(url)}`);
      chrome.tabs.update(tabId, { url: reflectionPageUrl }).catch(() => {});
    }
  }
}

// Handle Heartbeat Tick from active content script
async function handleHeartbeatTick(url, senderTab) {
  const state = await getState();
  const isPermitted = isWithinPermittedHours();
  const hourKey = getCurrentHourKey();
  const unlockInfo = getNextUnlockInfo();

  const isTarget = isBlockedUrl(url, state.blockedDomains);
  if (!isTarget) {
    return { remaining: 0, quota: state.quotaSeconds, hourKey, showPill: false, blocked: false, isPermitted };
  }

  // Outside 1PM - 9PM: Block immediately
  if (!isPermitted) {
    updateBadge(0, false);
    blockAllMatchingTabs(state.blockedDomains);
    return {
      remaining: 0,
      quota: state.quotaSeconds,
      hourKey,
      showPill: state.showPill,
      blocked: true,
      isPermitted: false,
      unlockInfo
    };
  }

  // Check if reflection is needed for this hour
  const reflections = state.hourlyReflections || {};
  const hasReflected = Boolean(reflections[hourKey]);

  if (!hasReflected) {
    return {
      remaining: 0,
      quota: state.quotaSeconds,
      hourKey,
      showPill: false,
      blocked: true,
      needsReflection: true,
      reflectionUrl: chrome.runtime.getURL(`reflection.html?url=${encodeURIComponent(url)}`)
    };
  }

  const hourlyUsage = state.hourlyUsage || {};
  const currentUsed = hourlyUsage[hourKey] || 0;
  const quota = state.quotaSeconds || DEFAULT_QUOTA_SECONDS;
  const remaining = Math.max(0, quota - currentUsed);

  if (remaining > 0) {
    const newUsed = currentUsed + 1;
    hourlyUsage[hourKey] = newUsed;
    await saveState({ hourlyUsage });

    const newRemaining = Math.max(0, quota - newUsed);
    updateBadge(newRemaining, true);

    if (newRemaining <= 0) {
      blockAllMatchingTabs(state.blockedDomains);
      return {
        remaining: 0,
        quota,
        hourKey,
        showPill: state.showPill,
        blocked: true,
        isPermitted: true,
        unlockInfo
      };
    }

    return {
      remaining: newRemaining,
      quota,
      hourKey,
      showPill: state.showPill,
      blocked: false,
      isPermitted: true,
      unlockInfo
    };
  } else {
    updateBadge(0, true);
    blockAllMatchingTabs(state.blockedDomains);
    return {
      remaining: 0,
      quota,
      hourKey,
      showPill: state.showPill,
      blocked: true,
      isPermitted: true,
      unlockInfo
    };
  }
}

// Initialize / schedule alarms for reset
function scheduleResetAlarms() {
  chrome.alarms.clearAll(() => {
    chrome.alarms.create('watchdog', { periodInMinutes: 1 });

    const now = new Date();
    const nextHour = new Date(
      now.getFullYear(),
      now.getMonth(),
      now.getDate(),
      now.getHours() + 1,
      0,
      0,
      50
    );
    chrome.alarms.create('hourReset', { when: nextHour.getTime() });
  });
}

// Handle alarms
chrome.alarms.onAlarm.addListener(async (alarm) => {
  const state = await getState();
  const isPermitted = isWithinPermittedHours();
  const hourKey = getCurrentHourKey();
  const currentUsed = (state.hourlyUsage && state.hourlyUsage[hourKey]) || 0;
  const remaining = isPermitted
    ? Math.max(0, (state.quotaSeconds || DEFAULT_QUOTA_SECONDS) - currentUsed)
    : 0;

  updateBadge(remaining, isPermitted);

  if (alarm.name === 'hourReset') {
    chrome.runtime.sendMessage({
      type: 'HOUR_RESET',
      hourKey: hourKey,
      remaining: remaining,
      isPermitted: isPermitted
    }).catch(() => {});

    scheduleResetAlarms();
  }
});

// Tab navigation and update listeners
chrome.tabs.onActivated.addListener(async (activeInfo) => {
  try {
    const tab = await chrome.tabs.get(activeInfo.tabId);
    if (tab && tab.url) {
      checkAndBlockTab(tab.id, tab.url);
    }
  } catch (e) {}
});

chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
  if (changeInfo.url) {
    checkAndBlockTab(tabId, changeInfo.url);
  }
});

// WebNavigation listeners (intercept navigations & Single Page App history transitions)
if (chrome.webNavigation) {
  if (chrome.webNavigation.onBeforeNavigate) {
    chrome.webNavigation.onBeforeNavigate.addListener((details) => {
      if (details.frameId === 0) {
        checkAndBlockTab(details.tabId, details.url);
      }
    });
  }
  if (chrome.webNavigation.onHistoryStateUpdated) {
    chrome.webNavigation.onHistoryStateUpdated.addListener((details) => {
      if (details.frameId === 0) {
        checkAndBlockTab(details.tabId, details.url);
      }
    });
  }
}

// Runtime messages from popup, blocked page, reflection page, and content scripts
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.type === 'CONTENT_HEARTBEAT_TICK') {
    handleHeartbeatTick(message.url, sender.tab).then(sendResponse);
    return true;
  }

  if (message.type === 'SUBMIT_REFLECTION') {
    getState().then(state => {
      const hourKey = getCurrentHourKey();
      const hourlyReflections = state.hourlyReflections || {};
      const reflectionHistory = state.reflectionHistory || [];

      const record = {
        hourKey,
        answer: message.answer || '',
        timestamp: Date.now(),
        url: message.url || ''
      };

      hourlyReflections[hourKey] = record;
      reflectionHistory.unshift(record);

      saveState({ hourlyReflections, reflectionHistory }).then(() => {
        sendResponse({ success: true, hourKey });
      });
    });
    return true;
  }

  if (message.type === 'GET_STATUS') {
    getState().then((state) => {
      const isPermitted = isWithinPermittedHours();
      const hourKey = getCurrentHourKey();
      const used = (state.hourlyUsage && state.hourlyUsage[hourKey]) || 0;
      const quota = state.quotaSeconds || DEFAULT_QUOTA_SECONDS;
      const remaining = isPermitted ? Math.max(0, quota - used) : 0;
      const unlockInfo = getNextUnlockInfo();
      const hasReflected = Boolean(state.hourlyReflections && state.hourlyReflections[hourKey]);

      sendResponse({
        remaining,
        quota,
        used,
        hourKey,
        isPermitted,
        hasReflected,
        unlockInfo,
        secondsUntilReset: unlockInfo.secondsRemaining,
        blockedDomains: state.blockedDomains || DEFAULT_BLOCKED_DOMAINS,
        reflectionHistory: state.reflectionHistory || [],
        showPill: state.showPill !== false,
        autoRedirectOnReset: state.autoRedirectOnReset !== false,
        hourlyUsage: state.hourlyUsage || {}
      });
    });
    return true;
  }

  if (message.type === 'UPDATE_SETTINGS') {
    saveState(message.settings).then(() => {
      sendResponse({ success: true });
      getState().then(s => {
        const isPermitted = isWithinPermittedHours();
        const hourKey = getCurrentHourKey();
        const used = (s.hourlyUsage && s.hourlyUsage[hourKey]) || 0;
        const remaining = isPermitted ? Math.max(0, (s.quotaSeconds || DEFAULT_QUOTA_SECONDS) - used) : 0;
        updateBadge(remaining, isPermitted);
      });
    });
    return true;
  }

  if (message.type === 'RESET_USAGE_DEBUG') {
    getState().then(state => {
      const hourKey = getCurrentHourKey();
      const usage = state.hourlyUsage || {};
      const reflections = state.hourlyReflections || {};
      usage[hourKey] = 0;
      reflections[hourKey] = { answer: 'Debug test unlock', timestamp: Date.now() };

      saveState({ hourlyUsage: usage, hourlyReflections: reflections }).then(() => {
        const isPermitted = isWithinPermittedHours();
        updateBadge(isPermitted ? (state.quotaSeconds || DEFAULT_QUOTA_SECONDS) : 0, isPermitted);
        sendResponse({ success: true, remaining: state.quotaSeconds || DEFAULT_QUOTA_SECONDS });
      });
    });
    return true;
  }
});

// Startup & Initialization
async function initialize() {
  scheduleResetAlarms();

  const state = await getState();
  const isPermitted = isWithinPermittedHours();
  const hourKey = getCurrentHourKey();
  const used = (state.hourlyUsage && state.hourlyUsage[hourKey]) || 0;
  const remaining = isPermitted ? Math.max(0, (state.quotaSeconds || DEFAULT_QUOTA_SECONDS) - used) : 0;
  updateBadge(remaining, isPermitted);
}

chrome.runtime.onInstalled.addListener(initialize);
chrome.runtime.onStartup.addListener(initialize);
initialize();
