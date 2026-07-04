# IS481 WordCard Components — Карта слоёв

Фича: inline-редактирование component-значений лексемы на экране WordCard через **унифицированный список компонентов** (translation, definition, прочие user-defined — один и тот же путь). Без `<T>`-generics, без двух параллельных code path'ов.

Главная цель — заменить специализированные ветки `Translation*` / `Definition*` в state / reducer / handler / UI на **один обобщённый набор** `ComponentValue*`, работающий поверх существующего generic API в `CoreDbApi.LexemeApi` (`addComponentValue` / `updateComponentValue` / `deleteComponentValue` / `getComponentTypes`).

---

## Карта гайдов (обязательны при реализации)

Проверено в Review #5b: план соответствует. При кодинге **сверяться с гайдом в точке изменения** — ссылки расставлены в слоевых файлах. Сводка:

| Слой / артефакт | Гайд | Что регламентирует |
|---|---|---|
| State (`ComponentValueState`, `ComponentValueKey`, `LexemeState`) | `docs/guides/state-modeling.md`, `state-and-extensions.md` | sum-типы для XOR-инвариантов (impossible states); derived-логика — computed property, НЕ в composable; одна задача на extension |
| Reducer (ветки, guard, `commitDecision`) | `docs/guides/reducer-patterns.md` | чистота, `(state, effects)`, `isGuardedByPending`, R-RP-010 (фильтрация — не в reducer) |
| Msg | `docs/guides/messages.md` | именование (Create*/`*Loaded`/`*Inserted`), data class vs object, `Empty` no-op |
| Effect / handler / FlowHandler | `docs/guides/effect-handlers.md` | `runEffect`, lifecycle, маппинг effect→msg |
| UseCase API + DAO | `docs/guides/data-layer.md` | `flow*`/`suspend`/nullable, trim-нормализация в UseCase |
| DI (`AvailableComponentTypesFlowHandler`, ViewModel) | `docs/guides/dagger-di.md`, `viewmodel-wiring.md` | `@Inject` для handler, mixed `@Assisted`+`@Inject`, `effectHandlerSet` |
| Composable (`ComponentValueField` и др.) | `docs/guides/ui-patterns.md`, `ui-primitives.md` | stateless, слоты, `sendMessage` |
| Naming (все новые имена) | `docs/guides/naming.md` | суффиксы/префиксы, R-N-010/011/013 |
| Logger | `docs/guides/logging.md` | `LexemeLogger`, `LogTags.WORDCARD`, без хардкода тегов |
| Тесты | `docs/guides/testing-reducers.md`, `testing-extensions.md` | `testReduce`/`assertEffects`, один класс на группу Msg |

**Осознанные отступления от гайдов** (зафиксированы, не баги):
- Reducer без логирования (R-RP-007) — legacy parity с текущим `WordCardReducer`; backlog.
- `NoOperation` вместо гайдового `Empty` (R-RP-008) — legacy parity, переименование затронет существующие тесты вне scope.
- `commitAndCloseAllEdits` возвращает `(State, effects)` (decision-логика в extension) — legacy parity с текущим State.kt.

---

## Slice по слоям

### 0. Domain — `:modules:domain:lexeme` (package `me.apomazkin.lexeme`)

**Что есть (EXISTING)**
- `Lexeme(lexemeId, components: List<ComponentValue>, translation @Deprecated, definition @Deprecated, addDate, changeDate)` — IS482 уже сделал `components` source of truth.
- `ComponentValue(id, lexemeId, type: ComponentType, data: TemplateValues)`.
- `ComponentType(id, systemKey: BuiltInComponent?, dictionaryId, name, template, position, isMulti, createdAt, updatedAt, removedAt)`.
- `ComponentTypeRef` sealed (`BuiltIn(key)` / `UserDefined(name)`) + `fun ComponentType.toRef()` уже реализован с правильной семантикой (используем как есть).
- `BuiltInComponent` enum (только `TRANSLATION("translation")`).
- `TemplateValues` sealed (`TextValues(value: Primitive.Text)`, `ImageValues`). Никаких `Map<String, …>`.
- `LexemeBuiltInExt.kt` → `fun Lexeme.builtIn(key)` для лукапа built-in компонента.

