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
import me.apomazkin.candidates.LookupWordUi
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
 * Добавление глазами редьюсера: что показывает кнопка (AWP-13), вопрос и пакет
 * записи (AWP-03, AWP-20, AWP-32), записано (AWP-23), отказ и повтор (AWP-21,
 * AWP-26). Адреса строк спеки — `AWP-NN_k`.
 */
class AddReducerTest {
    private val reducer = reducer()

    private val runKey = "ruwiktionary/1.verb.1"
    private val withRun = Fx.form(existing = listOf(Fx.runExisting))

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

    /** Окно после вопроса и «да»: идёт запись. */
    private fun adding(vararg words: LookupWordUi): ScanCandidatesState =
        reducer.step(reducer.step(reducer.window(*words), Msg.RequestAdd), Msg.ConfirmAdd)

    // ===== Кнопка «добавить» и вопрос (AWP-13) =====

    @Test
    fun `AWP-13_1 pressing add asks the question with the counts and writes nothing`() {
        val window = reducer.window(Fx.ran, Fx.bank)

        val result = reducer.testReduce(window, Msg.RequestAdd)

        assertTrue(result.state().isConfirmingAdd)
        assertEquals(3, result.state().summary.lexemeCount)
        assertEquals(2, result.state().summary.wordCount)
        result.assertNoEffects()
    }

    @Test
    fun `AWP-13_2 everything deleted leaves nothing to add and the button off`() {
        val window = reducer.window(Fx.ran, Fx.bank)

        val result = reducer.steps(window, Msg.DeleteCard("run"), Msg.DeleteCard("bank"))

        assertEquals(0, result.summary.lexemeCount)
        assertFalse(result.summary.isEnabled)
    }

    @Test
    fun `AWP-13_3 lexemes left without a core switch the button off with their count`() {
        val adding = adding(Fx.ran)
        val coreGone = Msg.AddComponentGone(AddFailureUi.TypeUnavailable(Fx.TRANSLATION))

        val result = reducer.step(adding, coreGone)

        assertEquals(2, result.summary.lacksCoreCount)
        assertFalse(result.summary.isEnabled)
    }

    @Test
    fun `AWP-13_5 one existing lexeme out of three is not counted but shown`() {
        val window = reducer.window(Fx.runThree, form = withRun)

        assertEquals(2, window.summary.lexemeCount)
        assertEquals(1, window.summary.wordCount)
        assertEquals(1, window.summary.existingCount)
        assertTrue(window.summary.isEnabled)
    }

    @Test
    fun `AWP-13_6 only existing lexemes left give nothing to add`() {
        val window = reducer.window(Fx.runThree, form = withRun)

        val result = reducer.step(
            reducer.step(window, Msg.DeleteLexeme("run", runKey)),
            Msg.DeleteLexeme("run", "enwiktionary/1.verb.37"),
        )

        assertEquals(0, result.summary.lexemeCount)
        assertEquals(1, result.summary.existingCount)
        assertFalse(result.summary.isEnabled)
    }

    @Test
    fun `AWP-13_7 cancel in the question keeps the window as it was and writes nothing`() {
        val asking = reducer.step(reducer.window(Fx.ran), Msg.RequestAdd)

        val result = reducer.testReduce(asking, Msg.CancelAdd)

        assertFalse(result.state().isConfirmingAdd)
        assertEquals(asking.cards, result.state().cards)
        result.assertNoEffects()
    }

    @Test
    fun `AWP-13_4 yes in the question closes it and the window goes busy`() {
        val asking = reducer.step(reducer.window(Fx.ran), Msg.RequestAdd)

        val result = reducer.testReduce(asking, Msg.ConfirmAdd)

        assertFalse(result.state().isConfirmingAdd)
        assertTrue(result.state().isAdding)
        assertNull(result.state().addFailure)
    }

    // ===== Пакет записи (AWP-03, AWP-20) =====

