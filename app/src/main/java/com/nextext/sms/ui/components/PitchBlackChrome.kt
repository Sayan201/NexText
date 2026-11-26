package com.nextext.sms.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
 * Real AGSL Liquid Glass Refraction Shader (Android 13+ / API 33+)
 * Reads actual underlying pixels from `uniform shader contents` and applies:
 * - Rounded-capsule SDF edge bending (Snell's Law refraction, refractiveIndex = 1.24, thickness = 26)
 * - Soft crystal micro-blur (Zangetsu blur = 3)
 * - Color saturation boost (1.15)
 * - Fresnel rim lighting (0.55) + directional top/bottom specular bevel (rimShade = 0.45, rimLight = 0.12)
 * - Upper-right caustic accent reflection
 */
private const val LIQUID_GLASS_REFRACTION_AGSL = """
uniform shader contents;
uniform float2 uSize;
uniform float uRadius;
uniform float4 uAccent;
uniform float uExpand;

float sdRoundedBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + float2(r);
    return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0))) - r;
}

half4 main(float2 fragCoord) {
    float2 halfSize = uSize * 0.5;
    float2 p = fragCoord - halfSize;
    float r = min(uRadius, min(halfSize.x, halfSize.y));
    float d = sdRoundedBox(p, halfSize, r);

    if (d > 0.0) {
        return half4(0.0);
    }

    float2 eps = float2(1.5, 0.0);
    float2 n = normalize(float2(
        sdRoundedBox(p + eps.xy, halfSize, r) - sdRoundedBox(p - eps.xy, halfSize, r),
        sdRoundedBox(p + eps.yx, halfSize, r) - sdRoundedBox(p - eps.yx, halfSize, r)
    ));

    float distIn = -d;
    float thickness = 28.0 + 8.0 * uExpand;
    float edgeRatio = clamp(distIn / thickness, 0.0, 1.0);

    // Strong Snell's Law refraction concentrated in the curved rim (center stays crystal clear)
    float bendFactor = pow(1.0 - edgeRatio, 2.2);
    float bendAmount = bendFactor * (26.0 + 20.0 * uExpand);
    float2 refractCoord = clamp(fragCoord - n * bendAmount, float2(1.0), uSize - float2(1.0));

    // Sharp center, slight optical dispersion/softness only near rim
    float rimBlur = 2.2 * (1.0 - edgeRatio);
    half4 c0 = contents.eval(refractCoord);
    float3 rgb = c0.rgb;
    if (rimBlur > 0.35) {
        half4 c1 = contents.eval(clamp(refractCoord + float2(rimBlur, 0.0), float2(1.0), uSize - float2(1.0)));
        half4 c2 = contents.eval(clamp(refractCoord - float2(rimBlur, 0.0), float2(1.0), uSize - float2(1.0)));
        half4 c3 = contents.eval(clamp(refractCoord + float2(0.0, rimBlur), float2(1.0), uSize - float2(1.0)));
        half4 c4 = contents.eval(clamp(refractCoord - float2(0.0, rimBlur), float2(1.0), uSize - float2(1.0)));
        rgb = mix(rgb, ((c0 + c1 + c2 + c3 + c4) * 0.2).rgb, 0.65);
    }

    // Saturation boost (Zangetsu saturation = 1.15)
    float luma = dot(rgb, float3(0.299, 0.587, 0.114));
    rgb = mix(float3(luma), rgb, 1.15);

    // Neutral dark-crystal body (no colored center wash; darker when expanded droplet)
    float bodyDarken = mix(0.22, 0.55, uExpand);
    rgb = mix(rgb, float3(0.02, 0.025, 0.035), bodyDarken * smoothstep(0.0, 0.5, edgeRatio));

    // Zangetsu double-band 3D liquid glass bevel:
    // 1) Outer crisp hairline rim (0..2.5px)
    float outerHairline = smoothstep(2.6, 0.0, distIn);
    // 2) Dark rimShade groove (2.5..6.5px, rimShade = 0.45)
    float groove = smoothstep(1.5, 4.0, distIn) * (1.0 - smoothstep(4.0, 7.5, distIn));
    rgb *= (1.0 - 0.55 * groove);
    // 3) Thick glossy inner bevel highlight (5..18px, concentrated top-left & bottom-right)
    float innerBevelBand = smoothstep(3.5, 8.5, distIn) * (1.0 - smoothstep(8.5, 20.0, distIn));

    // Directional light from top-left (-0.45, -0.89) and secondary bottom reflection
    float topLeftLight = clamp(dot(-n, normalize(float2(0.35, 0.94))), 0.0, 1.0);
    float bottomLight = clamp(n.y, 0.0, 1.0);
    float dirLight = pow(topLeftLight, 1.3) * 0.95 + pow(bottomLight, 1.6) * 0.55;

    float fresnelGlow = pow(1.0 - edgeRatio, 2.8) * (0.32 + 0.30 * uExpand);
    float spec = outerHairline * (0.45 + 0.35 * uExpand + 0.25 * dirLight)
               + innerBevelBand * dirLight * (0.28 + 0.62 * uExpand)
               + fresnelGlow * 0.22;

    rgb += float3(1.0) * spec;

    return half4(clamp(rgb, 0.0, 1.0), 1.0);
}
"""

/**
 * Ambient top glow for Pitch Black AMOLED mode (defaults to Cyan).
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

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun RealRefractionGlassLayer(
    backdropLayer: GraphicsLayer,
    backdropOriginInRoot: Offset,
    shape: Shape,
    cornerRadiusDp: Float,
    accentColor: Color,
    expandProgress: Float,
    modifier: Modifier = Modifier
) {
    val shader = remember { RuntimeShader(LIQUID_GLASS_REFRACTION_AGSL) }
    val density = LocalDensity.current
    var elementPosInRoot by remember { mutableStateOf(Offset.Zero) }
    var elementSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    Canvas(
        modifier = modifier
            .onGloballyPositioned { coords ->
                elementPosInRoot = coords.positionInRoot()
                elementSize = androidx.compose.ui.geometry.Size(
                    coords.size.width.toFloat(),
                    coords.size.height.toFloat()
                )
            }
            .clip(shape)
            .graphicsLayer {
                if (elementSize.width > 1f && elementSize.height > 1f) {
                    val radiusPx = with(density) { cornerRadiusDp.dp.toPx() }
                    shader.setFloatUniform("uSize", elementSize.width, elementSize.height)
                    shader.setFloatUniform("uRadius", radiusPx)
                    shader.setFloatUniform(
                        "uAccent",
                        accentColor.red,
                        accentColor.green,
                        accentColor.blue,
                        accentColor.alpha
                    )
                    shader.setFloatUniform("uExpand", expandProgress)
                    renderEffect = RenderEffect
                        .createRuntimeShaderEffect(shader, "contents")
                        .asComposeRenderEffect()
                }
            }
    ) {
        val offsetX = elementPosInRoot.x - backdropOriginInRoot.x
        val offsetY = elementPosInRoot.y - backdropOriginInRoot.y
        translate(left = -offsetX, top = -offsetY) {
            drawLayer(backdropLayer)
        }
    }
}

/**
 * 1-to-1 Zangetsu lower liquid glass capsule + circular button:
 * - Real pixel-level Snell's Law edge bending via AGSL RuntimeShader (API 33+)
 *   over the live NavHost GraphicsLayer (with Haze fallback on older Android).
 * - Selection lens pops outward (+10.dp horizontal/vertical) with 1.08x magnification
 *   when pressing, dragging, or switching tabs.
 */
@Composable
fun LiquidGlassNavBottomBar(
    selectedTab: Int,
    unreadMessagesCount: Int,
    accentColor: Color,
    hazeState: HazeState,
    backdropLayer: GraphicsLayer?,
    backdropOriginInRoot: Offset,
    swapPositions: Boolean = false,
    onSelectTab: (Int) -> Unit,
    onComposeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        Triple(0, "Messages", Icons.Default.ChatBubble),
        Triple(1, "Reminders", Icons.Default.Notifications),
        Triple(2, "Finance", Icons.Default.CreditCard)
    )

    val capsuleShape = RoundedCornerShape(30.dp)
    val pillShape = RoundedCornerShape(28.dp)
    val density = LocalDensity.current

    val currentOnSelectTab by rememberUpdatedState(onSelectTab)
    val currentSelectedTab by rememberUpdatedState(selectedTab)

    var isDragging by remember { mutableStateOf(false) }
    var dragIndex by remember { mutableFloatStateOf(selectedTab.toFloat()) }
    val interactionSources = remember { List(tabs.size) { MutableInteractionSource() } }
    val pressedStates = interactionSources.map { it.collectIsPressedAsState().value }
    val isPressed = pressedStates.any { it }

    val animatedIndex by animateFloatAsState(
        targetValue = if (isDragging) dragIndex else selectedTab.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "zangetsuSlide"
    )

    val isSwitching = isDragging || isPressed || abs(animatedIndex - selectedTab.toFloat()) > 0.02f

    val expandProgress by animateFloatAsState(
        targetValue = if (isSwitching) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "zangetsuExpand"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (swapPositions) {
            CircularCrystalComposeButton(
                accentColor = accentColor,
                hazeState = hazeState,
                backdropLayer = backdropLayer,
                backdropOriginInRoot = backdropOriginInRoot,
                onClick = onComposeClick
            )
        }

        // Crystal Clear Navigation Capsule (60.dp)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(60.dp)
        ) {
            // 1. Clipped capsule backdrop that shrinks slightly when selecting (so top droplet pops out further)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = 1f - 0.03f * expandProgress
                        scaleY = 1f - 0.12f * expandProgress
                    }
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && backdropLayer != null) {
                    RealRefractionGlassLayer(
                        backdropLayer = backdropLayer,
                        backdropOriginInRoot = backdropOriginInRoot,
                        shape = capsuleShape,
                        cornerRadiusDp = 30f,
                        accentColor = accentColor,
                        expandProgress = 0f,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(capsuleShape)
                            .hazeChild(
                                state = hazeState,
                                style = HazeDefaults.style(
                                    backgroundColor = Color(0x1F000000),
                                    blurRadius = 4.dp,
                                    tint = HazeTint(Color(0x14FFFFFF))
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.45f),
                                        Color.White.copy(alpha = 0.12f),
                                        Color.White.copy(alpha = 0.28f)
                                    )
                                ),
                                shape = capsuleShape
                            )
                    )
                }
            }

            // 2. Unclipped foreground layer for expanding liquid glass droplet + tabs
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp)
            ) {
                val slotWidth = maxWidth / tabs.size
                val slotWidthPx = with(density) { slotWidth.toPx() }
                val expandPx = with(density) { (9.dp * expandProgress).toPx() }

                // Resting subtle indicator (Zangetsu indicatorColor = accent.withValues(alpha = 0.14))
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset {
                            IntOffset(
                                x = (animatedIndex * slotWidthPx).roundToInt(),
                                y = 0
                            )
                        }
                        .width(slotWidth)
                        .height(52.dp)
                        .clip(pillShape)
                        .background(accentColor.copy(alpha = 0.14f * (1f - expandProgress)))
                )

                // Expanding 3D Liquid Glass Droplet (requiredSize bypasses 60.dp parent clamp so it floats outward!)
                if (expandProgress > 0.01f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset {
                                IntOffset(
                                    x = (animatedIndex * slotWidthPx - expandPx).roundToInt(),
                                    y = 0
                                )
                            }
                            .requiredSize(
                                width = slotWidth + (18.dp * expandProgress),
                                height = 52.dp + (26.dp * expandProgress)
                            )
                            .graphicsLayer { alpha = expandProgress.coerceIn(0f, 1f) }
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && backdropLayer != null) {
                            RealRefractionGlassLayer(
                                backdropLayer = backdropLayer,
                                backdropOriginInRoot = backdropOriginInRoot,
                                shape = pillShape,
                                cornerRadiusDp = 28f,
                                accentColor = accentColor,
                                expandProgress = expandProgress,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(pillShape)
                                .hazeChild(
                                    state = hazeState,
                                    style = HazeDefaults.style(
                                        backgroundColor = Color(0x1F000000),
                                        blurRadius = 6.dp,
                                        tint = HazeTint(accentColor.copy(alpha = 0.14f))
                                    )
                                )
                                .border(
                                    width = (1f + 0.5f * expandProgress).dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.45f + 0.45f * expandProgress),
                                            Color.White.copy(alpha = 0.14f),
                                            Color.White.copy(alpha = 0.30f + 0.40f * expandProgress)
                                        )
                                    ),
                                    shape = pillShape
                                )
                        )
                        }
                    }
                }

                // Tab items row + bidirectional continuous drag scrubber
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
                                    if (target != currentSelectedTab) {
                                        currentOnSelectTab(target)
                                    }
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
                        val itemScale = if (isActive) 1f + 0.08f * expandProgress else 1f

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .padding(horizontal = 2.dp)
                                .graphicsLayer {
                                    scaleX = itemScale
                                    scaleY = itemScale
                                }
                                .clip(pillShape)
                                .clickable(
                                    interactionSource = interactionSources[index],
                                    indication = null
                                ) { onSelectTab(index) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = when {
                                        isActive && expandProgress > 0.35f -> Color.White
                                        isActive -> accentColor
                                        else -> Color.White
                                    },
                                    modifier = Modifier.size(22.dp)
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
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isActive) Color.White else Color(0xFFB4BAC6),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        if (!swapPositions) {
            CircularCrystalComposeButton(
                accentColor = accentColor,
                hazeState = hazeState,
                backdropLayer = backdropLayer,
                backdropOriginInRoot = backdropOriginInRoot,
                onClick = onComposeClick
            )
        }
    }
}

@Composable
private fun CircularCrystalComposeButton(
    accentColor: Color,
    hazeState: HazeState,
    backdropLayer: GraphicsLayer?,
    backdropOriginInRoot: Offset,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && backdropLayer != null) {
            RealRefractionGlassLayer(
                backdropLayer = backdropLayer,
                backdropOriginInRoot = backdropOriginInRoot,
                shape = CircleShape,
                cornerRadiusDp = 28f,
                accentColor = accentColor,
                expandProgress = 0f,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .hazeChild(
                        state = hazeState,
                        style = HazeDefaults.style(
                            backgroundColor = Color(0x1F000000),
                            blurRadius = 4.dp,
                            tint = HazeTint(Color(0x14FFFFFF))
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.55f),
                                Color.White.copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.34f)
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "New message",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}
