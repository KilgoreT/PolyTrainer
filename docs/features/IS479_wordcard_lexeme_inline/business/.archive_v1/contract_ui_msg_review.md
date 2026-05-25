## Итерация 1 (2026-05-19T02:42:00-0600)

### F001 [architect] critical
<br>**Description:** CommitWordChanges описан как state-change `isEditMode = false` без сброса `edited` — нарушает инвариант 2 из contract_state.
<br>**Status:** approved
<br>**Verdict:** CommitWordChanges нарушает инвариант 2 (edited != "" при isEditMode=false).

### F002 [architect] critical
<br>**Description:** Семантика RefreshLexeme описана в разделе «Изменяются», при том что артефакт декларирует Datasource Msg out-of-scope.
<br>**Status:** rejected
<br>**Verdict:** RefreshLexeme в «Изменяются» — описание переходного контракта; артефакт сам помечает её каноном в contract_io.

### F003 [architect] minor
<br>**Description:** Guard `CreateLexeme` `!isCreatingLexeme && !isLoading && wordState.id != NOT_IN_DB` содержит избыточную проверку.
<br>**Status:** rejected
<br>**Verdict:** Инв. 1 не запрещает `id!=NOT_IN_DB && isLoading`; проверки независимы.

### F004 [architect] minor
<br>**Description:** UiMsg.ShowNotification(show=false) — политика для `text` из payload не зафиксирована.
<br>**Status:** approved
<br>**Verdict:** Политика text при show=false действительно не зафиксирована — пробел.

### F005 [architect] minor
<br>**Description:** RemoveWord с «state не меняется», но `topBarState.isMenuOpen` и `wordState.showWarningDialog` остаются `true` — политика закрытия не зафиксирована.
<br>**Status:** approved
<br>**Verdict:** Закрытие меню/диалога при RemoveWord — reducer-логика per UI Msg, в скоупе шага.

### F006 [architect] minor
<br>**Description:** ExitTranslationEditMode / ExitDefinitionEditMode — `translation.isEdit` остаётся `true` до прихода `RefreshTranslation`; политика UI не зафиксирована.
<br>**Status:** approved
<br>**Verdict:** Политика `translation.isEdit` при Exit — reducer-логика UI Msg, в скоупе.

### F007 [architect] minor
<br>**Description:** LoadingWord без guard — повторный вызов спамит `DatasourceEffect.LoadWord`.
<br>**Status:** approved
<br>**Verdict:** Guard на LoadingWord — reducer-логика per UI Msg, в скоупе.

### F008 [analyst] critical
<br>**Description:** `CommitWordChanges` ставит `isEditMode = false`, но не сбрасывает `edited = ""` (дубликат F001).
<br>**Status:** approved
<br>**Verdict:** Дубликат F001 — та же претензия к инварианту 2.

### F009 [analyst] critical
<br>**Description:** `ExitTranslationEditMode` / `ExitDefinitionEditMode` описаны как «коммит», но contract_state политика обещает что пустой `edited` ⇒ удаление через `RemoveTranslation` — reducer-логика превращения Update vs Remove не зафиксирована.
<br>**Status:** approved
<br>**Verdict:** Reducer должен явно зафиксировать когда Exit*EditMode инициирует Update vs Remove.

### F010 [analyst] critical
<br>**Description:** Для translation/definition отсутствует Msg «отмена редактирования без коммита» — у wordState есть пара ExitWordEditMode (cancel) + CommitWordChanges (commit), для chip-полей только один Exit.
<br>**Status:** approved
<br>**Verdict:** Отсутствие Msg «отмена без коммита» для chip-полей — пробел в наборе UI Msg.

### F011 [analyst] minor
<br>**Description:** EnterTranslationEditMode / EnterDefinitionEditMode без guard на `translation?.isEdit == true` — повторный клик сбросит edited.
<br>**Status:** approved
<br>**Verdict:** Guard на повторный Enter*EditMode — reducer-логика UI Msg, в скоупе.

### F012 [analyst] minor
<br>**Description:** CreateTranslation / CreateDefinition без guard на `translation == null` — повторный тап по chip перезапишет TextValueState.
<br>**Status:** approved
<br>**Verdict:** Guard `translation == null` для Create* — reducer-логика UI Msg, в скоупе.

