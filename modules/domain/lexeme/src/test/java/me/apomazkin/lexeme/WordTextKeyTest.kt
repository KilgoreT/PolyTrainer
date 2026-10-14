package me.apomazkin.lexeme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** AWP-07: «то же слово» — текст без регистра, пробелов по краям и с единой диакритикой. */
class WordTextKeyTest {

    @Test
    fun `AWP-07_1 case and surrounding spaces do not matter`() {
        assertEquals(wordTextKey("run"), wordTextKey("  Run "))
    }

    @Test
    fun `AWP-07_1 non-ASCII letters are lowered too`() {
        assertEquals("ñandú", wordTextKey("ÑANDÚ"))
        assertEquals("бег", wordTextKey("Бег"))
    }

    @Test
    fun `AWP-07_1 decomposed and composed diacritics give one key`() {
        assertEquals(wordTextKey("й"), wordTextKey("й"))
    }

    @Test
    fun `AWP-07_2 different words keep different keys`() {
        assertNotEquals(wordTextKey("run"), wordTextKey("running"))
    }
}
