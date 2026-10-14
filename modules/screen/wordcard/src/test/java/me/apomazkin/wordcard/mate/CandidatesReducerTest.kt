package me.apomazkin.wordcard.mate

import io.github.kilgoret.mate.NavigationEffect
import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.assertEffects
import io.github.kilgoret.mate.test.assertNoEffects
import io.github.kilgoret.mate.test.testReduce
import me.apomazkin.candidates.CandidateAction
import me.apomazkin.candidates.LemmaWordChoice
import me.apomazkin.candidates.PhotoAddFailure
import me.apomazkin.candidates.PhotoDataKind
import me.apomazkin.candidates.PhotoDenyReason
import me.apomazkin.candidates.PhotoFormOutcome
import me.apomazkin.candidates.PhotoLookupOutcome
import me.apomazkin.candidates.PhotoMapping
import me.apomazkin.candidates.PhotoSheet
import me.apomazkin.candidates.PhotoTarget
import me.apomazkin.candidates.PhotoTextField
import me.apomazkin.candidates.PhotoWord
import me.apomazkin.core_resources.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Диалог «значения из словаря» (WCL) по адресам строк спеки: запуск,
 * разделы лемм, пометки, действия, запись, ошибки, закрытие.
 */
class CandidatesReducerTest {
    private val reducer = WordCardReducer(NoopLogger)

    private fun base(value: String = "run"): WordCardState {
        val state = loaded(wordId = CFx.WORD, dictionaryId = CFx.DICT)
        val loadedWord = state.wordState as WordState.Loaded
        return state.copy(canAddFromDictionary = true, wordState = loadedWord.copy(value = value))
    }

    private fun opened(value: String = "run"): WordCardState = reducer.testReduce(base(value), Msg.OpenCandidates).state()

    private fun ready(
        word: PhotoWord = CFx.runWord,
        value: String = word.text,
        remembered: PhotoMapping? = null,
        form: me.apomazkin.candidates.PhotoDictionaryForm = CFx.form,
    ): WordCardState {
        val open = opened(value)
        val run = open.dialog().requestId
        val looked = reducer.testReduce(open, Msg.CandidatesLookedUp(PhotoLookupOutcome.Words(CFx.DICT, listOf(word)), run)).state()
        return reducer.testReduce(looked, Msg.CandidatesFormLoaded(PhotoFormOutcome.Loaded(form, remembered), run)).state()
    }

    private fun act(state: WordCardState, action: CandidateAction) = reducer.testReduce(state, Msg.Candidates(action))

    private fun WordCardState.dialog(): CandidatesState = candidates ?: error("dialog is closed")

    // ===== Ш1: запуск =====

    @Test
    fun `WCL-08_1 WCL-08_2 server access turns the button on regardless of lexemes`() {
        val off = loaded(lexemes = listOf(lexeme(8L, listOf(savedCv(60L)))))
        assertFalse(off.canAddFromDictionary)
        val on = reducer.testReduce(off, Msg.ServerAccessLoaded(true)).state()
        assertTrue(on.canAddFromDictionary)
    }

    @Test
    fun `WCL-08_3 without server access the dialog does not open`() {
        val state = base().copy(canAddFromDictionary = false)
        val result = reducer.testReduce(state, Msg.OpenCandidates)
        assertNull(result.state().candidates)
        result.assertNoEffects()
    }

    @Test
    fun `WCL-01_1 WCL-03_1 opening asks the server for the loaded word with a new run`() {
        val result = reducer.testReduce(base(), Msg.OpenCandidates)
        val dialog = result.state().dialog()
        assertEquals(CFx.WORD, dialog.wordId)
        assertEquals(CFx.DICT, dialog.dictionaryId)
        assertEquals("run", dialog.text)
        assertEquals(CandidatesPhase.LOADING, dialog.phase)
        assertEquals(1L, dialog.requestId)
        result.assertEffects(setOf(DatasourceEffect.LookupCandidates(wordId = CFx.WORD, requestId = 1L)))
    }

    @Test
    fun `WCL-08_4 a second open while the dialog is open is ignored`() {
        val result = reducer.testReduce(opened(), Msg.OpenCandidates)
        assertEquals(1L, result.state().dialog().requestId)
        result.assertNoEffects()
    }

