package com.zbrowser.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    background = AuraColors.Background,
    surface = AuraColors.Surface,
    surfaceVariant = AuraColors.SurfaceVariant,
    primary = AuraColors.BluePrimary,
    onPrimary = Color.White,
    primaryContainer = AuraColors.BluePale,
    onPrimaryContainer = AuraColors.BlueDeep,
    secondary = AuraColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = AuraColors.BluePale,
    onSecondaryContainer = AuraColors.BlueDark,
    tertiary = AuraColors.Success,
    onTertiary = Color.White,
    error = AuraColors.Danger,
    onError = Color.White,
    errorContainer = AuraColors.DangerLight,
    onErrorContainer = AuraColors.Danger,
    onBackground = AuraColors.Primary,
    onSurface = AuraColors.Primary,
    onSurfaceVariant = AuraColors.Secondary,
    outline = AuraColors.Border,
    outlineVariant = AuraColors.BorderLight,
    inverseSurface = AuraColors.Primary,
    inverseOnSurface = AuraColors.Background,
    inversePrimary = AuraColors.BlueLighter,
    surfaceTint = AuraColors.BluePrimary
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
