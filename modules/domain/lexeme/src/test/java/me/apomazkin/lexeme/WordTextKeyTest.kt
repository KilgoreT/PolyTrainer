package me.apomazkin.lexeme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** AWP-07: «то же слово» — текст без регистра, пробелов по краям и с единой диакритикой. */
class WordTextKeyTest {

    @Test
    fun `AWP-39_2 case and surrounding spaces do not matter`() {
        assertEquals(wordTextKey("run"), wordTextKey("  Run "))
    }

    @Test
    fun `AWP-39_2 non-ASCII letters are lowered too`() {
        assertEquals("ñandú", wordTextKey("ÑANDÚ"))
        assertEquals("бег", wordTextKey("Бег"))
    }

    @Test
    fun `decomposed and composed diacritics give one key`() {
        assertEquals(wordTextKey("й"), wordTextKey("й"))
    }

    @Test
    fun `AWP-39_3 different words keep different keys`() {
        assertNotEquals(wordTextKey("run"), wordTextKey("running"))
    }
}
