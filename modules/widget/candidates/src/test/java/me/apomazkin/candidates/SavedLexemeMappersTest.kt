package me.apomazkin.candidates

import me.apomazkin.candidates.mapper.toUi
import me.apomazkin.lexeme.BatchComponent
import me.apomazkin.lexeme.CaptionedTextValues
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.ExistingLemma
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.SavedLexeme
import me.apomazkin.lexeme.TextValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Сохранённые лексемы леммы словаря → значения по компонентам (AWP-46, AWP-18.10). */
class SavedLexemeMappersTest {

    private fun text(value: String) = TextValues(Primitive.Text(value))

    @Test
    fun `AWP-46_1 the values of a saved lexeme go one per component in the order of first appearance`() {
        val saved = SavedLexeme(
            id = 9L,
            components = listOf(
                BatchComponent(typeId = 3L, data = CaptionedTextValues(Primitive.Text("Run!"), Primitive.Text("Wiktionary"))),
                BatchComponent(typeId = 1L, data = text("бежать")),
                BatchComponent(typeId = 2L, data = ChoiceValues(21L)),
                BatchComponent(typeId = 3L, data = CaptionedTextValues(Primitive.Text("Go!"), caption = null)),
            ),
        )

        val ui = saved.toUi()

        assertEquals(9L, ui.id)
        assertEquals(
            listOf<CandidateValueUiItem>(
                CandidateValueUiItem.Text(3L, listOf(EditableTextUiItem.of("Run!"), EditableTextUiItem.of("Go!")), field = null),
                CandidateValueUiItem.Text(1L, listOf(EditableTextUiItem.of("бежать")), field = null),
                CandidateValueUiItem.Choice(2L, listOf(21L)),
            ),
            ui.values,
        )
    }

    @Test
    fun `AWP-46_1 saved values of the chosen lemma come to the card in the card order`() {
        val saved = SavedLexeme(
            id = 9L,
            components = listOf(
                BatchComponent(typeId = Fx.EXAMPLE, data = CaptionedTextValues(Primitive.Text("perro callejero"), null)),
                BatchComponent(typeId = Fx.GLOSS, data = text("dog")),
                BatchComponent(typeId = Fx.TRANSLATION, data = text("собака")),
            ),
        )
        val perro = Fx.existing(Fx.BANK_1, "perro", listOf("собака"), lexemes = listOf(saved.toUi()))
        val form = Fx.form(existing = listOf(perro))

        val values = form.savedLexemesOf(CardTargetUi.Existing(Fx.BANK_1)).single().values

        assertEquals(listOf(Fx.TRANSLATION, Fx.GLOSS, Fx.EXAMPLE), values.map { it.componentId })
    }

    @Test
    fun `AWP-46_4 AWP-18_10 a lemma without lexemes maps with an empty list and keeps its add date`() {
        val lemma = ExistingLemma(
            wordId = 5L,
            text = "run",
            lexemeCount = 0,
            coreValues = emptyList(),
            lexemeTextKeys = emptySet(),
            groupNames = emptyList(),
            addedAt = 1_760_000_000_000L,
            lexemes = emptyList(),
        )

        val ui = lemma.toUi()

        assertTrue(ui.lexemes.isEmpty())
        assertEquals(1_760_000_000_000L, ui.addedAt)
    }
}
