package me.apomazkin.candidates

import me.apomazkin.lexeme.BatchComponent
import me.apomazkin.lexeme.BatchLexeme
import me.apomazkin.lexeme.CaptionedTextValues
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.lexeme.TextValues
import me.apomazkin.lexeme.WordsBatch
import me.apomazkin.lexeme.lexemeTextKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Пакет записи из окна и из диалога карточки (AWP-03, AWP-08, AWP-20, AWP-32,
 * WCL-10, WCL-15): значения компонентов по сопоставлению и правкам, группа и
 * «куда класть».
 */
class WordsBatchesTest {

    private val form = Fx.form()

    private fun text(value: String): TemplateValues = TextValues(Primitive.Text(value))

    private fun choice(optionId: Long): TemplateValues = ChoiceValues(optionId)

    /** Значение из ответа в «Примере»: текст с подписью источника. */
    private fun sourced(value: String): TemplateValues =
        CaptionedTextValues(text = Primitive.Text(value), caption = Primitive.Text("Wiktionary"))

    private fun component(typeId: Long, data: TemplateValues) =
        BatchComponent(typeId = typeId, data = data)

    private fun batchOf(
        cards: List<LemmaCardUiItem>,
        form: DictionaryFormUi = this.form,
        commonGroupId: Long? = null,
    ) = wordsBatchOf(cards, form, commonGroupId)

    private fun BatchLexeme.typeIds(): List<Long> = components.map { it.typeId }

    /** Первая лексема первого слова пакета. */
    private fun WordsBatch.firstLexeme(): BatchLexeme = words.first().lexemes.first()

    /** Данные первого компонента каждой лексемы слова пакета — «Перевод» по форме. */
    private fun WordsBatch.firstData(): List<TemplateValues> =
        words.single().lexemes.map { it.components.first().data }

    // AWP-32 — компоненты лексемы по сопоставлению

    @Test
    fun `AWP-32_1 perro noun gets translation, part of speech, gender, example and gloss`() {
        val lexeme = batchOf(Fx.cards(Fx.perro)).firstLexeme()

        assertEquals(
            listOf(
                component(Fx.TRANSLATION, text("собака; пёс (также ругательство)")),
                component(Fx.PART_OF_SPEECH, choice(Fx.NOUN)),
                component(Fx.GENDER, choice(Fx.MASCULINE)),
                component(Fx.EXAMPLE, sourced("perro callejero")),
                component(Fx.GLOSS, text("dog (the species Canis familiaris …)")),
            ),
            lexeme.components,
        )
    }

    @Test
    fun `AWP-32_2 corrian verb gets translation, verb and gloss only`() {
        val lexeme = batchOf(Fx.cards(Fx.corrian)).firstLexeme()

        assertEquals(
            listOf(
                component(Fx.TRANSLATION, text("течь, литься, струиться (о жидкости)")),
                component(Fx.PART_OF_SPEECH, choice(Fx.VERB)),
                component(Fx.GLOSS, text("to flow")),
            ),
            lexeme.components,
        )
    }

    @Test
    fun `AWP-32_3 zarpo puts the english gloss into translation and gloss`() {
        val lexeme = batchOf(Fx.cards(Fx.zarpo)).firstLexeme()

        assertEquals(
            listOf(
                component(Fx.TRANSLATION, text("to embark; to set sail")),
                component(Fx.PART_OF_SPEECH, choice(Fx.VERB)),
                component(Fx.GLOSS, text("to embark; to set sail")),
            ),
            lexeme.components,
        )
    }

    @Test
    fun `AWP-32_5 an option changed by the user is the one written`() {
        val partOfSpeech = Fx.componentRef("perro", "ruwiktionary/1.noun.1", Fx.PART_OF_SPEECH)
        val cards = Fx.cards(Fx.perro)
            .withOption(partOfSpeech, Fx.ADJECTIVE, form)!!
            .withActiveValues(form)

        val lexeme = batchOf(cards).firstLexeme()

        assertEquals(
            component(Fx.PART_OF_SPEECH, choice(Fx.ADJECTIVE)),
            lexeme.components.first { it.typeId == Fx.PART_OF_SPEECH },
        )
    }

