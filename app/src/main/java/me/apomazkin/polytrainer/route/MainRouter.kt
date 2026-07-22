package me.apomazkin.polytrainer.route

import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import me.apomazkin.main.MainScreen
import me.apomazkin.polytrainer.appComponent
import me.apomazkin.polytrainer.uiDeps.CompositionRootImpl

enum class MainPoint(val route: String) {
    MAIN("MAIN"),
}

fun NavGraphBuilder.mainRouter(
    route: String,
    openDictionaryCreate: () -> Unit,
    openDictionaryList: () -> Unit,
) {
    navigation(
        startDestination = MainPoint.MAIN.route,
        route = route
    ) {
        composable(MainPoint.MAIN.route) {
            val context = LocalContext.current
            MainScreen(
                compositionRoot = CompositionRootImpl(
                        wordCardViewModelFactory = context.appComponent.getWordCardViewModelFactory(),
                        chatViewModelFactory = context.appComponent.getChatViewModelFactory(),
                        appBarViewModelFactory = context.appComponent.getDictionaryAppBarViewModelFactory(),
                        dictionaryTabViewModelFactory = context.appComponent.getDictionaryTabViewModelFactory(),
                        quizTabViewModelFactory = context.appComponent.getQuizTabViewModelFactory(),
                        statisticViewModelFactory = context.appComponent.getStatisticViewModelFactory(),
                        settingsTabViewModelFactory = context.appComponent.getSettingsTabViewModelFactory(),
                        perDictionaryComponentsViewModelFactory = context.appComponent.getPerDictionaryComponentsViewModelFactory(),
                        envParams = context.appComponent.getEnvParams(),
                        logger = context.appComponent.getLogger(),
                ),
                openDictionaryCreate = openDictionaryCreate,
                openDictionaryList = openDictionaryList
            )
        }
    }
}