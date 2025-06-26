package me.apomazkin.core_db_api.entity

import java.util.Date

data class WriteQuizComplexEntity(
    val quizData: WriteQuizApiEntity,
    val lexemeData: LexemeApiEntity,
    val wordData: WordApiEntity,
    val sampleData: List<SampleApiEntity>
)

data class WriteQuizApiEntity(
        val id: Long = 0,
        val langId: Long,
        val lexemeId: Long,
        val grade: Int = 0,
        val score: Int = 0,
        val errorCount: Int = 0,
        val addDate: Date,
        val lastCorrectAnswerDate: Date? = null,
)

data class WriteQuizUpsertApiEntity(
        val id: Long,
        val langId: Long,
        val lexemeId: Long,
        val grade: Int,
        val score: Int,
        val errorCount: Int,
        val addDate: Date,
        val lastCorrectAnswerDate: Date?,
)