    @Test
    fun `AWP-32_6 all examples deleted give no example component`() {
        val example = Fx.ref("tiempo", "ruwiktionary/1.noun.3", Fx.EXAMPLE)
        var cards = Fx.cards(Fx.tiempo)
        repeat(Fx.tiempoExamples.size) { cards = cards.deletingValue(example, form)!! }

        val lexeme = batchOf(cards).firstLexeme()

        assertEquals(listOf(Fx.TRANSLATION, Fx.PART_OF_SPEECH), lexeme.typeIds())
    }

    @Test
    fun `AWP-32_7 the core text edited by hand writes the typed text`() {
        val translation = Fx.ref("perro", "ruwiktionary/1.noun.2", Fx.TRANSLATION)
        val cards = Fx.cards(Fx.perro)
            .withTextEdit(translation, "обман")!!

        val lexeme = batchOf(cards).words.single().lexemes[2]

        assertEquals(component(Fx.TRANSLATION, text("обман")), lexeme.components.first())
    }

    @Test
    fun `AWP-32_8 a multiple choice with two mapping lines gives two values`() {
        val withStyle = Fx.formWithStyle()
        val cards = Fx.cards(Fx.llegar, form = withStyle)

        val lexeme = batchOf(cards, withStyle).firstLexeme()

        assertEquals(
            listOf(
                component(Fx.TRANSLATION, text("прибывать")),
                component(Fx.STYLE, choice(Fx.FIGURATIVE)),
                component(Fx.STYLE, choice(Fx.COLLOQUIAL)),
            ),
            lexeme.components,
        )
    }

    @Test
    fun `AWP-32_10 translations of run are one value without multiple, four with multiple`() {
        val single = Fx.form(components = listOf(Fx.translation))
        val multiple = Fx.form(components = listOf(Fx.translation.copy(isMultiple = true)))

        val one = batchOf(Fx.cards(Fx.runEnglish, form = single), single).firstLexeme()
        val four = batchOf(Fx.cards(Fx.runEnglish, form = multiple), multiple).firstLexeme()

        assertEquals(
            listOf(component(Fx.TRANSLATION, text("бегать, побегать, бежать, побежать"))),
            one.components,
        )
        val translations = listOf("бегать", "побегать", "бежать", "побежать")
        assertEquals(
            translations.map { component(Fx.TRANSLATION, text(it)) },
            four.components,
        )
    }

    @Test
    fun `AWP-32_11 perro as a verb does not write the gender`() {
        val partOfSpeech = Fx.componentRef("perro", "ruwiktionary/1.noun.1", Fx.PART_OF_SPEECH)
        val cards = Fx.cards(Fx.perro)
            .withOption(partOfSpeech, Fx.VERB, form)!!
            .withActiveValues(form)

        val lexeme = batchOf(cards).firstLexeme()

        assertEquals(
            listOf(Fx.TRANSLATION, Fx.PART_OF_SPEECH, Fx.EXAMPLE, Fx.GLOSS),
            lexeme.typeIds(),
        )
        assertEquals(
            choice(Fx.VERB),
            lexeme.components.first { it.typeId == Fx.PART_OF_SPEECH }.data,
        )
    }

    @Test
    fun `AWP-32_12 pronunciations of the lemma are written to the transcription`() {
        val bank = Fx.lexeme(
            key = "ruwiktionary/1.noun.1",
            translations = listOf("банк"),
            matches = listOf(
                Fx.byField(Fx.TRANSLATION, "translations"),
                Fx.byField(Fx.TRANSCRIPTION, "pronunciations"),
            ),
        )
        val lemma = Fx.lemma("bank", listOf(bank), pronunciations = listOf("/ˈbæŋk/"))
        val withTranscription = Fx.form(components = listOf(Fx.translation, Fx.transcription))
        val cards = Fx.cards(Fx.word("bank", lemma), form = withTranscription)

        val lexeme = batchOf(cards, withTranscription).firstLexeme()

        assertEquals(component(Fx.TRANSCRIPTION, text("/ˈbæŋk/")), lexeme.components.last())
    }

