---
status: done
---

# Summary — infra

## Что сделано

Infrastructure sub-flow phase 2 реализовал 4 узла, подготовив платформу под business / data / UI работу phase 2 IS481 (component_constructor). Прошёл цепочку walkthrough → design_tree → test (skip) → implement → summary без feedback_required: вся работа уложилась в infra scope, прочие слои не затронуты.

**Файлы (4 узла, 1 новый + 3 правки)**:

1. **NEW** `modules/core/logger/src/main/java/me/apomazkin/logger/LogTags.kt` — shared feature-tag объект для phase 2 IS481. Содержит `object LogTags { const val COMPONENT_CONSTRUCTOR: String = "###ComponentConstructor###" }`. Package `me.apomazkin.logger` — consistent с соседними файлами модуля (`LexemeLogger.kt` / `LogLevel.kt` / `LogSink.kt`). Single source of truth для smoke-фильтрации `adb logcat | grep '###ComponentConstructor###'` через 4 потребителей (Migration + UseCaseImpl + 2 screen reducers), все из которых уже имеют dep на `:modules:core:logger`.

2. **MOD** `modules/screen/components_manager/build.gradle.kts` — добавлена строка `implementation(project("path" to ":modules:widget:component_widgets"))` после `:modules:domain:lexeme`. Без этого dep screen-модуль не сможет импортировать widget-файлы из `me.apomazkin.component_widgets.*` после их выноса в shared widget-модуль (UI sub-flow).

3. **MOD** `modules/screen/per_dictionary_components/build.gradle.kts` — симметричная правка узлу 2 (та же одна строка, та же точка вставки). Parity-формат для maintenance-однообразия.

4. **MOD** `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/migrations/Migration_012_to_013.kt` — добавлены 2 import-строки (`android.util.Log` + `me.apomazkin.logger.LogTags`) и 9 вызовов `Log.d(LogTags.COMPONENT_CONSTRUCTOR, "M12→M13 step N <name>: ok")` внутри `migrateImpl(...)` перед каждым `maybeFail(N, failAfterStep)`. Покрытые шаги: renameComponentTypesRemoveDate, addComponentTypesNewColumns, dropUniqueComponentTypesDictName, addComponentValuesNewColumns, dropUniqueComponentValuesLexemeType, createComponentValuesLexemeIdIndex, consolidateLongTextTemplateKey, rewriteTextJson, rewriteImageJson. Dep `:modules:core:logger` уже присутствует в `core/core-db-impl/build.gradle.kts:40`, отдельной gradle-правки не потребовалось.

**Контракты**:
- Feature-tag literal `"###ComponentConstructor###"` зафиксирован — все потребители phase 2 (business / data / UI) обязаны импортировать `me.apomazkin.logger.LogTags.COMPONENT_CONSTRUCTOR`, без хардкода `private const val TAG`.
- Формат log message в Migration: `"M12→M13 step N <name>: ok"` (без affected rows count — MVP-минимум по design tree §Узел 4 «Метрики на шаг»).
- Per-module debug LogTags в screen-модулях (`COMPONENTS_MANAGER` / `PER_DICT_COMPONENTS`) остаются нетронутыми — двойная ось логирования (module-tag для debug + feature-tag для smoke) сохранена.

**Тесты**: не создавались. Решение infra_test (iter 1): 4 узла = pure configuration (deps) + typed const (LogTags) + logging-only поверх неизменной миграции; behavioral surface не расширяется, schema M13 идентична phase 1 baseline. Existing migration test покрывает schema-инвариант M12→M13. Pre-implementation тесты не требуются согласно `test.md` § 1 («Тесты НЕ нужны если: Изменение внутренней реализации без изменения поведения / Конфигурационные изменения без логики»). Корректность build-deps валидируется Gradle в CI pipeline `Build APK`.

**Лог-точки**: 9 точек в Migration M12→M13 (по шагу). Дополнительные start/done wrappers, упомянутые в design tree pseudocode, пропущены — следовал явной формулировке инструкции walkthrough (9 шагов), а не pseudocode (11 точек). Start/done легко добавляются позже без blast radius, если QA попросит.

## Ключевые решения

