/**
 * FocusMe - Productivity Reflection Controller
 * Modern interaction handlers, quick tags, and keyboard shortcuts
 */

const MIN_CHARS = 15;

const PRODUCTIVITY_QUOTES = [
  "Focus is not about doing everything; it is about protecting your attention for what truly matters.",
  "You have power over your mind - not outside events. Realize this, and you will find strength. — Marcus Aurelius",
  "Deep work is the ability to focus without distraction on a cognitively demanding task. — Cal Newport",
  "The secret of getting ahead is getting started on the single most important task.",
  "Action produces information. Clarity comes from engagement, not endless scrolling."
];

function getQueryParams() {
  const params = new URLSearchParams(window.location.search);
  return {
    originalUrl: params.get('url') || ''
  };
}

document.addEventListener('DOMContentLoaded', () => {
  const textarea = document.getElementById('reflection-input');
  const textareaContainer = textarea.closest('.textarea-container');
  const charCountEl = document.getElementById('char-count');
  const charBadgeEl = document.getElementById('char-badge');
  const indicatorDot = document.getElementById('indicator-dot');
  const indicatorText = document.getElementById('indicator-text');
  const submitBtn = document.getElementById('submit-btn');
  const stayFocusedBtn = document.getElementById('stay-focused-btn');
  const quoteBody = document.getElementById('quote-body');
  const destinationBanner = document.getElementById('destination-banner');
  const destinationHost = document.getElementById('destination-host');
  const tagButtons = document.querySelectorAll('.tag-btn');

  const { originalUrl } = getQueryParams();

  // 1. Show destination host if available
  if (originalUrl) {
    try {
      const parsed = new URL(originalUrl);
      destinationHost.textContent = parsed.hostname;
      destinationBanner.style.display = 'flex';
    } catch (e) {
      destinationHost.textContent = originalUrl;
      destinationBanner.style.display = 'flex';
    }
  }

  // 2. Set random motivating quote
  const randomQuote = PRODUCTIVITY_QUOTES[Math.floor(Math.random() * PRODUCTIVITY_QUOTES.length)];
  quoteBody.textContent = randomQuote;

  // 3. Quick Tag Button Handlers
  tagButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      const prefix = btn.dataset.prefix;
      if (!textarea.value.startsWith(prefix)) {
        textarea.value = prefix + textarea.value;
      }
      textarea.focus();
      // Move cursor to end
      textarea.setSelectionRange(textarea.value.length, textarea.value.length);
      updateStatus();
    });
  });

  // 4. Real-time character count and visual indicators
  function updateStatus() {
    const text = textarea.value.trim();
    const count = text.length;
    charCountEl.textContent = count;

    if (count >= MIN_CHARS) {
      textareaContainer.classList.add('ready');
      indicatorDot.classList.add('ready');
      indicatorText.classList.add('ready');
      charBadgeEl.classList.add('ready');

      indicatorText.textContent = '✓ Great reflection! Ready to unlock';
      charBadgeEl.textContent = `✓ ${count} chars`;
      submitBtn.disabled = false;
    } else {
      textareaContainer.classList.remove('ready');
      indicatorDot.classList.remove('ready');
      indicatorText.classList.remove('ready');
      charBadgeEl.classList.remove('ready');

      const needed = MIN_CHARS - count;
      indicatorText.textContent = count === 0 
        ? 'Waiting for your reflection...' 
        : `Type ${needed} more character${needed === 1 ? '' : 's'}...`;
      charBadgeEl.innerHTML = `<span id="char-count">${count}</span> / 15 chars`;
      submitBtn.disabled = true;
    }
  }

  textarea.addEventListener('input', updateStatus);

  // 5. Submit Handler
  function handleSubmit() {
    const answer = textarea.value.trim();
    if (answer.length < MIN_CHARS) return;

    submitBtn.disabled = true;
    submitBtn.querySelector('span').textContent = 'Unlocking 5m Session...';

    chrome.runtime.sendMessage({
      type: 'SUBMIT_REFLECTION',
      answer: answer,
      url: originalUrl
    }, () => {
      const destination = originalUrl || 'https://x.com';
      window.location.href = destination;
    });
  }

  submitBtn.addEventListener('click', handleSubmit);

  // 6. Stay in Flow / Cancel Button
  function handleStayFocused() {
    window.location.href = 'https://www.google.com';
  }

  stayFocusedBtn.addEventListener('click', handleStayFocused);

  // 7. Global Keyboard Shortcuts: ESC to exit, Enter / Ctrl+Enter to submit
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      handleStayFocused();
    } else if ((e.metaKey || e.ctrlKey) && e.key === 'Enter') {
      if (!submitBtn.disabled) {
        handleSubmit();
      }
    }
  });
});
