---
status: done
---

# Summary — IS479 business sub-flow

Sub-flow `business` для фичи `wordcard_lexeme_inline` (IS479 — карточка слова с inline-механикой создания лексемы).
Корневая директория артефактов: `docs/features/IS479_wordcard_lexeme_inline/business/`.
Spec-output: `docs/features-spec/wordcard.md` (опубликован впервые).

## Что сделано

### Контракты (5 артефактов)

1. **`contract_state.md` v2.5** (6 итераций) — финальный шейп `WordCardState` + sealed `WordState { NotLoaded | Loaded(...) }`. Константа `NOT_IN_DB = -1L` (общая конвенция кодбазы) как маркер локальной лексемы. Поле `isPendingDbOp: Boolean = false` — глобальный UI-блокировщик. Computed `isCreatingLexeme = lexemeList.any { it.id == NOT_IN_DB }`. Helper `closeAllEditModes()`. Mapper `Lexeme.toLexemeState()`. Удалён `AddLexemeBottomState` целиком + связанные extensions.

2. **`contract_ui_msg.md` v3.2** (7 итераций) — финальный `sealed interface Msg`. Удалено: `LoadingWord`, `RefreshLexeme(lexeme)`, `CreateLexemeFailed`, `OpenAddLexemeDialog`/`CloseAddLexemeDialog`, `EnableTranslationCreation`/`EnableDefinitionCreation`, `ExitTranslationEditMode`/`ExitDefinitionEditMode`, internal `sealed interface UiMsg`. Добавлено: `CommitTranslationEdit/CancelTranslationEdit/CommitDefinitionEdit/CancelDefinitionEdit`, `OpenLexemeMenu(lexemeId, isShow)`, `DismissNotification`, `NavigateBack` shared с handler. Reducer на pattern matching `when (state.wordState)` — все UI Msg на `NotLoaded` → `state to emptySet()`.

3. **`contract_io.md` v7** (7 итераций) — `sealed DatasourceEffect` (8 вариантов): `LoadWord`, `UpdateWord`, `RemoveWord`, `RemoveLexeme(wordId, lexemeId)`, `UpdateLexemeTranslation(wordId, lexemeId: Long?, value)`, `UpdateLexemeDefinition`, `RemoveTranslation(lexemeId)`, `RemoveDefinition`. Datasource Msg (9): `WordLoaded(Term)`, `WordNotFound`, `RefreshWord`, `RefreshTranslation(lexemeId, String?)`, `RefreshDefinition`, `RefreshLexemeList`, `LexemeCascadeRemoved`, `ShowNotification`, shared `NavigateBack`. UX-инвариант ит.7: каждый control-path handler'а ОБЯЗАН вернуть Msg сбрасывающий `isPendingDbOp = false`. Concurrent-Commit race (F062/F063/F066) сняты архитектурно через UI-блокировку.

4. **`contract_usecase.md` v1** (1 итерация) — финальный `WordCardUseCase` interface (8 методов):
   ```kotlin
   suspend fun getTermById(wordId: Long): Term?
   suspend fun deleteWord(wordId: Long): Int
   suspend fun updateWord(wordId: Long, value: String): Boolean
   suspend fun deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>?
   suspend fun addLexemeTranslation(wordId: Long, lexemeId: Long?, translation: String): Lexeme?
   suspend fun deleteLexemeTranslation(lexemeId: Long): RemoveTranslationResult?
   suspend fun addLexemeDefinition(wordId: Long, lexemeId: Long?, definition: String): Lexeme?
   suspend fun deleteLexemeDefinition(lexemeId: Long): RemoveDefinitionResult?
   ```
   Sealed `RemoveTranslationResult { TranslationRemoved(Lexeme) | LexemeCascadeRemoved }`, симметрично `RemoveDefinitionResult`. `addLexeme(wordId): Lexeme?` удалён из public API (internal helper). Atomicity contract: при `lexemeId == null` impl делает atomic insert лексемы + insert суб-сущности в одной транзакции — через **existing** `lexemeApi.addLexeme(wordId, TranslationApiEntity)` / `addLexeme(wordId, DefinitionApiEntity)` перегрузки.

5. **`contract_spec.md` v1** (1 итерация) — финальный черновик проектной спеки для публикации.

