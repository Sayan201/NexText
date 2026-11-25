package com.nextext.sms.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextext.sms.domain.model.MessageCategory
import com.nextext.sms.ui.viewmodel.InboxFilter

/**
 * Ambient top glow for Pitch Black AMOLED mode (defaults to Purple).
 * Renders a smooth radial + vertical glow at the top of the screen that fades into #000000.
 */
@Composable
fun TopAmbientGlow(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        val width = size.width
        val height = size.height
        // Soft vertical wash at the top edge
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.24f),
                    accentColor.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = height
            )
        )
        // Centered radial spotlight glow behind top app bar
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.34f),
                    accentColor.copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = Offset(width * 0.5f, 0f),
                radius = width * 0.85f
            )
        )
    }
}

/**
 * Top category filter tabs (Personal | Transactions | Promotions | Starred)
 * placed at the top of InboxScreen.
 */
@Composable
fun TopCategoryFilterTabs(
    selectedFilter: InboxFilter,
    unreadCounts: Map<MessageCategory, Int>,
    accentColor: Color,
    onSelectFilter: (InboxFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple(InboxFilter.PERSONAL, "Personal", unreadCounts[MessageCategory.PERSONAL] ?: 0),
        Triple(InboxFilter.TRANSACTIONS, "Transactions", unreadCounts[MessageCategory.TRANSACTIONS] ?: 0),
        Triple(InboxFilter.PROMOTIONS, "Promotions", unreadCounts[MessageCategory.PROMOTIONS] ?: 0),
        Triple(InboxFilter.STARRED, "Starred", 0)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEach { (filter, label, unread) ->
            val isSelected = selectedFilter == filter
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectFilter(filter) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF9CA3AF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (unread > 0) {
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                    }
                }
                Spacer(Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .width(46.dp)
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isSelected) accentColor else Color.Transparent)
                )
            }
        }
    }
}

/**
 * Larger WhatsApp-style floating capsule with Twitter-like liquid glass / acrylic sheen
 * containing the primary destinations: Messages | Reminders | Finance.
 */
@Composable
fun LiquidGlassNavBottomBar(
    selectedTab: Int,
    unreadMessagesCount: Int,
    accentColor: Color,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        Triple(0, "Messages", Icons.Default.ChatBubble),
        Triple(1, "Reminders", Icons.Default.Notifications),
        Triple(2, "Finance", Icons.Default.CreditCard)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(36.dp),
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.25.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.50f),
                        accentColor.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.14f)
                    )
                )
            ),
            shadowElevation = 22.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xBA1A1D28),
                                Color(0xCC0D0F16),
                                Color(0xDB08090E)
                            )
                        )
                    )
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.06f),
                                accentColor.copy(alpha = 0.10f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { (index, label, icon) ->
                        val isSelected = selectedTab == index
                        val pillShape = RoundedCornerShape(28.dp)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(pillShape)
                                .then(
                                    if (isSelected) {
                                        Modifier
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        accentColor.copy(alpha = 0.36f),
                                                        accentColor.copy(alpha = 0.16f)
                                                    )
                                                )
                                            )
                                            .border(
                                                width = 0.85.dp,
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.35f),
                                                        accentColor.copy(alpha = 0.25f)
                                                    )
                                                ),
                                                shape = pillShape
                                            )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onSelectTab(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) Color.White else Color(0xFFA1A7B3),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    if (index == 0 && unreadMessagesCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEF4444))
                                                .align(Alignment.TopEnd)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFFA1A7B3),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
