package me.apomazkin.candidates

import me.apomazkin.lexeme.lexemeTextKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Сборка карточек из ответа и формы (AWP-04, AWP-12, AWP-34, WCL-04), пометки
 * «есть» и «не попадёт в ядро» (AWP-07, AWP-11, WCL-06, WCL-15), сводка
 * кнопки (AWP-13).
 */
class LemmaCardsTest {

    private val form = Fx.form()

    private fun build(
        vararg words: LookupWordUi,
        form: DictionaryFormUi = this.form,
    ): CardsBuildUi = buildLemmaCards(words.toList(), form)

    /** «run» с тремя лексемами: две русские, одна английская. */
    private val threeRun = Fx.word(
        "run",
        Fx.lemma("run", listOf(Fx.runVerb1, Fx.runVerb2, Fx.runMachine)),
    )

    // AWP-04 — карточка на лемму

    @Test
    fun `AWP-04_1 ran and running give cards run and running with their source words`() {
        val cards = build(Fx.ran, Fx.running).cards

        assertEquals(listOf("run", "running"), cards.map { it.lemma })
        assertEquals(listOf("ran", "running"), cards[0].sourceWords)
        assertEquals(listOf("running"), cards[1].sourceWords)
    }

    @Test
    fun `AWP-04_2 fuera gives three cards all from fuera`() {
        val cards = build(Fx.fuera).cards

        assertEquals(listOf("fuera", "ser", "ir"), cards.map { it.lemma })
        assertTrue(cards.all { it.sourceWords == listOf("fuera") })
    }

    @Test
    fun `AWP-04_3 one lemma from two words is one card without doubled lexemes`() {
        val cards = build(Fx.ran, Fx.runs).cards

        val run = cards.single()
        assertEquals(listOf("ran", "runs"), run.sourceWords)
        assertEquals(
            listOf("ruwiktionary/1.verb.1", "ruwiktionary/1.verb.2"),
            run.lexemes.map { it.key },
        )
    }

    @Test
    fun `AWP-04_4 answer without found words gives no cards`() {
        val build = build(Fx.accorc, Fx.llave)

        assertTrue(build.cards.isEmpty())
        assertEquals(2, build.notAdded.size)
    }

    @Test
    fun `AWP-04_5 London gives a card with the noun part of speech`() {
        val cards = Fx.cards(Fx.london)

        val lexeme = cards.single().lexemes.single()
        assertEquals("London", cards.single().lemma)
        assertEquals(listOf("Перевод: Лондон", "Часть речи: существительное"), lexeme.shown(form))
    }

    @Test
    fun `AWP-04_6 three hundred lemmas of three lexemes give three hundred cards`() {
        val words = (1..300).map { index ->
            val lexemes = (1..3).map { number ->
                Fx.lexeme(
                    key = "ruwiktionary/1.noun.$number",
                    translations = listOf("значение $index.$number"),
                    matches = listOf(Fx.byField(Fx.TRANSLATION, "translations")),
                )
            }
            Fx.word("word$index", Fx.lemma("lemma$index", lexemes))
        }

        val cards = build(*words.toTypedArray()).cards
        val summary = summaryOf(cards)

        assertEquals((1..300).map { "lemma$it" }, cards.map { it.lemma })
        assertTrue(cards.all { it.lexemes.size == 3 })
        assertEquals(900, summary.lexemeCount)
        assertEquals(300, summary.wordCount)
        assertEquals("900 значений в 300 слов", countPhrase(summary.lexemeCount, summary.wordCount))
    }

    @Test
    fun `AWP-04_8 ran and running give one run card with lexemes of both in answer order`() {
        val run = Fx.cards(Fx.ran, Fx.running).first { it.lemma == "run" }

        assertEquals(
            listOf("ruwiktionary/1.verb.1", "ruwiktionary/1.verb.2", "enwiktionary/1.verb.37"),
            run.lexemes.map { it.key },
        )
        // `ruwiktionary/1.verb.1` целиком из «ran»: перевод и часть речи не взяты у «running».
        assertEquals(
            listOf("Перевод: бежать, бегать", "Часть речи: глагол"),
            run.lexemes.first().shown(form),
        )
    }

    @Test
    fun `AWP-04_9 found word without lemmas gives no card`() {
        val build = build(Fx.foundWithoutLemmas("bank"))

        assertTrue(build.cards.isEmpty())
        assertEquals(listOf(NotAddedWordUiItem("bank", NotAddedReason.NO_LEXEMES)), build.notAdded)
    }

