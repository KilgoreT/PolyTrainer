# global_code_review.md — IS481 component_constructor

Commit: f0f8284 (working tree) | Date: 2026-06-21
Branch: IS481_lexeme_component_constructor

## Что реализовано

### Infra (5 шагов done)

37 узлов design_tree → 17 create + 17 modify. Новые модули:
`:modules:widget:component_widgets` (Tier 2 widget, без DI/KSP),
`:modules:screen:components_manager` (TEA + Dagger + AssistedInject),
`:modules:screen:per_dictionary_components` (зеркально + ctor-арг `dictionaryId: Long`).
Расширены existing hosts: `SettingsTab` (`+ Msg.OpenComponentsManager` + effect/handler/navigator),
`DictionaryAppBar` (`+ Msg.OpenPerDictionaryComponents(dictionaryId)` + effect/handler/navigator),
`:modules:screen:main` (`CompositionRoot` + 2 новых `*ScreenDep`, расширение 4 host-сигнатур,
shared route `per_dict_components/{dictionaryId}` в Vocabulary.kt + extension
`NavHostController.goToPerDictionaryComponents` для Quiz.kt / Statistic.kt, выделение
`Statistic.kt` в отдельный NavGraphBuilder ext).
В `app/`: 2 NavigatorImpl modify + 2 new + 4 DI-класса + AppComponent / CompositionRootImpl /
MainRouter / `app/build.gradle.kts` / `settings.gradle.kts` wiring.
Тесты: `SettingsTabReducerTest` (новый, 1 case) + расширение `DictionaryAppBarReducerTest`
(+2 случая), всего 9/9 PASS.

### Business (9 шагов done)

Domain (`:modules:domain:lexeme`): новые `Primitive` / `PrimitiveType` / `Field` /
`TemplateValues` (sealed: `TextValues`, `ImageValues`) / `Scope` / `NameError` /
`AffectedQuizConfig` / `DeletionImpact` / `ComponentUsage` / `UserDefinedTypesSnapshot` /
`PerDictionarySnapshot` / `CreateOutcome` / `RenameOutcome` / `DeleteOutcome`;
модификация `ComponentTemplate` (drop `LONG_TEXT`, nullable `fromKey`, computed `fields`),
`ComponentType` (+ `isMulti / createdAt / updatedAt`, `removeDate → removedAt`),
`ComponentValue` (rebind на `TemplateValues`).

Data-API (`:core:core-db-api`): новые DTOs `DictionaryTypesSnapshot` /
`UserDefinedTypesUsageSnapshot` / `ComponentOutcomeApiEntity`; синхронизация
`ComponentTypeApiEntity` + `ComponentValueApiEntity` с domain; **5 BREAKING сигнатур**
LexemeApi (`addLexemeWithBuiltInComponent`, `addLexemeWithUserDefinedComponent`,
`addLexemeWithComponents`, `addComponentValue`, `updateComponentValue` — `ComponentValueData`
→ `TemplateValues`) + **6 NEW методов** (`flowAllUserDefinedTypesWithUsage`,
`flowUserDefinedTypesForDictionary`, `createUserDefinedComponent`, `renameComponentType`,
`previewDeletionImpact`, `softDeleteComponentType`).

UseCase + Mate: `ComponentsManagerUseCaseImpl` (CRUD + prefs cleanup best-effort) +
`PerDictionaryComponentsUseCaseImpl` (read собственный, write делегирует через DI
composition); два полных Mate-стека для `ComponentsManagerScreen` /
`PerDictionaryComponentsScreen` (State / Msg / UiEffect / DatasourceEffect / FlowHandler /
NavigationEffect / Reducer) — exhaustive `when`, epochId-pattern для race в `*Result`,
correlation by `typeId` для preview, `CancellationException` re-throw invariant.
Migration 6 business call-sites M12→M13 + mechanical rebind 11 test files (`LexemeMapper`,
`WordCardUseCaseImpl`, `WordCardUseCase`, `quiz/chat/QuizGameImpl`, `Lexeme.kt`).

