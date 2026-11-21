package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val CanvaAoDarkColorScheme = darkColorScheme(
    primary = OrangeAccent,
    onPrimary = Color.White,
    primaryContainer = OrangeContainerDark,
    onPrimaryContainer = OrangeBright,
    secondary = ElectricIndigo,
    onSecondary = Color.White,
    secondaryContainer = NavySurfaceElevated,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = CyanGlow,
    onTertiary = NavyDeep,
    background = NavyDeep,
    onBackground = DarkTextPrimary,
    surface = NavyPrimary,
    onSurface = DarkTextPrimary,
    surfaceVariant = NavySurface,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainerLowest = NavyDeep,
    surfaceContainerLow = NavyPrimary,
    surfaceContainer = NavySurface,
    surfaceContainerHigh = NavySurfaceElevated,
    surfaceContainerHighest = Color(0xFF21375C),
    outline = NavyBorder,
    outlineVariant = Color(0xFF1D3152),
    error = ErrorRed,
    onError = Color.White
)

private val CanvaAoLightColorScheme = lightColorScheme(
    primary = OrangeAccent,
    onPrimary = Color.White,
    primaryContainer = OrangeContainerLight,
    onPrimaryContainer = OrangeDeep,
    secondary = ElectricIndigo,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightTextPrimary,
    tertiary = CyanGlow,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9FAFD),
    surfaceContainer = LightSurfaceVariant,
    surfaceContainerHigh = Color(0xFFE3E9F3),
    surfaceContainerHighest = Color(0xFFD8E0EE),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = ErrorRed,
    onError = Color.White
)

val CanvaAoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CanvaAoTheme(
    themeMode: String = "dark",
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDark) CanvaAoDarkColorScheme else CanvaAoLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !useDark
                    isAppearanceLightNavigationBars = !useDark
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CanvaAoTypography,
        shapes = CanvaAoShapes,
        content = content
    )
}
