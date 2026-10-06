package me.apomazkin.flags

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Кейсы:
 * 1. без региона — "Russian (ru)" → ru / Russian
 * 2. с регионом — "Spanish (es-MX)" → es-MX / Spanish
 * 3. двойные скобки — "Occitan (post 1500) (oc)" → oc / "Occitan (post 1500)"
 * 4. трёхбуквенный код — "Hawaiian (haw)" → haw
 * 5. без скобок — "Klingon" → null
 * 6. пустые скобки — "()" → null (острова Буве и Херд в библиотеке)
 * 7. пустая строка — "" → null (Антарктида в библиотеке)
 */
class LanguageTagParserTest {

    @Test
    fun `plain language`() =
        assertEquals(CountryLanguage("ru", "Russian"), parseCountryLanguage("Russian (ru)"))

    @Test
    fun `regional variant kept`() =
        assertEquals(CountryLanguage("es-MX", "Spanish"), parseCountryLanguage("Spanish (es-MX)"))

    @Test
    fun `last parentheses win`() =
        assertEquals(
            CountryLanguage("oc", "Occitan (post 1500)"),
            parseCountryLanguage("Occitan (post 1500) (oc)"),
        )

    @Test
    fun `three letter code`() = assertEquals("haw", parseCountryLanguage("Hawaiian (haw)")?.tag)

    @Test
    fun `no parentheses`() = assertNull(parseCountryLanguage("Klingon"))

    @Test
    fun `empty parentheses`() = assertNull(parseCountryLanguage("()"))

    @Test
    fun `empty string`() = assertNull(parseCountryLanguage(""))
}
