package me.apomazkin.core_db_impl.entity

import androidx.room.Embedded
import androidx.room.Relation
import me.apomazkin.core_db_api.entity.ComponentValueApiEntity
import me.apomazkin.core_db_impl.mapper.parseTemplateValues
import me.apomazkin.lexeme.ChoiceValues
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.logger.LexemeLogger

/**
 * Multi-level @Relation helper для подгрузки `component_values` + parent
 * `component_types` одним batched запросом Room (IN-подзапрос).
 *
 * Используется в `LexemeDbEntity.componentValueListDb: List<ComponentValueWithType>`.
 */
data class ComponentValueWithType(
    @Embedded val value: ComponentValueDb,
    @Relation(parentColumn = "component_type_id", entityColumn = "id")
    val type: ComponentTypeDb,
)

/**
 * Fail-soft маппинг (aspect `parser_fail_soft`):
 *  - `type.toApiEntity()` вернул null (unknown templateKey) → skip + лог.
 *  - `parseTemplateValues` вернул null (malformed / schema mismatch) → skip + лог.
 *
 * Caller (`LexemeDbEntity.toApiEntity`) фильтрует null'ы через `mapNotNull`.
 */
fun ComponentValueWithType.toApiEntity(
    logger: LexemeLogger,
): ComponentValueApiEntity? {
    val typeApi = type.toApiEntity() ?: run {
        logger.e(
            tag = COMPONENT_VALUE_WITH_TYPE_TAG,
            message = "skip CV: unknown templateKey='${type.templateKey}' for typeId=${type.id}"
        )
        return null
    }
    // IS486: payload CHOICE живёт в option_id, JSON не парсится (пустой envelope).
    val data = if (typeApi.template == ComponentTemplate.CHOICE) {
        val optionId = value.optionId ?: run {
            logger.e(
                tag = COMPONENT_VALUE_WITH_TYPE_TAG,
                message = "skip CV id=${value.id}: CHOICE row without option_id"
            )
            return null
        }
        ChoiceValues(optionId)
    } else {
        parseTemplateValues(value.value, typeApi.template, logger) ?: run {
            logger.e(
                tag = COMPONENT_VALUE_WITH_TYPE_TAG,
                message = "skip CV id=${value.id}: parseTemplateValues returned null"
            )
            return null
        }
    }
    return ComponentValueApiEntity(
        id = value.id,
        lexemeId = value.lexemeId,
        type = typeApi,
        data = data,
        createdAt = value.createdAt,
        updatedAt = value.updatedAt,
        removedAt = value.removedAt,
    )
}

private const val COMPONENT_VALUE_WITH_TYPE_TAG = "ComponentValueWithType"
