---
status: draft
---

# design_tree — IS479 UI sub-flow

Граф UI-файлов под inline-механику создания/редактирования лексемы (sealed `WordState`, новый набор `Msg`, удалён `AddLexemeBottomState`).

Корень: `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/`.

Цель — восстановить компиляцию `:modules:screen:wordcard` (compile-break принят business sub-flow как intermediate state, см. F076 в `business/summary.md`) и реализовать UI по Figma-frames `9154-82519/82521/82532/82625`.

## Часть 1: Граф

```yaml
- id: 0
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/addlexeme/AddLexemeBottomWidget.kt
  action: "-"
  depends: []

- id: 1
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/addlexeme/LexemeMeaningWidget.kt
  action: "-"
  depends: []

- id: 2
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/addlexeme/ActionsWidget.kt
  action: "-"
  depends: []

- id: 3
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/ConfirmDeleteWordWidget.kt
  action: "~"
  depends: []

- id: 4
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/WordFieldWidget.kt
  action: "~"
  depends: []

- id: 5
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/AddLexemeWidget.kt
  action: "~"
  depends: []

- id: 6
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeChipPlaceholderWidget.kt
  action: "+"
  depends: []

- id: 7
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeValueFieldWidget.kt
  action: "~"
  depends: []

- id: 8
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeTitleWidget.kt
  action: "~"
  depends: []

- id: 9
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/lexeme/LexemeItemWidget.kt
  action: "~"
  depends: [6, 7, 8]

- id: 10
  file: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/WordCardScreen.kt
  action: "~"
  depends: [0, 1, 2, 3, 4, 5, 9]
```

Параллельность:
- Узлы #0, #1, #2 (удаления) и #3, #4, #5, #6, #7, #8 (правка/создание независимых виджетов) — параллельны.
- #9 ждёт #6, #7, #8 (composite виджет лексемы).
- #10 ждёт всех остальных (корневой экран собирает всё).

## Часть 2: Детали изменений

### #0 `widget/addlexeme/AddLexemeBottomWidget.kt` [-]

Удаляется целиком: bottom-sheet flow заменён inline-механикой. Виджет ссылается на удалённый `AddLexemeBottomState`, использует `ModalBottomSheet`, `Msg.EnableTranslationCreation`/`EnableDefinitionCreation`/`CreateLexeme` в bottom-sheet-семантике (`Msg.CreateLexeme` теперь по тапу FAB, не из bottom sheet).

