package com.zbrowser.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import com.zbrowser.app.ui.theme.AuraColors
import com.zbrowser.app.ui.theme.AuraDimensions

/**
 * EdgeIndicator component - Pulsing purple strip on the right edge.
 * Indicates the swipe target for revealing the navigation bar.
 */
@Composable
fun EdgeIndicator(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        // Infinite pulsing animation
        val infiniteTransition = rememberInfiniteTransition(label = "edgePulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = AuraColors.EdgeIndicatorAlphaMin,
            targetValue = AuraColors.EdgeIndicatorAlphaMax,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = AuraDimensions.EdgeIndicatorPulseDuration,
                    easing = { fraction -> fraction } // Linear
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "edgeAlpha"
        )

        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(AuraDimensions.EdgeIndicatorWidth)
                .alpha(alpha)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            AuraColors.PurpleStart.copy(alpha = 0f),
                            AuraColors.PurpleStart,
                            AuraColors.PurpleStart.copy(alpha = 0f)
                        )
                    )
                )
        )
    }
}
