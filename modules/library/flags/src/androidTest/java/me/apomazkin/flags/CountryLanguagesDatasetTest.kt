package me.apomazkin.flags

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * IS525: контракт данных библиотеки country-data — по ВСЕМ странам.
 *
 * Снимок 2026-10-05 (v1.5.4-alpha-1): 250 стран, 732 строки языков,
 * 729 разбираются; не разбираются ровно 3 пустые строки — Антарктида (AQ),
 * Буве (BV), Херд (HM). Собирательных кодов 6 в 3 странах, ни один не
 * основной. Коды — `xx` или `xxx`, необязательный регион из двух заглавных
 * букв. Тест ловит обновление библиотеки, которое сломает эти допущения.
 */
@RunWith(AndroidJUnit4::class)
class CountryLanguagesDatasetTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val provider = CountryProviderImpl(context)

    private val tagShape = Regex("^[a-z]{2,3}(-[A-Z]{2})?$")

    @Test
    fun everyNonEmptyLanguageStringParses_andTagsAreWellFormed() {
        val unparsed = mutableListOf<String>()
        val badTags = mutableListOf<String>()
        provider.getAllCountries().forEach { country ->
            provider.getLanguagesForCountry(country.numericCode).forEach { raw ->
                val parsed = parseCountryLanguage(raw)
                if (parsed == null) {
                    if (raw.isNotBlank() && raw != "()") unparsed += "${country.alpha2}: $raw"
                } else if (!tagShape.matches(parsed.tag)) {
                    badTags += "${country.alpha2}: $raw"
                }
            }
        }
        assertEquals("непустые строки без кода: $unparsed", emptyList<String>(), unparsed)
        assertEquals("коды неожиданной формы: $badTags", emptyList<String>(), badTags)
    }

    @Test
    fun countryLanguages_containNoCollectiveCodes() {
        val collective = provider.getAllCountries().flatMap { country ->
            provider.getCountryLanguages(country.numericCode)
                .filter { CollectiveLanguageCodes.isCollective(it.tag) }
                .map { "${country.alpha2}: ${it.tag}" }
        }

        assertEquals(emptyList<String>(), collective)
    }

    @Test
    fun dictionaryCountries_areAllButUninhabited() {
        val all = provider.getAllCountries()
            .map { it.alpha2 }
            .toSet()
        val forDictionary = provider.getDictionaryCountries()
            .map { it.alpha2 }
            .toSet()

        assertEquals(247, forDictionary.size)
        assertEquals(setOf("AQ", "BV", "HM"), all - forDictionary)
    }

    @Test
    fun mexicoMainLanguage_isMexicanSpanish() {
        val languages = provider.getCountryLanguages(484)

        assertTrue(languages.isNotEmpty())
        assertEquals("es-MX", languages.first().tag)
    }

    /** Неизвестный библиотеке код (в v1.5.4 `World.getLanguagesFrom` на него бросает NPE). */
    @Test
    fun unknownCountryCode_givesNoLanguages_withoutThrowing() {
        assertEquals(emptyList<CountryLanguage>(), provider.getCountryLanguages(999_999))
    }
}