Spec published: `docs/features-spec/component-constructor.md`.

Тесты: ~146 тестов across 8 файлов (CM Reducer 68, PerDict Reducer 62, CM UseCase 25,
PerDict UseCase 6, 2× DatasourceEffectHandler 14+14, 2× FlowHandler 4+4) — ALL PASS.

### UI (6 шагов done)

19 узлов: 14 widgets + 2 modify + 2 Screens. Resources: `ic_hammer.xml` /
`ic_components.xml` + ~33 string-ключа в en/ru.
Tier 1 primitives: `LexemeRadioRow` (generic radio-row с `Modifier.selectable`,
F162) + `ErrorStateWidget` (generic centered error + Retry, F163) в `:modules:core:ui`.
14 widgets: `ComponentsManageWidget`, `ComponentsToolsIconButton` (молоток),
`CreateComponentFab`, `ComponentsEmptyStateWidget`, `UserDefinedRowWidget`,
`PerDictRowWidget`, 2× `CreateComponentDialog` / `RenameComponentDialog` /
`DeleteComponentConfirmDialog` (дубликаты между CM / PerDict — F157 UI-extensions
`ComponentTemplate.labelRes()` / `NameError.labelRes()`).
2 modify: `DictionaryAppBar.actions` (icon-button перед picker, видим при
`currentDict != null && !isLoading`), `SettingsTabScreen` (вставка
`ComponentsManageWidget`). 2 Screens replaced: реальный UI поверх Mate-обвязки
с Scaffold + branching + 3 dialog-overlays + `BackHandler` + `LaunchedEffect(snackbarState)`.
Mate-contract extension: `Msg.OnRetryClick` + `DatasourceEffect.Load*` для re-subscribe
pattern в FlowHandler (`unsubscribe()` + пересоздание подписки с сохранённым scope+send).

Spec updated: `## UI Layout` section in `component-constructor.md` (~496 lines inline).

### Data (5 шагов done)

23 узла design_tree → 9 tiers.
**Migration M12→M13** (`Migration_012_to_013.kt`, 9-шаговая, test-hook через
`failAfterStep`): rename `remove_date → removed_at` (только в `component_types`),
DROP UNIQUE на `(dictionary_id, name)` + `(lexeme_id, component_type_id)`, add `is_multi /
created_at / updated_at` с backfill через `strftime('%s','now') * 1000`, phantom
`Index(lexeme_id)` после drop UNIQUE, template-key consolidation
`UPDATE component_types SET template_key='text' WHERE template_key='long_text'`,
JSON envelope rewrite через SQLite `json_object` / `json_extract` (idempotent через
`WHERE json_extract(value, '$.text') IS NOT NULL`).
Frozen `seedBuiltIns_v12()` в `Migration_011_to_012.kt` (F044) для upgrade-path
v11→v12→v13.
**Entities + DAOs**: `ComponentTypeDb` (+`isMulti/createdAt/updatedAt`,
`removeDate → removedAt`, drop UNIQUE на `(dictionary_id, name)`, `toApiEntity()` →
nullable для fail-soft skip unknown template); `ComponentValueDb`
(+`createdAt/updatedAt/removedAt`, drop UNIQUE → phantom `Index(lexeme_id)`);
`ComponentValueWithType.toApiEntity(logger)` nullable; `LexemeDbEntity.toApiEntity(logger)`
post-load filter `value.removedAt == null && type.removedAt == null` + `mapNotNull`;
`ComponentTypeDao` (audit `removed_at` в 5 queries + 6 new методов:
`renameUserDefined / findActiveUserDefinedByName / findActiveGlobalByName /
countActivePerDictByName / flowAllUserDefined / flowUserDefinedForDictionary`);
`ComponentValueDao` (фильтр `removed_at IS NULL` в active queries + `insertSingleSafe(@Transaction)`
cardinality guard + soft-delete cascade + aggregation методы + 2 DTOs);
`QuizConfigDao` (+ `flowAllConfigs / getAllConfigs / updateComponentRefs(id, newRefs)`).
**JSON mapper**: новый `TemplateValuesJson.kt` (`TemplateValues.toJson()` +
`parseTemplateValues(json, template, logger): TemplateValues?`, fail-soft контракт);
удалён `ComponentValueDataJson.kt` и `ComponentValueData.kt`.
`CoreDbApiImpl`: 5 BREAKING + 6 NEW методов, все multi-DAO operations обёрнуты в
`database.withTransaction { ... }` (F173 convention).
`SeedBuiltIns` обновлён под M13. `Database.version = 13`. Schema export `13.json`.
`RoomModule.addMigrations(Migration_011_to_012, Migration_012_to_013)`.

