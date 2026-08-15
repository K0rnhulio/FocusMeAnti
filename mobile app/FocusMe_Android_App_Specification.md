# 📱 FocusMe for Android — Complete Technical Specification & Architecture Document

> **Project Name:** FocusMe Discipline & Morning Unblocker  
> **Target Platform:** Android 8.0+ (API Level 26+)  
> **Language & Framework:** Kotlin, Jetpack Compose, CameraX, Google ML Kit, Android Accessibility Service, NotificationListenerService  
> **Version:** 1.2.0 (10:00 AM Rule & In-App WhatsApp/Zalo Doomscroll Shield)

---

## 1. Executive Summary & Core Philosophy

Smartphones are engineered around frictionless dopamine loops. Typical app blockers fail because unlocking an app only requires typing a password or waiting out a trivial timer—actions that require zero physical or cognitive exertion.

**FocusMe for Android** inverts this dynamic by introducing **Physical Friction, Cognitive Effort Gates, In-App Doomscroll Shielding, and Reactive-Only Communication**:
1. **The Morning Wake-up Protocol:** Forces physical bed-exit and neurological activation (Steps, G-Sensor Shakes, or Gyroscope Tilt Maze) before the phone unblocks in the morning.
2. **⏰ 10:00 AM – 9:00 PM Active Window (100% Locked Before 10:00 AM):** Social media and distracting apps **cannot be opened before 10:00 AM** or after 9:00 PM. Morning hours remain dedicated to pure deep work and clear-headed focus.
3. **The Hourly Dopamine Toll Gate:** During permitted hours (10:00 AM – 9:00 PM), enforces a strict **5 minutes per clock hour combined limit** on Twitter/X, Reddit, Facebook, Instagram, and TikTok with a mandatory 30-min productivity check-in.
4. **🛡️ WhatsApp Status & Zalo Video Doomscroll Shield:** Strips out WhatsApp Status/Stories and Zalo Video feeds directly inside the native apps so messaging remains pure communication without rabbit holes.
5. **🌙 Reactive-Only Nighttime Messaging Gate (WhatsApp & Zalo):** After 9:00 PM, you can only open WhatsApp or Zalo when a legitimate incoming message arrives, granting a 2–3 minute focused reply window.

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
│  │ • In-App View Hierarchy Scraper (Blocks WhatsApp Status & Zalo Video) │  │
│  │ • Enforces 10:00 AM – 9:00 PM daily active window                     │  │
│  │ • Tracks cumulative hourly usage bucket [YYYY-MM-DD-HH] (5m limit)    │  │
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
│  │ 💬 In-App Status Shield ("Status & Video Doomscrolling Disabled")     │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Detailed Feature Specifications

### 3.1. The Morning Wake-up Protocol (Bed-Exit Barrier)
Activates from midnight (00:00) until the user completes the morning protocol:
- **Challenge A — 50 G-Sensor Arm Shakes:**
  - Accelerometer threshold: $G > 1.8g$ with peak-to-trough interval validation.
  - Circular reactive progress bar with haptic feedback on each valid rep.
- **Challenge B — 25-Step Bed-Exit Walk:**
  - Utilizes `Sensor.TYPE_STEP_DETECTOR` to verify you physically left bed.
- **Challenge C — 🐭 Gyroscope Tilt Maze:**
  - Procedurally generated 2D labyrinth using physical phone tilt physics to wake up the brain.
- **Challenge D — AI Camera Push-up / Squat Counter:**
  - Real-time on-device body landmark tracking using **Google ML Kit Pose Detection**.

---

### 3.2. ⏰ Daytime Active Window (10:00 AM – 9:00 PM)
- **Before 10:00 AM (00:00 – 10:00):** **100% Locked.** Social media and leisure scrolling are completely inaccessible. Morning hours are protected for deep work and high productivity.
- **Between 10:00 AM and 9:00 PM (10:00 – 21:00):** Permitted window with the **5 minutes per clock hour** combined limit across Twitter/X, Reddit, Facebook, Instagram, and TikTok with zero rollover.
- **After 9:00 PM (21:00 – 23:59):** **100% Locked** to protect evening wind-down and sleep.

