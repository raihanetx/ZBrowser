package com.zbrowser.app.ui.theme

import androidx.compose.ui.graphics.Color

object AuraColors {
    // Background & Surface
    val Background = Color(0xFFF8FAFC)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF1F5F9)

    // Blue Accent Colors
    val BluePrimary = Color(0xFF2563EB)
    val BlueLight = Color(0xFF3B82F6)
    val BlueLighter = Color(0xFF60A5FA)
    val BluePale = Color(0xFFDBEAFE)
    val BlueDark = Color(0xFF1D4ED8)
    val BlueDeep = Color(0xFF1E40AF)

    // Text Colors
    val Primary = Color(0xFF0F172A)
    val Secondary = Color(0xFF64748B)
    val TextBody = Color(0xFF334155)
    val TextHint = Color(0xFF94A3B8)
    val TextOnAccent = Color(0xFFFFFFFF)

    // Border Colors
    val Border = Color(0xFFE2E8F0)
    val BorderLight = Color(0xFFF1F5F9)
    val BorderFocus = Color(0xFF93C5FD)

    // Status Colors
    val Success = Color(0xFF10B981)
    val SuccessLight = Color(0xFFD1FAE5)
    val Danger = Color(0xFFEF4444)
    val DangerLight = Color(0xFFFEE2E2)
    val Warning = Color(0xFFF59E0B)
    val WarningLight = Color(0xFFFEF3C7)

    // Gray Accent Colors
    val GrayDark = Color(0xFF334155)
    val GrayMedium = Color(0xFF64748B)
    val GrayLight = Color(0xFF94A3B8)
    val GrayLighter = Color(0xFFF1F5F9)

    // UI Element Colors
    val ClearButton = Color(0xFFE2E8F0)
    val ToastBg = Color(0xEF0F172A)
    val NavBarShadow = Color(0x1A000000)

    // Tablet Border Colors
    val TabletBorder = Color(0xFFCBD5E1)
    val TabletBorderOuter = Color(0xFFE2E8F0)

    // Gradient Definitions
    val BlueGradient = listOf(BlueDark, BluePrimary, BlueLight)
    val NavBarGradient = listOf(BlueDark, BluePrimary, BlueLight)
    val ProgressGradient = listOf(BluePrimary, BlueLighter)
    val SearchShimmer = listOf(Border, BluePale, Border)

    // Alpha Values
    val EdgeIndicatorAlphaMin = 0.3f
    val EdgeIndicatorAlphaMax = 0.65f
    val SuggestionPanelAlpha = 0.97f
}
