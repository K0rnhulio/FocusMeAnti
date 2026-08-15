const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

// Function to create a clean PNG with rounded shield/hourglass focus design
function createFocusIconPNG(size) {
  const width = size;
  const height = size;

  // Uncompressed RGBA bitmap buffer with filter byte per scanline
  // Each scanline: 1 byte filter (0 = None) + width * 4 bytes (RGBA)
  const scanlineLength = 1 + width * 4;
  const rawData = Buffer.alloc(scanlineLength * height);

  const centerX = width / 2;
  const centerY = height / 2;
  const radius = width * 0.45;

  for (let y = 0; y < height; y++) {
    const rowOffset = y * scanlineLength;
    rawData[rowOffset] = 0; // Filter: None

    for (let x = 0; x < width; x++) {
      const pixelOffset = rowOffset + 1 + x * 4;
      
      const dx = x - centerX;
      const dy = y - centerY;
      const dist = Math.sqrt(dx * dx + dy * dy);

      // Background rounded circle with smooth antialiasing
      const circleAlpha = Math.max(0, Math.min(1, radius - dist + 0.5));

      if (circleAlpha > 0) {
        // Gradient from vibrant indigo (#6366f1) to deep violet (#4f46e5)
        const t = y / height;
        const rBg = Math.round(99 * (1 - t) + 79 * t);
        const gBg = Math.round(102 * (1 - t) + 70 * t);
        const bBg = Math.round(241 * (1 - t) + 229 * t);

        // Draw inner stopwatch / hourglass / timer symbol
        // Outer ring of timer
        const timerRadius = width * 0.28;
        const timerDist = Math.sqrt(dx * dx + (dy + width * 0.03) * (dy + width * 0.03));
        const ringThickness = Math.max(1.2, width * 0.06);
        const isRing = Math.abs(timerDist - timerRadius) < ringThickness;

        // Timer hand (pointing to 10 o'clock - 5 min mark)
        const angle = Math.atan2(dy + width * 0.03, dx);
        const handAngle = -Math.PI * 0.65; // ~10 o'clock
        const angleDiff = Math.abs((angle - handAngle + Math.PI * 3) % (Math.PI * 2) - Math.PI);
        const isHand = timerDist <= timerRadius && angleDiff < 0.18 && timerDist > width * 0.04;

        // Top button of stopwatch
        const isButton = Math.abs(dx) <= width * 0.07 && (dy + width * 0.35) >= -width * 0.06 && (dy + width * 0.35) <= 0;

        if (isRing || isHand || isButton) {
          // White symbol (#ffffff)
          rawData[pixelOffset] = 255;
          rawData[pixelOffset + 1] = 255;
          rawData[pixelOffset + 2] = 255;
          rawData[pixelOffset + 3] = Math.round(255 * circleAlpha);
        } else {
          // Background gradient
          rawData[pixelOffset] = rBg;
          rawData[pixelOffset + 1] = gBg;
          rawData[pixelOffset + 2] = bBg;
          rawData[pixelOffset + 3] = Math.round(255 * circleAlpha);
        }
      } else {
        // Transparent
        rawData[pixelOffset] = 0;
        rawData[pixelOffset + 1] = 0;
        rawData[pixelOffset + 2] = 0;
        rawData[pixelOffset + 3] = 0;
      }
    }
  }

  // Build PNG chunks
  const signature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);

  // IHDR chunk
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8; // Bit depth
  ihdr[9] = 6; // Color type: RGBA
  ihdr[10] = 0; // Compression method: Deflate
  ihdr[11] = 0; // Filter method: Standard
  ihdr[12] = 0; // Interlace: None
  const ihdrChunk = createChunk('IHDR', ihdr);

  // IDAT chunk (compressed data)
  const compressed = zlib.deflateSync(rawData);
  const idatChunk = createChunk('IDAT', compressed);

  // IEND chunk
  const iendChunk = createChunk('IEND', Buffer.alloc(0));

  return Buffer.concat([signature, ihdrChunk, idatChunk, iendChunk]);
}

function createChunk(type, data) {
  const length = data.length;
  const chunk = Buffer.alloc(8 + length + 4);
  chunk.writeUInt32BE(length, 0);
  chunk.write(type, 4);
  data.copy(chunk, 8);

  const crc = crc32(chunk.subarray(4, 8 + length));
  chunk.writeInt32BE(crc, 8 + length);
  return chunk;
}

// Standard CRC32 table
const crcTable = [];
for (let n = 0; n < 256; n++) {
  let c = n;
  for (let k = 0; k < 8; k++) {
    if (c & 1) c = 0xedb88320 ^ (c >>> 1);
    else c = c >>> 1;
  }
  crcTable[n] = c;
}

function crc32(buf) {
  let crc = -1;
  for (let i = 0; i < buf.length; i++) {
    crc = (crc >>> 8) ^ crcTable[(crc ^ buf[i]) & 0xff];
  }
  return crc ^ -1;
}

const targetDirs = [
  path.join(__dirname, 'chrome', 'icons'),
  path.join(__dirname, 'firefox-zen', 'icons')
];

targetDirs.forEach(dir => {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
  [16, 32, 48, 128].forEach(size => {
    const png = createFocusIconPNG(size);
    fs.writeFileSync(path.join(dir, `icon${size}.png`), png);
    console.log(`Generated ${path.join(dir, `icon${size}.png`)}`);
  });
});
