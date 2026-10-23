package me.apomazkin.main

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

private const val WORD_ID_ARG = "wordId"
private const val SCAN_URI_ARG = "uri"
private const val PER_DICT_COMPONENTS_DICT_ID_ARG = "dictionaryId"

fun NavGraphBuilder.vocabulary(
    navController: NavHostController,
    compositionRoot: CompositionRoot,
) {
    tabComposable(TabPoint.VOCABULARY) { navBackStackEntry ->
        compositionRoot.VocabularyHostDep(savedStateHandle = navBackStackEntry.savedStateHandle)
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
        route = MainRoutes.SCAN_CANDIDATES_PATTERN,
        arguments = listOf(navArgument(SCAN_URI_ARG) { type = NavType.StringType }),
    ) { navBackStackEntry ->
        val uri: String = navBackStackEntry.arguments?.getString(SCAN_URI_ARG)
            ?: throw IllegalArgumentException("Unknown uri")
        ProcessBound(navController = navController) {
            compositionRoot.ScanCandidatesScreenDep(uri = uri)
        }
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
