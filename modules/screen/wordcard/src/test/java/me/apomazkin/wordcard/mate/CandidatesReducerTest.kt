package me.apomazkin.wordcard.mate

import io.github.kilgoret.mate.NavigationEffect
import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.assertEffects
import io.github.kilgoret.mate.test.assertNoEffects
import io.github.kilgoret.mate.test.testReduce
import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.candidates.CandidateAction
import me.apomazkin.candidates.CandidateValueUiItem
import me.apomazkin.candidates.CandidatesSheetUi
import me.apomazkin.candidates.CardLexemeUiItem
import me.apomazkin.candidates.CardTargetUi
import me.apomazkin.candidates.ComponentMatchLineUiItem
import me.apomazkin.candidates.ComponentRecommendationUi
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.LemmaWordChoice
import me.apomazkin.candidates.LookupWordUi
import me.apomazkin.candidates.RemarkUiItem
import me.apomazkin.candidates.ValueRefUi
import me.apomazkin.candidates.WordRemarkUi
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.BatchComponent
import me.apomazkin.lexeme.CaptionedTextValues
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TextValues
import me.apomazkin.lexeme.lexemeTextKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Диалог лексем-кандидатов (WCL) по адресам строк спеки: запуск, разделы лемм,
 * «Замечания» и «Сопоставление», пометки, действия, запись, ошибки, закрытие.
 * Редьюсер — через `testReduce` на сообщениях хендлера: исход юзкейса сюда
 * приходит уже разобранным.
 */
class CandidatesReducerTest {
    private val reducer = WordCardReducer(NoopLogger)

    private fun base(value: String = "run"): WordCardState {
        val state = loaded(wordId = CFx.WORD, dictionaryId = CFx.DICT)
        val loadedWord = state.wordState as WordState.Loaded
        return state.copy(canAddFromDictionary = true, wordState = loadedWord.copy(value = value))
    }

    private fun send(
        state: WordCardState,
        message: Msg,
    ) = reducer.testReduce(state, message)

    private fun act(
        state: WordCardState,
        action: CandidateAction,
    ) = send(state, Msg.Candidates(action))

    private fun opened(value: String = "run"): WordCardState =
        send(base(value), Msg.OpenCandidates).state()

    /** Диалог после ответа и формы: слово ответа, рекомендации и форма — сообщениями хендлера. */
    private fun ready(
        word: LookupWordUi = CFx.runWord,
        value: String = word.text,
        form: DictionaryFormUi = CFx.form,
        recommendations: List<ComponentRecommendationUi> = emptyList(),
    ): WordCardState {
        val open = opened(value)
        val run = open.runId()
        val found = send(open, Msg.CandidatesFound(word, recommendations, run)).state()
        return send(found, Msg.CandidatesFormLoaded(form, run)).state()
    }

    /** «running» с получателем переноса — словом словаря «run» (id 8). */
    private fun readyRunning(form: DictionaryFormUi = CFx.formWithRunWord): WordCardState =
        ready(CFx.runningWord, form = form)

    private fun addEffect(state: WordCardState): DatasourceEffect.AddCandidates =
        act(state, CandidateAction.RequestAdd).second.single() as DatasourceEffect.AddCandidates

    private fun WordCardState.dialog(): CandidatesState = candidates ?: error("dialog is closed")

    private fun WordCardState.runId(): Long = dialog().requestId

    private fun CandidatesState.lexeme(
        lemma: String,
        key: String,
    ): CardLexemeUiItem = cards.first { it.lemma == lemma }.lexemes.single { it.key == key }

    private fun CardLexemeUiItem.texts(componentId: Long): List<String> {
        val value = values.first { it.componentId == componentId }
        return (value as CandidateValueUiItem.Text).texts.map { it.text }
    }

    private fun CardLexemeUiItem.options(componentId: Long): List<Long> {
        val value = values.first { it.componentId == componentId }
        return (value as CandidateValueUiItem.Choice).options
    }

    private fun CardLexemeUiItem.isActive(componentId: Long): Boolean =
        values.first { it.componentId == componentId }.isActive

    private val translationRef = CFx.ref("run", "ru/v1", CFx.TRANSLATION)

    private val verbRef = CFx.componentRef("run", "ru/v1", CFx.PART_OF_SPEECH)

    /** Лист правки открывает UI с любым черновиком — текст и «сбросить» берёт атом из стейта. */
    private fun openTextEdit(
        state: WordCardState,
        ref: ValueRefUi = translationRef,
    ): WordCardState {
        val sheet = CandidatesSheetUi.TextEdit(ref, draft = "", canReset = false)
        return act(state, CandidateAction.OpenSheet(sheet)).state()
    }

    // ===== Ш1: запуск =====

    @Test
    fun `WCL-08_1 WCL-08_2 server access turns the button on regardless of lexemes`() {
        val off = loaded(lexemes = listOf(lexeme(8L, listOf(savedCv(60L)))))
        assertFalse(off.canAddFromDictionary)
        val on = send(off, Msg.ServerAccessLoaded(true)).state()
        assertTrue(on.canAddFromDictionary)
    }

    @Test
    fun `WCL-08_3 without server access the dialog does not open`() {
        val state = base().copy(canAddFromDictionary = false)
        val result = send(state, Msg.OpenCandidates)
        assertNull(result.state().candidates)
        result.assertNoEffects()
    }

    @Test
    fun `WCL-01_1 WCL-03_1 opening asks the server for the loaded word with a new run`() {
        val result = send(base(), Msg.OpenCandidates)
        val dialog = result.state().dialog()
        assertEquals(CFx.WORD, dialog.wordId)
        assertEquals(CFx.DICT, dialog.dictionaryId)
        assertEquals("run", dialog.text)
        assertEquals(CandidatesPhase.LOADING, dialog.phase)
        assertFalse(dialog.hasRetryButton)
        assertEquals(1L, dialog.requestId)
        val lookup = DatasourceEffect.LookupCandidates(wordId = CFx.WORD, requestId = 1L)
        result.assertEffects(setOf(lookup))
    }

    @Test
    fun `WCL-08_4 a second open while the dialog is open is ignored`() {
        val result = send(opened(), Msg.OpenCandidates)
        assertEquals(1L, result.state().dialog().requestId)
        result.assertNoEffects()
    }

    @Test
    fun `WCL-14_1 the fresh card has no dialog`() {
        assertNull(WordCardState().candidates)
    }

