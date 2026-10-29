package me.apomazkin.scancandidates.mate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Чистые функции списков: леммы текста → списки окна, пометка «в словаре» по
 * леммам словаря (AWP-39), тексты всех слов, слово и карточки по тексту,
 * карточки лемм, счёт недобавленных слов (AWP-51). Адреса строк спеки —
 * `AWP-NN_k`.
 */
class ScanWordListsTest {

    private fun card(
        phase: CardPhase,
        vararg exists: Boolean,
        isAdding: Boolean = false,
    ) = WordCardUi(phase = phase, lemma = Fx.lemmaCard("run", *exists), isAdding = isAdding)

    @Test
    fun `lemmas of the text become four lists of words without marks in the same order`() {
        val lists = DomainFx.textbook.toUi()

        assertEquals(Fx.textbookLexical, lists.lexical.texts())
        assertEquals(Fx.textbookProper, lists.proper.texts())
        assertEquals(Fx.textbookFunction, lists.function.texts())
        assertTrue(lists.unparsed.isEmpty())
        assertEquals(0, lists.countInDictionary())
    }

    @Test
    fun `AWP-39_1 a lemma run in the dictionary marks run in lexical and nothing else`() {
        val lists = Fx.lists(lexical = listOf("run", "link"), function = listOf("the"))

        val marked = lists.withDictionaryMarks(listOf(Fx.run1))

        assertEquals(listOf("run"), marked.lexical.marked())
        assertTrue(marked.function.marked().isEmpty())
        assertEquals(1, marked.countInDictionary())
    }

    @Test
    fun `AWP-39_2 the mark ignores case - Run in the dictionary marks run`() {
        val lists = Fx.lists(lexical = listOf("run"))

        val marked = lists.withDictionaryMarks(listOf(Fx.existing(Fx.RUN_WORD, "Run")))

        assertEquals(listOf("run"), marked.lexical.marked())
    }

    @Test
    fun `AWP-39_3 no lemma with the text - no mark`() {
        val lists = Fx.lists(lexical = listOf("run"))

        val marked = lists.withDictionaryMarks(listOf(Fx.existing(Fx.RUN_WORD, "runner")))

        assertTrue(marked.lexical.marked().isEmpty())
    }

    @Test
    fun `AWP-39_4 ёж in the dictionary does not mark еж - ё and е are different letters`() {
        val lists = Fx.lists(lexical = listOf("еж"))

        val marked = lists.withDictionaryMarks(listOf(Fx.existing(5L, "ёж")))

        assertTrue(marked.lexical.marked().isEmpty())
    }

    @Test
    fun `marks apply to every list and leave the texts and the order as they were`() {
        val lists = Fx.lists(
            lexical = listOf("study"),
            proper = listOf("Laurie"),
            function = listOf("the"),
            unparsed = listOf("sh"),
        )
        val existing = listOf(Fx.study1, Fx.existing(7L, "laurie"), Fx.existing(8L, "sh"))

        val marked = lists.withDictionaryMarks(existing)

        assertEquals(lists.allTexts(), marked.allTexts())
        assertEquals(listOf("study"), marked.lexical.marked())
        assertEquals(listOf("Laurie"), marked.proper.marked())
        assertTrue(marked.function.marked().isEmpty())
        assertEquals(listOf("sh"), marked.unparsed.marked())
        assertEquals(3, marked.countInDictionary())
    }

    @Test
    fun `all texts go in the order of the lists - lexical, proper, function, unparsed`() {
        val lists = Fx.lists(
            lexical = listOf("nube"),
            proper = listOf("Estela"),
            function = listOf("el"),
            unparsed = listOf("sh"),
        )

        assertEquals(listOf("nube", "Estela", "el", "sh"), lists.allTexts())
    }

    // ===== Слово и карточки по тексту =====

    @Test
    fun `wordOf finds the word in any list and gives null for an unknown text`() {
        val lists = Fx.lists(lexical = listOf("run"), unparsed = listOf("sh"))

        assertEquals("run", lists.wordOf("run")?.text)
        assertEquals("sh", lists.wordOf("sh")?.text)
        assertNull(lists.wordOf("bank"))
    }

