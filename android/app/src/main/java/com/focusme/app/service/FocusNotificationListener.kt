package com.focusme.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.focusme.app.FocusMeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class FocusNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        @Volatile
        var reactivePassExpiryTime: Long = 0L
            private set

        fun isReactivePassActive(): Boolean {
            return System.currentTimeMillis() < reactivePassExpiryTime
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        if (pkg != "com.whatsapp" && pkg != "com.zing.zalo") return

        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)

        serviceScope.launch {
            val isReactiveEnabled = FocusMeApp.instance.preferences.reactiveNight.first()
            if (!isReactiveEnabled) return@launch

            // If it's night time (after 9:00 PM or before 10:00 AM)
            if (currentHour >= 21 || currentHour < 10) {
                // Grant 3-minute reactive reply window (180,000 ms)
                reactivePassExpiryTime = System.currentTimeMillis() + (3 * 60 * 1000)
            }
        }
    }
}