    @Test
    fun `WCL-15_1 the answer goes to the form with the texts of form lemmas only`() {
        val cases = listOf(
            CFx.runWord to listOf("rin"),
            CFx.runningWord to listOf("run"),
            CFx.ranWord to listOf("run", "rin"),
            CFx.formsOnlyWord to listOf("run", "rin"),
        )
        for ((word, formLemmas) in cases) {
            val open = opened(word.text)
            val run = open.runId()

            val result = send(open, Msg.CandidatesFound(word, emptyList(), run))

            assertEquals(word.text, listOf(word), result.state().dialog().words)
            val form = DatasourceEffect.LoadCandidatesForm(CFx.WORD, formLemmas, run)
            result.assertEffects(setOf(form))
        }
    }

    // ===== WCL-04: прямая лемма и леммы-формы =====

    @Test
    fun `WCL-04_1 run gives the direct section first and rin as a form, every lexeme shown`() {
        val cards = ready(CFx.runWord).dialog().cards
        assertEquals(listOf("run", "rin"), cards.map { it.lemma })
        assertNull(cards[0].formNote)
        assertEquals("past participle", cards[1].formNote)
        // «ещё N» нет: все лексемы-кандидаты леммы в разделе (WCL-05 отменён).
        assertEquals(6, cards[0].lexemes.size)
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
    fun `WCL-04_7 WCL-11_1 a word without lexemes shows the reason instead of sections`() {
        for (word in listOf(CFx.notFoundWord, CFx.emptyLemmasWord)) {
            val open = opened(word.text)

            val result = send(open, Msg.CandidatesNotFound(open.runId()))

            val dialog = result.state().dialog()
            assertEquals(CandidatesPhase.ERROR, dialog.phase)
            assertEquals(R.string.candidates_not_found, dialog.errorRes)
            assertFalse(dialog.hasRetryButton)
            result.assertNoEffects()
        }
    }

    @Test
    fun `WCL-04_8 perro keeps the order of the answer and shows lexemes by components`() {
        val dialog = ready(CFx.perro, form = CFx.contractForm).dialog()

        assertEquals(listOf("perro"), dialog.cards.map { it.lemma })
        assertEquals(
            listOf("ruwiktionary/1.noun.1", "ruwiktionary/2.adjective.1", "ruwiktionary/1.noun.2"),
            dialog.cards.single().lexemes.map { it.key },
        )
        // Значения — по компонентам словаря в порядке дерева; «Род» только у
        // существительных (AWP-29, AWP-17.6).
        val noun = dialog.lexeme("perro", "ruwiktionary/1.noun.1")
        assertEquals(listOf(1L, 2L, 5L, 3L, 4L), noun.values.map { it.componentId })
        assertTrue(noun.values.all { it.isActive })
        val adjective = dialog.lexeme("perro", "ruwiktionary/2.adjective.1")
        assertEquals(listOf(1L, 2L, 4L), adjective.values.map { it.componentId })
    }

    // ===== WCL-06: «есть» по слову диалога =====

    @Test
    fun `WCL-06_1 the lexeme that the word already has is marked and skipped by the count`() {
        val dialog = ready().dialog()
        assertTrue(dialog.lexeme("run", "ru/v2").exists)
        assertEquals(1, dialog.summary.existingCount)
        // Семь лексем-кандидатов в двух разделах, одна «есть».
        assertEquals(6, dialog.summary.lexemeCount)
        assertEquals(2, dialog.summary.wordCount)
    }

    @Test
    fun `WCL-06_1 form sections are marked by the same word too`() {
        val word = CFx.word("ran", CFx.lemma("run", CFx.runLexemes, formNote = "simple past"))
        val dialog = ready(word).dialog()
        assertTrue(dialog.lexeme("run", "ru/v2").exists)
    }

    @Test
    fun `WCL-06_1 AWP-07_10 editing the core of an existing lexeme takes the mark off`() {
        val ref = CFx.ref("run", "ru/v2", CFx.TRANSLATION)
        val edited = act(ready(), CandidateAction.SubmitText(ref, "руководить")).state()
        assertFalse(edited.dialog().lexeme("run", "ru/v2").exists)
        assertEquals(0, edited.dialog().summary.existingCount)
        assertEquals(7, edited.dialog().summary.lexemeCount)
    }

    @Test
    fun `WCL-06_2 WCL-01_2 marks come from the dialog word, not from a namesake`() {
        // Одноимённое слово 8 имеет «бежать, бегать», слово диалога 7 — только «управлять».
        val form = CFx.form.copy(existingLemmas = CFx.form.existingLemmas + CFx.runWordLemma)

        val dialog = ready(form = form).dialog()

        assertFalse(dialog.lexeme("run", "ru/v1").exists)
        assertTrue(dialog.lexeme("run", "ru/v2").exists)
        assertEquals(1, dialog.summary.existingCount)
    }

    @Test
    fun `WCL-06_3 a word without lexemes gets no marks`() {
        val dialog = ready(form = CFx.emptyWordForm).dialog()
        assertTrue(dialog.cards.flatMap { it.lexemes }.none { it.exists })
        assertEquals(0, dialog.summary.existingCount)
    }

    @Test
    fun `WCL-06_4 when everything is there the button says nothing to add`() {
        val keys = CFx.runLexemes
            .flatMap { it.translations + listOfNotNull(it.gloss) }
            .map { lexemeTextKey(it) }
            .toSet()
        val existing = CFx.form.existingLemmas.first().copy(lexemeTextKeys = keys)
        val form = CFx.form.copy(existingLemmas = listOf(existing))
        val word = CFx.word("run", CFx.lemma("run", CFx.runLexemes))

        val dialog = ready(word, form = form).dialog()

        assertEquals(0, dialog.summary.lexemeCount)
        assertFalse(dialog.summary.isEnabled)
    }

    // ===== WCL-07: состав =====

    @Test
    fun `WCL-07_1 WCL-07_4 a ready dialog has the form, cards and count, no groups or targets`() {
        val dialog = ready().dialog()
        assertEquals(CandidatesPhase.READY, dialog.phase)
        assertNotNull(dialog.form)
        assertEquals(2, dialog.cards.size)
        assertTrue(dialog.summary.isEnabled)
        assertTrue(dialog.cards.all { it.target == CardTargetUi.NewWord })
    }

    @Test
    fun `WCL-07_1 perro of the contract gives one section, three lexemes and a closed mapping`() {
        val dialog = ready(CFx.perro, form = CFx.contractForm).dialog()

        assertEquals(CandidatesPhase.READY, dialog.phase)
        assertEquals(3, dialog.summary.lexemeCount)
        assertTrue(dialog.summary.isEnabled)
        assertTrue(dialog.remarks.isEmpty())
        assertTrue(dialog.componentMatchLines.isNotEmpty())
        assertFalse(dialog.isComponentMatchesExpanded)
    }

    @Test
    fun `WCL-07_1 AWP-17_6 a gender put by the answer under another option is inactive at once`() {
        val adjective = CFx.perro.lemmas.single().lexemes
            .first { it.key == "ruwiktionary/2.adjective.1" }
        val gender = CFx.byOption(CFx.C_GENDER, CFx.C_MASCULINE)
        val withGender = adjective.copy(componentMatches = adjective.componentMatches + gender)
        val word = CFx.word("perro", CFx.lemma("perro", listOf(withGender)))

        val dialog = ready(word, form = CFx.contractForm).dialog()

        val values = dialog.lexeme("perro", "ruwiktionary/2.adjective.1").values
        assertFalse(values.first { it.componentId == CFx.C_GENDER }.isActive)
        assertTrue(values.first { it.componentId == CFx.C_PART_OF_SPEECH }.isActive)
    }

    @Test
    fun `WCL-07_2 AWP-11 a lexeme the answer left without a core is marked at once`() {
        val match = CFx.byOption(CFx.PART_OF_SPEECH, CFx.VERB)
        val lexeme = CFx.lexeme("en/x1", gloss = "x", matches = listOf(match))
        val word = CFx.word("run", CFx.lemma("run", listOf(lexeme)))

        val dialog = ready(word).dialog()

        assertTrue(dialog.lexeme("run", "en/x1").lacksCore)
        assertEquals(1, dialog.summary.lacksCoreCount)
        assertFalse(dialog.summary.isEnabled)
    }

    @Test
    fun `WCL-07_2 WCL-09_5 AWP-11_5 an erased core text removes the lexeme, the button follows`() {
        var state = openTextEdit(ready())
        state = act(state, CandidateAction.SubmitText(translationRef, "   ")).state()

        // Без ядра нет лексемы: «не попадёт в ядро» в диалоге не живёт.
        val dialog = state.dialog()
        assertTrue(dialog.cards.first().lexemes.none { it.key == "ru/v1" })
        assertEquals(5, dialog.cards.first().lexemes.size)
        assertEquals(5, dialog.summary.lexemeCount)
        assertEquals(0, dialog.summary.lacksCoreCount)
        assertTrue(dialog.summary.isEnabled)
        assertNull(dialog.sheet)
        assertTrue(dialog.isDirty)
    }

    // ===== WCL-16: «Замечания» =====

    @Test
    fun `WCL-16_1 two no_translation of one word give one line with the count and language`() {
        val dialog = ready(CFx.zarpo, form = CFx.contractForm).dialog()

        assertEquals(
            listOf<RemarkUiItem>(RemarkUiItem.NoTranslation(count = 2, translationLanguage = "ru")),
            dialog.remarks,
        )
        // Обе лексемы с пометкой «англ.» (AWP-34).
        assertTrue(dialog.cards.single().lexemes.all { it.isEnglishFallback })
    }

    @Test
    fun `WCL-16_2 a recommendation of the answer gives a line with name, template, options`() {
        val dialog = ready(
            word = CFx.perro,
            form = CFx.contractForm,
            recommendations = listOf(CFx.genderRecommendation),
        ).dialog()

        val line = RemarkUiItem.ComponentRecommended(
            name = "Род",
            template = ComponentTemplate.CHOICE,
            options = listOf("м.р.", "ж.р."),
        )
        assertEquals(listOf<RemarkUiItem>(line), dialog.remarks)
    }

    @Test
    fun `WCL-16_3 no remarks and no recommendations give no section`() {
        assertTrue(ready(CFx.perro, form = CFx.contractForm).dialog().remarks.isEmpty())
    }

    @Test
    fun `WCL-16_1 an unclear component gives one line per component of the form`() {
        val unclear = WordRemarkUi.ComponentUnclear(CFx.C_GLOSS)
        val unknown = WordRemarkUi.ComponentUnclear(99L)
        val word = CFx.perro.copy(remarks = listOf(unclear, unclear, unknown))

        val dialog = ready(word, form = CFx.contractForm).dialog()

        val line = RemarkUiItem.ComponentUnclear(CFx.C_GLOSS)
        assertEquals(listOf<RemarkUiItem>(line), dialog.remarks)
    }

    // ===== WCL-17: «Сопоставление» =====

    @Test
    fun `WCL-17_1 WCL-17_2 perro gives mapping lines in the order of components, no remarks`() {
        val dialog = ready(CFx.perro, form = CFx.contractForm).dialog()

        fun field(
            field: String,
            componentId: Long,
        ) = ComponentMatchLineUiItem.FieldToComponent(field, componentId)

        fun option(
            componentId: Long,
            optionId: Long,
        ) = ComponentMatchLineUiItem.ComponentOption(componentId, optionId)

        assertEquals(
            listOf(
                field("translations", CFx.C_TRANSLATION),
                option(CFx.C_PART_OF_SPEECH, CFx.C_NOUN),
                option(CFx.C_PART_OF_SPEECH, CFx.C_ADJECTIVE),
                option(CFx.C_GENDER, CFx.C_MASCULINE),
                field("examples", CFx.C_EXAMPLE),
                field("gloss", CFx.C_GLOSS),
            ),
            dialog.componentMatchLines,
        )
        assertTrue(dialog.remarks.isEmpty())
    }

    @Test
    fun `WCL-17_1 AWP-31_3 deleting a lexeme leaves the mapping, a look at it is no change`() {
        val ready = ready(CFx.perro, form = CFx.contractForm)
        val lines = ready.dialog().componentMatchLines

        val deleted = act(ready, CandidateAction.DeleteLexeme("perro", "ruwiktionary/1.noun.1"))
        assertEquals(lines, deleted.state().dialog().componentMatchLines)

        val expanded = act(ready, CandidateAction.ToggleComponentMatches).state()
        assertTrue(expanded.dialog().isComponentMatchesExpanded)
        val collapsed = act(expanded, CandidateAction.ToggleComponentMatches).state()
        assertFalse(collapsed.dialog().isComponentMatchesExpanded)
    }

    // ===== WCL-09: действия =====

    @Test
    fun `WCL-09_1 deleting a lexeme leaves five rows and marks changes`() {
        val result = act(ready(), CandidateAction.DeleteLexeme("run", "ru/v2"))
        val dialog = result.state().dialog()
        assertEquals(5, dialog.cards.first().lexemes.size)
        assertTrue(dialog.isDirty)
        result.assertNoEffects()
    }

    @Test
    fun `WCL-09_1 AWP-15_2 deleting one example leaves the other and the lexeme`() {
        val ref = CFx.ref("run", "en/v1", CFx.EXAMPLE, index = 1)

        val result = act(ready(), CandidateAction.DeleteValue(ref))

        val dialog = result.state().dialog()
        assertEquals(listOf("Run!"), dialog.lexeme("run", "en/v1").texts(CFx.EXAMPLE))
        assertTrue(dialog.isDirty)
        assertFalse(dialog.lexeme("run", "en/v1").lacksCore)
        result.assertNoEffects()
    }

    @Test
    fun `WCL-09_1 AWP-11_5 deleting the only core text removes the lexeme, the count follows`() {
        val ref = CFx.ref("run", "ru/v2", CFx.TRANSLATION)

        val dialog = act(ready(), CandidateAction.DeleteValue(ref)).state().dialog()

        // Без ядра нет лексемы; «есть» была у неё — теперь «есть» нет, к записи остальные.
        assertTrue(dialog.cards.first().lexemes.none { it.key == "ru/v2" })
        assertEquals(5, dialog.cards.first().lexemes.size)
        assertEquals(0, dialog.summary.existingCount)
        assertEquals(6, dialog.summary.lexemeCount)
        assertEquals(0, dialog.summary.lacksCoreCount)
        assertTrue(dialog.summary.isEnabled)
        assertTrue(dialog.isDirty)
    }

    @Test
    fun `WCL-09_1 AWP-11_5 while another core has a text the lexeme stays, the last one removes it`() {
        // «en/v1» с двумя ядрами: «Перевод» и «Definition».
        val translation = CFx.ref("run", "en/v1", CFx.TRANSLATION)
        val definition = CFx.ref("run", "en/v1", CFx.DEFINITION)

        val first = act(ready(), CandidateAction.DeleteValue(translation)).state()

        val lexeme = first.dialog().lexeme("run", "en/v1")
        assertTrue(lexeme.values.none { it.componentId == CFx.TRANSLATION })
        assertFalse(lexeme.lacksCore)
        assertEquals(6, first.dialog().summary.lexemeCount)

        val second = act(first, CandidateAction.DeleteValue(definition)).state().dialog()

        assertTrue(second.cards.first().lexemes.none { it.key == "en/v1" })
        assertEquals(5, second.summary.lexemeCount)
        assertTrue(second.summary.isEnabled)
    }

    @Test
    fun `WCL-09_1 AWP-17_6 deleting the only example switches the dependent note off`() {
        val ready = ready(CFx.noteWord)
        assertTrue(ready.dialog().lexeme("run", "ru/n2").isActive(CFx.NOTE))
        val ref = CFx.ref("run", "ru/n2", CFx.EXAMPLE)

        val dialog = act(ready, CandidateAction.DeleteValue(ref)).state().dialog()

        // Пример не ядро: значение ушло, «Заметка» погасла, лексема на месте.
        val lexeme = dialog.lexeme("run", "ru/n2")
        assertTrue(lexeme.values.none { it.componentId == CFx.EXAMPLE })
        assertFalse(lexeme.isActive(CFx.NOTE))
        assertEquals(listOf("бег"), lexeme.texts(CFx.TRANSLATION))
    }

    @Test
    fun `WCL-09_2 AWP-17_6 an erased example switches the dependent note off, another text not`() {
        val ref = CFx.ref("run", "ru/n2", CFx.EXAMPLE)

        // Другой текст примера зависимую «Заметку» не трогает.
        val changed = act(ready(CFx.noteWord), CandidateAction.SubmitText(ref, "вечерний бег"))
            .state()
        assertTrue(changed.dialog().lexeme("run", "ru/n2").isActive(CFx.NOTE))

        val erased = act(changed, CandidateAction.SubmitText(ref, "   ")).state()

        val lexeme = erased.dialog().lexeme("run", "ru/n2")
        assertTrue(lexeme.values.none { it.componentId == CFx.EXAMPLE })
        assertFalse(lexeme.isActive(CFx.NOTE))
        assertEquals(listOf("бег"), lexeme.texts(CFx.TRANSLATION))
    }

    @Test
    fun `WCL-09_2 WCL-02_1 an edited text is trimmed and the batch carries what is left`() {
        var state = openTextEdit(ready())
        state = act(state, CandidateAction.UpdateDraft("  бежать  ")).state()
        state = act(state, CandidateAction.SubmitText(translationRef, "  бежать  ")).state()
        val edited = state.dialog().lexeme("run", "ru/v1")
        assertEquals(listOf("бежать"), edited.texts(CFx.TRANSLATION))
        assertTrue(edited.isEdited)
        assertNull(state.dialog().sheet)
        for (key in listOf("ru/v2", "ru/v3", "en/g1", "en/v1", "ru/n1")) {
            state = act(state, CandidateAction.DeleteLexeme("run", key)).state()
        }
        state = act(state, CandidateAction.DeleteCard("rin")).state()

        val word = addEffect(state).batch.words.single()

        assertEquals(CFx.WORD, word.wordId)
        assertEquals(
            listOf(
                BatchComponent(CFx.TRANSLATION, TextValues(Primitive.Text("бежать"))),
                BatchComponent(CFx.PART_OF_SPEECH, ChoiceValues(CFx.VERB)),
            ),
            word.lexemes.single().components,
        )
    }

    @Test
    fun `WCL-09_3 an option changes one lexeme only and marks changes`() {
        val dialog = act(ready(), CandidateAction.SetOption(verbRef, CFx.NOUN)).state().dialog()

        assertEquals(listOf(CFx.NOUN), dialog.lexeme("run", "ru/v1").options(CFx.PART_OF_SPEECH))
        assertEquals(listOf(CFx.VERB), dialog.lexeme("run", "ru/v2").options(CFx.PART_OF_SPEECH))
        assertTrue(dialog.isDirty)
    }

    @Test
    fun `WCL-09_3 AWP-17_6 AWP-17_7 a part of speech switches the dependent gender off and on`() {
        val ref = CFx.componentRef("perro", "ruwiktionary/1.noun.1", CFx.C_PART_OF_SPEECH)
        val isGenderActive = { state: WordCardState ->
            val lexeme = state.dialog().lexeme("perro", "ruwiktionary/1.noun.1")
            lexeme.values.first { it.componentId == CFx.C_GENDER }.isActive
        }
        val noun = ready(CFx.perro, form = CFx.contractForm)
        assertTrue(isGenderActive(noun))

        val verb = act(noun, CandidateAction.SetOption(ref, CFx.C_VERB)).state()
        assertFalse(isGenderActive(verb))

        val back = act(verb, CandidateAction.SetOption(ref, CFx.C_NOUN)).state()
        assertTrue(isGenderActive(back))
        // «Выбор» — не ядро: «есть» и «не попадёт в ядро» не трогаются.
        assertEquals(3, back.dialog().summary.lexemeCount)
        assertEquals(0, back.dialog().summary.lacksCoreCount)
    }

    @Test
    fun `WCL-09_2 AWP-16_8 the sheet takes the draft and the reset flag from the state`() {
        val opened = openTextEdit(ready())
        val original = CandidatesSheetUi.TextEdit(
            ref = translationRef,
            draft = "бежать, бегать",
            canReset = false,
        )
        assertEquals(original, opened.dialog().sheet)

        val typed = act(opened, CandidateAction.UpdateDraft("бежать")).state()
        val changed = CandidatesSheetUi.TextEdit(translationRef, "бежать", canReset = true)
        assertEquals(changed, typed.dialog().sheet)
        val typedBack = act(typed, CandidateAction.UpdateDraft("бежать, бегать")).state()
        assertEquals(original, typedBack.dialog().sheet)

        // Края не в счёт: «Сохранить» их обрежет, значит текст ответа вернулся.
        val padded = act(typed, CandidateAction.UpdateDraft("  бежать, бегать  ")).state()
        assertEquals(original.copy(draft = "  бежать, бегать  "), padded.dialog().sheet)

        // Правленый текст: при следующем открытии «сбросить» уже есть до ввода.
        val saved = act(typed, CandidateAction.SubmitText(translationRef, "бежать")).state()
        assertEquals(changed, openTextEdit(saved).dialog().sheet)
    }

    @Test
    fun `WCL-09_2 AWP-15_6 reset of an erased example is no step, the dependent note stays off`() {
        val ref = CFx.ref("run", "ru/n2", CFx.EXAMPLE)
        val erased = act(ready(CFx.noteWord), CandidateAction.SubmitText(ref, "   ")).state()
        assertFalse(erased.dialog().lexeme("run", "ru/n2").isActive(CFx.NOTE))

        val reset = act(erased, CandidateAction.ResetText(ref))

        // Отмены удаления нет: значения уже нет, «сбросить» по нему ничего не меняет.
        assertEquals(erased, reset.state())
        reset.assertNoEffects()
        val lexeme = reset.state().dialog().lexeme("run", "ru/n2")
        assertTrue(lexeme.values.none { it.componentId == CFx.EXAMPLE })
        assertFalse(lexeme.isActive(CFx.NOTE))
    }

    @Test
    fun `WCL-09_2 AWP-16_6 reset returns the text of the answer and closes the sheet`() {
        var state = openTextEdit(ready())
        state = act(state, CandidateAction.SubmitText(translationRef, "бежать")).state()
        state = openTextEdit(state)

        state = act(state, CandidateAction.ResetText(translationRef)).state()

        val dialog = state.dialog()
        val texts = dialog.lexeme("run", "ru/v1").texts(CFx.TRANSLATION)
        assertEquals(listOf("бежать, бегать"), texts)
        assertFalse(dialog.lexeme("run", "ru/v1").isEdited)
        assertNull(dialog.sheet)
    }

    @Test
    fun `WCL-09_3 a sheet of options opens as it is and the choice closes it`() {
        val sheet = CandidatesSheetUi.Option(verbRef)

        val opened = act(ready(), CandidateAction.OpenSheet(sheet)).state()
        assertEquals(sheet, opened.dialog().sheet)

        val chosen = act(opened, CandidateAction.SetOption(verbRef, CFx.NOUN)).state()
        assertNull(chosen.dialog().sheet)
    }

    // ===== WCL-10: запись =====

    @Test
    fun `WCL-10_1 WCL-10_4 WCL-01_2 add writes without a question into the word, no group`() {
        val result = act(ready(), CandidateAction.RequestAdd)
        assertTrue(result.state().dialog().isAdding)
        val effect = result.second.single() as DatasourceEffect.AddCandidates
        assertEquals(CFx.WORD, effect.wordId)
        assertFalse(effect.isTransfer)
        val word = effect.batch.words.single()
        assertEquals(CFx.WORD, word.wordId)
        assertEquals("run", word.text)
        assertNull(word.groupId)
        assertEquals(CFx.DICT, effect.batch.dictionaryId)
    }

    @Test
    fun `WCL-10_2 WCL-02_2 the existing lexeme is not in the batch, the others have components`() {
        val lexemes = addEffect(ready()).batch.words.single().lexemes

        assertEquals(6, lexemes.size)
        val components = lexemes.flatMap { it.components }
        assertTrue(components.none { it.data == TextValues(Primitive.Text("управлять")) })
        // «en/v1»: перевод, часть речи, два примера с подписью источника, толкование (AWP-32).
        fun example(text: String) =
            CaptionedTextValues(Primitive.Text(text), Primitive.Text("Wiktionary"))
        val translation = TextValues(Primitive.Text("бегать, побегать"))
        val definition = TextValues(Primitive.Text("To move forward quickly."))
        val lexeme = lexemes.first { lexeme -> lexeme.components.any { it.data == translation } }
        assertEquals(
            listOf(
                BatchComponent(CFx.TRANSLATION, translation),
                BatchComponent(CFx.PART_OF_SPEECH, ChoiceValues(CFx.VERB)),
                BatchComponent(CFx.EXAMPLE, example("Run!")),
                BatchComponent(CFx.EXAMPLE, example("Run, Sarah!")),
                BatchComponent(CFx.DEFINITION, definition),
            ),
            lexeme.components,
        )
    }

    @Test
    fun `WCL-10_1 after the write the dialog closes and the count is shown`() {
        val adding = act(ready(), CandidateAction.RequestAdd).state()

        val result = send(adding, Msg.CandidatesAdded(lexemeCount = 6, requestId = adding.runId()))

        assertNull(result.state().candidates)
        val count = UiEffect.ShowCountSnackbar(messageRes = R.plurals.candidates_added, count = 6)
        result.assertEffects(setOf(count))
    }

    // ===== WCL-11: ошибки =====

    @Test
    fun `WCL-11_2 a refusal shows its text without retry`() {
        val open = opened()

        val errorRes = R.string.photo_words_error_rate_limited
        val result = send(open, Msg.CandidatesLookupFailed(errorRes, open.runId()))

        val dialog = result.state().dialog()
        assertEquals(CandidatesPhase.ERROR, dialog.phase)
        assertEquals(R.string.photo_words_error_rate_limited, dialog.errorRes)
        assertFalse(dialog.hasRetryButton)
        result.assertNoEffects()
    }

    @Test
    fun `WCL-11_7 WCL-11_9 a word the server failed on shows the unavailable text with retry`() {
        val open = opened()
        val run = open.runId()

        val errorRes = R.string.scan_server_unavailable
        val result = send(open, Msg.CandidatesLookupUnavailable(errorRes, run))

        val dialog = result.state().dialog()
        assertEquals(CandidatesPhase.ERROR, dialog.phase)
        assertEquals(R.string.scan_server_unavailable, dialog.errorRes)
        assertTrue(dialog.hasRetryButton)
        result.assertNoEffects()
        // «Повторить» — тот же запрос под новым номером запуска.
        val retry = act(result.state(), CandidateAction.RetryLookup)
        retry.assertEffects(setOf(DatasourceEffect.LookupCandidates(CFx.WORD, run + 1)))
    }

    @Test
    fun `WCL-11_3 WCL-11_6 WCL-11_9 an unreachable or unavailable server shows text and retry`() {
        val open = opened()

        val texts = listOf(R.string.photo_words_error_server, R.string.scan_server_unavailable)
        for (errorRes in texts) {
            val result = send(open, Msg.CandidatesLookupUnavailable(errorRes, open.runId()))

            val dialog = result.state().dialog()
            assertEquals(CandidatesPhase.ERROR, dialog.phase)
            assertEquals(errorRes, dialog.errorRes)
            assertTrue(dialog.hasRetryButton)
            result.assertNoEffects()
        }
    }

    @Test
    fun `WCL-11_9 retry asks again under a new run, drops the old answer and takes the new`() {
        val open = opened()
        val first = open.runId()
        val unavailable = Msg.CandidatesLookupUnavailable(R.string.photo_words_error_server, first)
        val down = send(open, unavailable).state()

        val retry = act(down, CandidateAction.RetryLookup)

        val dialog = retry.state().dialog()
        assertEquals(CandidatesPhase.LOADING, dialog.phase)
        assertNull(dialog.errorRes)
        assertFalse(dialog.hasRetryButton)
        assertEquals(first + 1, dialog.requestId)
        assertEquals(first + 1, retry.state().candidatesRun)
        retry.assertEffects(setOf(DatasourceEffect.LookupCandidates(CFx.WORD, first + 1)))

        val stale = send(retry.state(), Msg.CandidatesFound(CFx.runWord, emptyList(), first))
        assertEquals(retry.state(), stale.state())
        stale.assertNoEffects()
        val fresh = send(retry.state(), Msg.CandidatesFound(CFx.runWord, emptyList(), first + 1))
        assertEquals(listOf(CFx.runWord), fresh.state().dialog().words)
    }

    @Test
    fun `WCL-11_9 a failure after retry shows retry again, a refusal after it does not`() {
        val open = opened()
        val server = R.string.photo_words_error_server
        val down = send(open, Msg.CandidatesLookupUnavailable(server, open.runId())).state()
        val retried = act(down, CandidateAction.RetryLookup).state()

        val unavailable = R.string.scan_server_unavailable
        val again = send(retried, Msg.CandidatesLookupUnavailable(unavailable, retried.runId()))
            .state()
        assertTrue(again.dialog().hasRetryButton)

        val retriedTwice = act(again, CandidateAction.RetryLookup).state()
        val noAccess = R.string.photo_words_error_no_access
        val refusal = Msg.CandidatesLookupFailed(noAccess, retriedTwice.runId())
        val denied = send(retriedTwice, refusal).state()
        assertFalse(denied.dialog().hasRetryButton)
        assertEquals(R.string.photo_words_error_no_access, denied.dialog().errorRes)
    }

    @Test
    fun `WCL-11_4 an answer after the dialog closed is dropped`() {
        val open = opened()
        val run = open.runId()
        val closed = act(open, CandidateAction.RequestClose).state()
        assertNull(closed.candidates)

        val result = send(closed, Msg.CandidatesFound(CFx.runWord, emptyList(), run))

        assertNull(result.state().candidates)
        result.assertNoEffects()
    }

    @Test
    fun `WCL-11_8 closing during the load and opening again takes the second answer only`() {
        val first = opened()
        val firstRun = first.runId()
        val closed = act(first, CandidateAction.RequestClose).state()
        val second = send(closed, Msg.OpenCandidates).state()
        val secondRun = second.runId()
        assertEquals(firstRun + 1, secondRun)

        val stale = send(second, Msg.CandidatesFound(CFx.runWord, emptyList(), firstRun))
        assertEquals(second, stale.state())
        stale.assertNoEffects()

        val accepted = send(second, Msg.CandidatesFound(CFx.runWord, emptyList(), secondRun))
        assertEquals(listOf(CFx.runWord), accepted.state().dialog().words)
    }

    @Test
    fun `WCL-11_4 every answer of the dialog carries the run, a foreign one changes nothing`() {
        val ready = ready()
        val foreign = ready.runId() + 1
        val foreignAnswers = listOf(
            Msg.CandidatesFound(CFx.runWord, emptyList(), foreign),
            Msg.CandidatesNotFound(foreign),
            Msg.CandidatesLookupFailed(R.string.candidates_word_missing, foreign),
            Msg.CandidatesLookupUnavailable(R.string.photo_words_error_server, foreign),
            Msg.CandidatesFormLoaded(CFx.form, foreign),
            Msg.CandidatesFormFailed(R.string.photo_add_form_failed, foreign),
            Msg.CandidatesAdded(1, foreign),
            Msg.CandidatesTransferred(foreign),
            Msg.CandidatesWordGone(AddFailureUi.WordMissing(CFx.WORD), foreign),
            Msg.CandidatesComponentGone(AddFailureUi.TypeUnavailable(CFx.DEFINITION), foreign),
            Msg.CandidatesAddFailed(AddFailureUi.Failed, foreign),
        )
        for (message in foreignAnswers) {
            val result = send(ready, message)
            assertEquals(message.toString(), ready, result.state())
            result.assertNoEffects()
        }
    }

    @Test
    fun `WCL-11_5 WCL-01_3 a failed form or a missing word shows the reason without retry`() {
        val open = opened()
        val run = open.runId()

        val failed = send(open, Msg.CandidatesFormFailed(R.string.photo_add_form_failed, run))
        assertEquals(R.string.photo_add_form_failed, failed.state().dialog().errorRes)
        assertFalse(failed.state().dialog().hasRetryButton)

        val gone = send(open, Msg.CandidatesLookupFailed(R.string.candidates_word_missing, run))
        assertEquals(R.string.candidates_word_missing, gone.state().dialog().errorRes)
        assertFalse(gone.state().dialog().hasRetryButton)

        val noDictionary = Msg.CandidatesFormFailed(R.string.photo_add_dictionary_missing, run)
        val missing = send(open, noDictionary).state()
        assertEquals(R.string.photo_add_dictionary_missing, missing.dialog().errorRes)
    }

    // ===== WCL-12: закрытие =====

    @Test
    fun `WCL-12_1 WCL-12_6 closing without edits or after a look at the mapping is at once`() {
        act(ready(), CandidateAction.RequestClose).let { result ->
            assertNull(result.state().candidates)
            result.assertNoEffects()
        }
        val ready = ready(CFx.perro, form = CFx.contractForm)
        val opened = act(ready, CandidateAction.ToggleComponentMatches).state()
        val looked = act(opened, CandidateAction.ToggleComponentMatches).state()
        assertFalse(looked.dialog().isDirty)
        assertNull(act(looked, CandidateAction.RequestClose).state().candidates)
    }

    @Test
    fun `WCL-12_2 closing after a deletion asks, stay keeps everything, close writes nothing`() {
        val deleted = act(ready(), CandidateAction.DeleteLexeme("run", "ru/v1")).state()
        val asking = act(deleted, CandidateAction.RequestClose).state()
        assertTrue(asking.dialog().isConfirmingClose)
        val stayed = act(asking, CandidateAction.Stay).state()
        assertFalse(stayed.dialog().isConfirmingClose)
        assertEquals(5, stayed.dialog().cards.first().lexemes.size)
        val closed = act(asking, CandidateAction.ConfirmClose)
        assertNull(closed.state().candidates)
        closed.assertNoEffects()
    }

    @Test
    fun `WCL-12_5 saving an unchanged text or the same option does not ask on close`() {
        var state = openTextEdit(ready())
        state = act(state, CandidateAction.SubmitText(translationRef, "  бежать, бегать  ")).state()
        state = act(state, CandidateAction.SetOption(verbRef, CFx.VERB)).state()
        state = act(state, CandidateAction.ResetText(translationRef)).state()
        assertFalse(state.dialog().isDirty)
        assertNull(act(state, CandidateAction.RequestClose).state().candidates)
    }

    @Test
    fun `WCL-12_5 a changed text or option asks on close`() {
        val edited = act(ready(), CandidateAction.SubmitText(translationRef, "бежать")).state()
        assertTrue(edited.dialog().isDirty)
        assertTrue(act(edited, CandidateAction.RequestClose).state().dialog().isConfirmingClose)

        val changed = act(ready(), CandidateAction.SetOption(verbRef, CFx.NOUN)).state()
        assertTrue(changed.dialog().isDirty)
        assertTrue(act(changed, CandidateAction.RequestClose).state().dialog().isConfirmingClose)
    }

    // ===== WCL-13: запись не удалась =====

    @Test
    fun `WCL-13_1 a failed write keeps the dialog and retry writes again without a question`() {
        val adding = act(ready(), CandidateAction.RequestAdd).state()

        val message = Msg.CandidatesAddFailed(AddFailureUi.Failed, adding.runId())
        val failed = send(adding, message).state()

        assertFalse(failed.dialog().isAdding)
        assertEquals(AddFailureUi.Failed, failed.dialog().addFailure)
        assertEquals(2, failed.dialog().cards.size)
        val retry = act(failed, CandidateAction.RetryAdd)
        assertTrue(retry.state().dialog().isAdding)
        assertNull(retry.state().dialog().addFailure)
        assertTrue(retry.second.single() is DatasourceEffect.AddCandidates)
    }

    @Test
    fun `WCL-13_1 the other rejections show not added with retry and keep the dialog`() {
        val adding = act(ready(), CandidateAction.RequestAdd).state()

        val failures = listOf(
            AddFailureUi.GroupMissing(5L),
            AddFailureUi.NoCoreValue,
            AddFailureUi.DictionaryMissing,
        )
        for (failure in failures) {
            val failed = send(adding, Msg.CandidatesAddFailed(failure, adding.runId())).state()
            assertEquals(CandidatesPhase.READY, failed.dialog().phase)
            assertEquals(failure, failed.dialog().addFailure)
            assertFalse(failed.dialog().isAdding)
        }
    }

    @Test
    fun `WCL-13_2 a deleted word shows the reason without retry`() {
        val adding = act(ready(), CandidateAction.RequestAdd).state()

        val gone = Msg.CandidatesWordGone(AddFailureUi.WordMissing(CFx.WORD), adding.runId())
        val result = send(adding, gone).state()

        assertEquals(CandidatesPhase.ERROR, result.dialog().phase)
        assertEquals(R.string.candidates_word_missing, result.dialog().errorRes)
        assertFalse(result.dialog().hasRetryButton)
        assertFalse(result.dialog().isAdding)
        assertNull(result.dialog().addFailure)
    }

    @Test
    fun `WCL-13_3 a disabled component leaves the form and the values, the marks are recounted`() {
        val adding = act(ready(), CandidateAction.RequestAdd).state()
        val failure = AddFailureUi.TypeUnavailable(CFx.DEFINITION)

        val dialog = send(adding, Msg.CandidatesComponentGone(failure, adding.runId()))
            .state()
            .dialog()

        assertTrue(dialog.form!!.components.none { it.id == CFx.DEFINITION })
        val values = dialog.cards.flatMap { it.lexemes }.flatMap { it.values }
        assertTrue(values.none { it.componentId == CFx.DEFINITION })
        assertEquals(failure, dialog.addFailure)
        assertFalse(dialog.isAdding)
        // «en/g1» и «en/rin1» имели ядро только в «Definition»: теперь «не попадут в ядро».
        assertTrue(dialog.lexeme("run", "en/g1").lacksCore)
        assertTrue(dialog.lexeme("rin", "en/rin1").lacksCore)
        assertEquals(2, dialog.summary.lacksCoreCount)
        assertFalse(dialog.summary.isEnabled)
    }

    @Test
    fun `WCL-13_3 AWP-21_6 a disabled part of speech takes its dependent gender off with it`() {
        val adding = act(ready(CFx.perro, form = CFx.contractForm), CandidateAction.RequestAdd)
            .state()
        val failure = AddFailureUi.TypeUnavailable(CFx.C_PART_OF_SPEECH)

        val dialog = send(adding, Msg.CandidatesComponentGone(failure, adding.runId()))
            .state()
            .dialog()

        assertEquals(listOf(1L, 5L, 3L, 4L), dialog.form!!.components.map { it.id })
        val noun = dialog.lexeme("perro", "ruwiktionary/1.noun.1")
        assertEquals(listOf(1L, 5L, 3L, 4L), noun.values.map { it.componentId })
        // «Род» зависел от опции «существительное»: без «Части речи» он неактивен и не пишется.
        assertFalse(noun.values.first { it.componentId == CFx.C_GENDER }.isActive)
        assertEquals(failure, dialog.addFailure)
        assertTrue(dialog.summary.isEnabled)
    }

    // ===== WCL-15: переименование или перенос в лемму =====

    private fun rename(lemma: String) =
        CandidateAction.ToggleLemmaWord(LemmaWordChoice.Rename(lemma))

    private fun transfer(
        lemma: String,
        wordId: Long,
    ) = CandidateAction.ToggleLemmaWord(LemmaWordChoice.Transfer(lemma, wordId))

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
        val chosen = act(readyRunning(), rename("run")).state()
        val effect = addEffect(chosen)
        val word = effect.batch.words.single()
        assertEquals(CFx.WORD, word.wordId)
        assertEquals("run", word.renameTo)
        assertNull(word.absorbWordId)
        assertFalse(effect.isTransfer)
        // «бежать, бегать» есть у слова 8, но при переименовании пишется: слова разные.
        assertEquals(3, word.lexemes.size)
        assertNull(addEffect(ready(CFx.runningWord)).batch.words.single().renameTo)
    }

