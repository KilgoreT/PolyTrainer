---
status: done
---

# Summary — data

## Что сделано

Data sub-flow закрыл 6 узлов (3 bug-fix + 3 logging) поверх phase 2 IS481, без изменений schema (M13 стабилен, миграции не трогали).

**Bug-fixes (impl ≠ API contract):**

1. **#0 `renameComponentType:531-532`** — заменён `removedAt != null → BuiltInProtected` (silent miscategorization) на `→ Removed`; порядок swap'нут на Removed → BuiltInProtected. **⚠ Note:** `editComponentType:573-574` использует **обратный** порядок (BuiltInProtected → Removed) — sibling-методы non-uniform, баг отслежен в `Backlog.md` § ВекторныйПиздеж.
2. **#1 `softDeleteComponentType:688`** — добавлен отсутствующий `if (type.removedAt != null) return SoftDeleteComponentOutcome.Removed` между lookup и system_key check. Повторный soft-delete уже удалённого типа теперь возвращает `Removed` вместо `Success(impact.valueCount=0)`.
3. **#2 `editComponentType:583-589` + `ComponentValueDao.findLexemesWithMultipleValuesForType`** — conservative approximation (всегда `emptyList()` блок) заменён на real per-lexeme SELECT с deterministic ORDER. Legitimate downgrade (один lexeme с одним value) больше не блокируется; real `List<Long>` доходит до UI preview.

**Logging additions (feature-tag для smoke):**

4. **#3 `cascadeRenameInQuizConfigs:641-660`** — `logger.d(FeatureLogTags.COMPONENT_CONSTRUCTOR, "cascade rename: configId=… refs=N→M write=… oldName=… newName=…")` внутри for-each.
5. **#4 `softDeleteComponentType` inline cascade:705-715** — параллельно #3: `"cascade soft-delete: configId=… refs=N→M write=… removedName=…"`.
6. **#5 `ComponentsManagerUseCaseImpl.resetQuizPickerPrefsBestEffort:185-216`** — добавлены start / per-pref ok / per-pref fail / done логи под feature-tag `COMPONENT_CONSTRUCTOR` параллельно existing module-tag `COMPONENTS_MANAGER` (double-tag pattern).

**Tests:** `:core:core-db-impl:testDebugUnitTest` + `:app:testDebugUnitTest` — PASS sequentially (no parallel gradle), регрессий нет; тесты не правили.

## Ключевые решения

1. **Removed check ПЕРЕД BuiltInProtected** — в `renameComponentType` swap не только bug-fix mapping, но и порядок (parity с post-#1 `softDeleteComponentType`). **⚠ Расхождение с `editComponentType:572-573`** — там порядок обратный (BuiltInProtected → Removed). Sibling CRUD non-uniform, отслежено в `Backlog.md` § ВекторныйПиздеж как задача на унификацию. Для user-defined типов (`systemKey IS NULL`) разницы нет; баг проявляется для теоретического built-in + soft-deleted кейса (защита по разным осям).
2. **Новый DAO `ComponentValueDao.findLexemesWithMultipleValuesForType(typeId): List<Long>`** — deterministic ORDER `MAX(updated_at) DESC, lexeme_id ASC` для тестируемости; LIMIT 3 НЕ в DAO (top-3 vs full filter живёт в Reducer `ImpactedLexemesPreview.InlineOnly` / `InlineWithDrillIn`). Возвращает `List<Long>` (не `List<LexemeIdCount>`) — API contract `CardinalityDowngradeBlocked(List<Long>)` достаточен, count не нужен.
3. **Double-tag pattern в prefs reset** — existing module-tag (`COMPONENTS_MANAGER`) остаётся для granular debug per-pref failure, feature-tag (`COMPONENT_CONSTRUCTOR`) добавлен для adb-фильтрации `###ComponentConstructor###` по smoke. Pattern parity с Migration_012_to_013 infra sub-flow.
4. **Import alias `me.apomazkin.logger.LogTags as FeatureLogTags`** — в `CoreDbApiImpl.kt` (collision с file-local `core_db_impl.LogTags` для `LogTags.DB`) и `ComponentsManagerUseCaseImpl.kt` (collision с `components_manager.LogTags`). Cleaner чем fully-qualified inline на каждом вызове.
5. **`countActiveByTypeId` + `dictionaryIdsForTypeId` НЕ удалены** — они продолжают использоваться в `previewDeletionImpact:660-661`, этот path не трогали.
6. **Cascade soft-delete для null oldName** — не логируется (built-in после Removed-check сюда не попадает, defense-in-depth wrapper `if (oldName != null)` сохранён); лог только внутри for-each.

## Артефакты

**Документы (3):**
- `docs/features/IS481_component_constructor_phase2/data_walkthrough.md` (status: done) — факты о реальной структуре, 3 области по 02_scope, найденные баги impl ≠ API.
- `docs/features/IS481_component_constructor_phase2/data_design_tree.md` (status: done) — DAG 6 узлов (5 independent + 1 с `depends_on=[1]`).
- `docs/features/IS481_component_constructor_phase2/data_implement.md` (status: done) — реализация 6 узлов, тесты PASS, нетривиальные решения.

**Modified source files (3):**
- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt` — узлы #0, #1, #2 call site, #3, #4.
- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/dao/ComponentValueDao.kt` — узел #2 DAO method.
- `app/src/main/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerUseCaseImpl.kt` — узел #5.

**Новый DAO method (1):**
- `ComponentValueDao.findLexemesWithMultipleValuesForType(typeId: Long): List<Long>` — per-lexeme SELECT с `GROUP BY lexeme_id HAVING COUNT(*) > 1 ORDER BY MAX(updated_at) DESC, lexeme_id ASC`.

**Migration tests:** skipped (needs_migration_tests=false — M13 не менялся в phase 2, миграции отдельно покрыты infra sub-flow).

_model: claude-opus-4-7[1m]_
