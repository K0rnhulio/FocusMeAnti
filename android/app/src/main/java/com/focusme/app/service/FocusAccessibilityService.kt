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
    private var lastZaloRedirectTimestamp = 0L

    private val hourFormat = SimpleDateFormat("yyyy-MM-dd-HH", Locale.getDefault())

    companion object {
        private const val TAG = "FocusAccessibility"

        val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "com.sec.android.app.sbrowser",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.brave.browser",
            "com.opera.browser",
            "com.duckduckgo.mobile.android"
        )

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
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
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
        Log.d(TAG, "FocusAccessibilityService connected with ALL_MASK flags")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // 1. Zalo Feed & Story Shield (Click-intercept & Active tab only)
        if (packageName.contains("zalo")) {
            handleZaloFeedShield(rootInActiveWindow, event)
            return
        }

        // 2. WhatsApp Status & Updates Shield
        if (packageName.contains("whatsapp")) {
            handleWhatsAppStatusShield(rootInActiveWindow, event)
            return
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

    /**
     * Precise Zalo Feed Blocker:
     * Intercepts clicks on "Nhật ký" (Timeline) & "Khám phá" (Discover) and forces navigation to "Tin nhắn" (Chats).
     * NEVER interrupts active 1-on-1 chat rooms or the Messages tab.
     */
    private fun handleZaloFeedShield(rootNode: AccessibilityNodeInfo?, event: AccessibilityEvent) {
        val now = System.currentTimeMillis()

        serviceScope.launch {
            val isEnabled = FocusMeApp.instance.preferences.zaloVideoBlock.first()
            if (!isEnabled) return@launch

            // Check if inside a direct chat room (Look for message input box)
            if (rootNode != null && isInsideChatRoom(rootNode)) {
                return@launch // Real chat message in progress -> DO NOT INTERFERE!
            }

            // Case A: User clicked directly on a Feed Tab or Feed Story/Video
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                val clickedText = event.text?.joinToString(" ") ?: ""
                val clickedDesc = event.contentDescription?.toString() ?: ""
                val combined = "$clickedText $clickedDesc".toLowerCase(Locale.getDefault())

                if (combined.contains("nhật ký") || combined.contains("khám phá") || 
                    combined.contains("timeline") || combined.contains("khoảnh khắc") || 
                    combined.contains("video")) {
                    
                    if (now - lastZaloRedirectTimestamp > 300) {
                        lastZaloRedirectTimestamp = now
                        redirectToZaloChats(rootNode)
                    }
                    return@launch
                }
            }

            // Case B: User switched to the Timeline / Discover tab (selected/focused tab)
            if (rootNode != null && isZaloFeedTabSelected(rootNode)) {
                if (now - lastZaloRedirectTimestamp > 500) {
                    lastZaloRedirectTimestamp = now
                    redirectToZaloChats(rootNode)
                }
            }
        }
    }

    private fun isInsideChatRoom(rootNode: AccessibilityNodeInfo): Boolean {
        // Chat rooms contain input fields or send buttons
        val inputNodes = rootNode.findAccessibilityNodeInfosByViewId("com.zing.zalo:id/chat_input_text")
        if (inputNodes.isNotEmpty()) return true

        val sendNodes = rootNode.findAccessibilityNodeInfosByViewId("com.zing.zalo:id/btn_send")
        if (sendNodes.isNotEmpty()) return true

        val hintNodes = rootNode.findAccessibilityNodeInfosByText("Nhập tin nhắn")
        if (hintNodes.isNotEmpty()) return true

        return false
    }

    private fun isZaloFeedTabSelected(rootNode: AccessibilityNodeInfo): Boolean {
        val feedKeywords = listOf("Nhật ký", "Khám phá", "Timeline", "Khoảnh khắc")
        for (kw in feedKeywords) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(kw)
            for (node in nodes) {
                if (node.isSelected || node.isFocused) {
                    return true
                }
                // Check parent clickable container selection
                val parent = node.parent
                if (parent != null && (parent.isSelected || parent.isFocused)) {
                    return true
                }
            }
        }
        return false
    }

    private fun redirectToZaloChats(rootNode: AccessibilityNodeInfo?) {
        if (rootNode == null) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            return
        }

        // Click the "Tin nhắn" or "Messages" tab (Tab 0)
        val chatKeywords = listOf("Tin nhắn", "Messages", "Chats")
        for (kw in chatKeywords) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(kw)
            for (node in nodes) {
                var target: AccessibilityNodeInfo? = node
                while (target != null && !target.isClickable) {
                    target = target.parent
                }
                if (target != null && target.isClickable) {
                    target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return
                }
            }
        }

        // Fallback: Back button
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun handleWhatsAppStatusShield(rootNode: AccessibilityNodeInfo?, event: AccessibilityEvent) {
        if (rootNode == null) return
        serviceScope.launch {
            val isEnabled = FocusMeApp.instance.preferences.whatsappStatusBlock.first()
            if (!isEnabled) return@launch

            // If user clicked or selected Updates / Status
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                val clickedText = event.text?.joinToString(" ") ?: ""
                val clickedDesc = event.contentDescription?.toString() ?: ""
                val combined = "$clickedText $clickedDesc".toLowerCase(Locale.getDefault())

                if (combined.contains("status") || combined.contains("updates") || combined.contains("cập nhật")) {
                    redirectToWhatsAppChats(rootNode)
                    return@launch
                }
            }

            val keywords = listOf("Status", "Updates", "Cập nhật", "Estado")
            for (kw in keywords) {
                val nodes = rootNode.findAccessibilityNodeInfosByText(kw)
                for (node in nodes) {
                    if (node.isSelected || node.isFocused) {
                        redirectToWhatsAppChats(rootNode)
                        return@launch
                    }
                }
            }
        }
    }

    private fun redirectToWhatsAppChats(rootNode: AccessibilityNodeInfo) {
        val chatKeywords = listOf("Chats", "Trò chuyện", "Tin nhắn", "Chats")
        for (ckw in chatKeywords) {
            val chatNodes = rootNode.findAccessibilityNodeInfosByText(ckw)
            for (node in chatNodes) {
                var target: AccessibilityNodeInfo? = node
                while (target != null && !target.isClickable) {
                    target = target.parent
                }
                if (target != null && target.isClickable) {
                    target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return
                }
            }
        }
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun handleBrowserUrlCheck(rootNode: AccessibilityNodeInfo?, browserPkg: String) {
        if (rootNode == null) return

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