    @Test
    fun `AWP-29_12 a component switched off after the request is not written`() {
        val withoutGloss = Fx.form(components = Fx.tree.filterNot { it.id == Fx.GLOSS })
        val cards = Fx.cards(Fx.perro, form = withoutGloss)

        val lexeme = batchOf(cards, withoutGloss).firstLexeme()

        assertEquals(
            listOf(Fx.TRANSLATION, Fx.PART_OF_SPEECH, Fx.GENDER, Fx.EXAMPLE),
            lexeme.typeIds(),
        )
    }

    @Test
    fun `AWP-17_3 part of speech switched off is not written`() {
        val withoutPartOfSpeech = Fx.form(components = listOf(Fx.translation, Fx.example, Fx.gloss))
        val cards = Fx.cards(Fx.perro, form = withoutPartOfSpeech)

        val lexeme = batchOf(cards, withoutPartOfSpeech).firstLexeme()

        assertEquals(listOf(Fx.TRANSLATION, Fx.EXAMPLE, Fx.GLOSS), lexeme.typeIds())
    }

    // AWP-16 — правка текста доходит до записи

    @Test
    fun `AWP-16_1 the edited translation is written, not the one from the answer`() {
        val take = Fx.lexeme(
            key = "ruwiktionary/1.verb.1",
            translations = listOf("брать; хватать; взять, схватить (также перен.)"),
            matches = listOf(Fx.byField(Fx.TRANSLATION, "translations")),
        )
        val translation = Fx.ref("take", "ruwiktionary/1.verb.1", Fx.TRANSLATION)
        val cards = Fx.cards(Fx.word("take", Fx.lemma("take", listOf(take))))
            .withTextEdit(translation, "брать")!!

        val lexeme = batchOf(cards).firstLexeme()

        assertEquals(listOf(component(Fx.TRANSLATION, text("брать"))), lexeme.components)
        assertTrue(cards.candidate("take", "ruwiktionary/1.verb.1").isEdited)
    }

    @Test
    fun `AWP-16_2 the edited gloss is written`() {
        val gloss = Fx.ref("correr", "ruwiktionary/1.verb.3", Fx.GLOSS)
        val cards = Fx.cards(Fx.corrian).withTextEdit(gloss, "move fast on feet")!!

        val lexeme = batchOf(cards).firstLexeme()

        assertEquals(component(Fx.GLOSS, text("move fast on feet")), lexeme.components.last())
    }

    @Test
    fun `AWP-16_3 the edges of the edited text are trimmed`() {
        val translation = Fx.ref("correr", "ruwiktionary/1.verb.3", Fx.TRANSLATION)
        val cards = Fx.cards(Fx.corrian).withTextEdit(translation, "  течь  ")!!

        val lexeme = batchOf(cards).firstLexeme()

        assertEquals(component(Fx.TRANSLATION, text("течь")), lexeme.components.first())
    }

    // AWP-03 — в словарь ложится ровно то, что осталось в окне

    @Test
    fun `AWP-03_1 two of three lexemes deleted write one lexeme`() {
        val run = Fx.word("run", Fx.lemma("run", listOf(Fx.runVerb1, Fx.runVerb2, Fx.runMachine)))
        val cards = Fx.cards(run)
            .deletingLexeme("run", "ruwiktionary/1.verb.1")!!
            .deletingLexeme("run", "enwiktionary/1.verb.37")!!

        val batch = batchOf(cards)

        assertEquals(listOf(text("управлять")), batch.firstData())
    }

    @Test
    fun `AWP-03_2 two of three examples deleted write one example`() {
        val example = Fx.ref("tiempo", "ruwiktionary/1.noun.3", Fx.EXAMPLE)
        val cards = Fx.cards(Fx.tiempo)
            .deletingValue(example, form)!!
            .deletingValue(example, form)!!

        val lexeme = batchOf(cards).firstLexeme()

        assertEquals(
            listOf(sourced("Hace buen tiempo.")),
            lexeme.components.filter { it.typeId == Fx.EXAMPLE }.map { it.data },
        )
    }

