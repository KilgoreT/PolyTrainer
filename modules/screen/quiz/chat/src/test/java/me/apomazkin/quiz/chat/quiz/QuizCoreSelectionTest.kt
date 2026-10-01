package me.apomazkin.quiz.chat.quiz

import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/** Инвариант «выбор ∩ кандидаты, пусто → все» — одно место, один тест. */
class QuizCoreSelectionTest {

    private val translation = ComponentType(
        id = ComponentTypeId(1L),
        systemKey = BuiltInComponent.TRANSLATION,
        dictionaryId = null,
        name = null,
        template = ComponentTemplate.TEXT,
        position = 0,
        core = true,
        createdAt = Date(0L),
        updatedAt = Date(0L),
    )

    private val definition = ComponentType(
        id = ComponentTypeId(2L),
        systemKey = null,
        dictionaryId = 1L,
        name = "Definition",
        template = ComponentTemplate.TEXT,
        position = 1,
        core = true,
        createdAt = Date(0L),
        updatedAt = Date(0L),
    )

    private val candidates = listOf(translation, definition)

    @Test
    fun `intersection keeps only selected candidates in candidate order`() {
        val selected = setOf(ComponentTypeRef.UserDefined("Definition"))

        assertEquals(listOf(definition), resolveQuizCores(candidates, selected))
    }

    @Test
    fun `empty selection - all candidates`() {
        assertEquals(candidates, resolveQuizCores(candidates, emptySet()))
    }

    @Test
    fun `stale selection - all candidates`() {
        val selected = setOf(ComponentTypeRef.UserDefined("Removed"))

        assertEquals(candidates, resolveQuizCores(candidates, selected))
    }

    @Test
    fun `stale ref alongside valid - only valid`() {
        val selected = setOf(
            ComponentTypeRef.UserDefined("Removed"),
            ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION),
        )

        assertEquals(listOf(translation), resolveQuizCores(candidates, selected))
    }

    @Test
    fun `no candidates - empty`() {
        val selected = setOf(ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION))

        assertTrue(resolveQuizCores(emptyList(), selected).isEmpty())
    }
}