**Что добавляется (NEW)**
- `fun TemplateValues.asText(): String?` (читает `TextValues.value.value`, иначе null) — общий helper для UI/mate. Domain без хардкода строк-меток.
- `fun textValuesOf(text: String): TemplateValues = TextValues(Primitive.Text(text))` — фабрика.

**Что НЕ делается**
- `ComponentType.displayLabel()` в domain **не вводится** (нарушает Clean: hardcoded локализация в domain). Маппинг `BuiltInComponent → @StringRes` живёт в UI слое (см. § UI).
- `ComponentType.toRef()` не дублируем — используем существующий.
- `Lexeme.translation/definition` @Deprecated shim'ы не трогаем — нужны quiz / другим читателям.

---

### 1. Data API — `:core:core-db-api` (package `me.apomazkin.core_db_api`)

**Что есть (EXISTING)**
- `CoreDbApi.LexemeApi` уже содержит generic component API:
  - `addLexemeWithBuiltInComponent(wordId, dictionaryId, systemKey, data)`
  - `addLexemeWithUserDefinedComponent(wordId, dictionaryId, name, data): Long?`
  - `addLexemeWithComponents(wordId, dictionaryId, components: List<Pair<ComponentTypeRef, TemplateValues>>): Long?` — atomic compound INSERT (MIN-9).
  - `addComponentValue(lexemeId, componentTypeId, data): Long` — возвращает rowid INSERT (детерминированная квитанция).
  - `updateComponentValue(componentValueId, data): Int`.
  - `deleteComponentValue(componentValueId): Int` — возвращает remaining count для cascade decision.
  - `getComponentTypes(dictionaryId): List<ComponentTypeApiEntity>` — однократный snapshot.
  - `flowUserDefinedTypesForDictionary(dictionaryId): Flow<DictionaryTypesSnapshot>` — реактивная подписка только на **user-defined** (built-in исключены).

**Что добавляется (NEW)**
- `fun CoreDbApi.LexemeApi.flowTypesForDictionary(dictionaryId: Long): Flow<List<ComponentTypeApiEntity>>` — реактивная подписка на **все** active типы для словаря (built-in + user-defined). DAO query `ComponentTypeDao.flowTypesForDictionary` уже есть — нужен только wire через API + Impl.

> **A1.** `getLexemeIdByComponentValueId` (reverse-lookup) НЕ добавляется. UseCase `updateComponentValue` получает `lexemeId` параметром (он уже есть в `Effect.UpsertComponentValue.lexemeId`) → re-read через `getLexemeById(lexemeId)`. Сигнатура приводится к виду `updateComponentValue(componentValueId, lexemeId, data)` — симметрия с `deleteComponentValue`. Старый TODO B4/C2 жил из-за отсутствия lexemeId-контекста, который новый дизайн привносит бесплатно.

**Что НЕ меняется**
- Маппер `LexemeApiEntity.toDomain()` в `app/.../mapper/LexemeMapper.kt` — без изменений.
- `restoreLexeme*` overload'ы в API не нужны; уже есть `addLexemeWithComponents`. UseCase должен перейти на компонент-based payload.

---

### 2. Data Impl — `:core:core-db-impl`

**Что есть (EXISTING)**
- `CoreDbApiImpl.LexemeApiImpl` уже реализует все generic методы корректно (включая `insertSingleSafe` с cardinality-guard и `deleteComponentValue` возврат remaining count).
- `ComponentValueDao` / `ComponentTypeDao` — все нужные queries уже есть.

**Что добавляется (NEW)**
- `LexemeApiImpl.flowTypesForDictionary(dictionaryId)` → делегирует на `componentTypeDao.flowTypesForDictionary(dictionaryId).map { list -> list.mapNotNull { it.toApiEntity() } }`.

