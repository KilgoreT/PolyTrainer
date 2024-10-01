package me.apomazkin.vocabulary.logic.processor

import me.apomazkin.vocabulary.entity.TermUiItem
import me.apomazkin.vocabulary.logic.TopBarState
import me.apomazkin.vocabulary.logic.VocabularyTabState
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class OnChangeActionModeKtTest {

    private val date = Date(System.currentTimeMillis())

    @Test
    fun enterInActionMode() {
        val termList = listOf(
            TermUiItem(
                id = 1,
                langId = 0,
                wordValue = "1",
                isSelected = false,
                addDate = date,
            ),
            TermUiItem(
                id = 2,
                langId = 0,
                wordValue = "2",
                isSelected = false,
                addDate = date,
            ),
            TermUiItem(
                id = 3,
                langId = 0,
                wordValue = "3",
                isSelected = false,
                addDate = date,
            ),
        )
        val state = VocabularyTabState(
            isLoading = false,
            topBarState = TopBarState(
                isActionMode = false,
                mainState = TopBarState.Main(),
                actionState = TopBarState.Action(),
            ),
            termList = termList,
        )

        val targetTerm = termList[0]
        val result = onChangeActionMode(state, true, targetTerm.id)
        assertTrue("Is action mode set?", result.first.topBarState.isActionMode)
        assertTrue("Is target term selected?", result.first.termList[0].isSelected)
        assertThat(
            "Selected term count?",
            1,
            equalTo(result.first.topBarState.actionState.selectedTermIds.size)
        )
        assertThat(
            "Effect count?",
            0,
            equalTo(result.second.size)
        )
    }

    @Test
    fun addTermToSelected() {
        val termList = listOf(
            TermUiItem(
                id = 1,
                langId = 0,
                wordValue = "1",
                isSelected = true,
                addDate = date,
            ),
            TermUiItem(
                id = 2,
                langId = 0,
                wordValue = "2",
                isSelected = false,
                addDate = date,
            ),
            TermUiItem(
                id = 3,
                langId = 0,
                wordValue = "3",
                isSelected = false,
                addDate = date,
            ),
        )
        val state = VocabularyTabState(
            isLoading = false,
            topBarState = TopBarState(
                isActionMode = true,
                actionState = TopBarState.Action(
                    selectedTermIds = setOf(1)
                ),
            ),
            termList = termList,
        )

        val targetTerm = termList[1]
        val result = onChangeActionMode(state, true, targetTerm.id)
        assertTrue("Is action mode set?", result.first.topBarState.isActionMode)
        assertTrue("Is init term selected?", result.first.termList[0].isSelected)
        assertTrue("Is target term selected?", result.first.termList[1].isSelected)
        assertTrue("Is other term selected?", !result.first.termList[2].isSelected)
        assertThat(
            "Selected term count?",
            2,
            equalTo(result.first.topBarState.actionState.selectedTermIds.size)
        )
        assertThat(
            "Effect count?",
            0,
            equalTo(result.second.size)
        )
    }

    @Test
    fun removeTermFromSelected() {
        val termList = listOf(
            TermUiItem(
                id = 1,
                langId = 0,
                wordValue = "1",
                isSelected = true,
                addDate = date,
            ),
            TermUiItem(
                id = 2,
                langId = 0,
                wordValue = "2",
                isSelected = true,
                addDate = date,
            ),
            TermUiItem(
                id = 3,
                langId = 0,
                wordValue = "3",
                isSelected = false,
                addDate = date,
            ),
        )
        val state = VocabularyTabState(
            isLoading = false,
            topBarState = TopBarState(
                isActionMode = true,
                actionState = TopBarState.Action(
                    selectedTermIds = setOf(1, 2)
                ),
            ),
            termList = termList,
        )

        val targetTerm = termList[1]
        val result = onChangeActionMode(state, true, targetTerm.id)
        assertTrue("Is action mode set?", result.first.topBarState.isActionMode)
        assertTrue("Is init term selected?", result.first.termList[0].isSelected)
        assertTrue("Is target term selected?", !result.first.termList[1].isSelected)
        assertTrue("Is other term selected?", !result.first.termList[2].isSelected)
        assertThat(
            "Selected term count?",
            1,
            equalTo(result.first.topBarState.actionState.selectedTermIds.size)
        )
        assertThat(
            "Effect count?",
            0,
            equalTo(result.second.size)
        )
    }

    @Test
    fun exitFromActionMode() {
        val termList = listOf(
            TermUiItem(
                id = 1,
                langId = 0,
                wordValue = "1",
                isSelected = true,
                addDate = date,
            ),
            TermUiItem(
                id = 2,
                langId = 0,
                wordValue = "2",
                isSelected = true,
                addDate = date,
            ),
            TermUiItem(
                id = 3,
                langId = 0,
                wordValue = "3",
                isSelected = false,
                addDate = date,
            ),
        )
        val state = VocabularyTabState(
            isLoading = false,
            topBarState = TopBarState(
                isActionMode = true,
                actionState = TopBarState.Action(
                    selectedTermIds = setOf(1, 2)
                ),
            ),
            termList = termList,
        )

        val result = onChangeActionMode(state, false, null)
        assertTrue("Is action mode set?", !result.first.topBarState.isActionMode)
        assertTrue("Is init term selected?", !result.first.termList[0].isSelected)
        assertTrue("Is target term selected?", !result.first.termList[1].isSelected)
        assertTrue("Is other term selected?", !result.first.termList[2].isSelected)
        assertThat(
            "Selected term count?",
            0,
            equalTo(result.first.topBarState.actionState.selectedTermIds.size)
        )
        assertThat(
            "Effect count?",
            0,
            equalTo(result.second.size)
        )
    }
}