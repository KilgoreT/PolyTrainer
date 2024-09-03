package me.apomazkin.polytrainer.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import me.apomazkin.langpicker.LangPickerScreen
import me.apomazkin.polytrainer.appComponent
import me.apomazkin.splash.SplashScreen

enum class RootPoint(
    val route: String
) {
    SPLASH("SPLASH"),
    INIT_LANG("INIT_LANG"),
    MAIN_ROUTER("MAIN_ROUTER")
}

class RootRouter {
    companion object {
        val START_DESTINATION = RootPoint.SPLASH
    }
}

@Composable
fun RootRouter(
    navController: NavHostController
) {
//    LaunchedEffect(Unit) {
//        if (BuildConfig.DEBUG) {
//            navController
//                .toLogEntityFlow()
//                .collectLatest {
//                    Log.d(Constants.LOG_TAG, it.processLog())
//                }
//        }
//    }
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
                    navigator?.openInitLangScreen()
                } else {
                    navigator?.openMainScreen()
                }
            }
        }
        composable(RootPoint.INIT_LANG.route) {
            LangPickerScreen(
                langPickerUseCase = context.appComponent.getLangPickerUseCase()
            ) {
                navigator?.openMainScreen()
            }
        }
        mainRouter(
            route = RootPoint.MAIN_ROUTER.route,
            onAddLang = {
                navigator?.openInitLangScreen()
            }
        )
    }

    navigator = object : RootRouterNavigation {
        override fun openSplashScreen() {
            navController.navigate(RootPoint.SPLASH.route)
        }

        override fun openInitLangScreen() {
            navController.navigate(RootPoint.INIT_LANG.route) {
                popUpTo(RootPoint.SPLASH.route) { inclusive = true }
            }
        }

        override fun openMainScreen() {
            if (navController.backQueue.any { it.destination.route == MainPoint.MAIN.route }) {
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
    fun openInitLangScreen()
    fun openMainScreen()
}