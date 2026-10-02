package me.apomazkin.main

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

private const val WORD_ID_ARG = "wordId"
private const val PER_DICT_COMPONENTS_DICT_ID_ARG = "dictionaryId"

fun NavGraphBuilder.vocabulary(compositionRoot: CompositionRoot) {
    tabComposable(TabPoint.VOCABULARY) {
        compositionRoot.VocabularyHostDep()
    }

    composable(
        route = MainRoutes.WORD_CARD_PATTERN,
        arguments = listOf(navArgument(WORD_ID_ARG) { type = NavType.LongType }),
    ) { navBackStackEntry ->
        val wordId: Long = navBackStackEntry.arguments?.getLong(WORD_ID_ARG)
            ?: throw IllegalArgumentException("Unknown WordId")
        compositionRoot.WordCardScreenDep(wordId = wordId)
    }

    composable(
        route = MainRoutes.PER_DICT_COMPONENTS_PATTERN,
        arguments = listOf(
            navArgument(PER_DICT_COMPONENTS_DICT_ID_ARG) { type = NavType.LongType },
        ),
    ) { navBackStackEntry ->
        val dictId: Long = navBackStackEntry.arguments?.getLong(PER_DICT_COMPONENTS_DICT_ID_ARG)
            ?: throw IllegalArgumentException("Unknown dictionaryId")
        compositionRoot.PerDictionaryComponentsScreenDep(dictionaryId = dictId)
    }
}
