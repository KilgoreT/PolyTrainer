package me.apomazkin.vocabulary.tools

import me.apomazkin.vocabulary.R
import me.apomazkin.vocabulary.entity.LangUiEntity
import me.apomazkin.vocabulary.entity.TermUiItem
import me.apomazkin.vocabulary.logic.TopBarActionState
import me.apomazkin.vocabulary.logic.VocabularyTabState
import java.util.*


object DataHelper {

    object Data {
        val termList = listOf(
            TermUiItem(
                id = 0,
                wordValue = "uno",
                langId = 0,
                addDate = Date(0),
                isExpand = false,
                isDropDownOpen = true
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
            topBarActionState = TopBarActionState(
                isLoading = false,
                currentLang = LangUiEntity(
                    iconRes = R.drawable.example_ic_flag_gb,
                    title = "Британи",
                    numericCode = 0,
                )
            )
        )
    }
}