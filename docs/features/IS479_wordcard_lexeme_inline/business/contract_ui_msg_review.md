## Итерация 1 v3 (2026-05-19T15:30:00-0600)

### F001 [architect] critical

**Description:** Инв. 10 (`Loaded ⇒ value != ""`) выведен в зависимость от UI-дисциплины — reducer `CommitWordChanges` явно отказывается быть последней линией обороны («reducer-страховка опциональна»), что превращает структурный инвариант в негарантируемый.

\*\*Status:\*\* approved

### F002 [architect] critical

**Description:** Несогласованная политика remove у translation/definition — `CommitTranslationEdit` ветвь 1 делает optimistic `translation = null` в reducer'е, а `RemoveTranslation` явно требует ждать `RefreshTranslation(null)` от data-слоя (pessimistic); при сбое effect-handler'а первая ветвь теряет данные без отката.

\*\*Status:\*\* approved

### F003 [architect] minor

**Description:** `EnterWordEditMode` инициализирует `edited = w.value`, но `w` после `closeAllEditModes()` уже имеет `edited = ""` — не баг, но псевдокод стоит читать с осторожностью; стоит явно показать через `when`/`is`, а не `as Loaded`.

\*\*Status:\*\* approved

### F004 [architect] minor

**Description:** `RemoveTranslation` / `RemoveDefinition` не сбрасывают `translation.isEdit` / `definition.isEdit` — если UI разрешит открыть меню лексемы во время её inline-edit, останется висящий edit-flag до прихода `RefreshTranslation(null)`; reducer полагается на UI-контракт скрытия меню в edit-mode.

\*\*Status:\*\* approved

### F005 [architect] minor

**Description:** `RemoveWord(wordId: Long)` — payload содержит `wordId`, но reducer не сверяет его с `(wordState as Loaded).id`.

\*\*Status:\*\* approved

### F006 [analyst] critical

**Description:** Дубликат F001 — Guard `CommitWordChanges` пропускает пустой `edited` (страховка опциональна), что нарушит инв. 10 если UI забыл disable кнопки.

\*\*Status:\*\* approved

### F007 [analyst] critical

**Description:** `OpenLexemeMenu(isShow=true)` — поведение по отношению к активному edit-mode не определено: либо явно зафиксировать что меню НЕ закрывает edit (и обосновать), либо добавить closeAllEditModes() хелпер.

\*\*Status:\*\* approved

### F008 [analyst] critical

**Description:** `CancelTranslationEdit` / `CancelDefinitionEdit` при `origin.isEmpty()` ставят поле в `null`, но guard разрешает срабатывание только при `isEdit == true`; комбинация `(translation != null, isEdit=false, origin="")` не запрещена инвариантами — нужен либо новый инвариант, либо явное упоминание невозможности этой комбинации.

\*\*Status:\*\* approved

### F009 [analyst] minor

**Description:** Описание `RefreshTranslation` в forward-ref не отмечает идемпотентность `translation = null` повторной записи после Commit ветви 1.

\*\*Status:\*\* approved

### F010 [analyst] minor

**Description:** `CloseDeleteWordDialog` guard на `wordState !is Loaded` — не указано что `Loaded ∧ !showWarningDialog` тоже игнорируется (или явно «noop-перезапись приемлема»).

\*\*Status:\*\* approved

### F011 [analyst] minor

**Description:** `DismissNotification` не имеет guard на `snackbarState.show == false` — повторный dismiss безвреден, но не указано явно как «идемпотентно».

\*\*Status:\*\* approved

---

## Итерация 2 (2026-05-19T16:00:00-0600)

### F012 [architect] critical

**Description:** `OpenLexemeMenu(isShow=true)` псевдокод `state.closeAllEditModes().copy(lexemeList = state.lexemeList.map { ... })` — `state.lexemeList` ссылается на исходный список (не обновлённый после `closeAllEditModes()`). Kotlin immutable receiver: `state` остаётся неизменным, новый объект из `closeAllEditModes()` слева от `.copy(...)` затирается исходным `lexemeList` справа. F007 закрыт неполно — инв.9 нарушается.

