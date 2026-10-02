package me.apomazkin.main

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.statistic(compositionRoot: CompositionRoot) {
    tabComposable(TabPoint.STATS) {
        compositionRoot.StatisticTabScreenDep()
    }
}
