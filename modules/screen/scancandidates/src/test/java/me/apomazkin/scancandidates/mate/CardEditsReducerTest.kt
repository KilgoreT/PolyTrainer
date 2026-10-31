package me.apomazkin.scancandidates.mate

import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.assertNoEffects
import io.github.kilgoret.mate.test.testReduce
import me.apomazkin.candidates.CandidatesSheetUi
import me.apomazkin.candidates.CardOwnGroupUi
import me.apomazkin.candidates.CardTargetUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Правки плашки нейронки глазами редьюсера: удаление значения (AWP-15), правка
 * текста (AWP-16), опции «Выбора» (AWP-17), ядро (AWP-11), раздел
 * «Сопоставление» (AWP-31.3), своя группа и «Куда» плашки (AWP-08, AWP-18).
 * Адрес правки — лемма карточки, то есть текст слова; правка ложится во все
 * строки слова. Адреса строк спеки — `AWP-NN_k`.
 */
class CardEditsReducerTest {
    private val reducer = reducer()

    private val runKey = "ruwiktionary/1.verb.1"
    private val manageKey = "ruwiktionary/1.verb.2"
    private val bankKey = "ruwiktionary/1.noun.1"
    private val translationOfRun = Fx.ref("run", runKey, Fx.TRANSLATION)

    // ===== Удаление значения (AWP-15) =====

    @Test
    fun `AWP-15_2 deleting one example keeps the other examples and the plate`() {
        val loaded = reducer.withCard("tiempo", Fx.tiempo)
        val second = Fx.ref("tiempo", "ruwiktionary/1.noun.3", Fx.EXAMPLE, index = 1)

        val result = reducer.step(loaded, Msg.DeleteValue(second))

        assertEquals(
            listOf(Fx.tiempoExamples[0], Fx.tiempoExamples[2]),
            result.texts("tiempo", "ruwiktionary/1.noun.3", Fx.EXAMPLE),
        )
        assertEquals(1, result.lemmaCard("tiempo").lexemes.size)
        assertTrue(result.lexeme("tiempo", "ruwiktionary/1.noun.3").canWrite)
    }

    @Test
    fun `AWP-15_7 deleting one value of a multiple choice keeps the other and the plate`() {
        val loaded = reducer.withCard("llegar", Fx.llegar, form = Fx.formWithStyle())
        val colloquial = Fx.ref("llegar", "ruwiktionary/1.verb.1", Fx.STYLE, index = 1)

        val result = reducer.step(loaded, Msg.DeleteValue(colloquial))

        assertEquals(listOf("Перевод: прибывать", "Стиль: перен."), result.shown("llegar", "ruwiktionary/1.verb.1"))
    }

    @Test
    fun `an edit of a word lands in both rows of a word that is in two lists`() {
        val lists = Fx.lists(lexical = listOf("have"), function = listOf("have"))
        val both = reducer.withCard("have", Fx.run.copy(text = "have"), lists = lists)

        val result = reducer.step(both, Msg.DismissLexeme("have", manageKey))

        assertEquals(listOf(runKey), result.lists.lexical.single().card?.lemma?.lexemes?.map { it.key })
        assertEquals(listOf(runKey), result.lists.function.single().card?.lemma?.lexemes?.map { it.key })
    }

    // ===== Правка текста (AWP-16) =====

    @Test
    fun `AWP-16_1 AWP-16_2 an edit through the sheet lands trimmed and marked, the sheet closes`() {
        val loaded = reducer.withCard("run", Fx.run)
        val sheet = CandidatesSheetUi.TextEdit(translationOfRun, draft = "", canReset = false)
        val opened = reducer.step(loaded, Msg.OpenSheet(sheet))
        val typed = reducer.step(opened, Msg.UpdateDraft("  брать "))

        val result = reducer.step(typed, Msg.SubmitText(translationOfRun, "  брать "))

        assertEquals(sheet.copy(draft = "бежать, бегать", canReset = false), opened.sheet)
        assertEquals(sheet.copy(draft = "  брать ", canReset = true), typed.sheet)
        assertEquals(listOf("брать"), result.texts("run", runKey))
        assertTrue(result.lexeme("run", runKey).isEdited)
        assertNull(result.sheet)
        assertTrue(result.lexeme("run", runKey).canWrite)
    }

    @Test
    fun `AWP-16_3 spaces around the text are cut`() {
        val loaded = reducer.withCard("run", Fx.run)

        val result = reducer.step(loaded, Msg.SubmitText(translationOfRun, "  брать  "))

        assertEquals(listOf("брать"), result.texts("run", runKey))
    }

