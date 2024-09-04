package me.apomazkin.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val appColorScheme = lightColorScheme(

    //brand
    primary = LexemeColor.primary,
    onPrimary = LexemeColor.onPrimary,
    primaryContainer = LexemeColor.primaryContainer,
    onPrimaryContainer = LexemeColor.onPrimaryContainer,
    inversePrimary = LexemeColor.inversePrimary,

    // gray
    secondary = LexemeColor.secondary,
    onSecondary = LexemeColor.onSecondary,
    secondaryContainer = LexemeColor.secondaryContainer,
    onSecondaryContainer = LexemeColor.onSecondaryContainer,

    // positive
    tertiary = LexemeColor.tertiary,
    onTertiary = LexemeColor.onTertiary,
    tertiaryContainer = LexemeColor.tertiaryContainer,
    onTertiaryContainer = LexemeColor.onTertiaryContainer,

    // ????
    background = LexemeColor.background,
    onBackground = LexemeColor.onBackground,

    // info
    surface = LexemeColor.surface,
    onSurface = LexemeColor.onSurface,
    surfaceVariant = LexemeColor.surfaceVariant,
    onSurfaceVariant = LexemeColor.onSurfaceVariant,
    surfaceTint = LexemeColor.surfaceTint,
    inverseSurface = LexemeColor.inverseSurface,
    inverseOnSurface = LexemeColor.inverseOnSurface,

    // error
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
    val colors = appColorScheme
    MaterialTheme(
        content = content,
        typography = Typography,
        colorScheme = colors,
        shapes = Shapes()
    )
}