    @Test
    fun `updateWord changes every row with the text and leaves the others`() {
        val lists = Fx.lists(lexical = listOf("have", "run"), function = listOf("have"))

        val expanded = lists.updateWord("have") { it.copy(isExpanded = true) }

        assertTrue(expanded.lexical[0].isExpanded)
        assertFalse(expanded.lexical[1].isExpanded)
        assertTrue(expanded.function.single().isExpanded)
    }

    @Test
    fun `AWP-48_1 removeWord drops every row with the text and keeps the order of the rest`() {
        val lists = Fx.lists(lexical = listOf("study", "have", "link"), function = listOf("have", "the"))

        val rest = lists.removeWord("have")

        assertEquals(listOf("study", "link"), rest.lexical.texts())
        assertEquals(listOf("the"), rest.function.texts())
        assertNull(rest.wordOf("have"))
    }

    @Test
    fun `updateCards changes the cards and leaves the words without a card`() {
        val lists = ScanWordListsUi(
            lexical = listOf(ScanWordUiItem("ran", card = card(CardPhase.LOADING)), ScanWordUiItem("the")),
        )

        val adding = lists.updateCards { it.copy(isAdding = true) }

        assertTrue(adding.lexical[0].card?.isAdding == true)
        assertNull(adding.lexical[1].card)
    }

    @Test
    fun `updateLemmaCards changes the cards of the lemma and is null when nothing changed`() {
        val run = card(CardPhase.CANDIDATES, false, true)
        val bank = WordCardUi(phase = CardPhase.CANDIDATES, lemma = Fx.lemmaCard("bank", false))
        val lists = ScanWordListsUi(
            lexical = listOf(
                ScanWordUiItem("ran", card = run),
                ScanWordUiItem("bank", card = bank),
                ScanWordUiItem("the"),
            ),
        )

        val changed = lists.updateLemmaCards("run") { it.copy(existingCount = 5) }

        assertEquals(5, changed?.lexical?.get(0)?.card?.lemma?.existingCount)
        assertEquals(0, changed?.lexical?.get(1)?.card?.lemma?.existingCount)
        assertNull(lists.updateLemmaCards("run") { null })
        assertNull(lists.updateLemmaCards("sky") { it })
    }

    @Test
    fun `allLemmaCards lists the card of a word once even when the word is in two lists`() {
        val have = card(CardPhase.CANDIDATES, false)
        val lists = ScanWordListsUi(
            lexical = listOf(ScanWordUiItem("have", card = have), ScanWordUiItem("link")),
            function = listOf(ScanWordUiItem("have", card = have)),
        )

        assertEquals(listOf(have.lemma), lists.allLemmaCards())
    }

    // ===== Счёт недобавленных слов (AWP-51) =====

    @Test
    fun `AWP-51_7 AWP-51_8 AWP-51_9 AWP-51_10 AWP-51_11 countPending counts loading cards and candidates with something to add`() {
        val lists = ScanWordListsUi(
            lexical = listOf(
                ScanWordUiItem("loading", card = card(CardPhase.LOADING)),
                ScanWordUiItem("new", card = card(CardPhase.CANDIDATES, false, true)),
                ScanWordUiItem("all-existing", card = card(CardPhase.CANDIDATES, true, true)),
                ScanWordUiItem("saved", card = card(CardPhase.SAVED), isInDictionary = true),
                ScanWordUiItem("not-found", card = card(CardPhase.NOT_FOUND)),
                ScanWordUiItem("error", card = card(CardPhase.ERROR)),
                ScanWordUiItem("untouched"),
            ),
            function = listOf(ScanWordUiItem("loading", card = card(CardPhase.LOADING))),
        )

        assertEquals(2, lists.countPending())
    }

    @Test
    fun `anyAdding is true while a card of any word is being written`() {
        val idle = ScanWordListsUi(lexical = listOf(ScanWordUiItem("ran", card = card(CardPhase.CANDIDATES, false))))
        val busy = idle.updateCards { it.copy(isAdding = true) }

        assertFalse(idle.anyAdding())
        assertTrue(busy.anyAdding())
    }
}