    @Test
    fun `AWP-16_5 an example erased to empty is deleted and the plate stays`() {
        val loaded = reducer.withCard("tiempo", Fx.tiempo)
        val second = Fx.ref("tiempo", "ruwiktionary/1.noun.3", Fx.EXAMPLE, index = 1)

        val result = reducer.step(loaded, Msg.SubmitText(second, ""))

        assertEquals(
            listOf(Fx.tiempoExamples[0], Fx.tiempoExamples[2]),
            result.texts("tiempo", "ruwiktionary/1.noun.3", Fx.EXAMPLE),
        )
        assertEquals(1, result.lemmaCard("tiempo").lexemes.size)
    }

    @Test
    fun `AWP-16_6 reset brings the text of the answer back without the mark and closes the sheet`() {
        val loaded = reducer.withCard("run", Fx.run)
        val edited = reducer.step(loaded, Msg.SubmitText(translationOfRun, "брать"))
        val sheet = CandidatesSheetUi.TextEdit(translationOfRun, draft = "", canReset = false)
        val opened = reducer.step(edited, Msg.OpenSheet(sheet))

        val result = reducer.step(opened, Msg.ResetText(translationOfRun))

        assertEquals(sheet.copy(draft = "брать", canReset = true), opened.sheet)
        assertEquals(listOf("бежать, бегать"), result.texts("run", runKey))
        assertFalse(result.lexeme("run", runKey).isEdited)
        assertNull(result.sheet)
    }

    @Test
    fun `AWP-16_7 AWP-34_4 a replaced translation keeps the english mark`() {
        val loaded = reducer.withCard("zarpó", Fx.zarpo)
        val translation = Fx.ref("zarpó", "enwiktionary/1.verb.1", Fx.TRANSLATION)

        val result = reducer.step(loaded, Msg.SubmitText(translation, "отплыть"))

        assertEquals(listOf("отплыть"), result.texts("zarpó", "enwiktionary/1.verb.1"))
        assertTrue(result.lexeme("zarpó", "enwiktionary/1.verb.1").isEnglishFallback)
    }

    @Test
    fun `a sheet opens one at a time and closes`() {
        val loaded = reducer.withCard("run", Fx.run)

        val group = reducer.step(loaded, Msg.OpenSheet(CandidatesSheetUi.CommonGroup))
        val target = reducer.step(group, Msg.OpenSheet(CandidatesSheetUi.CardTarget("run", runKey)))
        val closed = reducer.step(target, Msg.CloseSheet)

        assertEquals(CandidatesSheetUi.CommonGroup, group.sheet)
        assertEquals(CandidatesSheetUi.CardTarget("run", runKey), target.sheet)
        assertNull(closed.sheet)
    }

    // ===== Опция «Выбора» (AWP-17) =====

    @Test
    fun `AWP-17_1 a changed part of speech touches one plate only and closes the sheet`() {
        val loaded = reducer.withCard("run", Fx.run)
        val partOfSpeech = Fx.componentRef("run", runKey, Fx.PART_OF_SPEECH)
        val opened = reducer.step(loaded, Msg.OpenSheet(CandidatesSheetUi.Option(partOfSpeech)))

        val result = reducer.step(opened, Msg.SetOption(partOfSpeech, Fx.NOUN))

        assertEquals(listOf(Fx.NOUN), result.chosen("run", runKey, Fx.PART_OF_SPEECH))
        assertEquals(listOf(Fx.VERB), result.chosen("run", manageKey, Fx.PART_OF_SPEECH))
        assertNull(result.sheet)
    }

    @Test
    fun `AWP-17_1 without a value is an option too`() {
        val loaded = reducer.withCard("run", Fx.run)
        val partOfSpeech = Fx.componentRef("run", runKey, Fx.PART_OF_SPEECH)

        val result = reducer.step(loaded, Msg.SetOption(partOfSpeech, null))

        assertEquals(emptyList<Long>(), result.chosen("run", runKey, Fx.PART_OF_SPEECH))
        assertEquals(
            listOf("Перевод: бежать, бегать", "Часть речи: без значения"),
            result.shown("run", runKey),
        )
    }

    @Test
    fun `AWP-17_3 the part of speech switched off in the dictionary has no row and takes the gender with it`() {
        val withoutPartOfSpeech = Fx.form(components = listOf(Fx.translation, Fx.gender, Fx.example, Fx.gloss))

        val loaded = reducer.withCard("perro", Fx.perro, form = withoutPartOfSpeech)

        assertEquals(
            listOf(
                "Перевод: собака; пёс (также ругательство)",
                "Толкование: dog (the species Canis familiaris …)",
                "Пример: perro callejero",
            ),
            loaded.shown("perro", "ruwiktionary/1.noun.1"),
        )
        assertFalse(loaded.isActive("perro", "ruwiktionary/1.noun.1", Fx.GENDER))
    }

