const DEFAULT_BLOCKED_DOMAINS = [
  'twitter.com',
  'x.com',
  'reddit.com',
  'facebook.com',
  'fb.com'
];

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

const testUrls = [
  // Twitter & X
  'https://x.com',
  'https://x.com/home',
  'https://www.x.com/explore',
  'https://twitter.com',
  'https://twitter.com/notifications',
  'https://mobile.twitter.com/messages',
  // Facebook
  'https://facebook.com',
  'https://www.facebook.com/',
  'https://web.facebook.com/watch',
  'https://m.facebook.com/feed',
  'https://touch.facebook.com',
  'https://fb.com',
  'https://www.fb.com',
  // Reddit
  'https://reddit.com',
  'https://www.reddit.com/r/popular',
  'https://old.reddit.com',
  'https://new.reddit.com',
  'https://sh.reddit.com',
  // Non-blocked
  'https://google.com',
  'https://github.com',
  'https://wikipedia.org'
];

console.log('Testing URL Matching:');
testUrls.forEach(url => {
  const blocked = isBlockedUrl(url);
  console.log(`  ${url.padEnd(42)} -> ${blocked ? 'BLOCKED ✅' : 'ALLOWED ⚪'}`);
});
