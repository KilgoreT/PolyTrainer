package me.apomazkin.wordcard.entity

data class Term(
    val wordId: WordId,
    val word: Word,
    val lexemeList: List<Lexeme>
)

data class Lexeme(
    val lexemeId: LexemeId,
    val definition: String,
    val category: String,
)

@JvmInline
value class WordId(val id: Long)

@JvmInline
value class Word(val value: String)

@JvmInline
value class LexemeId(val id: Long)