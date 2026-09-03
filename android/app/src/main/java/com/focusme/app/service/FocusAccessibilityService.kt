package com.focusme.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
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
    private var lastBlockedTimestamp = 0L

    private val hourFormat = SimpleDateFormat("yyyy-MM-dd-HH", Locale.getDefault())

    companion object {
        private const val TAG = "FocusAccessibility"
        
        // Multi-browser package names
        val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "com.sec.android.app.sbrowser",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.brave.browser",
            "com.opera.browser",
            "com.duckduckgo.mobile.android"
        )

        // Blocked web domains
        val BLOCKED_DOMAINS = listOf(
            "reddit.com",
            "twitter.com",
            "x.com",
            "facebook.com",
            "instagram.com",
            "tiktok.com"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or 
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or 
                         AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 50
        }
        serviceInfo = info

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, "🛡️ FocusMe Protection Active!", Toast.LENGTH_SHORT).show()
        }
        Log.d(TAG, "FocusAccessibilityService connected and active!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // 1. WhatsApp Status & Updates Shield (Multi-language: English, Vietnamese, Spanish...)
        if (packageName.contains("whatsapp")) {
            handleWhatsAppStatusShield(rootInActiveWindow)
        }

        // 2. Zalo Video & Timeline / Nhật ký Shield
        if (packageName.contains("zalo")) {
            handleZaloVideoShield(rootInActiveWindow)
        }

        // 3. Browser URL Interception (Chrome, Samsung Internet, Firefox...)
        if (BROWSER_PACKAGES.contains(packageName)) {
            handleBrowserUrlCheck(rootInActiveWindow, packageName)
        }

        // 4. Standalone Target Apps Evaluation (Reddit, Twitter/X, FB, IG, TikTok, YouTube)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            if (currentForegroundPackage != packageName) {
                currentForegroundPackage = packageName
                evaluateAppDiscipline(packageName)
            }
        }
    }

    private fun handleWhatsAppStatusShield(rootNode: AccessibilityNodeInfo?) {
        if (rootNode == null) return
        serviceScope.launch {
            val isEnabled = FocusMeApp.instance.preferences.whatsappStatusBlock.first()
            if (!isEnabled) return@launch

            val keywords = listOf("Status", "Updates", "Cập nhật", "Estado", "Estados")
            for (kw in keywords) {
                val nodes = rootNode.findAccessibilityNodeInfosByText(kw)
                for (node in nodes) {
                    if (node.isSelected || node.isFocused) {
                        // Return to Chats
                        val chatKeywords = listOf("Chats", "Trò chuyện", "Chats", "Mensajes")
                        var redirected = false
                        for (ckw in chatKeywords) {
                            val chatNodes = rootNode.findAccessibilityNodeInfosByText(ckw)
                            if (chatNodes.isNotEmpty()) {
                                chatNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                redirected = true
                                break
                            }
                        }
                        if (!redirected) {
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        }
                        return@launch
                    }
                }
            }
        }
    }

    private fun handleZaloVideoShield(rootNode: AccessibilityNodeInfo?) {
        if (rootNode == null) return
        serviceScope.launch {
            val isEnabled = FocusMeApp.instance.preferences.zaloVideoBlock.first()
            if (!isEnabled) return@launch

            // Keywords for Video tab and Timeline/Diary feed
            val blockKeywords = listOf("Video", "Timeline", "Nhật ký", "Khám phá", "Shorts", "Reels")
            for (kw in blockKeywords) {
                val nodes = rootNode.findAccessibilityNodeInfosByText(kw)
                for (node in nodes) {
                    if (node.isSelected || node.isFocused) {
                        val msgKeywords = listOf("Tin nhắn", "Messages", "Chats")
                        var redirected = false
                        for (mkw in msgKeywords) {
                            val msgNodes = rootNode.findAccessibilityNodeInfosByText(mkw)
                            if (msgNodes.isNotEmpty()) {
                                msgNodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                redirected = true
                                break
                            }
                        }
                        if (!redirected) {
                            performGlobalAction(GLOBAL_ACTION_BACK)
                        }
                        return@launch
                    }
                }
            }
        }
    }

    private fun handleBrowserUrlCheck(rootNode: AccessibilityNodeInfo?, browserPkg: String) {
        if (rootNode == null) return

        // Search for address bar text in browser
        val urlBarIds = listOf(
            "$browserPkg:id/url_bar",
            "$browserPkg:id/location_bar_edit_text",
            "$browserPkg:id/search_box",
            "$browserPkg:id/toolbar"
        )

        var detectedUrl: String? = null
        for (id in urlBarIds) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNotEmpty() && nodes[0].text != null) {
                detectedUrl = nodes[0].text.toString().toLowerCase(Locale.getDefault())
                break
            }
        }

        if (detectedUrl != null) {
            for (domain in BLOCKED_DOMAINS) {
                if (detectedUrl.contains(domain)) {
                    evaluateAppDiscipline("web:$domain")
                    break
                }
            }
        }
    }

    private fun evaluateAppDiscipline(pkg: String) {
        val now = System.currentTimeMillis()
        if (now - lastBlockedTimestamp < 800) return // Debounce

        serviceScope.launch {
            val blockedSet = FocusMeApp.instance.preferences.blockedPackages.first()
            
            val isBlocked = blockedSet.contains(pkg) || 
                            pkg.contains("reddit") || 
                            pkg.contains("twitter") || 
                            pkg.contains("katana") || 
                            pkg.contains("instagram") ||
                            pkg.startsWith("web:")

            if (!isBlocked) {
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
                lastBlockedTimestamp = now
                launchLockOverlay("outside_schedule", pkg)
                return@launch
            }

            val hourKey = hourFormat.format(Date())
            val usageDao = FocusMeApp.instance.database.usageDao()
            var usage = usageDao.getUsage(hourKey) ?: HourlyUsage(hourKey = hourKey, usedSeconds = 0)
            val quota = FocusMeApp.instance.preferences.quotaSeconds.first()

            // 2. Hourly Quota Exhausted: 100% Lockout
            if (usage.usedSeconds >= quota) {
                lastBlockedTimestamp = now
                launchLockOverlay("quota_exhausted", pkg)
                return@launch
            }

            // 3. 30-Min Mindful Reflection Required
            if (!usage.hasReflected) {
                lastBlockedTimestamp = now
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
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
