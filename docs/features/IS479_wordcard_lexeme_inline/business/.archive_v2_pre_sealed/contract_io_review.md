## Итерация 1 (2026-05-19T12:00:00-0600)

### F001 [architect] minor

**Description:** `UiEffect.ShowNotification(text: String)` сигнатура (line 346) противоречит примерам в reducer-логике, где передают `R.string.X` (Int) как `text` (lines 166, 207) — либо менять сигнатуру на `@StringRes Int`, либо в примерах писать `context.getString(R.string.X)`.

**Status:** approved

### F002 [architect] minor

**Description:** F002 deferred-путь для `CommitTranslationEdit` ветка 1 нарушает симметрию с `CancelTranslationEdit` при `origin == ""` (immediate nullify, без effect) — два разных правила для одного концепта «удалить перевод»; стоит явно зафиксировать «Cancel-empty не идёт в БД ⇒ deferred неприменим».

**Status:** approved

### F003 [architect] minor

**Description:** Payload `RefreshTranslation(lexemeId, translation: TextValueState?)` over-specified — reducer всегда выставляет `isEdit = false`, поле `isEdit` из payload игнорируется. Минимальный payload: `translation: String?` (raw origin из БД), reducer формирует `TextValueState(origin, edited=origin, isEdit=false)`.

**Status:** approved

### F004 [architect] minor

**Description:** F003 guard в reducer-snippet структурно корректен (`||` short-circuit), но идиоматичнее `val lex = lexemeList.find { ... }; if (lex == null || lex.translation == null) ...` — один lookup, защита от refactor.

**Status:** approved

### F005 [architect] critical

**Description:** `LoadWord` failure (throw из `getTermById`) отнесён в Backlog, но это entry-point — необработанный throw оставит `isLoading = true` навсегда. `CreateLexeme/UpdateTranslation/UpdateDefinition` failure канонизируются safe-Msg прямо сейчас — несимметричность в обработке.

**Status:** approved

### F006 [qa_engineer] critical

**Description:** `UiEffect.ShowNotification(text: String)` строится в reducer'е, но контракт говорит «resolve Int→String на UI» — у reducer'а нет Context для resolve `R.string.*`. Дубликат F001 с другого угла.

**Status:** approved

### F007 [qa_engineer] critical

**Description:** `ShowNotification` потерял механизм dismissal — текущий код использует `UiMsg.ShowNotification(text, show: Boolean)` с веткой `show=false` для сброса `snackbarState.show`; в контракте `data class ShowNotification(val text: String)` без `show` — невозможно скрыть snackbar.

**Status:** rejected

### F008 [qa_engineer] critical

**Description:** `UpdateTranslationFailed`/`UpdateDefinitionFailed` reducer-логика «откатить `edited = origin`» небезопасна — failure-Msg async, пользователь мог снова войти в edit-mode и печатать; rollback затрёт активный ввод. Нужен guard `!isEdit` либо политика «failure выкидывает из edit».

**Status:** approved

### F009 [qa_engineer] critical

**Description:** Reducer-логика `RefreshTranslation` / `RefreshDefinition` не описывает поведение когда `lexemeList.none { it.id == lexemeId }` (race с `RemoveLexeme`) — без guard'а IndexOutOfBounds / NPE при `.first { it.id == lexemeId }`.

**Status:** approved

### F010 [qa_engineer] critical

**Description:** Дубликат F005 — `LoadWord` failure не покрыт.

**Status:** approved

### F011 [qa_engineer] critical

**Description:** `CreateLexeme` failure канонизирован только для `null` от use-case (`CreateLexemeFailed`), но throw из `addLexeme` оставлен без покрытия — handler упадёт, `isCreatingLexeme` останется `true` навсегда, FAB заблокирован.

**Status:** approved

### F012 [qa_engineer] critical

**Description:** F002 описание ветки `CommitTranslationEdit ветка 1` (transient «UI рендерит с `edited == ""`») неверно — в view-mode (`isEdit=false`) рендерится `origin` через `toValue(isEdit)`; до прихода `RefreshTranslation(null)` пользователь увидит **старое** translation, не пустое.

**Status:** approved

### F013 [qa_engineer] minor

**Description:** EC2 (двойной Commit translation) не покрывает сценарий «второй Commit пришёл когда первый Refresh уже обновил `origin`, а пользователь в edit-mode» — concurrent refresh write в `origin` может затереть active edit.

**Status:** approved

### F014 [qa_engineer] minor