    @Test
    fun `AWP-20_1 yes sends one batch with new and existing lemmas in the common group`() {
        val window = reducer.window(Fx.ran, Fx.bank, form = Fx.form(existing = listOf(Fx.bank1)))
        val grouped = reducer.step(window, Msg.SetCommonGroup(Fx.POTTER))

        val effects = reducer.effectsOf(reducer.step(grouped, Msg.RequestAdd), Msg.ConfirmAdd)

        val batch = (effects.single() as DatasourceEffect.AddWords).batch
        assertEquals(Fx.DICT, batch.dictionaryId)
        assertEquals(listOf("run", "bank"), batch.words.map { it.text })
        assertEquals(listOf<Long?>(null, Fx.BANK_1), batch.words.map { it.wordId })
        assertEquals(listOf(2, 1), batch.words.map { it.lexemes.size })
        assertEquals(listOf<Long?>(Fx.POTTER, Fx.POTTER), batch.words.map { it.groupId })
    }

    @Test
    fun `AWP-20_3 verb and noun lexemes of one lemma are two lexemes of one word`() {
        val window = reducer.window(Fx.runVerbAndNoun)

        val batch = reducer.batchOf(window)

        val word = batch.words.single()
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
        val window = reducer.window(Fx.ran, form = withRun)

        val batch = reducer.batchOf(window)

        assertEquals(
            listOf(runVerbWritten()),
            batch.words.single().lexemes.map { it.written() },
        )
    }

    @Test
    fun `AWP-03_1 only what is left in the window is written`() {
        val window = reducer.window(Fx.ran, Fx.running)
        val trimmed = listOf("ruwiktionary/1.verb.2", "enwiktionary/1.verb.37")
            .fold(window) { state, key -> reducer.step(state, Msg.DeleteLexeme("run", key)) }

        val batch = reducer.batchOf(trimmed)

        assertEquals(listOf("run", "running"), batch.words.map { it.text })
        assertEquals(listOf(1, 1), batch.words.map { it.lexemes.size })
    }

    @Test
    fun `AWP-03_2 two of three examples deleted leave one example in the batch`() {
        val window = reducer.window(Fx.tiempo)
        val first = Fx.ref("tiempo", "ruwiktionary/1.noun.3", Fx.EXAMPLE, index = 0)
        val trimmed = reducer.steps(window, Msg.DeleteValue(first), Msg.DeleteValue(first))

        val batch = reducer.batchOf(trimmed)

        val examples = batch.words.single().lexemes.single().written()
            .filter { it.first == Fx.EXAMPLE }
        assertEquals(listOf(Fx.EXAMPLE to sourced(Fx.tiempoExamples[2])), examples)
    }

    @Test
    fun `AWP-03_3 nothing deleted or edited writes all that is shown except the existing`() {
        val window = reducer.window(Fx.ran, Fx.bank, Fx.tiempo)

        val batch = reducer.batchOf(window)

        assertEquals(listOf("run", "bank", "tiempo"), batch.words.map { it.text })
        assertEquals(listOf(2, 1, 1), batch.words.map { it.lexemes.size })
    }

    // ===== Компоненты лексемы по сопоставлению (AWP-32) =====

    @Test
    fun `AWP-32_1 perro noun gets translation, part of speech, gender, example and gloss`() {
        val batch = reducer.batchOf(reducer.window(Fx.perro))

        assertEquals(
            listOf(
                Fx.TRANSLATION to text("собака; пёс (также ругательство)"),
                Fx.PART_OF_SPEECH to choice(Fx.NOUN),
                Fx.GENDER to choice(Fx.MASCULINE),
                Fx.EXAMPLE to sourced("perro callejero"),
                Fx.GLOSS to text("dog (the species Canis familiaris …)"),
            ),
            batch.words.single().lexemes.first().written(),
        )
    }

    @Test
    fun `AWP-32_2 corrian verb gets translation, verb and gloss only`() {
        val batch = reducer.batchOf(reducer.window(Fx.corrian))

        assertEquals(
            listOf(
                Fx.TRANSLATION to text("течь, литься, струиться (о жидкости)"),
                Fx.PART_OF_SPEECH to choice(Fx.VERB),
                Fx.GLOSS to text("to flow"),
            ),
            batch.words.single().lexemes.first().written(),
        )
    }

