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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.zbrowser.app.data.BookmarkDao
import com.zbrowser.app.data.BookmarkEntity
import com.zbrowser.app.data.HistoryDao
import com.zbrowser.app.data.HistoryEntity
import com.zbrowser.app.databinding.ActivityMainBinding
import com.zbrowser.app.di.AppModule
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Main browser activity — ZBrowser v3.0 with all 10 features integrated.
 *
 * Architecture:
 * - Hilt DI: dependency injection for all managers
 * - Room DB: bookmarks & history persistence
 * - Coroutines: async database operations
 * - Ad Blocker: request interception + CSS injection
 * - Popup Blocker: window.open() blocking
 * - Download Manager: system DownloadManager integration
 * - Crash Reporter: local crash log capture
 * - Permission Manager: runtime geolocation/camera/mic
 * - Multi-Window: resizeableActivity + configChanges
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), BrowserWebViewClient.Callback {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: BrowserViewModel by viewModels()

    @Inject lateinit var tabManager: TabManager
    @Inject lateinit var adBlocker: AdBlocker
    @Inject lateinit var popupBlocker: PopupBlocker
    @Inject lateinit var downloadManagerHelper: DownloadManagerHelper
    @Inject lateinit var permissionManager: PermissionManager
    @Inject lateinit var bookmarkDao: BookmarkDao
    @Inject lateinit var historyDao: HistoryDao

    private var mobileUserAgent: String? = null
    private var currentWebView: WebView? = null

    // === LIFECYCLE ===

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupUrlBar()
        setupBottomBar()
        setupSwipeRefresh()
        setupTabLayout()

        // Check for crash logs from previous session
        checkForCrash()

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

    /**
     * FEATURE 10: Forward permission results to PermissionManager.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
        s.allowFileAccess = false
        s.allowContentAccess = false
        s.loadsImagesAutomatically = true

        if (mobileUserAgent == null) {
            mobileUserAgent = s.userAgentString
        }

        BrowserWebViewClient.applyModeSettings(s, desktopMode, mobileUserAgent)

        // FEATURE 1: Apply popup blocker settings
        popupBlocker.applyToWebView(webView)

        // WebViewClient with all features
        val wvClient = BrowserWebViewClient(
            context = this,
            tabLookup = { webView -> tabManager.getTabForWebView(webView) },
            adBlocker = adBlocker,
            popupBlocker = popupBlocker,
            historyDao = historyDao,
            appScope = lifecycleScope
        )
        wvClient.callback = this
        webView.webViewClient = wvClient

        // WebChromeClient with popup blocker, permissions, and download support
        webView.webChromeClient = BrowserWebChromeClient(
            onProgressChanged = { newProgress ->
                binding.progressBar.progress = newProgress
                if (newProgress == 100) binding.progressBar.visibility = View.GONE
            },
            onNewWindowRequested = { resultMsg -> handleNewWindow(resultMsg) },
            popupBlocker = popupBlocker,
            permissionManager = permissionManager,
            onPopupBlocked = {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, R.string.popup_blocked_toast, Toast.LENGTH_SHORT).show()
                }
            }
        )

        // FEATURE 3: Download Manager - set download listener
        webView.setDownloadListener(downloadManagerHelper.webViewDownloadListener)

        return webView
    }

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

    override fun onPopupBlocked() {
        Toast.makeText(this, R.string.popup_blocked_toast, Toast.LENGTH_SHORT).show()
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
            popupBlocker.applyToWebView(wv)
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

        lifecycleScope.launch {
            val isBookmarked = currentWebView?.url?.let {
                withContext(Dispatchers.IO) { bookmarkDao.isBookmarked(it) }
            } ?: false

            val opts = arrayOf(
                if (isDesktop) getString(R.string.switch_to_mobile) else getString(R.string.switch_to_desktop),
                getString(R.string.open_in_system_browser),
                if (isBookmarked) getString(R.string.remove_bookmark) else getString(R.string.bookmark),
                getString(R.string.bookmarks),
                getString(R.string.history),
                getString(R.string.share),
                getString(R.string.find_in_page),
                if (adBlocker.isEnabled) getString(R.string.ad_blocker_off) else getString(R.string.ad_blocker_on),
                if (popupBlocker.isEnabled) getString(R.string.popup_blocker_off) else getString(R.string.popup_blocker_on),
                getString(R.string.settings)
            )
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(R.string.browser_menu)
                .setItems(opts) { _, w ->
                    when (w) {
                        0 -> toggleDesktopMode()
                        1 -> openInSystemBrowser()
                        2 -> toggleBookmark()
                        3 -> showBookmarks()
                        4 -> showFullHistory()
                        5 -> sharePage()
                        6 -> findInPage()
                        7 -> toggleAdBlocker()
                        8 -> togglePopupBlocker()
                        9 -> showSettings()
                    }
                }
                .show()
        }
    }

    // === BOOKMARKS (Room DB) ===

    private fun toggleBookmark() {
        val url = currentWebView?.url ?: return
        val title = currentWebView?.title ?: url

        lifecycleScope.launch {
            val isBookmarked = withContext(Dispatchers.IO) { bookmarkDao.isBookmarked(url) }
            if (isBookmarked) {
                withContext(Dispatchers.IO) { bookmarkDao.deleteByUrl(url) }
                Toast.makeText(this@MainActivity, R.string.bookmark_removed, Toast.LENGTH_SHORT).show()
            } else {
                withContext(Dispatchers.IO) { bookmarkDao.insert(BookmarkEntity(url = url, title = title)) }
                Toast.makeText(this@MainActivity, getString(R.string.bookmarked) + ": $title", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showBookmarks() {
        lifecycleScope.launch {
            val bookmarks = withContext(Dispatchers.IO) { bookmarkDao.getAllBookmarksOnce() }
            if (bookmarks.isEmpty()) {
                Toast.makeText(this@MainActivity, R.string.no_bookmarks, Toast.LENGTH_SHORT).show()
                return@launch
            }
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(R.string.bookmarks)
                .setItems(bookmarks.map { it.title }.toTypedArray()) { _, w ->
                    loadUrl(bookmarks[w].url)
                }
                .show()
        }
    }

    // === HISTORY (Room DB) ===

    private fun showFullHistory() {
        lifecycleScope.launch {
            val history = withContext(Dispatchers.IO) { historyDao.getRecentHistory(100) }
            if (history.isEmpty()) {
                Toast.makeText(this@MainActivity, R.string.no_history, Toast.LENGTH_SHORT).show()
                return@launch
            }
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle(R.string.history)
                .setItems(history.map { it.title }.toTypedArray()) { _, w ->
                    loadUrl(history[w].url)
                }
                .show()
        }
    }

    private fun showHistory() {
        // Use WebView back-forward list for current tab history
        val h = currentWebView?.copyBackForwardList()
        if (h == null || h.size == 0) {
            showFullHistory()
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

    // === AD BLOCKER ===

    private fun toggleAdBlocker() {
        adBlocker.isEnabled = !adBlocker.isEnabled
        Toast.makeText(
            this,
            if (adBlocker.isEnabled) R.string.ad_blocker_on else R.string.ad_blocker_off,
            Toast.LENGTH_SHORT
        ).show()
    }

    // === POPUP BLOCKER ===

    private fun togglePopupBlocker() {
        popupBlocker.isEnabled = !popupBlocker.isEnabled
        // Re-apply to current WebView
        currentWebView?.let { popupBlocker.applyToWebView(it) }
        Toast.makeText(
            this,
            if (popupBlocker.isEnabled) R.string.popup_blocker_on else R.string.popup_blocker_off,
            Toast.LENGTH_SHORT
        ).show()
    }

    // === SETTINGS ===

    private fun showSettings() {
        val options = arrayOf(
            getString(R.string.clear_browsing_data),
            getString(R.string.crash_logs),
            getString(R.string.about_zbrowser)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings)
            .setItems(options) { _, w ->
                when (w) {
                    0 -> clearData()
                    1 -> showCrashLogs()
                    2 -> showAbout()
                }
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
                // Clear Room databases
                lifecycleScope.launch(Dispatchers.IO) {
                    historyDao.deleteAll()
                    bookmarkDao.deleteAll()
                }
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

    // === CRASH REPORTER ===

    private fun checkForCrash() {
        if (CrashReporter.hasUnreadCrash()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.crash_detected_title)
                .setMessage(R.string.crash_detected_message)
                .setPositiveButton(R.string.view_crash_log) { _, _ ->
                    showCrashLogs()
                    CrashReporter.markCrashRead()
                }
                .setNegativeButton(R.string.dismiss_crash) { _, _ ->
                    CrashReporter.markCrashRead()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun showCrashLogs() {
        val logs = CrashReporter.getAllCrashLogs()
        if (logs.isEmpty()) {
            Toast.makeText(this, R.string.no_crash_logs, Toast.LENGTH_SHORT).show()
            return
        }

        val logContents = logs.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.crash_log_title)
            .setItems(logContents) { _, w ->
                val content = logs[w].readText()
                MaterialAlertDialogBuilder(this)
                    .setTitle(logs[w].name)
                    .setMessage(content.take(5000))
                    .setPositiveButton(R.string.ok, null)
                    .setNeutralButton(R.string.share_crash_log) { _, _ ->
                        startActivity(Intent.createChooser(Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, content)
                            type = "text/plain"
                        }, getString(R.string.share_via)))
                    }
                    .show()
            }
            .setNeutralButton(R.string.clear_crash_logs) { _, _ ->
                CrashReporter.clearAllLogs()
                Toast.makeText(this, R.string.cleared, Toast.LENGTH_SHORT).show()
            }
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
