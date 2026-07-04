# IS481 WordCard Components — Design Tree

Финальное дерево изменений. Подход — полная унификация: все компоненты (translation, definition, прочие) живут одним списком `LexemeState.components`. Mirror Translation/Definition в state/Msg/Effect/Reducer удаляется. Domain shim'ы `Lexeme.translation/definition` остаются (нужны quiz и др. читателям).

Порядок снизу-вверх — естественная последовательность имплементации.

---

## Tier 0 — Domain (`:modules:domain:lexeme`, package `me.apomazkin.lexeme`)

```
modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/
├── TemplateValues.kt                                         [MODIFY]
│   + fun TemplateValues.asText(): String?
│   + fun textValuesOf(text: String): TemplateValues
│
├── ComponentType.kt                                          [NO-OP]
├── ComponentTypeRef.kt                                       [NO-OP]   (toRef уже есть)
├── BuiltInComponent.kt                                       [NO-OP]
├── ComponentValue.kt                                         [NO-OP]
├── Lexeme.kt                                                 [NO-OP]   (shim'ы остаются)
├── LexemeBuiltInExt.kt                                       [NO-OP]
├── Primitive.kt                                              [NO-OP]
├── ComponentTemplate.kt                                      [NO-OP]
└── Field.kt / PrimitiveType.kt                               [NO-OP]
```

**Acceptance:** `./scripts/cc-build.sh :modules:domain:lexeme:assembleDebug` зелёный.

---

## Tier 1 — Data (`:core:core-db-api` + `:core:core-db-impl`)

```
core/core-db-api/src/main/java/me/apomazkin/core_db_api/
└── CoreDbApi.kt                                              [MODIFY]
    interface LexemeApi:
    + fun flowTypesForDictionary(dictionaryId: Long): Flow<List<ComponentTypeApiEntity>>
    (A1: getLexemeIdByComponentValueId УДАЛЁН — reverse-lookup не нужен, lexemeId уже на руках)

core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/
├── CoreDbApiImpl.kt                                          [MODIFY]
│   LexemeApiImpl:
│   + override fun flowTypesForDictionary(...) — delegate componentTypeDao.flowTypesForDictionary + mapNotNull toApiEntity
│
└── room/dao/ComponentValueDao.kt                             [NO-OP]   (A1: selectLexemeIdById НЕ добавляется)

(A1: ComponentValueDaoTest [NEW] УДАЛЁН — новых DAO-методов нет, instrumented-стадия не нужна)
```

**Acceptance:**
- `./scripts/cc-build.sh :core:core-db-api:assembleDebug` зелёный.
- `./scripts/cc-build.sh :core:core-db-impl:testDebugUnitTest` зелёный (unit).

---

## Tier 2 — Business (`:modules:screen:wordcard` + `app/.../di/module/wordCard/`)

### Tier 2a — UseCase

```
modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/deps/
└── WordCardUseCase.kt                                        [REWRITE]
    Финальный интерфейс (см. 03 §1):
    + getTermById / deleteWord / updateWord (UNCHANGED)
    + deleteLexeme(wordId, lexemeId): RemoveLexemeResult?
    + addLexemeWithComponent(wordId, dictionaryId, ref, data): Lexeme?
    + restoreLexemeWithComponents(wordId, dictionaryId, snapshot: Lexeme): Lexeme?   // C3: снимок целиком (маппинг внутри)
    + addComponentValue(lexemeId, typeId, data): AddComponentValueResult?
    + updateComponentValue(componentValueId, lexemeId, data): Lexeme?
    + deleteComponentValue(componentValueId, lexemeId): RemoveComponentResult?
    + flowAvailableComponentTypes(dictionaryId): Flow<List<ComponentType>>
    + data class AddComponentValueResult(lexeme, newComponentValueId)
    + sealed RemoveLexemeResult { Removed(removedLexeme) }
    + sealed RemoveComponentResult { ComponentRemoved(lexeme); LexemeCascadeRemoved(removedLexeme) }

    Удалено:
    - addLexemeTranslation / deleteLexemeTranslation / RemoveTranslationResult
    - addLexemeWithBuiltInComponent / addLexemeWithUserDefinedComponent
    - deleteDefinitionComponent
    - getComponentTypes (suspend) — заменяется flow
    - restoreLexeme(wordId, translation, definition) — заменяется restoreLexemeWithComponents

app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/
└── WordCardUseCaseImpl.kt                                    [REWRITE]
    Реализация generic methods (см. 03 §1.2).
    Ключевые изменения:
    - addLexemeWithComponent: lexemeApi.addLexemeWithComponents(...) + re-read
    - addComponentValue: ловит lexemeApi.addComponentValue → Long → re-read + AddComponentValueResult
    - updateComponentValue(cvId, lexemeId, data): lexemeApi.updateComponentValue(cvId, data) + re-read через getLexemeById(lexemeId) (A1: lexemeId — параметр, reverse-lookup удалён)
    - deleteComponentValue: snapshot before + cascade decision на основе remaining count
    - flowAvailableComponentTypes: lexemeApi.flowTypesForDictionary.map { toDomain }
    - prefsProvider / resolveCurrentDictionaryId — УДАЛЕНЫ (dictionaryId передаётся параметром)
    Удалено: всё translation/definition-specific + magic-string "Definition"

app/src/test/java/me/apomazkin/polytrainer/di/module/wordCard/
└── WordCardUseCaseImplTest.kt                                [REWRITE]
    NEW tests:
    - addLexemeWithComponent happy / null
    - addComponentValue → AddComponentValueResult с правильным newId
    - updateComponentValue happy (re-read via getLexemeById(lexemeId), lexemeId — параметр) / updated==0 → null
    - deleteComponentValue → ComponentRemoved / LexemeCascadeRemoved (cascade path)
    - flowAvailableComponentTypes emits domain ComponentType
    Удалены: tests на удалённые методы UseCase
```

