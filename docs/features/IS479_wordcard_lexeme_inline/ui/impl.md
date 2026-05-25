# implement — IS479 wordcard lexeme inline (UI)

## Что сделано

### #0 strings.xml (en) [~]

- путь: `core/core-resources/src/main/res/values/strings.xml`
- добавлено: `<string name="word_card_lexeme_remove">Remove</string>`
- удалено: ничего (старые ключи `word_card_bottom_title`, `word_card_bottom_title_append` оставлены — cleanup в backlog).

> 📎 guide: docs/guides/theme-and-resources.md — "Общие строки → `core/core-resources/src/main/res/values/strings.xml`"
>
> 📎 guide: docs/guides/theme-and-resources.md — "Именование: `<раздел>_<описание>`"

### #1 strings.xml (ru) [~]

- путь: `core/core-resources/src/main/res/values-ru-rRU/strings.xml`
- добавлено: `<string name="word_card_lexeme_remove">Удалить</string>`
- удалено: ничего.

> 📎 guide: docs/guides/theme-and-resources.md — "Локализация: `values-ru-rRU/` для русского"

### #2 SubentityChip.kt [+]

- путь: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/SubentityChip.kt`
- единый виджет с `SubentityChipState { Placeholder, Active }`.
- Placeholder: `SuggestionChip` (icon `ic_add_value` tint=primary + label tint=primary). Размер иконки 16dp.
- Active: `InputChip(selected=true)` с `selectedContainerColor = primary`, label tint=onPrimary, trailing `IconBoxed(ic_close, tint=onPrimary, onClick=onDeactivate)`.
- 4 превью: Placeholder × {enabled, disabled}, Active × {enabled, disabled}.
- `onActivate` no-op в Active, `onDeactivate` no-op в Placeholder (call-site задаёт `{}`).

> 📎 guide: docs/guides/code-style.md — "Виджет: `*Widget.kt` — `TopBarWidget.kt`, `LexemeItemWidget.kt`"
>
> 📎 guide: docs/guides/ui-patterns.md — "Логически самостоятельный UI-элемент выносить в отдельный `*Widget.kt` — даже при однократном использовании"
>
> 📎 guide: docs/guides/state-modeling.md — "Зависимые фильтры / взаимоисключающие состояния → sum type → sealed class"
>
> 📎 guide: docs/guides/theme-and-resources.md — "Для параметризованных превью: `@PreviewParameter`"

### #4 LexemeValueFieldWidget.kt [~]

- путь: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeValueFieldWidget.kt`
- удалён параметр `titleRes` и Text-заголовок над chip-вью (заголовок переехал в chip-Active внутри `LexemeMeaningField`).
- сигнатура: `state, enabled, onTextChange, onOpenEditMode, onCommitEdit, onCancelEdit, onRemove, modifier`.
- view-mode: `InputChip(selected=true)` с trailing `IconBoxed(ic_circle_delete, onRemove)`.
- edit-mode: вынесенный `EditRow` с `BasicTextField` + `IconBoxed(ic_confirm)` + `IconBoxed(ic_close)`; `FocusRequester` + `LaunchedEffect` сохранены as-is.
- preview обновлён — без `titleRes`.

> 📎 guide: docs/guides/ui-patterns.md — "Внутренняя stateless composable (чистый рендеринг). Получает стейт и callback. Без ссылки на ViewModel"
>
> 📎 guide: docs/guides/ui-patterns.md — "`LaunchedEffect` — для state-triggered UI-эффектов (фокус, анимации)"
>
> 📎 guide: docs/guides/ui-patterns.md — "Фокус для текстовых полей: `val focusRequester = remember { FocusRequester() }`"

### #8 AddLexemeWidget.kt [~]

- путь: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/AddLexemeWidget.kt`
- M3 `Button` (inline label+icon) → M3 `FloatingActionButton` icon-only.
- DRIFT (фиксирован в design_tree): `enabled` через `Modifier.alpha(if (enabled) 1f else 0.38f)` + `onClick = { if (enabled) onAddLexeme() }` (no-op при disabled). FAB не имеет нативного `enabled` параметра.
- icon `ic_add_value` 24dp, `contentDescription = word_card_add_lexeme` (для a11y).
- 1 параметризованный preview (`@PreviewParameter(BoolParam::class)`).

> 📎 guide: docs/guides/ui-patterns.md — "Размер FAB | 56.dp", "Размер иконок | 24.dp (стандарт)"
>
> 📎 guide: docs/guides/theme-and-resources.md — "Для параметризованных превью: `@PreviewParameter(BoolParam::class)`"

### #5 LexemeMeaningField.kt [+]

- путь: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeMeaningField.kt`
- depends: #2, #4.
- `Column(verticalArrangement = spacedBy(4.dp))` + `SubentityChip(state=Active)` + `LexemeValueFieldWidget`.
- `onDeactivate` chip-Active маппится на `onCancelEdit` (по бизнес-контракту — reducer трактует Cancel свежесозданной субсущности как nullify).
- сигнатура: `labelRes, state, enabled, onValueChange, onOpenEditMode, onCommitEdit, onCancelEdit, onRemove, modifier`.
- 3 превью: view-mode, edit-mode, disabled.

