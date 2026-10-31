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
 * Плашки под словом глазами редьюсера: раскрытие и сворачивание (AWP-42),
 * плашки нейронки нового слова (AWP-43, AWP-57), плашки словаря и проверка по
 * тексту (AWP-46, AWP-49, AWP-50), «Куда» (AWP-18), кнопки плашек (AWP-47),
 * «Не нужно» (AWP-64), «Убрать из списка» (AWP-48), «не найдено», ошибка и
 * отказ (AWP-55, AWP-61), разделы под плашками (AWP-30, AWP-31). Адреса строк
 * спеки — `AWP-NN_k`.
 */
class WordCardReducerTest {
    private val reducer = reducer()

    private val runKey = "ruwiktionary/1.verb.1"
    private val manageKey = "ruwiktionary/1.verb.2"

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
    fun `AWP-42_2 a second tap folds the plates of the word only, the other word stays open`() {
        val two = Fx.lists(lexical = listOf("run", "bank"))
        val run = reducer.withCard("run", Fx.run, lists = two)
        val both = reducer.step(run, Msg.LoadLexemes("bank"))

        val folded = reducer.testReduce(both, Msg.ToggleCard("run"))

        assertFalse(folded.state().word("run").isExpanded)
        assertTrue(folded.state().word("bank").isExpanded)
        assertEquals(both.card("run"), folded.state().card("run"))
        folded.assertNoEffects()
    }

    @Test
    fun `AWP-42_4 a folded word opens as it was, without the dismissed plate and without a new request`() {
        val loaded = reducer.withCard("run", Fx.run)
        val dismissed = reducer.step(loaded, Msg.DismissLexeme("run", manageKey))
        val folded = reducer.step(dismissed, Msg.ToggleCard("run"))

        val reopened = reducer.testReduce(folded, Msg.ToggleCard("run"))

        assertTrue(reopened.state().word("run").isExpanded)
        assertEquals(dismissed.card("run"), reopened.state().card("run"))
        assertEquals(listOf(runKey), reopened.state().lemmaCard("run").lexemes.map { it.key })
        reopened.assertNoEffects()
    }

