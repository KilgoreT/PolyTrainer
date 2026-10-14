package me.apomazkin.wordstab.logic.photo

import me.apomazkin.candidates.PhotoAddFailure
import me.apomazkin.candidates.PhotoCardTarget
import me.apomazkin.candidates.PhotoDataKind
import me.apomazkin.candidates.PhotoFormError
import me.apomazkin.candidates.PhotoLemma
import me.apomazkin.candidates.PhotoOwnGroup
import me.apomazkin.candidates.PhotoSheet
import me.apomazkin.candidates.PhotoTarget
import me.apomazkin.candidates.PhotoTextField
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.state
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import me.apomazkin.wordstab.logic.DatasourceEffect
import me.apomazkin.wordstab.logic.PhotoPhase
import me.apomazkin.wordstab.logic.PhotoWordsState
import me.apomazkin.wordstab.logic.SnackbarState
import me.apomazkin.wordstab.logic.WordsTabState
import me.apomazkin.wordstab.logic.isCurrentPhotoRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Слой атомов: каждый атом — один шаг, состояние и эффекты. Тест
 * наследует [PhotoStateAtoms] с пустым логгером, как reducer.
 */
class PhotoStateAtomsTest : PhotoStateAtoms(NoopLogger) {

    private object NoopLogger : LexemeLogger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {}
    }

    private val baseState = WordsTabState(
        isLoading = false,
        snackbarState = SnackbarState(title = "test", show = true),
    )

    private val form = Fx.form()

    private fun WordsTabState.photo() = photoWordsState

    /** Окно после ответа и формы: ran + bank. */
    private fun window(): WordsTabState = baseState
        .startRecognizing().state()
        .startLooking().state()
        .startFormLoading(Fx.DICT, listOf(Fx.ran, Fx.bank)).state()
        .applyForm(form, remembered = null).state()
        .refreshExistsMarks().state()
        .refreshCoreMarks().state()
        .refreshSummary().state()

    // ===== запуск =====

    @Test
    fun `chooser opens the dialog with a new run number and recognizing drops the previous result`() {
        val chooser = baseState.showPhotoSourceChooser().state()
        val recognizing = chooser.showPhotoError(1).state().startRecognizing().state()

        assertTrue(chooser.photo().isChoosingSource)
        assertEquals(1L, chooser.photo().requestId)
        assertEquals(PhotoPhase.RECOGNIZING, recognizing.photo().phase)
        assertNull(recognizing.photo().errorRes)
        assertEquals(2L, recognizing.photo().requestId)
    }

    @Test
    fun `looking keeps the run and form loading carries words dictionary and the effect`() {
        val looking = baseState.startRecognizing().state().startLooking().state()

        val (loading, effects) = looking.startFormLoading(Fx.DICT, listOf(Fx.ran, Fx.running))

        assertEquals(PhotoPhase.LOOKING, looking.photo().phase)
        assertEquals(PhotoPhase.FORM, loading.photo().phase)
        assertEquals(Fx.DICT, loading.photo().dictionaryId)
        assertEquals(2, loading.photo().words.size)
        assertEquals(
            setOf<Effect>(DatasourceEffect.LoadPhotoForm(dictionaryId = Fx.DICT, lemmaTexts = listOf("run", "running"), requestId = 1L)),
            effects,
        )
    }

    @Test
    fun `no words error and hide reset everything but the run number`() {
        val empty = baseState.startRecognizing().state().showNoPhotoWords().state()
        val error = baseState.startRecognizing().state().showPhotoError(7).state()
        val hidden = window().hidePhotoWords().state()

        assertTrue(empty.photo().isEmptyResult)
        assertFalse(empty.photo().isBusy)
        assertEquals(7, error.photo().errorRes)
        assertEquals(PhotoWordsState(requestId = 1L), hidden.photo())
        assertFalse(hidden.isCurrentPhotoRequest(1L))
    }

    @Test
    fun `should not touch other fields`() {
        val result = baseState.showPhotoSourceChooser().state()

        assertEquals(baseState.isLoading, result.isLoading)
        assertEquals(baseState.snackbarState, result.snackbarState)
        assertEquals(baseState.addWordDialogState, result.addWordDialogState)
    }

    // ===== форма и пометки =====

    @Test
    fun `AWP-24_1 AWP-24_4 form errors close the indicator and keep the dialog open`() {
        val loading = baseState.startRecognizing().state().startFormLoading(Fx.DICT, listOf(Fx.ran)).state()

        val failed = loading.showFormError(PhotoFormError.LOAD_FAILED).state()
        val missing = loading.showFormError(PhotoFormError.DICTIONARY_MISSING).state()

        assertEquals(PhotoPhase.IDLE, failed.photo().phase)
        assertEquals(PhotoFormError.LOAD_FAILED, failed.photo().formError)
        assertEquals(PhotoFormError.DICTIONARY_MISSING, missing.photo().formError)
        assertTrue(failed.photo().isOpen)
        assertFalse(failed.photo().isWindowReady)
    }

    @Test
    fun `applyForm builds cards resolves mapping and starts with common group all`() {
        val state = window()

        assertTrue(state.photo().isWindowReady)
        assertEquals(listOf("run", "bank"), state.photo().cards.map { it.lemma })
        assertEquals(Fx.defaultMapping, state.photo().mapping)
        assertNull(state.photo().commonGroupId)
        assertEquals(6, state.photo().summary.lexemeCount)
    }

    @Test
    fun `AWP-12_3 cards absent expands not added`() {
        val state = baseState.startRecognizing().state().startFormLoading(Fx.DICT, listOf(Fx.notFound)).state().applyForm(form, null).state()

        assertTrue(state.photo().isNotAddedExpanded)
        assertTrue(state.photo().cards.isEmpty())
    }

    // ===== действия =====

    @Test
    fun `AWP-15_1 AWP-15_3 deleting a lexeme and a whole card keeps the hidden count and drops the card`() {
        val state = window()

        val lexemeGone = state.deleteLexeme("run", "ru/v2").state()
        val cardGone = lexemeGone.deleteCard("bank").state()

        assertEquals(4, lexemeGone.photo().cards.first { it.lemma == "run" }.lexemes.size)
        assertEquals(2, lexemeGone.photo().cards.first { it.lemma == "run" }.hiddenCount)
        assertEquals(listOf("run"), cardGone.photo().cards.map { it.lemma })
        assertTrue(cardGone.photo().notAdded.isEmpty())
    }

    @Test
    fun `AWP-15_2 deleting an example keeps the lexeme`() {
        val state = baseState.startRecognizing().state()
            .startFormLoading(Fx.DICT, listOf(Fx.word("run", me.apomazkin.candidates.PhotoLemma("run", null, listOf(Fx.runLexemes[4]))))).state()
            .applyForm(form, null).state()

        val result = state.deleteExample("run", "en/v1", 0).state()

        assertEquals(listOf("She ran home."), result.photo().cards.single().lexemes.single().examples.map { it.text })
    }

    @Test
    fun `AWP-16_1 AWP-16_3 AWP-16_6 text edit trims marks edited and reset restores the source`() {
        val state = window()

        val edited = state.applyTextEdit("run", "ru/v1", PhotoTextField.Translation, "  брать ").state()
        val reset = edited.resetText("run", "ru/v1", PhotoTextField.Translation).state()

        val lexeme = edited.photo().cards.first().lexemes.first { it.key == "ru/v1" }
        assertEquals("брать", lexeme.translation.text)
        assertTrue(lexeme.translation.isEdited)
        assertEquals("бежать, бегать", reset.photo().cards.first().lexemes.first { it.key == "ru/v1" }.translation.text)
        assertFalse(reset.photo().cards.first().lexemes.first { it.key == "ru/v1" }.translation.isEdited)
    }

    @Test
    fun `AWP-16_5 emptied example is dropped by the next atom`() {
        val state = baseState.startRecognizing().state()
            .startFormLoading(Fx.DICT, listOf(Fx.word("run", me.apomazkin.candidates.PhotoLemma("run", null, listOf(Fx.runLexemes[4]))))).state()
            .applyForm(form, null).state()

        val result = state.applyTextEdit("run", "en/v1", PhotoTextField.Example(0), " ").state().dropEmptyExamples("run", "en/v1").state()

        assertEquals(listOf("She ran home."), result.photo().cards.single().lexemes.single().examples.map { it.text })
    }

    @Test
    fun `AWP-07_7 AWP-07_10 exists mark follows the edited text`() {
        val state = window()

        val nowExists = state.applyTextEdit("run", "ru/v1", PhotoTextField.Translation, "управлять").state().refreshExistsMarks().state()
        val gone = state.applyTextEdit("run", "ru/v2", PhotoTextField.Translation, "руководить").state().refreshExistsMarks().state()

        assertTrue(nowExists.photo().cards.first().lexemes.first { it.key == "ru/v1" }.exists)
        assertFalse(gone.photo().cards.first().lexemes.first { it.key == "ru/v2" }.exists)
    }

    @Test
    fun `AWP-17_1 part of speech changes only one lexeme`() {
        val state = window()

        val result = state.setPartOfSpeech("run", "ru/v1", Fx.NOUN).state()

        val lexemes = result.photo().cards.first().lexemes
        assertEquals(Fx.NOUN, lexemes.first { it.key == "ru/v1" }.partOfSpeechOptionId)
        assertEquals(Fx.VERB, lexemes.first { it.key == "ru/v2" }.partOfSpeechOptionId)
    }

    @Test
    fun `AWP-18_4 AWP-08_5 card target and own group are set and cleared`() {
        val state = window()

        val targeted = state.setCardTarget("bank", PhotoCardTarget.NewWord).state()
        val grouped = targeted.setCardGroup("bank", PhotoOwnGroup.Group(Fx.FINANCE)).state()
        val cleared = grouped.setCardGroup("bank", PhotoOwnGroup.AsCommon).state()

        assertEquals(PhotoCardTarget.NewWord, targeted.photo().cards.first { it.lemma == "bank" }.target)
        assertEquals(PhotoOwnGroup.Group(Fx.FINANCE), grouped.photo().cards.first { it.lemma == "bank" }.ownGroup)
        assertEquals(PhotoOwnGroup.AsCommon, cleared.photo().cards.first { it.lemma == "bank" }.ownGroup)
    }

    @Test
    fun `AWP-09_2 AWP-28_1 mapping target is chosen frees the other kind and is saved by effect`() {
        val state = window()

        val (chosen, effects) = state.setMappingTarget(PhotoDataKind.GLOSS, PhotoTarget.Component(Fx.TRANSLATION))

        assertEquals(PhotoTarget.Component(Fx.TRANSLATION), chosen.photo().mapping.gloss)
        assertEquals(PhotoTarget.None, chosen.photo().mapping.translation)
        assertEquals(setOf<Effect>(DatasourceEffect.SavePhotoMapping(Fx.DICT, chosen.photo().mapping)), effects)
    }

    @Test
    fun `AWP-11_4 core marks and summary follow the mapping`() {
        val state = window()
        val noCore = state.setMappingTarget(PhotoDataKind.TRANSLATION, PhotoTarget.None).state()
            .setMappingTarget(PhotoDataKind.GLOSS, PhotoTarget.Component(Fx.DEFINITION)).state()

        val marked = noCore.refreshCoreMarks().state().refreshSummary().state()
        val restored = marked.setMappingTarget(PhotoDataKind.TRANSLATION, PhotoTarget.Component(Fx.TRANSLATION)).state()
            .refreshCoreMarks().state().refreshSummary().state()

        assertTrue(marked.photo().summary.lacksCoreCount > 0)
        assertFalse(marked.photo().summary.isEnabled)
        assertEquals(0, restored.photo().summary.lacksCoreCount)
        assertTrue(restored.photo().summary.isEnabled)
    }

    @Test
    fun `sheets open close and take the draft`() {
        val state = window()
        val sheet = PhotoSheet.TextEdit("run", "ru/v1", PhotoTextField.Translation, draft = "бежать, бегать")

        val opened = state.openSheet(sheet).state()
        val typed = opened.updateSheetDraft("брать").state()
        val closed = typed.closeSheet().state()

        assertEquals(sheet, opened.photo().sheet)
        assertEquals("брать", (typed.photo().sheet as PhotoSheet.TextEdit).draft)
        assertNull(closed.photo().sheet)
    }

    // ===== добавление и закрытие =====

    @Test
    fun `AWP-13_4 AWP-20 starting to add marks the window busy and sends the batch of the run`() {
        val state = window()

        val (adding, effects) = state.startAdding()

        assertTrue(adding.photo().isAdding)
        val effect = effects.single() as DatasourceEffect.AddPhotoWords
        assertEquals(1L, effect.requestId)
        assertEquals(listOf("run", "bank"), effect.batch.words.map { it.text })
    }

    @Test
    fun `AWP-21_1 failure reopens the window with the reason and retry clears it`() {
        val adding = window().startAdding().state()

        val failed = adding.showAddFailure(PhotoAddFailure.Failed).state()
        val cleared = failed.clearAddFailure().state()

        assertFalse(failed.photo().isAdding)
        assertEquals(PhotoAddFailure.Failed, failed.photo().addFailure)
        assertNull(cleared.photo().addFailure)
    }

    @Test
    fun `AWP-21_3 AWP-21_5 dead group resets common and own groups and leaves the form`() {
        val state = window().setCommonGroup(Fx.POTTER).state().setCardGroup("bank", PhotoOwnGroup.Group(Fx.POTTER)).state()

        val result = state.resetDeadGroup(Fx.POTTER).state()

        assertNull(result.photo().commonGroupId)
        assertEquals(PhotoOwnGroup.AsCommon, result.photo().cards.first { it.lemma == "bank" }.ownGroup)
        assertEquals(listOf(Fx.FINANCE), result.photo().form!!.groups.map { it.id })
    }

    @Test
    fun `AWP-21_4 dead lemma resets the target to a new word`() {
        val result = window().resetDeadTarget(Fx.BANK_1).state()

        assertEquals(PhotoCardTarget.NewWord, result.photo().cards.first { it.lemma == "bank" }.target)
        assertEquals(listOf(Fx.RUN_WORD, Fx.BANK_2), result.photo().form!!.existingLemmas.map { it.wordId })
    }

    @Test
    fun `AWP-21_6 dropped component returns its kind to the default`() {
        val state = window().setMappingTarget(PhotoDataKind.GLOSS, PhotoTarget.Component(Fx.DEFINITION)).state()

        val result = state.dropComponent(Fx.DEFINITION).state()

        assertEquals(PhotoTarget.None, result.photo().mapping.gloss)
        assertTrue(result.photo().form!!.components.none { it.id == Fx.DEFINITION })
    }

    @Test
    fun `AWP-19 AWP-25 dirty flag and close confirm`() {
        val state = window()

        val dirty = state.markDirty().state()
        val asking = dirty.askCloseConfirm().state()
        val staying = asking.closeCloseConfirm().state()

        assertFalse(state.photo().isDirty)
        assertTrue(dirty.photo().isDirty)
        assertTrue(asking.photo().isConfirmingClose)
        assertFalse(staying.photo().isConfirmingClose)
    }
}
