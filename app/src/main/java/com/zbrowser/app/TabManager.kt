package com.zbrowser.app

import android.webkit.WebView

/**
 * Manages browser tab lifecycle: creation, switching, closing, and memory management.
 * Each tab directly holds its WebView reference, eliminating fragile index-based coupling.
 * Inactive tab WebViews are paused to save CPU/memory while keeping page state alive.
 */
class TabManager {

    private val _tabs = mutableListOf<BrowserTab>()
    val tabs: List<BrowserTab> get() = _tabs.toList()

    private var _activeTabId: Int = -1
    val activeTabId: Int get() = _activeTabId

    private var nextTabId: Int = 1

    /** Maximum number of tabs allowed to prevent excessive memory usage */
    companion object {
        const val MAX_TABS = 20
        const val HOME_URL = "https://www.google.com"
    }

    /**
     * Create a new tab with the given WebView and URL.
     * Returns the created tab, or null if max tabs reached.
     */
    fun addTab(webView: WebView, url: String = HOME_URL, isDesktopMode: Boolean = false): BrowserTab? {
        if (_tabs.size >= MAX_TABS) return null

        val tab = BrowserTab(
            id = nextTabId++,
            url = url,
            webView = webView,
            isDesktopMode = isDesktopMode
        )
        _tabs.add(tab)
        return tab
    }

    /**
     * Switch to a tab by ID. Pauses the previous active WebView and resumes the new one.
     * Returns the new active tab, or null if not found.
     */
    fun switchToTab(tabId: Int): BrowserTab? {
        val tab = _tabs.find { it.id == tabId } ?: return null

        // Pause the previously active WebView to save CPU/battery
        getActiveTab()?.webView?.onPause()

        _activeTabId = tabId

        // Resume the new active WebView
        tab.webView?.onResume()

        return tab
    }

    /**
     * Close a tab by ID. Destroys its WebView and removes from the list.
     * Returns the tab to switch to next, or null if no tabs remain.
     */
    fun closeTab(tabId: Int): BrowserTab? {
        val tab = _tabs.find { it.id == tabId } ?: return null
        val idx = _tabs.indexOf(tab)

        // Destroy the WebView
        tab.webView?.destroy()

        _tabs.removeAt(idx)

        if (_tabs.isEmpty()) return null

        // Switch to the nearest tab
        val nextIdx = minOf(idx, _tabs.size - 1)
        return _tabs[nextIdx]
    }

    /**
     * Close all tabs, destroying all WebViews.
     */
    fun closeAllTabs() {
        _tabs.forEach { it.webView?.destroy() }
        _tabs.clear()
        _activeTabId = -1
    }

    /**
     * Get the currently active tab.
     */
    fun getActiveTab(): BrowserTab? {
        return _tabs.find { it.id == _activeTabId }
    }

    /**
     * Get a tab by its WebView instance.
     */
    fun getTabForWebView(webView: WebView): BrowserTab? {
        return _tabs.find { it.webView === webView }
    }

    /**
     * Get a tab by ID.
     */
    fun getTab(tabId: Int): BrowserTab? {
        return _tabs.find { it.id == tabId }
    }

    /**
     * Get the index of a tab in the list.
     */
    fun indexOf(tab: BrowserTab): Int {
        return _tabs.indexOf(tab)
    }

    /**
     * Destroy all WebViews (for Activity.onDestroy).
     */
    fun destroyAll() {
        _tabs.forEach { it.webView?.destroy() }
    }
}
