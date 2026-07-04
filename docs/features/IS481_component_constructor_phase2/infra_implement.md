# infra_implement

## Узлы реализованы

### #1 LogTags.kt [+]
- Path: `modules/core/logger/src/main/java/me/apomazkin/logger/LogTags.kt`
- Content: `object LogTags { const val COMPONENT_CONSTRUCTOR: String = "###ComponentConstructor###" }`
- Package: `me.apomazkin.logger` (consistent с `LexemeLogger.kt` / `LogLevel.kt` / `LogSink.kt` в этом же модуле).

### #2 components_manager/build.gradle.kts [~]
- Path: `modules/screen/components_manager/build.gradle.kts`
- Added: `implementation(project("path" to ":modules:widget:component_widgets"))`
- Точка вставки: сразу после `implementation(project("path" to ":modules:domain:lexeme"))` и перед `implementation(project("path" to ":core:core-resources"))` — parity с design tree §Узел 2.

### #3 per_dictionary_components/build.gradle.kts [~]
- Path: `modules/screen/per_dictionary_components/build.gradle.kts`
- Added: `implementation(project("path" to ":modules:widget:component_widgets"))`
- Точка вставки: симметрично узлу 2 (после `:modules:domain:lexeme`, перед `:core:core-resources`).

### #4 Migration_012_to_013.kt [~]
- Path: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/migrations/Migration_012_to_013.kt`
- Added imports:
  - `import android.util.Log`
  - `import me.apomazkin.logger.LogTags`
- В `migrateImpl(...)` перед каждым из 9 вызовов `maybeFail(N, failAfterStep)` добавлен
  `Log.d(LogTags.COMPONENT_CONSTRUCTOR, "M12→M13 step N <name>: ok")` — 9 строк (steps 1..9).
- Шаги: `renameComponentTypesRemoveDate` / `addComponentTypesNewColumns` /
  `dropUniqueComponentTypesDictName` / `addComponentValuesNewColumns` /
  `dropUniqueComponentValuesLexemeType` / `createComponentValuesLexemeIdIndex` /
  `consolidateLongTextTemplateKey` / `rewriteTextJson` / `rewriteImageJson`.

## Нетривиальные решения

1. **Метрика per-step = "ok" (без affected rows count).** Design tree (§Узел 4 «Метрики на шаг»)
   явно допускает MVP-минимум — «логирует только step-name без count, MVP-минимум». Реальный
   `SELECT changes()` после каждого DML потребовал бы оборачивать каждый `execSQL` в `connection.prepare(...).use { ... }`
   и менять step-функции, что выходит за scope logging-only правки. Smoke-фильтрация по
   `###ComponentConstructor###` всё равно покажет последовательность шагов и факт прохождения
   до `done`. Если QA на phase 2 потребует counts — отдельный backlog item.

2. **`Log.d` без `migrate() start/done` wrappers.** Design tree pseudocode показывает дополнительные
   `Log.d(... "migration M12→M13: start" / "done")` в `override fun migrate(...)`. Однако
   инструкция шага explicitly перечисляет ровно 9 step-функций (без start/done). Применил
   формулировку инструкции (9 точек логирования внутри `migrateImpl`), а не pseudocode design tree
   (11 точек). Start/done легко добавляются позже, если QA попросит — minimal blast radius.

3. **Known violation `logging.md` («запрещено `android.util.Log` напрямую»).** Зафиксирован в
   design tree §Узел 4 Возражение как осознанный compromise (Migration — `object` без DI,
   рефактор в `class` с ctor-injected `LexemeLogger` вне scope phase 2). Не дублирую обсуждение
   тут — backlog item для будущего рефакторинга упомянут в design tree.

## Тесты

infra_test (iter 1) решил: тесты не нужны (4 узла = config + const + logging-only поверх
неизменной миграции). Соответственно ничего не запускалось.

## log_messages

- infra_implement (iter 1): 4 узла реализованы — Узел 1 (LogTags.kt NEW), Узлы 2/3 (build.gradle.kts +1 строка каждый), Узел 4 (Migration logging: +2 imports, +9 Log.d вызовов перед maybeFail). Метрика per-step = "ok" (без affected rows count) — MVP-минимум по design tree §Узел 4. Start/done wrappers пропущены — следую инструкции шага (9 точек), не pseudocode (11).

_model: claude-opus-4-7[1m]_
