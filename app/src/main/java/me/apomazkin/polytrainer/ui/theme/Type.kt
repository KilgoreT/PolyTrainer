package me.apomazkin.polytrainer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import me.apomazkin.core_resources.R


val PoppinsRegularW400 = FontFamily(
    Font(R.font.poppins_regular, FontWeight.W400),
)

val Typography = Typography(
    headlineSmall = TextStyle(
        fontFamily = PoppinsRegularW400,
        fontWeight = FontWeight.W400,
        fontSize = 24.sp
    )
)