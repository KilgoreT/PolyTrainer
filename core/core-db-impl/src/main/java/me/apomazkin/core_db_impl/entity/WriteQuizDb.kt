package me.apomazkin.core_db_impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import me.apomazkin.core_db_api.entity.WriteQuizApiEntity
import me.apomazkin.core_db_api.entity.WriteQuizUpsertApiEntity
import java.util.Date

@Entity(
    tableName = "write_quiz",
    foreignKeys = [
        ForeignKey(
            entity = LexemeDb::class,
            parentColumns = ["id"],
            childColumns = ["lexeme_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("lexeme_id")]
)
data class WriteQuizDb(
        @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
        @ColumnInfo(name = "dictionary_id")
    val dictionaryId: Long = 0,
        @ColumnInfo(name = "lexeme_id")
    val lexemeId: Long,
        @ColumnInfo(name = "grade")
    val grade: Int = 0,
        @ColumnInfo(name = "score")
    val score: Int = 0,
        @ColumnInfo(name = "error_count")
    val errorCount: Int = 0,
        @ColumnInfo(name = "add_date")
    val addDate: Date,
        @ColumnInfo(name = "last_select_date")
    val lastCorrectAnswerDate: Date? = null,
) {
    companion object {
        fun create(dictionaryId: Long, lexemeId: Long) = WriteQuizDb(
            dictionaryId = dictionaryId,
            lexemeId = lexemeId,
            addDate = Date(System.currentTimeMillis())
        )
    }
}

fun WriteQuizDb.toApiEntity() = WriteQuizApiEntity(
        id = id,
        dictionaryId = dictionaryId,
        lexemeId = lexemeId,
        grade = grade,
        score = score,
        errorCount = errorCount,
        addDate = addDate,
        lastCorrectAnswerDate = lastCorrectAnswerDate,
)

fun WriteQuizUpsertApiEntity.toDb() = WriteQuizDb(
        id = id,
        dictionaryId = dictionaryId,
        lexemeId = lexemeId,
        grade = grade,
        score = score,
        errorCount = errorCount,
        addDate = addDate,
        lastCorrectAnswerDate = lastCorrectAnswerDate,
)

fun List<WriteQuizUpsertApiEntity>.toDb() = map { it.toDb() }