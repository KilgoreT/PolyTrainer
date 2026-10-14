package me.apomazkin.wordstab.logic.photo

import me.apomazkin.candidates.wordsBatchOf
import me.apomazkin.lexeme.WordsBatch
import me.apomazkin.wordstab.logic.PhotoWordsState

/**
 * Пакет из окна (AWP-20): карточки окна с их «куда класть» и группами —
 * общий сборщик [wordsBatchOf]; нет формы — пустой пакет.
 */
internal fun PhotoWordsState.toWordsBatch(): WordsBatch {
    val form = form ?: return WordsBatch(dictionaryId = dictionaryId ?: 0L, words = emptyList())
    return wordsBatchOf(cards = cards, mapping = mapping, form = form, commonGroupId = commonGroupId)
}
