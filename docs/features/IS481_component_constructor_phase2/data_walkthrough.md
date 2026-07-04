# data_walkthrough

Факты о реальной структуре data-слоя для phase 2 IS481.
4 области по 02_scope.md → editComponentType SQL, cardinality downgrade SELECT,
Removed mapping в API outcomes, migration / cascade / prefs reset logging.

## 1. `editComponentType` SQL orchestration

### 1.1. Текущая impl (business_implement уже добавил)

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt:564-635`
— `LexemeApiImpl.editComponentType(typeId, name, template, isMulti)`.

Уже реализованы шаги (per scope §LexemeApiImpl + design_tree):

- **`:570-571`** lookup current type: `componentTypeDao.getById(typeId)` → `BuiltInProtected` если null.
- **`:572`** `system_key IS NOT NULL` → `BuiltInProtected`.
- **`:573`** **`existing.removedAt != null` → `EditComponentOutcome.Removed`** (правильный порядок per scope F004 «Removed проверяется ДО BuiltInProtected по разным осям» — в реальности порядок обратный: BuiltInProtected → Removed; см. §1.3 ниже).
- **`:577-578`** `ComponentTemplate.fromKey(existing.templateKey)` → null → `TemplateImmutable`.
- **`:581`** template mismatch (defense-in-depth, F017) → `TemplateImmutable`.
- **`:584-597`** cardinality downgrade precondition (см. §2).
- **`:600-616`** collision check (two-prong, только если `name != oldName`).
- **`:618-630`** `withTransaction { componentTypeDao.update(...) + cascadeRenameInQuizConfigs(oldName, name) }` если `name != oldName`.
- **`:632-634`** re-read updated → `Success(toApiEntity())` или `BuiltInProtected` (fallback при null).

### 1.2. Pattern для cascade + collision — `renameComponentType`

`CoreDbApiImpl.kt:525-562` — существующий шаблон.

Аналог найден полностью: same shape (`componentTypeDao.renameUserDefined`/`update` + `cascadeRenameInQuizConfigs(oldName, newName)` внутри `withTransaction`), та же two-prong SELECT (`findActiveUserDefinedByName` + `findActiveGlobalByName` + `countActivePerDictByName`), та же ось self-id exclude (`sameScope.id != typeId`).

`cascadeRenameInQuizConfigs` (private suspend fun, `:641-653`) общий helper:

```kotlin
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
```

Используется обоими — `renameComponentType:556` и `editComponentType:628`. Дублирования нет, DRY-цель Q6 из scope-questions выполнена.

### 1.3. Расхождения с scope

**Несоответствие порядка `Removed` vs `BuiltInProtected` в `editComponentType` (`:570-573`).**

Scope §LexemeApiImpl: «(c) `removed_at IS NOT NULL` check → `Removed`; (d) `system_key IS NOT NULL` check → `BuiltInProtected`» — то есть Removed ПЕРЕД BuiltInProtected.

Real impl (`:572-573`): BuiltInProtected (system_key) ПЕРЕД Removed (removedAt).

Эффект: built-in type, попавший в soft-deleted (теоретически — built-ins не должны soft-deletиться, но защита по разным осям), вернёт `BuiltInProtected` а не `Removed`. Для user-defined типов разницы нет (system_key всегда null). Pattern parity с `renameComponentType:531-532` (где порядок тот же).

**Аналог найден** (renameComponentType). Расхождение — не блокер, но если scope требует точного порядка — поправить или признать «pattern parity побеждает scope».

## 2. Cardinality downgrade SELECT — текущая conservative impl

### 2.1. Текущая реализация (business_implement)

`CoreDbApiImpl.kt:583-597`:

```kotlin
if (existing.isMulti && !isMulti) {
    val impactedCount = componentValueDao.countActiveByTypeId(typeId)
    if (impactedCount > 0) {
        val dictIds = componentValueDao.dictionaryIdsForTypeId(typeId)
        // TODO(data sub-flow): SELECT per-lexeme count, return ids where >1.
        if (dictIds.isNotEmpty()) {
            return EditComponentOutcome.CardinalityDowngradeBlocked(emptyList())
        }
    }
}
```

Conservative approximation: блокирует downgrade всегда когда есть active values + есть dictIds, возвращает **пустой список** impactedLexemeIds. Не различает «один lexeme с 1 value» (downgrade legitimate) от «один lexeme с 2+ values» (downgrade нужно блокировать).

### 2.2. Существующие DAO methods (что есть)

`ComponentValueDao.kt`:

- **`:104` `countActiveByTypeId(typeId): Int`** — общий COUNT, не per-lexeme.
- **`:110-119` `dictionaryIdsForTypeId(typeId): List<Long>`** — DISTINCT dict ids через JOIN cv → lexemes → words.
- **`:125-132` `aggregatedValueCountPerType(): List<TypeIdCount>`** — `GROUP BY component_type_id`.
- **`:137-147` `aggregatedValueCountPerTypeForDict(dictId)`** — `GROUP BY component_type_id`, scoped к dictionary.
- **`:152-162` `typeDictPairs()`** — DISTINCT `(typeId, dictId)` пары.

**Аналог найден частично:** есть `GROUP BY component_type_id` (aggregated) и `JOIN cv → lexemes → words` (dictionaryIdsForTypeId). Точного аналога для `GROUP BY lexeme_id HAVING COUNT(*) > 1 ORDER BY MAX(updated_at) DESC, lexeme_id ASC` в DAO **нет**.

### 2.3. Требуемая полная замена (per scope §cardinality_downgrade_guard + §editComponentType data)

SQL по scope:

```sql
SELECT lexeme_id, COUNT(*) AS cnt
FROM component_values
WHERE component_type_id = :typeId AND removed_at IS NULL
GROUP BY lexeme_id
HAVING COUNT(*) > 1
ORDER BY MAX(updated_at) DESC, lexeme_id ASC
```

Возвращаемый тип — `List<Long>` (lexeme_id) либо `List<LexemeIdCount>` если caller'у нужен count. По scope `CardinalityDowngradeBlocked(List<Long>)` — достаточно List<Long>.

Локация нового DAO метода: `ComponentValueDao.kt` (рядом с `countActiveByTypeId:101-104`). Например `findLexemesWithMultipleValuesForType(typeId): List<Long>`.

Caller (`editComponentType:584-597`) заменяет conservative ветку на:

```kotlin
if (existing.isMulti && !isMulti) {
    val impacted = componentValueDao.findLexemesWithMultipleValuesForType(typeId)
    if (impacted.isNotEmpty()) {
        return EditComponentOutcome.CardinalityDowngradeBlocked(impacted)
    }
}
```

И тогда `countActiveByTypeId` + `dictionaryIdsForTypeId` в этой ветке НЕ нужны.

**Полная замена для preview/drill-in** (top-3 vs full list — scope §cardinality_downgrade_guard «LIMIT 3 для preview; полный список для drill-in без LIMIT»): по факту API возвращает **полный** `List<Long>`, а **top-3 vs все** — UI-level в Reducer (state `ImpactedLexemesPreview.InlineOnly` для ≤3 vs `InlineWithDrillIn` для >3, см. business_implement #7). DAO LIMIT 3 не нужен.

## 3. Removed mapping в API outcomes

### 3.1. API уровень — `EditComponentOutcome.Removed`

`core/core-db-api/.../entity/ComponentOutcomeApiEntity.kt:65` — variant добавлен.
`CoreDbApiImpl.kt:573` — branch есть.

### 3.2. `RenameComponentOutcome.Removed`

`ComponentOutcomeApiEntity.kt:30` — variant добавлен.

`CoreDbApiImpl.kt:532` — **bug**: `if (existing.removedAt != null) return RenameComponentOutcome.BuiltInProtected`. Должно быть `RenameComponentOutcome.Removed` per scope F004 «removed_at IS NOT NULL → Removed (не путать с BuiltInProtected)».

UseCaseImpl mapping ОК: `ComponentsManagerUseCaseImpl.kt:98` ловит `RenameComponentOutcome.Removed → RenameOutcome.Removed`. Но API его никогда не вернёт — soft-deleted типы будут падать в `BuiltInProtected`. **Реальное поведение — silent miscategorization** для Rename-flow.

### 3.3. `SoftDeleteComponentOutcome.Removed`

`ComponentOutcomeApiEntity.kt:38` — variant добавлен.

`CoreDbApiImpl.kt:684-713` — `softDeleteComponentType` — **bug**: **нет** `if (type.removedAt != null) return SoftDeleteComponentOutcome.Removed` check вообще. Защита есть только по `system_key:689` и `previewDeletionImpact` (который сам не возвращает Removed).

Поведение при повторном soft-delete уже удалённого типа: `componentTypeDao.softDelete` (запрос `:69` `UPDATE ... WHERE id = :id AND system_key IS NULL`) затрёт `removed_at = now` поверх старого значения, `componentValueDao.softDeleteByTypeId` обновит `removed_at IS NULL` rows (которых уже нет), cascade refs в `quiz_configs` тоже пройдёт впустую. Result: `SoftDeleteComponentOutcome.Success(impact)` с `impact.valueCount = 0` и пустыми списками.

UseCaseImpl mapping ОК: `ComponentsManagerUseCaseImpl.kt:139` ловит `SoftDeleteComponentOutcome.Removed → DeleteOutcome.Removed`. Но API его никогда не вернёт.

**Аналог найден на API уровне; на impl уровне — нет.** Два bug-fix-точки для data sub-flow:
1. `renameComponentType:532` — заменить `BuiltInProtected` → `Removed`.
2. `softDeleteComponentType:686-689` — добавить `if (type.removedAt != null) return SoftDeleteComponentOutcome.Removed` между `:687` lookup и `:689` system_key check (или после — порядок Removed vs BuiltInProtected по scope).

## 4. Migration / cascade / prefs reset logging

### 4.1. Migration_012_to_013 (infra sub-flow уже сделал)

`core/core-db-impl/.../room/migrations/Migration_012_to_013.kt:57-90` — 9 `Log.d(LogTags.COMPONENT_CONSTRUCTOR, "M12→M13 step N <name>: ok")` после каждого private step (`renameComponentTypesRemoveDate` / `addComponentTypesNewColumns` / ... / `rewriteImageJson`). Per-step metric = literal `"ok"` без affected rows count (MVP minimum по infra design tree).

Import: `me.apomazkin.logger.LogTags` (`Migration_012_to_013.kt:7`).
Логгер: `android.util.Log` напрямую (`:6`), не `LexemeLogger` — обоснование infra: `migrate(connection: SQLiteConnection)` запускается до DI graph (Room internal); LexemeLogger inject невозможен.

**Аналог найден.**

### 4.2. `cascadeRenameInQuizConfigs` logging (call site `:567-579`)

Scope §migration_logging «логи в `QuizConfigDao.updateComponentRefs` (before/after refs count)» + infra_walkthrough §6.3 fact: Room DAO interface — logger inject невозможен, логи должны жить в LexemeApiImpl call site.

Real location: `CoreDbApiImpl.kt:641-653` `cascadeRenameInQuizConfigs` private suspend fun. Внутри for-each:

- `:643` `refs = config.componentRefs.toComponentTypeRefList()` — before-snapshot.
- `:649-651` `if (updated != refs) { quizConfigDao.updateComponentRefs(config.id, updated.toJson()) }` — write call site.

Текущее состояние: **0 логов вообще** в `cascadeRenameInQuizConfigs` (нет `logger.d` / `Log.d`).

Также вызывается из:
- `renameComponentType:556`.
- `editComponentType:628`.

И параллельная cascade (filter, не rename) в `softDeleteComponentType:700-708` — там тоже **0 логов**, тот же `quizConfigDao.updateComponentRefs(config.id, filtered.toJson())` call.

Инфра готова: `LexemeApiImpl:229` уже инжектит `private val logger: LexemeLogger`. Только add `logger.d(tag = LogTags.COMPONENT_CONSTRUCTOR, message = "cascade rename: configId=${config.id} refs=${refs.size}→${updated.size}")` или подобный — design data sub-flow.

**Аналог найден** в смысле инфраструктуры (logger уже есть в LexemeApiImpl); фактических логов нет.

### 4.3. Prefs reset logging — `resetQuizPickerPrefsBestEffort`

Real location: `app/src/main/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerUseCaseImpl.kt:183-196`.

Текущая impl: `impact.affectedPrefs.forEach { dictId -> try { prefsProvider.setStringByRawKey(quizPickerPrefKey(dictId), null) } catch ... { logger.w(tag = LogTags.COMPONENTS_MANAGER, message = "...failure: ${e.message}") } }`.

Тег — `LogTags.COMPONENTS_MANAGER` (module-tag `"ComponentsManager"`, `modules/screen/components_manager/.../LogTags.kt:11`), **не** feature tag `###ComponentConstructor###`.

