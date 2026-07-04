package me.apomazkin.wordcard.mate.ext

import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.lexeme.ImageValues
import me.apomazkin.lexeme.Primitive
import me.apomazkin.wordcard.mate.ComponentValueKey
import me.apomazkin.wordcard.mate.ctype
import me.apomazkin.wordcard.mate.domainCv
import me.apomazkin.wordcard.mate.domainLexeme
import me.apomazkin.wordcard.mate.toComponentValueState
import me.apomazkin.wordcard.mate.toLexemeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §2.1 Mappers: Lexeme.toLexemeState / ComponentValue.toComponentValueState.
 */
class MappersTest {

    @Test
    fun `toLexemeState maps id and components`() {
        val lex = domainLexeme(8L, listOf(domainCv(5L, 8L, "a"), domainCv(6L, 8L, "b")))
        val state = lex.toLexemeState()
        assertEquals(8L, state.id)
        assertEquals(2, state.components.size)
        assertTrue(state.components.all { it.key is ComponentValueKey.Saved })
    }

    @Test
    fun `toComponentValueState maps fields and origin from data`() {
        val cv = domainCv(5L, 1L, "hi", typeId = 50L, ref = ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION), isMultiple = true)
        val state = cv.toComponentValueState()
        assertEquals(ComponentValueKey.Saved(ComponentValueId(5L)), state.key)
        assertEquals(ComponentTypeId(50L), state.componentTypeId)
        assertEquals(ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION), state.componentTypeRef)
        assertTrue(state.isMultiple)
        assertEquals("hi", state.origin)
        assertEquals("", state.edited)
        assertFalse(state.isEdit)
    }

    @Test
    fun `G1_origin_empty_when_data_not_text`() {
        val type = ctype(50L, ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION))
        val cv = me.apomazkin.lexeme.ComponentValue(
            id = ComponentValueId(5L),
            lexemeId = me.apomazkin.lexeme.LexemeId(1L),
            type = type,
            data = ImageValues(Primitive.Image("uri://x")),
        )
        assertEquals("", cv.toComponentValueState().origin)
    }

    @Test
    fun `toComponentValueState user-defined ref by name`() {
        val cv = domainCv(5L, 1L, "e", typeId = 51L, ref = ComponentTypeRef.UserDefined("Example"))
        assertEquals(ComponentTypeRef.UserDefined("Example"), cv.toComponentValueState().componentTypeRef)
    }

    @Test
    fun `toLexemeState preserves order`() {
        val lex = domainLexeme(8L, listOf(domainCv(5L, 8L, "a"), domainCv(6L, 8L, "b"), domainCv(7L, 8L, "c")))
        val ids = lex.toLexemeState().components.mapNotNull { it.componentValueId?.id }
        assertEquals(listOf(5L, 6L, 7L), ids)
    }
}
