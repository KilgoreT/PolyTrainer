package me.apomazkin.scancandidates.mate

import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.assertNoEffects
import io.github.kilgoret.mate.test.testReduce
import me.apomazkin.candidates.CandidatesSheetUi
import me.apomazkin.candidates.ComponentMatchLineUiItem
import me.apomazkin.candidates.RemarkUiItem
import me.apomazkin.candidates.WordRemarkUi
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.DependencyTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Значения лексемы-кандидата по компонентам глазами редьюсера: что показано
 * (AWP-29, AWP-34), «Замечания» и «Сопоставление» (AWP-30, AWP-31), правка
 * текста (AWP-16, AWP-11), опции «Выбора» (AWP-17). Адреса строк спеки —
 * `AWP-NN_k`.
 */
class ValuesReducerTest {
    private val reducer = reducer()

    private val runKey = "ruwiktionary/1.verb.1"
    private val translationOfRun = Fx.ref("run", runKey, Fx.TRANSLATION)

    // ===== Значения по компонентам (AWP-29) =====

    @Test
    fun `AWP-29_1 perro noun shows its values in the order of components`() {
        val window = reducer.window(Fx.perro)

        assertEquals(
            listOf(
                "Перевод: собака; пёс (также ругательство)",
                "Часть речи: существительное",
                "Род: м.р.",
                "Пример: perro callejero",
                "Толкование: dog (the species Canis familiaris …)",
            ),
            window.shown("perro", "ruwiktionary/1.noun.1"),
        )
    }

    @Test
    fun `AWP-29_2 corrian verb shows translation, verb and gloss and no gender`() {
        val window = reducer.window(Fx.corrian)

        assertEquals(
            listOf(
                "Перевод: течь, литься, струиться (о жидкости)",
                "Часть речи: глагол",
                "Толкование: to flow",
            ),
            window.shown("correr", "ruwiktionary/1.verb.3"),
        )
    }

    @Test
    fun `AWP-29_3 zarpo puts the english gloss into the translation and the gloss`() {
        val window = reducer.window(Fx.zarpo)

        assertEquals(
            listOf(
                "Перевод: to embark; to set sail",
                "Часть речи: глагол",
                "Толкование: to embark; to set sail",
            ),
            window.shown("zarpar", "enwiktionary/1.verb.1"),
        )
    }

    @Test
    fun `AWP-29_9 a list field in a single value component is joined with a comma`() {
        val window = reducer.window(Fx.runEnglish)

        assertEquals(
            listOf("Перевод: бегать, побегать, бежать, побежать"),
            window.shown("run", "enwiktionary/1.verb.1"),
        )
    }

    @Test
    fun `AWP-29_12 a component switched off after the request has no row`() {
        val components = listOf(Fx.translation, Fx.partOfSpeech, Fx.gender, Fx.example)

        val window = reducer.window(Fx.perro, form = Fx.form(components = components))

        assertEquals(
            listOf(
                "Перевод: собака; пёс (также ругательство)",
                "Часть речи: существительное",
                "Род: м.р.",
                "Пример: perro callejero",
            ),
            window.shown("perro", "ruwiktionary/1.noun.1"),
        )
    }

    @Test
    fun `AWP-29_13 an option deleted after the request leaves the gender without a value`() {
        val options = Fx.options + (Fx.GENDER to Fx.options.getValue(Fx.GENDER).drop(1))

        val window = reducer.window(Fx.perro, form = Fx.form(options = options))

        assertEquals(
            listOf(
                "Перевод: собака; пёс (также ругательство)",
                "Часть речи: существительное",
                "Род: без значения",
                "Пример: perro callejero",
                "Толкование: dog (the species Canis familiaris …)",
            ),
            window.shown("perro", "ruwiktionary/1.noun.1"),
        )
    }

    // ===== Пометка «англ.» (AWP-34) =====

    @Test
    fun `AWP-34_1 both lexemes with a no translation remark are marked english`() {
        val window = reducer.window(Fx.zarpo)

        assertTrue(window.lexeme("zarpar", "enwiktionary/1.verb.1").isEnglishFallback)
        assertTrue(window.lexeme("zarpar", "enwiktionary/2.verb.1").isEnglishFallback)
    }

    @Test
    fun `AWP-34_3 the remark marks only the lexeme it names`() {
        val remark = Fx.noTranslation("zarpar", "enwiktionary/2.verb.1")

        val window = reducer.window(Fx.zarpo.copy(remarks = listOf(remark)))

        assertFalse(window.lexeme("zarpar", "enwiktionary/1.verb.1").isEnglishFallback)
        assertTrue(window.lexeme("zarpar", "enwiktionary/2.verb.1").isEnglishFallback)
    }

