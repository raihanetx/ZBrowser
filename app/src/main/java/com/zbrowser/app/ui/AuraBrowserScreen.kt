package com.zbrowser.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.zbrowser.app.ui.components.BottomNavBar
import com.zbrowser.app.ui.components.EdgeIndicator
import com.zbrowser.app.ui.components.HistoryEntry
import com.zbrowser.app.ui.components.HomePage
import com.zbrowser.app.ui.components.MenuItem
import com.zbrowser.app.ui.components.MenuSection
import com.zbrowser.app.ui.components.MenuSheet
import com.zbrowser.app.ui.components.NavBar
import com.zbrowser.app.ui.components.QuickLink
import com.zbrowser.app.ui.components.SearchHeader
import com.zbrowser.app.ui.components.SuggestionItem
import com.zbrowser.app.ui.components.SuggestionsPanel
import com.zbrowser.app.ui.components.TabItem
import com.zbrowser.app.ui.components.TopProgressBar
import com.zbrowser.app.ui.theme.AuraColors
import com.zbrowser.app.ui.theme.AuraDimensions

@Composable
fun AuraBrowserScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // State management
    var url by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf("") }
    var isSearchMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var isNavBarVisible by remember { mutableStateOf(false) }
    var isMenuSheetVisible by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isSecure by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var showHomePage by remember { mutableStateOf(true) }
    var pageTitle by remember { mutableStateOf("") }

    // Tab management
    val tabs = remember {
        mutableStateListOf(
            TabItem(id = "1", title = "Home", url = "home", isActive = true)
        )
    }

    // Quick links for home page
    val quickLinks = remember {
        listOf(
            QuickLink("g", "Google", "https://google.com", AuraColors.BluePrimary, AuraColors.BlueLight, "G"),
            QuickLink("yt", "YouTube", "https://youtube.com", AuraColors.Danger, AuraColors.Danger.copy(alpha = 0.7f), "Y"),
            QuickLink("gh", "GitHub", "https://github.com", AuraColors.Primary, AuraColors.GrayDark, "\u25C6"),
            QuickLink("r", "Reddit", "https://reddit.com", AuraColors.Warning, AuraColors.Warning.copy(alpha = 0.7f), "R"),
            QuickLink("w", "Wikipedia", "https://wikipedia.org", AuraColors.GrayMedium, AuraColors.GrayLight, "W"),
            QuickLink("x", "X", "https://x.com", AuraColors.Primary, AuraColors.GrayDark, "X"),
            QuickLink("n", "Netflix", "https://netflix.com", AuraColors.Danger, AuraColors.Danger.copy(alpha = 0.5f), "N"),
            QuickLink("s", "Spotify", "https://spotify.com", AuraColors.Success, AuraColors.Success.copy(alpha = 0.7f), "S")
        )
    }

    // Recent history (mock - would connect to HistoryDao)
    val recentHistory = remember {
        mutableStateListOf(
            HistoryEntry("1", "Google Search", "google.com", "2m ago"),
            HistoryEntry("2", "YouTube", "youtube.com", "15m ago"),
            HistoryEntry("3", "GitHub", "github.com", "1h ago")
        )
    }

    // Search suggestions
    val suggestions = remember {
        listOf(
            SuggestionItem(id = "1", title = "best android browser 2026", subtitle = "google.com", isRecent = true),
            SuggestionItem(id = "2", title = "kotlin jetpack compose tutorial", subtitle = "developer.android.com", isRecent = true),
            SuggestionItem(id = "3", title = "weather today", isRecent = true),
            SuggestionItem(id = "4", title = "top trending news"),
            SuggestionItem(id = "5", title = "new movie releases 2026"),
            SuggestionItem(id = "6", title = "best phones under 500")
        )
    }

    // Menu sections
    val menuSections = remember {
        listOf(
            MenuSection(
                title = "Navigation",
                items = listOf(
                    MenuItem("home", "Home", Icons.Default.Home) {
                        webView?.loadUrl("https://www.google.com")
                        url = "https://www.google.com"
                        currentUrl = "https://www.google.com"
                        showHomePage = false
                    },
                    MenuItem("back", "Back", Icons.AutoMirrored.Filled.ArrowBack) {
                        if (webView?.canGoBack() == true) webView?.goBack()
                    },
                    MenuItem("forward", "Forward", Icons.AutoMirrored.Filled.ArrowForward) {
                        if (webView?.canGoForward() == true) webView?.goForward()
                    },
                    MenuItem("bookmarks", "Bookmarks", Icons.Default.Bookmark) { }
                )
            ),
            MenuSection(
                title = "Tools",
                items = listOf(
                    MenuItem("find", "Find", Icons.Default.FindInPage) { },
                    MenuItem("share", "Share", Icons.Default.Share) {
                        if (currentUrl.isNotEmpty()) {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, currentUrl)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share URL"))
                        }
                    },
                    MenuItem("print", "Print", Icons.Default.Print) { },
                    MenuItem("settings", "Settings", Icons.Default.Settings) { }
                )
            )
        )
    }

    fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun handleSearch(query: String) {
        if (query.isEmpty()) return

        isLoading = true
        progress = 0.1f
        showHomePage = false

        val urlToLoad = if (query.startsWith("http://") || query.startsWith("https://")) {
            query
        } else if (query.contains(".") && !query.contains(" ")) {
            "https://$query"
        } else {
            "https://www.google.com/search?q=${query.replace(" ", "+")}"
        }

        url = urlToLoad
        currentUrl = urlToLoad
        webView?.loadUrl(urlToLoad)
        isSearchMode = false

        // Update active tab
        val activeIndex = tabs.indexOfFirst { it.isActive }
        if (activeIndex >= 0) {
            tabs[activeIndex] = tabs[activeIndex].copy(title = query.take(20), url = urlToLoad)
        }
    }

    fun navigateToQuickLink(link: QuickLink) {
        url = link.url
        currentUrl = link.url
        showHomePage = false
        isLoading = true
        progress = 0.1f
        webView?.loadUrl(link.url)

        // Update active tab
        val activeIndex = tabs.indexOfFirst { it.isActive }
        if (activeIndex >= 0) {
            tabs[activeIndex] = tabs[activeIndex].copy(title = link.title, url = link.url)
        }
    }

    fun navigateToHistory(entry: HistoryEntry) {
        val urlToLoad = if (entry.url.startsWith("http")) entry.url else "https://${entry.url}"
        url = urlToLoad
        currentUrl = urlToLoad
        showHomePage = false
        isLoading = true
        progress = 0.1f
        webView?.loadUrl(urlToLoad)
    }

    fun goHome() {
        showHomePage = true
        url = ""
        currentUrl = ""
        isSearchMode = false
        webView?.loadUrl("about:blank")

        val activeIndex = tabs.indexOfFirst { it.isActive }
        if (activeIndex >= 0) {
            tabs[activeIndex] = tabs[activeIndex].copy(title = "Home", url = "home")
        }
    }

    fun newTab() {
        tabs.forEachIndexed { i, tab -> tabs[i] = tab.copy(isActive = false) }
        tabs.add(
            TabItem(
                id = (tabs.size + 1).toString(),
                title = "New Tab",
                url = "home",
                isActive = true
            )
        )
        goHome()
        isNavBarVisible = false
    }

    fun switchToTab(tab: TabItem) {
        tabs.forEachIndexed { i, t -> tabs[i] = t.copy(isActive = t.id == tab.id) }
        if (tab.url == "home") {
            goHome()
        } else {
            url = tab.url
            currentUrl = tab.url
            showHomePage = false
            webView?.loadUrl(tab.url)
        }
        isNavBarVisible = false
    }

    fun closeTab(tab: TabItem) {
        val index = tabs.indexOfFirst { it.id == tab.id }
        if (index < 0) return

        tabs.removeAt(index)

        if (tabs.isEmpty()) {
            newTab()
            return
        }

        if (tab.isActive) {
            val newIndex = if (index > 0) index - 1 else 0
            tabs[newIndex] = tabs[newIndex].copy(isActive = true)
            switchToTab(tabs[newIndex])
        }
    }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 500

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuraColors.Background)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount < -50) {
                        isNavBarVisible = true
                    } else if (dragAmount > 50) {
                        isNavBarVisible = false
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isTablet) Modifier.padding(AuraDimensions.TabletPadding) else Modifier
                )
        ) {
            // Top progress bar
            TopProgressBar(
                progress = progress,
                isVisible = isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            // Search header
            SearchHeader(
                url = if (showHomePage) "" else url,
                isSearchMode = isSearchMode,
                isSecure = isSecure,
                isLoading = isLoading,
                onUrlChange = { url = it },
                onSearch = { query -> handleSearch(query) },
                onSearchModeChange = { isSearchMode = it },
                onCopyUrl = {
                    if (currentUrl.isNotEmpty()) {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("url", currentUrl))
                        showToast("URL copied")
                    }
                },
                onMenuClick = { isMenuSheetVisible = true },
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            // Suggestions panel
            SuggestionsPanel(
                suggestions = suggestions,
                onSuggestionClick = { suggestion ->
                    url = suggestion.title
                    isSearchMode = false
                    handleSearch(suggestion.title)
                },
                onClearHistory = { showToast("History cleared") },
                isVisible = isSearchMode && url.isEmpty()
            )

            // Content area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(AuraColors.Background)
            ) {
                // Home page or WebView
                if (showHomePage) {
                    HomePage(
                        quickLinks = quickLinks,
                        recentHistory = recentHistory,
                        onQuickLinkClick = { link -> navigateToQuickLink(link) },
                        onHistoryClick = { entry -> navigateToHistory(entry) }
                    )
                } else {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webView = this

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    setSupportMultipleWindows(false)
                                    javaScriptCanOpenWindowsAutomatically = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    loadsImagesAutomatically = true
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val reqUrl = request?.url?.toString() ?: return false
                                        if (reqUrl.startsWith("http://") || reqUrl.startsWith("https://")) {
                                            return false
                                        }
                                        return true
                                    }

                                    override fun onPageStarted(
                                        view: WebView?,
                                        pageUrl: String?,
                                        favicon: Bitmap?
                                    ) {
                                        super.onPageStarted(view, pageUrl, favicon)
                                        isLoading = true
                                        progress = 0.2f
                                    }

                                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                        super.onPageFinished(view, pageUrl)
                                        isLoading = false
                                        progress = 1f
                                        currentUrl = pageUrl ?: ""
                                        url = pageUrl ?: ""
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true

                                        // Update tab title
                                        val activeIndex = tabs.indexOfFirst { it.isActive }
                                        if (activeIndex >= 0 && pageUrl != null) {
                                            val title = view?.title ?: pageUrl
                                            tabs[activeIndex] = tabs[activeIndex].copy(
                                                title = title.take(20),
                                                url = pageUrl
                                            )
                                        }

                                        // Check SSL
                                        isSecure = pageUrl?.startsWith("https://") == true
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        if (request?.isForMainFrame == true) {
                                            isLoading = false
                                            progress = 0f
                                        }
                                    }

                                    override fun onReceivedSslError(
                                        view: WebView?,
                                        handler: SslErrorHandler?,
                                        error: SslError?
                                    ) {
                                        handler?.cancel()
                                        isSecure = false
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(
                                        view: WebView?,
                                        newProgress: Int
                                    ) {
                                        progress = newProgress / 100f
                                        canGoBack = view?.canGoBack() == true
                                        canGoForward = view?.canGoForward() == true
                                    }

                                    override fun onReceivedTitle(
                                        view: WebView?,
                                        title: String?
                                    ) {
                                        super.onReceivedTitle(view, title)
                                        if (title != null) {
                                            pageTitle = title
                                        }
                                    }
                                }

                                loadUrl("about:blank")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Bottom Navigation Bar
            BottomNavBar(
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                isLoading = isLoading,
                tabCount = tabs.size,
                onBack = {
                    if (webView?.canGoBack() == true) {
                        webView?.goBack()
                    }
                },
                onForward = {
                    if (webView?.canGoForward() == true) {
                        webView?.goForward()
                    }
                },
                onRefresh = {
                    if (isLoading) {
                        webView?.stopLoading()
                        isLoading = false
                    } else {
                        webView?.reload()
                    }
                },
                onHome = { goHome() },
                onTabs = { isNavBarVisible = !isNavBarVisible },
                onMenu = { isMenuSheetVisible = true }
            )
        }

        // Edge indicator (right side)
        EdgeIndicator(
            isVisible = !isNavBarVisible,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        // Navigation bar (slides in from right)
        NavBar(
            tabs = tabs,
            isVisible = isNavBarVisible,
            onTabClick = { tab -> switchToTab(tab) },
            onCloseTab = { tab -> closeTab(tab) },
            onNewTabClick = { newTab() },
            onDismiss = { isNavBarVisible = false }
        )
    }

    // Menu sheet
    MenuSheet(
        isVisible = isMenuSheetVisible,
        menuSections = menuSections,
        onDismiss = { isMenuSheetVisible = false }
    )
}
