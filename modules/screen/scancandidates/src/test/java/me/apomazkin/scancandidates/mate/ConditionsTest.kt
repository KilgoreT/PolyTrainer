package me.apomazkin.scancandidates.mate

import me.apomazkin.candidates.CandidatesView
import me.apomazkin.candidates.CardOwnGroupUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Условия редьюсера на стейте — инварианты флага правок (AWP-19.9) — и вид
 * для общих виджетов. Чистые функции, редьюсер не нужен.
 */
class ConditionsTest {
    private val reducer = reducer()

    private val runKey = "ruwiktionary/1.verb.1"
    private val translation = Fx.ref("run", runKey, Fx.TRANSLATION)
    private val partOfSpeech = Fx.componentRef("run", runKey, Fx.PART_OF_SPEECH)

    // ===== textChanged =====

    @Test
    fun `AWP-19_9 textChanged compares the input with cut edges to the current text`() {
        val window = reducer.window(Fx.ran)

        assertTrue(window.textChanged(translation, "брать"))
        assertFalse(window.textChanged(translation, "бежать, бегать"))
        assertFalse(window.textChanged(translation, "  бежать, бегать  "))
        assertTrue(window.textChanged(translation, ""))
    }

    @Test
    fun `textChanged is false for a value that is not there`() {
        val window = reducer.window(Fx.ran)

        assertFalse(window.textChanged(Fx.ref("run", "no/such", Fx.TRANSLATION), "брать"))
        assertFalse(fresh().textChanged(translation, "брать"))
    }

    // ===== textEdited =====

    @Test
    fun `AWP-19_9 textEdited is true only for a text that differs from the answer`() {
        val window = reducer.window(Fx.ran)
        val edited = reducer.step(window, Msg.SubmitText(translation, "брать"))

        assertFalse(window.textEdited(translation))
        assertTrue(edited.textEdited(translation))
        assertFalse(window.textEdited(Fx.ref("run", "no/such", Fx.TRANSLATION)))
    }

    // ===== optionChanged =====

    @Test
    fun `AWP-19_9 optionChanged is true only when the choice would differ`() {
        val window = reducer.window(Fx.ran)

        assertFalse(window.optionChanged(partOfSpeech, Fx.VERB))
        assertTrue(window.optionChanged(partOfSpeech, Fx.NOUN))
        assertTrue(window.optionChanged(partOfSpeech, null))
    }

    @Test
    fun `optionChanged of a multiple choice is true for any option and for clearing`() {
        val window = reducer.window(Fx.llegar, form = Fx.formWithStyle())
        val style = Fx.componentRef("llegar", runKey, Fx.STYLE)

        assertTrue(window.optionChanged(style, Fx.FIGURATIVE))
        assertTrue(window.optionChanged(style, null))
    }

    @Test
    fun `optionChanged is false for a value that is not there and without a form`() {
        val window = reducer.window(Fx.ran)

        val missingLexeme = Fx.componentRef("run", "no/such", Fx.PART_OF_SPEECH)
        val missingValue = Fx.componentRef("run", runKey, Fx.GENDER)
        assertFalse(window.optionChanged(missingLexeme, Fx.NOUN))
        assertFalse(window.optionChanged(missingValue, Fx.MASCULINE))
        assertFalse(fresh().optionChanged(partOfSpeech, Fx.NOUN))
    }

    // ===== toView =====

    @Test
    fun `toView carries everything the shared widgets draw and leaves the dialog fields default`() {
        val opened = reducer.window(Fx.ran, Fx.notFound("qwrt"))
        val window = reducer.step(
            reducer.step(opened, Msg.SetCommonGroup(Fx.POTTER)),
            Msg.SetCardGroup("run", CardOwnGroupUi.All),
        )
        val state = reducer.step(window, Msg.ToggleComponentMatches).copy(
            isAdding = true,
            isConfirmingAdd = true,
            isConfirmingClose = true,
        )

        val view = state.toView()

        assertEquals(
            CandidatesView(
                form = state.form,
                cards = state.cards,
                summary = state.summary,
                sheet = state.sheet,
                isAdding = true,
                addFailure = null,
                isConfirmingClose = true,
                remarks = state.remarks,
                componentMatchLines = state.componentMatchLines,
                isComponentMatchesExpanded = true,
                notAdded = state.notAdded,
                isNotAddedExpanded = false,
                commonGroupId = Fx.POTTER,
                isConfirmingAdd = true,
            ),
            view,
        )
        assertEquals(null, view.lemmaWordChoice)
        assertTrue(view.sameValues.isEmpty())
    }
}