    @Test
    fun `AWP-34_4 a translation replaced by the user keeps the english mark`() {
        val window = reducer.window(Fx.zarpo)
        val translation = Fx.ref("zarpar", "enwiktionary/1.verb.1", Fx.TRANSLATION)

        val result = reducer.step(window, Msg.SubmitText(translation, "отплыть"))

        assertEquals(listOf("отплыть"), result.texts("zarpar", "enwiktionary/1.verb.1"))
        assertTrue(result.lexeme("zarpar", "enwiktionary/1.verb.1").isEnglishFallback)
    }

    // ===== «Замечания» (AWP-30) =====

    @Test
    fun `AWP-30_1 an unclear component named by two words makes one line`() {
        val unclear = listOf(WordRemarkUi.ComponentUnclear(Fx.UNCLEAR))
        val form = Fx.form(components = Fx.tree + Fx.unclear)

        val window = reducer.window(
            Fx.perro.copy(remarks = unclear),
            Fx.corrian.copy(remarks = unclear),
            form = form,
        )

        assertEquals(listOf(RemarkUiItem.ComponentUnclear(Fx.UNCLEAR)), window.remarks)
    }

    @Test
    fun `AWP-30_3 two lexemes without a translation make one line with count and language`() {
        val window = reducer.window(Fx.zarpo)

        assertEquals(
            listOf(RemarkUiItem.NoTranslation(count = 2, translationLanguage = "ru")),
            window.remarks,
        )
    }

    @Test
    fun `AWP-30_4 no remarks and no recommendations make no section`() {
        val window = reducer.window(Fx.perro)

        assertTrue(window.remarks.isEmpty())
    }

    @Test
    fun `AWP-30_12 the line about the word comes before the line about the answer`() {
        val word = Fx.perro.copy(remarks = listOf(WordRemarkUi.ComponentUnclear(Fx.UNCLEAR)))
        val form = Fx.form(components = Fx.tree + Fx.unclear)

        val window = reducer.window(
            word,
            form = form,
            recommendations = listOf(Fx.genderRecommendation),
        )

        assertEquals(
            listOf(
                RemarkUiItem.ComponentUnclear(Fx.UNCLEAR),
                RemarkUiItem.ComponentRecommended(
                    name = "Род",
                    template = ComponentTemplate.CHOICE,
                    options = listOf("м.р.", "ж.р."),
                ),
            ),
            window.remarks,
        )
    }

    // ===== «Сопоставление» (AWP-31) =====

    @Test
    fun `AWP-31_1 the section lists the matches of all lexemes once in the order of components`() {
        val window = reducer.window(Fx.perro, Fx.corrian)
        val form = requireNotNull(window.form)

        assertEquals(
            listOf(
                "translations → Перевод",
                "Часть речи: существительное",
                "Часть речи: прилагательное",
                "Часть речи: глагол",
                "Род: м.р.",
                "examples → Пример",
                "gloss → Толкование",
            ),
            window.componentMatchLines.map { it.shown(form) },
        )
    }

    @Test
    fun `AWP-31_1 field and option lines carry component ids for the interface to name`() {
        val window = reducer.window(Fx.perro)

        val field = ComponentMatchLineUiItem.FieldToComponent("translations", Fx.TRANSLATION)
        val option = ComponentMatchLineUiItem.ComponentOption(Fx.GENDER, Fx.MASCULINE)
        assertTrue(field in window.componentMatchLines)
        assertTrue(option in window.componentMatchLines)
    }

    @Test
    fun `AWP-31_2 an answer without matches has no section`() {
        val bare = Fx.word("perro", Fx.lemma("perro", listOf(Fx.lexeme("ruwiktionary/1.noun.1"))))

        val window = reducer.window(bare)

        assertTrue(window.componentMatchLines.isEmpty())
    }

    @Test
    fun `AWP-31_3 deleting a lexeme or an example does not change the section`() {
        val window = reducer.window(Fx.perro, Fx.tiempo)
        val example = Fx.ref("tiempo", "ruwiktionary/1.noun.3", Fx.EXAMPLE)

        val adjective = Msg.DeleteLexeme("perro", "ruwiktionary/2.adjective.1")
        val result = reducer.steps(window, adjective, Msg.DeleteValue(example))

        assertEquals(window.componentMatchLines, result.componentMatchLines)
    }

