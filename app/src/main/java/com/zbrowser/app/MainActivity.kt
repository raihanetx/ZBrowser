package com.zbrowser.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    // Views
    private lateinit var toolbar: Toolbar
    private lateinit var urlBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var tabLayout: TabLayout
    private lateinit var webViewContainer: FrameLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var bottomBar: LinearLayout
    private lateinit var btnBack: ImageView
    private lateinit var btnForward: ImageView
    private lateinit var btnRefresh: ImageView
    private lateinit var btnHome: ImageView
    private lateinit var btnTabs: TextView
    private lateinit var btnMenu: ImageView
    private lateinit var fabNewTab: FloatingActionButton
    private lateinit var sslIcon: ImageView

    // Core components
    private lateinit var tabManager: TabManager
    private lateinit var settingsRepo: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settingsRepo = SettingsRepository(this)
        tabManager = TabManager(this)

        initViews()
        setupToolbar()
        setupUrlBar()
        setupBottomBar()
        setupSwipeRefresh()
        setupTabLayout()
        setupTabManagerCallbacks()

        // Restore or create first tab
        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        } else {
            tabManager.addTab(NavigationController.HOME_URL)
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    override fun onResume() {
        super.onResume()
        tabManager.resumeActive()
    }

    override fun onPause() {
        super.onPause()
        tabManager.pauseAll()
    }

    override fun onDestroy() {
        tabManager.destroyAll()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save tab snapshots
        val snapshots = tabManager.tabs.map { it.toSnapshot() }
        val urls = ArrayList(snapshots.map { it.url })
        val titles = ArrayList(snapshots.map { it.title })
        val desktopModes = ArrayList(snapshots.map { it.isDesktopMode })
        outState.putStringArrayList("tab_urls", urls)
        outState.putStringArrayList("tab_titles", titles)
        outState.putSerializable("tab_desktop_modes", desktopModes)
        outState.putInt("active_tab_id", tabManager.activeTabId)
    }

    private fun restoreState(savedInstanceState: Bundle) {
        val urls = savedInstanceState.getStringArrayList("tab_urls") ?: return
        val titles = savedInstanceState.getStringArrayList("tab_titles") ?: return
        @Suppress("DEPRECATION")
        val desktopModes = savedInstanceState.getSerializable("tab_desktop_modes") as? ArrayList<Boolean> ?: return

        for (i in urls.indices) {
            if (i < titles.size && i < desktopModes.size) {
                val tab = tabManager.addTab(urls[i], desktopModes[i])
                tab.title = titles[i]
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val wv = tabManager.activeWebView
            if (wv?.canGoBack() == true) {
                wv.goBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    // === INIT ===

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        urlBar = findViewById(R.id.urlBar)
        progressBar = findViewById(R.id.progressBar)
        tabLayout = findViewById(R.id.tabLayout)
        webViewContainer = findViewById(R.id.webViewContainer)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        bottomBar = findViewById(R.id.bottomBar)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnHome = findViewById(R.id.btnHome)
        btnTabs = findViewById(R.id.btnTabs)
        btnMenu = findViewById(R.id.btnMenu)
        fabNewTab = findViewById(R.id.fabNewTab)
        sslIcon = findViewById(R.id.sslIcon)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupUrlBar() {
        urlBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH) {
                val input = urlBar.text.toString().trim()
                if (input.isNotEmpty()) {
                    loadUrl(NavigationController.processInput(input))
                }
                true
            } else false
        }
        urlBar.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) urlBar.selectAll() }
    }

    private fun setupBottomBar() {
        btnBack.setOnClickListener { goBack() }
        btnForward.setOnClickListener { goForward() }
        btnRefresh.setOnClickListener { refresh() }
        btnHome.setOnClickListener { loadUrl(NavigationController.HOME_URL) }
        btnTabs.setOnClickListener { showTabSwitcher() }
        btnMenu.setOnClickListener { showBrowserMenu() }
        fabNewTab.setOnClickListener { tabManager.addTab(NavigationController.HOME_URL) }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorPrimary))
        swipeRefresh.setOnRefreshListener { refresh() }
    }

    private fun setupTabLayout() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val i = tab.position
                if (i < tabManager.tabs.size) tabManager.switchToTab(tabManager.tabs[i].id)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupTabManagerCallbacks() {
        tabManager.onTabAdded = { tab ->
            tab.webView?.let { wv ->
                configureWebView(wv, tab)
                webViewContainer.addView(wv)
            }
            tabLayout.addTab(tabLayout.newTab().apply { text = tab.title.ifEmpty { getString(R.string.new_tab) } }, true)
            updateTabCount()
            updateTabLayoutVisibility()
        }

        tabManager.onTabRemoved = { tab ->
            tab.webView?.let { webViewContainer.removeView(it) }
            val idx = tabManager.tabs.indexOf(tab)
            if (idx in 0 until tabLayout.tabCount) tabLayout.removeTabAt(idx)
            updateTabCount()
            updateTabLayoutVisibility()
        }

        tabManager.onTabsCleared = {
            webViewContainer.removeAllViews()
            tabLayout.removeAllTabs()
            updateTabCount()
            updateTabLayoutVisibility()
        }

        tabManager.onActiveTabChanged = { tab ->
            if (tab == null) return@set

            webViewContainer.removeAllViews()
            tab.webView?.let { wv ->
                if (wv.parent != null) (wv.parent as FrameLayout).removeView(wv)
                webViewContainer.addView(wv)
            }

            urlBar.setText(tab.url)
            updateSslIcon(tab.url)
            val idx = tabManager.tabs.indexOf(tab)
            if (idx >= 0 && idx < tabLayout.tabCount) tabLayout.getTabAt(idx)?.select()
            updateNavigationButtons()
        }
    }

    // === WEBVIEW CONFIGURATION ===

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(webView: WebView, tab: BrowserTab) {
        webView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                val scheme = request.url.scheme?.lowercase() ?: ""

                // Handle external schemes (tel, mailto, etc.)
                val externalScheme = NavigationController.getExternalScheme(url)
                if (externalScheme != null) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, getString(R.string.cannot_open_scheme, externalScheme), Toast.LENGTH_SHORT).show()
                    }
                    return true
                }

                // SECURITY: Do NOT handle intent:// scheme — prevents package enumeration attacks
                // All http/https URLs stay inside the WebView
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                // Re-apply desktop mode settings BEFORE every page load
                view?.let { wv ->
                    val t = tabManager.findTabForWebView(wv)
                    if (t != null) {
                        BrowserWebViewFactory.applyModeSettings(wv.settings, t.isDesktopMode)
                    }
                }

                url?.let {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = 0
                    tab.url = it
                    if (tab.id == tabManager.activeTabId) {
                        urlBar.setText(it)
                        updateSslIcon(it)
                    }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.let { wv ->
                    val title = wv.title ?: ""
                    val pageUrl = url ?: ""
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false

                    // Inject viewport JavaScript
                    val t = tabManager.findTabForWebView(wv)
                    if (t?.isDesktopMode == true) {
                        wv.evaluateJavascript(BrowserWebViewFactory.DESKTOP_VIEWPORT_JS, null)
                    } else {
                        wv.evaluateJavascript(BrowserWebViewFactory.MOBILE_VIEWPORT_JS, null)
                    }

                    t?.let { tb ->
                        tb.title = if (title.isNotEmpty()) title else pageUrl
                        tb.url = pageUrl
                        val idx = tabManager.tabs.indexOf(tb)
                        if (idx >= 0 && idx < tabLayout.tabCount) {
                            tabLayout.getTabAt(idx)?.text = tb.title
                        }
                        if (tb.id == tabManager.activeTabId) {
                            urlBar.setText(pageUrl)
                        }
                    }
                    updateNavigationButtons()
                }
            }

            // FIX: Handle page load errors — HTML-escape all values to prevent XSS
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    view?.let { wv ->
                        val errorMsg = TextUtils.htmlEncode(error?.description?.toString() ?: getString(R.string.unknown_error))
                        val safeUrl = TextUtils.htmlEncode(view?.url ?: "")
                        wv.loadDataWithBaseURL(
                            null,
                            """<html><body style='display:flex;justify-content:center;align-items:center;height:100vh;font-family:sans-serif;text-align:center;color:#666'>
                            <div><h2>${getString(R.string.page_load_error)}</h2><p>$errorMsg</p><p><a href="$safeUrl">${getString(R.string.try_again)}</a></p></div>
                            </body></html>""",
                            "text/html",
                            "UTF-8",
                            null
                        )
                        progressBar.visibility = View.GONE
                        swipeRefresh.isRefreshing = false
                    }
                }
            }

            // FIX: Handle SSL errors — ask user instead of silently failing
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle(R.string.ssl_certificate_error)
                    .setMessage(getString(R.string.ssl_error_message, error?.toString() ?: getString(R.string.unknown)))
                    .setPositiveButton(R.string.proceed) { _, _ -> handler?.proceed() }
                    .setNegativeButton(R.string.cancel) { _, _ -> handler?.cancel() }
                    .setOnCancelListener { handler?.cancel() }
                    .show()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            private var lastProgress = 0

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                // Throttle progress updates to reduce UI jank
                if (newProgress - lastProgress >= 5 || newProgress == 100) {
                    progressBar.progress = newProgress
                    lastProgress = newProgress
                    if (newProgress == 100) progressBar.visibility = View.GONE
                }
            }

            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                // Create a minimal temp WebView — no JavaScript, no network
                val tempWebView = WebView(this@MainActivity)
                tempWebView.settings.javaScriptEnabled = false
                tempWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(wv: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: return false
                        // SECURITY: Validate URL before creating tab
                        if (NavigationController.isSafeUrl(url)) {
                            runOnUiThread { tabManager.addTab(url) }
                        }
                        wv?.destroy()
                        return true
                    }
                }

                val transport = resultMsg?.obj as? WebView.WebViewTransport
                transport?.webView = tempWebView
                resultMsg?.sendToTarget()
                return true
            }
        }
    }

    // === NAVIGATION ===

    private fun goBack() {
        tabManager.activeWebView?.let { if (it.canGoBack()) it.goBack() }
    }

    private fun goForward() {
        tabManager.activeWebView?.let { if (it.canGoForward()) it.goForward() }
    }

    private fun refresh() {
        tabManager.activeWebView?.reload()
        swipeRefresh.isRefreshing = false
    }

    private fun loadUrl(url: String) {
        tabManager.activeWebView?.loadUrl(url)
        urlBar.setText(url)
    }

    private fun updateNavigationButtons() {
        val wv = tabManager.activeWebView
        btnBack.alpha = if (wv?.canGoBack() == true) 1.0f else 0.4f
        btnForward.alpha = if (wv?.canGoForward() == true) 1.0f else 0.4f
    }

    private fun updateSslIcon(url: String) {
        val isHttps = url.startsWith("https://")
        sslIcon.visibility = if (isHttps) View.VISIBLE else View.GONE
        sslIcon.setImageDrawable(
            if (isHttps) ContextCompat.getDrawable(this, R.drawable.ic_lock)
            else null
        )
    }

    private fun updateTabCount() {
        btnTabs.text = tabManager.tabCount.toString()
    }

    private fun updateTabLayoutVisibility() {
        tabLayout.visibility = if (tabManager.tabCount > 1) View.VISIBLE else View.GONE
    }

    // === DESKTOP MODE ===

    @SuppressLint("SetJavaScriptEnabled")
    private fun toggleDesktopMode() {
        val tab = tabManager.activeTab ?: return
        tab.isDesktopMode = !tab.isDesktopMode

        tab.webView?.let { wv ->
            BrowserWebViewFactory.applyModeSettings(wv.settings, tab.isDesktopMode)
            wv.setInitialScale(0)

            if (tab.isDesktopMode) {
                wv.evaluateJavascript(BrowserWebViewFactory.DESKTOP_VIEWPORT_JS, null)
                wv.reload()
                Toast.makeText(this, R.string.desktop_mode_on, Toast.LENGTH_SHORT).show()
            } else {
                wv.evaluateJavascript(BrowserWebViewFactory.MOBILE_VIEWPORT_JS, null)
                wv.reload()
                Toast.makeText(this, R.string.mobile_mode_on, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // === SYSTEM BROWSER ===

    private fun openInSystemBrowser() {
        tabManager.activeWebView?.url?.let {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
        }
    }

    // === MENU ===

    private fun showBrowserMenu() {
        val tab = tabManager.activeTab
        val isBookmarked = tab?.url?.let { settingsRepo.isBookmarked(it) } == true
        val bookmarkLabel = if (isBookmarked) getString(R.string.remove_bookmark) else getString(R.string.bookmark_page)

        val opts = arrayOf(
            if (tab?.isDesktopMode == true) getString(R.string.switch_to_mobile) else getString(R.string.switch_to_desktop),
            getString(R.string.open_in_system_browser),
            bookmarkLabel,
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

    private fun toggleBookmark() {
        val tab = tabManager.activeTab ?: return
        val url = tab.url
        if (settingsRepo.isBookmarked(url)) {
            settingsRepo.removeBookmark(url)
            Toast.makeText(this, R.string.bookmark_removed, Toast.LENGTH_SHORT).show()
        } else {
            val title = tab.title.ifEmpty { url }
            settingsRepo.addBookmark(url, title)
            Toast.makeText(this, getString(R.string.bookmarked, title), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBookmarks() {
        val bookmarks = settingsRepo.getBookmarks()
        if (bookmarks.isEmpty()) {
            Toast.makeText(this, R.string.no_bookmarks, Toast.LENGTH_SHORT).show()
            return
        }

        val items = bookmarks.map { it.title.ifEmpty { it.url } }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.bookmarks)
            .setItems(items) { _, w ->
                loadUrl(bookmarks[w].url)
            }
            .setNeutralButton(R.string.clear_all) { _, _ ->
                settingsRepo.clearBookmarks()
                Toast.makeText(this, R.string.bookmarks_cleared, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showHistory() {
        val h = tabManager.activeWebView?.copyBackForwardList()
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

    private fun sharePage() {
        tabManager.activeWebView?.url?.let { url ->
            val title = tabManager.activeTab?.title ?: ""
            startActivity(Intent.createChooser(Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "$title\n$url")
                type = "text/plain"
            }, getString(R.string.share_via)))
        }
    }

    private fun findInPage() {
        val input = EditText(this).apply {
            hint = getString(R.string.search_on_page)
            setPadding(48, 24, 48, 24)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.find_in_page)
            .setView(input)
            .setPositiveButton(R.string.find) { _, _ ->
                tabManager.activeWebView?.findAllAsync(input.text.toString())
            }
            .setNeutralButton(R.string.clear) { _, _ -> tabManager.activeWebView?.clearMatches() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSettings() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings)
            .setItems(arrayOf(getString(R.string.clear_browsing_data), getString(R.string.about_zbrowser))) { _, w ->
                when (w) {
                    0 -> clearData()
                    1 -> showAbout()
                }
            }.show()
    }

    private fun clearData() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_browsing_data)
            .setMessage(R.string.clear_browsing_data_message)
            .setPositiveButton(R.string.clear) { _, _ ->
                tabManager.closeAllTabs()
                settingsRepo.clearAllBrowsingData(this)
                tabManager.addTab(NavigationController.HOME_URL)
                Toast.makeText(this, R.string.data_cleared, Toast.LENGTH_SHORT).show()
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

    // === TAB SWITCHER ===

    private fun showTabSwitcher() {
        if (tabManager.tabs.isEmpty()) return
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.open_tabs)
            .setItems(tabManager.tabs.map { it.title.ifEmpty { it.url } }.toTypedArray()) { _, w ->
                tabManager.switchToTab(tabManager.tabs[w].id)
            }
            .setNeutralButton(R.string.close_all) { _, _ ->
                tabManager.closeAllTabs()
                tabManager.addTab(NavigationController.HOME_URL)
            }
            .setPositiveButton(R.string.new_tab) { _, _ ->
                tabManager.addTab(NavigationController.HOME_URL)
            }
            .show()
    }

    // === INTENT HANDLING ===

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                val url = uri.toString()
                // SECURITY: Only allow http/https URLs
                if (NavigationController.isSafeUrl(url)) {
                    tabManager.addTab(url)
                }
            }
        }
    }
}
