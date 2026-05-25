# IS479 — Business sub-flow: test

## Решение

Тесты НУЖНЫ — меняется публичное поведение бизнес-логики: sealed `WordState`, новый глобальный
`isPendingDbOp` guard, переработанные ветки `Commit*Edit` (1a/1/2/3), replacement `NOT_IN_DB → реальный id`
через `Refresh*`, локальная/серверная cascade, sealed `RemoveTranslationResult`/`RemoveDefinitionResult`,
переименованный набор `Msg` (`CommitTranslationEdit`/`CancelTranslationEdit`, `RefreshLexemeList`,
`LexemeCascadeRemoved`, `ShowNotification(text)` без `show`, `DismissNotification`).

TDD: тесты зафиксированы под новый контракт **до** реализации production-кода. На текущий момент они
не компилируются (Msg `CommitTranslationEdit`, `RefreshLexemeList`, sealed `WordState.Loaded`, новые
`DatasourceEffect`-варианты и т.д. ещё не существуют в коде) — это ожидаемое поведение шага `test` в
сценарии TDD.

## Файлы

### Удалены ([-])

- `modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/LoadingWordTest.kt` — `Msg.LoadingWord`
  удалён, loading инициирует только ViewModel через `initEffects`.
- `modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/ext/AddLexemeExtTest.kt` —
  `AddLexemeBottomState` и связанные extensions удалены (старый bottom sheet исключён из IS479).

### Перезаписаны ([~]) — reducer snapshot

- `WordLoadedTest.kt` — сценарии 1 (атомарность WordLoaded), 2 (empty lexeme list), 3 (multiple),
  4 (WordNotFound silent exit + NavigationEffect.Back, snackbar не ставится).
- `LexemeManagementTest.kt` — CreateLexeme локально (без Effect), guard повторного создания NOT_IN_DB,
  RemoveLexeme real id (Effect + pending), RemoveLexeme NOT_IN_DB (локально), глобальный guard
  isPendingDbOp на RemoveLexeme, `RefreshLexemeList` ветка, `LexemeCascadeRemoved` ветка,
  `OpenLexemeMenu` базовый.
- `TranslationManagementTest.kt` — 4 ветки `CommitTranslationEdit` (1a empty-empty, 1 pessimistic remove,
  2 no-op, 3 first-Commit NOT_IN_DB и Update real id), `RefreshTranslation` NOT_IN_DB-replacement,
  `RefreshTranslation` preserves active edit (F073), `RefreshTranslation(null)` nullify,
  локальная cascade на `RemoveTranslation(NOT_IN_DB)`, real-id RemoveTranslation Effect,
  глобальный guard isPendingDbOp, `CancelTranslationEdit` (cascade + reset).
- `DefinitionManagementTest.kt` — зеркало `TranslationManagementTest`. Все ветки `CommitDefinitionEdit`,
  `RefreshDefinition` replacement/preserve/nullify, cascade, real-id RemoveDefinition.
- `WordEditTest.kt` — sealed `WordState.Loaded`, EnterWordEditMode guard на `NotLoaded`,
  `closeAllEditModes` cascade (chip → reset при EnterWordEditMode), CommitWordChanges blank guard (инв. 10),
  CommitWordChanges valid → Effect + pending, CommitWordChanges под isPendingDbOp = no-op, RefreshWord.
- `DeleteWordDialogTest.kt` — guards на `NotLoaded` для `OpenDeleteWordDialog`/`RemoveWord`,
  RemoveWord id-mismatch guard, RemoveWord success → Effect + pending, isPendingDbOp guard.
- `NavigateBackTest.kt` — emits `NavigationEffect.Back`, clears isPendingDbOp (shared handler-confirm
  path после RemoveWord), works under NotLoaded.
- `NoOperationTest.kt` — no-op identity под NotLoaded и Loaded.
- `ShowNotificationTest.kt` — `Msg.ShowNotification(text)` (без `show`) → snackbar + clears pending,
  `Msg.DismissNotification` hides shown snackbar, guard `!show` на DismissNotification.
- `OpenTopBarMenuTest.kt` — простой toggle.
- `CloseTopBarMenuTest.kt` — toggle + guard `!isMenuOpen`.

### Перезаписаны ([~]) — ext

- `ext/WordExtTest.kt` — выжившие extensions под `WordState.Loaded`: `enableWordEdit`, `disableWordEdit`,
  `updateWordEdited`, `showWordWarningDialog`, `hideWordWarningDialog`, плюс guard на NotLoaded.
  Удалённые `setWordId`/`setWordAdded`/`setWordValue`/`setTerm` больше не тестируются (мэппинг term
  делается в reducer-ветке WordLoaded).
- `ext/LexemeExtTest.kt` — generic helpers: `setLexemeList`, `addLexeme`, `updateLexeme`, `removeLexeme`,
  `toggleLexemeMenu`.
- `ext/LoadingExtTest.kt` — `showLoading`/`hideLoading` + default state (`isLoading = true`,
  `isPendingDbOp = false`).
- `ext/SnackbarExtTest.kt` — `showSnackbar`/`hideSnackbar`.
- `ext/TopBarExtTest.kt` — `showMenu`/`hideMenu`.
- `ext/SpecializedLexemeExtTest.kt` — `setLexemeMenuOpen`, `createLexemeTranslation`,
  `createLexemeDefinition`, `updateLexemeTranslationText`, `updateLexemeDefinitionText`,
  `enableLexemeTranslationEdit`, `enableLexemeDefinitionEdit`, **новый** `closeAllEditModes`
  (сбрасывает Word.isEditMode/edited + все chip isEdit/edited; на NotLoaded не падает).

### Созданы ([+])

