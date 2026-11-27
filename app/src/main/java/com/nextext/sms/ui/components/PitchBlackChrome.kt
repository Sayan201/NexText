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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
 * Ported from Zangetsu's `liquidNavGlassSettings` (bodyMode = clear):
 * - Rounded-capsule SDF edge bending (Snell's Law refraction, refractiveIndex = 1.24, thickness = 26)
 * - `uFrost` drives a 12-tap disc blur + milky white film — 0 is crystal clear, 1 is heavy frost.
 *   It never paints a black scrim over the backdrop; depth comes from bodyShade only.
 * - Color saturation boost (1.15)
 * - Fresnel rim lighting (0.55) + directional top/bottom specular bevel (rimShade = 0.45, rimLight = 0.12)
 * - Soft outer glow (glowIntensity = 0.12) on the popped droplet
 */
private const val LIQUID_GLASS_REFRACTION_AGSL = """
uniform shader contents;
uniform float2 uSize;
uniform float uRadius;
uniform float4 uAccent;
uniform float uExpand;
uniform float uFrost;
uniform float uRim;

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
    float frost = clamp(uFrost, 0.0, 1.0);
    float expandP = clamp(uExpand, 0.0, 1.0);
    float thickness = 28.0 + 8.0 * expandP;
    float edgeRatio = clamp(distIn / thickness, 0.0, 1.0);

    // Strong Snell's Law refraction concentrated in the curved rim (center stays crystal clear)
    float bendFactor = pow(1.0 - edgeRatio, 2.2);
    float bendAmount = bendFactor * (26.0 + 20.0 * expandP);
    float2 refractCoord = clamp(fragCoord - n * bendAmount, float2(1.0), uSize - float2(1.0));
    float2 lo = float2(1.0);
    float2 hi = uSize - float2(1.0);

    // Frost body: 12-tap golden-angle disc, radius grows with uFrost (Zangetsu blur 3 at ~35%)
    half4 acc = contents.eval(refractCoord);
    float taps = 1.0;
    if (frost > 0.02) {
        float radius = 1.5 + 9.5 * frost;
        for (int i = 0; i < 12; i++) {
            float fi = float(i);
            float ang = fi * 2.39996323;
            float rr = radius * sqrt((fi + 0.5) / 12.0);
            float2 off = float2(cos(ang), sin(ang)) * rr;
            acc += contents.eval(clamp(refractCoord + off, lo, hi));
            taps += 1.0;
        }
    }
    float3 rgb = (acc / taps).rgb;

    // Rim-only optical softness; fades out as the body frost takes over
    float rimBlur = 2.2 * (1.0 - edgeRatio) * (1.0 - 0.6 * frost);
    if (rimBlur > 0.35) {
        half4 c1 = contents.eval(clamp(refractCoord + float2(rimBlur, 0.0), lo, hi));
        half4 c2 = contents.eval(clamp(refractCoord - float2(rimBlur, 0.0), lo, hi));
        half4 c3 = contents.eval(clamp(refractCoord + float2(0.0, rimBlur), lo, hi));
        half4 c4 = contents.eval(clamp(refractCoord - float2(0.0, rimBlur), lo, hi));
        rgb = mix(rgb, (rgb + c1.rgb + c2.rgb + c3.rgb + c4.rgb) * 0.2, 0.6);
    }

    // Saturation boost (1.15)
    float luma = dot(rgb, float3(0.299, 0.587, 0.114));
    rgb = mix(float3(luma), rgb, 1.15);

    // Milky frost film (Zangetsu glassColor white @ 0.12) — lightens, never darkens
    rgb = mix(rgb, mix(rgb, float3(1.0), 0.42) + float3(0.02), 0.20 * frost);

    // bodyShade 0.12, bottom-weighted so labels keep contrast on bright backdrops
    float bodyShade = smoothstep(-0.1, 1.0, p.y / max(halfSize.y, 1.0)) * 0.12;
    rgb *= (1.0 - bodyShade);

    // Local capsule-rim pinch illusion inside the held droplet's curved edges only
    if (uExpand > 0.05) {
        float pinchedHalfH = halfSize.y * 0.72;
        float capEdgeDist = abs(abs(p.y) - pinchedHalfH);
        float capEdgeLine = smoothstep(2.4, 0.0, capEdgeDist) * (1.0 - smoothstep(0.0, 0.55, edgeRatio));
        rgb += float3(1.0) * capEdgeLine * 0.22 * uExpand;
    }

    // Double-band 3D liquid glass bevel scaled by configurable rim brightness
    float rimMult = mix(uRim * 2.22, max(uRim * 2.22, 1.0), uExpand);
    float outerHairline = smoothstep(2.6, 0.0, distIn);
    float groove = smoothstep(1.5, 4.0, distIn) * (1.0 - smoothstep(4.0, 7.5, distIn));
    rgb *= (1.0 - 0.55 * groove * clamp(rimMult, 0.0, 1.3));
    float innerBevelBand = smoothstep(3.5, 8.5, distIn) * (1.0 - smoothstep(8.5, 20.0, distIn));

    // Directional light from top-left and secondary bottom reflection
    float topLeftLight = clamp(dot(-n, normalize(float2(0.35, 0.94))), 0.0, 1.0);
    float bottomLight = clamp(n.y, 0.0, 1.0);
    float dirLight = pow(topLeftLight, 1.3) * 0.95 + pow(bottomLight, 1.6) * 0.55;

    float fresnelGlow = pow(1.0 - edgeRatio, 2.8) * (0.32 + 0.30 * uExpand);
    float spec = (outerHairline * (0.45 + 0.35 * uExpand + 0.25 * dirLight)
               + innerBevelBand * dirLight * (0.28 + 0.62 * uExpand)
               + fresnelGlow * 0.22) * rimMult;

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
 * Top category filter tabs (Personal | Transactions | Promotions)
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
        Triple(InboxFilter.PROMOTIONS, "Promotions", unreadCounts[MessageCategory.PROMOTIONS] ?: 0)
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
    frost: Float = 0.35f,
    rimBrightness: Float = 0.45f,
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
                    shader.setFloatUniform("uFrost", frost.coerceIn(0f, 1f))
                    shader.setFloatUniform("uRim", rimBrightness.coerceIn(0f, 1f))
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

data class LiquidGlassTabItem(
    val label: String,
    val icon: ImageVector,
    val unreadCount: Int = 0
)

/**
 * Top primary destination tabs (Messages | Reminders | Finance)
 * used when Swap menu bar positions is enabled.
 */
@Composable
fun TopNavDestinationTabs(
    selectedTab: Int,
    unreadMessagesCount: Int,
    accentColor: Color,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple(0, "Messages", unreadMessagesCount),
        Triple(1, "Reminders", 0),
        Triple(2, "Finance", 0)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEach { (index, label, unread) ->
            val isSelected = selectedTab == index
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectTab(index) }
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
 * Liquid Glass Design Language lower capsule + separate circular New Message button:
 * - Real pixel-level Snell's Law edge bending via AGSL RuntimeShader (API 33+)
 *   over the live NavHost GraphicsLayer (with Haze fallback on older Android).
 * - Single smooth outward crystal droplet expansion while pressing, dragging, or switching tabs.
 */
@Composable
fun LiquidGlassNavBottomBar(
    selectedTab: Int,
    tabs: List<LiquidGlassTabItem>,
    accentColor: Color,
    hazeState: HazeState,
    backdropLayer: GraphicsLayer?,
    backdropOriginInRoot: Offset,
    onSelectTab: (Int) -> Unit,
    onComposeClick: () -> Unit,
    modifier: Modifier = Modifier,
    heightPercent: Int = 100,
    maxWidthPercent: Int = 100,
    buttonSpacingDp: Int = 4,
    frostPercent: Int = 35,
    rimBrightnessPercent: Int = 45,
    showComposeButton: Boolean = true
) {
    val heightScale = (heightPercent.coerceIn(80, 135) / 100f)
    val maxWidthFraction = (maxWidthPercent.coerceIn(65, 100) / 100f)
    val spacingDp = buttonSpacingDp.coerceIn(0, 24).dp
    val frost = (frostPercent.coerceIn(0, 100) / 100f)
    val rimBrightness = (rimBrightnessPercent.coerceIn(0, 100) / 100f)

    val capsuleHeight = (60f * heightScale).dp
    val pillHeight = (52f * heightScale).dp
    val capsuleCorner = (30f * heightScale).dp
    val pillCorner = (28f * heightScale).dp
    val capsuleShape = RoundedCornerShape(capsuleCorner)
    val pillShape = RoundedCornerShape(pillCorner)
    val density = LocalDensity.current

    val currentOnSelectTab by rememberUpdatedState(onSelectTab)
    val currentSelectedTab by rememberUpdatedState(selectedTab)

    var isDragging by remember { mutableStateOf(false) }
    var dragIndex by remember { mutableFloatStateOf(selectedTab.toFloat()) }
    val interactionSources = remember(tabs.size) { List(tabs.size) { MutableInteractionSource() } }
    val pressedStates = interactionSources.map { it.collectIsPressedAsState().value }
    val isPressed = pressedStates.any { it }

    // Latch press state briefly so there is zero 1-frame dip between finger-up and selectedTab update (fixes double bubble)
    var recentPressLatch by remember { mutableStateOf(false) }
    LaunchedEffect(isPressed) {
        if (isPressed) {
            recentPressLatch = true
        } else if (recentPressLatch) {
            kotlinx.coroutines.delay(95)
            recentPressLatch = false
        }
    }

    val animatedIndex by animateFloatAsState(
        targetValue = if (isDragging) dragIndex else selectedTab.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "liquidGlassSlide"
    )

    val isSwitching = isDragging || isPressed || recentPressLatch || abs(animatedIndex - selectedTab.toFloat()) > 0.03f

    val expandProgress by animateFloatAsState(
        targetValue = if (isSwitching) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "liquidGlassExpand"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (showComposeButton) Modifier.navigationBarsPadding() else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(maxWidthFraction)
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = if (showComposeButton) 14.dp else 4.dp,
                    bottom = if (showComposeButton) 12.dp else 4.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Crystal Clear Navigation Capsule
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(capsuleHeight)
            ) {
                // 1. Clipped capsule backdrop
                Box(
                    modifier = Modifier.matchParentSize()
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && backdropLayer != null) {
                        RealRefractionGlassLayer(
                            backdropLayer = backdropLayer,
                            backdropOriginInRoot = backdropOriginInRoot,
                            shape = capsuleShape,
                            cornerRadiusDp = capsuleCorner.value,
                            accentColor = accentColor,
                            expandProgress = 0f,
                            frost = frost,
                            rimBrightness = rimBrightness,
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
                                        backgroundColor = Color.White.copy(alpha = 0.03f + 0.11f * frost),
                                        blurRadius = (0.5f + 7.5f * frost).dp,
                                        tint = HazeTint(Color.White.copy(alpha = 0.05f + 0.12f * frost))
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = rimBrightness),
                                            Color.White.copy(alpha = rimBrightness * 0.28f),
                                            Color.White.copy(alpha = rimBrightness * 0.62f)
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
                    val totalSpacing = spacingDp * (tabs.size - 1).coerceAtLeast(0)
                    val slotWidth = ((maxWidth - totalSpacing) / tabs.size).coerceAtLeast(36.dp)
                    val slotStepPx = with(density) { (slotWidth + spacingDp).toPx() }
                    val extraWidth = 8.dp + 12.dp * expandProgress
                    val extraHeight = 14.dp + 20.dp * expandProgress
                    val dropletWidth = slotWidth + extraWidth
                    val dropletHeight = pillHeight + extraHeight
                    val dropletShiftPx = with(density) { (extraWidth / 2).toPx() }

                    // Soft outer halo behind the droplet (Zangetsu glowIntensity 0.12)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset {
                                IntOffset(
                                    x = (animatedIndex * slotStepPx - dropletShiftPx).roundToInt(),
                                    y = 0
                                )
                            }
                            .requiredSize(dropletWidth + 12.dp, dropletHeight + 12.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(
                                            alpha = (0.10f + 0.10f * expandProgress) *
                                                (0.35f + 0.65f * rimBrightness)
                                        ),
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(pillCorner + 6.dp)
                            )
                    )

                    // Persistent liquid-glass droplet: always visible, pops wider and taller on switch
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset {
                                IntOffset(
                                    x = (animatedIndex * slotStepPx - dropletShiftPx).roundToInt(),
                                    y = 0
                                )
                            }
                            .requiredSize(dropletWidth, dropletHeight)
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && backdropLayer != null) {
                            RealRefractionGlassLayer(
                                backdropLayer = backdropLayer,
                                backdropOriginInRoot = backdropOriginInRoot,
                                shape = pillShape,
                                cornerRadiusDp = pillCorner.value,
                                accentColor = accentColor,
                                expandProgress = expandProgress,
                                frost = frost,
                                rimBrightness = rimBrightness,
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
                                            backgroundColor = Color.White.copy(alpha = 0.03f + 0.11f * frost),
                                            blurRadius = (0.5f + 7.5f * frost).dp,
                                            tint = HazeTint(Color.White.copy(alpha = 0.05f + 0.12f * frost))
                                        )
                                    )
                                    .border(
                                        width = (1f + 0.4f * expandProgress).dp,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = (rimBrightness + 0.45f * expandProgress).coerceIn(0f, 1f)),
                                                Color.White.copy(alpha = rimBrightness * 0.32f),
                                                Color.White.copy(alpha = (rimBrightness * 0.65f + 0.40f * expandProgress).coerceIn(0f, 1f))
                                            )
                                        ),
                                        shape = pillShape
                                    )
                            )
                        }
                    }

                    // Tab items row + bidirectional continuous drag scrubber
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(tabs.size, slotStepPx) {
                                detectHorizontalDragGestures(
                                    onDragStart = { offset ->
                                        isDragging = true
                                        dragIndex = ((offset.x / slotStepPx) - 0.5f)
                                            .coerceIn(0f, (tabs.size - 1).toFloat())
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        dragIndex = (dragIndex + dragAmount / slotStepPx)
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
                        horizontalArrangement = Arrangement.spacedBy(spacingDp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    tabs.forEachIndexed { index, item ->
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
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (isActive) accentColor else Color.White.copy(alpha = 0.82f),
                                    modifier = Modifier.size(22.dp)
                                )
                                if (item.unreadCount > 0) {
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
                                text = item.label,
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

            if (showComposeButton) {
                CircularCrystalComposeButton(
                    accentColor = accentColor,
                    hazeState = hazeState,
                    backdropLayer = backdropLayer,
                    backdropOriginInRoot = backdropOriginInRoot,
                    sizeDp = (56f * heightScale.coerceIn(0.85f, 1.25f)).dp,
                    frost = frost,
                    rimBrightness = rimBrightness,
                    onClick = onComposeClick
                )
            }
        }
    }
}

@Composable
private fun CircularCrystalComposeButton(
    accentColor: Color,
    hazeState: HazeState,
    backdropLayer: GraphicsLayer?,
    backdropOriginInRoot: Offset,
    sizeDp: Dp = 56.dp,
    frost: Float = 0.35f,
    rimBrightness: Float = 0.45f,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(sizeDp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && backdropLayer != null) {
            RealRefractionGlassLayer(
                backdropLayer = backdropLayer,
                backdropOriginInRoot = backdropOriginInRoot,
                shape = CircleShape,
                cornerRadiusDp = sizeDp.value * 0.5f,
                accentColor = accentColor,
                expandProgress = 0f,
                frost = frost,
                rimBrightness = rimBrightness,
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
                            backgroundColor = Color.White.copy(alpha = 0.03f + 0.11f * frost),
                            blurRadius = (0.5f + 7.5f * frost).dp,
                            tint = HazeTint(Color.White.copy(alpha = 0.05f + 0.12f * frost))
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = rimBrightness),
                                Color.White.copy(alpha = rimBrightness * 0.28f),
                                Color.White.copy(alpha = rimBrightness * 0.62f)
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
