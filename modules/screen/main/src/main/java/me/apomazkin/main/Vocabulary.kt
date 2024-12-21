package me.apomazkin.main

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

private const val WORD_CARD_ROUTE = "wordCard"
private const val WORD_ID_ARG = "wordId"

fun NavGraphBuilder.vocabulary(
    navController: NavHostController,
    mainUiDeps: MainUiDeps,
    openAddDict: () -> Unit,
) {
    composable(TabPoint.VOCABULARY.route) {
        mainUiDeps.VocabularyTabDep(
            openAddDict = openAddDict,
            openWordCard = { navController.goToWordCard(it) },
        )
    }

    composable(
        route = "$WORD_CARD_ROUTE/{$WORD_ID_ARG}",
        arguments = listOf(navArgument(WORD_ID_ARG) { type = NavType.LongType })
    ) { navBackStackEntry ->
        val wordId: Long = navBackStackEntry.arguments?.getLong(WORD_ID_ARG)
            ?: throw IllegalArgumentException("Unknown WordId")
        mainUiDeps.WordCardScreenDep(
            wordId = wordId,
        ) {
            navController.backPress()
        }
    }

}

private fun NavHostController.goToWordCard(wordId: Long) {
    navigate(route = "$WORD_CARD_ROUTE/$wordId")
}

private fun NavHostController.backPress() {
    popBackStack()
}