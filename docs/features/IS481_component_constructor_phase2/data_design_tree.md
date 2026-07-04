# Data design tree: IS481 component_constructor phase 2

DAG узлов data-слоя для phase 2: закрываем 3 bug'а реальной impl
(`renameComponentType` Removed mapping, `softDeleteComponentType` Removed
check, `editComponentType` cardinality SELECT) + 2 logging gap'а
(cascade rename / cascade soft-delete в `quiz_configs` + feature-tag в
prefs reset). Schema стабильна (M13 не меняется), новых миграций нет;
все правки — внутри `CoreDbApiImpl.kt` + `ComponentsManagerUseCaseImpl.kt`.

## Граф (YAML)

```yaml
nodes:
  - id: 0
    file: core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt
    symbol: LexemeApiImpl.renameComponentType
    location: ":525-562 (точечно :532)"
    op: "~"
    depends_on: []
    summary: >-
      Bug-fix: `if (existing.removedAt != null) return RenameComponentOutcome.BuiltInProtected`
      → `RenameComponentOutcome.Removed`. UseCase mapping `Removed → RenameOutcome.Removed`
      уже работает (UseCaseImpl phase business), но API его никогда не возвращает.

  - id: 1
    file: core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt
    symbol: LexemeApiImpl.softDeleteComponentType
    location: ":684-713 (вставка после :688)"
    op: "~"
    depends_on: []
    summary: >-
      Bug-fix: добавить `if (type.removedAt != null) return SoftDeleteComponentOutcome.Removed`
      между lookup (`:687`) и system_key check (`:689`). Сейчас повторный
      soft-delete уже удалённого типа возвращает Success(impact с valueCount=0)
      вместо Removed. Порядок Removed → BuiltInProtected по разным осям (parity
      с `editComponentType:572-573` после узла #0).

  - id: 2
    file: core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt
    symbol: LexemeApiImpl.editComponentType (+ new ComponentValueDao method)
    location: ":583-597 + ComponentValueDao.kt (new method рядом с :101-104)"
    op: "~"
    depends_on: []
    summary: >-
      Bug-fix: заменить conservative approximation (всегда блокирует с empty
      list когда есть active values + dictIds) на real per-lexeme SELECT.
      Новый DAO `findLexemesWithMultipleValuesForType(typeId): List<Long>`
      с deterministic ORDER. Caller возвращает `CardinalityDowngradeBlocked(impacted)`
      с реальными lexeme_id (не emptyList()). После этого узла call site не
      использует `countActiveByTypeId` + `dictionaryIdsForTypeId` в downgrade-ветке.

  - id: 3
    file: core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt
    symbol: LexemeApiImpl.cascadeRenameInQuizConfigs
    location: ":641-653"
    op: "~"
    depends_on: []
    summary: >-
      Logging gap: добавить `Log.d(LogTags.COMPONENT_CONSTRUCTOR, ...)` before/after
      внутри for-each (refs.size before vs updated.size after, configId, write
      выполнен/нет). Logger уже есть (`LexemeApiImpl:229 logger: LexemeLogger`).
      Шаблон: `logger.d(tag = LogTags.COMPONENT_CONSTRUCTOR, message = "cascade rename: configId=${config.id} refs=${refs.size}→${updated.size} write=${updated != refs}")`.

  - id: 4
    file: core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt
    symbol: LexemeApiImpl.softDeleteComponentType (inline cascade)
    location: ":700-708"
    op: "~"
    depends_on: [1]
    summary: >-
      Logging gap: параллельно #3 для cascade filter (soft-delete refs из
      `quiz_configs`). Те же поля: configId, refs.size before vs filtered.size
      after, write выполнен/нет. depends_on=[1] чтобы после bug-fix Removed-check
      не логировать filter для never-reached path.

  - id: 5
    file: app/src/main/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerUseCaseImpl.kt
    symbol: ComponentsManagerUseCaseImpl.resetQuizPickerPrefsBestEffort
    location: ":183-196"
    op: "~"
    depends_on: []
    summary: >-
      Logging gap: добавить второй `logger.d/w` с feature tag
      `LogTags.COMPONENT_CONSTRUCTOR` параллельно существующему module-tag log
      (`COMPONENTS_MANAGER`). Double-tag pattern из scope §feature_log_tag:
      module-tag для debug, feature-tag для adb-фильтрации smoke по
      `###ComponentConstructor###`. Также добавить success-логи (per-pref OK reset
      + total count), не только error branch — для smoke-фильтрации видимости.
