package me.apomazkin.candidates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Переходы над списком карточек (AWP-15, AWP-16, AWP-17, AWP-18, AWP-08,
 * AWP-21): чистые функции, которые зовут атомы экрана и диалога. `null`
 * в ответе — «шага нет».
 */
class CardTransitionsTest {

    private val form = Fx.form()

    private val threeRun = Fx.word(
        "run",
        Fx.lemma("run", listOf(Fx.runVerb1, Fx.runVerb2, Fx.runMachine)),
    )

    private fun runTexts(cards: List<LemmaCardUiItem>): List<String> =
        cards.cardOf("run")!!.lexemes.map { it.texts(Fx.TRANSLATION).first() }

    // AWP-15 — удаление из окна

    @Test
    fun `AWP-15_1 deleting one of three lexemes leaves two`() {
        val cards = Fx.cards(threeRun)

        val after = cards.deletingLexeme("run", "ruwiktionary/1.verb.2")!!

        assertEquals(
            listOf("бежать, бегать", "работать, функционировать, идти"),
            runTexts(after),
        )
    }

    @Test
    fun `AWP-15_2 deleting one example leaves the others and the lexeme`() {
        val cards = Fx.cards(Fx.tiempo)
        val example = Fx.ref("tiempo", "ruwiktionary/1.noun.3", Fx.EXAMPLE, index = 1)

        val after = cards.deletingValue(example, form)!!

        val lexeme = after.candidate("tiempo", "ruwiktionary/1.noun.3")
        assertEquals(
            listOf("¡Hoy hace un tiempo de perro!", "Hace buen tiempo."),
            lexeme.texts(Fx.EXAMPLE),
        )
        assertEquals(listOf("погода"), lexeme.texts(Fx.TRANSLATION))
    }

    @Test
    fun `AWP-15_3 deleting every lexeme of a card leaves an empty card that writes nothing`() {
        val cards = Fx.cards(Fx.ran)

        val empty = cards
            .deletingLexeme("run", "ruwiktionary/1.verb.1")!!
            .deletingLexeme("run", "ruwiktionary/1.verb.2")!!

        assertTrue(empty.single().lexemes.isEmpty())
        assertNull(lexemeBatchOf(empty.single(), "ruwiktionary/1.verb.1", form, commonGroupId = null))
    }

    @Test
    fun `AWP-15_4 deleting the run card keeps the bank card`() {
        val cards = Fx.cards(Fx.ran, Fx.bank)

        val after = cards.deletingCard("run")!!

        assertEquals(listOf("bank"), after.map { it.lemma })
    }

    @Test
    fun `AWP-15_7 deleting one value of a multiple choice leaves the other`() {
        val withStyle = Fx.formWithStyle()
        val cards = Fx.cards(Fx.llegar, form = withStyle)
        val colloquial = Fx.ref("llegar", "ruwiktionary/1.verb.1", Fx.STYLE, index = 1)

        val after = cards.deletingValue(colloquial, withStyle)!!

        val lexeme = after.candidate("llegar", "ruwiktionary/1.verb.1")
        assertEquals(listOf("Перевод: прибывать", "Стиль: перен."), lexeme.shown(withStyle))
    }

    @Test
    fun `AWP-15_8 the second quick delete of the same lexeme is no step, the neighbour stays`() {
        val cards = Fx.cards(threeRun)

        val once = cards.deletingLexeme("run", "ruwiktionary/1.verb.2")!!
        val twice = once.deletingLexeme("run", "ruwiktionary/1.verb.2")

        assertNull(twice)
        assertEquals(2, once.cardOf("run")!!.lexemes.size)
    }

    @Test
    fun `AWP-15_8 a value, a lexeme or a card that is not there is no step`() {
        val cards = Fx.cards(Fx.ran)
        val noGloss = Fx.ref("run", "ruwiktionary/1.verb.1", Fx.GLOSS)
        val noText = Fx.ref("run", "ruwiktionary/1.verb.1", Fx.TRANSLATION, index = 5)

        assertNull(cards.deletingCard("walk"))
        assertNull(cards.deletingLexeme("run", "ruwiktionary/9.verb.9"))
        assertNull(cards.deletingValue(noGloss, form))
        assertNull(cards.deletingValue(noText, form))
    }

