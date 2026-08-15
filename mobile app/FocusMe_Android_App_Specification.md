# 📱 FocusMe for Android — Complete Technical Specification & Architecture Document

> **Project Name:** FocusMe Discipline & Morning Unblocker  
> **Target Platform:** Android 8.0+ (API Level 26+)  
> **Language & Framework:** Kotlin, Jetpack Compose, CameraX, Google ML Kit, Android Accessibility Service, NotificationListenerService  
> **Version:** 1.1.0 (Reactive-Only Messaging & Nighttime Gate Specification)

---

## 1. Executive Summary & Core Philosophy

Smartphones are engineered around frictionless dopamine loops. Typical app blockers fail because unlocking an app only requires typing a password or waiting out a trivial timer—actions that require zero physical or cognitive exertion.

**FocusMe for Android** inverts this dynamic by introducing **Physical Friction, Cognitive Effort Gates, and Reactive-Only Communication**:
1. **The Morning Wake-up Protocol:** Forces physical bed-exit and neurological activation (Steps, G-Sensor Shakes, or Gyroscope Tilt Maze) before the phone unblocks in the morning.
2. **The Hourly Dopamine Toll Gate:** Enforces a strict **5 minutes per clock hour combined limit** (1:00 PM – 9:00 PM) on Twitter/X, Reddit, Facebook, Instagram, and TikTok with a mandatory 30-min productivity check-in.
3. **🌙 Reactive-Only Nighttime Messaging Gate (WhatsApp & Zalo):** After 9:00 PM, the phone is locked against proactive scrolling. You can only open WhatsApp or Zalo when a legitimate incoming message has arrived, granting a 2–3 minute focused reply window.

---

## 2. High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              SENSORS & HARDWARE                             │
│   ┌──────────────────┐  ┌──────────────────┐  ┌─────────────────────────┐   │
│   │ 3-Axis Accel /   │  │ Hardware Step    │  │ Front Camera Feed       │   │
│   │ Gyroscope Sensor │  │ Detector Sensor  │  │ (CameraX 30 FPS)        │   │
│   └─────────┬────────┘  └────────┬─────────┘  └────────────┬────────────┘   │
└─────────────┼────────────────────┼─────────────────────────┼────────────────┘
              │                    │                         │
              ▼                    ▼                         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CORE DETECTION ENGINES                            │
│  ┌───────────────────────┐ ┌──────────────────────┐ ┌────────────────────┐  │
│  │ Motion & Tilt Engine  │ │ Step Tracker Engine  │ │ Google ML Kit Pose │  │
│  │ (Shake, Tilt Physics) │ │ (Bed-Exit Detector)  │ │ (Push-Up Tracker)  │  │
│  └──────────┬────────────┘ └──────────┬───────────┘ └─────────┬──────────┘  │
└─────────────┼─────────────────────────┼───────────────────────┼─────────────┘
              │                         │                       │
              └────────────────────┬────┴───────────────────────┘
                                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          DISCIPLINE CONTROLLER SERVICE                      │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ AccessibilityService + UsageStatsManager + NotificationListener       │  │
│  │ • Monitors foreground app package names (Twitter, Reddit, FB, IG...)  │  │
│  │ • Tracks cumulative hourly usage bucket [YYYY-MM-DD-HH]               │  │
│  │ • Evaluates 1:00 PM – 9:00 PM schedule window                         │  │
│  │ • NotificationListener grants 3-min Reactive Pass for WhatsApp/Zalo   │  │
│  │ • Enforces 30-min reflection & physical toll completion               │  │
│  └───────────────────────────────────┬───────────────────────────────────┘  │
└──────────────────────────────────────┼──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         FULL-SCREEN OVERLAY ENGINE                          │
│                           (SYSTEM_ALERT_WINDOW)                             │
│  ┌────────────────────────┐ ┌────────────────────────┐ ┌─────────────────┐  │
│  │ 🐭 Gyroscope Maze Game │ │ 💪 Push-Up Counter UI  │ │ ✍️ 30-Min Prompt │  │
│  └────────────────────────┘ └────────────────────────┘ └─────────────────┘  │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ 🔒 5-Minute Floating Timer Pill + Locked Countdown Screen             │  │
│  │ 💬 Reactive Messaging Grace Period Banner (e.g., "2:45 to reply")     │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Detailed Feature Specifications

### 3.1. The Morning Wake-up Protocol (Bed-Exit Barrier)
Activates from midnight (00:00) until the user completes the morning protocol:
- **Challenge A — 50 G-Sensor Arm Shakes:**
  - Accelerometer threshold: $G > 1.8g$ with peak-to-trough interval validation (prevents gentle wiggling).
  - Circular reactive progress bar with haptic feedback on each valid rep.
- **Challenge B — 25-Step Bed-Exit Walk:**
  - Utilizes `Sensor.TYPE_STEP_DETECTOR` or real-time step acceleration analysis.
  - Automatically resets if steps stop for longer than 8 seconds.
- **Challenge C — 🐭 Gyroscope Tilt Maze:**
  - Procedurally generated 2D labyrinth (Recursive Backtracker algorithm).
  - Uses phone accelerometer/gyroscope as physical tilt gravity to roll the mouse through the maze to reach the cheese.
