package com.zbrowser.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.zbrowser.app.databinding.ActivityMainBinding

/**
 * Main browser activity — fully refactored from 638-line God Object into clean architecture.
 *
 * Architecture:
 * - BrowserViewModel: state preservation across config changes
 * - TabManager: tab/WebView lifecycle with direct references (no fragile index coupling)
 * - BookmarkManager: encrypted bookmark storage (EncryptedSharedPreferences)
 * - BrowserWebViewClient: URL interception, security, XSS-safe error pages
 * - BrowserWebChromeClient: progress updates, new window handling
 * - SecurityUtils: HTML escaping, URL validation, intent URL sanitization
 *
 * Fixes applied (22 total from expert review):
 * CRITICAL: XSS in error page, usesCleartextTraffic=false, intent:// scheme blocked
 * HIGH: God Object refactored, direct WebView refs, state preservation, encrypted bookmarks,
 *        URL validation, string extraction, ViewBinding
 * MEDIUM: Single desktop mode source of truth, dynamic SSL icon, visible TabLayout,
 *          bookmark management UI, tab limit, resource shrinking, CI linting
 * LOW: AtomicInteger removed, updated UA string (Chrome 131), tight ProGuard rules
 */
class MainActivity : AppCompatActivity(), BrowserWebViewClient.Callback {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: BrowserViewModel by viewModels()
    private lateinit var tabManager: TabManager
    private lateinit var bookmarkManager: BookmarkManager
    private var mobileUserAgent: String? = null
    private var currentWebView: WebView? = null

    // === LIFECYCLE ===

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tabManager = TabManager()
        bookmarkManager = BookmarkManager(this)

        setupToolbar()
        setupUrlBar()
        setupBottomBar()
        setupSwipeRefresh()
        setupTabLayout()