### Tier 2b — State / Msg / Effect

```
modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/
├── ComponentValueKey.kt                                      [NEW]
│   sealed interface { Pristine(pristineKey: Long); Saved(componentValueId: ComponentValueId) }
│
├── CommitOutcome.kt                                          [NEW]
│   sealed interface { NoOp; LocalRemove; PessimisticRemove; Update(text) }
│   + internal fun ComponentValueState.commitDecision(): CommitOutcome
│
├── State.kt                                                  [REWRITE]
│   WordCardState:
│   + availableComponentTypes: List<ComponentType> = emptyList()
│   + nextPristineKey: Long = 1L
│   + isExiting: Boolean = false       // flush-on-back: лоадер + отложенная навигация (§6.2.3)
│   + val hasInFlightCommits: Boolean (computed: ∃ component.isCommitting) — драйвер выхода
│   - hasDefinitionComponent (DELETE)
│   WordState.Loaded:
│   + dictionaryId: Long  (NEW required field)
│   LexemeState:
│   data class LexemeState(id, components: List<ComponentValueState>)
│     + val addedNonMultiTypeIds: Set<ComponentTypeId> (computed — гайд state-and-extensions)
│   - translation, definition, canAddTranslation, canAddDefinition (DELETE)
│   ComponentValueState:                                      [NEW shape]
│   data class ComponentValueState(
│     key: ComponentValueKey,           // F4-guide: sealed XOR, НЕ два nullable
│     componentTypeId: ComponentTypeId,
│     componentTypeRef: ComponentTypeRef,
│     isMulti: Boolean,
│     isEdit: Boolean = false,
│     isCommitting: Boolean = false,    // A10: in-flight маркер (закрытие edit по success-refresh)
│     origin: String = "",
│     edited: String = "",
│   )
│   + val isPristine / componentValueId / pristineKey (computed-аксессоры из key)
│   + val ComponentValueState.isPristine: Boolean
│
│   Mappers:
│   * Lexeme.toLexemeState() — REWRITE: читает lexeme.components
│   + ComponentValue.toComponentValueState() — NEW
│
│   Helpers:
│   + LexemeState.findByKey(key) / updateComponent(key, fn) / removeComponent(key) / appendPristine(c)
│   * closeAllEditModes() — REWRITE (drop pristine с empty edited; remove пустую NOT_IN_DB лексему)
│   * commitAndCloseAllEdits() — REWRITE (per-component decision; NOT_IN_DB single-INSERT инвариант; B3: финальный filterNot пустых NOT_IN_DB лексем — иначе вечная блокировка CreateLexeme)
│   Удалено: createLexemeTranslation/updateLexemeTranslationText/enableLexemeTranslationEdit
│            + зеркальные definition helpers; TextValueState (заменён ComponentValueState)
│
├── Message.kt                                                [REWRITE]
│   NEW Msg (см. 03 §2):
│   + CreateComponentValue(lexemeId, componentTypeId)
│   + UpdateComponentValueInput(lexemeId, key, value)
│   + EnterComponentValueEditMode(lexemeId, key)
│   + CommitComponentValueEdit(lexemeId, key)
│   + RemoveComponentValueRequested(lexemeId, key)
│   + RefreshLexemeComponents(lexemeId, components: List<ComponentValue>)
│   + ComponentValueInserted(lexemeId, pristineKey, newComponentValueId)
│   + LexemeDraftPromoted(newLexeme: Lexeme, anchorPristineKey: Long)   // H-1: двухаргументный (F3/F4)
│   + OperationFailed(messageRes), RestoreLexemeFailed(snapshot), WordNotFound   // H-6
│   - ShowError (УДАЛЁН → заменён OperationFailed)
│   + LexemeCascadeRemoved(removedLexeme: Lexeme)
│   + LexemeRemoved(removedLexeme: Lexeme)   ← reshape (one snapshot arg)
│   + ComponentTypesLoaded(types) / ComponentTypesLoadFailed(error) / RetryLoadComponentTypes
│   + UndoRestoreLexeme(lexeme: Lexeme)
│   * WordLoaded(word) — без componentTypes (FlowHandler сам подтягивает)
│   Удалено:
│   - CreateTranslation/UpdateTranslationInput/EnterTranslationEditMode/CommitTranslationEdit/
│     RemoveTranslation/RefreshTranslation/TranslationDeleted/UndoRemoveTranslation
│   - зеркальный набор Definition (8 Msg)
│   - LexemeCascadeRemovedWithUndo / UndoRemoveLexeme / RefreshLexemeList
│
├── DatasourceEffectHandler.kt                                [REWRITE]
│   sealed interface DatasourceEffect (внутри файла):
│   + LoadWord(wordId)
│   + UpdateWord / RemoveWord (UNCHANGED)
│   + RemoveLexeme(wordId, lexemeId)
│   + sealed UpsertComponentValue (A3 — 3 варианта, impossible states impossible):
│       CreateLexeme(wordId, dictionaryId, pristineKey, componentTypeId, componentTypeRef, data)
│       AddValue(wordId, dictionaryId, lexemeId, pristineKey, componentTypeId, componentTypeRef, data)
│       UpdateValue(wordId, dictionaryId, lexemeId, componentValueId, componentTypeId, componentTypeRef, data)
│   + RemoveComponentValue(componentValueId, lexemeId)
│   + LoadAvailableComponentTypes(dictionaryId)
│   + RestoreLexemeWithComponents(wordId, dictionaryId, snapshot: Lexeme)   // H-2: snapshot, не components (для A17 retry round-trip)
│   Удалено: UpdateLexemeTranslation/RemoveTranslation/UpdateLexemeDefinition/RemoveDefinition/
│            RestoreLexeme(t, d)
│
│   onEffect ветки (см. 03 §5):
│   - LoadWord → WordLoaded(term) / WordNotFound
│   - UpsertComponentValue: when по sealed (A3)
│      CreateLexeme → addLexemeWithComponent → LexemeDraftPromoted(newLex, effect.pristineKey)
│      AddValue     → addComponentValue → RefreshLexemeComponents + ComponentValueInserted(pristineKey, newId)
│      UpdateValue  → updateComponentValue → RefreshLexemeComponents
│   - RemoveComponentValue → ComponentRemoved → RefreshLexemeComponents
│                          → LexemeCascadeRemoved(snapshot) → LexemeCascadeRemoved Msg
│   - LoadAvailableComponentTypes → no-op (FlowHandler ловит через runEffect)
│   - RestoreLexemeWithComponents → useCase + getTermById → WordLoaded (H-12: LoadAvailableComponentTypes эмитит reducer-ветка WordLoaded, НЕ handler — иначе двойная подписка)
│
├── UiEffect.kt                                                [MODIFY]
│   + ShowSnackbarWithRetry(messageRes, actionLabelRes, retryMsg: Msg)
│   (ShowSnackbarWithUndo, ShowErrorSnackbar — без изменений)
│
├── UiEffectHandler.kt                                         [MODIFY]
│   + branch на ShowSnackbarWithRetry (то же uiHost.showSnackbarWithAction → если pressed → consumer(retryMsg))
│
├── WordCardReducer.kt                                         [REWRITE]
│   Все translation/definition mirror handlers — DELETE.
│   NEW handlers (см. 03 §6):
│   + CreateComponentValue / UpdateComponentValueInput / EnterComponentValueEditMode
│   + CommitComponentValueEdit / RemoveComponentValueRequested
│   + RefreshLexemeComponents (B4 union-by-id merge: saved из payload; A10 id-targeted close — закрыть edit ТОЛЬКО isCommitting-компонента, чужой активный edit сохранить; pristine НИКОГДА не трогать; saved без payload — удалить)
│   + ComponentValueInserted (identity-flip pristine→saved по pristineKey, idempotent)
│   + LexemeDraftPromoted (NOT_IN_DB → real; B2: смержить выжившие pristine + реэмит upsert'ов)
│   + LexemeCascadeRemoved / LexemeRemoved → snackbar undo + remove
│   + UndoRestoreLexeme → RestoreLexemeWithComponents
│   + ComponentTypesLoaded / ComponentTypesLoadFailed / RetryLoadComponentTypes
│   MODIFY handlers:
│   * WordLoaded → set dictionaryId на Loaded; emit LoadAvailableComponentTypes
│   * NavigateBack → flush-on-back: commitAndCloseAllEdits + isExiting=true; навигация — пост-шагом reduce (03 §6.2.3)
│   * OperationFailed → F7 (pending=false) + снять isCommitting (вкл. pristine-survivors) + isExiting=false
│   * isGuardedByPending() → reshape под новые Msg (+ guard на isExiting)
│   reduce() пост-шаг: isExiting && !hasInFlightCommits → +NavigationEffect.Back & isExiting=false (Back один раз)
│   Удалено: commitTranslationEdit / commitDefinitionEdit / refreshTranslation / refreshDefinition
│   Reducer constructor — без параметров (как сейчас).
│
└── AvailableComponentTypesFlowHandler.kt                      [NEW]
    @Inject constructor(useCase, logger)
    subscribe() — no-op (await effect)
    runEffect(LoadAvailableComponentTypes(dictId)) → unsubscribe + launch flowAvailableComponentTypes
    emit ComponentTypesLoaded / ComponentTypesLoadFailed
    LogTags.WORDCARD
```

