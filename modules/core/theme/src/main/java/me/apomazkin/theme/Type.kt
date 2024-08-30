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
    headlineSmall = TextStyle(
        fontFamily = PoppinsRegular,
        fontWeight = FontWeight.W400,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.W700,
        fontSize = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = PoppinsMedium,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = PoppinsRegular,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PoppinsRegular,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = PoppinsRegular,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = PoppinsMedium,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = PoppinsMedium,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
    )
)