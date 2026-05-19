package com.zbrowser.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

/**
 * Application class for ZBrowser v4.0.
 *
 * Initializes:
 * - Crash reporter (BEFORE WebView pool, so crashes during init are caught)
 * - WebView pool for near-instant tab creation (includes data directory suffix)
 * - BookmarkManager migration from legacy EncryptedSharedPreferences
 *
 * Uses @HiltAndroidApp for proper Hilt initialization.
 */
@HiltAndroidApp
class ZBrowserApp : Application() {

    @Inject lateinit var bookmarkManager: BookmarkManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // H6 FIX: Initialize crash reporter FIRST so it catches any subsequent init crashes
        CrashReporter.init(this)

        // Initialize WebView pool — sets data directory suffix for Android P+ internally
        WebViewPool.init(this)

        // Migrate legacy bookmarks to Room in background (non-blocking)
        appScope.launch(Dispatchers.IO) {
            bookmarkManager.migrateIfNeeded()
        }
    }

    override fun onTerminate() {
        appScope.cancel()
        super.onTerminate()
    }
}
