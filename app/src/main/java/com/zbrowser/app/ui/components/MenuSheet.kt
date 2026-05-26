package com.zbrowser.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zbrowser.app.ui.theme.AuraColors
import com.zbrowser.app.ui.theme.AuraDimensions
import com.zbrowser.app.ui.theme.AuraTypography

data class MenuSection(
    val title: String,
    val items: List<MenuItem>
)

data class MenuItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val action: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuSheet(
    isVisible: Boolean,
    menuSections: List<MenuSection>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = AuraColors.Surface,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp
            ),
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(AuraColors.Border)
                    )
                }

                menuSections.forEach { section ->
                    // Section header
                    Text(
                        text = section.title,
                        style = AuraTypography.MenuSectionHeader,
                        color = AuraColors.Secondary,
                        modifier = Modifier.padding(
                            start = 24.dp,
                            top = 12.dp,
                            bottom = 8.dp
                        )
                    )

                    // Menu items grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(AuraDimensions.MenuItemGridColumns),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(AuraDimensions.MenuItemSpacing),
                        verticalArrangement = Arrangement.spacedBy(AuraDimensions.MenuItemSpacing),
                        modifier = Modifier.height(
                            if (section.items.size <= 4) 90.dp else 180.dp
                        )
                    ) {
                        items(section.items) { item ->
                            MenuItemCard(
                                item = item,
                                onClick = {
                                    item.action()
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MenuItemCard(
    item: MenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(AuraDimensions.MenuItemIconSize)
                .clip(RoundedCornerShape(14.dp))
                .background(AuraColors.BluePale),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = AuraColors.BluePrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Text(
            text = item.label,
            style = AuraTypography.MenuItemLabel,
            color = AuraColors.Secondary,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
