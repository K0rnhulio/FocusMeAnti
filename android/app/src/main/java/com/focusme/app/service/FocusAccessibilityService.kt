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
    private var lastBlockedDomainSeenTimestamp = 0L

    // Wall-clock tracking variables to prevent fast ticking
    private var trackingStartTime = 0L
    private var trackingBaseUsedSeconds = 0
    private var trackingTarget = ""

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
            notificationTimeout = 20
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

        // 1. Zalo Full Newsfeed & Video Tab Blocker
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
            return // Do NOT fall through to standalone app evaluation!
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
     * Ironclad Zalo Newsfeed & Video Shield:
     * - Allows full access to direct chat messages, group chats, contacts, calls, and chat rooms.
     * - Completely blocks "Nhật ký" (Timeline), "Khám phá" (Discover), and the "Video" tab/player.
     * - Immediately bounces out if video player or video scrolling is detected.
     */
    private fun handleZaloFeedShield(rootNode: AccessibilityNodeInfo?, event: AccessibilityEvent) {
        val now = System.currentTimeMillis()

        serviceScope.launch {
            val isEnabled = FocusMeApp.instance.preferences.zaloVideoBlock.first()
            if (!isEnabled) return@launch

            // Always allow active chat conversations (text input present)
            if (rootNode != null && isInsideChatRoom(rootNode)) {
                return@launch
            }

            val eventType = event.eventType

            // Trigger A: User clicked on Video tab, Timeline, Discover, or a Video/Reel element
            if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                val clickedText = event.text?.joinToString(" ") ?: ""
                val clickedDesc = event.contentDescription?.toString() ?: ""
                val combined = "$clickedText $clickedDesc".toLowerCase(Locale.getDefault())

                if (combined.contains("nhật ký") || combined.contains("khám phá") || 
                    combined.contains("timeline") || combined.contains("khoảnh khắc") || 
                    combined.contains("video") || combined.contains("shorts") || 
                    combined.contains("reels")) {
                    
                    if (now - lastZaloRedirectTimestamp > 250) {
                        lastZaloRedirectTimestamp = now
                        redirectToZaloChats(rootNode)
                    }
                    return@launch
                }
            }

            // Trigger B: User is actively in a Video feed / Video player activity (or scrolling videos)
            val isVideoFeed = isZaloVideoFeedActive(rootNode, event)
            if (isVideoFeed) {
                if (now - lastZaloRedirectTimestamp > 250) {
                    lastZaloRedirectTimestamp = now
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    redirectToZaloChats(rootNode)
                }
                return@launch
            }

            // Trigger C: Feed / Video tab is selected in bottom bar
            if (rootNode != null && isZaloFeedTabSelected(rootNode)) {
                if (now - lastZaloRedirectTimestamp > 350) {
                    lastZaloRedirectTimestamp = now
                    redirectToZaloChats(rootNode)
                }
            }
        }
    }

    private fun isInsideChatRoom(rootNode: AccessibilityNodeInfo): Boolean {
        // Chat rooms contain message input fields or send buttons
        val inputNodes = rootNode.findAccessibilityNodeInfosByViewId("com.zing.zalo:id/chat_input_text")
        if (inputNodes.isNotEmpty()) return true

        val sendNodes = rootNode.findAccessibilityNodeInfosByViewId("com.zing.zalo:id/btn_send")
        if (sendNodes.isNotEmpty()) return true

        val hintNodes = rootNode.findAccessibilityNodeInfosByText("Nhập tin nhắn")
        if (hintNodes.isNotEmpty()) return true

        val voiceNodes = rootNode.findAccessibilityNodeInfosByViewId("com.zing.zalo:id/btn_voice_message")
        if (voiceNodes.isNotEmpty()) return true

        return false
    }

    private fun isZaloVideoFeedActive(rootNode: AccessibilityNodeInfo?, event: AccessibilityEvent): Boolean {
        val className = event.className?.toString()?.toLowerCase(Locale.getDefault()) ?: ""
        if (className.contains("video") || className.contains("player") || className.contains("zinstant")) {
            return true
        }

        if (rootNode == null) return false

        val videoKeywords = listOf(
            "lướt để xem video",
            "dành cho bạn",
            "theo dõi video",
            "kênh video",
            "thước phim",
            "âm thanh gốc",
            "chia sẻ video"
        )
        for (kw in videoKeywords) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(kw)
            if (nodes.isNotEmpty()) return true
        }

        val videoIds = listOf(
            "com.zing.zalo:id/video_container",
            "com.zing.zalo:id/exo_player",
            "com.zing.zalo:id/player_view",
            "com.zing.zalo:id/layout_feed_video"
        )
        for (vid in videoIds) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(vid)
            if (nodes.isNotEmpty()) return true
        }

        return false
    }

    private fun isZaloFeedTabSelected(rootNode: AccessibilityNodeInfo): Boolean {
        val feedKeywords = listOf("Nhật ký", "Khám phá", "Timeline", "Khoảnh khắc", "Video")
        for (kw in feedKeywords) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(kw)
            for (node in nodes) {
                if (node.isSelected || node.isFocused) {
                    return true
                }
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

        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun handleWhatsAppStatusShield(rootNode: AccessibilityNodeInfo?, event: AccessibilityEvent) {
        if (rootNode == null) return
        serviceScope.launch {
            val isEnabled = FocusMeApp.instance.preferences.whatsappStatusBlock.first()
            if (!isEnabled) return@launch

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
        val chatKeywords = listOf("Chats", "Trò chuyện", "Tin nhắn")
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

    /**
     * Precise Browser URL Interception & Dwell Time Tracker with Grace Period:
     * Scans for blocked domains (Reddit, Twitter, etc.) inside Chrome, Samsung Internet, Firefox...
     */
    private fun handleBrowserUrlCheck(rootNode: AccessibilityNodeInfo?, browserPkg: String) {
        if (rootNode == null) return

        val urlBarIds = listOf(
            "$browserPkg:id/url_bar",
            "$browserPkg:id/location_bar_edit_text",
            "$browserPkg:id/search_box",
            "$browserPkg:id/toolbar",
            "$browserPkg:id/url_bar_title"
        )

        var detectedUrl: String? = null
        for (id in urlBarIds) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNotEmpty() && nodes[0].text != null) {
                detectedUrl = nodes[0].text.toString().toLowerCase(Locale.getDefault())
                break
            }
        }

        // Fallback: search for domain text if URL bar is collapsed
        if (detectedUrl == null) {
            for (domain in BLOCKED_DOMAINS) {
                val nodes = rootNode.findAccessibilityNodeInfosByText(domain)
                if (nodes.isNotEmpty()) {
                    detectedUrl = domain
                    break
                }
            }
        }

        var matchedDomain: String? = null
        if (detectedUrl != null) {
            for (domain in BLOCKED_DOMAINS) {
                if (detectedUrl.contains(domain)) {
                    matchedDomain = domain
                    break
                }
            }
        }

        val now = System.currentTimeMillis()

        if (matchedDomain != null) {
            lastBlockedDomainSeenTimestamp = now
            val webTarget = "web:$matchedDomain"
            if (currentForegroundPackage != webTarget) {
                currentForegroundPackage = webTarget
                evaluateAppDiscipline(webTarget)
            }
        } else {
            // User is in browser, but on an allowed page (e.g. google.com, stackoverflow)
            // Apply a 2.5s grace period to prevent flickering when URL bar momentarily hides during scroll
            if (currentForegroundPackage.startsWith("web:")) {
                if (now - lastBlockedDomainSeenTimestamp > 2500) {
                    currentForegroundPackage = browserPkg
                    stopHeartbeat()
                    OverlayService.hidePill(this@FocusAccessibilityService)
                }
            }
        }
    }

    private fun evaluateAppDiscipline(pkg: String) {
        val now = System.currentTimeMillis()
        if (now - lastBlockedTimestamp < 800) return // Debounce

        serviceScope.launch {
            val prefs = FocusMeApp.instance.preferences
            val blockedSet = prefs.blockedPackages.first()
            
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
            val startHour = prefs.startHour.first()
            val endHour = prefs.endHour.first()

            // 1. Outside 10:00 AM - 9:00 PM: 100% Lockout
            if (currentHour < startHour || currentHour >= endHour) {
                lastBlockedTimestamp = now
                stopHeartbeat()
                OverlayService.hidePill(this@FocusAccessibilityService)
                launchLockOverlay("outside_schedule", pkg)
                return@launch
            }

            val hourKey = hourFormat.format(Date())
            val usageDao = FocusMeApp.instance.database.usageDao()
            var usage = usageDao.getUsage(hourKey) ?: HourlyUsage(hourKey = hourKey, usedSeconds = 0)
            val quota = prefs.quotaSeconds.first()

            // 2. Hourly Quota Exhausted: 100% Lockout
            if (usage.usedSeconds >= quota) {
                lastBlockedTimestamp = now
                stopHeartbeat()
                OverlayService.hidePill(this@FocusAccessibilityService)
                launchLockOverlay("quota_exhausted", pkg)
                return@launch
            }

            // 3. Check if all 3 Physical & Cognitive Toll Gates are cleared for this hour
            val mazeHour = prefs.mazeSolvedHour.first()
            val shakeHour = prefs.shakeSolvedHour.first()
            val pushUpHour = prefs.pushUpSolvedHour.first()
            val allGatesCleared = (mazeHour == hourKey && shakeHour == hourKey && pushUpHour == hourKey)

            if (!allGatesCleared) {
                lastBlockedTimestamp = now
                stopHeartbeat()
                OverlayService.hidePill(this@FocusAccessibilityService)
                launchLockOverlay("gauntlet_required", pkg)
                return@launch
            }

            // 4. 30-Min Mindful Reflection Required
            if (!usage.hasReflected) {
                lastBlockedTimestamp = now
                stopHeartbeat()
                OverlayService.hidePill(this@FocusAccessibilityService)
                launchLockOverlay("reflection_required", pkg)
                return@launch
            }

            // 5. Start wall-clock dwell timer & floating pill
            startHeartbeat(pkg, hourKey, quota)
        }
    }

    /**
     * Hardware Wall-Clock Dwell Time Engine:
     * Derives elapsed seconds from System.currentTimeMillis() so the timer can NEVER run faster than real time.
     */
    private fun startHeartbeat(pkg: String, hourKey: String, quota: Int) {
        heartbeatJob?.cancel()

        heartbeatJob = serviceScope.launch {
            val usageDao = FocusMeApp.instance.database.usageDao()
            val prefs = FocusMeApp.instance.preferences

            val currentDbUsage = usageDao.getUsage(hourKey) ?: HourlyUsage(hourKey = hourKey, usedSeconds = 0)

            // Initialize wall-clock anchor
            trackingStartTime = System.currentTimeMillis()
            trackingBaseUsedSeconds = currentDbUsage.usedSeconds
            trackingTarget = pkg

            while (isActive && currentForegroundPackage == pkg) {
                val elapsedSeconds = ((System.currentTimeMillis() - trackingStartTime) / 1000).toInt()
                val currentTotalUsed = trackingBaseUsedSeconds + elapsedSeconds

                usageDao.insertOrUpdate(
                    currentDbUsage.copy(
                        usedSeconds = currentTotalUsed,
                        lastUpdated = System.currentTimeMillis()
                    )
                )

                val remaining = (quota - currentTotalUsed).coerceAtLeast(0)

                val showPillEnabled = prefs.showPill.first()
                if (showPillEnabled) {
                    OverlayService.showPill(this@FocusAccessibilityService, remaining)
                }

                if (remaining <= 0) {
                    OverlayService.hidePill(this@FocusAccessibilityService)
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
        trackingTarget = ""
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