**Status:** approved

### F013 [architect] minor

**Description:** В `CommitTranslationEdit`/`CommitDefinitionEdit` для конструирования эффекта используется `(state.wordState as WordState.Loaded).id` (явный cast). F003 декларировал переход на `when` — закрыт для word-Msg, но не для адресных эффектов в translation/definition.

**Status:** approved

### F014 [architect] minor

**Description:** `Cancel*Edit` при `origin == ""` ставит `translation = null` локально (без effect), что асимметрично F002 pessimistic-подходу `Commit*Edit` ветви 1. Стоит явно зафиксировать ремарку «в Cancel: ни effect, ни write в БД не было, локальный nullify безопасен; симметрии с F002 не требуется».

**Status:** approved

### F015 [architect] minor

**Description:** `OpenLexemeMenu(isShow=false)` псевдокод не делает явный guard «лексема не найдена» — несогласованность с таблицей guards.

**Status:** approved

### F016 [architect] minor

**Description:** Транзитивная семантика snackbar при `Remove*` failure не упомянута в reducer-секции; это `contract_io` ответственность, можно проигнорировать.

**Status:** rejected

### F017 [analyst] minor

**Description:** В `OpenLexemeMenu(isShow=true)` `map { l -> l.copy(isMenuOpen = ...) }` создаёт новый объект для каждой лексемы — лишние аллокации, инвалидирует Compose reference equality. Корректнее conditional copy.

**Status:** approved

### F018 [analyst] minor

**Description:** Чек-лист инв.9 говорит `closeAllEditModes()` вызывается «первым шагом» в `Create*`, но фактически после guard'а. Формулировка «первым шагом» неточна.

**Status:** approved

### F019 [analyst] minor

**Description:** В сводной таблице guards для `CreateTranslation` указано только `lexeme.translation != null`, без `(+ лексема не найдена)`. Несимметрично с `RemoveLexeme`.

**Status:** approved

### F020 [analyst] minor

**Description:** `RefreshTranslation` forward-ref описано как «синхронизирует `translation.origin` (или ставит `translation = null`)» — асимметрия payload (origin vs полный объект) вводит в заблуждение. Финал в `contract_io`, но текущая формулировка неоднозначна.

**Status:** rejected

### F021 [analyst] minor

**Description:** `Cancel*Edit` при `origin == ""` зависит от неявного «`CreateTranslation` не шлёт effect, в БД ничего нет, Refresh не придёт». Стоит явно зафиксировать.

**Status:** approved

### F022 [analyst] minor

**Description:** Чек-лист инв.4 не упоминает что `translation = null` в Cancel ветке тоже корректно удерживает инвариант (через nullable).

**Status:** approved

### F023 [analyst] minor

**Description:** В `OpenLexemeMenu` логика «exclusive open» может быть extension `setLexemeMenuOpenExclusive(lexemeId)` — стилистическая рекомендация.

**Status:** rejected

---

## Итерация 3 (2026-05-19T16:30:00-0600)

### F024 [analyst] critical

**Description:** `RemoveLexeme` нарушает инв.9 симметрично F004 — если пользователь активно редактировал `translation`/`definition` лексемы и параллельно через меню запустил `RemoveLexeme`, `isEdit=true` остаётся живым на удаляемой лексеме до прихода Refresh. Нужно сбросить `translation?.isEdit, definition?.isEdit` целевой лексемы в reducer'е.

**Status:** approved

### F025 [architect] minor

**Description:** Pseudo-code `EnterTranslationEditMode`/`EnterDefinitionEditMode` использует `l.translation?.copy(isEdit = true, edited = l.translation.origin)` — двойное обращение к nullable property через safe-call. Рекомендация: `l.translation?.let { tr -> l.copy(translation = tr.copy(...)) }`.

**Status:** approved

### F026 [architect] minor

**Description:** Презентационная асимметрия pseudocode wordState-мутаций. `OpenDeleteWordDialog`, `CloseDeleteWordDialog`, `RemoveWord`, `RemoveLexeme` описаны текстом без `when`, тогда как Word-edit Msg — полным `when (val w = state.wordState)`. F003 требует везде `when`.

