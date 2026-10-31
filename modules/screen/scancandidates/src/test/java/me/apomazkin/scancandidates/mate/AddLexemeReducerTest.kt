package me.apomazkin.scancandidates.mate

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.effects
import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.assertEffects
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
 * Запись плашки глазами редьюсера: «Создать» и «Перенести» с пакетом одной
 * лексемы (AWP-02, AWP-03, AWP-20, AWP-47.4), записано — плашка ушла, плашки
 * словаря перечитаны, «Куда» остальных переехало (AWP-62, AWP-63, AWP-39.5),
 * отказ и сбой (AWP-21), повтор (AWP-26). Адреса строк спеки — `AWP-NN_k`.
 */
class AddLexemeReducerTest {
    private val reducer = reducer()

    private val runKey = "ruwiktionary/1.verb.1"
    private val manageKey = "ruwiktionary/1.verb.2"
    private val machineKey = "enwiktionary/1.verb.37"
    private val bankKey = "ruwiktionary/1.noun.1"
    private val tiempoKey = "ruwiktionary/1.noun.3"

    private fun text(value: String): TemplateValues = TextValues(Primitive.Text(value))

    private fun choice(optionId: Long): TemplateValues = ChoiceValues(optionId)

    /** Значение из ответа в «Примере»: текст с подписью источника. */
    private fun sourced(value: String): TemplateValues =
        CaptionedTextValues(text = Primitive.Text(value), caption = Primitive.Text("Wiktionary"))

    private fun BatchLexeme.written(): List<Pair<Long, TemplateValues>> =
        components.map { it.typeId to it.data }

    /** Что пишется из `ruwiktionary/1.verb.1` слова «run»: перевод и глагол. */
    private fun runVerbWritten(): List<Pair<Long, TemplateValues>> = listOf(
        Fx.TRANSLATION to text("бежать, бегать"),
        Fx.PART_OF_SPEECH to choice(Fx.VERB),
    )

    /** Плашки «run» загружены, «Создать» у первой нажато: идёт запись. */
    private fun adding(form: DictionaryFormUi = Fx.form()): ScanCandidatesState =
        reducer.step(reducer.withCard("run", Fx.run, form = form), Msg.AddLexeme("run", runKey))

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

    private val reloadRun = setOf<Effect>(DatasourceEffect.ReloadLemmas("run", Fx.DICT))

    // ===== Кнопка и пакет (AWP-02, AWP-47.4, AWP-20, AWP-03) =====

    @Test
    fun `AWP-02_1 AWP-47_4 create sends one batch of the plate and marks the plate and the word as being written`() {
        val loaded = reducer.withCard("run", Fx.run)

        val result = reducer.testReduce(loaded, Msg.AddLexeme("run", runKey))

        val card = result.state().card("run")
        assertTrue(card.isAdding)
        assertTrue(result.state().isAdding)
        assertTrue(result.state().lexeme("run", runKey).isAdding)
        assertFalse(result.state().lexeme("run", manageKey).isAdding)
        assertNull(result.state().lexeme("run", runKey).addFailure)
        val effect = result.effects().single() as DatasourceEffect.CreateWord
        assertEquals("run", effect.word)
        assertEquals(runKey, effect.key)
        assertEquals(Fx.DICT, effect.batch.dictionaryId)
        val word = effect.batch.words.single()
        assertEquals("run", word.text)
        assertNull(word.wordId)
        assertEquals(listOf(runVerbWritten()), word.lexemes.map { it.written() })
    }

    @Test
    fun `AWP-20_9 AWP-20_2 AWP-08_2 a new word goes with the common group or without a group`() {
        val loaded = reducer.withCard("run", Fx.run)
        val grouped = reducer.step(loaded, Msg.SetCommonGroup(Fx.POTTER))

        val inGroup = reducer.batchOf(grouped, "run", runKey).words.single()
        val withoutGroup = reducer.batchOf(loaded, "run", runKey).words.single()

        assertEquals(Fx.POTTER, inGroup.groupId)
        assertEquals(1, inGroup.lexemes.size)
        assertNull(withoutGroup.groupId)
    }

