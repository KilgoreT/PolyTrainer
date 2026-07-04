# Data design tree: IS481 component_constructor

DAG-граф data-слоя для IS481. Покрывает: M12→M13 миграцию (composite JSON rewrite, drop UNIQUE на `component_values` + `component_types(dictionary_id, name)`, ADD `is_multi/created_at/updated_at`, RENAME `remove_date → removed_at` в `component_types`, template-key consolidation `long_text → text`, timestamps backfill), обновление entity (`ComponentTypeDb`, `ComponentValueDb`, `ComponentValueWithType`, `LexemeDbEntity` post-load filter), обновление DAO (`ComponentTypeDao`, `ComponentValueDao`, `QuizConfigDao`, `WordDao`), полную замену JSON-маппера (`ComponentValueDataJson → TemplateValuesJson`), seed под M13, регистрацию миграции в `RoomModule`, реализацию 11 методов `LexemeApi` в `CoreDbApiImpl` (5 BREAKING + 6 NEW), bump `Database.version = 13`, schema export `13.json`, миграционные / DAO / mapper тесты, а также финальный узел всего IS481 — удаление `ComponentValueData.kt` из `:modules:domain:lexeme`.

> Pre-conditions:
> - business-фаза уже закрыла domain (`TemplateValues / Primitive / Field / PrimitiveType / Scope / DeletionImpact / *Outcome`), core-db-api (`ComponentTypeApiEntity / ComponentValueApiEntity / DictionaryTypesSnapshot / UserDefinedTypesUsageSnapshot / ComponentOutcomeApiEntity`), `LexemeApi` сигнатуры (5 BREAKING + 6 NEW методов в `CoreDbApi.kt`);
> - infra-фаза зарегистрировала screen-/widget-модули и расширила host'ы; `RoomModule.kt:50` оставлен с `.addMigrations(Migration_011_to_012)` (узел id 35 infra-tree отложен — реализуется здесь).

## Часть 1: Граф

```yaml
# ============================================================
# Tier 0: JSON mapper — полная замена (`ComponentValueDataJson → TemplateValuesJson`)
# ============================================================
- id: 1
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/mapper/TemplateValuesJson.kt
  action: "+"
  depends: []

- id: 2
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/mapper/ComponentValueDataJson.kt
  action: "-"
  depends: [1]

# ============================================================
# Tier 1: Room entities (schema под M13)
# ============================================================
- id: 3
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/ComponentTypeDb.kt
  action: "~"
  depends: []

- id: 4
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/ComponentValueDb.kt
  action: "~"
  depends: []

- id: 5
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/ComponentValueWithType.kt
  action: "~"
  depends: [1, 3, 4]

- id: 6
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/LexemeDbEntity.kt
  action: "~"
  depends: [5]

# ============================================================
# Tier 2: DAO updates (active filter, atomic ops, new queries)
# ============================================================
- id: 7
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/dao/ComponentTypeDao.kt
  action: "~"
  depends: [3]

- id: 8
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/dao/ComponentValueDao.kt
  action: "~"
  depends: [4]

- id: 9
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/dao/QuizConfigDao.kt
  action: "~"
  depends: []

- id: 10
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/WordDao.kt
  action: "~"
  depends: [4]

# ============================================================
# Tier 3: Seed + Database version bump
# ============================================================
- id: 11
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/SeedBuiltIns.kt
  action: "~"
  depends: [3]

- id: 12
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/Database.kt
  action: "~"
  depends: [3, 4]

# ============================================================
# Tier 4: Migration M12 → M13
# ============================================================
- id: 13
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/migrations/Migration_012_to_013.kt
  action: "+"
  depends: [11]

- id: 14
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/migrations/Migration_011_to_012.kt
  action: "~"
  depends: []

- id: 15
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/schemas/me.apomazkin.core_db_impl.room.Database/13.json
  action: "+"
  depends: [3, 4, 12]

# ============================================================
# Tier 5: DI — RoomModule registration
# ============================================================
- id: 16
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/module/RoomModule.kt
  action: "~"
  depends: [12, 13]

# ============================================================
# Tier 6: CoreDbApiImpl — implementation 5 BREAKING + 6 NEW methods
# ============================================================
- id: 17
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt
  action: "~"
  depends: [1, 3, 4, 5, 6, 7, 8, 9, 10]

# ============================================================
# Tier 7: Final domain cleanup (узел перенесён из business_design_tree #6)
# ============================================================
- id: 18
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentValueData.kt
  action: "-"
  depends: [1, 2, 17]

# ============================================================
# Tier 8: Tests (instrumented + unit)
# ============================================================
- id: 19
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/MigrationFrom12to13.kt
  action: "+"
  depends: [13, 15]

- id: 20
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/MigrationFrom12to13IdempotencyTest.kt
  action: "+"
  depends: [13]

- id: 21
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/test/java/me/apomazkin/core_db_impl/mapper/TemplateValuesJsonTest.kt
  action: "+"
  depends: [1]

- id: 22
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/test/resources/fixtures/component_values/text_value.json
  action: "+"
  depends: []

- id: 23
  file: /Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/test/resources/fixtures/component_values/image_value.json
  action: "+"
  depends: []
```

## Часть 2: Детали изменений

### Tier 0: JSON mapper полная замена

#### #1 TemplateValuesJson.kt [+]

Назначение: новый mapper для M13 формата. Полностью замещает `ComponentValueDataJson.kt`. Симметричный round-trip + fail-soft парсинг (per aspect `forward_compat_unknown`).

**Формат M13** (per `business_summary.md` §1 + scope.md `db_migration`):
```json
{"fields": {"value": {"type": "text", "value": "..."}}}
{"fields": {"value": {"type": "image", "uri": "..."}}}
```

Сигнатура:
```kotlin
package me.apomazkin.core_db_impl.mapper

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ImageValues
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.lexeme.TextValues
import me.apomazkin.logger.LexemeLogger
import org.json.JSONException
import org.json.JSONObject

/**
 * JSON envelope: {"fields": {"<fieldName>": {"type": "<primType>", "<typedPayload>": ...}}}
 *
 * - "text"  → {"type":"text","value":"..."}
 * - "image" → {"type":"image","uri":"..."}
 * - "color" → {"type":"color","hex":"..."}  // зарезервирован (Primitive.Color)
 *
 * MVP M13 поддерживает только `TextValues` и `ImageValues`. Composite (multi-field)
 * values добавляются в будущих фичах без breaking changes в envelope.
 *
 * Fail-soft контракт (forward_compat_unknown):
 *  - malformed JSON → null + Crashlytics-лог (через caller, передавший logger).
 *  - unknown primitive type → null + лог.
 *  - schema-mismatch (template ждёт text, json содержит image) → null + лог.
 *
 * Caller-mapper (`ComponentValueWithType.toApiEntity()`) обрабатывает `null` как
 * skip компонента (см. #5).
 */
fun TemplateValues.toJson(): String = when (this) {
    is TextValues -> JSONObject().apply {
        put(
            "fields", JSONObject().put(
                "value", JSONObject().apply {
                    put("type", "text")
                    put("value", value.value)
                }
            )
        )
    }.toString()

    is ImageValues -> JSONObject().apply {
        put(
            "fields", JSONObject().put(
                "value", JSONObject().apply {
                    put("type", "image")
                    put("uri", value.uri)
                }
            )
        )
    }.toString()
}

fun parseTemplateValues(
    json: String,
    template: ComponentTemplate,
    logger: LexemeLogger,
): TemplateValues? = try {
    val root = JSONObject(json)
    val fields = root.getJSONObject("fields")
    val valueObj = fields.getJSONObject("value")
    val type = valueObj.getString("type")
    when (template) {
        ComponentTemplate.TEXT -> when (type) {
            "text" -> TextValues(Primitive.Text(valueObj.getString("value")))
            else -> {
                logger.e(
                    tag = TEMPLATE_VALUES_JSON_TAG,
                    message = "schema mismatch: template=TEXT, json type='$type'"
                )
                null
            }
        }
        ComponentTemplate.IMAGE -> when (type) {
            "image" -> ImageValues(Primitive.Image(valueObj.getString("uri")))
            else -> {
                logger.e(
                    tag = TEMPLATE_VALUES_JSON_TAG,
                    message = "schema mismatch: template=IMAGE, json type='$type'"
                )
                null
            }
        }
    }
} catch (e: JSONException) {
    logger.e(tag = TEMPLATE_VALUES_JSON_TAG, message = "malformed JSON: ${e.message}")
    null
}

private const val TEMPLATE_VALUES_JSON_TAG = "TemplateValuesJson"
```

Note: `logger` пробрасывается caller'ом (`ComponentValueWithType.toApiEntity` — добавит параметр; либо use module-level singleton). Альтернатива (избежать ctor-injection в data class extension) — top-level `logger: LexemeLogger?` параметр с default `null`. Финализация — на `data_implement` (mechanical).

#### #2 ComponentValueDataJson.kt [-]

Удалить файл целиком. Все imports `me.apomazkin.core_db_impl.mapper.toComponentValueData` / `.toJson` (от `ComponentValueData`) → compile-fail; rebind на новый `parseTemplateValues` / `TemplateValues.toJson` в #5 + #17.

### Tier 1: Room entities (schema под M13)

#### #3 ComponentTypeDb.kt [~]

**Было** (lines 23-49):
```kotlin
@Entity(
    tableName = "component_types",
    foreignKeys = [ ... ],
    indices = [
        Index("dictionary_id"),
        Index(value = ["system_key"], unique = true),
        Index(value = ["dictionary_id", "name"], unique = true),     // DROP в M13
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
    @ColumnInfo(name = "remove_date") val removeDate: Date? = null,
)

fun ComponentTypeDb.toApiEntity(): ComponentTypeApiEntity = ComponentTypeApiEntity(
    id = id, systemKey = systemKey?.let(BuiltInComponent::fromKey),
    dictionaryId = dictionaryId, name = name,
    template = ComponentTemplate.fromKey(templateKey),                // non-null contract сейчас
    position = position, removeDate = removeDate,
)
```

**Стало:**
```kotlin
@Entity(
    tableName = "component_types",
    foreignKeys = [ ... ],
    indices = [
        Index("dictionary_id"),
        Index(value = ["system_key"], unique = true),
        // UNIQUE (dictionary_id, name) DROPPED — uniqueness переносится в UseCase
        // (aspect `soft_delete_unique_collision` + `userdefined_identity_invariant`).
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
    @ColumnInfo(name = "is_multi") val isMulti: Boolean = false,     // NEW M13
    @ColumnInfo(name = "created_at") val createdAt: Date,            // NEW M13
    @ColumnInfo(name = "updated_at") val updatedAt: Date,            // NEW M13
    @ColumnInfo(name = "removed_at") val removedAt: Date? = null,    // RENAME M13
)

/**
 * Fail-soft маппинг: unknown `templateKey` → null (F019 Best-guess B —
 * skip row). Caller (`getComponentTypes`, `ComponentValueWithType.toApiEntity`)
 * фильтрует null'ы.
 */
fun ComponentTypeDb.toApiEntity(): ComponentTypeApiEntity? {
    val tpl = ComponentTemplate.fromKey(templateKey) ?: return null  // F019
    return ComponentTypeApiEntity(
        id = id,
        systemKey = systemKey?.let(BuiltInComponent::fromKey),
        dictionaryId = dictionaryId,
        name = name,
        template = tpl,
        position = position,
        isMulti = isMulti,
        createdAt = createdAt,
        updatedAt = updatedAt,
        removedAt = removedAt,
    )
}
```

