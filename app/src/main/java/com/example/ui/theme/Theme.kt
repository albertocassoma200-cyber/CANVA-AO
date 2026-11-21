package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.core.audio.LocalSoundEffects
import com.example.core.audio.SoundEffectsManager
import kotlinx.coroutines.launch

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
        LaunchedEffect(view, useDark) {
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
        shapes = CanvaAoShapes
    ) {
        val baseIndication = LocalIndication.current
        val sound = LocalSoundEffects.current
        if (baseIndication is IndicationNodeFactory && sound != null) {
            val soundIndication = remember(baseIndication, sound) {
                SoundTapIndicationFactory(baseIndication, sound)
            }
            CompositionLocalProvider(LocalIndication provides soundIndication) {
                content()
            }
        } else {
            content()
        }
    }
}

private class SoundTapIndicationFactory(
    private val delegate: IndicationNodeFactory,
    private val soundManager: SoundEffectsManager
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode {
        return SoundTapIndicationNode(
            delegateNode = delegate.create(interactionSource),
            interactionSource = interactionSource,
            soundManager = soundManager
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SoundTapIndicationFactory) return false
        return delegate == other.delegate && soundManager === other.soundManager
    }

    override fun hashCode(): Int {
        return 31 * delegate.hashCode() + System.identityHashCode(soundManager)
    }
}

private class SoundTapIndicationNode(
    delegateNode: DelegatableNode,
    private val interactionSource: InteractionSource,
    private val soundManager: SoundEffectsManager
) : DelegatingNode() {
    init {
        delegate(delegateNode)
    }

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Release) {
                    soundManager.playTap()
                }
            }
        }
    }
}
