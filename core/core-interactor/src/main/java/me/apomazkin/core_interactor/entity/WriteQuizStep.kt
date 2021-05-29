package me.apomazkin.core_interactor.entity

import java.util.*

data class WriteQuizStep(
    val id: Long,
    val definition: String,
    val definitionId: Long,
    val answer: String,
    val grade: Int,
    val score: Int,
    val addDate: Date? = null,
    val lastSelectDate: Date? = null,
)