**Description:** EC3 (Remove во время edit): параллельный `UpdateLexemeTranslation` (Commit перед Remove) после `LexemeRemoved` вернёт `RefreshTranslation` на отсутствующую лексему — см. F009 (одна проблема, два угла).

**Status:** approved

### F015 [qa_engineer] minor

**Description:** `RefreshTranslation(translation != null)` не оговаривает что делать если на момент прихода `isEdit == true` (race с EnterTranslationEditMode после Commit) — затирание `edited` рушит сессию редактирования.

**Status:** approved

### F016 [qa_engineer] minor

**Description:** EC4 (`WordNotFound` параллельно с FAB) — нет сводной таблицы «что блокируется на isLoading=true», только разрозненные guards в `contract_ui_msg`.

**Status:** approved

### F017 [qa_engineer] minor

**Description:** `RemoveLexeme` race описан неверно — два `RemoveLexeme` Msg подряд: между ними лексема ещё в `lexemeList` (первый эффект не вернул `LexemeRemoved`), второй пройдёт guard и улетит второй DELETE.

**Status:** approved

### F018 [qa_engineer] minor

**Description:** `UpdateLexemeTranslation` edge case «`lexemeId == -1L` мёртвая ветвь» — use-case API `lexemeId: Long?` теоретически позволит создать новую лексему с `NOT_IN_DB`, `RefreshTranslation(newLexemeId)` придёт на пустой filter. Defense-in-depth нужно.

**Status:** approved

### F019 [qa_engineer] minor

**Description:** `NavigationEffect.Back` double-pop race — два `RemoveWord` подряд → два `NavigateBack` Msg → два `NavigationEffect.Back`; guard `isLoading` тут не помогает.

**Status:** approved

### F020 [qa_engineer] minor

**Description:** Subscribers self-check вопрос 4 (DAO-flow) формулировка слишком узкая — переформулировать как «текущее архитектурное решение, требует пересмотра при reactive миграции».

**Status:** approved

### F021 [qa_engineer] minor

**Description:** `UpdateWord` Edge cases — race «Commit → EnterEdit → Commit до первого ответа» = last-write-wins. Acceptable, но семантика должна быть явной.

**Status:** approved

### F022 [qa_engineer] minor

**Description:** `UiEffect.ShowNotification` concurrent — два Show в один tick могут потеряться (Compose batched recomposition). На практике M3 SnackbarHost обрабатывает по очереди, но контракт это не гарантирует.

**Status:** approved

## Итерация 2 (2026-05-19T12:35:00-0600)

### F023 [architect] critical

**Description:** F019 рассинхрон: contract_io v2 добавил поле `isRemovingWord: Boolean` в State, но `contract_state.md` v2 этого поля НЕ содержит. contract_io канонизирует изменение shape State вне своего шага.

**Status:** approved

### F024 [architect] critical

**Description:** Рассинхрон `RemoveWord` UI Msg: contract_io v2 фиксирует `data object RemoveWord : Msg` без payload, contract_ui_msg v2 — `data class RemoveWord(val wordId: Long)`. Источник истины для UI Msg — contract_ui_msg, контракт разорван.

**Status:** approved

### F025 [architect] critical

**Description:** Рассинхрон поведения `RemoveTranslation`/`RemoveDefinition` UI Msg: contract_ui_msg v2 говорит «translation не обнуляем — ждём Refresh», contract_io v2 под F012-driven immediate nullify — обнуляем сразу.

**Status:** approved

### F026 [architect] minor

