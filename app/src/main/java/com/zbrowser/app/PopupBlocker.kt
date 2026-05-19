package com.zbrowser.app

import android.content.SharedPreferences
import android.webkit.WebView

/**
 * Popup blocker that controls whether JavaScript window.open() calls are allowed.
 * When enabled, all popup windows (window.open, target="_blank" without user gesture)
 * are silently blocked. User-initiated clicks still open new tabs.
 *
 * The blocker setting is persisted in SharedPreferences so it survives app restarts.
 */
class PopupBlocker(private val prefs: SharedPreferences) {

    companion object {
        private const val KEY_POPUP_BLOCKER_ENABLED = "popup_blocker_enabled"
        const val DEFAULT_ENABLED = true
    }

    /** Whether the popup blocker is currently active */
    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_POPUP_BLOCKER_ENABLED, DEFAULT_ENABLED)
        set(value) = prefs.edit().putBoolean(KEY_POPUP_BLOCKER_ENABLED, value).apply()

    /**
     * Apply popup blocker settings to a WebView.
     * When blocker is ON: javaScriptCanOpenWindowsAutomatically = false
     * When blocker is OFF: javaScriptCanOpenWindowsAutomatically = true
     */
    fun applyToWebView(webView: WebView) {
        webView.settings.javaScriptCanOpenWindowsAutomatically = !isEnabled
    }

    /**
     * Check if a new window request should be blocked.
     * If popup blocker is enabled, only user-gesture-initiated windows are allowed.
     * Since we can't reliably detect user gestures in onCreateWindow,
     * when blocker is ON we block ALL programmatic popups.
     */
    fun shouldBlockPopup(): Boolean {
        return isEnabled
    }
}
