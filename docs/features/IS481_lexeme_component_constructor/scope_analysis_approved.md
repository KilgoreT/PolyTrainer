# scope_analysis — approved findings (iteration 1)

Findings подтверждённые инквизитором на итерации 1. Нужно закрыть в следующей итерации.

## Критические (требуют закрытия)

### F001 [architect] critical — DatasourceEffectHandler misplaced

**Проблема:** `DatasourceEffectHandler` отнесён к `app/`, но реально лежит в `modules/screen/wordcard/mate/`.

**Что исправить:** В секции «Затронутые файлы» переместить упоминание `DatasourceEffectHandler` из под `app/` в подсекцию `modules/screen/wordcard/`.

### F002 [architect] critical — QuizChatUseCaseImpl wrong location

**Проблема:** `QuizChatUseCaseImpl.fetchData` указан под `modules/screen/quiz/chat/`, но реально файл в `app/.../di/module/quizchat/QuizChatUseCaseImpl.kt`. В модуле только interface.

**Что исправить:** В подсекции `modules/screen/quiz/chat/` упоминать только `QuizChatUseCase` (interface) + `QuizGameImpl.toQuizItem`. `QuizChatUseCaseImpl.fetchData` переместить в подсекцию `app/`.

### F003 [architect] critical — RoomModule wrong layer

**Проблема:** `RoomModule.provideDatabase` отнесён к `app/`, реально в `core/core-db-impl/.../di/module/RoomModule.kt`.

**Что исправить:** Перенести упоминание `RoomModule.provideDatabase` (с `.addMigrations(Migration_011_to_012)`) из подсекции `app/` в подсекцию `core/core-db-impl/`.

### F004 [architect] critical — DictionaryDao не существует

**Проблема:** В описании `addDictionary` транзакции упомянут несуществующий `DictionaryDao`. Метод реально в `WordDao` (`core_db_impl/room/WordDao.kt:29`).

**Что исправить:** Заменить «`DictionaryDao.addDictionary`» на «`WordDao.addDictionary`» в подсекции `core/core-db-impl/`.

### F007 [qa_engineer] critical — db_migration аспект отсутствует

**Проблема:** Секция «Аспекты» содержит только `context_output` флаги (`needs_tests`, `needs_migration_tests`, `feature_has_ui_contract`). Стандартный аспект `db_migration` (per scope_analysis.md:86) для миграционных фич не указан.

**Что исправить:** Добавить в секцию «Аспекты» (ниже `feature_has_ui_contract`):
- `db_migration` — миграция M11→M12, удаление колонок `lexemes.translation/.definition`, добавление таблиц `component_types` / `component_values` / `quiz_configs`.
- Также рассмотреть: `public_contract_change` (CoreDbApi.LexemeApi сигнатуры меняются — translation @Deprecated, definition wrappers удалены).
- `new_dependency` (новая Gradle dep edge `core-db-api` → `modules/domain/lexeme`, MIN-2).

## Минорные (улучшения)

### F006 [architect] minor — context_output блок отсутствует

**Проблема:** В артефакте нет явного блока со всеми 8 переменными `context_output` (`infra_touched`, `business_touched`, `ui_touched`, `data_touched`, `needs_tests`, `needs_migration_tests`, `feature_has_ui_contract`, `spec_filename`).

**Что исправить:** Добавить в конец артефакта (перед `_model:` строкой) секцию `## context_output`:

```yaml
infra_touched: false
business_touched: true
ui_touched: true
data_touched: true
needs_tests: true
needs_migration_tests: true
feature_has_ui_contract: true
spec_filename: wordcard.md
```

### F013 [qa_engineer] minor — Atomicity rollback тест не указан

**Проблема:** Тест на rollback атомарности `addLexemeWithBuiltInComponent` (FK violation → rollback) — зафиксирован в alignment Test gaps batch как обязательный, в `needs_tests` отсутствует.

**Что исправить:** В Аспектах раздела `needs_tests` явно упомянуть: «Atomicity rollback тест для `addLexemeWithBuiltInComponent` (FK violation → rollback `lexemes` + `write_quiz`) — регрессия IS479 F1».

## Rejected findings (для справки, без действий)

F005 (DAO subdir), F008 (interrupted migration restart — MIN-12c rejected defensive), F009 (orphan уже покрыт), F010 (spec chars / size — sub-flow), F011 (UI compose tests — sub-flow), F012 (verify prerequisite vs in-flow — sub-flow).
