package me.apomazkin.scancandidates.mate

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.NavigationEffect
import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.assertEffects
import io.github.kilgoret.mate.test.assertNoEffects
import io.github.kilgoret.mate.test.testReduce
import me.apomazkin.candidates.CardTargetUi
import me.apomazkin.candidates.RemarkUiItem
import me.apomazkin.candidates.WordRemarkUi
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.ComponentTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Карточка под словом глазами редьюсера: раскрытие и сворачивание (AWP-42),
 * значения нового слова (AWP-43, AWP-57), слово «в словаре» и проверка по
 * тексту (AWP-46, AWP-49, AWP-18), «Добавить» (AWP-47, AWP-50.12), удаление
 * слова (AWP-48), «не найдено», ошибка и отказ (AWP-55, AWP-61), разделы
 * карточки (AWP-30, AWP-31). Адреса строк спеки — `AWP-NN_k`.
 */
class WordCardReducerTest {
    private val reducer = reducer()

    private fun lookupOf(word: String) = DatasourceEffect.LookupLemma(
        word = word,
        lemma = word,
        dictionaryId = Fx.DICT,
        text = Fx.TEXTBOOK_TEXT,
        translation = Fx.TEXTBOOK_TRANSLATION,
    )

    // ===== Раскрытие и сворачивание (AWP-42) =====

    @Test
    fun `AWP-42_1 AWP-57_1 a tap on a new word opens the loading card under it and asks the server, the next word opens its own`() {
        val window = reducer.windowOf("friendship", "sibling")

        val first = reducer.testReduce(window, Msg.LoadLexemes("friendship"))
        val both = reducer.step(first.state(), Msg.LoadLexemes("sibling"))

        val friendship = first.state().word("friendship")
        assertTrue(friendship.isExpanded)
        assertEquals(CardPhase.LOADING, friendship.card?.phase)
        assertEquals("friendship", friendship.card?.lemma?.lemma)
        first.assertEffects(setOf<Effect>(lookupOf("friendship")))
        assertTrue(both.word("friendship").isExpanded && both.word("sibling").isExpanded)
        assertEquals(CardPhase.LOADING, both.card("sibling").phase)
    }

    @Test
    fun `AWP-42_2 a second tap folds the card of the word only, the other card stays open`() {
        val two = Fx.lists(lexical = listOf("ran", "bank"))
        val ran = reducer.withCard("ran", Fx.ran, lists = two)
        val both = reducer.step(ran, Msg.LoadLexemes("bank"))

        val folded = reducer.testReduce(both, Msg.ToggleCard("ran"))

        assertFalse(folded.state().word("ran").isExpanded)
        assertTrue(folded.state().word("bank").isExpanded)
        assertEquals(both.card("ran"), folded.state().card("ran"))
        folded.assertNoEffects()
    }

    @Test
    fun `AWP-42_4 a folded card opens as it was, without the deleted value and without a new request`() {
        val loaded = reducer.withCard("ran", Fx.ran)
        val deleted = reducer.step(loaded, Msg.DeleteLexeme("run", "ruwiktionary/1.verb.2"))
        val folded = reducer.step(deleted, Msg.ToggleCard("ran"))

        val reopened = reducer.testReduce(folded, Msg.ToggleCard("ran"))

        assertTrue(reopened.state().word("ran").isExpanded)
        assertEquals(deleted.card("ran"), reopened.state().card("ran"))
        assertEquals(
            listOf("ruwiktionary/1.verb.1"),
            reopened.state().lemmaCard("ran").lexemes.map { it.key },
        )
        reopened.assertNoEffects()
    }

    @Test
    fun `AWP-42_5 the answer lands in a card folded during loading and shows on the next tap`() {
        val loading = reducer.step(reducer.windowOf("corrían"), Msg.LoadLexemes("corrían"))
        val folded = reducer.step(loading, Msg.ToggleCard("corrían"))

        val answered = reducer.step(folded, Fx.lexemes("corrían", Fx.corrian))
        val reopened = reducer.step(answered, Msg.ToggleCard("corrían"))

        assertFalse(answered.word("corrían").isExpanded)
        assertEquals(CardPhase.CANDIDATES, answered.card("corrían").phase)
        assertTrue(reopened.word("corrían").isExpanded)
        assertEquals(3, reopened.lemmaCard("corrían").lexemes.size)
    }

