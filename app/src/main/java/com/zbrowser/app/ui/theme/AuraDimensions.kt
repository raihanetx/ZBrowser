package com.zbrowser.app.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dimension constants for the Aura Browser UI.
 * Based on the new UI design specifications.
 */
object AuraDimensions {
    // Search Bar
    val SearchBarHeight = 44.dp
    val SearchBarCornerRadius = 26.dp
    val SearchBarBorderWidth = 1.5.dp
    val SearchBarPaddingHorizontal = 16.dp

    // Navigation Bar
    val NavBarHeight = 52.dp
    val NavBarCornerRadius = 26.dp
    val NavBarShadowElevation = 16.dp
    val NavBarPaddingHorizontal = 12.dp

    // Tab Circles
    val TabCircleSize = 38.dp
    val TabCircleSpacing = 8.dp
    val TabCircleIconSize = 18.dp
    val TabCircleTextSize = 13.sp

    // Menu Dot
    val MenuDotSize = 34.dp
    val MenuDotIconSize = 20.dp

    // Clear/Copy Buttons
    val CircleIconButtonSize = 18.dp
    val CircleIconButtonPadding = 4.dp

    // Suggestions Panel
    val SuggestionIconSize = 30.dp
    val SuggestionItemHeight = 44.dp
    val SuggestionPanelCornerRadius = 24.dp
    val SuggestionPanelShadowElevation = 8.dp

    // Menu Items
    val MenuItemIconSize = 44.dp
    val MenuItemSpacing = 16.dp
    val MenuItemGridColumns = 4

    // Content Area
    val HeroHeight = 160.dp
    val HeroCornerRadius = 16.dp
    val ContentPaddingHorizontal = 20.dp
    val ContentPaddingVertical = 16.dp

    // Progress Bar
    val ProgressBarHeight = 2.dp

    // Edge Indicator
    val EdgeIndicatorWidth = 3.dp

    // Tablet Layout
    val TabletPadding = 20.dp
    val TabletCornerRadius = 44.dp
    val TabletBorderWidth = 10.dp

    // Toast
    val ToastCornerRadius = 20.dp
    val ToastPaddingHorizontal = 16.dp
    val ToastPaddingVertical = 10.dp
    val ToastTopMargin = 62.dp

    // Empty State
    val EmptyStateIconSize = 48.sp
    val EmptyStateIconAlpha = 0.3f

    // Shadows
    val DefaultShadowElevation = 4.dp
    val CardShadowElevation = 8.dp

    // Animation Durations (in milliseconds)
    const val ProgressAnimationDuration = 600
    const val ProgressFadeDuration = 150
    const val SearchFocusDuration = 120
    const val SuggestionPanelDuration = 200
    const val NavBarSlideDuration = 350
    const val EdgeIndicatorPulseDuration = 2000
    const val ToastDuration = 1400
    const val LoadingSimulatedDuration = 300L
    const val LoadingSimulatedRandom = 150L
}
