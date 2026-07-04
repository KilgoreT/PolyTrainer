# Code review: IS481 component_constructor phase 2

Commit: f0f82845 (pre-phase-2 baseline) | Date: 2026-06-22 | Reviewers: architect / bugs / yagni

## Architecture

### [critical] A1. DictionariesFlowHandler не подключён к Mate → multi-dict scope picker нерабочий
**Где:** `modules/screen/components_manager/.../ComponentsManagerViewModel.kt:42-48`
**Что не так:** Handler создан как `MateFlowHandler` + инжектится в `DatasourceEffectHandler`, но НЕ зарегистрирован в `effectHandlerSet` ViewModel. `Mate.subscribeToLongRunningFlows()` подписывает только handler'ы из effectHandlerSet. Также `SubscribeDictionaries` effect нигде не эмитится reducer'ом.
**Почему важно:** Один из 4 главных пунктов phase 2 (multi-dict scope picker) полностью сломан. `availableDictionaries` навсегда `emptyList()` → chip-list пустой → submit с `Scope.PerDictionaries` недоступен.
**Предложение:** Добавить `dictionariesFlowHandler` в `effectHandlerSet` ViewModel. Удалить `SubscribeDictionaries` effect + ctor inject из `DatasourceEffectHandler` (мёртвый код после подписки).
**Verify:** Read `ComponentsManagerViewModel.kt:42-47` → effectHandlerSet: datasourceHandler/flowHandler/uiHandler/navHandler без DictionariesFlowHandler. Grep `SubscribeDictionaries` в Reducer → 0 матчей emit.

### [critical] A2. Template-immutability gate отсутствует в UseCaseImpl несмотря на контракт F017
**Где:** `app/.../componentsmanager/ComponentsManagerUseCaseImpl.kt:144-172`
**Что не так:** Контракт + KDoc + `02_scope.md` явно фиксируют: «UseCaseImpl сравнивает template параметра с current → TemplateImmutable без обращения к data API (F017)». Реальный код только `trim+isBlank` check, сразу делегирует API. Defense-in-depth есть на data (`CoreDbApiImpl:582`), но **основной gate отсутствует**.
**Почему важно:** Контракт нарушен. Каждый changed-template submit делает лишний DB round-trip. Test `whenSubmitEditWithChangedTemplate_thenTemplateImmutable_andDataApiNotCalled` отсутствует (только API passthrough mapping test).
**Предложение:** Либо реализовать gate в UseCase (получить current type через новый API method `getById`), либо явно отказаться от F017 в KDoc/contract и оставить только data check. Сейчас худшее — контракт говорит одно, код другое.
**Verify:** Read `ComponentsManagerUseCaseImpl.kt:144-172` — нет current.template lookup. Grep TemplateImmutable в UseCaseImplTest — только API passthrough test.

### [critical] A3. Inconsistency порядка защит: editComponentType vs rename/softDelete
**Где:** `core/core-db-impl/.../CoreDbApiImpl.kt:573-574` (edit: BuiltInProtected первым) vs `:532-533` (rename: Removed первым) vs `:690-691` (softDelete: Removed первым)
**Что не так:** edit использует обратный порядок защит относительно rename/softDelete. `data_summary.md` ложно утверждает parity между ними.
**Почему важно:** Sibling CRUD методы non-uniform. Для built-in+soft-deleted типа edit вернёт BuiltInProtected, rename/softDelete — Removed. UI показывает разные snackbar для одной реальности.
**Предложение:** Привести edit к Removed → BuiltInProtected (parity). Исправить ложь в `data_summary.md`.
**Verify:** Read `CoreDbApiImpl.kt` строки 530-533 / 571-574 / 688-691.

### [minor] A4. EditDialog dirty-loop при CardinalityDowngradeBlocked
**Где:** `modules/widget/component_widgets/.../dialogs/EditComponentDialog.kt:65-71`
**Что не так:** `canSubmit = dirty + name valid + !isSubmitting` — не учитывает `impactedLexemesPreview != null`. Submit на blocked downgrade → server reject → preview показан → user снова жмёт → submit опять с тем же isMulti=false → loop.
**Предложение:** Передавать `previewActive: Boolean` либо считать `canSubmit` как computed extension val в State (как `CreateDialogState.canSubmit`), widget принимает готовый Boolean.

### [minor] A5. Дублирование EditDialogState/EditNameError/ImpactedLexemesPreview между двумя screen-mate
**Где:** Manager `mate/State.kt:101-145` vs PerDict `mate/State.kt:108-137`
**Предложение:** Backlog — вынести в shared module (или принять дублирование как осознанный choice, зафиксировать в spec).

