package me.apomazkin.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color

import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun SystemBarsWidget(
    color: Color = Color.Transparent,
    statusBarDarkIcon: Boolean = true,
    navigationBarDarkIcon: Boolean = true,
    navigationBarContrastEnforced: Boolean = false,
) {
    val systemUiController = rememberSystemUiController()
    DisposableEffect(
        color,
        statusBarDarkIcon,
        navigationBarDarkIcon,
        navigationBarContrastEnforced
    ) {
        systemUiController.setStatusBarColor(color, statusBarDarkIcon)
        systemUiController.setNavigationBarColor(
            color = color,
            darkIcons = navigationBarDarkIcon,
            navigationBarContrastEnforced = navigationBarContrastEnforced
        )
        onDispose {}
    }
}

@Composable
fun SystemBarsWidget(
    statusBarColor: Color = Color.Transparent,
    navigationBarColor: Color = Color.Transparent,
    statusBarDarkIcon: Boolean = true,
    navigationBarDarkIcon: Boolean = true,
    navigationBarContrastEnforced: Boolean = false,
) {
    val systemUiController = rememberSystemUiController()
    DisposableEffect(
        statusBarColor,
        navigationBarColor,
        statusBarDarkIcon,
        navigationBarDarkIcon,
        navigationBarContrastEnforced
    ) {
        systemUiController.setStatusBarColor(statusBarColor, statusBarDarkIcon)
        systemUiController.setNavigationBarColor(
            color = navigationBarColor,
            darkIcons = navigationBarDarkIcon,
            navigationBarContrastEnforced = navigationBarContrastEnforced
        )
        onDispose {}
    }
}