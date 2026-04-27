package me.apomazkin.polytrainer.route

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import me.apomazkin.dictionary.form.DictionaryFormScreen
import me.apomazkin.dictionary.list.DictionaryListScreen
import me.apomazkin.polytrainer.appComponent
import me.apomazkin.splash.SplashScreen

enum class RootPoint(
    val route: String
) {
    SPLASH("SPLASH"),
    DICTIONARY_SETUP("DICTIONARY_SETUP"),
    DICTIONARY_CREATE("DICTIONARY_CREATE"),
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
            SplashScreen(
                splashUseCase = context.appComponent.getSplashUseCase()
            ) { isInitLaunch ->
                if (isInitLaunch) {
                    navigator?.openDictionarySetup()
                } else {
                    navigator?.openMainScreen()
                }
            }
        }
        composable(RootPoint.DICTIONARY_SETUP.route) {
            DictionaryFormScreen(
                dictionaryUseCase = context.appComponent.getDictionaryUseCase(),
                onClose = { navigator?.openMainScreen() },
            )
        }
        composable(RootPoint.DICTIONARY_CREATE.route) {
            DictionaryFormScreen(
                dictionaryUseCase = context.appComponent.getDictionaryUseCase(),
                onClose = { navController.popBackStack() },
                onBackPress = { navController.popBackStack() },
            )
        }
        composable(RootPoint.DICTIONARY_LIST.route) {
            DictionaryListScreen(
                dictionaryUseCase = context.appComponent.getDictionaryUseCase(),
                onBackPress = { navController.popBackStack() },
                onExit = onExitApp,
                onOpenForm = { navController.navigate(RootPoint.DICTIONARY_CREATE.route) },
            )
        }
        mainRouter(
            route = RootPoint.MAIN_ROUTER.route,
            openDictionaryCreate = {
                navController.navigate(RootPoint.DICTIONARY_CREATE.route)
            },
            openDictionaryList = {
                navController.navigate(RootPoint.DICTIONARY_LIST.route)
            }
        )
    }

    navigator = object : RootRouterNavigation {
        override fun openSplashScreen() {
            navController.navigate(RootPoint.SPLASH.route)
        }

        override fun openDictionarySetup() {
            navController.navigate(RootPoint.DICTIONARY_SETUP.route) {
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
