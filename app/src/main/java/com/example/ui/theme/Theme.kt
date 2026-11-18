package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.model.ThemePreference

private val RahnamaLightColorScheme = lightColorScheme(
    primary = RahnamaCobalt,
    onPrimary = Color.White,
    primaryContainer = RahnamaCobaltContainer,
    onPrimaryContainer = Color(0xFF001C3B),
    secondary = RahnamaEmerald,
    onSecondary = Color.White,
    secondaryContainer = RahnamaEmeraldContainer,
    onSecondaryContainer = Color(0xFF002114),
    tertiary = RahnamaCrimson,
    onTertiary = Color.White,
    tertiaryContainer = RahnamaCrimsonContainer,
    onTertiaryContainer = Color(0xFF410002),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE8EEF6),
    surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = LightOutline,
    outlineVariant = Color(0xFFE2E8F0),
    error = RahnamaCrimson,
    onError = Color.White
)

private val RahnamaDarkColorScheme = darkColorScheme(
    primary = RahnamaCobaltDark,
    onPrimary = Color(0xFF002E69),
    primaryContainer = RahnamaCobaltContainerDark,
    onPrimaryContainer = Color(0xFFD8E8FF),
    secondary = RahnamaEmeraldDark,
    onSecondary = Color(0xFF003825),
    secondaryContainer = RahnamaEmeraldContainerDark,
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = RahnamaCrimsonDark,
    onTertiary = Color(0xFF5C0A0A),
    tertiaryContainer = RahnamaCrimsonContainerDark,
    onTertiaryContainer = Color(0xFFFEE2E2),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFF090E1A),
    surfaceContainerLow = Color(0xFF111827),
    surfaceContainer = Color(0xFF172033),
    surfaceContainerHigh = Color(0xFF1E293B),
    surfaceContainerHighest = Color(0xFF283548),
    outline = DarkOutline,
    outlineVariant = Color(0xFF1E293B),
    error = RahnamaCrimsonDark,
    onError = Color(0xFF5C0A0A)
)

val RahnamaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RahnamaTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDarkTheme) RahnamaDarkColorScheme else RahnamaLightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RahnamaTypography,
            shapes = RahnamaShapes,
            content = content
        )
    }
}
