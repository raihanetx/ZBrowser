package com.zbrowser.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import com.zbrowser.app.ui.theme.AuraColors
import com.zbrowser.app.ui.theme.AuraDimensions

@Composable
fun TopProgressBar(
    progress: Float,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (isVisible) progress else 0f,
        animationSpec = tween(
            durationMillis = AuraDimensions.ProgressAnimationDuration,
            easing = { fraction -> fraction }
        ),
        label = "progress"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(AuraDimensions.ProgressFadeDuration),
        label = "alpha"
    )

    if (isVisible || animatedAlpha > 0f) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(AuraDimensions.ProgressBarHeight)
                .alpha(animatedAlpha)
        ) {
            // Background track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AuraDimensions.ProgressBarHeight)
                    .background(AuraColors.BluePale.copy(alpha = 0.5f))
            )

            // Progress indicator with blue gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(AuraDimensions.ProgressBarHeight)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = AuraColors.ProgressGradient
                        )
                    )
            )
        }
    }
}