    @Test
    fun `AWP-03_3 nothing deleted writes every card and lexeme except the existing ones`() {
        val form = Fx.form(existing = listOf(Fx.runExisting))
        val cards = Fx.cards(Fx.ran, Fx.bank, form = form)

        val batch = batchOf(cards, form)

        assertEquals(listOf("run", "bank"), batch.words.map { it.text })
        assertEquals(listOf(1, 1), batch.words.map { it.lexemes.size })
    }

    // AWP-20 и AWP-08 — запись, «куда класть» и группы

    @Test
    fun `AWP-20_1 new and existing lemma go to the common group, the existing one by id`() {
        val form = Fx.form(existing = listOf(Fx.bank1))
        val cards = Fx.cards(Fx.ran, Fx.bank, form = form)

        val batch = batchOf(cards, form, commonGroupId = Fx.POTTER)

        val run = batch.words.first { it.text == "run" }
        val bank = batch.words.first { it.text == "bank" }
        assertEquals(Fx.DICT, batch.dictionaryId)
        assertNull(run.wordId)
        assertEquals(Fx.BANK_1, bank.wordId)
        assertEquals(Fx.POTTER, run.groupId)
        assertEquals(Fx.POTTER, bank.groupId)
        assertEquals(3, batch.words.sumOf { it.lexemes.size })
    }

    @Test
    fun `AWP-20_2 AWP-08_2 common group all and no own groups give lemmas without a group`() {
        val batch = batchOf(Fx.cards(Fx.ran, Fx.bank), commonGroupId = null)

        assertTrue(batch.words.all { it.groupId == null })
    }

    @Test
    fun `AWP-20_3 verb and noun of run go to one lemma with different parts of speech`() {
        val noun = Fx.lexeme(
            key = "ruwiktionary/2.noun.1",
            translations = listOf("бег"),
            matches = listOf(
                Fx.byField(Fx.TRANSLATION, "translations"),
                Fx.byOption(Fx.PART_OF_SPEECH, Fx.NOUN),
            ),
        )
        val run = Fx.word("run", Fx.lemma("run", listOf(Fx.runVerb1, noun)))

        val word = batchOf(Fx.cards(run)).words.single()

        assertEquals(2, word.lexemes.size)
        assertEquals(
            listOf(choice(Fx.VERB), choice(Fx.NOUN)),
            word.lexemes.map { lexeme ->
                lexeme.components.first { it.typeId == Fx.PART_OF_SPEECH }.data
            },
        )
    }

    @Test
    fun `AWP-20_4 an existing lexeme is not written, a card of existing ones gives no word`() {
        val partly = Fx.form(existing = listOf(Fx.runExisting))
        val both = Fx.existing(5L, "run", listOf("бежать, бегать", "управлять"))
        val all = Fx.form(existing = listOf(both))

        val onlyNew = batchOf(Fx.cards(Fx.ran, form = partly), partly)
        val nothing = batchOf(Fx.cards(Fx.ran, form = all), all)

        assertEquals(listOf(text("бежать, бегать")), onlyNew.firstData())
        assertTrue(nothing.words.isEmpty())
    }

    @Test
    fun `AWP-20_7 AWP-08_1 own group of a card works instead of the common one`() {
        val form = Fx.form(existing = listOf(Fx.bank1))
        val cards = Fx.cards(Fx.ran, Fx.bank, form = form)
            .withCardGroup("bank", CardOwnGroupUi.Group(Fx.FINANCE))!!

        val batch = batchOf(cards, form, commonGroupId = Fx.POTTER)

        assertEquals(Fx.POTTER, batch.words.first { it.text == "run" }.groupId)
        assertEquals(Fx.FINANCE, batch.words.first { it.text == "bank" }.groupId)
    }

    @Test
    fun `AWP-08_4 own group all with a common group gives a lemma without a group`() {
        val cards = Fx.cards(Fx.ran).withCardGroup("run", CardOwnGroupUi.All)!!

        val batch = batchOf(cards, commonGroupId = Fx.POTTER)

        assertNull(batch.words.single().groupId)
    }