`KDoc` обновить: `removeDate → removedAt`; добавить заметки про `isMulti / createdAt / updatedAt`; удалить упоминание UNIQUE на `(dictionary_id, name)`.

#### #4 ComponentValueDb.kt [~]

**Было** (lines 21-48):
```kotlin
@Entity(
    tableName = "component_values",
    foreignKeys = [ ... ],
    indices = [
        Index("component_type_id"),
        Index(value = ["lexeme_id", "component_type_id"], unique = true),  // DROP в M13
    ],
)
data class ComponentValueDb(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "lexeme_id") val lexemeId: Long,
    @ColumnInfo(name = "component_type_id") val componentTypeId: Long,
    @ColumnInfo(name = "value") val value: String,
)
```

**Стало:**
```kotlin
@Entity(
    tableName = "component_values",
    foreignKeys = [ ... ],
    indices = [
        Index("component_type_id"),
        Index("lexeme_id"),                                         // NEW — phantom index
                                                                    // после drop UNIQUE
                                                                    // (нужен для lookup
                                                                    // by lexeme_id).
    ],
)
data class ComponentValueDb(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "lexeme_id") val lexemeId: Long,
    @ColumnInfo(name = "component_type_id") val componentTypeId: Long,
    @ColumnInfo(name = "value") val value: String,
    @ColumnInfo(name = "created_at") val createdAt: Date,           // NEW M13
    @ColumnInfo(name = "updated_at") val updatedAt: Date,           // NEW M13
    @ColumnInfo(name = "removed_at") val removedAt: Date? = null,   // NEW M13
)
```

**Обоснование `Index("lexeme_id")`**: composite UNIQUE `(lexeme_id, component_type_id)` покрывал leading column `lexeme_id` для index lookup. После DROP UNIQUE — `getForLexeme(lexemeId)` уйдёт в full-scan. Phantom `Index("lexeme_id")` восстанавливает производительность.

`KDoc` обновить — описать M13 формат JSON envelope (`{"fields": {...}}`); удалить упоминание composite UNIQUE.

#### #5 ComponentValueWithType.kt [~]

**Было** (lines 20-29):
```kotlin
fun ComponentValueWithType.toApiEntity(): ComponentValueApiEntity {
    val typeApi = type.toApiEntity()                              // non-nullable сейчас
    val data = value.value.toComponentValueData(typeApi.template) // M12 mapper
    return ComponentValueApiEntity(
        id = value.id, lexemeId = value.lexemeId, type = typeApi, data = data,
    )
}
```

**Стало:**
```kotlin
fun ComponentValueWithType.toApiEntity(
    logger: LexemeLogger,
): ComponentValueApiEntity? {
    val typeApi = type.toApiEntity() ?: run {                     // F019 skip
        logger.e(
            tag = COMPONENT_VALUE_WITH_TYPE_TAG,
            message = "skip CV: unknown template key for typeId=${type.id}"
        )
        return null
    }
    val data = parseTemplateValues(value.value, typeApi.template, logger) ?: run {
        // parser уже залогировал; здесь — second-line context (caller знает id)
        logger.e(
            tag = COMPONENT_VALUE_WITH_TYPE_TAG,
            message = "skip CV id=${value.id}: parseTemplateValues returned null"
        )
        return null
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
```

Сигнатура меняется: `toApiEntity()` теперь принимает `LexemeLogger` параметр и возвращает nullable. Caller (`LexemeDbEntity.toApiEntity`, `LexemeApiImpl.getComponentTypes`) пробрасывает logger и фильтрует null'ы.

#### #6 LexemeDbEntity.kt [~]

**Было** (lines 34-43):
```kotlin
fun LexemeDbEntity.toApiEntity(): LexemeApiEntity = LexemeApiEntity(
    id = lexemeDb.id,
    components = componentValueListDb.map { it.toApiEntity() },     // подтягивает soft-deleted
    wordClass = lexemeDb.wordClass,
    options = lexemeDb.options,
    addDate = lexemeDb.addDate,
    changeDate = lexemeDb.changeDate,
)

fun List<LexemeDbEntity>.toApiEntity() = map { it.toApiEntity() }
```

**Стало:**
```kotlin
/**
 * F031 best-guess A: **post-load filter** на soft-deleted `component_values`
 * + `component_types` (Room `@Relation` не поддерживает WHERE в SQL).
 *
 * Дополнительно `mapNotNull` отбрасывает значения которые `toApiEntity()` отверг
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
```

Caller `LexemeApiImpl.getLexemeById` (CoreDbApiImpl.kt:218) пробрасывает `logger`.

### Tier 2: DAO updates

#### #7 ComponentTypeDao.kt [~]

**Изменения** (audit + новые методы под business contract):

1. **Audit existing методы** — заменить `remove_date IS NULL` на `removed_at IS NULL` (rename column). Всего 5 callsite'ов: `flowTypesForDictionary` (line 28), `getTypesForDictionary` (line 38), `getBuiltInTypes` (line 43), `getBySystemKey` (line 46). `getById` (line 49) — **оставить без фильтра** (intentional, для read-only views / lookup при rename / soft-delete-impact). F170: write paths (`addLexemeWithComponents` #10, `addComponentValue` / `updateComponentValue` в #17) обязаны после `getById(...)` explicit-проверять `type.removedAt != null` и кидать `IllegalStateException("Cannot insert ComponentValue for soft-deleted type X")`. Альтернатива — отдельный `getActiveById(): ComponentTypeDb?` с фильтром `removed_at IS NULL`; отброшена в пользу explicit-guard'а на каждом write call-site (видим в code review, чем абстракция в DAO).

2. **`softDelete` query update** — `remove_date = :now` → `removed_at = :now`:
   ```kotlin
   @Query("UPDATE component_types SET removed_at = :now, updated_at = :now WHERE id = :id AND system_key IS NULL")
   suspend fun softDelete(id: Long, now: Date): Int
   ```
   Дополнение: `updated_at` тоже трогаем при soft-delete (semantic — изменение состояния row).

3. **NEW методы для CRUD из business contract:**
   ```kotlin
   /**
    * Atomic rename + update updated_at. Built-in защищён `WHERE system_key IS NULL`.
    * @return затронутых строк (0 если built-in либо id не найден).
    */
   @Query("UPDATE component_types SET name = :newName, updated_at = :now WHERE id = :id AND system_key IS NULL")
   suspend fun renameUserDefined(id: Long, newName: String, now: Date): Int

   /**
    * Lookup по identity-tuple для two-prong SELECT (aspect `soft_delete_unique_collision`).
    * Per-dict branch.
    */
   @Query(
       """
       SELECT * FROM component_types
       WHERE dictionary_id = :dictId AND name = :name
         AND system_key IS NULL AND removed_at IS NULL
       LIMIT 1
       """
   )
   suspend fun findActiveUserDefinedByName(dictId: Long, name: String): ComponentTypeDb?

   /**
    * Global branch (`dictionary_id IS NULL`) — отдельный @Query т.к. SQL
    * `dictionary_id = NULL` всегда даёт UNKNOWN (F032).
    */
   @Query(
       """
       SELECT * FROM component_types
       WHERE dictionary_id IS NULL AND name = :name
         AND system_key IS NULL AND removed_at IS NULL
       LIMIT 1
       """
   )
   suspend fun findActiveGlobalByName(name: String): ComponentTypeDb?

   /**
    * Cross-scope коллизия (userdefined_identity_invariant, F039):
    * при создании global "X" проверить отсутствие active per-dict "X" в любом словаре.
    */
   @Query(
       """
       SELECT COUNT(*) FROM component_types
       WHERE dictionary_id IS NOT NULL AND name = :name
         AND system_key IS NULL AND removed_at IS NULL
       """
   )
   suspend fun countActivePerDictByName(name: String): Int

   /**
    * Подписка на все user-defined active (для `flowAllUserDefinedTypesWithUsage`).
    */
   @Query(
       """
       SELECT * FROM component_types
       WHERE system_key IS NULL AND removed_at IS NULL
       ORDER BY position ASC, created_at ASC
       """
   )
   fun flowAllUserDefined(): Flow<List<ComponentTypeDb>>

   /**
    * Подписка на типы применимые к словарю (active user-defined per-dict + global).
    * Для `flowUserDefinedTypesForDictionary` (read-side).
    */
   @Query(
       """
       SELECT * FROM component_types
       WHERE (dictionary_id = :dictId OR dictionary_id IS NULL)
         AND system_key IS NULL AND removed_at IS NULL
       ORDER BY (dictionary_id IS NULL) DESC, position ASC, created_at ASC
       """
   )
   fun flowUserDefinedForDictionary(dictId: Long): Flow<List<ComponentTypeDb>>
   ```

4. **INSERT method** — добавить timestamps (опциональный convention; уже на `@Insert` Room сериализует Date через TypeConverter). Существующий `insert(type: ComponentTypeDb): Long` остаётся, но caller (CoreDbApiImpl) обязан передать `createdAt = updatedAt = now` в data class.

#### #8 ComponentValueDao.kt [~]

**Изменения:**

1. **Audit existing методы** — добавить `removed_at IS NULL` (active фильтр) по convention `dao_convention`:
   ```kotlin
   @Query("SELECT * FROM component_values WHERE lexeme_id = :lexemeId AND removed_at IS NULL")
   suspend fun getForLexeme(lexemeId: Long): List<ComponentValueDb>

   @Query("SELECT * FROM component_values WHERE id = :id AND removed_at IS NULL")
   suspend fun getById(id: Long): ComponentValueDb?

   @Query(
       """
       SELECT * FROM component_values
       WHERE lexeme_id = :lexemeId AND component_type_id = :typeId AND removed_at IS NULL
       """
   )
   suspend fun getForLexemeAndType(lexemeId: Long, typeId: Long): ComponentValueDb?

   @Query("SELECT COUNT(*) FROM component_values WHERE lexeme_id = :lexemeId AND removed_at IS NULL")
   suspend fun countForLexeme(lexemeId: Long): Int
   ```

   `delete` / `deleteByLexemeAndType` — hard-delete для legacy callsites (используется в `LexemeApiImpl.deleteComponentValue`); оставить без фильтра — caller знает что хочет hard-delete. Альтернатива (переписать legacy callsites на soft-delete) — out-of-scope (нет ui-flow для удаления одиночного value в IS481).