**Description:** Таблицы guards между io и ui_msg частично расходятся (io добавляет guard `isRemovingWord` на word-Msg, в ui_msg-таблице такого guard'а нет, поле в state отсутствует).

**Status:** approved

### F027 [architect] minor

**Description:** contract_ui_msg v2 forward-ref таблица не содержит новых Datasource Msg, введённых в io v2: `WordLoadFailed`, `UpdateWordFailed`, `RemoveWordFailed`, `RemoveLexemeFailed`, `UpdateTranslationFailed`, `RemoveTranslationFailed`, `UpdateDefinitionFailed`, `RemoveDefinitionFailed`, `LexemeRemoved`.

**Status:** approved

### F028 [architect] minor

**Description:** Reducer `UpdateTranslationFailed` имеет избыточный safe-call `?.` после early-return на null — псевдокод вводит ясность для читателя, минор по стилю.

**Status:** approved

### F029 [architect] minor

**Description:** Reducer-секция `NavigateBack` не описывает что `isRemovingWord` остаётся true до уничтожения ViewModel — стоит явная нота для будущего читателя.

**Status:** approved

### F030 [architect] minor

**Description:** В io null vs throw для `addLexeme` оба ведут к `CreateLexemeFailed` — нет различения source ошибки. Если в будущем нужны different error-codes — Msg-семейство потребует расширения. Backlog-кандидат.

**Status:** approved

### F031 [qa_engineer] critical

**Description:** Дубликат F023 — F019 isRemovingWord не синхронизирован с contract_state.

**Status:** approved

### F032 [qa_engineer] critical

**Description:** F021 утверждает «Mate сериализует effects через `viewModelScope.launch`» — фактологически неверно: `launch` НЕ сериализует, каждый effect-handler запускается в независимой корутине. Race UPDATE/DELETE возможен (immediate-nullify RemoveTranslation + CreateTranslation + UpdateLexemeTranslation в разных порядках).

**Status:** approved

### F033 [qa_engineer] critical

**Description:** Concurrent `CreateLexeme` (isCreatingLexeme=true в полёте) + `RemoveWord` не блокирован — таблица guards `isRemovingWord` не закрывает обратный путь, `RemoveWord` guards не упоминают `isCreatingLexeme`; возможна orphan-lexeme в БД (INSERT после CASCADE DELETE word'а) либо crash на FK.

**Status:** approved

### F034 [qa_engineer] critical

**Description:** `isRemovingWord=true` НЕ блокирует lexeme/translation/definition UI Msg — пользователь может писать пока удаление word в полёте, эти effects уйдут в БД параллельно с word-DELETE → race FK / orphan записи.

**Status:** approved

### F035 [qa_engineer] critical

**Description:** `UpdateWordFailed` явно отказывается от rollback `wordState.value`, но F012 для translation/definition требует savedOrigin/immediate rollback. Асимметрия не обоснована.

**Status:** approved

### F036 [qa_engineer] minor

**Description:** NavigateBack UI Msg (системный back) во время `isRemovingWord=true` не покрыт — ViewModel умирает, DB DELETE прерывается (viewModelScope cancellation), partial-state в БД.

**Status:** approved

### F037 [qa_engineer] minor

**Description:** `OpenLexemeMenu` отсутствует в таблице guards для `isLoading`/`wordState.id == NOT_IN_DB`/`isRemovingWord` — связан с F034.

**Status:** approved

### F038 [qa_engineer] minor

**Description:** EC2 (Race Commit during Refresh) не покрывает сценарий `UpdateTranslationFailed` приходящий для ПЕРВОГО commit пока пользователь во ВТОРОМ edit.

**Status:** approved

### F039 [qa_engineer] minor

**Description:** F018 псевдокод использует `Log.w(TAG, ...)` без определения `TAG` — нужно указать конкретный logger.

**Status:** approved

### F040 [qa_engineer] minor

**Description:** `RemoveTranslationFailed`/`RemoveDefinitionFailed` rollback `translation` не должен затрагивать `isMenuOpen` — явно не зафиксировано в reducer-логике.

**Status:** approved

### F041 [qa_engineer] minor

**Description:** DismissNotification reducer logic сбрасывает `show = false`, оставляя `title` как есть; concurrent ShowNotifications batching сценарий не описан полностью.

**Status:** approved

### F042 [qa_engineer] minor

**Description:** Двойной Commit-empty в один тик — guard `!isEdit` страхует, но явной EC проверки нет.

**Status:** approved

## Итерация 3 (2026-05-19T13:00:00-0600)

### PASS [architect]

### PASS [qa_engineer]

---

## Итог

3 итерации: 42 findings → 41 approved + 1 rejected. На ит.3 — PASS у обоих ревьюверов. `approved_critical = []` → `review_passed=true` → выход из repeat.

**Финал:** ит.3 откатил решения ит.2, которые выходили за scope `contract_io` (поле `isRemovingWord`, форма `RemoveWord` UI Msg, immediate nullify). Все рассинхроны с `contract_state`/`contract_ui_msg` устранены. Сохранены хорошие решения (failure-Msg семейство, try/catch, find-guards, минимальный payload Refresh*).

**Новый раздел `## Feedback в предыдущие шаги`** в артефакте — 3 пункта для conductor'а:
1. `contract_ui_msg`: `isLoading=true` в reducer'е `RemoveWord` (закроет concurrent race).
2. `contract_state`: tech debt `CommitWordChanges` без savedValue (silent revert).
3. `contract_ui_msg`: forward-ref минимальный (wontfix).