---

### 3.3. The 2-Step Social Media Toll Gate
When opening target apps between 10:00 AM and 9:00 PM:
1. **Step 1: Mindful Reflection:**
   - Asks: *"What have you accomplished in the last 30 minutes?"* (minimum 15 characters).
2. **Step 2: Physical / Cognitive Toll (Configurable):**
   - 5 camera-tracked push-ups or 30 G-sensor shakes to earn the 5-minute break.
3. **Step 3: Access Granted:**
   - 5-minute countdown starts with a floating in-app timer pill. When time expires, app returns to the locked screen.

---

### 3.4. 🛡️ In-App WhatsApp Status & Zalo Video Doomscroll Shield

#### The Problem:
People often open WhatsApp just to send a message, but end up spending 30 minutes clicking through friends' **WhatsApp Status stories**. Similarly, in Zalo, the **Video / Shorts / Timeline** tab leads to endless video doomscrolling.

#### The Android Accessibility Implementation:
- **In WhatsApp (`com.whatsapp`):**
  - The `AccessibilityService` listens for `TYPE_VIEW_CLICKED` and `TYPE_WINDOW_CONTENT_CHANGED`.
  - When the user taps the **"Updates" / "Status" tab** (`com.whatsapp:id/status_tab` or view pager index 1):
    - The service automatically forces the view back to the **"Chats" tab** or draws a mini-banner:  
      > 🚫 *"Status Doomscrolling is Disabled. Focus on Direct Chats."*
    - Direct 1-on-1 and group chat messaging remains 100% functional.
- **In Zalo (`com.zing.zalo`):**
  - The `AccessibilityService` detects navigation to the **"Video" / "Shorts" tab** or **"Timeline" feed**:
    - Automatically intercepts and navigates back to the **Messages tab**.
    - Zalo Video reels and timeline autoplay are completely disabled.

---

### 3.5. 🌙 Reactive-Only Nighttime Messaging Gate (After 9:00 PM)

- **Proactive Opening Blocked:** Tapping WhatsApp or Zalo after 9:00 PM with no new messages displays:  
  > 🔒 *"Night Lock Active — You can open WhatsApp/Zalo when an incoming message arrives."*
- **Notification-Triggered 3-Minute Reply Window (`NotificationListenerService`):**
  - When an incoming message arrives, a **3-minute reply window** unlocks.
  - Floating pill counts down: `Reply Window: 2m 45s left`.
- **Emergency Calls:** Phone and VoIP calls are **100% unrestricted 24/7**.

---

## 4. Android System APIs & Required Permissions

| Permission | Purpose |
|---|---|
| `android.permission.BIND_ACCESSIBILITY_SERVICE` | Real-time foreground app detection, In-App WhatsApp Status / Zalo Video blocking, and UI tree filtering. |
| `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` | Detects incoming WhatsApp & Zalo messages to activate the 3-minute reactive reply window after 9 PM. |
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
- **In-App Node Inspector:** AccessibilityNodeInfo tree parser for WhatsApp Status & Zalo Video suppression
- **Sensor Management:** Android SensorManager (Accelerometer, Gyroscope, Step Detector)
- **Local Persistence:** Room Database + Jetpack DataStore Preferences
- **Dependency Injection:** Hilt / Kotlin Coroutines & Flow

---

## 6. Development & Installation Guide

To build the `.apk` from source and install onto your phone:
1. Open the project in **Android Studio**.
2. Enable **Developer Options** & **USB Debugging** on your phone.
3. Connect your phone via USB and click **▶️ Run**.
4. Grant the Accessibility, Overlay, and Notification permissions when prompted.
