package me.apomazkin.lexeme

import java.util.Date

/**
 * IS486 фаза 1: общие фикстур-билдеры тестов иерархии (зоны A3–A7).
 * Граф словаря D1 (id=1); чужой словарь D2 (id=2).
 */
internal object HierarchyFixtures {

    val now = Date(0L)
    val removed = Date(1L)

    fun type(
        id: Long,
        dictionaryId: Long? = 1L,
        template: ComponentTemplate = ComponentTemplate.TEXT,
        core: Boolean = false,
        enabled: Boolean = true,
        isMultiple: Boolean = false,
        dependsOn: DependencyTarget = DependencyTarget.Lexeme,
        systemKey: BuiltInComponent? = null,
        removedAt: Date? = null,
        name: String? = "type-$id",
    ) = ComponentType(
        id = ComponentTypeId(id),
        systemKey = systemKey,
        dictionaryId = dictionaryId,
        name = name,
        template = template,
        position = 0,
        isMultiple = isMultiple,
        core = core,
        enabled = enabled,
        dependsOn = dependsOn,
        createdAt = now,
        updatedAt = now,
        removedAt = removedAt,
    )

    fun option(
        id: Long,
        ownerTypeId: Long,
        removedAt: Date? = null,
    ) = ComponentOption(
        id = id,
        componentTypeId = ComponentTypeId(ownerTypeId),
        label = "option-$id",
        position = 0,
        removedAt = removedAt,
    )
}
