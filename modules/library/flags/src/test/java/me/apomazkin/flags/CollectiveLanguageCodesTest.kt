package me.apomazkin.flags

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Кейсы:
 * 1. семья — cau, tut, sit, inc, ber, bh → собирательный
 * 2. семья с регионом — "sla-RU" → собирательный (основа до дефиса)
 * 3. настоящие языки — ru, es-MX, haw, ktu, ms (макроязык) → не собирательный
 */
class CollectiveLanguageCodesTest {

    @Test
    fun `families are collective`() {
        listOf("cau", "tut", "sit", "inc", "ber", "bh").forEach { tag ->
            assertTrue(tag, CollectiveLanguageCodes.isCollective(tag))
        }
    }

    @Test
    fun `region does not hide a family`() {
        assertTrue(CollectiveLanguageCodes.isCollective("sla-RU"))
    }

    @Test
    fun `real languages and macrolanguages are not collective`() {
        listOf("ru", "es-MX", "haw", "ktu", "ms", "sw").forEach { tag ->
            assertFalse(tag, CollectiveLanguageCodes.isCollective(tag))
        }
    }
}
