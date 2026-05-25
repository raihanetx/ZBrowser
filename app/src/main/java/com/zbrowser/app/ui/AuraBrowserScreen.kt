package com.zbrowser.app.ui

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
import androidx.compose.ui.unit.dp
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
 * Composes all UI components together.
 */
@Composable
fun AuraBrowserScreen(
    modifier: Modifier = Modifier
) {
    // State management
    var url by remember { mutableStateOf("") }
    var isSearchMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var isNavBarVisible by remember { mutableStateOf(false) }
    var isMenuSheetVisible by remember { mutableStateOf(false) }

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
            MenuItem(id = "home", label = "Home", icon = Icons.Default.Home) { },
            MenuItem(id = "history", label = "History", icon = Icons.Default.History) { },
            MenuItem(id = "downloads", label = "Downloads", icon = Icons.Default.SaveAlt) { },
            MenuItem(id = "private", label = "Private", icon = Icons.Default.PrivateConnectivity) { },
            MenuItem(id = "find", label = "Find", icon = Icons.Default.FindInPage) { },
            MenuItem(id = "share", label = "Share", icon = Icons.Default.Share) { },
            MenuItem(id = "print", label = "Print", icon = Icons.Default.Print) { },
            MenuItem(id = "settings", label = "Settings", icon = Icons.Default.Settings) { }
        )
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
                    // Handle search
                    isLoading = true
                    // Simulate loading
                    progress = 0.8f
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
                },
                onClearHistory = { /* Handle clear history */ },
                isVisible = isSearchMode && url.isEmpty()
            )

            // Content area (placeholder)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(AuraColors.Background),
                contentAlignment = Alignment.Center
            ) {
                // Empty state or content would go here
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
                // Handle tab click
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
