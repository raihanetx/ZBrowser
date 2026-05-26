package com.zbrowser.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zbrowser.app.ui.theme.AuraColors
import com.zbrowser.app.ui.theme.AuraDimensions
import com.zbrowser.app.ui.theme.AuraTypography

data class SuggestionItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val isRecent: Boolean = false
)

@Composable
fun SuggestionsPanel(
    suggestions: List<SuggestionItem>,
    onSuggestionClick: (SuggestionItem) -> Unit,
    onClearHistory: () -> Unit,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(AuraDimensions.SuggestionPanelDuration)) +
                scaleIn(
                    tween(AuraDimensions.SuggestionPanelDuration),
                    initialScale = 0.97f
                ) +
                slideInVertically(
                    tween(AuraDimensions.SuggestionPanelDuration),
                    initialOffsetY = { -8 }
                ),
        exit = fadeOut(tween(150)) +
                scaleOut(tween(150), targetScale = 0.97f) +
                slideOutVertically(tween(150), targetOffsetY = { -8 })
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = AuraDimensions.ContentPaddingHorizontal)
                .heightIn(max = 400.dp)
                .shadow(
                    elevation = AuraDimensions.SuggestionPanelShadowElevation,
                    shape = RoundedCornerShape(AuraDimensions.SuggestionPanelCornerRadius),
                    ambientColor = AuraColors.BlueDark.copy(alpha = 0.06f),
                    spotColor = AuraColors.BlueDark.copy(alpha = 0.1f)
                )
                .background(
                    color = AuraColors.Surface,
                    shape = RoundedCornerShape(AuraDimensions.SuggestionPanelCornerRadius)
                )
                .padding(vertical = 8.dp)
        ) {
            // Recent suggestions
            val recentItems = suggestions.filter { it.isRecent }
            val trendingItems = suggestions.filter { !it.isRecent }

            if (recentItems.isNotEmpty()) {
                SuggestionGroupHeader(
                    title = "Recent",
                    onClear = onClearHistory
                )
                recentItems.forEach { suggestion ->
                    SuggestionRow(
                        suggestion = suggestion,
                        iconTint = AuraColors.Secondary,
                        iconBg = AuraColors.SurfaceVariant,
                        onClick = { onSuggestionClick(suggestion) }
                    )
                }
                if (trendingItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(AuraColors.BorderLight)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            if (trendingItems.isNotEmpty()) {
                SuggestionGroupHeader(
                    title = "Suggestions",
                    onClear = null
                )
                trendingItems.forEach { suggestion ->
                    SuggestionRow(
                        suggestion = suggestion,
                        iconTint = AuraColors.BluePrimary,
                        iconBg = AuraColors.BluePale,
                        onClick = { onSuggestionClick(suggestion) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionGroupHeader(
    title: String,
    onClear: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = AuraTypography.SuggestionHeader,
            color = AuraColors.Secondary
        )
        if (onClear != null) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear history",
                tint = AuraColors.Secondary,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onClear() }
            )
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: SuggestionItem,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AuraDimensions.SuggestionItemHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(AuraDimensions.SuggestionIconSize)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (suggestion.isRecent) Icons.Default.AccessTime else Icons.Default.Search,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(15.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = suggestion.title,
                style = AuraTypography.SuggestionItem,
                color = AuraColors.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (suggestion.subtitle != null) {
                Text(
                    text = suggestion.subtitle,
                    style = AuraTypography.SuggestionUrl,
                    color = AuraColors.Secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
