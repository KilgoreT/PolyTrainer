package me.apomazkin.dictionarytab.entity

import java.util.Date


@JvmInline
value class TranslationUiEntity(val value: String)

@JvmInline
value class DefinitionUiEntity(val value: String)

data class LexemeUiItem(
    val id: Long,
    val wordId: Long,
    val translation: TranslationUiEntity?,
    val definition: DefinitionUiEntity?,
    val addDate: Date,
    val changeDate: Date? = null,
//    val category: LexemeLabel,
)