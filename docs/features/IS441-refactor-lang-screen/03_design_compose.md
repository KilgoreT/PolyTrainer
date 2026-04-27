# IS441. Задача 3 — Compose-компоненты

Маппинг UI-элементов на реальные Compose-компоненты. Переиспользуем существующие виджеты где возможно.

---

## Существующие виджеты (переиспользуем)

| Виджет | Где | Для чего |
|--------|-----|----------|
| `ImageFlagWidget` | `core/ui` | Отображение флага (24dp, круг) |
| `IconBoxed` | `core/ui` | Иконка удаления (🗑️), иконка назад |
| `PrimaryFullButtonWidget` | `core/ui` | Кнопка "Создать" / "Сохранить" / "Дальше" |
| `AlarmDialogWidget` | `core/ui` | Диалог подтверждения удаления (уже с Cancel + Alarm кнопками) |
| `LexemeTextFieldWidget` | `core/ui` | Поле ввода названия словаря |
| `LexemeBottomSheet` | `core/ui` | Заготовка bottom sheet (пустая, нужно доработать или написать свой) |
| `DictionaryAppBar` | `screen/dictionary` | AppBar с кнопкой назад (создан в задаче 2) |

---

## Переключение между списком и формой

```kotlin
AnimatedContent(targetState = state.screenMode) { mode ->
    when (mode) {
        ScreenMode.LIST -> DictionaryListContent(...)
        ScreenMode.FORM -> DictionaryFormContent(...)
    }
}
```

`ScreenMode` — enum в стейте: `LIST`, `FORM`. Переключается через Msg (тап на элемент → FORM, тап назад → LIST). Анимация — дефолтный `fadeIn/fadeOut` из `AnimatedContent`.

---

## Состояние 1: Список словарей

### Scaffold

```
Scaffold(
    topBar = { onBackPress?.let { DictionaryAppBar(it) } }
) { paddings -> ... }
```

Существующий `DictionaryAppBar` из задачи 2.

### Элемент списка словаря

```
Row(verticalAlignment = CenterVertically)
├── ImageFlagWidget(flagRes)          // существующий, 24dp круг
│   или Icon(ic_book)                 // дефолтная иконка если нет флага
├── Spacer(8.dp)
├── Column(weight = 1f)
│   ├── Text(name, LexemeStyle.BodyL) // название словаря
│   └── Text(lang, LexemeStyle.BodyS, grayTextColor) // язык, если привязан
├── IconBoxed(ic_trash, onClick)      // существующий, иконка удаления
```

Новый виджет: `DictionaryListItemWidget`.

### Список

```
LazyColumn
├── items(dictionaries) { DictionaryListItemWidget(it, onTap, onDelete) }
```

### Пустое состояние

```
Column(center)
├── Text("Нет словарей", LexemeStyle.H5)
├── Text("Создайте первый!", LexemeStyle.BodyL, grayTextColor)
```

### Кнопка "Новый словарь"

```
PrimaryFullButtonWidget(titleRes = R.string.new_dictionary, onClick)
```

Существующий виджет, как сейчас.

### Кнопка "Дальше" (только setup)

```
PrimaryFullButtonWidget(titleRes = R.string.continue_button, onClick)
```

Та же кнопка, другой текст. Показывается только в setup режиме, только когда есть хотя бы один словарь.

---

## Состояние 2: Форма создания/редактирования

### Поле "Название"

```
LexemeTextFieldWidget(
    value = state.name,
    onValueChange = { sendMsg(Msg.NameChanged(it)) },
    placeHolder = R.string.dictionary_name_hint,
)
```

Существующий виджет.

### Чекбокс "Привязать к языку"

```
Row(verticalAlignment = CenterVertically, clickable)
├── Checkbox(checked = state.isLanguageBound, onCheckedChange)
├── Spacer(8.dp)
├── Text("Привязать к языку", LexemeStyle.BodyM)
```

Стандартный Material3 `Checkbox`. Нет кастомного виджета — не нужен.

### Поле "Язык" (появляется при включённом чекбоксе)

```
OutlinedTextField(
    value = state.selectedLanguageName ?: "",
    readOnly = true,
    trailingIcon = { Icon(ic_arrow_down) },
    onClick = { sendMsg(Msg.OpenLanguagePicker) },
)
```

Или `Surface` + `Text` + `Icon` стилизованное как поле. Тап → открывает bottom sheet.

### Bottom sheet выбора языка

