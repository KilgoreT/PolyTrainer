package me.apomazkin.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.apomazkin.main.widget.MainBottomBarWidget
import me.apomazkin.main.widget.MainTopBarWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.M3Black
import me.apomazkin.theme.M3Neutral
import me.apomazkin.ui.StatusBarColorWidget

enum class TabPoint(val route: String) {
    VOCABULARY("vocabulary"),
    TRAINING("training"),
    DASHBOARD("dashboard"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(

) {
    val navController = rememberNavController()

    StatusBarColorWidget(
        statusBarColor = M3Black,
        navigationBarColor = M3Neutral,
    )
    Scaffold(
        modifier = Modifier,
        topBar = { MainTopBarWidget() },
        bottomBar = { MainBottomBarWidget(navController = navController) }

    ) { paddingValues ->
        NavHost(
            modifier = Modifier
                .padding(paddingValues),
            navController = navController,
            startDestination = TabPoint.VOCABULARY.route
        ) {
            composable(TabPoint.VOCABULARY.route) {

            }
            composable(TabPoint.TRAINING.route) {

            }
            composable(TabPoint.DASHBOARD.route) {

            }
        }
    }

}

@Composable
@Preview(
    showBackground = true,
    device = Devices.PIXEL_3
)
private fun Preview() {
    AppTheme {
        MainScreen()
    }
}