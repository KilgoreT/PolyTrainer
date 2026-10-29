package me.apomazkin.candidates

import me.apomazkin.lexeme.lexemeTextKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Карточка леммы под словом из ответа и формы (AWP-18, AWP-43, AWP-44,
 * AWP-46, AWP-34), разделы диалога карточки слова (WCL-04), пометки «уже в
 * словаре», «есть» и «не попадёт в ядро» (AWP-50, AWP-11, WCL-06, WCL-15),
 * сводка кнопки диалога (AWP-13).
 */
class LemmaCardsTest {

    private val form = Fx.form()

    /** «run» с тремя лексемами: две русские, одна английская. */
    private val threeRun = Fx.word(
        "run",
        Fx.lemma("run", listOf(Fx.runVerb1, Fx.runVerb2, Fx.runMachine)),
    )

    // AWP-18 — пустая карточка леммы и «куда класть»

    @Test
    fun `AWP-18_3 AWP-18_2 empty card targets the newest lemma with this text, otherwise a new word`() {
        val form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))

        val bank = emptyLemmaCard("bank", form)
        val ser = emptyLemmaCard("ser", form)

        assertEquals(CardTargetUi.Existing(Fx.BANK_1), bank.target)
        assertEquals(2, bank.existingCount)
        assertEquals(CardTargetUi.NewWord, ser.target)
        assertEquals(0, ser.existingCount)
        assertTrue(bank.lexemes.isEmpty() && ser.lexemes.isEmpty())
        assertEquals(CardOwnGroupUi.AsCommon, bank.ownGroup)
        assertEquals(listOf("bank"), bank.sourceWords)
        assertTrue(form.targetRowsOf("ser").isEmpty())
    }

    @Test
    fun `AWP-18_1 AWP-18_9 AWP-18_10 target rows carry core values count groups and the add date of each lemma newest first`() {
        val newest = Fx.existing(Fx.BANK_1, "bank", listOf("банк", "банкомёт"), groups = listOf("Finance"), addedAt = 1_760_000_000_000L)
        val older = Fx.existing(Fx.BANK_2, "bank", listOf("берег"), addedAt = 1_756_000_000_000L)
        val form = Fx.form(existing = listOf(newest, older))

        val rows = form.targetRowsOf("Bank")

        assertEquals(
            listOf(
                TargetRowUi(Fx.BANK_1, listOf("банк", "банкомёт"), 2, listOf("Finance"), 1_760_000_000_000L),
                TargetRowUi(Fx.BANK_2, listOf("берег"), 1, emptyList(), 1_756_000_000_000L),
            ),
            rows,
        )
    }

    @Test
    fun `AWP-46_3 saved lexemes come from the chosen lemma of the dictionary and a new word has none`() {
        val newest = Fx.existing(Fx.BANK_1, "bank", listOf("банк"), lexemes = listOf(Fx.saved(1L, "банк")))
        val older = Fx.existing(Fx.BANK_2, "bank", listOf("берег"), lexemes = listOf(Fx.saved(2L, "берег")))
        val form = Fx.form(existing = listOf(newest, older))

        assertEquals(listOf(Fx.saved(1L, "банк")), form.savedLexemesOf(CardTargetUi.Existing(Fx.BANK_1)))
        assertEquals(listOf(Fx.saved(2L, "берег")), form.savedLexemesOf(CardTargetUi.Existing(Fx.BANK_2)))
        assertTrue(form.savedLexemesOf(CardTargetUi.NewWord).isEmpty())
        assertTrue(form.savedLexemesOf(CardTargetUi.Existing(999L)).isEmpty())
    }

    @Test
    fun `AWP-50_1 the keys for the mark come from the chosen lemma only and a new word gives none`() {
        val form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))

        assertEquals(Fx.bank2.lexemeTextKeys, form.lexemeKeysOf(CardTargetUi.Existing(Fx.BANK_2)))
        assertEquals(Fx.bank1.lexemeTextKeys, form.lexemeKeysOf(CardTargetUi.Existing(Fx.BANK_1)))
        assertTrue(form.lexemeKeysOf(CardTargetUi.NewWord).isEmpty())
    }

    @Test
    fun `AWP-43_7 the form takes the lemmas of the dictionary it did not have and skips the known ones`() {
        val form = Fx.form(existing = listOf(Fx.runExisting))
        val child = Fx.existing(5L, "child", listOf("ребёнок"))

        val extended = form.withExistingLemmas(listOf(child, Fx.runExisting))

        assertEquals(listOf(Fx.runExisting, child), extended.existingLemmas)
        assertEquals(form, form.withExistingLemmas(listOf(Fx.runExisting)))
    }

    // AWP-43, AWP-44 — карточка из слова ответа

    private val corrianTapped = Fx.corrian.copy(
        lemmas = listOf(
            Fx.lemma(
                text = "correr",
                lexemes = listOf(Fx.corrianVerb3.copy(inContext = true), Fx.corrianVerb1, Fx.corrianVerb5),
                formNote = "third-person plural imperfect indicative",
            ),
        ),
    )

    @Test
    fun `AWP-43_1 AWP-43_4 the card of corrian keeps the selection order and marks only the first lexeme in context`() {
        val card = lemmaCardOf(corrianTapped, emptyLemmaCard("corrían", form), form)

        assertEquals("correr", card.lemma)
        assertEquals(listOf("corrían"), card.sourceWords)
        assertEquals("third-person plural imperfect indicative", card.formNote)
        assertEquals(
            listOf("ruwiktionary/1.verb.3", "ruwiktionary/1.verb.1", "ruwiktionary/1.verb.5"),
            card.lexemes.map { it.key },
        )
        assertEquals(listOf(true, false, false), card.lexemes.map { it.isInContext })
        assertEquals(
            listOf("Перевод: течь, литься, струиться (о жидкости)", "Часть речи: глагол", "Толкование: to flow"),
            card.lexemes.first().shown(form),
        )
    }

    @Test
    fun `AWP-43_7 the same lemma keeps the target and the group of the prior card, another lemma is built from the form anew`() {
        val child = Fx.existing(5L, "child", listOf("ребёнок"))
        val form = Fx.form(existing = listOf(Fx.runExisting, child))
        val prior = emptyLemmaCard("run", form).copy(ownGroup = CardOwnGroupUi.Group(Fx.FINANCE))
        val children = Fx.word(
            "children",
            Fx.lemma("child", listOf(Fx.runVerb1.copy(key = "enwiktionary/1.noun.1")), formNote = "plural"),
        )

        val same = lemmaCardOf(Fx.ran, prior, form)
        val other = lemmaCardOf(children, emptyLemmaCard("children", form), form)

        assertEquals(CardTargetUi.Existing(Fx.RUN_WORD), same.target)
        assertEquals(CardOwnGroupUi.Group(Fx.FINANCE), same.ownGroup)
        assertEquals(listOf("ran"), same.sourceWords)
        assertEquals("child", other.lemma)
        assertEquals(CardTargetUi.Existing(5L), other.target)
        assertEquals(1, other.existingCount)
        assertEquals("plural", other.formNote)
        assertEquals(listOf("children"), other.sourceWords)
    }

    @Test
    fun `AWP-43_7 a word whose lemmas carry no lexemes keeps the prior card without lexemes`() {
        val prior = emptyLemmaCard("bank", form)

        val card = lemmaCardOf(Fx.foundWithoutLemmas("bank"), prior, form)

        assertEquals(prior, card)
    }

    @Test
    fun `AWP-44_1 values of the card go cores first then parameters then examples whatever the dictionary order`() {
        val examplesFirst = Fx.form(
            components = listOf(Fx.translation, Fx.example.copy(position = 1), Fx.partOfSpeech.copy(position = 2)),
        )

        val card = lemmaCardOf(Fx.tiempo, emptyLemmaCard("tiempo", examplesFirst), examplesFirst)

        assertEquals(
            listOf(Fx.TRANSLATION, Fx.PART_OF_SPEECH, Fx.EXAMPLE),
            card.lexemes.single().values.map { it.componentId },
        )
    }

    @Test
    fun `AWP-44_2 AWP-44_3 a core gloss follows the translation and the gender follows the part of speech it depends on`() {
        val glossCore = Fx.form(
            components = listOf(Fx.translation, Fx.partOfSpeech, Fx.gender, Fx.example, Fx.gloss.copy(isCore = true)),
        )

        val card = lemmaCardOf(Fx.perro, emptyLemmaCard("perro", glossCore), glossCore)

        assertEquals(
            listOf(Fx.TRANSLATION, Fx.GLOSS, Fx.PART_OF_SPEECH, Fx.GENDER, Fx.EXAMPLE),
            card.lexemes.first().values.map { it.componentId },
        )
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
        val ser = Fx.fuera.lemmas.first { it.text == "ser" }
        val ir = Fx.fuera.lemmas.first { it.text == "ir" }
        val remarkForIr = listOf(Fx.noTranslation("ir", "enwiktionary/1.verb.1"))

        val irCard = Fx.cards(Fx.word("fuera", ir, remarks = remarkForIr))
        val serCard = Fx.cards(Fx.word("fuera", ser, remarks = remarkForIr))

        assertTrue(irCard.candidate("ir", "enwiktionary/1.verb.1").isEnglishFallback)
        assertFalse(serCard.candidate("ser", "enwiktionary/1.verb.1").isEnglishFallback)
    }

    @Test
    fun `AWP-34_4 AWP-16_7 a hand-made translation keeps the english mark`() {
        val translation = Fx.ref("zarpar", "enwiktionary/1.verb.1", Fx.TRANSLATION)

        val edited = Fx.cards(Fx.zarpo).withTextEdit(translation, "отплыть")!!

        val lexeme = edited.candidate("zarpar", "enwiktionary/1.verb.1")
        assertEquals(listOf("отплыть"), lexeme.texts(Fx.TRANSLATION))
        assertTrue(lexeme.isEnglishFallback)
    }

    // AWP-50 — пометка «уже в словаре» по выбранной лемме словаря

    @Test
    fun `AWP-50_1 the lexeme equal to a value of the chosen lemma is already in the dictionary, the other is not`() {
        val form = Fx.form(existing = listOf(Fx.runExisting))

        val run = Fx.cards(Fx.ran, form = form).single()

        assertEquals(1, run.existingCount)
        assertTrue(run.lexemes.first { it.key == "ruwiktionary/1.verb.2" }.exists)
        assertFalse(run.lexemes.first { it.key == "ruwiktionary/1.verb.1" }.exists)
    }

    @Test
    fun `AWP-50_1 AWP-18_8 the mark follows the chosen lemma, not every lemma with the text`() {
        val newest = Fx.existing(Fx.BANK_1, "run", listOf("бежать, бегать"))
        val older = Fx.existing(Fx.BANK_2, "run", listOf("управлять"))
        val form = Fx.form(existing = listOf(newest, older))

        val byNewest = Fx.cards(Fx.ran, form = form).single()
        val byOlder = listOf(byNewest.copy(target = CardTargetUi.Existing(Fx.BANK_2)))
            .withExistsMarks(form.lexemeKeysOf(CardTargetUi.Existing(Fx.BANK_2)), form)
            .single()

        assertEquals(CardTargetUi.Existing(Fx.BANK_1), byNewest.target)
        assertTrue(byNewest.lexemes.first { it.key == "ruwiktionary/1.verb.1" }.exists)
        assertFalse(byNewest.lexemes.first { it.key == "ruwiktionary/1.verb.2" }.exists)
        assertFalse(byOlder.lexemes.first { it.key == "ruwiktionary/1.verb.1" }.exists)
        assertTrue(byOlder.lexemes.first { it.key == "ruwiktionary/1.verb.2" }.exists)
    }

    @Test
    fun `AWP-39_2 case of the lemma does not matter`() {
        val form = Fx.form(existing = listOf(Fx.existing(5L, "Run", listOf("управлять"))))

        val run = Fx.cards(Fx.ran, form = form).single()

        assertEquals(1, run.existingCount)
        assertTrue(run.lexemes.first { it.key == "ruwiktionary/1.verb.2" }.exists)
    }

    @Test
    fun `AWP-39_1 the text of the lemma is compared in the unicode normal form`() {
        val composed = "Май"
        val decomposed = "Май"
        val form = Fx.form(existing = listOf(Fx.existing(5L, decomposed, listOf("x"))))

        assertEquals(1, form.existingOf(composed).size)
    }

    @Test
    fun `AWP-50_3 stress mark of the dictionary value does not matter`() {
        val stressed = "бежа́ть, бегать"
        val form = Fx.form(existing = listOf(Fx.existing(5L, "run", listOf(stressed))))

        val run = Fx.cards(Fx.ran, form = form).single()

        assertTrue(run.lexemes.first { it.key == "ruwiktionary/1.verb.1" }.exists)
    }

    @Test
    fun `AWP-50_5 definition only dictionary marks by the definition`() {
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
    fun `AWP-50_4 yo and ye are different values`() {
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
    fun `AWP-50_13 the value of the core is compared whole`() {
        val form = Fx.form(existing = listOf(Fx.existing(5L, "run", listOf("бежать"))))

        val run = Fx.cards(Fx.ran, form = form).single()

        assertFalse(run.lexemes.first { it.key == "ruwiktionary/1.verb.1" }.exists)
    }

    @Test
    fun `AWP-50_1 a translation shortened by hand becomes already in the dictionary after the edit`() {
        val take = Fx.lexeme(
            key = "ruwiktionary/1.verb.1",
            translations = listOf("брать; хватать; взять, схватить (также перен.)"),
            matches = listOf(Fx.byField(Fx.TRANSLATION, "translations")),
        )
        val word = Fx.word("take", Fx.lemma("take", listOf(take)))
        val form = Fx.form(existing = listOf(Fx.existing(5L, "take", listOf("брать"))))
        val cards = Fx.cards(word, form = form)
        val translation = Fx.ref("take", "ruwiktionary/1.verb.1", Fx.TRANSLATION)
        val keys = form.lexemeKeysOf(cards.single().target)

        val edited = cards.withTextEdit(translation, "брать")!!.withExistsMarks(keys, form)

        assertFalse(cards.single().lexemes.single().exists)
        assertTrue(edited.single().lexemes.single().exists)
    }

    @Test
    fun `AWP-50_1 no lemma with the text gives no marks`() {
        val form = Fx.form(existing = listOf(Fx.runExisting, Fx.bank1))

        val cards = Fx.cards(Fx.fuera, form = form)

        assertTrue(cards.all { it.existingCount == 0 })
        assertTrue(cards.flatMap { it.lexemes }.none { it.exists })
    }

    @Test
    fun `AWP-50_1 a hand-made text not in the dictionary removes the mark`() {
        val form = Fx.form(existing = listOf(Fx.existing(5L, "run", listOf("бежать, бегать"))))
        val cards = Fx.cards(Fx.ran, form = form)
        val translation = Fx.ref("run", "ruwiktionary/1.verb.1", Fx.TRANSLATION)
        val keys = form.lexemeKeysOf(cards.single().target)

        val edited = cards.withTextEdit(translation, "бегать быстро")!!.withExistsMarks(keys, form)

        assertTrue(cards.candidate("run", "ruwiktionary/1.verb.1").exists)
        assertFalse(edited.candidate("run", "ruwiktionary/1.verb.1").exists)
    }

    @Test
    fun `AWP-50_6 values of parameters are not compared`() {
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

    // AWP-13 — сводка кнопки диалога

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
    fun `AWP-13_6 AWP-50_12 only existing lexemes left give nothing to add`() {
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