    @Test
    fun `WCL-14_1 the fresh card has no dialog and a remembered mapping is applied when it opens`() {
        assertNull(WordCardState().candidates)
        val state = ready(remembered = CFx.remembered)
        assertEquals(CFx.remembered, state.dialog().mapping)
    }

    // ===== WCL-04: прямая лемма и леммы-формы =====

    @Test
    fun `WCL-04_1 run gives the direct section first and rin as a form`() {
        val cards = ready(CFx.runWord).dialog().cards
        assertEquals(listOf("run", "rin"), cards.map { it.lemma })
        assertNull(cards[0].formNote)
        assertEquals("past participle", cards[1].formNote)
        assertTrue(cards.all { it.isExpanded })
    }

    @Test
    fun `WCL-04_2 running gives running first and run as a form with the server note`() {
        val cards = ready(CFx.runningWord).dialog().cards
        assertEquals(listOf("running", "run"), cards.map { it.lemma })
        assertEquals("present participle and gerund", cards[1].formNote)
    }

    @Test
    fun `WCL-04_3 a direct lemma with one lexeme stays first and hides no forms`() {
        val cards = ready(CFx.ranWord).dialog().cards
        assertEquals(listOf("ran", "run", "rin"), cards.map { it.lemma })
        assertEquals(1, cards[0].lexemes.size)
    }

    @Test
    fun `WCL-04_4 forms only keep the server order`() {
        val cards = ready(CFx.formsOnlyWord).dialog().cards
        assertEquals(listOf("run", "rin"), cards.map { it.lemma })
        assertEquals("simple past", cards[0].formNote)
    }

    @Test
    fun `WCL-04_5 the direct lemma is found regardless of case`() {
        val cards = ready(CFx.runWord, value = "Run").dialog().cards
        assertEquals("run", cards[0].lemma)
        assertNull(cards[0].formNote)
    }

    @Test
    fun `WCL-04_6 WCL-11_1 a word without lexemes or not found shows the reason instead of sections`() {
        for (word in listOf(CFx.emptyLemmasWord, CFx.notFoundWord)) {
            val open = opened(word.text)
            val result = reducer.testReduce(open, Msg.CandidatesLookedUp(PhotoLookupOutcome.Words(CFx.DICT, listOf(word)), open.dialog().requestId))
            assertEquals(CandidatesPhase.ERROR, result.state().dialog().phase)
            assertEquals(R.string.candidates_not_found, result.state().dialog().errorRes)
            result.assertNoEffects()
        }
    }

    // ===== WCL-05: пятёрка =====

    @Test
    fun `WCL-05_1 nineteen lexemes give five rows and fourteen hidden`() {
        val card = ready(CFx.bigWord(19)).dialog().cards.single()
        assertEquals(5, card.lexemes.size)
        assertEquals(14, card.hiddenCount)
    }

    @Test
    fun `WCL-05_2 four lexemes are all shown without a hidden count`() {
        val card = ready(CFx.bigWord(4)).dialog().cards.single()
        assertEquals(4, card.lexemes.size)
        assertEquals(0, card.hiddenCount)
    }

    // ===== WCL-06: «есть» по слову диалога =====

    @Test
    fun `WCL-06_1 the lexeme that the word already has is marked and skipped by the count`() {
        val dialog = ready().dialog()
        val existing = dialog.cards.first().lexemes.single { it.key == "ru/v2" }
        assertTrue(existing.exists)
        assertEquals(1, dialog.summary.existingCount)
        assertEquals(5, dialog.summary.lexemeCount)
    }

    @Test
    fun `WCL-06_1 form sections are marked by the same word too`() {
        val word = PhotoWord("ran", me.apomazkin.candidates.PhotoWordStatus.FOUND, listOf(CFx.runWord.lemmas.first().copy(formNote = "simple past")))
        val dialog = ready(word).dialog()
        assertTrue(dialog.cards.first().lexemes.single { it.key == "ru/v2" }.exists)
    }

    @Test
    fun `WCL-06_3 a word without lexemes gets no marks`() {
        val dialog = ready(form = CFx.emptyWordForm).dialog()
        assertTrue(dialog.cards.flatMap { it.lexemes }.none { it.exists })
        assertEquals(0, dialog.summary.existingCount)
    }

