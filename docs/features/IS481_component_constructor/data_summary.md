---
status: done
---

# Summary — data sub-flow (IS481 component_constructor)

Data sub-flow реализовал миграцию M12→M13 + полную перестройку component_types/component_values
схемы под business contract (5 BREAKING + 6 NEW методов `LexemeApi`), включая JSON envelope
rewrite, soft-delete pattern, cardinality safety и явные timestamps. Все 23 узла
`data_design_tree.md` закрыты в одном execute-проходе (де-факто Pass 1+2 слиты — chain
schema → DAO → mapper → API → seed жёстко связан).

## Что сделано

### Created (production)

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/mapper/TemplateValuesJson.kt`
  — новый JSON envelope для M13: `TemplateValues.toJson()` + `parseTemplateValues(json, template, logger)`
  для `TextValues` / `ImageValues`. Fail-soft контракт: malformed JSON / schema-mismatch /
  unknown primitive type → `null` + `logger.e(...)` через `LexemeLogger`. Формат
  `{"fields": {"value": {"type": "text|image", "value|uri": "..."}}}`.

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/migrations/Migration_012_to_013.kt`
  — 9-шаговая миграция (см. секцию ниже).

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-db-impl/schemas/me.apomazkin.core_db_impl.room.Database/13.json`
  — auto-generated Room schema export (KSP при bump version=13).

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/TemplateValues.kt`
  — sealed `TemplateValues` (`TextValues`, `ImageValues`) + `Primitive` (создано вместе с
  business-фазой, нужно data-слою для маппинга).

### Modified (entity + DAO + API + impl)

- `entity/ComponentTypeDb.kt` — добавлены `isMulti: Boolean`, `createdAt: Date`, `updatedAt: Date`;
  rename `removeDate → removedAt`; убран UNIQUE на `(dictionary_id, name)`; `toApiEntity()`
  стал nullable (F019 fail-soft skip на unknown templateKey).
- `entity/ComponentValueDb.kt` — добавлены `createdAt: Date`, `updatedAt: Date`, `removedAt: Date?`;
  UNIQUE `(lexeme_id, component_type_id)` заменён на non-unique `Index("lexeme_id")`
  (phantom index для производительности `getForLexeme`).
- `entity/ComponentValueWithType.kt` — `toApiEntity(logger)` принимает logger, возвращает nullable
  (skip rows с unknown template / malformed JSON).
- `entity/LexemeDbEntity.kt` — `toApiEntity(logger)` делает post-load filter на
  `value.removedAt == null && type.removedAt == null` (Room @Relation не поддерживает WHERE)
  + `mapNotNull` для fail-soft skip.
- `room/dao/ComponentTypeDao.kt` — audit `remove_date → removed_at` в 5 queries; новые методы:
  `renameUserDefined`, `findActiveUserDefinedByName`, `findActiveGlobalByName`,
  `countActivePerDictByName`, `flowAllUserDefined`, `flowUserDefinedForDictionary`.
- `room/dao/ComponentValueDao.kt` — добавлен `removed_at IS NULL` filter в active queries;
  новые методы: `insertSingleSafe(@Transaction)` с cardinality guard, `softDeleteByTypeId`
  (cascade), `countActiveByTypeId`, `dictionaryIdsForTypeId`, `aggregatedValueCountPerType`,
  `aggregatedValueCountPerTypeForDict`, `typeDictPairs` + DTO `TypeIdCount` / `TypeDictPair`.
- `room/dao/QuizConfigDao.kt` — `flowAllConfigs` / `getAllConfigs` / `updateComponentRefs(id, newRefs)`
  для cascade очистки/переименования refs.
- `room/WordDao.kt` — `addLexemeWithComponents` signature: `List<Pair> → List<ComponentValueDb>`;
  cardinality pre-check (F169/F170) вынесен из DAO в `LexemeApiImpl`.
- `core-db-api/CoreDbApi.kt` — 5 BREAKING (`ComponentValueData → TemplateValues`) +
  6 NEW методов в `LexemeApi` (flow snapshots, create/rename/softDelete user-defined types,
  previewDeletionImpact).
