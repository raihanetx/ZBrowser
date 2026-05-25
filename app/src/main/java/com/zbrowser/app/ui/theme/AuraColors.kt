package com.zbrowser.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color palette for the Aura Browser UI redesign.
 * Based on Apple's system colors with purple accent gradient.
 */
object AuraColors {
    // Background & Surface
    val Background = Color(0xFFF5F5F7)      // Apple system gray background
    val Surface = Color(0xFFFFFFFF)          // White cards, search bar, nav bar active tab
    val SurfaceVariant = Color(0xFFF2F2F7)   // Lighter surface for secondary areas

    // Text Colors
    val Primary = Color(0xFF1C1C1E)          // Main text color (near-black)
    val Secondary = Color(0xFF8E8E93)        // Secondary/hint text (iOS system gray)
    val TextBody = Color(0xFF3A3A3C)         // Body text in web pages
    val TextHint = Color(0xFFAEAEB2)         // Hint text

    // Border Colors
    val Border = Color(0xFFE5E5EA)           // Default borders (iOS separator)
    val BorderLight = Color(0xFFD1D1D6)      // Light borders

    // Status Colors
    val Success = Color(0xFF34C759)          // Copy button, active tab indicator (iOS green)
    val Danger = Color(0xFFFF3B30)           // Error states (iOS red)
    val Warning = Color(0xFFFF9500)          // Warning states (iOS orange)

    // Purple Accent Gradient
    val PurpleDeep = Color(0xFF6D28D9)       // Nav bar gradient start, progress bar
    val PurpleStart = Color(0xFF7C3AED)      // Nav bar gradient middle, edge indicator
    val PurpleEnd = Color(0xFFA855F7)        // Nav bar gradient end
    val PurpleLight = Color(0xFFEDE9FE)      // Light purple for backgrounds

    // UI Element Colors
    val ClearButton = Color(0xFFC7C7CC)      // Clear/search button background
    val ToastBg = Color(0xEF1C1C1E)         // Toast background (translucent dark)
    val NavBarShadow = Color(0x40000000)     // Nav bar shadow

    // Tablet Border Colors
    val TabletBorder = Color(0xFF0A0A0A)     // Dark border for tablet mode
    val TabletBorderOuter = Color(0xFF0F0F12) // Outer border for tablet mode

    // Gradient Definitions
    val PurpleGradient = listOf(PurpleDeep, PurpleStart, PurpleEnd)
    val NavBarGradient = listOf(PurpleDeep, PurpleStart, PurpleEnd)

    // Alpha Values
    val EdgeIndicatorAlphaMin = 0.35f
    val EdgeIndicatorAlphaMax = 0.7f
    val SuggestionPanelAlpha = 0.95f
}