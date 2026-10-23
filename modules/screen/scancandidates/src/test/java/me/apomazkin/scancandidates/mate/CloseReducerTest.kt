package me.apomazkin.scancandidates.mate

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.NavigationEffect
import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.assertEffects
import io.github.kilgoret.mate.test.assertNoEffects
import io.github.kilgoret.mate.test.testReduce
import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.candidates.CardOwnGroupUi
import me.apomazkin.candidates.CardTargetUi
import me.apomazkin.core_resources.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Закрытие глазами редьюсера: сразу, если правок не было (AWP-19), с вопросом
 * после удалений, правок текста и смены опций (AWP-25), пока виден индикатор
 * (AWP-24). Во время записи крестик и «назад» не действуют на уровне UI
 * (AWP-19.5). Адреса строк спеки — `AWP-NN_k`.
 */
class CloseReducerTest {
    private val reducer = reducer()

    private val back = setOf<Effect>(NavigationEffect.Back)
    private val runKey = "ruwiktionary/1.verb.1"

    private fun dirty() = reducer.step(
        reducer.window(Fx.ran, Fx.bank),
        Msg.DeleteLexeme("run", "ruwiktionary/1.verb.2"),
    )

    // ===== Без вопроса (AWP-19) =====

    @Test
    fun `AWP-19_1 closing an untouched window leaves at once`() {
        val window = reducer.window(Fx.ran)

        val result = reducer.testReduce(window, Msg.RequestClose)

        assertEquals(window, result.state())
        result.assertEffects(back)
    }

    @Test
    fun `AWP-19_6 only the common group changed leaves without a question`() {
        val window = reducer.window(Fx.ran, form = Fx.form())
        val grouped = reducer.step(window, Msg.SetCommonGroup(Fx.POTTER))

        val result = reducer.testReduce(grouped, Msg.RequestClose)

        assertFalse(result.state().isConfirmingClose)
        result.assertEffects(back)
    }

    @Test
    fun `AWP-19_8 where to put and the own group of a card leave without a question`() {
        val window = reducer.window(Fx.bank, form = Fx.form(existing = listOf(Fx.bank1)))
        val target = reducer.step(window, Msg.SetCardTarget("bank", CardTargetUi.NewWord))
        val own = reducer.step(target, Msg.SetCardGroup("bank", CardOwnGroupUi.Group(Fx.FINANCE)))

        val result = reducer.testReduce(own, Msg.RequestClose)

        assertFalse(result.state().isConfirmingClose)
        result.assertEffects(back)
    }

    @Test
    fun `AWP-19_9 the same text, an unchanged reset and the same option are no change`() {
        val window = reducer.window(Fx.ran)
        val translation = Fx.ref("run", runKey, Fx.TRANSLATION)
        val partOfSpeech = Fx.componentRef("run", runKey, Fx.PART_OF_SPEECH)

        val saved = reducer.step(window, Msg.SubmitText(translation, "  бежать, бегать "))
        val reset = reducer.step(saved, Msg.ResetText(translation))
        val same = reducer.step(reset, Msg.SetOption(partOfSpeech, Fx.VERB))
        val result = reducer.testReduce(same, Msg.RequestClose)

        assertFalse(saved.isDirty)
        assertFalse(reset.isDirty)
        assertFalse(same.isDirty)
        result.assertEffects(back)
    }

    @Test
    fun `AWP-19_10 expanding sections and cards is no change`() {
        val window = reducer.window(Fx.ran, Fx.notFound("qwrt"))
        val touched = listOf(Msg.ToggleComponentMatches, Msg.ToggleNotAdded)
            .fold(window) { state, message -> reducer.step(state, message) }

        val result = reducer.testReduce(touched, Msg.RequestClose)

        assertFalse(touched.isDirty)
        result.assertEffects(back)
    }

    // ===== Пока виден индикатор (AWP-24) =====

    @Test
    fun `AWP-24_3 closing while the indicator is visible leaves at once without a question`() {
        val states = listOf(fresh(), reducer.recognized(), reducer.answered(Fx.ran))

        states.forEach { state ->
            val result = reducer.testReduce(state, Msg.RequestClose)

            assertEquals(state, result.state())
            result.assertEffects(back)
        }
    }

    @Test
    fun `closing from no words or from an error leaves at once`() {
        val noWords = reducer.step(fresh(), Msg.TextEmpty)
        val failure = Msg.RecognitionFailed(R.string.photo_words_error_recognition)
        val error = reducer.step(fresh(), failure)

        reducer.testReduce(noWords, Msg.RequestClose).assertEffects(back)
        reducer.testReduce(error, Msg.RequestClose).assertEffects(back)
    }

    // ===== Вопрос при закрытии (AWP-25) =====

    @Test
    fun `AWP-25_1 closing after a deletion asks the question and writes nothing`() {
        val result = reducer.testReduce(dirty(), Msg.RequestClose)

        assertTrue(result.state().isConfirmingClose)
        result.assertNoEffects()
    }

    @Test
    fun `AWP-25_2 stay closes the question and keeps the window as it was`() {
        val asking = reducer.step(dirty(), Msg.RequestClose)

        val result = reducer.testReduce(asking, Msg.Stay)

        assertFalse(result.state().isConfirmingClose)
        assertEquals(asking.cards, result.state().cards)
        assertTrue(result.state().isDirty)
        result.assertNoEffects()
    }

    @Test
    fun `AWP-25_3 close in the question leaves without a write`() {
        val asking = reducer.step(dirty(), Msg.RequestClose)

        val result = reducer.testReduce(asking, Msg.ConfirmClose)

        result.assertEffects(back)
    }

    @Test
    fun `AWP-25_4 a changed option asks the question on closing`() {
        val window = reducer.window(Fx.ran)
        val partOfSpeech = Fx.componentRef("run", runKey, Fx.PART_OF_SPEECH)
        val changed = reducer.step(window, Msg.SetOption(partOfSpeech, Fx.NOUN))

        val result = reducer.testReduce(changed, Msg.RequestClose)

        assertTrue(result.state().isConfirmingClose)
        result.assertNoEffects()
    }

    @Test
    fun `AWP-25_1 a changed text asks the question on closing`() {
        val window = reducer.window(Fx.ran)
        val translation = Fx.ref("run", runKey, Fx.TRANSLATION)
        val edited = reducer.step(window, Msg.SubmitText(translation, "брать"))

        val result = reducer.testReduce(edited, Msg.RequestClose)

        assertTrue(result.state().isConfirmingClose)
        result.assertNoEffects()
    }

    // ===== После отказа записи (AWP-26) =====

    @Test
    fun `AWP-26_2 closing from the failed state asks when something was deleted`() {
        val adding = reducer.step(reducer.step(dirty(), Msg.RequestAdd), Msg.ConfirmAdd)
        val failed = reducer.step(adding, Msg.AddFailed(AddFailureUi.Failed))

        val result = reducer.testReduce(failed, Msg.RequestClose)

        assertTrue(result.state().isConfirmingClose)
        result.assertNoEffects()
    }

    @Test
    fun `closing from the failed state of an untouched window leaves at once`() {
        val asking = reducer.step(reducer.window(Fx.ran), Msg.RequestAdd)
        val adding = reducer.step(asking, Msg.ConfirmAdd)
        val failed = reducer.step(adding, Msg.AddFailed(AddFailureUi.Failed))

        val result = reducer.testReduce(failed, Msg.RequestClose)

        result.assertEffects(back)
    }
}