    @Test
    fun `WCL-06_4 when everything is there the button says nothing to add`() {
        val keys = CFx.runLexemes.flatMap { it.translations + listOfNotNull(it.gloss) }.map { me.apomazkin.lexeme.lexemeTextKey(it) }.toSet()
        val form = CFx.form.copy(existingLemmas = listOf(CFx.form.existingLemmas.first().copy(lexemeTextKeys = keys)))
        val word = PhotoWord("run", me.apomazkin.candidates.PhotoWordStatus.FOUND, listOf(CFx.runWord.lemmas.first()))
        val dialog = ready(word, form = form).dialog()
        assertEquals(0, dialog.summary.lexemeCount)
        assertFalse(dialog.summary.isEnabled)
        val result = act(ready(word, form = form), CandidateAction.RequestAdd)
        result.assertNoEffects()
    }

    // ===== WCL-07: состав =====

    @Test
    fun `WCL-07_1 WCL-07_4 a ready dialog carries the form the cards and the count without groups or targets`() {
        val dialog = ready().dialog()
        assertEquals(CandidatesPhase.READY, dialog.phase)
        assertNotNull(dialog.form)
        assertEquals(2, dialog.cards.size)
        assertTrue(dialog.summary.isEnabled)
        assertEquals(PhotoTarget.Component(CFx.TRANSLATION), dialog.mapping.translation)
        assertTrue(dialog.cards.all { it.target == me.apomazkin.candidates.PhotoCardTarget.NewWord })
    }

    @Test
    fun `WCL-07_2 WCL-02_3 a lexeme without a core keeps the button off`() {
        val state = act(ready(), CandidateAction.SetMapping(PhotoDataKind.TRANSLATION, PhotoTarget.None)).state()
        val dialog = state.dialog()
        assertTrue(dialog.cards.first().lexemes.any { it.lacksCore })
        assertFalse(dialog.summary.isEnabled)
        act(state, CandidateAction.RequestAdd).assertNoEffects()
    }

    // ===== WCL-09: действия =====

    @Test
    fun `WCL-09_1 deleting a lexeme leaves four rows keeps the hidden count and marks changes`() {
        val result = act(ready(), CandidateAction.DeleteLexeme("run", "ru/v2"))
        val card = result.state().dialog().cards.first()
        assertEquals(4, card.lexemes.size)
        assertEquals(1, card.hiddenCount)
        assertTrue(result.state().dialog().isDirty)
        result.assertNoEffects()
    }

    @Test
    fun `WCL-09_2 WCL-02_1 an edited text is trimmed and the batch carries only what is left`() {
        var state = act(ready(), CandidateAction.OpenSheet(PhotoSheet.TextEdit("run", "ru/v1", PhotoTextField.Translation, "бежать, бегать"))).state()
        state = act(state, CandidateAction.UpdateDraft("  бежать  ")).state()
        state = act(state, CandidateAction.SubmitText).state()
        val edited = state.dialog().cards.first().lexemes.single { it.key == "ru/v1" }
        assertEquals("бежать", edited.translation.text)
        assertTrue(edited.translation.isEdited)
        // Видимая пятёрка «run»: ru/v1, ru/n1 (по одной на часть речи), затем ru/v2, ru/v3, en/g1; en/v1 — «ещё 1».
        for (key in listOf("ru/v3", "en/g1", "ru/n1")) state = act(state, CandidateAction.DeleteLexeme("run", key)).state()
        state = act(state, CandidateAction.DeleteCard("rin")).state()
        val effect = act(state, CandidateAction.RequestAdd).second.single() as DatasourceEffect.AddCandidates
        val word = effect.batch.words.single()
        assertEquals(1, word.lexemes.size)
        assertEquals(CFx.WORD, word.wordId)
    }

    @Test
    fun `WCL-09_3 a part of speech changes one lexeme only`() {
        val state = act(ready(), CandidateAction.SetPartOfSpeech("run", "ru/v1", CFx.NOUN)).state()
        val lexemes = state.dialog().cards.first().lexemes
        assertEquals(CFx.NOUN, lexemes.single { it.key == "ru/v1" }.partOfSpeechOptionId)
        assertEquals(CFx.VERB, lexemes.single { it.key == "ru/v2" }.partOfSpeechOptionId)
        assertTrue(state.dialog().isDirty)
    }

