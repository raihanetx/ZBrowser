package com.zbrowser.app

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Application class for ZBrowser.
 * Initializes global state: crash reporting, SharedPreferences, etc.
 */
class ZBrowserApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize default SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        // Initialize crash reporting
        CrashReporter.init(this)
    }

    companion object {
        lateinit var instance: ZBrowserApp
            private set
        lateinit var prefs: SharedPreferences
            private set
    }
}
