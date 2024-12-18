package me.apomazkin.core_db_impl.entity

import androidx.room.Embedded
import androidx.room.Relation
import me.apomazkin.core_db_api.entity.DefinitionApiEntity
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.core_db_api.entity.TranslationApiEntity

data class LexemeDbEntity(
    @Embedded val lexemeDb: LexemeDb,
    @Relation(
        parentColumn = "id",
        entityColumn = "lexemeId"
    )
    val sampleDbList: List<SampleDb>
)

fun LexemeDbEntity.toApiEntity() = LexemeApiEntity(
    id = lexemeDb.id ?: throw IllegalArgumentException("Lexeme id is null"),
    wordId = lexemeDb.wordId ?: throw IllegalArgumentException("Word id is null"),
    translation = lexemeDb.translation?.let { TranslationApiEntity(it) },
    definition = lexemeDb.definition?.let { DefinitionApiEntity(it) },
    wordClass = lexemeDb.wordClass,
    options = lexemeDb.options,
    addDate = lexemeDb.addDate,
    changeDate = lexemeDb.changeDate,
    removeDate = lexemeDb.removeDate,
)

fun List<LexemeDbEntity>.toApiEntity() = map { it.toApiEntity() }