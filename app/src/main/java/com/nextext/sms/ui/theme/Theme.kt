package com.nextext.sms.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.nextext.sms.data.preferences.PitchBlackAccent
import com.nextext.sms.data.preferences.ThemeMode

private val LightColors = lightColorScheme(
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

private val DarkColors = darkColorScheme(
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

private fun pitchBlackColors(accent: PitchBlackAccent) = darkColorScheme(
    primary = Color(accent.primaryHex),
    onPrimary = Color.White,
    primaryContainer = Color(accent.containerHex),
    onPrimaryContainer = Color(accent.onContainerHex),
    secondary = Color(0xFF25D366),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF132E20),
    onSecondaryContainer = Color(0xFF86EFAC),
    tertiary = Color(0xFF38BDF8),
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF121418),
    onSurface = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF9E9E9E),
    outline = Color(0xFF22242A),
    outlineVariant = Color(0xFF16181D)
)

@Composable
fun NexTextTheme(
    themeMode: ThemeMode = ThemeMode.PITCH_BLACK,
    pitchBlackAccent: PitchBlackAccent = PitchBlackAccent.CYAN,
    useMaterial3Design: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.PITCH_BLACK -> true
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        !useMaterial3Design -> {
            if (darkTheme) pitchBlackColors(pitchBlackAccent) else LightColors
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