    @Test
    fun `AWP-42_5 the answer lands under a word folded during loading and shows on the next tap`() {
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
    fun `AWP-42_6 a word folded with an error opens with the same error and retry, no request goes by itself`() {
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
    fun `AWP-42_7 a checked word in the dictionary folds and opens with the saved and the neural plates together`() {
        val checked = reducer.steps(savedRun(), Msg.LoadLexemes("run"), Fx.lexemes("run", Fx.run))
        val folded = reducer.step(checked, Msg.ToggleCard("run"))

        val reopened = reducer.step(folded, Msg.ToggleCard("run"))

        assertFalse(folded.word("run").isExpanded)
        assertEquals(checked.card("run"), reopened.card("run"))
        assertEquals(listOf(Fx.savedWord(Fx.runExisting)), reopened.card("run").saved)
        assertEquals(2, reopened.lemmaCard("run").lexemes.size)
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

    // ===== Плашки нейронки нового слова (AWP-43, AWP-18) =====

    @Test
    fun `AWP-43_1 AWP-43_4 AWP-57_3 AWP-18_3 corrian brings three plates in the selection order, only the first in the text, each for a new word`() {
        val state = reducer.withCard("corrían", Fx.corrian)

        val card = state.card("corrían")
        assertEquals(CardPhase.CANDIDATES, card.phase)
        assertEquals("corrían", card.lemma.lemma)
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
        assertTrue(card.lemma.lexemes.all { it.target == CardTargetUi.NewWord && it.canWrite })
        assertTrue(card.saved.isEmpty())
        assertNull(card.errorRes)
    }

    @Test
    fun `AWP-18_2 AWP-50_16 a new word without lemmas of the dictionary has nothing to choose and no marks`() {
        val state = reducer.withCard("friendship", Fx.run.copy(text = "friendship"))

        assertTrue(requireNotNull(state.form).existingLemmas.none { it.text == "friendship" })
        assertEquals(0, state.lemmaCard("friendship").existingCount)
        assertTrue(state.lemmaCard("friendship").lexemes.none { it.exists })
    }

    @Test
    fun `AWP-18_12 the target of one plate is chosen apart and the sheet closes`() {
        val loaded = reducer.withCard("run", Fx.run, form = Fx.form(existing = listOf(Fx.runExisting)))

        val chosen = reducer.testReduce(loaded, Msg.SetCardTarget("run", runKey, CardTargetUi.Existing(Fx.RUN_WORD)))

        assertEquals(CardTargetUi.Existing(Fx.RUN_WORD), chosen.state().lexeme("run", runKey).target)
        assertEquals(CardTargetUi.NewWord, chosen.state().lexeme("run", manageKey).target)
        assertNull(chosen.state().sheet)
        chosen.assertNoEffects()
    }

    @Test
    fun `AWP-30_15 AWP-30_12 AWP-31_6 the remarks and the matches of the answer are under the plates of the word`() {
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
    fun `AWP-31_4 the matches section is folded on open and toggles per word`() {
        val state = reducer.withCard("perro", Fx.perro)

        val expanded = reducer.testReduce(state, Msg.ToggleComponentMatches("perro"))
        val folded = reducer.step(expanded.state(), Msg.ToggleComponentMatches("perro"))

        assertFalse(state.card("perro").isComponentMatchesExpanded)
        assertTrue(expanded.state().card("perro").isComponentMatchesExpanded)
        assertFalse(folded.card("perro").isComponentMatchesExpanded)
        expanded.assertNoEffects()
    }

    // ===== Плашки словаря (AWP-46) =====

    @Test
    fun `AWP-46_1 AWP-46_3 a tap on a word in the dictionary opens a saved plate per lemma with the text, newest first, without a request`() {
        val form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))
        val window = reducer.windowOf("bank", form = form)

        val result = reducer.testReduce(window, Msg.OpenSavedCard("bank"))

        val card = result.state().card("bank")
        assertTrue(result.state().word("bank").isExpanded)
        assertEquals(CardPhase.SAVED, card.phase)
        assertEquals(listOf(Fx.savedWord(Fx.bank1), Fx.savedWord(Fx.bank2)), card.saved)
        assertEquals(
            listOf(listOf(listOf("Перевод: банк"), listOf("Перевод: банкомёт")), listOf(listOf("Перевод: берег"))),
            card.saved.map { saved -> saved.lexemes.map { it.shown(form) } },
        )
        assertEquals(2, card.lemma.existingCount)
        assertTrue(card.lemma.lexemes.isEmpty())
        result.assertNoEffects()
    }

    @Test
    fun `AWP-46_4 a word in the dictionary without lexemes opens a saved plate without values`() {
        val form = Fx.form(existing = listOf(Fx.run1))

        val opened = reducer.step(reducer.windowOf("run", form = form), Msg.OpenSavedCard("run"))

        val card = opened.card("run")
        assertEquals(CardPhase.SAVED, card.phase)
        assertEquals(listOf(Fx.savedWord(Fx.run1)), card.saved)
        assertTrue(card.saved.single().lexemes.isEmpty())
        assertTrue(card.lemma.lexemes.isEmpty())
    }

    // ===== Проверить по тексту (AWP-49, AWP-50) =====

    private val withRun = Fx.form(existing = listOf(Fx.runExisting))

    private fun savedRun() = reducer.step(reducer.windowOf("run", form = withRun), Msg.OpenSavedCard("run"))

    @Test
    fun `AWP-49_1 AWP-57_2 check against the text asks the lemma of the word with the context of the photo, saved plates stay while loading`() {
        val result = reducer.testReduce(savedRun(), Msg.LoadLexemes("run"))

        val card = result.state().card("run")
        assertEquals(CardPhase.LOADING, card.phase)
        assertEquals(listOf(Fx.savedWord(Fx.runExisting)), card.saved)
        result.assertEffects(setOf<Effect>(lookupOf("run")))
    }

    @Test
    fun `AWP-49_4 AWP-47_6 AWP-50_1 the answer to the check marks the saved value and leaves the new one to write`() {
        val loading = reducer.step(savedRun(), Msg.LoadLexemes("run"))

        val answered = reducer.step(loading, Fx.lexemes("run", Fx.run))

        val card = answered.card("run")
        assertEquals(CardPhase.CANDIDATES, card.phase)
        assertEquals(listOf(Fx.savedWord(Fx.runExisting)), card.saved)
        assertTrue(answered.lexeme("run", manageKey).exists)
        assertFalse(answered.lexeme("run", manageKey).canWrite)
        assertFalse(answered.lexeme("run", runKey).exists)
        assertTrue(answered.lexeme("run", runKey).canWrite)
        assertEquals(CardTargetUi.NewWord, answered.lexeme("run", runKey).target)
    }

    @Test
    fun `AWP-49_5 AWP-47_7 AWP-55_10 not found after the check leaves the saved plates and no plates to write`() {
        val loading = reducer.step(savedRun(), Msg.LoadLexemes("run"))

        val notFound = reducer.step(loading, Msg.LexemesNotFound("run", R.string.scan_not_found))

        val card = notFound.card("run")
        assertEquals(CardPhase.NOT_FOUND, card.phase)
        assertEquals(R.string.scan_not_found, card.errorRes)
        assertFalse(card.hasRetryButton)
        assertEquals(listOf(Fx.savedWord(Fx.runExisting)), card.saved)
        assertTrue(card.lemma.lexemes.isEmpty())
    }

    @Test
    fun `AWP-50_14 AWP-18_8 the mark follows every lemma with the text whatever target the plate has`() {
        val form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))
        val shore = Fx.lexeme(
            key = "ruwiktionary/1.noun.2",
            translations = listOf("берег"),
            partOfSpeechKey = "noun",
            matches = listOf(Fx.byField(Fx.TRANSLATION, "translations"), Fx.byOption(Fx.PART_OF_SPEECH, Fx.NOUN)),
        )
        val answer = Fx.word("bank", Fx.lemma("bank", listOf(Fx.bankNoun, shore)))
        val opened = reducer.step(reducer.windowOf("bank", form = form), Msg.OpenSavedCard("bank"))

        val checked = reducer.steps(opened, Msg.LoadLexemes("bank"), Fx.lexemes("bank", answer))
        val retargeted = reducer.step(checked, Msg.SetCardTarget("bank", "ruwiktionary/1.noun.1", CardTargetUi.Existing(Fx.BANK_1)))

        assertTrue(checked.lexeme("bank", "ruwiktionary/1.noun.2").exists)
        assertFalse(checked.lexeme("bank", "ruwiktionary/1.noun.1").exists)
        assertTrue(retargeted.lexeme("bank", "ruwiktionary/1.noun.2").exists)
        assertFalse(retargeted.lexeme("bank", "ruwiktionary/1.noun.1").exists)
    }

