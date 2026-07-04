# Review: 05_migration_strategy.md

3 параллельных subagent'а: Migration Correctness / Bundled SQLite Risk / Impact Analysis. Каждый finding — `Verify:` через встроенные Claude tools.

## Сводка

| Направление | Critical | Minor |
|---|---|---|
| Migration Correctness | 4 | 4 |
| Bundled SQLite Risk | 3 | 4 |
| Impact Analysis | 5 | 3 |
| **Итого** | **12** | **11** |

**ГЛАВНОЕ: реальный scope миграции значительно больше чем заложено в `05`.** Это не «БД переехала», это **полный рефакторинг lexeme-домена**.

**Топ-блокеры (требуют решения до начала реализации):**

1. **`payload` vs `value` в SQL миграции `05`** — accepted решение `value`, но в SQL остался `payload`. Миграция упадёт на runtime.
2. **Room legacy builder vs новый Driver API** — `Room.databaseBuilder(context, klass, name)` может игнорировать `.setDriver()`. Bundled не активен → миграция падает на `ALTER TABLE DROP COLUMN`.
3. **WordCard mate refactor — отдельная фича** (~400+ строк, 20+ Msg, 14 reducer tests). Документ преподносит как «как обычный user-defined» — нереалистично.
4. **CoreDbApi требует переработки** — 7 методов завязанных на translation/definition + новый API contract нужен (атомарность `addLexemeWithTranslation`).
5. **Quiz crash на definition-only лексемах** — нужна очистка `write_quiz` или валидация.
6. **Domain Lexeme дублируется в 3+ модулях** (wordcard / quiz/chat / dictionaryTab). Каждый со своим Translation/Definition value class. Три параллельных рефакторинга.
7. **APK size +4-5 MB**, не +1MB — нет ABI splits.
8. **Auto-backup риск** — bundled SQLite + WAL может расходиться при restore.

---

## Migration Correctness Review

