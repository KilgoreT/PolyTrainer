package me.apomazkin.candidates.mapper

import me.apomazkin.candidates.ComponentMatchUi
import me.apomazkin.candidates.ComponentRecommendationUi
import me.apomazkin.candidates.LookupLemmaUi
import me.apomazkin.candidates.LookupLexemeUi
import me.apomazkin.candidates.LookupWordUi
import me.apomazkin.candidates.WordRemarkUi
import me.apomazkin.lexeme.ComponentMatch
import me.apomazkin.lexeme.ComponentRecommendation
import me.apomazkin.lexeme.LookupLemma
import me.apomazkin.lexeme.LookupLexeme
import me.apomazkin.lexeme.LookupWord
import me.apomazkin.lexeme.WordRemark

/* Домен → UI, поле в поле; зовут хендлеры эффектов обоих окон. */

fun LookupWord.toUi(): LookupWordUi = LookupWordUi(
    text = text,
    status = status,
    lemmas = lemmas.map { it.toUi() },
    remarks = remarks.map { it.toUi() },
)

fun LookupLemma.toUi(): LookupLemmaUi = LookupLemmaUi(
    text = text,
    formNote = formNote,
    pronunciations = pronunciations,
    lexemes = lexemes.map { it.toUi() },
)

fun LookupLexeme.toUi(): LookupLexemeUi = LookupLexemeUi(
    key = key,
    partOfSpeechKey = partOfSpeechKey,
    source = source,
    translations = translations,
    gloss = gloss,
    caption = caption,
    notes = notes,
    examples = examples,
    grammar = grammar,
    level = level,
    synonyms = synonyms,
    antonyms = antonyms,
    componentMatches = componentMatches.map { it.toUi() },
)

fun ComponentMatch.toUi(): ComponentMatchUi = ComponentMatchUi(
    componentId = componentId,
    optionId = optionId,
    field = field,
)

fun WordRemark.toUi(): WordRemarkUi = when (this) {
    is WordRemark.ComponentUnclear -> WordRemarkUi.ComponentUnclear(componentId)
    is WordRemark.NoTranslation -> WordRemarkUi.NoTranslation(lemma = lemma, lexemeKey = lexemeKey)
}

fun ComponentRecommendation.toUi(): ComponentRecommendationUi = ComponentRecommendationUi(
    name = name,
    template = template,
    options = options,
)