### Tier 2c — Тесты mate

> Полный TDD-контракт тестов — **`07_test_design.md`** (пинящие таблицы, truth-tables, scenario-цепочки, anti-regression translation, §0 contract-gaps). Пишутся ПЕРВЫМИ (красные), код подгоняется, тесты не меняются.

```
modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/
├── TranslationManagementTest.kt                               [DELETE]
├── DefinitionManagementTest.kt                                [DELETE]
├── ext/SpecializedLexemeExtTest.kt                            [DELETE] (B7 — тестирует удаляемые createLexemeTranslation/Definition helpers)
├── ext/LexemeExtTest.kt                                       [REWRITE] (B7 — fixtures на ComponentValueState; updateLexeme/removeLexeme выживают)
│   (ext/TopBarExtTest.kt, ext/WordExtTest.kt — NO-OP, не зависят от компонентов)
├── UndoDeleteTest.kt                                          [REWRITE]
│   под LexemeCascadeRemoved / LexemeRemoved / UndoRestoreLexeme
├── LexemeManagementTest.kt                                    [MODIFY]
│   проверки NOT_IN_DB → real (LexemeDraftPromoted)
├── WordLoadedTest.kt                                          [MODIFY]
│   WordLoaded(term) — без componentTypes; reducer эмитит LoadAvailableComponentTypes
├── DatasourceEffectHandlerTest.kt                             [REWRITE]
│   под новые effects
├── ComponentValueLifecycleTest.kt                             [NEW]
│   8+ тестов CRUD pristine/saved (см. 03 §8.3)
├── ComponentValueRefreshTest.kt                               [NEW]
│   RefreshLexemeComponents (A10 id-targeted close) + ComponentValueInserted identity-flip
├── ComponentTypesFlowTest.kt                                  [NEW]
│   WordLoaded → LoadAvailableComponentTypes; Loaded / Failed / Retry
├── ComponentCascadeRemoveTest.kt                              [NEW]
│   LexemeCascadeRemoved + UndoRestoreLexeme
└── CommitAndCloseAllEditsTest.kt                              [NEW]
    mix NoOp/LocalRemove/PessimisticRemove/Update; NOT_IN_DB single-INSERT;
    closeAllEditModes drop empty pristine
```