    @Test
    fun `AWP-31_4 the section is collapsed on open and toggles`() {
        val window = reducer.window(Fx.perro)

        val expanded = reducer.step(window, Msg.ToggleComponentMatches)
        val collapsed = reducer.step(expanded, Msg.ToggleComponentMatches)

        assertFalse(window.isComponentMatchesExpanded)
        assertTrue(expanded.isComponentMatchesExpanded)
        assertFalse(collapsed.isComponentMatchesExpanded)
    }

    // ===== Правка текста (AWP-16) =====

    @Test
    fun `AWP-16_1 AWP-16_2 an edit through the sheet lands trimmed, marked, sheet closed`() {
        val window = reducer.window(Fx.ran)
        val sheet = CandidatesSheetUi.TextEdit(translationOfRun, draft = "", canReset = false)
        val opened = reducer.step(window, Msg.OpenSheet(sheet))
        val typed = reducer.step(opened, Msg.UpdateDraft("  брать "))

        val result = reducer.step(typed, Msg.SubmitText(translationOfRun, "  брать "))

        assertEquals(sheet.copy(draft = "бежать, бегать", canReset = false), opened.sheet)
        assertEquals(sheet.copy(draft = "  брать ", canReset = true), typed.sheet)
        assertEquals(listOf("брать"), result.texts("run", runKey))
        assertTrue(result.lexeme("run", runKey).isEdited)
        assertNull(result.sheet)
        assertTrue(result.isDirty)
    }

    @Test
    fun `AWP-16_3 spaces around the text are cut`() {
        val window = reducer.window(Fx.ran)

        val result = reducer.step(window, Msg.SubmitText(translationOfRun, "  брать  "))

        assertEquals(listOf("брать"), result.texts("run", runKey))
    }

    @Test
    fun `AWP-16_5 an example erased to empty is deleted and the lexeme stays`() {
        val window = reducer.window(Fx.tiempo)
        val second = Fx.ref("tiempo", "ruwiktionary/1.noun.3", Fx.EXAMPLE, index = 1)

        val result = reducer.step(window, Msg.SubmitText(second, ""))

        assertEquals(
            listOf(Fx.tiempoExamples[0], Fx.tiempoExamples[2]),
            result.texts("tiempo", "ruwiktionary/1.noun.3", Fx.EXAMPLE),
        )
        assertTrue(result.isDirty)
    }

    @Test
    fun `AWP-16_6 reset brings the text of the answer back without the mark`() {
        val window = reducer.window(Fx.ran)
        val edited = reducer.step(window, Msg.SubmitText(translationOfRun, "брать"))

        val result = reducer.step(edited, Msg.ResetText(translationOfRun))

        assertEquals(listOf("бежать, бегать"), result.texts("run", runKey))
        assertFalse(result.lexeme("run", runKey).isEdited)
    }

    @Test
    fun `a sheet opens one at a time and closes`() {
        val window = reducer.window(Fx.ran)

        val group = reducer.step(window, Msg.OpenSheet(CandidatesSheetUi.CommonGroup))
        val card = reducer.step(group, Msg.OpenSheet(CandidatesSheetUi.CardTarget("run")))
        val closed = reducer.step(card, Msg.CloseSheet)

        assertEquals(CandidatesSheetUi.CommonGroup, group.sheet)
        assertEquals(CandidatesSheetUi.CardTarget("run"), card.sheet)
        assertNull(closed.sheet)
    }

    // ===== Ядро (AWP-11): без ядра нет лексемы =====

    @Test
    fun `AWP-11_1 a lexeme with a translation stays in the window without a mark`() {
        val window = reducer.window(Fx.ran)

        assertEquals(
            listOf(runKey, "ruwiktionary/1.verb.2"),
            window.card("run").lexemes.map { it.key },
        )
        assertTrue(window.card("run").lexemes.none { it.lacksCore })
    }

    @Test
    fun `AWP-11_5 a translation erased to empty deletes the lexeme, the button counts the rest`() {
        val window = reducer.window(Fx.ran)

        val result = reducer.step(window, Msg.SubmitText(translationOfRun, ""))

        assertEquals(listOf("ruwiktionary/1.verb.2"), result.card("run").lexemes.map { it.key })
        assertEquals(1, result.summary.lexemeCount)
        assertEquals(1, result.summary.wordCount)
        assertEquals(0, result.summary.lacksCoreCount)
        assertTrue(result.summary.isEnabled)
        assertTrue(result.isDirty)
    }

    @Test
    fun `AWP-11_5 the only translation text deleted with the cross deletes the lexeme`() {
        val window = reducer.window(Fx.ran)

        val result = reducer.step(window, Msg.DeleteValue(translationOfRun))

        assertEquals(listOf("ruwiktionary/1.verb.2"), result.card("run").lexemes.map { it.key })
        assertEquals(1, result.summary.lexemeCount)
        assertEquals(0, result.summary.lacksCoreCount)
        assertTrue(result.summary.isEnabled)
        assertTrue(result.isDirty)
    }