    @Test
    fun `AWP-11_5 deleting the last text of the only core deletes the lexeme`() {
        val multiple = Fx.form(components = listOf(Fx.translation.copy(isMultiple = true)))
        val translation = Fx.ref("run", "enwiktionary/1.verb.1", Fx.TRANSLATION)
        var cards = Fx.cards(Fx.runEnglish, form = multiple)
        val textCount = cards.candidate("run", "enwiktionary/1.verb.1").texts(Fx.TRANSLATION).size

        repeat(textCount - 1) { cards = cards.deletingValue(translation, multiple)!! }
        assertEquals(1, cards.candidate("run", "enwiktionary/1.verb.1").texts(Fx.TRANSLATION).size)

        cards = cards.deletingValue(translation, multiple)!!

        assertNull(cards.lexemeOf("run", "enwiktionary/1.verb.1"))
    }

    // AWP-16 — правка текстов

    @Test
    fun `AWP-16_1 an edited text is marked as edited and keeps the text of the answer`() {
        val cards = Fx.cards(Fx.perro)
        val translation = Fx.ref("perro", "ruwiktionary/1.noun.1", Fx.TRANSLATION)

        val after = cards.withTextEdit(translation, "брать")!!

        val lexeme = after.candidate("perro", "ruwiktionary/1.noun.1")
        val value = lexeme.values.first { it.componentId == Fx.TRANSLATION }
        val item = (value as CandidateValueUiItem.Text).texts.single()
        assertEquals("брать", item.text)
        assertEquals("собака; пёс (также ругательство)", item.original)
        assertTrue(lexeme.isEdited)
        assertFalse(cards.candidate("perro", "ruwiktionary/1.noun.1").isEdited)
    }

    @Test
    fun `AWP-16_2 the gloss is edited as any text component`() {
        val gloss = Fx.ref("correr", "ruwiktionary/1.verb.3", Fx.GLOSS)

        val after = Fx.cards(Fx.corrian).withTextEdit(gloss, "move fast on feet")!!

        val lexeme = after.candidate("correr", "ruwiktionary/1.verb.3")
        assertEquals(listOf("move fast on feet"), lexeme.texts(Fx.GLOSS))
    }

    @Test
    fun `AWP-16_3 the edges of the edited text are cut`() {
        val translation = Fx.ref("perro", "ruwiktionary/1.noun.1", Fx.TRANSLATION)

        val after = Fx.cards(Fx.perro).withTextEdit(translation, "  брать  ")!!

        val lexeme = after.candidate("perro", "ruwiktionary/1.noun.1")
        assertEquals(listOf("брать"), lexeme.texts(Fx.TRANSLATION))
    }

    @Test
    fun `AWP-16_5 an erased example is deleted and the rest stay`() {
        val example = Fx.ref("tiempo", "ruwiktionary/1.noun.3", Fx.EXAMPLE, index = 1)

        val after = Fx.cards(Fx.tiempo)
            .withTextEdit(example, "")!!
            .droppingEmptyTexts(example, form)!!

        val lexeme = after.candidate("tiempo", "ruwiktionary/1.noun.3")
        assertEquals(
            listOf("¡Hoy hace un tiempo de perro!", "Hace buen tiempo."),
            lexeme.texts(Fx.EXAMPLE),
        )
    }

    @Test
    fun `AWP-16_5 an erased parameter that was the only text leaves the component`() {
        val gloss = Fx.ref("correr", "ruwiktionary/1.verb.3", Fx.GLOSS)

        val after = Fx.cards(Fx.corrian)
            .withTextEdit(gloss, "")!!
            .droppingEmptyTexts(gloss, form)!!

        val lexeme = after.candidate("correr", "ruwiktionary/1.verb.3")
        val components = lexeme.values.map { it.componentId }
        assertEquals(listOf(Fx.TRANSLATION, Fx.PART_OF_SPEECH), components)
    }

    @Test
    fun `AWP-16_5 nothing erased is no step`() {
        val translation = Fx.ref("perro", "ruwiktionary/1.noun.1", Fx.TRANSLATION)

        assertNull(Fx.cards(Fx.perro).droppingEmptyTexts(translation, form))
    }

