package me.apomazkin.wordcard.entity

import java.util.Date

@JvmInline
value class WordId(val id: Long)

@JvmInline
value class Word(val value: String)

data class Term(
    val wordId: WordId,
    val word: Word,
    val addedDate: Date,
    val changedDate: Date?,
    val removedDate: Date?,
    val lexemeList: List<Lexeme>
)