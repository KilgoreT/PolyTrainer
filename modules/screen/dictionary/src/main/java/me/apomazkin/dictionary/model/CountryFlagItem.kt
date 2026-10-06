package me.apomazkin.dictionary.model

data class CountryFlagItem(
    val numericCode: Int,
    val countryName: String,
    val localizedName: String = "",
    val flagRes: Int,
    /** Сырые строки библиотеки — для поиска флага по названию языка. */
    val languages: List<String> = listOf(),
    /** IS525: языки страны, основной первым — для подстановки по флагу. */
    val languageItems: List<LanguageItem> = listOf(),
)
