package me.apomazkin.lexeme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Date

/**
 * Unit tests для extension `ComponentType.toRef()` (IS481 AGG-12).
 *
 * Stable identity mapping ComponentType (DB-id + meta) → ComponentTypeRef
 * (stable identity без id). Используется в reducer для membership check
 * restored ref в available.
 */
class ComponentTypeRefExtTest {

    private val now = Date(0L)

    private fun builtInType(name: String? = null) = ComponentType(
        id = ComponentTypeId(1L),
        systemKey = BuiltInComponent.TRANSLATION,
        dictionaryId = null,
        name = name,
        template = ComponentTemplate.TEXT,
        position = 0,
        createdAt = now,
        updatedAt = now,
    )

    private fun userDefinedType(name: String?) = ComponentType(
        id = ComponentTypeId(2L),
        systemKey = null,
        dictionaryId = 1L,
        name = name,
        template = ComponentTemplate.TEXT,
        position = 1,
        createdAt = now,
        updatedAt = now,
    )

    @Test
    fun `built-in systemKey only maps to BuiltIn ref`() {
        val type = builtInType(name = null)

        val ref = type.toRef()

        assertEquals(ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION), ref)
    }

    @Test
    fun `built-in with name override still maps to BuiltIn (name ignored)`() {
        val type = builtInType(name = "Перевод")

        val ref = type.toRef()

        assertEquals(ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION), ref)
    }

    @Test
    fun `user-defined maps to UserDefined with name`() {
        val type = userDefinedType(name = "Definition")

        val ref = type.toRef()

        assertEquals(ComponentTypeRef.UserDefined("Definition"), ref)
    }

    @Test
    fun `user-defined with unicode and special chars in name preserved`() {
        val type = userDefinedType(name = "hé:llo")

        val ref = type.toRef()

        assertEquals(ComponentTypeRef.UserDefined("hé:llo"), ref)
    }

    @Test
    fun `user-defined without name throws IllegalStateException`() {
        val type = userDefinedType(name = null)

        assertThrows(IllegalStateException::class.java) {
            type.toRef()
        }
    }
}
