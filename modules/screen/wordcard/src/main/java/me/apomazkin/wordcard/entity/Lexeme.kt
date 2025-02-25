package me.apomazkin.wordcard.entity

import java.util.Date

@JvmInline
value class LexemeId(val id: Long)

@JvmInline
value class Translation(val value: String)

@JvmInline
value class Definition(val value: String)

data class Lexeme(
    val lexemeId: LexemeId,
    val translation: Translation?,
    val definition: Definition?,
    val category: String?,
    val addDate: Date,
    val changeDate: Date? = null,
)