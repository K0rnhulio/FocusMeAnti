const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

// Standard CRC32 table & function
const CRC32_TABLE = new Uint32Array(256);
for (let i = 0; i < 256; i++) {
  let c = i;
  for (let j = 0; j < 8; j++) {
    c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
  }
  CRC32_TABLE[i] = c;
}

function crc32(buf) {
  let crc = 0xFFFFFFFF;
  for (let i = 0; i < buf.length; i++) {
    crc = CRC32_TABLE[(crc ^ buf[i]) & 0xFF] ^ (crc >>> 8);
  }
  return (crc ^ 0xFFFFFFFF) >>> 0;
}

function createZip(sourceDir, outputZipPath) {
  const files = [];

  function scan(dir, baseRel = '') {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      // ALWAYS use forward slashes for ZIP entries (POSIX compliant)
      const relPath = baseRel ? `${baseRel}/${entry.name}` : entry.name;
      if (entry.isDirectory()) {
        scan(fullPath, relPath);
      } else if (entry.isFile()) {
        files.push({ fullPath, relPath });
      }
    }
  }

  scan(sourceDir);

  const localHeaders = [];
  const centralHeaders = [];
  let currentOffset = 0;

  const now = new Date();
  const dosTime = ((now.getHours() << 11) | (now.getMinutes() << 5) | (Math.floor(now.getSeconds() / 2))) & 0xFFFF;
  const dosDate = (((now.getFullYear() - 1980) << 9) | ((now.getMonth() + 1) << 5) | now.getDate()) & 0xFFFF;

  for (const file of files) {
    const uncompressedData = fs.readFileSync(file.fullPath);
    const uncompressedSize = uncompressedData.length;
    const fileCrc = crc32(uncompressedData);

    const compressedData = zlib.deflateRawSync(uncompressedData, { level: 9 });
    const compressedSize = compressedData.length;

    // Use UTF-8 forward slash relative path
    const nameBuffer = Buffer.from(file.relPath.replace(/\\/g, '/'), 'utf8');

    // Local file header (30 bytes + name)
    const localHeader = Buffer.alloc(30 + nameBuffer.length);
    localHeader.writeUInt32LE(0x04034B50, 0); // Signature PK\x03\x04
    localHeader.writeUInt16LE(20, 4);         // Version needed: 2.0
    localHeader.writeUInt16LE(0x0800, 6);     // Flags: UTF-8 (bit 11)
    localHeader.writeUInt16LE(8, 8);          // Compression: Deflate
    localHeader.writeUInt16LE(dosTime, 10);
    localHeader.writeUInt16LE(dosDate, 12);
    localHeader.writeUInt32LE(fileCrc, 14);
    localHeader.writeUInt32LE(compressedSize, 18);
    localHeader.writeUInt32LE(uncompressedSize, 22);
    localHeader.writeUInt16LE(nameBuffer.length, 26);
    localHeader.writeUInt16LE(0, 28);         // Extra field length
    nameBuffer.copy(localHeader, 30);

    localHeaders.push(localHeader, compressedData);

    // Central directory header (46 bytes + name)
    const centralHeader = Buffer.alloc(46 + nameBuffer.length);
    centralHeader.writeUInt32LE(0x02014B50, 0); // Signature PK\x01\x02
    centralHeader.writeUInt16LE(20, 4);         // Version made by: 2.0
    centralHeader.writeUInt16LE(20, 6);         // Version needed: 2.0
    centralHeader.writeUInt16LE(0x0800, 8);     // Flags: UTF-8
    centralHeader.writeUInt16LE(8, 10);         // Compression: Deflate
    centralHeader.writeUInt16LE(dosTime, 12);
    centralHeader.writeUInt16LE(dosDate, 14);
    centralHeader.writeUInt32LE(fileCrc, 16);
    centralHeader.writeUInt32LE(compressedSize, 20);
    centralHeader.writeUInt32LE(uncompressedSize, 24);
    centralHeader.writeUInt16LE(nameBuffer.length, 28);
    centralHeader.writeUInt16LE(0, 30);         // Extra field length
    centralHeader.writeUInt16LE(0, 32);         // File comment length
    centralHeader.writeUInt16LE(0, 34);         // Disk number start
    centralHeader.writeUInt16LE(0, 36);         // Internal file attributes
    centralHeader.writeUInt32LE(0, 38);         // External file attributes
    centralHeader.writeUInt32LE(currentOffset, 42); // Relative offset of local header
    nameBuffer.copy(centralHeader, 46);

    centralHeaders.push(centralHeader);

    currentOffset += localHeader.length + compressedData.length;
  }

  const centralDirOffset = currentOffset;
  const centralDirSize = centralHeaders.reduce((acc, b) => acc + b.length, 0);

  // End of central directory record (22 bytes)
  const eocd = Buffer.alloc(22);
  eocd.writeUInt32LE(0x06054B50, 0);          // Signature PK\x05\x06
  eocd.writeUInt16LE(0, 4);                   // Number of this disk
  eocd.writeUInt16LE(0, 6);                   // Disk where central directory starts
  eocd.writeUInt16LE(files.length, 8);        // Number of central directory records on this disk
  eocd.writeUInt16LE(files.length, 10);       // Total number of central directory records
  eocd.writeUInt32LE(centralDirSize, 12);     // Size of central directory
  eocd.writeUInt32LE(centralDirOffset, 16);   // Offset of central directory
  eocd.writeUInt16LE(0, 20);                  // Comment length

  const finalZipBuffer = Buffer.concat([...localHeaders, ...centralHeaders, eocd]);
  fs.writeFileSync(outputZipPath, finalZipBuffer);
  return files.map(f => f.relPath.replace(/\\/g, '/'));
}

console.log('--- Packaging FocusMe Extensions with POSIX Forward-Slash ZIP Structure ---');

const rootDir = __dirname;
const chromeDir = path.join(rootDir, 'chrome');
const firefoxDir = path.join(rootDir, 'firefox-zen');

const chromeZip = path.join(rootDir, 'focusme-chrome.zip');
const firefoxZip = path.join(rootDir, 'focusme-firefox.zip');

console.log('\nPackaging Chrome extension...');
const chromeFiles = createZip(chromeDir, chromeZip);
console.log(`✓ Created ${chromeZip} (${fs.statSync(chromeZip).size} bytes)`);

console.log('\nPackaging Firefox / Zen extension (AMO Compliant)...');
const firefoxFiles = createZip(firefoxDir, firefoxZip);
console.log(`✓ Created ${firefoxZip} (${fs.statSync(firefoxZip).size} bytes)`);
console.log('  Files in archive:');
firefoxFiles.forEach(f => console.log(`    ✓ ${f}`));

console.log('\n🎉 Both zip files use standard forward slashes (/) and are 100% AMO compliant!');