Единственный потребитель — `WordCardScreen.kt` (узел #10), там же удалится conditional render `if (state.addLexemeBottomState.show) { ... }`.

После удаления каталог `widget/addlexeme/` пустеет (вместе с #1, #2) — удалить и пустой пакет.

### #1 `widget/addlexeme/LexemeMeaningWidget.kt` [-]

Удаляется: переключатель «Перевод/Definition» с Checkbox использовался только внутри `AddLexemeBottomWidget` (узел #0). В новой механике на FAB-тап создаётся локальная `LexemeState(id = NOT_IN_DB, translation=null, definition=null)` без выбора типа — пользователь сам тапает chip `+ Перевод` или `+ Определение` (chip-placeholders, узел #6).

### #2 `widget/addlexeme/ActionsWidget.kt` [-]

Удаляется: пара кнопок «Cancel / Add» использовалась только в `AddLexemeBottomWidget` (узел #0). В inline-механике подтверждение/отмена живут на уровне chip-edit (`Commit*Edit` / `Cancel*Edit`, см. узел #7).

### #3 `ConfirmDeleteWordWidget.kt` [~]

**Было:**
```kotlin
internal fun ConfirmDeleteWordWidget(
    state: WordState,                    // sealed — не имеет .id / .showWarningDialog
    sendMessage: (Msg) -> Unit,
) {
    ...
    onAlarmClick = { sendMessage(Msg.RemoveWord(state.id)) }
}
```

**Стало:**
```kotlin
internal fun ConfirmDeleteWordWidget(
    loaded: WordState.Loaded,            // принимаем уже-приведённый Loaded
    sendMessage: (Msg) -> Unit,
) {
    AlarmDialogWidget(
        onAlarmClick = { sendMessage(Msg.RemoveWord(loaded.id)) },
        onDismissRequest = { sendMessage(Msg.CloseDeleteWordDialog) },
    ) {
        // тексты без изменений
    }
}
```

Cast `wordState as? WordState.Loaded` делается в `WordCardScreen.kt` (узел #10) перед инстанцированием диалога — виджет получает уже узкий тип, внутрь не пробрасывается `NotLoaded`-ветка. Аналогичный паттерн применён в `closeAllEditModes()` / ext-функциях `State.kt`.

### #4 `WordFieldWidget.kt` [~]

**Было:**
```kotlin
internal fun WordFieldWidget(
    wordState: WordState,                // sealed — .value/.edited/.isEditMode/.added только у Loaded
    sendMessage: (Msg) -> Unit,
) {
    ...
    LexemeEditableText(
        originValue = wordState.value,
        changedValue = wordState.edited,
        isEditMode = wordState.isEditMode,
        ...
    )
    wordState.added?.let { date -> ... }
}
```

**Стало:**
```kotlin
internal fun WordFieldWidget(
    loaded: WordState.Loaded,            // уже Loaded
    isPendingDbOp: Boolean,              // для блокировки edit-режима
    sendMessage: (Msg) -> Unit,
) {
    Surface(...) {
        LexemeEditableText(
            originValue = loaded.value,
            changedValue = loaded.edited,
            isEditMode = loaded.isEditMode,
            enabled = !isPendingDbOp,
            onOpenEditMode = { sendMessage(Msg.EnterWordEditMode) },
            onCloseEditMode = { sendMessage(Msg.CommitWordChanges) },
            // ExitWordEditMode + CommitWordChanges склеены в CommitWordChanges
            // (reducer сам делает disableWordEdit при commit; см. business/contract_ui_msg v3.2)
        )
        // блок даты: loaded.added всегда non-null в Loaded
        Row { Text(date) }
        ImageFlagWidget(...)
    }
}
```

Поле `added` в `WordState.Loaded` — non-nullable `Date` (см. `State.kt:38`), поэтому `?.let` не нужен.

### #5 `AddLexemeWidget.kt` [~]

Назначение перепроектируется: было FAB по `R.drawable.ic_add_value` → теперь inline-кнопка по Figma `9154-82532` (помещается в основном Column карточки, не в `floatingActionButton`-слоте Scaffold).

**Было:**
```kotlin
internal fun AddLexemeWidget(
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
    onAddLexeme: () -> Unit,
) {
    PrimaryLongFabWidget(
        iconRes = R.drawable.ic_add_value,
        titleRes = R.string.word_card_add_lexeme,
        enabled = enabled,
    ) { onAddLexeme.invoke() }
}
```

**Стало:**
```kotlin
internal fun AddLexemeWidget(
    enabled: Boolean,                    // = !isPendingDbOp && !isCreatingLexeme
    onAddLexeme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // inline-кнопка по 9154-82532:
    // полная ширина, контейнер MaterialTheme.colorScheme.primary,
    // иконка + текст "Добавить лексему".
    // Material3 Button или Surface + Row на ваш выбор (Figma-screenshot покажет точную форму).
    Button(
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        onClick = onAddLexeme,
    ) {
        Icon(painter = painterResource(R.drawable.ic_add_value), ...)
        Spacer(8.dp)
        Text(stringResource(R.string.word_card_add_lexeme))
    }
}
```

`PrimaryLongFabWidget` / `LexemeLongFab` после удаления потребителя становятся мёртвым кодом в `core/ui` — пометка для backlog (см. `02_scope.md § Затронутые файлы → Мёртвый код`); удаление **не** в скоупе IS479 (требует ревизии других модулей-консьюмеров — отдельная infra-задача).

`onAddLexeme` шлёт `Msg.CreateLexeme` (вместо удалённого `Msg.OpenAddLexemeDialog`).

### #6 `widget/lexeme/LexemeChipPlaceholderWidget.kt` [+]

Назначение: chip-кнопка-плейсхолдер `+ Перевод` / `+ Определение` (Figma `9154-82521` образец, `9154-82625` группа). Рендерится в `LexemeItemWidget`, когда `state.translation == null` или `state.definition == null` (lexeme частично заполнена либо локальная NOT_IN_DB).

Сигнатура:
```kotlin
@Composable
internal fun LexemeChipPlaceholderWidget(
    @StringRes labelRes: Int,            // R.string.word_card_lexeme_add_translation / _definition
    enabled: Boolean,                    // = !isPendingDbOp
    onClick: () -> Unit,                 // Msg.CreateTranslation(lexemeId) / Msg.CreateDefinition(lexemeId)
    modifier: Modifier = Modifier,
)
```

Реализация: Material3 `SuggestionChip` (прецедент — `modules/widget/chipPicker/.../ChipPickerWidget.kt:106`; ExperimentalMaterial3Api OptIn приемлем по проектной палитре). `leadingIcon` — `+`-иконка (`R.drawable.ic_add_value` или новая). Label — `stringResource(labelRes)`.

После тапа `onClick` шлёт `Msg.CreateTranslation(lexemeId)` либо `Msg.CreateDefinition(lexemeId)` — reducer создаёт `TextValueState(isEdit = true)` для соответствующего поля (см. `State.createLexemeTranslation` / `createLexemeDefinition`), сразу включая chip-edit (узел #7).

### #7 `widget/lexeme/LexemeValueFieldWidget.kt` [~]

Адаптируется под inline chip-edit: было «полноширинное поле + delete-иконка справа», стало «chip-стиль с встроенным редактором».

**Было:**
```kotlin
fun LexemeValueFieldWidget(
    state: TextValueState,
    @StringRes titleRes: Int,
    onTextChange: (String) -> Unit,
    onOpenEditMode: () -> Unit,
    onCloseEditMode: () -> Unit,         // дёргался по Msg.ExitTranslationEditMode/ExitDefinitionEditMode (УДАЛЕНЫ)
    onActionIconClick: () -> Unit,       // Msg.RemoveTranslation/RemoveDefinition
    modifier: Modifier = Modifier,
)
```

**Стало:**
```kotlin
internal fun LexemeValueFieldWidget(
    state: TextValueState,
    @StringRes titleRes: Int,
    enabled: Boolean,                    // = !isPendingDbOp
    onTextChange: (String) -> Unit,      // Msg.UpdateTranslationInput / UpdateDefinitionInput
    onOpenEditMode: () -> Unit,          // Msg.EnterTranslationEditMode / EnterDefinitionEditMode
    onCommitEdit: () -> Unit,            // Msg.CommitTranslationEdit / CommitDefinitionEdit (NEW)
    onCancelEdit: () -> Unit,            // Msg.CancelTranslationEdit / CancelDefinitionEdit (NEW)
    onRemove: () -> Unit,                // Msg.RemoveTranslation / RemoveDefinition
    modifier: Modifier = Modifier,
) {
    // chip-стиль по 9154-82521:
    // в view-режиме — InputChip(selected=true) с label = state.origin,
    //   trailingIcon = ic_circle_delete (тап → onRemove),
    //   onClick → onOpenEditMode
    // в edit-режиме — Row[ TextField(state.edited), IconButton(check, onCommitEdit), IconButton(close, onCancelEdit) ]
    //   (chip превращается в textfield, либо chip-shape сохраняется через Surface)
    if (state.isEdit) {
        // edit: текст-поле + commit/cancel actions
        TextField(value = state.edited, onValueChange = onTextChange, enabled = enabled)
        IconButton(onClick = onCommitEdit, enabled = enabled) { Icon(check) }
        IconButton(onClick = onCancelEdit, enabled = enabled) { Icon(close) }
    } else {
        InputChip(
            selected = true,
            enabled = enabled,
            onClick = onOpenEditMode,
            label = { Text(state.origin) },
            trailingIcon = { Icon(ic_circle_delete, onClick = onRemove) },
        )
    }
}
```

Замена `onCloseEditMode` (1 callback) на `onCommitEdit` + `onCancelEdit` (2 callbacks) — по новому Msg-набору. Семантика:
- `CommitEdit` (галочка) → отправка изменения в БД (handler делает atomic INSERT если `id == NOT_IN_DB`, иначе update).
- `CancelEdit` (крестик) → откат `edited → origin`, выход из edit-режима без БД-вызова.

### #8 `widget/lexeme/LexemeTitleWidget.kt` [~]

Косметика: уже корректно использует `Msg.OpenLexemeMenu(lexemeId, isShow)` и `Msg.RemoveLexeme(lexemeId)`. Изменения минимальные:

**Было:**
```kotlin
fun LexemeTitleWidget(
    order: Int,
    state: LexemeState,
    sendMessage: (Msg) -> Unit,
)
// state.isMenuOpen → triggers menu; menu items без enabled-guard
```

**Стало:**
```kotlin
internal fun LexemeTitleWidget(
    order: Int,
    state: LexemeState,
    isPendingDbOp: Boolean,              // блокирует menu-trigger
    sendMessage: (Msg) -> Unit,
) {
    // IconDropdownWidget с onClickDropDown disabled when isPendingDbOp
    // menu-items: AddTranslationLexemeMenuItem / AddDefinitionLexemeMenuItem / DeleteLexemeMenuItem — без правок
    // (см. AddTranslationLexemeMenuItem.kt, AddDefinitionLexemeMenuItem.kt, DeleteLexemeMenuItem.kt — без изменений)
}
```

Видимость dropdown-меню (`isMenuOpen`) уже находится в `LexemeState` (см. `State.kt:51`); меню-item «Удалить лексему» отправляет `Msg.RemoveLexeme(lexemeId)` (поле уже корректно). Пункты «Add Translation» / «Add Definition» доступны только если соответствующий suben null — текущая логика `state.translation ?: AddTranslationLexemeMenuItem { ... }` сохраняется.

**Tech debt пометка (F074 из `business/summary.md` § Tech debt):** клик `DeleteLexemeMenuItem` для NOT_IN_DB-лексемы должен показать ConfirmDialog (data-loss buffer). Не в скоупе IS479 — оставляем сразу `Msg.RemoveLexeme` без подтверждения.

### #9 `widget/lexeme/LexemeItemWidget.kt` [~]

Главная композитная единица лексемы. Адаптируется под:
- новый Msg-набор (`Commit*Edit` / `Cancel*Edit` вместо `Exit*EditMode`);
- chip-placeholders когда suben отсутствует (узел #6);
- chip-edit вместо полноширинного field (узел #7);
- `isPendingDbOp` пропагация в дочерние виджеты.

**Было:**
```kotlin
fun LexemeItemWidget(
    order: Int,
    state: LexemeState,
    sendMessage: (Msg) -> Unit,
) {
    LexemeTitleWidget(order, state, sendMessage)
    Surface { Column {
        state.translation?.let { tv -> LexemeValueFieldWidget(
            state = tv, titleRes = R.string.word_card_bottom_translation,
            onCloseEditMode = { sendMessage(Msg.ExitTranslationEditMode(state.id)) }, // УДАЛЁН
            ...
        ) }
        state.definition?.let { ... onCloseEditMode = ExitDefinitionEditMode } // УДАЛЁН
    } }
}
```

**Стало:**
```kotlin
internal fun LexemeItemWidget(
    order: Int,
    state: LexemeState,
    isPendingDbOp: Boolean,
    sendMessage: (Msg) -> Unit,
) {
    Column {
        LexemeTitleWidget(order, state, isPendingDbOp, sendMessage)
        Surface {
            Column {
                // TRANSLATION
                if (state.translation != null) {
                    LexemeValueFieldWidget(
                        state = state.translation,
                        titleRes = R.string.word_card_bottom_translation,
                        enabled = !isPendingDbOp,
                        onOpenEditMode = { sendMessage(Msg.EnterTranslationEditMode(state.id)) },
                        onTextChange   = { sendMessage(Msg.UpdateTranslationInput(state.id, it)) },
                        onCommitEdit   = { sendMessage(Msg.CommitTranslationEdit(state.id)) },
                        onCancelEdit   = { sendMessage(Msg.CancelTranslationEdit(state.id)) },
                        onRemove       = { sendMessage(Msg.RemoveTranslation(state.id)) },
                    )
                } else {
                    // chip-placeholder "+ Перевод" (9154-82521)
                    LexemeChipPlaceholderWidget(
                        labelRes = R.string.word_card_lexeme_add_translation,
                        enabled = !isPendingDbOp,
                        onClick = { sendMessage(Msg.CreateTranslation(state.id)) },
                    )
                }
                // DEFINITION — симметрично; разделитель только если оба suben присутствуют
                if (state.definition != null) {
                    if (state.translation != null) HorizontalDivider(...)
                    LexemeValueFieldWidget(
                        state = state.definition,
                        titleRes = R.string.word_card_bottom_definition,
                        enabled = !isPendingDbOp,
                        onCommitEdit = { sendMessage(Msg.CommitDefinitionEdit(state.id)) },
                        onCancelEdit = { sendMessage(Msg.CancelDefinitionEdit(state.id)) },
                        onRemove     = { sendMessage(Msg.RemoveDefinition(state.id)) },
                        ...
                    )
                } else {
                    LexemeChipPlaceholderWidget(
                        labelRes = R.string.word_card_lexeme_add_definition,
                        enabled = !isPendingDbOp,
                        onClick = { sendMessage(Msg.CreateDefinition(state.id)) },
                    )
                }
            }
        }
    }
}
```

**Важно:** если `translation == null && definition == null` (NOT_IN_DB сразу после `Msg.CreateLexeme` без последующего `Create*`), рендерим **оба** chip-placeholder'а. Это соответствует Figma `9154-82519` (только-что добавленная лексема без контента). Первый `CommitTranslationEdit` / `CommitDefinitionEdit` → handler делает atomic INSERT через `lexemeApi.addLexeme(wordId, ...ApiEntity)`, reducer заменяет NOT_IN_DB → real id (см. `business/summary.md § Ключевые решения § 1`).

### #10 `WordCardScreen.kt` [~]

Корневой composable. Изменения:
1. Удалить FAB-слот `Scaffold.floatingActionButton = { AddLexemeWidget(...) }` — `AddLexemeWidget` (узел #5) теперь inline-кнопка в основном Column (после lexeme-list, по Figma `9154-82532`).
2. Удалить conditional render `if (state.addLexemeBottomState.show) { AddLexemeBottomWidget(...) }` — `state.addLexemeBottomState` удалён.
3. Cast `state.wordState as? WordState.Loaded` для `WordFieldWidget` (узел #4) и `ConfirmDeleteWordWidget` (узел #3). `NotLoaded` → рендерим спиннер или ничего (см. ниже).
4. Заменить `UiMsg.ShowNotification(text = EMPTY_STRING, show = false)` в callback'е `SnackbarLaunchEffect.onResetState` на `Msg.DismissNotification` (`UiMsg` удалён, см. `contract_ui_msg.md v3.2`).
5. Передавать `isPendingDbOp` в дочерние виджеты (#4, #9, #5).

**Было (схематично):**
```kotlin
WordCardScreen(state, sendMessage) {
    Scaffold(
        topBar = { TopBarWidget(state.topBarState, ..., sendMessage) },
        floatingActionButton = {
            AddLexemeWidget(
                enabled = true,
                onAddLexeme = { sendMessage(Msg.OpenAddLexemeDialog) },  // УДАЛЁН Msg
                modifier = Modifier.navigationBarsPadding(),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValue ->
        Column {
            WordFieldWidget(wordState = state.wordState, sendMessage)    // wordState: WordState (sealed) — НЕ ИМЕЕТ .value
            state.lexemeList.forEachIndexed { i, l -> LexemeItemWidget(i+1, l, sendMessage) }
        }
        if (state.wordState.showWarningDialog) {                          // НЕ ИМЕЕТ .showWarningDialog на sealed
            ConfirmDeleteWordWidget(state.wordState, sendMessage)
        }
        if (state.addLexemeBottomState.show) {                            // ПОЛЕ УДАЛЕНО
            AddLexemeBottomWidget(state.addLexemeBottomState, ..., sendMessage)
        }
    }
    SnackbarLaunchEffect(state.snackbarState, snackbarHostState,
        onResetState = { sendMessage(UiMsg.ShowNotification(...)) })      // UiMsg УДАЛЁН
}
```

**Стало:**
```kotlin
internal fun WordCardScreen(state: WordCardState, sendMessage: (Msg) -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    SnackbarLaunchEffect(
        snackState = state.snackbarState,
        host = snackbarHostState,
        onResetState = { sendMessage(Msg.DismissNotification) }          // NEW
    )

    val loaded = state.wordState as? WordState.Loaded                    // одно приведение на frame

    Scaffold(
        topBar = { TopBarWidget(state.topBarState, { sendMessage(Msg.NavigateBack) }, sendMessage) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // floatingActionButton — УДАЛЁН
        ...
    ) { paddingValue ->
        Box(modifier = Modifier...padding(paddingValue)...imePadding()) {
            Column(verticalScroll = ..., padding = 16.dp) {
                when {
                    state.isLoading && loaded == null -> {
                        // optional: spinner / skeleton
                    }
                    loaded != null -> {
                        WordFieldWidget(loaded = loaded, isPendingDbOp = state.isPendingDbOp, sendMessage)
                        Spacer(8.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.lexemeList.forEachIndexed { i, lex ->
                                key(lex.id) {
                                    LexemeItemWidget(
                                        order = i + 1,
                                        state = lex,
                                        isPendingDbOp = state.isPendingDbOp,
                                        sendMessage = sendMessage,
                                    )
                                }
                            }
                        }
                        Spacer(16.dp)
                        // INLINE FAB — 9154-82532
                        AddLexemeWidget(
                            enabled = !state.isPendingDbOp && !state.isCreatingLexeme,
                            onAddLexeme = { sendMessage(Msg.CreateLexeme) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            // Delete-word confirm — только если loaded и флаг
            loaded?.takeIf { it.showWarningDialog }?.let { l ->
                ConfirmDeleteWordWidget(loaded = l, sendMessage = sendMessage)
            }
            // AddLexemeBottomWidget — БЛОК УДАЛЁН
        }
    }
}
```

Замечание про `isLoading`: business sub-flow оставил `isLoading: Boolean = true` в `WordCardState` (см. `State.kt:14`). До прихода `Msg.WordLoaded` рендерим спиннер либо пустой контейнер; после — `loaded != null` всегда true (если БД-find успешен) либо `WordNotFound` → переход через `NavigateBack` (handler-логика, не UI).

Computed `state.isCreatingLexeme` (см. `State.kt:24`) блокирует FAB пока в списке висит NOT_IN_DB-лексема — соответствует UX «нельзя создать вторую лексему, пока первая не закоммичена».

## Лог итераций

- **ит.1** (2026-05-21T18:07:46-0600) — первый драфт DAG. 11 узлов: 3 удаления (`AddLexemeBottomWidget`, `LexemeMeaningWidget`, `ActionsWidget`), 1 создание (`LexemeChipPlaceholderWidget`), 7 изменений (`ConfirmDeleteWordWidget`, `WordFieldWidget`, `AddLexemeWidget`, `LexemeValueFieldWidget`, `LexemeTitleWidget`, `LexemeItemWidget`, `WordCardScreen`). Корневая зависимость — `WordCardScreen` (#10), ждёт всех. Чтение производства подтвердило 5 файлов compile-break: `WordCardScreen`, `WordFieldWidget`, `ConfirmDeleteWordWidget`, `AddLexemeBottomWidget`, `LexemeItemWidget` (`Exit*EditMode` Msg удалены). `LexemeTitleWidget` уже корректно использует новый Msg-набор. Меню-items (`AddTranslationLexemeMenuItem`, `AddDefinitionLexemeMenuItem`, `DeleteLexemeMenuItem`) в графе отсутствуют — без правок. `SnackbarLaunchEffect.kt` тоже без правок — only call-site в `WordCardScreen` (узел #10) меняет `UiMsg.ShowNotification(false)` → `Msg.DismissNotification`.

- **ит.2** (2026-05-20T00:25:00-0600) — conductor-патч после review (3 critical + 3 minor):
  - **F-arch-1/2/3 critical (Preview compile-break):** ВСЕ узлы `[~]` в DAG ОБЯЗАНЫ обновить Preview-блоки (`@PreviewScreen`/`@PreviewWidget`) под новые сигнатуры/типы. Конкретно:
    - `WordCardScreen.kt:164-203` Preview — заменить `WordState(value="Word", added=Date())` на `WordState.Loaded(id=1L, added=Date(), value="Word")`. Удалить `import me.apomazkin.wordcard.mate.UiMsg` (узел #10).
    - `WordFieldWidget.kt:101-117` Preview — заменить `WordState(value="apple", added=Date())` на `WordState.Loaded(id=1L, added=Date(), value="apple")` либо передавать только value-параметры если виджет работает с уже-cast `Loaded`.
    - `ConfirmDeleteWordWidget.kt:50-58` Preview — заменить `WordState()` на `WordState.Loaded(id=1L, added=Date(), value="apple", showWarningDialog=true)`.
    - `LexemeTitleWidget.kt:95-99` Preview — добавить `isPendingDbOp = false` параметром.
    - `LexemeItemWidget.kt:110-126` Preview — то же.
    - `LexemeValueFieldWidget.kt:74-86` Preview — обновить onCommitEdit/onCancelEdit callback'и под новый contract.
  - **F-arch-4 minor (cancel-путь ExitWordEditMode):** Accept tech debt. `Msg.ExitWordEditMode` в контракте остаётся жив, но в текущем UI нет cancel-кнопки в word edit (back-нажатие закрывает экран целиком, не отменяет edit). Реализация в `LexemeEditableText.onCloseEditMode` → `CommitWordChanges` соответствует поведению «save on close». Cancel-trigger для word edit — backlog (отдельный UX-тикет).
  - **F-arch-5 minor (Figma frames):** Frames `82509/86012/86182/86353/86499` — варианты состояний (loading, error, success, focus), не отдельные виджеты. Покрываются: `82509` — loading overlay (`isLoading=true`), `86012/86182` — keyboard-focus состояния (handled by Compose `TextField`), `86353/86499` — empty state / single-lexeme variants (handled by `lexemeList.isEmpty()` / single-item rendering). Доп. узлов не требуется.
  - **F-arch-6 minor (divider rule):** Уточнение для `LexemeItemWidget`: `HorizontalDivider` рендерится **только** когда оба `translation` и `definition` имеют значение (real или NOT_IN_DB chip-placeholder с реальным контентом). Для свежего NOT_IN_DB-лексемы (оба null/placeholder) — без divider. По Figma `9154-82519` — два chip placeholder без разделителя.