    @Test
    fun `WCL-09_4 a mapping change is remembered for the dictionary`() {
        val result = act(ready(), CandidateAction.SetMapping(PhotoDataKind.GLOSS, PhotoTarget.Component(CFx.DEFINITION)))
        val mapping = result.state().dialog().mapping
        assertEquals(PhotoTarget.Component(CFx.DEFINITION), mapping.gloss)
        result.assertEffects(setOf(DatasourceEffect.SaveCandidatesMapping(dictionaryId = CFx.DICT, mapping = mapping)))
        assertFalse(result.state().dialog().isDirty)
    }

    @Test
    fun `WCL-09_5 an emptied translation lacks a core and turns the button off`() {
        var state = act(ready(), CandidateAction.OpenSheet(PhotoSheet.TextEdit("run", "ru/v1", PhotoTextField.Translation, "бежать, бегать"))).state()
        state = act(state, CandidateAction.UpdateDraft("   ")).state()
        state = act(state, CandidateAction.SubmitText).state()
        assertTrue(state.dialog().cards.first().lexemes.single { it.key == "ru/v1" }.lacksCore)
        assertFalse(state.dialog().summary.isEnabled)
    }

    // ===== WCL-10: запись =====

    @Test
    fun `WCL-10_1 WCL-10_4 WCL-01_2 add writes without a question into the dialog word without a group`() {
        val result = act(ready(), CandidateAction.RequestAdd)
        assertTrue(result.state().dialog().isAdding)
        val effect = result.second.single() as DatasourceEffect.AddCandidates
        assertEquals(CFx.WORD, effect.wordId)
        val word = effect.batch.words.single()
        assertEquals(CFx.WORD, word.wordId)
        assertEquals("run", word.text)
        assertNull(word.groupId)
        assertEquals(CFx.DICT, effect.batch.dictionaryId)
    }

    @Test
    fun `WCL-10_2 WCL-02_2 the existing lexeme is not in the batch and everything else is`() {
        val effect = act(ready(), CandidateAction.RequestAdd).second.single() as DatasourceEffect.AddCandidates
        val texts = effect.batch.words.single().lexemes.flatMap { lexeme -> lexeme.components.map { it.data.toString() } }
        assertTrue(texts.none { it.contains("управлять") })
        assertEquals(5, effect.batch.words.single().lexemes.size)
    }

    @Test
    fun `WCL-10_3 a second add during the write does nothing`() {
        val adding = act(ready(), CandidateAction.RequestAdd).state()
        act(adding, CandidateAction.RequestAdd).assertNoEffects()
        act(adding, CandidateAction.ConfirmAdd).assertNoEffects()
    }

    @Test
    fun `WCL-10_1 after the write the dialog closes and the count is shown`() {
        val adding = act(ready(), CandidateAction.RequestAdd).state()
        val result = reducer.testReduce(adding, Msg.CandidatesAdded(lexemeCount = 5, requestId = adding.dialog().requestId))
        assertNull(result.state().candidates)
        result.assertEffects(setOf(UiEffect.ShowCountSnackbar(messageRes = R.plurals.candidates_added, count = 5)))
    }

    // ===== WCL-11: ошибки =====

    @Test
    fun `WCL-11_2 WCL-11_3 a refusal or an unreachable server shows its text`() {
        val open = opened()
        val run = open.dialog().requestId
        val denied = reducer.testReduce(open, Msg.CandidatesLookedUp(PhotoLookupOutcome.Denied(PhotoDenyReason.RATE_LIMITED), run)).state()
        assertEquals(R.string.photo_words_error_rate_limited, denied.dialog().errorRes)
        val down = reducer.testReduce(open, Msg.CandidatesLookedUp(PhotoLookupOutcome.Unavailable, run)).state()
        assertEquals(R.string.photo_words_error_server, down.dialog().errorRes)
    }

