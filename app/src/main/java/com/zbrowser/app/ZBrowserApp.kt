package com.zbrowser.app

import android.app.Application
import android.webkit.WebView
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

/**
 * Application class for ZBrowser v3.2.
 *
 * Initializes:
 * - WebView pool for near-instant tab creation
 * - Crash reporting (local file-based)
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

        // Initialize WebView pool — seed one WebView for instant first tab
        WebViewPool.init(this)

        // Initialize crash reporting
        CrashReporter.init(this)

        // Migrate legacy bookmarks to Room in background (non-blocking)
        appScope.launch(Dispatchers.IO) {
            bookmarkManager.migrateIfNeeded()
        }

        // Set WebView data directory for Android P+ to prevent disk IO on main thread
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            WebView.setDataDirectorySuffix("zbrowser_webview")
        }
    }

    override fun onTerminate() {
        appScope.cancel()
        super.onTerminate()
    }
}