- `modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/DatasourceEffectHandlerTest.kt` —
  3 сценария handler interaction из contract_spec плюс расширенные ветки:
  - `LoadWord exception → WordNotFound` (silent exit) + null-результат → `WordNotFound` + success →
    `WordLoaded(term)`.
  - `UpdateLexemeTranslation` first-Commit success → `RefreshTranslation(realId, value)`, failure
    (exception) → `ShowNotification("Не удалось сохранить перевод")`, null-результат → `ShowNotification`.
  - `RemoveTranslation` cascade → `LexemeCascadeRemoved(lexemeId)`, non-cascade
    (`TranslationRemoved(lexeme с translation=null)`) → `RefreshTranslation(lexemeId, null)`,
    null-результат → `ShowNotification`.
  - `RemoveWord` success → `NavigateBack`, failure (0) → `ShowNotification`, exception → `ShowNotification`.

  Используется in-test `FakeUseCase` (без mock-фреймворка — модуль не подключает Mockito/MockK).

## Не покрыто

- UseCase contract (контракт-сценарии 16/17/18) — реализация в `app/src/main/.../WordCardUseCaseImpl.kt`
  завязана на Room/Prefs/quizApi. Полноценный тест требует либо MockK/Mockito (не подключены в
  `:modules:screen:wordcard`), либо подключения этих библиотек к `:app:test`. Решение: оставлены как
  acceptance-сценарии для шага `implement` — atomicity (`addLexemeTranslation(lexemeId = null)` через
  одну `lexemeApi.addLexeme(wordId, TranslationApiEntity(...))` перегрузку — атомарно по построению),
  cascade (`deleteLexemeTranslation` возвращает `LexemeCascadeRemoved` если `canRemoveTranslation()==false`),
  non-cascade (`TranslationRemoved(lexeme.copy(translation=null))`). Проверка на этапе integration
  /manual или после `implement`.

- Тесты `closeAllEditModes` в reducer-сценариях покрыты косвенно через `EnterWordEditMode` (chip-сброс)
  и `EnterTranslationEditMode` (word-сброс + закрытие конкурирующих chip-edit). Прямая
  reducer-ветка для `CreateLexeme` + `closeAllEditModes` — покрыта `LexemeManagementTest` (поведение
  `CreateLexeme` локально, без явной проверки `closeAllEditModes` поверх активного edit — out of scope
  спецификации, но защищено `EnterTranslationEditMode`-тестом и `closeAllEditModes` ext-тестом).

## Лог итераций

### ит.1 (2026-05-21T17:01:31-0600)

Финальная фиксация тестов под IS479 business contract.

**Сводка покрытия (по contract_spec § "Тестовые сценарии"):**

| # | Scenario | Файл | Тест |
|---|----------|------|------|
| 1 | WordLoaded атомарность | `WordLoadedTest` | `given NotLoaded with isLoading when WordLoaded ...` |
| 2 | WordNotFound silent exit | `WordLoadedTest` | `given NotLoaded when WordNotFound ...` |
| 3 | CreateLexeme локально | `LexemeManagementTest` | `given Loaded when CreateLexeme ...` |
| 4 | CreateLexeme guard | `LexemeManagementTest` | `given existing NOT_IN_DB lexeme when CreateLexeme ...` |
| 5 | CommitTranslationEdit ветвь 3 first-Commit | `TranslationManagementTest` | `branch 3 first-Commit on NOT_IN_DB ...` |
| 6 | CommitTranslationEdit ветвь 1a empty-empty | `TranslationManagementTest` | `branch 1a empty-empty ...` |
| 7 | CommitTranslationEdit ветвь 1 pessimistic Remove | `TranslationManagementTest` | `branch 1 pessimistic remove ...` |
| 8 | CommitTranslationEdit ветвь 2 no-op | `TranslationManagementTest` | `branch 2 no-op ...` |
| 9 | Локальная cascade | `TranslationManagementTest` | `RemoveTranslation for NOT_IN_DB with no definition cascade removes lexeme` |
| 10 | Серверная cascade | `LexemeManagementTest` | `when LexemeCascadeRemoved then removes lexeme ...` |
| 11 | RefreshTranslation сохраняет активный edit (F073) | `TranslationManagementTest` | `RefreshTranslation preserves active edit ...` |
| 12 | isPendingDbOp guard | `TranslationManagementTest`, `WordEditTest`, `DeleteWordDialogTest`, `LexemeManagementTest` | `given isPendingDbOp true ... is no-op` |
| 13 | Handler UpdateLexemeTranslation first-Commit success | `DatasourceEffectHandlerTest` | `... success yields RefreshTranslation with real id` |
| 14 | Handler UpdateLexemeTranslation failure | `DatasourceEffectHandlerTest` | `... failure (exception) yields ShowNotification ...` |
| 15 | Handler RemoveTranslation cascade-path | `DatasourceEffectHandlerTest` | `RemoveTranslation cascade-path yields LexemeCascadeRemoved` |
| 16-18 | UseCase contract | (не покрыто unit-тестом, см. § "Не покрыто") | acceptance в `implement` |

Дополнительные ветки сверх contract_spec, покрытые для регрессии:
- `RefreshWord`, `RefreshLexemeList`, `DismissNotification` + guard, `CommitWordChanges` blank guard
  (инв. 10), guards на `NotLoaded` для всех word-Msg, `RemoveTranslation` non-cascade-path в handler,
  null-результат UseCase → `ShowNotification`.

**Файлы изменены:** 13 (12 переписано, 1 создан).
**Файлы удалены:** 2.
**Reducer-сценариев покрыто:** 12 из 12.
**Handler-сценариев покрыто:** 3 из 3 + 5 дополнительных ветвлений.
**UseCase-сценариев покрыто:** 0 из 3 (требуют DI mock / acceptance после `implement`).

---

_model: claude opus 4.7 (1M context)_
