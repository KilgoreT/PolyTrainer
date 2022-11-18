package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.DefinitionDump
import me.apomazkin.core_db_impl.entity.DefinitionDb


fun DefinitionDb.toDumpEntity() = DefinitionDump(
    id = id,
    wordId = wordId,
    definition = definition,
    wordClass = wordClass,
    options = options
)

fun List<DefinitionDb>.toDumpEntity() = this.map { it.toDumpEntity() }


fun DefinitionDump.toDbEntity() = DefinitionDb(
    id = id,
    wordId = wordId,
    definition = definition,
    wordClass = wordClass,
    options = options
)

fun List<DefinitionDump>.toDbEntity() = this.map { it.toDbEntity() }