    @Test
    fun `WCL-15_1 WCL-15_5 a second toggle clears, another form or word replaces the choice`() {
        val state = ready(CFx.ranWord)
        val run = act(state, rename("run")).state()
        assertEquals(LemmaWordChoice.Rename("run"), run.dialog().lemmaWordChoice)
        val rin = act(run, rename("rin")).state()
        assertEquals(LemmaWordChoice.Rename("rin"), rin.dialog().lemmaWordChoice)
        assertNull(act(rin, rename("rin")).state().dialog().lemmaWordChoice)

        val twins = readyRunning(CFx.formWithTwoRunWords)
        assertNull(act(twins, transfer("run", 99L)).state().dialog().lemmaWordChoice)
        val first = act(twins, transfer("run", 8L)).state()
        assertEquals(LemmaWordChoice.Transfer("run", 8L), first.dialog().lemmaWordChoice)
        val second = act(first, transfer("run", 9L)).state()
        assertEquals(LemmaWordChoice.Transfer("run", 9L), second.dialog().lemmaWordChoice)
        val renamed = act(second, rename("run")).state()
        assertEquals(LemmaWordChoice.Rename("run"), renamed.dialog().lemmaWordChoice)
        assertNull(act(second, transfer("run", 9L)).state().dialog().lemmaWordChoice)
    }

