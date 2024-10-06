package me.apomazkin.vocabulary.logic

import me.apomazkin.mate.effects
import me.apomazkin.mate.state
import me.apomazkin.ui.logger.LexemeLogger
import me.apomazkin.vocabulary.entity.WordInfo
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.Test

class OnChangeActionModeKtTest {

    private val reducer = VocabularyTabReducer(
        logger = object : LexemeLogger {
            override fun log(tag: String, message: String) {}
        }
    )

    /**
     * Если в сообщении не указан targetWord, то actionMode не включается
     */
    @Test
    fun testChangeActionModeWithoutTargetWord() {
        val state = VocabularyTabState()
        val result = reducer.reduce(state, Msg.ChangeActionMode(isActionMode = true))
        Assert.assertFalse(
            "Action mode must not be on because targetWord not set",
            result.state().topBarState.isActionMode
        )
        MatcherAssert.assertThat(
            "Must be 0 effects",
            0,
            CoreMatchers.equalTo(result.effects().size)
        )
    }

    /**
     * TODO: может не хранить выбранные слова отдельно,
     * а просто искать их в termList по selected = true?
     *
     * Вход в режим действий над списком terms.
     */
    @Test
    fun onEnterInActionMode() {
        val state = VocabularyTabState(
            isLoading = false,
            termList = DataHelper.termList,
        )
        val targetTerm = DataHelper.termList.first()
        val result = reducer.reduce(
            state,
            Msg.ChangeActionMode(
                isActionMode = true,
                targetWord = WordInfo(targetTerm.id, targetTerm.wordValue)
            )
        )
        Assert.assertTrue(
            "Action mode must be on",
            result.state().topBarState.isActionMode
        )
        MatcherAssert.assertThat(
            "Selected term count must be 1",
            1,
            CoreMatchers.equalTo(result.state().topBarState.actionState.selectedTermIds.size)
        )
        Assert.assertTrue(
            "Target term must be selected",
            result.state().termList.find { it.id == targetTerm.id }?.isSelected == true
        )
        MatcherAssert.assertThat(
            "Count of selected terms must be 1",
            1,
            CoreMatchers.equalTo(result.state().termList.filter { it.isSelected }.size)
        )
        MatcherAssert.assertThat(
            "Must be 0 effects",
            0,
            CoreMatchers.equalTo(result.effects().size)
        )
    }

    /**
     * Добавление слова в список слов для действий Action Mode.
     */
    @Test
    fun onTapAnotherTermWhenActionMode() {
        val addedTerm = DataHelper.termList.first()
        val state = VocabularyTabState(
            isLoading = false,
            termList = DataHelper.termList.map { if (it.id == addedTerm.id) it.copy(isSelected = true) else it },
            topBarState = TopBarState(
                isActionMode = true,
                actionState = TopBarState.Action(
                    selectedTermIds = setOf(WordInfo(addedTerm.id, addedTerm.wordValue))
                )
            )
        )
        val targetTerm = DataHelper.termList[1]
        val result = reducer.reduce(
            state,
            Msg.ChangeActionMode(
                isActionMode = true,
                targetWord = WordInfo(targetTerm.id, targetTerm.wordValue)
            )
        )
        Assert.assertTrue(
            "Action mode must be on",
            result.state().topBarState.isActionMode
        )
        MatcherAssert.assertThat(
            "Selected term count must be 2",
            2,
            CoreMatchers.equalTo(result.state().topBarState.actionState.selectedTermIds.size)
        )
        Assert.assertTrue(
            "Target term must be selected",
            result.state().termList.find { it.id == targetTerm.id }?.isSelected == true
        )
        MatcherAssert.assertThat(
            "Count of selected terms must be 2",
            2,
            CoreMatchers.equalTo(result.state().termList.filter { it.isSelected }.size)
        )
        MatcherAssert.assertThat(
            "Must be 0 effects",
            0,
            CoreMatchers.equalTo(result.effects().size)
        )
    }

