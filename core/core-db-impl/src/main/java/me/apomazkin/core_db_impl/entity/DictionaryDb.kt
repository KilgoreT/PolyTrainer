package me.apomazkin.core_db_impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import java.util.Date

@Entity(
        tableName = "dictionaries",
)
data class DictionaryDb(
        // TODO: сделать ненулабельным
        //  https://github.com/KilgoreT/PolyTrainer/issues/375
        @PrimaryKey(autoGenerate = true)
        val id: Long? = null,
        val numericCode: Int? = null,
        val name: String = "",
        val addDate: Date,
        val changeDate: Date? = null,
)

fun DictionaryDb.toApiEntity() = DictionaryApiEntity(
        id = id ?: throw IllegalArgumentException("DictionaryDb id is null"),
        numericCode = numericCode,
        name = name,
        addDate = addDate,
        changeDate = changeDate,
)

fun List<DictionaryDb>.toApiEntity() = map { it.toApiEntity() }
