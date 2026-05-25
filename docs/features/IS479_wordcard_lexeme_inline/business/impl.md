# IS479 — Business sub-flow implementation report

## Изменённые файлы

### Production (~) — 7 файлов

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/deps/WordCardUseCase.kt` — новый interface (8 методов), sealed `RemoveTranslationResult` / `RemoveDefinitionResult`.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/State.kt` — sealed `WordState { NotLoaded | Loaded(...) }`, поле `isPendingDbOp`, computed `isCreatingLexeme`, helper `closeAllEditModes()`, mapper `Lexeme.toLexemeState()`. Удалён `AddLexemeBottomState` и связанные extensions (`showAddLexemeBottom`, `hideAddLexemeBottom`, `setTranslationCheck`, `setDefinitionCheck`, `setWordId`, `setWordAdded`, `setWordValue`, `setTerm`). Word-extensions (`enableWordEdit`, `disableWordEdit`, `updateWordEdited`, `showWordWarningDialog`, `hideWordWarningDialog`) переписаны под sealed `WordState.Loaded` (guard: no-op на `NotLoaded`). `TextValueState.isEdit` дефолт → `false`. `LexemeState` дополнен `isMenuOpen` в default-аргументы (без structural изменений).
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/Message.kt` — обновлённый `sealed interface Msg` (per contract_ui_msg v3.2). Удалён `LoadingWord`, `RefreshLexeme(Lexeme)`, `OpenAddLexemeDialog`, `CloseAddLexemeDialog`, `EnableTranslationCreation`, `EnableDefinitionCreation`, `ExitTranslationEditMode`, `ExitDefinitionEditMode`, `internal sealed interface UiMsg`. Добавлены `CommitTranslationEdit`, `CancelTranslationEdit`, `CommitDefinitionEdit`, `CancelDefinitionEdit`, `RefreshWord(Term)`, `RefreshTranslation(lexemeId, translation?)`, `RefreshDefinition(lexemeId, definition?)`, `RefreshLexemeList(List<Lexeme>)`, `LexemeCascadeRemoved(lexemeId)`, `DismissNotification`, `ShowNotification(text)` (теперь не `UiMsg`).
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/DatasourceEffectHandler.kt` — sealed `DatasourceEffect` с новыми вариантами (`RemoveLexeme(wordId, lexemeId)`, `UpdateLexemeTranslation.lexemeId: Long?`, `UpdateLexemeDefinition.lexemeId: Long?`). Удалён `CreateLexeme`. Handler с try/catch по всему `when` — каждый control-path возвращает разблокирующий Msg. Sealed-result branching в `RemoveTranslation/RemoveDefinition`.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/WordCardReducer.kt` — переписан с глобальным `isPendingDbOp` guard через helper `Msg.isGuardedByPending()`. Sealed `WordState` pattern matching через `when`. Ветки `CommitTranslationEdit` / `CommitDefinitionEdit` — 4 ветки (1a локальный nullify, 1 pessimistic remove, 2 no-op, 3 update/first-Commit). `RefreshTranslation` / `RefreshDefinition` — replace для real id, замена NOT_IN_DB→real для notInDb. `RefreshLexemeList` сохраняет локальную NOT_IN_DB лексему. `closeAllEditModes()` для Enter*/Open*/Create* веток. Удалены ветки: `LoadingWord`, `RefreshLexeme`, `OpenAddLexemeDialog`, `CloseAddLexemeDialog`, `EnableTranslationCreation`, `EnableDefinitionCreation`, `ExitTranslationEditMode`, `ExitDefinitionEditMode`, `UiMsg.ShowNotification`. `WordNotFound` теперь silent exit (`NavigationEffect.Back`, не `TODO`).
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/WordCardViewModel.kt` — удалён `uiHandler: UiEffectHandler` параметр и из `effectHandlerSet`. Импорт `UiEffectHandler` снят.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt` — переписан под новый interface. `deleteLexeme(wordId, lexemeId): List<Lexeme>?` (idempotent, resync через `termApi.getTermById(wordId).lexemes`). `addLexemeTranslation/Definition(lexemeId = null, ...)` — atomic через **existing** `lexemeApi.addLexeme(wordId, TranslationApiEntity(...))` / `addLexeme(wordId, DefinitionApiEntity(...))` (один INSERT). `deleteLexemeTranslation/Definition` возвращают sealed result. Все методы записи обёрнуты в `try { ... } catch (_: Throwable) { null }`. Private helpers `insertLexemeWithTranslation` / `insertLexemeWithDefinition` инкапсулируют atomic INSERT + addWriteQuiz.

### Удалённый файл (-) — 1

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/UiEffectHandler.kt` — мёртвый код per contract_io v7 («UI Effects отсутствуют»).

