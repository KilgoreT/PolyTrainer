package me.apomazkin.flags

import android.content.Context
import com.blongho.country_data.World

data class CountryInfo(
    val numericCode: Int,
    val name: String,
    val alpha2: String,
)

interface CountryProvider {
    fun getFlagRes(numericCode: Int): Int
    fun getAllCountries(): List<CountryInfo>
    fun getLanguagesForCountry(numericCode: Int): List<String>

    /**
     * IS525: языки страны, пригодные для словаря, в порядке библиотеки —
     * основной первым («es-MX», «en-US»). Отброшены строки без кода (Ф1) и
     * собирательные коды семей (Ф2, [CollectiveLanguageCodes]).
     */
    fun getCountryLanguages(numericCode: Int): List<CountryLanguage>

    /**
     * IS525: страны, пригодные для словаря — у которых после Ф1 и Ф2 есть
     * хотя бы один язык (Ф3). В библиотеке v1.5.4 это 247 стран из 250:
     * без языков Антарктида, Буве, Херд.
     */
    fun getDictionaryCountries(): List<CountryInfo>
}

class CountryProviderImpl(
    context: Context
) : CountryProvider {

    init {
        World.init(context)
    }

    override fun getFlagRes(numericCode: Int): Int {
        return World.getFlagOf(numericCode)
    }

    override fun getAllCountries(): List<CountryInfo> {
        return World.getAllCountries().map { CountryInfo(it.id, it.name, it.alpha2) }
    }

    override fun getLanguagesForCountry(numericCode: Int): List<String> {
        return World.getLanguagesFrom(numericCode)
    }

    override fun getCountryLanguages(numericCode: Int): List<CountryLanguage> {
        // Библиотека на неизвестный код падает с NPE (countryFrom → universe
        // == null в v1.5.4) — граница с библиотекой не должна ронять ни
        // форму, ни миграцию: неизвестная страна = страна без языков.
        val raw = runCatching { World.getLanguagesFrom(numericCode) }.getOrDefault(emptyList())
        return raw
            .mapNotNull(::parseCountryLanguage)
            .filterNot { CollectiveLanguageCodes.isCollective(it.tag) }
    }

    override fun getDictionaryCountries(): List<CountryInfo> {
        return getAllCountries().filter { getCountryLanguages(it.numericCode).isNotEmpty() }
    }
}