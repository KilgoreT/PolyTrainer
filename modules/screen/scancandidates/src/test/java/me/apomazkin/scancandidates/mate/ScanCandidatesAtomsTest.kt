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
import me.apomazkin.candidates.NotAddedReason
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
    private val runKey = "ruwiktionary/1.verb.1"
    private val llegarKey = "ruwiktionary/1.verb.1"
    private val translation = Fx.ref("run", runKey, Fx.TRANSLATION)

    /** Окно после ответа и формы: цепочка атомов, как у редьюсера. */
    private fun window(
        vararg words: LookupWordUi,
        form: DictionaryFormUi = Fx.form(),
    ): ScanCandidatesState = fresh()
        .startLooking(Fx.tokens).state()
        .startFormLoading(Msg.WordsFound(Fx.DICT, words.toList(), listOf(Fx.genderRecommendation)))
        .state()
        .applyForm(form).state()
        .applyRemarks().state()
        .applyComponentMatchLines().state()
        .refreshActiveValues().state()
        .refreshExistsMarks().state()
        .refreshCoreMarks().state()
        .refreshSummary().state()

    private fun assertNoOp(
        before: ScanCandidatesState,
        result: ScanCandidatesResult,
    ) {
        assertEquals(before, result.state())
        assertTrue(result.effects().isEmpty())
    }

    // ===== Запуск =====

    @Test
    fun `startLooking keeps the tokens, shows the looking phase, clears the error, asks`() {
        val failed = fresh().showRetryableError(R.string.photo_words_error_server).state()

        val (looking, effects) = failed.startLooking(Fx.tokens)

        assertEquals(ScanPhase.LOOKING, looking.phase)
        assertEquals(Fx.tokens, looking.tokens)
        assertNull(looking.errorRes)
        assertFalse(looking.hasRetryButton)
        assertEquals(setOf<Effect>(DatasourceEffect.LookupWords(Fx.tokens)), effects)
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
        val retryable = fresh().showRetryableError(R.string.scan_server_unavailable)

        assertEquals(ScanPhase.ERROR, plain.state().phase)
        assertEquals(R.string.photo_words_error_no_access, plain.state().errorRes)
        assertFalse(plain.state().hasRetryButton)
        assertEquals(ScanPhase.ERROR, retryable.state().phase)
        assertEquals(R.string.scan_server_unavailable, retryable.state().errorRes)
        assertTrue(retryable.state().hasRetryButton)
        assertTrue(plain.effects().isEmpty() && retryable.effects().isEmpty())
    }

    @Test
    fun `startFormLoading keeps the answer and asks the form with the lemma texts of the answer`() {
        val words = listOf(Fx.ran, Fx.running)
        val found = Msg.WordsFound(Fx.DICT, words, listOf(Fx.genderRecommendation))

        val (loading, effects) = fresh().startFormLoading(found)

        assertEquals(ScanPhase.FORM_LOADING, loading.phase)
        assertEquals(Fx.DICT, loading.dictionaryId)
        assertEquals(listOf("ran", "running"), loading.words.map { it.text })
        assertEquals(listOf(Fx.genderRecommendation), loading.recommendations)
        val request = DatasourceEffect.LoadForm(Fx.DICT, listOf("run", "running"))
        assertEquals(setOf<Effect>(request), effects)
    }

    // ===== Окно =====

    @Test
    fun `applyForm builds the cards, starts with the group all and shows the window`() {
        val loading = fresh()
            .startFormLoading(Msg.WordsFound(Fx.DICT, listOf(Fx.ran, Fx.accorc), emptyList()))
            .state()
            .setCommonGroup(Fx.POTTER).state()

        val (state, effects) = loading.applyForm(Fx.form())

        assertEquals(ScanPhase.WINDOW, state.phase)
        assertEquals(Fx.form(), state.form)
        assertEquals(listOf("run"), state.cards.map { it.lemma })
        assertEquals(listOf("accorc"), state.notAdded.map { it.text })
        assertEquals(NotAddedReason.NOT_FOUND, state.notAdded.single().reason)
        assertNull(state.commonGroupId)
        assertFalse(state.isNotAddedExpanded)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `applyForm expands the not added block when no card is built`() {
        val found = Msg.WordsFound(Fx.DICT, listOf(Fx.accorc), emptyList())
        val loading = fresh().startFormLoading(found).state()

        val state = loading.applyForm(Fx.form()).state()

        assertTrue(state.cards.isEmpty())
        assertTrue(state.isNotAddedExpanded)
    }

    @Test
    fun `the steps that need the form do nothing without it`() {
        val loading = fresh()
            .startFormLoading(Msg.WordsFound(Fx.DICT, listOf(Fx.ran), emptyList()))
            .state()

        assertNoOp(loading, loading.applyRemarks())
        assertNoOp(loading, loading.applyComponentMatchLines())
        assertNoOp(loading, loading.refreshActiveValues())
        assertNoOp(loading, loading.refreshExistsMarks())
        assertNoOp(loading, loading.refreshCoreMarks())
        assertNoOp(loading, loading.deleteValue(translation))
        assertNoOp(loading, loading.dropEmptyTexts(translation))
        val partOfSpeech = Fx.componentRef("run", runKey, Fx.PART_OF_SPEECH)
        assertNoOp(loading, loading.setOption(partOfSpeech, Fx.NOUN))
        assertNoOp(loading, loading.startAdding())
        assertNoOp(loading, loading.dropComponent(Fx.GLOSS))
    }

    @Test
    fun `applyRemarks and applyComponentMatchLines fill the sections from the answer`() {
        val found = Msg.WordsFound(Fx.DICT, listOf(Fx.zarpo), listOf(Fx.genderRecommendation))
        val loaded = fresh()
            .startFormLoading(found).state()
            .applyForm(Fx.form()).state()

        val remarks = loaded.applyRemarks().state()
        val lines = loaded.applyComponentMatchLines().state()

        assertEquals(2, remarks.remarks.size)
        assertTrue(lines.componentMatchLines.isNotEmpty())
        assertTrue(remarks.componentMatchLines.isEmpty())
        assertTrue(lines.remarks.isEmpty())
    }

    @Test
    fun `refreshActiveValues hides the dependent values and keeps them for later`() {
        val window = window(Fx.perro)
        val partOfSpeech = Fx.componentRef("perro", "ruwiktionary/1.noun.1", Fx.PART_OF_SPEECH)
        val verb = window.setOption(partOfSpeech, Fx.VERB).state()

        val refreshed = verb.refreshActiveValues().state()

        val before = window.lexeme("perro", "ruwiktionary/1.noun.1").shownComponents()
        val after = refreshed.lexeme("perro", "ruwiktionary/1.noun.1").shownComponents()
        assertTrue(Fx.GENDER in before)
        assertFalse(Fx.GENDER in after)
    }

    @Test
    fun `the refresh steps recount the exists mark, the core mark and the button`() {
        val window = window(Fx.ran, form = Fx.form(existing = listOf(Fx.runExisting)))

        assertTrue(window.lexeme("run", "ruwiktionary/1.verb.2").exists)
        assertEquals(1, window.summary.lexemeCount)
        assertEquals(1, window.summary.existingCount)
        assertTrue(window.summary.isEnabled)
    }

    @Test
    fun `refreshSummary needs no form`() {
        val state = fresh().refreshSummary().state()

        assertEquals(0, state.summary.lexemeCount)
        assertFalse(state.summary.isEnabled)
    }

    // ===== Действия =====

    @Test
    fun `deleteCard and deleteLexeme remove what they name and do nothing for the unknown`() {
        val window = window(Fx.ran, Fx.bank)

        val lexemeGone = window.deleteLexeme("run", "ruwiktionary/1.verb.2").state()
        val cardGone = window.deleteCard("bank").state()

        assertEquals(listOf(runKey), lexemeGone.card("run").lexemes.map { it.key })
        assertEquals(listOf("run"), cardGone.cards.map { it.lemma })
        assertNoOp(window, window.deleteLexeme("run", "no/such"))
        assertNoOp(window, window.deleteCard("nothing"))
        assertFalse(lexemeGone.isDirty)
    }

    @Test
    fun `deleteValue removes one value and does nothing for an unknown address`() {
        val window = window(Fx.tiempo)
        val first = Fx.ref("tiempo", "ruwiktionary/1.noun.3", Fx.EXAMPLE, index = 0)
        val tenth = Fx.ref("tiempo", "ruwiktionary/1.noun.3", Fx.EXAMPLE, index = 9)

        val result = window.deleteValue(first).state()

        assertEquals(
            Fx.tiempoExamples.drop(1),
            result.texts("tiempo", "ruwiktionary/1.noun.3", Fx.EXAMPLE),
        )
        assertNoOp(window, window.deleteValue(tenth))
    }

    @Test
    fun `applyTextEdit trims the text and does nothing for an unknown address`() {
        val window = window(Fx.ran)

        val result = window.applyTextEdit(translation, "  брать ").state()

        assertEquals(listOf("брать"), result.texts("run", runKey))
        assertNoOp(window, window.applyTextEdit(Fx.ref("run", "no/such", Fx.TRANSLATION), "x"))
    }

    @Test
    fun `deleteValue deletes the lexeme when the last core text goes`() {
        val window = window(Fx.ran)

        val result = window.deleteValue(translation).state()

        assertEquals(listOf("ruwiktionary/1.verb.2"), result.card("run").lexemes.map { it.key })
    }

    @Test
    fun `dropEmptyTexts removes an erased parameter text and does nothing without an empty one`() {
        val example = Fx.ref("tiempo", "ruwiktionary/1.noun.3", Fx.EXAMPLE, index = 1)
        val tiempo = window(Fx.tiempo)
        val erasedExample = tiempo.applyTextEdit(example, "").state()

        val dropped = erasedExample.dropEmptyTexts(example).state()

        assertEquals(
            listOf(Fx.tiempoExamples[0], Fx.tiempoExamples[2]),
            dropped.texts("tiempo", "ruwiktionary/1.noun.3", Fx.EXAMPLE),
        )
        assertNoOp(tiempo, tiempo.dropEmptyTexts(example))
    }

    @Test
    fun `dropEmptyTexts deletes the lexeme when the last core text is erased`() {
        val erasedCore = window(Fx.ran).applyTextEdit(translation, "").state()

        val result = erasedCore.dropEmptyTexts(translation).state()

        assertEquals(listOf(""), erasedCore.texts("run", runKey))
        assertEquals(listOf("ruwiktionary/1.verb.2"), result.card("run").lexemes.map { it.key })
    }

    @Test
    fun `dropEmptyTexts drops only the value when another core still has text`() {
        val key = "ruwiktionary/1.noun.1"
        val lexeme = Fx.lexeme(
            key = key,
            translations = listOf("собака"),
            gloss = "dog",
            matches = listOf(
                Fx.byField(Fx.TRANSLATION, "translations"),
                Fx.byField(Fx.DEFINITION, "gloss"),
            ),
        )
        val word = Fx.word("perro", Fx.lemma("perro", listOf(lexeme)))
        val form = Fx.form(components = listOf(Fx.translation, Fx.definition))
        val cores = window(word, form = form)
        val perroTranslation = Fx.ref("perro", key, Fx.TRANSLATION)
        val erased = cores.applyTextEdit(perroTranslation, "").state()

        val result = erased.dropEmptyTexts(perroTranslation).state()

        assertEquals(listOf("perro"), result.cards.map { it.lemma })
        val left = result.lexeme("perro", key).values.map { it.componentId }
        assertEquals(listOf(Fx.DEFINITION), left)
    }

    @Test
    fun `resetText brings the answer text back and does nothing for an unknown address`() {
        val edited = window(Fx.ran).applyTextEdit(translation, "брать").state()

        val reset = edited.resetText(translation).state()

        assertEquals(listOf("бежать, бегать"), reset.texts("run", runKey))
        assertNoOp(edited, edited.resetText(Fx.ref("run", "no/such", Fx.TRANSLATION)))
    }

    @Test
    fun `setOption replaces a single choice, toggles a multiple one and clears with null`() {
        val partOfSpeech = Fx.componentRef("run", runKey, Fx.PART_OF_SPEECH)
        val ran = window(Fx.ran)
        val style = Fx.componentRef("llegar", llegarKey, Fx.STYLE)
        val llegar = window(Fx.llegar, form = Fx.formWithStyle())

        val noun = ran.setOption(partOfSpeech, Fx.NOUN).state()
        val cleared = ran.setOption(partOfSpeech, null).state()
        val figurativeOff = llegar.setOption(style, Fx.FIGURATIVE).state()
        val figurativeOn = figurativeOff.setOption(style, Fx.FIGURATIVE).state()

        assertEquals(listOf(Fx.NOUN), noun.chosen("run", runKey, Fx.PART_OF_SPEECH))
        assertEquals(emptyList<Long>(), cleared.chosen("run", runKey, Fx.PART_OF_SPEECH))
        assertEquals(listOf(Fx.COLLOQUIAL), figurativeOff.chosen("llegar", llegarKey, Fx.STYLE))
        assertEquals(
            listOf(Fx.COLLOQUIAL, Fx.FIGURATIVE),
            figurativeOn.chosen("llegar", llegarKey, Fx.STYLE),
        )
        val missing = Fx.componentRef("run", "no/such", Fx.PART_OF_SPEECH)
        assertNoOp(ran, ran.setOption(missing, Fx.NOUN))
    }

    @Test
    fun `card target and own group are set and a missing card is ignored`() {
        val window = window(Fx.bank, form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2)))

        val targeted = window.setCardTarget("bank", CardTargetUi.NewWord).state()
        val grouped = targeted.setCardGroup("bank", CardOwnGroupUi.Group(Fx.FINANCE)).state()

        assertEquals(CardTargetUi.NewWord, targeted.card("bank").target)
        assertEquals(CardOwnGroupUi.Group(Fx.FINANCE), grouped.card("bank").ownGroup)
        assertNoOp(window, window.setCardTarget("nothing", CardTargetUi.NewWord))
        assertNoOp(window, window.setCardGroup("nothing", CardOwnGroupUi.All))
    }

    @Test
    fun `setCommonGroup sets a group and all`() {
        val window = window(Fx.ran)

        val group = window.setCommonGroup(Fx.POTTER).state()
        val all = group.setCommonGroup(null).state()

        assertEquals(Fx.POTTER, group.commonGroupId)
        assertNull(all.commonGroupId)
    }

    @Test
    fun `the section toggles flip their flags`() {
        val window = window(Fx.ran)

        assertTrue(window.toggleComponentMatchesExpanded().state().isComponentMatchesExpanded)
        assertTrue(window.toggleNotAddedExpanded().state().isNotAddedExpanded)
    }

    // ===== Листы =====

    @Test
    fun `openSheet of a text edit takes the current text and offers reset for an edited one`() {
        val window = window(Fx.ran)
        val edited = window.applyTextEdit(translation, "брать").state()
        val sheet = CandidatesSheetUi.TextEdit(translation, draft = "", canReset = false)

        val plain = window.openSheet(sheet).state()
        val afterEdit = edited.openSheet(sheet).state()

        assertEquals(CandidatesSheetUi.TextEdit(translation, "бежать, бегать", false), plain.sheet)
        assertEquals(CandidatesSheetUi.TextEdit(translation, "брать", true), afterEdit.sheet)
    }

    @Test
    fun `openSheet keeps other sheets as they are and ignores a text edit of a missing value`() {
        val window = window(Fx.ran)
        val missingRef = Fx.ref("run", "no/such", Fx.TRANSLATION)
        val missing = CandidatesSheetUi.TextEdit(missingRef, draft = "", canReset = false)

        assertEquals(
            CandidatesSheetUi.CardGroup("run"),
            window.openSheet(CandidatesSheetUi.CardGroup("run")).state().sheet,
        )
        assertNoOp(window, window.openSheet(missing))
    }

    @Test
    fun `closeSheet closes an open sheet and does nothing without one`() {
        val window = window(Fx.ran)
        val opened = window.openSheet(CandidatesSheetUi.CommonGroup).state()

        assertNull(opened.closeSheet().state().sheet)
        assertNoOp(window, window.closeSheet())
    }

    @Test
    fun `updateSheetDraft offers reset only for an input that differs from the answer`() {
        val opened = window(Fx.ran)
            .openSheet(CandidatesSheetUi.TextEdit(translation, "", false))
            .state()

        val different = opened.updateSheetDraft("брать").state()
        val same = different.updateSheetDraft(" бежать, бегать ").state()
        val erased = same.updateSheetDraft("").state()

        assertEquals(CandidatesSheetUi.TextEdit(translation, "брать", true), different.sheet)
        assertEquals(CandidatesSheetUi.TextEdit(translation, " бежать, бегать ", false), same.sheet)
        assertEquals(CandidatesSheetUi.TextEdit(translation, "", true), erased.sheet)
    }

    @Test
    fun `updateSheetDraft does nothing when the open sheet is not a text edit`() {
        val window = window(Fx.ran)
        val group = window.openSheet(CandidatesSheetUi.CommonGroup).state()

        assertNoOp(window, window.updateSheetDraft("x"))
        assertNoOp(group, group.updateSheetDraft("x"))
    }

    @Test
    fun `markDirty raises the flag`() {
        assertTrue(window(Fx.ran).markDirty().state().isDirty)
    }

    // ===== Добавление =====

    @Test
    fun `the add question opens and closes`() {
        val window = window(Fx.ran)

        val asking = window.askAddConfirm().state()

        assertTrue(asking.isConfirmingAdd)
        assertFalse(asking.closeAddConfirm().state().isConfirmingAdd)
    }

    @Test
    fun `startAdding makes the window busy and sends the batch of the window`() {
        val failed = window(Fx.ran, Fx.bank).showAddFailure(AddFailureUi.Failed).state()

        val (adding, effects) = failed.startAdding()

        assertTrue(adding.isAdding)
        assertNull(adding.addFailure)
        val batch = (effects.single() as DatasourceEffect.AddWords).batch
        assertEquals(listOf("run", "bank"), batch.words.map { it.text })
        assertEquals(Fx.DICT, batch.dictionaryId)
    }

    @Test
    fun `showAddFailure reopens the window with the reason and clearAddFailure takes it off`() {
        val adding = window(Fx.ran).startAdding().state()

        val failed = adding.showAddFailure(AddFailureUi.NoCoreValue).state()
        val cleared = failed.clearAddFailure().state()

        assertFalse(failed.isAdding)
        assertEquals(AddFailureUi.NoCoreValue, failed.addFailure)
        assertNull(cleared.addFailure)
        val window = window(Fx.ran)
        assertNoOp(window, window.clearAddFailure())
    }

    @Test
    fun `resetDeadGroup drops the group everywhere and keeps the other groups`() {
        val window = window(Fx.ran, Fx.bank)
            .setCommonGroup(Fx.POTTER).state()
            .setCardGroup("bank", CardOwnGroupUi.Group(Fx.POTTER)).state()
            .setCardGroup("run", CardOwnGroupUi.Group(Fx.FINANCE)).state()

        val result = window.resetDeadGroup(Fx.POTTER).state()

        assertNull(result.commonGroupId)
        assertEquals(CardOwnGroupUi.AsCommon, result.card("bank").ownGroup)
        assertEquals(CardOwnGroupUi.Group(Fx.FINANCE), result.card("run").ownGroup)
        assertEquals(listOf(Fx.FINANCE), requireNotNull(result.form).groups.map { it.id })
    }

    @Test
    fun `resetDeadGroup keeps a common group that is alive`() {
        val window = window(Fx.ran).setCommonGroup(Fx.FINANCE).state()

        val result = window.resetDeadGroup(Fx.POTTER).state()

        assertEquals(Fx.FINANCE, result.commonGroupId)
    }

    @Test
    fun `resetDeadTarget turns the target into a new word and drops the lemma from the form`() {
        val window = window(Fx.bank, form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2)))

        val result = window.resetDeadTarget(Fx.BANK_1).state()

        val lemmaIds = requireNotNull(result.form).existingLemmas.map { it.wordId }
        assertEquals(CardTargetUi.NewWord, result.card("bank").target)
        assertEquals(listOf(Fx.BANK_2), lemmaIds)
    }

    @Test
    fun `dropComponent removes the component from the form and its values from the lexemes`() {
        val window = window(Fx.perro)

        val result = window.dropComponent(Fx.GLOSS).state()

        val values = result.lexeme("perro", "ruwiktionary/1.noun.1").values
        assertTrue(requireNotNull(result.form).components.none { it.id == Fx.GLOSS })
        assertTrue(values.none { it.componentId == Fx.GLOSS })
    }

    // ===== Закрытие =====

    @Test
    fun `the close question opens and closes`() {
        val window = window(Fx.ran)

        val asking = window.askCloseConfirm().state()

        assertTrue(asking.isConfirmingClose)
        assertFalse(asking.closeCloseConfirm().state().isConfirmingClose)
    }

    @Test
    fun `closeWithResult and close send a navigation effect and leave the state`() {
        val window = window(Fx.ran)

        val withResult = window.closeWithResult(lexemeCount = 3, wordCount = 2)
        val plain = window.close()

        assertEquals(window, withResult.state())
        assertEquals(
            setOf<Effect>(ScanNavigationEffect.CloseWithResult(3, 2)),
            withResult.effects(),
        )
        assertEquals(window, plain.state())
        assertEquals(setOf<Effect>(NavigationEffect.Back), plain.effects())
    }
}