    @Test
    fun `AWP-08_5 taking the own group off brings the common group back`() {
        val own = Fx.cards(Fx.ran).withCardGroup("run", CardOwnGroupUi.Group(Fx.FINANCE))!!

        val back = own.withCardGroup("run", CardOwnGroupUi.AsCommon)!!

        assertEquals(Fx.FINANCE, batchOf(own, commonGroupId = Fx.POTTER).words.single().groupId)
        assertEquals(Fx.POTTER, batchOf(back, commonGroupId = Fx.POTTER).words.single().groupId)
    }

    @Test
    fun `AWP-20_8 three hundred new lemmas of three lexemes are one batch of 2700 values`() {
        val words = (1..300).map { index ->
            val lexemes = (1..3).map { number ->
                Fx.lexeme(
                    key = "ruwiktionary/1.verb.$number",
                    translations = listOf("значение $index.$number"),
                    gloss = "meaning $index.$number",
                    partOfSpeechKey = "verb",
                    matches = listOf(
                        Fx.byField(Fx.TRANSLATION, "translations"),
                        Fx.byOption(Fx.PART_OF_SPEECH, Fx.VERB),
                        Fx.byField(Fx.GLOSS, "gloss"),
                    ),
                )
            }
            Fx.word("word$index", Fx.lemma("lemma$index", lexemes))
        }
        val cards = Fx.cards(*words.toTypedArray())

        val batch = batchOf(cards, commonGroupId = Fx.POTTER)

        assertEquals(300, batch.words.size)
        assertEquals(900, batch.words.sumOf { it.lexemes.size })
        assertEquals(2700, batch.words.sumOf { word -> word.lexemes.sumOf { it.components.size } })
        assertTrue(batch.words.all { it.wordId == null && it.groupId == Fx.POTTER })
    }

    // WCL-02 и WCL-10 — запись в слово диалога

    @Test
    fun `WCL-02_1 two of three lexemes deleted write one lexeme to the dialog word`() {
        val run = Fx.word("run", Fx.lemma("run", listOf(Fx.runVerb1, Fx.runVerb2, Fx.runMachine)))
        val cards = buildCandidateCards(run, "run", form)
            .deletingLexeme("run", "ruwiktionary/1.verb.1")!!
            .deletingLexeme("run", "enwiktionary/1.verb.37")!!

        val batch = wordsBatchForWord(Fx.RUN_WORD, "run", cards, form)

        assertEquals(listOf(text("управлять")), batch.firstData())
    }

    @Test
    fun `WCL-02_2 nothing deleted writes every visible lexeme except the existing ones`() {
        val own = setOf(lexemeTextKey("управлять"))
        val cards = buildCandidateCards(Fx.ran, "run", form).withExistsMarks(own, form)

        val batch = wordsBatchForWord(Fx.RUN_WORD, "run", cards, form)

        assertEquals(listOf(text("бежать, бегать")), batch.firstData())
    }

    @Test
    fun `WCL-10_1 three left lexemes go to the dialog word without a group`() {
        val cards = buildCandidateCards(Fx.corrian, "corrían", form)

        val batch = wordsBatchForWord(Fx.CORRIAN_WORD, "corrían", cards, form)

        val word = batch.words.single()
        assertEquals(Fx.CORRIAN_WORD, word.wordId)
        assertEquals("corrían", word.text)
        assertNull(word.groupId)
        assertNull(word.renameTo)
        assertNull(word.absorbWordId)
        assertEquals(3, word.lexemes.size)
    }

    @Test
    fun `WCL-10_2 an existing lexeme is not written`() {
        val own = setOf(lexemeTextKey("течь, литься, струиться (о жидкости)"))
        val cards = buildCandidateCards(Fx.corrian, "corrían", form).withExistsMarks(own, form)

        val batch = wordsBatchForWord(Fx.CORRIAN_WORD, "corrían", cards, form)

        assertEquals(2, batch.words.single().lexemes.size)
    }

    @Test
    fun `WCL-10_4 the dialog never touches the groups`() {
        val cards = buildCandidateCards(Fx.corrian, "corrían", form)
            .withCardGroup("correr", CardOwnGroupUi.Group(Fx.FINANCE))!!

        val batch = wordsBatchForWord(Fx.CORRIAN_WORD, "corrían", cards, form)

        assertNull(batch.words.single().groupId)
    }

