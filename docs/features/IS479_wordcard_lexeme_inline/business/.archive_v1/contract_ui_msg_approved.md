# Approved findings — contract_ui_msg, итерация 2

Все 18 findings approved (3 уникальных critical, 12 уникальных minor; есть дубликаты от разных ревьюверов). Закрой все.

## Критические (3 уникальных)

### F017 / F024 — Guard LoadingWord ломает первую загрузку

Текущий guard: `if (state.isLoading || state.wordState.id != NOT_IN_DB) state to emptySet() else ...`

Дефолт `isLoading: Boolean = true` в contract_state ⇒ первая эмиссия LoadingWord shortcut-проигнорируется, LoadWord effect не уйдёт, слово не загрузится.

**Что сделать:** Инвертируй guard на правильную семантику.
- Вариант A: guard `if (!state.isLoading || state.wordState.id != NOT_IN_DB) state to emptySet() else ...` — «загружаем только когда isLoading=true и id ещё NOT_IN_DB». Но это требует чтобы кто-то ДО LoadingWord выставил isLoading=true. Если LoadingWord и есть первая команда — она сама должна установить.
- Вариант B (рекомендую): уточни семантику — `LoadingWord(wordId: Long)` payload, reducer в guard проверяет «уже загружено или уже грузится»: `if (state.wordState.id != NOT_IN_DB && state.wordState.id == wordId) state to emptySet() else { state.copy(wordState = state.wordState.copy(id = wordId), isLoading = true) to LoadWord(wordId) }`.
- Или: оставь дефолт `isLoading = true` (первая эмиссия в reducer — это переход в loading), но **верни** правильный guard: «уже загружено» = `wordState.id != NOT_IN_DB && wordState.value.isNotEmpty()` (после WordLoaded).

Выбери одну согласованную модель и зафиксируй явно в Reducer-логике + триггере (Lifecycle / LaunchedEffect + WordId payload).

### F018 / F028 — UpdateWordInput без guard

UpdateTranslationInput / UpdateDefinitionInput имеют guard (F013). UpdateWordInput — нет. UpdateWordInput при `wordState.isEditMode == false` запишет `edited != ""` и нарушит инвариант 2.

**Что сделать:** Guard `if (!state.wordState.isEditMode) state to emptySet() else state.copy(wordState = state.wordState.copy(edited = msg.value)) to emptySet()`.

### F025 — Remove*-Msg не сбрасывают isMenuOpen

`RemoveLexeme(lexemeId)`, `RemoveTranslation(lexemeId)`, `RemoveDefinition(lexemeId)` — все триггерятся из меню лексемы, на момент Msg `LexemeState.isMenuOpen == true`. State в reducer-шаге не меняется → меню остаётся открытым до возврата refresh из БД.

**Что сделать:** Симметрично F005 (RemoveWord закрывает меню/диалог): в state changes этих трёх Msg добавь сброс `lexemeList[i].isMenuOpen = false` для лексемы с id == lexemeId.

## Minor (12 уникальных)

### F019 / F031 — NavigateBack rationale самопротиворечив

Текущее обоснование совмещает несовместимые модели ViewModel-lifetime. **Что сделать:** Зафиксируй ОДНУ модель:
- Модель A: ViewModel уничтожается с экраном (типичный Compose Nav). Тогда сброс `isCreatingLexeme` в reducer — избыточен (state умирает). Удали сброс.
- Модель B: ViewModel переживает (back-stack, process restore). Тогда нужно сбрасывать все «грязные» поля (isCreatingLexeme, изменения isEditMode, открытые меню). Расширь reducer.

Рекомендую A (типичный паттерн).

### F020 — Scope-leak RefreshLexeme

Раздел «Изменяются» описывает reducer-логику `RefreshLexeme` (append, reset isCreatingLexeme), но артефакт декларирует Datasource Msg out-of-scope.

**Что сделать:** Перенеси описание `RefreshLexeme` в раздел «Канон в contract_io» с короткой ссылкой. В разделе «Изменяются» оставь только удаление старого фрагмента reducer'а (читавшего `addLexemeBottomState.isTranslationCheck/isDefinitionCheck`).

### F021 — Cancel создаёт «empty TextValueState» — третий режим

CancelTranslationEdit/CancelDefinitionEdit при `origin == ""` оставляет `TextValueState(isEdit=false, origin="", edited="")` ненульным. Этот режим не описан в contract_state.

**Что сделать (рекомендация):** При `origin == ""` Cancel эквивалентен Remove (симметрично политике «пустой Exit ⇒ RemoveTranslation»). Reducer: `if (translation.origin.isEmpty()) state.copy(lexemeList = ... translation = null) else state.copy(... isEdit = false, edited = origin)`. Это сохраняет 2 наблюдаемых режима nullable.