### [minor] A6. CreateComponentDialog дублирует canSubmit логику из CreateDialogState extension val
**Где:** `dialogs/CreateComponentDialog.kt:68-74` vs `mate/State.kt:91-95`
**Предложение:** Расширить `CreateDialogState.canSubmit` чтобы включало nameError, widget принимает готовый Boolean.

### [minor] A7. UseCaseImpl trim/isBlank inconsistency: rename без trim, edit/create с trim
**Где:** `ComponentsManagerUseCaseImpl.kt:88-107` (rename) vs `:149-152` (edit) / `:65-86` (create)
**Предложение:** Унифицировать `val trimmed = name.trim(); if (trimmed.isBlank()) ...` для rename.

## Bugs

### [critical] B1. DictionariesFlowHandler не подписан (дубликат A1, подтверждено независимо)
См. A1.

### [critical] B2. flowAllUserDefinedTypesWithUsage и flowUserDefinedTypesForDictionary НЕ реактивны на component_values changes
**Где:** `core/core-db-impl/.../CoreDbApiImpl.kt:432-465`
**Что не так:** В `combine`/`map` вызываются `componentValueDao.aggregatedValueCountPerType()` / `typeDictPairs()` / `aggregatedValueCountPerTypeForDict()` — все `suspend`, не Flow. Trigger пересчёта только при изменении component_types/dictionaries. Изменения в component_values НЕ триггерят re-emit.
**Почему важно:** `valueCountByType`, `dictionaryIdsByType`, `dictionaryNames` остаются stale. Пользователь добавил 5 переводов с custom типом — на Manager-экране всё ещё `0 · —`. Реактивность сломана.
**Предложение:** Использовать `flow { ... }` с invalidation triggers, либо добавить Flow-варианты в DAO (`flowAggregatedValueCountPerType(): Flow<...>` через `@Query` Room автоматически инвалидирует по таблице).
**Verify:** Read `ComponentValueDao.kt:152, 167, 182` — все 3 метода suspend, не Flow. Read `CoreDbApiImpl.kt:432-465` — вызываются внутри `.map` блока без re-emit trigger.

### [minor] B3. Race в editComponentType: cardinality check ВНЕ withTransaction
**Где:** `core/core-db-impl/.../CoreDbApiImpl.kt:588-622`
**Что не так:** `findLexemesWithMultipleValuesForType` (line 589) ВНЕ `withTransaction` (line 615). Concurrent INSERT в component_values может произойти между check и UPDATE → invariant single-cardinality нарушен.
**Предложение:** Перенести cardinality SELECT внутрь `withTransaction { ... }`.

### [minor] B4. Race в softDeleteComponentType: previewDeletionImpact ВНЕ withTransaction → неполный affectedPrefs
**Где:** `core/core-db-impl/.../CoreDbApiImpl.kt:685-720`
**Предложение:** Перенести `previewDeletionImpact` (или хотя бы расчёт affectedConfigs) внутрь withTransaction.

### [minor] B5. previewDeletionImpact.affectedPrefs покрывает только affectedConfigs, не все dictionariesWithValues
**Где:** `CoreDbApiImpl.kt:677-683`
**Предложение:** Расширить `affectedPrefs = (affectedConfigs.map { it.dictionaryId } + dictionariesWithValues).distinct()`.

### [minor] B6. getById returns null → fallback BuiltInProtected (confusing для hard-deleted)
**Где:** `CoreDbApiImpl.kt:530-531, 571-572, 688-689`
**Предложение:** Изменить fallback на NotFound или Removed.

### [minor] B7. WordCardUseCaseImpl `catch (e: Exception)` глотает CancellationException (10 sites)
**Где:** `app/.../WordCardUseCaseImpl.kt:51, 77, 125, 175, 187, 208, 230, 245, 252, 298`
**Что не так:** Pre-existing pattern, но phase 2 модифицировал эти методы (TemplateValues migration) — был шанс исправить.
**Предложение:** Добавить `} catch (e: CancellationException) { throw e } catch (e: Exception) { ... }` в каждый catch site.

### [minor] B8. Tests не покрывают bug A1/B1: 75 tests pass дают ложную уверенность
**Где:** `modules/screen/components_manager/src/test/...`
**Предложение:** Интеграционный test ViewModel: мокировать `flowDictionaries()` → assert `state.availableDictionaries.isNotEmpty()` после короткой задержки.

## YAGNI

