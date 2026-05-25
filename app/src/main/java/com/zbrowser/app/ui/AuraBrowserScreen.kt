package com.zbrowser.app.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.PrivateConnectivity
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
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
import com.zbrowser.app.ui.components.EdgeIndicator
import com.zbrowser.app.ui.components.MenuItem
import com.zbrowser.app.ui.components.MenuSheet
import com.zbrowser.app.ui.components.NavBar
import com.zbrowser.app.ui.components.SearchHeader
import com.zbrowser.app.ui.components.SuggestionItem
import com.zbrowser.app.ui.components.SuggestionsPanel
import com.zbrowser.app.ui.components.TabItem
import com.zbrowser.app.ui.components.TopProgressBar
import com.zbrowser.app.ui.theme.AuraColors
import com.zbrowser.app.ui.theme.AuraDimensions

/**
 * Main Aura Browser screen composable.
 * Composes all UI components together with actual WebView functionality.
 */
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

    // Mock data
    val tabs = remember {
        mutableStateListOf(
            TabItem(id = "1", title = "Home", url = "home", isActive = true),
            TabItem(id = "2", title = "Google", url = "https://google.com"),
            TabItem(id = "3", title = "GitHub", url = "https://github.com")
        )
    }

    val suggestions = remember {
        listOf(
            SuggestionItem(id = "1", title = "Recent search", isRecent = true),
            SuggestionItem(id = "2", title = "Another search", isRecent = true),
            SuggestionItem(id = "3", title = "Trending topic 1"),
            SuggestionItem(id = "4", title = "Trending topic 2"),
            SuggestionItem(id = "5", title = "Trending topic 3")
        )
    }

    val menuItems = remember {
        listOf(
            MenuItem(id = "home", label = "Home", icon = Icons.Default.Home) {
                webView?.loadUrl("https://www.google.com")
                url = "https://www.google.com"
                currentUrl = "https://www.google.com"
            },
            MenuItem(id = "history", label = "History", icon = Icons.Default.History) { },
            MenuItem(id = "downloads", label = "Downloads", icon = Icons.Default.SaveAlt) { },
            MenuItem(id = "private", label = "Private", icon = Icons.Default.PrivateConnectivity) { },
            MenuItem(id = "find", label = "Find", icon = Icons.Default.FindInPage) { },
            MenuItem(id = "share", label = "Share", icon = Icons.Default.Share) { },
            MenuItem(id = "print", label = "Print", icon = Icons.Default.Print) { },
            MenuItem(id = "settings", label = "Settings", icon = Icons.Default.Settings) { }
        )
    }

    // Function to handle search
    fun handleSearch(query: String) {
        if (query.isEmpty()) return

        isLoading = true
        progress = 0.3f

        // Check if it's a URL or search query
        val urlToLoad = if (query.startsWith("http://") || query.startsWith("https://")) {
            query
        } else if (query.contains(".") && !query.contains(" ")) {
            // Likely a URL without protocol
            "https://$query"
        } else {
            // Search query - use Google search
            "https://www.google.com/search?q=${query.replace(" ", "+")}"
        }

        url = urlToLoad
        currentUrl = urlToLoad
        webView?.loadUrl(urlToLoad)
        isSearchMode = false
    }

    // Responsive layout detection
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 500

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuraColors.Background)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    if (dragAmount < -50) { // Swipe left to reveal nav bar
                        isNavBarVisible = true
                    } else if (dragAmount > 50) { // Swipe right to dismiss
                        isNavBarVisible = false
                    }
                }
            }
    ) {
        // Main content area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isTablet) {
                        Modifier.padding(AuraDimensions.TabletPadding)
                    } else {
                        Modifier
                    }
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
                url = url,
                isSearchMode = isSearchMode,
                onUrlChange = { url = it },
                onSearch = { searchQuery ->
                    handleSearch(searchQuery)
                },
                onSearchModeChange = { isSearchMode = it },
                onCopyUrl = { /* Handle copy */ },
                onMenuClick = { isMenuSheetVisible = true },
                isLoading = isLoading,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Suggestions panel
            SuggestionsPanel(
                suggestions = suggestions,
                onSuggestionClick = { suggestion ->
                    url = suggestion.title
                    isSearchMode = false
                    handleSearch(suggestion.title)
                },
                onClearHistory = { /* Handle clear history */ },
                isVisible = isSearchMode && url.isEmpty()
            )

            // WebView content area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(AuraColors.Background)
            ) {
                // WebView
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webView = this

                            // Configure WebView settings
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

                            // Set WebViewClient to handle navigation
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    // Allow http and https URLs
                                    if (url.startsWith("http://") || url.startsWith("https://")) {
                                        return false // Let WebView handle it
                                    }
                                    return true // Block other schemes
                                }

                                override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                    super.onPageFinished(view, pageUrl)
                                    isLoading = false
                                    progress = 1f
                                    currentUrl = pageUrl ?: ""
                                    url = pageUrl ?: ""
                                }
                            }

                            // Set WebChromeClient for progress updates
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress / 100f
                                }
                            }

                            // Load initial URL
                            loadUrl("https://www.google.com")
                            url = "https://www.google.com"
                            currentUrl = "https://www.google.com"
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
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
            onTabClick = { tab ->
                // Handle tab click - load the tab's URL
                if (tab.url.isNotEmpty() && tab.url != "home") {
                    webView?.loadUrl(tab.url)
                    url = tab.url
                    currentUrl = tab.url
                }
                isNavBarVisible = false
            },
            onNewTabClick = {
                // Handle new tab
                tabs.add(
                    TabItem(
                        id = (tabs.size + 1).toString(),
                        title = "New Tab",
                        url = ""
                    )
                )
                webView?.loadUrl("https://www.google.com")
                url = "https://www.google.com"
                currentUrl = "https://www.google.com"
                isNavBarVisible = false
            },
            onDismiss = { isNavBarVisible = false }
        )
    }

    // Menu sheet
    MenuSheet(
        isVisible = isMenuSheetVisible,
        menuItems = menuItems,
        onDismiss = { isMenuSheetVisible = false }
    )
}