- `core-db-api/entity/ComponentTypeApiEntity.kt`, `ComponentValueApiEntity.kt` —
  добавлены `createdAt/updatedAt/removedAt` (+ `isMulti` для type).
- `CoreDbApiImpl.kt` — реализация 5 BREAKING + 6 NEW методов; все multi-statement операции
  обёрнуты в `database.withTransaction { ... }` (F173 convention); добавлена ctor-зависимость
  `database: Database` + import `androidx.room.withTransaction`.
- `room/SeedBuiltIns.kt` — INSERT под M13 schema (`is_multi=0, created_at=updated_at=now`).
- `room/Database.kt` — version 12 → 13.
- `room/migrations/Migration_011_to_012.kt` — frozen `seedBuiltIns_v12()` private helper
  (F044 — separate fresh-install seed M13 от upgrade-path с v11).
- `di/module/RoomModule.kt` — `.addMigrations(Migration_011_to_012, Migration_012_to_013)`.

### Deleted

- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/mapper/ComponentValueDataJson.kt`
  — старый M12 envelope `{"v":1,...}`.
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentValueData.kt`
  — domain DTO вытеснена `TemplateValues` (последний узел data-DAG, после полного rebind data callsite'ов).

### Tests

- `core-db-impl/src/test/java/me/apomazkin/core_db_impl/mapper/TemplateValuesJsonTest.kt`
  — 10 unit-тестов (round-trip text/image, schema mismatch, malformed JSON, unknown type,
  golden fixtures). **PASS 10/10.**
- `core-db-impl/src/test/resources/fixtures/component_values/{text_value.json, image_value.json}`
  — golden round-trip fixtures.
- `core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/MigrationFrom12to13.kt`
  — 13 instrumented кейсов (A translation-only, B user-defined text, C image, D long_text
  consolidation, D2 mixed, E soft-deleted RENAME, F/G drop UNIQUE, H fail-soft valid-no-key
  JSON, I phantom Index, J timestamps backfill + invariant `updated_at >= created_at`,
  K special chars, L FK CASCADE chain). **Файл создан, runner не запускался в Pass 1.**
- `core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/MigrationFrom12to13IdempotencyTest.kt`
  — M3 (failAfterStep=1 RENAME + failAfterStep=8 JSON rewrite, F186 min coverage) +
  M3b WHERE-filter partial-state idempotency без rollback (F175). **Файл создан, runner
  не запускался.**

### Build status

- `./scripts/cc-build.sh :app:compileDebugKotlin` — PASS.
- `./scripts/cc-build.sh testDebugUnitTest` — PASS (все JVM unit-тесты зелёные).

## Ключевые решения

- **`database.withTransaction { ... }` convention для всех multi-DAO операций**
  (F173, Open Q #1 resolved). Reason: cross-DAO (componentTypeDao + componentValueDao +
  quizConfigDao + wordDao) требует RoomDatabase context — `@Dao interface` не имеет
  cross-DAO access. Применено в `addComponentValue`, `updateComponentValue`,
  `addLexemeWithComponents`, `createUserDefinedComponent` (atomic insert N rows),
  `renameWithQuizConfigsCascade`, `softDeleteAtomic`. См.
  `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_component_constructor/data_design_tree.md`
  §iter 3-4 log.

- **Soft-delete pattern** — DROP UNIQUE на `(dictionary_id, name)` + `(lexeme_id, component_type_id)`,
  enforce identity invariants в UseCase через `findActive*` queries с `removed_at IS NULL`
  filter (Option B; партиционный UNIQUE отброшен — Room не поддерживает partial index в
  schema export). Two-prong SELECT (`findActiveUserDefinedByName` per-dict +
  `findActiveGlobalByName` global) — раздельные queries из-за SQL `NULL = NULL → UNKNOWN`
  (F032).

- **Cardinality safety (F169/F170/F171/F172)** — `is_multi=false` enforce в `ComponentValueDao.insertSingleSafe(@Transaction)`
  + explicit pre-check `type.removedAt == null` на write paths в `LexemeApiImpl` внутри
  `withTransaction { ... }` (TOCTOU guard). `WordDao` делает только cascading INSERTs;
  pre-check ответственность caller'а (cross-DAO lookup).

- **Fail-soft JSON mapper** — `parseTemplateValues` возвращает `null` на malformed JSON
  / schema mismatch / unknown type + `logger.e(...)`. `ComponentValueWithType.toApiEntity()`
  и `ComponentTypeDb.toApiEntity()` стали nullable; caller (`LexemeDbEntity.toApiEntity`,
  `LexemeApiImpl.getComponentTypes`) делает `mapNotNull` — soft-deleted и corrupted rows
  невидимы для UI / quiz / wordcard.

- **Frozen seed v12 для upgrade-path (F044)** — `Migration_011_to_012` получил private
  `seedBuiltIns_v12()` с literal SQL под M12-схему (без `is_multi/created_at/updated_at`,
  с `remove_date`); `SeedBuiltIns.kt` обновлён под M13 (fresh install path). Цепочка
  M11→M12→M13 завершает данные built-in `translation` row корректно (через ALTER ADD COLUMN
  backfill в M13).

- **Phantom `Index(lexeme_id)` после drop UNIQUE** — composite UNIQUE
  `(lexeme_id, component_type_id)` покрывал leading column для index lookup;
  после DROP `getForLexeme` ушёл бы в full-scan. Non-unique `index_component_values_lexeme_id`
  создаётся в шаге 6 миграции.

- **9-step migration с test-hook** — `Migration_012_to_013.migrateImpl(connection, failAfterStep)`
  бросает `MigrationTestFailureException(step)` (reused из `Migration_011_to_012`) для
  M3 idempotency теста. Step 7 (`long_text → text` consolidation) обязан выполниться
  ДО step 8 (text JSON rewrite), иначе `WHERE template_key='text'` не подхватит former
  `long_text` rows. JSON rewrite через `json_object` + `json_extract` на bundled SQLite ≥ 3.45.
  Idempotency через `WHERE json_extract(value, '$.text') IS NOT NULL` фильтр — M13-формат
  имеет `text` вложенный в `fields.value`, top-level `$.text` возвращает NULL → skip.

## Что вне scope

- **Запуск instrumented migration tests** (`MigrationFrom12to13*.kt`) — файлы созданы,
  но `connectedDebugAndroidTest` НЕ выполнен в Pass 1 (требует emulator/device runner).
  **Должен переехать в `data_implement` Pass 2 либо отдельный infra-ticket** для проверки
  на CI / эмуляторе. До запуска instrumented тестов M12→M13 миграция не имеет
  hands-on validation на реальном Android SQLite.

- **Manual smoke run `assembleDebug`** на устройстве (проверка upgrade-path c v11/v12
  installations) — отложен до infra walkthrough / publish-ui фазы.

- **`13.json` schema export validity** — файл присутствует, но cross-проверка через
  `runMigrationsAndValidate(13, ...)` требует instrumented runner (см. выше).

- **Stale-reference sweep** на `ComponentValueData` / `ComponentValueDataJson` —
  Grep по проекту не выявил production-ссылок (только удалённый файл в git diff);
  release build verify рекомендован.

- **Prefs cleanup финальный shape** — `previewDeletionImpact.affectedPrefs` data возвращает
  candidate `dictionaryIds` (из `affectedQuizConfigs`); фактический pref-key match
  (`quiz_picker_dict_<id>`) делает UseCase `ComponentsManagerUseCaseImpl` (business-фаза).

## Артефакты

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_component_constructor/data_design_tree.md`
  — DAG из 23 узлов, 9 tiers, audit покрытия scope / aspects / open questions,
  4 iter log (F168-F174 fixes).
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_component_constructor/data_migration_test.md`
  — 13 instrumented cases A-L + M3/M3b idempotency, helpers, TDD контракт, 4 iter
  log (F175-F199 fixes).
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_component_constructor/data_implement.md`
  — лог реализации (Created / Modified / Deleted / Tests / Нетривиальные решения /
  Известные TODO).

_model: claude-opus-4-7[1m]_