2. **NEW `insertSingleSafe @Transaction` default-method** (aspect `cardinality_safety`):
   ```kotlin
   /**
    * Atomic insert одного value с cardinality-safety на `is_multi=false`.
    * Если для (lexeme_id, component_type_id) уже есть active value (removed_at IS NULL)
    * и компонент с `is_multi=false` — abort transaction через IllegalStateException.
    *
    * `is_multi` lookup делается caller-mapper'ом (LexemeApiImpl) и передаётся
    * параметром — DAO не должен JOIN'ить component_types.
    *
    * @throws IllegalStateException если попытка добавить single-value второй раз.
    */
   @Transaction
   suspend fun insertSingleSafe(value: ComponentValueDb, isMulti: Boolean): Long {
       if (!isMulti) {
           val existing = countActiveForLexemeAndType(value.lexemeId, value.componentTypeId)
           if (existing > 0) {
               throw IllegalStateException(
                   "Single-cardinality violation: typeId=${value.componentTypeId} " +
                       "already has active value for lexemeId=${value.lexemeId}"
               )
           }
       }
       return insert(value)
   }

   @Query(
       """
       SELECT COUNT(*) FROM component_values
       WHERE lexeme_id = :lexemeId AND component_type_id = :typeId
         AND removed_at IS NULL
       """
   )
   suspend fun countActiveForLexemeAndType(lexemeId: Long, typeId: Long): Int
   ```

3. **NEW soft-delete cascade** при soft-delete `component_type` — отдельный bulk query:
   ```kotlin
   /**
    * Cascade soft-delete всех active values для компонента
    * (вызывается из @Transaction в `LexemeApiImpl.softDeleteComponentType`).
    */
   @Query(
       """
       UPDATE component_values
       SET removed_at = :now, updated_at = :now
       WHERE component_type_id = :typeId AND removed_at IS NULL
       """
   )
   suspend fun softDeleteByTypeId(typeId: Long, now: Date): Int
   ```

4. **NEW aggregate queries** для `previewDeletionImpact` + `flowAllUserDefinedTypesWithUsage`:
   ```kotlin
   /**
    * COUNT active values для типа (preview deletion impact).
    */
   @Query(
       "SELECT COUNT(*) FROM component_values WHERE component_type_id = :typeId AND removed_at IS NULL"
   )
   suspend fun countActiveByTypeId(typeId: Long): Int

   /**
    * Distinct dictionary ids среди active lexemes у которых есть active value этого типа.
    * JOIN: cv → lexemes (lexeme_id → id) → words (word_id → id) → dictionaries.
    */
   @Query(
       """
       SELECT DISTINCT w.dictionary_id
       FROM component_values cv
       JOIN lexemes l ON l.id = cv.lexeme_id
       JOIN words w ON w.id = l.word_id
       WHERE cv.component_type_id = :typeId AND cv.removed_at IS NULL
       """
   )
   suspend fun dictionaryIdsForTypeId(typeId: Long): List<Long>

   /**
    * Aggregated count per type — для `UserDefinedTypesUsageSnapshot.valueCountByType`.
    * Возвращает `List<TypeIdCount>` (вспомогательный DTO с @Embedded).
    */
   @Query(
       """
       SELECT component_type_id AS typeId, COUNT(*) AS count
       FROM component_values
       WHERE removed_at IS NULL
       GROUP BY component_type_id
       """
   )
   suspend fun aggregatedValueCountPerType(): List<TypeIdCount>

   /**
    * Per-dictionary count: для `DictionaryTypesSnapshot.valueCountByType`.
    * JOIN cv → lexemes → words WHERE w.dictionary_id = :dictId
    * GROUP BY cv.component_type_id.
    */
   @Query(
       """
       SELECT cv.component_type_id AS typeId, COUNT(*) AS count
       FROM component_values cv
       JOIN lexemes l ON l.id = cv.lexeme_id
       JOIN words w ON w.id = l.word_id
       WHERE cv.removed_at IS NULL AND w.dictionary_id = :dictId
       GROUP BY cv.component_type_id
       """
   )
   suspend fun aggregatedValueCountPerTypeForDict(dictId: Long): List<TypeIdCount>

   /**
    * Distinct dictionary ids per type — для `dictionaryIdsByType` в aggregated snapshot.
    */
   @Query(
       """
       SELECT cv.component_type_id AS typeId, w.dictionary_id AS dictId
       FROM component_values cv
       JOIN lexemes l ON l.id = cv.lexeme_id
       JOIN words w ON w.id = l.word_id
       WHERE cv.removed_at IS NULL
       GROUP BY cv.component_type_id, w.dictionary_id
       """
   )
   suspend fun typeDictPairs(): List<TypeDictPair>
   ```

   Где `TypeIdCount` / `TypeDictPair` — вспомогательные DTO внутри DAO-файла либо в `entity/`:
   ```kotlin
   data class TypeIdCount(val typeId: Long, val count: Int)
   data class TypeDictPair(val typeId: Long, val dictId: Long)
   ```
   Размещение — в DAO-файле inline (приватные `data class` верхнего уровня для DAO-only consumption).

#### #9 QuizConfigDao.kt [~]

**Изменения:**

1. **NEW `flowAllConfigs()`** — нужен для `previewDeletionImpact.affectedQuizConfigs` (scan + JSON match на UseCase-уровне):
   ```kotlin
   @Query("SELECT * FROM quiz_configs")
   fun flowAllConfigs(): Flow<List<QuizConfigDb>>

   @Query("SELECT * FROM quiz_configs")
   suspend fun getAllConfigs(): List<QuizConfigDb>
   ```

