package me.apomazkin.core_db_api.entity

/**
 * IS525: языки словаря по умолчанию — для миграции 15→16. Данные (языки
 * стран, язык телефона) живут в app; слой базы получает их этим
 * интерфейсом тем же путём, что [ReservedGroupNames]. Оба метода всегда
 * возвращают код языка.
 */
interface DictionaryLanguageDefaults {

    /** Изучаемый язык для страны флага; [numericCode] == null — словарь без флага. */
    fun learningLanguageFor(numericCode: Int?): String

    /** Язык перевода. */
    fun translationLanguage(): String
}