    @Test
    fun `WCL-11_4 an answer after the dialog closed is dropped`() {
        val open = opened()
        val run = open.dialog().requestId
        val closed = act(open, CandidateAction.RequestClose).state()
        assertNull(closed.candidates)
        val result = reducer.testReduce(closed, Msg.CandidatesLookedUp(PhotoLookupOutcome.Words(CFx.DICT, listOf(CFx.runWord)), run))
        assertNull(result.state().candidates)
        result.assertNoEffects()
    }

    @Test
    fun `WCL-11_5 WCL-01_3 a failed form or a missing word shows the reason`() {
        val open = opened()
        val run = open.dialog().requestId
        val failed = reducer.testReduce(open, Msg.CandidatesFormLoaded(PhotoFormOutcome.Failed("boom"), run)).state()
        assertEquals(R.string.photo_add_form_failed, failed.dialog().errorRes)
        val missing = reducer.testReduce(open, Msg.CandidatesLookedUp(null, run)).state()
        assertEquals(R.string.candidates_word_missing, missing.dialog().errorRes)
        val noDictionary = reducer.testReduce(open, Msg.CandidatesFormLoaded(PhotoFormOutcome.DictionaryMissing, run)).state()
        assertEquals(R.string.photo_add_dictionary_missing, noDictionary.dialog().errorRes)
    }

    // ===== WCL-12: закрытие =====

    @Test
    fun `WCL-12_1 WCL-12_3 closing without edits or after a mapping change closes at once`() {
        act(ready(), CandidateAction.RequestClose).let { result ->
            assertNull(result.state().candidates)
            result.assertNoEffects()
        }
        val mapped = act(ready(), CandidateAction.SetMapping(PhotoDataKind.GLOSS, PhotoTarget.Component(CFx.DEFINITION))).state()
        assertNull(act(mapped, CandidateAction.RequestClose).state().candidates)
    }

    @Test
    fun `WCL-12_2 closing after a deletion asks stay keeps everything close writes nothing`() {
        val deleted = act(ready(), CandidateAction.DeleteLexeme("run", "ru/v1")).state()
        val asking = act(deleted, CandidateAction.RequestClose).state()
        assertTrue(asking.dialog().isConfirmingClose)
        val stayed = act(asking, CandidateAction.Stay).state()
        assertFalse(stayed.dialog().isConfirmingClose)
        assertEquals(4, stayed.dialog().cards.first().lexemes.size)
        val closed = act(asking, CandidateAction.ConfirmClose)
        assertNull(closed.state().candidates)
        closed.assertNoEffects()
    }

    @Test
    fun `WCL-12_4 closing during the write does nothing`() {
        val adding = act(ready(), CandidateAction.RequestAdd).state()
        val result = act(adding, CandidateAction.RequestClose)
        assertNotNull(result.state().candidates)
        result.assertNoEffects()
    }

    // ===== WCL-13: запись не удалась =====

    @Test
    fun `WCL-13_1 a failed write keeps the dialog and retry writes again without a question`() {
        val adding = act(ready(), CandidateAction.RequestAdd).state()
        val run = adding.dialog().requestId
        val failed = reducer.testReduce(adding, Msg.CandidatesRejected(PhotoAddFailure.Failed, run)).state()
        assertFalse(failed.dialog().isAdding)
        assertEquals(PhotoAddFailure.Failed, failed.dialog().addFailure)
        assertEquals(2, failed.dialog().cards.size)
        val retry = act(failed, CandidateAction.RetryAdd)
        assertTrue(retry.state().dialog().isAdding)
        assertNull(retry.state().dialog().addFailure)
        assertTrue(retry.second.single() is DatasourceEffect.AddCandidates)
    }

    @Test
    fun `WCL-13_2 a deleted word shows the reason without retry`() {
        val adding = act(ready(), CandidateAction.RequestAdd).state()
        val result = reducer.testReduce(adding, Msg.CandidatesRejected(PhotoAddFailure.WordMissing(CFx.WORD), adding.dialog().requestId)).state()
        assertEquals(CandidatesPhase.ERROR, result.dialog().phase)
        assertEquals(R.string.candidates_word_missing, result.dialog().errorRes)
        assertNull(result.dialog().addFailure)
    }

