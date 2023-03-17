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
    onAddLang: () -> Unit,
) {
    navigation(
        startDestination = MainPoint.MAIN.route,
        route = route
    ) {
        composable(MainPoint.MAIN.route) {
            val context = LocalContext.current
            MainScreen(
                mainUiDeps = MainUiDepsProvider(
                    vocabularyUseCase = context.appComponent.getVocabularyUseCase(),
                    wordCardUseCase = context.appComponent.getWordCardUseCase(),
                ),
                onAddLang = onAddLang
            )
        }
    }
}