### F022 — OpenLexemeMenu не определяет «единственное открытое меню»

UX DropdownMenu в Compose — модальный popup, эксклюзивный.

**Что сделать:** В reducer-логике OpenLexemeMenu (isShow=true): закрой меню остальных лексем. State change: `lexemeList.map { l -> if (l.id == lexemeId) l.copy(isMenuOpen = isShow) else l.copy(isMenuOpen = false) }`. Зафиксируй как инвариант: `forall l1 != l2: !(l1.isMenuOpen && l2.isMenuOpen)`.

### F023 — Маркировка [ИЗМЕНЕНО]/[НОВОЕ] непоследовательна

**Что сделать:** Промаркируй все изменённые Msg одинаково: RemoveWord, CommitWordChanges, ExitTranslationEditMode, ExitDefinitionEditMode, NavigateBack, UiMsg.ShowNotification, LoadingWord, EnterTranslationEditMode/Definition, CreateTranslation/Definition, UpdateTranslationInput/Definition — все имеют изменения в reducer-логике после F-правок. Помечай `**[ИЗМЕНЕНО]**`. Или сними маркировку отовсюду — единообразие.

### F026 — CommitWordChanges без guard

**Что сделать:** Guard `if (!state.wordState.isEditMode || state.wordState.id == NOT_IN_DB || state.isLoading) state to emptySet() else ...`.

### F027 — EnterWordEditMode без guard

**Что сделать:** Guard `if (state.wordState.isEditMode || state.wordState.id == NOT_IN_DB || state.isLoading) state to emptySet() else state.copy(wordState = ... isEditMode = true, edited = state.wordState.value) to emptySet()`.

### F029 — OpenDeleteWordDialog без guard

**Что сделать:** Guard `if (state.wordState.id == NOT_IN_DB || state.isLoading) state to emptySet() else state.copy(wordState = ... showWarningDialog = true) to emptySet()`.

### F030 — Asymmetric naming Exit/Cancel

Word: `ExitWordEditMode` (cancel) + `CommitWordChanges` (commit).
Translation/Definition: `ExitTranslationEditMode` (commit!) + `CancelTranslationEdit` (cancel).

«Exit» означает противоположное на разных сущностях.

**Что сделать:** Либо переименуй пару translation: `ExitTranslationEditMode` → `CommitTranslationEdit` (а CancelTranslationEdit оставь). Симметрично для definition. Имена выровнены: Commit / Cancel везде.
Либо для word ввести явное `CancelWordEdit` = `ExitWordEditMode` (alias) + `CommitWordChanges` остаётся. Хуже — двойной namespace.

Рекомендую первый вариант: переименовать Exit*EditMode → Commit*Edit для translation/definition.

### F032 — UiMsg.ShowNotification противоречит UI Msg scoping

Trigger описан как «программный (effect-handler шлёт)». Это Datasource→UI Msg.

**Что сделать:** Опции:
- (a) Вынеси `UiMsg.ShowNotification` в раздел «Canonical в contract_io» (это Datasource Msg).
- (b) Расширь определение UI Msg в начале артефакта: «UI Msg = действия пользователя + UI feedback inbound от effects (Show/Hide UI элементов)».
- (c) Раздели на два: `UserDismissNotification` (UI Msg, тап на snackbar) + `ShowNotification` (Datasource → в contract_io).

Рекомендую (c).

### F033 — Cancel*Edit triggers underspecified

**Что сделать:** Зафиксируй конкретный список triggers: «системный back-жест внутри поля → CancelTranslationEdit», «Esc на железной клавиатуре (если поддерживается) → CancelTranslationEdit», «тап вне поля при пустом edited → CancelTranslationEdit (опционально, спорно — TextField обычно не реагирует на тап-вне)». Если UI-trigger пока неизвестен — оставь Msg без концептуального trigger но в комментарии «trigger будет выбран в UI sub-flow».

### F034 — Exit*EditMode без no-op guard

Если `translation.edited == translation.origin` — Update в БД лишний.

**Что сделать:** Расширь ветвление в Exit*EditMode:
1. `edited.isEmpty()` ⇒ Remove (уже сделано F009).
2. `edited == origin` ⇒ просто `isEdit = false`, effect = emptySet().
3. Иначе ⇒ Update.

---

## Задача

Перепиши `contract_ui_msg.md` закрыв все 14 уникальных approved findings (F017/F024 одно решение; F018/F028 одно; F019/F031 одно — итого 3 critical + 11 minor решений).

Сохрани header «черновик», структуру разделов, sealed interface Msg + варианты. `_model_` в конце.