Scope §migration_logging «прес reset logger» + `feature_log_tag` double-tag pattern: «module-tag для debug + feature-tag для smoke-фильтрации».

Текущий лог:
- Только catch branch (warning при per-pref failure).
- Нет success-логов (per-pref OK reset, total count).
- Нет feature-tag (`COMPONENT_CONSTRUCTOR`).

Для adb-фильтрации smoke по `###ComponentConstructor###` (acceptance §5) — добавить **второй** log line с feature tag (parity с Migration), либо заменить tag, либо emit два лога. По scope §feature_log_tag «двойная ось» — два лога / два тега.

**Аналог найден частично:** prefs reset log есть (`:190-193`), но только error branch и под module-tag. Feature-tag для smoke — отсутствует.

### 4.4. `editComponentType` / `renameComponentType` / `softDeleteComponentType` entry/exit logging

Scope §LexemeApiImpl «(i) feature-tag log на entry / exit с outcome».

Real state: `CoreDbApiImpl.kt:525-713` — все три метода (`renameComponentType` / `editComponentType` / `softDeleteComponentType`) **не имеют entry/exit логов вообще**. Существующие `logger.e/d` в файле:
- `:290, :332` — error в `addLexemeWithUserDefinedComponent` / `addLexemeWithComponents`.
- `:803, :805` — `getWriteQuizByIds` через `LogTags.DB` (`core_db_impl.LogTags`, не feature-tag).