### F013 [analyst] minor
<br>**Description:** UpdateTranslationInput / UpdateDefinitionInput не описывают поведение если TextValueState == null или isEdit == false.
<br>**Status:** approved
<br>**Verdict:** Поведение Update*Input при null/!isEdit — reducer-логика UI Msg, в скоупе.

### F014 [analyst] minor
<br>**Description:** RemoveWord не имеет guard `wordId != NOT_IN_DB` / `!isLoading`.
<br>**Status:** approved
<br>**Verdict:** Guard для RemoveWord — reducer-логика UI Msg, в скоупе.

### F015 [analyst] minor
<br>**Description:** NavigateBack сбрасывает только `isCreatingLexeme`, но не описывает поведение `isEditMode` / chip-полей / диалога / меню.
<br>**Status:** approved
<br>**Verdict:** Политика NavigateBack для остальных полей — reducer-логика UI Msg, в скоупе.

### F016 [analyst] minor
<br>**Description:** LoadingWord без guard (дубликат F007).
<br>**Status:** approved
<br>**Verdict:** Дубликат F007.

## Итерация 2 (2026-05-19T02:55:00-0600)

### F017 [architect] critical
<br>**Description:** Guard `LoadingWord` `if (isLoading || wordState.id != NOT_IN_DB) state to emptySet()` противоречив: contract_state default `isLoading: Boolean = true` ⇒ первая эмиссия LoadingWord будет shortcut-проигнорирована, LoadWord effect не уйдёт, слово не загрузится. Либо дефолт isLoading = false, либо инвертировать guard.
<br>**Status:** pending

### F018 [architect] critical
<br>**Description:** Асимметрия guards: для UpdateTranslationInput/UpdateDefinitionInput добавлены guards (F013), для UpdateWordInput — нет. Получение UpdateWordInput при `wordState.isEditMode == false` нарушит инвариант 2.
<br>**Status:** pending

### F019 [architect] minor
<br>**Description:** NavigateBack обоснование самопротиворечиво: «ViewModel уничтожается» vs «async-эффект должен не писать в умершее состояние» vs «залипший FAB если ViewModel переживает». Логика рассыпается. Нужна одна модель ViewModel-lifetime.
<br>**Status:** pending

### F020 [architect] minor
<br>**Description:** Scope-leak: RefreshLexeme описан в «Изменяются» (append, reset isCreatingLexeme, удаление чтения addLexemeBottomState), хотя артефакт декларирует Datasource Msg out-of-scope.
<br>**Status:** pending

### F021 [architect] minor
<br>**Description:** CancelTranslationEdit/CancelDefinitionEdit при отмене свежесозданного поля (`origin == ""`) оставляет TextValueState(isEdit=false, origin="", edited="") ненульным. Появляется третий наблюдаемый режим nullable translation (null/non-null+empty/non-null+filled), не зафиксированный в contract_state. Либо при `origin == ""` Cancel = null (RemoveTranslation), либо явно зафиксировать третий режим.
<br>**Status:** pending

### F022 [architect] minor
<br>**Description:** OpenLexemeMenu не определяет инвариант «единственное открытое меню» — допустимо состояние когда у нескольких лексем `isMenuOpen == true` одновременно. UX DropdownMenu подразумевает эксклюзивность.
<br>**Status:** pending

### F023 [architect] minor
<br>**Description:** Маркировка [ИЗМЕНЕНО]/[НОВОЕ] непоследовательна — стоит на CreateLexeme/CancelTranslationEdit/CancelDefinitionEdit, но не стоит на RemoveWord/CommitWordChanges/ExitTranslationEditMode/ExitDefinitionEditMode/NavigateBack/UiMsg.ShowNotification (тоже изменены).
<br>**Status:** pending

### F024 [analyst] critical
<br>**Description:** Guard LoadingWord shortcut-игнорирует первую загрузку при дефолте isLoading=true (дубликат F017).
<br>**Status:** pending

### F025 [analyst] critical
<br>**Description:** RemoveLexeme не сбрасывает `LexemeState.isMenuOpen` для удаляемой лексемы — меню остаётся открытым до возврата `RefreshLexeme`/failure. Симметрично F005 (закрытие меню/диалога в RemoveWord). То же для RemoveTranslation/RemoveDefinition (триггер из меню лексемы).
<br>**Status:** pending