### 🔒 [critical] Несоответствие имени колонки: `payload` vs `value`
**Triage:** → закрыто. Massive replace `payload` → `value` сделан в `05` SQL миграции (2 INSERT'а).

### ❌ [critical] Шаг 5.1 — нет dedup для словаря с существующим user-defined «Definition»
**Triage:** → rejected. Defensive coding не нужно — тесты миграции должны быть строго однопроходные (v11 БД → migrate → v12 проверка). `OR IGNORE` маскировал бы неправильные тесты вместо требования их правильности.

### ❌ [critical] Шаг 5.2 — NULL subquery без NOT NULL гарантии
**Triage:** → rejected. Defensive от ошибки разработчика при правке миграции. Миграция write-once — сломанную правку поймает миграционный тест.

### 🔒 [critical] § 5.1 GROUP BY без агрегата — SQLite-специфичное расширение
**Triage:** → закрыто. `GROUP BY` заменён на `SELECT DISTINCT` в § 5.1. Чистая семантика dedup, без SQLite-специфичности.

### 🔒 [minor] Шаг 7 проверка имён индексов
**Triage:** → закрыто. В чеклист `05` добавлен пункт «Имена индексов в миграции должны совпадать с автогенерируемыми Room (pattern `index_<table>_<col1>_<col2>`). Проверить через `12.json` snapshot».

### 🔒 [minor] Edge case dedup в тестах
**Triage:** → закрыто. В § «Тестовые сценарии / Согласованность» добавлены п.12-15: count match для translation/definition/orphan + partial UNIQUE check.

### 🔒 [minor] § 6 противоречие чеклист vs SQL
**Triage:** → закрыто. Чеклист в `05` обновлён: «ALTER TABLE DROP COLUMN (bundled SQLite 3.45+, без recreate-таблицы)».

### 🔒 [minor] Edge cases: orphan lexeme + огромный definition
**Triage:** → закрыто. В § «Edge cases» добавлены п.8 (orphan lexeme) и п.9 (огромный definition).

### 🔒 [minor] § 0 «Транзакция» — Room автоматически
**Triage:** → закрыто. § 0 переписан: «Room автоматически оборачивает `migrate()` в транзакцию. Явных `beginTransaction()` не нужно».

---

## Bundled SQLite Risk Analysis

### 🔒 [critical] Room legacy builder vs новый Driver API
**Triage:** → закрыто. В чеклист `05` добавлен явный пункт с before/after кодом RoomModule.kt — переход на KMP-builder Room 2.7+ через `Room.databaseBuilder<Database>(...).setDriver(BundledSQLiteDriver())`. Плюс логирование `SELECT sqlite_version()` в Callback.onOpen дев-сборки. Помечено как **блокер** для всей миграции.

### ❌ [critical] APK +4-5 MB, не +1MB — нет ABI splits
**Triage:** → rejected. Релиз через Play AAB — Play автоматически делит per-abi, пользователь получает ~1MB. Сплит `splits { abi { ... } }` нужен только для legacy APK-релиза, не для AAB. Debug-APK universal с +5MB — ОК (debug и так тяжёлый без R8).

### ❌ [critical] Auto-backup восстановит старый формат — bundled может его не открыть атомарно
**Triage:** → rejected (YAGNI). Релизов в Play пока нет, восстанавливаемых данных нет. SQLite форматы файлов backward-совместимы. Отключение backup = плохой UX (потеря словарей при reinstall). Если в практике появятся crash-reports от restored-устройств — добавим `dataExtractionRules` отдельной миграцией.

### 🔒 [minor] R8 strip нативных методов BundledSQLiteDriver
**Triage:** → закрыто. В чеклист `05` добавлен пункт с явными keep-правилами для `androidx.sqlite.driver.bundled.**` + native methods.

### 🔒 [minor] Старые миграции 1→11 vs bundled — регресс-тестирование
**Triage:** → закрыто. В чеклист `05` добавлен пункт «Прогнать `AllMigrationTest` под bundled driver» — убедиться что 1→11 корректны.

### ❌ [minor] WAL по умолчанию — bundled включает автоматически
**Triage:** → rejected. Связан с auto-backup risk (rejected). Если backup-проблемы появятся в практике — добавим тогда.

### ❌ [minor] Откат от bundled — заложить phased rollout
**Triage:** → rejected. Overengineering для текущего объёма. Bundled — proven Room paттерн, риск краш у пользователей маловероятен.

---

## Migration Impact Analysis

### Сводка callsites

- `lexeme.translation` / `.translation` (field access): **~60 production + ~50 тестов**.
- `lexeme.definition` / `.definition`: **~55 production + ~70 тестов**.
- **Концентрация:** `wordcard/` (mate — 80+, UseCaseImpl — 11, screen — 4), `dictionaryTab/` (UseCaseImpl 4, widget 8), `quiz/chat/` (QuizGameImpl 8), `core-db-impl/` (20+), `core-db-api/` (12).
- **stattab** и **quiztab** — нулевые matches (не затронуты).
- **Search не затронут** — оперирует только `words.value` (документ был неверен).
- Существующий `Lexeme.builtIn(...)` extension — отсутствует, пока design-намерение.

### 🔒 [critical] Document грубо недооценил scope в WordCard
**Triage:** → закрыто через `@Deprecated`-обёртки. IS481 = только data-слой + shim. Старые методы (`addLexemeWithTranslation` etc.) остаются `@Deprecated` обёртками над generic. Domain Lexeme получает computed `translation`/`definition` extension. **Mate / UI не меняются** в IS481. Wordcard mate refactor вынесен в backlog (`docs/Backlog.md` § Архитектура) — триггер: появление UI для user-defined компонентов. См. § «API совместимости» в чеклисте `05`.

### 🔒 [critical] CoreDbApi.LexemeApi требует полной переработки
**Triage:** → закрыто через Impact #1. Новые generic-методы добавляются, старые специфичные (translation / definition) остаются как `@Deprecated` обёртки. Атомарность `addLexemeWithTranslation` сохраняется через wrapper → новый атомарный `addLexemeWithBuiltInComponent`. `LexemeApiEntity` получает поле `components`; старые `translation/definition` остаются как `@Deprecated` computed (читаются из components).

### 🔒 [critical] Quiz crash на definition-only лексемах
**Triage:** → вынесено в [`07_quiz_strategy.md`](07_quiz_strategy.md). Это вопрос архитектуры quiz, не миграции БД. 4 варианта (A: definition в built-in / B: skip definition-only / C: поиск по имени / D: generic quiz). Решение TBD.

### 🔒 [critical] Domain Lexeme дублируется в 3+ модулях
**Triage:** → вынесено в backlog (`docs/Backlog.md` § Архитектура — «Domain unification: `modules/domain/lexeme`»). Новый модуль `modules/domain/lexeme` (новая категория `modules/domain/`). Желательно до IS481 — иначе `@Deprecated` computed extensions в 3 местах. Не блокирует IS481 (тогда tech debt больше, но миграция возможна).

### 🔒 [critical] Search использует только `term.value`, документ неверен
**Triage:** → закрыто. § «Search» в `05` переписан: «Не затронут. Поиск через `WordDao.searchTerms*` фильтрует только по `words.value`. Расширение на text-компоненты — опциональная отдельная фича».

### 🔒 [minor] `canRemoveTranslation` / `canRemoveDefinition` — invariant ломается
**Triage:** → закрыто. В чеклист `05` добавлен пункт: старые методы остаются как `@Deprecated`; новый generic `canRemoveComponent(componentId): Boolean = components.size > 1`.

### ❌ [minor] Tests — ~14 wordcard reducer tests
**Triage:** → rejected (на IS481). mate не меняется в IS481 (через `@Deprecated` обёртки), тесты wordcard mate продолжают работать. Refactor mate тестов — вместе с фичей «Wordcard mate refactor» (в backlog).

### 🔒 [minor] `WriteQuizDb.lexemeId` — не задевается миграцией
**Triage:** → закрыто. В § «Влияние на код» добавлен подраздел `write_quiz`: schema не меняется, runtime — см. `07_quiz_strategy.md`.

---

## Triage

Заполняется conductor'ом с пользователем.
