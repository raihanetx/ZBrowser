package com.zbrowser.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zbrowser.app.ui.theme.AuraColors
import com.zbrowser.app.ui.theme.AuraDimensions
import com.zbrowser.app.ui.theme.AuraTypography

/**
 * Data class representing a tab item in the navigation bar.
 */
data class TabItem(
    val id: String,
    val title: String,
    val url: String,
    val isActive: Boolean = false,
    val colorStart: Color = AuraColors.PurpleStart,
    val colorEnd: Color = AuraColors.PurpleEnd
)

/**
 * NavBar component - Slide-in purple pill navigation bar from the right edge.
 * Contains circular tab icons and a new tab button.
 */
@Composable
fun NavBar(
    tabs: List<TabItem>,
    isVisible: Boolean,
    onTabClick: (TabItem) -> Unit,
    onNewTabClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            animationSpec = tween(
                durationMillis = AuraDimensions.NavBarSlideDuration,
                easing = { fraction -> fraction * fraction * (3 - 2 * fraction) } // EaseOutCubic
            ),
            initialOffsetX = { it }
        ),
        exit = slideOutHorizontally(
            animationSpec = tween(
                durationMillis = AuraDimensions.NavBarSlideDuration / 2
            ),
            targetOffsetX = { it }
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(300.dp) // Fixed width for the nav bar
                .padding(end = 16.dp, top = 100.dp, bottom = 100.dp)
                .shadow(
                    elevation = AuraDimensions.NavBarShadowElevation,
                    shape = RoundedCornerShape(AuraDimensions.NavBarCornerRadius)
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = AuraColors.NavBarGradient
                    ),
                    shape = RoundedCornerShape(AuraDimensions.NavBarCornerRadius)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = AuraDimensions.NavBarPaddingHorizontal),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tab circles in a horizontal scrollable row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(AuraDimensions.TabCircleSpacing)
                ) {
                    items(tabs) { tab ->
                        TabCircle(
                            tab = tab,
                            onClick = { onTabClick(tab) }
                        )
                    }
                }

                // New tab button
                Box(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .size(AuraDimensions.TabCircleSize)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onNewTabClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New tab",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TabCircle(
    tab: TabItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = modifier
                .size(AuraDimensions.TabCircleSize)
                .clip(CircleShape)
                .background(
                    color = if (tab.isActive) Color.White
                    else tab.colorStart.copy(alpha = 0.3f)
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            if (tab.url == "home") {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = if (tab.isActive) AuraColors.PurpleDeep else Color.White,
                    modifier = Modifier.size(AuraDimensions.TabCircleIconSize)
                )
            } else {
                Text(
                    text = tab.title.firstOrNull()?.uppercase() ?: "?",
                    style = AuraTypography.TabCircleText,
                    color = if (tab.isActive) AuraColors.PurpleDeep else Color.White
                )
            }
        }

        // Active indicator dot
        if (tab.isActive) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(AuraColors.Success)
            )
        }
    }
}