```

Все узлы independent кроме #4 (depends_on=[1] — без #1 cascade filter в
soft-delete всё ещё затирает refs уже удалённого типа повторно, что
делает лог false-positive). Узлы можно делать параллельно по парам:
`{#0}`, `{#1, #2}`, `{#3, #4}`, `{#5}`.

## Детали изменений

### #0 LexemeApiImpl.renameComponentType:532 — Removed mapping bug

**Текущее состояние:**

```kotlin
if (existing.removedAt != null) return RenameComponentOutcome.BuiltInProtected
```

**Замена:**

```kotlin
if (existing.removedAt != null) return RenameComponentOutcome.Removed
```

**Эффект:** silent miscategorization для soft-deleted типов прекращается.
Reducer уже умеет `RenameResult(Removed) → close dialog + snackbar
«Компонент удалён»` (`Msg.RenameResult` handling в Manager / PerDict
mate). UseCaseImpl mapping `RenameComponentOutcome.Removed →
RenameOutcome.Removed` уже работает (`ComponentsManagerUseCaseImpl.kt:98`).

**Тесты:** existing UseCaseImplTest `whenRenameApiReturnsRemoved_thenDomainRenameOutcomeRemoved`
— mock возвращает `RenameComponentOutcome.Removed`, проверяет
`RenameOutcome.Removed`. Тест не валидирует data-impl path; для verify
прохода bug-fix'а — manual smoke / integration.

### #1 LexemeApiImpl.softDeleteComponentType:684-713 — missing Removed check

**Текущее состояние (`:687-689`):**

```kotlin
val type = componentTypeDao.getById(typeId)
    ?: return SoftDeleteComponentOutcome.BuiltInProtected
if (type.systemKey != null) return SoftDeleteComponentOutcome.BuiltInProtected
```

**Замена — вставить Removed check ПЕРЕД system_key:**

```kotlin
val type = componentTypeDao.getById(typeId)
    ?: return SoftDeleteComponentOutcome.BuiltInProtected
if (type.removedAt != null) return SoftDeleteComponentOutcome.Removed
if (type.systemKey != null) return SoftDeleteComponentOutcome.BuiltInProtected
```

**Порядок Removed → BuiltInProtected** — parity с `editComponentType`
после узла #0 fix-а. Built-in типы не могут быть soft-deleted (по
schema/policy), но защита по разным осям независима — soft-deleted
built-in (теоретически) вернёт Removed. Для user-defined типов
(systemKey IS NULL) разницы порядка нет.

**Эффект:** повторный soft-delete уже удалённого типа возвращает
`SoftDeleteComponentOutcome.Removed` вместо `Success(impact с valueCount=0
и пустыми cascade-списками)`. Reducer ветка `DeleteResult(Removed) →
close confirm dialog + snackbar` уже работает.

### #2 LexemeApiImpl.editComponentType:583-597 + ComponentValueDao — real cardinality SELECT

**Новый DAO метод (`ComponentValueDao.kt` рядом с `:101-104` `countActiveByTypeId`):**

```kotlin
@Query("""
    SELECT lexeme_id
    FROM component_values
    WHERE component_type_id = :typeId AND removed_at IS NULL
    GROUP BY lexeme_id
    HAVING COUNT(*) > 1
    ORDER BY MAX(updated_at) DESC, lexeme_id ASC
""")
suspend fun findLexemesWithMultipleValuesForType(typeId: Long): List<Long>
```

Поля per scope §cardinality_downgrade_guard: deterministic ORDER
`MAX(updated_at) DESC, lexeme_id ASC` (tie-break). LIMIT 3 НЕ
добавляется в DAO — top-3 vs full filter живёт в Reducer
(`ImpactedLexemesPreview.InlineOnly` / `InlineWithDrillIn`).

**Текущая impl call site (`:583-597`):**

