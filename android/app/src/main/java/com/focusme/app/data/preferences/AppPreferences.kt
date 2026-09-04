package com.focusme.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "focusme_settings")

class AppPreferences(private val context: Context) {

    companion object {
        val KEY_START_HOUR = intPreferencesKey("start_hour")
        val KEY_END_HOUR = intPreferencesKey("end_hour")
        val KEY_QUOTA_SECONDS = intPreferencesKey("quota_seconds")
        val KEY_BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")
        val KEY_SHOW_PILL = booleanPreferencesKey("show_pill")
        val KEY_WHATSAPP_STATUS_BLOCK = booleanPreferencesKey("whatsapp_status_block")
        val KEY_ZALO_VIDEO_BLOCK = booleanPreferencesKey("zalo_video_block")
        val KEY_REACTIVE_NIGHT = booleanPreferencesKey("reactive_night_messaging")
        val KEY_MORNING_DATE = stringPreferencesKey("morning_unlocked_date")
        val KEY_REACTIVE_EXPIRY = stringPreferencesKey("reactive_pass_expiry") // Format: timestamp
        val KEY_LIFE_GOAL = stringPreferencesKey("life_goal_statement")

        const val DEFAULT_LIFE_GOAL = "Build financial freedom, master my craft & create a legendary life for my family."

        val DEFAULT_BLOCKED_PACKAGES = setOf(
            "com.twitter.android",
            "com.reddit.frontpage",
            "com.facebook.katana",
            "com.instagram.android",
            "com.zhiliaoapp.musically", // TikTok
            "com.google.android.youtube"
        )
    }

    val startHour: Flow<Int> = context.dataStore.data.map { it[KEY_START_HOUR] ?: 10 }
    val endHour: Flow<Int> = context.dataStore.data.map { it[KEY_END_HOUR] ?: 21 }
    val quotaSeconds: Flow<Int> = context.dataStore.data.map { it[KEY_QUOTA_SECONDS] ?: 300 }
    val blockedPackages: Flow<Set<String>> = context.dataStore.data.map { it[KEY_BLOCKED_PACKAGES] ?: DEFAULT_BLOCKED_PACKAGES }
    val showPill: Flow<Boolean> = context.dataStore.data.map { it[KEY_SHOW_PILL] ?: true }
    val whatsappStatusBlock: Flow<Boolean> = context.dataStore.data.map { it[KEY_WHATSAPP_STATUS_BLOCK] ?: true }
    val zaloVideoBlock: Flow<Boolean> = context.dataStore.data.map { it[KEY_ZALO_VIDEO_BLOCK] ?: true }
    val reactiveNight: Flow<Boolean> = context.dataStore.data.map { it[KEY_REACTIVE_NIGHT] ?: true }
    val morningUnlockedDate: Flow<String> = context.dataStore.data.map { it[KEY_MORNING_DATE] ?: "" }
    val lifeGoal: Flow<String> = context.dataStore.data.map { it[KEY_LIFE_GOAL] ?: DEFAULT_LIFE_GOAL }

    suspend fun setMorningUnlockedToday() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        context.dataStore.edit { it[KEY_MORNING_DATE] = today }
    }

    suspend fun setLifeGoal(goal: String) {
        context.dataStore.edit { it[KEY_LIFE_GOAL] = goal }
    }

    suspend fun updateBlockedPackages(packages: Set<String>) {
        context.dataStore.edit { it[KEY_BLOCKED_PACKAGES] = packages }
    }

    suspend fun setShowPill(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_PILL] = show }
    }
}
