package me.apomazkin.scancandidates.mate

import io.github.kilgoret.mate.effects
import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.assertNoEffects
import io.github.kilgoret.mate.test.testReduce
import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.candidates.CardOwnGroupUi
import me.apomazkin.candidates.CardTargetUi
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.lexeme.BatchLexeme
import me.apomazkin.lexeme.CaptionedTextValues
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.lexeme.TextValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Запись слова глазами редьюсера: «Добавить» и пакет (AWP-02, AWP-03, AWP-20,
 * AWP-47.4), записано — слово ушло (AWP-62), отказ и сбой (AWP-21), повтор
 * (AWP-26). Адреса строк спеки — `AWP-NN_k`.
 */
class AddWordReducerTest {
    private val reducer = reducer()

    private val runKey = "ruwiktionary/1.verb.1"

    private fun text(value: String): TemplateValues = TextValues(Primitive.Text(value))

    private fun choice(optionId: Long): TemplateValues = ChoiceValues(optionId)

    /** Значение из ответа в «Примере»: текст с подписью источника. */
    private fun sourced(value: String): TemplateValues =
        CaptionedTextValues(text = Primitive.Text(value), caption = Primitive.Text("Wiktionary"))

    private fun BatchLexeme.written(): List<Pair<Long, TemplateValues>> =
        components.map { it.typeId to it.data }

    /** Что пишется из `ruwiktionary/1.verb.1` слова «ran»: перевод и глагол. */
    private fun runVerbWritten(): List<Pair<Long, TemplateValues>> = listOf(
        Fx.TRANSLATION to text("бежать, бегать"),
        Fx.PART_OF_SPEECH to choice(Fx.VERB),
    )

    /** Карточка «ran» загружена, «Добавить» нажата: идёт запись. */
    private fun adding(form: DictionaryFormUi = Fx.form()): ScanCandidatesState =
        reducer.step(reducer.withCard("ran", Fx.ran, form = form), Msg.AddWord("ran"))

    private val friendship = Fx.word(
        "friendship",
        Fx.lemma(
            "friendship",
            listOf(
                Fx.lexeme(
                    key = "ruwiktionary/1.noun.1",
                    translations = listOf("дружба"),
                    partOfSpeechKey = "noun",
                    matches = listOf(Fx.byField(Fx.TRANSLATION, "translations"), Fx.byOption(Fx.PART_OF_SPEECH, Fx.NOUN)),
                    inContext = true,
                ),
            ),
        ),
    )

    // ===== Кнопка и пакет (AWP-02, AWP-47.4, AWP-20, AWP-03) =====

    @Test
    fun `AWP-02_1 AWP-47_4 add sends one batch of the word and switches its buttons off`() {
        val loaded = reducer.withCard("ran", Fx.ran)

        val result = reducer.testReduce(loaded, Msg.AddWord("ran"))

        val card = result.state().card("ran")
        assertTrue(card.isAdding)
        assertTrue(result.state().isAdding)
        assertNull(card.addFailure)
        val effect = result.effects().single() as DatasourceEffect.AddWord
        assertEquals("ran", effect.word)
        assertEquals(Fx.DICT, effect.batch.dictionaryId)
        assertEquals(listOf("run"), effect.batch.words.map { it.text })
        assertEquals(listOf<Long?>(null), effect.batch.words.map { it.wordId })
        assertEquals(listOf(2), effect.batch.words.map { it.lexemes.size })
    }

    @Test
    fun `AWP-20_9 AWP-20_2 a new word with two lexemes goes with the common group or without a group`() {
        val loaded = reducer.withCard("ran", Fx.ran)
        val grouped = reducer.step(loaded, Msg.SetCommonGroup(Fx.POTTER))

        val inGroup = reducer.batchOf(grouped, "ran").words.single()
        val withoutGroup = reducer.batchOf(loaded, "ran").words.single()

        assertEquals("run", inGroup.text)
        assertNull(inGroup.wordId)
        assertEquals(Fx.POTTER, inGroup.groupId)
        assertEquals(2, inGroup.lexemes.size)
        assertNull(withoutGroup.groupId)
    }

    @Test
    fun `AWP-20_10 AWP-20_5 a lexeme into the chosen lemma of the dictionary keeps the word and writes only the new one`() {
        val form = Fx.form(existing = listOf(Fx.bank1))
        val loaded = reducer.withCard("bank", Fx.bank, form = form)
        val grouped = reducer.step(loaded, Msg.SetCommonGroup(Fx.POTTER))

        val word = reducer.batchOf(grouped, "bank").words.single()

        assertEquals(Fx.BANK_1, word.wordId)
        assertEquals("bank", word.text)
        assertEquals(Fx.POTTER, word.groupId)
        assertEquals(1, word.lexemes.size)
        assertNull(word.renameTo)
    }

