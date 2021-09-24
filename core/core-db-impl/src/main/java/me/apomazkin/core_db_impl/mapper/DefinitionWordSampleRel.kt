package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.Definition
import me.apomazkin.core_db_impl.entity.DefinitionWordSampleRel

fun DefinitionWordSampleRel.getDefinition(): Definition = Definition(
    id = definitionDb.id ?: 0,
    wordId = definitionDb.wordId,
    value = definitionDb.definition,
    wordClass = definitionDb.toWordClass(),
    sampleList = sampleDbList.map { item -> item.toAppEntity() }
)