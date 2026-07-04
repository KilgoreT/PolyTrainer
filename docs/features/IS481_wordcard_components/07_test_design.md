# IS481 WordCard Components — Test Design (TDD-контракт)

> **Гайды:** `docs/guides/testing-reducers.md`, `testing-extensions.md`. DSL: `me.apomazkin.mate.test.{testReduce, testScenario, assertEffects, assertNoEffects, assertSingleEffect, assertHasEffect, assertEffectsCount}` + `.state()` / `.effects()`. Эталон строгости — `mate/TranslationManagementTest.kt` (точные значения полей + полный `assertEffects(setOf(...))` + `isPendingDbOp`).

**Назначение.** Это контракт. Тесты пишутся ПЕРВЫМИ (красные — типы из §2-§6 плана ещё не существуют, это ожидаемо), затем код подгоняется под них. **Тесты в ходе реализации не меняются.** Каждый кейс — таблица «вход → точный state + точный Set<Effect>», написуемый только против публичного контракта Msg/State/Effect.

Правило именования (R-TR-001 / R-TE-001): имя теста встраивает инвариант — `..._F7_...`, `..._B2_...`, `..._B1_5_...` — для трассируемости.

---

## §0. Разрешённые contract-gaps (зафиксированы ДО написания тестов)

Без этих решений тесты недетерминированы. Все — технические (не продуктовые), приняты:

| # | Gap | Решение |
|---|---|---|
| G1 | `TemplateValues.asText()` | `fun TemplateValues.asText(): String? = (this as? TextValues)?.value?.value`. См. `02 §`. |
| G2 | `textValuesOf(text)` | `fun textValuesOf(text: String): TemplateValues = TextValues(Primitive.Text(text))`. |
| G3 | **trim-политика** | Единая точка: payload эффекта всегда `textValuesOf(<trimmed>)`. `CommitOutcome.Update(text)` несёт trimmed. Реэмит в `LexemeDraftPromoted` тоже trim. UseCase trim'ит повторно (idempotent). Решение по ветке — по `trimmed`. **Тесты ассертят trimmed.** |
| G4 | snackbar-тип для load-failure | НОВЫЙ `UiEffect.ShowSnackbarWithRetry(messageRes, actionLabelRes, retryMsg)`. НЕ переиспользуем ShowSnackbarWithUndo. |
| G5 | string-ресурсы | `R.string.word_card_error_load_component_types`, `word_card_action_retry`, `word_card_error_generic`, `word_card_error_restore_lexeme` (существующий, A17/H-8), `word_card_snackbar_lexeme_deleted`, `word_card_snackbar_undo` (последние два — существующие). Тесты ассертят эти `@StringRes` Int. |
| G6 | `commitAndCloseAllEdits()` сигнатура | `fun WordCardState.commitAndCloseAllEdits(): Pair<WordCardState, Set<Effect>>`. |
| G7 | `RefreshLexemeComponents` порядок результата | `components = mergedSaved + keptPristine` (saved по порядку payload, pristine в хвосте по порядку state). **Тесты ассертят поэлементно.** |
| G8 | `LexemeDraftPromoted` — позиция | Замена черновика **на его месте**: `lexemeList.map { if (it.id == NOT_IN_DB) merged else it }`. Порядок списка НЕ меняется, свежая лексема остаётся сверху (создана через prepend). Случай «черновика нет» нереален (promote приходит только на свой черновик) — спец-fallback не вводим. |
| G9 | `CreateComponentValue` когда `typeId ∉ availableComponentTypes` | no-op: `assertEquals(initial, state)` + `assertNoEffects()`. (Тип soft-deleted между load и tap — chip уже не показан, но защищаемся.) |

Доп. решения по краевым:
- **closeAllEditModes** для non-empty pristine: сбрасывает `isEdit=false, edited=""` (discard, не commit). pristine с empty edited — удаляется. Empty NOT_IN_DB лексема — удаляется.
- **RemoveComponentValueRequested** для `Saved` с `origin==""`: local nullify (remove из components), без эффекта, без pending.

---

## §1. Reducer-тесты (per Msg)

Общая фикстура (rewrite reference `loaded()` — добавлен `dictionaryId`):

```kotlin
private val reducer = WordCardReducer()
private fun loaded(
    wordId: Long = 7L, dictionaryId: Long = 3L,
    lexemes: List<LexemeState> = emptyList(),
    isPendingDbOp: Boolean = false,
    isExiting: Boolean = false,
    availableTypes: List<ComponentType> = emptyList(),
    nextPristineKey: Long = 1L,
) = WordCardState(
    isLoading = false, isPendingDbOp = isPendingDbOp, isExiting = isExiting,
    wordState = WordState.Loaded(id = wordId, dictionaryId = dictionaryId, added = Date(0L), value = "w"),
    lexemeList = lexemes, availableComponentTypes = availableTypes, nextPristineKey = nextPristineKey,
)
private val TR = ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION)
private fun savedCv(id: Long, typeId: Long = 50L, ref: ComponentTypeRef = TR, origin: String = "",
    isMulti: Boolean = false, isEdit: Boolean = false, isCommitting: Boolean = false, edited: String = "") =
    ComponentValueState(ComponentValueKey.Saved(ComponentValueId(id)), ComponentTypeId(typeId), ref, isMulti, isEdit, isCommitting, origin, edited)
private fun pristineCv(key: Long, typeId: Long = 50L, ref: ComponentTypeRef = TR,
    isMulti: Boolean = false, isCommitting: Boolean = false, edited: String = "") =
    ComponentValueState(ComponentValueKey.Pristine(key), ComponentTypeId(typeId), ref, isMulti, isEdit = true, isCommitting = isCommitting, origin = "", edited = edited)
```

### 1.1 `WordLoadedTest.kt` (REWRITE)

| Тест | initial | Msg | expected state | expected effects |
|---|---|---|---|---|
| `WordLoaded sets word, lexemes, dictionaryId and emits LoadAvailableComponentTypes` | `WordCardState()` (isLoading=true) | `WordLoaded(term{dictId=3, 1 lexeme [cv(TR,"hi")]})` | `wordState=Loaded(id, dictionaryId=3)`, `isLoading=false`, `isPendingDbOp=false`, `lexemeList=[LexemeState(id, [savedCv origin="hi", key=Saved])]` | `setOf(LoadAvailableComponentTypes(3L))` (size==1) |
| `WordLoaded clears pending` | default+pending | `WordLoaded(term)` | `isPendingDbOp=false` | LoadAvailableComponentTypes |
| `WordLoaded maps multi component preserving order` | default | term lexeme=[TR, UserDefined("Example",multi)] | components[0].ref=TR, [1].isMulti=true, size==2, порядок сохранён | LoadAvailableComponentTypes |
| `WordLoaded does not prepopulate availableComponentTypes` | default | WordLoaded | `availableComponentTypes==emptyList()` | LoadAvailableComponentTypes |
| `WordLoaded keeps nextPristineKey unchanged` | default(nextPristineKey=5) | WordLoaded | `nextPristineKey==5` | `setOf(LoadAvailableComponentTypes(3L))` (WordLoaded ВСЕГДА эмитит) |
| `RefreshWord updates word value, resets word-edit, clears pending, keeps lexemes` | `loaded(pending=true, lexemes=[L(1,[savedCv(5)])])` | `RefreshWord(term{value="w2"})` | `wordState.value=="w2"`, `wordState.isEditMode==false`, `wordState.edited==""`, `isPendingDbOp==false`, `lexemeList` без изменений | `assertNoEffects()` |

### 1.2 `ComponentTypesFlowTest.kt` (NEW)

| Тест | initial | Msg | state | effects |
|---|---|---|---|---|
| `ComponentTypesLoaded stores types` | `loaded(availableTypes=[])` | `ComponentTypesLoaded([t1,t2])` | `availableComponentTypes==[t1,t2]` | `assertNoEffects()` |
| `ComponentTypesLoaded overwrites previous` | `loaded(availableTypes=[t1])` | `ComponentTypesLoaded([t2])` | `==[t2]`, lexemeList unchanged | none |
| `ComponentTypesLoaded does not touch lexemeList or pending` | `loaded([t1],lexemes=[L],pending=true)` | `ComponentTypesLoaded([t2])` | lexemeList unchanged, pending unchanged(true) | none |
| `ComponentTypesLoadFailed_emits_retry_snackbar` | `loaded()` | `ComponentTypesLoadFailed(e)` | `assertEquals(initial,state)` | `setOf(ShowSnackbarWithRetry(R.string.word_card_error_load_component_types, R.string.word_card_action_retry, RetryLoadComponentTypes))` |
| `ComponentTypesLoadFailed not guarded by pending` | `loaded(pending=true)` | Failed(e) | unchanged | emits retry snackbar (доказывает не-guarded) |
| `RetryLoadComponentTypes emits load with loaded dictionaryId` | `loaded(dictionaryId=9)` | `RetryLoadComponentTypes` | unchanged | `setOf(LoadAvailableComponentTypes(9L))` |
| `RetryLoadComponentTypes on NotLoaded is no-op` (G-boundary) | `WordCardState()` (NotLoaded) | Retry | unchanged | `assertNoEffects()` |