    @Test
    fun `AWP-20_10 AWP-20_5 transfer into the chosen word keeps its groups and sends the effect of a transfer`() {
        val form = Fx.form(existing = listOf(Fx.bank1))
        val loaded = reducer.withCard("bank", Fx.bank, form = form)
        val chosen = reducer.steps(
            loaded,
            Msg.SetCommonGroup(Fx.POTTER),
            Msg.SetCardTarget("bank", bankKey, CardTargetUi.Existing(Fx.BANK_1)),
        )

        val result = reducer.testReduce(chosen, Msg.AddLexeme("bank", bankKey))

        val effect = result.effects().single() as DatasourceEffect.TransferLexeme
        val word = effect.batch.words.single()
        assertEquals(Fx.BANK_1, word.wordId)
        assertEquals("bank", word.text)
        assertNull(word.groupId)
        assertEquals(1, word.lexemes.size)
        assertNull(word.renameTo)
    }

    @Test
    fun `AWP-20_3 verb and noun of run go one by one into the same word, the second by the created word`() {
        val loaded = reducer.withCard("run", Fx.runVerbAndNoun)

        val created = reducer.created(loaded, "run", runKey, Fx.RUN_CREATED)
        val second = reducer.batchOf(created, "run", "ruwiktionary/1.noun.1").words.single()

        assertEquals(Fx.RUN_CREATED, second.wordId)
        assertEquals(
            listOf(Fx.TRANSLATION to text("бег"), Fx.PART_OF_SPEECH to choice(Fx.NOUN)),
            second.lexemes.single().written(),
        )
    }

    @Test
    fun `AWP-20_11 the second plate switched back to a new word writes a second word`() {
        val created = reducer.created(reducer.withCard("run", Fx.run), "run", runKey, Fx.RUN_CREATED)
        val back = reducer.step(created, Msg.SetCardTarget("run", manageKey, CardTargetUi.NewWord))

        val batch = reducer.batchOf(back, "run", manageKey)

        assertEquals(CardTargetUi.Existing(Fx.RUN_CREATED), created.lexeme("run", manageKey).target)
        assertNull(batch.words.single().wordId)
    }

    @Test
    fun `AWP-20_7 AWP-08_1 the own group of the plate acts instead of the common one`() {
        val common = reducer.step(reducer.withCard("run", Fx.run), Msg.SetCommonGroup(Fx.POTTER))
        val own = reducer.step(common, Msg.SetCardGroup("run", runKey, CardOwnGroupUi.Group(Fx.FINANCE)))

        assertEquals(Fx.FINANCE, reducer.batchOf(own, "run", runKey).words.single().groupId)
        assertEquals(Fx.POTTER, reducer.batchOf(own, "run", manageKey).words.single().groupId)
    }

    @Test
    fun `AWP-03_1 only the plate pressed is written, the dismissed ones are gone`() {
        val loaded = reducer.withCard("run", Fx.runThree)
        val trimmed = reducer.steps(
            loaded,
            Msg.DismissLexeme("run", manageKey),
            Msg.DismissLexeme("run", machineKey),
        )

        val word = reducer.batchOf(trimmed, "run", runKey).words.single()

        assertEquals(listOf(runVerbWritten()), word.lexemes.map { it.written() })
        assertEquals(listOf(runKey), trimmed.lemmaCard("run").lexemes.map { it.key })
    }

    @Test
    fun `AWP-03_2 two of three examples deleted leave one example in the batch`() {
        val loaded = reducer.withCard("tiempo", Fx.tiempo)
        val first = Fx.ref("tiempo", tiempoKey, Fx.EXAMPLE, index = 0)
        val trimmed = reducer.steps(loaded, Msg.DeleteValue(first), Msg.DeleteValue(first))

        val examples = reducer.batchOf(trimmed, "tiempo", tiempoKey).words.single().lexemes.single().written()
            .filter { it.first == Fx.EXAMPLE }

        assertEquals(listOf(Fx.EXAMPLE to sourced(Fx.tiempoExamples[2])), examples)
    }

    @Test
    fun `AWP-03_3 nothing deleted or edited writes the plate as it came, the other plates stay`() {
        val loaded = reducer.withCard("run", Fx.runThree)

        val word = reducer.batchOf(loaded, "run", runKey).words.single()

        assertEquals(listOf(runVerbWritten()), word.lexemes.map { it.written() })
        assertEquals(3, loaded.lemmaCard("run").lexemes.size)
    }

    // ===== Записано (AWP-62, AWP-63) =====