> 📎 guide: docs/guides/ui-patterns.md — "Composable выносится в отдельный `*Widget.kt` если 2+ критериев: ... Принимает state-объект + callback'и"
>
> 📎 guide: docs/guides/code-style.md — "Виджет: `*Widget.kt`"

### #6 DeleteLexemeButton.kt [+]

- путь: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/DeleteLexemeButton.kt`
- depends: #0, #1.
- `TextButton` с `contentPadding = PaddingValues(4dp horizontal, 0dp vertical)`, `contentColor = onError`.
- `Icon(ic_trash, 16dp)` + `Spacer(width=4dp)` + `Text(word_card_lexeme_remove, style=LexemeStyle.BodyS)`.
- 1 параметризованный preview.

> 📎 guide: docs/guides/theme-and-resources.md — "Использование: `Text(text = \"Заголовок\", style = LexemeStyle.H2)`"
>
> 📎 guide: docs/guides/ui-patterns.md — "Размер иконок | 24.dp (стандарт), 12.dp (малый)"

### #7 LexemeItemWidget.kt [~]

- путь: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeItemWidget.kt`
- depends: #2, #5, #6.
- Структура переписана: одна `Surface(Card)` (RoundedCornerShape 12dp, shadow=4dp, color=whiteColor) → `Column(padding=16dp, spacing=12dp)`:
  1. `state.translation?.let { LexemeMeaningField(translation) }`
  2. `state.definition?.let { LexemeMeaningField(definition) }`
  3. `if (hasTranslationPlaceholder || hasDefinitionPlaceholder) FlowRow { SubentityChip(Placeholder) ... }`
  4. `DeleteLexemeButton` footer
- Удалены: `LexemeTitleWidget`, `LexemeChipPlaceholderWidget`, `HorizontalDivider`, импорты `MaterialTheme`, `R.string.word_card_bottom_*` (теперь через `LexemeMeaningField.labelRes`).
- `FlowRow` импортирован из `androidx.compose.foundation.layout.FlowRow` + `@OptIn(ExperimentalLayoutApi::class)`.
- `order: Int` сохранён в сигнатуре (как указано в design_tree — для future a11y/debug), помечен `@Suppress("UNUSED_PARAMETER")`.
- 3 превью: Translation+Definition, OnlyTranslation, Empty (only placeholders).

> 📎 guide: docs/guides/ui-patterns.md — "Горизонтальный padding экрана | 16.dp"

### #9 WordCardScreen.kt [~]

- путь: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/WordCardScreen.kt`
- depends: #7, #8.
- Добавлен слот `Scaffold.floatingActionButton = { if (loaded != null) AddLexemeWidget(...) }`.
- Удалены из основной `Column`: `Spacer(height=16dp)` + inline `AddLexemeWidget(modifier=fillMaxWidth)`.
- Внутренний `Column { state.lexemeList.forEachIndexed { ... key(it.id) { LexemeItemWidget(...) } } }` сохранён как обычный `Column` (LazyColumn — DRIFT, backlog).
- Снэкбар, диалог удаления, верхний бар, имэ-инсетс — без изменений.

> 📎 guide: docs/guides/ui-patterns.md — "Типичный скелет экрана: `Scaffold( topBar = ..., floatingActionButton = { PrimaryFabWidget(...) { sendMessage(Msg.AddLexeme) } }, ...)`"
>
> 📎 guide: docs/guides/ui-patterns.md — "`key()` для стабильной рекомпозиции"

### #10 LexemeTitleWidget.kt [-]

- удалён: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeTitleWidget.kt`.

### #11 AddTranslationLexemeMenuItem.kt [-]

- удалён: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/AddTranslationLexemeMenuItem.kt`.

### #12 AddDefinitionLexemeMenuItem.kt [-]

- удалён: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/AddDefinitionLexemeMenuItem.kt`.

### #13 DeleteLexemeMenuItem.kt [-]

- удалён: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/DeleteLexemeMenuItem.kt`.

### #3 LexemeChipPlaceholderWidget.kt [-]

- удалён: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeChipPlaceholderWidget.kt`.
- Решение по design_tree: **именно удалить** (а не переименовать), поскольку API расширен (Placeholder + Active state), и единственный потребитель (`LexemeItemWidget`) уже переведён на `SubentityChip`.

## Ит.2 (closing F001/F002/F003)

- **F001 closed** — `LexemeValueFieldWidget.EditRow`: `LaunchedEffect(value)` сохраняет старый cursor offset перед обновлением. `selection = TextRange(minOf(oldOffset, value.length))` — курсор больше не прыгает в конец при parent-трансформации ввода.
- **F002 closed** — `SubentityChip` Active-ветка: `InputChip(selected=true, onClick={})` → `Surface(shape=RoundedCornerShape(999.dp), color=primary, contentColor=onPrimary)` + `Row(padding=start=6/top=2/end=2/bottom=2, spacing=2)`. Тело chip'а больше не кликабельно (без ripple). ✕ остаётся через `IconBoxed` (стандартный clickable trailing). Label → `MaterialTheme.typography.labelSmall` (Body XS 11sp Medium per ui_layout). Pill `RoundedCornerShape(999.dp)` сохранён по project_decision `subentity_chip_pill`.
- **F003 closed** — `@file:OptIn(ExperimentalMaterial3Api::class)` удалён из обоих файлов:
  - `SubentityChip.kt` — больше не использует experimental API (`SuggestionChip`/`Surface` стабильны в M3 1.3.2).
  - `LexemeValueFieldWidget.kt` — `InputChip` в view-mode остался, но компилятор не требует OptIn (M3 1.3.2 `compileDebugKotlin` + `compileDebugUnitTestKotlin` оба зелёные, EXIT=0, ноль errors).
- Тесты ит.2: `:modules:screen:wordcard:testDebugUnitTest` — 113 PASS / 0 FAIL.
- F004 (`@Suppress("UNUSED_PARAMETER") order: Int`) — оставлен как rejected.

## Нетривиальные решения

1. **`order: Int` сохранён в `LexemeItemWidget`.** Design_tree предписывает оставить параметр в сигнатуре (future a11y / debug) и не показывать в UI. Чтобы не получить компилятор-warning об unused parameter — добавлен `@Suppress("UNUSED_PARAMETER")`. Альтернативой было бы удалить параметр и каскадно править WordCardScreen + previews, но design_tree однозначно говорит оставить.

2. **`onDeactivate` Active-chip = `onCancelEdit`.** В `LexemeMeaningField` ✕ chip-Active не имеет отдельного Msg — маппится на `Msg.CancelTranslationEdit/CancelDefinitionEdit` (см. design_tree #2 note). Reducer для свежесозданной субсущности (origin пустой) трактует Cancel как nullify — UX совпадает с описанием в ui_layout ("MeaningField уничтожается, chip возвращается в FlowRow").

> 📎 guide: docs/guides/messages.md — "`Cancel*` — закрыть локальный UI-элемент (диалог, bottom sheet). НЕ для закрытия экрана"

3. **DRIFT FAB enabled.** Зафиксирован в design_tree #8 — реализован как `Modifier.alpha(0.38f)` + no-op onClick. Реальный disabled FAB — отдельной задачей через core/ui обёртку (backlog).

> 📎 guide: docs/guides/ui-patterns.md — "Библиотека общих виджетов. Расположена в `modules/core/ui`. Паттерн base + themed wrapper"

4. **Imports cleanup в WordCardScreen.** Все импорты (`Arrangement`, `Spacer`, `height`, `fillMaxWidth`) остались валидны — внутренняя Column со списком лексем продолжает использовать `Arrangement.spacedBy` и `Spacer(height=8dp)` между WordFieldWidget и списком.

> 📎 guide: docs/guides/ui-patterns.md — "Spacing между элементами списка | 8.dp (`Arrangement.spacedBy`)"

## Тесты

- Запущено: `./gradlew :modules:screen:wordcard:testDebugUnitTest --console=plain`
- Результат: **PASS**
- Статистика (build/reports/tests/testDebugUnitTest/index.html):
  - tests: 113
  - failures: 0
  - ignored: 0
  - successRate: 100%
- Все 113 тестов прошли без изменений тестов (UI-код не затрагивает reducer/state контракты, которые тесты покрывают).
- Exit code = 0 → компиляция модуля `:modules:screen:wordcard` восстановлена.

## log_messages

- Восстановлена компиляция `:modules:screen:wordcard` после business sub-flow.
- Реализованы 14 узлов design_tree: 3 [+] (SubentityChip, LexemeMeaningField, DeleteLexemeButton), 5 [~] (strings × 2, LexemeValueFieldWidget, AddLexemeWidget, LexemeItemWidget, WordCardScreen), 6 [-] (LexemeTitleWidget, LexemeChipPlaceholderWidget, 3 menu-item, ничего лишнего).
- FAB вынесен в `Scaffold.floatingActionButton` per project_decision #4. Inline-кнопка + Spacer убраны из основной Column.
- LexemeItem переписан в единый Surface(Card) с порядком: ∀ active LexemeMeaningField → FlowRow placeholder SubentityChip → DeleteLexemeButton footer. LexemeTitleWidget удалён.
- DRIFT FAB `enabled` через alpha+no-op зафиксирован per design_tree #8.
- Тесты: 113 PASS / 0 FAIL / 100%. Тесты не правились (UI-changes не затрагивают reducer/state контракты).
- **Ит.2**: закрыты F001 (LaunchedEffect cursor preservation), F002 (SubentityChip Active → Surface+Row без ripple), F003 (@file:OptIn убран из обоих lexeme/* файлов). compileDebugKotlin + compileDebugUnitTestKotlin зелёные. Тесты ит.2: 113 PASS / 0 FAIL.
- **Ит.2 регресс-чек**: `:modules:screen:wordcard:compileDebugKotlin --rerun-tasks` EXIT=0 без OptIn — `InputChip` в `LexemeValueFieldWidget` оказался compatible без аннотации в M3 1.3.2 (либо stable, либо unused-symbol path).

_model: claude-opus-4-7[1m]_
