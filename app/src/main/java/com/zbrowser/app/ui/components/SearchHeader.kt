package com.zbrowser.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zbrowser.app.ui.theme.AuraColors
import com.zbrowser.app.ui.theme.AuraDimensions
import com.zbrowser.app.ui.theme.AuraTypography

/**
 * SearchHeader component - Pill-shaped search bar at the top of the screen.
 * Supports two modes: display mode (URL) and search mode (text input).
 */
@Composable
fun SearchHeader(
    url: String,
    isSearchMode: Boolean,
    onUrlChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSearchModeChange: (Boolean) -> Unit,
    onCopyUrl: () -> Unit,
    onMenuClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Animated border color based on state
    val borderColor by animateColorAsState(
        targetValue = when {
            isLoading -> AuraColors.PurpleStart
            isFocused -> AuraColors.Primary
            else -> AuraColors.Border
        },
        animationSpec = tween(AuraDimensions.SearchFocusDuration),
        label = "borderColor"
    )

    // Animated background color
    val backgroundColor by animateColorAsState(
        targetValue = AuraColors.Surface,
        animationSpec = tween(AuraDimensions.SearchFocusDuration),
        label = "backgroundColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AuraDimensions.ContentPaddingHorizontal),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(AuraDimensions.SearchBarHeight)
                .clip(RoundedCornerShape(AuraDimensions.SearchBarCornerRadius))
                .background(backgroundColor)
                .border(
                    width = AuraDimensions.SearchBarBorderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(AuraDimensions.SearchBarCornerRadius)
                )
                .padding(horizontal = AuraDimensions.SearchBarPaddingHorizontal),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isSearchMode) {
                // Search mode - text input (takes full space, receives all touch events)
                BasicTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    modifier = Modifier
                        .fillMaxWidth(),
                    interactionSource = interactionSource,
                    textStyle = AuraTypography.SearchBarText.copy(
                        color = AuraColors.Primary
                    ),
                    cursorBrush = SolidColor(AuraColors.PurpleStart),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            onSearch(url)
                            focusManager.clearFocus()
                            onSearchModeChange(false)
                        }
                    ),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (url.isEmpty()) {
                                    Text(
                                        text = "Search or enter url",
                                        style = AuraTypography.SearchBarPlaceholder,
                                        color = AuraColors.Secondary
                                    )
                                }
                                innerTextField()
                            }

                            // Clear button
                            if (url.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(AuraDimensions.CircleIconButtonSize)
                                        .clip(CircleShape)
                                        .background(AuraColors.ClearButton)
                                        .clickable { onUrlChange("") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Clear",
                                        tint = AuraColors.Surface,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            } else {
                // Display mode - URL text (clickable to enter search mode)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSearchModeChange(true) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (url.isNotEmpty()) url else "Search or enter url",
                        style = AuraTypography.SearchBarPlaceholder.copy(
                            color = if (url.isNotEmpty()) AuraColors.Primary else AuraColors.Secondary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Copy button
                    if (url.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(AuraDimensions.CircleIconButtonSize)
                                .clip(CircleShape)
                                .background(AuraColors.Success)
                                .clickable { onCopyUrl() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy URL",
                                tint = AuraColors.Surface,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Menu dot button
        Box(
            modifier = Modifier
                .size(AuraDimensions.MenuDotSize)
                .clip(CircleShape)
                .background(AuraColors.ClearButton)
                .clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ) {
            // Vertical dots icon
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Menu",
                tint = AuraColors.Primary,
                modifier = Modifier.size(AuraDimensions.MenuDotIconSize)
            )
        }
    }
}