    @Test
    fun `AWP-32_6 all examples deleted give no example component`() {
        val window = reducer.window(Fx.tiempo)
        val example = Fx.ref("tiempo", "ruwiktionary/1.noun.3", Fx.EXAMPLE)
        val cleared = (1..Fx.tiempoExamples.size)
            .fold(window) { state, _ -> reducer.step(state, Msg.DeleteValue(example)) }

        val batch = reducer.batchOf(cleared)

        assertEquals(
            listOf(Fx.TRANSLATION to text("погода"), Fx.PART_OF_SPEECH to choice(Fx.NOUN)),
            batch.words.single().lexemes.single().written(),
        )
    }

    @Test
    fun `AWP-32_8 a multiple choice with two matches gives two values`() {
        val window = reducer.window(Fx.llegar, form = Fx.formWithStyle())

        val batch = reducer.batchOf(window)

        assertEquals(
            listOf(
                Fx.TRANSLATION to text("прибывать"),
                Fx.STYLE to choice(Fx.FIGURATIVE),
                Fx.STYLE to choice(Fx.COLLOQUIAL),
            ),
            batch.words.single().lexemes.single().written(),
        )
    }

    @Test
    fun `AWP-32_11 a gender is not written after the part of speech changed to a verb`() {
        val window = reducer.window(Fx.perro)
        val partOfSpeech = Fx.componentRef("perro", "ruwiktionary/1.noun.1", Fx.PART_OF_SPEECH)
        val verb = reducer.step(window, Msg.SetOption(partOfSpeech, Fx.VERB))

        val batch = reducer.batchOf(verb)

        val written = batch.words.single().lexemes.first().written()
        assertTrue(written.none { it.first == Fx.GENDER })
        assertTrue(Fx.PART_OF_SPEECH to choice(Fx.VERB) in written)
    }

    // ===== Записано (AWP-23) =====

    @Test
    fun `AWP-23_1 a finished write closes the screen with the counts for the tab`() {
        val window = reducer.window(Fx.ran, Fx.bank)
        val adding = reducer.steps(window, Msg.RequestAdd, Msg.ConfirmAdd)

        val result = reducer.testReduce(adding, Msg.AddFinished(lexemeCount = 3, wordCount = 2))

        assertEquals(adding, result.state())
        result.assertEffects(setOf<Effect>(ScanNavigationEffect.CloseWithResult(3, 2)))
    }

    // ===== Запись не удалась (AWP-21) =====

    @Test
    fun `AWP-21_1 a failed write keeps the window and shows the failure with retry`() {
        val adding = adding(Fx.ran, Fx.bank)

        val result = reducer.testReduce(adding, Msg.AddFailed(AddFailureUi.Failed))

        assertFalse(result.state().isAdding)
        assertEquals(AddFailureUi.Failed, result.state().addFailure)
        assertEquals(adding.cards, result.state().cards)
        result.assertNoEffects()
    }

    @Test
    fun `AWP-21_3 a dead common group goes back to all and own groups of the cards stay`() {
        val window = reducer.window(Fx.ran, Fx.bank, form = Fx.form(existing = listOf(Fx.bank1)))
        val common = reducer.step(window, Msg.SetCommonGroup(Fx.POTTER))
        val own = reducer.step(common, Msg.SetCardGroup("bank", CardOwnGroupUi.Group(Fx.FINANCE)))
        val adding = reducer.step(reducer.step(own, Msg.RequestAdd), Msg.ConfirmAdd)

        val result = reducer.step(adding, Msg.AddGroupGone(AddFailureUi.GroupMissing(Fx.POTTER)))

        assertNull(result.commonGroupId)
        assertEquals(CardOwnGroupUi.Group(Fx.FINANCE), result.card("bank").ownGroup)
        assertEquals(listOf(Fx.FINANCE), requireNotNull(result.form).groups.map { it.id })
        assertEquals(AddFailureUi.GroupMissing(Fx.POTTER), result.addFailure)
        assertFalse(result.isAdding)
    }

