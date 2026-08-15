/**
 * FocusMe - In-Page Live Time Pill & Content Blocker (including Facebook Reels)
 * Ensures persistent, reliable 1-second ticks and reflection checks
 */

(function () {
  // =========================================================================
  // 1. Facebook Reels Interception & Cleaner
  // =========================================================================
  const isFacebook = window.location.hostname.includes('facebook.com') || window.location.hostname.includes('fb.com');

  function isReelsUrl(urlStr) {
    try {
      const u = new URL(urlStr || window.location.href);
      return u.pathname.startsWith('/reel') || u.pathname.startsWith('/reels') || u.pathname.includes('/watch/reels');
    } catch (e) {
      return false;
    }
  }

  function blockFacebookReels() {
    if (!isFacebook) return;

    // Direct navigation check
    if (isReelsUrl(window.location.href)) {
      window.location.replace('https://www.facebook.com/');
      return;
    }

    // Remove Reels DOM elements
    const reelsSelectors = [
      'a[href*="/reel/"]',
      'a[href*="/reels/"]',
      'a[aria-label="Reels"]',
      'div[aria-label="Reels"]',
      'div[data-pagelet*="Reel"]',
      'div[data-pagelet*="reel"]',
      'div[aria-label*="Reels and short videos" i]'
    ];

    document.querySelectorAll(reelsSelectors.join(',')).forEach(el => {
      const feedCard = el.closest('div[role="feed"] > div') || el.closest('div[data-pagelet]') || el;
      if (feedCard) {
        feedCard.style.setProperty('display', 'none', 'important');
      } else {
        el.style.setProperty('display', 'none', 'important');
      }
    });

    // Also look for headers with text "Reels"
    document.querySelectorAll('h2, h3, span, div').forEach(el => {
      if (el.children.length === 0 && (el.textContent.trim() === 'Reels and short videos' || el.textContent.trim() === 'Reels')) {
        const feedCard = el.closest('div[role="feed"] > div') || el.closest('div[data-pagelet]');
        if (feedCard) {
          feedCard.style.setProperty('display', 'none', 'important');
        }
      }
    });
  }

  if (isFacebook) {
    const originalPushState = history.pushState;
    history.pushState = function () {
      originalPushState.apply(this, arguments);
      if (isReelsUrl(window.location.href)) {
        window.location.replace('https://www.facebook.com/');
      }
    };

    const originalReplaceState = history.replaceState;
    history.replaceState = function () {
      originalReplaceState.apply(this, arguments);
      if (isReelsUrl(window.location.href)) {
        window.location.replace('https://www.facebook.com/');
      }
    };

    window.addEventListener('popstate', () => {
      if (isReelsUrl(window.location.href)) {
        window.location.replace('https://www.facebook.com/');
      }
    });

    blockFacebookReels();
    const observer = new MutationObserver(blockFacebookReels);
    observer.observe(document.documentElement, { childList: true, subtree: true });
  }

  // =========================================================================
  // 2. Floating Time Pill & Heartbeat Engine
  // =========================================================================
  if (window.__focusMePillInjected) return;
  window.__focusMePillInjected = true;

  let pillElement = null;
  let timeTextElement = null;
  let statusTextElement = null;
  let isMinimized = false;
  let heartbeatInterval = null;

  function formatTime(seconds) {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  }

  function createPill() {
    if (document.getElementById('focusme-floating-pill')) return;

    const host = document.createElement('div');
    host.id = 'focusme-floating-pill';
    host.className = 'focusme-pill-container';

    host.innerHTML = `
      <div class="focusme-pill-content">
        <div class="focusme-pill-icon">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10"></circle>
            <polyline points="12 6 12 12 16 14"></polyline>
          </svg>
        </div>
        <div class="focusme-pill-body">
          <span class="focusme-pill-time">--:--</span>
          <span class="focusme-pill-label">left this hr</span>
        </div>
        <button class="focusme-pill-toggle" title="Minimize" aria-label="Minimize timer">
          <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
        </button>
      </div>
    `;

    document.body.appendChild(host);

    pillElement = host;
    timeTextElement = host.querySelector('.focusme-pill-time');
    statusTextElement = host.querySelector('.focusme-pill-label');

    const toggleBtn = host.querySelector('.focusme-pill-toggle');
    toggleBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      isMinimized = !isMinimized;
      if (isMinimized) {
        pillElement.classList.add('focusme-minimized');
      } else {
        pillElement.classList.remove('focusme-minimized');
      }
    });

    host.addEventListener('click', () => {
      if (isMinimized) {
        isMinimized = false;
        pillElement.classList.remove('focusme-minimized');
      }
    });
  }

  function updatePill(remaining, quota, showPill) {
    if (!showPill) {
      if (pillElement) pillElement.style.display = 'none';
      return;
    }

    if (!pillElement) {
      createPill();
    }
    if (!pillElement) return;

    pillElement.style.display = 'flex';
    timeTextElement.textContent = formatTime(remaining);

    // Apply urgency classes
    pillElement.classList.remove('focusme-warn', 'focusme-critical');
    if (remaining <= 15) {
      pillElement.classList.add('focusme-critical');
      statusTextElement.textContent = 'closing soon!';
    } else if (remaining <= 60) {
      pillElement.classList.add('focusme-warn');
      statusTextElement.textContent = 'final minute';
    } else {
      statusTextElement.textContent = 'left this hr';
    }
  }

  function redirectToBlocked() {
    try {
      const blockedPageUrl = chrome.runtime.getURL('blocked.html?url=' + encodeURIComponent(window.location.href));
      window.location.href = blockedPageUrl;
    } catch (e) {}
  }

  // Active Tab Heartbeat function
  function sendActiveHeartbeat() {
    if (document.visibilityState !== 'visible') {
      return;
    }

    try {
      chrome.runtime.sendMessage({
        type: 'CONTENT_HEARTBEAT_TICK',
        url: window.location.href
      }, (res) => {
        if (chrome.runtime.lastError || !res) return;

        if (res.needsReflection && res.reflectionUrl) {
          window.location.href = res.reflectionUrl;
        } else if (res.blocked || res.remaining <= 0) {
          redirectToBlocked();
        } else {
          updatePill(res.remaining, res.quota, res.showPill !== false);
        }
      });
    } catch (e) {}
  }

  // Start 1-second active heartbeat
  if (heartbeatInterval) clearInterval(heartbeatInterval);
  heartbeatInterval = setInterval(sendActiveHeartbeat, 1000);

  // Resume heartbeat immediately when tab becomes visible or focused
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
      sendActiveHeartbeat();
      if (isFacebook) blockFacebookReels();
    }
  });
  window.addEventListener('focus', () => {
    sendActiveHeartbeat();
    if (isFacebook) blockFacebookReels();
  });

  // Initial check on load
  function init() {
    createPill();
    sendActiveHeartbeat();
    if (isFacebook) blockFacebookReels();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
