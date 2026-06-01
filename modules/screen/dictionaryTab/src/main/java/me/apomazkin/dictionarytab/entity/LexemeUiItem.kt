package me.apomazkin.dictionarytab.entity

import me.apomazkin.lexeme.Lexeme
import java.util.Date


@JvmInline
value class TranslationUiEntity(val value: String)

@JvmInline
value class DefinitionUiEntity(val value: String)

data class LexemeUiItem(
    val id: Long,
    val translation: TranslationUiEntity?,
    val definition: DefinitionUiEntity?,
    val addDate: Date,
    val changeDate: Date? = null,
//    val category: LexemeLabel,
)

fun Lexeme.toUiItem(): LexemeUiItem = LexemeUiItem(
    id = lexemeId.id,
    translation = translation?.let { TranslationUiEntity(it.value) },
    definition = definition?.let { DefinitionUiEntity(it.value) },
    addDate = addDate,
    changeDate = changeDate,
)