    @Test
    fun `AWP-11_5 the last lexeme of the window deleted this way leaves nothing to add`() {
        val window = reducer.window(Fx.bank)
        val translation = Fx.ref("bank", "ruwiktionary/1.noun.1", Fx.TRANSLATION)

        val result = reducer.step(window, Msg.SubmitText(translation, ""))

        assertTrue(result.card("bank").lexemes.isEmpty())
        assertEquals(0, result.summary.lexemeCount)
        assertFalse(result.summary.isEnabled)
    }

    @Test
    fun `AWP-11_9 with two cores having text the erased translation leaves the other core`() {
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
        val window = reducer.window(word, form = form)
        val translation = Fx.ref("perro", key, Fx.TRANSLATION)

        val erased = reducer.step(window, Msg.SubmitText(translation, ""))
        val crossed = reducer.step(window, Msg.DeleteValue(translation))

        assertEquals(listOf("Перевод: собака", "Определение: dog"), window.shown("perro", key))
        assertEquals(listOf("Определение: dog"), erased.shown("perro", key))
        assertEquals(listOf("Определение: dog"), crossed.shown("perro", key))
        assertFalse(erased.lexeme("perro", key).lacksCore)
        assertTrue(erased.summary.isEnabled)
    }

    // ===== Опция «Выбора» (AWP-17) =====

    @Test
    fun `AWP-17_1 a changed part of speech touches one lexeme only and raises the flag`() {
        val window = reducer.window(Fx.ran)
        val partOfSpeech = Fx.componentRef("run", runKey, Fx.PART_OF_SPEECH)
        val opened = reducer.step(window, Msg.OpenSheet(CandidatesSheetUi.Option(partOfSpeech)))

        val result = reducer.step(opened, Msg.SetOption(partOfSpeech, Fx.NOUN))

        assertEquals(listOf(Fx.NOUN), result.chosen("run", runKey, Fx.PART_OF_SPEECH))
        assertEquals(
            listOf(Fx.VERB),
            result.chosen("run", "ruwiktionary/1.verb.2", Fx.PART_OF_SPEECH),
        )
        assertNull(result.sheet)
        assertTrue(result.isDirty)
    }

    @Test
    fun `AWP-17_1 without a value is an option too`() {
        val window = reducer.window(Fx.ran)
        val partOfSpeech = Fx.componentRef("run", runKey, Fx.PART_OF_SPEECH)

        val result = reducer.step(window, Msg.SetOption(partOfSpeech, null))

        assertEquals(emptyList<Long>(), result.chosen("run", runKey, Fx.PART_OF_SPEECH))
        assertTrue(result.isDirty)
    }

    @Test
    fun `AWP-17_5 a changed gender touches one lexeme only`() {
        val window = reducer.window(Fx.perro)
        val gender = Fx.componentRef("perro", "ruwiktionary/1.noun.1", Fx.GENDER)

        val result = reducer.step(window, Msg.SetOption(gender, Fx.FEMININE))

        assertEquals(
            listOf(Fx.FEMININE),
            result.chosen("perro", "ruwiktionary/1.noun.1", Fx.GENDER),
        )
        assertEquals(
            listOf(Fx.MASCULINE),
            result.chosen("perro", "ruwiktionary/1.noun.2", Fx.GENDER),
        )
    }

    @Test
    fun `AWP-17_6 AWP-17_7 a gender depends on the noun option and comes back with it`() {
        val window = reducer.window(Fx.perro)
        val partOfSpeech = Fx.componentRef("perro", "ruwiktionary/1.noun.1", Fx.PART_OF_SPEECH)

        val verb = reducer.step(window, Msg.SetOption(partOfSpeech, Fx.VERB))
        val noun = reducer.step(verb, Msg.SetOption(partOfSpeech, Fx.NOUN))

        assertEquals(
            listOf(
                "Перевод: собака; пёс (также ругательство)",
                "Часть речи: глагол",
                "Пример: perro callejero",
                "Толкование: dog (the species Canis familiaris …)",
            ),
            verb.shown("perro", "ruwiktionary/1.noun.1"),
        )
        assertEquals(
            window.shown("perro", "ruwiktionary/1.noun.1"),
            noun.shown("perro", "ruwiktionary/1.noun.1"),
        )
    }

