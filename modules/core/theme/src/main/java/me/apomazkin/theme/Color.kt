package me.apomazkin.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val White = Color(0xFFFFFFFF)
val Black = Color(0xFF10324C)
val BlackDisabled = Color(0x1F10324C)
val SecondarySelected = Color(0x1FFF7D05)

@Deprecated("Outdated")
val M3Black = Color(0xff32344F)

@Deprecated("Outdated")
val M3Neutral = Color(0xFFE9F3EE)

@Deprecated("Outdated")
val M3Primary95 = Color(0xFFB9FFE8)

@Deprecated("Outdated")
val FFD9E3 = Color(0x80FFD9E3)

@Deprecated("Outdated")
val clr1F001F24 = Color(0x1F001F24)

val clr242792 = Color(0xFF242792)
val clr3170D7 = Color(0xFF3170D7)
val gradientPrimary = Brush
    .horizontalGradient(listOf(clr3170D7, clr242792))

val clrFFB75E = Color(0xFFFFB75E)
val clrED8F03 = Color(0xFFED8F03)
val gradientSecondaryVertical = Brush
    .verticalGradient(listOf(clrED8F03, clrFFB75E))

val clr1F10324C = Color(0x1F10324C)

object LexemeColor {
    val primary = Color(0xFF2F51BE)
    val onPrimary = White
    val primaryContainer = primary
    val onPrimaryContainer = onPrimary

    val secondary = Color(0xFFED8F03)
    val onSecondary = White
    val secondaryContainer = secondary
    val onSecondaryContainer = onSecondary

    val tertiary = Color(0xFF53D4AF)
    val onTertiary = White
    val tertiaryContainer = tertiary
    val onTertiaryContainer = onTertiary

    val error = Color(0xFFFF5449)
    val errorContainer = error
    val onError = White
    val onErrorContainer = onError

    val background = Color(0xFFF7FBFE)
    val onBackground = Black

    val surface = White
    val onSurface = Black

    val surfaceVariant = background
    val onSurfaceVariant = Black

    val outline = background
    val outlineVariant = Black

    val inverseOnSurface = background
    val inverseSurface = Black

    val surfaceTint = Black
    val inversePrimary = White
}