package me.apomazkin.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import me.apomazkin.main.widget.BottomBarWidget
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.SystemBarsWidget

enum class TabPoint(val route: String) {
    VOCABULARY("vocabulary"),
    QUIZ("quiz"),
    STATS("statistic"),
    SETTINGS("settings"),
}

@Composable
fun MainScreen(
    navController: NavHostController,
    compositionRoot: CompositionRoot,
) {
    SystemBarsWidget(
        color = whiteColor,
    )
    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        NavHost(
            modifier = Modifier
                .weight(1F),
            navController = navController,
            startDestination = TabPoint.VOCABULARY.route
        ) {
            vocabulary(compositionRoot = compositionRoot)
            quiz(compositionRoot = compositionRoot)
            statistic(compositionRoot = compositionRoot)
            settings(
                navController = navController,
                compositionRoot = compositionRoot,
            )
        }
        BottomBarWidget(navController = navController)
    }
}
