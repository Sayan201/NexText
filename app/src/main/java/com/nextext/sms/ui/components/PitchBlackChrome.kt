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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
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
// 0 = crystal bead (compose button), 1 = flat pane of glass (nav droplet).
uniform float uFlat;
// Rim ink: white on AMOLED black, dark slate on a light page. A white
// specular over a white background is a rim nobody can see.
uniform float4 uInk;

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
    float flatness = clamp(uFlat, 0.0, 1.0);
    float thickness = mix(28.0 + 8.0 * expandP, 16.0 + 4.0 * expandP, flatness);
    float edgeRatio = clamp(distIn / thickness, 0.0, 1.0);

    // Snell's Law bending, concentrated in the curved rim. Flat glass bends
    // gently: the point is to see the capsule behind it, not to smear it.
    float bendFactor = pow(1.0 - edgeRatio, 2.2);
    float bendAmount = bendFactor * mix(26.0 + 20.0 * expandP, 9.0 + 6.0 * expandP, flatness);
    float2 refractCoord = clamp(fragCoord - n * bendAmount, float2(1.0), uSize - float2(1.0));
    float2 lo = float2(1.0);
    float2 hi = uSize - float2(1.0);

    // Frost body: 12-tap golden-angle disc, radius grows with uFrost (Zangetsu blur 3 at ~35%)
    half4 acc = contents.eval(refractCoord);
    float taps = 1.0;
    if (frost > 0.02) {
        float radius = 2.0 + 14.0 * frost;
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

    // Saturation: 1.15 gives the crystal bead its colour pop; flat glass is neutral.
    float luma = dot(rgb, float3(0.299, 0.587, 0.114));
    rgb = mix(float3(luma), rgb, mix(1.15, 1.0, flatness));

    // Milky frost film (Zangetsu glassColor white @ 0.12). Flat glass keeps
    // almost none of it — that film was what hid the capsule underneath.
    float film = 0.40 * frost * (1.0 - 0.85 * flatness);
    rgb = mix(rgb, mix(rgb, float3(1.0), 0.45) + float3(0.025), film);

    // Faint pane lift so flat glass still reads as a surface, not a hole.
    rgb = mix(rgb, float3(1.0), 0.05 * flatness);
    // The flat indicator carries the accent as a light tint, the same way the
    // resting chip does — a pane of tinted glass, not a clear hole in the slab.
    rgb = mix(rgb, uAccent.rgb, 0.14 * flatness);

    // bodyShade 0.12, bottom-weighted so labels keep contrast on bright backdrops.
    // Flat glass has no body shading at all.
    float bodyShade = smoothstep(-0.1, 1.0, p.y / max(halfSize.y, 1.0)) * 0.12 * (1.0 - flatness);
    rgb *= (1.0 - bodyShade);

    // The capsule is composited into `contents` before this shader runs, so the
    // droplet refracts the slab it sits on. No painted approximation of the rim.

    // Rim: one thin hairline for flat glass, the full double-band bevel for the bead.
    float rimMult = mix(uRim * 2.22, max(uRim * 2.22, 1.0), uExpand);
    float outerHairline = smoothstep(2.6, 0.0, distIn);
    float groove = smoothstep(1.5, 4.0, distIn) * (1.0 - smoothstep(4.0, 7.5, distIn));
    rgb *= (1.0 - 0.55 * groove * clamp(rimMult, 0.0, 1.3) * (1.0 - flatness));
    float innerBevelBand = smoothstep(3.5, 8.5, distIn) * (1.0 - smoothstep(8.5, 20.0, distIn));

    // Directional light from top-left and secondary bottom reflection
    float topLeftLight = clamp(dot(-n, normalize(float2(0.35, 0.94))), 0.0, 1.0);
    float bottomLight = clamp(n.y, 0.0, 1.0);
    float dirLight = pow(topLeftLight, 1.3) * 0.95 + pow(bottomLight, 1.6) * 0.55;

    float fresnelGlow = pow(1.0 - edgeRatio, 2.8) * (0.32 + 0.30 * uExpand);
    float hairline = outerHairline * (0.45 + 0.35 * uExpand + 0.25 * dirLight);
    float bead = innerBevelBand * dirLight * (0.28 + 0.62 * uExpand) + fresnelGlow * 0.22;
    float spec = (hairline + bead * (1.0 - 0.85 * flatness)) * rimMult;

    rgb += uInk.rgb * spec;

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
    // The glow is a lift on AMOLED black; on a light page it is a tint, not a wash.
    val onLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val topAlpha = if (onLight) 0.10f else 0.24f
    val midAlpha = if (onLight) 0.04f else 0.08f
    val coreAlpha = if (onLight) 0.14f else 0.34f
    val haloAlpha = if (onLight) 0.06f else 0.14f
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
                    accentColor.copy(alpha = topAlpha),
                    accentColor.copy(alpha = midAlpha),
                    Color.Transparent
                ),
                startY = 0f,
                endY = height
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = coreAlpha),
                    accentColor.copy(alpha = haloAlpha),
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
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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
    underlayLayer: GraphicsLayer? = null,
    underlayPosInRoot: Offset = Offset.Zero,
    flat: Float = 0f,
    modifier: Modifier = Modifier
) {
    // A bad AGSL build must degrade to plain glass, never take the app down.
    // `RuntimeShader` compiles in its constructor and throws on error.
    val shader = remember {
        runCatching { RuntimeShader(LIQUID_GLASS_REFRACTION_AGSL) }.getOrNull()
    }
    val density = LocalDensity.current
    val ink = MaterialTheme.colorScheme.onSurface
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
                val sh = shader
                if (sh != null && elementSize.width > 1f && elementSize.height > 1f) {
                    val radiusPx = with(density) { cornerRadiusDp.dp.toPx() }
                    sh.setFloatUniform("uSize", elementSize.width, elementSize.height)
                    sh.setFloatUniform("uRadius", radiusPx)
                    sh.setFloatUniform(
                        "uAccent",
                        accentColor.red,
                        accentColor.green,
                        accentColor.blue,
                        accentColor.alpha
                    )
                    sh.setFloatUniform("uExpand", expandProgress)
                    sh.setFloatUniform("uFrost", frost.coerceIn(0f, 1f))
                    sh.setFloatUniform("uRim", rimBrightness.coerceIn(0f, 1f))
                    sh.setFloatUniform("uFlat", flat.coerceIn(0f, 1f))
                    sh.setFloatUniform("uInk", ink.red, ink.green, ink.blue, ink.alpha)
                    renderEffect = RenderEffect
                        .createRuntimeShaderEffect(sh, "contents")
                        .asComposeRenderEffect()
                }
            }
    ) {
        val offsetX = elementPosInRoot.x - backdropOriginInRoot.x
        val offsetY = elementPosInRoot.y - backdropOriginInRoot.y
        translate(left = -offsetX, top = -offsetY) {
            drawLayer(backdropLayer)
        }
        // Composite the glass slab the droplet sits on top of the screen layer.
        // It becomes part of `contents`, so the shader refracts the capsule's
        // body and rim where they overlap — glass over glass, not an opaque
        // bubble masking a bite out of the slab.
        if (underlayLayer != null) {
            translate(
                left = underlayPosInRoot.x - elementPosInRoot.x,
                top = underlayPosInRoot.y - elementPosInRoot.y
            ) {
                drawLayer(underlayLayer)
            }
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
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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

    // The capsule's own glass, recorded into its own layer. The selection
    // droplet samples it as part of its shader input, so a popped-out bubble
    // refracts the slab underneath instead of hiding it.
    val capsuleGlassLayer = rememberGraphicsLayer()
    var capsuleOriginInRoot by remember { mutableStateOf(Offset.Zero) }

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
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "liquidGlassSlide"
    )

    // Frame-accurate velocity of the sliding pill. Drives the jelly deformation
    // (travel stretch + landing squish) exactly like Zangetsu's
    // IndicatorJellyPhysics: the leading edge races ahead and thins while the
    // indicator lands with a bounce.
    val currentIndex by rememberUpdatedState(animatedIndex)
    val pillVelocity = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var prev = currentIndex
        var prevNanos = withFrameNanos { it }
        while (true) {
            val nanos = withFrameNanos { it }
            val dt = (nanos - prevNanos) / 1_000_000_000f
            if (dt > 0.0001f) {
                val instantaneous = (currentIndex - prev) / dt
                val next = pillVelocity.floatValue * 0.55f + instantaneous * 0.45f
                // Snap to zero so the jelly settles and the bar stops recomposing.
                pillVelocity.floatValue = if (abs(next) < 0.06f) 0f else next
            }
            prev = currentIndex
            prevNanos = nanos
        }
    }

    val slideTarget = selectedTab.toFloat()
    val slideVelocity = pillVelocity.floatValue
    val slidingSpeed = abs(slideVelocity)
    val approaching = (slideTarget - animatedIndex) * slideVelocity > 0f
    val jelly = (slidingSpeed / 7.5f).coerceIn(0f, 1f)
    // Travel: stretch along X, thin along Y. Rebound past the target: squash and bulge.
    val lensWidthDelta = if (approaching) jelly * 0.19f else -jelly * 0.13f
    val lensHeightDelta = if (approaching) -jelly * 0.10f else jelly * 0.11f

    val isSwitching = isDragging || isPressed || recentPressLatch || abs(animatedIndex - selectedTab.toFloat()) > 0.03f

    val expandProgress by animateFloatAsState(
        targetValue = if (isSwitching) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "liquidGlassExpand"
    )

    // Droplet pop. Damping < 1 on purpose: the lens inflates past its target and
    // settles back, so the bubble visibly escapes the capsule instead of easing
    // flat inside it. This is the "pops out of the bar" read from the reference.
    val popProgress by animateFloatAsState(
        targetValue = if (isSwitching) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.42f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dropletPop"
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
                    // Room for the droplet to clear the capsule on both sides.
                    top = if (showComposeButton) 16.dp else 9.dp,
                    bottom = if (showComposeButton) 14.dp else 9.dp
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
                // 1. Clipped capsule backdrop, recorded into its own layer so the
                //    droplet can refract the glass it is sitting on.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .onGloballyPositioned { capsuleOriginInRoot = it.positionInRoot() }
                        .drawWithContent {
                            capsuleGlassLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(capsuleGlassLayer)
                        }
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
                                        backgroundColor = Color.White.copy(alpha = 0.04f + 0.16f * frost),
                                        blurRadius = (1f + 15f * frost).dp,
                                        tint = HazeTint(Color.White.copy(alpha = 0.06f + 0.18f * frost))
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
                    // Droplet geometry. Flat glass: a pill that sits INSIDE the
                    // capsule, inset top and bottom so the slab's rim stays visible
                    // above and below it, and swells to fill the capsule on press.
                    // No vertical bulge — the reference indicator is a pane, not a bead.
                    val pop = popProgress.coerceIn(0f, 1.5f)
                    val dropletWidth = slotWidth * (1.02f + lensWidthDelta.coerceAtMost(0.12f) + 0.03f * pop)
                    val dropletHeight = capsuleHeight * (0.86f + 0.10f * pop) *
                        (1f + lensHeightDelta * 0.30f).coerceAtLeast(0.95f)
                    val dropletCorner = RoundedCornerShape(dropletHeight / 2f)

                    // Resting indicator: the solid accent chip Zangetsu leaves
                    // under the selected tab (indicatorColor: accent @ 0.14).
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset {
                                IntOffset(
                                    x = (animatedIndex * slotStepPx).roundToInt(),
                                    y = 0
                                )
                            }
                            .width(slotWidth)
                            .height(pillHeight)
                            .clip(pillShape)
                            .background(accentColor.copy(alpha = 0.14f))
                    )

                    // Selection droplet. Absent at rest; on press / drag / travel it
                    // fades in as a flat pane of glass inside the capsule, stretches
                    // along the travel direction and settles on the rebound. The
                    // capsule is composited underneath, so the slab reads through it.
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset {
                                val wPx = dropletWidth.toPx()
                                IntOffset(
                                    x = (animatedIndex * slotStepPx - (wPx - slotWidth.toPx()) / 2f)
                                        .roundToInt(),
                                    // No vertical bias: the droplet is centred on the
                                    // capsule, so it breaks the top and bottom edges by
                                    // the same margin on every frame.
                                    y = 0
                                )
                            }
                            .requiredSize(dropletWidth, dropletHeight)
                            .graphicsLayer {
                                // Nothing at rest. The droplet is an interaction
                                // response, not a resident indicator — it dissolves
                                // in on press / drag / travel and leaves the accent
                                // chip as the resting state.
                                alpha = (pop * 1.15f).coerceIn(0f, 1f)
                            }
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && backdropLayer != null) {
                            RealRefractionGlassLayer(
                                backdropLayer = backdropLayer,
                                backdropOriginInRoot = backdropOriginInRoot,
                                shape = dropletCorner,
                                cornerRadiusDp = dropletHeight.value / 2f,
                                accentColor = accentColor,
                                expandProgress = (0.35f + 0.65f * pop).coerceIn(0f, 1f),
                                rimBrightness = rimBrightness,
                                underlayLayer = capsuleGlassLayer,
                                underlayPosInRoot = capsuleOriginInRoot,
                                // Flat pane: thin rim, gentle bend, and clear enough
                                // that the capsule composites through it.
                                flat = 1f,
                                frost = frost * 0.30f,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(dropletCorner)
                                    .hazeChild(
                                        state = hazeState,
                                        style = HazeDefaults.style(
                                            backgroundColor = Color.White.copy(alpha = 0.05f + 0.10f * frost),
                                            blurRadius = (1f + 5f * frost).dp,
                                            tint = HazeTint(Color.White.copy(alpha = 0.06f + 0.10f * frost))
                                        )
                                    )
                                    .border(
                                        width = (1f + 0.5f * pop).dp,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = (rimBrightness + 0.30f * pop).coerceIn(0f, 1f)),
                                                Color.White.copy(alpha = rimBrightness * 0.30f),
                                                Color.White.copy(alpha = (rimBrightness * 0.70f + 0.25f * pop).coerceIn(0f, 1f))
                                            )
                                        ),
                                        shape = dropletCorner
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
                        // The lens magnifies its content (Zangetsu: 1.16 natively).
                        val itemScale = if (isActive) 1f + 0.16f * expandProgress else 1f

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
                                    tint = if (isActive) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
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
                                color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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
                            backgroundColor = Color.White.copy(alpha = 0.04f + 0.16f * frost),
                            blurRadius = (1f + 15f * frost).dp,
                            tint = HazeTint(Color.White.copy(alpha = 0.06f + 0.18f * frost))
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
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
        )
    }
}