### Тесты — без изменений (написаны на шаге `test`)

Все тестовые файлы #8–#26 уже соответствуют новому контракту (написаны на шаге `test` ит.1). Файлы `LoadingWordTest.kt` (#8) и `AddLexemeExtTest.kt` (#20) уже отсутствуют — удалены ранее.

## Нетривиальные решения

1. **`OpenLexemeMenu(isShow=true)` без `closeAllEditModes` на toggle-off.** Контракт говорит «`closeAllEditModes()` для Enter*/Open*/Create*», но `OpenLexemeMenu(isShow=false)` фактически закрывает меню — это локальная toggle-off, не вход в edit. Применён `closeAllEditModes()` только при `isShow=true`.

2. **`RefreshTranslation/RefreshDefinition` — конструирование `TextValueState` если был `null`.** Pseudocode в design_tree предусматривает `l.translation?.copy(origin = translation) ?: TextValueState(origin = translation, ...)`. Это сделано явно в реализации (для случая когда было `translation = null` и пришёл новый origin после первого commit).

3. **`RefreshLexemeList` сохранение NOT_IN_DB.** Pseudocode `mapped + listOfNotNull(keepLocal)` — если в `mapped` уже есть лексема с id `NOT_IN_DB` (теоретически невозможно, но защита), `keepLocal` не дублируется.

4. **`WordExtTest.kt` ожидает no-op на `NotLoaded` для всех Word-extensions.** Реализовано через `wordState as? WordState.Loaded ?: return this` в каждом extension.

5. **`canRemoveTranslation` / `canRemoveDefinition` — boolean predicates на `LexemeApiEntity`.** Возвращают true если другая суб-сущность ≠ null. При false (нельзя удалить translation/definition) `deleteLexemeTranslation/Definition` каскадно удаляет всю лексему — `LexemeCascadeRemoved`. Это уже было в старой реализации.

## Compile status

`./gradlew compileDebugKotlin` на `:modules:screen:wordcard` — **FAILED** в UI-файлах (`WordCardScreen.kt`, `widget/ConfirmDeleteWordWidget.kt`, `widget/WordFieldWidget.kt`, `widget/addlexeme/AddLexemeBottomWidget.kt`, `widget/lexeme/LexemeItemWidget.kt`), что является **ожидаемым промежуточным состоянием** per design_tree:

> "Compile-break принят как intermediate state (F076 ит.1): UI sub-flow стартует сразу после business и восстанавливает компиляцию под inline-механику (Figma). Business sub-flow не вводит compile-shim для UI-call-site."

Список зафиксированных compile-ошибок (выборка):

```
WordCardScreen.kt:41   Unresolved reference 'UiMsg'.
WordCardScreen.kt:81   Unresolved reference 'UiMsg'.
WordCardScreen.kt:99   Unresolved reference 'OpenAddLexemeDialog'.
WordCardScreen.kt:146  Unresolved reference 'showWarningDialog'.   (state.wordState.showWarningDialog — теперь только в Loaded)
WordCardScreen.kt:152  Unresolved reference 'addLexemeBottomState'.
WordCardScreen.kt:155  Unresolved reference 'CloseAddLexemeDialog'.
WordCardScreen.kt:168  Interface 'WordState' does not have constructors.   (sealed теперь)
WordCardScreen.kt:184  Interface 'WordState' does not have constructors.
ConfirmDeleteWordWidget.kt:28   Unresolved reference 'id'.       (state.wordState.id — нужен smart cast на Loaded)
ConfirmDeleteWordWidget.kt:55   Interface 'WordState' does not have constructors.
WordFieldWidget.kt:53-65        Unresolved 'value','edited','isEditMode','added'.   (нужен smart cast)
WordFieldWidget.kt:111          Interface 'WordState' does not have constructors.
AddLexemeBottomWidget.kt:29,37,63,64,67,68,70,85   Unresolved 'AddLexemeBottomState', 'isTranslationCheck', 'EnableTranslationCreation', etc.
LexemeItemWidget.kt:62  Unresolved reference 'ExitTranslationEditMode'.
LexemeItemWidget.kt:89  Unresolved reference 'ExitDefinitionEditMode'.
```

