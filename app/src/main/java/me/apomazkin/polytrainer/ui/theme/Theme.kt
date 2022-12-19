package me.apomazkin.polytrainer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppLightColorScheme = lightColorScheme()
private val AppDarkColorScheme = darkColorScheme()

@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
//    val colors = if (darkTheme) {
//        AppDarkColorScheme
//    } else {
//        AppLightColorScheme
//    }
    val colors = AppLightColorScheme
//    val systemUiController = rememberSystemUiController()
//    systemUiController.setSystemBarsColor(
//        color = Color.White
//    )

    MaterialTheme(
        content = content,
        typography = Typography,
        colorScheme = colors,
        shapes = Shapes()
    )
}