**Acceptance Tier 2:**
- `./scripts/cc-build.sh :modules:screen:wordcard:assembleDebug` зелёный.
- `./scripts/cc-build.sh :modules:screen:wordcard:testDebugUnitTest` зелёный, новый suite passes.
- `./scripts/cc-build.sh :app:testDebugUnitTest` зелёный.
- grep по `"Translation"` / `"Definition"` в `wordcard/mate/` и `wordcard/deps/` — clean.

---

## Tier 3 — UI (`:modules:screen:wordcard/widget/lexeme` + `WordCardScreen`)

```
modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/
├── ComponentLabel.kt                                          [NEW]
│   @Composable internal fun componentValueLabel(componentTypeId, snapshotRef, availableTypes): String
│      (A12: резолв по componentTypeId из живого availableTypes → лейбл следует за rename по id;
│       fallback на snapshotRef если тип уже не в справочнике)
│   + helpers labelOfRef(ref) / componentLabelOf(type)
│
├── ComponentValueField.kt                                     [NEW]
│   ComponentValueField(label: String, state, enabled, onValueChange, onOpenEditMode,
│                       onCommitEdit, onRemove) — заменяет LexemeMeaningField
│   A12: принимает готовый label (резолв снаружи через componentValueLabel)
│
├── ComponentChipsRow.kt                                       [NEW]
│   ComponentChipsRow(availableTypes, addedNonMultiTypeIds, enabled, onAddComponent)
│   FlowRow + SubentityChip(label, iconRes, enabled, onClick)
│   Фильтр: template == TEXT (P3: image-типы вне скоупа — chip не показывается) + non-multi уже добавленные скрыты
│
├── LexemeComponentsBlock.kt                                   [NEW]
│   Orchestrator: ComponentValueField list + ComponentChipsRow
│
├── SubentityChip.kt                                           [MODIFY]
│   + overload SubentityChip(label: String, iconRes, enabled, onClick) (для user-defined names)
│   Existing @StringRes overload — сохранён
│
├── AddLexemeMeaningRow.kt                                     [DELETE]
├── LexemeMeaningField.kt                                      [DELETE]
├── LexemeCard.kt                                              [NO-OP]
└── DeleteLexemeButton.kt                                      [NO-OP]

modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/
└── WordCardScreen.kt                                          [MODIFY]
    Lines 154-203 (translation/definition block + AddLexemeMeaningRow) — REPLACE с
    LexemeComponentsBlock(lexemeState, availableTypes=state.availableComponentTypes,
                          enabled=!state.isPendingDbOp && !state.isExiting, sendMessage)
    + блокирующий лоадер поверх контента при state.isExiting (flush-on-back, 03 §6.2.3)
    Удалить imports AddLexemeMeaningRow, LexemeMeaningField, TextValueState. Add LexemeComponentsBlock, ComponentValueState.
    Update Preview: новый LexemeState shape + WordState.Loaded требует dictionaryId (без дефолта → иначе compile-fail).
```