    @Test
    fun `AWP-20_3 verb and noun lexemes of one lemma are two lexemes of one word`() {
        val loaded = reducer.withCard("run", Fx.runVerbAndNoun)

        val word = reducer.batchOf(loaded, "run").words.single()

        assertEquals("run", word.text)
        assertEquals(
            listOf(
                runVerbWritten(),
                listOf(Fx.TRANSLATION to text("бег"), Fx.PART_OF_SPEECH to choice(Fx.NOUN)),
            ),
            word.lexemes.map { it.written() },
        )
    }

    @Test
    fun `AWP-20_4 AWP-03_5 an existing lexeme is not written`() {
        val loaded = reducer.withCard("ran", Fx.ran, form = Fx.form(existing = listOf(Fx.runExisting)))

        val word = reducer.batchOf(loaded, "ran").words.single()

        assertEquals(Fx.RUN_WORD, word.wordId)
        assertEquals(listOf(runVerbWritten()), word.lexemes.map { it.written() })
    }

    @Test
    fun `AWP-20_7 AWP-08_1 an own group of the word acts instead of the common one`() {
        val common = reducer.step(reducer.withCard("ran", Fx.ran), Msg.SetCommonGroup(Fx.POTTER))
        val own = reducer.step(common, Msg.SetCardGroup("run", CardOwnGroupUi.Group(Fx.FINANCE)))

        assertEquals(Fx.FINANCE, reducer.batchOf(own, "ran").words.single().groupId)
    }

    @Test
    fun `AWP-03_1 only what is left in the card is written`() {
        val loaded = reducer.withCard("run", Fx.runThree)
        val trimmed = reducer.steps(
            loaded,
            Msg.DeleteLexeme("run", "ruwiktionary/1.verb.2"),
            Msg.DeleteLexeme("run", "enwiktionary/1.verb.37"),
        )

        val word = reducer.batchOf(trimmed, "run").words.single()

        assertEquals(listOf(runVerbWritten()), word.lexemes.map { it.written() })
    }

    @Test
    fun `AWP-03_2 two of three examples deleted leave one example in the batch`() {
        val loaded = reducer.withCard("tiempo", Fx.tiempo)
        val first = Fx.ref("tiempo", "ruwiktionary/1.noun.3", Fx.EXAMPLE, index = 0)
        val trimmed = reducer.steps(loaded, Msg.DeleteValue(first), Msg.DeleteValue(first))

        val examples = reducer.batchOf(trimmed, "tiempo").words.single().lexemes.single().written()
            .filter { it.first == Fx.EXAMPLE }

        assertEquals(listOf(Fx.EXAMPLE to sourced(Fx.tiempoExamples[2])), examples)
    }

    @Test
    fun `AWP-03_3 nothing deleted or edited writes all that is shown`() {
        val loaded = reducer.withCard("run", Fx.runThree)

        val word = reducer.batchOf(loaded, "run").words.single()

        assertEquals(3, word.lexemes.size)
        assertEquals(runVerbWritten(), word.lexemes.first().written())
    }

    // ===== Записано (AWP-62, AWP-47) =====

    @Test
    fun `AWP-62_1 a written word leaves the list with its card, the rest keep the order`() {
        val loaded = reducer.withCard("friendship", friendship, lists = Fx.textbookLists())
        val adding = reducer.step(loaded, Msg.AddWord("friendship"))

        val added = reducer.testReduce(adding, Msg.WordAdded("friendship"))

        val state = added.state()
        assertEquals(Fx.textbookLexical - "friendship", state.lists.lexical.texts())
        assertEquals(Fx.textbookProper, state.lists.proper.texts())
        assertEquals(Fx.textbookFunction, state.lists.function.texts())
        assertNull(state.lists.wordOf("friendship"))
        assertEquals(ScanPhase.WINDOW, state.phase)
        assertFalse(state.isAdding)
        added.assertNoEffects()
    }

    @Test
    fun `AWP-62_2 a second word added after the first leaves the window open with the rest in order`() {
        val loaded = reducer.withCard("friendship", friendship, lists = Fx.textbookLists())
        val first = reducer.steps(loaded, Msg.AddWord("friendship"), Msg.WordAdded("friendship"))
        val sibling = Fx.word("sibling", Fx.lemma("sibling", listOf(friendship.lemmas.single().lexemes.single())))

        val second = reducer.steps(
            first,
            Msg.LoadLexemes("sibling"),
            Fx.lexemes("sibling", sibling),
            Msg.AddWord("sibling"),
            Msg.WordAdded("sibling"),
        )

        assertEquals(Fx.textbookLexical - "friendship" - "sibling", second.lists.lexical.texts())
        assertEquals(22, second.lists.lexical.size)
        assertEquals(ScanPhase.WINDOW, second.phase)
        assertFalse(second.isAdding)
    }