        // Restore state or create first tab
        if (savedInstanceState != null) {
            restoreTabs()
        } else {
            addNewTab(TabManager.HOME_URL)
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    override fun onResume() {
        super.onResume()
        currentWebView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        currentWebView?.onPause()
        // Save state for preservation across config changes
        viewModel.saveTabStates(tabManager.tabs, tabManager.activeTabId, tabManager.nextTabId)
    }

    override fun onDestroy() {
        tabManager.destroyAll()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && currentWebView?.canGoBack() == true) {
            currentWebView?.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // === SETUP ===

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupUrlBar() {
        binding.urlBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH) {
                val input = binding.urlBar.text.toString().trim()
                if (input.isNotEmpty()) loadUrl(processInput(input))
                true
            } else false
        }
        binding.urlBar.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) binding.urlBar.selectAll() }
    }

    private fun setupBottomBar() {
        binding.btnBack.setOnClickListener { goBack() }
        binding.btnForward.setOnClickListener { goForward() }
        binding.btnRefresh.setOnClickListener { refresh() }
        binding.btnHome.setOnClickListener { loadUrl(TabManager.HOME_URL) }
        binding.btnTabs.setOnClickListener { showTabSwitcher() }
        binding.btnMenu.setOnClickListener { showBrowserMenu() }
        binding.fabNewTab.setOnClickListener { addNewTab(TabManager.HOME_URL) }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorPrimary))
        binding.swipeRefresh.setOnRefreshListener { refresh() }
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val i = tab.position
                val tabs = tabManager.tabs
                if (i < tabs.size) switchToTab(tabs[i].id)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    // === WEBVIEW CREATION ===

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(desktopMode: Boolean): WebView {
        val webView = WebView(this)
        webView.layoutParams = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        )

        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.setSupportZoom(true)
        s.builtInZoomControls = true
        s.displayZoomControls = false
        s.setSupportMultipleWindows(true)
        s.javaScriptCanOpenWindowsAutomatically = true
        s.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        s.cacheMode = WebSettings.LOAD_DEFAULT

        // Security: Disable file access from web content
        s.allowFileAccess = false
        s.allowContentAccess = false
        s.loadsImagesAutomatically = true

        // Cache the default mobile UA from the first WebView
        if (mobileUserAgent == null) {
            mobileUserAgent = s.userAgentString
        }

        BrowserWebViewClient.applyModeSettings(s, desktopMode, mobileUserAgent)

        // WebViewClient with context and tab lookup
        val wvClient = BrowserWebViewClient(this) { webView ->
            tabManager.getTabForWebView(webView)
        }
        wvClient.callback = this
        webView.webViewClient = wvClient

        // WebChromeClient with functional callbacks
        webView.webChromeClient = BrowserWebChromeClient(
            onProgressChanged = { newProgress ->
                binding.progressBar.progress = newProgress
                if (newProgress == 100) binding.progressBar.visibility = View.GONE
            },
            onNewWindowRequested = { resultMsg ->
                handleNewWindow(resultMsg)
            }
        )

        return webView
    }

    /**
     * Handle new window requests (target="_blank" links).
     * Creates a temporary WebView to intercept the URL, validates it, then opens in new tab.
     */
    private fun handleNewWindow(resultMsg: android.os.Message?) {
        val newWebView = WebView(this)
        newWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(wv: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (SecurityUtils.isUrlSafe(url)) {
                    runOnUiThread { addNewTab(url) }
                }
                wv?.destroy()
                return true
            }
        }

        val transport = resultMsg?.obj as? WebView.WebViewTransport
        transport?.webView = newWebView
        resultMsg?.sendToTarget()
    }

    // === BrowserWebViewClient.Callback ===

    override fun onPageLoadStarted(url: String, isDesktopMode: Boolean) {
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.progress = 0

        val tab = tabManager.getActiveTab()
        if (tab != null) {
            tab.url = url
            if (tab.id == tabManager.activeTabId) {
                binding.urlBar.setText(url)
            }
        }
    }

    override fun onPageLoadFinished(title: String, url: String, isDesktopMode: Boolean) {
        binding.progressBar.visibility = View.GONE
        binding.swipeRefresh.isRefreshing = false

        val tab = tabManager.getActiveTab()
        tab?.let { t ->
            t.title = title
            t.url = url
            val idx = tabManager.indexOf(t)
            if (idx >= 0 && idx < binding.tabLayout.tabCount) {
                binding.tabLayout.getTabAt(idx)?.text = t.title
            }
            if (t.id == tabManager.activeTabId) {
                binding.urlBar.setText(url)
            }
        }
        updateNavigationButtons()
        updateSslIcon(url)
    }

    override fun onPageLoadError(errorMsg: String, url: String) {
        currentWebView?.let { wv ->
            val safeErrorPage = SecurityUtils.buildErrorPage(errorMsg, url)
            wv.loadDataWithBaseURL(null, safeErrorPage, "text/html", "UTF-8", null)
        }
        binding.progressBar.visibility = View.GONE
        binding.swipeRefresh.isRefreshing = false
    }

    override fun onSslError(handler: SslErrorHandler, error: SslError) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.ssl_error_title)
            .setMessage(getString(R.string.ssl_error_message) + "\n\nError: ${error.toString()}")
            .setPositiveButton(R.string.proceed) { _, _ -> handler.proceed() }
            .setNegativeButton(R.string.cancel) { _, _ -> handler.cancel() }
            .setOnCancelListener { handler.cancel() }
            .show()
    }

    // === TABS ===

    @SuppressLint("SetJavaScriptEnabled")
    private fun addNewTab(url: String = TabManager.HOME_URL) {
        val isDesktop = tabManager.getActiveTab()?.isDesktopMode ?: false
        val webView = createWebView(isDesktop)
        val tab = tabManager.addTab(webView, url, isDesktop)

        if (tab == null) {
            webView.destroy()
            Toast.makeText(this, R.string.tab_limit_reached, Toast.LENGTH_SHORT).show()
            return
        }

        binding.tabLayout.addTab(binding.tabLayout.newTab().apply { text = getString(R.string.new_tab) }, true)
        webView.loadUrl(url)
        switchToTab(tab.id)
        updateTabCount()
    }

    private fun switchToTab(tabId: Int) {
        val tab = tabManager.switchToTab(tabId) ?: return

        binding.webViewContainer.removeAllViews()
        tab.webView?.let { wv ->
            binding.webViewContainer.addView(wv)
            currentWebView = wv
            BrowserWebViewClient.applyModeSettings(wv.settings, tab.isDesktopMode, mobileUserAgent)
        }

        binding.urlBar.setText(tab.url)
        updateSslIcon(tab.url)

        val idx = tabManager.indexOf(tab)
        if (idx >= 0 && idx < binding.tabLayout.tabCount) binding.tabLayout.getTabAt(idx)?.select()
        updateNavigationButtons()
    }

    private fun closeTab(tabId: Int) {
        val closedIdx = tabManager.tabs.indexOf(tabManager.getTab(tabId))
        val nextTab = tabManager.closeTab(tabId)

        if (nextTab == null) {
            binding.tabLayout.removeAllTabs()
            binding.webViewContainer.removeAllViews()
            currentWebView = null
            addNewTab(TabManager.HOME_URL)
            return
        }

        if (closedIdx >= 0 && closedIdx < binding.tabLayout.tabCount) {
            binding.tabLayout.removeTabAt(closedIdx)
        }
        switchToTab(nextTab.id)
        updateTabCount()
    }

    private fun showTabSwitcher() {
        if (tabManager.tabs.isEmpty()) return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.open_tabs)
            .setItems(tabManager.tabs.map { it.title }.toTypedArray()) { _, w ->
                switchToTab(tabManager.tabs[w].id)
            }
            .setNeutralButton(R.string.close_all) { _, _ ->
                tabManager.closeAllTabs()
                binding.tabLayout.removeAllTabs()
                binding.webViewContainer.removeAllViews()
                currentWebView = null
                addNewTab(TabManager.HOME_URL)
            }
            .setPositiveButton(R.string.new_tab_button) { _, _ -> addNewTab(TabManager.HOME_URL) }
            .show()
    }

    private fun updateTabCount() {
        binding.btnTabs.text = tabManager.tabs.size.toString()
    }

    // === NAVIGATION ===

    private fun goBack() { currentWebView?.let { if (it.canGoBack()) it.goBack() } }
    private fun goForward() { currentWebView?.let { if (it.canGoForward()) it.goForward() } }
    private fun refresh() { currentWebView?.reload(); binding.swipeRefresh.isRefreshing = false }
    private fun loadUrl(url: String) { currentWebView?.loadUrl(url); binding.urlBar.setText(url) }

    private fun updateNavigationButtons() {
        binding.btnBack.alpha = if (currentWebView?.canGoBack() == true) 1.0f else 0.4f
        binding.btnForward.alpha = if (currentWebView?.canGoForward() == true) 1.0f else 0.4f
    }

    /**
     * MEDIUM FIX: Dynamic SSL icon - shows lock (green) for HTTPS, unlock (red) for HTTP
     */
    private fun updateSslIcon(url: String) {
        val isSecure = url.startsWith("https://")
        with(binding.sslIcon) {
            setImageResource(if (isSecure) R.drawable.ic_lock else R.drawable.ic_lock_open)
            imageTintList = ContextCompat.getColorStateList(
                this@MainActivity,
                if (isSecure) R.color.ssl_secure else R.color.ssl_insecure
            )
            contentDescription = getString(
                if (isSecure) R.string.ssl_secure_status else R.string.ssl_not_secure_status
            )
        }
    }

    // === DESKTOP MODE ===

    @SuppressLint("SetJavaScriptEnabled")
    private fun toggleDesktopMode() {
        val tab = tabManager.getActiveTab() ?: return
        tab.isDesktopMode = !tab.isDesktopMode
        val isDesktop = tab.isDesktopMode

        currentWebView?.let { wv ->
            BrowserWebViewClient.applyModeSettings(wv.settings, isDesktop, mobileUserAgent)
            wv.setInitialScale(0)
            wv.reload()
            Toast.makeText(
                this,
                if (isDesktop) R.string.desktop_mode_on else R.string.mobile_mode_on,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // === SYSTEM BROWSER ===

    private fun openInSystemBrowser() {
        currentWebView?.url?.let { url ->
            if (SecurityUtils.isUrlSafe(url)) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    }

    // === MENU ===

    private fun showBrowserMenu() {
        val isDesktop = tabManager.getActiveTab()?.isDesktopMode ?: false
        val isBookmarked = currentWebView?.url?.let { bookmarkManager.isBookmarked(it) } ?: false

        val opts = arrayOf(
            if (isDesktop) getString(R.string.switch_to_mobile) else getString(R.string.switch_to_desktop),
            getString(R.string.open_in_system_browser),
            if (isBookmarked) getString(R.string.remove_bookmark) else getString(R.string.bookmark),
            getString(R.string.bookmarks),
            getString(R.string.history),
            getString(R.string.share),
            getString(R.string.find_in_page),
            getString(R.string.settings)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.browser_menu)
            .setItems(opts) { _, w ->
                when (w) {
                    0 -> toggleDesktopMode()
                    1 -> openInSystemBrowser()
                    2 -> toggleBookmark()
                    3 -> showBookmarks()
                    4 -> showHistory()
                    5 -> sharePage()
                    6 -> findInPage()
                    7 -> showSettings()
                }
            }
            .show()
    }

    // === BOOKMARKS ===

    private fun toggleBookmark() {
        val url = currentWebView?.url ?: return
        val title = currentWebView?.title ?: url
        if (bookmarkManager.isBookmarked(url)) {
            bookmarkManager.removeBookmark(url)
            Toast.makeText(this, R.string.bookmark_removed, Toast.LENGTH_SHORT).show()
        } else {
            bookmarkManager.addBookmark(url, title)
            Toast.makeText(this, getString(R.string.bookmarked) + ": $title", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBookmarks() {
        val bookmarks = bookmarkManager.getAllBookmarks()
        if (bookmarks.isEmpty()) {
            Toast.makeText(this, R.string.no_bookmarks, Toast.LENGTH_SHORT).show()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.bookmarks)
            .setItems(bookmarks.map { it.first }.toTypedArray()) { _, w ->
                loadUrl(bookmarks[w].second)
            }
            .show()
    }

    // === HISTORY ===

    private fun showHistory() {
        val h = currentWebView?.copyBackForwardList()
        if (h == null || h.size == 0) {
            Toast.makeText(this, R.string.no_history, Toast.LENGTH_SHORT).show()
            return
        }
        val items = (0 until h.size).map { h.getItemAtIndex(it).title ?: h.getItemAtIndex(it).url }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.history)
            .setItems(items.toTypedArray()) { _, w -> loadUrl(h.getItemAtIndex(w).url) }
            .show()
    }

    // === SHARE ===

    private fun sharePage() {
        currentWebView?.url?.let { url ->
            val title = currentWebView?.title ?: ""
            startActivity(Intent.createChooser(Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "$title\n$url")
                type = "text/plain"
            }, getString(R.string.share_via)))
        }
    }

    // === FIND IN PAGE ===

    private fun findInPage() {
        val input = EditText(this).apply {
            hint = getString(R.string.search_on_page)
            setPadding(48, 24, 48, 24)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.find_in_page)
            .setView(input)
            .setPositiveButton(R.string.find) { _, _ ->
                currentWebView?.findAllAsync(input.text.toString())
            }
            .setNeutralButton(R.string.clear) { _, _ -> currentWebView?.clearMatches() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // === SETTINGS ===

    private fun showSettings() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings)
            .setItems(arrayOf(getString(R.string.clear_browsing_data), getString(R.string.about_zbrowser))) { _, w ->
                when (w) { 0 -> clearData(); 1 -> showAbout() }
            }.show()
    }

    private fun clearData() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_browsing_data)
            .setMessage(R.string.clear_data_message)
            .setPositiveButton(R.string.clear_button) { _, _ ->
                binding.webViewContainer.removeAllViews()
                tabManager.tabs.forEach { it.webView?.clearCache(true) }
                tabManager.closeAllTabs()
                binding.tabLayout.removeAllTabs()
                currentWebView = null
                android.webkit.CookieManager.getInstance().removeAllCookies(null)
                WebView.clearClientCertPreferences(null)
                addNewTab(TabManager.HOME_URL)
                Toast.makeText(this, R.string.cleared, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAbout() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.about_zbrowser)
            .setMessage(R.string.about_message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    // === INPUT ===

    private fun processInput(input: String): String = when {
        input.startsWith("http://") || input.startsWith("https://") -> input
        input.contains(".") && !input.contains(" ") -> "https://$input"
        else -> "https://www.google.com/search?q=${Uri.encode(input)}"
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                val url = uri.toString()
                if (SecurityUtils.isUrlSafe(url)) {
                    addNewTab(url)
                }
            }
        }
    }

    // === STATE PRESERVATION ===

    private fun restoreTabs() {
        val tabStates = viewModel.getTabStates()
        if (tabStates.isNullOrEmpty()) {
            addNewTab(TabManager.HOME_URL)
            return
        }

        for (state in tabStates) {
            val webView = createWebView(state.isDesktopMode)
            val tab = tabManager.addTab(webView, state.url, state.isDesktopMode)
            if (tab != null) {
                tab.title = state.title
                binding.tabLayout.addTab(binding.tabLayout.newTab().apply { text = state.title }, false)
                webView.loadUrl(state.url)
            }
        }

        val activeId = viewModel.getActiveTabId()
        if (activeId > 0) {
            switchToTab(activeId)
        }
    }
}
