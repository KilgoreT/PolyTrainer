package me.apomazkin.vocabulary.tools

import me.apomazkin.vocabulary.R
import me.apomazkin.vocabulary.entity.DictUiEntity
import me.apomazkin.vocabulary.entity.TermUiItem
import me.apomazkin.vocabulary.entity.WordInfo
import me.apomazkin.vocabulary.logic.TopBarState
import me.apomazkin.vocabulary.logic.VocabularyTabState
import java.util.Date


object DataHelper {

    object Data {
        val termList = listOf(
            TermUiItem(
                id = 0,
                wordValue = "uno",
                langId = 0,
                addDate = Date(0),
                isExpand = false,
            ),
            TermUiItem(
                id = 1,
                wordValue = "dos",
                langId = 0,
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