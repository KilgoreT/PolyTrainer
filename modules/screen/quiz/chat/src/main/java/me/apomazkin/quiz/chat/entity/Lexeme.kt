package me.apomazkin.quiz.chat.entity

import java.util.Date

@JvmInline
value class Translation(val value: String)

@JvmInline
value class Definition(val value: String)

data class Lexeme(
    val id: Long,
    val translation: Translation? = null,
    val definition: Definition? = null,
    val addDate: Date,
    val changeDate: Date? = null,
)
