package me.apomazkin.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.apomazkin.main.widget.BottomBarWidget
import me.apomazkin.ui.StatusBarColorWidget

enum class TabPoint(val route: String) {
    VOCABULARY("vocabulary"),
    TRAINING("training"),
    STATS("statistic"),
    SETTINGS("settings"),
}

@Composable
fun MainScreen(
    mainUiDeps: MainUiDeps,
    onAddDictionary: () -> Unit,
) {
    val navController = rememberNavController()

    StatusBarColorWidget(
        color = Color.White,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        NavHost(
            modifier = Modifier
                .weight(1F),
            navController = navController,
            startDestination = TabPoint.VOCABULARY.route
        ) {
            vocabulary(
                navController = navController,
                mainUiDeps = mainUiDeps,
                onAddDictionary = onAddDictionary,
            )
            composable(TabPoint.TRAINING.route) {}
            composable(TabPoint.STATS.route) {}
            composable(TabPoint.SETTINGS.route) {}
        }
        BottomBarWidget(navController = navController)
    }
}