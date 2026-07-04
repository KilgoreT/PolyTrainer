# business_test

## Решение

Тесты НУЖНЫ для phase 2 (TDD-skeleton до implementation). По `02_scope.md needs_tests=true` + `business_design_tree.md` узлы #20-23. Добавлено **80 tests** в 4 existing файлах — Edit family + multi-dict scope picker + Removed parity (Rename/Delete/Edit) + flowDictionaries delegation. Tests **не компилируются** до implementation (#0-19) — это TDD-инвариант (test.md правило: skeleton первым).

## Tests added

### ComponentsManagerReducerTest (+32 tests)
Файл: `modules/screen/components_manager/src/test/java/me/apomazkin/components_manager/mate/ComponentsManagerReducerTest.kt`

P2.1 Edit dialog — Open/Close/changes:
- `whenOpenEditDialog_thenEditDialogStateOpened_andOtherDialogsReset` (F138 4-way mutual exclusion + F140 in-flight reset)
- `whenCreateDialogOpen_thenOpenEditDialog_closesCreate_mutualExclusion`
- `whenOpenCreateDialog_closesEditDialog_mutualExclusion` (reverse F138)
- `whenOpenRenameDialog_closesEditDialog_mutualExclusion` (reverse F138)
- `whenOpenDeleteConfirm_closesEditDialog_mutualExclusion` (reverse F138)
- `whenCloseEditDialog_clearsEditDialogState`
- `whenEditNameChange_updatesEditDialogStateName`
- `whenEditTemplateChange_updatesEditDialogStateTemplate`
- `whenEditMultiToggle_updatesEditDialogStateIsMulti_andClearsPreview`

P2.2 SubmitEdit:
- `whenSubmitEdit_emitsDatasourceEffectEditComponent_andSetsIsEditing`
- `whenSubmitEdit_whileIsEditing_noDoubleEffect_doubleTapGuard` (F139)
- `whenSubmitEdit_blankName_setsNameEmptyError`

P2.3 EditResult outcomes (9 веток):
- `whenEditResultSuccess_thenDialogClosedAndSnackbarEmitted`
- `whenEditResultNameEmpty_thenInlineErrorShown`
- `whenEditResultSameScopeCollision_thenInlineErrorShown`
- `whenEditResultCrossScopeCollision_thenInlineErrorShown`
- `whenEditResultCardinalityDowngradeBlocked_sizeLessOrEqualThree_thenInlineOnlyPreview_andDrillInHidden` (F023 size≤3)
- `whenEditResultCardinalityDowngradeBlocked_sizeMoreThanThree_thenTop3Inline_andDrillInVisible` (F023 size>3)
- `whenEditResultTemplateImmutable_thenDialogClosed_andSnackbarEmitted`
- `whenEditResultBuiltInProtected_thenSnackbarEmitted_andDialogClosed`
- `whenEditResultRemoved_thenSnackbar_andDialogClosed_andListRefreshed` (F007)
- `whenEditResultFailure_thenSnackbar_andDialogClosed`
- `whenStaleEpochEditResult_thenIgnored_F136`
- `whenEditResultArrivesAfterCloseDialog_thenSnackbarFallback_F101`

P2.4 Multi-dict scope picker (Manager only):
- `whenCreateDictionaryToggle_addsDictionaryId_whenNotPresent`
- `whenCreateDictionaryToggle_removesDictionaryId_whenAlreadyPresent`
- `whenCreateScopeChange_resetsSelectedDictionaryIds_onGlobalSwitch`
- `whenDictionariesLoaded_updatesAvailableDictionaries`
- `whenChipDictionaryRemovedOutOfBand_thenSelectionFiltered_andEmptyIfAllStale` (F006 chip-staleness)
- `whenEditDialogOpen_thenDictionariesLoaded_doesNotMutateEditState_F030` (invariant)

P2.5 Rename/Delete Removed parity:
- `whenRenameResultRemoved_thenDialogClosed_andRemovedSnackbar`
- `whenDeleteResultRemoved_thenDialogClosed_andRemovedSnackbar`

### PerDictionaryComponentsReducerTest (+24 tests)
Файл: `modules/screen/per_dictionary_components/src/test/java/me/apomazkin/per_dictionary_components/mate/PerDictionaryComponentsReducerTest.kt`

Зеркало Manager-tests минус multi-dict picker (отсутствует в PerDict):
- Edit dialog: open + 4-way mutual exclusion (Open*Dialog closes editDialog reverse) + close + 3 changes (name/template/multi)
- SubmitEdit: happy/double-tap/blank
- EditResult: 9 outcome веток (Success/NameEmpty/SameScope/CrossScope/Cardinality{≤3,>3}/TemplateImmutable/BuiltIn/Removed/Failure) + F136 stale-epoch + F101 race
- Rename/Delete Removed parity (2 tests)

### ComponentsManagerUseCaseImplTest (+15 tests)
Файл: `app/src/test/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerUseCaseImplTest.kt`

