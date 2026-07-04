package me.apomazkin.main

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

fun NavGraphBuilder.statistic(
    navController: NavHostController,
    compositionRoot: CompositionRoot,
    openDictionaryCreate: () -> Unit,
) {
    composable(TabPoint.STATS.route) {
        compositionRoot.StatisticTabScreenDep(
            openDictionaryCreate = openDictionaryCreate,
            openPerDictionaryComponents = { dictId -> navController.goToPerDictionaryComponents(dictId) },
        )
    }
}
