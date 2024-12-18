package me.apomazkin.core_db_api.entity

import java.util.Date

@JvmInline
value class TranslationApiEntity(val value: String)

@JvmInline
value class DefinitionApiEntity(val value: String)

data class LexemeApiEntity(
    val id: Long,
    val wordId: Long,
    val translation: TranslationApiEntity? = null,
    val definition: DefinitionApiEntity? = null,
    val wordClass: String? = null,
    val options: Long = 0,
    val addDate: Date,
    val changeDate: Date? = null,
    val removeDate: Date? = null,
)

fun LexemeApiEntity.canRemoveTranslation() = definition != null
fun LexemeApiEntity.canRemoveDefinition() = translation != null