package me.apomazkin.chippicker

import androidx.annotation.StringRes

enum class CategoryLabel(
    val stringValue: String,
    @StringRes override val valueRes: Int,
    @StringRes val shortValueRes: Int
) : ChipValue {
    NOUN("noun", R.string.lexeme_noun, R.string.lexeme_short_noun),
    VERB("verb", R.string.lexeme_verb, R.string.lexeme_short_verb),
    ADJ("adjective", R.string.lexeme_adj, R.string.lexeme_short_adj),
    ADV("adverb", R.string.lexeme_adv, R.string.lexeme_short_adv),
    PRON("pronoun", R.string.lexeme_pronoun, R.string.lexeme_short_pronoun),
    NUMERAL("numeral", R.string.lexeme_numeral, R.string.lexeme_short_numeral),
    PREP("preposition", R.string.lexeme_prep, R.string.lexeme_short_prep),
    CONJ("conjunction", R.string.lexeme_conj, R.string.lexeme_short_conj),
    PHRASE("phrase", R.string.lexeme_phrase, R.string.lexeme_short_phrase),
    OTHER("other", R.string.lexeme_other, R.string.lexeme_short_other),
    UNDEFINED("", R.string.empty_value, R.string.empty_value),
}

fun String.toCategoryLabel() = when (this) {
    CategoryLabel.NOUN.stringValue -> CategoryLabel.NOUN
    CategoryLabel.VERB.stringValue -> CategoryLabel.VERB
    CategoryLabel.ADJ.stringValue -> CategoryLabel.ADJ
    CategoryLabel.ADV.stringValue -> CategoryLabel.ADV
    CategoryLabel.PRON.stringValue -> CategoryLabel.PRON
    CategoryLabel.NUMERAL.stringValue -> CategoryLabel.NUMERAL
    CategoryLabel.PREP.stringValue -> CategoryLabel.PREP
    CategoryLabel.CONJ.stringValue -> CategoryLabel.CONJ
    CategoryLabel.PHRASE.stringValue -> CategoryLabel.PHRASE
    CategoryLabel.OTHER.stringValue -> CategoryLabel.OTHER
    else -> CategoryLabel.UNDEFINED
}

fun CategoryLabel.toChipPicker(): ChipPicker = when (this) {
    CategoryLabel.UNDEFINED -> ChipPicker.Off
    else -> ChipPicker.Selected(this)
}

val lexicalCategory = listOf(
    CategoryLabel.NOUN,
    CategoryLabel.VERB,
    CategoryLabel.ADJ,
    CategoryLabel.ADV,
    CategoryLabel.PRON,
    CategoryLabel.NUMERAL,
    CategoryLabel.PREP,
    CategoryLabel.CONJ,
    CategoryLabel.PHRASE,
    CategoryLabel.OTHER,
)