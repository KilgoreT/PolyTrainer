package me.apomazkin.lexeme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * IS486 фаза 1, зона A1: шаблон CHOICE.
 * phase1_plan.md § Зона A.
 */
class ChoiceTemplateTest {

    @Test
    fun `fromKey choice maps to CHOICE`() {
        assertEquals(ComponentTemplate.CHOICE, ComponentTemplate.fromKey("choice"))
    }

    @Test
    fun `CHOICE fields is empty - value expressed via option ref`() {
        assertTrue(ComponentTemplate.CHOICE.fields.isEmpty())
    }

    @Test
    fun `TEXT fields unchanged`() {
        assertEquals(
            listOf(Field("value", PrimitiveType.TEXT)),
            ComponentTemplate.TEXT.fields,
        )
    }

    @Test
    fun `IMAGE fields unchanged`() {
        assertEquals(
            listOf(Field("value", PrimitiveType.IMAGE)),
            ComponentTemplate.IMAGE.fields,
        )
    }

    @Test
    fun `fromKey unknown still fail-soft null`() {
        assertEquals(null, ComponentTemplate.fromKey("unknown_key"))
    }
}
