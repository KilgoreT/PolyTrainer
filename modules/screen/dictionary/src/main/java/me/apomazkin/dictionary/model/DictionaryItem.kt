package me.apomazkin.dictionary.model

data class DictionaryItem(
    val id: Long,
    val name: String,
    val numericCode: Int?,
    val learningLanguage: LanguageItem,
    val translationLanguage: LanguageItem,
)