    /**
     * Удаление последнего слова из списка слов для действий Action Mode.
     */
    @Test
    fun onTapLastSelectedWordInActionMode() {
        val targetTerm = DataHelper.termList.first()
        val state = VocabularyTabState(
            isLoading = false,
            termList = DataHelper.termList.map { if (it.id == targetTerm.id) it.copy(isSelected = true) else it },
            topBarState = TopBarState(
                isActionMode = true,
                actionState = TopBarState.Action(
                    selectedTermIds = setOf(WordInfo(targetTerm.id, targetTerm.wordValue))
                )
            )
        )
        val result = reducer.reduce(
            state,
            Msg.ChangeActionMode(
                isActionMode = false,
                targetWord = WordInfo(targetTerm.id, targetTerm.wordValue)
            )
        )
        Assert.assertFalse(
            "Action mode must be off",
            result.state().topBarState.isActionMode
        )
        MatcherAssert.assertThat(
            "Selected term count must be 0",
            0,
            CoreMatchers.equalTo(result.state().topBarState.actionState.selectedTermIds.size)
        )
        Assert.assertTrue(
            "Target term must be unselected",
            result.state().termList.find { it.id == targetTerm.id }?.isSelected == false
        )
        MatcherAssert.assertThat(
            "Count of selected terms must be 0",
            0,
            CoreMatchers.equalTo(result.state().termList.filter { it.isSelected }.size)
        )
        MatcherAssert.assertThat(
            "Must be 0 effects",
            0,
            CoreMatchers.equalTo(result.effects().size)
        )
    }

    @Test
    fun onTapTermThatAlreadyInActionMode() {
        val targetTerm = DataHelper.termList.first()
        val alreadyAddedIds = DataHelper.termList
            .subList(0, 2)
            .map { it.id }
        val alreadyAddedWordInfo = DataHelper.termList
            .subList(0, 2)
            .map { WordInfo(it.id, it.wordValue) }

        val state = VocabularyTabState(
            isLoading = false,
            termList = DataHelper.termList.map {
                if (alreadyAddedIds.contains(it.id)) it.copy(isSelected = true) else it
            },
            topBarState = TopBarState(
                isActionMode = true,
                actionState = TopBarState.Action(
                    selectedTermIds = alreadyAddedWordInfo.toSet()
                )
            )
        )
        val result = reducer.reduce(
            state,
            Msg.ChangeActionMode(
                isActionMode = true,
                targetWord = WordInfo(targetTerm.id, targetTerm.wordValue)
            )
        )
        Assert.assertTrue(
            "Action mode must be on",
            result.state().topBarState.isActionMode
        )
        MatcherAssert.assertThat(
            "Selected term count must be 1",
            1,
            CoreMatchers.equalTo(result.state().topBarState.actionState.selectedTermIds.size)
        )
        Assert.assertTrue(
            "Target term must not be selected",
            result.state().termList.find { it.id == targetTerm.id }?.isSelected == false
        )
        MatcherAssert.assertThat(
            "Count of selected terms must be 1",
            1,
            CoreMatchers.equalTo(result.state().termList.filter { it.isSelected }.size)
        )
        MatcherAssert.assertThat(
            "Must be 0 effects",
            0,
            CoreMatchers.equalTo(result.effects().size)
        )
    }

    @Test
    fun onExitFromActionMode() {
        val alreadyAddedTerm = DataHelper.termList.first()
        val state = VocabularyTabState(
            isLoading = false,
            termList = DataHelper.termList.map {
                if (it.id == alreadyAddedTerm.id) it.copy(
                    isSelected = true
                ) else it
            },
            topBarState = TopBarState(
                isActionMode = true,
                actionState = TopBarState.Action(
                    selectedTermIds = setOf(
                        WordInfo(
                            alreadyAddedTerm.id,
                            alreadyAddedTerm.wordValue
                        )
                    )
                )
            )
        )
        val result = reducer.reduce(
            state,
            Msg.ChangeActionMode(
                isActionMode = false,
            )
        )
        Assert.assertFalse(
            "Action mode must be off",
            result.state().topBarState.isActionMode
        )
        MatcherAssert.assertThat(
            "Selected term count must be 0",
            0,
            CoreMatchers.equalTo(result.state().topBarState.actionState.selectedTermIds.size)
        )
        Assert.assertTrue(
            "Target term must be unselected",
            result.state().termList.find { it.id == alreadyAddedTerm.id }?.isSelected == false
        )
        MatcherAssert.assertThat(
            "Count of selected terms must be 0",
            0,
            CoreMatchers.equalTo(result.state().termList.filter { it.isSelected }.size)
        )
        MatcherAssert.assertThat(
            "Must be 0 effects",
            0,
            CoreMatchers.equalTo(result.effects().size)
        )
    }
}