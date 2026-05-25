---
name: migration_test
output: migration_test.md
input_criteria:
  - design_tree.output существует
output_criteria:
  - миграционные тесты написаны / обновлены под изменения schema
  - тесты компилируются под `androidTest`
  - в migration_test.md описано что было сделано
---

Прочитай `design_tree.output` — он содержит план изменений schema и миграций. Напиши или обнови миграционные тесты для них.

Миграционные тесты — отдельный пласт от обычных unit-тестов:
- **Инфраструктура:** `androidTest/` (instrumented), не `test/`
- **Helper:** `androidx.room.testing.MigrationTestHelper`
- **Запуск:** `./gradlew connectedAndroidTest`, не `testDebugUnitTest`

## Что прочитать перед написанием

1. **Гайд проекта:** `docs/guides/testing-migrations.md` — обязательное чтение, описывает паттерны и каркас для PolyTrainer'а.
2. **Существующие миграционные тесты:** ищи в `core/core-db-impl/src/androidTest/` — пойми принятый стиль.
3. **Schema JSON:** `core/core-db-impl/schemas/<dbName>/<version>.json` — Room генерит их для каждой версии. Тест валидирует переход между ними.

## Что должно быть покрыто тестами

Минимум для каждой новой миграции `from N to N+1`:
- **Тест миграции** — пустая БД на версии N → миграция → проверка структуры на N+1
- **Тест с данными** — БД с типовыми данными на N → миграция → данные не потеряны и корректно мигрированы
- **Тест граничных случаев** — null'ы, пустые таблицы, edge cases затронутые миграцией

Для CASCADE / FK constraints — отдельный тест что constraint реально срабатывает после миграции.

## Что НЕ делать

- Не писать unit-тесты (это работа generic `test` шага в Business sub-flow).
- Не реализовывать саму миграцию — это работа шага `implement`. Ты пишешь только тесты, которые ДОЛЖНЫ упасть до реализации (TDD).
- Не запускать тесты сам — runner запустит на шаге `check` master flow (через `connectedAndroidTest` или эквивалент в `forgeflow.yml.commands`).

## Формат `migration_test.md`

```markdown
# Migration tests — <ticket>

## Затронутые миграции

- `from N to N+1` — <короткое описание изменения schema>

## Созданные / обновлённые тесты

- `core/core-db-impl/src/androidTest/.../MigrationsTest.kt` — добавлены тесты:
  - `migrate_from_<N>_to_<N+1>_emptyDb` — пустая БД, валидация структуры
  - `migrate_from_<N>_to_<N+1>_withData` — данные сохранены и мигрированы
  - `migrate_from_<N>_to_<N+1>_cascadeDelete` — (если применимо)

## TDD контракт

Все эти тесты сейчас **падают** (миграции ещё не реализованы — это шаг `implement`).
После `implement` все тесты должны пройти.
```
