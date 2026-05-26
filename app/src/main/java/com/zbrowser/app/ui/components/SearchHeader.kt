package com.zbrowser.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zbrowser.app.ui.theme.AuraColors
import com.zbrowser.app.ui.theme.AuraDimensions
import com.zbrowser.app.ui.theme.AuraTypography
import kotlinx.coroutines.delay

@Composable
fun SearchHeader(
    url: String,
    isSearchMode: Boolean,
    isSecure: Boolean,
    isLoading: Boolean,
    onUrlChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSearchModeChange: (Boolean) -> Unit,
    onCopyUrl: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            delay(100)
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused -> AuraColors.BluePrimary
            isLoading -> AuraColors.BlueLighter
            else -> AuraColors.Border
        },
        animationSpec = tween(AuraDimensions.SearchFocusDuration),
        label = "borderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isFocused || isSearchMode) AuraColors.Surface else AuraColors.SurfaceVariant,
        animationSpec = tween(AuraDimensions.SearchFocusDuration),
        label = "backgroundColor"
    )

    // Shimmer effect when loading
    val shimmerOffset = if (isLoading) {
        val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        val offset by infiniteTransition.animateFloat(
            initialValue = -1f,
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerOffset"
        )
        offset
    } else 0f

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
                .then(
                    if (isLoading) {
                        Modifier.background(
                            brush = Brush.linearGradient(
                                colors = AuraColors.SearchShimmer,
                                start = Offset(shimmerOffset * 500f, 0f),
                                end = Offset((shimmerOffset + 0.5f) * 500f, 0f)
                            )
                        )
                    } else Modifier
                )
                .border(
                    width = AuraDimensions.SearchBarBorderWidth,
                    color = borderColor,
                    shape = RoundedCornerShape(AuraDimensions.SearchBarCornerRadius)
                )
                .padding(horizontal = AuraDimensions.SearchBarPaddingHorizontal),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isSearchMode) {
                BasicTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused },
                    textStyle = AuraTypography.SearchBarText.copy(
                        color = AuraColors.Primary
                    ),
                    cursorBrush = SolidColor(AuraColors.BluePrimary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (url.isNotEmpty()) onSearch(url)
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
                                        text = "Search or enter URL",
                                        style = AuraTypography.SearchBarPlaceholder,
                                        color = AuraColors.TextHint
                                    )
                                }
                                innerTextField()
                            }
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
                                        text = "\u2715",
                                        color = AuraColors.Secondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSearchModeChange(true) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // SSL Lock Icon
                    Icon(
                        imageVector = if (isSecure) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (isSecure) "Secure" else "Not secure",
                        tint = if (isSecure) AuraColors.Success else AuraColors.Danger,
                        modifier = Modifier.size(16.dp)
                    )

                    // URL Text
                    Text(
                        text = if (url.isNotEmpty()) {
                            url.removePrefix("https://").removePrefix("http://")
                        } else "Search or enter URL",
                        style = AuraTypography.SearchBarPlaceholder.copy(
                            color = if (url.isNotEmpty()) AuraColors.Primary else AuraColors.TextHint
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Copy button
                    if (url.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(AuraDimensions.CircleIconButtonSize)
                                .clip(CircleShape)
                                .background(AuraColors.BluePale)
                                .clickable { onCopyUrl() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy URL",
                                tint = AuraColors.BluePrimary,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }

        // Three-dot menu button (right side)
        Box(
            modifier = Modifier
                .size(AuraDimensions.MenuDotSize)
                .clip(CircleShape)
                .border(1.dp, AuraColors.Border, CircleShape)
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
    }
}