    @Test
    fun `AWP-62_1 a word whose only plate is created leaves the list, the rest keep the order, no reload`() {
        val loaded = reducer.withCard("friendship", friendship, lists = Fx.textbookLists())
        val adding = reducer.step(loaded, Msg.AddLexeme("friendship", "ruwiktionary/1.noun.1"))

        val added = reducer.testReduce(adding, Msg.WordCreated("friendship", "ruwiktionary/1.noun.1", 9L))

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
    fun `AWP-62_2 a second word created after the first leaves the window open with the rest in order`() {
        val loaded = reducer.withCard("friendship", friendship, lists = Fx.textbookLists())
        val first = reducer.created(loaded, "friendship", "ruwiktionary/1.noun.1", 9L)
        val sibling = Fx.word("sibling", Fx.lemma("sibling", listOf(friendship.lemmas.single().lexemes.single())))

        val second = reducer.created(
            reducer.steps(first, Msg.LoadLexemes("sibling"), Fx.lexemes("sibling", sibling)),
            "sibling",
            "ruwiktionary/1.noun.1",
            10L,
        )

        assertEquals(Fx.textbookLexical - "friendship" - "sibling", second.lists.lexical.texts())
        assertEquals(22, second.lists.lexical.size)
        assertEquals(ScanPhase.WINDOW, second.phase)
        assertFalse(second.isAdding)
    }

    @Test
    fun `AWP-62_4 AWP-63_1 AWP-18_14 create of the first plate keeps the word, retargets the rest and asks the lemmas again`() {
        val adding = adding()

        val created = reducer.testReduce(adding, Msg.WordCreated("run", runKey, Fx.RUN_CREATED))

        val state = created.state()
        assertEquals(listOf("run"), state.lists.lexical.texts())
        assertEquals(listOf(manageKey), state.lemmaCard("run").lexemes.map { it.key })
        assertEquals(CardTargetUi.Existing(Fx.RUN_CREATED), state.lexeme("run", manageKey).target)
        assertFalse(state.card("run").isAdding)
        assertFalse(state.isAdding)
        created.assertEffects(reloadRun)
    }

    @Test
    fun `AWP-63_1 AWP-63_5 AWP-39_5 the reloaded lemmas bring the saved plate of the created word and the mark in the dictionary`() {
        val created = reducer.created(reducer.withCard("run", Fx.run), "run", runKey, Fx.RUN_CREATED)

        val reloaded = reducer.testReduce(created, Msg.LemmasReloaded("run", listOf(Fx.runCreated)))

        val state = reloaded.state()
        assertEquals(listOf(Fx.savedWord(Fx.runCreated)), state.card("run").saved)
        assertEquals(1, state.lemmaCard("run").existingCount)
        assertTrue(state.word("run").isInDictionary)
        assertEquals(listOf(Fx.RUN_CREATED), requireNotNull(state.form).existingLemmas.map { it.wordId })
        assertFalse(state.lexeme("run", manageKey).exists)
        assertTrue(state.lexeme("run", manageKey).canWrite)
        reloaded.assertNoEffects()
    }

    @Test
    fun `AWP-63_2 AWP-63_4 transfer into the word in the dictionary shows the lexeme in its saved plate and moves no target`() {
        val checked = reducer.steps(
            reducer.step(reducer.windowOf("run", form = Fx.form(existing = listOf(Fx.runExisting))), Msg.OpenSavedCard("run")),
            Msg.LoadLexemes("run"),
            Fx.lexemes("run", Fx.runThree),
            Msg.SetCardTarget("run", runKey, CardTargetUi.Existing(Fx.RUN_WORD)),
        )
        val grown = Fx.runExisting.copy(
            lexemeCount = 2,
            coreValues = listOf("управлять", "бежать, бегать"),
            lexemes = listOf(Fx.saved(1L, "управлять"), Fx.saved(12L, "бежать, бегать")),
        )

        val transferred = reducer.testReduce(reducer.step(checked, Msg.AddLexeme("run", runKey)), Msg.LexemeAdded("run", runKey))
        val reloaded = reducer.step(transferred.state(), Msg.LemmasReloaded("run", listOf(grown)))

        assertEquals(listOf(manageKey, machineKey), transferred.state().lemmaCard("run").lexemes.map { it.key })
        assertEquals(CardTargetUi.NewWord, transferred.state().lexeme("run", machineKey).target)
        transferred.assertEffects(reloadRun)
        assertEquals(listOf(Fx.savedWord(grown)), reloaded.card("run").saved)
        assertEquals(listOf(listOf("Перевод: управлять"), listOf("Перевод: бежать, бегать")), reloaded.card("run").saved.single().lexemes.map { it.shown(requireNotNull(reloaded.form)) })
    }

    @Test
    fun `AWP-63_3 an explicit choice of another word stays after create`() {
        val form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))
        val shore = Fx.lexeme(
            key = "ruwiktionary/1.noun.2",
            translations = listOf("набережная"),
            partOfSpeechKey = "noun",
            matches = listOf(Fx.byField(Fx.TRANSLATION, "translations"), Fx.byOption(Fx.PART_OF_SPEECH, Fx.NOUN)),
        )
        val answer = Fx.word("bank", Fx.lemma("bank", listOf(Fx.bankNoun, shore)))
        val loaded = reducer.step(
            reducer.withCard("bank", answer, form = form),
            Msg.SetCardTarget("bank", "ruwiktionary/1.noun.2", CardTargetUi.Existing(Fx.BANK_2)),
        )

