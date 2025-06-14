package me.apomazkin.polytrainer.route

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import me.apomazkin.createdictionary.CreateDictionaryScreen
import me.apomazkin.polytrainer.appComponent
import me.apomazkin.splash.SplashScreen

enum class RootPoint(
    val route: String
) {
    SPLASH("SPLASH"),
    CREATE_DICTIONARY("CREATE_DICTIONARY"),
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
                    navigator?.openCreateDictionaryScreen()
                } else {
                    navigator?.openMainScreen()
                }
            }
        }
        composable(RootPoint.CREATE_DICTIONARY.route) {
            CreateDictionaryScreen(
                createDictionaryUseCase = context.appComponent.getCreateDictionaryUseCase()
            ) {
                navigator?.openMainScreen()
            }
        }
        mainRouter(
            route = RootPoint.MAIN_ROUTER.route,
            openAddDict = {
                navigator?.openCreateDictionaryScreen()
            }
        )
    }

    navigator = object : RootRouterNavigation {
        override fun openSplashScreen() {
            navController.navigate(RootPoint.SPLASH.route)
        }

        override fun openCreateDictionaryScreen() {
            navController.navigate(RootPoint.CREATE_DICTIONARY.route) {
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
    fun openCreateDictionaryScreen()
    fun openMainScreen()
}