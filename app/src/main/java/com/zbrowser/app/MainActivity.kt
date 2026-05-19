package com.zbrowser.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
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
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : AppCompatActivity() {

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

    private val tabs = mutableListOf<BrowserTab>()
    private val webViews = mutableListOf<WebView>()
    private val tabIdGenerator = AtomicInteger(0)
    private var activeTabId: Int = -1
    private var isDesktopMode = false
    private var currentWebView: WebView? = null

    // FIX: Cache the default mobile UA on first real WebView creation instead of
    // creating a throwaway WebView that would leak memory and context
    private var mobileUserAgent: String? = null

    private val desktopUserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    // JavaScript to force desktop viewport (1024px wide)
    private val desktopViewportJs = """
        (function() {
            var meta = document.querySelector('meta[name="viewport"]');
            if (meta) {
                meta.setAttribute('content', 'width=1024, initial-scale=1');
            } else {
                meta = document.createElement('meta');
                meta.name = 'viewport';
                meta.content = 'width=1024, initial-scale=1';
                document.head.appendChild(meta);
            }
        })();
    """.trimIndent()

    // JavaScript to restore mobile viewport
    private val mobileViewportJs = """
        (function() {
            var meta = document.querySelector('meta[name="viewport"]');
            if (meta) {
                meta.setAttribute('content', 'width=device-width, initial-scale=1, maximum-scale=5');
            } else {
                meta = document.createElement('meta');
                meta.name = 'viewport';
                meta.content = 'width=device-width, initial-scale=1, maximum-scale=5';
                document.head.appendChild(meta);
            }
        })();
    """.trimIndent()

    companion object {
        const val HOME_URL = "https://www.google.com"
    }

    // === LIFECYCLE ===

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        setupToolbar()
        setupUrlBar()
        setupBottomBar()
        setupSwipeRefresh()
        setupTabLayout()
        addNewTab(HOME_URL)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    override fun onResume() { super.onResume(); currentWebView?.onResume() }
    override fun onPause() { super.onPause(); currentWebView?.onPause() }

    override fun onDestroy() {
        webViews.forEach { it.destroy() }
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && currentWebView?.canGoBack() == true) {
            currentWebView?.goBack()
            return true
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
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupUrlBar() {
        urlBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH) {
                val input = urlBar.text.toString().trim()
                if (input.isNotEmpty()) loadUrl(processInput(input))
                true
            } else false
        }
        urlBar.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) urlBar.selectAll() }
    }

    private fun setupBottomBar() {
        btnBack.setOnClickListener { goBack() }
        btnForward.setOnClickListener { goForward() }
        btnRefresh.setOnClickListener { refresh() }
        btnHome.setOnClickListener { loadUrl(HOME_URL) }
        btnTabs.setOnClickListener { showTabSwitcher() }
        btnMenu.setOnClickListener { showBrowserMenu() }
        fabNewTab.setOnClickListener { addNewTab(HOME_URL) }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorPrimary))
        swipeRefresh.setOnRefreshListener { refresh() }
    }

    private fun setupTabLayout() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val i = tab.position
                if (i < tabs.size) switchToTab(tabs[i].id)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    // === HELPER: Find the tab that owns a given WebView ===

    private fun findTabForWebView(webView: WebView): BrowserTab? {
        val wvIdx = webViews.indexOf(webView)
        if (wvIdx < 0) return null
        return tabs.find { it.webViewIndex == wvIdx }
    }

    // === WEBVIEW ===

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(desktopMode: Boolean): WebView {
        val webView = WebView(this)
        webView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
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

        // FIX: Disable file access from web content to prevent local file theft
        s.allowFileAccess = false
        s.allowContentAccess = false

        s.loadsImagesAutomatically = true

        // FIX: Cache the default mobile UA from the first WebView instead of
        // creating a separate throwaway WebView that leaks memory
        if (mobileUserAgent == null) {
            mobileUserAgent = s.userAgentString
        }

        applyModeSettings(s, desktopMode)

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                val scheme = request.url.scheme?.lowercase() ?: ""

                // Only special schemes go to external apps
                when (scheme) {
                    "tel", "mailto", "sms", "geo", "market" -> {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Cannot open: $scheme", Toast.LENGTH_SHORT).show()
                        }
                        return true
                    }
                    // intent:// scheme - extract URL and load in WebView
                    "intent" -> {
                        try {
                            val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                            val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                            if (fallbackUrl != null) {
                                view?.loadUrl(fallbackUrl)
                                return true
                            }
                            if (intent.resolveActivity(packageManager) != null) {
                                startActivity(intent)
                                return true
                            }
                        } catch (e: Exception) {
                            // Bad intent URL, ignore
                        }
                        return true
                    }
                }

                // ALL http/https URLs stay inside the WebView
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                // Re-apply desktop mode settings BEFORE every page load
                view?.let { wv ->
                    val tab = findTabForWebView(wv)
                    if (tab != null) {
                        applyModeSettings(wv.settings, tab.isDesktopMode)
                    }
                }

                url?.let {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = 0
                    val tab = view?.let { findTabForWebView(it) }
                    if (tab != null) {
                        tab.url = it
                        if (tab.id == activeTabId) {
                            urlBar.setText(it)
                        }
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

                    val tab = findTabForWebView(wv)
                    if (tab?.isDesktopMode == true) {
                        wv.evaluateJavascript(desktopViewportJs, null)
                    } else {
                        wv.evaluateJavascript(mobileViewportJs, null)
                    }

                    tab?.let { t ->
                        t.title = if (title.isNotEmpty()) title else pageUrl
                        t.url = pageUrl
                        val idx = tabs.indexOf(t)
                        if (idx >= 0 && idx < tabLayout.tabCount) {
                            tabLayout.getTabAt(idx)?.text = t.title
                        }
                        if (t.id == activeTabId) {
                            urlBar.setText(pageUrl)
                        }
                    }
                    updateNavigationButtons()
                }
            }

            // FIX: Handle page load errors so user sees feedback instead of blank screen
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    view?.let { wv ->
                        val errorMsg = error?.description?.toString() ?: "Unknown error"
                        wv.loadDataWithBaseURL(
                            null,
                            """<html><body style='display:flex;justify-content:center;align-items:center;height:100vh;font-family:sans-serif;text-align:center;color:#666'>
                            <div><h2>Page Load Error</h2><p>$errorMsg</p><p><a href="${view?.url}">Try Again</a></p></div>
                            </body></html>""",
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                }
            }

            // FIX: Handle SSL errors properly — ask user instead of silently failing
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("SSL Certificate Error")
                    .setMessage("The site's security certificate is not trusted.\n\nError: ${error?.toString() ?: "Unknown"}\n\nProceed anyway?")
                    .setPositiveButton("Proceed") { _, _ -> handler?.proceed() }
                    .setNegativeButton("Cancel") { _, _ -> handler?.cancel() }
                    .setOnCancelListener { handler?.cancel() }
                    .show()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 100) progressBar.visibility = View.GONE
            }

            // FIX: Properly handle new window requests — destroy temp WebView after use
            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                val newWebView = WebView(this@MainActivity)
                newWebView.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(wv: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: return false
                        runOnUiThread { addNewTab(url) }
                        // FIX: Destroy the temp WebView after intercepting the URL to prevent memory leak
                        wv?.destroy()
                        return true
                    }
                }

                val transport = resultMsg?.obj as? WebView.WebViewTransport
                transport?.webView = newWebView
                resultMsg?.sendToTarget()
                return true
            }
        }

        return webView
    }

    /**
     * Apply desktop or mobile mode WebView settings.
     * Desktop mode = desktop UA + wide viewport + no overview + 1024px viewport
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun applyModeSettings(settings: WebSettings, desktopMode: Boolean) {
        if (desktopMode) {
            settings.userAgentString = desktopUserAgent
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = false
        } else {
            settings.userAgentString = mobileUserAgent ?: settings.userAgentString
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
        }
    }

    // === TABS ===

    private fun addNewTab(url: String = HOME_URL) {
        val tabId = tabIdGenerator.incrementAndGet()
        val webView = createWebView(isDesktopMode)
        webViews.add(webView)

        val tab = BrowserTab(id = tabId, url = url, webViewIndex = webViews.size - 1, isDesktopMode = isDesktopMode)
        tabs.add(tab)
        tabLayout.addTab(tabLayout.newTab().apply { text = "New Tab" }, true)
        webView.loadUrl(url)
        switchToTab(tabId)
        updateTabCount()
    }

    private fun switchToTab(tabId: Int) {
        val tab = tabs.find { it.id == tabId } ?: return
        activeTabId = tabId
        webViewContainer.removeAllViews()

        if (tab.webViewIndex < webViews.size) {
            val wv = webViews[tab.webViewIndex]
            webViewContainer.addView(wv)
            currentWebView = wv
            isDesktopMode = tab.isDesktopMode
            applyModeSettings(wv.settings, tab.isDesktopMode)
        }

        urlBar.setText(tab.url)
        val idx = tabs.indexOf(tab)
        if (idx >= 0 && idx < tabLayout.tabCount) tabLayout.getTabAt(idx)?.select()
        updateNavigationButtons()
    }

    private fun closeTab(tabId: Int) {
        val tab = tabs.find { it.id == tabId } ?: return
        val idx = tabs.indexOf(tab)
        val removedWvIdx = tab.webViewIndex

        // Destroy and remove the WebView
        if (removedWvIdx < webViews.size) {
            webViews[removedWvIdx].let { webViewContainer.removeView(it); it.destroy() }
            webViews.removeAt(removedWvIdx)
        }

        // Remove the tab
        tabs.removeAt(idx)
        tabLayout.removeTabAt(idx)

        // FIX: Properly update webViewIndex for all remaining tabs
        // Any tab whose webViewIndex was above the removed one needs to shift down by 1
        for (t in tabs) {
            if (t.webViewIndex > removedWvIdx) {
                t.webViewIndex -= 1
            }
        }

        if (tabs.isEmpty()) { addNewTab(HOME_URL); return }
        switchToTab(tabs[minOf(idx, tabs.size - 1)].id)
        updateTabCount()
    }

    private fun showTabSwitcher() {
        if (tabs.isEmpty()) return
        MaterialAlertDialogBuilder(this)
            .setTitle("Open Tabs")
            .setItems(tabs.map { it.title }.toTypedArray()) { _, w -> switchToTab(tabs[w].id) }
            .setNeutralButton("Close All") { _, _ ->
                tabs.clear(); webViews.forEach { it.destroy() }; webViews.clear()
                tabLayout.removeAllTabs(); webViewContainer.removeAllViews(); currentWebView = null
                addNewTab(HOME_URL)
            }
            .setPositiveButton("New Tab") { _, _ -> addNewTab(HOME_URL) }
            .show()
    }

    private fun updateTabCount() { btnTabs.text = tabs.size.toString() }

    // === NAVIGATION ===

    private fun goBack() { currentWebView?.let { if (it.canGoBack()) it.goBack() } }
    private fun goForward() { currentWebView?.let { if (it.canGoForward()) it.goForward() } }
    private fun refresh() { currentWebView?.reload(); swipeRefresh.isRefreshing = false }
    private fun loadUrl(url: String) { currentWebView?.loadUrl(url); urlBar.setText(url) }

    private fun updateNavigationButtons() {
        btnBack.alpha = if (currentWebView?.canGoBack() == true) 1.0f else 0.4f
        btnForward.alpha = if (currentWebView?.canGoForward() == true) 1.0f else 0.4f
    }

    // === DESKTOP MODE ===

    @SuppressLint("SetJavaScriptEnabled")
    private fun toggleDesktopMode() {
        isDesktopMode = !isDesktopMode
        tabs.find { it.id == activeTabId }?.isDesktopMode = isDesktopMode

        currentWebView?.let { wv ->
            applyModeSettings(wv.settings, isDesktopMode)
            wv.setInitialScale(0)

            if (isDesktopMode) {
                wv.evaluateJavascript(desktopViewportJs, null)
                wv.reload()
                Toast.makeText(this, "Desktop Mode ON", Toast.LENGTH_SHORT).show()
            } else {
                wv.evaluateJavascript(mobileViewportJs, null)
                wv.reload()
                Toast.makeText(this, "Mobile Mode ON", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // === SYSTEM BROWSER ===

    private fun openInSystemBrowser() {
        currentWebView?.url?.let { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
    }

    // === MENU ===

    private fun showBrowserMenu() {
        val opts = arrayOf(
            if (isDesktopMode) "Switch to Mobile Mode" else "Switch to Desktop Mode",
            "Open in System Browser", "Bookmark", "History", "Share", "Find in Page", "Settings"
        )
        MaterialAlertDialogBuilder(this)
            .setTitle("Browser Menu")
            .setItems(opts) { _, w ->
                when (w) {
                    0 -> toggleDesktopMode()
                    1 -> openInSystemBrowser()
                    2 -> bookmarkPage()
                    3 -> showHistory()
                    4 -> sharePage()
                    5 -> findInPage()
                    6 -> showSettings()
                }
            }
            .show()
    }

    private fun bookmarkPage() {
        val url = currentWebView?.url ?: return
        val title = currentWebView?.title ?: url
        getSharedPreferences("bookmarks", MODE_PRIVATE).edit().putString(url, title).apply()
        Toast.makeText(this, "Bookmarked: $title", Toast.LENGTH_SHORT).show()
    }

    private fun showHistory() {
        val h = currentWebView?.copyBackForwardList()
        if (h == null || h.size == 0) { Toast.makeText(this, "No history", Toast.LENGTH_SHORT).show(); return }
        val items = (0 until h.size).map { h.getItemAtIndex(it).title ?: h.getItemAtIndex(it).url }
        MaterialAlertDialogBuilder(this)
            .setTitle("History")
            .setItems(items.toTypedArray()) { _, w -> loadUrl(h.getItemAtIndex(w).url) }
            .show()
    }

    private fun sharePage() {
        currentWebView?.url?.let { url ->
            val title = currentWebView?.title ?: ""
            startActivity(Intent.createChooser(Intent().apply {
                action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, "$title\n$url"); type = "text/plain"
            }, "Share via"))
        }
    }

    private fun findInPage() {
        val input = EditText(this).apply { hint = "Search on page..."; setPadding(48, 24, 48, 24) }
        MaterialAlertDialogBuilder(this)
            .setTitle("Find in Page").setView(input)
            .setPositiveButton("Find") { _, _ ->
                currentWebView?.findAllAsync(input.text.toString())
            }
            // FIX: Add "Clear" button to dismiss search highlights
            .setNeutralButton("Clear") { _, _ -> currentWebView?.clearMatches() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showSettings() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Settings")
            .setItems(arrayOf("Clear Browsing Data", "About ZBrowser")) { _, w ->
                when (w) { 0 -> clearData(); 1 -> showAbout() }
            }.show()
    }

    private fun clearData() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Clear Browsing Data")
            .setMessage("Clear cache, cookies, history?")
            .setPositiveButton("Clear") { _, _ ->
                webViewContainer.removeAllViews()
                // FIX: Clear cache on each WebView before destroying
                webViews.forEach { it.clearCache(true); it.destroy() }
                webViews.clear()
                tabs.clear(); tabLayout.removeAllTabs(); currentWebView = null
                android.webkit.CookieManager.getInstance().removeAllCookies(null)
                WebView.clearClientCertPreferences(null)
                addNewTab(HOME_URL)
                Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showAbout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("About ZBrowser")
            .setMessage("ZBrowser v1.2\n\nAndroid browser with Kotlin & WebView.\n\nFeatures: Multi-tab, Desktop mode, Search, Navigation, Bookmarks, Find in page, Share")
            .setPositiveButton("OK", null).show()
    }

    // === INPUT ===

    private fun processInput(input: String): String = when {
        input.startsWith("http://") || input.startsWith("https://") -> input
        input.contains(".") && !input.contains(" ") -> "https://$input"
        else -> "https://www.google.com/search?q=${Uri.encode(input)}"
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) intent.data?.let { addNewTab(it.toString()) }
    }
}