    @Test
    fun `AWP-42_6 a card folded with an error opens with the same error and retry, no request goes by itself`() {
        val loading = reducer.step(reducer.windowOf("corrían"), Msg.LoadLexemes("corrían"))
        val failure = Msg.LexemesUnavailable("corrían", R.string.scan_server_unavailable)
        val folded = reducer.steps(loading, failure, Msg.ToggleCard("corrían"))

        val reopened = reducer.testReduce(folded, Msg.ToggleCard("corrían"))

        val card = reopened.state().card("corrían")
        assertEquals(CardPhase.ERROR, card.phase)
        assertEquals(R.string.scan_server_unavailable, card.errorRes)
        assertTrue(card.hasRetryButton)
        reopened.assertNoEffects()
    }

    @Test
    fun `a word with one text in two lists carries one card in both rows`() {
        val lists = Fx.lists(lexical = listOf("have"), function = listOf("have"))
        val window = reducer.window(found = Fx.found(lists = lists))

        val loading = reducer.step(window, Msg.LoadLexemes("have"))

        assertEquals(CardPhase.LOADING, loading.lists.lexical.single().card?.phase)
        assertEquals(CardPhase.LOADING, loading.lists.function.single().card?.phase)
        assertTrue(loading.lists.function.single().isExpanded)
        assertEquals(1, loading.lists.allLemmaCards().size)
        assertEquals(1, loading.lists.countPending())
    }

    // ===== Значения нового слова (AWP-43) =====

    @Test
    fun `AWP-43_1 AWP-43_4 AWP-57_3 corrian brings correr with three lexemes in the selection order, only the first in the text`() {
        val state = reducer.withCard("corrían", Fx.corrian)

        val card = state.card("corrían")
        assertEquals(CardPhase.CANDIDATES, card.phase)
        assertEquals("correr", card.lemma.lemma)
        assertEquals("third-person plural imperfect indicative", card.lemma.formNote)
        assertEquals(listOf("corrían"), card.lemma.sourceWords)
        assertEquals(
            listOf("ruwiktionary/1.verb.3", "ruwiktionary/1.verb.1", "ruwiktionary/1.verb.5"),
            card.lemma.lexemes.map { it.key },
        )
        assertEquals(listOf(true, false, false), card.lemma.lexemes.map { it.isInContext })
        assertEquals(
            listOf("Перевод: течь, литься, струиться (о жидкости)", "Часть речи: глагол", "Толкование: to flow"),
            state.shown("corrían", "ruwiktionary/1.verb.3"),
        )
        assertTrue(card.canAdd)
        assertNull(card.errorRes)
    }

    @Test
    fun `AWP-43_7 children answered with child puts the card on child with the lemma of the dictionary and its saved lexemes`() {
        val state = reducer.withCard("children", Fx.children, existing = listOf(Fx.childExisting))

        val card = state.card("children")
        assertEquals("child", card.lemma.lemma)
        assertEquals("plural", card.lemma.formNote)
        assertEquals(CardTargetUi.Existing(Fx.CHILD_WORD), card.lemma.target)
        assertEquals(1, card.lemma.existingCount)
        assertEquals(listOf(Fx.saved(9L, "ребёнок")), card.saved)
        assertTrue(state.lexeme("children", "ruwiktionary/1.noun.1").exists)
        assertFalse(state.lexeme("children", "ruwiktionary/1.noun.2").exists)
        assertTrue(card.canAdd)
        assertEquals(listOf(Fx.CHILD_WORD), requireNotNull(state.form).existingLemmas.map { it.wordId })
    }

