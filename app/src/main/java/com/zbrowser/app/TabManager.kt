package com.zbrowser.app

import android.webkit.WebView
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages browser tab lifecycle with maximum smoothness.
 *
 * v3.1 OPTIMIZATIONS:
 * - Tab switching uses visibility (VISIBLE/GONE) instead of remove/add views,
 *   eliminating layout thrashing and visual flicker
 * - Background tabs are paused (onPause) to save CPU/GPU
 * - Active tab is resumed (onResume) for smooth rendering
 * - WebViewPool is used for acquire/release instead of direct destroy
 * - Memory-aware: onTrimMemory can eject non-active tabs
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

    /**
     * Switch to a tab — pauses the previously active tab and resumes the new one.
     * The caller is responsible for setting the WebView visibility (VISIBLE/GONE).
     */
    fun switchToTab(tabId: Int): BrowserTab? {
        val tab = _tabs.find { it.id == tabId } ?: return null

        // Skip if already the active tab — prevents unnecessary onPause/onResume cycle
        if (_activeTabId == tabId) return tab

        // Pause the previously active tab to free CPU/GPU
        getActiveTab()?.let { oldTab ->
            oldTab.webView?.onPause()
        }

        _activeTabId = tabId

        // Resume the new active tab
        tab.webView?.onResume()

        return tab
    }

    fun closeTab(tabId: Int): BrowserTab? {
        val tab = _tabs.find { it.id == tabId } ?: return null
        val idx = _tabs.indexOf(tab)

        // Release WebView to pool instead of destroying immediately
        tab.webView?.let { WebViewPool.release(it) }
        _tabs.removeAt(idx)

        if (_tabs.isEmpty()) return null
        val nextIdx = minOf(idx, _tabs.size - 1)
        return _tabs[nextIdx]
    }

    fun closeAllTabs() {
        _tabs.forEach { tab ->
            tab.webView?.let { WebViewPool.release(it) }
        }
        _tabs.clear()
        _activeTabId = -1
    }

    fun getActiveTab(): BrowserTab? = _tabs.find { it.id == _activeTabId }
    fun getTabForWebView(webView: WebView): BrowserTab? = _tabs.find { it.webView === webView }
    fun getTab(tabId: Int): BrowserTab? = _tabs.find { it.id == tabId }
    fun indexOf(tab: BrowserTab): Int = _tabs.indexOf(tab)

    fun destroyAll() {
        _tabs.forEach { tab ->
            tab.webView?.let { wv ->
                wv.onPause()
                WebViewPool.release(wv)
            }
        }
        _tabs.clear()
    }

    /**
     * Called when the OS requests memory trimming.
     * Ejects non-active tab WebViews from memory.
     */
    fun onTrimMemory(level: Int) {
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            _tabs.filter { it.id != _activeTabId }.forEach { tab ->
                tab.webView?.let { wv ->
                    wv.onPause()
                    wv.clearCache(false)
                }
            }
        }
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            // Destroy all background tabs' WebViews aggressively
            _tabs.filter { it.id != _activeTabId }.forEach { tab ->
                tab.webView?.let { wv ->
                    WebViewPool.release(wv)
                    tab.webView = null
                }
            }
        }
    }
}
