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
    DisposableEffect(Unit) {
        systemUiController.setStatusBarColor(color, statusBarDarkIcon)
        systemUiController.setNavigationBarColor(
            color = color,
            darkIcons = navigationBarDarkIcon,
            navigationBarContrastEnforced = navigationBarContrastEnforced
        )
        onDispose {}
    }
}