### 1.3 `ComponentValueLifecycleTest.kt` (NEW)

**CreateComponentValue:**

| Тест | initial | Msg | state | effects |
|---|---|---|---|---|
| `CreateComponentValue appends pristine and increments nextPristineKey` | `loaded(lexemes=[L(1,[])], availableTypes=[type(50,multi=false)], nextPristineKey=1)` | `CreateComponentValue(1, ComponentTypeId(50))` | L(1).components=[pristineCv(key=1,typeId=50,isEdit=true,origin="",edited="")], `nextPristineKey==2` | `assertNoEffects()` (нет открытых edit) |
| `CreateComponentValue commits open edits first` | `loaded(lexemes=[L(1,[savedCv(5,origin="old",isEdit=true,edited="new")])], availableTypes=[type50])` | `CreateComponentValue(1,50)` | #5 commit-эффект эмитнут, **A10: `isEdit` остаётся true** (закроется на success-refresh) + `isCommitting=true`, pristine добавлен, nextPristineKey++, `isPendingDbOp=true` | `setOf(UpsertComponentValue.UpdateValue(lexemeId=1, componentValueId=CvId(5), data=textValuesOf("new")))` |
| `CreateComponentValue_B5_not_guarded_under_pending` | `loaded(pending=true, lexemes=[L(1,[])], availableTypes=[type50 multi])` | `CreateComponentValue(1,50)` | pristine добавлен несмотря на pending; `assertNotEquals(initial.lexemeList, state.lexemeList)` | — |
| `CreateComponentValue sets isMulti from availableTypes` | `loaded(availableTypes=[type(50,multi=true)], lexemes=[L(1,[])])` | `CreateComponentValue(1,50)` | новый pristine `isMulti==true` | — |
| `CreateComponentValue_G9_unknown_type_is_noop` | `loaded(availableTypes=[], lexemes=[L(1,[])])` | `CreateComponentValue(1, ComponentTypeId(999))` | `assertEquals(initial, state)` | `assertNoEffects()` |

**UpdateComponentValueInput:**

| Тест | initial | Msg | state | effects |
|---|---|---|---|---|
| `UpdateComponentValueInput updates only edited on pristine` | `[pristineCv(1,edited="")]` | `UpdateComponentValueInput(1,Pristine(1),"hi")` | edited="hi", origin="", isEdit=true, key unchanged | none |
| `UpdateComponentValueInput updates edited on saved in edit mode` | `[savedCv(5,origin="old",isEdit=true,edited="old")]` | `Update(1,Saved(CvId 5),"new")` | edited="new", origin="old" | none |
| `UpdateComponentValueInput on saved NOT in edit mode is no-op` | `[savedCv(5,origin="x",isEdit=false)]` | `Update(1,Saved(5),"hack")` | unchanged (edited не записан); anti: edited≠"hack" | none |
| `UpdateComponentValueInput not guarded by pending` | `loaded(pending=true,[pristineCv(1)])` | `Update(1,Pristine(1),"hi")` | edited="hi" | none |
| `UpdateComponentValueInput unknown key is no-op` | `[pristineCv(1)]` | `Update(1,Saved(CvId 999),"x")` | unchanged | none |

**EnterComponentValueEditMode:**

| Тест | initial | Msg | state | effects |
|---|---|---|---|---|
| `EnterComponentValueEditMode sets edited=origin and isEdit` | `[savedCv(5,origin="word",isEdit=false)]` | `Enter(1,Saved(5))` | target isEdit=true, edited="word" | none |
| `EnterComponentValueEditMode commits other open edits first` | `[savedCv(5,origin="a",isEdit=false), savedCv(6,origin="old",isEdit=true,edited="new")]` | `Enter(1,Saved(5))` | #6 commit-эффект эмитнут, **A10: #6 isEdit остаётся true** (закроет refresh) + `isCommitting=true`; #5 isEdit=true edited="a"; pending=true | `setOf(UpsertComponentValue.UpdateValue(lexemeId=1, componentValueId=CvId(6), data=textValuesOf("new")))` |
| `EnterComponentValueEditMode also commits word edit` | word `Loaded(isEditMode=true,edited="ed")` + `[savedCv(5)]` | `Enter(1,Saved(5))` | word.isEditMode=false, word.value="ed" | `UpdateWord(7,"ed")` ∈ effects |
| `EnterComponentValueEditMode_IS_guarded_by_pending` | `loaded(pending=true,[savedCv(5,isEdit=false)])` | `Enter(1,Saved(5))` | `assertEquals(initial,state)` | `assertNoEffects()` |

### 1.4 `CommitComponentValueEditTest.kt` (NEW) — полная матрица 4×(pristine/saved)×(NOT_IN_DB/real)

`commitDecision` зависит от `(isEdit, edited.trim(), origin)`.

| # | ветка | edited | origin | key | lexeme.id | expected state | expected effects |
|---|---|---|---|---|---|---|---|
| 1 | NoOp (not editing) | — | — | Saved(5) isEdit=false | real | unchanged | none |
| 2 | NoOp (edited==origin) | "same" | "same" | Saved(5) isEdit=true | real | isEdit=false, edited="", origin="same"; **pending=false** | none |
| 3 | NoOp (trimmed==origin) | "  same  " | "same" | Saved(5) isEdit=true | real | как #2 (trim) | none |
| 4 | LocalRemove pristine (empty+empty) | "   " | "" | Pristine(1) | real(7) | pristine удалён; **pending=false** | none |
| 5 | LocalRemove → cascade empty NOT_IN_DB | "" | "" | Pristine(1) | NOT_IN_DB, only this | лексема удалена целиком; pending=false; `isCreatingLexeme==false` | none |
| 6 | PessimisticRemove (empty edit, non-empty origin) | "" | "x" | Saved(5) isEdit=true | real(7) | **pending=true, isCommitting=true** (B6); компонент остаётся (origin сохранён) | `setOf(RemoveComponentValue(CvId(5), 7L))` |
| 7 | Update saved | "new" | "old" | Saved(5) isEdit=true | real(7) | **pending=true; A10: isEdit остаётся true, isCommitting=true, edited="new"** (закроет success-refresh) | `setOf(UpsertComponentValue.UpdateValue(wordId=7,dictId=3,lexemeId=7,componentValueId=CvId(5),typeId,ref,data=textValuesOf("new")))` |
| 8 | Update pristine, real lexeme | "new" | "" | Pristine(1) | real(7) | pending=true; isCommitting=true; pristine остаётся до Inserted-flip | `setOf(UpsertComponentValue.AddValue(lexemeId=7,pristineKey=1,typeId,ref,data=textValuesOf("new")))` |
| 9 | Update pristine, NOT_IN_DB (создание) | "hello" | "" | Pristine(1) | NOT_IN_DB | pending=true; isCommitting=true | `setOf(UpsertComponentValue.CreateLexeme(pristineKey=1,typeId,ref,data=textValuesOf("hello")))` |
| 10 | Update с trim | "  new  " | "old" | Saved(5) | real | data=textValuesOf("new") (G3) | `UpsertComponentValue.UpdateValue(... data=textValuesOf("new"))` |
| 11 | guarded by pending | "" | "x" | Saved(5) | real | unchanged (guard) | none |

Anti-assertions: #6/#7/#8/#9 — `pending==true` И эффект (ловит «эффект без pending» = залипание). #2/#3/#4/#5 — `pending==false` (ловит «pending на local-only»). #5 — `state.lexemeList.none { it.id==NOT_IN_DB && it.components.isEmpty() }`.

### 1.5 `RemoveComponentValueRequested` (в `ComponentValueLifecycleTest`)

| Тест | key | lexeme | state | effects |
|---|---|---|---|---|
| `pristine removes locally no effect` | Pristine(1) | real(7), 2 comps | pristine удалён, второй остался, pending=false | none |
| `pristine last comp on NOT_IN_DB cascades removal` | Pristine(1) | NOT_IN_DB only | лексема удалена; pending=false | none |
| `saved emits RemoveComponentValue and sets pending` | Saved(5) origin="x" | real(7) | pending=true | `setOf(RemoveComponentValue(CvId(5),7L))` |
| `saved with empty origin local nullify` (G-краевой) | Saved(5) origin="" | real(7) | компонент удалён локально; pending=false | none |
| `guarded by pending` | Saved(5) | real | unchanged | none |

### 1.6 `ComponentValueRefreshTest.kt` (NEW) — `RefreshLexemeComponents` union-by-id (B4), порядок G7

