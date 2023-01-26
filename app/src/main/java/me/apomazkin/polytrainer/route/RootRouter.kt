package me.apomazkin.polytrainer.route

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.flow.collectLatest
import me.apomazkin.polytrainer.BuildConfig
import me.apomazkin.polytrainer.navigation.Constants
import me.apomazkin.polytrainer.navigation.processLog
import me.apomazkin.polytrainer.navigation.toLogEntityFlow
import me.apomazkin.polytrainer.ui.screen.langSelection.LanguageSelectionScreen
import me.apomazkin.polytrainer.ui.screen.splash.SplashScreen

enum class RootPoint(
    val route: String
) {
    ROOT("ROOT"),
    SPLASH("SPLASH"),
    INIT_LANG("INIT_LANG")
}

class RootRouter {
    companion object {
        val START_DESTINATION = RootPoint.ROOT
    }
}

@Composable
fun RootRouter(
    navController: NavHostController
) {
    LaunchedEffect(Unit) {
        if (BuildConfig.DEBUG) {
            navController
                .toLogEntityFlow()
                .collectLatest {
                    Log.d(Constants.LOG_TAG, it.processLog())
                }
        }
    }

    var navigator: RootRouterNavigation? = null

    NavHost(
        navController = navController,
        startDestination = RootRouter.START_DESTINATION.route
    ) {
        composable(RootPoint.ROOT.route) {
            navigator?.openSplashScreen()
        }

        composable(RootPoint.SPLASH.route) {
            SplashScreen { isInitLaunch ->
                if (isInitLaunch) {
                    navigator?.openInitLang()
                } else {

                }
            }
        }
        composable(RootPoint.INIT_LANG.route) {
            LanguageSelectionScreen()
        }
    }

    navigator = object : RootRouterNavigation {
        override fun openSplashScreen() {
            navController.navigate(RootPoint.SPLASH.route)
        }

        override fun openInitLang() {
            navController.navigate(RootPoint.INIT_LANG.route) {
                popUpTo(RootPoint.ROOT.route) { inclusive = false }
            }
        }

    }
}

interface RootRouterNavigation {
    fun openSplashScreen()
    fun openInitLang()
}