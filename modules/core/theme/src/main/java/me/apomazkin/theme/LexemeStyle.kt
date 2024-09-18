package me.apomazkin.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


val PoppinsRegular = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
)

val PoppinsMedium = FontFamily(
    Font(R.font.poppins_medium, FontWeight.Normal),
)

private val defaultFontFamily = FontFamily.Default

val Typography = Typography(
    //H1: displayLarge
    displayLarge = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 50.sp
    ),
    //H2: displayMedium
    displayMedium = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp
    ),
    //H3: displaySmall
    displaySmall = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp
    ),
    //H4: headlineLarge
    headlineLarge = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp
    ),

    //H5: headlineMedium
    headlineMedium = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp
    ),

    //H6: headlineSmall
    headlineSmall = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp
    ),
    //Body XL Bold: titleLarge
    titleLarge = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
    ),
    //Body XL: titleMedium
    titleMedium = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 19.sp,
    ),
    //Body L Bold: titleSmall
    titleSmall = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
    ),
    //Body L: bodyLarge
    bodyLarge = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
    ),
    //Body M Bold: bodyMedium
    bodyMedium = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
    ),
    //Body M: bodySmall
    bodySmall = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
    ),
    //Body S Bold: labelLarge
    labelLarge = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
    ),
    //Body S: labelMedium
    labelMedium = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
    ),
    //Body XS: labelSmall
    labelSmall = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
    ),
)

object LexemeStyle {
    val H1 = Typography.displayLarge
    val H2 = Typography.displayMedium
    val H3 = Typography.displaySmall
    val H4 = Typography.headlineLarge
    val H5 = Typography.headlineMedium
    val H6 = Typography.headlineSmall

    val BodyXLBold = Typography.titleLarge
    val BodyXL = Typography.titleMedium
    val BodyLBold = Typography.titleSmall
    val BodyL = Typography.bodyLarge
    val BodyMBold = Typography.bodyMedium
    val BodyM = Typography.bodySmall
    val BodySBold = Typography.labelLarge
    val BodyS = Typography.labelMedium
}