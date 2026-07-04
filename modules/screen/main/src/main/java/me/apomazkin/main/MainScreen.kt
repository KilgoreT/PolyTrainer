package me.apomazkin.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
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
    compositionRoot: CompositionRoot,
    openDictionaryCreate: () -> Unit,
    openDictionaryList: () -> Unit,
) {
    val navController = rememberNavController()

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
            vocabulary(
                navController = navController,
                compositionRoot = compositionRoot,
                openDictionaryCreate = openDictionaryCreate,
            )
            quiz(
                navController = navController,
                compositionRoot = compositionRoot,
                openDictionaryCreate = openDictionaryCreate,
            )
            statistic(
                navController = navController,
                compositionRoot = compositionRoot,
                openDictionaryCreate = openDictionaryCreate,
            )

            settings(
                navController = navController,
                compositionRoot = compositionRoot,
                openDictionaryList = openDictionaryList,
            )
        }
        BottomBarWidget(navController = navController)
    }
}
