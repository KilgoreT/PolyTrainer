package me.apomazkin.polytrainer.di.module.dictionary

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import me.apomazkin.flags.CountryLanguage
import me.apomazkin.flags.CountryProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/**
 * Кейсы:
 * 1. страна с языками → основной (первый) код
 * 2. страны нет (null) → en, провайдер не трогается
 * 3. у страны нет разобранных языков → en
 * 4. язык перевода — язык телефона без региона
 * 5. устаревший код локали (iw) → современный (he)
 */
class DictionaryLanguageRulesTest {

    private val countryProvider: CountryProvider = mockk()
    private var locale = Locale("ru", "RU")
    private val rules = DictionaryLanguageRules(
        countryProvider = lazyOf(countryProvider),
        deviceLocale = { locale },
    )

    @Test
    fun `country main language first`() {
        every { countryProvider.getCountryLanguages(484) } returns listOf(
            CountryLanguage("es-MX", "Spanish"),
            CountryLanguage("yua", "Yucateco"),
        )

        assertEquals("es-MX", rules.learningLanguageFor(484))
    }

    @Test
    fun `no flag gives english without touching provider`() {
        assertEquals("en", rules.learningLanguageFor(null))
        verify(exactly = 0) { countryProvider.getCountryLanguages(any()) }
    }

    @Test
    fun `country without parsed languages gives english`() {
        every { countryProvider.getCountryLanguages(10) } returns emptyList() // Антарктида

        assertEquals("en", rules.learningLanguageFor(10))
    }

    @Test
    fun `translation is device language without region`() {
        assertEquals("ru", rules.translationLanguage())
    }

    @Test
    fun `legacy locale code is modernised`() {
        locale = Locale("iw")

        assertEquals("he", rules.translationLanguage())
    }
}
