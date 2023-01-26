package me.apomazkin.langpicker

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

object LanguageData {
    val langList = listOf(
        Country.ENGLISH,
        Country.SPANISH,
        Country.FRENCH,
        Country.GERMAN,
        Country.ITALIAN,
        Country.PORTUGUESE,
    )
}

enum class Country(val numericCode: Int) {
    FRENCH(250),
    GERMAN(276),
    SPANISH(724),
    ENGLISH(826),
    ITALIAN(380),
    PORTUGUESE(620),
}

@Composable
fun Int.toStringName() = when (this) {
    Country.FRENCH.numericCode -> stringResource(id = R.string.lang_french)
    Country.GERMAN.numericCode -> stringResource(id = R.string.lang_german)
    Country.SPANISH.numericCode -> stringResource(id = R.string.lang_spanish)
    Country.ENGLISH.numericCode -> stringResource(id = R.string.lang_english)
    Country.ITALIAN.numericCode -> stringResource(id = R.string.lang_italian)
    Country.PORTUGUESE.numericCode -> stringResource(id = R.string.lang_portuguese)
    else -> throw IllegalArgumentException("Unknown country numeric code.")
}

@StringRes
fun Int.toLangNameRes() = when (this) {
    Country.FRENCH.numericCode -> R.string.lang_french
    Country.GERMAN.numericCode -> R.string.lang_german
    Country.SPANISH.numericCode -> R.string.lang_spanish
    Country.ENGLISH.numericCode -> R.string.lang_english
    Country.ITALIAN.numericCode -> R.string.lang_italian
    Country.PORTUGUESE.numericCode -> R.string.lang_portuguese
    else -> throw IllegalArgumentException("Unknown country numeric code.")
}