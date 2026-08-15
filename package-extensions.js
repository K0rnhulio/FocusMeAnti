const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

console.log('--- Packaging FocusMe Extensions ---');

const rootDir = __dirname;
const chromeDir = path.join(rootDir, 'chrome');
const firefoxDir = path.join(rootDir, 'firefox-zen');

const chromeZip = path.join(rootDir, 'focusme-chrome.zip');
const firefoxZip = path.join(rootDir, 'focusme-firefox.zip');

if (fs.existsSync(chromeZip)) fs.unlinkSync(chromeZip);
if (fs.existsSync(firefoxZip)) fs.unlinkSync(firefoxZip);

try {
  console.log('Packaging Chrome extension...');
  execSync(`powershell -Command "Compress-Archive -Path '${chromeDir}\\*' -DestinationPath '${chromeZip}' -Force"`);
  console.log(`✓ Created: ${chromeZip} (${fs.statSync(chromeZip).size} bytes)`);

  console.log('Packaging Firefox / Zen extension (AMO Ready)...');
  execSync(`powershell -Command "Compress-Archive -Path '${firefoxDir}\\*' -DestinationPath '${firefoxZip}' -Force"`);
  console.log(`✓ Created: ${firefoxZip} (${fs.statSync(firefoxZip).size} bytes)`);

  console.log('\n🎉 Both zip files are ready for upload!');
} catch (err) {
  console.error('Error packaging:', err.message);
  process.exit(1);
}
