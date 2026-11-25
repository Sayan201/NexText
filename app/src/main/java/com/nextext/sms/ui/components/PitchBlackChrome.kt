package com.nextext.sms.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextext.sms.domain.model.MessageCategory
import com.nextext.sms.ui.viewmodel.InboxFilter
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlin.math.abs
import kotlin.math.roundToInt

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
 * Zangetsu-style crystal clear liquid glass bottom bar:
 * - Left: Crystal clear frosted capsule with continuous spring-sliding lens that expands
 *   outward beyond the capsule edges while switching or dragging.
 * - Right: Separate circular crystal liquid glass New Message button.
 */
@Composable
fun LiquidGlassNavBottomBar(
    selectedTab: Int,
    unreadMessagesCount: Int,
    accentColor: Color,
    hazeState: HazeState,
    onSelectTab: (Int) -> Unit,
    onComposeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        Triple(0, "Messages", Icons.Default.ChatBubble),
        Triple(1, "Reminders", Icons.Default.Notifications),
        Triple(2, "Finance", Icons.Default.CreditCard)
    )

    val capsuleShape = RoundedCornerShape(34.dp)
    val pillShape = RoundedCornerShape(28.dp)
    val density = LocalDensity.current

    var isDragging by remember { mutableStateOf(false) }
    var dragIndex by remember { mutableFloatStateOf(selectedTab.toFloat()) }

    val animatedIndex by animateFloatAsState(
        targetValue = if (isDragging) dragIndex else selectedTab.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "crystalPillSlide"
    )

    val isSwitching = isDragging || abs(animatedIndex - selectedTab.toFloat()) > 0.04f

    val lensScaleX by animateFloatAsState(
        targetValue = if (isSwitching) 1.22f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "crystalScaleX"
    )
    val lensScaleY by animateFloatAsState(
        targetValue = if (isSwitching) 1.44f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "crystalScaleY"
    )
    val lensCrystalAlpha by animateFloatAsState(
        targetValue = if (isSwitching) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "crystalAlpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Crystal Clear Navigation Capsule
        Box(
            modifier = Modifier
                .weight(1f)
                .height(62.dp)
        ) {
            // Crystal frosted glass background (clipped to capsuleShape so outer Box remains unclipped for lens bulge)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(capsuleShape)
                    .hazeChild(
                        state = hazeState,
                        style = HazeDefaults.style(
                            backgroundColor = Color(0x14FFFFFF),
                            blurRadius = 11.dp,
                            tint = HazeTint(Color(0x14000000))
                        )
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.White.copy(alpha = 0.03f),
                                Color.Black.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.42f),
                                Color.White.copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.24f)
                            )
                        ),
                        shape = capsuleShape
                    )
            )

            // Unclipped foreground layer for continuous expanding liquid glass lens & tab items
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 5.dp, vertical = 5.dp)
            ) {
                val slotWidth = maxWidth / tabs.size
                val slotWidthPx = with(density) { slotWidth.toPx() }

                // Expanding crystal lens indicator
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (animatedIndex * slotWidthPx).roundToInt(),
                                y = 0
                            )
                        }
                        .width(slotWidth)
                        .height(52.dp)
                        .graphicsLayer {
                            scaleX = lensScaleX
                            scaleY = lensScaleY
                        }
                        .clip(pillShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.10f + 0.14f * lensCrystalAlpha),
                                    accentColor.copy(alpha = 0.24f * (1f - 0.55f * lensCrystalAlpha)),
                                    Color.White.copy(alpha = 0.05f + 0.08f * lensCrystalAlpha)
                                )
                            )
                        )
                        .border(
                            width = (0.85f + 0.5f * lensCrystalAlpha).dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.35f + 0.45f * lensCrystalAlpha),
                                    Color.White.copy(alpha = 0.10f + 0.25f * lensCrystalAlpha),
                                    Color.White.copy(alpha = 0.20f + 0.30f * lensCrystalAlpha)
                                )
                            ),
                            shape = pillShape
                        )
                )

                // Tab items row + horizontal drag scrubbing
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(tabs.size, slotWidthPx) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    dragIndex = ((offset.x / slotWidthPx) - 0.5f)
                                        .coerceIn(0f, (tabs.size - 1).toFloat())
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragIndex = (dragIndex + dragAmount / slotWidthPx)
                                        .coerceIn(0f, (tabs.size - 1).toFloat())
                                },
                                onDragEnd = {
                                    val target = dragIndex.roundToInt().coerceIn(0, tabs.size - 1)
                                    isDragging = false
                                    onSelectTab(target)
                                },
                                onDragCancel = {
                                    isDragging = false
                                }
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { (index, label, icon) ->
                        val distance = abs(animatedIndex - index.toFloat())
                        val isActive = distance < 0.5f

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(pillShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onSelectTab(index) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isActive) accentColor else Color(0xFFE5E7EB),
                                    modifier = Modifier.size(21.dp)
                                )
                                if (index == 0 && unreadMessagesCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.5.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444))
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isActive) Color.White else Color(0xFFD1D5DB),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Right: Separate Circular Crystal Liquid Glass New Message Button
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .hazeChild(
                    state = hazeState,
                    style = HazeDefaults.style(
                        backgroundColor = Color(0x14FFFFFF),
                        blurRadius = 11.dp,
                        tint = HazeTint(Color(0x14000000))
                    )
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.04f),
                            Color.Black.copy(alpha = 0.12f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.25f)
                        )
                    ),
                    shape = CircleShape
                )
                .clickable(onClick = onComposeClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "New message",
                tint = Color.White,
                modifier = Modifier.size(23.dp)
            )
        }
    }
}
