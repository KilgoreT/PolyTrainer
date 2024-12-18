package me.apomazkin.dictionarytab.tools

import me.apomazkin.dictionarytab.R
import me.apomazkin.dictionarytab.entity.DefinitionUiEntity
import me.apomazkin.dictionarytab.entity.DictUiEntity
import me.apomazkin.dictionarytab.entity.LexemeUiItem
import me.apomazkin.dictionarytab.entity.TermUiItem
import me.apomazkin.dictionarytab.entity.TranslationUiEntity
import me.apomazkin.dictionarytab.entity.WordInfo
import me.apomazkin.dictionarytab.logic.TopBarState
import me.apomazkin.dictionarytab.logic.VocabularyTabState
import java.util.Date


object DataHelper {

    object Data {
        val termList = listOf(
            TermUiItem(
                id = 0,
                wordValue = "uno",
                langId = 0,
                lexemeList = listOf(
                    LexemeUiItem(
                        id = 0,
                        wordId = 0,
                        translation = TranslationUiEntity("одын"),
                        definition = DefinitionUiEntity(" одын одын одын одын одын одын"),
                        addDate = Date(0),
                    ),
                    LexemeUiItem(
                        id = 1,
                        wordId = 0,
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
                langId = 0,
                lexemeList = listOf(
                    LexemeUiItem(
                        id = 2,
                        wordId = 1,
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
                langId = 0,
                addDate = Date(0),
                isExpand = false
            ),
        )
    }

    object State {
        val empty = VocabularyTabState(isLoading = false)
        val loading = VocabularyTabState(isLoading = true)
        val loaded = VocabularyTabState(
            isLoading = false,
            termList = Data.termList,
            topBarState = TopBarState(
                isActionMode = true,
                mainState = TopBarState.Main(
                    isLoading = false,
                    currentDict = DictUiEntity(
                        flagRes = R.drawable.example_ic_flag_gb,
                        title = "Британи",
                        numericCode = 0,
                    )
                ),
                actionState = TopBarState.Action(
                    selectedTermIds = setOf(
                        WordInfo(id = 0, wordValue = "uno")
                    )
                )
            )
        )
    }
}