package me.apomazkin.polytrainer.di.module.dictionary

import me.apomazkin.core_db_api.entity.DictionaryLanguageDefaults
import me.apomazkin.flags.CountryProvider
import java.util.Locale

/**
 * IS525: языки словаря по умолчанию — одно правило для миграции 15→16
 * ([DictionaryLanguageDefaults]) и для формы словаря:
 *  - изучаемый: основной язык страны флага; флага нет или у страны нет
 *    разобранного языка — английский;
 *  - перевод: язык телефона без региона («ru-RU» → «ru»).
 *
 * [countryProvider] ленивый: библиотека флагов поднимается, только когда
 * правило действительно спросили про страну (миграция идёт один раз).
 */
class DictionaryLanguageRules(
    private val countryProvider: Lazy<CountryProvider>,
    private val deviceLocale: () -> Locale = Locale::getDefault,
) : DictionaryLanguageDefaults {

    override fun learningLanguageFor(numericCode: Int?): String {
        if (numericCode == null) return NO_FLAG_LANGUAGE
        return countryProvider.value
            .getCountryLanguages(numericCode)
            .firstOrNull()
            ?.tag
            ?: NO_FLAG_LANGUAGE
    }

    override fun translationLanguage(): String {
        val deviceTag = deviceLocale().toLanguageTag()
        val language = deviceTag.substringBefore('-')
        return if (language.isEmpty() || language == "und") NO_FLAG_LANGUAGE else language
    }

    companion object {
        /** Язык словаря без флага (решение юзера, IS525). */
        const val NO_FLAG_LANGUAGE = "en"
    }
}