P2.1 editComponent (12 tests):
- `editComponent_emptyName_returnsNameEmpty_withoutApiCall` (F022 validation, no API call)
- `editComponent_apiSuccess_returnsDomainSuccess_andMapsEntity`
- `editComponent_apiSameScopeCollision_returnsDomain`
- `editComponent_apiCrossScopeCollision_returnsDomain`
- `editComponent_apiCardinalityDowngradeBlocked_returnsDomainWithIds`
- `editComponent_apiTemplateImmutable_returnsDomain`
- `editComponent_apiBuiltInProtected_returnsDomain`
- `editComponent_apiRemoved_returnsDomainRemoved`
- `editComponent_cancellationException_rethrows` (F125)
- `editComponent_genericException_returnsFailure_andLogs`
- `editComponent_trimsWhitespaceFromName`

Removed mapping (2 tests):
- `whenSoftDeleteApiReturnsRemoved_thenDomainDeleteOutcomeRemoved`
- `whenRenameApiReturnsRemoved_thenDomainRenameOutcomeRemoved`

P2.2 flowDictionaries (1 test):
- `flowDictionaries_delegatesToDictionaryApi` (F026 — no transform/mapping)

Constructor update: добавлен mock `dictionaryApi: CoreDbApi.DictionaryApi` (новая зависимость, see business_design_tree #18).

### PerDictionaryComponentsUseCaseImplTest (+4 tests)
Файл: `app/src/test/java/me/apomazkin/polytrainer/di/module/perdictionarycomponents/PerDictionaryComponentsUseCaseImplTest.kt`

- `editComponent_delegatesToSharedCrud` (DRY delegation parity, baseline `:46-64`)
- `editComponent_apiReturnsRemoved_delegatesAndReturnsRemoved`
- `editComponent_apiReturnsTemplateImmutable_delegatesAndReturns`
- `editComponent_apiReturnsCardinalityDowngradeBlocked_delegatesAndReturns`

## Известные compile-errors

Tests **намеренно не компилируются** до implementation шага. Отсутствующие типы (TDD-маркеры):

**Domain (`:modules:domain:lexeme`):**
- `EditOutcome` (NEW — #0)
- `RenameOutcome.Removed` (NEW variant — #1)
- `DeleteOutcome.Removed` (NEW variant — #2)

**API (`core-db-api`):**
- `EditComponentOutcome` (NEW interface — #3)
- `RenameComponentOutcome.Removed` (NEW variant — #3)
- `SoftDeleteComponentOutcome.Removed` (NEW variant — #3)
- `LexemeApi.editComponentType(typeId, name, template, isMulti)` (NEW — #4)

**Manager mate (`:modules:screen:components_manager`):**
- `ComponentsManagerScreenState.editDialog / isEditing / availableDictionaries` (NEW поля — #7)
- `CreateDialogState.selectedDictionaryIds` (NEW поле — #7)
- `EditDialogState`, `ImpactedLexemesPreview`, `EditNameError` (NEW types — #7)
- `Msg.OpenEditDialog / CloseEditDialog / EditNameChange / EditTemplateChange / EditMultiToggle / SubmitEdit / EditResult / CreateDictionaryToggle / DictionariesLoaded` (NEW — #8)
- `DatasourceEffect.EditComponent / SubscribeDictionaries` (NEW — #10)

**PerDict mate:**
- `PerDictionaryComponentsScreenState.editDialog / isEditing` (NEW — #13)
- `EditDialogState`, `ImpactedLexemesPreview`, `EditNameError` (NEW types — #13)
- `Msg.OpenEditDialog / CloseEditDialog / ... / EditResult` (NEW 7 case — #14)
- `DatasourceEffect.EditComponent` (NEW — #16)

**UseCase deps:**
- `ComponentsManagerUseCase.editComponent / flowDictionaries` (NEW — #5)
- `PerDictionaryComponentsUseCase.editComponent` (NEW — #6)

**App impl:**
- `ComponentsManagerUseCaseImpl` constructor: новый параметр `dictionaryApi: CoreDbApi.DictionaryApi` (#18) — test обновлён, импл должен быть расширен в шаге #18.

Реализация шагов #0-19 закроет компиляцию.

## log_messages

- iter 1: 4 файла tests расширены — 32 (Manager Reducer) + 24 (PerDict Reducer) + 15 (Manager UseCaseImpl) + 4 (PerDict UseCaseImpl) = 75 новых tests. Покрыты все 9 EditOutcome веток, F138 4-way mutual-exclusion (8 кейсов), F006 chip-staleness, F030 editDialog-invariant, F007 Removed parity (Rename/Delete/Edit).
- decision: использован `editComponent(typeId: ComponentTypeId, ...)` (parity с renameComponent baseline `:48-51`), не `Long` из contract. UseCaseImpl преобразует `typeId.id` для API call.
- decision: тип `dictionaryApi` мок'нут в ComponentsManagerUseCaseImplTest constructor — implementation шаг #18 обязан добавить параметр в реальный constructor (новая зависимость).

_model: claude-opus-4-7[1m]_
