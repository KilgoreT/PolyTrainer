package me.apomazkin.candidates

import me.apomazkin.candidates.ui.optionLabelRes
import me.apomazkin.candidates.ui.valuesPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Чистые части подписей окна: ресурсы встроенных опций и первые значения слова. */
class LabelsTest {

    private val builtIn = listOf(
        "noun",
        "verb",
        "adjective",
        "adverb",
        "pronoun",
        "numeral",
        "preposition",
        "interjection",
        "phrasal_verb",
        "collocation",
        "idiom",
        "phrase",
    )

    @Test
    fun `AWP-17_4 every one of the twelve built-in options has its own label`() {
        val resources = builtIn.map { optionLabelRes(it) }

        assertEquals(12, resources.size)
        resources.forEach { assertNotNull(it) }
        assertEquals(12, resources.toSet().size)
    }

    @Test
    fun `AWP-17_4 a key that is not built in has no resource and shows as it is`() {
        assertNull(optionLabelRes("ergative"))
        assertNull(optionLabelRes(null))
    }

    @Test
    fun `WCL-15_1 a word without values has no preview`() {
        assertNull(valuesPreview(emptyList()))
    }

    @Test
    fun `WCL-15_1 up to three values are shown in full`() {
        assertEquals("бегать, бежать", valuesPreview(listOf("бегать", "бежать")))
        assertEquals("бегать, бежать, течь", valuesPreview(listOf("бегать", "бежать", "течь")))
    }

    @Test
    fun `WCL-15_1 more than three values are cut to the first three with an ellipsis`() {
        assertEquals(
            "бегать, бежать, течь…",
            valuesPreview(listOf("бегать", "бежать", "течь", "литься")),
        )
    }
}
