package com.zbrowser.app

import android.webkit.WebView
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages browser tab lifecycle: creation, switching, closing, and memory management.
 * Each tab directly holds its WebView reference, eliminating fragile index-based coupling.
 * Injected as a singleton via Hilt.
 */
@Singleton
class TabManager @Inject constructor() {

    private val _tabs = mutableListOf<BrowserTab>()
    val tabs: List<BrowserTab> get() = _tabs.toList()

    private var _activeTabId: Int = -1
    val activeTabId: Int get() = _activeTabId

    private var _nextTabId: Int = 1
    val nextTabId: Int get() = _nextTabId

    companion object {
        const val MAX_TABS = 20
        const val HOME_URL = "https://www.google.com"
    }

    fun addTab(webView: WebView, url: String = HOME_URL, isDesktopMode: Boolean = false): BrowserTab? {
        if (_tabs.size >= MAX_TABS) return null

        val tab = BrowserTab(
            id = _nextTabId++,
            url = url,
            webView = webView,
            isDesktopMode = isDesktopMode
        )
        _tabs.add(tab)
        return tab
    }

    fun switchToTab(tabId: Int): BrowserTab? {
        val tab = _tabs.find { it.id == tabId } ?: return null
        getActiveTab()?.webView?.onPause()
        _activeTabId = tabId
        tab.webView?.onResume()
        return tab
    }

    fun closeTab(tabId: Int): BrowserTab? {
        val tab = _tabs.find { it.id == tabId } ?: return null
        val idx = _tabs.indexOf(tab)
        tab.webView?.destroy()
        _tabs.removeAt(idx)

        if (_tabs.isEmpty()) return null
        val nextIdx = minOf(idx, _tabs.size - 1)
        return _tabs[nextIdx]
    }

    fun closeAllTabs() {
        _tabs.forEach { it.webView?.destroy() }
        _tabs.clear()
        _activeTabId = -1
    }

    fun getActiveTab(): BrowserTab? = _tabs.find { it.id == _activeTabId }
    fun getTabForWebView(webView: WebView): BrowserTab? = _tabs.find { it.webView === webView }
    fun getTab(tabId: Int): BrowserTab? = _tabs.find { it.id == tabId }
    fun indexOf(tab: BrowserTab): Int = _tabs.indexOf(tab)
    fun destroyAll() { _tabs.forEach { it.webView?.destroy() } }
}
