package me.apomazkin.core_db_api.entity

import java.util.Date

data class DictionaryApiEntity(
    val id: Long,
    val numericCode: Int?,
    val name: String,
    val addDate: Date,
    val changeDate: Date? = null,
    val deleteDate: Date? = null,
    /**
     * IS525: изучаемый язык — код с региональным вариантом («es-MX»).
     * Обязателен; значение по умолчанию — только для тестовых конструкций,
     * из базы приходит всегда.
     */
    val learningLanguage: String = "en",
    /** IS525: язык перевода. Обязателен; значение по умолчанию — для тестов. */
    val translationLanguage: String = "en",
)