**Status:** approved

### F027 [architect] minor

**Description:** `RemoveTranslation`/`RemoveDefinition` без shortcut-guard на `lexeme.translation == null` / `definition == null` — reducer отправит лишний DB round-trip на несуществующее поле.

**Status:** approved

### F028 [architect] minor

**Description:** Локальный invariant в `OpenLexemeMenu` сформулирован как «свойство reducer-маппинга», но reducer не **восстанавливает** инвариант, а **сохраняет** — только из определённого стартового state.

**Status:** approved

### F029 [analyst] minor

**Description:** `EnterWordEditMode` нарушает букву правила F012 — читается `w.value` из исходного `wordState`, не из `closed.wordState`. Семантически работает, но правило в хелпере декларирует «после хелпера читать из результата, не из state».

**Status:** approved

### F030 [analyst] minor

**Description:** Асимметрия shortcut-ignore guards — `CloseDeleteWordDialog` имеет guard `!showWarningDialog`, `DismissNotification` имеет `!show`, но `CloseTopBarMenu` — без аналогичного `!topBarState.isMenuOpen ⇒ ignore`.

**Status:** approved

### F031 [analyst] minor

**Description:** `RemoveWord` не имеет полного pseudocode-блока — описание текстовое «`topBarState.isMenuOpen = false` (в Loaded)» без явного `when`.

**Status:** approved

### F032 [analyst] minor

**Description:** Transient-окно после `Commit*Edit` ветви 1 (pessimistic) — если пользователь успеет `EnterEditMode` до прихода `RefreshTranslation(null)`, активный edit будет тихо обнулён. Контракт `Refresh*` не уточняет поведение при активном edit.

**Status:** rejected


---

## Итерация 4 (2026-05-19T17:00:00-0600)

10 minor approved (без critical). Streak only-minor: 1.

### F033 [architect/analyst] minor — F029 self-contradiction в EnterWordEditMode
**Status:** approved
### F034 [analyst] minor — CommitTranslationEdit empty-origin scenario (после Create без ввода)
**Status:** approved
### F035 [analyst] minor — Whitespace asymmetry Word (isBlank) vs Translation (isEmpty)
**Status:** approved
### F036 [architect+analyst] minor — Open*/Close* shortcut асимметрия (OpenTopBarMenu, OpenDeleteWordDialog, OpenLexemeMenu без guard на «уже открыто»)
**Status:** approved
### F037 [analyst] minor — Неявный cross-contract invariant `translation.origin != "" ∨ translation == null`
**Status:** approved
### F038 [analyst] minor — F017 conditional-copy нарушен для target в OpenLexemeMenu (безусловный copy)
**Status:** approved
### F039 [architect] minor — Refresh* signature mismatch между forward-ref таблицей и разделом «Переносятся»
**Status:** approved
### F040 [architect] minor — CommitTranslationEdit guard + ветвь 3 — два when вместо одного полного
**Status:** approved
### F041 [architect] minor — F022 dangling reference в чек-листе инвариантов (не в log_messages ит.4)
**Status:** approved
### F042 [analyst] minor — RemoveLexeme pseudocode неоптимален (безусловный copy на target)
**Status:** approved

---

## Итерация 5 (2026-05-19T17:30:00-0600)

architect → PASS. analyst → 1 minor.

### F043 [analyst] minor

**Description:** F019 в сводной таблице guards (строка 862) утверждает «дублирует общую сноску ниже таблицы», но сноска расположена ВЫШЕ таблицы (строка 821 < строка 829) — фактическая ошибка указания направления.

**Status:** approved

**Verdict:** валидный текстовый проёб — направление неверное; одна строка правки.

---

## Итог по правилу minor-only streak

- Ит.4: 10 minor / 0 critical → streak = 1
- Ит.5: 1 minor / 0 critical → streak = 2 → **выход**

Артефакт принимается с tech debt:
- F043 (направление сноски в F019) — остаётся в этом файле как tech debt.

`contract_ui_msg.md` v3 закрыт. Переход к `contract_io`.
