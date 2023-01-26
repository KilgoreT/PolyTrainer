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

val Typography = Typography(
    headlineSmall = TextStyle(
        fontFamily = PoppinsRegular,
        fontWeight = FontWeight.W400,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PoppinsRegular,
        fontWeight = FontWeight.W400,
        fontSize = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = PoppinsRegular,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = PoppinsMedium,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
    )
)