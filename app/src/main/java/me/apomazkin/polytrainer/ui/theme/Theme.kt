package me.apomazkin.polytrainer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppLightColorScheme = lightColorScheme(
    primary = ColorLight.Primary,
    onPrimary = ColorLight.OnPrimary,
    primaryContainer = ColorLight.PrimaryContainer,
    onPrimaryContainer = ColorLight.OnPrimaryContainer,
    inversePrimary = ColorLight.inversePrimary,

    secondary = ColorLight.secondary,
    onSecondary = ColorLight.onSecondary,
    secondaryContainer = ColorLight.secondaryContainer,
    onSecondaryContainer = ColorLight.onSecondaryContainer,

    tertiary = ColorLight.tertiary,
    onTertiary = ColorLight.onTertiary,
    tertiaryContainer = ColorLight.tertiaryContainer,
    onTertiaryContainer = ColorLight.onTertiaryContainer,

    background = ColorLight.background,
    onBackground = ColorLight.onBackground,

    surface = ColorLight.surface,
    onSurface = ColorLight.onSurface,
    surfaceVariant = ColorLight.surfaceVariant,
    onSurfaceVariant = ColorLight.onSurfaceVariant,
    surfaceTint = ColorLight.surfaceTint,
    inverseSurface = ColorLight.inverseSurface,
    inverseOnSurface = ColorLight.inverseOnSurface,

    error = ColorLight.error,
    onError = ColorLight.onError,
    errorContainer = ColorLight.errorContainer,
    onErrorContainer = ColorLight.onErrorContainer,

    outline = ColorLight.outline,
    outlineVariant = ColorLight.outlineVariant,
)

private val AppDarkColorScheme = darkColorScheme()

@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
//        AppDarkColorScheme
        AppLightColorScheme
    } else {
        AppLightColorScheme
    }
    MaterialTheme(
        content = content,
        typography = Typography,
        colorScheme = colors,
        shapes = Shapes()
    )
}