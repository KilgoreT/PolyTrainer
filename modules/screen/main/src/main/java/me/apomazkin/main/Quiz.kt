package me.apomazkin.main

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

private const val QUIZ_ROUTE_ARG = "quizType"

fun NavGraphBuilder.quiz(compositionRoot: CompositionRoot) {
    composable(TabPoint.QUIZ.route) {
        compositionRoot.QuizTabScreenDep()
    }

    composable(
        route = MainRoutes.QUIZ_CHAT_PATTERN,
        arguments = listOf(
            navArgument(QUIZ_ROUTE_ARG) {
                type = NavType.StringType
            },
        ),
    ) { navBackStackEntry ->
        // Пока квиз один: quizType из route зарезервирован под выбор
        // экрана по типу.
        navBackStackEntry.arguments?.getString(QUIZ_ROUTE_ARG)
            ?: throw IllegalArgumentException("Unknown quizType")
        compositionRoot.ChatQuizScreenDep()
    }
}
