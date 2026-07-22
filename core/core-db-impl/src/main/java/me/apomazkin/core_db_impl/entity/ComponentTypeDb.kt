package me.apomazkin.core_db_impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import me.apomazkin.core_db_api.entity.ComponentTypeApiEntity
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.DependencyTarget
import java.util.Date

/**
 * Room entity для component_types. IS481 (M13).
 *
 * - `systemKey` null → user-defined; non-null → built-in (stable enum-key).
 * - `dictionaryId` null → global; non-null → per-dictionary.
 * - `name` null допустим для built-in (display из enum); user-defined требует name.
 * - `templateKey` — `"text"` / `"image"` (`"long_text"` consolidated в M13).
 * - `isMultiple` — cardinality, default false; true → разрешает несколько values на лексему.
 * - `createdAt` / `updatedAt` — audit timestamps (M13).
 * - `removedAt` — soft-delete (стиль HintDb); RENAME `remove_date → removed_at` в M13.
 *
 * `systemKey` IMMUTABLE после INSERT — см. 04_builtin_strategy.md.
 *
 * UNIQUE `(dictionary_id, name)` DROPPED в M13 — uniqueness переносится в UseCase
 * (aspect `soft_delete_unique_collision` + `userdefined_identity_invariant`).
 */
@Entity(
    tableName = "component_types",
    foreignKeys = [
        ForeignKey(
            entity = DictionaryDb::class,
            parentColumns = ["id"],
            childColumns = ["dictionary_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        // IS486: XOR-ссылки цели зависимости. CASCADE безопасен: hard-delete целей
        // случается только при удалении словаря, дерево целиком внутри словаря (spec §10).
        ForeignKey(
            entity = ComponentTypeDb::class,
            parentColumns = ["id"],
            childColumns = ["depends_on_type_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ComponentOptionDb::class,
            parentColumns = ["id"],
            childColumns = ["depends_on_option_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("dictionary_id"),
        // IS486: UNIQUE(system_key) дропнут — builtin пословарные;
        // уникальность «(ключ, словарь)» — UseCase-валидация (spec §10/§11).
        Index("system_key"),
        Index("depends_on_type_id"),
        Index("depends_on_option_id"),
    ],
)
data class ComponentTypeDb(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "system_key") val systemKey: String?,
    @ColumnInfo(name = "dictionary_id") val dictionaryId: Long?,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "template_key") val templateKey: String,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "is_multiple") val isMultiple: Boolean = false,
    // IS486: ядро; валиден только при цели-лексеме (обе depends-ссылки NULL).
    @ColumnInfo(name = "core", defaultValue = "0") val core: Boolean = false,
    // IS486: рубильник; false → не предлагается для новых значений.
    @ColumnInfo(name = "enabled", defaultValue = "1") val enabled: Boolean = true,
    // IS486: XOR-ссылки цели зависимости: обе NULL → цель «лексема»;
    // заполнена одна → зависимый от компонента / от опции; обе → нелегально (fail-soft в маппере).
    @ColumnInfo(name = "depends_on_type_id") val dependsOnTypeId: Long? = null,
    @ColumnInfo(name = "depends_on_option_id") val dependsOnOptionId: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Date,
    @ColumnInfo(name = "updated_at") val updatedAt: Date,
    @ColumnInfo(name = "removed_at") val removedAt: Date? = null,
)

/**
 * Fail-soft маппинг: unknown `templateKey` → null (F019 Best-guess B — skip row).
 * IS486: нелегальное XOR-состояние (обе depends-ссылки заполнены) → null (skip row).
 * Caller (`getComponentTypes`, `ComponentValueWithType.toApiEntity`) фильтрует null'ы.
 */
fun ComponentTypeDb.toApiEntity(): ComponentTypeApiEntity? {
    val tpl = ComponentTemplate.fromKey(templateKey) ?: return null
    val target = when {
        dependsOnTypeId != null && dependsOnOptionId != null -> return null
        dependsOnTypeId != null -> DependencyTarget.Component(ComponentTypeId(dependsOnTypeId))
        dependsOnOptionId != null -> DependencyTarget.Option(dependsOnOptionId)
        else -> DependencyTarget.Lexeme
    }
    return ComponentTypeApiEntity(
        id = id,
        systemKey = systemKey?.let(BuiltInComponent::fromKey),
        dictionaryId = dictionaryId,
        name = name,
        template = tpl,
        position = position,
        isMultiple = isMultiple,
        core = core,
        enabled = enabled,
        dependsOn = target,
        createdAt = createdAt,
        updatedAt = updatedAt,
        removedAt = removedAt,
    )
}
