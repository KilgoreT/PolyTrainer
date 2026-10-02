package me.apomazkin.polytrainer.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import me.apomazkin.dictionary.form.DictionaryFormScreen
import me.apomazkin.dictionary.list.DictionaryListScreen
import me.apomazkin.polytrainer.appComponent
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

/**
 * Root-граф приложения. Переходами управляет AppNavigationExecutor
 * (living-цели привязываются здесь bind/unbind) — сами composable
 * только собирают экраны, навигационных лямбд у них нет.
 */
@Composable
fun RootRouter(
    navController: NavHostController,
    onExitApp: () -> Unit = {},
) {
    val context = LocalContext.current

    // Навконтроллер табов создаётся ЗДЕСЬ (в живущем всё приложение RootRouter), а не
    // внутри MainScreen: заход в DICTIONARY_CREATE уничтожает композицию MAIN, и
    // rememberNavController внутри MainScreen давал бы новый инстанс при возврате —
    // тогда удержанный навигатор аппбара ходил бы на старый navController (баг:
    // компоненты не открываются после создания 2-го словаря).
    val tabsNavController = rememberNavController()

    val executor = context.appComponent.getNavigationExecutor()
    DisposableEffect(navController, tabsNavController, onExitApp) {
        executor.bind(
            rootNavController = navController,
            tabsNavController = tabsNavController,
            finishApp = onExitApp,
        )
        onDispose { executor.unbind() }
    }

    NavHost(
        navController = navController,
        startDestination = RootRouter.START_DESTINATION.route,
        enterTransition = { RootTransitions.enter(this) },
        exitTransition = { RootTransitions.exit(this) },
        popEnterTransition = { RootTransitions.enter(this) },
        popExitTransition = { RootTransitions.exit(this) },
    ) {
        composable(RootPoint.SPLASH.route) {
            SplashScreen(
                factory = context.appComponent.getSplashViewModelFactory(),
            )
        }
        composable(RootPoint.DICTIONARY_SETUP.route) {
            DictionaryFormScreen(
                factory = context.appComponent.getDictionaryFormViewModelFactory(),
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
            DictionaryFormScreen(
                factory = context.appComponent.getDictionaryFormViewModelFactory(),
                editingDictionaryId = if (editId != -1L) editId else null,
            )
        }
        composable(RootPoint.DICTIONARY_LIST.route) {
            DictionaryListScreen(
                factory = context.appComponent.getDictionaryListViewModelFactory(),
            )
        }
        mainRouter(
            route = RootPoint.MAIN_ROUTER.route,
            tabsNavController = tabsNavController,
        )
    }
}