> **A1.** `ComponentValueDao.selectLexemeIdById` и override `getLexemeIdByComponentValueId` НЕ добавляются (reverse-lookup удалён).

**Тесты (NO-OP)**
- Новых DAO-методов нет → `ComponentValueDaoTest` не создаётся; instrumented-стадия не нужна.

---

### 3. Business — `:modules:screen:wordcard` (package `me.apomazkin.wordcard`)

**Реальная архитектура (EXISTING)**
- Mate (TEA): `Mate { initState, initEffects, reducer, effectHandlerSet }` в `WordCardViewModel` (AssistedInject, factory передаёт `wordId / navigator / uiHost`).
- `WordCardReducer : MateReducer<WordCardState, Msg, Effect>` — **no-arg constructor**, чистая логика. Логировать в reducer — не вводим (отдельный backlog).
- `DatasourceEffectHandler` (@Inject) + `WordCardNavigationEffectHandler` (assisted-create через navigator) + `UiEffectHandler` (assisted-create через uiHost). Sealed `DatasourceEffect` живёт внутри `DatasourceEffectHandler.kt`.
- `WordCardUseCase` (`deps/`) реализован в `app/di/module/wordCard/WordCardUseCaseImpl.kt`.
- LogTags локальные: `me.apomazkin.wordcard.LogTags.WORDCARD = "###WORDCARD###"`. Существующий handler uses `me.apomazkin.mate.LogTags.MATE` в catch-all. Для новой работы держим `LogTags.WORDCARD` (parity с UseCase / других мест внутри модуля).

**Что есть в state (EXISTING)**
- `WordCardState { topBarState, isLoading, isPendingDbOp, wordState: WordState, lexemeList: List<LexemeState>, lexemeIdPendingDelete: Long?, hasDefinitionComponent: Boolean }`.
- `WordState.Loaded { id, added, value, isEditMode, edited, showWarningDialog }`. **dictionaryId здесь НЕТ** (приходит через `Term.dictionaryId` в `Msg.WordLoaded.word`).
- `LexemeState { id, translation: TextValueState?, definition: TextValueState? }` + `canAddTranslation` / `canAddDefinition` computed.
- `TextValueState { isEdit, origin, edited }`.
- Helpers: `closeAllEditModes()`, `commitAndCloseAllEdits(): Pair<State, Set<Effect>>`, `updateLexeme(id, fn)`, `removeLexeme(id)`, mapping `Lexeme.toLexemeState()` через @Deprecated shim'ы.
- `NOT_IN_DB = -1L` для черновика.

**Что меняется (REWRITE)**
- `LexemeState` → `{ id, components: List<ComponentValueState> }`.
- `ComponentValueState` — NEW. Identity — sealed `ComponentValueKey` (Pristine XOR Saved), НЕ два nullable-поля. См. § 03 §3.1/§4.1.
- `WordState.Loaded` → добавляется `dictionaryId: Long` (для лукапа `componentTypeId` в reducer).
- `WordCardState.hasDefinitionComponent` УДАЛЯЕТСЯ — заменяется `availableComponentTypes: List<ComponentType>` (chip rendering driven by this list).
- `WordCardState.nextPristineKey: Long = 1L` — counter для генерации уникальных pristine identity (Reducer-only, без логики "now()").
- `WordCardState.isExiting: Boolean = false` + computed `hasInFlightCommits` — NEW (flush-on-back: лоадер + отложенная навигация при «назад», 03 §6.2.3).
- Mapper `Lexeme.toLexemeState()` REWRITE — читает `lexeme.components`, НЕ shim'ы.
- `closeAllEditModes` REWRITE — теперь удаляет pristine с empty `edited` (закрывает finding 18: «висячий pristine»).
- `commitAndCloseAllEdits` REWRITE — итерируется по `components`, decision per component через `commitDecision()`.
- Все translation/definition-специфичные extensions удаляются.

