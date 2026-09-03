package com.focusme.app

import android.app.Application
import com.focusme.app.data.database.AppDatabase
import com.focusme.app.data.preferences.AppPreferences

class FocusMeApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val preferences by lazy { AppPreferences(this) }

    companion object {
        lateinit var instance: FocusMeApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