### F026 [analyst] minor
<br>**Description:** Отсутствует guard на CommitWordChanges: (a) `isEditMode == false` (коммит без edit-mode перезапишет value пустой строкой); (b) `id == NOT_IN_DB || isLoading` (коммит до загрузки). Симметрично F013/F014.
<br>**Status:** pending

### F027 [analyst] minor
<br>**Description:** Отсутствует guard на EnterWordEditMode: (a) `isEditMode == true` (повторный вход потеряет ввод, как F011); (b) `id == NOT_IN_DB || isLoading`. Симметрия с F011/F014.
<br>**Status:** pending

### F028 [analyst] minor
<br>**Description:** UpdateWordInput без guard `!isEditMode` — нарушит инвариант 2 (дубликат F018).
<br>**Status:** pending

### F029 [analyst] minor
<br>**Description:** OpenDeleteWordDialog без guard `id == NOT_IN_DB || isLoading` — открытие диалога удаления до загрузки слова.
<br>**Status:** pending

### F030 [analyst] minor
<br>**Description:** Несимметричный нейминг Exit/Cancel: для word `ExitWordEditMode` = cancel, `CommitWordChanges` = commit; для translation/definition `ExitTranslationEditMode` = commit, `CancelTranslationEdit` = cancel. Один глагол означает противоположные действия.
<br>**Status:** pending

### F031 [analyst] minor
<br>**Description:** NavigateBack-обоснование самопротиворечиво (дубликат F019).
<br>**Status:** pending

### F032 [analyst] minor
<br>**Description:** UiMsg.ShowNotification отнесён к UI-стороне, но trigger описан как «программный (effect-handler шлёт)» — это Datasource→UI Msg, противоречит scoping на стр.13.
<br>**Status:** pending

### F033 [analyst] minor
<br>**Description:** Триггер для CancelTranslationEdit/CancelDefinitionEdit оставлен открытым («UI sub-flow выбирает»). Для consistency с остальными Msg перечислить минимум допустимых triggers.
<br>**Status:** pending

### F034 [analyst] minor
<br>**Description:** Exit*EditMode не различает «коммит без изменений» (`edited == origin`, isChanged=false) — отправляется Update в БД даже когда писать нечего. Можно расширить: `!isChanged() ⇒ просто isEdit=false без эффекта`.
<br>**Status:** pending

## Итерация 3 (2026-05-19T03:10:00-0600)

### F035 [architect] critical
<br>**Description:** CommitTranslationEdit/CommitDefinitionEdit ветвь 3 (`edited != origin`) ставит `isEdit = false`, но `origin` не обновляет. До прихода RefreshTranslation/RefreshDefinition: `isEdit == false ∧ edited != origin` — нарушение инварианта 8 из contract_state (жёсткое утверждение, не политика). Артефакт оборачивает в «политику», но contract_state такой политики не содержит. Либо инвариант 8 переформулировать в contract_state, либо reducer должен синхронизировать edited↔origin оптимистично.
<br>**Status:** approved

### F036 [architect] critical
<br>**Description:** NavigateBack Модель A удаляет сброс `isCreatingLexeme`. Contract_state раздел «Политики» явно фиксирует: «isCreatingLexeme сбрасывается также при NavigateBack ... Reducer-политика; детали — в contract_ui_msg». Артефакт делегирован контрактом state на формализацию, но отменяет её. Без согласования — артефакты расходятся.
<br>**Status:** approved

### F037 [analyst] critical
<br>**Description:** Дубликат F035 — CommitTranslationEdit ветвь 3 нарушает инвариант 8.
<br>**Status:** approved

### F038 [analyst] critical
<br>**Description:** Дубликат F036 — NavigateBack vs политика contract_state.
<br>**Status:** approved

### F039 [analyst] minor
<br>**Description:** F022-инвариант «не более одной лексемы с isMenuOpen == true» объявлен в contract_ui_msg, должен быть в contract_state (инвариант шейпа state).
<br>**Status:** approved

### F040 [analyst] minor
<br>**Description:** Термин «производный» guard в guard-таблице не определён в артефакте — читатель не понимает чем отличается от обычного.
<br>**Status:** approved
