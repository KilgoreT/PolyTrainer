package me.apomazkin.core_interactor.scenario

data class WriteQuizStep(
    val id: Long,
    val definition: String,
    val definitionId: Long,
    val answer: String,
    val grade: Int,
    val score: Int,
)