```kotlin
if (existing.isMulti && !isMulti) {
    val impactedCount = componentValueDao.countActiveByTypeId(typeId)
    if (impactedCount > 0) {
        val dictIds = componentValueDao.dictionaryIdsForTypeId(typeId)
        if (dictIds.isNotEmpty()) {
            return EditComponentOutcome.CardinalityDowngradeBlocked(emptyList())
        }
    }
}
```

**Замена:**

```kotlin
if (existing.isMulti && !isMulti) {
    val impacted = componentValueDao.findLexemesWithMultipleValuesForType(typeId)
    if (impacted.isNotEmpty()) {
        return EditComponentOutcome.CardinalityDowngradeBlocked(impacted)
    }
}
```

**Эффект:** legitimate downgrade (один lexeme с одним value) больше не
блокируется. Real impacted lexeme_ids доходят до UI preview вместо
empty list. UseCaseImpl mapping passthrough'ит ids в
`EditOutcome.CardinalityDowngradeBlocked(ids)`, Reducer применяет
`ImpactedLexemesPreview` per F023 (≤3 inline / >3 inline+drill-in).

**Старые DAO методы (`countActiveByTypeId`, `dictionaryIdsForTypeId`)** в
downgrade-ветке больше не нужны. Они продолжают использоваться в
`previewDeletionImpact:660-661` — не трогать.

**Тесты:** existing UseCaseImplTest с mock API остаются валидны
(`editComponent_cardinalityDowngradeBlocked_*`). Если стоит задача
протестировать data-impl SELECT — нужен Room DAO test (in-memory DB,
вставить 2 component_values для одного lexeme_id, проверить что
`findLexemesWithMultipleValuesForType(typeId)` вернёт этот lexeme_id).
В scope phase 2 это **не требуется** (migration tests не нужны, DAO
tests не были обозначены) — оставить за рамками.

### #3 LexemeApiImpl.cascadeRenameInQuizConfigs:641-653 — before/after refs logging

**Текущее состояние:** 0 логов.

**Замена — логи внутри for-each:**

```kotlin
private suspend fun cascadeRenameInQuizConfigs(oldName: String, newName: String) {
    quizConfigDao.getAllConfigs().forEach { config ->
        val refs = config.componentRefs.toComponentTypeRefList()
        val updated = refs.map { ref ->
            if (ref is ComponentTypeRef.UserDefined && ref.name == oldName) {
                ComponentTypeRef.UserDefined(newName)
            } else ref
        }
        val willWrite = updated != refs
        logger.d(
            tag = LogTags.COMPONENT_CONSTRUCTOR,
            message = "cascade rename: configId=${config.id} refs=${refs.size}→${updated.size} write=$willWrite oldName=$oldName newName=$newName",
        )
        if (willWrite) {
            quizConfigDao.updateComponentRefs(config.id, updated.toJson())
        }
    }
}
```

**Поля:** `configId`, `refs.size before/after`, `write` (true/false для
no-op конфигов), oldName + newName для cross-check correlation. Один
лог на config — at-most O(N configs) per cascade, приемлемо для smoke.

**Инфра:** logger уже инжектится в `LexemeApiImpl:229`, import
`me.apomazkin.logger.LogTags` есть (используется для DB / других тегов
в файле). `LogTags.COMPONENT_CONSTRUCTOR` константа существует
(добавлена infra sub-flow, scope §feature_log_tag F014).

### #4 LexemeApiImpl.softDeleteComponentType inline cascade:700-708 — filter logging

**Текущее состояние:** 0 логов в cascade filter.

**Замена — аналогично #3:**

```kotlin
if (oldName != null) {
    quizConfigDao.getAllConfigs().forEach { config ->
        val refs = config.componentRefs.toComponentTypeRefList()
        val filtered = refs.filter {
            !(it is ComponentTypeRef.UserDefined && it.name == oldName)
        }
        val willWrite = filtered != refs
        logger.d(
            tag = LogTags.COMPONENT_CONSTRUCTOR,
            message = "cascade soft-delete: configId=${config.id} refs=${refs.size}→${filtered.size} write=$willWrite removedName=$oldName",
        )
        if (willWrite) {
            quizConfigDao.updateComponentRefs(config.id, filtered.toJson())
        }
    }
}
```

