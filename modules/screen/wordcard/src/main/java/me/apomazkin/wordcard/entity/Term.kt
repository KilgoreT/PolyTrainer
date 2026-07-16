package me.apomazkin.wordcard.entity

import me.apomazkin.lexeme.Lexeme
import java.util.Date

@JvmInline
value class WordId(val id: Long)

@JvmInline
value class Word(val value: String)

data class Term(
    val wordId: WordId,
    val word: Word,
    /** IS481 F1 fix — нужен handler'у `LoadWord` для `getComponentTypes(dictionaryId)`. */
    val dictionaryId: Long,
    /** IS485 — drawable флага словаря для шапки карточки; null → плейсхолдер. */
    val dictionaryFlagRes: Int? = null,
    val addedDate: Date,
    val changedDate: Date?,
    val removedDate: Date?,
    val lexemeList: List<Lexeme>
)