    @Test
    fun `AWP-62_3 AWP-21_1 a failed write keeps the row and the card with the failure and retry`() {
        val adding = adding()

        val result = reducer.testReduce(adding, Msg.AddFailed("ran", AddFailureUi.Failed))

        val card = result.state().card("ran")
        assertEquals(AddFailureUi.Failed, card.addFailure)
        assertFalse(card.isAdding)
        assertFalse(result.state().isAdding)
        assertEquals(adding.lemmaCard("ran"), card.lemma)
        assertEquals(CardPhase.CANDIDATES, card.phase)
        assertEquals(listOf("ran"), result.state().lists.lexical.texts())
        result.assertNoEffects()
    }

    @Test
    fun `the screen stays adding while another word is still being written`() {
        val two = Fx.lists(lexical = listOf("ran", "bank"))
        val both = reducer.steps(
            reducer.withCard("ran", Fx.ran, lists = two),
            Msg.LoadLexemes("bank"),
            Fx.lexemes("bank", Fx.bank),
            Msg.AddWord("ran"),
            Msg.AddWord("bank"),
        )

        val oneLeft = reducer.step(both, Msg.WordAdded("ran"))
        val done = reducer.step(oneLeft, Msg.WordAdded("bank"))

        assertTrue(both.isAdding)
        assertTrue(oneLeft.isAdding)
        assertTrue(oneLeft.card("bank").isAdding)
        assertFalse(done.isAdding)
        assertTrue(done.lists.lexical.isEmpty())
    }

    // ===== Запись не удалась (AWP-21) =====

    @Test
    fun `AWP-21_3 a dead common group goes back to all, own groups of the cards stay`() {
        val two = Fx.lists(lexical = listOf("ran", "bank"))
        val form = Fx.form(existing = listOf(Fx.bank1))
        val both = reducer.steps(
            reducer.withCard("ran", Fx.ran, form = form, lists = two),
            Msg.LoadLexemes("bank"),
            Fx.lexemes("bank", Fx.bank),
            Msg.SetCommonGroup(Fx.POTTER),
            Msg.SetCardGroup("bank", CardOwnGroupUi.Group(Fx.FINANCE)),
            Msg.AddWord("ran"),
        )

        val result = reducer.step(both, Msg.AddGroupGone("ran", AddFailureUi.GroupMissing(Fx.POTTER)))

        assertNull(result.commonGroupId)
        assertEquals(CardOwnGroupUi.Group(Fx.FINANCE), result.lemmaCard("bank").ownGroup)
        assertEquals(listOf(Fx.FINANCE), requireNotNull(result.form).groups.map { it.id })
        assertEquals(AddFailureUi.GroupMissing(Fx.POTTER), result.card("ran").addFailure)
        assertFalse(result.card("ran").isAdding)
        assertFalse(result.isAdding)
    }