### Дизайн и тесты

6. **`design_tree.md` v2** (2 итерации: 1 + conductor patch F076) — DAG из 27 узлов (8 production + 19 тестов). Узлы #0-#7 production. Conductor patch F076: «compile-break принят как intermediate state — UI sub-flow восстановит компиляцию». Conductor patch F077: atomicity через existing API, никакого `@Transaction`/`AppDatabase` расширения не нужно.

7. **`test.md` v1** (1 итерация) — 13 тестовых файлов изменено, 2 удалено (`LoadingWordTest`, `AddLexemeExtTest`). Покрытие 16/18 сценариев из `contract_spec § Тестовые сценарии`: 12 reducer (все) + 4 handler (3 canon + 1 ветвление). 2 UseCase contract сценария (16/17/18 — atomicity/cascade/non-cascade) отложены до integration test.

8. **`impl.md` ит.2** (2 итерации: основная имплементация + review fixes) — **7 production-файлов изменено, 1 удалён**:
   - `~ deps/WordCardUseCase.kt` — interface + sealed results.
   - `~ mate/State.kt` — sealed `WordState`, `isPendingDbOp`, `closeAllEditModes`, mapper; удалён `AddLexemeBottomState`.
   - `~ mate/Message.kt` — новый набор Msg per `contract_ui_msg` v3.2.
   - `~ mate/DatasourceEffectHandler.kt` — `DatasourceEffect` sealed, try/catch, sealed result branching. ит.2: `catch (e: Exception)` + `LexemeLogger` injection.
   - `~ mate/WordCardReducer.kt` — sealed `WordState` pattern matching, `isPendingDbOp` guard, 4 ветки `Commit*Edit`, `RefreshTranslation/Definition` с replacement NOT_IN_DB→real id, `RefreshLexemeList` сохраняет локальную NOT_IN_DB. ит.2: добавлены guards `!isEdit ⇒ ignore` для `Commit*Edit/Cancel*Edit`, `OpenLexemeMenu(isShow=true)` закрывает `isMenuOpen` у других; инверсия default `isGuardedByPending`.
   - `~ WordCardViewModel.kt` — снят `uiHandler` dep.
   - `~ app/.../WordCardUseCaseImpl.kt` — atomic INSERT через existing `lexemeApi.addLexeme(wordId, TranslationApiEntity)`; sealed result returns; private helpers `insertLexemeWith*`. ит.2: `LexemeLogger` injection, `catch (e: Exception)` + log.
   - `- mate/UiEffectHandler.kt` — удалён (UI Effects = ∅).

   **Compile status:** `:modules:screen:wordcard` НЕ компилируется — compile-break в UI-call-sites (5 файлов: `WordCardScreen.kt`, `widget/ConfirmDeleteWordWidget.kt`, `widget/WordFieldWidget.kt`, `widget/addlexeme/AddLexemeBottomWidget.kt`, `widget/lexeme/LexemeItemWidget.kt`). Accepted intermediate per F076.
   **Test status:** транзитивно заблокирован compile-break. Выполнение после UI sub-flow.

9. **`publish_spec.md` v1** (1 итерация) — опубликован **новый файл** `docs/features-spec/wordcard.md` (~442 строки). Обновлён `docs/features-spec/README.md`: добавлена ссылка. PUML-шаг пропущен — `.puml` файлов в проекте нет. Спека опубликована as-is относительно `contract_spec.md` v1: ит.2 impl-патчи либо уже декларированы в спеке (guards, F028), либо детали реализации (catch-type, logger, inverted predicate).

## Ключевые решения

1. **Лексема создаётся в БД при первом Commit Translation/Definition, не при тапе FAB.** Локально `LexemeState(id = NOT_IN_DB, translation=null, definition=null)`. Первый Commit → effect `UpdateLexeme*(wordId, lexemeId=null, value)` → atomic INSERT → handler шлёт `RefreshTranslation(realId, value)` → reducer заменяет id. Обоснование: соответствует Figma `9154-82519`, переиспользует существующий mechanism через `lexemeApi.addLexeme(wordId, ...ApiEntity)` перегрузки, убирает целую ось состояния (`AddLexemeBottomState`).