    @Test
    fun `AWP-30_15 AWP-30_12 AWP-31_6 the remarks and the matches of the answer are in the card of the word`() {
        val unclear = Fx.perro.copy(remarks = listOf(WordRemarkUi.ComponentUnclear(Fx.UNCLEAR)))
        val form = Fx.form(components = Fx.tree + Fx.unclear)

        val state = reducer.withCard(
            "perro",
            unclear,
            form = form,
            recommendations = listOf(Fx.genderRecommendation),
        )

        val card = state.card("perro")
        assertEquals(
            listOf(
                RemarkUiItem.ComponentUnclear(Fx.UNCLEAR),
                RemarkUiItem.ComponentRecommended("Род", ComponentTemplate.CHOICE, listOf("м.р.", "ж.р.")),
            ),
            card.remarks,
        )
        assertEquals(
            listOf(
                "translations → Перевод",
                "Часть речи: существительное",
                "Часть речи: прилагательное",
                "Род: м.р.",
                "examples → Пример",
                "gloss → Толкование",
            ),
            card.componentMatchLines.map { it.shown(form) },
        )
    }

    @Test
    fun `AWP-31_4 the matches section of the card is folded on open and toggles per word`() {
        val state = reducer.withCard("perro", Fx.perro)

        val expanded = reducer.testReduce(state, Msg.ToggleComponentMatches("perro"))
        val folded = reducer.step(expanded.state(), Msg.ToggleComponentMatches("perro"))

        assertFalse(state.card("perro").isComponentMatchesExpanded)
        assertTrue(expanded.state().card("perro").isComponentMatchesExpanded)
        assertFalse(folded.card("perro").isComponentMatchesExpanded)
        expanded.assertNoEffects()
    }

    // ===== Слово «в словаре» (AWP-46, AWP-18) =====

