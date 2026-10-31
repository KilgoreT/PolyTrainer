package me.apomazkin.lexeme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** AWP-07: «та же лексема» — без регистра, краёв, формы Unicode и знаков ударения. */
class LexemeTextKeyTest {

    @Test
    fun `AWP-50_3 stress mark does not matter`() {
        assertEquals(lexemeTextKey("бежать"), lexemeTextKey("бежа́ть"))
        assertEquals(lexemeTextKey("бежать"), lexemeTextKey("бежа́ть"))
    }

    @Test
    fun `AWP-50_1 case and edges do not matter`() {
        assertEquals(lexemeTextKey("управлять"), lexemeTextKey("  Управлять "))
    }

    @Test
    fun `AWP-50_4 yo and short i stay themselves`() {
        assertNotEquals(lexemeTextKey("ёж"), lexemeTextKey("еж"))
        assertNotEquals(lexemeTextKey("бой"), lexemeTextKey("бои"))
        assertEquals(lexemeTextKey("й"), lexemeTextKey("й"))
    }

    @Test
    fun `AWP-50_5 english definitions compare the same way`() {
        assertEquals(lexemeTextKey("To move swiftly."), lexemeTextKey("to move swiftly. "))
    }
}
