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

    /** Окно с одним смысловым словом и его плашками нейронки: цепочка атомов, как у редьюсера. */
    private fun loaded(
        text: String = "run",
        word: LookupWordUi = Fx.run,
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
    fun `openSavedCard builds the saved plates per lemma with the text and is a no-op without the word or the form`() {
        val form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))
        val window = window(found = Fx.found(lists = Fx.lists(lexical = listOf("bank"))), form = form)

        val (opened, effects) = window.openSavedCard("bank")

        val card = opened.card("bank")
        assertEquals(CardPhase.SAVED, card.phase)
        assertEquals(listOf(Fx.savedWord(Fx.bank1), Fx.savedWord(Fx.bank2)), card.saved)
        assertEquals(2, card.lemma.existingCount)
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
    fun `applyLexemes without the word is a no-op, with it builds the plates under the text of the word`() {
        val window = window(found = Fx.found(lists = Fx.lists(lexical = listOf("ran"))))
        val loading = window.startLookup("ran").state()

        val (applied, effects) = loading.applyLexemes(Fx.lexemes("ran", Fx.ran))

        assertEquals(CardPhase.CANDIDATES, applied.card("ran").phase)
        assertEquals("ran", applied.card("ran").lemma.lemma)
        assertEquals(2, applied.card("ran").lemma.lexemes.size)
        assertTrue(applied.card("ran").lemma.lexemes.all { it.target == CardTargetUi.NewWord })
        assertTrue(effects.isEmpty())
        assertNoOp(loading, loading.applyLexemes(Fx.lexemes("sky", Fx.ran)))
    }

    @Test
    fun `refreshCardValues and refreshCardMarks set the activity and the marks, no-ops without a card`() {
        val form = Fx.form(existing = listOf(Fx.runExisting))
        val loaded = loaded(form = form)

        assertTrue(loaded.lexeme("run", "ruwiktionary/1.verb.2").exists)
        assertFalse(loaded.lexeme("run", "ruwiktionary/1.verb.2").canWrite)
        assertFalse(loaded.lexeme("run", "ruwiktionary/1.verb.1").exists)
        assertTrue(loaded.lexeme("run", "ruwiktionary/1.verb.1").canWrite)
        val window = window(found = Fx.found(lists = Fx.lists(lexical = listOf("run"))))
        assertNoOp(window, window.refreshCardValues("run"))
        assertNoOp(window, window.refreshCardMarks("run"))
    }

    @Test
    fun `card messages keep the lemma and the saved plates and are no-ops without a card`() {
        val form = Fx.form(existing = listOf(Fx.runExisting))
        val saved = window(found = Fx.found(lists = Fx.lists(lexical = listOf("run"))), form = form)
            .openSavedCard("run").state()
            .startLookup("run").state()

        val notFound = saved.showCardNotFound("run", R.string.scan_not_found).state().card("run")
        val error = saved.showCardError("run", R.string.photo_words_error_no_access).state().card("run")
        val retryable = saved.showCardRetryableError("run", R.string.scan_server_unavailable).state().card("run")

        assertEquals(CardPhase.NOT_FOUND, notFound.phase)
        assertEquals(R.string.scan_not_found, notFound.errorRes)
        assertEquals(listOf(Fx.savedWord(Fx.runExisting)), notFound.saved)
        assertEquals("run", notFound.lemma.lemma)
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
    fun `removeWord drops the rows and is a no-op without the word`() {
        val window = window(found = Fx.found(lists = Fx.lists(lexical = listOf("nube"), unparsed = listOf("sh"))))

        val (removed, effects) = window.removeWord("sh")

        assertTrue(removed.lists.unparsed.isEmpty())
        assertTrue(effects.isEmpty())
        assertNoOp(window, window.removeWord("sky"))
    }

    @Test
    fun `dismissLexeme drops the plate, removeEmptyWord drops the word only when no plate is left and there is an answer`() {
        val loaded = loaded()

        val one = loaded.dismissLexeme("run", "ruwiktionary/1.verb.1").state()
        val none = one.dismissLexeme("run", "ruwiktionary/1.verb.2").state()

        assertEquals(listOf("ruwiktionary/1.verb.2"), one.lemmaCard("run").lexemes.map { it.key })
        assertNoOp(one, one.removeEmptyWord("run"))
        assertNull(none.removeEmptyWord("run").state().lists.wordOf("run"))
        val saved = window(found = Fx.found(lists = Fx.lists(lexical = listOf("run"))), form = Fx.form(existing = listOf(Fx.run1)))
            .openSavedCard("run").state()
        assertNoOp(saved, saved.removeEmptyWord("run"))
        assertNoOp(loaded, loaded.removeEmptyWord("sky"))
    }

    // ===== Правки плашки =====

    @Test
    fun `lemma steps are no-ops when the word has no card or the transition changes nothing`() {
        val loaded = loaded()

        assertNoOp(loaded, loaded.dismissLexeme("sky", "ruwiktionary/1.verb.1"))
        assertNoOp(loaded, loaded.dismissLexeme("run", "ruwiktionary/9.verb.9"))
        assertNoOp(loaded, loaded.resetText(Fx.ref("run", "ruwiktionary/9.verb.9", Fx.TRANSLATION)))
        assertNoOp(loaded, loaded.setLexemeTarget("run", "ruwiktionary/9.verb.9", CardTargetUi.NewWord))
        assertNoOp(loaded, loaded.refreshLemmaMarks("sky"))
        assertNoOp(loaded, loaded.refreshCardSaved("sky"))
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

    // ===== Запись плашки и перечитка =====

    private val runKey = "ruwiktionary/1.verb.1"
    private val manageKey = "ruwiktionary/1.verb.2"
    private val bankKey = "ruwiktionary/1.noun.1"

    @Test
    fun `startAddingLexeme sends the batch of the plate by its target and is a no-op without a plate or a plate not to write`() {
        val loaded = loaded()

        val (adding, effects) = loaded.startAddingLexeme("run", runKey)
        val transfer = loaded.setLexemeTarget("run", runKey, CardTargetUi.Existing(Fx.RUN_WORD)).state()
            .startAddingLexeme("run", runKey).effects()

        assertTrue(adding.isAdding)
        assertTrue(adding.card("run").isAdding)
        assertTrue(adding.lexeme("run", runKey).isAdding)
        assertFalse(adding.lexeme("run", manageKey).isAdding)
        assertEquals("run", (effects.single() as DatasourceEffect.CreateWord).word)
        assertTrue(transfer.single() is DatasourceEffect.TransferLexeme)
        assertNoOp(loaded, loaded.startAddingLexeme("sky", runKey))
        assertNoOp(loaded, loaded.startAddingLexeme("run", "ruwiktionary/9.verb.9"))
        val existing = loaded(form = Fx.form(existing = listOf(Fx.runExisting)))
        assertNoOp(existing, existing.startAddingLexeme("run", manageKey))
    }

    @Test
    fun `finishAdding drops the plate and clears adding, retarget and reload follow, removeEmptyWord waits for the last plate`() {
        val adding = loaded().startAddingLexeme("run", runKey).state()

        val (finished, effects) = adding.finishAdding("run", runKey)
        val retargeted = finished.retargetNewWordLexemes("run", Fx.RUN_CREATED).state()
        val last = finished.finishAdding("run", manageKey).state()

        assertEquals(listOf(manageKey), finished.lemmaCard("run").lexemes.map { it.key })
        assertFalse(finished.isAdding)
        assertFalse(finished.card("run").isAdding)
        assertTrue(effects.isEmpty())
        assertNoOp(finished, finished.finishAdding("run", runKey))
        assertNoOp(finished, finished.removeEmptyWord("run"))
        assertEquals(CardTargetUi.Existing(Fx.RUN_CREATED), retargeted.lexeme("run", manageKey).target)
        assertNoOp(retargeted, retargeted.retargetNewWordLexemes("run", Fx.RUN_CREATED))
        assertNull(last.removeEmptyWord("run").state().lists.wordOf("run"))
        assertEquals(setOf<Effect>(DatasourceEffect.ReloadLemmas("run", Fx.DICT)), finished.startLemmasReload("run").effects())
        assertNoOp(last.removeEmptyWord("run").state(), last.removeEmptyWord("run").state().startLemmasReload("run"))
        val noDictionary = finished.copy(dictionaryId = null)
        assertNoOp(noDictionary, noDictionary.startLemmasReload("run"))
    }

    @Test
    fun `applyReloadedLemmas replaces the lemmas with the text, refreshes the saved plates and is a no-op without the form`() {
        val loaded = loaded(form = Fx.form(existing = listOf(Fx.bank1)))

        val (reloaded, effects) = loaded.applyReloadedLemmas("run", listOf(Fx.runCreated))

        assertEquals(listOf(Fx.BANK_1, Fx.RUN_CREATED), requireNotNull(reloaded.form).existingLemmas.map { it.wordId })
        assertEquals(listOf(Fx.savedWord(Fx.runCreated)), reloaded.card("run").saved)
        assertEquals(1, reloaded.lemmaCard("run").existingCount)
        assertTrue(effects.isEmpty())
        val noForm = loaded.copy(form = null)
        assertNoOp(noForm, noForm.applyReloadedLemmas("run", listOf(Fx.runCreated)))
    }

    @Test
    fun `showAddFailure and clearAddFailure work per plate, clear is a no-op without a failure`() {
        val adding = loaded().startAddingLexeme("run", runKey).state()

        val failed = adding.showAddFailure("run", runKey, AddFailureUi.Failed).state()
        val cleared = failed.clearAddFailure("run", runKey).state()

        assertEquals(AddFailureUi.Failed, failed.lexeme("run", runKey).addFailure)
        assertFalse(failed.lexeme("run", runKey).isAdding)
        assertFalse(failed.card("run").isAdding)
        assertFalse(failed.isAdding)
        assertNull(cleared.lexeme("run", runKey).addFailure)
        assertNoOp(cleared, cleared.clearAddFailure("run", runKey))
        assertNoOp(adding, adding.showAddFailure("sky", runKey, AddFailureUi.Failed))
        assertNoOp(adding, adding.showAddFailure("run", "ruwiktionary/9.verb.9", AddFailureUi.Failed))
    }

    @Test
    fun `resetDeadGroup, resetDeadTarget and dropComponent clean the form and every plate`() {
        val form = Fx.form(existing = listOf(Fx.bank1))
        val loaded = loaded(text = "bank", word = Fx.bank, form = form)
            .setCommonGroup(Fx.POTTER).state()
            .setLexemeGroup("bank", bankKey, CardOwnGroupUi.Group(Fx.FINANCE)).state()
            .setLexemeTarget("bank", bankKey, CardTargetUi.Existing(Fx.BANK_1)).state()

        val group = loaded.resetDeadGroup(Fx.FINANCE).state()
        val target = loaded.resetDeadTarget(Fx.BANK_1).state().refreshAllCards().state()
        val component = loaded.dropComponent(Fx.PART_OF_SPEECH).state()

        assertEquals(CardOwnGroupUi.AsCommon, group.lexeme("bank", bankKey).ownGroup)
        assertEquals(Fx.POTTER, group.commonGroupId)
        assertEquals(listOf(Fx.POTTER), requireNotNull(group.form).groups.map { it.id })
        assertEquals(CardTargetUi.NewWord, target.lexeme("bank", bankKey).target)
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
