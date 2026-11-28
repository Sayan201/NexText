package com.nextext.sms.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.nextext.sms.data.preferences.PitchBlackAccent
import com.nextext.sms.data.preferences.ThemeMode

/**
 * Neutral Material 3 palette, used only when the operator turns the
 * "Material 3 Design" switch on. The default NexText look below is
 * accent-driven in both Dark and Light.
 */
private val Material3LightColors = lightColorScheme(
    primary = Color(0xFF0F4C81),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001C3B),
    secondary = Color(0xFF0E7490),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF083344),
    tertiary = Color(0xFF15803D),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEEF2F6),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569)
)

private val Material3DarkColors = darkColorScheme(
    primary = Color(0xFF8BB8FF),
    onPrimary = Color(0xFF002E5C),
    primaryContainer = Color(0xFF0B3B66),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFF67E8F9),
    onSecondary = Color(0xFF003640),
    secondaryContainer = Color(0xFF0E4D5C),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = Color(0xFF4ADE80),
    background = Color(0xFF0B111A),
    surface = Color(0xFF111827),
    surfaceVariant = Color(0xFF1E293B),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8)
)

/** Readable foreground for a filled accent surface (button, chip, badge). */
private fun onAccent(primary: Color): Color =
    if (primary.luminance() > 0.50f) Color(0xFF0B0F14) else Color.White

/**
 * The accent swatch the operator picked, nudged darker when it is too bright
 * to read as text on a white page. Same hue, usable contrast.
 */
private fun lightPrimary(accent: PitchBlackAccent): Color {
    val raw = Color(accent.primaryHex)
    val l = raw.luminance()
    return when {
        l > 0.50f -> lerp(raw, Color(0xFF0B0F14), 0.32f)
        l > 0.28f -> lerp(raw, Color(0xFF0B0F14), 0.20f)
        else -> raw
    }
}

/**
 * Dark (AMOLED) scheme. Black background is the point — Pitch Black was never
 * a separate theme, it was this one.
 */
private fun pitchBlackColors(accent: PitchBlackAccent): ColorScheme {
    val primary = Color(accent.primaryHex)
    return darkColorScheme(
        primary = primary,
        onPrimary = onAccent(primary),
        primaryContainer = Color(accent.containerHex),
        onPrimaryContainer = Color(accent.onContainerHex),
        inversePrimary = lerp(primary, Color.White, 0.35f),
        secondary = Color(0xFF25D366),
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFF132E20),
        onSecondaryContainer = Color(0xFF86EFAC),
        tertiary = Color(0xFF38BDF8),
        onTertiary = Color(0xFF04222E),
        background = Color(0xFF000000),
        onBackground = Color(0xFFF3F4F6),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFF3F4F6),
        surfaceVariant = Color(0xFF121418),
        onSurfaceVariant = Color(0xFF9E9E9E),
        outline = Color(0xFF3A3E46),
        outlineVariant = Color(0xFF22242A),
        scrim = Color(0xFF000000),
        surfaceTint = primary
    )
}

/**
 * Light scheme built from the same accent. Backgrounds are near-white so the
 * AMOLED scheme's literal black never leaks into light mode.
 */
private fun lightColors(accent: PitchBlackAccent): ColorScheme {
    val primary = lightPrimary(accent)
    val container = lerp(Color.White, primary, 0.20f)
    val onContainer = lerp(Color(0xFF05080C), primary, 0.45f)
    return lightColorScheme(
        primary = primary,
        onPrimary = onAccent(primary),
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        inversePrimary = lerp(primary, Color.White, 0.35f),
        secondary = lerp(primary, Color(0xFF0F766E), 0.40f),
        onSecondary = Color.White,
        secondaryContainer = lerp(Color.White, primary, 0.14f),
        onSecondaryContainer = onContainer,
        tertiary = lerp(primary, Color(0xFF15803D), 0.35f),
        onTertiary = Color.White,
        tertiaryContainer = lerp(Color.White, primary, 0.12f),
        onTertiaryContainer = onContainer,
        background = lerp(Color(0xFFF7F8FA), primary, 0.04f),
        onBackground = Color(0xFF11151C),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF11151C),
        surfaceVariant = lerp(Color(0xFFEFF2F6), primary, 0.06f),
        onSurfaceVariant = Color(0xFF59616E),
        outline = Color(0xFFB7BFCB),
        outlineVariant = Color(0xFFDFE4EC),
        scrim = Color(0xFF000000),
        surfaceTint = primary
    )
}

@Composable
fun NexTextTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    pitchBlackAccent: PitchBlackAccent = PitchBlackAccent.CYAN,
    useMaterial3Design: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        !useMaterial3Design -> {
            // The NexText scheme: accent-driven, AMOLED black in dark mode,
            // near-white in light mode. The accent applies to both.
            if (darkTheme) pitchBlackColors(pitchBlackAccent) else lightColors(pitchBlackAccent)
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> Material3DarkColors
        else -> Material3LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