2. **Cascade-aware sealed result для Remove\*.** `RemoveTranslationResult { TranslationRemoved(lexeme) | LexemeCascadeRemoved }` делает явным существующий cascade (`canRemoveTranslation()` логика в impl). Handler маппит в `RefreshTranslation(lexemeId, null)` или `LexemeCascadeRemoved(lexemeId)` соответственно.

3. **Atomicity через existing `lexemeApi.addLexeme(wordId, TranslationApiEntity/DefinitionApiEntity)` перегрузки.** Не расширяем data-API, не вводим `@Transaction`/`AppDatabase` injection.

4. **Глобальный `isPendingDbOp` UI-блокировка вместо локальных guard'ов в reducer.** Поле в state, set true в Effect-sending Msg ветках, reset false в любом разблокирующем Msg. Глобальный guard `Msg.isGuardedByPending()` в reducer + UX-инвариант handler'а («каждый control-path возвращает разблокирующий Msg»). Снимает F062/F063/F066 архитектурно.

5. **Compile-break принят как intermediate state.** Business sub-flow не вводит compile-shim для UI-call-sites. UI sub-flow восстанавливает компиляцию следующим шагом.

## Артефакты

- `business/contract_state.md` v2.5
- `business/contract_ui_msg.md` v3.2
- `business/contract_io.md` v7
- `business/contract_usecase.md` v1
- `business/contract_spec.md` v1
- `business/design_tree.md` v2
- `business/test.md`
- `business/impl.md` (ит.1 + ит.2 review fixes)
- `business/publish_spec.md`
- `docs/features-spec/wordcard.md` (опубликованная спека — новый файл, ~442 строки)
- `docs/features-spec/README.md` (обновлён)

Review-артефакты для аудита: `contract_state_review.md`, `contract_ui_msg_review.md`, `contract_io_review.md`, `design_tree_review.md`, `test_review.md`, `publish_spec_review.md`.

## Tech debt / backlog

1. **`quizApi.addWriteQuiz` не в транзакции с `lexemeApi.addLexeme`.** Между двумя suspend-вызовами теоретическая возможность отказа: лексема создана, write-quiz не создан. Требует расширения data-api — отдельный тикет.

2. **F074 (data-loss `NOT_IN_DB`-буфера при `RemoveLexeme` через menu).** Локальное удаление без подтверждения теряет typed text. Требует ConfirmDialog UX-логики — отдельный тикет / UI sub-flow.

3. **F054 (undefined ordering Room `@Relation`).** `lexemeList` Room возвращает в порядке физических строк, без `ORDER BY`. Требует изменения DAO — отдельный тикет на data-слой.

4. **F049 (diagnostic-бедность snackbar).** `ShowNotification(text)` несёт только локализованный текст без structured reason. Требует sealed error-result + UX-обогащение — отдельная фича.

5. **Duplication Translation/Definition reducer-веток (10 пар).** Lens-pattern + rename Msg в `UpsertLexeme*(kind: SubentityKind, ...)` — большой архитектурный refactor, отдельный тикет.

## Метрика итераций

| Шаг               | Итераций | Заметка |
|-------------------|----------|---------|
| `contract_state`  | 6        | Pivot на NOT_IN_DB архитектуру (ит.4-5). |
| `contract_ui_msg` | 7        | max-iteration лимит. |
| `contract_io`     | 7        | max-iteration лимит; финальный мини-патч ит.7 — UX-инвариант handler'а. |
| `contract_usecase`| 1        | Сборка из `contract_io` v7. |
| `contract_spec`   | 1        | Финальный черновик. |
| `design_tree`     | 2        | 1 + conductor patch F076. |
| `test`            | 1        | С tech debt (16/18). |
| `implement`       | 2        | 1 + review fixes. |
| `publish_spec`    | 1        | Опубликовано as-is. |

Итого: **9 шагов, 27 итераций совокупно**.

Вне scope для master flow:
- **UI sub-flow** — обязателен следующим шагом, восстанавливает компиляцию `:modules:screen:wordcard`.
- **Final check** sub-flow — `compileDebugKotlin / testDebugUnitTest / assembleDebug / lintDebug` + integration acceptance UseCase-сценариев 16/17/18.
- 5 backlog items (см. § Tech debt).
