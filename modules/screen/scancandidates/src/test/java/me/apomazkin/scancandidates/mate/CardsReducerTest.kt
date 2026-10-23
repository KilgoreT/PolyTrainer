package me.apomazkin.scancandidates.mate

import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.assertNoEffects
import io.github.kilgoret.mate.test.testReduce
import me.apomazkin.candidates.CardTargetUi
import me.apomazkin.candidates.NotAddedReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Карточки лемм глазами редьюсера: сборка по ответу (AWP-04), пометки «есть»
 * (AWP-07), слова без добавления (AWP-12), удаление (AWP-15). Адреса строк
 * спеки — `AWP-NN_k`.
 */
class CardsReducerTest {
    private val reducer = reducer()

    // ===== Сборка (AWP-04) =====

    @Test
    fun `AWP-04_1 a form and its lemma make two cards, the shared lemma lists both words`() {
        val window = reducer.window(Fx.ran, Fx.running)

        assertEquals(listOf("run", "running"), window.cards.map { it.lemma })
        assertEquals(listOf("ran", "running"), window.card("run").sourceWords)
        assertEquals(listOf("running"), window.card("running").sourceWords)
    }

    @Test
    fun `AWP-04_3 two words with one lemma make one card without doubled lexemes`() {
        val window = reducer.window(Fx.ran, Fx.runs)

        assertEquals(listOf("run"), window.cards.map { it.lemma })
        assertEquals(listOf("ran", "runs"), window.card("run").sourceWords)
        assertEquals(
            listOf("ruwiktionary/1.verb.1", "ruwiktionary/1.verb.2"),
            window.card("run").lexemes.map { it.key },
        )
    }

    @Test
    fun `AWP-04_4 an answer without found words has no cards and nothing to add`() {
        val window = reducer.window(Fx.accorc)

        assertTrue(window.cards.isEmpty())
        assertEquals(0, window.summary.lexemeCount)
        assertFalse(window.summary.isEnabled)
    }

    @Test
    fun `AWP-04_8 one lexeme key from two words is one lexeme taken whole from the first word`() {
        val window = reducer.window(Fx.ran, Fx.running)

        assertEquals(
            listOf("ruwiktionary/1.verb.1", "ruwiktionary/1.verb.2", "enwiktionary/1.verb.37"),
            window.card("run").lexemes.map { it.key },
        )
        assertEquals(
            listOf("Перевод: бежать, бегать", "Часть речи: глагол"),
            window.shown("run", "ruwiktionary/1.verb.1"),
        )
    }

    @Test
    fun `AWP-04_9 a found word without lemmas has no card`() {
        val window = reducer.window(Fx.ran, Fx.foundWithoutLemmas("bank"))

        assertEquals(listOf("run"), window.cards.map { it.lemma })
        assertEquals(listOf("bank"), window.notAdded.map { it.text })
    }

    @Test
    fun `AWP-04_12 lexemes of a card keep the order of the answer`() {
        val window = reducer.window(Fx.corrian)

        assertEquals(
            listOf("ruwiktionary/1.verb.3", "ruwiktionary/1.verb.1", "ruwiktionary/1.verb.5"),
            window.card("correr").lexemes.map { it.key },
        )
    }

    // ===== Пометка «есть» (AWP-07) =====

    @Test
    fun `AWP-07_1 a lexeme whose core is in the dictionary is marked, the card counts lemmas`() {
        val window = reducer.window(Fx.ran, form = Fx.form(existing = listOf(Fx.runExisting)))

        assertEquals(1, window.card("run").existingCount)
        assertEquals(CardTargetUi.Existing(Fx.RUN_WORD), window.card("run").target)
        assertTrue(window.lexeme("run", "ruwiktionary/1.verb.2").exists)
        assertFalse(window.lexeme("run", "ruwiktionary/1.verb.1").exists)
    }