**Acceptance Tier 3:**
- `./scripts/cc-build.sh :modules:screen:wordcard:assembleDebug` зелёный.
- `./scripts/cc-build.sh :app:lintDebug` зелёный.
- Manual visual smoke (9 пунктов из 04 §8).

---

## Tier 4 — Infra (`:modules:screen:wordcard/...`, strings, ViewModel)

```
modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/
└── WordCardViewModel.kt                                       [MODIFY]
    + ctor: availableTypesFlowHandler: AvailableComponentTypesFlowHandler (auto-@Inject через AssistedInject)
    + effectHandlerSet += availableTypesFlowHandler
    Factory сигнатура — БЕЗ изменений

modules/screen/wordcard/build.gradle.kts                       [NO-OP]
modules/screen/wordcard/.../LogTags.kt                         [NO-OP]   (WORDCARD используется)

app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/
└── WordCardModule.kt                                          [NO-OP]   (Dagger auto-resolve)

app/src/main/java/me/apomazkin/polytrainer/di/AppComponent.kt  [NO-OP]   (no DI graph change)

core/core-resources/src/main/res/values/strings.xml            [MODIFY]
+ word_card_error_load_component_types
+ word_card_action_retry
+ word_card_error_generic   (generic write-error для OperationFailed; без неё compile-fail)
  (NB: путь модуля — core/core-resources, не core-resources. Локализованный values-ru-rRU/strings.xml
   существует — при необходимости добавить переводы новых ключей и туда.)

modules/widget/component_widgets/...                           [NO-OP]   (BlueAssistChip не трогаем)
```

