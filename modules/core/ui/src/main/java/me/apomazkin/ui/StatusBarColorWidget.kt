package me.apomazkin.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun StatusBarColorWidget(
    color: Color = Color.Transparent,
    statusBarDarkIcon: Boolean = true,
    navigationBarDarkIcon: Boolean = true,
    navigationBarContrastEnforced: Boolean = false,
) {
    StatusBarColorWidget(
        statusBarColor = color,
        statusBarDarkIcon = statusBarDarkIcon,
        navigationBarColor = color,
        navigationBarDarkIcon = navigationBarDarkIcon,
        navigationBarContrastEnforced = navigationBarContrastEnforced
    )
}

@Composable
fun StatusBarColorWidget(
    statusBarColor: Color = Color.Transparent,
    navigationBarColor: Color = Color.Transparent,
    statusBarDarkIcon: Boolean = true,
    navigationBarDarkIcon: Boolean = true,
    navigationBarContrastEnforced: Boolean = false,
) {
    val systemUiController = rememberSystemUiController()
    DisposableEffect(Unit) {
        systemUiController.setStatusBarColor(statusBarColor, statusBarDarkIcon)
        systemUiController.setNavigationBarColor(
            color = navigationBarColor,
            darkIcons = navigationBarDarkIcon,
            navigationBarContrastEnforced = navigationBarContrastEnforced
        )
        onDispose {}
    }
}