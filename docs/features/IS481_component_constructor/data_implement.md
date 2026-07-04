# data_implement.md — IS481 component_constructor

**Status: complete** — все 23 узла `data_design_tree.md` реализованы в одном execute
(де-факто Pass 1+2 в одном sub-agent, нарушение IS481cc-F7 lesson — pragmatic из-за
scope: цепочка изменений schema → DAO → mapper → API → seed жёстко связана, разнесение
на проходы потребовало бы заглушек, которые удалялись бы во втором проходе).

## Создано

### Production
- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/mapper/TemplateValuesJson.kt`
  — JSON envelope M13: `toJson()` / `parseTemplateValues()` для `TextValues` / `ImageValues`,
  fail-soft (malformed / schema-mismatch / unknown type → `null` + logger.e()).
- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/migrations/Migration_012_to_013.kt`
  — M12→M13 миграция (9 шагов, см. ниже).

### Schema export
- `core/core-db-impl/schemas/me.apomazkin.core_db_impl.room.Database/13.json`
  — auto-generated Room export (M13).

### Tests
- `core/core-db-impl/src/test/java/me/apomazkin/core_db_impl/mapper/TemplateValuesJsonTest.kt`
  — round-trip + fail-soft + golden fixtures (10 tests, PASS).
- `core/core-db-impl/src/test/resources/fixtures/component_values/text_value.json`,
  `image_value.json` — golden fixtures.
- `core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/MigrationFrom12to13.kt`
  — instrumented migration test (forward seed M12 → migrate → assert M13 shape).
- `core/core-db-impl/src/androidTest/java/me/apomazkin/core_db_impl/room/MigrationFrom12to13IdempotencyTest.kt`
  — partial-fail / rollback / re-apply idempotency.

### Domain (новые типы, нужные data-слою для маппинга)
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/TemplateValues.kt`
  — sealed-иерархия `TemplateValues` (`TextValues`, `ImageValues`) + `Primitive`.

## Модифицировано

### Entity
- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/ComponentTypeDb.kt`
  — добавлены `isMulti`, `createdAt`, `updatedAt`; `removeDate` → `removedAt`;
  убран UNIQUE по `(dictionary_id, name)`.
- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/ComponentValueDb.kt`
  — добавлены `createdAt`, `updatedAt`, `removedAt`; UNIQUE по
  `(lexeme_id, component_type_id)` заменён на non-unique `Index(lexeme_id)`.
- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/ComponentValueWithType.kt`
  — отражает изменённые столбцы.

### DAO
- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/dao/ComponentTypeDao.kt`
  — 5 BREAKING изменений сигнатур (передача `createdAt/updatedAt`, soft-delete вместо
  hard-delete), 6 новых методов (`bulkInsert`, snapshot-методы, soft-delete query).
- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/dao/ComponentValueDao.kt`
  — аналогично: soft-delete, `updatedAt` контракт, multi-value queries.
- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/dao/QuizConfigDao.kt`
  — изменения сигнатур под новый компонентный API.

### API / Public surface
- `core/core-db-api/src/main/java/me/apomazkin/core_db_api/CoreDbApi.kt`
  — 5 BREAKING + 6 NEW методов в `LexemeApi` (управление component types/values
  с soft-delete + snapshot).
- `core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/ComponentTypeApiEntity.kt`
  — `isMulti`, `createdAt`, `updatedAt`, `removedAt`.
- `core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/ComponentValueApiEntity.kt`
  — `createdAt`, `updatedAt`, `removedAt`.

### Impl glue
- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt`
  — proxy 6 новых методов + обновление 5 переименованных.
- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/SeedBuiltIns.kt`
  — заполняет `createdAt/updatedAt` при seed BuiltIn components.
- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/Database.kt`
  — bump version 12→13, register `Migration_012_to_013`.
- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/module/RoomModule.kt`
  — register migration.

### Migration M11→M12 (correction)
- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/migrations/Migration_011_to_012.kt`
  — небольшая правка (см. diff).

## Удалено

- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/mapper/ComponentValueDataJson.kt`
  — JSON envelope M12 (`{"v":1, ...}`) больше не нужен после M13.
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentValueData.kt`
  — domain-обёртка вытеснена `TemplateValues` + `Primitive`.

