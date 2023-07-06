package me.apomazkin.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = lightColorScheme(
    primary = LexemeColor.primary,
    onPrimary = LexemeColor.onPrimary,
    primaryContainer = LexemeColor.primaryContainer,
    onPrimaryContainer = LexemeColor.onPrimaryContainer,
    inversePrimary = LexemeColor.inversePrimary,

    secondary = LexemeColor.secondary,
    onSecondary = LexemeColor.onSecondary,
    secondaryContainer = LexemeColor.secondaryContainer,
    onSecondaryContainer = LexemeColor.onSecondaryContainer,

    tertiary = LexemeColor.tertiary,
    onTertiary = LexemeColor.onTertiary,
    tertiaryContainer = LexemeColor.tertiaryContainer,
    onTertiaryContainer = LexemeColor.onTertiaryContainer,

    background = LexemeColor.background,
    onBackground = LexemeColor.onBackground,

    surface = LexemeColor.surface,
    onSurface = LexemeColor.onSurface,
    surfaceVariant = LexemeColor.surfaceVariant,
    onSurfaceVariant = LexemeColor.onSurfaceVariant,
    surfaceTint = LexemeColor.surfaceTint,
    inverseSurface = LexemeColor.inverseSurface,
    inverseOnSurface = LexemeColor.inverseOnSurface,

    error = LexemeColor.error,
    onError = LexemeColor.onError,
    errorContainer = LexemeColor.errorContainer,
    onErrorContainer = LexemeColor.onErrorContainer,

    outline = LexemeColor.outline,
    outlineVariant = LexemeColor.outlineVariant,
)

private val AppDarkColorScheme = darkColorScheme()

@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = AppColorScheme
    MaterialTheme(
        content = content,
        typography = Typography,
        colorScheme = colors,
        shapes = Shapes()
    )
}