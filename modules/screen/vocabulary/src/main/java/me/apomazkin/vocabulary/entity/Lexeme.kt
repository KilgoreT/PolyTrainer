package me.apomazkin.vocabulary.entity

import androidx.annotation.StringRes
import me.apomazkin.chippicker.ChipPicker
import me.apomazkin.chippicker.ChipValue
import me.apomazkin.vocabulary.R

// TODO: Rename to LexicalCategoryLabel?
enum class LexemeLabel(
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

fun String.toLexemeLabel() = when (this) {
    LexemeLabel.NOUN.stringValue -> LexemeLabel.NOUN
    LexemeLabel.VERB.stringValue -> LexemeLabel.VERB
    LexemeLabel.ADJ.stringValue -> LexemeLabel.ADJ
    LexemeLabel.ADV.stringValue -> LexemeLabel.ADV
    LexemeLabel.PRON.stringValue -> LexemeLabel.PRON
    LexemeLabel.NUMERAL.stringValue -> LexemeLabel.NUMERAL
    LexemeLabel.PREP.stringValue -> LexemeLabel.PREP
    LexemeLabel.CONJ.stringValue -> LexemeLabel.CONJ
    LexemeLabel.PHRASE.stringValue -> LexemeLabel.PHRASE
    LexemeLabel.OTHER.stringValue -> LexemeLabel.OTHER
    else -> LexemeLabel.UNDEFINED
}

fun LexemeLabel.toChipPicker(): ChipPicker = when (this) {
    LexemeLabel.UNDEFINED -> ChipPicker.Off
    else -> ChipPicker.Selected(this)
}

val lexicalCategory = listOf(
    LexemeLabel.NOUN,
    LexemeLabel.VERB,
    LexemeLabel.ADJ,
    LexemeLabel.ADV,
    LexemeLabel.PRON,
    LexemeLabel.NUMERAL,
    LexemeLabel.PREP,
    LexemeLabel.CONJ,
    LexemeLabel.PHRASE,
    LexemeLabel.OTHER,
)