**F014 (зафиксировано в `02_scope.md` § Логгер) — shared feature-tag в `:modules:core:logger`**, а не per-module duplication. Обоснование: tag используется в 4 потребителях из 3 модулей (`components_manager`, `per_dictionary_components`, `core-db-impl`); duplicating string literal в каждом — нарушение SSOT и риск drift при ребрендинге smoke-фильтра. Все 4 потребителя уже имеют dep на `:modules:core:logger` — incremental cost = 1 файл, 1 const.

**Format asymmetry — feature-tag vs module-tag**. Новый `COMPONENT_CONSTRUCTOR = "###ComponentConstructor###"` следует стандартному `logging.md` convention (тройные решётки, БОЛЬШИЕ буквы), но существующие screen-LogTags (`COMPONENTS_MANAGER = "ComponentsManager"` / `PER_DICT_COMPONENTS = "PerDictComponents"`) намеренно используют module-tag без `###` markers. Это и есть двойная ось: feature-tag = smoke-фильтрация phase 2, module-tag = per-module debug. Сохранено без unification — каждая ось обслуживает свою плоскость анализа.

**Known violation `logging.md` в Migration (Узел 4)** — осознанный compromise. `logging.md` запрещает `android.util.Log` напрямую (всё через `LexemeLogger`), но Migration — `object` без DI. Альтернативы: (a) ввести top-level `var migrationLogger: LexemeLogger?` сетимый из app init — нарушает purity object ещё сильнее; (b) перевести Migration в `class` с ctor-параметром `logger: LexemeLogger` — серьёзный архитектурный рефактор `RoomDatabase.Builder.addMigrations(...)` cite, выходит за scope phase 2 infra. Принятый best-guess (по walkthrough §6.1): прямой `android.util.Log` + shared `LogTags`, фиксация в design tree §Узел 4 Возражение. Если architect-ревьюер потребует — отдельный backlog item «вытащить Migration в class с logger ctor» (per ЖЕЛЕЗНОЕ ПРАВИЛО архив-предложений).

**MVP-минимум для метрики per-step = `"ok"` (без `SELECT changes()` affected rows count)**. Design tree (§Узел 4) явно допускает оба варианта; реальный counts потребовал бы оборачивать каждый `execSQL` в `connection.prepare("SELECT changes()").use { ... }` и менять private step-функции, что выходит за scope logging-only правки. Smoke-фильтр всё равно покажет последовательность шагов и факт достижения `done`. Если QA на phase 2 потребует counts — отдельный backlog item.

**Out of scope (этой phase / этого sub-flow)**:
- DI binding для `flowDictionaries` — по walkthrough §4 не требуется, `dictionaryApi` подтянется через ctor-инжект `UseCaseImpl` (это **business** sub-flow).
- Compose-tooling dep в `:modules:widget:component_widgets/build.gradle.kts` — по walkthrough §3.2 уже транзитивно доступен через `:modules:core:ui` (debugApi composePreview bundle).
- Module registration в `settings.gradle.kts` / `app/build.gradle.kts` — по walkthrough §7 все три модуля уже зарегистрированы.
- Caller-site логирование `quizConfigDao.updateComponentRefs` в `LexemeApiImpl.cascadeRenameInQuizConfigs` — относится к **data** sub-flow (orchestration внутри `CoreDbApiImpl`).
- Start/done wrappers вокруг `migrate()` — формулировка walkthrough фиксирует ровно 9 точек логирования.
- Backlog item «Migration → class с ctor-injected LexemeLogger» — будет заведён архитектурным ревью, если потребуется.

## Артефакты

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_component_constructor_phase2/infra_design_tree.md` — 4 узла, DAG (Узел 4 depends on Узел 1; Узлы 2/3 независимы), детали изменений каждого узла, согласие с гайдами + known violation для Узла 4.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_component_constructor_phase2/infra_test.md` — решение «тесты не нужны» с обоснованием по узлам; existing Migration test покрывает schema-инвариант; behavioral testing передан в business / data / UI sub-flows.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS481_component_constructor_phase2/infra_implement.md` — 4 узла реализованы с точными путями и точками вставки; нетривиальные решения (MVP-метрика per-step, пропуск start/done wrappers, фиксация known violation).

_model: claude-opus-4-7[1m]_