    @Test
    fun `WCL-13_3 a disabled component drops out of the form and its mapping falls back`() {
        val mapped = act(ready(), CandidateAction.SetMapping(PhotoDataKind.GLOSS, PhotoTarget.Component(CFx.DEFINITION))).state()
        val adding = act(mapped, CandidateAction.RequestAdd).state()
        val result = reducer.testReduce(adding, Msg.CandidatesRejected(PhotoAddFailure.TypeUnavailable(CFx.DEFINITION), adding.dialog().requestId)).state()
        val dialog = result.dialog()
        assertTrue(dialog.form!!.components.none { it.id == CFx.DEFINITION })
        assertEquals(PhotoTarget.None, dialog.mapping.gloss)
        assertEquals(PhotoAddFailure.TypeUnavailable(CFx.DEFINITION), dialog.addFailure)
        assertFalse(dialog.isAdding)
    }

    @Test
    fun `WCL-12_5 saving an unchanged text or the same part of speech does not ask on close`() {
        var state = act(ready(), CandidateAction.OpenSheet(PhotoSheet.TextEdit("run", "ru/v1", PhotoTextField.Translation, "бежать, бегать"))).state()
        state = act(state, CandidateAction.SubmitText).state()
        state = act(state, CandidateAction.SetPartOfSpeech("run", "ru/v1", CFx.VERB)).state()
        state = act(state, CandidateAction.ResetText("run", "ru/v1", PhotoTextField.Translation)).state()
        assertFalse(state.dialog().isDirty)
        assertNull(act(state, CandidateAction.RequestClose).state().candidates)
    }

    // ===== WCL-15: переименование или перенос в лемму =====

    private fun rename(lemma: String) = CandidateAction.ToggleLemmaWord(LemmaWordChoice.Rename(lemma))

    private fun transfer(lemma: String, wordId: Long) = CandidateAction.ToggleLemmaWord(LemmaWordChoice.Transfer(lemma, wordId))

    @Test
    fun `WCL-15_1 WCL-15_5 the choice is off by default and only form sections take it`() {
        val state = ready(CFx.runningWord)
        assertNull(state.dialog().lemmaWordChoice)
        val direct = act(state, rename("running"))
        assertNull(direct.state().dialog().lemmaWordChoice)
        val chosen = act(state, rename("run")).state()
        assertEquals(LemmaWordChoice.Rename("run"), chosen.dialog().lemmaWordChoice)
        assertFalse(chosen.dialog().isDirty)
        assertNull(act(chosen, CandidateAction.RequestClose).state().candidates)
    }

    @Test
    fun `WCL-15_2 rename puts the lemma into the batch and keeps the word`() {
        val chosen = act(ready(CFx.runningWord, form = CFx.formWithRunWord), rename("run")).state()
        val effect = act(chosen, CandidateAction.RequestAdd).second.single() as DatasourceEffect.AddCandidates
        val word = effect.batch.words.single()
        assertEquals(CFx.WORD, word.wordId)
        assertEquals("run", word.renameTo)
        assertNull(word.absorbWordId)
        assertFalse(effect.isTransfer)
        // «бежать, бегать» есть у слова 8, но при переименовании пишется: слова разные.
        assertEquals(3, word.lexemes.size)
        val plain = act(ready(CFx.runningWord), CandidateAction.RequestAdd).second.single() as DatasourceEffect.AddCandidates
        assertNull(plain.batch.words.single().renameTo)
    }

    @Test
    fun `WCL-15_1 WCL-15_3 a second toggle clears and another form or word replaces the choice`() {
        val state = ready(CFx.ranWord)
        val run = act(state, rename("run")).state()
        assertEquals(LemmaWordChoice.Rename("run"), run.dialog().lemmaWordChoice)
        val rin = act(run, rename("rin")).state()
        assertEquals(LemmaWordChoice.Rename("rin"), rin.dialog().lemmaWordChoice)
        assertNull(act(rin, rename("rin")).state().dialog().lemmaWordChoice)

        val twins = ready(CFx.runningWord, form = CFx.formWithTwoRunWords)
        assertNull(act(twins, transfer("run", 99L)).state().dialog().lemmaWordChoice)
        val first = act(twins, transfer("run", 8L)).state()
        assertEquals(LemmaWordChoice.Transfer("run", 8L), first.dialog().lemmaWordChoice)
        val second = act(first, transfer("run", 9L)).state()
        assertEquals(LemmaWordChoice.Transfer("run", 9L), second.dialog().lemmaWordChoice)
        assertEquals(LemmaWordChoice.Rename("run"), act(second, rename("run")).state().dialog().lemmaWordChoice)
        assertNull(act(second, transfer("run", 9L)).state().dialog().lemmaWordChoice)
    }

