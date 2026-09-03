package com.focusme.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.focusme.app.FocusMeApp
import com.focusme.app.data.model.HourlyUsage
import com.focusme.app.ui.screens.OverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FocusAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var heartbeatJob: Job? = null
    private var currentForegroundPackage = ""

    private val hourFormat = SimpleDateFormat("yyyy-MM-dd-HH", Locale.getDefault())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // 1. In-App WhatsApp Status & Updates Shield
        if (packageName == "com.whatsapp") {
            handleWhatsAppStatusShield(rootInActiveWindow)
        }

        // 2. In-App Zalo Video & Timeline Shield
        if (packageName == "com.zing.zalo") {
            handleZaloVideoShield(rootInActiveWindow)
        }

        // 3. Foreground App State Tracking & Lockout Evaluation
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            currentForegroundPackage = packageName
            evaluateAppDiscipline(packageName)
        }
    }

    private fun handleWhatsAppStatusShield(rootNode: AccessibilityNodeInfo?) {
        if (rootNode == null) return
        serviceScope.launch {
            val isEnabled = FocusMeApp.instance.preferences.whatsappStatusBlock.first()
            if (!isEnabled) return@launch

            // Detect if user is in the Status / Updates tab
            val statusNodes = rootNode.findAccessibilityNodeInfosByText("Status")
            val updatesNodes = rootNode.findAccessibilityNodeInfosByText("Updates")
            
            for (node in (statusNodes + updatesNodes)) {
                if (node.isSelected || node.isFocused) {
                    // Force navigation back to Chats tab
                    val chatNodes = rootNode.findAccessibilityNodeInfosByText("Chats")
                    if (chatNodes.isNotEmpty()) {
                        chatNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    } else {
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                    break
                }
            }
        }
    }

    private fun handleZaloVideoShield(rootNode: AccessibilityNodeInfo?) {
        if (rootNode == null) return
        serviceScope.launch {
            val isEnabled = FocusMeApp.instance.preferences.zaloVideoBlock.first()
            if (!isEnabled) return@launch

            val videoNodes = rootNode.findAccessibilityNodeInfosByText("Video")
            val timelineNodes = rootNode.findAccessibilityNodeInfosByText("Timeline")

            for (node in (videoNodes + timelineNodes)) {
                if (node.isSelected || node.isFocused) {
                    val messageNodes = rootNode.findAccessibilityNodeInfosByText("Messages")
                    if (messageNodes.isNotEmpty()) {
                        messageNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    } else {
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                    break
                }
            }
        }
    }

    private fun evaluateAppDiscipline(pkg: String) {
        serviceScope.launch {
            val blockedSet = FocusMeApp.instance.preferences.blockedPackages.first()
            if (!blockedSet.contains(pkg)) {
                stopHeartbeat()
                OverlayService.hidePill(this@FocusAccessibilityService)
                return@launch
            }

            val cal = Calendar.getInstance()
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)
            val startHour = FocusMeApp.instance.preferences.startHour.first()
            val endHour = FocusMeApp.instance.preferences.endHour.first()

            // 1. Outside 10:00 AM - 9:00 PM: 100% Lockout
            if (currentHour < startHour || currentHour >= endHour) {
                launchLockOverlay("outside_schedule", pkg)
                return@launch
            }

            val hourKey = hourFormat.format(Date())
            val usageDao = FocusMeApp.instance.database.usageDao()
            var usage = usageDao.getUsage(hourKey) ?: HourlyUsage(hourKey = hourKey, usedSeconds = 0)
            val quota = FocusMeApp.instance.preferences.quotaSeconds.first()

            // 2. Hourly Quota Exhausted: 100% Lockout for remaining clock hour
            if (usage.usedSeconds >= quota) {
                launchLockOverlay("quota_exhausted", pkg)
                return@launch
            }

            // 3. 30-Min Mindful Reflection Required
            if (!usage.hasReflected) {
                launchLockOverlay("reflection_required", pkg)
                return@launch
            }

            // 4. Start live active ticker & floating pill
            startHeartbeat(pkg, hourKey, quota)
        }
    }

    private fun startHeartbeat(pkg: String, hourKey: String, quota: Int) {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            val usageDao = FocusMeApp.instance.database.usageDao()
            while (isActive && currentForegroundPackage == pkg) {
                val usage = usageDao.getUsage(hourKey) ?: HourlyUsage(hourKey = hourKey, usedSeconds = 0)
                val newUsed = usage.usedSeconds + 1
                usageDao.insertOrUpdate(usage.copy(usedSeconds = newUsed, lastUpdated = System.currentTimeMillis()))

                val remaining = (quota - newUsed).coerceAtLeast(0)
                OverlayService.showPill(this@FocusAccessibilityService, remaining)

                if (remaining <= 0) {
                    launchLockOverlay("quota_exhausted", pkg)
                    break
                }
                delay(1000)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun launchLockOverlay(reason: String, targetPkg: String) {
        val intent = Intent(this, OverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("overlay_reason", reason)
            putExtra("target_package", targetPkg)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        stopHeartbeat()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopHeartbeat()
    }
}
