package me.apomazkin.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Deprecated("Outdated")
val FFD9E3 = Color(0x80FFD9E3)

@Deprecated("Outdated")
val clr1F001F24 = Color(0x1F001F24)

val blackDisabledColor = Color(0x1F10324C)
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


val bgAlfa = Color(0xA3333333)
val whiteColor = Color(0xFFFFFFFF)
val blackColor = Color(0xFF000000)
val unselectedGreyColor = Color(0xff95989D)
val dividerColor = Color(0xffE4E5E7)
val enableIconColor = Color(0xff252628)
val disableButtonTitleColor = Color(0xffB5AEE1)
val grayTextColor = Color(0xFF7B7E85)
val translationBgColor = Color(0xFF938CD5)

object LexemeColor {
    // brand
    val primary = Color(0xFF4A49BC)
    val onPrimary = whiteColor
    val primaryContainer = primary
    val onPrimaryContainer = onPrimary

    // gray
    val secondary = Color(0xFF19191B)
    val onSecondary = Color(0xFFF2F2F3)
    val secondaryContainer = secondary
    val onSecondaryContainer = onSecondary

    // positive
    val tertiary = Color(0xFFF1E9FA)
    val onTertiary = blackColor
    val tertiaryContainer = tertiary
    val onTertiaryContainer = onTertiary

    // danger and error
    val error = Color(0xFFFEE2E2)
    val onError = Color(0xFFDE2424)
    val errorContainer = error
    val onErrorContainer = onError

    //
    val background = whiteColor
    val onBackground = onSecondary

    //
    val surface = whiteColor
    val onSurface = blackColor

    val surfaceVariant = background
    val onSurfaceVariant = blackColor

    val outline = background
    val outlineVariant = blackColor

    val inverseOnSurface = background
    val inverseSurface = blackColor

    val surfaceTint = blackColor
    val inversePrimary = whiteColor
}