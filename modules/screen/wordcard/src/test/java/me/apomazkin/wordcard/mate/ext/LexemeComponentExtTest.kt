package me.apomazkin.wordcard.mate.ext

import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.wordcard.mate.ComponentValueKey
import me.apomazkin.wordcard.mate.LexemeState
import me.apomazkin.wordcard.mate.appendPristine
import me.apomazkin.wordcard.mate.findByKey
import me.apomazkin.wordcard.mate.pristineCv
import me.apomazkin.wordcard.mate.removeComponent
import me.apomazkin.wordcard.mate.savedCv
import me.apomazkin.wordcard.mate.updateComponent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §2.2 LexemeState component-extensions: findByKey/updateComponent/removeComponent/appendPristine.
 */
class LexemeComponentExtTest {

    private fun savedK(id: Long) = ComponentValueKey.Saved(ComponentValueId(id))
    private fun pK(k: Long) = ComponentValueKey.Pristine(k)

    @Test
    fun `findByKey found by pristine and saved, null for absent`() {
        val l = LexemeState(1L, listOf(pristineCv(1L), savedCv(5L)))
        assertNotNull(l.findByKey(pK(1L)))
        assertNotNull(l.findByKey(savedK(5L)))
        assertNull(l.findByKey(savedK(999L)))
    }

    @Test
    fun `findByKey pristine and saved with same number do not collide`() {
        val l = LexemeState(1L, listOf(pristineCv(1L), savedCv(1L)))
        assertTrue(l.findByKey(pK(1L))!!.isPristine)
        assertEquals(savedK(1L), l.findByKey(savedK(1L))!!.key)
    }

    @Test
    fun `updateComponent applies transform to matched only`() {
        val l = LexemeState(1L, listOf(savedCv(5L, origin = "a"), savedCv(6L, origin = "b")))
        val updated = l.updateComponent(savedK(5L)) { it.copy(origin = "z") }
        assertEquals("z", updated.findByKey(savedK(5L))!!.origin)
        assertEquals("b", updated.findByKey(savedK(6L))!!.origin)
        assertEquals(1L, updated.id)
    }

    @Test
    fun `updateComponent unknown key is no-op`() {
        val l = LexemeState(1L, listOf(savedCv(5L)))
        assertEquals(l, l.updateComponent(savedK(999L)) { it.copy(origin = "z") })
    }

    @Test
    fun `removeComponent removes matched keeps others`() {
        val l = LexemeState(1L, listOf(savedCv(5L), savedCv(6L)))
        val r = l.removeComponent(savedK(5L))
        assertEquals(1, r.components.size)
        assertEquals(savedK(6L), r.components.single().key)
    }

    @Test
    fun `removeComponent last yields empty without auto-removing lexeme`() {
        val l = LexemeState(1L, listOf(savedCv(5L)))
        val r = l.removeComponent(savedK(5L))
        assertTrue(r.components.isEmpty())
        assertEquals(1L, r.id)
    }

    @Test
    fun `appendPristine adds to tail keeps existing`() {
        val l = LexemeState(1L, listOf(savedCv(5L)))
        val r = l.appendPristine(pristineCv(10L, edited = "x"))
        assertEquals(2, r.components.size)
        assertEquals(pK(10L), r.components.last().key)
    }
}
