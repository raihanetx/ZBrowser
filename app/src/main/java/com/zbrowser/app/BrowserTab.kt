package com.zbrowser.app

import android.webkit.WebView

/**
 * Represents a single browser tab.
 * Holds a direct reference to its WebView, eliminating fragile index-based coupling.
 * The WebView is nullable to support state preservation during tab lifecycle transitions.
 */
data class BrowserTab(
    val id: Int,
    var title: String = "New Tab",
    var url: String = "",
    var webView: WebView? = null,
    var isDesktopMode: Boolean = false
)
