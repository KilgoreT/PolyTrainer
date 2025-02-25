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
    @ColumnInfo(name = "lang_id")
    val langId: Long = 0,
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
    val lastSelectDate: Date? = null,
) {
    companion object {
        fun create(langId: Long, lexemeId: Long) = WriteQuizDb(
            langId = langId,
            lexemeId = lexemeId,
            addDate = Date(System.currentTimeMillis())
        )
    }
}

fun WriteQuizDb.toApiEntity() = WriteQuizApiEntity(
    id = id,
    langId = langId,
    lexemeId = lexemeId,
    grade = grade,
    score = score,
    errorCount = errorCount,
    addDate = addDate,
    lastSelectDate = lastSelectDate,
)

fun WriteQuizUpsertApiEntity.toDb() = WriteQuizDb(
    id = id,
    langId = langId,
    lexemeId = lexemeId,
    grade = grade,
    score = score,
    errorCount = errorCount,
    addDate = addDate,
    lastSelectDate = lastSelectDate,
)

fun List<WriteQuizUpsertApiEntity>.toDb() = map { it.toDb() }