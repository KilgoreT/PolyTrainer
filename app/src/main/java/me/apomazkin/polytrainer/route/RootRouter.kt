package me.apomazkin.polytrainer.route

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.runtime.remember
import me.apomazkin.dictionary.form.DictionaryFormScreen
import me.apomazkin.dictionary.list.DictionaryListScreen
import me.apomazkin.polytrainer.appComponent
import me.apomazkin.polytrainer.navigator.FormNavigatorImpl
import me.apomazkin.polytrainer.navigator.ListNavigatorImpl
import me.apomazkin.polytrainer.navigator.SplashNavigatorImpl
import me.apomazkin.splash.SplashScreen

enum class RootPoint(
    val route: String
) {
    SPLASH("SPLASH"),
    DICTIONARY_SETUP("DICTIONARY_SETUP"),
    DICTIONARY_CREATE("DICTIONARY_CREATE?editId={editId}"),
    DICTIONARY_LIST("DICTIONARY_LIST"),
    MAIN_ROUTER("MAIN_ROUTER")
}

class RootRouter {
    companion object {
        val START_DESTINATION = RootPoint.SPLASH
    }
}

@Composable
fun RootRouter(
    navController: NavHostController,
    onExitApp: () -> Unit = {},
) {
    val context = LocalContext.current
    var navigator: RootRouterNavigation? = null

    NavHost(
        navController = navController,
        startDestination = RootRouter.START_DESTINATION.route
    ) {
        composable(RootPoint.SPLASH.route) {
            val splashNavigator = remember {
                SplashNavigatorImpl(
                    onOpenDictionarySetup = { navigator?.openDictionarySetup() },
                    onOpenMainScreen = { navigator?.openMainScreen() },
                )
            }
            SplashScreen(
                factory = context.appComponent.getSplashViewModelFactory(),
                navigator = splashNavigator,
            )
        }
        composable(RootPoint.DICTIONARY_SETUP.route) {
            val formNavigator = remember { FormNavigatorImpl(onBack = { navigator?.openMainScreen() }) }
            DictionaryFormScreen(
                factory = context.appComponent.getDictionaryFormViewModelFactory(),
                navigator = formNavigator,
                showAppBar = false,
            )
        }
        composable(
            route = RootPoint.DICTIONARY_CREATE.route,
            arguments = listOf(
                navArgument("editId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            ),
        ) { backStackEntry ->
            val editId = backStackEntry.arguments?.getLong("editId", -1L) ?: -1L
            val formNavigator = remember(navController) {
                FormNavigatorImpl(onBack = { navController.popBackStack() })
            }
            DictionaryFormScreen(
                factory = context.appComponent.getDictionaryFormViewModelFactory(),
                navigator = formNavigator,
                editingDictionaryId = if (editId != -1L) editId else null,
            )
        }
        composable(RootPoint.DICTIONARY_LIST.route) {
            val listNavigator = remember(navController) {
                ListNavigatorImpl(
                    navController = navController,
                    onExit = onExitApp,
                )
            }
            DictionaryListScreen(
                factory = context.appComponent.getDictionaryListViewModelFactory(),
                navigator = listNavigator,
            )
        }
        mainRouter(
            route = RootPoint.MAIN_ROUTER.route,
            openDictionaryCreate = {
                navController.navigate("DICTIONARY_CREATE") {
                    launchSingleTop = true
                }
            },
            openDictionaryList = {
                navController.navigate(RootPoint.DICTIONARY_LIST.route) {
                    launchSingleTop = true
                }
            }
        )
    }

    navigator = object : RootRouterNavigation {
        override fun openSplashScreen() {
            navController.navigate(RootPoint.SPLASH.route) {
                launchSingleTop = true
            }
        }

        override fun openDictionarySetup() {
            navController.navigate(RootPoint.DICTIONARY_SETUP.route) {
                launchSingleTop = true
                popUpTo(RootPoint.SPLASH.route) { inclusive = true }
            }
        }

        @SuppressLint("RestrictedApi")
        override fun openMainScreen() {

            val isMainInBackStack = navController.currentBackStack.value
                    .any { it.destination.route == MainPoint.MAIN.route }
            if (isMainInBackStack) {
                navController.popBackStack(MainPoint.MAIN.route, false)
            } else {
                navController.navigate(RootPoint.MAIN_ROUTER.route) {
                    navController.currentDestination?.route?.let { currentRoute ->
                        launchSingleTop = true
                        popUpTo(currentRoute) { inclusive = true }
                    }
                }
            }
        }

    }
}

interface RootRouterNavigation {
    fun openSplashScreen()
    fun openDictionarySetup()
    fun openMainScreen()
}