    @Test
    fun `AWP-17_6 deleting the noun option crosses out the gender too`() {
        val window = reducer.window(Fx.perro)
        val noun = Fx.ref("perro", "ruwiktionary/1.noun.1", Fx.PART_OF_SPEECH)

        val result = reducer.step(window, Msg.DeleteValue(noun))

        assertEquals(
            listOf(
                "Перевод: собака; пёс (также ругательство)",
                "Часть речи: без значения",
                "Пример: perro callejero",
                "Толкование: dog (the species Canis familiaris …)",
            ),
            result.shown("perro", "ruwiktionary/1.noun.1"),
        )
    }

    // ===== Зависимость от компонента (AWP-17.6) =====

    private val dependentKey = "ruwiktionary/1.noun.1"
    private val firstExample = Fx.ref("perro", dependentKey, Fx.EXAMPLE, index = 0)
    private val secondExample = Fx.ref("perro", dependentKey, Fx.EXAMPLE, index = 1)

    /**
     * «Разбор» зависит от «Примера» (`DependencyTarget.Component`), не от ядра: у
     * лексемы один текст перевода, [examples] примера и значение «Разбора» — пока в
     * примере есть текст, оно активно. Ядро тут ни при чём: стёртый единственный
     * текст ядра удалил бы лексему целиком (AWP-11.5).
     */
    private fun windowWithDependent(
        examples: List<String> = listOf("perro callejero"),
    ): ScanCandidatesState {
        val dependent = Fx.component(
            id = Fx.UNCLEAR,
            systemKey = null,
            name = "Разбор",
            template = ComponentTemplate.TEXT,
            isCore = false,
            position = 5,
            dependsOn = DependencyTarget.Component(ComponentTypeId(Fx.EXAMPLE)),
        )
        val lexeme = Fx.lexeme(
            key = dependentKey,
            translations = listOf("собака"),
            examples = examples,
            notes = listOf("разг."),
            matches = listOf(
                Fx.byField(Fx.TRANSLATION, "translations"),
                Fx.byField(Fx.EXAMPLE, "examples"),
                Fx.byField(Fx.UNCLEAR, "notes"),
            ),
        )
        val word = Fx.word("perro", Fx.lemma("perro", listOf(lexeme)))
        val form = Fx.form(components = listOf(Fx.translation, Fx.example, dependent))
        return reducer.window(word, form = form)
    }

    @Test
    fun `AWP-17_6 AWP-15_2 deleting the only example crosses out its dependent value`() {
        val window = windowWithDependent()

        val result = reducer.step(window, Msg.DeleteValue(firstExample))

        assertTrue(window.isActive("perro", dependentKey, Fx.UNCLEAR))
        assertFalse(result.isActive("perro", dependentKey, Fx.UNCLEAR))
        assertEquals(listOf("Перевод: собака"), result.shown("perro", dependentKey))
        assertTrue(result.isDirty)
    }

    @Test
    fun `AWP-17_6 AWP-11_5 saving the only example empty crosses out its dependent value`() {
        val window = windowWithDependent()

        val result = reducer.step(window, Msg.SubmitText(firstExample, ""))

        assertTrue(window.isActive("perro", dependentKey, Fx.UNCLEAR))
        assertFalse(result.isActive("perro", dependentKey, Fx.UNCLEAR))
        assertEquals(listOf("Перевод: собака"), result.shown("perro", dependentKey))
        assertTrue(result.isDirty)
    }

    @Test
    fun `AWP-17_6 the dependent value lives while the parent still has a text`() {
        val window = windowWithDependent(examples = listOf("perro callejero", "perro de agua"))

        val oneLeft = reducer.step(window, Msg.DeleteValue(secondExample))
        val noneLeft = reducer.step(oneLeft, Msg.DeleteValue(firstExample))

        assertTrue(oneLeft.isActive("perro", dependentKey, Fx.UNCLEAR))
        assertEquals(
            listOf("Перевод: собака", "Пример: perro callejero", "Разбор: разг."),
            oneLeft.shown("perro", dependentKey),
        )
        assertFalse(noneLeft.isActive("perro", dependentKey, Fx.UNCLEAR))
        assertEquals(listOf("Перевод: собака"), noneLeft.shown("perro", dependentKey))
    }

    @Test
    fun `AWP-17_6 AWP-15_6 a deleted parent does not come back, reset has nothing to bring`() {
        val erased = reducer.step(windowWithDependent(), Msg.SubmitText(firstExample, ""))

        val reset = reducer.testReduce(erased, Msg.ResetText(firstExample))

        assertEquals(erased, reset.state())
        assertFalse(reset.state().isActive("perro", dependentKey, Fx.UNCLEAR))
        reset.assertNoEffects()
    }
}