    @Test
    fun `AWP-50_12 all plates already in the dictionary give nothing to write and no pending word`() {
        val run = Fx.existing(Fx.RUN_WORD, "run", listOf("бежать, бегать", "управлять"))
        val form = Fx.form(existing = listOf(run))

        val state = reducer.withCard("run", Fx.run, form = form)

        assertTrue(state.lemmaCard("run").lexemes.all { it.exists && !it.canWrite })
        assertEquals(0, state.lists.countPending())
    }

    // ===== «Не нужно» (AWP-64, AWP-62) =====

    @Test
    fun `AWP-64_1 AWP-64_3 not needed drops the plate and leaves the others, a second press drops nothing more`() {
        val loaded = reducer.withCard("run", Fx.runThree)

        val first = reducer.testReduce(loaded, Msg.DismissLexeme("run", manageKey))
        val second = reducer.testReduce(first.state(), Msg.DismissLexeme("run", manageKey))

        assertEquals(listOf(runKey, "enwiktionary/1.verb.37"), first.state().lemmaCard("run").lexemes.map { it.key })
        assertEquals(first.state(), second.state())
        first.assertNoEffects()
        second.assertNoEffects()
    }

    @Test
    fun `AWP-64_4 AWP-62_5 not needed on the last plate drops the word from the list without a write`() {
        val loaded = reducer.withCard("tiempo", Fx.tiempo, lists = Fx.lists(lexical = listOf("tiempo", "cielo")))

        val result = reducer.testReduce(loaded, Msg.DismissLexeme("tiempo", "ruwiktionary/1.noun.3"))

        assertNull(result.state().lists.wordOf("tiempo"))
        assertEquals(listOf("cielo"), result.state().lists.lexical.texts())
        result.assertNoEffects()
    }