        val created = reducer.created(loaded, "bank", bankKey, 9L)

        assertEquals(CardTargetUi.Existing(Fx.BANK_2), created.lexeme("bank", "ruwiktionary/1.noun.2").target)
    }

    @Test
    fun `AWP-50_15 after the write the marks are counted again by the reloaded dictionary`() {
        val twin = Fx.lexeme(
            key = "enwiktionary/1.verb.2",
            translations = listOf("бежать, бегать"),
            partOfSpeechKey = "verb",
            matches = listOf(Fx.byField(Fx.TRANSLATION, "translations"), Fx.byOption(Fx.PART_OF_SPEECH, Fx.VERB)),
        )
        val answer = Fx.word("run", Fx.lemma("run", listOf(Fx.runVerb1, twin)))
        val created = reducer.created(reducer.withCard("run", answer), "run", runKey, Fx.RUN_CREATED)

        val reloaded = reducer.step(created, Msg.LemmasReloaded("run", listOf(Fx.runCreated)))

        assertFalse(created.lexeme("run", "enwiktionary/1.verb.2").exists)
        assertTrue(reloaded.lexeme("run", "enwiktionary/1.verb.2").exists)
        assertFalse(reloaded.lexeme("run", "enwiktionary/1.verb.2").canWrite)
    }

    @Test
    fun `AWP-63_6 AWP-21_7 a failed reload or a failed write leaves the saved plates as they were`() {
        val checked = reducer.steps(
            reducer.step(reducer.windowOf("run", form = Fx.form(existing = listOf(Fx.runExisting))), Msg.OpenSavedCard("run")),
            Msg.LoadLexemes("run"),
            Fx.lexemes("run", Fx.run),
        )

        val reloadFailed = reducer.testReduce(reducer.transferred(checked, "run", runKey), Msg.LemmasReloadFailed("run"))
        val writeFailed = reducer.step(reducer.step(checked, Msg.AddLexeme("run", runKey)), Msg.AddFailed("run", runKey, AddFailureUi.Failed))

        assertEquals(listOf(Fx.savedWord(Fx.runExisting)), reloadFailed.state().card("run").saved)
        reloadFailed.assertNoEffects()
        assertEquals(listOf(Fx.savedWord(Fx.runExisting)), writeFailed.card("run").saved)
    }

    @Test
    fun `AWP-62_3 AWP-21_1 a failed write keeps the row and the plate with the failure and retry`() {
        val adding = adding()

        val result = reducer.testReduce(adding, Msg.AddFailed("run", runKey, AddFailureUi.Failed))

        val state = result.state()
        assertEquals(AddFailureUi.Failed, state.lexeme("run", runKey).addFailure)
        assertFalse(state.lexeme("run", runKey).isAdding)
        assertFalse(state.card("run").isAdding)
        assertFalse(state.isAdding)
        assertEquals(adding.lemmaCard("run").lexemes.map { it.key }, state.lemmaCard("run").lexemes.map { it.key })
        assertEquals(CardPhase.CANDIDATES, state.card("run").phase)
        assertEquals(listOf("run"), state.lists.lexical.texts())
        result.assertNoEffects()
    }

    @Test
    fun `the screen stays adding while a plate of another word is still being written`() {
        val two = Fx.lists(lexical = listOf("bank", "tiempo"))
        val both = reducer.steps(
            reducer.withCard("bank", Fx.bank, lists = two),
            Msg.LoadLexemes("tiempo"),
            Fx.lexemes("tiempo", Fx.tiempo),
            Msg.AddLexeme("bank", bankKey),
            Msg.AddLexeme("tiempo", tiempoKey),
        )

        val oneLeft = reducer.step(both, Msg.WordCreated("bank", bankKey, 9L))
        val done = reducer.step(oneLeft, Msg.WordCreated("tiempo", tiempoKey, 10L))

        assertTrue(both.isAdding)
        assertTrue(oneLeft.isAdding)
        assertTrue(oneLeft.card("tiempo").isAdding)
        assertFalse(done.isAdding)
        assertTrue(done.lists.lexical.isEmpty())
    }

    // ===== Запись не удалась (AWP-21) =====

    @Test
    fun `AWP-21_3 a dead common group goes back to all, own groups of the other plates stay`() {
        val both = reducer.steps(
            reducer.withCard("run", Fx.run),
            Msg.SetCommonGroup(Fx.POTTER),
            Msg.SetCardGroup("run", manageKey, CardOwnGroupUi.Group(Fx.FINANCE)),
            Msg.AddLexeme("run", runKey),
        )

        val result = reducer.step(both, Msg.AddGroupGone("run", runKey, AddFailureUi.GroupMissing(Fx.POTTER)))

        assertNull(result.commonGroupId)
        assertEquals(CardOwnGroupUi.Group(Fx.FINANCE), result.lexeme("run", manageKey).ownGroup)
        assertEquals(listOf(Fx.FINANCE), requireNotNull(result.form).groups.map { it.id })
        assertEquals(AddFailureUi.GroupMissing(Fx.POTTER), result.lexeme("run", runKey).addFailure)
        assertFalse(result.card("run").isAdding)
        assertFalse(result.isAdding)
    }

    @Test
    fun `AWP-21_4 a dead word of the dictionary turns the target into a new word and its saved plate goes`() {
        val form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))
        val chosen = reducer.step(
            reducer.withCard("bank", Fx.bank, form = form),
            Msg.SetCardTarget("bank", bankKey, CardTargetUi.Existing(Fx.BANK_1)),
        )
        val adding = reducer.step(chosen, Msg.AddLexeme("bank", bankKey))

        val result = reducer.step(adding, Msg.AddWordGone("bank", bankKey, AddFailureUi.WordMissing(Fx.BANK_1)))

        assertEquals(CardTargetUi.Existing(Fx.BANK_1), adding.lexeme("bank", bankKey).target)
        assertEquals(CardTargetUi.NewWord, result.lexeme("bank", bankKey).target)
        assertEquals(listOf(Fx.savedWord(Fx.bank2)), result.card("bank").saved)
        assertEquals(1, result.lemmaCard("bank").existingCount)
        assertEquals(listOf(Fx.BANK_2), requireNotNull(result.form).existingLemmas.map { it.wordId })
        assertEquals(AddFailureUi.WordMissing(Fx.BANK_1), result.lexeme("bank", bankKey).addFailure)
        assertFalse(result.card("bank").isAdding)
    }

    @Test
    fun `AWP-21_5 a dead own group falls back to the common one`() {
        val own = reducer.step(reducer.withCard("bank", Fx.bank), Msg.SetCardGroup("bank", bankKey, CardOwnGroupUi.Group(Fx.FINANCE)))
        val adding = reducer.step(own, Msg.AddLexeme("bank", bankKey))

        val result = reducer.step(adding, Msg.AddGroupGone("bank", bankKey, AddFailureUi.GroupMissing(Fx.FINANCE)))

        assertEquals(CardOwnGroupUi.AsCommon, result.lexeme("bank", bankKey).ownGroup)
        assertEquals(listOf(Fx.POTTER), requireNotNull(result.form).groups.map { it.id })
    }

    @Test
    fun `AWP-21_6 AWP-47_3 a switched off component loses its values, a switched off core switches the write off`() {
        val perroKey = "ruwiktionary/1.noun.1"
        val adding = reducer.step(reducer.withCard("perro", Fx.perro), Msg.AddLexeme("perro", perroKey))
        val glossGone = Msg.AddComponentGone("perro", perroKey, AddFailureUi.TypeUnavailable(Fx.GLOSS))
        val coreGone = Msg.AddComponentGone("perro", perroKey, AddFailureUi.TypeUnavailable(Fx.TRANSLATION))

        val gloss = reducer.step(adding, glossGone)
        val core = reducer.step(adding, coreGone)

        assertEquals(
            listOf(
                "Перевод: собака; пёс (также ругательство)",
                "Часть речи: существительное",
                "Род: м.р.",
                "Пример: perro callejero",
            ),
            gloss.shown("perro", perroKey),
        )
        assertTrue(requireNotNull(gloss.form).components.none { it.id == Fx.GLOSS })
        assertTrue(gloss.lexeme("perro", perroKey).canWrite)
        assertEquals(AddFailureUi.TypeUnavailable(Fx.GLOSS), gloss.lexeme("perro", perroKey).addFailure)
        assertFalse(gloss.card("perro").isAdding)
        assertTrue(core.lemmaCard("perro").lexemes.all { it.lacksCore && !it.canWrite })
    }

    @Test
    fun `AWP-21_6 a switched off option owner takes the dependent gender with it`() {
        val perroKey = "ruwiktionary/1.noun.1"
        val adding = reducer.step(reducer.withCard("perro", Fx.perro), Msg.AddLexeme("perro", perroKey))

        val result = reducer.step(adding, Msg.AddComponentGone("perro", perroKey, AddFailureUi.TypeUnavailable(Fx.PART_OF_SPEECH)))

        assertEquals(
            listOf(
                "Перевод: собака; пёс (также ругательство)",
                "Толкование: dog (the species Canis familiaris …)",
                "Пример: perro callejero",
            ),
            result.shown("perro", perroKey),
        )
    }

    // ===== Повтор записи (AWP-26) =====

    @Test
    fun `AWP-26_1 AWP-21_2 AWP-02_2 retry writes the plate again once without the failure`() {
        val failed = reducer.step(adding(), Msg.AddFailed("run", runKey, AddFailureUi.Failed))

        val result = reducer.testReduce(failed, Msg.RetryAdd("run", runKey))

        val state = result.state()
        assertTrue(state.lexeme("run", runKey).isAdding)
        assertNull(state.lexeme("run", runKey).addFailure)
        assertTrue(state.card("run").isAdding)
        assertTrue(state.isAdding)
        val effect = result.effects().single() as DatasourceEffect.CreateWord
        assertEquals("run", effect.word)
        assertEquals(runKey, effect.key)
    }

    @Test
    fun `AWP-26_1 retry after a group reset writes without the dead group`() {
        val grouped = reducer.step(reducer.withCard("run", Fx.run), Msg.SetCommonGroup(Fx.POTTER))
        val adding = reducer.step(grouped, Msg.AddLexeme("run", runKey))
        val rejected = reducer.step(adding, Msg.AddGroupGone("run", runKey, AddFailureUi.GroupMissing(Fx.POTTER)))

        val effects = reducer.effectsOf(rejected, Msg.RetryAdd("run", runKey))

        val batch = (effects.single() as DatasourceEffect.CreateWord).batch
        assertTrue(batch.words.all { it.groupId == null })
    }

    @Test
    fun `AWP-26_2 closing from the failed state asks the question while the word is still pending`() {
        val failed = reducer.step(adding(), Msg.AddFailed("run", runKey, AddFailureUi.Failed))

        val result = reducer.testReduce(failed, Msg.RequestClose)

        assertTrue(result.state().isConfirmingClose)
        assertEquals(1, result.state().pendingWords)
        result.assertNoEffects()
    }

    @Test
    fun `write on a plate already in the dictionary or without a core changes nothing`() {
        val form = Fx.form(existing = listOf(Fx.existing(Fx.RUN_WORD, "run", listOf("бежать, бегать", "управлять"))))
        val allExisting = reducer.withCard("run", Fx.run, form = form)

        val result = reducer.testReduce(allExisting, Msg.AddLexeme("run", runKey))

        assertEquals(allExisting, result.state())
        result.assertNoEffects()
        assertTrue(allExisting.lexeme("run", runKey).exists)
    }
}
