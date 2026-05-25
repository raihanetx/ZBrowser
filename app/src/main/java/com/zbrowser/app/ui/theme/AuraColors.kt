package com.zbrowser.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color palette for the Aura Browser UI redesign.
 * Using gray color combination with flat design (no 3D effects).
 */
object AuraColors {
    // Background & Surface
    val Background = Color(0xFFF5F5F5)      // Light gray background
    val Surface = Color(0xFFFFFFFF)          // White cards, search bar
    val SurfaceVariant = Color(0xFFEEEEEE)   // Lighter surface for secondary areas

    // Text Colors
    val Primary = Color(0xFF333333)          // Main text color (dark gray)
    val Secondary = Color(0xFF888888)        // Secondary/hint text (medium gray)
    val TextBody = Color(0xFF555555)         // Body text in web pages
    val TextHint = Color(0xFFAAAAAA)         // Hint text

    // Border Colors
    val Border = Color(0xFFDDDDDD)           // Default borders (light gray)
    val BorderLight = Color(0xFFEEEEEE)      // Light borders

    // Status Colors
    val Success = Color(0xFF666666)          // Copy button (gray)
    val Danger = Color(0xFFCC0000)           // Error states (red)
    val Warning = Color(0xFFFF9500)          // Warning states (orange)

    // Gray Accent Colors (replacing purple)
    val GrayDark = Color(0xFF555555)         // Dark gray for active elements
    val GrayMedium = Color(0xFF888888)       // Medium gray
    val GrayLight = Color(0xFFBBBBBB)        // Light gray
    val GrayLighter = Color(0xFFEEEEEE)      // Lighter gray for backgrounds

    // UI Element Colors
    val ClearButton = Color(0xFFCCCCCC)      // Clear/search button background
    val ToastBg = Color(0xDD333333)          // Toast background (dark gray)
    val NavBarShadow = Color(0x00000000)     // No shadow (transparent)

    // Tablet Border Colors
    val TabletBorder = Color(0xFFCCCCCC)     // Gray border for tablet mode
    val TabletBorderOuter = Color(0xFFDDDDDD) // Outer border for tablet mode

    // Gradient Definitions (using gray)
    val GrayGradient = listOf(GrayDark, GrayMedium, GrayLight)
    val NavBarGradient = listOf(GrayDark, GrayMedium, GrayLight)

    // Alpha Values
    val EdgeIndicatorAlphaMin = 0.35f
    val EdgeIndicatorAlphaMax = 0.7f
    val SuggestionPanelAlpha = 0.95f
}
