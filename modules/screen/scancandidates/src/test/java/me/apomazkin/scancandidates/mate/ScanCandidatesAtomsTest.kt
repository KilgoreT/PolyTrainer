package me.apomazkin.scancandidates.mate

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.NavigationEffect
import io.github.kilgoret.mate.effects
import io.github.kilgoret.mate.state
import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.candidates.CandidatesSheetUi
import me.apomazkin.candidates.CardOwnGroupUi
import me.apomazkin.candidates.CardTargetUi
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.LookupWordUi
import me.apomazkin.core_resources.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Слой атомов: каждый атом — один шаг, состояние и эффекты. Тест наследует
 * [ScanCandidatesAtoms] с пустым логгером, как редьюсер. Атом без своих данных
 * — no-op: состояние то же, эффектов нет.
 */
class ScanCandidatesAtomsTest : ScanCandidatesAtoms(NoopLogger) {

    /** Окно после ответа и формы: цепочка атомов, как у редьюсера. */
    private fun window(
        found: Msg.LemmasFound = Fx.found(),
        form: DictionaryFormUi = Fx.form(),
    ): ScanCandidatesState = fresh()
        .startAnalyzing(Fx.tokens).state()
        .startFormLoading(found).state()
        .applyForm(form).state()
        .markInDictionary().state()

    /** Окно с одним смысловым словом и его загруженной карточкой: цепочка атомов, как у редьюсера. */
    private fun loaded(
        text: String = "ran",
        word: LookupWordUi = Fx.ran,
        form: DictionaryFormUi = Fx.form(),
    ): ScanCandidatesState = window(found = Fx.found(lists = Fx.lists(lexical = listOf(text))), form = form)
        .startLookup(text).state()
        .applyLexemes(Fx.lexemes(text, word)).state()
        .refreshCardValues(text).state()
        .refreshCardMarks(text).state()

    private fun assertNoOp(
        before: ScanCandidatesState,
        result: ScanCandidatesResult,
    ) {
        assertEquals(before, result.state())
        assertTrue(result.effects().isEmpty())
    }

    // ===== Запуск =====

    @Test
    fun `startAnalyzing keeps the tokens, shows the analyzing phase, clears the error, asks`() {
        val failed = fresh().showRetryableError(R.string.photo_words_error_server).state()

        val (analyzing, effects) = failed.startAnalyzing(Fx.tokens)

        assertEquals(ScanPhase.ANALYZING, analyzing.phase)
        assertEquals(Fx.tokens, analyzing.tokens)
        assertNull(analyzing.errorRes)
        assertFalse(analyzing.hasRetryButton)
        assertEquals(setOf<Effect>(DatasourceEffect.ExtractLemmas(Fx.tokens)), effects)
    }