    @Test
    fun `WCL-15_3 the transfer batch targets the lemma word, absorbs the dialog word and skips its values`() {
        val chosen = act(ready(CFx.runningWord, form = CFx.formWithRunWord), transfer("run", 8L)).state()
        val effect = act(chosen, CandidateAction.RequestAdd).second.single() as DatasourceEffect.AddCandidates
        val word = effect.batch.words.single()
        assertEquals(8L, word.wordId)
        assertEquals(CFx.WORD, word.absorbWordId)
        assertNull(word.renameTo)
        assertTrue(effect.isTransfer)
        // Четыре кандидата: «управлять» есть у слова диалога, «бежать, бегать» — у слова 8; остальные два пишутся.
        assertEquals(2, word.lexemes.size)
    }

    @Test
    fun `WCL-15_4 values of the lemma word are marked, counted and shrink the button on transfer`() {
        val state = ready(CFx.runningWord, form = CFx.formWithRunWord)
        val run = state.dialog().cards.first { it.lemma == "run" }
        assertTrue(run.lexemes.first { it.key == "ru/v1" }.existsInLemma)
        assertTrue(run.lexemes.first { it.key == "ru/v2" }.existsInLemma)
        assertFalse(run.lexemes.first { it.key == "ru/v3" }.existsInLemma)
        assertFalse(state.dialog().cards.first { it.lemma == "running" }.lexemes.single().existsInLemma)
        // Два кандидата плюс «управлять» самого слова «running».
        assertEquals(mapOf("run" to 3), state.dialog().sameValues)
        assertEquals(3, state.dialog().summary.lexemeCount)

        val chosen = act(state, transfer("run", 8L)).state()
        assertEquals(2, chosen.dialog().summary.lexemeCount)
        assertEquals(2, chosen.dialog().summary.existingCount)
        assertEquals(3, act(chosen, rename("run")).state().dialog().summary.lexemeCount)

        val deleted = act(state, CandidateAction.DeleteLexeme("run", "ru/v1")).state()
        assertEquals(mapOf("run" to 2), deleted.dialog().sameValues)
        assertTrue(deleted.dialog().cards.first { it.lemma == "run" }.lexemes.first { it.key == "ru/v2" }.exists)
        assertEquals(emptyMap<String, Int>(), ready(CFx.runningWord, form = CFx.runningForm).dialog().sameValues)
    }

    @Test
    fun `WCL-15_3 after the transfer the dialog closes and the card leaves`() {
        val chosen = act(ready(CFx.runningWord, form = CFx.formWithRunWord), transfer("run", 8L)).state()
        val adding = act(chosen, CandidateAction.RequestAdd).state()

        val result = reducer.testReduce(adding, Msg.CandidatesTransferred(adding.dialog().requestId))

        assertNull(result.state().candidates)
        assertTrue(result.state().isExiting)
        assertTrue(result.second.any { it is NavigationEffect.Back })
        reducer.testReduce(adding, Msg.CandidatesTransferred(999L)).assertNoEffects()
    }

    @Test
    fun `WCL-15_6 a transfer rejected for a vanished word shows the reason and keeps the card`() {
        val chosen = act(ready(CFx.runningWord, form = CFx.formWithRunWord), transfer("run", 8L)).state()
        val adding = act(chosen, CandidateAction.RequestAdd).state()

        val result = reducer.testReduce(adding, Msg.CandidatesRejected(PhotoAddFailure.WordMissing(8L), adding.dialog().requestId)).state()

        assertEquals(CandidatesPhase.ERROR, result.dialog().phase)
        assertEquals(R.string.candidates_word_missing, result.dialog().errorRes)
        assertFalse(result.dialog().isAdding)
        assertFalse(result.isExiting)
    }
}
