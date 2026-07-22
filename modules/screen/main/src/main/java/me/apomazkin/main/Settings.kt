package me.apomazkin.main

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

private const val ABOUT_APP_ROUTE = "about_app"
private const val WEBVIEW_ROUTE = "webview/{pageKey}"

fun NavGraphBuilder.settings(
    navController: NavHostController,
    compositionRoot: CompositionRoot,
    openDictionaryList: () -> Unit,
) {

    composable(TabPoint.SETTINGS.route) {
        compositionRoot.SettingsTabScreenDep(
            onLangManagementClick = openDictionaryList,
            onAboutAppClick = { navController.goToAboutApp() },
            onPrivacyPolicyClick = { navController.goToWebView("privacy_policy") },
        )
    }

    composable(
        route = ABOUT_APP_ROUTE,
    ) {
        compositionRoot.AboutAppScreenDep(
            onBackPress = { navController.backPress() }
        )
    }

    composable(
        route = WEBVIEW_ROUTE,
        arguments = listOf(navArgument("pageKey") { type = NavType.StringType })
    ) { backStackEntry ->
        val pageKey = backStackEntry.arguments?.getString("pageKey") ?: run {
            return@composable
        }
        compositionRoot.WebViewScreenDep(
            pageKey = pageKey,
            onBackPress = { navController.backPress() }
        )
    }

    // IS486 фаза 4 (spec §20): роут components_manager выпилен — Manager недостижим
    // из навигации; модуль остаётся в коде как консервированный контракт.
}

private fun NavHostController.goToAboutApp() {
    navigate(route = ABOUT_APP_ROUTE) {
        launchSingleTop = true
    }
}

private fun NavHostController.goToWebView(pageKey: String) {
    navigate(route = "webview/$pageKey") {
        launchSingleTop = true
    }
}

private fun NavHostController.backPress() {
    popBackStack()
}