    @Test
    fun `showNoWords shows the no words phase without effects`() {
        val (state, effects) = fresh().showNoWords()

        assertEquals(ScanPhase.NO_WORDS, state.phase)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `showError has no retry button and showRetryableError has one`() {
        val plain = fresh().showError(R.string.photo_words_error_no_access)
        val retryable = fresh().showRetryableError(R.string.scan_analysis_unavailable)

        assertEquals(ScanPhase.ERROR, plain.state().phase)
        assertEquals(R.string.photo_words_error_no_access, plain.state().errorRes)
        assertFalse(plain.state().hasRetryButton)
        assertEquals(ScanPhase.ERROR, retryable.state().phase)
        assertEquals(R.string.scan_analysis_unavailable, retryable.state().errorRes)
        assertTrue(retryable.state().hasRetryButton)
        assertTrue(plain.effects().isEmpty() && retryable.effects().isEmpty())
    }

    @Test
    fun `startFormLoading keeps the answer and asks the form with the texts of all lists once each`() {
        val found = Fx.found(
            lists = Fx.lists(
                lexical = listOf("have", "study"),
                function = listOf("have", "a", "a"),
            ),
        )

        val (loading, effects) = fresh().startFormLoading(found)

        assertEquals(ScanPhase.FORM_LOADING, loading.phase)
        assertEquals(Fx.DICT, loading.dictionaryId)
        assertEquals(Fx.TEXTBOOK_TEXT, loading.text)
        assertEquals(Fx.TEXTBOOK_TRANSLATION, loading.translation)
        assertEquals(found.lists, loading.lists)
        val request = DatasourceEffect.LoadForm(Fx.DICT, listOf("have", "study", "a"))
        assertEquals(setOf<Effect>(request), effects)
    }

    // ===== Окно =====

    @Test
    fun `applyForm keeps the form, starts with the group all, folds the translation and shows the window`() {
        val loading = fresh()
            .startFormLoading(Fx.found()).state()
            .setCommonGroup(Fx.POTTER).state()
            .toggleTranslation().state()

        val (state, effects) = loading.applyForm(Fx.form())

        assertEquals(ScanPhase.WINDOW, state.phase)
        assertEquals(Fx.form(), state.form)
        assertNull(state.commonGroupId)
        assertFalse(state.isTranslationExpanded)
        assertEquals(Fx.textbookLists(), state.lists)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `markInDictionary marks the words whose text is a lemma of the dictionary`() {
        val loaded = fresh()
            .startFormLoading(Fx.found()).state()
            .applyForm(Fx.form(existing = listOf(Fx.study1, Fx.existing(7L, "Kramer")))).state()

        val (state, effects) = loaded.markInDictionary()

        assertEquals(listOf("study"), state.lists.lexical.marked())
        assertEquals(listOf("Kramer"), state.lists.proper.marked())
        assertTrue(state.lists.function.marked().isEmpty())
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `markInDictionary without a form is a no-op`() {
        val loading = fresh().startFormLoading(Fx.found()).state()

        assertNoOp(loading, loading.markInDictionary())
    }

    @Test
    fun `toggleTranslation expands and folds the block without effects`() {
        val window = window()

        val expanded = window.toggleTranslation()
        val folded = expanded.state().toggleTranslation()

        assertTrue(expanded.state().isTranslationExpanded)
        assertFalse(folded.state().isTranslationExpanded)
        assertTrue(expanded.effects().isEmpty() && folded.effects().isEmpty())
    }

    // ===== Карточка под словом =====

    @Test
    fun `openSavedCard builds the saved card of the newest lemma and is a no-op without the word or the form`() {
        val form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))
        val window = window(found = Fx.found(lists = Fx.lists(lexical = listOf("bank"))), form = form)

        val (opened, effects) = window.openSavedCard("bank")

        val card = opened.card("bank")
        assertEquals(CardPhase.SAVED, card.phase)
        assertEquals(CardTargetUi.Existing(Fx.BANK_1), card.lemma.target)
        assertEquals(Fx.bank1.lexemes, card.saved)
        assertTrue(opened.word("bank").isExpanded)
        assertTrue(effects.isEmpty())
        assertNoOp(window, window.openSavedCard("sky"))
        val noForm = window.copy(form = null)
        assertNoOp(noForm, noForm.openSavedCard("bank"))
    }

    @Test
    fun `startLookup needs the form, the dictionary and the word`() {
        val window = window(found = Fx.found(lists = Fx.lists(lexical = listOf("ran"))))

        val (loading, effects) = window.startLookup("ran")

        assertEquals(CardPhase.LOADING, loading.card("ran").phase)
        assertEquals("ran", loading.card("ran").lemma.lemma)
        assertEquals(1, effects.size)
        assertNoOp(window, window.startLookup("sky"))
        val noDictionary = window.copy(dictionaryId = null)
        assertNoOp(noDictionary, noDictionary.startLookup("ran"))
    }

    @Test
    fun `applyLexemes without the word is a no-op and keeps the form, with it builds the candidates card`() {
        val window = window(found = Fx.found(lists = Fx.lists(lexical = listOf("ran"))))
        val loading = window.startLookup("ran").state()

        val (applied, effects) = loading.applyLexemes(Fx.lexemes("ran", Fx.ran, existing = listOf(Fx.childExisting)))

        assertEquals(CardPhase.CANDIDATES, applied.card("ran").phase)
        assertEquals("run", applied.card("ran").lemma.lemma)
        assertEquals(listOf(Fx.CHILD_WORD), requireNotNull(applied.form).existingLemmas.map { it.wordId })
        assertTrue(effects.isEmpty())
        assertNoOp(loading, loading.applyLexemes(Fx.lexemes("sky", Fx.ran)))
    }

    @Test
    fun `refreshCardValues and refreshCardMarks set the activity, the marks and add, no-ops without a card`() {
        val form = Fx.form(existing = listOf(Fx.runExisting))
        val loaded = loaded(form = form)

        assertTrue(loaded.lexeme("ran", "ruwiktionary/1.verb.2").exists)
        assertFalse(loaded.lexeme("ran", "ruwiktionary/1.verb.1").exists)
        assertTrue(loaded.card("ran").canAdd)
        val window = window(found = Fx.found(lists = Fx.lists(lexical = listOf("ran"))))
        assertNoOp(window, window.refreshCardValues("ran"))
        assertNoOp(window, window.refreshCardMarks("ran"))
    }

    @Test
    fun `card messages keep the lemma and the saved rows and are no-ops without a card`() {
        val form = Fx.form(existing = listOf(Fx.runExisting))
        val saved = window(found = Fx.found(lists = Fx.lists(lexical = listOf("run"))), form = form)
            .openSavedCard("run").state()
            .startLookup("run").state()

        val notFound = saved.showCardNotFound("run", R.string.scan_not_found).state().card("run")
        val error = saved.showCardError("run", R.string.photo_words_error_no_access).state().card("run")
        val retryable = saved.showCardRetryableError("run", R.string.scan_server_unavailable).state().card("run")

        assertEquals(CardPhase.NOT_FOUND, notFound.phase)
        assertEquals(R.string.scan_not_found, notFound.errorRes)
        assertEquals(Fx.runExisting.lexemes, notFound.saved)
        assertEquals(CardTargetUi.Existing(Fx.RUN_WORD), notFound.lemma.target)
        assertEquals(CardPhase.ERROR, error.phase)
        assertFalse(error.hasRetryButton)
        assertTrue(retryable.hasRetryButton)
        val window = window(found = Fx.found(lists = Fx.lists(lexical = listOf("run"))), form = form)
        assertNoOp(window, window.showCardNotFound("run", R.string.scan_not_found))
        assertNoOp(window, window.showCardRetryableError("run", R.string.scan_server_unavailable))
    }

    @Test
    fun `toggleCard and toggleCardMatches flip their flags and are no-ops without a card`() {
        val loaded = loaded(text = "perro", word = Fx.perro)

        val folded = loaded.toggleCard("perro").state()
        val matches = loaded.toggleCardMatches("perro").state()

        assertFalse(folded.word("perro").isExpanded)
        assertTrue(matches.card("perro").isComponentMatchesExpanded)
        val window = window(found = Fx.found(lists = Fx.lists(lexical = listOf("perro"))))
        assertNoOp(window, window.toggleCard("perro"))
        assertNoOp(window, window.toggleCardMatches("perro"))
    }

    @Test
    fun `deleteWord drops the rows and is a no-op without the word`() {
        val window = window(found = Fx.found(lists = Fx.lists(lexical = listOf("nube"), unparsed = listOf("sh"))))

        val (deleted, effects) = window.deleteWord("sh")

        assertTrue(deleted.lists.unparsed.isEmpty())
        assertTrue(effects.isEmpty())
        assertNoOp(window, window.deleteWord("sky"))
    }

    // ===== Правки карточки =====

    @Test
    fun `lemma steps are no-ops when the lemma has no card or the transition changes nothing`() {
        val loaded = loaded()

        assertNoOp(loaded, loaded.deleteLexeme("sky", "ruwiktionary/1.verb.1"))
        assertNoOp(loaded, loaded.deleteLexeme("run", "ruwiktionary/9.verb.9"))
        assertNoOp(loaded, loaded.resetText(Fx.ref("run", "ruwiktionary/9.verb.9", Fx.TRANSLATION)))
        assertNoOp(loaded, loaded.refreshLemmaMarks("sky"))
        assertNoOp(loaded, loaded.refreshLemmaSaved("sky"))
    }

    @Test
    fun `openSheet fills the draft of the text edit from the cards and is a no-op without the text`() {
        val loaded = loaded()
        val ref = Fx.ref("run", "ruwiktionary/1.verb.1", Fx.TRANSLATION)
        val edit = CandidatesSheetUi.TextEdit(ref, draft = "", canReset = true)

        val opened = loaded.openSheet(edit).state()

        assertEquals(edit.copy(draft = "бежать, бегать", canReset = false), opened.sheet)
        val missing = CandidatesSheetUi.TextEdit(Fx.ref("sky", "x", Fx.TRANSLATION), draft = "", canReset = false)
        assertNoOp(loaded, loaded.openSheet(missing))
        assertNoOp(loaded, loaded.closeSheet())
    }

    // ===== Запись слова =====

    @Test
    fun `startAddingWord sends the batch of the word and is a no-op without a card or without lexemes to write`() {
        val loaded = loaded()

        val (adding, effects) = loaded.startAddingWord("ran")

        assertTrue(adding.isAdding)
        assertTrue(adding.card("ran").isAdding)
        assertEquals("ran", (effects.single() as DatasourceEffect.AddWord).word)
        assertNoOp(loaded, loaded.startAddingWord("sky"))
        val emptied = loaded
            .deleteLexeme("run", "ruwiktionary/1.verb.1").state()
            .deleteLexeme("run", "ruwiktionary/1.verb.2").state()
        assertNoOp(emptied, emptied.startAddingWord("ran"))
    }

    @Test
    fun `removeAddedWord drops the rows and clears adding when no other word is being written`() {
        val adding = loaded().startAddingWord("ran").state()

        val (removed, effects) = adding.removeAddedWord("ran")

        assertNull(removed.lists.wordOf("ran"))
        assertFalse(removed.isAdding)
        assertTrue(effects.isEmpty())
        assertNoOp(removed, removed.removeAddedWord("ran"))
    }

    @Test
    fun `showAddFailure and clearAddFailure work per word, clear is a no-op without a failure`() {
        val adding = loaded().startAddingWord("ran").state()

        val failed = adding.showAddFailure("ran", AddFailureUi.Failed).state()
        val cleared = failed.clearAddFailure("ran").state()

        assertEquals(AddFailureUi.Failed, failed.card("ran").addFailure)
        assertFalse(failed.card("ran").isAdding)
        assertFalse(failed.isAdding)
        assertNull(cleared.card("ran").addFailure)
        assertNoOp(cleared, cleared.clearAddFailure("ran"))
        assertNoOp(adding, adding.showAddFailure("sky", AddFailureUi.Failed))
    }

    @Test
    fun `resetDeadGroup, resetDeadTarget and dropComponent clean the form and every card`() {
        val form = Fx.form(existing = listOf(Fx.bank1))
        val loaded = loaded(text = "bank", word = Fx.bank, form = form)
            .setCommonGroup(Fx.POTTER).state()
            .setCardGroup("bank", CardOwnGroupUi.Group(Fx.FINANCE)).state()

        val group = loaded.resetDeadGroup(Fx.FINANCE).state()
        val target = loaded.resetDeadTarget(Fx.BANK_1).state().refreshAllCards().state()
        val component = loaded.dropComponent(Fx.PART_OF_SPEECH).state()

        assertEquals(CardOwnGroupUi.AsCommon, group.lemmaCard("bank").ownGroup)
        assertEquals(Fx.POTTER, group.commonGroupId)
        assertEquals(listOf(Fx.POTTER), requireNotNull(group.form).groups.map { it.id })
        assertEquals(CardTargetUi.NewWord, target.lemmaCard("bank").target)
        assertTrue(target.card("bank").saved.isEmpty())
        assertEquals(0, target.lemmaCard("bank").existingCount)
        assertTrue(requireNotNull(target.form).existingLemmas.isEmpty())
        assertTrue(requireNotNull(component.form).components.none { it.id == Fx.PART_OF_SPEECH })
        assertTrue(component.lemmaCard("bank").lexemes.single().values.none { it.componentId == Fx.PART_OF_SPEECH })
    }

    // ===== Общая группа и листы =====

    @Test
    fun `setCommonGroup keeps the group and null is all`() {
        val window = window()

        val chosen = window.setCommonGroup(Fx.POTTER).state()
        val all = chosen.setCommonGroup(null).state()

        assertEquals(Fx.POTTER, chosen.commonGroupId)
        assertNull(all.commonGroupId)
    }

    @Test
    fun `openSheet keeps the sheet and closeSheet drops it, closing without a sheet is a no-op`() {
        val window = window()

        val opened = window.openSheet(CandidatesSheetUi.CommonGroup).state()
        val closed = opened.closeSheet().state()

        assertEquals(CandidatesSheetUi.CommonGroup, opened.sheet)
        assertNull(closed.sheet)
        assertNoOp(window, window.closeSheet())
    }

    // ===== Закрытие =====

    @Test
    fun `askCloseConfirm counts the pending words and closeCloseConfirm clears the question`() {
        val loaded = loaded()

        val asking = loaded.askCloseConfirm().state()
        val stayed = asking.closeCloseConfirm().state()

        assertTrue(asking.isConfirmingClose)
        assertEquals(1, asking.pendingWords)
        assertFalse(stayed.isConfirmingClose)
        assertEquals(0, stayed.pendingWords)
    }

    @Test
    fun `close sends back and keeps the state`() {
        val window = window()

        val (state, effects) = window.close()

        assertEquals(window, state)
        assertEquals(setOf<Effect>(NavigationEffect.Back), effects)
    }
}