Тесты: `TemplateValuesJsonTest` (10/10 PASS — round-trip text/image, schema mismatch,
malformed JSON, unknown type, golden fixtures). Instrumented:
`MigrationFrom12to13.kt` (13 кейсов A-L) + `MigrationFrom12to13IdempotencyTest.kt`
(M3 + M3b) — файлы написаны, runner НЕ запущен (требует emulator).

## Quality gates

- **Compile**: PASS (все модули, `:app:compileDebugKotlin` зелёный).
- **Lint**: PASS (`./scripts/cc-build.sh :app:lintDebug` → BUILD SUCCESSFUL EXIT=0).
- **Unit tests**: PASS (`./scripts/cc-build.sh testDebugUnitTest` → BUILD SUCCESSFUL EXIT=0).
  Все unit tests зелёные (business ~146 + data 10 + infra 9 + 11 mechanical rebind).
- **Build APK**: PASS (`./scripts/cc-build.sh assembleDebug` → BUILD SUCCESSFUL EXIT=0).
- **Instrumented tests**: NOT RUN. `MigrationFrom12to13.kt` (13 кейсов) +
  `MigrationFrom12to13IdempotencyTest.kt` (M3/M3b) написаны со спецификацией но
  требуют `connectedDebugAndroidTest` на emulator/device. До запуска M12→M13 не имеет
  hands-on validation на реальном Android SQLite.
- **Manual smoke**: NOT RUN. 8 checklist сценариев pending (Cardinality downgrade #6 —
  blocked / out-of-scope, остальные pending — все code-paths присутствуют).

## Final assessment

**ready-for-merge с soft tech debt**.

Все три quality gate (lint / test / build APK) PASS. Code-уровень: 7 из 8 root
business scenarios реализованы (1-5, 7, 8); root #6 (cardinality downgrade) явно
out-of-scope, deferred в Backlog phase 2 + FlowBacklog IS481cc-F6. Контракт LexemeApi
расширен консистентно (5 BREAKING + 6 NEW), все callsite'ы migrated, domain
typed-rewrite (`ComponentValueData → TemplateValues`) выполнен exhaustive через
compile-time gate. Миграция M12→M13 — единственный non-trivial остаток без runtime
validation (instrumented tests НЕ запущены); риск контролируется через `fallbackToDestructiveMigration`
+ Crashlytics (зафиксированы в prereq). Manual smoke 8 сценариев pending — стандартный
follow-up на debug build после merge либо до push.

Открытые soft-gaps (см. ниже) — не блокирующие; либо backlog phase 2, либо minor
semantic-fix, либо process findings про FF (не код).

## Открытые вопросы / soft tech debt

- **Instrumented migration tests НЕ запущены**. `MigrationFrom12to13.kt` (13 кейсов
  A-L) + `MigrationFrom12to13IdempotencyTest.kt` (M3 + M3b) написаны но не выполнены —
  требуют emulator/device runner. До запуска M12→M13 не имеет hands-on validation
  на реальном Android SQLite. Рекомендация: запустить локально перед push либо на CI.

