package me.apomazkin.main

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

fun NavGraphBuilder.settings(
    navController: NavHostController,
    compositionRoot: CompositionRoot,
) {
    tabComposable(TabPoint.SETTINGS) {
        compositionRoot.SettingsTabScreenDep()
    }

    composable(
        route = MainRoutes.ABOUT_APP,
    ) {
        compositionRoot.AboutAppScreenDep(
            onBackPress = { navController.popBackStack() },
        )
    }

    composable(
        route = MainRoutes.WEBVIEW_PATTERN,
        arguments = listOf(navArgument("pageKey") { type = NavType.StringType }),
    ) { backStackEntry ->
        val pageKey = backStackEntry.arguments?.getString("pageKey") ?: run {
            return@composable
        }
        compositionRoot.WebViewScreenDep(
            pageKey = pageKey,
            onBackPress = { navController.popBackStack() },
        )
    }

    // IS486 фаза 4 (spec §20): роут components_manager выпилен — Manager недостижим
    // из навигации; модуль остаётся в коде как консервированный контракт.
}
