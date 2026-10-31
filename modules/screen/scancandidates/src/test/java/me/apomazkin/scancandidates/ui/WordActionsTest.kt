package me.apomazkin.scancandidates.ui

import me.apomazkin.scancandidates.mate.CardPhase
import me.apomazkin.scancandidates.mate.Fx
import me.apomazkin.scancandidates.mate.Msg
import me.apomazkin.scancandidates.mate.ScanWordUiItem
import me.apomazkin.scancandidates.mate.WordCardUi
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Действия строки слова и плашек становятся сообщениями экрана: тап — по
 * видимому состоянию строки (AWP-42, AWP-43, AWP-46), кнопки слова и плашек —
 * своё сообщение каждая (AWP-47, AWP-48, AWP-49, AWP-61.7, AWP-20, AWP-26,
 * AWP-64, AWP-31.4).
 */
class WordActionsTest {
    private val card = WordCardUi(phase = CardPhase.CANDIDATES, lemma = Fx.lemmaCard("run", false))
    private val key = "ruwiktionary/1.verb.1"

    @Test
    fun `AWP-43_1 AWP-46_1 AWP-42_2 a tap becomes the message by the visible state of the row`() {
        val newWord = ScanWordUiItem("run")
        val inDictionary = ScanWordUiItem("run", isInDictionary = true)
        val withCard = ScanWordUiItem("run", card = card, isExpanded = true)
        val savedWithCard = ScanWordUiItem("run", isInDictionary = true, card = card)

        assertEquals(Msg.LoadLexemes("run"), WordAction.Tap(newWord).toMsg())
        assertEquals(Msg.OpenSavedCard("run"), WordAction.Tap(inDictionary).toMsg())
        assertEquals(Msg.ToggleCard("run"), WordAction.Tap(withCard).toMsg())
        assertEquals(Msg.ToggleCard("run"), WordAction.Tap(savedWithCard).toMsg())
    }

    @Test
    fun `AWP-47_1 AWP-47_2 the buttons of the word and of the plates become the messages with the same addresses`() {
        val cases = listOf(
            WordAction.Remove("run") to Msg.RemoveWord("run"),
            WordAction.CheckByText("run") to Msg.LoadLexemes("run"),
            WordAction.RetryLookup("run") to Msg.LoadLexemes("run"),
            WordAction.Write("run", key) to Msg.AddLexeme("run", key),
            WordAction.RetryAdd("run", key) to Msg.RetryAdd("run", key),
            WordAction.Dismiss("run", key) to Msg.DismissLexeme("run", key),
            WordAction.ToggleMatches("run") to Msg.ToggleComponentMatches("run"),
        )

        cases.forEach { (action, message) -> assertEquals(message, action.toMsg()) }
    }
}