| Тест | initial components | payload | expected components (поэлементно) | pending |
|---|---|---|---|---|
| `overwrites saved origin by id` | [savedCv(5,origin="old")] | [cv(5,"fresh")] | [savedCv(5,origin="fresh",isEdit=false)] | false |
| `A10 refresh closes ONLY committing component` | [savedCv(5,isEdit=true,isCommitting=true,edited="t5"), savedCv(6,isEdit=true,isCommitting=false,edited="t6")] | [cv(5,"db5"),cv(6,"db6")] | #5 → isEdit=false,isCommitting=false,origin="db5" (закрыт); **#6 → isEdit=true,edited="t6" СОХРАНЁН** (origin="db6"); — БЛОКЕР-кейс: refresh от #5 НЕ закрывает редактируемый #6 | false |
| `A10 saved committing refresh closes edit` | [savedCv(5,origin="old",isEdit=true,isCommitting=true,edited="typing")] | [cv(5,"dbval")] | [savedCv(5,origin="dbval",isEdit=false,isCommitting=false,edited="")] | false |
| `updates origin for non-edited saved` | [savedCv(5,origin="old",isEdit=false)] | [cv(5,"new")] | [savedCv(5,origin="new",isEdit=false)] | false |
| `B4_keeps_in_flight_pristine` | [savedCv(5), pristineCv(10,edited="draft")] | [cv(5)] | [savedCv(5 из payload), pristineCv(10,edited="draft")] — pristine в ХВОСТЕ (G7) | false |
| `removes saved absent from payload` | [savedCv(5), savedCv(6)] | [cv(5)] | [savedCv(5)] | false |
| `adds new saved from payload` | [savedCv(5)] | [cv(5),cv(7)] | [savedCv(5),savedCv(7)] | false |
| `B4_multi_race_keeps_pristine_adds_saved` | [savedCv(5), pristineCv(10,edited="x")] | [cv(5),cv(7)] | [savedCv(5),savedCv(7),pristineCv(10)] (saved сначала, pristine в хвост) | false |
| `clears pending` | pending=true | any | — | `isPendingDbOp==false` |
| `unknown lexemeId clears pending, list untouched` | lex(7, pending=true) | Msg.lexemeId=999 | список не тронут; **`isPendingDbOp=false`** (снят даже при unknown — §9.3) | — |
| `not guarded by pending` | pending=true | payload | мутация применена | — |
| `B1_keeps_saved_value_when_types_not_yet_loaded` | availableTypes=[], [savedCv(5,typeId=99)] | [cv(5,type99)] | savedCv(5) присутствует (рендер не зависит от availableTypes — окно ДО `ComponentTypesLoaded`; удалённый тип сюда не относится, его значение не грузится, см. 09 A9) | — |

Anti (пинит «full replace»): в `B4_keeps_in_flight_pristine` — assert компонент `key==Pristine(10)` присутствует И `edited=="draft"`. Full-replace impl падает.

### 1.7 `ComponentValueInserted` (в `ComponentValueRefreshTest`)

| Тест | initial | Msg | expected |
|---|---|---|---|
| `flips pristine to saved` | [pristineCv(10,typeId=50)] | `Inserted(lex,pristineKey=10,newCvId=CvId(77))` | компонент `key==Saved(CvId(77))`, typeId/ref/origin/edited сохранены, isPristine==false; none |
| `idempotent for nonexistent pristineKey` | [savedCv(5)] | `Inserted(lex,999,CvId(77))` | unchanged; none |
| `flip closes edit, keeps edited text` | [pristineCv(10,edited="x",isEdit=true)] | `Inserted(10→77)` | Saved(77), edited="x", **isEdit=false, isCommitting=false** (A10 — успех добавления закрывает edit; данные доедут через Refresh) | 
| `not guarded by pending` | pending=true,[pristineCv(10)] | Inserted(10→77) | flip happens |

### 1.8 `LexemeManagementTest.kt` (REWRITE) — `LexemeDraftPromoted` (B2 / F3/F4 / G8)

