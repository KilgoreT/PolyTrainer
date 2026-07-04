package me.apomazkin.main

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

private const val WORD_CARD_ROUTE = "wordCard"
private const val WORD_ID_ARG = "wordId"
private const val PER_DICT_COMPONENTS_ROUTE = "per_dict_components"
private const val PER_DICT_COMPONENTS_DICT_ID_ARG = "dictionaryId"

fun NavGraphBuilder.vocabulary(
    navController: NavHostController,
    compositionRoot: CompositionRoot,
    openDictionaryCreate: () -> Unit,
) {
    composable(TabPoint.VOCABULARY.route) {
        compositionRoot.VocabularyTabDep(
            openDictionaryCreate = openDictionaryCreate,
            openWordCard = { navController.goToWordCard(it) },
            openPerDictionaryComponents = { dictId -> navController.goToPerDictionaryComponents(dictId) },
        )
    }

    composable(
        route = "$WORD_CARD_ROUTE/{$WORD_ID_ARG}",
        arguments = listOf(navArgument(WORD_ID_ARG) { type = NavType.LongType })
    ) { navBackStackEntry ->
        val wordId: Long = navBackStackEntry.arguments?.getLong(WORD_ID_ARG)
            ?: throw IllegalArgumentException("Unknown WordId")
        compositionRoot.WordCardScreenDep(
            wordId = wordId,
        ) {
            navController.backPress()
        }
    }

    composable(
        route = "$PER_DICT_COMPONENTS_ROUTE/{$PER_DICT_COMPONENTS_DICT_ID_ARG}",
        arguments = listOf(
            navArgument(PER_DICT_COMPONENTS_DICT_ID_ARG) { type = NavType.LongType }
        ),
    ) { navBackStackEntry ->
        val dictId: Long = navBackStackEntry.arguments?.getLong(PER_DICT_COMPONENTS_DICT_ID_ARG)
            ?: throw IllegalArgumentException("Unknown dictionaryId")
        compositionRoot.PerDictionaryComponentsScreenDep(
            dictionaryId = dictId,
            onBackPress = { navController.backPress() },
        )
    }

}

private fun NavHostController.goToWordCard(wordId: Long) {
    navigate(route = "$WORD_CARD_ROUTE/$wordId") {
        launchSingleTop = true
    }
}

internal fun NavHostController.goToPerDictionaryComponents(dictionaryId: Long) {
    navigate(route = "$PER_DICT_COMPONENTS_ROUTE/$dictionaryId") {
        launchSingleTop = true
    }
}

private fun NavHostController.backPress() {
    popBackStack()
}
