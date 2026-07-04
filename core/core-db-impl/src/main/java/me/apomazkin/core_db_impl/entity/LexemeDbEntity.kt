package me.apomazkin.core_db_impl.entity

import androidx.room.Embedded
import androidx.room.Relation
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.logger.LexemeLogger

/**
 * Lexeme + child collections подгружаемые Room через Multi-level @Relation:
 * - `sampleDbList` — примеры лексемы (legacy, camelCase `lexemeId`).
 * - `componentValueListDb` — компоненты лексемы с типами (IS481, snake_case).
 *
 * Room делает 3 batched SELECT (lexemes → component_values WHERE lexeme_id IN
 * (...) → component_types WHERE id IN (...)). N+1 не возникает.
 */
data class LexemeDbEntity(
    @Embedded val lexemeDb: LexemeDb,
    @Relation(
        parentColumn = "id",
        entityColumn = "lexemeId"
    )
    val sampleDbList: List<SampleDb>,
    @Relation(
        entity = ComponentValueDb::class,
        parentColumn = "id",
        entityColumn = "lexeme_id",
    )
    val componentValueListDb: List<ComponentValueWithType>,
)

/**
 * F031 (M13): **post-load filter** на soft-deleted `component_values` +
 * `component_types` (Room `@Relation` не поддерживает WHERE в SQL).
 *
 * Дополнительно `mapNotNull` отбрасывает значения которые `toApiEntity(logger)` отверг
 * fail-soft (unknown template / malformed JSON).
 */
fun LexemeDbEntity.toApiEntity(logger: LexemeLogger): LexemeApiEntity = LexemeApiEntity(
    id = lexemeDb.id,
    components = componentValueListDb
        .filter { it.value.removedAt == null && it.type.removedAt == null }
        .mapNotNull { it.toApiEntity(logger) },
    wordClass = lexemeDb.wordClass,
    options = lexemeDb.options,
    addDate = lexemeDb.addDate,
    changeDate = lexemeDb.changeDate,
)

fun List<LexemeDbEntity>.toApiEntity(logger: LexemeLogger) = map { it.toApiEntity(logger) }