**Msg (REWRITE сегмент)**
- Удаляются: `CreateTranslation / UpdateTranslationInput / EnterTranslationEditMode / CommitTranslationEdit / RemoveTranslation / RefreshTranslation / TranslationDeleted / UndoRemoveTranslation` + зеркальный набор Definition + `LexemeCascadeRemovedWithUndo` (заменяется обобщённым).
- Добавляются `ComponentValue*` Msg (см. § 03 §2).

**Effect (REWRITE сегмент)**
- Удаляются: `UpdateLexemeTranslation / RemoveTranslation / UpdateLexemeDefinition / RemoveDefinition / RestoreLexeme(t, d)`.
- Добавляются: `UpsertComponentValue` / `RemoveComponentValue` / `LoadAvailableComponentTypes` / `RestoreLexemeWithComponents` (см. § 03 §5).

**UseCase (REWRITE)**
- `WordCardUseCase` обобщается. Удаляется magic-string `"Definition"` и translation-specific wrappers. Финальный набор методов в § 03 §1.

**FlowHandler — NEW**
- `AvailableComponentTypesFlowHandler` (@Inject, не Assisted — обоснование: handler не знает `dictionaryId` до load'а, использует `runEffect(LoadAvailableComponentTypes(dictId))` для (re-)subscribe — паттерн `AllUserDefinedTypesFlowHandler`). Parity с `ComponentsForDictionaryFlowHandler` (assisted с dictionaryId) НЕ требуется т.к. в WordCard `dictionaryId` неизвестен на construction-time Mate.

---

### 4. UI — `:modules:screen:wordcard/widget/lexeme`

**Что есть (EXISTING)**
- `WordCardScreen` рендерит `LexemeCard { translation? + definition? + AddLexemeMeaningRow + DeleteLexemeButton }` (lines 154-203).
- `LexemeMeaningField(labelRes, state, onValueChange, onOpenEditMode, onCommitEdit, onRemove)` — chip-заголовок (`SubentityChip`) + `LexemeEditableText` (autosave on focus-lost).
- `SubentityChip(@StringRes labelRes, @DrawableRes iconRes, enabled, onClick)` — internal widget модуля wordcard; имеет clickable.
- `AddLexemeMeaningRow(canAddTranslation, canAddDefinition, enabled, onCreateTranslation, onCreateDefinition)` — `FlowRow { SubentityChip + SubentityChip }`.
- `BlueAssistChip(@StringRes textRes)` в `:modules:widget:component_widgets` — **read-only, без onClick**. Используется только в `PerDictRowWidget` + `UserDefinedRowWidget` (4 call site total, не 6).

**Что меняется (REWRITE / NEW)**
- WordCard переходит на унифицированные composable'ы:
  - NEW: `ComponentValueField(state, label, enabled, onValueChange, onOpenEditMode, onCommitEdit, onRemove)` — заменяет `LexemeMeaningField`. Лейбл значения резолвится СНАРУЖИ через `componentValueLabel(componentTypeId, snapshotRef, availableTypes)` (A12: живой тип по id + fallback на снимок), см. 04 §1. (`componentLabelOf(type)` — только для chip'ов в `ComponentChipsRow`, где есть живой `ComponentType`.)
  - NEW: `ComponentChipsRow(availableTypes, addedNonMultiTypeIds, enabled, onAddComponent: (ComponentTypeId) -> Unit)` — фильтрует non-multi (если type уже в addedNonMultiTypeIds — скрыть). Multi всегда видим.
  - NEW: `LexemeComponentsBlock(lexemeState, availableTypes, enabled, sendMessage)` — orchestrator: список добавленных полей + chip-row.
  - DELETE: `LexemeMeaningField.kt`, `AddLexemeMeaningRow.kt`.
- `WordCardScreen.kt` — lines 154-203 заменяются вызовом `LexemeComponentsBlock`.

**BlueAssistChip — НЕ трогаем**
- Текущий `BlueAssistChip` read-only — корректно для Manager / PerDict (там chip — индикатор). WordCard'у нужен clickable вариант. Не делаем shared chip между модулями (4 callsite в widget + 1 новый в wordcard — недостаточный коммон).
- В wordcard рендерим chip через локальный `SubentityChip` (уже clickable; правильный API, без переименования). `SubentityChip` остаётся, переиспользуется в `ComponentChipsRow`. Косметический rename «subentity → component» — в backlog (out of scope этой фичи).

**Локализация имени типа (UI-extension)**
- NEW `@Composable fun componentLabelOf(type: ComponentType): String` или `@StringRes Int? + String?` — лукап:
  - `type.systemKey == TRANSLATION` → `R.string.word_card_bottom_translation` (existing).
  - `type.name != null` → `type.name` (user-defined).
- Domain remains label-less.

---

### 5. Infra

- `:modules:screen:wordcard/build.gradle.kts` — без изменений (`:modules:domain:lexeme`, `:modules:core:mate`, `:modules:core:di`, `:modules:core:ui` уже подключены).
- `WordCardModule` (Dagger) — без изменений. `AvailableComponentTypesFlowHandler` — @Inject, разрешается автоматически.
- `WordCardViewModel` — добавляется ctor-параметр `availableTypesFlowHandler: AvailableComponentTypesFlowHandler` (@Inject); добавляется в `effectHandlerSet`. Factory сигнатура без изменений (AssistedInject `wordId / navigator / uiHost`).
- strings.xml — `word_card_bottom_translation`, `word_card_snackbar_lexeme_deleted`, `word_card_snackbar_undo` уже есть (cascade-undo переиспользует существующие). Добавляются ТОЛЬКО:
  - `word_card_error_load_component_types`
  - `word_card_action_retry`
  - `word_card_error_generic` — generic write-error для `OperationFailed` (без неё compile-fail).
  - (M3: отдельная строка `word_card_snackbar_lexeme_cascade_removed` НЕ нужна — используется существующая `word_card_snackbar_lexeme_deleted`.)

---

## Граф зависимостей слоёв

```
Domain (TemplateValues helpers)
  └─> Data API (flowTypesForDictionary)
        └─> Data Impl (wire flowTypesForDictionary через Impl)
              └─> UseCase (REWRITE generic; lexemeId передаётся, reverse-lookup не нужен — A1)
                    └─> Business State (REWRITE → components list)
                          └─> Msg / Effect / Reducer / FlowHandler (REWRITE generic)
                                └─> UI (REWRITE composable tree)
                                      └─> Infra (ViewModel ctor, strings)
```

Этот порядок и есть последовательность имплементации (см. § 06).

---

## Acceptance global

1. Сборка `:app:assembleDebug` + `:app:lintDebug` зелёные.
2. `:modules:screen:wordcard:testDebugUnitTest` и `:app:testDebugUnitTest` зелёные.
3. На WordCard для лексемы из словаря с N global + M per-dict компонентами видна chip-row с (N+M) clickable chips. Built-in `Translation` — first chip.
4. Тап на chip non-multi → input появляется, chip исчезает. Тап на multi-chip → input появляется, chip остаётся.
5. Autosave на blur (как сейчас translation): новое value пишется в `component_values`. A10: правка saved держит `isEdit` до УСПЕХА (success-refresh закрывает, ошибка оставляет поле с текстом → ввод не теряется); in-flight pristine при refresh сохраняется (B4).
6. Trash на любом значении удаляет component_value. Если последний — cascade-delete лексемы (тот же UX что сейчас).
7. Undo для cascade-delete лексемы — restore через `addLexemeWithComponents`.
8. Тесты Translation/Definition заменены на generic ComponentValue suite (минимум parity по покрытию).

---

## Out of scope

- Image template (`ImageValues`). Только TEXT.
- Reorder / drag-and-drop добавленных value.
- Validation длины/формата.
- Undo для одиночного `RemoveComponentValue` (только cascade-lexeme). Match с brief'ом.
- AGG-1 migration `definition` user-defined → built-in `DEFINITION` enum.
- Удаление `@Deprecated` shim'ов `Lexeme.translation/definition`.
- i18n built-in label «Перевод».
- Rename `SubentityChip` → `AddComponentChip` (cosmetic).
