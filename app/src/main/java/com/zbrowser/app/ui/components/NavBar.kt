package com.zbrowser.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zbrowser.app.ui.theme.AuraColors
import com.zbrowser.app.ui.theme.AuraDimensions
import com.zbrowser.app.ui.theme.AuraTypography

data class TabItem(
    val id: String,
    val title: String,
    val url: String,
    val isActive: Boolean = false,
    val colorStart: Color = AuraColors.BlueLighter,
    val colorEnd: Color = AuraColors.BlueLight
)

@Composable
fun NavBar(
    tabs: List<TabItem>,
    isVisible: Boolean,
    onTabClick: (TabItem) -> Unit,
    onCloseTab: (TabItem) -> Unit,
    onNewTabClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            animationSpec = tween(AuraDimensions.NavBarSlideDuration),
            initialOffsetX = { it }
        ) + fadeIn(tween(AuraDimensions.NavBarSlideDuration)),
        exit = slideOutHorizontally(
            animationSpec = tween(AuraDimensions.NavBarSlideDuration / 2),
            targetOffsetX = { it }
        ) + fadeOut(tween(AuraDimensions.NavBarSlideDuration / 2))
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(AuraDimensions.NavBarHeight)
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .shadow(
                    elevation = AuraDimensions.NavBarShadowElevation,
                    shape = RoundedCornerShape(AuraDimensions.NavBarCornerRadius),
                    ambientColor = AuraColors.BlueDark.copy(alpha = 0.15f),
                    spotColor = AuraColors.BlueDark.copy(alpha = 0.2f)
                )
                .background(
                    brush = Brush.horizontalGradient(
                        colors = AuraColors.NavBarGradient
                    ),
                    shape = RoundedCornerShape(AuraDimensions.NavBarCornerRadius)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = AuraDimensions.NavBarPaddingHorizontal),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close all button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable {
                            tabs.forEach { onCloseTab(it) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close all",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Tab circles in a horizontal scrollable row
                LazyRow(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(AuraDimensions.TabCircleSpacing)
                ) {
                    items(tabs, key = { it.id }) { tab ->
                        TabCircle(
                            tab = tab,
                            onClick = { onTabClick(tab) },
                            onClose = { onCloseTab(tab) }
                        )
                    }
                }

                // New tab button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onNewTabClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New tab",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
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
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(AuraDimensions.TabCircleSize)
            .clip(CircleShape)
            .background(
                color = if (tab.isActive) Color.White
                else Color.White.copy(alpha = 0.2f)
            )
            .then(
                if (tab.isActive) Modifier.border(2.dp, Color.White, CircleShape)
                else Modifier
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (tab.url == "home") {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                tint = if (tab.isActive) AuraColors.BluePrimary else Color.White,
                modifier = Modifier.size(AuraDimensions.TabCircleIconSize)
            )
        } else {
            Text(
                text = tab.title.firstOrNull()?.uppercase() ?: "?",
                style = AuraTypography.TabCircleText.copy(
                    fontSize = AuraDimensions.TabCircleTextSize
                ),
                color = if (tab.isActive) AuraColors.BluePrimary else Color.White
            )
        }

        // Close button on active tab
        if (tab.isActive && tab.url != "home") {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(AuraDimensions.TabCloseButtonSize)
                    .clip(CircleShape)
                    .background(AuraColors.Danger)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\u2715",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