    @Test
    fun `WCL-15_3 the transfer batch targets the lemma word, absorbs the dialog word`() {
        val chosen = act(readyRunning(), transfer("run", 8L)).state()
        val effect = addEffect(chosen)
        val word = effect.batch.words.single()
        assertEquals(8L, word.wordId)
        assertEquals(CFx.WORD, word.absorbWordId)
        assertNull(word.renameTo)
        assertTrue(effect.isTransfer)
        // Четыре кандидата: «управлять» есть у слова диалога, «бежать, бегать» — у слова 8.
        assertEquals(2, word.lexemes.size)
    }

    @Test
    fun `WCL-15_4 values of the lemma word are marked, counted and shrink the button`() {
        val state = readyRunning()
        val dialog = state.dialog()
        assertTrue(dialog.lexeme("run", "ru/v1").existsInLemma)
        assertTrue(dialog.lexeme("run", "ru/v2").existsInLemma)
        assertFalse(dialog.lexeme("run", "ru/v3").existsInLemma)
        assertFalse(dialog.lexeme("running", "ru/a1").existsInLemma)
        // Два кандидата плюс «управлять» самого слова «running».
        assertEquals(mapOf("run" to 3), dialog.sameValues)
        assertEquals(3, dialog.summary.lexemeCount)

        val chosen = act(state, transfer("run", 8L)).state()
        assertEquals(2, chosen.dialog().summary.lexemeCount)
        assertEquals(2, chosen.dialog().summary.existingCount)
        assertEquals(3, act(chosen, rename("run")).state().dialog().summary.lexemeCount)

        val deleted = act(state, CandidateAction.DeleteLexeme("run", "ru/v1")).state()
        assertEquals(mapOf("run" to 2), deleted.dialog().sameValues)
        assertTrue(deleted.dialog().lexeme("run", "ru/v2").exists)
        val withoutRunWord = readyRunning(CFx.runningForm)
        assertEquals(emptyMap<String, Int>(), withoutRunWord.dialog().sameValues)
    }

    @Test
    fun `WCL-15_3 after the transfer the dialog closes and the card leaves`() {
        val chosen = act(readyRunning(), transfer("run", 8L)).state()
        val adding = act(chosen, CandidateAction.RequestAdd).state()

        val result = send(adding, Msg.CandidatesTransferred(adding.runId()))

        assertNull(result.state().candidates)
        assertTrue(result.state().isExiting)
        assertTrue(result.second.any { it is NavigationEffect.Back })
        send(adding, Msg.CandidatesTransferred(999L)).assertNoEffects()
    }

    @Test
    fun `WCL-15_6 a transfer rejected for a vanished word shows the reason, keeps the card`() {
        val chosen = act(readyRunning(), transfer("run", 8L)).state()
        val adding = act(chosen, CandidateAction.RequestAdd).state()

        val gone = Msg.CandidatesWordGone(AddFailureUi.WordMissing(8L), adding.runId())
        val result = send(adding, gone).state()

        assertEquals(CandidatesPhase.ERROR, result.dialog().phase)
        assertEquals(R.string.candidates_word_missing, result.dialog().errorRes)
        assertFalse(result.dialog().isAdding)
        assertFalse(result.isExiting)
    }
}