- **Challenge D — AI Camera Push-up / Squat Counter:**
  - Real-time on-device body landmark tracking using **Google ML Kit Pose Detection**.
  - Analyzes Shoulder-Elbow-Wrist angle ($< 90^\circ$ for down, $> 160^\circ$ for up) to verify 5 genuine push-ups.

---

### 3.2. Daytime Hourly Limit & Active Window
- **Active Window:** **1:00 PM – 9:00 PM (13:00 – 21:00)**.
- **Off-Hours Lockout:** Outside this window, target apps are immediately covered by the Locked Screen with a countdown to 1:00 PM.
- **Allowance:** Combined **5 minutes per clock hour** across all blocked apps.
- **Zero Rollover:** At `:00:00` of every hour, the timer resets to 5:00.

---

### 3.3. The 2-Step Social Media Toll Gate
When opening Twitter/X, Reddit, Facebook, Instagram, or TikTok during 1PM–9PM:
1. **Step 1: Mindful Reflection:**
   - Asks: *"What have you accomplished in the last 30 minutes?"*
   - Requires at least 15 characters.
   - Saves entry to the daily accountability log.
2. **Step 2: Physical Toll (Optional / Configurable):**
   - User does **5 camera-tracked push-ups** or **30 shakes** or **1 maze** to claim their 5 minutes.
3. **Step 3: Access Granted:**
   - 5-minute countdown starts with a floating in-app timer pill.
   - When 5 minutes expire, overlay immediately returns to the blocked screen.

---

### 3.4. 💬 Reactive-Only Nighttime Messaging Gate (WhatsApp & Zalo)

#### The Problem:
After 9:00 PM, you want the phone locked to protect your sleep, but you still need to be reachable for important messages from family, friends, or clients. If messaging apps are permanently open, it leads to late-night status browsing and channel scrolling.

#### The Solution (Reactive Messaging):
- **Proactive Opening Blocked:** After 9:00 PM, tapping WhatsApp (`com.whatsapp`) or Zalo (`com.zing.zalo`) out of boredom displays:  
  > 🔒 *"Night Lock Active — Messaging is in Reactive Mode. You can reply when a message arrives."*
- **Notification-Triggered Reply Pass (`NotificationListenerService`):**
  1. When an incoming message notification arrives from **WhatsApp** or **Zalo**:
     - The notification is posted to the lock screen with standard Android inline quick-reply.
     - A **3-Minute Reactive Reply Window** is automatically activated for that specific app.
  2. When the user taps the notification, WhatsApp or Zalo opens without being blocked.
  3. A subtle floating pill displays: `Reply Window: 2m 45s left`.
  4. Once the 3 minutes expire, the app smoothly re-locks to prevent lingering and status doomscrolling.
- **Emergency & Incoming Phone Calls:**
  - Phone calls (Cellular, WhatsApp Audio/Video Calls, Zalo Calls) are **100% unrestricted at all times**.

---

## 4. Android System APIs & Required Permissions

| Permission | Purpose |
|---|---|
| `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` | Detects incoming WhatsApp & Zalo messages to activate the 3-minute reactive reply window after 9 PM. |
| `android.permission.BIND_ACCESSIBILITY_SERVICE` | Real-time foreground app detection, Facebook Reels stripping, and URL interception in browsers. |
| `android.permission.SYSTEM_ALERT_WINDOW` | Drawing the full-screen reflection barrier, maze game, and floating countdown pill over other apps. |
| `android.permission.PACKAGE_USAGE_STATS` | Backing up hourly usage calculations and tracking app activity. |
| `android.permission.CAMERA` | Front camera feed for on-device ML Kit pose detection (Push-up/Squat counter). |
| `android.permission.BODY_SENSORS` / `HIGH_SAMPLING_RATE_SENSORS` | Access to 60Hz+ G-Sensor, Gyroscope, and Step Counter hardware. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Automatically restarts the background monitoring service upon device reboot. |
| `android.permission.FOREGROUND_SERVICE` | Ensures the service is never killed by Android's aggressive battery management. |

---

## 5. Technology Stack

- **Language:** Kotlin (1.9+)
- **UI Framework:** Jetpack Compose (Material Design 3 with custom obsidian glassmorphism)
- **Computer Vision:** Google ML Kit Pose Detection (`com.google.mlkit:pose-detection:18.0.0-beta3`)
- **Camera Pipeline:** AndroidX CameraX (`androidx.camera:camera-camera2:1.3.1`)
- **Notification Pipeline:** `NotificationListenerService` with package filters for WhatsApp & Zalo
- **Sensor Management:** Android SensorManager (Accelerometer, Gyroscope, Step Detector)
- **Local Persistence:** Room Database + Jetpack DataStore Preferences
- **Dependency Injection:** Hilt / Kotlin Coroutines & Flow

---

## 6. How Development & Installation Works

### Can I build the codebase?
**Yes!** I can write and scaffold the entire native Android project (all Kotlin classes, Compose screens, ML Kit pose analyzers, Accessibility services, and Gradle configurations).

### What do you need on your computer?
To compile the code into an `.apk` and install it onto your Android phone:
1. **Download & Install Android Studio** (free from [developer.android.com/studio](https://developer.android.com/studio)).
2. Enable **Developer Options** and **USB Debugging** on your Android phone.
3. Plug your phone into your PC with a USB cable.
4. Open the project in Android Studio and press **Run (▶️)**.
5. Android Studio automatically compiles the app and installs it directly onto your phone!
