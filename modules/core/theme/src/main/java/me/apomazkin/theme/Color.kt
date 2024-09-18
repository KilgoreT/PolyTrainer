package me.apomazkin.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
val BlackDisabled = Color(0x1F10324C)
val Tonal = Color(0xFFE0EEF9)

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
val gradientSecondaryHorizontal = Brush
    .horizontalGradient(listOf(clrFFB75E, clrED8F03))

val clr1F10324C = Color(0x1F10324C)

val bgAlfa = Color(0x5210324C)

val unselectedGreyColor = Color(0xff95989D)
val dividerColor = Color(0xffE4E5E7)

object LexemeColor {
    // brand
    val primary = Color(0xFF4A49BC)
    val onPrimary = White
    val primaryContainer = primary
    val onPrimaryContainer = onPrimary

    // gray
    val secondary = Color(0xFFF2F2F3)
    val onSecondary = Color(0xFF19191B)
    val secondaryContainer = secondary
    val onSecondaryContainer = onSecondary

    // positive
    val tertiary = Color(0xFF3AA981)
    val onTertiary = Color(0xFF23785B)
    val tertiaryContainer = tertiary
    val onTertiaryContainer = onTertiary

    // danger and error
    val error = Color(0xFFF03D3D)
    val onError = Color(0xFFDE2424)
    val errorContainer = error
    val onErrorContainer = onError

    //
    val background = White
    val onBackground = onSecondary

    //
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