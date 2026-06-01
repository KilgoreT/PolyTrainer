package me.apomazkin.dictionarytab.entity

import me.apomazkin.lexeme.Definition
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.lexeme.LexemeId
import me.apomazkin.lexeme.Translation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

/**
 * Unit tests for [Lexeme.toUiItem] mapper (domain → UI).
 *
 * Per business_design_tree.md § Узел 3:
 * - id берётся как lexemeId.id (распаковка LexemeId).
 * - translation/definition оборачиваются в Ui-обёртки (nullable propagation).
 * - addDate / changeDate проходят насквозь.
 *
 * TDD: extension создаётся в business_implement — тесты могут не компилироваться до того момента.
 */
class LexemeUiItemTest {

    private val addDate = Date(1_700_000_000_000L)
    private val changeDate = Date(1_700_000_100_000L)

    private fun makeDomain(
        lexemeId: LexemeId = LexemeId(42L),
        translation: Translation? = null,
        definition: Definition? = null,
        addDate: Date = this.addDate,
        changeDate: Date? = this.changeDate,
    ) = Lexeme(
        lexemeId = lexemeId,
        translation = translation,
        definition = definition,
        addDate = addDate,
        changeDate = changeDate,
    )

    @Test
    fun `given null translation when toUiItem then ui translation is null`() {
        val domain = makeDomain(translation = null)

        val ui = domain.toUiItem()

        assertNull("translation must be null", ui.translation)
    }

    @Test
    fun `given non-null translation when toUiItem then wraps into TranslationUiEntity`() {
        val domain = makeDomain(translation = Translation("hola"))

        val ui = domain.toUiItem()

        assertEquals(TranslationUiEntity("hola"), ui.translation)
    }

    @Test
    fun `given null definition when toUiItem then ui definition is null`() {
        val domain = makeDomain(definition = null)

        val ui = domain.toUiItem()

        assertNull("definition must be null", ui.definition)
    }

    @Test
    fun `given non-null definition when toUiItem then wraps into DefinitionUiEntity`() {
        val domain = makeDomain(definition = Definition("greeting"))

        val ui = domain.toUiItem()

        assertEquals(DefinitionUiEntity("greeting"), ui.definition)
    }

    @Test
    fun `given LexemeId when toUiItem then id is unwrapped to raw Long`() {
        val domain = makeDomain(lexemeId = LexemeId(123L))

        val ui = domain.toUiItem()

        assertEquals(123L, ui.id)
    }

    @Test
    fun `given addDate when toUiItem then addDate is passed through unchanged`() {
        val expected = Date(1_234_567_890_000L)
        val domain = makeDomain(addDate = expected)

        val ui = domain.toUiItem()

        assertEquals(expected, ui.addDate)
    }

    @Test
    fun `given non-null changeDate when toUiItem then changeDate is passed through unchanged`() {
        val expected = Date(1_234_999_999_000L)
        val domain = makeDomain(changeDate = expected)

        val ui = domain.toUiItem()

        assertEquals(expected, ui.changeDate)
    }

    @Test
    fun `given null changeDate when toUiItem then changeDate is null`() {
        val domain = makeDomain(changeDate = null)

        val ui = domain.toUiItem()

        assertNull("changeDate must be null", ui.changeDate)
    }

    @Test
    fun `given full domain Lexeme when toUiItem then all mapped fields match`() {
        val domain = Lexeme(
            lexemeId = LexemeId(1L),
            translation = Translation("hi"),
            definition = Definition("a salutation"),
            addDate = addDate,
            changeDate = changeDate,
        )

        val ui = domain.toUiItem()

        assertEquals(1L, ui.id)
        assertEquals(TranslationUiEntity("hi"), ui.translation)
        assertEquals(DefinitionUiEntity("a salutation"), ui.definition)
        assertEquals(addDate, ui.addDate)
        assertEquals(changeDate, ui.changeDate)
    }
}
