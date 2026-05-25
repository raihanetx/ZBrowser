package com.zbrowser.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zbrowser.app.ui.theme.AuraColors
import com.zbrowser.app.ui.theme.AuraDimensions
import com.zbrowser.app.ui.theme.AuraTypography
import kotlinx.coroutines.delay

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
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    var isTextFieldReady by remember { mutableStateOf(false) }

    // Request focus when entering search mode and text field is ready
    LaunchedEffect(isSearchMode, isTextFieldReady) {
        if (isSearchMode && isTextFieldReady) {
            delay(150) // Small delay to ensure text field is fully composed
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // FocusRequester not yet attached, ignore
            }
        }
    }

    // Animated border color based on state
    val borderColor by animateColorAsState(
        targetValue = when {
            isLoading -> AuraColors.GrayMedium
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
                // Search mode - text input
                BasicTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                            isTextFieldReady = true
                            if (!focusState.isFocused && url.isEmpty()) {
                                onSearchModeChange(false)
                            }
                        },
                    textStyle = AuraTypography.SearchBarText.copy(
                        color = AuraColors.Primary
                    ),
                    cursorBrush = SolidColor(AuraColors.GrayMedium),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (url.isNotEmpty()) {
                                onSearch(url)
                            }
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
                                    Text(
                                        text = "✕",
                                        color = AuraColors.Surface,
                                        style = AuraTypography.SearchBarText
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
                        .clickable {
                            onSearchModeChange(true)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Three-dot menu icon on the left
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable { onMenuClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = AuraColors.Secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = if (url.isNotEmpty()) url else "Search or enter url",
                        style = AuraTypography.SearchBarPlaceholder.copy(
                            color = if (url.isNotEmpty()) AuraColors.Primary else AuraColors.Secondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
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
    }
}