    @Test
    fun `AWP-16_6 reset brings back the text of the answer and removes the mark`() {
        val translation = Fx.ref("perro", "ruwiktionary/1.noun.1", Fx.TRANSLATION)
        val edited = Fx.cards(Fx.perro).withTextEdit(translation, "брать")!!

        val reset = edited.withTextReset(translation)!!

        val lexeme = reset.candidate("perro", "ruwiktionary/1.noun.1")
        assertEquals(listOf("собака; пёс (также ругательство)"), lexeme.texts(Fx.TRANSLATION))
        assertFalse(lexeme.isEdited)
    }

    @Test
    fun `AWP-16_6 an edit, a drop or a reset of a text that is not there is no step`() {
        val cards = Fx.cards(Fx.perro)
        val missing = Fx.ref("perro", "ruwiktionary/1.noun.1", Fx.TRANSLATION, index = 3)
        val choice = Fx.ref("perro", "ruwiktionary/1.noun.1", Fx.PART_OF_SPEECH)

        assertNull(cards.withTextEdit(missing, "x"))
        assertNull(cards.withTextReset(missing))
        assertNull(cards.withTextEdit(choice, "x"))
        assertNull(cards.droppingEmptyTexts(choice, form))
    }

    // AWP-17 — опция «Выбора»

    @Test
    fun `AWP-17_1 a new part of speech of one lexeme leaves the others as they were`() {
        val cards = Fx.cards(Fx.ran)
        val partOfSpeech = Fx.componentRef("run", "ruwiktionary/1.verb.1", Fx.PART_OF_SPEECH)

        val after = cards.withOption(partOfSpeech, Fx.NOUN, form)!!

        val first = after.candidate("run", "ruwiktionary/1.verb.1")
        val second = after.candidate("run", "ruwiktionary/1.verb.2")
        assertEquals(listOf(Fx.NOUN), first.chosen(Fx.PART_OF_SPEECH))
        assertEquals(listOf(Fx.VERB), second.chosen(Fx.PART_OF_SPEECH))
    }

    @Test
    fun `AWP-17_2 a lexeme without a part of speech has nothing to choose`() {
        val cards = Fx.cards(Fx.los)
        val partOfSpeech = Fx.componentRef("el", "enwiktionary/1.article.1", Fx.PART_OF_SPEECH)

        assertNull(cards.withOption(partOfSpeech, Fx.VERB, form))
    }

    @Test
    fun `AWP-17_5 another gender of one lexeme leaves the others as they were`() {
        val cards = Fx.cards(Fx.perro)
        val gender = Fx.componentRef("perro", "ruwiktionary/1.noun.1", Fx.GENDER)

        val after = cards.withOption(gender, Fx.FEMININE, form)!!

        val first = after.candidate("perro", "ruwiktionary/1.noun.1")
        val other = after.candidate("perro", "ruwiktionary/1.noun.2")
        assertEquals(listOf(Fx.FEMININE), first.chosen(Fx.GENDER))
        assertEquals(listOf(Fx.MASCULINE), other.chosen(Fx.GENDER))
    }

    @Test
    fun `AWP-17_1 without value takes the option away`() {
        val partOfSpeech = Fx.componentRef("run", "ruwiktionary/1.verb.1", Fx.PART_OF_SPEECH)

        val after = Fx.cards(Fx.ran).withOption(partOfSpeech, null, form)!!

        val lexeme = after.candidate("run", "ruwiktionary/1.verb.1")
        assertEquals(emptyList<Long>(), lexeme.chosen(Fx.PART_OF_SPEECH))
        assertEquals(
            listOf("Перевод: бежать, бегать", "Часть речи: без значения"),
            lexeme.shown(form),
        )
    }

    @Test
    fun `AWP-17 a multiple choice adds an option, takes it off on repeat and clears on none`() {
        val withStyle = Fx.formWithStyle()
        val cards = Fx.cards(Fx.llegar, form = withStyle)
        val style = Fx.componentRef("llegar", "ruwiktionary/1.verb.1", Fx.STYLE)

        val offFigurative = cards.withOption(style, Fx.FIGURATIVE, withStyle)!!
        val backFigurative = offFigurative.withOption(style, Fx.FIGURATIVE, withStyle)!!
        val cleared = cards.withOption(style, null, withStyle)!!

        fun List<LemmaCardUiItem>.styles() =
            candidate("llegar", "ruwiktionary/1.verb.1").chosen(Fx.STYLE)
        assertEquals(listOf(Fx.COLLOQUIAL), offFigurative.styles())
        assertEquals(listOf(Fx.COLLOQUIAL, Fx.FIGURATIVE), backFigurative.styles())
        assertEquals(emptyList<Long>(), cleared.styles())
    }