| Тест | initial NOT_IN_DB components | Msg | expected merged | expected effects |
|---|---|---|---|---|
| `single anchor pristine yields no reemit` | [pristineCv(1,TR)] | `LexemeDraftPromoted(newLexeme{id=55,[cv(TR)]}, anchorPristineKey=1)` | id=55, [savedCv(TR из promoted)]; pending=false | `assertNoEffects()` |
| `B2_two_pristines_reemit_survivor` | [pristineCv(1,TR), pristineCv(2,Example,edited="ex")] | `Promoted(newLexeme{55,[cv(TR)]}, anchor=1)` | id=55, [savedCv(TR), **pristineCv(2,edited="ex",isCommitting=true)**]; pending=false | `setOf(UpsertComponentValue.AddValue(lexemeId=55,pristineKey=2,typeId=Example,data=textValuesOf("ex")))` |
| `F3_F4_excludes_anchor_by_key_not_order` | order=[pristineCv(5,Example,edited="ex"), pristineCv(1,TR)] (anchor не первый) | `Promoted(newLexeme{55,[cv(TR)]}, anchor=1)` | survivors=[**pristineCv(5,isCommitting=true)**]; НЕТ дубля TR | реэмит только pristineKey=5; **anti: нет эффекта с pristineKey==1; ровно один BuiltIn(TRANSLATION) в components** |
| `F3_F4_B5_added_pristine_after_emit_excluded_by_key` | [pristineCv(1,TR), pristineCv(2,Ex,edited="a"), pristineCv(3,Ex2,edited="b")] | `Promoted(..., anchor=1)` | survivors=[2,3], **оба `isCommitting=true`** (реэмит in-flight) | 2 эффекта (keys 2,3); ни одного key=1 |
| `empty_survivor_dropped_no_reemit` (#2) | [pristineCv(1,TR), pristineCv(2,Ex,edited="")] | `Promoted(newLexeme{55,[cv(TR)]}, anchor=1)` | id=55, components=[savedCv(TR)] (**пустой pristine 2 дропнут — не домержен**) | `assertNoEffects()` (**пустой INSERT не шлём**) |
| `clears pending` | pending=true | promoted | pending=false | — |
| `not guarded by pending` | pending=true | promoted | применено | — |
| `keeps_position_replaces_in_place` | [NOT_IN_DB draft (сверху), real L(8)] | `Promoted(newLexeme{55}, anchor=1)` | [L(55) НА МЕСТЕ черновика (сверху), L(8)]; порядок не изменился; pending=false | — |
| `preserves existing real lexemes` | [real L(8), NOT_IN_DB draft] | promoted | L(8) не тронут на своём месте, draft→55 на своём месте | — |

### 1.9 `UndoDeleteTest.kt` (REWRITE)

| Тест | initial | Msg | state | effects |
|---|---|---|---|---|
| `LexemeCascadeRemoved removes and emits undo` | `loaded(lexemes=[L(8),L(9)])` | `LexemeCascadeRemoved(removed{id=8,[cv(TR,"hi")]})` | lexemeList==[L(9)]; pending=false | `setOf(ShowSnackbarWithUndo(R.string.word_card_snackbar_lexeme_deleted, R.string.word_card_snackbar_undo, UndoRestoreLexeme(removed)))` |
| `LexemeRemoved removes and emits undo` | `[L(8),L(9)]` | `LexemeRemoved(removed{9})` | ==[L(8)]; pending=false | ShowSnackbarWithUndo(...,UndoRestoreLexeme(removed)) |
| `cascade clears pending` | pending=true | LexemeCascadeRemoved | pending=false | snackbar |
| `not guarded by pending` (оба) | pending=true | each | removal применён | — |
| `UndoRestoreLexeme_sets_pending_and_emits_restore` | `loaded(wordId=7,dictId=3)` | `UndoRestoreLexeme(lex{[cv(TR,"hi"),cv(Ex,"e")]})` | pending=true | `setOf(RestoreLexemeWithComponents(7, 3, snapshot = lex))` — H-2: эффект несёт snapshot:Lexeme (маппинг в пары — внутри UseCase) |
| `RemoveLexeme emits effect (unchanged)` | `loaded(wordId=7,[L(8)])` | `RemoveLexeme(8)` | snapshot+emit | `setOf(RemoveLexeme(7,8))` |
| `RemoveLexeme_NOT_IN_DB_local_no_effect_no_undo` | `loaded(wordId=7,[L(NOT_IN_DB), L(8)])` | `RemoveLexeme(NOT_IN_DB)` | черновик удалён локально, L(8) цел; pending=false; `isCreatingLexeme==false` | `assertNoEffects()` (в БД ничего нет — ни RemoveLexeme, ни undo) |

### 1.10 `OperationFailedTest.kt` (NEW — F7 блокер)

| Тест | initial | Msg | state | effects |
|---|---|---|---|---|
| `F7_clears_pending_and_emits_error` | `loaded(pending=true)` | `OperationFailed(R.string.word_card_error_generic)` | `isPendingDbOp==false` | `setOf(ShowErrorSnackbar(R.string.word_card_error_generic))` |
| `F7_does_not_touch_pristine_or_edit` | `loaded(pending=true, lexemes=[L(1,[pristineCv(10,edited="draft"), savedCv(5,isEdit=true,edited="x")])])` | OperationFailed(msg) | `assertEquals(initial.lexemeList, state.lexemeList)` (полное равенство), изменён ТОЛЬКО isPendingDbOp | error snackbar |
| `F7_messageRes_passed_through` | `loaded(pending=true)` | OperationFailed(R.string.X) | — | `ShowErrorSnackbar(messageRes = R.string.X)` (точный ресурс) |
| `F7_not_guarded_runs_while_pending` | pending=true | OperationFailed | обработан (pending снят) | — |
| `F7_clears_isCommitting_keeps_edit` (A10) | `loaded(pending=true, [savedCv(5,isEdit=true,isCommitting=true,origin="old",edited="new")])` | OperationFailed(msg) | `isCommitting==false`, **`isEdit==true`, `edited=="new"` целы**, pending=false (поле открыто для повтора) | error snackbar |

### 1.11 `PendingGuardTest.kt` (NEW) — `isGuardedByPending` для КАЖДОГО Msg

Стратегия: state с `isPendingDbOp=true` + мутабельный setup; для guarded — `assertEquals(initial,state)+assertNoEffects()`; для non-guarded — assert изменение/эффект.

**Guarded:** `RemoveWord`, `CommitWordChanges`, `RemoveLexeme`, `CommitComponentValueEdit`, `RemoveComponentValueRequested`, `EnterComponentValueEditMode`, `EnterWordEditMode`, `CreateLexeme`, `OpenDeleteWordDialog`, `OpenDeleteLexemeDialog`, `OpenTopBarMenu` (сверить с реальным §6.4).

**NOT guarded:** `CreateComponentValue` (B5), `UpdateComponentValueInput`, `RefreshLexemeComponents`, `ComponentValueInserted`, `LexemeDraftPromoted`, `LexemeCascadeRemoved`, `LexemeRemoved`, `ComponentTypesLoaded`, `ComponentTypesLoadFailed`, `RetryLoadComponentTypes`, `WordLoaded`, `RefreshWord`, `OperationFailed`, `UndoRestoreLexeme`, `NavigateBack`, `NoOperation`, диалог-close Msg, `UpdateWordInput`.

Два именованных теста на осознанные отступления:
- `CreateComponentValue_NOT_guarded_deviation_from_CreateTranslation` — старый reducer guard'ил `CreateTranslation`; новый НЕ guard'ит `CreateComponentValue`.
- `EnterComponentValueEditMode_IS_guarded_parity_with_EnterTranslationEditMode`.

Плюс позитивный тест C6:
- `NoOperation_is_pure_noop` — `NoOperation` → `assertEquals(initial, state)` + `assertNoEffects()` (при любом pending; ветка есть и ничего не делает).

### 1.12 `NavigateBackTest.kt` (NEW — flush-on-back, маркеры §6.2.3)

| Тест | initial | Msg | expected state | expected effects |
|---|---|---|---|---|
| `clean_back_navigates_immediately` | `loaded(lexemes=[savedCv(5)])` (нет открытых правок) | `NavigateBack` | **`isExiting=true`** (остаётся; Back эмитится на ПЕРЕХОДЕ false→ready); lexemeList без изменений | `setOf(NavigationEffect.Back)` |
| `empty_draft_back_cleans_then_navigates` | `loaded(lexemes=[L(NOT_IN_DB,[pristineCv(1,edited="")])])` | `NavigateBack` | пустой черновик удалён (`commitAndCloseAllEdits` B3); **`isExiting=true`** | `setOf(NavigationEffect.Back)` |
| `dirty_saved_back_sets_exiting_no_navigate_yet` | `loaded(lexemes=[L(7,[savedCv(5,isEdit=true,edited="new",origin="old")])])` | `NavigateBack` | компонент `isCommitting=true`, `isEdit=true`; `isExiting=true`; `isPendingDbOp=true` | `setOf(UpsertComponentValue.UpdateValue(...,componentValueId=CvId(5),data=text("new")))` — **`NavigationEffect.Back` НЕ эмитится** (есть in-flight) |
| `dirty_NOT_IN_DB_draft_back_waits_for_create` (дыра A) | `loaded(lexemes=[L(NOT_IN_DB,[pristineCv(1,TR,edited="t")])])` | `NavigateBack` | **anchor pristine `isCommitting=true`**; `isExiting=true`; `isPendingDbOp=true` | `setOf(UpsertComponentValue.CreateLexeme(pristineKey=1,...))` — **без `Back`** (ждём INSERT; иначе преждевременный выход) |
| `flush_done_navigates` | `loaded(isExiting=true, lexemes=[L(7,[savedCv(5,isEdit=true,isCommitting=true,origin="old",edited="new")])])` | `RefreshLexemeComponents(7,[cv(5,"new")])` | компонент `isEdit=false,isCommitting=false,origin="new"`; `hasInFlightCommits==false`; **`isExiting=true`** (остаётся; Back на ПЕРЕХОДЕ in-flight→done) | эффекты содержат `NavigationEffect.Back` (пост-шаг §6.1) |
| `late_msg_after_back_no_double_nav` | `loaded(isExiting=true, lexemes=[savedCv(5)])` (Back уже эмитнут, isExiting ОСТАЁТСЯ true, in-flight нет) | `RefreshWord(term)` / `ComponentTypesLoaded(...)` | без навигации (`readyBefore==readyNow`, перехода нет) | `assertNoEffects()` по навигации — **второго `NavigationEffect.Back` НЕТ** |
| `clean_double_back_no_second_nav` | `loaded(isExiting=true, lexemes=[savedCv(5)])` (первый Back эмитнут, in-flight нет, isExiting=true) | `NavigateBack` (второй тап) | unchanged (no-op: `isExiting` уже true) | `assertNoEffects()` — **второго `Back` нет** (нет перехода) |
| `flush_still_has_inflight_no_navigate` | `loaded(isExiting=true, lexemes=[L(55,[savedCv(anchor), pristineCv(2,isCommitting=true), pristineCv(3,isCommitting=true)])])` (2 выживших pristine в полёте после промоушена — реальный multi-inflight; saved-вариант недостижим, правки сериализованы) | `ComponentValueInserted(55,pristineKey=2,CvId(77))` (долетел только survivor #2) | #2 → Saved(77),`isCommitting=false`; #3 всё ещё pristine `isCommitting=true`; `hasInFlightCommits==true`; `isExiting` остаётся true | **без `NavigationEffect.Back`** (ждём #3) |
| `survivor_AddValue_fail_no_hang` (дыра-hang) | `loaded(isExiting=true, lexemes=[L(55,[savedCv(anchor), pristineCv(2,isCommitting=true,edited="ex")])])` | `OperationFailed(R.string.X)` | survivor #2 **`isCommitting=false`** (снят даже у pristine!), `edited="ex"` цел; `isExiting=false` | `setOf(ShowErrorSnackbar)` — без `Back`; `hasInFlightCommits==false` → лоадер не виснет |
| `flush_fail_cancels_exit_stays` | `loaded(isExiting=true, isPendingDbOp=true, lexemes=[L(7,[savedCv(5,isEdit=true,isCommitting=true,edited="new",origin="old")])])` | `OperationFailed(R.string.X)` | `isExiting=false`, `isPendingDbOp=false`, `isCommitting=false`, **`isEdit=true`,`edited="new"` целы** | `setOf(ShowErrorSnackbar(R.string.X))` — **без `Back`** |
| `second_back_while_exiting_noop` | `loaded(isExiting=true, lexemes=[L(7,[savedCv(5,isCommitting=true)])])` | `NavigateBack` | unchanged (`isExiting` остаётся true) | `assertNoEffects()` (нет повторного commit, нет `Back` — in-flight ещё держит) |

Anti: `dirty_saved_back...` обязан пинить ОТСУТСТВИЕ `NavigationEffect.Back` в Set (иначе выход до записи). `flush_done_navigates` пинит присутствие `Back` ровно когда снят ПОСЛЕДНИЙ `isCommitting`.

---

## §2. State extension-тесты (testing-extensions.md)

### 2.1 `ext/MappersTest.kt`

- `toLexemeState maps id and components` — `Lexeme(LexemeId(8),[cv1,cv2])` → `LexemeState(id=8, components.size==2)`, каждый `key is Saved`.
- `toComponentValueState maps fields and origin from data` — `cv(5, type{50,isMulti=true,systemKey=TRANSLATION}, text("hi"))` → `ComponentValueState(key=Saved(CvId(5)), componentTypeId=ComponentTypeId(50), componentTypeRef=BuiltIn(TRANSLATION), isMulti=true, origin="hi", edited="", isEdit=false)`.
- `G1_origin_empty_when_data_not_text` — `data=ImageValues(...)` → `origin==""` (asText null → orEmpty).
- `toComponentValueState user-defined ref by name` — `type{systemKey=null,name="Example"}` → `componentTypeRef==UserDefined("Example")`.
- `toLexemeState preserves order`.

### 2.2 `ext/LexemeComponentExtTest.kt`

- `findByKey` — found by Pristine; found by Saved; null для absent; **Pristine(1) ≠ Saved(CvId(1))** (одно число, разный тип ключа — не коллизируют).
- `updateComponent` — применяет transform к matched, остальные не тронуты (immutability), no-op для unknown, id лексемы unchanged.
- `removeComponent` — удаляет matched, остальные сохранены, unknown→unchanged, удаление последнего → пустой components (БЕЗ авто-удаления лексемы — это работа reducer).
- `appendPristine` — добавляет в хвост, существующие сохранены, size+1.

### 2.3 `ext/CommitDecisionTest.kt` (сердце — truth table)

| isEdit | edited | origin | expected |
|---|---|---|---|
| false | "x" | "y" | `NoOp` |
| true | "" | "" | `LocalRemove` |
| true | "   " | "" | `LocalRemove` (trim empty) |
| true | "" | "x" | `PessimisticRemove` |
| true | "   " | "x" | `PessimisticRemove` |
| true | "same" | "same" | `NoOp` |
| true | "  same  " | "same" | `NoOp` (trimmed==origin) |
| true | "new" | "old" | `Update("new")` |
| true | "  new  " | "old" | `Update("new")` (G3 trim) |
| true | "new" | "" | `Update("new")` (pristine first commit) |

Один тест на строку, имя в стиле `commitDecision_returns_PessimisticRemove_when_editing_emptied_nonempty_origin`. Assert точный sealed-инстанс (`is CommitOutcome.Update && text=="new"`). **Anti-confusion:** строка `("", "")→LocalRemove` обязана assert'ить именно LocalRemove (не NoOp).

### 2.4 `CommitAndCloseAllEditsTest.kt`

`fun WordCardState.commitAndCloseAllEdits(): Pair<WordCardState, Set<Effect>>` (G6).

| Тест | initial lexeme(s) | expected state | expected effects |
|---|---|---|---|
| `mix_of_outcomes_emits_correct_effects` | L(7,[savedCv(5, edited==origin → NoOp), savedCv(6,Update "new"/"old"), savedCv(8,Pessimistic ""/"x"), pristineCv(9,LocalRemove ""/"" )]) | #5 reset isEdit=false (NoOp); **#6 isEdit остаётся true + isCommitting=true (A10, Update)**; #8 эмитит remove + isCommitting=true; #9 удалён; pending=true | `setOf(UpsertComponentValue.UpdateValue(lexemeId=7,componentValueId=CvId(6),data=text("new")), RemoveComponentValue(CvId(8),7))` (ровно 2) |
| `B6_only_Update_PessimisticRemove_set_pending` (isolated) | L([Update only]) | pending=true | UpsertComponentValue |
| `B6_NoOp_LocalRemove_do_not_set_pending` (isolated) | L([NoOp saved, LocalRemove pristine]) | pending=false | `assertNoEffects()` |
| `NOT_IN_DB_3_pristine_emits_exactly_one_upsert` (#17) | L(NOT_IN_DB,[pristineCv(1,TR,"t"), pristineCv(2,Ex,"e"), pristineCv(3,Ex2,"x")]) | pending=true; survivors остаются локально | size==1, единственный = `UpsertComponentValue.CreateLexeme(pristineKey=1, ...)` (anchor=первый по порядку, A2) |
| `NOT_IN_DB_anchor_first_by_order_NOT_translation` (A2) | L(NOT_IN_DB,[pristineCv(5,Ex,"e"), pristineCv(1,TR,"t")]) | — | единственный эффект `CreateLexeme(pristineKey==5)` — первый по порядку (Ex), НЕ TR (A2: без приоритета TRANSLATION) |
| `NOT_IN_DB_picks_first_by_order` | L(NOT_IN_DB,[pristineCv(2,Ex,"a"), pristineCv(3,Ex2,"b")]) | — | единственный эффект `CreateLexeme(pristineKey==2)` |
| `B3_NOT_IN_DB_single_empty_pristine_removed` | L(NOT_IN_DB,[pristineCv(1,TR,edited="")]) | lexemeList пуст; pending=false; `isCreatingLexeme==false` | `assertNoEffects()` |
| `B3_NOT_IN_DB_empty_plus_filled_keeps_lexeme` | L(NOT_IN_DB,[pristineCv(1,TR,""), pristineCv(2,Ex,"e")]) | #1 dropped, #2 anchor | size==1 эффект |
| `word_edit_committed_alongside_components` | word Loaded(isEditMode=true,edited="ed") + Update component | word.value="ed",isEditMode=false | effects include `UpdateWord(7,"ed")` |

### 2.5 `ext/CloseAllEditModesExtTest.kt` (NEW, чистый extension)

- `drops_pristine_with_empty_edited` — [pristineCv(1,edited=""), savedCv(5,isEdit=true,edited="x")] → #1 удалён, #5 isEdit=false,edited="" (discard, без эффекта).
- `discards_nonempty_pristine_text` — pristineCv(1,edited="hi") → остаётся? **Решение G:** non-empty pristine при closeAll → сбрасывается isEdit=false,edited="" но НЕ удаляется (origin="" остаётся pristine-плейсхолдер). Assert это явно.
- `resets_word_edit_mode` — Loaded(isEditMode=true,edited="ed") → isEditMode=false,edited="" (value НЕ продвигается — это cancel).
- `removes_empty_NOT_IN_DB_lexeme`.
- `does_not_change_availableComponentTypes_or_nextPristineKey` (immutability).

### 2.6 `ext/LexemeStateTest.kt`

- `addedNonMultiTypeIds_excludes_multi` — [savedCv(typeId=1,multi=false), savedCv(typeId=2,multi=true)] → `{ComponentTypeId(1)}`.
- `empty_when_all_multi` → emptySet.
- `dedupes_same_typeId` → set.
- `includes_pristine_non_multi` — pristine non-multi тоже скрывает chip (assert входит в set).

---

## §3. Scenario-тесты (end-to-end через reducer, `testScenario`)

Файл `mate/scenario/WordCardScenarioTest.kt`. Фикстуры — `ScenarioFixtures` (типы TRANSLATION/DEFINITION/EXAMPLE(multi)/SYNONYM/IMAGE). Проверять state+effects на ключевых шагах + финал.

22 юзкейса + 4 trap (S-trap-1..4). Сжатый список (полные цепочки — в Review B; ключевые ниже):

- **S1** Новый словарь: `WordLoaded`→`LoadAvailableComponentTypes`; `ComponentTypesLoaded([TR])` → `availableComponentTypes==[TR]`.
- **S2** Старый словарь: `ComponentTypesLoaded([TR,DEF,EX,SYN])` → size==4, built-in first; нет `hasDefinitionComponent`-флага.
- **S3** non-multi add: `CreateComponentValue(SYN)`→pristine; `UpdateInput("syn")`; `Commit`→Upsert(pristineKey,UserDefined("Synonym"),text("syn")),pending; `RefreshLexemeComponents([cv(50)])`+`ComponentValueInserted(→CvId(50))`→финал Saved(50), pending=false, `SYN.id ∈ addedNonMultiTypeIds`.
- **S4** multi ×3: три pristine с РАЗНЫМИ pristineKey (1,2,3), `nextPristineKey==4`, `EX.id ∉ addedNonMultiTypeIds`.
- **S5** NOT_IN_DB первым НЕ-translation: `Commit(Pristine)`→Upsert(lexemeId=null,UserDefined("Example")); `LexemeDraftPromoted(900,anchor)`→id=900, no extra effects, pending=false.
- **S6 (B2 центр)** NOT_IN_DB неск.компонентов: `commitAndCloseAllEdits`→РОВНО 1 Upsert(lexemeId=null,anchor=TR); `LexemeDraftPromoted(900,[cv(TR)],anchor)`→merged=promoted+2 survivors, **2 реэмит Upsert(lexemeId=900,pristineKey=survivors)**, anchor НЕ реэмичен. + trap-вариант: anchor не первый + добавлен pristine после INSERT (B5) → исключение по ключу, нет дубля.
- **S7** edit saved: Enter→Update→Commit→Upsert(componentValueId)→Refresh→origin updated.
- **S8** switch fields: Enter(A)→Input→Enter(B)→commitAndCloseAllEdits эмитит Upsert(A), B в edit.
- **S9** clear→blur→delete (P1): Update("")→Commit→PessimisticRemove→RemoveComponentValue.
- **S10** trash non-multi→chip возврат: `addedNonMultiTypeIds` снова без typeId.
- **S11** delete last→cascade+undo: Remove→`LexemeCascadeRemoved`→snackbar→`UndoRestoreLexeme`→Restore→`WordLoaded` restored.
- **S12** delete one of multi: Remove→`RefreshLexemeComponents([остальные])`.
- **S13** реактивный chip: повторный `ComponentTypesLoaded([TR,SYN])`→availableComponentTypes обновлён, lexeme не тронут.
- **S15 (P2)** back: на Msg ухода `commitAndCloseAllEdits` — empty pristine dropped, non-empty edit → upsert. Проверяем reducer-наблюдаемую часть (effects+cleanup).
- **S16** независимость лексем: мутация L(100) не трогает L(200).
- **S17** load fail→retry: `ComponentTypesLoadFailed`→retry snackbar; `RetryLoadComponentTypes`→Load; `ComponentTypesLoaded`→set.
- **S18 (translation parity)** S3 но с TR/`BuiltIn(TRANSLATION)`: ТОТ ЖЕ Msg/Effect путь, нет translation-специфичных Msg/Effect.
- **S19** Definition как user-defined: add/edit/delete цикл на UserDefined("Definition") идентичен любому.
- **S20 (P3)** image: `ComponentTypesLoaded([TR,IMAGE])`→оба в availableComponentTypes (фильтр — UI; reducer хранит обоих). Фильтрация chip по `template!=IMAGE` — в UI-тесте.
- **S-trap-1 (F7)** ошибка INSERT при promote: вместо `LexemeDraftPromoted` приходит `OperationFailed`→pending=false, pristine/edit не тронуты, лексема всё ещё NOT_IN_DB, ShowErrorSnackbar; следующий guarded Msg проходит.
- **S-trap-2 (F9/B5)** быстрый multi commit при pending: commit#1→pending; commit#2 проглочен guard (значение не потеряно, осталось pristine); `RefreshLexemeComponents` снимает pending; повторный commit#2→эмитит. И: `CreateComponentValue` НЕ guarded (два pristine при pending).
- **S-trap-3 (B4)** Refresh/Inserted порядок: state=[Saved(5),Pristine(P10)"typing"]; `RefreshLexemeComponents([5,7])`→P10 сохранён,7 добавлен,5 обновлён; затем `Inserted(P10→11)`→flip. Обратный порядок (Inserted раньше Refresh) → без потери.
- **S-trap-4 (A17 restore-fail)** удалил → `LexemeCascadeRemoved`/`LexemeRemoved` → undo → `RestoreLexemeWithComponents(snapshot)` → restore упал → `RestoreLexemeFailed(snapshot)` → `isPendingDbOp=false` + `ShowSnackbarWithRetry(word_card_error_restore_lexeme, word_card_action_retry, UndoRestoreLexeme(snapshot))`; тап retry → снова `RestoreLexemeWithComponents(snapshot)` (snapshot тот же, цел). Пин: snapshot в retry-Msg идентичен исходному; pending снят.

---

## §4. UseCase-тесты — `WordCardUseCaseImplTest.kt` (REWRITE)

Mock `CoreDbApi.LexemeApi`. Удалить legacy (`addLexemeTranslation`/`deleteDefinitionComponent`/`getComponentTypes`/`addLexemeWith{BuiltIn,UserDefined}Component`/`restoreLexeme(t,d)`).

| # | Тест | stub | assert |
|---|---|---|---|
| T1 | `addLexemeWithComponent_happy_BuiltIn` | `addLexemeWithComponents(7,3, [BuiltIn(TR) to data]) returns 100`; `getLexemeById(100) returns entity` | Lexeme id=100; `coVerify { addLexemeWithComponents(7L,3L, match{ it.single().first is BuiltIn }) }` |
| T2 | `addLexemeWithComponent_UserDefined_branch` | ref=UserDefined("Example") | match `first is UserDefined` |
| T3 | `addLexemeWithComponent_null_type_not_found` | `addLexemeWithComponents returns null` | result null; `coVerify(exactly=0){ getLexemeById(any()) }` |
| T4 | `addComponentValue_returns_result_with_exact_newId` | `addComponentValue(100,3,data) returns 60`; `getLexemeById(100) returns entity` | `result.newComponentValueId==ComponentValueId(60)`, `result.lexeme.lexemeId.id==100` |
| T5 | `addComponentValue_getLexemeById_null_returns_null` | addComponentValue 60, getLexemeById null | null |
| T6 | `addComponentValue_exception_returns_null` | api throws | null |
| T7 | `updateComponentValue_happy_via_lexemeId_param` | `updateComponentValue(50,data) returns 1`; `getLexemeById(100) returns entity` (lexemeId=100 — ПАРАМЕТР, A1) | Lexeme id=100; `coVerify{ getLexemeById(100L) }` |
| T8 | `updateComponentValue_returns_0_returns_null` | update returns 0 | null; `coVerify(exactly=0){ getLexemeById(any()) }` |
| T9 | `updateComponentValue_softDeletedType_B1_5_caught_returns_null` | `updateComponentValue throws IllegalStateException` | result **null** (не проброшено). NB: защита data-слоя; в потоке WordCard недостижимо (значение с удалённым типом не грузится, 09 A9), но UseCase обязан не падать |
| T10 | `updateComponentValue_getLexemeById_null_returns_null` | update 1, `getLexemeById(100) returns null` | null |
| T11 | `deleteComponentValue_ComponentRemoved_when_remaining` | snapshot getLexemeById before (2 comps); `deleteComponentValue(50) returns 1`; getLexemeById after (1 comp) | `RemoveComponentResult.ComponentRemoved`, lexeme.components.size==1; `coVerify(exactly=0){ deleteLexeme(any()) }` |
| T12 | `deleteComponentValue_LexemeCascade_when_zero` | snapshot before (1 comp); `deleteComponentValue returns 0`; `deleteLexeme(100) returns 1` | `LexemeCascadeRemoved(removedLexeme==snapshot)`; `coVerify{ deleteLexeme(100L) }` |
| T13 | `deleteComponentValue_works_without_removedAt_check` | delete не бросает (нет `check(removedAt)`) | success path (контраст с T9: delete не валидирует removedAt типа) |
| T14 | `deleteComponentValue_snapshot_null_returns_null` | before-snapshot null | null, delete не вызван |
| T15 | `restoreLexemeWithComponents_maps_and_returns` / `_null` | `addLexemeWithComponents returns 900`/null | Lexeme / null |
| T16 | `flowAvailableComponentTypes_maps_domain` | `flowTypesForDictionary(10) returns flowOf([trEntity, exEntity])` | collect→size 2, [0].systemKey==TRANSLATION, isMulti carried |
| T17 | `deleteLexeme_returns_Removed_snapshot` | getLexemeById before; deleteLexeme returns 1 | `RemoveLexemeResult.Removed(snapshot)` |
| T18 | `addComponentValue_trims_before_write` (G3 data-layer) | data из "  v  " | `coVerify{ addComponentValue(any(),any(), match{ it.asText()=="v" }) }` |

---

## §5. DAO-тесты — НЕ НУЖНЫ (A1)

A1 удалил `selectLexemeIdById` → новых DAO-методов нет → `ComponentValueDaoTest` не создаётся, instrumented-стадия не требуется. `ComponentTypeDao.flowTypesForDictionary` уже существует и покрыт интеграционными UseCase-тестами (`PerDictionaryComponentsUseCaseImplTest` использует идентичный `flowUserDefinedTypesForDictionary`); отдельно его сортировку/фильтр пинит reducer-fixture (`WordLoaded → ComponentTypesLoaded c TRANSLATION → chip first`). См. `02 §5`.

---

## §6. Anti-regression: Translation как обычный компонент (F8 блокер)

**Цель:** удаляемый `TranslationManagementTest` был эталоном поведения перевода. Унификация не должна тихо его изменить. Группа в `ComponentValueLifecycleTest` (или отдельный `TranslationParityTest.kt`), где фикстура — `ComponentValueState(componentTypeRef = BuiltIn(TRANSLATION))`, реплицирующая ключевые кейсы старого `TranslationManagementTest` на новой модели (точный состав — §9.6: из 15 тестов оригинала к generic-пути релевантны 7; НЕ «1:1»):

- `translation_create_gives_empty_editable_pristine` (как старый `CreateTranslation`).
- `translation_update_input`.
- `translation_enter_edit_commits_pending` (switch).
- `translation_commit_4_branches` (NoOp/LocalRemove/PessimisticRemove/Update) — точные эффекты.
- `translation_refresh_closes_edit` (A10: success-refresh закрывает edit).
- `translation_cascade_on_last_component` (удаление перевода-единственного-компонента → cascade).
- `translation_pending_guard`.

Каждый assert — точные значения как в оригинале. Это «снимок старого теста на новой модели» — ловит изменение поведения перевода.

---

## §7. Сводка покрытия инвариантов (трассировка)

| Инвариант | Тест(ы) |
|---|---|
| B1 render до загрузки типов (reducer) | 1.6 `B1_keeps_saved_value_when_types_not_yet_loaded` |
| B1.5 DB-защита (UseCase, в WordCard недостижимо) | T9, T13 |
| B2 pristine-merge | 1.8 `B2_...`, S6 |
| B3 empty NOT_IN_DB cleanup | 1.4 #5, 2.4 `B3_...` |
| B4 union-by-id | 1.6 (вся таблица), S-trap-3 |
| B5 Create не guarded | 1.3 `B5_...`, 1.11, S-trap-2 |
| B6 pending только Update/Pessimistic | 1.4 #2-9, 2.4 `B6_...` |
| B7 ext-тесты | 2.1-2.6 (rewrite/delete) |
| F3/F4 anchor by key | 1.8 `F3_F4_...` |
| F7 OperationFailed | 1.10, S-trap-1 |
| A10 isEdit держится до success-refresh | 1.4 #7, 1.6 (saved closes), CommitComponentValueEditTest |
| P1 empty=delete | 1.4 #6, S9 |
| P3 image filter | S20 (UI-тест) |
| Translation parity | §6 |
| pristineKey уникальность/монотонность | S4, 1.3 |
| guard полнота | 1.11 |

---

## §8. Открытые предусловия перед написанием тестов

1. **DAO-тесты** — НЕ нужны (A1 убрал `selectLexemeIdById`; новых DAO-методов нет). `flowTypesForDictionary` уже существует и покрыт UseCase-тестами. См. §5 / `02 §5`.
2. **`AvailableComponentTypesFlowHandlerTest`** — см. §9.4 (полный 5-тестовый набор с механикой отмены).
3. **`DatasourceEffectHandlerTest`** two-Msg burst — см. §9.5.
4. **`CreateLexemeTest`** (был пропущен): `CreateLexeme` → prepend NOT_IN_DB; при `isCreatingLexeme` → no-op; при pending → guarded; при открытой правке → `commitAndCloseAllEdits` effects + черновик.
5. **`NavigateBackTest`:** flush-on-back — полный набор в §1.12. Кратко: dirty → `commitAndCloseAllEdits`+`isExiting=true`, `Back` только когда `hasInFlightCommits=false`; clean/empty → `Back` сразу; ошибка → `isExiting=false`, остаёмся.
6. **`WordLoadedTest` + WordNotFound:** `LoadWord` null → `WordNotFound`; `LoadWord` exception → `WordNotFound` (НЕ generic, `isLoading=false`); `WordNotFound` → `isLoading=false` + `NavigationEffect.Back`.
7. **A10 lifecycle (redesign — isCommitting):**
   - `CommitComponentValueEdit(Update)` → `isEdit=true`, `isCommitting=true`, pending=true.
   - success-`RefreshLexemeComponents` → закрывает edit ТОЛЬКО компонента с `isCommitting` (по id); другой редактируемый (`isCommitting=false`) сохраняет `isEdit/edited` (id-targeted, БЛОКЕР-кейс).
   - `OperationFailed` после commit → `isCommitting=false` со всех, `isEdit/edited` целы (поле открыто, повтор возможен).
   - **anti-дубль:** `commitAndCloseAllEdits` / `CreateComponentValue` (не guarded) при наличии компонента с `isCommitting=true` → НЕ переэмитят его (нет второго Upsert). Тест: pending+isCommitting → CreateComponentValue → 0 повторных Upsert для in-flight.

---

## §9. Усиление после построчного ревью (АВТОРИТЕТНО — supersedes §1-§8 где конфликт)

Построчное ревью 3 агентов вскрыло: 3 контрактных противоречия (разрешены в `03`), отсутствие фикстур-фабрик, под-ассерченные кейсы, неполную §6. Этот раздел — обязательный слой поверх §1-§8.

### 9.0 Разрешённые контрактные противоречия (зафиксированы в `03`)
- **S-DUP:** `ComponentValueInserted` dedup-aware — если `Saved(newCvId)` уже есть, pristine УДАЛЯЕТСЯ (не флипается в дубль). `03 §6.2` таблица.
- **S-CREATE:** пустые pristine НЕ стекаются (`CreateComponentValue`→`commitAndCloseAllEdits`→LocalRemove пустых). Multi-тесты ОБЯЗАНЫ чередовать `UpdateComponentValueInput`. `03 §6.4`.
- **S-TRAP3:** `RefreshLexemeComponents.components` = всегда post-mutation полное состояние; reverse-order тест использует payload, включающий newCvId. `03 §6.2.2`.

### 9.1 Глобальные правила ассертов (применять ко ВСЕМ кейсам §1-§3)

- **G-ASSERT-1 (full-equality).** Где тест меняет компонент/список — ассертить ВЕСЬ `ComponentValueState` и ВЕСЬ `components` через `assertEquals`, не подмножество полей. Это одновременно пинит `key`, `componentTypeId`, `componentTypeRef`, `isMulti`, `isEdit`, `origin`, `edited` И порядок (G7).
- **G-ASSERT-2 (effect literal, A3 sealed).** Любой `UpsertComponentValue` в `assertEffects(setOf(...))` — ассертить КОНКРЕТНЫЙ вариант (`CreateLexeme` / `AddValue` / `UpdateValue`) со ВСЕМИ его полями литералами (у каждого варианта свой набор: Create — без lexemeId/cvId; Add — с lexemeId+pristineKey; Update — с lexemeId+componentValueId). Никаких `...`. Никаких `∈`/`assertHasEffect` где можно `assertEffects(setOf(...))`.
- **G-ASSERT-3 (OperationFailed whole-state).** `OperationFailed` сбрасывает `isPendingDbOp=false`, снимает `isCommitting=false` со ВСЕХ компонентов всех лексем (вкл. pristine-survivors — иначе маркер залипнет → вечный лоадер) И **сбрасывает `isExiting=false`, если был true** (запись упала при выходе → отменяем выход, остаёмся показать ошибку); `isEdit/edited` НЕ трогает (ввод цел, повтор возможен). Ассертить весь state: `initial` с `isPendingDbOp=false`, `isExiting=false`, всеми компонентами с `isCommitting=false`, прочие поля без изменений. Если в фикстуре не было ни `isCommitting=true`, ни `isExiting=true` — эквивалентно `initial.copy(isPendingDbOp=false)`. Согласовано с §8.7 и 03 §6.2 (authoritative).
- **G-ASSERT-4 (guarded ⇒ paired positive).** Для КАЖДОГО guarded Msg — пара: (a) `pending=true` → `assertEquals(initial,state)+assertNoEffects()`; (b) `pending=false` (тот же setup) → мутация ПРОИСХОДИТ. Без (b) guard-тест тривиально зелёный.
- **G-ASSERT-5 (фикстуры дат).** Все доменные билдеры (`ComponentType`, `Lexeme`, `ComponentValue`, `Term`) — фиксированные `Date(0L)` для `createdAt/updatedAt/addDate/changeDate`. Иначе `assertEffects`-set с доменным payload (LexemeDraftPromoted/Undo/Cascade) недетерминирован.

### 9.2 Фикстуры-фабрики (ОБЯЗАТЕЛЬНЫ — без них ничего не компилируется)

Дополнить фикстуру §1 (реальные конструкторы: `ComponentType` = 10 полей, `ComponentValue` = id/lexemeId/type/data):
```kotlin
private val D0 = Date(0L)
private fun ctype(id: Long, ref: ComponentTypeRef, isMulti: Boolean = false, dictId: Long? = 3L, position: Int = 0) =
    ComponentType(
        id = ComponentTypeId(id),
        systemKey = (ref as? ComponentTypeRef.BuiltIn)?.key,
        dictionaryId = if (ref is ComponentTypeRef.BuiltIn) null else dictId,
        name = (ref as? ComponentTypeRef.UserDefined)?.name,
        template = ComponentTemplate.TEXT, position = position, isMulti = isMulti,
        createdAt = D0, updatedAt = D0, removedAt = null,
    )
private fun domainCv(id: Long, lexemeId: Long, type: ComponentType, text: String) =
    ComponentValue(ComponentValueId(id), LexemeId(lexemeId), type, textValuesOf(text))
private fun domainLexeme(id: Long, comps: List<ComponentValue>) =
    Lexeme(LexemeId(id), comps, addDate = D0, changeDate = null)
private fun term(dictId: Long, lexemes: List<Lexeme>) = Term(/* WordId, Word, dictId, lexemes, dates=D0 */)
```
**Правило (X2):** payload Msg (`RefreshLexemeComponents.components`, `LexemeDraftPromoted.newLexeme`) — ДОМЕННЫЕ типы (`domainCv`/`domainLexeme`); expected state — `savedCv`/`pristineCv` (state-типы). НЕ путать.

### 9.3 Недостающие/усиленные кейсы §1-§3 (добавить)

**§1.1 WordLoaded:** + reload-wipes-pristine (full rebuild затирает in-flight pristine/edits — restore-after-undo путь); + empty lexemeList boundary; row5 effects = `setOf(LoadAvailableComponentTypes(3L))` (не «—»).

**§1.2:** + `ComponentTypesLoaded([])` очищает непустой список (B1: рендер уже добавленных значений не зависит от `availableComponentTypes` — окно до загрузки типов); + RetryLoad под pending fires.

**§1.3:** + раскрыть «...» эффекты (G-ASSERT-2); + multi same-typeId collision в UpdateInput (бьёт точный key); + unknown lexemeId/key no-op family; + Create non-multi уже-добавленного типа — решение: всё равно append (dedup только UI через `addedNonMultiTypeIds`); + Create при открытом word-edit → `UpdateWord` в effects.

**§1.4 CommitComponentValueEdit:** + whitespace-only PessimisticRemove на Saved (`edited="   ", origin="x"` — доказывает маршрут через `commitDecision`/trim); + #8/#9 пинят survivor-pristine `isEdit`/`edited` post-commit + (#9) `isCreatingLexeme==true`; + **A10:** после Commit-Update `isEdit=true` (держится), success-`RefreshLexemeComponents` → `isEdit=false`; commit-Update→`OperationFailed` → `isEdit=true`, `edited` цел (ввод не потерян).

**§1.6 Refresh:** + empty-payload (все saved удалены, pristine оставлены, лексема НЕ cascade); + edited-saved отсутствует в payload → удалён несмотря на edit; + unknown lexemeId → pending СНЯТ, список не тронут (разрешает конфликт двух строк); full-list equality (G7).

**§1.7 Inserted:** + dedup-branch (Saved(newCvId) уже есть → pristine удалён, размер списка не растёт — S-DUP); + multi (несколько pristine, флипается только matching key); + unknown lexemeId no-op; + pending unchanged.

**§1.8 LexemeDraftPromoted:** + empty-survivor (survivor с `edited==""&&origin==""` → решение: дропать, НЕ реэмитить пустой INSERT); + G8 ветка `>1 NOT_IN_DB` (не только 0); полный 8-field effect.

**§2.3 commitDecision:** + `isEdit=false | "" | "x" → NoOp` (guard precedence — самый вероятный реальный баг: impl проверяет пустоту до guard'а); + симметричный anti-confusion `("","x")→PessimisticRemove ≠ LocalRemove`; + `"  new  " | "" → Update("new")` (trim на pristine-пути); + инвариант «origin всегда pre-trimmed» (док).

**§2.4 commitAndCloseAllEdits (КРИТИЧНО — туннельное зрение на 1 лексему):**
- + **multi-lexeme:** `L(7,[Update#6]), L(8,[Update#9])` → `setOf(Upsert(lexemeId=7,cv=6), Upsert(lexemeId=8,cv=9))` (ловит `firstOrNull`/early-return баг).
- + **anchor gated на NOT_IN_DB:** real лексема с 2 pristine-Update → **ОБА** Upsert (anchor-правило НЕ применяется к real); ловит impl, применяющий anchor-pick ко всем.
- + word-edit NoOp (`edited==value` → нет `UpdateWord`).
- + NOT_IN_DB survivors `edited` сохранён (для корректного реэмита §6.2.1).

**§2.5 closeAllEditModes:** + discarded-saved сохраняет `origin`; + multi-lexeme immutability; + no-op when nothing in edit; **проверить call-site** (если не вызывается — dead code; если cancel/back — назвать). Zombie non-empty pristine — пинить точный resulting `key/origin` (или пересмотреть: cancel дропает ВСЕ uncommitted pristine).

### 9.4 `AvailableComponentTypesFlowHandlerTest` (NEW, runTest + TestScope) — race-critical

1. `runEffect(Load(d1))` БЕЗ предшествующего `subscribe()` → no-op, no Msg, no crash (`scope ?: return`).
2. Happy: `subscribe(scope, send)`; `runEffect(Load(1))`; back `flowOf([tr,ex])`; `advanceUntilIdle()` → `ComponentTypesLoaded([tr,ex])`.
3. Error: `flowAvailableComponentTypes(1)` = `flow { throw IOException() }` (throw в эмиттере, upstream от collectLatest) → `ComponentTypesLoadFailed`.
4. **Resubscribe отменяет старый job:** d1 = долгоживущий `MutableSharedFlow`; `runEffect(Load(1))`→emit A→`Loaded(A)`; `runEffect(Load(2))` (другой flow); emit B на d1 → **НЕ получен** (старый job отменён `unsubscribe()`); emit C на d2 → получен. Единственный тест, доказывающий отмену.
5. `CancellationException` пробрасывается (не глотается catch'ем) — parity с F125-тестом `AllUserDefinedTypesFlowHandler`.

### 9.5 `DatasourceEffectHandlerTest` (two-Msg burst + error)

- **List-collector обязателен:** `val msgs = mutableListOf<Msg>(); handler.onEffect(effect){ msgs += it }` (одиночный `var captured` НЕ выразит burst — второй Msg потеряется).
- INSERT (lexemeId!=null,cvId==null): `msgs.size==2`, `msgs[0] is RefreshLexemeComponents`, `msgs[1] is ComponentValueInserted`, `msgs[1].pristineKey==effect.pristineKey`, `msgs[1].newCvId==result.newComponentValueId` (порядок load-bearing §5.2).
- promote (lexemeId==null) success: `msgs==[LexemeDraftPromoted(result, effect.pristineKey)]` (size 1; anchorPristineKey из effect, не из state).
- update (cvId!=null) success: `msgs==[RefreshLexemeComponents]` (size 1, НЕ 2).
- Remove → ComponentRemoved: `[RefreshLexemeComponents]`; → Cascade: `[LexemeCascadeRemoved]`.
- **3 null→OperationFailed** (по веткам) + **exception-path** (useCase throws → `OperationFailed(generic)`, single Msg, no crash) + **CancellationException пробрасывается** (не мапится в OperationFailed).

### 9.6 §6 anti-regression — НЕ 1:1 (исправить claim)

Реальный `TranslationManagementTest` = 15 тестов. §6 реплицирует 7. Добавить/уточнить:
- **MISSING originals:** #2 `CreateTranslation when exists` (теперь — НЕ guard, а chip-hiding через `addedNonMultiTypeIds`; оформить как **documented deviation**, не parity); #14 `RemoveTranslation real-id → effect+pending` (`translation_remove_saved_emits_RemoveComponentValue`).
- **Split branch-3:** originals #8 (`lexemeId=null` create) и #9 (`lexemeId=7` update) — РАЗНЫЕ эффекты в новой модели (`Upsert(lexemeId=null,pristineKey)` vs `Upsert(lexemeId=7,componentValueId)`); два отдельных кейса, не один «Update».
- **Architecturally replaced (НЕ реплицировать, явно отметить):** original #10 `RefreshTranslation NOT_IN_DB→real promotion` → теперь `LexemeDraftPromoted`/`ComponentValueInserted` (§1.7/1.8); original #12 `Refresh null → nullify` → теперь «saved отсутствует в payload → removed» (§1.6). Убрать claim «ВСЕ 1:1» — заменить на «поведенческий снимок + явные замены/девиации».

### 9.7 §4 UseCase — добавить
- `restoreLexemeWithComponents` с пустым списком (assert never-called или null).
- `flowAvailableComponentTypes` empty → пустой collect; throws → пробрасывается (НЕ глотать `.catch` в UseCase `.map`).
- `addLexemeWithComponent` throws → null (parity T6).
- T14 пинит `coVerify(exactly=0){ deleteComponentValue(any()) }`.
- T18/G3 wording: «две идемпотентные точки trim (эффект-payload + UseCase)»; Image-case (asText null) — trim no-op, отметить непокрытым.

### 9.8 Доп. пользовательские сценарии (аналитика полноты путей)

**Продуктовое решение (зафиксировано):** «Перевод» — ОБЫЧНОЕ значение. Удаляется как любой компонент; **лексема может существовать без перевода** (с другой сущностью). Никакого инварианта «перевод обязателен», никакого спец-кейса для translation при удалении. (Совместимость с quiz, где лексема без перевода — забота quiz, out-of-scope здесь.)

Добавить к §3:

- **S21 (вход: создание лексемы).** `CreateLexeme` → появляется черновик NOT_IN_DB; повторный `CreateLexeme` пока черновик не сохранён → проглочен (`isCreatingLexeme` single-draft guard, `canAddLexeme=false`). Это «вход» в S5/S6, которые начинались с уже-существующего черновика.
- **S22 (передумал без ввода).** `CreateComponentValue(EX)` → пустой pristine; `CreateComponentValue(SYN)` (или tap другого) → пустой первый pristine ИСЧЕЗАЕт (S-CREATE: commitAndCloseAllEdits дропает пустой), остаётся только новый. Пин: после второго create в components ровно 1 pristine (key нового).
- **S23 (удаление значения перевода при наличии других).** Лексема = [Saved(перевод "arg"), Saved(пример "good arg")]. `RemoveComponentValueRequested(Saved перевод)` → `RemoveComponentValue` effect; после `RefreshLexemeComponents([пример])` → лексема ОСТАЁТСЯ с одним «примером», БЕЗ перевода, чип «Перевод» снова доступен. **Пин: лексема не каскад-удалена, перевода нет, пример на месте.** (Прямое подтверждение продуктового решения.)
- **C (undo проигнорирован) — НЕ тестируется на reducer** (нет Msg для «снек истёк»): пометить честно как UI/manual. После dismiss удаление окончательно (restore не вызывается).
- **D (пустое состояние) — manual/UI smoke:** лексема без значений и без доступных типов кроме «Перевод» → виден только чип «Перевод», пустая зона, не падает. Onboarding-плейсхолдер — out-of-scope (бриф).
