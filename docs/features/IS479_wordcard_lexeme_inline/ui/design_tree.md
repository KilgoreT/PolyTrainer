# design_tree — IS479 wordcard lexeme inline (UI)

Источники: `02_scope.md`, `business/summary.md` (контракты State/Msg/Reducer/DatasourceEffect уже синхронизированы business sub-flow), `ui/ui_layout.md` (финальный UI snapshot), текущий код виджетов `modules/screen/wordcard/.../widget/`.

> ℹ Business sub-flow ужé применил все изменения в `mate/*`, `WordCardViewModel.kt`, `app/.../WordCardUseCaseImpl.kt` и удалил `mate/UiEffectHandler.kt` + `widget/addlexeme/*` (см. `git status`). UI sub-flow восстанавливает компиляцию `:modules:screen:wordcard`, переводит виджеты на финальный layout по Figma `9154:82509` (FAB в Scaffold-слоте, единый Surface(Card) с FlowRow placeholder'ов + DeleteLexemeButton footer, двусостоятельный SubentityChip + LexemeMeaningField wrapper) и удаляет ⋮-menu-инфраструктуру лексемы.

> 📎 guide: docs/guides/ui-patterns.md — "Каждый экран состоит из двух composable-функций: публичная точка входа (DI) + внутренняя stateless composable (чистый рендеринг)"

> ⚠ **Не дублируем business-узлы.** `mate/State.kt`, `mate/Message.kt`, `mate/WordCardReducer.kt`, `mate/DatasourceEffectHandler.kt`, `WordCardViewModel.kt`, `app/.../WordCardUseCaseImpl.kt`, `mate/UiEffectHandler.kt [-]` — out of scope UI sub-flow (закрыты business). UI-call-sites (`WordCardScreen.kt`, `widget/ConfirmDeleteWordWidget.kt`, `widget/WordFieldWidget.kt`, виджеты лексемы) уже синхронизированы с новым Msg-набором — компиляция модуля сейчас не ломается. Меняется только визуальный layout + структура виджетов.

> ⚠ **Не удаляем `widget/addlexeme/AddLexemeBottomWidget.kt`, `ActionsWidget.kt`, `LexemeMeaningWidget.kt`** — этих файлов уже нет на диске (удалены до старта UI sub-flow вместе с реструктуризацией business). Аналогично `mate/UiEffectHandler.kt` — отсутствует.

> ⚠ **`PrimaryLongFabWidget.kt` НЕ удаляем — мёртвым кодом не стал.** Текущий `AddLexemeWidget.kt` использует Material3 `Button` напрямую (не `PrimaryLongFabWidget`). База `LexemeLongFab.kt` имеет второго потребителя — `core/ui/.../text/LexemeEditableText.kt`. Удалять `PrimaryLongFabWidget` в IS479 не на чем — wordcard-локальной wordcard-зависимости нет; вынос в backlog как cleanup-задача.

> 📎 guide: docs/guides/ui-patterns.md — "Три уровня виджетов: примитивы в core/ui/, модуль-виджеты в modules/widget/, экранные виджеты в screen/*/widget/ — зависимости строго послойно"

## Часть 1: Граф

```yaml
- id: 0
  file: core/core-resources/src/main/res/values/strings.xml
  action: "~"
  depends: []

- id: 1
  file: core/core-resources/src/main/res/values-ru-rRU/strings.xml
  action: "~"
  depends: []

- id: 2
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/SubentityChip.kt
  action: "+"
  depends: []

- id: 3
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeChipPlaceholderWidget.kt
  action: "-"
  depends: [2]

- id: 4
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeValueFieldWidget.kt
  action: "~"
  depends: []

- id: 5
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeMeaningField.kt
  action: "+"
  depends: [2, 4]

- id: 6
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/DeleteLexemeButton.kt
  action: "+"
  depends: [0, 1]

- id: 7
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeItemWidget.kt
  action: "~"
  depends: [2, 5, 6]

- id: 8
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/AddLexemeWidget.kt
  action: "~"
  depends: []

- id: 9
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/WordCardScreen.kt
  action: "~"
  depends: [7, 8]

- id: 10
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeTitleWidget.kt
  action: "-"
  depends: [7]

- id: 11
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/AddTranslationLexemeMenuItem.kt
  action: "-"
  depends: [10]

- id: 12
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/AddDefinitionLexemeMenuItem.kt
  action: "-"
  depends: [10]

- id: 13
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/DeleteLexemeMenuItem.kt
  action: "-"
  depends: [10]
```

### Параллелизм

- Группа 1 (корни): `#0`, `#1`, `#2`, `#4`, `#8` — независимы, параллельно.
- Группа 2: `#3` (удаление LexemeChipPlaceholderWidget) после `#2`; `#5` (LexemeMeaningField) после `#2` + `#4`; `#6` (DeleteLexemeButton) после `#0`+`#1` (новый стринг `word_card_lexeme_remove`).
- Группа 3: `#7` (LexemeItemWidget reshape) после `#2`+`#5`+`#6`.
- Группа 4: `#9` (WordCardScreen Scaffold-FAB) после `#7`+`#8`.
- Группа 5 (cleanup, серийно после `#7` уже не ссылается на LexemeTitle): `#10` (удаление LexemeTitleWidget) → параллельно `#11`/`#12`/`#13` (удаление трёх menu-item файлов; они импортировались только в LexemeTitleWidget).

DAG проверен: циклов нет (все зависимости от меньших id к большим).

## Часть 2: Детали изменений

### #0 strings.xml (en) [~]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-resources/src/main/res/values/strings.xml`

**Добавить:**
- `<string name="word_card_lexeme_remove">Remove</string>` — label DeleteLexemeButton.

> 📎 guide: docs/guides/theme-and-resources.md — "Общие строки → core/core-resources/src/main/res/values/strings.xml; именование <раздел>_<описание>"

Опционально (если в коде потребуется content-description для FAB icon-only) — `word_card_add_lexeme` уже существует; reuse как `contentDescription`.

Удалять старые ключи (`word_card_bottom_title`, `word_card_bottom_title_append`) НЕ нужно — они отсутствуют как потребители уже после удаления `AddLexemeBottomWidget`. Если ProGuard/build-time проверки покажут unused — отдельным cleanup-узлом в backlog.

---

### #1 strings.xml (ru) [~]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-resources/src/main/res/values-ru-rRU/strings.xml`

**Добавить:**
- `<string name="word_card_lexeme_remove">Удалить</string>` — label DeleteLexemeButton.

> 📎 guide: docs/guides/theme-and-resources.md — "Локализация: values-ru-rRU/ для русского"

---

### #2 SubentityChip.kt [+]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/SubentityChip.kt`

Новый виджет: единый chip с двумя визуальными state'ами (`Placeholder` | `Active`). Соответствует Figma `9154:82625` (placeholder) + `9154:82521` (active). Project decision `subentity_chip_single_widget`: один виджет в двух режимах, не два разных компонента. Project decision `subentity_chip_pill`: borderRadius=999 (pill) для ОБОИХ state'ов (стандартный M3-chip shape).

> 📎 guide: docs/guides/ui-patterns.md — "Только один экран → screen/*/widget/; экранный виджет — без state-management, props + callbacks"
>
> 📎 guide: docs/guides/code-style.md — "Виджет → *Widget.kt"
>
> 📎 guide: docs/guides/ui-patterns.md — "Логически самостоятельный UI-элемент выносить в отдельный *Widget.kt — даже при однократном использовании"

```kotlin
enum class SubentityChipState { Placeholder, Active }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubentityChip(
    @StringRes labelRes: Int,            // "Перевод" / "Определение"
    state: SubentityChipState,
    enabled: Boolean,
    onActivate: () -> Unit,              // Placeholder → CreateTranslation/CreateDefinition
    onDeactivate: () -> Unit,            // Active ✕ → ? (см. note ниже)
    modifier: Modifier = Modifier,
)
```

> 📎 guide: docs/guides/ui-patterns.md — "Отправка сообщений из UI: UI-события диспатчатся как сообщения через callback sendMessage"

Pseudocode:
- `Placeholder` → `SuggestionChip(onClick = onActivate, icon = ic_add_value tint=primary, label = stringResource(labelRes), enabled)`. По текущей реализации `LexemeChipPlaceholderWidget` — переносится 1:1.
- `Active` → `InputChip(selected = true, onClick = {}, label = stringResource(labelRes), trailingIcon = IconBoxed(ic_close, onClick = onDeactivate), enabled, colors = secondaryContainer ... onPrimary tint label)`.

**Важно про `onDeactivate`:** в `LexemeMeaningField` chip-заголовок (state=Active) с trailing ✕ означает «убрать активный edit-mode суб-сущности». В текущем Msg-наборе (business contract v3.2) НЕТ Msg для «закрыть chip без commit». Ближайшее — `CancelTranslationEdit(lexemeId)` / `CancelDefinitionEdit(lexemeId)`. Reducer уже трактует Cancel для свежесозданного chip (`origin.isEmpty()`) как nullify суб-сущности — что и нужно по UX. → `onDeactivate` маппится на `Msg.CancelTranslationEdit(lexemeId)` / `Msg.CancelDefinitionEdit(lexemeId)`.

> 📎 guide: docs/guides/messages.md — "Действия пользователя: императивный глагол, описывающий намерение (Show*, Hide*, Open*, Close*, Cancel*)"

Preview: `@PreviewWidget` × 4 (Placeholder×{enabled, disabled}, Active×{enabled, disabled}).

> 📎 guide: docs/guides/theme-and-resources.md — "Для виджетов: @PreviewWidget — два языка, с фоном"

---

### #3 LexemeChipPlaceholderWidget.kt [-]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeChipPlaceholderWidget.kt`

Удаляется. Заменён на `SubentityChip(state = SubentityChipState.Placeholder, ...)` (#2). Потребитель — только `LexemeItemWidget.kt` (#7) — переключается на новый виджет.

---

### #4 LexemeValueFieldWidget.kt [~]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeValueFieldWidget.kt`

**Было:** виджет с двумя режимами (view = `InputChip` + remove icon; edit = `BasicTextField` + commit/cancel icons). В viewmode рисует заголовок `Text(stringResource(titleRes))` внутри себя (titleRes = `word_card_bottom_translation` / `_definition`).

**Стало:** заголовок (title) уезжает в `LexemeMeaningField` (chip Active), здесь остаётся ТОЛЬКО value-вью. Виджет теряет параметр `titleRes`. Сигнатура:

```kotlin
@Composable
internal fun LexemeValueFieldWidget(
    state: TextValueState,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onOpenEditMode: () -> Unit,
    onCommitEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
)
```

> 📎 guide: docs/guides/state-and-extensions.md — "TextValueState — паттерн для toggle edit/view: origin (сохранённое) + edited (в процессе) + isEdit (режим)"

Pseudocode (тот же что и сейчас, минус `Text(stringResource(titleRes))` в Column):
- `if (state.isEdit) EditRow(...)` — BasicTextField + IconBoxed(commit) + IconBoxed(cancel).
- `else InputChip(selected=true, onClick=onOpenEditMode, label=state.origin, trailingIcon=IconBoxed(ic_circle_delete, onRemove))`.

Preview: обновить — убрать `titleRes` из вызовов.

**Замечание (ℹ ui_layout):** ui_layout `LexemeValueField` указывает `titleRes` как параметр и `content[0] = Text title`. Это **наследие** до выделения `LexemeMeaningField`. После #5 заголовок становится chip-Active внутри MeaningField. Решение принято по UX: дублировать title + chip = визуальный шум; chip-Active сам по себе несёт label. → `titleRes` удаляется из `LexemeValueFieldWidget`.

---

### #5 LexemeMeaningField.kt [+]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeMeaningField.kt`

Новый виджет: Column-обёртка над chip-заголовком (SubentityChip Active) и полем ввода (LexemeValueFieldWidget). Один MeaningField = одна активная суб-сущность (Translation ИЛИ Definition). Project decision `lexeme_meaning_field` — нет аналога в Figma, UX-решение проекта.

> 📎 guide: docs/guides/ui-patterns.md — "Только один экран → screen/*/widget/; экранный виджет — без state-management, props + callbacks"
>
> 📎 guide: docs/guides/ui-patterns.md — "Composable выносится в отдельный *Widget.kt если 2+ критериев: принимает state-объект + callback'и, представляет визуальный блок со своим стилем, имеет preview"

```kotlin
@Composable
internal fun LexemeMeaningField(
    @StringRes labelRes: Int,                // chip label: Translation / Definition
    state: TextValueState,                   // origin/edited/isEdit
    enabled: Boolean,                        // = !isPendingDbOp
    onValueChange: (String) -> Unit,
    onOpenEditMode: () -> Unit,
    onCommitEdit: () -> Unit,
    onCancelEdit: () -> Unit,                // также Deactivate chip ✕
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
)
```

Pseudocode:
```
Column(verticalArrangement = spacedBy(4.dp), modifier):
    SubentityChip(
        labelRes = labelRes,
        state = SubentityChipState.Active,
        enabled = enabled,
        onActivate = {},                     // no-op в Active
        onDeactivate = onCancelEdit,         // ✕ chip = cancel (см. note в #2)
    )
    LexemeValueFieldWidget(
        state = state,
        enabled = enabled,
        onTextChange = onValueChange,
        onOpenEditMode = onOpenEditMode,
        onCommitEdit = onCommitEdit,
        onCancelEdit = onCancelEdit,
        onRemove = onRemove,
    )
```

> 📎 guide: docs/guides/ui-patterns.md — "Spacing между элементами списка: 8.dp (Arrangement.spacedBy)"

Preview: `@PreviewWidget` × 3 (view-mode, edit-mode, disabled).

> 📎 guide: docs/guides/theme-and-resources.md — "Для виджетов: @PreviewWidget — два языка, с фоном"

---

### #6 DeleteLexemeButton.kt [+]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/DeleteLexemeButton.kt`

Новый виджет: footer-action внутри карточки лексемы (Figma `9162:40713`). Заменяет ⋮-меню удаления.

> 📎 guide: docs/guides/ui-patterns.md — "Только один экран → screen/*/widget/; экранный виджет — props + callbacks"

```kotlin
@Composable
internal fun DeleteLexemeButton(
    enabled: Boolean,                        // = !isPendingDbOp
    onClick: () -> Unit,                     // → Msg.RemoveLexeme(lexemeId)
    modifier: Modifier = Modifier,
)
```

> 📎 guide: docs/guides/messages.md — "Действия пользователя: Add* / Delete* — CRUD"

Pseudocode:
```
TextButton(
    onClick = onClick,
    enabled = enabled,
    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
    colors = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.onError,    // hex #DE2424
    ),
    modifier = modifier,
):
    Icon(
        painter = painterResource(R.drawable.ic_trash),
        contentDescription = null,
        modifier = Modifier.size(16.dp),
    )
    Spacer(width = 4)
    Text(
        text = stringResource(R.string.word_card_lexeme_remove),
        style = LexemeStyle.BodyS,
    )
```

> 📎 guide: docs/guides/theme-and-resources.md — "onError = Color(0xFFDE2424) — текст ошибки"
>
> 📎 guide: docs/guides/theme-and-resources.md — "Типографика: LexemeStyle.BodyS = TextStyle(fontSize = 12.sp)"

Иконка: фиксируем `R.drawable.ic_trash` (есть в `core/core-resources/src/main/res/drawable/ic_trash.xml`). Соответствует Figma `9162:40713` (tui-button с trash-icon). Альтернативы (`ic_circle_delete` / `ic_close`) не рассматриваются — решение принято на уровне design_tree, не оставляем выбор в impl.

> 📎 guide: docs/guides/theme-and-resources.md — "Все SVG-иконки как XML vectors в core/core-resources/src/main/res/drawable/"

Preview: `@PreviewWidget` × 2 (enabled, disabled).

> 📎 guide: docs/guides/theme-and-resources.md — "Для параметризованных превью: @PreviewWidget + @PreviewParameter(BoolParam::class) enabled: Boolean"

---

### #7 LexemeItemWidget.kt [~]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeItemWidget.kt`

**Было:** `Column { LexemeTitleWidget(...) ; Surface(Card) { Column { translation-block ; Divider? ; definition-block } } }`. Translation/Definition блоки — `if (state.translation != null) LexemeValueFieldWidget else LexemeChipPlaceholderWidget`. Заголовок-карточки + ⋮-меню добавляли translation/definition / удаляли лексему.

**Стало** (per ui_layout § Карта 🔍 LexemeItem):

```kotlin
@Composable
internal fun LexemeItemWidget(
    order: Int,                              // оставляем — может пригодиться для future a11y / debug; передаётся, но в UI не отображается (по ui_layout — заголовок "Value N" удалён)
    state: LexemeState,
    isPendingDbOp: Boolean,
    sendMessage: (Msg) -> Unit,
)
```

> 📎 guide: docs/guides/ui-patterns.md — "Внутренняя stateless composable: получает стейт и callback. Без ссылки на ViewModel. Без сайд-эффектов в теле. Все события идут через sendMessage"

Pseudocode:
```
Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
    color = whiteColor,
    shadowElevation = 4.dp,
):
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = spacedBy(12.dp)):

        // 1) Активные суб-сущности — сверху, по одной MeaningField на каждую
        state.translation?.let { t ->
            LexemeMeaningField(
                labelRes = R.string.word_card_bottom_translation,
                state = t,
                enabled = !isPendingDbOp,
                onValueChange = { sendMessage(Msg.UpdateTranslationInput(state.id, it)) },
                onOpenEditMode = { sendMessage(Msg.EnterTranslationEditMode(state.id)) },
                onCommitEdit = { sendMessage(Msg.CommitTranslationEdit(state.id)) },
                onCancelEdit = { sendMessage(Msg.CancelTranslationEdit(state.id)) },
                onRemove = { sendMessage(Msg.RemoveTranslation(state.id)) },
            )
        }
        state.definition?.let { d ->
            LexemeMeaningField(
                labelRes = R.string.word_card_bottom_definition,
                state = d,
                enabled = !isPendingDbOp,
                onValueChange = { sendMessage(Msg.UpdateDefinitionInput(state.id, it)) },
                onOpenEditMode = { sendMessage(Msg.EnterDefinitionEditMode(state.id)) },
                onCommitEdit = { sendMessage(Msg.CommitDefinitionEdit(state.id)) },
                onCancelEdit = { sendMessage(Msg.CancelDefinitionEdit(state.id)) },
                onRemove = { sendMessage(Msg.RemoveDefinition(state.id)) },
            )
        }

        // 2) Placeholder chip'ы (translation == null → +Перевод, definition == null → +Определение)
        val hasTranslationPlaceholder = state.translation == null
        val hasDefinitionPlaceholder = state.definition == null
        if (hasTranslationPlaceholder || hasDefinitionPlaceholder) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ):
                if (hasTranslationPlaceholder) {
                    SubentityChip(
                        labelRes = R.string.word_card_lexeme_add_translation,
                        state = SubentityChipState.Placeholder,
                        enabled = !isPendingDbOp,
                        onActivate = { sendMessage(Msg.CreateTranslation(state.id)) },
                        onDeactivate = {},                  // no-op в Placeholder
                    )
                }
                if (hasDefinitionPlaceholder) {
                    SubentityChip(
                        labelRes = R.string.word_card_lexeme_add_definition,
                        state = SubentityChipState.Placeholder,
                        enabled = !isPendingDbOp,
                        onActivate = { sendMessage(Msg.CreateDefinition(state.id)) },
                        onDeactivate = {},
                    )
                }
        }

        // 3) Footer — кнопка удалить лексему (alignSelf = Stretch — растягиваем строку
        //    через Modifier.fillMaxWidth() c text-button по центру/началу — детали в impl)
        DeleteLexemeButton(
            enabled = !isPendingDbOp,
            onClick = { sendMessage(Msg.RemoveLexeme(state.id)) },
        )
```

> 📎 guide: docs/guides/ui-patterns.md — "Клик кнопки → Сообщение через sendMessage; действие с элементом списка → Сообщение с ID"
>
> 📎 guide: docs/guides/state-and-extensions.md — "Computed properties для derived полей: hasTranslationPlaceholder из state.translation == null — оформлять как extension val на State, НЕ вычислять в composable"

Удаляется:
- импорт + вызов `LexemeTitleWidget(...)` (заголовок «Value N» + ⋮-меню).
- импорт `LexemeChipPlaceholderWidget` → переход на `SubentityChip(state = Placeholder)`.
- импорт `HorizontalDivider` + ветка `if (state.translation != null && state.definition != null) HorizontalDivider(...)` — divider больше не нужен (chip-заголовки SubentityChip Active визуально разделяют MeaningField'ы).
- старое использование `LexemeValueFieldWidget(titleRes = ...)` — параметр `titleRes` удалён (см. #4); label теперь у chip-заголовка через `LexemeMeaningField.labelRes`.

Поток `Msg`'ов не меняется относительно текущей реализации (business sub-flow уже привёл call-sites в соответствие contract_ui_msg v3.2).

Preview: обновить — убрать `LexemeTitleWidget` из preview-композиций; добавить preview с `state.translation = null, definition = null` (только placeholder'ы).

---

### #8 AddLexemeWidget.kt [~]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/AddLexemeWidget.kt`

**Было:** Inline-кнопка M3 `Button` (`containerColor = primary`, icon `ic_add_value` + Text `word_card_add_lexeme`), `Modifier.fillMaxWidth()` извне. Рисуется внутри основной Column карточки.

**Стало** (per ui_layout § AddLexeme + project_decision #4 `fab_scaffold_slot`): M3 `FloatingActionButton` icon-only, рисуется через слот `Scaffold.floatingActionButton` (см. #9).

> 📎 guide: docs/guides/ui-patterns.md — "Структура Scaffold: floatingActionButton слот — PrimaryFabWidget(iconRes = R.drawable.ic_add) { sendMessage(Msg.AddLexeme) }"
>
> 📎 guide: docs/guides/ui-patterns.md — "Размер FAB: 56.dp; Corner radius FAB: 16.dp"

```kotlin
@Composable
internal fun AddLexemeWidget(
    enabled: Boolean,
    onAddLexeme: () -> Unit,
    modifier: Modifier = Modifier,
)
```

Pseudocode:
```
FloatingActionButton(
    onClick = if (enabled) onAddLexeme else {},                // no-op when disabled
    modifier = modifier.alpha(if (enabled) 1f else 0.38f),     // визуальный disabled state
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    shape = FloatingActionButtonDefaults.shape,                // M3 default = RoundedCornerShape 16
):
    Icon(
        painter = painterResource(R.drawable.ic_add_value),
        contentDescription = stringResource(R.string.word_card_add_lexeme),
        modifier = Modifier.size(24.dp),
    )
```

> 📎 guide: docs/guides/theme-and-resources.md — "Размер иконок: 24.dp (стандарт)"

**DRIFT 🚨 (behavioral decision) — `enabled` через alpha+no-op:**

M3 `FloatingActionButton` не имеет нативного param `enabled`. Реализация через:
- `Modifier.alpha(if (enabled) 1f else 0.38f)` — визуальный disabled-state (M3 disabled opacity token = 0.38).
- `onClick = if (enabled) onAddLexeme else {}` — no-op при disabled.

Тривиальная семантическая эквивалентность стандартному disabled-state. Реальный disabled (с blocked focus, accessibility semantics `disabled`, отключение ripple) — можно добавить через кастомную FAB-обёртку в `core/ui` (backlog).

> 📎 guide: docs/guides/ui-patterns.md — "Библиотека общих виджетов: modules/core/ui. Паттерн base + themed wrapper (LexemeFab → PrimaryFabWidget)"

Импорт `Text` + `padding(start = 8.dp)` + Row-layout — удаляются (icon-only).

Preview: обновить — `@PreviewWidget × {enabled, disabled}`, поместить в Box с tertiary background, размер фиксированный (не fillMaxWidth — теперь FAB hug-content).

> 📎 guide: docs/guides/theme-and-resources.md — "Для параметризованных превью: @PreviewWidget + @PreviewParameter(BoolParam::class) enabled: Boolean"

---

### #9 WordCardScreen.kt [~]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/WordCardScreen.kt`

**Было:** Scaffold с topBar + snackbarHost. `floatingActionButton` слот НЕ используется. `AddLexemeWidget(Modifier.fillMaxWidth())` рисуется inline внутри основной Column карточки (после списка лексем + Spacer 16.dp). Условный рендер по `loaded != null`.

**Стало** (per ui_layout § Карта + § МЕНЯЕМ "WordCardScreen"):

> 📎 guide: docs/guides/ui-patterns.md — "Двухуровневый паттерн composable: публичная точка входа (DI) + внутренняя stateless composable"
>
> 📎 guide: docs/guides/ui-patterns.md — "Структура Scaffold: topBar + floatingActionButton + snackbarHost + containerColor + contentWindowInsets"

Изменения:
1. Добавить слот `floatingActionButton = { ... }` в Scaffold — только если `loaded != null` (FAB не имеет смысла без слова):
   ```
   floatingActionButton = {
       if (loaded != null) {
           AddLexemeWidget(
               enabled = !state.isPendingDbOp && !state.isCreatingLexeme,
               onAddLexeme = { sendMessage(Msg.CreateLexeme) },
           )
       }
   },
   ```

> 📎 guide: docs/guides/state-and-extensions.md — "Computed properties для derived полей: state.canAddLexeme из !isPendingDbOp && !isCreatingLexeme — оформлять как computed val на State, не вычислять в composable"

2. Удалить `Spacer(height = 16.dp)` + inline-вызов `AddLexemeWidget(Modifier.fillMaxWidth())` из основной Column.
3. Опционально: внутренний Column со списком лексем (`Column { state.lexemeList.forEach { ... } }`) сохраняется как есть (LazyColumn в ui_layout — концептуальный узел; реальная impl — вертикально scrollable обычный Column внутри `verticalScroll(rememberScrollState())`). По ui_layout "LazyColumn × N" — корректно либо переход на `LazyColumn` (recomposition optimization), либо сохранение текущего Column (упрощение). Решение: **сохранить Column** — список лексем редко >5, перформанс не критичен; LazyColumn потребовал бы убрать `verticalScroll(rememberScrollState())` на родительском Column (LazyColumn не работает внутри scrollable). Это backlog-cleanup. **DRIFT 🚨 ui_layout → impl**: LazyColumn остаётся декларативным.

> 📎 guide: docs/guides/ui-patterns.md — "Рендеринг списков: key() для стабильной рекомпозиции — state.list.forEach { key(it.id) { ItemWidget(...) } }"

Pseudocode (изменённая часть):
```kotlin
Scaffold(
    topBar = { TopBarWidget(...) },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    floatingActionButton = {
        if (loaded != null) {
            AddLexemeWidget(
                enabled = !state.isPendingDbOp && !state.isCreatingLexeme,
                onAddLexeme = { sendMessage(Msg.CreateLexeme) },
            )
        }
    },
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets(0,0,0,0),
) { paddingValue ->
    Box(modifier.fillMaxHeight().padding(paddingValue)...) {
        if (loaded != null) {
            Column(verticalScroll(rememberScrollState())):
                WordFieldWidget(loaded, isPendingDbOp, sendMessage)
                Spacer(8.dp)
                Column(verticalArrangement = spacedBy(8.dp)):
                    state.lexemeList.forEachIndexed { i, l -> key(l.id) { LexemeItemWidget(i+1, l, isPendingDbOp, sendMessage) } }
                // Spacer(16.dp) + AddLexemeWidget — УДАЛЕНЫ из основного Column.
        }
        if (loaded != null && loaded.showWarningDialog) {
            ConfirmDeleteWordWidget(loaded, sendMessage)
        }
    }
}
```

> 📎 guide: docs/guides/ui-patterns.md — "Условные оверлеи: диалоги и bottom sheet по флагам стейта — if (state.showWarningDialog) { ConfirmDeleteWordWidget(...) }"
>
> 📎 guide: docs/guides/ui-patterns.md — "AppBar — всегда отдельный виджет. AppBar никогда не пишется inline в Scaffold"

Замечание (a11y / ux): `Scaffold.floatingActionButton` авто-position = BottomEnd с учётом `WindowInsets.safeDrawing` через `Scaffold` — соответствует Figma `9154:82532` (правый-низ, padding по navigationBars). Дополнительной обёртки/padding не нужно.

Импорт `Spacer`, `height` — НЕ удалять (используется внутри основной Column между WordField и списком).

Preview: без изменений — preview уже параметризован `WordCardState`, FAB появится автоматически (loaded ≠ null).

> 📎 guide: docs/guides/theme-and-resources.md — "Для экранов: @PreviewScreen — system UI, Pixel 3, два языка"

---

### #10 LexemeTitleWidget.kt [-]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeTitleWidget.kt`

Удаляется. Заголовок «Value N» + ⋮-dropdown больше не нужны: Add-actions переехали в `<SubentityChip state=Placeholder>` внутри FlowRow LexemeItem; Delete-action — в `<DeleteLexemeButton>` footer. Никакой замены — концепция «строки заголовка с порядковым номером» исключена per ui_layout.

Потребитель — только `LexemeItemWidget.kt` (#7); после #7 импорт удаляется.

Импорты `me.apomazkin.icondropdowned.IconDropdownWidget` и `Msg.OpenLexemeMenu` (в reducer/state Msg остаётся неиспользуемым из UI — обработчик в reducer'е может быть удалён cleanup'ом в backlog; в IS479 не трогаем).

---

### #11 AddTranslationLexemeMenuItem.kt [-]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/AddTranslationLexemeMenuItem.kt`

Удаляется. Использовался только в `LexemeTitleWidget` (#10). Add-translation действие переехало в onClick chip-placeholder'а внутри FlowRow `<LexemeItem>`.

---

### #12 AddDefinitionLexemeMenuItem.kt [-]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/AddDefinitionLexemeMenuItem.kt`

Удаляется. Использовался только в `LexemeTitleWidget` (#10). Add-definition действие переехало в onClick chip-placeholder'а внутри FlowRow `<LexemeItem>`.

---

### #13 DeleteLexemeMenuItem.kt [-]

Полный путь: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/DeleteLexemeMenuItem.kt`

Удаляется. Использовался только в `LexemeTitleWidget` (#10). Delete-action переехал в `<DeleteLexemeButton>` (#6) footer.

---

## log_messages

- Собран `design_tree.md` UI sub-flow IS479 — 14 узлов (3 `[+]` create + 5 `[~]` modify + 6 `[-]` delete). Корни (параллельная стартовая группа): strings.xml × 2, SubentityChip [+], LexemeValueFieldWidget [~], AddLexemeWidget [~]. Финальные узлы: WordCardScreen [~] (FAB-slot), cleanup-цепочка LexemeTitleWidget + 3 menu-item [-].
- Зафиксирован out-of-scope блок: `mate/*` + `WordCardViewModel.kt` + `app/.../WordCardUseCaseImpl.kt` + `mate/UiEffectHandler.kt [-]` + удалённая директория `widget/addlexeme/*` — всё закрыто business sub-flow (см. `business/summary.md`), компиляция модуля не сломана. UI sub-flow меняет ТОЛЬКО визуальный layout и структуру виджетов.
- DRIFT 🚨 ui_layout → design_tree: `LazyColumn × N` в ui_layout сохранён как обычный `Column` (внутри `verticalScroll`). Backlog: переход на `LazyColumn` потребует убрать parent vertical scroll. `PrimaryLongFabWidget.kt` НЕ удаляется (не мёртвый код в текущем виде) — backlog cleanup-задача.
- DRIFT 🚨 ui_layout → design_tree: `LexemeValueFieldWidget.titleRes` удалён (label переехал в chip-Active заголовок SubentityChip внутри LexemeMeaningField — устраняет визуальный дубль title + chip).
- Итерация 2: закрыты approved minor findings F004 + F005. F004 → в #6 (DeleteLexemeButton) зафиксирован конкретный drawable `R.drawable.ic_trash`, убраны альтернативы. F005 → в #8 (AddLexemeWidget) `enabled` через `alpha(0.38f)` + no-op `onClick` оформлен как явный DRIFT/behavioral decision; реальный disabled — backlog (кастомная FAB-обёртка в core/ui).

_model: claude-opus-4-7[1m]_
