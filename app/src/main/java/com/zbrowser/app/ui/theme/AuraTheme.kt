package com.zbrowser.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Aura Browser theme configuration.
 * Uses Material 3 with custom color palette based on Apple's design system.
 */
private val LightColorScheme = lightColorScheme(
    background = AuraColors.Background,
    surface = AuraColors.Surface,
    surfaceVariant = AuraColors.SurfaceVariant,
    primary = AuraColors.Primary,
    secondary = AuraColors.Secondary,
    onBackground = AuraColors.Primary,
    onSurface = AuraColors.Primary,
    onSurfaceVariant = AuraColors.Secondary,
    outline = AuraColors.Border,
    outlineVariant = AuraColors.BorderLight,
    error = AuraColors.Danger,
    onError = Color.White,
    primaryContainer = AuraColors.PurpleLight,
    onPrimaryContainer = AuraColors.PurpleDeep
)

@Composable
fun AuraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