    @Test
    fun `AWP-07_2 two lemmas with the same text count two`() {
        val window = reducer.window(Fx.bank, form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2)))

        assertEquals(2, window.card("bank").existingCount)
    }

    @Test
    fun `AWP-07_7 a text shortened to a value in the dictionary gets the mark after the edit`() {
        val window = reducer.window(Fx.ran, form = Fx.form(existing = listOf(Fx.runExisting)))
        val ref = Fx.ref("run", "ruwiktionary/1.verb.1", Fx.TRANSLATION)

        val result = reducer.step(window, Msg.SubmitText(ref, "управлять"))

        assertTrue(result.lexeme("run", "ruwiktionary/1.verb.1").exists)
        assertEquals(0, result.summary.lexemeCount)
        assertEquals(2, result.summary.existingCount)
    }

    @Test
    fun `AWP-07_10 a changed text of an existing lexeme loses the mark`() {
        val window = reducer.window(Fx.ran, form = Fx.form(existing = listOf(Fx.runExisting)))
        val ref = Fx.ref("run", "ruwiktionary/1.verb.2", Fx.TRANSLATION)

        val result = reducer.step(window, Msg.SubmitText(ref, "руководить"))

        assertFalse(result.lexeme("run", "ruwiktionary/1.verb.2").exists)
        assertEquals(2, result.summary.lexemeCount)
    }

    // ===== Слова без добавления (AWP-12) =====

    @Test
    fun `AWP-12_1 AWP-12_2 not found and error words go to a collapsed block that toggles`() {
        val window = reducer.window(Fx.ran, Fx.notFound("qwrt"), Fx.failed("Grimmauld"))

        val expanded = reducer.step(window, Msg.ToggleNotAdded)
        val collapsed = reducer.step(expanded, Msg.ToggleNotAdded)

        assertEquals(listOf("qwrt", "Grimmauld"), window.notAdded.map { it.text })
        assertEquals(
            listOf(NotAddedReason.NOT_FOUND, NotAddedReason.ERROR),
            window.notAdded.map { it.reason },
        )
        assertFalse(window.isNotAddedExpanded)
        assertTrue(expanded.isNotAddedExpanded)
        assertFalse(collapsed.isNotAddedExpanded)
    }

    @Test
    fun `AWP-12_3 no cards open the block expanded with the button off`() {
        val window = reducer.window(Fx.notFound("qwrt"), Fx.failed("Grimmauld"))

        assertTrue(window.cards.isEmpty())
        assertTrue(window.isNotAddedExpanded)
        assertFalse(window.summary.isEnabled)
    }

    @Test
    fun `AWP-12_9 all words failed open the window with the block expanded and nothing to add`() {
        val window = reducer.window(Fx.llave, Fx.banco, Fx.failed("Grimmauld"))

        assertTrue(window.cards.isEmpty())
        assertEquals(listOf("llave", "banco", "Grimmauld"), window.notAdded.map { it.text })
        assertTrue(window.notAdded.all { it.reason == NotAddedReason.ERROR })
        assertTrue(window.isNotAddedExpanded)
        assertFalse(window.summary.isEnabled)
    }

    @Test
    fun `AWP-12_6 AWP-12_7 an error word and a found word without lemmas are listed`() {
        val window = reducer.window(Fx.ran, Fx.banco, Fx.foundWithoutLemmas("bank"))

        assertEquals(listOf("banco", "bank"), window.notAdded.map { it.text })
        assertEquals(
            listOf(NotAddedReason.ERROR, NotAddedReason.NO_LEXEMES),
            window.notAdded.map { it.reason },
        )
    }

    // ===== Удаление (AWP-15) =====

    @Test
    fun `AWP-15_1 deleting a lexeme leaves the others, raises the flag and recounts the button`() {
        val window = reducer.window(Fx.ran, Fx.running)

        val result = reducer.step(window, Msg.DeleteLexeme("run", "ruwiktionary/1.verb.2"))

        assertEquals(
            listOf("ruwiktionary/1.verb.1", "enwiktionary/1.verb.37"),
            result.card("run").lexemes.map { it.key },
        )
        assertTrue(result.isDirty)
        assertEquals(3, result.summary.lexemeCount)
    }

    @Test
    fun `AWP-15_2 deleting one example keeps the other examples and the lexeme`() {
        val window = reducer.window(Fx.tiempo)
        val second = Fx.ref("tiempo", "ruwiktionary/1.noun.3", Fx.EXAMPLE, index = 1)

        val result = reducer.step(window, Msg.DeleteValue(second))

        assertEquals(
            listOf(Fx.tiempoExamples[0], Fx.tiempoExamples[2]),
            result.card("tiempo").lexemes.single().texts(Fx.EXAMPLE),
        )
        assertTrue(result.isDirty)
        assertEquals(1, result.summary.lexemeCount)
    }

    @Test
    fun `AWP-15_3 deleting all lexemes leaves an empty card that adds nothing`() {
        val window = reducer.window(Fx.ran, Fx.running)

        val result = listOf(
            "ruwiktionary/1.verb.1",
            "ruwiktionary/1.verb.2",
            "enwiktionary/1.verb.37",
        ).fold(window) { state, key -> reducer.step(state, Msg.DeleteLexeme("run", key)) }

        assertTrue(result.card("run").lexemes.isEmpty())
        assertEquals(listOf("run", "running"), result.cards.map { it.lemma })
        assertEquals(1, result.summary.lexemeCount)
        assertEquals(1, result.summary.wordCount)
    }

    @Test
    fun `AWP-15_4 deleting a card removes the lemma only, its words are not listed as not added`() {
        val window = reducer.window(Fx.ran, Fx.running)

        val result = reducer.step(window, Msg.DeleteCard("run"))

        assertEquals(listOf("running"), result.cards.map { it.lemma })
        assertTrue(result.notAdded.isEmpty())
        assertTrue(result.isDirty)
    }

    @Test
    fun `AWP-15_7 deleting one value of a multiple choice keeps the other and the lexeme`() {
        val form = Fx.formWithStyle()
        val window = reducer.window(Fx.llegar, form = form)
        val colloquial = Fx.ref("llegar", "ruwiktionary/1.verb.1", Fx.STYLE, index = 1)

        val result = reducer.step(window, Msg.DeleteValue(colloquial))

        assertEquals(
            listOf("Перевод: прибывать", "Стиль: перен."),
            result.shown("llegar", "ruwiktionary/1.verb.1"),
        )
    }

    @Test
    fun `AWP-15_8 a second delete of the same lexeme removes nothing more`() {
        val window = reducer.window(Fx.ran, Fx.running)
        val first = reducer.step(window, Msg.DeleteLexeme("run", "ruwiktionary/1.verb.2"))

        val second = reducer.testReduce(first, Msg.DeleteLexeme("run", "ruwiktionary/1.verb.2"))

        assertEquals(first.cards, second.state().cards)
        assertEquals(
            listOf("ruwiktionary/1.verb.1", "enwiktionary/1.verb.37"),
            second.state().card("run").lexemes.map { it.key },
        )
        second.assertNoEffects()
    }
}