    @Test
    fun `AWP-21_4 a dead lemma of the dictionary turns the target into a new word and its saved rows go`() {
        val form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))
        val adding = reducer.step(reducer.withCard("bank", Fx.bank, form = form), Msg.AddWord("bank"))

        val result = reducer.step(adding, Msg.AddWordGone("bank", AddFailureUi.WordMissing(Fx.BANK_1)))

        val card = result.card("bank")
        assertEquals(CardTargetUi.Existing(Fx.BANK_1), adding.lemmaCard("bank").target)
        assertEquals(CardTargetUi.NewWord, card.lemma.target)
        assertTrue(card.saved.isEmpty())
        assertEquals(1, card.lemma.existingCount)
        assertEquals(listOf(Fx.BANK_2), requireNotNull(result.form).existingLemmas.map { it.wordId })
        assertEquals(AddFailureUi.WordMissing(Fx.BANK_1), card.addFailure)
        assertFalse(card.isAdding)
    }

    @Test
    fun `AWP-21_5 a dead own group falls back to the common one`() {
        val own = reducer.step(reducer.withCard("bank", Fx.bank), Msg.SetCardGroup("bank", CardOwnGroupUi.Group(Fx.FINANCE)))
        val adding = reducer.step(own, Msg.AddWord("bank"))

        val result = reducer.step(adding, Msg.AddGroupGone("bank", AddFailureUi.GroupMissing(Fx.FINANCE)))

        assertEquals(CardOwnGroupUi.AsCommon, result.lemmaCard("bank").ownGroup)
        assertEquals(listOf(Fx.POTTER), requireNotNull(result.form).groups.map { it.id })
    }

    @Test
    fun `AWP-21_6 AWP-47_3 a switched off component loses its values, a switched off core switches add off`() {
        val adding = reducer.step(reducer.withCard("perro", Fx.perro), Msg.AddWord("perro"))
        val glossGone = Msg.AddComponentGone("perro", AddFailureUi.TypeUnavailable(Fx.GLOSS))
        val coreGone = Msg.AddComponentGone("perro", AddFailureUi.TypeUnavailable(Fx.TRANSLATION))

        val gloss = reducer.step(adding, glossGone)
        val core = reducer.step(adding, coreGone)

        assertEquals(
            listOf(
                "Перевод: собака; пёс (также ругательство)",
                "Часть речи: существительное",
                "Род: м.р.",
                "Пример: perro callejero",
            ),
            gloss.shown("perro", "ruwiktionary/1.noun.1"),
        )
        assertTrue(requireNotNull(gloss.form).components.none { it.id == Fx.GLOSS })
        assertTrue(gloss.card("perro").canAdd)
        assertEquals(AddFailureUi.TypeUnavailable(Fx.GLOSS), gloss.card("perro").addFailure)
        assertFalse(gloss.card("perro").isAdding)
        assertTrue(core.lemmaCard("perro").lexemes.all { it.lacksCore })
        assertFalse(core.card("perro").canAdd)
    }

    @Test
    fun `AWP-21_6 a switched off option owner takes the dependent gender with it`() {
        val adding = reducer.step(reducer.withCard("perro", Fx.perro), Msg.AddWord("perro"))

        val result = reducer.step(adding, Msg.AddComponentGone("perro", AddFailureUi.TypeUnavailable(Fx.PART_OF_SPEECH)))

        assertEquals(
            listOf(
                "Перевод: собака; пёс (также ругательство)",
                "Толкование: dog (the species Canis familiaris …)",
                "Пример: perro callejero",
            ),
            result.shown("perro", "ruwiktionary/1.noun.1"),
        )
    }

    // ===== Повтор записи (AWP-26) =====

    @Test
    fun `AWP-26_1 AWP-21_2 AWP-02_2 retry writes the word again once without the failure`() {
        val failed = reducer.step(adding(), Msg.AddFailed("ran", AddFailureUi.Failed))

        val result = reducer.testReduce(failed, Msg.RetryAdd("ran"))

        val card = result.state().card("ran")
        assertTrue(card.isAdding)
        assertNull(card.addFailure)
        assertTrue(result.state().isAdding)
        assertEquals(failed.lemmaCard("ran"), card.lemma)
        assertEquals("ran", (result.effects().single() as DatasourceEffect.AddWord).word)
    }

    @Test
    fun `AWP-26_1 retry after a group reset writes without the dead group`() {
        val grouped = reducer.step(reducer.withCard("ran", Fx.ran), Msg.SetCommonGroup(Fx.POTTER))
        val adding = reducer.step(grouped, Msg.AddWord("ran"))
        val rejected = reducer.step(adding, Msg.AddGroupGone("ran", AddFailureUi.GroupMissing(Fx.POTTER)))

        val effects = reducer.effectsOf(rejected, Msg.RetryAdd("ran"))

        val batch = (effects.single() as DatasourceEffect.AddWord).batch
        assertTrue(batch.words.all { it.groupId == null })
    }

    @Test
    fun `AWP-26_2 closing from the failed state asks the question while the word is still pending`() {
        val failed = reducer.step(adding(), Msg.AddFailed("ran", AddFailureUi.Failed))

        val result = reducer.testReduce(failed, Msg.RequestClose)

        assertTrue(result.state().isConfirmingClose)
        assertEquals(1, result.state().pendingWords)
        result.assertNoEffects()
    }

    @Test
    fun `add on a card with nothing to write changes nothing`() {
        val form = Fx.form(existing = listOf(Fx.existing(Fx.RUN_WORD, "run", listOf("бежать, бегать", "управлять"))))
        val allExisting = reducer.withCard("ran", Fx.ran, form = form)

        val result = reducer.testReduce(allExisting, Msg.AddWord("ran"))

        assertEquals(allExisting, result.state())
        result.assertNoEffects()
        assertTrue(allExisting.lexeme("ran", runKey).exists)
    }
}