- **Manual smoke 8 сценариев pending** (`checklist_run.md`). Cardinality downgrade
  (#6) — blocked / out-of-scope. Остальные 7 — стандартный smoke на debug build,
  минимум 2 словаря для покрытия aggregated view.

- **IS481 phase 2** — cardinality downgrade (`is_multi=true→false`) + edit-component
  flow. UseCase `downgradeCardinality(typeId)` / `DowngradeCheck` / `DowngradeBlocked`
  outcomes + edit-dialog в UI. Перенесено в `docs/Backlog.md → IS481 phase 2` и
  `docs/FlowBacklog.md → IS481cc-F6`.

- **`RenameOutcome.NameTooLong` не имплементирован** (только в `CreateOutcome`).
  Minor-gap; impl TODO в `ComponentsManagerUseCaseImpl`. Также `BuiltInProtected`
  conflation для soft-deleted (semantic fix потом) — упоминалось в business_summary.

- **Multi-dict picker из manager-экрана (F158 deferred)** — phase 2 (MVP создаёт только
  `Scope.Global` из Manager / `Scope.PerDictionaries([currentDict])` из PerDict).
  В контракте `CreateDialogState.scope` + `Msg.CreateScopeChange` присутствуют,
  но UI не рендерит scope-control.

- **Дублирование Dialog'ов между CM / PerDict screen-модулями** — архитектурно
  правильное решение вынести в `:modules:widget:component_widgets` (модуль создан в
  infra phase, source-files пустые). Требует рефакторинга mate-пакетов обоих
  модулей. Out-of-scope текущего flow; кандидат в Backlog → Архитектура.

- **Логи `###ComponentConstructor###` тег не внедрён**. Checklist требовал
  unique tag-стрима; фактически используются модуль-локальные tags
  (`ComponentsManager` / `PerDictComponents`) + нет логов в `Migration_012_to_013`
  (счётчики rewrite'нутых rows / дроп индексов / backfill timestamps). Решение:
  либо отдельный лог-pass с rename, либо обновить checklist под фактические теги.

- **Stale-reference sweep** на `ComponentValueData` / `ComponentValueDataJson` —
  Grep не выявил production-ссылок, release build verify рекомендован.

- **`13.json` schema export validity** — файл присутствует, но cross-проверка через
  `runMigrationsAndValidate(13, ...)` требует instrumented runner (см. выше).

## FlowBacklog (IS481cc-*)

Cross-reference на FlowBacklog F1-F8 (8 process findings flow IS481 component_constructor).
Все статусы — `open`, цель — улучшение FF на следующих фичах. Не блокируют merge кода.

- **IS481cc-F1**: Conductor не соблюдает mode=autonomy — останавливается между шагами
  (RECURRING, минимум 3 повторения в одной сессии).
- **IS481cc-F2**: Step `task` не учитывает заранее проработанные design-документы
  (`concept/` рядом с brief).
- **IS481cc-F3**: Sub-agent `scope_analysis` не делает систематический аудит —
  закрывает текущие findings без проверки оставшихся слепых зон (10 итераций потребовалось).
- **IS481cc-F4**: Conductor не обновляет plan.yml между итерациями repeat-шага —
  status остаётся `pending`, `iteration` не выставлен.
- **IS481cc-F5**: Edge case `execute_repeat`: approved-at-max-with-unverified-clean —
  псевдокод runner.md не покрывает.
- **IS481cc-F6**: Process gap между scope_analysis → business_contract — aspect из
  scope не отражён в contract (cardinality downgrade выпал).
- **IS481cc-F7**: Conductor разбил execute фазу `business_implement` на multiple
  sub-agent passes — нарушение `execute_step_once` псевдокода runner.md (RECURRING с F4).
- **IS481cc-F8**: Inquisitor sub-agent выдал bogus rejections — не проверил реальное
  содержание артефакта (5/5 findings были bogus-rejected, conductor override через verify).

---

_model: claude-opus-4-7[1m]_