**Аналог не найден** для feature-tag entry/exit pattern. Infrastructure готова (`logger` в `LexemeApiImpl:229`, `LogTags.COMPONENT_CONSTRUCTOR` import доступен через `me.apomazkin.logger.LogTags`).

## 5. Прочие data-layer factual notes

### 5.1. `editComponentType` DAO method — НЕТ separate query

Scope §Data impl: «новый `editComponentType` DAO метод (плоский UPDATE name / template / isMulti / updatedAt без оркестрации)».

Real impl: `CoreDbApiImpl.kt:620-626` использует существующий `componentTypeDao.update(existing.copy(name=..., isMulti=..., updatedAt=...))` (`ComponentTypeDao.kt:60-61` — `@Update suspend fun update(type: ComponentTypeDb)`).

Template поле в реальности НЕ обновляется (TemplateImmutable check на `:581` гарантирует `template == oldTemplate`, поэтому `existing.templateKey` остаётся в копии). Плоский UPDATE через `@Update` — minimal DAO surface.

**Аналог найден** — universal `@Update` row replace. Отдельный `@Query("UPDATE component_types SET name=?, is_multi=?, updated_at=? WHERE ...")` не нужен (избыточно vs Room `@Update`).

### 5.2. `updated_at` обновляется в orchestration, не БД-trigger