**Альтернатива (DRY):** extract в `cascadeFilterInQuizConfigs(removedName)`
private suspend fun (mirror к `cascadeRenameInQuizConfigs`).
Phase 2 scope не требует — текущий cascade живёт inline в
`softDeleteComponentType:700-708`, refactor — отдельный backlog item.
В рамках текущего узла — оставить inline + добавить лог.

**depends_on=[1]** — после bug-fix Removed-check soft-delete уже
удалённого типа возвращается рано (`:688` Removed branch), cascade
filter недостижим для already-removed, логи не false-positive'ят.

### #5 ComponentsManagerUseCaseImpl.resetQuizPickerPrefsBestEffort:183-196 — feature-tag

**Текущее состояние (только error branch, только module-tag):**

```kotlin
private suspend fun resetQuizPickerPrefsBestEffort(impact: DeletionImpact) {
    impact.affectedPrefs.forEach { dictId ->
        try {
            prefsProvider.setStringByRawKey(quizPickerPrefKey(dictId), null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.w(
                tag = LogTags.COMPONENTS_MANAGER,
                message = "resetQuizPickerPrefsBestEffort: dictId=$dictId failed: ${e.message}",
            )
        }
    }
}
```

**Замена — добавить success-логи + feature-tag double-tag pattern:**

```kotlin
private suspend fun resetQuizPickerPrefsBestEffort(impact: DeletionImpact) {
    logger.d(
        tag = LogTags.COMPONENT_CONSTRUCTOR,
        message = "prefs reset start: count=${impact.affectedPrefs.size}",
    )
    var successCount = 0
    impact.affectedPrefs.forEach { dictId ->
        try {
            prefsProvider.setStringByRawKey(quizPickerPrefKey(dictId), null)
            successCount++
            logger.d(
                tag = LogTags.COMPONENT_CONSTRUCTOR,
                message = "prefs reset ok: dictId=$dictId",
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.w(
                tag = LogTags.COMPONENTS_MANAGER,
                message = "resetQuizPickerPrefsBestEffort: dictId=$dictId failed: ${e.message}",
            )
            logger.w(
                tag = LogTags.COMPONENT_CONSTRUCTOR,
                message = "prefs reset fail: dictId=$dictId cause=${e.message}",
            )
        }
    }
    logger.d(
        tag = LogTags.COMPONENT_CONSTRUCTOR,
        message = "prefs reset done: ok=$successCount/${impact.affectedPrefs.size}",
    )
}
```

**Двойная ось:** existing module-tag log (`COMPONENTS_MANAGER`) остаётся
для debug (granular per-pref failure context). Новые feature-tag логи
(`COMPONENT_CONSTRUCTOR`) дают smoke-фильтрацию через
`adb logcat | grep '###ComponentConstructor###'` — start, per-pref ok,
per-pref fail, done с total. Pattern parity с infra sub-flow
Migration_012_to_013 (per-step ok-логи под feature-tag).

**Import:** `me.apomazkin.logger.LogTags` уже доступен (использует
`LogTags.COMPONENTS_MANAGER` локальный — нужно switch на shared
`me.apomazkin.logger.LogTags.COMPONENT_CONSTRUCTOR` через explicit
qualifier либо отдельный import). Если конфликт имён —
`me.apomazkin.logger.LogTags as SharedLogTags` или fully-qualified
access в новых вызовах.

## log_messages

- Прочёл data_walkthrough, 02_scope §LexemeApiImpl + §migration_logging,
  business_implement §нетривиальные решения; verify через Read
  `CoreDbApiImpl.kt:525-713` (все ключевые точки) + UseCaseImpl
  `:170-197`. Все 6 узлов независимы кроме #4 (depends_on=[1]).
- Структурное решение: новый DAO `findLexemesWithMultipleValuesForType`
  возвращает `List<Long>` (без count), per scope §cardinality_downgrade_guard
  `CardinalityDowngradeBlocked(List<Long>)`. ORDER `MAX(updated_at) DESC,
  lexeme_id ASC` deterministic для тестируемости.

_model: claude-opus-4-7[1m]_