    @Test
    fun `AWP-04_10 the window opens with every card whole and without own group`() {
        val form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))

        val cards = build(Fx.ran, Fx.bank, form = form).cards

        assertTrue(cards.all { it.ownGroup == CardOwnGroupUi.AsCommon })
        assertEquals(listOf(0, 2), cards.map { it.existingCount })
        assertEquals(listOf(listOf("ran"), listOf("bank")), cards.map { it.sourceWords })
    }

    @Test
    fun `AWP-11_5 erasing the only core of a lexeme drops it from the card, the rest keep their order`() {
        val translation = Fx.ref("perro", "ruwiktionary/1.noun.2", Fx.TRANSLATION)
        val before = Fx.cards(Fx.perro).single().lexemes.map { it.key }
        val cards = Fx.cards(Fx.perro)
            .withTextEdit(translation, "")!!
            .droppingEmptyTexts(translation, form)!!

        val keys = cards.single().lexemes.map { it.key }

        assertEquals(before.filterNot { it == "ruwiktionary/1.noun.2" }, keys)
    }

    @Test
    fun `AWP-04_12 lexemes of the card keep the answer order with the main one first`() {
        val correr = build(Fx.corrian).cards.single()

        assertEquals(
            listOf("ruwiktionary/1.verb.3", "ruwiktionary/1.verb.1", "ruwiktionary/1.verb.5"),
            correr.lexemes.map { it.key },
        )
        assertEquals("third-person plural imperfect indicative", correr.formNote)
    }

    // AWP-12 — слова без добавления

    @Test
    fun `AWP-12_1 not found and error words go to not added with reasons in answer order`() {
        val build = build(Fx.accorc, Fx.ran, Fx.llave)

        assertEquals(
            listOf(
                NotAddedWordUiItem("accorc", NotAddedReason.NOT_FOUND),
                NotAddedWordUiItem("llave", NotAddedReason.ERROR),
            ),
            build.notAdded,
        )
        assertEquals(listOf("run"), build.cards.map { it.lemma })
    }

    @Test
    fun `AWP-12_3 all words not found give no cards and every word in not added`() {
        val build = build(Fx.accorc, Fx.notFound("qwrt"))

        assertTrue(build.cards.isEmpty())
        assertEquals(listOf("accorc", "qwrt"), build.notAdded.map { it.text })
        assertTrue(build.notAdded.all { it.reason == NotAddedReason.NOT_FOUND })
    }

    @Test
    fun `AWP-12_6 banco error without lemmas is listed as not loaded`() {
        val build = build(Fx.banco)

        assertEquals(listOf(NotAddedWordUiItem("banco", NotAddedReason.ERROR)), build.notAdded)
    }

    @Test
    fun `AWP-12_7 found bank without lemmas is listed as without values`() {
        val build = build(Fx.foundWithoutLemmas("bank"))

        assertEquals(listOf(NotAddedWordUiItem("bank", NotAddedReason.NO_LEXEMES)), build.notAdded)
    }

    @Test
    fun `AWP-12_8 eight hundred words with four hundred fifty errors give as many not added`() {
        val failed = (1..450).map { Fx.failed("error$it") }
        val found = (1..350).map { Fx.word("w$it", Fx.lemma("l$it", listOf(Fx.runVerb1))) }

        val build = build(*(failed + found).toTypedArray())

        assertEquals(450, build.notAdded.size)
        assertEquals(350, build.cards.size)
    }

    @Test
    fun `AWP-12_9 all words error give no cards and every word in not added`() {
        val build = build(Fx.llave, Fx.banco)

        assertTrue(build.cards.isEmpty())
        assertEquals(listOf("llave", "banco"), build.notAdded.map { it.text })
        assertTrue(build.notAdded.all { it.reason == NotAddedReason.ERROR })
    }

    // AWP-18 — куда класть по умолчанию

    @Test
    fun `AWP-18_3 default target is the newest word with this text, otherwise a new word`() {
        val form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))

        val cards = build(Fx.bank, Fx.fuera, form = form).cards

        assertEquals(CardTargetUi.Existing(Fx.BANK_1), cards.first { it.lemma == "bank" }.target)
        assertEquals(CardTargetUi.NewWord, cards.first { it.lemma == "ser" }.target)
    }

    // AWP-34 — пометка «англ.»

    @Test
    fun `AWP-34_1 zarpo marks both lexemes with english`() {
        val cards = Fx.cards(Fx.zarpo)

        assertTrue(cards.candidate("zarpar", "enwiktionary/1.verb.1").isEnglishFallback)
        assertTrue(cards.candidate("zarpar", "enwiktionary/2.verb.1").isEnglishFallback)
    }

    @Test
    fun `AWP-34_2 without a remark there is no english mark`() {
        val english = Fx.form(
            components = listOf(Fx.translation, Fx.partOfSpeech),
            translationLanguage = "en",
        )
        val verb = Fx.lexeme(
            key = "enwiktionary/1.verb.1",
            gloss = "to run, to jog",
            partOfSpeechKey = "verb",
            matches = listOf(
                Fx.byField(Fx.TRANSLATION, "gloss"),
                Fx.byOption(Fx.PART_OF_SPEECH, Fx.VERB),
            ),
        )
        val correr = Fx.lemma(
            text = "correr",
            lexemes = listOf(verb),
            formNote = "third-person plural imperfect indicative",
        )

        val lexeme = Fx.cards(Fx.word("corrían", correr), form = english)
            .candidate("correr", "enwiktionary/1.verb.1")

        assertEquals(listOf("Перевод: to run, to jog", "Часть речи: глагол"), lexeme.shown(english))
        assertFalse(lexeme.isEnglishFallback)
    }

    @Test
    fun `AWP-34_3 only the lexeme named in the remark gets the mark`() {
        val onlySecond = Fx.zarpo.copy(
            remarks = listOf(Fx.noTranslation("zarpar", "enwiktionary/2.verb.1")),
        )

        val cards = Fx.cards(onlySecond)

        assertTrue(cards.candidate("zarpar", "enwiktionary/2.verb.1").isEnglishFallback)
        assertFalse(cards.candidate("zarpar", "enwiktionary/1.verb.1").isEnglishFallback)
    }

    @Test
    fun `AWP-34_3 a remark of another lemma with the same key does not mark the lexeme`() {
        val forIr = Fx.fuera.copy(remarks = listOf(Fx.noTranslation("ir", "enwiktionary/1.verb.1")))

        val cards = Fx.cards(forIr)

        assertTrue(cards.candidate("ir", "enwiktionary/1.verb.1").isEnglishFallback)
        assertFalse(cards.candidate("ser", "enwiktionary/1.verb.1").isEnglishFallback)
    }

    @Test
    fun `AWP-34_4 AWP-16_7 a hand-made translation keeps the english mark`() {
        val translation = Fx.ref("zarpar", "enwiktionary/1.verb.1", Fx.TRANSLATION)

        val edited = Fx.cards(Fx.zarpo).withTextEdit(translation, "отплыть")!!

        val lexeme = edited.candidate("zarpar", "enwiktionary/1.verb.1")
        assertEquals(listOf("отплыть"), lexeme.texts(Fx.TRANSLATION))
        assertTrue(lexeme.isEnglishFallback)
    }

    // AWP-07 — пометка «есть»

    @Test
    fun `AWP-07_1 existing lemma marks the card and the lexeme with the same translation`() {
        val form = Fx.form(existing = listOf(Fx.runExisting))

        val run = Fx.cards(Fx.ran, form = form).single()

        assertEquals(1, run.existingCount)
        assertTrue(run.lexemes.first { it.key == "ruwiktionary/1.verb.2" }.exists)
        assertFalse(run.lexemes.first { it.key == "ruwiktionary/1.verb.1" }.exists)
    }

    @Test
    fun `AWP-07_2 two lemmas bank in the dictionary give the count two`() {
        val form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))

        val bank = Fx.cards(Fx.bank, form = form).single()

        assertEquals(2, bank.existingCount)
    }

    @Test
    fun `AWP-07_3 case of the lemma does not matter`() {
        val form = Fx.form(existing = listOf(Fx.existing(5L, "Run", listOf("управлять"))))

        val run = Fx.cards(Fx.ran, form = form).single()

        assertEquals(1, run.existingCount)
        assertTrue(run.lexemes.first { it.key == "ruwiktionary/1.verb.2" }.exists)
    }

    @Test
    fun `AWP-07_3 the text of the lemma is compared in the unicode normal form`() {
        val composed = "Май"
        val decomposed = "Май"
        val form = Fx.form(existing = listOf(Fx.existing(5L, decomposed, listOf("x"))))

        assertEquals(1, form.existingOf(composed).size)
    }

    @Test
    fun `AWP-07_4 stress mark of the dictionary value does not matter`() {
        val stressed = "бежа́ть, бегать"
        val form = Fx.form(existing = listOf(Fx.existing(5L, "run", listOf(stressed))))

        val run = Fx.cards(Fx.ran, form = form).single()

        assertTrue(run.lexemes.first { it.key == "ruwiktionary/1.verb.1" }.exists)
    }

    @Test
    fun `AWP-07_5 definition only dictionary marks by the definition`() {
        val definitions = Fx.form(
            components = listOf(Fx.definition),
            existing = listOf(Fx.existing(5L, "run", listOf("To move swiftly."))),
        )
        val swiftly = Fx.lexeme(
            key = "enwiktionary/1.verb.g1",
            gloss = "To move swiftly.",
            matches = listOf(Fx.byField(Fx.DEFINITION, "gloss")),
        )
        val word = Fx.word("run", Fx.lemma("run", listOf(swiftly)))

        val run = Fx.cards(word, form = definitions).single()

        val lexeme = run.lexemes.single()
        assertEquals(listOf("Определение: To move swiftly."), lexeme.shown(definitions))
        assertTrue(lexeme.exists)
    }

    @Test
    fun `AWP-07_6 yo and ye are different values`() {
        val form = Fx.form(existing = listOf(Fx.existing(5L, "ёж", listOf("ёж"))))
        val hedgehog = Fx.lexeme(
            key = "ruwiktionary/1.noun.1",
            translations = listOf("еж"),
            matches = listOf(Fx.byField(Fx.TRANSLATION, "translations")),
        )
        val word = Fx.word("ёж", Fx.lemma("ёж", listOf(hedgehog)))

        val lexeme = Fx.cards(word, form = form).single().lexemes.single()

        assertFalse(lexeme.exists)
    }

    @Test
    fun `AWP-07_7 a translation shortened by hand becomes exists after the edit`() {
        val take = Fx.lexeme(
            key = "ruwiktionary/1.verb.1",
            translations = listOf("брать; хватать; взять, схватить (также перен.)"),
            matches = listOf(Fx.byField(Fx.TRANSLATION, "translations")),
        )
        val word = Fx.word("take", Fx.lemma("take", listOf(take)))
        val form = Fx.form(existing = listOf(Fx.existing(5L, "take", listOf("брать"))))
        val cards = Fx.cards(word, form = form)
        val translation = Fx.ref("take", "ruwiktionary/1.verb.1", Fx.TRANSLATION)

        val edited = cards.withTextEdit(translation, "брать")!!.withExistsMarks(form)

        assertFalse(cards.single().lexemes.single().exists)
        assertTrue(edited.single().lexemes.single().exists)
    }

    @Test
    fun `AWP-07_8 no lemma with the text gives no marks`() {
        val form = Fx.form(existing = listOf(Fx.runExisting, Fx.bank1))

        val cards = Fx.cards(Fx.fuera, form = form)

        assertTrue(cards.all { it.existingCount == 0 })
        assertTrue(cards.flatMap { it.lexemes }.none { it.exists })
    }

    @Test
    fun `AWP-07_10 a hand-made text not in the dictionary removes the exists mark`() {
        val form = Fx.form(existing = listOf(Fx.existing(5L, "run", listOf("бежать, бегать"))))
        val cards = Fx.cards(Fx.ran, form = form)
        val translation = Fx.ref("run", "ruwiktionary/1.verb.1", Fx.TRANSLATION)

        val edited = cards.withTextEdit(translation, "бегать быстро")!!.withExistsMarks(form)

        assertTrue(cards.candidate("run", "ruwiktionary/1.verb.1").exists)
        assertFalse(edited.candidate("run", "ruwiktionary/1.verb.1").exists)
    }

    @Test
    fun `AWP-07_11 values of parameters are not compared`() {
        val gloss = "student (a person who studies…)"
        val keys = setOf(lexemeTextKey("студент, учащийся"), lexemeTextKey(gloss))
        val existing = Fx.existing(5L, "estudiante", listOf("студент, учащийся"))
            .copy(lexemeTextKeys = keys)
        val form = Fx.form(existing = listOf(existing))

        val lexeme = Fx.cards(Fx.estudiante, form = form).single().lexemes.single()

        assertEquals(listOf("Перевод: студентка", "Толкование: $gloss"), lexeme.shown(form))
        assertFalse(lexeme.exists)
    }

    // AWP-11 — ядро у каждой лексемы

    /** Лексема только с «Толкованием»: ядра нет. Сервер такую не отдаёт, руками — можно. */
    private val glossOnly = Fx.word(
        "run",
        Fx.lemma(
            text = "run",
            lexemes = listOf(
                Fx.runVerb1,
                Fx.lexeme(
                    key = "enwiktionary/1.verb.g1",
                    gloss = "To move swiftly.",
                    matches = listOf(Fx.byField(Fx.GLOSS, "gloss")),
                ),
            ),
        ),
    )

    @Test
    fun `AWP-11_1 a translation in the core gives no mark`() {
        val cards = Fx.cards(Fx.ran)

        assertTrue(cards.flatMap { it.lexemes }.none { it.lacksCore })
    }

    @Test
    fun `AWP-11_3 deleting the lexeme without a core enables add`() {
        val cards = Fx.cards(glossOnly)
        val beforeDelete = summaryOf(cards)

        val afterDelete = summaryOf(cards.deletingLexeme("run", "enwiktionary/1.verb.g1")!!)

        assertFalse(beforeDelete.isEnabled)
        assertEquals(1, beforeDelete.lacksCoreCount)
        assertTrue(afterDelete.isEnabled)
        assertEquals(0, afterDelete.lacksCoreCount)
    }

    @Test
    fun `AWP-11_5 erasing the only core text deletes the lexeme and the count drops`() {
        val translation = Fx.ref("run", "ruwiktionary/1.verb.1", Fx.TRANSLATION)
        val before = summaryOf(Fx.cards(Fx.ran).withCoreMarks(form))

        val erased = Fx.cards(Fx.ran)
            .withTextEdit(translation, "")!!
            .droppingEmptyTexts(translation, form)!!
            .withCoreMarks(form)

        val summary = summaryOf(erased)
        assertNull(erased.lexemeOf("run", "ruwiktionary/1.verb.1"))
        assertEquals(before.lexemeCount - 1, summary.lexemeCount)
        assertEquals(0, summary.lacksCoreCount)
    }

    // AWP-13 — кнопка «добавить»

    @Test
    fun `AWP-13_1 two lexemes of a new lemma and one of an existing give three in two words`() {
        val form = Fx.form(existing = listOf(Fx.bank1))

        val summary = summaryOf(Fx.cards(Fx.ran, Fx.bank, form = form))

        assertEquals(3, summary.lexemeCount)
        assertEquals(2, summary.wordCount)
        assertEquals(0, summary.existingCount)
        assertTrue(summary.isEnabled)
        assertEquals("3 значения в 2 слова", countPhrase(summary.lexemeCount, summary.wordCount))
    }

    @Test
    fun `AWP-13_2 everything deleted gives nothing to add`() {
        val cards = Fx.cards(Fx.ran, Fx.bank)

        val empty = cards.deletingCard("run")!!.deletingCard("bank")!!

        val summary = summaryOf(empty)
        assertEquals(0, summary.lexemeCount)
        assertEquals(0, summary.wordCount)
        assertFalse(summary.isEnabled)
    }

    @Test
    fun `AWP-13_3 two lexemes without a core value from the server disable the button`() {
        val withoutCore = Fx.cards(Fx.ran)
            .withoutComponent(Fx.TRANSLATION)
            .withCoreMarks(form)

        val summary = summaryOf(withoutCore)
        assertEquals(withoutCore.single().lexemes.size, summary.lacksCoreCount)
        assertFalse(summary.isEnabled)
    }

    @Test
    fun `AWP-13_5 one of three lexemes exists, the button counts two in one word`() {
        val form = Fx.form(existing = listOf(Fx.runExisting))

        val summary = summaryOf(Fx.cards(threeRun, form = form))

        assertEquals(2, summary.lexemeCount)
        assertEquals(1, summary.wordCount)
        assertEquals(1, summary.existingCount)
        assertTrue(summary.isEnabled)
        assertEquals("2 значения в 1 слово", countPhrase(summary.lexemeCount, summary.wordCount))
    }

    @Test
    fun `AWP-13_6 only existing lexemes left give nothing to add`() {
        val existing = Fx.existing(5L, "run", listOf("бежать, бегать", "управлять"))
        val form = Fx.form(existing = listOf(existing))

        val summary = summaryOf(Fx.cards(Fx.ran, form = form))

        assertEquals(0, summary.lexemeCount)
        assertEquals(0, summary.wordCount)
        assertEquals(2, summary.existingCount)
        assertFalse(summary.isEnabled)
    }

    // WCL-04 — прямая лемма и леммы-формы

    private val rin = Fx.lemma(
        text = "rin",
        lexemes = listOf(
            Fx.lexeme(
                key = "enwiktionary/1.verb.1",
                gloss = "To run.",
                matches = listOf(Fx.byField(Fx.TRANSLATION, "gloss")),
            ),
        ),
        formNote = "past participle",
    )

    private fun runLemma(formNote: String?) = Fx.lemma(
        text = "run",
        lexemes = listOf(Fx.runVerb1, Fx.runVerb2, Fx.runMachine),
        formNote = formNote,
    )

    @Test
    fun `WCL-04_1 run gives the direct lemma first and the form of rin second`() {
        val word = Fx.word("run", rin, runLemma(formNote = null))

        val cards = buildCandidateCards(word, "run", form)

        assertEquals(listOf("run", "rin"), cards.map { it.lemma })
        assertNull(cards[0].formNote)
        assertEquals("past participle", cards[1].formNote)
    }

    @Test
    fun `WCL-04_2 running gives the direct lemma running first and then the form of run`() {
        val running = Fx.lemma(
            text = "running",
            lexemes = listOf(Fx.runningNoun, Fx.runningNoun.copy(key = "enwiktionary/1.noun.2")),
        )
        val word = Fx.word("running", runLemma(formNote = "present participle and gerund"), running)

        val cards = buildCandidateCards(word, "running", form)

        assertEquals(listOf("running", "run"), cards.map { it.lemma })
        assertNull(cards[0].formNote)
        assertEquals("present participle and gerund", cards[1].formNote)
    }

    @Test
    fun `WCL-04_3 ran gives three sections with the direct lemma first even with one lexeme`() {
        val ran = Fx.lemma(
            text = "ran",
            lexemes = listOf(Fx.runVerb1.copy(key = "enwiktionary/1.verb.5")),
        )
        val word = Fx.word("ran", runLemma(formNote = "simple past"), rin, ran)

        val cards = buildCandidateCards(word, "ran", form)

        assertEquals(listOf("ran", "run", "rin"), cards.map { it.lemma })
        assertEquals(listOf(1, 3, 1), cards.map { it.lexemes.size })
    }

    @Test
    fun `WCL-04_4 corrian gives one section, the form of correr with the note of the server`() {
        val cards = buildCandidateCards(Fx.corrian, "corrían", form)

        assertEquals(1, cards.size)
        assertEquals("correr", cards.single().lemma)
        assertEquals("third-person plural imperfect indicative", cards.single().formNote)
    }

    @Test
    fun `WCL-04_5 case and edges of the word do not matter for the direct lemma`() {
        val word = Fx.word("Run", runLemma(formNote = "simple past"))

        val cards = buildCandidateCards(word, " Run ", form)

        assertNull(cards.single().formNote)
    }

    @Test
    fun `WCL-04_7 found word without lemmas gives no sections`() {
        assertTrue(buildCandidateCards(Fx.foundWithoutLemmas("bank"), "bank", form).isEmpty())
    }

    @Test
    fun `WCL-04_7 a lemma without lexemes gets no section`() {
        val empty = Fx.lemma("rin", emptyList(), formNote = "past participle")
        val word = Fx.word("run", empty, runLemma(formNote = null))

        assertEquals(listOf("run"), buildCandidateCards(word, "run", form).map { it.lemma })
    }

    @Test
    fun `WCL-04_8 perro gives lexemes in the order of the selection with the main one first`() {
        val perro = buildCandidateCards(Fx.perro, "perro", form).single()

        assertEquals(
            listOf("ruwiktionary/1.noun.1", "ruwiktionary/2.adjective.1", "ruwiktionary/1.noun.2"),
            perro.lexemes.map { it.key },
        )
    }

    // WCL-06 — «есть» по слову диалога

    private val runKeys = setOf(lexemeTextKey("управлять"))

    @Test
    fun `WCL-06_1 a lexeme equal to a lexeme of the dialog word is exists and not counted`() {
        val cards = buildCandidateCards(Fx.ran, "run", form).withExistsMarks(runKeys, form)

        val summary = summaryOf(cards)
        assertTrue(cards.candidate("run", "ruwiktionary/1.verb.2").exists)
        assertFalse(cards.candidate("run", "ruwiktionary/1.verb.1").exists)
        assertEquals(1, summary.existingCount)
        assertEquals(1, summary.lexemeCount)
    }

    @Test
    fun `WCL-06_2 a same-named other word does not mark, only the keys of the dialog word do`() {
        val other = Fx.existing(Fx.BANK_2, "run", listOf("бежать, бегать"))
        val form = Fx.form(existing = listOf(other, Fx.runExisting))
        val dialogWordKeys = Fx.runExisting.lexemeTextKeys

        val cards = buildCandidateCards(Fx.ran, "run", form).withExistsMarks(dialogWordKeys, form)

        assertFalse(cards.candidate("run", "ruwiktionary/1.verb.1").exists)
        assertTrue(cards.candidate("run", "ruwiktionary/1.verb.2").exists)
    }

    @Test
    fun `WCL-06_3 a word without lexemes gives no marks`() {
        val cards = buildCandidateCards(Fx.ran, "run", form).withExistsMarks(emptySet(), form)

        assertTrue(cards.flatMap { it.lexemes }.none { it.exists })
    }

    @Test
    fun `WCL-06_4 all lexemes exist, nothing to add and the button is off`() {
        val keys = setOf(lexemeTextKey("бежать, бегать"), lexemeTextKey("управлять"))
        val cards = buildCandidateCards(Fx.ran, "run", form).withExistsMarks(keys, form)

        val summary = summaryOf(cards)

        assertEquals(0, summary.lexemeCount)
        assertEquals(2, summary.existingCount)
        assertFalse(summary.isEnabled)
    }

    // WCL-15 — «есть в «correr»» и счёт совпадений

    private val correrForm = Fx.form(existing = listOf(Fx.correrExisting))

    private val corrianKeys = setOf(lexemeTextKey("течь, литься, струиться (о жидкости)"))

    @Test
    fun `WCL-15_4 a lexeme equal to one of the lemma word gets the mark in lemma and the count`() {
        val cards = buildCandidateCards(Fx.corrian, "corrían", correrForm)
            .withExistsMarks(corrianKeys, correrForm)
            .withLemmaExistsMarks(correrForm)

        val correr = cards.single()
        assertTrue(correr.lexemes.first { it.key == "ruwiktionary/1.verb.1" }.existsInLemma)
        assertFalse(correr.lexemes.first { it.key == "ruwiktionary/1.verb.5" }.existsInLemma)
        assertTrue(correr.lexemes.first { it.key == "ruwiktionary/1.verb.3" }.exists)
        assertEquals(mapOf("correr" to 1), sameValuesOf(cards, correrForm, ownKeys = corrianKeys))
    }

    @Test
    fun `WCL-15_4 a value that both the dialog word and the lemma word have adds to the count`() {
        val cards = buildCandidateCards(Fx.corrian, "corrían", correrForm)
            .withLemmaExistsMarks(correrForm)
        val shared = corrianKeys + lexemeTextKey("Бегать; бежать")

        assertEquals(mapOf("correr" to 2), sameValuesOf(cards, correrForm, ownKeys = shared))
    }

    @Test
    fun `WCL-15_4 no word with the text of the lemma gives no marks and no count`() {
        val none = Fx.form(existing = emptyList())
        val cards = buildCandidateCards(Fx.corrian, "corrían", none).withLemmaExistsMarks(none)

        assertTrue(cards.flatMap { it.lexemes }.none { it.existsInLemma })
        assertEquals(emptyMap<String, Int>(), sameValuesOf(cards, none, ownKeys = corrianKeys))
    }

    @Test
    fun `WCL-15_5 the direct lemma has no mark in lemma`() {
        val existing = Fx.existing(5L, "run", listOf("бежать, бегать"))
        val form = Fx.form(existing = listOf(existing))
        val word = Fx.word("run", runLemma(formNote = null))

        val cards = buildCandidateCards(word, "run", form).withLemmaExistsMarks(form)

        assertTrue(cards.single().lexemes.none { it.existsInLemma })
        assertEquals(emptyMap<String, Int>(), sameValuesOf(cards, form, ownKeys = emptySet()))
    }
}
