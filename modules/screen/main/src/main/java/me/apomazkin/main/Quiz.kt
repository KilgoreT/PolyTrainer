package me.apomazkin.main

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

private const val QUIZ_ROUTE = "quiz"
private const val QUIZ_ROUTE_ARG = "quizType"

fun NavGraphBuilder.quiz(
    navController: NavHostController,
    mainUiDeps: MainUiDeps,
    openAddDict: () -> Unit,
) {
    composable(TabPoint.QUIZ.route) {
        mainUiDeps.QuizTabScreenDep(
            openAddDict = openAddDict,
            openChatQuiz = { navController.goToQuiz(it) },
        )
    }
    
    composable(
        route = "$QUIZ_ROUTE/{$QUIZ_ROUTE_ARG}",
        arguments = listOf(navArgument(QUIZ_ROUTE_ARG) {
            type = NavType.StringType
        })
    ) { navBackStackEntry ->
        //TODO kilg 23.01.2025 05:59 - в зависимости от quizType создавать экран, пока только один
        val quizType: String =
            navBackStackEntry.arguments?.getString(QUIZ_ROUTE_ARG)
                ?: throw IllegalArgumentException("Unknown quizType")
        mainUiDeps.ChatQuizScreenDep(
            onBackPress = { navController.backPress() }
        )
    }
    
}

private fun NavHostController.goToQuiz(quizType: String) {
    navigate(route = "$QUIZ_ROUTE/$quizType")
}

private fun NavHostController.backPress() {
    popBackStack()
}