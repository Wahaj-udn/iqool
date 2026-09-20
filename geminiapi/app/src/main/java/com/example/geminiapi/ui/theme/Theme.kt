package com.example.geminiapi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Charcoal,
    onPrimary = Color.White,
    primaryContainer = Lime,
    onPrimaryContainer = Charcoal,
    secondary = Lime,
    onSecondary = Charcoal,
    secondaryContainer = Color(0xFFE9F9C7),
    onSecondaryContainer = Charcoal,
    tertiary = AccentPurple,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Charcoal,
    surface = Color.White,
    onSurface = Charcoal,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Grey600,
    outline = Grey400,
    inverseSurface = Charcoal,
    inverseOnSurface = LightBackground
)

private val DarkColorScheme = darkColorScheme(
    primary = Lime,
    onPrimary = Charcoal,
    primaryContainer = Lime,
    onPrimaryContainer = Charcoal,
    secondary = Lime,
    onSecondary = Charcoal,
    secondaryContainer = Grey800,
    onSecondaryContainer = Lime,
    tertiary = AccentPurple,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Grey400,
    outline = Grey600,
    inverseSurface = LightBackground,
    inverseOnSurface = Charcoal
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun GeminiapiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
