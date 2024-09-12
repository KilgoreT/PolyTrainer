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
    // displayLarge     : H1
    // displayMedium    : H2
    // displaySmall     : H3
    // headlineLarge    : H4

    // headlineMedium   : H5
    headlineMedium = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 24.sp
    ),

    // headlineSmall    : H6
    headlineSmall = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 20.sp
    ),
    // titleLarge       : B XL Bold
    titleLarge = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 24.sp,
    ),
    // titleMedium      : B XL
    titleMedium = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
    ),
    // titleSmall       : B L Bold
    titleSmall = TextStyle(
        fontFamily = PoppinsMedium,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
    ),
    // bodyLarge        : B L
    bodyLarge = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 17.sp,
    ),
    // bodyMedium       : B M Bold
    bodyMedium = TextStyle(
        fontFamily = PoppinsRegular,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
    ),
    // bodySmall        : B M
    bodySmall = TextStyle(
        fontFamily = PoppinsRegular,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
    ),
    // labelLarge       : B S Bold
    labelLarge = TextStyle(
        fontFamily = PoppinsMedium,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
    ),
    // labelMedium      : B S
    labelMedium = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.W400,
        fontSize = 13.sp,
    )
    // labelSmall      : B XS
)