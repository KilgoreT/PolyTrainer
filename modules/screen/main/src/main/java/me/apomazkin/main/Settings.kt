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
    mainUiDeps: MainUiDeps,
    openDictionaryList: () -> Unit,
) {

    composable(TabPoint.SETTINGS.route) {
        mainUiDeps.SettingsTabScreenDep(
            onLangManagementClick = openDictionaryList,
            onAboutAppClick = { navController.goToAboutApp() },
            onPrivacyPolicyClick = { navController.goToWebView("privacy_policy") }
        )
    }

    composable(
        route = ABOUT_APP_ROUTE,
    ) {
        mainUiDeps.AboutAppScreenDep(
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
        mainUiDeps.WebViewScreenDep(
            pageKey = pageKey,
            onBackPress = { navController.backPress() }
        )
    }

}

private fun NavHostController.goToAboutApp() {
    navigate(route = ABOUT_APP_ROUTE)
}

private fun NavHostController.goToWebView(pageKey: String) {
    navigate(route = "webview/$pageKey")
}

private fun NavHostController.backPress() {
    popBackStack()
}
