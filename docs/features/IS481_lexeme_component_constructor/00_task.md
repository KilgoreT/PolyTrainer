## Задача

Превратить набор компонентов лексемы из **жёстко зашитого** (translation / definition) в **конструктор**: домен оперирует обобщёнными `ComponentType` + `ComponentValue` с выбранным шаблоном (`text` / `long-text` / `image`). Translation остаётся built-in типом, definition мигрирует в user-defined per-dictionary тип. Существующие пользовательские translation и definition сохраняются — миграция переносит их в новую модель без потери данных.

Фактический скоуп IS481 main — **data-слой + API совместимости + точечные правки wordcard/quiz** под новый домен. UI создания / редактирования user-defined типов **не делаем** (отдельная фича в backlog). Через `@Deprecated` обёртки и shim-поля `Lexeme.translation` / `.definition` mate, UI wordcard и большинство reducer-тестов остаются нетронутыми.

## Что НЕ делаем (пока)

- UI управления типами (создание / переименование / удаление user-defined типов).
- Шаринг типов между пользователями / устройствами.
- Расширение набора шаблонов (audio / video / link / table).
- Сложные валидации шаблонов.
- Рефактор mate wordcard на generic компоненты (триггер — после удаления shim в отдельной backlog-фиче).

## Контекст

**GitHub issue:** https://github.com/KilgoreT/PolyTrainer/issues/481

**Prereq уже выполнен** — фича `IS481_lexeme_component_constructor_vPrepared` (закоммичена) закрыла инфраструктуру:
- Подключён `BundledSQLiteDriver` (`androidx.sqlite:sqlite-bundled`) + KMP-builder Room 2.7+.
- Все 10 historical migrations v1→v11 и migration test infrastructure **дропнуты целиком** (verify через tags 0.1.0 / 0.1.5 — DB version = 11 у всех internal testers).
- `RoomModule.provideDatabase` — `.fallbackToDestructiveMigration(dropAllTables = true)` + `Database.Callback.onDestructiveMigration(connection)` логирует через `LexemeLogger.e` → Crashlytics (для edge case pre-0.1.0 install).
- ProGuard keep-rules под bundled driver добавлены.

**IS482 уже закрыл** Lexeme domain unification — `modules/domain/lexeme` существует (PR #483, merged `6d3499c`) с unified `Lexeme` / `Translation` / `Definition` / `LexemeId`. IS481 этот модуль **расширяет** (добавляет `ComponentValue`, `ComponentType`, `BuiltInComponent`, `ComponentTemplate`, `ComponentValueData`, `QuizConfig`, `ComponentTypeRef`, computed extensions).

**Canonical sources для downstream шагов (НЕ дублировать здесь):**

- [`plan/_alignment_decisions.md`](plan/_alignment_decisions.md) — лог решений. Ключевые: **AGG-12** (canonical migration strategy — одна новая `Migration_011_to_012` directly на `SQLiteConnection` API, никаких legacy миграций), **AGG-1** (только `BuiltInComponent.TRANSLATION`, `DEFINITION` удалён), **AGG-4/5** (реверс — QuizConfig wire в IS481), **AGG-6** (definition wrappers удалены, callsites переписаны на generic + UI блок), **AGG-2** (маппер в `app/`), **AGG-10** (QuizConfig в lexeme domain как trade-off), B4/C2 (shim Lexeme.translation/.definition).
- [`plan/02_design_sketch.md`](plan/02_design_sketch.md) — обзор изменений по слоям (data, api, domain, mate, quiz).
- [`plan/03_database_design.md`](plan/03_database_design.md) — Entity / DAO / Database / FK / CASCADE.
- [`plan/04_builtin_strategy.md`](plan/04_builtin_strategy.md) — seed built-in types + lookup стратегия.
- [`plan/05_migration_strategy.md`](plan/05_migration_strategy.md) — чеклист M11→M12, шаги миграции, тестовый план.
- [`plan/06_mapping_design.md`](plan/06_mapping_design.md) — мапперы DB ↔ API ↔ Domain, shim invariant, JSON helper.
- [`plan/07_quiz_strategy.md`](plan/07_quiz_strategy.md) — QuizConfig table, lookup, invariants F1/F4/F5/F6.

_model: claude-opus-4-7 (1M context)_