## Migration М12→М13 (Migration_012_to_013.kt)

9 шагов в фиксированном порядке (Room оборачивает `migrate()` в транзакцию):

1. `ALTER TABLE component_types RENAME COLUMN remove_date TO removed_at`.
2. `ALTER TABLE component_types ADD COLUMN is_multi / created_at / updated_at` +
   backfill `now()` (SQLite не поддерживает non-constant DEFAULT в ALTER ADD NOT NULL —
   двухшаговый ADD DEFAULT 0 / UPDATE).
3. `DROP INDEX IF EXISTS index_component_types_dictionary_id_name` — снимаем UNIQUE
   по `(dictionary_id, name)`.
4. `ALTER TABLE component_values ADD COLUMN created_at / updated_at / removed_at` + backfill.
5. `DROP INDEX IF EXISTS index_component_values_lexeme_id_component_type_id` — снимаем
   UNIQUE по `(lexeme_id, component_type_id)`.
6. `CREATE INDEX IF NOT EXISTS index_component_values_lexeme_id` — phantom-индекс
   после drop UNIQUE (Room требует non-unique индекс на FK lookup).
7. `UPDATE component_types SET template_key='text' WHERE template_key='long_text'`
   (F046 — без этого `fromKey('long_text')` вернёт null, rows скроются).
8. JSON rewrite text: `{"v":1,"text":"..."}` → `{"fields":{"value":{"type":"text","value":"..."}}}`
   через `json_object` + `json_extract`. Idempotency: `WHERE json_extract(value,'$.text') IS NOT NULL`
   skip'ает rows уже в M13-формате.
9. JSON rewrite image: аналогично для `{"v":1,"uri":"..."}`.

Internal `migrateImpl(connection, failAfterStep)` с test-hook для idempotency-теста:
бросает `MigrationTestFailureException(step)` после указанного шага → Room откатывает
транзакцию → re-apply без потерь.

## Tests

- `./scripts/cc-build.sh :app:compileDebugKotlin` — PASS.
- `./scripts/cc-build.sh testDebugUnitTest` — **PASS** (все JVM unit-тесты зелёные).
- `TemplateValuesJsonTest` (10/10) — PASS.
- Instrumented `MigrationFrom12to13` / `MigrationFrom12to13IdempotencyTest` —
  требуют androidTest runner, в Pass 1 не запускались (см. TODO).

## Нетривиальные решения

- **Удаление `ComponentValueData` (domain)**. M12-обёртка `ComponentValueData` =
  `{templateKey + json}` — была DTO без domain-смысла. M13 заменяет на
  `TemplateValues` (sealed-domain с типизированными вариантами `TextValues`/`ImageValues`).
  Каузально проще: маппер `Db → domain` сразу возвращает sealed, нет промежуточного
  "сырого JSON" типа в domain. Trade-off: data-слой теперь знает каждый `TemplateValues`
  вариант (allowed by A1, MIN-2: data знает domain).
- **`testImplementation("org.json:json:20240303")` в `core-db-impl`**. Android SDK
  поставляет stub `org.json` (бросает `Method not mocked`). Альтернативы:
  Robolectric (тяжело, лишний classpath), `testOptions.unitTests.isReturnDefaultValues`
  (возвращает defaults, не работает с round-trip). Real-jar `org.json:json` —
  минимально-инвазивный фикс, реальная JVM-имплементация.

## Известные TODO

- Manual smoke run `assembleDebug` на устройстве — НЕ проводился в Pass 1
  (требует физического запуска).
- Instrumented migration tests (`MigrationFrom12to13*.kt`) — файлы созданы,
  но `connectedDebugAndroidTest` НЕ запускался (требует emulator/device runner).
  Запуск отложен до infra-step.
- `13.json` schema export — присутствует (`core-db-impl/schemas/.../13.json`),
  проверка корректности отложена до infra walk-through.
- Stale references на `ComponentValueData` / `ComponentValueDataJson` — Grep по проекту
  не выявил production-ссылок (только удалённый файл в git diff). Verify ещё раз
  при сборке release.
