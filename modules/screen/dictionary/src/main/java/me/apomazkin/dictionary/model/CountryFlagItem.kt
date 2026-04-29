package me.apomazkin.dictionary.model

data class CountryFlagItem(
    val numericCode: Int,
    val countryName: String,
    val localizedName: String = "",
    val flagRes: Int,
    val languages: List<String> = listOf(),
)