2. **NEW bulk update method** для cascade при soft-delete / rename:
   ```kotlin
   /**
    * Bulk update `component_refs` — caller собирает новый JSON (через
    * `ComponentTypeRefJson` mapper), DAO просто обновляет.
    * Используется в `LexemeApiImpl.softDeleteComponentType` + `renameComponentType`
    * для cascade очистки/переименования refs.
    */
   @Query("UPDATE quiz_configs SET component_refs = :newRefs WHERE id = :id")
   suspend fun updateComponentRefs(id: Long, newRefs: String)
   ```

   Альтернатива — использовать существующий `update(config)`. Различие: `updateComponentRefs(id, newRefs)` точечный (одно поле), `update(config)` целиком entity. Точечный предпочтительнее: меньше шансов на race при concurrent reader (config может быть прочитан, изменён в другом месте, и `update(config)` затрёт чужое изменение). Финализация — на `data_implement`.

   **Решение здесь**: добавить `updateComponentRefs(id, newRefs)` (точечный). Cascade выполняется внутри `@Transaction` в `LexemeApiImpl` (см. #17).

#### #10 WordDao.kt [~]

**Изменения** — два места:

1. **`addLexemeWithComponents` signature update** (line 199-217) — теперь принимает четвёрку `(typeId, jsonValue, isMulti, now: Date)` либо отдельная сигнатура. Решение: упростить — caller (`LexemeApiImpl`) собирает `ComponentValueDb` напрямую (с createdAt/updatedAt) и передаёт List уже готовых entities:
   ```kotlin
   /**
    * Atomic INSERT новой lexeme + WriteQuiz + N component_values.
    *
    * **F171 (iter 3):** cardinality pre-check (F169 / F170) **перенесён**
    * в `LexemeApiImpl.addLexemeWithComponents` (#17), т.к. Room `@Dao interface`
    * не имеет cross-DAO access — отсюда нельзя вызвать `componentTypeDao.getById(...)`.
    * WordDao выполняет **только** cascading INSERTs; pre-check ответственность
    * caller'а (LexemeApiImpl внутри `database.withTransaction { ... }`).
    *
    * Cascading INSERTs: lexeme → write_quiz → N component_values
    * (через `_insertComponentValue`).
    */
   @Transaction
   suspend fun addLexemeWithComponents(
       lexemeDb: LexemeDb,
       dictionaryId: Long,
       components: List<ComponentValueDb>,                  // CHANGED — full entity, не Pair
   ): Long {
       val newLexemeId = addLexeme(lexemeDb)
       addWriteQuiz(WriteQuizDb.create(dictionaryId = dictionaryId, lexemeId = newLexemeId))
       components.forEach { cv ->
           _insertComponentValue(cv.copy(lexemeId = newLexemeId))
       }
       return newLexemeId
   }
   ```
   F169 / F170 pre-check выполняется в `LexemeApiImpl.addLexemeWithComponents` (#17)
   внутри `database.withTransaction { ... }` — там доступен `componentTypeDao`.
   Single/multi differentiation для single-INSERT (legacy callsites) остаётся
   в `addComponentValue` — переписывается на `componentValueDao.insertSingleSafe(...)`
   с F170 / cardinality guard'ами на уровне `LexemeApiImpl` (см. #17 / F172).

2. **`_insertComponentValue` остаётся** — caller (compound INSERT) уже проверил invariants
   pre-check'ом; per-row safety не дублируется.

KDoc update — описать новый параметр (`List<ComponentValueDb>`) + явные invariants F169 / F170.

### Tier 3: Seed + Database

#### #11 SeedBuiltIns.kt [~]

**Было** (lines 23-31):
```kotlin
internal fun seedBuiltIns(connection: SQLiteConnection) {
    connection.execSQL(
        """
        INSERT OR IGNORE INTO component_types
            (system_key, dictionary_id, name, template_key, position, remove_date)
        VALUES ('translation', NULL, NULL, 'text', 0, NULL)
        """.trimIndent()
    )
}
```

**Стало:**
```kotlin
/**
 * Seed built-in component types для M13 fresh install path.
 *
 * Идемпотентна через `INSERT OR IGNORE` (UNIQUE на `system_key`).
 * Built-in `translation` — `is_multi = 0` (false), `created_at = updated_at = now`,
 * `removed_at = NULL`.
 *
 * **Используется только в `RoomDatabase.Callback.onCreate` (fresh install path).**
 * Upgrade-path с v11 — через frozen seed `seedBuiltIns_v12()` в `Migration_011_to_012`
 * (F044 Best-guess A — frozen copy под M12-схему); upgrade с v12 → vM13
 * НЕ требует seed (built-in row уже существует, миграция M12→M13 ADD COLUMN
 * backfill'ит timestamps + is_multi для существующих rows).
 */
internal fun seedBuiltIns(connection: SQLiteConnection) {
    val now = System.currentTimeMillis()
    connection.execSQL(
        """
        INSERT OR IGNORE INTO component_types
            (system_key, dictionary_id, name, template_key, position,
             is_multi, created_at, updated_at, removed_at)
        VALUES ('translation', NULL, NULL, 'text', 0,
                0, $now, $now, NULL)
        """.trimIndent()
    )
}
```

**F044 Best-guess A — frozen seed для M11→M12** (см. #14 ниже): `Migration_011_to_012` получает свой `seedBuiltIns_v12()` приватный helper с literal SQL под M12-схему (без `is_multi/created_at/updated_at`, с `remove_date`). Это разделяет fresh install path (#11) от upgrade-path с v11 (#14).

#### #12 Database.kt [~]

**Было** (line 32): `version = 12`.

**Стало:**
```kotlin
@Database(
    entities = [
        // ... (без изменений в списке entities)
    ],
    version = 13                                                  // BUMP 12 → 13
)
```

### Tier 4: Migration

#### #13 Migration_012_to_013.kt [+]

Назначение: главная миграция фичи. Composite JSON rewrite + DDL changes + timestamps backfill + template-key consolidation. Использует bundled SQLite ≥ 3.45 `json_object` / `json_set` / `json_extract` (verified в `BundledSqliteFeatureTest`).

**Структура шагов** (10 шагов под M3 idempotency test через `failAfterStep`):

```kotlin
package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * IS481 component_constructor migration M12 → M13.
 *
 * Изменения схемы (только `component_types` + `component_values`):
 *   1. `component_types` — ADD `is_multi`, `created_at`, `updated_at`;
 *      RENAME `remove_date → removed_at`; DROP UNIQUE `(dictionary_id, name)`.
 *   2. `component_values` — ADD `created_at`, `updated_at`, `removed_at`;
 *      DROP UNIQUE `(lexeme_id, component_type_id)`; ADD non-unique `Index(lexeme_id)`.
 *   3. Template-key consolidation: `UPDATE component_types SET template_key='text'
 *      WHERE template_key='long_text'` (F046).
 *   4. JSON rewrite в `component_values.value`:
 *      - M12 `{"v":1,"text":"..."}` (text/long_text) → M13
 *        `{"fields":{"value":{"type":"text","value":"..."}}}`.
 *      - M12 `{"v":1,"uri":"..."}` (image) → M13
 *        `{"fields":{"value":{"type":"image","uri":"..."}}}`.
 *   5. Backfill `created_at/updated_at` = now() для existing rows; `removed_at`
 *      из старого `remove_date` для `component_types` (RENAME сохраняет значения).
 *
 * Critical порядок:
 *   1. ALTER TABLE component_types RENAME COLUMN remove_date TO removed_at;
 *   2. ALTER TABLE component_types ADD COLUMN is_multi (DEFAULT 0);
 *   3. ALTER TABLE component_types ADD COLUMN created_at + UPDATE backfill;
 *   4. ALTER TABLE component_types ADD COLUMN updated_at + UPDATE backfill;
 *   5. DROP INDEX index_component_types_dictionary_id_name (F028/F034);
 *   6. ALTER TABLE component_values ADD COLUMN created_at + backfill;
 *   7. ALTER TABLE component_values ADD COLUMN updated_at + backfill;
 *   8. ALTER TABLE component_values ADD COLUMN removed_at;
 *   9. DROP INDEX index_component_values_lexeme_id_component_type_id;
 *  10. CREATE INDEX index_component_values_lexeme_id (phantom после drop UNIQUE);
 *  11. Template-key consolidation `long_text → text`;
 *  12. JSON rewrite через UPDATE с json_object (idempotency-aware — skip rows
 *      уже в M13-формате).
 *
 * Room оборачивает `migrate()` в транзакцию автоматически.
 */
object Migration_012_to_013 : Migration(12, 13) {

    override fun migrate(connection: SQLiteConnection) {
        migrateImpl(connection, failAfterStep = null)
    }

    internal fun migrateImpl(connection: SQLiteConnection, failAfterStep: Int? = null) {
        renameComponentTypesRemoveDate(connection)
        maybeFail(1, failAfterStep)

        addComponentTypesNewColumns(connection)
        maybeFail(2, failAfterStep)

        dropUniqueComponentTypesDictName(connection)
        maybeFail(3, failAfterStep)

        addComponentValuesNewColumns(connection)
        maybeFail(4, failAfterStep)

        dropUniqueComponentValuesLexemeType(connection)
        maybeFail(5, failAfterStep)

        createComponentValuesLexemeIdIndex(connection)
        maybeFail(6, failAfterStep)

        consolidateLongTextTemplateKey(connection)
        maybeFail(7, failAfterStep)

        rewriteTextJson(connection)
        maybeFail(8, failAfterStep)

        rewriteImageJson(connection)
        maybeFail(9, failAfterStep)
    }

    private fun maybeFail(step: Int, fail: Int?) {
        if (fail != null && step == fail) throw MigrationTestFailureException(step)
    }

    private fun renameComponentTypesRemoveDate(c: SQLiteConnection) {
        c.execSQL("ALTER TABLE component_types RENAME COLUMN remove_date TO removed_at")
    }

    private fun addComponentTypesNewColumns(c: SQLiteConnection) {
        val now = System.currentTimeMillis()
        // SQLite не позволяет non-constant DEFAULT при ADD COLUMN NOT NULL —
        // двухшаговый подход: ADD nullable / UPDATE backfill / опционально ADD CHECK.
        c.execSQL("ALTER TABLE component_types ADD COLUMN is_multi INTEGER NOT NULL DEFAULT 0")
        c.execSQL("ALTER TABLE component_types ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0")
        c.execSQL("ALTER TABLE component_types ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
        c.execSQL("UPDATE component_types SET created_at = $now WHERE created_at = 0")
        c.execSQL("UPDATE component_types SET updated_at = $now WHERE updated_at = 0")
    }

    private fun dropUniqueComponentTypesDictName(c: SQLiteConnection) {
        c.execSQL("DROP INDEX IF EXISTS `index_component_types_dictionary_id_name`")
    }

    private fun addComponentValuesNewColumns(c: SQLiteConnection) {
        val now = System.currentTimeMillis()
        c.execSQL("ALTER TABLE component_values ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0")
        c.execSQL("ALTER TABLE component_values ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
        c.execSQL("ALTER TABLE component_values ADD COLUMN removed_at INTEGER")
        c.execSQL("UPDATE component_values SET created_at = $now WHERE created_at = 0")
        c.execSQL("UPDATE component_values SET updated_at = $now WHERE updated_at = 0")
    }

    private fun dropUniqueComponentValuesLexemeType(c: SQLiteConnection) {
        c.execSQL("DROP INDEX IF EXISTS `index_component_values_lexeme_id_component_type_id`")
    }

    private fun createComponentValuesLexemeIdIndex(c: SQLiteConnection) {
        c.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_component_values_lexeme_id` ON `component_values` (`lexeme_id`)"
        )
    }

    private fun consolidateLongTextTemplateKey(c: SQLiteConnection) {
        // F046 — без этого `fromKey('long_text')` (nullable) вернёт null
        // и rows молча скроются в UI.
        c.execSQL("UPDATE component_types SET template_key = 'text' WHERE template_key = 'long_text'")
    }

    private fun rewriteTextJson(c: SQLiteConnection) {
        // Idempotency: skip rows уже в M13-формате (json_extract возвращает NULL
        // если ключ отсутствует — на M13-формате json_extract(value,'$.text') = NULL).
        // M12: {"v":1,"text":"..."}; ключ "text" присутствует на верхнем уровне.
        // M13: {"fields":{"value":{"type":"text","value":"..."}}}; ключ "text"
        //      отсутствует на верхнем уровне (вложен через fields).
        c.execSQL(
            """
            UPDATE component_values
            SET value = json_object(
                    'fields',
                    json_object(
                        'value',
                        json_object('type', 'text', 'value', json_extract(value, '$.text'))
                    )
                ),
                updated_at = ${System.currentTimeMillis()}
            WHERE json_extract(value, '$.text') IS NOT NULL
              AND component_type_id IN (
                  SELECT id FROM component_types WHERE template_key = 'text'
              )
            """.trimIndent()
        )
    }

    private fun rewriteImageJson(c: SQLiteConnection) {
        c.execSQL(
            """
            UPDATE component_values
            SET value = json_object(
                    'fields',
                    json_object(
                        'value',
                        json_object('type', 'image', 'uri', json_extract(value, '$.uri'))
                    )
                ),
                updated_at = ${System.currentTimeMillis()}
            WHERE json_extract(value, '$.uri') IS NOT NULL
              AND component_type_id IN (
                  SELECT id FROM component_types WHERE template_key = 'image'
              )
            """.trimIndent()
        )
    }
}
```

Note: `MigrationTestFailureException` уже определён в `Migration_011_to_012.kt:256` — переиспользуется (`internal class` доступен в том же `migrations/` package).

**Edge-case handling** (per aspect `migration_edge_cases`):
- Malformed JSON row — `json_extract` возвращает NULL → row skip'ается в WHERE-clause. Row остаётся с raw M12 строкой; парсер на чтении (`parseTemplateValues`) логирует и пропускает.
- Unknown template key — после `consolidateLongTextTemplateKey` остаётся только `text/image`. Rewrite WHERE-clause не ловит unknown — row остаётся с M12 значением, fail-soft парсер на чтении вернёт null.
- Идемпотентность — обе rewrite-функции фильтруют `WHERE json_extract(value, '$.text') IS NOT NULL` либо `'$.uri'` (на M13-формате эти ключи nested внутри `fields.value` → top-level extract вернёт NULL → row skip).

#### #14 Migration_011_to_012.kt [~]

**Изменение** (F044 Best-guess A — frozen seed): заменить вызов `seedBuiltIns(connection)` (line 58) на inline private helper с M12-схемой:

**Было** (line 48-80, ключевая строка 58):
```kotlin
internal fun migrateImpl(connection: SQLiteConnection, failAfterStep: Int? = null) {
    createComponentTypesTable(connection)
    maybeFail(1, failAfterStep)
    ...
    seedBuiltIns(connection)                                     // line 58 — обращение к общему seed
    maybeFail(4, failAfterStep)
    ...
}
```

**Стало:**
```kotlin
internal fun migrateImpl(connection: SQLiteConnection, failAfterStep: Int? = null) {
    createComponentTypesTable(connection)
    maybeFail(1, failAfterStep)
    ...
    seedBuiltIns_v12(connection)                                 // CHANGED — frozen M12 seed
    maybeFail(4, failAfterStep)
    ...
}

/**
 * F044 frozen seed под M12 schema (без `is_multi/created_at/updated_at`,
 * с `remove_date`). Выполняется только на upgrade-path с v11.
 *
 * После M11→M12 + M12→M13 цепочки result в `component_types` row
 * получит `removed_at` (RENAME), `is_multi=0`, `created_at=updated_at=now()`
 * через M12→M13 миграцию — конечная схема валидна.
 */
