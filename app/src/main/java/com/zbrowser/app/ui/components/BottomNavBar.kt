package com.zbrowser.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zbrowser.app.ui.theme.AuraColors
import com.zbrowser.app.ui.theme.AuraDimensions
import com.zbrowser.app.ui.theme.AuraTypography

@Composable
fun BottomNavBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    isLoading: Boolean,
    tabCount: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onHome: () -> Unit,
    onTabs: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = AuraDimensions.BottomNavBarElevation,
                shape = RoundedCornerShape(
                    topStart = AuraDimensions.BottomNavBarCornerRadius,
                    topEnd = AuraDimensions.BottomNavBarCornerRadius
                ),
                ambientColor = AuraColors.BlueDark.copy(alpha = 0.08f),
                spotColor = AuraColors.BlueDark.copy(alpha = 0.12f)
            )
            .background(
                color = AuraColors.Surface,
                shape = RoundedCornerShape(
                    topStart = AuraDimensions.BottomNavBarCornerRadius,
                    topEnd = AuraDimensions.BottomNavBarCornerRadius
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AuraDimensions.BottomNavBarHeight)
                .padding(horizontal = AuraDimensions.BottomNavBarPaddingHorizontal),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                label = "Back",
                enabled = canGoBack,
                onClick = onBack
            )
            NavButton(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                label = "Forward",
                enabled = canGoForward,
                onClick = onForward
            )
            NavButton(
                icon = Icons.Default.Refresh,
                label = if (isLoading) "Stop" else "Refresh",
                enabled = true,
                onClick = onRefresh
            )
            NavButton(
                icon = Icons.Default.Home,
                label = "Home",
                enabled = true,
                onClick = onHome
            )
            NavButtonWithBadge(
                icon = Icons.Default.Tab,
                label = "Tabs",
                count = tabCount,
                onClick = onTabs
            )
            NavButton(
                icon = Icons.Default.MoreVert,
                label = "Menu",
                enabled = true,
                onClick = onMenu
            )
        }
    }
}

@Composable
private fun NavButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val iconColor by animateColorAsState(
        targetValue = when {
            !enabled -> AuraColors.TextHint
            isPressed -> AuraColors.BluePrimary
            else -> AuraColors.Secondary
        },
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "navIconColor"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(AuraDimensions.BottomNavBarIconSize)
        )
        Text(
            text = label,
            style = AuraTypography.BottomNavLabel,
            color = iconColor
        )
    }
}

@Composable
private fun NavButtonWithBadge(
    icon: ImageVector,
    label: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val iconColor by animateColorAsState(
        targetValue = if (isPressed) AuraColors.BluePrimary else AuraColors.Secondary,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "navIconColor"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(AuraDimensions.BottomNavBarIconSize)
            )
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(AuraColors.BluePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                    text = if (count > 9) "9+" else count.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }
        Text(
            text = label,
            style = AuraTypography.BottomNavLabel,
            color = iconColor
        )
    }
}
