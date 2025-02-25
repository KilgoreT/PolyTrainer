package me.apomazkin.core_db_impl.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import me.apomazkin.core_db_api.entity.LanguageApiEntity
import java.util.Date

@Entity(
    tableName = "languages",
    indices = [Index(value = arrayOf("numericCode"), unique = true)]
)
data class LanguageDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null, // TODO: сделать ненулабельным
    val numericCode: Int,
    // TODO: Удалить?
    val code: String,
    // TODO: сделать ненулабельным
    val name: String? = null,
    val addDate: Date,
    val changeDate: Date? = null,
)

fun LanguageDb.toApiEntity() = LanguageApiEntity(
    id = id?.toInt() ?: throw IllegalArgumentException("LanguageDb id is null"),
    numericCode = numericCode,
    code = code,
    name = name ?: throw IllegalArgumentException("LanguageDb name is null"),
    addDate = addDate,
    changeDate = changeDate,
)

fun List<LanguageDb>.toApiEntity() = map { it.toApiEntity() }