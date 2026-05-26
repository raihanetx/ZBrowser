package com.zbrowser.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zbrowser.app.ui.theme.AuraColors
import com.zbrowser.app.ui.theme.AuraDimensions
import com.zbrowser.app.ui.theme.AuraTypography

data class QuickLink(
    val id: String,
    val title: String,
    val url: String,
    val colorStart: Color,
    val colorEnd: Color,
    val initial: String
)

data class HistoryEntry(
    val id: String,
    val title: String,
    val url: String,
    val timeAgo: String
)

@Composable
fun HomePage(
    quickLinks: List<QuickLink>,
    recentHistory: List<HistoryEntry>,
    onQuickLinkClick: (QuickLink) -> Unit,
    onHistoryClick: (HistoryEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = AuraDimensions.ContentPaddingHorizontal,
            vertical = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Greeting / Brand
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(AuraColors.BlueGradient)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Z",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ZBrowser",
                    style = AuraTypography.PageTitle,
                    color = AuraColors.Primary
                )
                Text(
                    text = "Fast. Secure. Private.",
                    style = AuraTypography.SuggestionUrl,
                    color = AuraColors.Secondary
                )
            }
        }

        // Quick Links Section
        item {
            Text(
                text = "Quick Links",
                style = AuraTypography.SectionTitle,
                color = AuraColors.Primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        item {
            QuickLinksGrid(
                links = quickLinks,
                onLinkClick = onQuickLinkClick
            )
        }

        // Recent History Section
        if (recentHistory.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent",
                        style = AuraTypography.SectionTitle,
                        color = AuraColors.Primary
                    )
                }
            }

            items(recentHistory) { entry ->
                HistoryRow(
                    entry = entry,
                    onClick = { onHistoryClick(entry) }
                )
            }
        }

        // Bottom spacer for nav bar
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun QuickLinksGrid(
    links: List<QuickLink>,
    onLinkClick: (QuickLink) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AuraDimensions.QuickLinkSpacing)
    ) {
        // Split into rows of 4
        links.chunked(4).forEach { rowLinks ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AuraDimensions.QuickLinkSpacing)
            ) {
                rowLinks.forEach { link ->
                    QuickLinkItem(
                        link = link,
                        onClick = { onLinkClick(link) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining space if row is not full
                repeat(4 - rowLinks.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickLinkItem(
    link: QuickLink,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AuraDimensions.QuickLinkCornerRadius))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(AuraDimensions.QuickLinkIconSize)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(link.colorStart, link.colorEnd)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = link.initial,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = link.title,
            style = AuraTypography.QuickLinkLabel,
            color = AuraColors.Secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(AuraDimensions.HistoryItemHeight)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AuraColors.BluePale),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = AuraColors.BluePrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = entry.title,
                style = AuraTypography.SuggestionItem,
                color = AuraColors.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = entry.url,
                style = AuraTypography.SuggestionUrl,
                color = AuraColors.Secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = entry.timeAgo,
            style = AuraTypography.SuggestionUrl,
            color = AuraColors.TextHint
        )

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = AuraColors.TextHint,
            modifier = Modifier.size(18.dp)
        )
    }
}
