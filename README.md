# 🛡️ FocusMe - Distraction Blocker & Hourly Limiter

[![Manifest V3](https://img.shields.io/badge/Manifest-V3-blue.svg)](https://developer.chrome.com/docs/extensions/mv3/intro/)
[![Chrome](https://img.shields.io/badge/Browser-Chrome-green.svg)](https://google.com/chrome)
[![Zen Browser](https://img.shields.io/badge/Browser-Zen%20%2F%20Firefox-orange.svg)](https://zen-browser.app/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A privacy-first, productivity-enforcing browser extension for **Google Chrome** and **Zen Browser (Firefox)** that strictly regulates time spent on **Twitter/X, Reddit, and Facebook** through mindful check-ins, hourly quotas, and scheduled focus windows.

---

## ⚡ Core Rules & Features

| Feature | Behavior |
|---|---|
| ⏳ **5 Min / Hour Combined** | Limits total time on Twitter/X, Reddit, and Facebook to **5 minutes per clock hour** combined across all sites. |
| 🚫 **Zero Rollover** | Unused time from one hour does **not** carry over into the next. |
| ⏰ **Active Daily Window** | Access is only permitted between **1:00 PM and 9:00 PM (13:00 – 21:00)**. Outside of these hours, distracting sites are **100% locked**. |
| 🧠 **30-Min Mindful Reflection** | Before opening distracting sites in any allowed hour, you must answer: *"What have you accomplished in the last 30 minutes?"* (minimum 15 characters). |
| 🎬 **Facebook Reels Blocker** | Completely strips out the Reels tab, "Reels and short videos" feed carousels, and redirects direct `/reel` links back to the home feed. |
| ⏱️ **Live In-Page Pill** | Floating timer in the corner of blocked sites showing real-time remaining seconds with warning and urgency indicators. |
| 📊 **Dashboard & Journal** | Popup with visual circular countdown, customizable blocked domains, today's 24-hour activity bar chart, and check-in history log. |

---

## 📁 Repository Structure

```
FocusMeAnti/
├── chrome/                          # Ready to load in Google Chrome & Chromium browsers
│   ├── manifest.json                # Chrome Manifest V3 (service_worker)
│   ├── background.js                # Core tracking & navigation engine
│   ├── content.js & content.css     # In-page floating pill & Reels remover
│   ├── blocked.html, .js, .css      # Ambient countdown & schedule screen
│   ├── reflection.html, .js, .css   # Mindful 30-min check-in screen
│   ├── popup.html, .js, .css        # Circular timer dashboard & stats log
│   └── icons/ (16, 32, 48, 128px)
│
├── firefox-zen/                     # Ready to load in Zen Browser & Firefox
│   ├── manifest.json                # Firefox/Zen Manifest V3 (scripts + Gecko ID)
│   ├── background.js
│   ├── content.js & content.css
│   ├── blocked.html, .js, .css
│   ├── reflection.html, .js, .css
│   ├── popup.html, .js, .css
│   └── icons/ (16, 32, 48, 128px)
│
├── .gitignore
├── verify-extensions.js             # Automated validation test script
└── README.md
```

---

## 🚀 Installation Guide

### 1. Google Chrome / Chromium (Brave, Edge, Arc)
1. Open Google Chrome and enter `chrome://extensions` in the address bar.
2. Turn on **Developer mode** (toggle in the top-right corner).
3. Click **Load unpacked** (top-left button).
4. Select the `chrome` folder from this repository.
5. Pin **FocusMe** to your toolbar!

---

### 2. Zen Browser (or Firefox)
1. Open Zen Browser and navigate to `about:debugging#/runtime/this-firefox`.
2. Click **Load Temporary Add-on...**.
3. Select `manifest.json` inside the `firefox-zen` folder.
4. The extension is immediately active in Zen Browser!

---

## 🛠️ Verification & Testing

To run the automated validation test suite:

```bash
node verify-extensions.js
```

---

## 📄 License

MIT License • Built for high-performance deep work and intentional focus.
