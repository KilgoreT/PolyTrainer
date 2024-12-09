package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.Definition
import me.apomazkin.core_db_impl.entity.DefinitionWordSampleRel

fun DefinitionWordSampleRel.getDefinition(): Definition = Definition(
    id = lexemeDb.id ?: 0,
    wordId = lexemeDb.wordId,
    value = lexemeDb.definition,
    wordClass = lexemeDb.toWordClass(),
    sampleList = sampleDbList.map { item -> item.toAppEntity() }
)