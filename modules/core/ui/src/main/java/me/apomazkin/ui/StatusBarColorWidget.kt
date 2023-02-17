package me.apomazkin.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun StatusBarColorWidget(
    color: Color = Color.Transparent
) {
    val systemUiController = rememberSystemUiController()
    DisposableEffect(Unit) {
        systemUiController.setSystemBarsColor(
            color = color
        )
        onDispose {}
    }
}

@Composable
fun StatusBarColorWidget(
    statusBarColor: Color = Color.Transparent,
    navigationBarColor: Color = Color.Transparent,
) {
    val systemUiController = rememberSystemUiController()
    DisposableEffect(Unit) {
        systemUiController.setStatusBarColor(statusBarColor)
        systemUiController.setNavigationBarColor(navigationBarColor)
        onDispose {}
    }
}