    @Test
    fun `AWP-17_5 a changed gender touches one plate only`() {
        val loaded = reducer.withCard("perro", Fx.perro)
        val gender = Fx.componentRef("perro", "ruwiktionary/1.noun.1", Fx.GENDER)

        val result = reducer.step(loaded, Msg.SetOption(gender, Fx.FEMININE))

        assertEquals(listOf(Fx.FEMININE), result.chosen("perro", "ruwiktionary/1.noun.1", Fx.GENDER))
        assertEquals(listOf(Fx.MASCULINE), result.chosen("perro", "ruwiktionary/1.noun.2", Fx.GENDER))
    }

    @Test
    fun `AWP-17_6 AWP-17_7 a gender depends on the noun option and comes back with it`() {
        val loaded = reducer.withCard("perro", Fx.perro)
        val partOfSpeech = Fx.componentRef("perro", "ruwiktionary/1.noun.1", Fx.PART_OF_SPEECH)

        val verb = reducer.step(loaded, Msg.SetOption(partOfSpeech, Fx.VERB))
        val noun = reducer.step(verb, Msg.SetOption(partOfSpeech, Fx.NOUN))

        assertEquals(
            listOf(
                "Перевод: собака; пёс (также ругательство)",
                "Часть речи: глагол",
                "Толкование: dog (the species Canis familiaris …)",
                "Пример: perro callejero",
            ),
            verb.shown("perro", "ruwiktionary/1.noun.1"),
        )
        assertEquals(loaded.shown("perro", "ruwiktionary/1.noun.1"), noun.shown("perro", "ruwiktionary/1.noun.1"))
    }

    @Test
    fun `AWP-17_6 deleting the noun option crosses out the gender too`() {
        val loaded = reducer.withCard("perro", Fx.perro)
        val noun = Fx.ref("perro", "ruwiktionary/1.noun.1", Fx.PART_OF_SPEECH)

        val result = reducer.step(loaded, Msg.DeleteValue(noun))

        assertEquals(
            listOf(
                "Перевод: собака; пёс (также ругательство)",
                "Часть речи: без значения",
                "Толкование: dog (the species Canis familiaris …)",
                "Пример: perro callejero",
            ),
            result.shown("perro", "ruwiktionary/1.noun.1"),
        )
    }

    // ===== Ядро (AWP-11): без ядра нет лексемы =====

    @Test
    fun `AWP-11_1 a plate with a translation stays without a mark`() {
        val loaded = reducer.withCard("run", Fx.run)

        assertEquals(listOf(runKey, manageKey), loaded.lemmaCard("run").lexemes.map { it.key })
        assertTrue(loaded.lemmaCard("run").lexemes.none { it.lacksCore })
    }

    @Test
    fun `AWP-11_5 a translation erased to empty drops the plate, the rest stay writable`() {
        val loaded = reducer.withCard("run", Fx.run)

        val result = reducer.step(loaded, Msg.SubmitText(translationOfRun, ""))

        assertEquals(listOf(manageKey), result.lemmaCard("run").lexemes.map { it.key })
        assertTrue(result.lemmaCard("run").lexemes.none { it.lacksCore })
        assertTrue(result.lexeme("run", manageKey).canWrite)
        assertNull(result.sheet)
    }

    @Test
    fun `AWP-11_5 AWP-15_9 the only translation text deleted with the cross drops the plate`() {
        val loaded = reducer.withCard("run", Fx.run)

        val result = reducer.step(loaded, Msg.DeleteValue(translationOfRun))

        assertEquals(listOf(manageKey), result.lemmaCard("run").lexemes.map { it.key })
    }

    @Test
    fun `AWP-11_5 AWP-62_5 the last plate erased this way takes the word out of the list`() {
        val loaded = reducer.withCard("bank", Fx.bank, lists = Fx.lists(lexical = listOf("bank", "run")))
        val translation = Fx.ref("bank", bankKey, Fx.TRANSLATION)

        val erased = reducer.testReduce(loaded, Msg.SubmitText(translation, ""))
        val crossed = reducer.testReduce(loaded, Msg.DeleteValue(translation))

        assertNull(erased.state().lists.wordOf("bank"))
        assertNull(crossed.state().lists.wordOf("bank"))
        assertEquals(listOf("run"), erased.state().lists.lexical.texts())
        erased.assertNoEffects()
    }