Все эти ошибки — в UI-call-sites, которые **out-of-scope** business sub-flow.

## testDebugUnitTest

`./gradlew :modules:screen:wordcard:testDebugUnitTest` — **не запускается**, т.к. `compileDebugKotlin` падает на UI-файлах (выше). Test sourceset транзитивно блокирован.

Это **ожидаемое поведение** business sub-flow: тесты валидны против нового контракта, но физически выполнены могут быть только после UI sub-flow (когда модуль скомпилируется целиком).

**Альтернатива не реализована** (compile-shim для UI): прямой запрет в design_tree («Business sub-flow не вводит compile-shim для UI-call-site»). Выполнение валидируется на следующем шаге pipeline (`check`) после UI sub-flow.

## Sanity check на сами production-файлы

Прочитав все 7 production-файлов после записи, я подтверждаю их синтаксическую корректность по визуальному осмотру. В частности:

- Mapper `Lexeme.toLexemeState()` корректно ставит `edited = ""` (согласно тесту `WordLoadedTest.kt` — `assertEquals("translation edited empty (per mapper)", "", lex.translation?.edited)`).
- `TextValueState` default `isEdit = false` — корректно для маппера из БД и для других дефолтных конструкций; тесты `EnterTranslationEditMode` явно создают `isEdit = true` где нужно.
- В reducer глобальный guard `isPendingDbOp` корректно пропускает Cancel*Edit / Update*Input / Close*Dialog / ExitWordEditMode / Datasource Msg и всё, что в `isGuardedByPending()` → `false`.

## Лог итераций

### ит.1 (2026-05-21T17:30:00-0600)

7 production-файлов перезаписано (включая один app-модуль), 1 файл удалён (`UiEffectHandler.kt`). Тесты не тронуты.

**Соответствие design_tree:**
- [#0] WordCardUseCase.kt — interface обновлён, sealed result-types добавлены ✓
- [#1] State.kt — sealed WordState + isPendingDbOp + isCreatingLexeme + closeAllEditModes + mapper ✓ (AddLexemeBottomState удалён, Word-extensions guarded by Loaded)
- [#2] Message.kt — новый Msg per contract_ui_msg v3.2 ✓ (UiMsg удалён, Refresh* плоские, Cancel*Edit добавлены)
- [#3] DatasourceEffectHandler.kt — Effect-варианты обновлены, handler с try/catch и sealed-result branching ✓
- [#4] WordCardReducer.kt — глобальный guard + sealed WordState + commitTranslationEdit/Definition 4 ветки + refreshTranslation/Definition + RefreshLexemeList preserves NOT_IN_DB + LexemeCascadeRemoved ✓
- [#5] UiEffectHandler.kt — удалён ✓
- [#6] WordCardViewModel.kt — uiHandler dep снят ✓
- [#7] WordCardUseCaseImpl.kt — atomic insert через existing `lexemeApi.addLexeme(wordId, Translation/DefinitionApiEntity)`, sealed result returns, deleteLexeme(wordId, lexemeId) idempotent ✓

**Test status:** запуск `testDebugUnitTest` блокирован compile-break в UI-call-sites (ожидаемо per design_tree F076).
**Compile status:** `compileDebugKotlin` падает на 25+ ошибках в `WordCardScreen.kt` / `widget/*` — ожидаемо, UI sub-flow на следующем шаге восстановит компиляцию.

### ит.2 (2026-05-19T23:25:00-0600) — мини-патч review fixes

- Reducer: добавлены guards `!isEdit ⇒ ignore` для Commit*Edit/Cancel*Edit (architect F-arch-1, F-arch-2).
- Reducer: OpenLexemeMenu(isShow=true) закрывает isMenuOpen у других лексем (architect F-arch-3).
- Handler/UseCaseImpl: catch (e: Throwable) → catch (e: Exception) + логирование (senior critical).
- isGuardedByPending: инверсия default (architect/senior).

Tech debt (backlog):
- quizApi.addWriteQuiz не в транзакции с addLexeme — требует расширения data-api (out of scope).
- Translation/Definition duplication — refactor отдельно.
- Effect naming Update vs Upsert — accepted.

---

_model: claude opus 4.7 (1M context)_