    @Test
    fun `AWP-21_4 a dead lemma of the dictionary turns the target into a new word`() {
        val window = reducer.window(Fx.bank, form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2)))
        val adding = reducer.step(reducer.step(window, Msg.RequestAdd), Msg.ConfirmAdd)

        val result = reducer.step(adding, Msg.AddWordGone(AddFailureUi.WordMissing(Fx.BANK_1)))

        assertEquals(CardTargetUi.NewWord, result.card("bank").target)
        val lemmaIds = requireNotNull(result.form).existingLemmas.map { it.wordId }
        assertEquals(listOf(Fx.BANK_2), lemmaIds)
        assertEquals(AddFailureUi.WordMissing(Fx.BANK_1), result.addFailure)
    }

    @Test
    fun `AWP-21_5 a dead own group falls back to the common one`() {
        val window = reducer.window(Fx.bank)
        val own = reducer.step(window, Msg.SetCardGroup("bank", CardOwnGroupUi.Group(Fx.FINANCE)))
        val adding = reducer.step(reducer.step(own, Msg.RequestAdd), Msg.ConfirmAdd)

        val result = reducer.step(adding, Msg.AddGroupGone(AddFailureUi.GroupMissing(Fx.FINANCE)))

        assertEquals(CardOwnGroupUi.AsCommon, result.card("bank").ownGroup)
    }

    @Test
    fun `AWP-21_6 a switched off component loses its values and the marks are recounted`() {
        val adding = adding(Fx.perro)

        val glossGone = Msg.AddComponentGone(AddFailureUi.TypeUnavailable(Fx.GLOSS))
        val coreGone = Msg.AddComponentGone(AddFailureUi.TypeUnavailable(Fx.TRANSLATION))
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
        assertTrue(gloss.summary.isEnabled)
        assertEquals(AddFailureUi.TypeUnavailable(Fx.GLOSS), gloss.addFailure)
        assertFalse(gloss.isAdding)
        assertEquals(3, core.summary.lacksCoreCount)
        assertFalse(core.summary.isEnabled)
    }

    @Test
    fun `AWP-21_6 a switched off option owner takes the dependent gender with it`() {
        val adding = adding(Fx.perro)

        val result = reducer.step(
            adding,
            Msg.AddComponentGone(AddFailureUi.TypeUnavailable(Fx.PART_OF_SPEECH)),
        )

        assertEquals(
            listOf(
                "Перевод: собака; пёс (также ругательство)",
                "Пример: perro callejero",
                "Толкование: dog (the species Canis familiaris …)",
            ),
            result.shown("perro", "ruwiktionary/1.noun.1"),
        )
    }

    // ===== Повтор записи (AWP-26) =====

    @Test
    fun `AWP-26_1 AWP-21_2 AWP-02_2 retry writes again once without the question`() {
        val adding = adding(Fx.ran)
        val failed = reducer.step(adding, Msg.AddFailed(AddFailureUi.Failed))

        val result = reducer.testReduce(failed, Msg.RetryAdd)

        assertTrue(result.state().isAdding)
        assertNull(result.state().addFailure)
        assertFalse(result.state().isConfirmingAdd)
        assertEquals(failed.cards, result.state().cards)
        assertTrue(result.effects().single() is DatasourceEffect.AddWords)
    }

    @Test
    fun `AWP-26_1 retry after a group reset writes without the dead group`() {
        val window = reducer.step(reducer.window(Fx.ran), Msg.SetCommonGroup(Fx.POTTER))
        val adding = reducer.step(reducer.step(window, Msg.RequestAdd), Msg.ConfirmAdd)
        val rejected = reducer.step(adding, Msg.AddGroupGone(AddFailureUi.GroupMissing(Fx.POTTER)))

        val effects = reducer.effectsOf(rejected, Msg.RetryAdd)

        val batch = (effects.single() as DatasourceEffect.AddWords).batch
        assertTrue(batch.words.all { it.groupId == null })
    }
}
