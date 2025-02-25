package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.DefinitionDump
import me.apomazkin.core_db_impl.entity.LexemeDb
import java.util.Date


fun LexemeDb.toDumpEntity() = DefinitionDump(
    id = id,
    wordId = wordId,
    definition = definition,
    wordClass = wordClass,
    options = options
)

fun List<LexemeDb>.toDumpEntity() = this.map { it.toDumpEntity() }


fun DefinitionDump.toDbEntity() = LexemeDb(
    id = id ?: throw IllegalArgumentException("Definition id is null"),
    wordId = wordId
        ?: throw IllegalArgumentException("Definition wordId is null"),
    definition = definition,
    wordClass = wordClass,
    options = options,
    addDate = Date(0),
)

fun List<DefinitionDump>.toDbEntity() = this.map { it.toDbEntity() }
