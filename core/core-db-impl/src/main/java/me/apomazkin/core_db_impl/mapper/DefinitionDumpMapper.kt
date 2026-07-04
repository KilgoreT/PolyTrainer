package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.DefinitionDump
import me.apomazkin.core_db_impl.entity.LexemeDb
import java.util.Date


/**
 * IS481 (v12): legacy маппер. Колонки `translation` / `definition` удалены
 * из `lexemes` — `DefinitionDump.definition` теперь всегда null. Маппер
 * сохранён для backward-compat ApiEntity DTO (DefinitionDump живёт в
 * `core-db-api`). Реальные данные definition теперь в `component_values`.
 *
 * TODO mapper-refactor (backlog): переписать dump-логику на новые таблицы либо
 * удалить если функционал не используется.
 */
fun LexemeDb.toDumpEntity() = DefinitionDump(
    id = id,
    wordId = wordId,
    definition = null,
    wordClass = wordClass,
    options = options
)

fun List<LexemeDb>.toDumpEntity() = this.map { it.toDumpEntity() }


fun DefinitionDump.toDbEntity() = LexemeDb(
    id = id ?: throw IllegalArgumentException("Definition id is null"),
    wordId = wordId
        ?: throw IllegalArgumentException("Definition wordId is null"),
    wordClass = wordClass,
    options = options,
    addDate = Date(0),
)

fun List<DefinitionDump>.toDbEntity() = this.map { it.toDbEntity() }