private fun seedBuiltIns_v12(connection: SQLiteConnection) {
    connection.execSQL(
        """
        INSERT OR IGNORE INTO component_types
            (system_key, dictionary_id, name, template_key, position, remove_date)
        VALUES ('translation', NULL, NULL, 'text', 0, NULL)
        """.trimIndent()
    )
}
```

Также — заменить `import me.apomazkin.core_db_impl.room.seedBuiltIns` на использование privatе helper (либо оставить import + перестать на него ссылаться — компилятор предупредит unused-import).

Также аналогичный freeze для `migrateTranslationData` / `migrateDefinitionData` (lines 181-211) **НЕ требуется** — эти запросы пишут в `component_values.value` M12-формат (`json_object('v',1,'text',...)`), и M11→M12 для свежего апгрейда оставляет данные в M12-формате; затем M12→M13 их перепишет в M13-формат (через `rewriteTextJson`). Цепочка идемпотентна.

#### #15 13.json [+]

Назначение: Room schema export для validation `MigrationTestHelper.runMigrationsAndValidate(13, ...)`. Файл генерируется автоматически KSP при сборке после bump `version = 13` + добавления новых полей в `@Entity`.

Содержит pre-computed схему `component_types / component_values / quiz_configs / dictionaries / lexemes / words / write_quiz / hints / samples / sample_to_definition` с новыми колонками. Имена индексов:
- `index_component_types_dictionary_id` (non-unique).
- `index_component_types_system_key` (UNIQUE).
- `index_component_values_component_type_id` (non-unique).
- `index_component_values_lexeme_id` (non-unique, NEW).
- (`index_component_types_dictionary_id_name` UNIQUE — **dropped**).
- (`index_component_values_lexeme_id_component_type_id` UNIQUE — **dropped**).

Note: schema-export генерируется KSP — на `data_implement` шаге запуск `./gradlew :core:core-db-impl:compileDebugKotlin` создаст файл. На `data_design_tree` фиксируется как узел graph'а; реальный JSON-контент — output build'а.

### Tier 5: DI

#### #16 RoomModule.kt [~]

Закрывает узел id 35 infra-tree (отложенный — реализуется здесь, после создания `Migration_012_to_013.kt`).

**Было** (line 50):
```kotlin
.addMigrations(Migration_011_to_012)
```

**Стало:**
```kotlin
.addMigrations(Migration_011_to_012, Migration_012_to_013)
```

+ добавить import:
```kotlin
import me.apomazkin.core_db_impl.room.migrations.Migration_012_to_013
```

Также обновить docstring (lines 22-36): `v12 → v13`, описать новый upgrade-path (v11→v12→v13 цепочка для legacy юзеров) + новые ADD COLUMN / DROP UNIQUE.

### Tier 6: CoreDbApiImpl

#### #17 CoreDbApiImpl.kt [~]

Реализация 5 BREAKING + 6 NEW методов `LexemeApi`. Контракт зафиксирован в `CoreDbApi.kt` (business-фаза); здесь — impl.

**Изменения:**

1. **5 BREAKING сигнатур** (`addLexemeWithBuiltInComponent` / `addLexemeWithUserDefinedComponent` / `addLexemeWithComponents` / `addComponentValue` / `updateComponentValue`) — параметр `data: ComponentValueData → TemplateValues`. Каждое тело:
   - `data.toJson()` остаётся вызов (новый mapper `TemplateValues.toJson()`).
   - `addComponentValue` обязан использовать `componentValueDao.insertSingleSafe(value, isMulti)` — lookup `isMulti` через `componentTypeDao.getById(componentTypeId)`.
   - `updateComponentValue` обновляет `updated_at = now()`.
   - `@Deprecated addLexemeWithTranslation` / `updateLexemeTranslation` — переписать обращения к `ComponentValueData.TextValue(...)` на `TextValues(Primitive.Text(...))`.

   Пример `addComponentValue` (F172 — обёрнут в `database.withTransaction { ... }`
   для TOCTOU guard: lookup type + cardinality check + insert — атомарно):
   ```kotlin
   override suspend fun addComponentValue(
       lexemeId: Long,
       componentTypeId: Long,
       data: TemplateValues,
   ): Long = database.withTransaction {
       val type = componentTypeDao.getById(componentTypeId)
           ?: error("Type not found: $componentTypeId")
       // F170: explicit guard на soft-deleted type
       check(type.removedAt == null) {
           "Cannot insert ComponentValue for soft-deleted type $componentTypeId"
       }
       val now = Date(System.currentTimeMillis())
       componentValueDao.insertSingleSafe(
           value = ComponentValueDb(
               lexemeId = lexemeId,
               componentTypeId = componentTypeId,
               value = data.toJson(),
               createdAt = now,
               updatedAt = now,
           ),
           isMulti = type.isMulti,                                  // F169 cardinality enforced в DAO
       )
   }
   ```
   `updateComponentValue` — аналогично обёрнут в `database.withTransaction { ... }`
   с F170 guard после `getById(componentTypeId)`.

   **F171 pre-check для `addLexemeWithComponents`** — выполняется в `LexemeApiImpl`
   (а не в `WordDao` — Room `@Dao interface` не имеет cross-DAO access):
   ```kotlin
   override suspend fun addLexemeWithComponents(
       lexemeDb: LexemeDb,
       dictionaryId: Long,
       components: List<ComponentValueDb>,
   ): LexemeApiEntity = database.withTransaction {
       // F169 / F170 pre-check: lookup type metadata через componentTypeDao
       val typeGroups = components.groupBy { it.componentTypeId }
       for ((typeId, group) in typeGroups) {
           val type = componentTypeDao.getById(typeId)
               ?: throw IllegalStateException("Type not found: $typeId")
           check(type.removedAt == null) {
               "Cannot insert ComponentValue for soft-deleted type $typeId"
           }
           check(type.isMulti || group.size <= 1) {
               "Cardinality violation: type $typeId is_multi=false, но передано ${group.size} values"
           }
       }
       // cascading INSERTs делегируются WordDao (см. #10)
       val newLexemeId = wordDao.addLexemeWithComponents(lexemeDb, dictionaryId, components)
       wordDao.getLexemeById(newLexemeId)!!.toApiEntity(logger)
   }
   ```

2. **6 NEW методов**:

   - `flowAllUserDefinedTypesWithUsage(): Flow<UserDefinedTypesUsageSnapshot>`:
     ```kotlin
     override fun flowAllUserDefinedTypesWithUsage(): Flow<UserDefinedTypesUsageSnapshot> =
         combine(
             componentTypeDao.flowAllUserDefined(),
             wordDao.flowDictionaries(),                         // ↓ источник для names (F168: ctor-consistent)
         ) { types, dicts ->
             val typesApi = types.mapNotNull { it.toApiEntity() }
             val countsList = componentValueDao.aggregatedValueCountPerType()
             val dictPairs = componentValueDao.typeDictPairs()
             UserDefinedTypesUsageSnapshot(
                 types = typesApi,
                 valueCountByType = countsList.associate { it.typeId to it.count },
                 dictionaryIdsByType = dictPairs
                     .groupBy { it.typeId }
                     .mapValues { entry -> entry.value.map { it.dictId }.toSet() },
                 dictionaryNames = dicts.associate { it.id to it.name },
             )
         }
     ```
     Note: combine emits на каждое изменение types либо dicts; aggregated counts/pairs читаются suspend внутри transform — это означает что snapshot будет slightly stale между emit'ами. Если требуется строгая reactivity на counts — `flowAllConfigs()`-style Flow на cv-таблице (но это требует `@Query("SELECT ... FROM component_values")` flow + downstream aggregation в Kotlin). Финализация — на `data_implement` с benchmark.

   - `flowUserDefinedTypesForDictionary(dictId)`:
     ```kotlin
     override fun flowUserDefinedTypesForDictionary(dictionaryId: Long) =
         componentTypeDao.flowUserDefinedForDictionary(dictionaryId).map { types ->
             val typesApi = types.mapNotNull { it.toApiEntity() }
             val counts = componentValueDao.aggregatedValueCountPerTypeForDict(dictionaryId)
             val dictName = wordDao.getDictionaryById(dictionaryId)?.name.orEmpty()
             DictionaryTypesSnapshot(
                 dictionaryId = dictionaryId,
                 dictionaryName = dictName,
                 types = typesApi,
                 valueCountByType = counts.associate { it.typeId to it.count },
             )
         }
     ```

   - `createUserDefinedComponent(name, template, isMulti, scope): CreateComponentOutcome`:
     ```kotlin
     override suspend fun createUserDefinedComponent(
         name: String,
         template: ComponentTemplate,
         isMulti: Boolean,
         scope: Scope,
     ): CreateComponentOutcome {
         // 1. two-prong SELECT (per aspect userdefined_identity_invariant)
         val sameScope = when (scope) {
             Scope.Global -> componentTypeDao.findActiveGlobalByName(name)
             is Scope.PerDictionaries -> scope.ids.firstNotNullOfOrNull {
                 componentTypeDao.findActiveUserDefinedByName(it, name)
             }
         }
         if (sameScope != null) return CreateComponentOutcome.SameScopeCollision

         val crossScope = when (scope) {
             Scope.Global -> componentTypeDao.countActivePerDictByName(name) > 0
             is Scope.PerDictionaries -> componentTypeDao.findActiveGlobalByName(name) != null
         }
         if (crossScope) return CreateComponentOutcome.CrossScopeCollision

         // 2. INSERT N rows в одной транзакции
         val now = Date(System.currentTimeMillis())
         val newRows = when (scope) {
             Scope.Global -> listOf(
                 ComponentTypeDb(
                     systemKey = null, dictionaryId = null, name = name,
                     templateKey = template.key, position = 0,
                     isMulti = isMulti, createdAt = now, updatedAt = now,
                 )
             )
             is Scope.PerDictionaries -> scope.ids.map { dictId ->
                 ComponentTypeDb(
                     systemKey = null, dictionaryId = dictId, name = name,
                     templateKey = template.key, position = 0,
                     isMulti = isMulti, createdAt = now, updatedAt = now,
                 )
             }
         }
         // F173: atomic insert N rows через database.withTransaction { ... }
         val inserted = database.withTransaction {
             newRows.map { row ->
                 val id = componentTypeDao.insert(row)
                 row.copy(id = id)
             }
         }.mapNotNull { it.toApiEntity() }
         return CreateComponentOutcome.Success(inserted)
     }
     ```
     **F173 (iter 3):** wrap через `database.withTransaction { ... }` (cross-DAO operations требуют RoomDatabase context — `@Dao` default-method не имеет доступа к другим DAO). Единый convention для всех multi-DAO транзакций в `LexemeApiImpl`.

   - `renameComponentType(typeId, newName)`:
     ```kotlin
     override suspend fun renameComponentType(
         typeId: Long,
         newName: String,
     ): RenameComponentOutcome {
         val existing = componentTypeDao.getById(typeId) ?: return RenameComponentOutcome.BuiltInProtected
         if (existing.systemKey != null) return RenameComponentOutcome.BuiltInProtected

         // Collision checks — те же two-prong что в create
         val sameScope = if (existing.dictionaryId != null) {
             componentTypeDao.findActiveUserDefinedByName(existing.dictionaryId, newName)
         } else {
             componentTypeDao.findActiveGlobalByName(newName)
         }
         if (sameScope != null && sameScope.id != typeId) return RenameComponentOutcome.SameScopeCollision

         val crossScope = if (existing.dictionaryId != null) {
             componentTypeDao.findActiveGlobalByName(newName) != null
         } else {
             componentTypeDao.countActivePerDictByName(newName) > 0
         }
         if (crossScope) return RenameComponentOutcome.CrossScopeCollision

         val now = Date(System.currentTimeMillis())
         val oldName = existing.name ?: error("user-defined без name: typeId=$typeId")
         // F173: atomic rename + cascade через database.withTransaction { ... }
         renameWithQuizConfigsCascade(typeId, newName, oldName, now)

         val updated = componentTypeDao.getById(typeId)!!.toApiEntity()
             ?: return RenameComponentOutcome.BuiltInProtected
         return RenameComponentOutcome.Success(updated)
     }

     /** F173: wrap в `database.withTransaction { ... }` (cross-DAO). */
     private suspend fun renameWithQuizConfigsCascade(
         typeId: Long, newName: String, oldName: String, now: Date,
     ) = database.withTransaction {
         componentTypeDao.renameUserDefined(typeId, newName, now)
         // Cascade — все configs где `user:<oldName>` появляется
         quizConfigDao.getAllConfigs().forEach { config ->
             val refs = config.componentRefs.toComponentTypeRefList()
             val updated = refs.map { ref ->
                 if (ref is ComponentTypeRef.UserDefined && ref.name == oldName) {
                     ComponentTypeRef.UserDefined(newName)
                 } else ref
             }
             if (updated != refs) {
                 quizConfigDao.updateComponentRefs(config.id, updated.toJson())
             }
         }
     }
     ```
     **F173 (iter 3):** `database.withTransaction { ... }` — cross-DAO (componentTypeDao + quizConfigDao) требует RoomDatabase context.

   - `previewDeletionImpact(typeId): DeletionImpact?`:
     ```kotlin
     override suspend fun previewDeletionImpact(typeId: Long): DeletionImpact? {
         val type = componentTypeDao.getById(typeId) ?: return null
         if (type.systemKey != null) return null                  // built-in не удаляется

         val valueCount = componentValueDao.countActiveByTypeId(typeId)
         val dictIds = componentValueDao.dictionaryIdsForTypeId(typeId)

         val affectedConfigs = quizConfigDao.getAllConfigs().mapNotNull { config ->
             val refs = config.componentRefs.toComponentTypeRefList()
             val refName = type.name
             if (refName != null && refs.any { it is ComponentTypeRef.UserDefined && it.name == refName }) {
                 AffectedQuizConfig(
                     dictionaryId = config.dictionaryId,
                     quizMode = config.quizMode,
                 )
             } else null
         }

         // affectedPrefs — UseCase-уровень (DataStore вне Room, см. F049 Best-guess B).
         // DAO/Repo возвращает affectedQuizConfigs.dictionaryIds — UseCase
         // пересечёт с фактическими pref-keys через DictionaryApi.getDictionaryList().
         return DeletionImpact(
             valueCount = valueCount,
             dictionariesWithValues = dictIds,
             affectedQuizConfigs = affectedConfigs,
             affectedPrefs = affectedConfigs.map { it.dictionaryId }.distinct(),
         )
     }
     ```
     Note: `affectedPrefs` — изначально вычисляется data-уровнем как «словари с references в `quiz_configs.component_refs`»; UseCase проверит фактический `quiz_picker_dict_<id>` pref на match (F049 Best-guess B). Альтернатива — оставить `affectedPrefs = emptyList()` в data, и UseCase сам сосчитает; либо передавать `affectedPrefs = dictIds` ─ финализация — `data_implement`.

   - `softDeleteComponentType(typeId): SoftDeleteComponentOutcome`:
     ```kotlin
     override suspend fun softDeleteComponentType(typeId: Long): SoftDeleteComponentOutcome {
         val type = componentTypeDao.getById(typeId) ?: return SoftDeleteComponentOutcome.BuiltInProtected
         if (type.systemKey != null) return SoftDeleteComponentOutcome.BuiltInProtected

         // 1. Получить impact ДО soft-delete (active values считаются)
         val impact = previewDeletionImpact(typeId) ?: return SoftDeleteComponentOutcome.BuiltInProtected

         val now = Date(System.currentTimeMillis())
         // F173: atomic soft-delete + cascade через database.withTransaction { ... }
         softDeleteAtomic(typeId, type.name, now)

         return SoftDeleteComponentOutcome.Success(impact)
     }

     /** F173: wrap в `database.withTransaction { ... }` (cross-DAO). */
     private suspend fun softDeleteAtomic(typeId: Long, oldName: String?, now: Date) =
         database.withTransaction {
             componentTypeDao.softDelete(typeId, now)
             componentValueDao.softDeleteByTypeId(typeId, now)    // cascade soft-delete values
             if (oldName != null) {
                 quizConfigDao.getAllConfigs().forEach { config ->
                     val refs = config.componentRefs.toComponentTypeRefList()
                     val filtered = refs.filter {
                         !(it is ComponentTypeRef.UserDefined && it.name == oldName)
                     }
                     if (filtered != refs) {
                         quizConfigDao.updateComponentRefs(config.id, filtered.toJson())
                     }
                 }
             }
         }
     ```

3. **`getLexemeById` update** — `wordDao.getLexemeById(id)?.toApiEntity()` → пробросить logger в `toApiEntity(logger)`:
   ```kotlin
   override suspend fun getLexemeById(id: Long): LexemeApiEntity? =
       wordDao.getLexemeById(id)?.toApiEntity(logger)
   ```

4. **`getComponentTypes` update** — `mapNotNull` вместо `map` (skip null от fail-soft `toApiEntity()`):
   ```kotlin
   override suspend fun getComponentTypes(dictionaryId: Long): List<ComponentTypeApiEntity> =
       componentTypeDao.getTypesForDictionary(dictionaryId).mapNotNull { it.toApiEntity() }
   ```

5. **ctor injection**: `LexemeApiImpl` уже принимает `componentTypeDao` / `componentValueDao` / `quizConfigDao` / `wordDao` / `logger`. Для `flowAllUserDefinedTypesWithUsage` нужен `dictionaryDao` либо ссылка на `wordDao.flowDictionaries()` (существует на line 70). Используем `wordDao.flowDictionaries()` (без отдельного `DictionaryDao`).

   **F174 (iter 4):** добавить новую ctor-зависимость для F172/F173 `database.withTransaction { ... }` обёрток:
   ```kotlin
   private val database: Database     // me.apomazkin.core_db_impl.room.Database (verified, не AppDatabase)
   ```
   `Database` — `abstract class Database : RoomDatabase()` из `core/core-db-impl/.../room/Database.kt`. Required by `androidx.room.withTransaction` (extension on RoomDatabase). DI wiring — обновить `RoomModule` / `ApiModule` (`provideLexemeApi`) пробросить `database` instance в `LexemeApiImpl` constructor.

6. **Imports**: добавить
   ```kotlin
   import androidx.room.withTransaction                          // F174 (iter 4) — для database.withTransaction { ... }
   import kotlinx.coroutines.flow.combine
   import kotlinx.coroutines.flow.map
   import me.apomazkin.core_db_api.entity.CreateComponentOutcome
   import me.apomazkin.core_db_api.entity.DictionaryTypesSnapshot
   import me.apomazkin.core_db_api.entity.RenameComponentOutcome
   import me.apomazkin.core_db_api.entity.SoftDeleteComponentOutcome
   import me.apomazkin.core_db_api.entity.UserDefinedTypesUsageSnapshot
   import me.apomazkin.core_db_impl.mapper.parseTemplateValues
   import me.apomazkin.core_db_impl.mapper.toJson  // TemplateValues.toJson
   import me.apomazkin.core_db_impl.room.Database                // F174 (iter 4) — ctor dep для withTransaction
   import me.apomazkin.lexeme.AffectedQuizConfig
   import me.apomazkin.lexeme.ComponentTemplate
   import me.apomazkin.lexeme.ComponentTypeRef
   import me.apomazkin.lexeme.DeletionImpact
   import me.apomazkin.lexeme.Primitive
   import me.apomazkin.lexeme.Scope
   import me.apomazkin.lexeme.TemplateValues
   import me.apomazkin.lexeme.TextValues
   ```
   Removed:
   ```kotlin
   import me.apomazkin.lexeme.ComponentValueData               // domain symbol удалён
   import me.apomazkin.core_db_impl.mapper.toComponentValueData
   ```

### Tier 7: Final domain cleanup

#### #18 ComponentValueData.kt [-]

**Узел перенесён из `business_design_tree.md` #6**. На business-фазе удаление было отложено — символ ещё ссылался из data (`ComponentValueDataJson.kt` → #2, `ComponentValueWithType.toApiEntity` → #5, `CoreDbApiImpl` deprecated shim → #17). После Tier 0-6 закрытия (все data callsite'ы переписаны на `TemplateValues`) — символ можно удалить.

Действие: `git rm /Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentValueData.kt`.

Verify cleanup:
```bash
./scripts/cc-build.sh assembleDebug
./scripts/cc-build.sh :modules:domain:lexeme:testDebugUnitTest
```

Если builds зелёный — символ безопасно удалён.

### Tier 8: Tests

#### #19 MigrationFrom12to13.kt [+]

Назначение: instrumented migration test для M12→M13. Паттерн — `MigrationFrom11to12.kt` (см. `MigrationFrom11to12.kt:26-100`).

**Кейсы** (под `data_migration_test.md` для IS481 — будут детализированы на `data_test` шаге; здесь — high-level):
- **A. Translation-only** — built-in `translation` row получает `is_multi=0, created_at=updated_at=now, removed_at=NULL`; existing `component_values` row рерайтится в M13-формат.
- **B. User-defined "Definition" с text payload** — JSON rewrite корректен; `is_multi=0` backfill.
- **C. User-defined image-template** — `"uri"` → `{"fields":{"value":{"type":"image","uri":"..."}}}`.
- **D. `long_text` template consolidation** — type получает `template_key='text'`; values остаются с тем же payload (text rewrite их подхватит).
- **E. Removed type (M12 soft-deleted `remove_date != NULL`)** — `remove_date` → `removed_at` сохраняет значение; type остаётся soft-deleted.
- **F. UNIQUE drop check** — после M13 INSERT двух rows с одинаковым `(dictionary_id, name)` не падает.
- **G. UNIQUE drop check (component_values)** — INSERT двух rows с одинаковым `(lexeme_id, component_type_id)` не падает.
- **H. Malformed JSON row** — остаётся в M12-формате (rewrite skip), последующее чтение через парсер вернёт `null`.
- **I. Phantom Index(lexeme_id)** — `EXPLAIN QUERY PLAN` показывает использование индекса при `WHERE lexeme_id = ?`.
- **J. Timestamps backfill** — все existing rows имеют `created_at > 0` и `updated_at > 0`.

```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationFrom12to13 {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val dbFile = instrumentation.targetContext.getDatabasePath(DB_NAME)

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = dbFile,
        driver = BundledSQLiteDriver(),
        databaseClass = Database::class,
    )

    @Test
    fun caseA_translationOnly() {
        helper.createDatabase(12).use { v12 ->
            v12.insertDictionary(1, "EN")
            v12.insertWord(1, 1, "cat")
            v12.insertLexeme(1, 1)
            v12.execSQL(
                "INSERT INTO component_values (id, lexeme_id, component_type_id, value) " +
                    "VALUES (1, 1, (SELECT id FROM component_types WHERE system_key='translation'), " +
                    "'{\"v\":1,\"text\":\"кошка\"}')"
            )
        }
        val v13 = helper.runMigrationsAndValidate(13, listOf(Migration_012_to_013))

        // JSON rewrite
        assertEquals(
            """{"fields":{"value":{"type":"text","value":"кошка"}}}""",
            v13.scalarText("SELECT value FROM component_values WHERE id=1")
        )
        // Timestamps backfill
        assertTrue(v13.scalarLong("SELECT created_at FROM component_values WHERE id=1") > 0)
        assertTrue(v13.scalarLong("SELECT created_at FROM component_types WHERE system_key='translation'") > 0)
        v13.close()
    }
    // ... остальные кейсы B-J
}
```

`insertDictionary/insertWord/insertLexeme` — переиспользуем helper extensions из `MigrationFrom11to12.kt` (либо вынести в common `MigrationTestExt.kt`).

#### #20 MigrationFrom12to13IdempotencyTest.kt [+]

Паттерн — `MigrationFrom11to12IdempotencyTest.kt`. Phase 1 — inject failure через `Migration_012_to_013.migrateImpl(connection, failAfterStep = N)` → v12 остаётся целый; Phase 2 — retry без injection → v13 валиден. Кейсы — `failAfterStep = 1..9` (9 шагов миграции в #13).

#### #21 TemplateValuesJsonTest.kt [+]

Unit test для нового маппера (нет instrumented Room — чистый JUnit на JVM):
- Round-trip `TextValues → toJson() → parseTemplateValues() == TextValues`.
- Round-trip `ImageValues → toJson() → parseTemplateValues() == ImageValues`.
- Schema mismatch: parse `image` JSON с `template=TEXT` → returns `null` + logger.e called.
- Malformed JSON → returns `null` + logger.e called.
- Unknown primitive type (`{"type":"video"}`) → returns `null` + logger.e called.
- Golden fixtures (см. #22, #23) — load файлы, проверить parsing.

`logger` — `FakeLexemeLogger` (test double, фиксирует вызовы `.e(...)` для assertions).

#### #22, #23 fixtures/component_values/*.json [+]

Golden round-trip fixtures (aspect `mapper_golden_fixtures`). Файлы:
- `text_value.json`: `{"fields":{"value":{"type":"text","value":"Sample"}}}`.
- `image_value.json`: `{"fields":{"value":{"type":"image","uri":"file:///tmp/img.jpg"}}}`.

Future: composite values (когда добавятся multi-field templates) — добавятся новые fixture-файлы без изменений в существующих (template-immutability gate).

---

## Часть 3: UI/Business dependencies

Этот граф data-слоя имеет следующие downstream dependencies (не входят в DAG, но указаны для ориентации):

| Узел data | Downstream consumers (business / UI / app) |
|---|---|
| #1 TemplateValuesJson | `LexemeApiImpl` (#17), `ComponentValueWithType` (#5) — внутри data. Outside — `LexemeMapper.kt` (business #52) уже импортирует только domain типы (`TextValues / Primitive`). |
| #3-#4 entity timestamps | `ComponentTypeApiEntity` / `ComponentValueApiEntity` (business #19, #20) — `createdAt/updatedAt/removedAt` пробрасываются. UI uses `removedAt` для skip-rows, остальные timestamps не отображаются в IS481 MVP. |
| #5 ComponentValueWithType signature change (logger param) | `LexemeApiImpl.getLexemeById` (#17) пробрасывает logger. Других callsite'ов нет. |
| #6 LexemeDbEntity post-load filter | `LexemeApiImpl.getLexemeById` (#17). Soft-deleted values теперь невидимы → UI / quiz / wordcard всегда видят только active. |
| #7-#8 DAO methods | Используются ТОЛЬКО `LexemeApiImpl` (#17). UseCase impl (`ComponentsManagerUseCaseImpl` / `PerDictionaryComponentsUseCaseImpl` — business #27, #28) не имеют прямого DAO доступа — общается через `CoreDbApi.LexemeApi` interface. |
| #13 Migration_012_to_013 | Зарегистрирована в `RoomModule.kt` (#16). Старые юзеры с v12 проходят миграцию при первом запуске после установки IS481. |
| #17 CoreDbApiImpl | Контракт `LexemeApi` (5 BREAKING + 6 NEW) уже был зафиксирован business-фазой; здесь — impl. UseCase impl (`ComponentsManagerUseCaseImpl`) использует 6 NEW методов; wordcard / quiz / app-mapper используют 5 BREAKING. |
| #18 delete ComponentValueData.kt | После удаления символа — `:modules:domain:lexeme` экспортирует только `TemplateValues / Primitive / Field`. Все consumers (business / UI / data) уже rebind'нуты. |

---

## Часть 4: Аудит

### 4.1 Покрытие § Затронутые файлы → Data layer из `02_scope.md`

| Файл из scope | Узел графа | Статус |
|---|---|---|
| `core/core-db-impl/.../room/Database.kt` (`version 12→13`) | #12 | ok |
| `Migration_012_to_013.kt` (новый) | #13 | ok |
| `RoomModule.kt` (`addMigrations(...)`) | #16 | ok |
| `entity/ComponentTypeDb.kt` (rename + new fields) | #3 | ok |
| `entity/ComponentValueDb.kt` (add timestamps + drop UNIQUE) | #4 | ok |
| `entity/LexemeDbEntity.kt` (filter soft-deleted) | #6 | ok |
| `entity/ComponentValueWithType.kt` (toApiEntity rebind) | #5 | ok |
| `room/dao/ComponentTypeDao.kt` (soft-delete audit + new queries) | #7 | ok |
| `room/dao/ComponentValueDao.kt` (filter + insertSingleSafe + aggregates) | #8 | ok |
| `room/dao/QuizConfigDao.kt` (cleanup methods) | #9 | ok |
| `mapper/ComponentValueDataJson.kt` (replace) | #1 (TemplateValuesJson) + #2 (delete) | ok |
| `CoreDbApiImpl.kt` (~10 callsites + 6 new methods) | #17 | ok |
| `room/SeedBuiltIns.kt` (M13 schema seed) | #11 + #14 (frozen v12 seed) | ok |
| `core/core-db-impl/schemas/.../13.json` | #15 | ok |
| `core-db-api/CoreDbApi.kt` (5 BREAKING + 6 NEW) | business-фаза (закрыто) | ok |
| `core-db-api/entity/ComponentTypeApiEntity.kt` | business-фаза | ok |
| `core-db-api/entity/ComponentValueApiEntity.kt` | business-фаза | ok |
| `SampleApiEntity.kt / WordApiEntity.kt` (НЕ трогать F002) | — | ok (нет узла) |
| `WordDao.kt` (addLexemeWithComponents adjustments) | #10 | ok |
| `modules/datasource/prefs/PrefsProvider.kt` (Option B — НЕ трогать) | — | ok (нет узла; UseCase композиция в business #27) |
| `QuizPickerPrefKey.kt` (helper, не меняется) | — | ok (нет узла) |
| `QuizPickerFlowHandler.kt` (consumer stale-ref) | вне data — UI/business sub-flow | ok |
| Domain (`ComponentTemplate/ComponentType/ComponentValue/TemplateValues/Primitive/Field/PrimitiveType` + `ComponentValueData` delete) | business-фаза (Tier 0) + #18 (delete) | ok |
| `Migration_011_to_012.kt` frozen seed (F044) | #14 | ok (новый узел для F044) |

### 4.2 Покрытие aspects из `02_scope.md`

| Aspect | Узлы | Статус |
|---|---|---|
| `db_migration` | #13 (DDL), #11/#14 (seed), #15 (schema export), #12 (version bump) | ok |
| `migration_edge_cases` | #13 (WHERE filter на rewrite, idempotency через json_extract NULL skip) | ok |
| `migration_timestamps_backfill` | #13 (UPDATE backfill после ADD COLUMN) | ok |
| `public_contract_change` | #17 (5 BREAKING + 6 NEW impl); API уже расширен business-фазой | ok |
| `domain_rewrite` | #18 (final delete) — остальное на business-фазе | ok |
| `dao_convention` | #7 (rename remove_date → removed_at в queries), #8 (add removed_at filter), #6 (post-load filter в LexemeDbEntity) | ok |
| `cardinality_safety` | #8 (`insertSingleSafe @Transaction`) | ok |
| `soft_delete_unique_collision` | #7 (`findActiveUserDefinedByName` / `findActiveGlobalByName`), #17 (two-prong SELECT в `createUserDefinedComponent`) | ok |
| `userdefined_identity_invariant` | #7 (`countActivePerDictByName`), #17 (cross-scope check в create/rename) | ok |
| `quiz_configs_cleanup` | #9 (`updateComponentRefs`), #17 (`softDeleteAtomic` + `renameWithQuizConfigsCascade`) | ok |
| `prefs_cleanup_on_soft_delete` | #17 `previewDeletionImpact.affectedPrefs` = candidate dictIds; финальный pref reset — business UseCase (`ComponentsManagerUseCaseImpl.resetQuizPickerPrefsFor`, business #27) | ok |
| `forward_compat_unknown` / `parser_fail_soft` | #1 (fail-soft `parseTemplateValues`), #3 (nullable `toApiEntity`), #5 (skip null) | ok |
| `mapper_golden_fixtures` | #22, #23 | ok |
| `transactional_save_lexeme` | #10 (`addLexemeWithComponents` @Transaction уже есть; signature update) | ok |

### 4.3 Покрытие Open Questions из `02_scope.md`

| Open Q | Решение / узел | Status |
|---|---|---|
| Новый `ComponentApi` vs `LexemeApi` | Расширить `LexemeApi` (best-guess) — business-фаза закрыла | ok |
| Один screen-module vs два | Два — infra-фаза закрыла | ok |
| `ComponentBlock` wrapper location | ui_design_tree | вне data |
| Storage migration SQL vs Kotlin | **SQL `json_object` / `json_extract`** (#13) — bundled SQLite 3.45 verified. Альтернатива (Kotlin loop) — НЕ выбрана; SQL быстрее для 25k+ rows. | ok |
| `SettingsNavigator` reuse | infra-фаза закрыла | вне data |
| UNIQUE collision при пересоздании soft-deleted | **Option B** — DROP UNIQUE в Room + enforce в UseCase via `findActiveUserDefinedByName/findActiveGlobalByName` (#7, #17). Партиционный UNIQUE отброшен (Room не поддерживает partial index в schema export). | ok |
| `template` nullability стратегия | **Option B** — `toApiEntity()` returns nullable, skip row (#3, #5) | ok |
| Cross-scope identity invariant | **Option A** — two-prong SELECT в `createUserDefinedComponent` / `renameComponentType` (#17). Option B (обогащение `ComponentTypeRef.UserDefined.typeId`) — отброшен (breaking change в JSON `quiz_configs.component_refs` + prefs migration). | ok |
| `LexemeDbEntity.componentValueListDb` filter | **Option A** — post-load filter в `toApiEntity()` (#6). Option B (custom @Query) — отброшен (больше изменений; post-load filter покрывает все callsite'ы единообразно). | ok |
| Prefs cleanup strategy | **Option B** — UseCase композиция через `DictionaryApi.getDictionaryList()` (business #27). `PrefsProvider` НЕ расширяется новыми API. | ok |
| `seedBuiltIns` schema mismatch на upgrade-path | **Option A** — frozen `seedBuiltIns_v12()` в `Migration_011_to_012` (#14); current `SeedBuiltIns.kt` (#11) — под M13 schema (fresh install path) | ok |

### 4.4 Циклы / порядок зависимостей

- Tier 0 (#1, #2) — `TemplateValuesJson` (новый) → удаление `ComponentValueDataJson` (старого). Узел #2 depends [#1] — символ нужен перед удалением старого.
- Tier 1 (#3-#6) — entity changes; #3/#4 — листья (только новые колонки + drop UNIQUE), #5 зависит от #1, #3, #4 (`toApiEntity` маппер использует и тот, и тот), #6 зависит от #5.
- Tier 2 (#7-#10) — DAO updates; #7 зависит от #3 (queries на новые column'ы), #8 зависит от #4, #9 — leaf, #10 зависит от #4 (signature update).
- Tier 3 (#11, #12) — seed under M13 schema (#11 depends [#3]) + Database version bump (#12 depends [#3, #4]).
- Tier 4 (#13-#15) — миграция (#13 depends [#11] — frozen seed pattern, либо вообще independent: миграция сама ALTER'ит существующие rows, seed только для fresh install). Поправлено: #13 depends [#11] — для **знания** M13 schema (важен порядок: схема через @Entity (#3, #4) → @Database version (#12) → миграция (#13) валидирует transition к новой схеме). #14 (Migration_011_to_012 frozen seed) — leaf; #15 (schema export) depends [#3, #4, #12] — генерируется KSP.
- Tier 5 (#16) — RoomModule registration; depends [#12, #13] — оба должны существовать.
- Tier 6 (#17) — `CoreDbApiImpl` impl; depends [#1, #3-#10] — всё (mapper + entity + DAO).
- Tier 7 (#18) — final delete `ComponentValueData.kt`; depends [#1, #2, #17] — после полного rebind всех data callsite'ов.
- Tier 8 (#19-#23) — тесты; #19 (instrumented migration test) depends [#13, #15], #20 depends [#13], #21 depends [#1], #22/#23 — leaves (fixtures).

**Циклов нет.** Каждый узел реализуется только когда зависимости готовы.

### 4.5 Verify existing files

Проверено через Read tool:
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/Database.kt` — version=12 (line 32).
- `/Users/kilg/.../core_db_impl/room/migrations/Migration_011_to_012.kt` — существует; `seedBuiltIns` вызов на line 58.
- `/Users/kilg/.../core_db_impl/room/SeedBuiltIns.kt` — текущий SQL под M12 schema.
- `/Users/kilg/.../core_db_impl/di/module/RoomModule.kt` — `.addMigrations(Migration_011_to_012)` на line 50.
- `/Users/kilg/.../core_db_impl/CoreDbApiImpl.kt` — `LexemeApiImpl` lines 209-400, 5 callsite'ов `ComponentValueData` подтверждены (241, 257, 279, 311, 324) + 2 в shim (360, 386, 394).
- `/Users/kilg/.../core_db_impl/entity/{ComponentTypeDb, ComponentValueDb, ComponentValueWithType, LexemeDbEntity}.kt` — все существуют.
- `/Users/kilg/.../core_db_impl/room/dao/{ComponentTypeDao, ComponentValueDao, QuizConfigDao}.kt` + `room/WordDao.kt` — все существуют.
- `/Users/kilg/.../core_db_impl/mapper/ComponentValueDataJson.kt` — существует, текущий M12-формат.
- `/Users/kilg/.../core_db_impl/schemas/.../{1..12}.json` — все 12 schema export'ов есть; `13.json` отсутствует (создаётся #15).
- `/Users/kilg/.../core_db_impl/src/androidTest/java/.../room/{MigrationFrom11to12, MigrationFrom11to12IdempotencyTest, BundledSqliteFeatureTest}.kt` — все существуют; `MigrationFrom12to13.kt` / `MigrationFrom12to13IdempotencyTest.kt` отсутствуют (создаются #19, #20).
- `/Users/kilg/.../core_db_impl/src/test/java/.../mapper/DefinitionOldMapperTest.kt` — legacy mapper test (не M13).
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-api/src/main/java/me/apomazkin/core_db_api/CoreDbApi.kt` — содержит 5 BREAKING + 6 NEW сигнатур (business-фаза).
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/datasource/prefs/src/main/java/me/apomazkin/prefs/PrefsProvider.kt` — `getStringByRawKey / setStringByRawKey / getStringFlowByRawKey` API, scan-by-prefix отсутствует (Option B без расширения API).
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentValueData.kt` — существует (business-фаза перенесла delete сюда, #18).

Все existing файлы реально присутствуют в репозитории.

---

## Часть 5: Open questions для финализации в data_implement

1. **Стратегия `@Transaction` обёртки для multi-step операций** — **RESOLVED (iter 3, F173):** используем `database.withTransaction { ... }` в `LexemeApiImpl` (не DAO default-method). Reason: cross-DAO operations (`componentTypeDao` + `componentValueDao` + `quizConfigDao` + `wordDao`) требуют RoomDatabase context — `@Dao interface` не имеет cross-DAO access. Единый convention для всех multi-DAO транзакций: `addLexemeWithComponents` (F171), `addComponentValue` / `updateComponentValue` (F172), `createUserDefinedAtomic`, `renameWithQuizConfigsCascade`, `softDeleteAtomic`.

2. **`logger` параметр в `ComponentValueWithType.toApiEntity()` / `LexemeDbEntity.toApiEntity()` extension functions**. Alternative — module-level `@Inject` либо service locator. Best-guess: явный параметр (явные dependencies на extension function'е). Финализация — `data_implement` (mechanical, не блокирует design).

3. **`affectedPrefs` location** — в `previewDeletionImpact` data возвращает `dictionariesWithValues.map { it }` либо `affectedQuizConfigs.map { it.dictionaryId }` либо пустой список (UseCase composing самостоятельно). Best-guess: data возвращает `affectedQuizConfigs.dictionaryIds` (наиболее семантично — pref связан с quiz_config выбором); UseCase затем проверяет фактический pref-key match (F049 Option B). Финализация — `data_implement`.

4. **`Migration_011_to_012` frozen helpers область применения** — frozen `seedBuiltIns_v12` (#14) обязателен. Дополнительно `migrateTranslationData/migrateDefinitionData` (lines 181-211) пишут в M12-формат через `json_object('v',1,'text',...)` — формат остаётся валиден между M11→M12 и M12→M13 цепочкой (M12→M13 потом переписывает их в M13). Best-guess: `migrateTranslation/Definition` НЕ замораживать. Финализация — `data_implement` через cross-проверку миграционных тестов M11→M13 chain.

5. **SQLite `ALTER TABLE DROP COLUMN` support для `DROP INDEX` сценария** — `DROP INDEX IF EXISTS` поддерживается bundled SQLite ≥ 3.35; verified индиректно через `Migration_011_to_012.createComponentTypesTable` использующий `CREATE UNIQUE INDEX IF NOT EXISTS`. Финализация — `data_test` (verify через `MigrationFrom12to13` тест).

6. **`Index("lexeme_id")` phantom после drop UNIQUE** — нужно убедиться что Room генерирует именно `index_component_values_lexeme_id` (по convention `index_<table>_<col>`). Если KSP-вывод отличается — `MigrationTestHelper` schema validation покажет diff. Best-guess: имя правильное (соответствие KSP convention). Финализация — `data_test`.

7. **Backfill `created_at = strftime('%s','now') * 1000` vs literal `System.currentTimeMillis()`** — оба работают, второй проще читать; первый более «SQL-native». Best-guess: literal interpolation `${System.currentTimeMillis()}` (как в #13). Финализация — стилистика, на `data_implement`.

---

## log_messages

- iter 1: построен DAG из 23 узлов, 9 tiers; покрытие всех файлов из § Затронутые files → Data layer scope.md + Aspects mapping; добавлены frozen seed (#14) для F044, post-load filter (#6) для F031, two-prong SELECT через два @Query (#7) для F032/F039, phantom Index(lexeme_id) (#4) после drop UNIQUE.
- Final delete `ComponentValueData.kt` перенесён сюда из `business_design_tree #6` как последний узел DAG (#18) — после всех data rebind'ов.
- Циклов нет.

### iter 2 (2026-06-17): F168/F169/F170 fixed

- F168: #17 snippet — `wordDao.flowDictionaries()` consistent.
- F169: #10 + #17 — explicit cardinality pre-check в @Transaction.
- F170: write paths — explicit guard на `type.removedAt != null`.

### iter 3 (2026-06-17): F171/F172/F173 fixed

- F171: cardinality pre-check переехал из WordDao в LexemeApiImpl.addLexemeWithComponents (внутри withTransaction).
- F172: addComponentValue/updateComponentValue теперь обёрнут в withTransaction (TOCTOU guard).
- F173: Open Q #1 закрыт — withTransaction в LexemeApiImpl convention для всех multi-statement ops.

### iter 4 (2026-06-17): F174 fixed

- #17 ctor + imports — добавлены `database: Database` (класс `me.apomazkin.core_db_impl.room.Database`, не `AppDatabase`) + `androidx.room.withTransaction` import. DI wiring update пробрасывает `Database` instance в `LexemeApiImpl`.

_model: claude-opus-4-7[1m]_
