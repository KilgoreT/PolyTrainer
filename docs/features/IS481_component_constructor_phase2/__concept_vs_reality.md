# Концепт vs Реальность — IS481 phase 2

> Snapshot phase 2 на момент REVIEW.md (28 findings, 10 critical). Branch `IS481_lexeme_component_constructor`, baseline commit `f0f8284`. Сверка: brief (`task.md`) ↔ phase 1 concept (`concept/*.md`) ↔ реальная реализация.

## 1. Edit component + cardinality downgrade

| Аспект | Содержание |
|---|---|
| **Brief acceptance** | `task.md:11-26` — новый Msg.OpenEditDialog/EditNameChange/EditTemplateChange/EditMultiToggle/SubmitEdit/CloseEditDialog/EditResult, новый `EditOutcome` с 7 вариантами (Success/NameEmpty/SameScopeCollision/CrossScopeCollision/CardinalityDowngradeBlocked/TemplateImmutable/Failure), epoch correlation, `SQL SELECT … GROUP BY lexeme_id HAVING COUNT(*) > 1` для downgrade check, cascade `quiz_configs.component_refs`, Edit template запрещён (TemplateImmutable). |
| **Concept reference** | `template_model.md` § Open Q9 (cardinality downgrade — жёсткий запрет, preview top-3 + drill-in, без bulk actions), § Open Q10 (template immutable после релиза — `ComponentType.name` свободно меняется, `Field.name` / `fields` zashite). |
| **Реализация** | `EditOutcome` sealed: `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/EditOutcome.kt:21-31` (9 вариантов — включая `BuiltInProtected` + `Removed`, что превышает brief на 2). UseCaseImpl: `app/src/main/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerUseCaseImpl.kt:144-172`. Data: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt:565-631` + `findLexemesWithMultipleValuesForType` DAO (`ORDER BY MAX(updated_at) DESC, lexeme_id ASC`). UI: `modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/dialogs/EditComponentDialog.kt` + `CardinalityDowngradePreviewWidget.kt`. Reducer: 7 Edit-веток с epoch guard + 4-way mutual-exclusion. |
| **Соответствие** | ⚠ Частично — функционально работает, но обширный contract drift. |
| **Драфты/Drift** | (a) **A2 / REVIEW.md:14-19** — Template-immutability gate отсутствует в UseCaseImpl несмотря на F017. KDoc `EditOutcome.kt:8-10` явно утверждает «возвращается БЕЗ обращения к data API если `template != current.template`», но `ComponentsManagerUseCaseImpl.kt:144-172` делегирует на data layer без `current.template` lookup. Defense-in-depth check только на `CoreDbApiImpl.kt:582`. Verify: Read `ComponentsManagerUseCaseImpl.kt:144-172` → no current lookup. (b) **A3 / REVIEW.md:21-26** — порядок защит в `editComponentType:573-574` (BuiltInProtected первым, Removed вторым) обратен `renameComponentType:532-533` и `softDeleteComponentType:690-691` (Removed первым). Для built-in+soft-deleted типа edit вернёт `BuiltInProtected`, rename/softDelete — `Removed`. Verify: Read `CoreDbApiImpl.kt:530-533, 571-574, 688-691`. (c) **A4 / REVIEW.md:28-32** — `EditDialog.canSubmit` не учитывает `impactedLexemesPreview != null` → blocked downgrade повторяется в loop. (d) **B3 / REVIEW.md:57-60** — cardinality SELECT вне `withTransaction`, race окно между check и UPDATE. (e) **Y6 / REVIEW.md:108-112** — `ImpactedLexemesPreview` sealed моментально разворачивается в плоские примитивы в `ComponentsManagerScreen.kt:216-221` и `State.kt:136-145` — избыточная обёртка. (f) **Y9 / Y1** — `EditNameError` mate-локальный enum дублирует domain `NameError` (5 точек дублирования). |

## 2. Multi-dict scope picker

| Аспект | Содержание |
|---|---|
| **Brief acceptance** | `task.md:29-41` — `availableDictionaries: List<DictionaryListEntry>` в State, `Msg.DictionariesLoaded(list)`, `flowDictionaries(): Flow<List<DictionaryEntry>>` метод, radio «На все / На конкретные» + chip-list multi-select, submit с empty PerDictionaries → disabled, в PerDict экране scope hardcoded остаётся как есть. |
| **Concept reference** | `ui_placement.md:48-51` § «Создать новый» — scope переключатель «На все словари» (global, `dictionaryId=null`) / «На конкретные» (выбор одного или нескольких dict; в БД = N независимых записей). |
| **Реализация** | State: `availableDictionaries` + `selectedDictionaryIds: Set<Long>` + `CreateDictionaryToggle/DictionariesLoaded` Msg. UseCase: `ComponentsManagerUseCase.flowDictionaries(): Flow<List<DictionaryApiEntity>>` (`modules/screen/components_manager/.../deps/ComponentsManagerUseCase.kt:98`). UseCaseImpl: pure delegate на `dictionaryApi.flowDictionaryList()` (`ComponentsManagerUseCaseImpl.kt:174-175`). FlowHandler: `modules/screen/components_manager/.../mate/DictionariesFlowHandler.kt` (auto-subscribe через `subscribe(scope, send)`). UI: `CreateComponentDialog.kt` с `HostVariant.Manager` + chip-list. Wiring screen→state: `ComponentsManagerScreen.kt:162-165`. |
| **Соответствие** | ❌ Не работает в production. |
| **Драфты/Drift** | **A1/B1 — BLOCKER REVIEW.md:8-12, 47-48** — `DictionariesFlowHandler` создан и инжектится в `DatasourceEffectHandler`, но **НЕ зарегистрирован** в `effectHandlerSet` ViewModel. Verify: Read `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/ComponentsManagerViewModel.kt:42-48` → `effectHandlerSet = setOf(datasourceHandler, flowHandler, uiHandler, navHandlerFactory.create(navigator))`, где `flowHandler: AllUserDefinedTypesFlowHandler`. `Mate.subscribeToLongRunningFlows()` подписывает только handler'ы из `effectHandlerSet` → `DictionariesFlowHandler.subscribe(...)` никогда не вызывается → `availableDictionaries` навсегда `emptyList()` → chip-list пустой → submit с `Scope.PerDictionaries` недоступен. `SubscribeDictionaries` Effect нигде не эмитится Reducer'ом (Grep по Reducer → 0 матчей emit). Главный пункт phase 2 (multi-dict picker) полностью сломан в Manager-экране несмотря на 75 PASS-tests (B8 — false positive coverage). |

## 3. Shared widget module

| Аспект | Содержание |
|---|---|
| **Brief acceptance** | `task.md:44-61` — наполнить пустой `:modules:widget:component_widgets/` (dialogs/ + widgets/ + templates/ с `TextWidget`, `ComponentBlock`, `ComponentByTemplate` resolver). Вынести 6 дублированных widgets из обоих screen-модулей. Добавить `EditComponentDialog`. Build PASS, existing tests PASS. |
| **Concept reference** | `typed_views.md` § «Где жить» — `modules/widget/component_widgets/` (Tier 2): composable widgets per template (`TextWidget`, `QuoteWithSourceWidget`, …). `ComponentBlock(name, content)` wrapper. Resolver `ComponentTemplate -> @Composable (TemplateValues) -> Unit` — exhaustive `when` через sealed `TemplateValues`. |
| **Реализация** | 14 NEW source files в `modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/`: templates/ (3 — TextWidget, ComponentBlock, ComponentByTemplate), widgets/ (7 — ComponentTemplateLabel, NameErrorLabel, ComponentsEmptyStateWidget, CreateComponentFab, UserDefinedRowWidget, PerDictRowWidget, CardinalityDowngradePreviewWidget), dialogs/ (4 — Rename/Delete/Create/Edit). Verify: `ls modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/{templates,widgets,dialogs}/` → 14 файлов. Старые widget-пакеты в `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/widget/` и `per_dictionary_components/.../widget/` удалены (Verify: оба `ls` → пустой stdout). `HostVariant.Manager|PerDict` enum в CreateComponentDialog управляет видимостью scope picker. Build deps: `infra_summary.md:13-14` — `implementation(project(":modules:widget:component_widgets"))` в обоих screen `build.gradle.kts`. |
| **Соответствие** | ✅ Полностью реализовано. |
| **Драфты/Drift** | Незначительные: (a) `lexemeLabel` placeholder TODO — `CardinalityDowngradePreviewWidget` рендерит «Lexeme #N» вместо реальных имён (требует UseCase `getLexemesByIds`, в backlog). (b) `onShowAllImpacted` no-op в обоих screens (drill-in destination не реализован — Y12). (c) `EditNameError.toLabelRes()` host-local в каждом screen (a-la copy-paste — A5/Y1). (d) `DeletionImpactRef` / `DictionaryRef` declared inline в файлах dialog'ов (избегаем dep `:core:core-db-api` в widget module). (e) **Concept compliance:** `ComponentByTemplate` resolver написан (`when (template)` exhaustive), но `TemplateValues` sealed имеет только `TextValues` — composite templates (`QuoteWithSourceValues`, `ImageWithCaptionValues`) намеренно вне scope phase 2 (`task.md:143`). |

## 4. RenameOutcome.BuiltInProtected → Removed conflation

| Аспект | Содержание |
|---|---|
| **Brief acceptance** | `task.md:64-72` — добавить `Removed` variant в `RenameOutcome` + `DeleteOutcome` + новый `EditOutcome`. UseCase: `type.systemKey != null → BuiltInProtected`; `type.removedAt != null → Removed`; иначе rename. Reducer: snackbar «Компонент удалён» + close dialog для `Removed`. Unit-тесты per outcome. |
| **Concept reference** | `deletion_concept.md:38-39` § `component_types` — `softDelete` с защитой `WHERE system_key IS NULL`. Концепция различает built-in (системный, нельзя удалить) и soft-deleted (юзерский, уже удалён). |
| **Реализация** | `RenameOutcome.Removed` (`RenameOutcome.kt:17`). `DeleteOutcome.Removed` (`DeleteOutcome.kt:14`). `EditOutcome.Removed` (`EditOutcome.kt:29`). API mirrors: `core/core-db-api/.../entity/ComponentOutcomeApiEntity.kt` — `RenameComponentOutcome.Removed`, `SoftDeleteComponentOutcome.Removed`, `EditComponentOutcome.Removed`. Data impl: `CoreDbApiImpl.kt:532` (rename — Removed раньше BuiltInProtected), `:690` (softDelete — Removed раньше BuiltInProtected, data_summary.md #1 bug-fix), `:574` (edit — BuiltInProtected раньше Removed — **inconsistency**). UseCaseImpl mapping: `ComponentsManagerUseCaseImpl.kt:99, 140, 161` (passthrough). Reducer: `Removed` ветки в `RenameResult/DeleteResult/EditResult` (см. business_summary.md § Manager mate). |
| **Соответствие** | ⚠ Частично — outcome variants есть, но порядок защит inconsistent. |
| **Драфты/Drift** | (a) **A3 — REVIEW.md:21-26** — `editComponentType:573-574` использует обратный порядок относительно rename/softDelete. Для built-in+soft-deleted типа edit вернёт `BuiltInProtected`, rename/softDelete — `Removed` → разные snackbar на одной реальности. `data_summary.md:27 п.1` ложно утверждает parity. (b) **B6 — REVIEW.md:69-72** — `getById` returns null fallback везде `BuiltInProtected` (3 callsites: `:530-531, :571-572, :688-689`) — confusing для теоретического hard-deleted кейса. Лучше `NotFound`/`Removed`. (c) **CreateOutcome намеренно НЕ трогали** (business_summary.md §10) — Create не оперирует existing `type.id`, soft-deleted name-collision покрывается existing Same/CrossScopeCollision (асимметрия задокументирована). |

## 5. Feature-tag + Migration logs

| Аспект | Содержание |
|---|---|
| **Brief acceptance** | `task.md:75-92` — `LogTags.COMPONENT_CONSTRUCTOR = "###ComponentConstructor###"` в shared logger. Логи в каждом UseCase методе (create/rename/softDelete/preview/edit). Логи в Migration_012_to_013 per step. Логи в `QuizConfigDao.updateComponentRefs` cascade. Логи в `resetQuizPickerPrefsBestEffort`. Сохранить параллельно module-tags. |
| **Concept reference** | Нет прямого упоминания в concept docs — это технический requirement для smoke verify. Связан с `template_model.md` § «Принципы реализации» п.2 (fail-soft с логом). |
| **Реализация** | (a) `LogTags`: `modules/core/logger/src/main/java/me/apomazkin/logger/LogTags.kt:3-5` — `object LogTags { const val COMPONENT_CONSTRUCTOR: String = "###ComponentConstructor###" }`. (b) Migration: `core/core-db-impl/.../room/migrations/Migration_012_to_013.kt:55-91` — 9 точек логирования через `Log.d(LogTags.COMPONENT_CONSTRUCTOR, "M12→M13 step N <name>: ok")`. (c) DAO cascade rename: `CoreDbApiImpl.kt:646-649` — `logger.d(FeatureLogTags.COMPONENT_CONSTRUCTOR, "cascade rename: configId=… refs=N→M write=… oldName=… newName=…")`. (d) DAO cascade soft-delete: `CoreDbApiImpl.kt:708-711` — `"cascade soft-delete: configId=… refs=N→M write=… removedName=…"`. (e) Prefs reset: `ComponentsManagerUseCaseImpl.kt:184-216` — start / per-pref ok / per-pref fail / done логи под double-tag (module + feature). |
| **Соответствие** | ⚠ Частично. |
| **Драфты/Drift** | (a) **Module-tags в каждом UseCase методе** (createUserDefinedComponent / renameComponent / softDeleteComponent / previewDeletionImpact / editComponent) пишут лог через `LogTags.COMPONENTS_MANAGER` (module-tag), но **feature-tag НЕ дублируется на каждом public методе** как требует brief `task.md:80`. Сейчас feature-tag только в: prefs reset, cascade rename, cascade soft-delete, Migration. Methods без feature-tag для smoke filter: create/rename/edit/softDelete success path, previewDeletionImpact. (b) **Migration log message MVP-минимум** — `"<step>: ok"` без `SELECT changes()` affected-rows count (infra_summary.md §«MVP-минимум»). Brief требует «rows affected / backfilled / rewritten / skipped». Признано осознанным compromise — counts потребовали бы оборачивать каждый execSQL в `connection.prepare("SELECT changes()").use{}` с private-step рефактором. (c) **Known violation** `logging.md` — Migration использует `android.util.Log` напрямую вместо `LexemeLogger` (Migration — `object` без DI; перевод в `class` с ctor-injected logger — out-of-scope phase 2). (d) **Double-tag pattern** реализован только в prefs reset; остальные UseCase methods и data cascade — single-tag. |

## Out-of-scope от концепта (фиксированный долг)

Перечислены в `task.md:139-144` как `Out of scope phase 2` + распылены по REVIEW.md → backlog (14 пунктов):

- **Composite templates** (`QuoteWithSourceValues`, `ImageWithCaptionValues`, …) — concept `template_model.md` § Domain types фиксирует sealed `TemplateValues` с N вариантами; реализован только `TextValues`. Фундамент (`ComponentByTemplate` resolver, `ComponentBlock`, `TextWidget`) заложен. Rationale: MVP per `template_model.md` Open Q6 («только TEXT в MVP конструктора»).
- **Recovery UI** (корзина / архив для soft-deleted типов) — concept `deletion_concept.md` § «Где раздел recovery» явно делегирует в отдельную фичу. `removed_at` накапливается бесконечно (нет TTL).
- **Background TTL hard-delete** — concept `deletion_concept.md` § TTL фиксирует «в этой фиче TTL не реализуется, бесконечное хранение». Job — отдельная фича.
- **Per-dictionary disable built-in** (например словарь без translation) — concept `ui_placement.md` § Open Q2 («read-only глобально на MVP», disable — отдельная фича позже).
- **Soft-delete для `dictionaries` / `words` / `lexemes`** — concept `deletion_concept.md` § Финальное распределение scope: в этой фиче только `component_types` + `component_values`. Остальные — отдельные фичи (recovery критичен).
- **Соединение `samples` / `hints` с template-моделью** — concept `deletion_concept.md` § «Не трогаем сейчас»: отложено до миграции в template'ы.
- **Real `lexemeLabel` resolve** (ui_summary.md backlog) — UseCase query `getLexemesByIds(ids): List<LexemeRef>` для замены placeholder «Lexeme #N» в `CardinalityDowngradePreviewWidget`.
- **Drill-in destination «Показать все N»** (ui_summary.md backlog, Y12) — bottom-sheet либо `ImpactedLexemesScreen`. Сейчас `onShowAllImpacted` no-op.
- **Реактивность `flowAllUserDefinedTypesWithUsage` на `component_values` changes** (B2) — текущие suspend-методы в combine/map не триггерят re-emit при INSERT/UPDATE `component_values`. Архитектурная задача (Flow-варианты в DAO через `@Query`). В backlog.
- **`PrimaryTextButtonWidget` vararg formatArgs overload** (ui_summary.md backlog) — для drill-in label с `%1$d` counter; сейчас inline M3 `TextButton`.
- **`LexemeStyle.LabelM/LabelL`** в theme — отсутствуют, fallback на `BodyS/BodyLBold`.
- **Унификация `UserDefinedRowWidget` + `PerDictRowWidget`** (ui_summary.md backlog) в единый `ComponentRowWidget(row: ComponentRowState)` с sealed `ComponentRowState`.
- **Y2/Y3/Y4/Y5 dead code cleanup** — `addLexemeWithTranslation/updateLexemeTranslation` (Verify: 0 production callers — Grep подтвердил, только тестовый `updateLexemeTranslationText` неоднороден), `WordCardUseCase.addComponentValue/updateComponentValue/deleteComponentValue` (impl сам признаёт «generic path не имеет caller'ов»), `Field / PrimitiveType / ComponentTemplate.fields` (Verify: 0 production imports), `Primitive.Color` (Verify: 0 production references) — все speculative future API без consumers.
- **Migration_012_to_013 → `class` с ctor-injected logger** (infra_summary.md «Out of scope») — устранение `android.util.Log` direct call в Migration.

## Итог

- **3/5 полностью реализовано:** п.3 (shared widget module ✅), п.5 (feature-tag + Migration logs — ⚠ MVP-минимум по rows count, single-tag в части мест) — частично; п.4 (Removed variants — ⚠ inconsistency в порядке защит A3).
- **2/5 частично, **критичный блокер** в одном:**
  - **п.2 multi-dict picker — ❌ BLOCKER A1/B1** — `DictionariesFlowHandler` не подписан в Mate, главная фича phase 2 нерабоча в Manager despite 75 PASS-tests. Релиз заблокирован.
  - **п.1 Edit + cardinality downgrade — ⚠** функционально работает, но contract drift: A2 (gate отсутствует despite F017), A3 (inconsistency защит), A4 (canSubmit dirty-loop), B3 (race), Y1/Y6 (избыточные обёртки).
- **Open questions:**
  - A1/B1, A2/Y9, A3, Y1, Y6 — критичные fix-in-feature (13 findings → закрыть в фиче по triage REVIEW.md:189).
  - B2 — крупная архитектурная задача реактивности, отдельный тикет.
  - 14 findings → backlog (Y2-Y5 dead code, A5 дублирование Edit*State, B5-B8 minor, Y7-Y15).
  - 1 finding → rejected (Y16 test hook in migration).
- **Concept compliance gaps** относительно phase 1 concept docs:
  - `ui_placement.md` § «Создать новый» multi-dict scope — формально реализован, но runtime broken (A1/B1).
  - `template_model.md` § Open Q9 cardinality downgrade — реализован, preview top-3 + drill-in есть, но Y6 показывает что sealed-обёртка избыточна.
  - `template_model.md` § Open Q10 template immutable — defensive check на data есть, но UseCase gate (F017) отсутствует (A2).
  - `deletion_concept.md` § component_types: Removed-vs-BuiltInProtected различение — реализовано, но A3 inconsistency в edit-методе.

_model: claude-opus-4-7[1m]_