`CoreDbApiImpl.kt:618` `val now = Date(System.currentTimeMillis())` + `:624` `updatedAt = now` — convention `updated_at` в repository per data-layer.md §9.

Аналог: `renameComponentType:553-555` `now` + `renameUserDefined(typeId, newName, now)` (через explicit `@Query` `SET updated_at = :now` `:75-76`). Pattern parity ✓.

### 5.3. `withTransaction` обёртка

`editComponentType:619` — `database.withTransaction { ... }` per scope §LexemeApiImpl «(a) `withTransaction` обёртку». Аналог: `renameComponentType:554`, `softDeleteComponentType:696`, `addLexemeWithBuiltInComponent:257` etc — pattern parity ✓.

## Вердикт

Аналог: **частично найден**

- Pattern для editComponentType orchestration (cascade + collision + transaction + updated_at) — **полный аналог в `renameComponentType:525-562`**, business_implement переиспользует.
- Cardinality downgrade SELECT (per-lexeme `HAVING COUNT(*) > 1` + deterministic ORDER) — **аналога нет**, текущая impl `CoreDbApiImpl.kt:583-597` conservative с empty `impactedLexemeIds`. Замена: новый DAO method в `ComponentValueDao.kt`.
- Removed mapping в API outcomes — **API уровень готов**, но `renameComponentType:532` и `softDeleteComponentType:684-713` impl-bug'и: первый возвращает `BuiltInProtected` вместо `Removed`, второй не имеет `removedAt`-check вообще. UseCaseImpl mapping корректен но недостижим для текущего impl.
- Migration logging — **полный аналог сделан infra sub-flow** (`Migration_012_to_013.kt:57-90`).
- `cascadeRenameInQuizConfigs` (`:641-653`) и `softDeleteComponentType` cascade (`:700-708`) — **аналога логов нет**, инфра готова (`LexemeApiImpl:229` injects logger).
- Prefs reset (`ComponentsManagerUseCaseImpl.kt:183-196`) — **существующий error-only log под module-tag**, feature-tag для smoke отсутствует.

## log_messages

- Read scope §02 + business_implement, обошёл `CoreDbApiImpl.kt:525-713`, `ComponentValueDao.kt:1-169`, `ComponentTypeDao.kt:1-144`, `QuizConfigDao.kt:1-64`, `Migration_012_to_013.kt:1-186`, `ComponentsManagerUseCaseImpl.kt:1-197`, `ComponentOutcomeApiEntity.kt:1-73`.
- Фиксирую 3 bug'а в data impl (impl ≠ API contract): `renameComponentType:532` (BuiltInProtected вместо Removed), `softDeleteComponentType` (no `removedAt`-check), `editComponentType` cardinality (conservative empty list вместо per-lexeme SELECT).
- Фиксирую 2 logging-gap: cascadeRenameInQuizConfigs (0 логов) + prefs reset (нет feature-tag).

_model: claude-opus-4-7[1m]_
