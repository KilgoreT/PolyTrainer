package me.apomazkin.vocabulary.logic

import me.apomazkin.mate.effects
import me.apomazkin.mate.state
import me.apomazkin.ui.logger.LexemeLogger
import me.apomazkin.vocabulary.entity.WordInfo
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyTabReducerKtTest {

    private val reducer = VocabularyTabReducer(
        logger = object : LexemeLogger {
            override fun log(tag: String, message: String) {}
        }
    )

    @Test
    fun testTermDataLoadMsg() {
        val state = VocabularyTabState(
            isLoading = false
        )
        val result = reducer.reduce(state, Msg.TermDataLoad)
        assertTrue(result.state().isLoading)
        assertThat(
            "Must be 1 effect",
            1,
            equalTo(result.effects().size)
        )
        assertThat(
            "Effect type must be DatasourceEffect.LoadTermData",
            result.effects().first(),
            CoreMatchers.instanceOf(DatasourceEffect.LoadTermData::class.java)
        )
    }

    @Test
    fun testTermDataLoaded() {
        val state = VocabularyTabState()
        assertTrue("Must be loading state", state.isLoading)

        val result = reducer.reduce(state, Msg.TermDataLoaded(termList = DataHelper.termList))
        assertFalse("Must not be loading state", result.state().isLoading)
        assertThat(
            "Term list count must be ${DataHelper.termList.size}",
            DataHelper.termList.size,
            equalTo(result.state().termList.size)
        )
        assertThat("Must be 0 effects", 0, equalTo(result.effects().size))
    }

    @Test
    fun testStartAddWord() {
        val state = VocabularyTabState()

        val result = reducer.reduce(state, Msg.StartAddWord(show = true))
        assertTrue(
            "Is add word dialog opened?",
            result.state().addWordDialogState.isOpen
        )
        assertTrue(
            "Word value is empty?",
            result.state().addWordDialogState.wordValue.isEmpty()
        )
        assertTrue(
            "Word id must be null",
            result.state().addWordDialogState.wordId == null
        )
        assertThat("Must be 0 effects", 0, equalTo(result.effects().size))
    }

    @Test
    fun testWordValueChange() {
        val state = VocabularyTabState(
            addWordDialogState = AddWordDialogState(
                isOpen = true,
                wordValue = ""
            )
        )
        val result = reducer.reduce(state, Msg.WordValueChange("w"))
        assertTrue(
            "Add word dialog must be open",
            result.state().addWordDialogState.isOpen
        )
        assertTrue(
            "Word value is correct after type",
            result.state().addWordDialogState.wordValue == "w"
        )
        assertThat("Must be 0 effects", 0, equalTo(result.effects().size))
    }

    @Test
    fun testStartChangeWord() {
        val targetTerm = DataHelper.termList.first()
        val state = VocabularyTabState(
            topBarState = TopBarState(
                isActionMode = true
            )
        )
        val result = reducer.reduce(
            state = state,
            message = Msg.StartChangeWord(wordId = targetTerm.id, wordValue = targetTerm.wordValue)
        )
        assertTrue("Action mode must be off", !result.state().topBarState.isActionMode)
        assertTrue("Add word dialog must be open", result.state().addWordDialogState.isOpen)
        assertTrue(
            "Word value is correct",
            result.state().addWordDialogState.wordValue == targetTerm.wordValue
        )
        assertTrue(
            "Word id is correct",
            result.state().addWordDialogState.wordId == targetTerm.id
        )
        assertThat("Must be 0 effects", 0, equalTo(result.effects().size))
    }

    @Test
    fun testAddWord() {
        val targetTerm = DataHelper.termList.first()
        val state = VocabularyTabState(
            addWordDialogState = AddWordDialogState(
                isOpen = true,
                wordValue = targetTerm.wordValue
            )
        )
        val result = reducer.reduce(state, Msg.AddWord(targetTerm.wordValue))
        assertTrue("Add word dialog must be closed", !result.state().addWordDialogState.isOpen)
        assertThat(
            "Effects count must be 1",
            1,
            equalTo(result.effects().size)
        )
        assertThat(
            "Effect type must be DatasourceEffect.AddWord",
            result.effects().first(),
            CoreMatchers.instanceOf(DatasourceEffect.AddWord::class.java)
        )
    }

    @Test
    fun testChangeWord() {
        val targetTerm = DataHelper.termList.first()
        val state = VocabularyTabState(
            topBarState = TopBarState(
                isActionMode = true
            ),
            addWordDialogState = AddWordDialogState(
                isOpen = true,
                wordValue = targetTerm.wordValue
            ),
        )
        val result = reducer.reduce(state, Msg.ChangeWord(targetTerm.id, "new value"))
        assertTrue("Action mode must be off", !result.state().topBarState.isActionMode)
        assertTrue("Add word dialog must be closed", !result.state().addWordDialogState.isOpen)
        assertThat(
            "Effects count must be 1",
            1,
            equalTo(result.effects().size)
        )
        assertThat(
            "Effect type must be DatasourceEffect.ChangeWord",
            result.effects().first(),
            CoreMatchers.instanceOf(DatasourceEffect.ChangeWord::class.java)
        )
        assertThat(
            "Must be same wordId",
            targetTerm.id,
            equalTo((result.effects().first() as DatasourceEffect.ChangeWord).wordId)
        )
        assertThat(
            "Must be new word value",
            "new value",
            equalTo((result.effects().first() as DatasourceEffect.ChangeWord).value)
        )
    }

    @Test
    fun testConfirmDeleteWordDialog() {
        val state = VocabularyTabState(
            topBarState = TopBarState(
                isActionMode = true
            ),
        )
        val result = reducer.reduce(
            state, Msg.ConfirmDeleteWordDialog(
                isOpen = true, wordIds = setOf(
                    WordInfo(1, "1"),
                    WordInfo(2, "2")
                )
            )
        )
        assertTrue("Must be action mode", result.state().topBarState.isActionMode)
        assertTrue(
            "Confirm delete word dialog must be opened",
            result.state().confirmWordDeleteDialogState.isOpen
        )
        assertThat(
            "Deleted word count",
            2,
            equalTo(result.state().confirmWordDeleteDialogState.wordIds.size)
        )
        assertThat("Must be 0 effects", 0, equalTo(result.effects().size))
    }

    @Test
    fun testDeleteWord() {
        val state = VocabularyTabState(
            topBarState = TopBarState(
                isActionMode = true
            ),
            confirmWordDeleteDialogState = ConfirmWordDeleteDialogState(
                isOpen = true,
                wordIds = setOf(
                    WordInfo(1, "1"),
                    WordInfo(2, "2"),
                )
            )
        )
        val result = reducer.reduce(
            state = state,
            message = Msg.DeleteWord(
                setOf(
                    WordInfo(1, "1"),
                    WordInfo(2, "2")
                )
            )
        )
        assertFalse(
            "Must not be action mode",
            result.state().topBarState.isActionMode
        )
        assertTrue(
            "Confirm delete word dialog must be closed",
            !result.state().confirmWordDeleteDialogState.isOpen
        )
        assertThat(
            "Effects count must be 2",
            2,
            equalTo(result.effects().size)
        )
        assertThat(
            "Must have DatasourceEffect.DeleteWord effect",
            result.effects().any { it is DatasourceEffect.DeleteWord }
        )
        assertThat(
            "Must be 2 wordIds",
            2,
            equalTo(
                result.effects().filterIsInstance<DatasourceEffect.DeleteWord>()
                    .first().wordSet.size
            )
        )
        assertThat(
            "Must have UiEffect.ShowSnackbar effect",
            result.effects().any { it is UiEffect.ShowSnackbar }
        )
    }
}