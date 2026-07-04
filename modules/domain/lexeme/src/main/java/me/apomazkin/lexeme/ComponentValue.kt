package me.apomazkin.lexeme

@JvmInline
value class ComponentValueId(val id: Long)

/**
 * Domain entity значения компонента лексемы.
 *
 * `type` — full embedded `ComponentType` (multi-level @Relation в DAO).
 */
data class ComponentValue(
    val id: ComponentValueId,
    val lexemeId: LexemeId,
    val type: ComponentType,
    val data: TemplateValues,
)