    @Test
    fun `AWP-46_1 AWP-18_3 a tap on a word in the dictionary opens the saved lexemes of the newest lemma without a request`() {
        val form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))
        val window = reducer.windowOf("bank", form = form)

        val result = reducer.testReduce(window, Msg.OpenSavedCard("bank"))

        val card = result.state().card("bank")
        assertTrue(result.state().word("bank").isExpanded)
        assertEquals(CardPhase.SAVED, card.phase)
        assertEquals(CardTargetUi.Existing(Fx.BANK_1), card.lemma.target)
        assertEquals(2, card.lemma.existingCount)
        assertEquals(
            listOf(listOf("Перевод: банк"), listOf("Перевод: банкомёт")),
            card.saved.map { it.shown(form) },
        )
        assertTrue(card.lemma.lexemes.isEmpty())
        assertFalse(card.canAdd)
        result.assertNoEffects()
    }

    @Test
    fun `AWP-46_4 a word in the dictionary without lexemes opens an empty card`() {
        val form = Fx.form(existing = listOf(Fx.run1))

        val opened = reducer.step(reducer.windowOf("run", form = form), Msg.OpenSavedCard("run"))

        val card = opened.card("run")
        assertEquals(CardPhase.SAVED, card.phase)
        assertTrue(card.saved.isEmpty())
        assertTrue(card.lemma.lexemes.isEmpty())
        assertEquals(CardTargetUi.Existing(Fx.RUN_WORD), card.lemma.target)
    }

    @Test
    fun `AWP-18_8 choosing the older bank shows its saved lexemes and the check compares with them`() {
        val form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))
        val opened = reducer.step(reducer.windowOf("bank", form = form), Msg.OpenSavedCard("bank"))
        val shore = Fx.lexeme(
            key = "ruwiktionary/1.noun.2",
            translations = listOf("берег"),
            partOfSpeechKey = "noun",
            matches = listOf(Fx.byField(Fx.TRANSLATION, "translations"), Fx.byOption(Fx.PART_OF_SPEECH, Fx.NOUN)),
        )
        val answer = Fx.word("bank", Fx.lemma("bank", listOf(Fx.bankNoun, shore)))

        val older = reducer.step(opened, Msg.SetCardTarget("bank", CardTargetUi.Existing(Fx.BANK_2)))
        val checked = reducer.steps(older, Msg.LoadLexemes("bank"), Fx.lexemes("bank", answer))

        assertEquals(CardTargetUi.Existing(Fx.BANK_2), older.lemmaCard("bank").target)
        assertEquals(listOf(listOf("Перевод: берег")), older.card("bank").saved.map { it.shown(form) })
        assertNull(older.sheet)
        assertEquals(CardTargetUi.Existing(Fx.BANK_2), checked.lemmaCard("bank").target)
        assertTrue(checked.lexeme("bank", "ruwiktionary/1.noun.2").exists)
        assertFalse(checked.lexeme("bank", "ruwiktionary/1.noun.1").exists)
    }

    // ===== Проверить по тексту (AWP-49) =====

    private val withRun = Fx.form(existing = listOf(Fx.runExisting))

    private fun savedRun() = reducer.step(reducer.windowOf("run", form = withRun), Msg.OpenSavedCard("run"))

    @Test
    fun `AWP-49_1 AWP-57_2 check against the text asks the lemma of the word with the context of the photo, saved lexemes stay while loading`() {
        val result = reducer.testReduce(savedRun(), Msg.LoadLexemes("run"))

        val card = result.state().card("run")
        assertEquals(CardPhase.LOADING, card.phase)
        assertEquals(Fx.runExisting.lexemes, card.saved)
        assertEquals(CardTargetUi.Existing(Fx.RUN_WORD), card.lemma.target)
        result.assertEffects(setOf<Effect>(lookupOf("run")))
    }

    @Test
    fun `AWP-49_4 AWP-47_6 the answer to the check marks the saved value and leaves the new one to add`() {
        val loading = reducer.step(savedRun(), Msg.LoadLexemes("run"))

        val answered = reducer.step(loading, Fx.lexemes("run", Fx.run))

        val card = answered.card("run")
        assertEquals(CardPhase.CANDIDATES, card.phase)
        assertEquals(CardTargetUi.Existing(Fx.RUN_WORD), card.lemma.target)
        assertEquals(Fx.runExisting.lexemes, card.saved)
        assertTrue(answered.lexeme("run", "ruwiktionary/1.verb.2").exists)
        assertFalse(answered.lexeme("run", "ruwiktionary/1.verb.1").exists)
        assertTrue(card.canAdd)
    }

    @Test
    fun `AWP-49_5 AWP-47_7 not found after the check leaves the saved lexemes in the card and no add`() {
        val loading = reducer.step(savedRun(), Msg.LoadLexemes("run"))

        val notFound = reducer.step(loading, Msg.LexemesNotFound("run", R.string.scan_not_found))

        val card = notFound.card("run")
        assertEquals(CardPhase.NOT_FOUND, card.phase)
        assertEquals(R.string.scan_not_found, card.errorRes)
        assertFalse(card.hasRetryButton)
        assertEquals(Fx.runExisting.lexemes, card.saved)
        assertFalse(card.canAdd)
    }

    // ===== «Добавить» (AWP-47, AWP-50.12) =====

    @Test
    fun `AWP-47_1 a new word with candidates can be added`() {
        assertTrue(reducer.withCard("ran", Fx.ran).card("ran").canAdd)
    }

    @Test
    fun `AWP-47_2 AWP-15_3 all lexemes deleted leave the card empty with add off`() {
        val loaded = reducer.withCard("ran", Fx.ran)

        val emptied = reducer.steps(
            loaded,
            Msg.DeleteLexeme("run", "ruwiktionary/1.verb.1"),
            Msg.DeleteLexeme("run", "ruwiktionary/1.verb.2"),
        )

        assertTrue(emptied.lemmaCard("ran").lexemes.isEmpty())
        assertFalse(emptied.card("ran").canAdd)
        assertEquals(CardPhase.CANDIDATES, emptied.card("ran").phase)
    }

    @Test
    fun `AWP-50_12 all candidates already in the dictionary give nothing to add and no pending word`() {
        val run = Fx.existing(Fx.RUN_WORD, "run", listOf("бежать, бегать", "управлять"))
        val form = Fx.form(existing = listOf(run))

        val state = reducer.withCard("ran", Fx.ran, form = form)

        assertTrue(state.lemmaCard("ran").lexemes.all { it.exists })
        assertFalse(state.card("ran").canAdd)
        assertEquals(0, state.lists.countPending())
    }

    // ===== Удаление слова (AWP-48) =====

    @Test
    fun `AWP-48_1 delete removes the word from its list and leaves the others`() {
        val lists = Fx.lists(lexical = listOf("nube"), unparsed = listOf("sh"))
        val window = reducer.window(found = Fx.found(lists = lists))

        val result = reducer.testReduce(window, Msg.DeleteWord("sh"))

        assertTrue(result.state().lists.unparsed.isEmpty())
        assertEquals(listOf("nube"), result.state().lists.lexical.texts())
        result.assertNoEffects()
    }

    @Test
    fun `AWP-48_4 a deleted loading word does not come back with the late answer and is not counted at closing`() {
        val loading = reducer.step(reducer.windowOf("sibling", "link"), Msg.LoadLexemes("sibling"))
        val deleted = reducer.step(loading, Msg.DeleteWord("sibling"))

        val late = reducer.testReduce(deleted, Fx.lexemes("sibling", Fx.ran))
        val closing = reducer.testReduce(late.state(), Msg.RequestClose)

        assertNull(deleted.lists.wordOf("sibling"))
        assertEquals(deleted, late.state())
        late.assertNoEffects()
        assertEquals(0, late.state().lists.countPending())
        closing.assertEffects(setOf<Effect>(NavigationEffect.Back))
    }

    // ===== «Не найдено», ошибка и отказ (AWP-55, AWP-61) =====

    private fun loading(text: String = "accorc") =
        reducer.step(reducer.windowOf(text), Msg.LoadLexemes(text))

    @Test
    fun `AWP-55_1 AWP-55_7 not found and no meanings show the text under the word without retry`() {
        val notFound = reducer.testReduce(loading(), Msg.LexemesNotFound("accorc", R.string.scan_not_found))
        val noLexemes = reducer.step(loading(), Msg.LexemesNotFound("accorc", R.string.scan_no_lexemes))

        val card = notFound.state().card("accorc")
        assertEquals(CardPhase.NOT_FOUND, card.phase)
        assertEquals(R.string.scan_not_found, card.errorRes)
        assertFalse(card.hasRetryButton)
        assertFalse(card.canAdd)
        assertEquals(R.string.scan_no_lexemes, noLexemes.card("accorc").errorRes)
        notFound.assertNoEffects()
    }

    @Test
    fun `AWP-61_1 AWP-61_2 AWP-61_5 unavailable shows the reason with retry`() {
        val failure = Msg.LexemesUnavailable("accorc", R.string.photo_words_error_server)

        val result = reducer.testReduce(loading(), failure)

        val card = result.state().card("accorc")
        assertEquals(CardPhase.ERROR, card.phase)
        assertEquals(R.string.photo_words_error_server, card.errorRes)
        assertTrue(card.hasRetryButton)
        result.assertNoEffects()
    }

    @Test
    fun `AWP-61_3 AWP-61_4 AWP-61_6 AWP-61_8 a denial shows the reason without retry`() {
        val denial = Msg.LexemesDenied("accorc", R.string.photo_words_error_rate_limited)

        val result = reducer.testReduce(loading(), denial)

        val card = result.state().card("accorc")
        assertEquals(CardPhase.ERROR, card.phase)
        assertEquals(R.string.photo_words_error_rate_limited, card.errorRes)
        assertFalse(card.hasRetryButton)
        result.assertNoEffects()
    }

    @Test
    fun `AWP-61_7 retry on the error asks again and shows the loading sign`() {
        val failed = reducer.step(loading(), Msg.LexemesUnavailable("accorc", R.string.scan_server_unavailable))

        val retried = reducer.testReduce(failed, Msg.LoadLexemes("accorc"))

        val card = retried.state().card("accorc")
        assertEquals(CardPhase.LOADING, card.phase)
        assertNull(card.errorRes)
        assertFalse(card.hasRetryButton)
        retried.assertEffects(setOf<Effect>(lookupOf("accorc")))
    }

    // ===== Прочее =====

    @Test
    fun `messages about a word that is not in the lists change nothing`() {
        val window = reducer.windowOf("ran")
        val strangers = listOf(
            Msg.LoadLexemes("qwerty"),
            Msg.OpenSavedCard("qwerty"),
            Msg.ToggleCard("qwerty"),
            Msg.DeleteWord("qwerty"),
            Msg.ToggleComponentMatches("qwerty"),
            Msg.AddWord("qwerty"),
        )

        strangers.forEach { message ->
            val result = reducer.testReduce(window, message)

            assertEquals(message.toString(), window, result.state())
            result.assertNoEffects()
        }
    }
}
