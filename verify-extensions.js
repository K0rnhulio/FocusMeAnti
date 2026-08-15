const fs = require('fs');
const path = require('path');

console.log('--- Verifying Extension Packages ---');

function verifyExtension(dirName, manifestType) {
  console.log(`\nChecking [${dirName}]...`);
  const base = path.join(__dirname, dirName);
  
  const requiredFiles = [
    'manifest.json',
    'background.js',
    'content.js',
    'content.css',
    'blocked.html',
    'blocked.js',
    'blocked.css',
    'reflection.html',
    'reflection.js',
    'reflection.css',
    'popup.html',
    'popup.js',
    'popup.css',
    'icons/icon16.png',
    'icons/icon32.png',
    'icons/icon48.png',
    'icons/icon128.png'
  ];

  for (const f of requiredFiles) {
    const fullPath = path.join(base, f);
    if (!fs.existsSync(fullPath)) {
      throw new Error(`Missing file: ${fullPath}`);
    }
    const stat = fs.statSync(fullPath);
    if (stat.size === 0) {
      throw new Error(`File is empty: ${fullPath}`);
    }
    console.log(`  ✓ ${f} (${stat.size} bytes)`);
  }

  const manifestContent = JSON.parse(fs.readFileSync(path.join(base, 'manifest.json'), 'utf8'));
  if (manifestContent.manifest_version !== 3) {
    throw new Error(`Invalid manifest_version in ${dirName}`);
  }
  
  if (manifestType === 'chrome') {
    if (!manifestContent.background || !manifestContent.background.service_worker) {
      throw new Error(`Chrome manifest must have background.service_worker`);
    }
  } else if (manifestType === 'firefox') {
    if (!manifestContent.background || !manifestContent.background.scripts) {
      throw new Error(`Firefox/Zen manifest must have background.scripts`);
    }
    if (!manifestContent.browser_specific_settings || !manifestContent.browser_specific_settings.gecko) {
      throw new Error(`Firefox/Zen manifest must have browser_specific_settings.gecko`);
    }
  }

  console.log(`  ✓ manifest.json is valid for ${manifestType}`);

  const jsFiles = ['background.js', 'content.js', 'blocked.js', 'reflection.js', 'popup.js'];
  for (const js of jsFiles) {
    const code = fs.readFileSync(path.join(base, js), 'utf8');
    new Function(code);
    console.log(`  ✓ ${js} syntax valid`);
  }
}

try {
  verifyExtension('chrome', 'chrome');
  verifyExtension('firefox-zen', 'firefox');
  console.log('\n✅ All checks passed successfully!');
} catch (err) {
  console.error('\n❌ Verification failed:', err);
  process.exit(1);
}
