package me.apomazkin.core_db_impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import me.apomazkin.core_db_api.entity.SampleApiEntity
import me.apomazkin.core_db_api.entity.Source
import java.util.Date

@Entity(tableName = "samples")
data class SampleDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val lexemeId: Long? = null,
    val value: String,
    val source: String?,
    val addDate: Date,
    val changeDate: Date? = null,
    val removeDate: Date? = null,
)

fun SampleDb.toApiEntity() = SampleApiEntity(
    id = id ?: throw IllegalArgumentException("Sample id is null"),
    lexemeId = lexemeId,
    value = value,
    source = source?.let { Source(it) },
    addDate = addDate,
    changeDate = changeDate,
    removeDate = removeDate,
)

fun List<SampleDb>.toApiEntity() = map { it.toApiEntity() }