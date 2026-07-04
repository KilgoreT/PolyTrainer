# data_implement

Реальная имплементация data-слоя phase 2 IS481 — 6 узлов из
data_design_tree.md (3 bug-fix + 2 logging gap + 1 feature-tag для prefs reset).

## Узлы реализованы (6)

### #0 `LexemeApiImpl.renameComponentType` [~] — Removed mapping bug-fix

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt:531-532`

Заменён неправильный mapping (был `removedAt != null → BuiltInProtected`,
silent miscategorization) на правильный `Removed`. Дополнительно
изменён порядок: Removed check теперь ПЕРЕД system_key check (parity с
post-#0 `editComponentType:572-573` и с #1 softDelete после fix).

```kotlin
val existing = componentTypeDao.getById(typeId)
    ?: return RenameComponentOutcome.BuiltInProtected
if (existing.removedAt != null) return RenameComponentOutcome.Removed
if (existing.systemKey != null) return RenameComponentOutcome.BuiltInProtected
```

UseCaseImpl mapping `RenameComponentOutcome.Removed → RenameOutcome.Removed`
(ComponentsManagerUseCaseImpl.kt:98) и Reducer ветка
`RenameResult(Removed)` уже работали — теперь достижимы.

### #1 `LexemeApiImpl.softDeleteComponentType` [~] — добавлен Removed check

`CoreDbApiImpl.kt:688` (новая строка между lookup и system_key check).

```kotlin
val type = componentTypeDao.getById(typeId)
    ?: return SoftDeleteComponentOutcome.BuiltInProtected