```
ModalBottomSheet(onDismissRequest)
├── Column(modifier = Modifier.imePadding())
│   ├── OutlinedTextField(search query, поле поиска, autoFocus)
│   ├── LazyColumn
│   │   └── items(filteredLanguages) {
│   │       Row(clickable)
│   │       ├── Text(language.displayName, LexemeStyle.BodyL)
│   │   }
```

`imePadding()` на контенте sheet'а — чтобы клавиатура не перекрывала список. `OutlinedTextField` получает фокус при открытии → клавиатура появляется сразу.

`LexemeBottomSheet` из `core/ui` — заготовка (пустая). Нужно либо доработать, либо написать новый компонент.

### Grid флагов (появляется после выбора языка)

```
LazyVerticalGrid(columns = GridCells.Fixed(5))
├── items(flags) { flag ->
│   Surface(
│       shape = CircleShape,
│       border = if (selected) BorderStroke(2.dp, primary) else null,
│       onClick = { sendMsg(Msg.FlagSelected(flag.numericCode)) }
│   ) {
│       ImageFlagWidget(flagRes = flag.flagRes)  // существующий, но нужен размер побольше
│   }
}
```

`ImageFlagWidget` — существующий (24dp). Для grid возможно нужен побольше (48dp). Можно передать `modifier = Modifier.size(48.dp)` — виджет принимает modifier.

### Кнопка "Создать" / "Сохранить"

```
PrimaryFullButtonWidget(
    titleRes = if (isEditing) R.string.save else R.string.create,
    enabled = state.name.isNotBlank(),
    onClick = { sendMsg(Msg.Save) }
)
```

---

## Диалог удаления

```
AlarmDialogWidget(
    alarmButtonText = R.string.button_delete,
    onAlarmClick = { sendMsg(Msg.ConfirmDelete(dictionaryId)) },
    onDismissRequest = { sendMsg(Msg.DismissDeleteDialog) },
) {
    Text("Удалить словарь?", LexemeStyle.H6, secondary)
    Spacer(8.dp)
    Text("Все слова, определения и результаты квизов будут удалены.", LexemeStyle.BodyL, grayTextColor)
}
```

Существующий `AlarmDialogWidget` — 1:1 паттерн с `ConfirmDeleteWordWidget` из WordCard.

---

## Новые виджеты (нужно создать)

| Виджет | Уровень | Описание |
|--------|---------|----------|
| `DictionaryListItemWidget` | screen/dictionary/widget | Элемент списка: флаг + название + язык + иконка удаления |
| `DictionaryFormWidget` | screen/dictionary/widget | Форма: название + чекбокс + язык + флаги + кнопка |
| `LanguagePickerBottomSheet` | screen/dictionary/widget | Bottom sheet с поиском и списком языков |
| `FlagGridWidget` | screen/dictionary/widget | Grid флагов стран для выбранного языка |
| `ConfirmDeleteDictionaryWidget` | screen/dictionary/widget | Диалог удаления (обёртка над AlarmDialogWidget) |
| `EmptyDictionaryWidget` | screen/dictionary/widget | Пустое состояние |

Все — экранный уровень (используются только в dictionary screen).

---

## Новые строковые ресурсы (нужно добавить)

**English** (`values/strings.xml`):
```xml
<string name="dictionary_name_hint">Dictionary name</string>
<string name="dictionary_bind_language">Bind to language</string>
<string name="dictionary_new">New dictionary</string>
<string name="dictionary_continue">Continue</string>
<string name="dictionary_delete_title">Delete dictionary?</string>
<string name="dictionary_delete_message">All words, definitions and quiz results will be deleted.</string>
<string name="dictionary_empty_title">No dictionaries</string>
<string name="dictionary_empty_subtitle">Create your first!</string>
<string name="dictionary_save">Save</string>
<string name="dictionary_create">Create</string>
```

**Russian** (`values-ru-rRU/strings.xml`):
```xml
<string name="dictionary_name_hint">Название словаря</string>
<string name="dictionary_bind_language">Привязать к языку</string>
<string name="dictionary_new">Новый словарь</string>
<string name="dictionary_continue">Далее</string>
<string name="dictionary_delete_title">Удалить словарь?</string>
<string name="dictionary_delete_message">Все слова, определения и результаты квизов будут удалены.</string>
<string name="dictionary_empty_title">Нет словарей</string>
<string name="dictionary_empty_subtitle">Создайте первый!</string>
<string name="dictionary_save">Сохранить</string>
<string name="dictionary_create">Создать</string>
```
