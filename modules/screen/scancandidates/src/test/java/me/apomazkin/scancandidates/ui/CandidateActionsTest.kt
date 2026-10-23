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

/** Действия общих виджетов окна становятся сообщениями экрана с теми же данными. */
class CandidateActionsTest {
    private val key = "ruwiktionary/1.verb.1"
    private val value = ValueRefUi(lemma = "run", key = key, componentId = 1L, index = 0)
    private val component = ComponentRefUi(lemma = "run", key = key, componentId = 2L)

    @Test
    fun `every action of the shared widgets becomes the message with the same data`() {
        val sheet = CandidatesSheetUi.TextEdit(ref = value, draft = "брать", canReset = true)
        val cases = listOf(
            CandidateAction.DeleteCard("run") to Msg.DeleteCard("run"),
            CandidateAction.DeleteLexeme("run", "k") to Msg.DeleteLexeme("run", "k"),
            CandidateAction.DeleteValue(value) to Msg.DeleteValue(value),
            CandidateAction.OpenSheet(sheet) to Msg.OpenSheet(sheet),
            CandidateAction.CloseSheet to Msg.CloseSheet,
            CandidateAction.UpdateDraft("x") to Msg.UpdateDraft("x"),
            CandidateAction.SubmitText(value, "x") to Msg.SubmitText(value, "x"),
            CandidateAction.ResetText(value) to Msg.ResetText(value),
            CandidateAction.SetOption(component, 21L) to Msg.SetOption(component, 21L),
            CandidateAction.SetOption(component, null) to Msg.SetOption(component, null),
            CandidateAction.SetCardTarget("run", CardTargetUi.NewWord) to
                Msg.SetCardTarget("run", CardTargetUi.NewWord),
            CandidateAction.SetCardGroup("run", CardOwnGroupUi.All) to
                Msg.SetCardGroup("run", CardOwnGroupUi.All),
            CandidateAction.SetCommonGroup(701L) to Msg.SetCommonGroup(701L),
            CandidateAction.ToggleComponentMatches to Msg.ToggleComponentMatches,
            CandidateAction.ToggleNotAdded to Msg.ToggleNotAdded,
            CandidateAction.RequestAdd to Msg.RequestAdd,
            CandidateAction.ConfirmAdd to Msg.ConfirmAdd,
            CandidateAction.CancelAdd to Msg.CancelAdd,
            CandidateAction.RetryAdd to Msg.RetryAdd,
            CandidateAction.RequestClose to Msg.RequestClose,
            CandidateAction.ConfirmClose to Msg.ConfirmClose,
            CandidateAction.Stay to Msg.Stay,
        )

        cases.forEach { (action, message) -> assertEquals(message, action.toMsg()) }
    }

    @Test
    fun `the actions of the card dialog change nothing on this screen`() {
        assertEquals(
            Msg.NoOperation,
            CandidateAction.ToggleLemmaWord(LemmaWordChoice.Rename("run")).toMsg(),
        )
        assertEquals(Msg.NoOperation, CandidateAction.RetryLookup.toMsg())
    }
}
