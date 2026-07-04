# IS481 — Схлопывание миграций БД (2 → 1)

**Тикет:** IS481
**Ветка:** `IS481_lexeme_component_constructor` (коммитим сюда же)

## Контекст

Ветка содержит **две** Room-миграции, рождённые внутри неё:
- **M11 → M12** (`Migration_011_to_012.kt`) — bundled SQLite, composite JSON rewrite, DROP UNIQUE `(lexeme_id, component_type_id)` и пр.
- **M12 → M13** (`Migration_012_to_013.kt`) — ADD `is_multi` / `created_at` / `updated_at` в `component_types` + backfill; seed built-ins под новую схему.

Ветка релизится **одной фичей / одним релизом**. **Гейт подтверждён заказчиком: v12 и v13 НИГДЕ не релизились** (ни прод, ни бета, ни чужие устройства как released) — у пользователей БД максимум на **v11** (релизный baseline). Значит промежуточный путь `11→12→13` никому в проде не нужен.

## Цель

Схлопнуть две миграции в **одну** `11 → 12` (финальная v12 = текущая схема v13). Один bump версии от релизного baseline, одна миграция, один migration-тест.

**Не меняем поведение схемы** — итоговая структура БД идентична текущей v13, меняется только «упаковка» (одна миграция вместо двух, версия 13→12).

## Подход — TDD (тесты ПЕРВЫМИ)

1. **Написать исчерпывающий migration-тест** `MigrationFrom11to12` (instrumented, `connectedDebugAndroidTest`), покрывающий ВЕСЬ объединённый переход на реальных данных v11:
   - composite JSON rewrite значений компонентов;
   - DROP UNIQUE `(lexeme_id, component_type_id)` (поддержка multi);
   - ADD `is_multi`/`created_at`/`updated_at` + backfill (built-in `translation` → `is_multi=0`, timestamps = now);
   - seed built-ins;
   - сохранность данных (значения/лексемы не теряются), removed_at rename.
2. **Прогнать red** — объединённой `Migration(11→12)` ещё нет → тест красный (или идёт по старому 11→12→13).
3. **Схлопнуть код**, прогнать green.

- Слить тела `Migration_011_to_012` + `Migration_012_to_013` → одна `Migration(11, 12)`; удалить `Migration_012_to_013.kt`.
- Версия БД **13 → 12** везде: `AppDatabase`/`RoomModule` (`version=`), exported schema JSON (`schemas/13.json` → `12.json` с финальной структурой), регистрация миграций.
- `SeedBuiltIns` — под версию 12.
- androidTest: заменить `MigrationFrom12to13` (+ при наличии промежуточных) на единый `MigrationFrom11to12`; обновить `AllMigrationTest`.

## Заодно: переименовать колонку БД `is_multi` → `is_multiple`

Раз миграция всё равно объединяется и v12 **не релизилась** — переименовываем колонку прямо в объединённой миграции (БЕЗ отдельной rename-миграции). После этого Kotlin-поле `isMultiple` и колонка `is_multiple` — единое имя (сейчас рассинхрон: поле `isMultiple`, колонка `is_multi`).

Затронуть:
- `ComponentTypeDb.kt` — `@ColumnInfo(name = "is_multi")` → `"is_multiple"`.
- Объединённая миграция: `ADD COLUMN is_multiple ...` (вместо `is_multi`).
- Весь SQL с `is_multi`: `SeedBuiltIns`, `ComponentValueDao` (cardinality-check), `CoreDbApiImpl` (сообщение/lookup), exported schema JSON.
- Migration-тест: `SELECT is_multiple ...`.

> Замечание: это меняет инвариант «колонка не трогается» из предыдущего брифа — теперь намеренно трогаем, т.к. есть объединённая миграция и нет релизов v12.

## Что меняется в коде (детали выше)

## Гейт / риски

- ✅ **Гейт:** v12/v13 не релизились — подтверждено. (Если бы релизились — схлопывать нельзя.)
- **Dev-БД** на v12/v13 (своё устройство, коллеги) перестанут мигрировать → нужен fresh install / clear data на дев-девайсах. Прод (v11) не затронут.
- **Обязательный прогон на эмуляторе** (`connectedDebugAndroidTest`) — schema-critical; destructive fallback есть (prereq), но цель — корректная миграция, не fallback.
- Room schema-JSON: следить, чтобы exported schema v12 точно соответствовала результату объединённой миграции (Room validate identity hash).

## Открытый вопрос (решить в начале)

- **Финальная версия:** `11→12` (renumber, рекомендую — чистый single-bump) **vs** оставить `Migration(11,13)` одним классом с версией 13 (меньше переименований schema JSON, но версия «13» при одной фиче выглядит странно). Рекомендация — **11→12**.

## Бриф

_TBD — детализировать после старта (точный список SQL объединённой миграции, поля schema JSON)._
