package me.apomazkin.main

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

private const val ABOUT_APP_ROUTE = "about_app"

fun NavGraphBuilder.settings(
    navController: NavHostController,
    mainUiDeps: MainUiDeps,
    openAddDict: () -> Unit,
) {
    
    composable(TabPoint.SETTINGS.route) {
        mainUiDeps.SettingsTabScreenDep(
            onLangManagementClick = openAddDict,
            onAboutAppClick = { navController.goToAboutApp() }
        )
    }
    
    composable(
        route = ABOUT_APP_ROUTE,
    ) {
        mainUiDeps.AboutAppScreenDep(
            onBackPress = { navController.backPress() }
        )
    }
    
}

private fun NavHostController.goToAboutApp() {
    navigate(route = ABOUT_APP_ROUTE)
}

private fun NavHostController.backPress() {
    popBackStack()
}

