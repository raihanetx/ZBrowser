package com.zbrowser.app

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.widget.FrameLayout
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages browser tab lifecycle: creation, switching, closing, and freezing.
 * Eliminates the fragile index-based coupling by using direct WebView references.
 */
class TabManager(private val context: Context) {

    private val _tabs = mutableListOf<BrowserTab>()
    val tabs: List<BrowserTab> get() _tabs

    private val tabIdGenerator = AtomicInteger(0)
    var activeTabId: Int = -1
        private set

    var onTabAdded: ((BrowserTab) -> Unit)? = null
    var onTabRemoved: ((BrowserTab) -> Unit)? = null
    var onTabsCleared: (() -> Unit)? = null
    var onActiveTabChanged: ((BrowserTab?) -> Unit)? = null

    val activeTab: BrowserTab?
        get() = _tabs.find { it.id == activeTabId }

    val activeWebView: WebView?
        get() = activeTab?.webView

    val tabCount: Int
        get() = _tabs.size

    /**
     * Create a new tab with its own WebView.
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun addTab(url: String = NavigationController.HOME_URL, desktopMode: Boolean = false): BrowserTab {
        val tabId = tabIdGenerator.incrementAndGet()
        val webView = BrowserWebViewFactory.create(context, desktopMode)
        val tab = BrowserTab(
            id = tabId,
            url = url,
            webView = webView,
            isDesktopMode = desktopMode
        )
        _tabs.add(tab)
        onTabAdded?.invoke(tab)
        switchToTab(tabId)
        webView.loadUrl(url)
        return tab
    }

    /**
     * Switch to a specific tab by ID.
     * Freezes the previously active tab and unfreezes the new one.
     */
    fun switchToTab(tabId: Int) {
        val tab = _tabs.find { it.id == tabId } ?: return

        // Freeze the previous active tab
        activeTab?.let { freezeTab(it) }

        activeTabId = tabId

        // Unfreeze the new active tab
        unfreezeTab(tab)

        onActiveTabChanged?.invoke(tab)
    }

    /**
     * Close a specific tab.
     * If the closed tab was active, switch to the nearest tab.
     */
    fun closeTab(tabId: Int) {
        val tab = _tabs.find { it.id == tabId } ?: return
        val idx = _tabs.indexOf(tab)
        val wasActive = tab.id == activeTabId

        // Destroy the WebView
        tab.webView?.destroy()
        _tabs.removeAt(idx)
        onTabRemoved?.invoke(tab)

        if (_tabs.isEmpty()) {
            activeTabId = -1
            onTabsCleared?.invoke()
            return
        }

        if (wasActive) {
            val newIdx = minOf(idx, _tabs.size - 1)
            switchToTab(_tabs[newIdx].id)
        }
    }

    /**
     * Close all tabs and reset.
     */
    fun closeAllTabs() {
        _tabs.forEach { it.webView?.destroy() }
        _tabs.clear()
        activeTabId = -1
        onTabsCleared?.invoke()
    }

    /**
     * Freeze a tab: pause its WebView to save resources.
     */
    private fun freezeTab(tab: BrowserTab) {
        if (tab.isFrozen) return
        tab.webView?.let { wv ->
            tab.savedScrollY = wv.scrollY
            wv.onPause()
        }
        tab.isFrozen = true
    }

    /**
     * Unfreeze a tab: resume its WebView and restore scroll position.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun unfreezeTab(tab: BrowserTab) {
        if (!tab.isFrozen) return
        tab.webView?.let { wv ->
            wv.onResume()
            BrowserWebViewFactory.applyModeSettings(wv.settings, tab.isDesktopMode)
        }
        tab.isFrozen = false
    }

    /**
     * Find the tab that owns a given WebView.
     * O(n) lookup but with direct reference comparison — much safer than index.
     */
    fun findTabForWebView(webView: WebView): BrowserTab? {
        return _tabs.find { it.webView === webView }
    }

    /**
     * Pause all WebViews (called from Activity.onPause).
     */
    fun pauseAll() {
        _tabs.forEach { it.webView?.onPause() }
    }

    /**
     * Resume the active WebView (called from Activity.onResume).
     */
    fun resumeActive() {
        activeTab?.webView?.onResume()
    }

    /**
     * Destroy all WebViews (called from Activity.onDestroy).
     */
    fun destroyAll() {
        _tabs.forEach { it.webView?.destroy() }
        _tabs.clear()
        activeTabId = -1
    }
}
