package com.zbrowser.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zbrowser.app.ui.theme.AuraColors
import com.zbrowser.app.ui.theme.AuraDimensions
import com.zbrowser.app.ui.theme.AuraTypography

/**
 * Data class representing a suggestion item.
 */
data class SuggestionItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector? = null,
    val isRecent: Boolean = false
)

/**
 * SuggestionsPanel component - Dropdown panel showing search suggestions.
 * Appears below the search bar when in search mode.
 */
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
        enter = fadeIn(
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = AuraDimensions.SuggestionPanelDuration
            )
        ) + scaleIn(
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = AuraDimensions.SuggestionPanelDuration
            ),
            initialScale = 0.98f
        ) + slideInVertically(
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = AuraDimensions.SuggestionPanelDuration
            ),
            initialOffsetY = { -6 }
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = AuraDimensions.ContentPaddingHorizontal)
                .shadow(
                    elevation = AuraDimensions.SuggestionPanelShadowElevation,
                    shape = RoundedCornerShape(AuraDimensions.SuggestionPanelCornerRadius)
                )
                .background(
                    color = AuraColors.Surface,
                    shape = RoundedCornerShape(AuraDimensions.SuggestionPanelCornerRadius)
                )
                .padding(vertical = 12.dp)
        ) {
            // Recent suggestions header
            if (suggestions.any { it.isRecent }) {
                SuggestionGroupHeader(
                    title = "Recent",
                    onClear = onClearHistory
                )

                suggestions.filter { it.isRecent }.forEach { suggestion ->
                    SuggestionRow(
                        suggestion = suggestion,
                        onClick = { onSuggestionClick(suggestion) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Trending suggestions
            if (suggestions.any { !it.isRecent }) {
                SuggestionGroupHeader(
                    title = "Suggestions",
                    onClear = null
                )

                suggestions.filter { !it.isRecent }.forEach { suggestion ->
                    SuggestionRow(
                        suggestion = suggestion,
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
            .padding(horizontal = 16.dp, vertical = 4.dp),
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AuraDimensions.SuggestionItemHeight)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(AuraDimensions.SuggestionIconSize)
                .clip(CircleShape)
                .background(AuraColors.ClearButton),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = suggestion.icon ?: Icons.Default.Search,
                contentDescription = null,
                tint = AuraColors.Secondary,
                modifier = Modifier.size(16.dp)
            )
        }

        // Text content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = suggestion.title,
                style = AuraTypography.SuggestionItem,
                color = AuraColors.Primary
            )

            if (suggestion.subtitle != null) {
                Text(
                    text = suggestion.subtitle,
                    style = AuraTypography.SuggestionUrl,
                    color = AuraColors.Secondary
                )
            }
        }
    }
}
