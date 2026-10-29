package me.apomazkin.scancandidates.ui

import me.apomazkin.candidates.CandidateAction
import me.apomazkin.candidates.CandidatesSheetUi
import me.apomazkin.candidates.CardOwnGroupUi
import me.apomazkin.candidates.CardTargetUi
import me.apomazkin.candidates.ComponentRefUi
import me.apomazkin.candidates.LemmaWordChoice
import me.apomazkin.candidates.ValueRefUi
import me.apomazkin.scancandidates.mate.Msg
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Действия общих виджетов окна становятся сообщениями экрана с теми же
 * данными: правки карточки, листы, «куда класть», группы, закрытие и вопрос о
 * нём; крестик карточки, кнопки панели первой версии и действия диалога
 * карточки слова ничего не меняют.
 */
class CandidateActionsTest {
    private val key = "ruwiktionary/1.verb.1"
    private val value = ValueRefUi(lemma = "run", key = key, componentId = 1L, index = 0)
    private val component = ComponentRefUi(lemma = "run", key = key, componentId = 2L)

    @Test
    fun `edits, sheets, targets, groups and closing become the messages with the same data`() {
        val sheet = CandidatesSheetUi.TextEdit(ref = value, draft = "брать", canReset = true)
        val cases = listOf(
            CandidateAction.DeleteLexeme("run", key) to Msg.DeleteLexeme("run", key),
            CandidateAction.DeleteValue(value) to Msg.DeleteValue(value),
            CandidateAction.OpenSheet(sheet) to Msg.OpenSheet(sheet),
            CandidateAction.OpenSheet(CandidatesSheetUi.CommonGroup) to Msg.OpenSheet(CandidatesSheetUi.CommonGroup),
            CandidateAction.CloseSheet to Msg.CloseSheet,
            CandidateAction.UpdateDraft("x") to Msg.UpdateDraft("x"),
            CandidateAction.SubmitText(value, "x") to Msg.SubmitText(value, "x"),
            CandidateAction.ResetText(value) to Msg.ResetText(value),
            CandidateAction.SetOption(component, 21L) to Msg.SetOption(component, 21L),
            CandidateAction.SetOption(component, null) to Msg.SetOption(component, null),
            CandidateAction.SetCardTarget("run", CardTargetUi.NewWord) to Msg.SetCardTarget("run", CardTargetUi.NewWord),
            CandidateAction.SetCardGroup("run", CardOwnGroupUi.All) to Msg.SetCardGroup("run", CardOwnGroupUi.All),
            CandidateAction.SetCommonGroup(701L) to Msg.SetCommonGroup(701L),
            CandidateAction.SetCommonGroup(null) to Msg.SetCommonGroup(null),
            CandidateAction.RequestClose to Msg.RequestClose,
            CandidateAction.ConfirmClose to Msg.ConfirmClose,
            CandidateAction.Stay to Msg.Stay,
        )

        cases.forEach { (action, message) -> assertEquals(message, action.toMsg()) }
    }

    @Test
    fun `the card cross, the panel buttons of the first version and the card dialog actions change nothing on this screen`() {
        val ignored = listOf(
            CandidateAction.DeleteCard("run"),
            CandidateAction.ToggleComponentMatches,
            CandidateAction.ToggleLemmaWord(LemmaWordChoice.Rename("run")),
            CandidateAction.RequestAdd,
            CandidateAction.ConfirmAdd,
            CandidateAction.CancelAdd,
            CandidateAction.RetryAdd,
            CandidateAction.RetryLookup,
        )

        ignored.forEach { action ->
            assertEquals(action.toString(), Msg.NoOperation, action.toMsg())
        }
    }
}
