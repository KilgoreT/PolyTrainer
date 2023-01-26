package me.apomazkin.polytrainer.ui.screen.langSelection

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.polytrainer.R

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
fun Country.toStringName() = when (this) {
    Country.FRENCH -> stringResource(id = R.string.lang_french)
    Country.GERMAN -> stringResource(id = R.string.lang_german)
    Country.SPANISH -> stringResource(id = R.string.lang_spanish)
    Country.ENGLISH -> stringResource(id = R.string.lang_english)
    Country.ITALIAN -> stringResource(id = R.string.lang_italian)
    Country.PORTUGUESE -> stringResource(id = R.string.lang_portuguese)
}