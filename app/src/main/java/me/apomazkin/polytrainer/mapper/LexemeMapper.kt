package me.apomazkin.polytrainer.mapper

import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.lexeme.Definition
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.lexeme.LexemeId
import me.apomazkin.lexeme.Translation

fun LexemeApiEntity.toDomain(): Lexeme = Lexeme(
    lexemeId = LexemeId(id),
    translation = translation?.let { Translation(it.value) },
    definition = definition?.let { Definition(it.value) },
    addDate = addDate,
    changeDate = changeDate,
)