### [critical] Y1. EditNameError mate-локальный дублирует domain NameError
**Где:** Manager `State.kt:122-126` + PerDict `State.kt:122-125` + `toLabelRes()` в screens + `NameError.kt` (domain)
**Что не так:** 3 идентичных варианта (NameEmpty/SameScopeCollision/CrossScopeCollision), 5 точек дублирования.
**Предложение:** Reducer хранит domain `NameError?`, EditComponentDialog принимает `NameError?` (как Create). Удалить EditNameError x2 и toLabelRes x2.

### [critical] Y2. addLexemeWithTranslation / updateLexemeTranslation — deprecated shims без callers
**Где:** `CoreDbApi.kt:243-254` + `CoreDbApiImpl.kt:724-776` (~52 строки)
**Что не так:** `@Deprecated` методы, 0 callers в production/test.
**Предложение:** Удалить оба + проверить TranslationApiEntity (возможно тоже мёртв).

### [critical] Y3. WordCardUseCase generic addComponentValue/updateComponentValue/deleteComponentValue без callers
**Где:** `WordCardUseCase.kt:52-65` + `WordCardUseCaseImpl.kt:180-233` (impl сам признаёт «generic path не имеет caller'ов в IS481»)
**Предложение:** Удалить три метода до момента реального caller'а. TODO в FlowBacklog.

### [critical] Y4. Field / PrimitiveType / ComponentTemplate.fields — speculative future API без consumers
**Где:** `modules/domain/lexeme/Field.kt`, `PrimitiveType.kt`, `ComponentTemplate.kt:17-21`
**Что не так:** 0 imports в production/test.
**Предложение:** Удалить. Когда composite templates появятся — добавить заново.

### [critical] Y5. Primitive.Color + "color" JSON branch reserved-but-unused
**Где:** `modules/domain/lexeme/Primitive.kt:17`
**Предложение:** Удалить Color variant. sealed → расширение non-breaking.

### [critical] Y6. ImpactedLexemesPreview sealed-обёртка моментально разворачивается в плоские примитивы
**Где:** State.kt:136-145 + ComponentsManagerScreen.kt:206-226 (identical в PerDict)
**Что не так:** Reducer пакует в InlineOnly/InlineWithDrillIn, Screen сразу разворачивает обратно в `inlineIds + totalCount + showAllVisible`.
**Предложение:** Хранить `impactedLexemeIds: List<Long>?` в EditDialogState. Логика `take(3) / size > 3` в widget.

### [minor] Y7. PerDictionaryComponentsUseCaseImpl 5/6 методов — pass-through delegate
**Предложение:** Reducer PerDict берёт ComponentsManagerUseCase напрямую, либо в PerDict interface оставить только `flowComponentsForDictionary`.

### [minor] Y8. Defensive `check(typeDb.removedAt == null)` после SQL фильтра `removed_at IS NULL`
**Где:** `CoreDbApiImpl.kt:263-265, 297-299`
**Предложение:** Удалить — impossible cases. (Сохранить только в `:342-348` / `:378-380` где `getById` без removed_at фильтра.)

### [minor] Y9. TemplateImmutable doc lies о UseCase check (дубликат A2)
См. A2.

### [minor] Y10. insertDefaultQuizConfig DAO-метод dead-code; hardcoded JSON inline в WordDao
**Где:** `QuizConfigDao.kt:56-63` + `WordDao.kt:41-52`
**Предложение:** Либо удалить `insertDefaultQuizConfig`, либо использовать его в WordDao вместо inline JSON.

### [minor] Y11. WordCardUseCaseImpl.resolveDictionaryIdForLexeme(lexemeId) — dead param
**Где:** `WordCardUseCaseImpl.kt:332-334`
**Предложение:** Удалить параметр, TODO в backlog.

### [minor] Y12. onShowAllImpacted dead callback chain — noop в обоих screens
**Где:** 3 слоя (EditComponentDialog / CardinalityDowngradePreviewWidget / Screens) → noop comment `/* TBD */`
**Предложение:** Скрыть кнопку (showAllVisible=false) пока drill-in не реализован, либо показать snackbar «Coming soon».

### [minor] Y13. Lexeme.builtIn(key) extension — нет production callers (только тесты)
**Предложение:** Использовать в `LexemeMapper.kt:76,81` (или удалить).

### [minor] Y14. ComponentValueApiEntity.createdAt/updatedAt/removedAt — write-only
**Где:** `ComponentValueApiEntity.kt:19-21` (writes в `ComponentValueWithType.kt:50-52`, не читаются на read mapping)
**Предложение:** Удалить три поля + write code.

### [minor] Y15. Magic-string "Definition" в production-коде (4 occurrences)
**Где:** `WordCardUseCaseImpl.kt:241, 281`, `DatasourceEffectHandler.kt:141`, `LexemeMapper.kt:81`
**Предложение:** `const val DEFINITION_USER_DEFINED_NAME = "Definition"` либо WellKnownUserDefined enum.

### [minor] Y16. internal fun migrateImpl(failAfterStep) — test hook в production migration
**Где:** `Migration_012_to_013.kt:50-97`
**Предложение:** Low-priority — оставить или обернуть в test-only decorator над SQLiteConnection.

## Triage

| ID | Severity | Решение | Comment |
|---|---|---|---|
| A1/B1 | critical | → закрыть в фиче | **Блокирующий баг multi-dict picker.** Фиксить немедленно: добавить handler в effectHandlerSet. |
| A2/Y9 | critical | → закрыть в фиче | Template-immutability gate — добавить current.template lookup в UseCase (нужен `getById` API extension). |
| A3 | critical | → закрыть в фиче | Swap order в editComponentType (Removed → BuiltInProtected), исправить data_summary.md. |
| B2 | critical | → backlog | flowAllUserDefinedTypesWithUsage не реактивен на component_values — крупная архитектурная задача (Flow-варианты в DAO), отдельный тикет. |
| Y1 | critical | → закрыть в фиче | Удалить EditNameError (×2) + toLabelRes (×2). Reducer/widget использует domain NameError. |
| Y2 | critical | → backlog | Удалить deprecated `addLexemeWithTranslation/updateLexemeTranslation`. Отдельный clean-up commit. |
| Y3 | critical | → backlog | Удалить generic `addComponentValue/updateComponentValue/deleteComponentValue` в WordCardUseCase. |
| Y4 | critical | → backlog | Удалить speculative Field/PrimitiveType/ComponentTemplate.fields. |
| Y5 | critical | → backlog | Удалить Primitive.Color reserved-but-unused. |
| Y6 | critical | → закрыть в фиче | Удалить ImpactedLexemesPreview sealed — заменить на плоский List<Long>. |
| A4 | minor | → закрыть в фиче | EditDialog dirty-loop — добавить previewActive в canSubmit. |
| A5 | minor | → backlog | Дублирование Edit*State между двумя screen-mate. |
| A6 | minor | → закрыть в фиче | Дублирование canSubmit логики (вместе с A4). |
| A7 | minor | → закрыть в фиче | rename trim consistency с edit/create. |
| B3 | minor | → закрыть в фиче | Cardinality check внутрь withTransaction. |
| B4 | minor | → закрыть в фиче | softDelete preview внутрь withTransaction. |
| B5 | minor | → backlog | affectedPrefs полнота — edge case minor UX. |
| B6 | minor | → backlog | Add NotFound outcome — diagnostic improvement. |
| B7 | minor | → backlog | WordCardUseCaseImpl CancellationException re-throw (pre-existing). |
| B8 | minor | → backlog | Integration test для ViewModel handler subscription. |
| Y7 | minor | → backlog | PerDictionaryComponentsUseCaseImpl pass-through cleanup. |
| Y8 | minor | → закрыть в фиче | Defensive check после SQL — удалить из 263-265 / 297-299. |
| Y10 | minor | → backlog | insertDefaultQuizConfig dead vs hardcoded JSON dup. |
| Y11 | minor | → закрыть в фиче | resolveDictionaryIdForLexeme dead param. |
| Y12 | minor | → закрыть в фиче | onShowAllImpacted скрыть кнопку до реализации drill-in. |
| Y13 | minor | → backlog | Lexeme.builtIn(key) usage. |
| Y14 | minor | → закрыть в фиче | ComponentValueApiEntity write-only timestamps — удалить. |
| Y15 | minor | → backlog | Magic-string "Definition" константа. |
| Y16 | minor | → rejected | Test hook в migration — приемлемый trade-off, документировать. |

## Summary

- **28 findings total** (10 critical + 18 minor)
- **3 reviewers** independently caught DictionariesFlowHandler bug (architect+bugs) — strongest signal
- **Phase 2 принципиально нерабоч в Manager** (A1/B1) без fix → блокер релиза
- **→ закрыть в фиче (13):** A1/B1, A2/Y9, A3, Y1, Y6, A4, A6, A7, B3, B4, Y8, Y11, Y12, Y14
- **→ backlog (14):** B2, Y2, Y3, Y4, Y5, A5, B5, B6, B7, B8, Y7, Y10, Y13, Y15
- **→ rejected (1):** Y16

_model: claude-opus-4-7[1m] (3 reviewers)_
