package com.zbrowser.app

import android.webkit.WebView

/**
 * Represents a single browser tab with a direct reference to its WebView.
 * Eliminates the fragile index-based coupling between tabs and WebViews.
 */
data class BrowserTab(
    val id: Int,
    var title: String = "",
    var url: String = "",
    var webView: WebView? = null,
    var isDesktopMode: Boolean = false,
    var isFrozen: Boolean = false,
    var savedScrollY: Int = 0
) {
    /** Snapshot of tab state for persistence across config changes. */
    data class Snapshot(
        val id: Int,
        val title: String,
        val url: String,
        val isDesktopMode: Boolean,
        val savedScrollY: Int
    )

    fun toSnapshot(): Snapshot = Snapshot(id, title, url, isDesktopMode, savedScrollY)
}