    @Test
    fun `WCL-10_1 nothing to write gives a batch without words`() {
        val batch = wordsBatchForWord(Fx.CORRIAN_WORD, "corrían", emptyList(), form)

        assertTrue(batch.words.isEmpty())
        assertEquals(Fx.DICT, batch.dictionaryId)
    }

    // WCL-15 — переименование или перенос в лемму

    private val correrForm = Fx.form(existing = listOf(Fx.correrExisting))

    /** Перенос слова диалога «corrían» в слово «correr» словаря. */
    private fun transferTo(cards: List<LemmaCardUiItem>) = wordsBatchForTransfer(
        targetWordId = Fx.CORRER_WORD,
        targetText = "correr",
        absorbWordId = Fx.CORRIAN_WORD,
        cards = cards,
        form = correrForm,
    )

    @Test
    fun `WCL-15_2 rename gives the dialog word the text of the lemma with two lexemes`() {
        val cards = buildCandidateCards(Fx.corrian, "corrían", form)
            .deletingLexeme("correr", "ruwiktionary/1.verb.5")!!

        val batch = wordsBatchForWord(Fx.CORRIAN_WORD, "corrían", cards, form, renameTo = "correr")

        val word = batch.words.single()
        assertEquals(Fx.CORRIAN_WORD, word.wordId)
        assertEquals("correr", word.renameTo)
        assertEquals(2, word.lexemes.size)
        assertNull(word.groupId)
    }

    @Test
    fun `WCL-15_3 transfer writes new lexemes into the lemma word and absorbs the dialog word`() {
        val cards = buildCandidateCards(Fx.corrian, "corrían", correrForm)

        val batch = transferTo(cards)

        val word = batch.words.single()
        assertEquals(Fx.CORRER_WORD, word.wordId)
        assertEquals("correr", word.text)
        assertEquals(Fx.CORRIAN_WORD, word.absorbWordId)
        assertNull(word.groupId)
        // «бегать; бежать» уже есть у «correr»: из трёх кандидатов пишутся два.
        assertEquals(
            listOf(
                text("течь, литься, струиться (о жидкости)"),
                text("бежать, проходить, лететь (о времени)"),
            ),
            batch.firstData(),
        )
    }

    @Test
    fun `WCL-15_3 transfer skips what the dialog word already has`() {
        val own = setOf(lexemeTextKey("течь, литься, струиться (о жидкости)"))
        val cards = buildCandidateCards(Fx.corrian, "corrían", correrForm)
            .withExistsMarks(own, correrForm)

        val batch = transferTo(cards)

        assertEquals(listOf(text("бежать, проходить, лететь (о времени)")), batch.firstData())
    }

    @Test
    fun `WCL-15_4 rename writes the lexeme that the lemma word has, transfer does not`() {
        val cards = buildCandidateCards(Fx.corrian, "corrían", correrForm)
            .withLemmaExistsMarks(correrForm)

        val rename = wordsBatchForWord(
            wordId = Fx.CORRIAN_WORD,
            text = "corrían",
            cards = cards,
            form = correrForm,
            renameTo = "correr",
        )
        val transfer = transferTo(cards)

        assertEquals(3, rename.words.single().lexemes.size)
        assertEquals(2, transfer.words.single().lexemes.size)
    }

    @Test
    fun `WCL-15_3 transfer batch has the word even when every lexeme is skipped`() {
        val cards = buildCandidateCards(Fx.corrian, "corrían", correrForm)
            .deletingLexeme("correr", "ruwiktionary/1.verb.3")!!
            .deletingLexeme("correr", "ruwiktionary/1.verb.5")!!

        val batch = transferTo(cards)

        // `ruwiktionary/1.verb.1` есть у «correr»: писать нечего, но перенос нужен сам по себе.
        assertEquals(1, batch.words.size)
        assertTrue(batch.words.single().lexemes.isEmpty())
        assertEquals(Fx.CORRIAN_WORD, batch.words.single().absorbWordId)
    }
}