    @Test
    fun `AWP-11_9 with two cores having text the erased translation leaves the other core`() {
        val key = "ruwiktionary/1.noun.1"
        val lexeme = Fx.lexeme(
            key = key,
            translations = listOf("собака"),
            gloss = "dog",
            matches = listOf(Fx.byField(Fx.TRANSLATION, "translations"), Fx.byField(Fx.DEFINITION, "gloss")),
        )
        val word = Fx.word("perro", Fx.lemma("perro", listOf(lexeme)))
        val form = Fx.form(components = listOf(Fx.translation, Fx.definition))
        val loaded = reducer.withCard("perro", word, form = form)
        val translation = Fx.ref("perro", key, Fx.TRANSLATION)

        val erased = reducer.step(loaded, Msg.SubmitText(translation, ""))
        val crossed = reducer.step(loaded, Msg.DeleteValue(translation))

        assertEquals(listOf("Перевод: собака", "Определение: dog"), loaded.shown("perro", key))
        assertEquals(listOf("Определение: dog"), erased.shown("perro", key))
        assertEquals(listOf("Определение: dog"), crossed.shown("perro", key))
        assertFalse(erased.lexeme("perro", key).lacksCore)
        assertTrue(erased.lexeme("perro", key).canWrite)
    }

    // ===== Раздел «Сопоставление» (AWP-31.3) =====

    @Test
    fun `AWP-31_3 not needed on a plate or deleting an example does not change the section`() {
        val loaded = reducer.withCard("perro", Fx.perro)
        val example = Fx.ref("perro", "ruwiktionary/1.noun.1", Fx.EXAMPLE)

        val result = reducer.steps(
            loaded,
            Msg.DismissLexeme("perro", "ruwiktionary/2.adjective.1"),
            Msg.DeleteValue(example),
        )

        assertEquals(loaded.card("perro").componentMatchLines, result.card("perro").componentMatchLines)
        assertTrue(result.card("perro").componentMatchLines.isNotEmpty())
    }

    // ===== Своя группа и «Куда» плашки (AWP-08, AWP-18) =====

    @Test
    fun `AWP-08_4 the own group all at a common group writes the new word without a group`() {
        val common = reducer.step(reducer.withCard("run", Fx.run), Msg.SetCommonGroup(Fx.POTTER))

        val own = reducer.step(common, Msg.SetCardGroup("run", runKey, CardOwnGroupUi.All))

        assertEquals(CardOwnGroupUi.All, own.lexeme("run", runKey).ownGroup)
        assertNull(own.sheet)
        assertNull(reducer.batchOf(own, "run", runKey).words.single().groupId)
    }

    @Test
    fun `AWP-08_1 AWP-08_5 AWP-08_8 an own group of one plate acts instead of the common one and taking it off brings the common back`() {
        val common = reducer.step(reducer.withCard("run", Fx.run), Msg.SetCommonGroup(Fx.POTTER))

        val own = reducer.step(common, Msg.SetCardGroup("run", runKey, CardOwnGroupUi.Group(Fx.FINANCE)))
        val back = reducer.step(own, Msg.SetCardGroup("run", runKey, CardOwnGroupUi.AsCommon))

        assertEquals(Fx.FINANCE, reducer.batchOf(own, "run", runKey).words.single().groupId)
        assertEquals(CardOwnGroupUi.AsCommon, own.lexeme("run", manageKey).ownGroup)
        assertEquals(Fx.POTTER, reducer.batchOf(own, "run", manageKey).words.single().groupId)
        assertEquals(Fx.POTTER, reducer.batchOf(back, "run", runKey).words.single().groupId)
    }

    @Test
    fun `AWP-18_3 AWP-18_4 a plate starts with a new word and goes back to it after a chosen word, the saved plates stay`() {
        val form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))
        val loaded = reducer.withCard("bank", Fx.bank, form = form)

        val chosen = reducer.step(loaded, Msg.SetCardTarget("bank", bankKey, CardTargetUi.Existing(Fx.BANK_1)))
        val third = reducer.step(chosen, Msg.SetCardTarget("bank", bankKey, CardTargetUi.NewWord))

        assertEquals(CardTargetUi.NewWord, loaded.lexeme("bank", bankKey).target)
        assertEquals(CardTargetUi.Existing(Fx.BANK_1), chosen.lexeme("bank", bankKey).target)
        assertEquals(CardTargetUi.NewWord, third.lexeme("bank", bankKey).target)
        assertEquals(listOf(Fx.savedWord(Fx.bank1), Fx.savedWord(Fx.bank2)), third.card("bank").saved)
        assertNull(reducer.batchOf(third, "bank", bankKey).words.single().wordId)
        assertEquals("bank", reducer.batchOf(third, "bank", bankKey).words.single().text)
    }
}
