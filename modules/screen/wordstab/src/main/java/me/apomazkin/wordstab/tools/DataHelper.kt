package me.apomazkin.wordstab.tools

import me.apomazkin.wordrow.entity.DefinitionUiEntity
import me.apomazkin.wordrow.entity.LexemeUiItem
import me.apomazkin.wordrow.entity.TermUiItem
import me.apomazkin.wordrow.entity.TranslationUiEntity
import me.apomazkin.wordstab.entity.WordInfo
import me.apomazkin.wordstab.logic.WordsTabState
import me.apomazkin.wordstab.logic.TopBarState
import java.util.Date


object DataHelper {

    object Data {
        val termList = listOf(
            TermUiItem(
                id = 0,
                wordValue = "uno",
                dictionaryId = 0,
                lexemeList = listOf(
                    LexemeUiItem(
                        id = 0,
                        translation = TranslationUiEntity("одын"),
                        definition = DefinitionUiEntity(" одын одын одын одын одын одын"),
                        addDate = Date(0),
                    ),
                    LexemeUiItem(
                        id = 1,
                        translation = TranslationUiEntity("единица"),
                        definition = DefinitionUiEntity("раз-раз раз-раз раз-раз раз-раз"),
                        addDate = Date(0),
                    ),
                ),
                addDate = Date(0),
                isExpand = false,
            ),
            TermUiItem(
                id = 1,
                wordValue = "dos",
                dictionaryId = 0,
                lexemeList = listOf(
                    LexemeUiItem(
                        id = 2,
                        translation = null,
                        definition = DefinitionUiEntity("два два два два дваааа дваааа два двааааааа"),
                        addDate = Date(0),
                    )
                ),
                addDate = Date(0),
                isExpand = false
            ),
            TermUiItem(
                id = 2,
                wordValue = "tres",
                dictionaryId = 0,
                addDate = Date(0),
                isExpand = false
            ),
        )
    }

    object State {
        val empty = WordsTabState(isLoading = false)
        val loading = WordsTabState(isLoading = true)
        val loaded = WordsTabState(
            isLoading = false,
//            termList = Data.termList,
            topBarState = TopBarState(
                isActionMode = true,
                actionState = TopBarState.Action(
                    selectedTermIds = setOf(
                        WordInfo(id = 0, wordValue = "uno")
                    )
                )
            )
        )
    }
}