**Acceptance Tier 4:**
- `./scripts/cc-build.sh :app:assembleDebug` зелёный.
- `./scripts/cc-build.sh :app:lintDebug` зелёный.
- Manual: открытие WordCard — нет Dagger crash, FlowHandler стартует.

---

## Граф зависимостей

```
[Domain] (TemplateValues helpers)
   │
   ▼
[Data API] (flowTypesForDictionary)
   │
   ▼
[Data Impl] (flowTypesForDictionary wire)
   │
   ▼
[Business UseCase] (REWRITE generic + B4/C2 fix)
   │
   ▼
[Business State] (REWRITE LexemeState.components + mapper + helpers)
   │
   ▼
[Business Msg / Effect / Reducer / FlowHandler] (REWRITE generic, удалить mirror)
   │
   ▼
[Tests] (DELETE Translation/Definition; NEW generic suite)
   │
   ▼
[UI] (NEW ComponentValueField / ChipsRow / Block; DELETE old; WordCardScreen MODIFY)
   │
   ▼
[Infra] (ViewModel ctor +1, strings, no DI/build changes)
```

---

## Порядок имплементации (рекомендуемый)

1. **Domain TemplateValues helpers** (Tier 0) — ~10 минут.
2. **Data Impl DAO query + API method + impl wire + DAO test** (Tier 1) — ~45 минут.
3. **UseCase rewrite + UseCase tests** (Tier 2a) — ~90 минут.
4. **ComponentValueKey + CommitOutcome + State rewrite** (Tier 2b shape) — ~60 минут.
5. **Msg rewrite** (Tier 2b Msg) — ~20 минут.
6. **Effect rewrite + DatasourceEffectHandler rewrite** (Tier 2b handler) — ~75 минут.
7. **UiEffect добавка + UiEffectHandler branch** — ~15 минут.
8. **AvailableComponentTypesFlowHandler NEW** — ~30 минут.
9. **WordCardReducer rewrite** (Tier 2b reducer) — ~120 минут.
10. **Тесты mate REWRITE + NEW suite** (Tier 2c) — ~150 минут.
11. **UI rewrite** (Tier 3) — ~90 минут.
12. **WordCardViewModel ctor + strings + manual smoke** (Tier 4) — ~30 минут.

**Итоговая оценка: ~10-12 часов.**

Существенно больше brief'а (medium 3-5h) — потому что это рефакторинг unification, не аддитив.

---

## Out of scope

- Image template (`ImageValues`). Только TEXT.
- Reorder / drag-and-drop добавленных value.
- Validation длины/формата.
- Undo для одиночного `RemoveComponentValue` (только cascade-lexeme).
- AGG-1 migration definition user-defined → built-in DEFINITION enum (отдельная фича).
- Удаление `@Deprecated` shim'ов `Lexeme.translation/definition`.
- i18n built-in label «Перевод».
- Rename `SubentityChip` → `AddComponentChip` (cosmetic).
- Миграция `BlueAssistChip` в `:modules:core:ui` (нет общего callsite).
- Durability правки самого СЛОВА при выходе (word-edit = IS479; flush-on-back держит только компоненты, 03 §6.2.3). Компоненты при «назад» дожимаются — это в скоупе.

---

## Известные технические долги (backlog после фичи)

1. **Удаление `@Deprecated` Lexeme.translation/definition.** Только после миграции всех читателей (quiz и др.) на `lexeme.components`.
2. **AGG-1 definition → built-in.** Сейчас definition — user-defined `name="Definition"`. После built-in enum миграции — фильтрация label через `BuiltInComponent.DEFINITION`.
3. **Durability word-edit при выходе.** Flush-on-back (03 §6.2.3) дожимает записи КОМПОНЕНТОВ перед выходом; правка самого слова (`UpdateWord`) не удерживается (IS479, вне скоупа). Трекинг word-commit маркером — при необходимости.
4. **i18n built-in labels.** Hardcoded русский в core-resources. На локализационной фиче.
5. **`SubentityChip` rename.** Косметика, не блокирует.
6. **`BlueAssistChip` consolidation.** Если/когда появится 3-й callsite общего clickable chip — мигрировать.
7. **Image template поддержка в WordCard.** Skip image chips в `ComponentChipsRow` до появления image-input UI.

---

## Связано

- `IS481_component_constructor_phase2` — закоммичено `f0f8284` (Quiz picker + cc-build/cc-src + FF overlay).
- `IS482` — Lexeme domain unification — `6d3499c`.
- `IS481_wordcard_components/brief.md` — пользовательский бриф (не редактируется этой работой).