    @Test
    fun `AWP-64_5 AWP-62_6 a plate already in the dictionary goes by not needed too and takes the word with it when last`() {
        val checked = reducer.steps(savedRun(), Msg.LoadLexemes("run"), Fx.lexemes("run", Fx.run))
        val written = reducer.transferred(checked, "run", runKey)

        val dismissed = reducer.step(written, Msg.DismissLexeme("run", manageKey))

        assertTrue(written.lexeme("run", manageKey).exists)
        assertEquals(listOf(manageKey), written.lemmaCard("run").lexemes.map { it.key })
        assertNull(dismissed.lists.wordOf("run"))
    }

    // ===== «Убрать из списка» (AWP-48) =====

    @Test
    fun `AWP-48_1 remove drops the word from its list and leaves the others`() {
        val lists = Fx.lists(lexical = listOf("nube"), unparsed = listOf("sh"))
        val window = reducer.window(found = Fx.found(lists = lists))

        val result = reducer.testReduce(window, Msg.RemoveWord("sh"))

        assertTrue(result.state().lists.unparsed.isEmpty())
        assertEquals(listOf("nube"), result.state().lists.lexical.texts())
        result.assertNoEffects()
    }

    @Test
    fun `AWP-48_4 a removed loading word does not come back with the late answer and is not counted at closing`() {
        val loading = reducer.step(reducer.windowOf("sibling", "link"), Msg.LoadLexemes("sibling"))
        val removed = reducer.step(loading, Msg.RemoveWord("sibling"))

        val late = reducer.testReduce(removed, Fx.lexemes("sibling", Fx.run))
        val closing = reducer.testReduce(late.state(), Msg.RequestClose)

        assertNull(removed.lists.wordOf("sibling"))
        assertEquals(removed, late.state())
        late.assertNoEffects()
        assertEquals(0, late.state().lists.countPending())
        closing.assertEffects(setOf<Effect>(NavigationEffect.Back))
    }

    @Test
    fun `AWP-48_5 removing a word in the dictionary drops its row and leaves the dictionary as it was`() {
        val saved = savedRun()

        val result = reducer.testReduce(saved, Msg.RemoveWord("run"))

        assertNull(result.state().lists.wordOf("run"))
        assertEquals(withRun, result.state().form)
        result.assertNoEffects()
    }

    // ===== «Не найдено», ошибка и отказ (AWP-55, AWP-61) =====

    private fun loading(text: String = "accorc") =
        reducer.step(reducer.windowOf(text), Msg.LoadLexemes(text))

    @Test
    fun `AWP-55_1 AWP-55_7 not found and no meanings show the text under the word without retry and without plates`() {
        val notFound = reducer.testReduce(loading(), Msg.LexemesNotFound("accorc", R.string.scan_not_found))
        val noLexemes = reducer.step(loading(), Msg.LexemesNotFound("accorc", R.string.scan_no_lexemes))

        val card = notFound.state().card("accorc")
        assertEquals(CardPhase.NOT_FOUND, card.phase)
        assertEquals(R.string.scan_not_found, card.errorRes)
        assertFalse(card.hasRetryButton)
        assertTrue(card.lemma.lexemes.isEmpty())
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
        val window = reducer.windowOf("run")
        val strangers = listOf(
            Msg.LoadLexemes("qwerty"),
            Msg.OpenSavedCard("qwerty"),
            Msg.ToggleCard("qwerty"),
            Msg.RemoveWord("qwerty"),
            Msg.ToggleComponentMatches("qwerty"),
            Msg.AddLexeme("qwerty", runKey),
            Msg.DismissLexeme("qwerty", runKey),
            Msg.RetryAdd("qwerty", runKey),
            Msg.SetCardTarget("qwerty", runKey, CardTargetUi.NewWord),
        )

        strangers.forEach { message ->
            val result = reducer.testReduce(window, message)

            assertEquals(message.toString(), window, result.state())
            result.assertNoEffects()
        }
    }
}
