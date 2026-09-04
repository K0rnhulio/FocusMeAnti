package com.focusme.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
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

        @Volatile
        var isRunning: Boolean = false
            internal set

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
            "tiktok.com",
            "youtube.com",
            "linkedin.com"
        )

        fun isEnabled(context: Context): Boolean {
            if (isRunning) return true

            // 1. Direct AccessibilityManager check
            try {
                val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
                val enabledServices = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                if (enabledServices != null) {
                    for (service in enabledServices) {
                        if (service.resolveInfo?.serviceInfo?.packageName == context.packageName) {
                            return true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking AccessibilityManager: ${e.message}")
            }

            // 2. Settings.Secure check
            try {
                val enabledServicesSetting = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: return false

                val expectedComponentName = ComponentName(context, FocusAccessibilityService::class.java)
                val full = expectedComponentName.flattenToString()
                val short = expectedComponentName.flattenToShortString()
                val pkg = context.packageName

                return enabledServicesSetting.contains(full) ||
                       enabledServicesSetting.contains(short) ||
                       enabledServicesSetting.contains(pkg)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking Settings.Secure: ${e.message}")
                return false
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = info.flags or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        info.notificationTimeout = 20
        serviceInfo = info

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, "🛡️ FocusMe Protection Active!", Toast.LENGTH_SHORT).show()
        }
        Log.d(TAG, "FocusAccessibilityService connected, isRunning = true")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isRunning = false
        stopHeartbeat()
        Log.d(TAG, "FocusAccessibilityService unbind, isRunning = false")
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return // Ignore events from our own app

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

        // 4. Standalone Target Apps Evaluation (Reddit, Twitter/X, FB, IG, TikTok, YouTube, LinkedIn)
        val lowerPkg = packageName.lowercase()
        val isTargetApp = lowerPkg.contains("reddit") ||
                lowerPkg.contains("twitter") ||
                lowerPkg.contains("katana") ||
                lowerPkg.contains("facebook") ||
                lowerPkg.contains("instagram") ||
                lowerPkg.contains("tiktok") ||
                lowerPkg.contains("musically") ||
                lowerPkg.contains("youtube") ||
                lowerPkg.contains("snapchat") ||
                lowerPkg.contains("linkedin") ||
                lowerPkg.contains("webapk")

        if (isTargetApp) {
            currentForegroundPackage = packageName
            evaluateAppDiscipline(packageName)
        } else if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            currentForegroundPackage = packageName
            evaluateAppDiscipline(packageName)
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

            // Immediate Trigger A: Check event class or text for video feeds
            if (isZaloVideoFeedActive(rootNode, event)) {
                if (now - lastZaloRedirectTimestamp > 350) {
                    lastZaloRedirectTimestamp = now
                    redirectToZaloChats(rootNode)
                }
                return@launch
            }

            // Exemption: Inside an active chat room or composing message
            if (rootNode != null && isInsideChatRoom(rootNode)) {
                return@launch
            }

            // Trigger B: User tapped on Timeline ("Nhật ký"), Discover ("Khám phá"), or Video
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                val clickedText = event.text.joinToString(" ")
                val clickedDesc = event.contentDescription?.toString() ?: ""
                val combined = "$clickedText $clickedDesc".lowercase()

                val blockedWords = listOf("nhật ký", "khám phá", "timeline", "khoảnh khắc", "video")
                if (blockedWords.any { combined.contains(it) }) {
                    if (now - lastZaloRedirectTimestamp > 350) {
                        lastZaloRedirectTimestamp = now
                        redirectToZaloChats(rootNode)
                    }
                    return@launch
                }
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
        val className = event.className?.toString()?.lowercase() ?: ""
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
                val clickedText = event.text.joinToString(" ")
                val clickedDesc = event.contentDescription?.toString() ?: ""
                val combined = "$clickedText $clickedDesc".lowercase()

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
        val now = System.currentTimeMillis()
        var detectedUrl: String? = null

        // 1. Scan known URL bar view IDs
        if (rootNode != null) {
            val urlBarIds = listOf(
                "$browserPkg:id/url_bar",
                "$browserPkg:id/location_bar_edit_text",
                "$browserPkg:id/search_box",
                "$browserPkg:id/toolbar",
                "$browserPkg:id/url_bar_title"
            )
            for (id in urlBarIds) {
                val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
                if (nodes.isNotEmpty() && nodes[0].text != null) {
                    detectedUrl = nodes[0].text.toString().lowercase()
                    break
                }
            }
        }

        // 2. Scan window titles for site names / domains
        if (detectedUrl == null) {
            try {
                for (w in windows) {
                    val title = w.title?.toString()?.lowercase() ?: ""
                    for (domain in BLOCKED_DOMAINS) {
                        val base = domain.substringBefore(".")
                        if (title.contains(domain) || (base.length > 3 && title.contains(base))) {
                            detectedUrl = domain
                            break
                        }
                    }
                    if (detectedUrl != null) break
                }
            } catch (e: Exception) {
                // Fallback
            }
        }

        // 3. Fallback: search for domain text in active window
        if (detectedUrl == null && rootNode != null) {
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
                val base = domain.substringBefore(".")
                if (detectedUrl.contains(domain) || (base.length > 3 && detectedUrl.contains(base))) {
                    matchedDomain = domain
                    break
                }
            }
        }

        if (matchedDomain != null) {
            lastBlockedDomainSeenTimestamp = now
            val webTarget = "web:$matchedDomain"
            currentForegroundPackage = webTarget
            evaluateAppDiscipline(webTarget)
        } else {
            // User is in browser, but on an allowed page (e.g. google.com, stackoverflow)
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
        if (pkg == applicationContext.packageName) return // Never block our own app

        val now = System.currentTimeMillis()
        if (now - lastBlockedTimestamp < 500) return // Debounce

        serviceScope.launch {
            val prefs = FocusMeApp.instance.preferences
            val blockedSet = prefs.blockedPackages.first()
            val lowerPkg = pkg.lowercase()

            val isBlocked = blockedSet.contains(pkg) || 
                            lowerPkg.contains("reddit") || 
                            lowerPkg.contains("twitter") || 
                            lowerPkg.contains("katana") || 
                            lowerPkg.contains("facebook") || 
                            lowerPkg.contains("instagram") || 
                            lowerPkg.contains("tiktok") || 
                            lowerPkg.contains("musically") || 
                            lowerPkg.contains("youtube") || 
                            lowerPkg.contains("snapchat") || 
                            lowerPkg.contains("linkedin") || 
                            lowerPkg.contains("webapk") || 
                            lowerPkg.startsWith("web:")

            Log.d(TAG, "evaluateAppDiscipline: pkg=$pkg, isBlocked=$isBlocked")

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
                Log.d(TAG, "Blocked: outside schedule")
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
                Log.d(TAG, "Blocked: quota exhausted")
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
                Log.d(TAG, "Blocked: gauntlet required")
                stopHeartbeat()
                OverlayService.hidePill(this@FocusAccessibilityService)
                launchLockOverlay("gauntlet_required", pkg)
                return@launch
            }

            // 4. 30-Min Mindful Reflection Required
            if (!usage.hasReflected) {
                Log.d(TAG, "Blocked: reflection required")
                stopHeartbeat()
                OverlayService.hidePill(this@FocusAccessibilityService)
                launchLockOverlay("reflection_required", pkg)
                return@launch
            }

            // 5. Start wall-clock dwell timer & floating pill
            Log.d(TAG, "Session allowed: starting heartbeat")
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
        val now = System.currentTimeMillis()
        if (now - lastBlockedTimestamp < 600) return
        lastBlockedTimestamp = now

        Log.d(TAG, "🛡️ LAUNCHING LOCK OVERLAY for $targetPkg (reason: $reason)")

        // 1. Only minimize native apps to Home if overlay permission is missing (never minimize Chrome/browsers)
        if (!targetPkg.startsWith("web:") && !Settings.canDrawOverlays(this)) {
            try {
                performGlobalAction(GLOBAL_ACTION_HOME)
            } catch (e: Exception) {
                Log.e(TAG, "Failed performGlobalAction HOME: ${e.message}")
            }
        }

        // 2. Launch full-screen lock overlay directly on top of current task
        val intent = Intent(this, OverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
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
        isRunning = false
        stopHeartbeat()
    }
}
