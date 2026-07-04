package me.apomazkin.lexeme

/**
 * Описание поля шаблона компонента — связка имени и [PrimitiveType].
 *
 * Шаблон ([ComponentTemplate]) определяет список полей через [ComponentTemplate.fields].
 * Schema живёт в коде (см. concept/template_model.md § ComponentTemplate).
 */
data class Field(
    val name: String,
    val type: PrimitiveType,
)
