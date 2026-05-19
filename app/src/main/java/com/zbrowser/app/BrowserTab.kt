package com.zbrowser.app

import android.webkit.WebView

/**
 * Represents a single browser tab.
 * Holds a direct reference to its WebView, eliminating fragile index-based coupling.
 * The WebView is nullable to support state preservation during tab lifecycle transitions
 * and aggressive memory trimming (WebViewPool release).
 *
 * v4.0 FIX: Added needsWebViewRecreation flag — set when onTrimMemory ejects the
 * WebView so the Activity can recreate it when the user switches back to this tab.
 */
data class BrowserTab(
    val id: Int,
    var title: String = "New Tab",
    var url: String = "",
    var webView: WebView? = null,
    var isDesktopMode: Boolean = false,
    var needsWebViewRecreation: Boolean = false
)
