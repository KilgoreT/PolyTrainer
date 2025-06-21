package me.apomazkin.polytrainer.route

import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import me.apomazkin.main.MainScreen
import me.apomazkin.polytrainer.appComponent
import me.apomazkin.polytrainer.uiDeps.MainUiDepsProvider

enum class MainPoint(val route: String) {
    MAIN("MAIN"),
}

fun NavGraphBuilder.mainRouter(
    route: String,
    openAddDict: () -> Unit,
) {
    navigation(
        startDestination = MainPoint.MAIN.route,
        route = route
    ) {
        composable(MainPoint.MAIN.route) {
            val context = LocalContext.current
            MainScreen(
                mainUiDeps = MainUiDepsProvider(
                    dictionaryTabUseCase = context.appComponent.getVocabularyUseCase(),
                    wordCardUseCase = context.appComponent.getWordCardUseCase(),
                    quizTabUseCase = context.appComponent.getQuizTabUseCase(),
                    quizChatUseCase = context.appComponent.getQuizChatUseCase(),
                    settingsTabUseCase = context.appComponent.getSettingsTabUseCase(),
                    prefsProvider = context.appComponent.getPrefsProvider(),
                    resourceManager = context.appComponent.getResourceManager(),
                    envParams = context.appComponent.getEnvParams(),
                    logger = context.appComponent.getLogger(),
                ),
                openAddDict = openAddDict
            )
        }
    }
}