if (type.removedAt != null) return SoftDeleteComponentOutcome.Removed
if (type.systemKey != null) return SoftDeleteComponentOutcome.BuiltInProtected
```

Повторный soft-delete уже удалённого типа теперь возвращает
`SoftDeleteComponentOutcome.Removed` вместо `Success(impact.valueCount=0)`.
UseCaseImpl mapping (ComponentsManagerUseCaseImpl.kt:139) и Reducer ветка
`DeleteResult(Removed)` уже работали.

### #2 `LexemeApiImpl.editComponentType` + `ComponentValueDao.findLexemesWithMultipleValuesForType` [~] — real per-lexeme cardinality SELECT

**Новый DAO метод** (`ComponentValueDao.kt:106-127`, рядом с `countActiveByTypeId`):

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

Deterministic ORDER `MAX(updated_at) DESC, lexeme_id ASC` per
scope §cardinality_downgrade_guard. LIMIT не применяется (top-3 vs full
filter — в Reducer `ImpactedLexemesPreview.InlineOnly` / `InlineWithDrillIn`).

**Call site** (`CoreDbApiImpl.kt:583-589`) — conservative
approximation заменён на real SELECT:

```kotlin
if (existing.isMulti && !isMulti) {
    val impacted = componentValueDao.findLexemesWithMultipleValuesForType(typeId)
    if (impacted.isNotEmpty()) {
        return EditComponentOutcome.CardinalityDowngradeBlocked(impacted)
    }
}
```

Legitimate downgrade (один lexeme с одним value) больше не блокируется.
Real impacted lexeme_ids доходят до UI preview вместо empty list.

`countActiveByTypeId` + `dictionaryIdsForTypeId` в downgrade-ветке не
используются (они продолжают жить в `previewDeletionImpact:660-661` —
не трогал).

### #3 `LexemeApiImpl.cascadeRenameInQuizConfigs` [~] — before/after refs logging

`CoreDbApiImpl.kt:641-660`. Внутри for-each добавлен `logger.d` с
feature-tag `LogTags.COMPONENT_CONSTRUCTOR`:

```kotlin
logger.d(
    tag = FeatureLogTags.COMPONENT_CONSTRUCTOR,
    message = "cascade rename: configId=${config.id} refs=${refs.size}→${updated.size} write=$willWrite oldName=$oldName newName=$newName",
)
```

Один лог на config — at-most O(N configs) per cascade, приемлемо для
smoke. Поля: configId, refs.size before/after (after = same length при
rename, разница в structure), write flag, oldName/newName для cross-check.

Import: `me.apomazkin.logger.LogTags as FeatureLogTags` (alias чтобы не
коллидировать с file-local `me.apomazkin.core_db_impl.LogTags` который
используется для `LogTags.DB`).

### #4 `LexemeApiImpl.softDeleteComponentType` inline cascade [~] — filter logging

`CoreDbApiImpl.kt:705-715`. Аналогично #3 для filter:

```kotlin
logger.d(
    tag = FeatureLogTags.COMPONENT_CONSTRUCTOR,
    message = "cascade soft-delete: configId=${config.id} refs=${refs.size}→${filtered.size} write=$willWrite removedName=$oldName",
)
```

depends_on=[1] выполнен: после #1 Removed-check soft-delete уже
удалённого типа возвращается рано, cascade filter недостижим для
already-removed → логи не false-positive'ят.

### #5 `ComponentsManagerUseCaseImpl.resetQuizPickerPrefsBestEffort` [~] — double-tag feature logging

`app/src/main/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerUseCaseImpl.kt:185-216`.

Добавлен **второй** лог с feature-tag `LogTags.COMPONENT_CONSTRUCTOR`
параллельно existing module-tag (`LogTags.COMPONENTS_MANAGER`):

- Start: `resetQuizPickerPrefs start: count=$total`.
- Per-pref ok: `resetQuizPickerPrefs ok: dictId=$dictId`.
- Per-pref fail: warning под обоими тегами (existing module-tag +
  новый feature-tag `resetQuizPickerPrefs fail: dictId=$dictId cause=$msg`).
- Done: `resetQuizPickerPrefs done: ok=$successCount/$total`.

Двойная ось per scope §feature_log_tag — module-tag для debug
(granular per-pref failure context), feature-tag для smoke-фильтрации
через `adb logcat | grep '###ComponentConstructor###'`.

Import: `me.apomazkin.logger.LogTags as FeatureLogTags` (collision с
existing `me.apomazkin.components_manager.LogTags`).

## Тесты

Запущены sequentially per memory rule `feedback_findings_dependency_reduction`
(no parallel gradle calls):

1. `./scripts/cc-build.sh :core:core-db-impl:testDebugUnitTest`
   → **BUILD SUCCESSFUL** (existing 2 mapper-tests — `TemplateValuesJsonTest`
   + `DefinitionOldMapperTest`; нет DAO/CoreDbApiImpl unit tests в module).
2. `./scripts/cc-build.sh :app:testDebugUnitTest` → **BUILD SUCCESSFUL**
   (включает `ComponentsManagerUseCaseImplTest` — 15 phase 2 tests
   business_implement + Removed mapping branches; double-tag prefs reset
   не покрыт unit tests, но behaviour сохранён).

Никаких регрессий. Тесты не правил (только код).

## Нетривиальные решения

1. **Порядок Removed → BuiltInProtected в `renameComponentType` изменён**
   (не только bug-fix, но и swap). Раньше: system_key → removedAt (оба
   → BuiltInProtected, баг). Теперь: removedAt → system_key (parity с
   editComponentType:572-573 и softDelete после #1). Для user-defined
   типов (systemKey IS NULL) разницы нет — баг только для теоретического
   built-in + soft-deleted кейса. Задача phrase: «добавь Removed check
   ПЕРЕД BuiltInProtected check» — выполнено буквально.

2. **Import alias `me.apomazkin.logger.LogTags as FeatureLogTags`** в
   `CoreDbApiImpl.kt` и `ComponentsManagerUseCaseImpl.kt`. Обе файла
   уже импортируют локальный `LogTags` (file-local `core_db_impl.LogTags`
   для DB и module-local `components_manager.LogTags` для
   COMPONENTS_MANAGER). Fully-qualified access работал бы тоже, но
   alias чище и не дублирует длинный FQN на каждом вызове.

3. **DAO `findLexemesWithMultipleValuesForType` возвращает `List<Long>`**
   (не `List<LexemeIdCount>`). API contract
   `CardinalityDowngradeBlocked(List<Long>)` достаточно — count не
   нужен в UI per design tree («top-3 vs full filter живёт в Reducer»).

4. **`countActiveByTypeId` + `dictionaryIdsForTypeId` НЕ удалены** из
   DAO. Они продолжают использоваться в `previewDeletionImpact:660-661`
   (per data_walkthrough §1.1) — этот path не трогаем.

5. **Cascade soft-delete logging для **отсутствующего** oldName.**
   Текущая impl (`CoreDbApiImpl.kt:699-703`) защищена `if (oldName != null)`
   wrapper'ом. Лог для null-name случая (built-in после Removed-check
   не должен сюда попасть, но defense-in-depth) не добавлен — нет смысла
   логировать пропуск cascade. Лог только внутри for-each.

6. **Per-pref logging не покрыт unit tests** — `resetQuizPickerPrefsBestEffort`
   private suspend fun, существующий `ComponentsManagerUseCaseImplTest`
   проверяет outcome mapping, не log emissions. Smoke verification —
   через adb logcat фильтрацию `###ComponentConstructor###` (acceptance §5).

## log_messages
- iter 1: 6 узлов реализованы — 3 bug-fix (Removed mapping в
  renameComponentType + Removed check в softDeleteComponentType + real
  per-lexeme cardinality SELECT в editComponentType) + 2 cascade logging
  gap (rename + soft-delete) + 1 prefs reset double-tag. Новый DAO method
  `ComponentValueDao.findLexemesWithMultipleValuesForType` с deterministic
  ORDER.
- iter 1: tests passed sequentially (`:core:core-db-impl:testDebugUnitTest`
  + `:app:testDebugUnitTest`), no regressions. Тесты не правил.
- decision: import alias `FeatureLogTags` для `me.apomazkin.logger.LogTags`
  чтобы не коллидировать с file-local `LogTags` в обоих файлах (cleaner
  than fully-qualified inline).
- decision: cardinality DAO возвращает `List<Long>` (per design tree #2).

_model: claude-opus-4-7[1m]_