    @Test
    fun `AWP-17 an option of a text component or of a missing lexeme is no step`() {
        val cards = Fx.cards(Fx.ran)
        val text = Fx.componentRef("run", "ruwiktionary/1.verb.1", Fx.TRANSLATION)
        val missing = Fx.componentRef("run", "ruwiktionary/9.verb.9", Fx.PART_OF_SPEECH)

        assertNull(cards.withOption(text, Fx.NOUN, form))
        assertNull(cards.withOption(missing, Fx.NOUN, form))
    }

    // AWP-18 и AWP-08 — куда класть и группа карточки

    @Test
    fun `AWP-18_1 two lemmas with the text of the card and a new word are the choice`() {
        val form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))

        assertEquals(listOf(Fx.BANK_1, Fx.BANK_2), form.existingOf("bank").map { it.wordId })
    }

    @Test
    fun `AWP-18_2 no lemma with the text gives nothing but a new word to choose`() {
        val form = Fx.form(existing = listOf(Fx.bank1))

        assertTrue(form.existingOf("run").isEmpty())
    }

    @Test
    fun `AWP-18_4 AWP-18_12 the target of one lexeme is chosen apart from the others`() {
        val second = Fx.existing(5L, "run", listOf("бег"))
        val form = Fx.form(existing = listOf(Fx.runExisting, second))
        val cards = Fx.cards(Fx.ran, form = form)

        val chosen = cards.withLexemeTarget("run", "ruwiktionary/1.verb.1", CardTargetUi.Existing(5L))!!
        val fresh = chosen.withLexemeTarget("run", "ruwiktionary/1.verb.1", CardTargetUi.NewWord)!!

        assertEquals(CardTargetUi.Existing(5L), chosen.candidate("run", "ruwiktionary/1.verb.1").target)
        assertEquals(CardTargetUi.NewWord, chosen.candidate("run", "ruwiktionary/1.verb.2").target)
        assertEquals(CardTargetUi.NewWord, fresh.candidate("run", "ruwiktionary/1.verb.1").target)
        assertEquals(5L, lexemeBatchOf(chosen.single(), "ruwiktionary/1.verb.1", form, null)!!.words.single().wordId)
        assertNull(lexemeBatchOf(fresh.single(), "ruwiktionary/1.verb.1", form, null)!!.words.single().wordId)
    }

    @Test
    fun `AWP-18_4 the target or the group of a lexeme that is not there is no step`() {
        assertNull(Fx.cards(Fx.ran).withLexemeTarget("walk", "ruwiktionary/1.verb.1", CardTargetUi.NewWord))
        assertNull(Fx.cards(Fx.ran).withLexemeGroup("run", "ruwiktionary/1.verb.9", CardOwnGroupUi.All))
    }

    @Test
    fun `AWP-08_5 AWP-08_8 own group is set and taken off per lexeme`() {
        val cards = Fx.cards(Fx.ran)

        val own = cards.withLexemeGroup("run", "ruwiktionary/1.verb.1", CardOwnGroupUi.Group(Fx.FINANCE))!!
        val taken = own.withLexemeGroup("run", "ruwiktionary/1.verb.1", CardOwnGroupUi.AsCommon)!!

        assertEquals(CardOwnGroupUi.Group(Fx.FINANCE), own.candidate("run", "ruwiktionary/1.verb.1").ownGroup)
        assertEquals(CardOwnGroupUi.AsCommon, own.candidate("run", "ruwiktionary/1.verb.2").ownGroup)
        assertEquals(CardOwnGroupUi.AsCommon, taken.candidate("run", "ruwiktionary/1.verb.1").ownGroup)
    }

    // AWP-63 — после «Создать»

    @Test
    fun `AWP-63_1 AWP-63_3 after create the lexemes with a new word take the created word, an explicit choice stays`() {
        val threeRun = Fx.word("run", Fx.lemma("run", listOf(Fx.runVerb1, Fx.runVerb2, Fx.runMachine)))
        val form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))
        val cards = Fx.cards(threeRun, form = form)
            .withLexemeTarget("run", "ruwiktionary/1.verb.2", CardTargetUi.Existing(Fx.BANK_2))!!

        val after = cards.withNewWordRetargeted("run", 9L)!!

        assertEquals(CardTargetUi.Existing(9L), after.candidate("run", "ruwiktionary/1.verb.1").target)
        assertEquals(CardTargetUi.Existing(Fx.BANK_2), after.candidate("run", "ruwiktionary/1.verb.2").target)
        assertEquals(CardTargetUi.Existing(9L), after.candidate("run", "enwiktionary/1.verb.37").target)
    }

    @Test
    fun `AWP-63_4 nothing to retarget is no step`() {
        val cards = Fx.cards(Fx.ran)
            .withLexemeTarget("run", "ruwiktionary/1.verb.1", CardTargetUi.Existing(Fx.RUN_WORD))!!
            .withLexemeTarget("run", "ruwiktionary/1.verb.2", CardTargetUi.Existing(Fx.RUN_WORD))!!

        assertNull(cards.withNewWordRetargeted("run", 9L))
        assertNull(cards.withNewWordRetargeted("walk", 9L))
    }

    // AWP-21 — выбор, ставший недействительным

    @Test
    fun `AWP-21_3 AWP-21_5 a deleted group gives the lexemes that had it as common`() {
        val cards = Fx.cards(Fx.ran, Fx.bank)
            .withLexemeGroup("run", "ruwiktionary/1.verb.1", CardOwnGroupUi.Group(Fx.FINANCE))!!
            .withLexemeGroup("bank", "ruwiktionary/1.noun.1", CardOwnGroupUi.Group(Fx.POTTER))!!

        val after = cards.withoutGroup(Fx.FINANCE)

        assertEquals(CardOwnGroupUi.AsCommon, after.candidate("run", "ruwiktionary/1.verb.1").ownGroup)
        assertEquals(CardOwnGroupUi.Group(Fx.POTTER), after.candidate("bank", "ruwiktionary/1.noun.1").ownGroup)
    }

    @Test
    fun `AWP-21_4 a deleted lemma gives the lexemes that chose it a new word`() {
        val form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))
        val cards = Fx.cards(Fx.bank, Fx.ran, form = form)
            .withLexemeTarget("bank", "ruwiktionary/1.noun.1", CardTargetUi.Existing(Fx.BANK_1))!!
            .withLexemeTarget("run", "ruwiktionary/1.verb.1", CardTargetUi.Existing(Fx.BANK_1))!!

        val after = cards.withoutTarget(Fx.BANK_1)

        assertEquals(CardTargetUi.NewWord, after.candidate("bank", "ruwiktionary/1.noun.1").target)
        assertEquals(CardTargetUi.NewWord, after.candidate("run", "ruwiktionary/1.verb.1").target)
    }

    @Test
    fun `AWP-21_4 a deleted lemma that nobody chose changes no card`() {
        val form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))
        val cards = Fx.cards(Fx.bank, form = form)

        assertEquals(cards, cards.withoutTarget(Fx.BANK_2))
    }

    @Test
    fun `AWP-21_6 a component switched off loses its values and the core marks are recounted`() {
        val cards = Fx.cards(Fx.perro)

        val withoutGloss = cards.withoutComponent(Fx.GLOSS)
        val withoutTranslation = cards.withoutComponent(Fx.TRANSLATION).withCoreMarks(form)

        val glossValues = withoutGloss
            .flatMap { it.lexemes }
            .flatMap { lexeme -> lexeme.values.filter { it.componentId == Fx.GLOSS } }
        assertTrue(glossValues.isEmpty())
        assertEquals(
            listOf(
                "Перевод: собака; пёс (также ругательство)",
                "Часть речи: существительное",
                "Род: м.р.",
                "Пример: perro callejero",
            ),
            withoutGloss.candidate("perro", "ruwiktionary/1.noun.1").shown(form),
        )
        assertTrue(withoutTranslation.flatMap { it.lexemes }.all { it.lacksCore })
        assertEquals(3, summaryOf(withoutTranslation).lacksCoreCount)
    }
}
