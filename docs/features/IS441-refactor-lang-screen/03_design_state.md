# IS441. Задача 3 — Стейт

## Текущий стейт (будет полностью заменён)

```kotlin
data class DictionaryState(
    val isLoading: Boolean = true,
    val needClose: Boolean = false,
    val dictionarySelectionState: DictionarySelectionState = DictionarySelectionState()
)

data class DictionarySelectionState(
    val dictionaryList: List<PresetDictionaryUi> = listOf(),
    val selectedNumericCode: Int? = null,
    val addDictionaryButtonEnable: Boolean = false,
)
```

Старый стейт заточен под выбор из 6 захардкоженных языков. Не подходит.

---

## Новый стейт

```kotlin
@Immutable
data class DictionaryState(
    val isLoading: Boolean = true,
    val needClose: Boolean = false,
    val screenMode: ScreenMode = ScreenMode.LIST,
    val listState: DictionaryListState = DictionaryListState(),
    val formState: DictionaryFormState = DictionaryFormState(),
    val deleteDialogState: DeleteDialogState = DeleteDialogState(),
    val languagePickerState: LanguagePickerState = LanguagePickerState(),
)
```

### ScreenMode

```kotlin
enum class ScreenMode {
    LIST,   // список словарей
    FORM,   // форма создания/редактирования
}
```

### DictionaryListState

```kotlin
@Immutable
data class DictionaryListState(
    val dictionaries: List<DictionaryListItem> = listOf(),
    val showContinueButton: Boolean = false,   // только в setup — кнопка "Далее"
)

@Immutable
data class DictionaryListItem(
    val id: Long,
    val name: String,
    val flagRes: Int? = null,
    val languageName: String? = null,
)
```

- `dictionaries` — загруженные словари для отображения
- `showContinueButton` — `true` в setup режиме после создания хотя бы одного словаря

### DictionaryFormState

```kotlin
@Immutable
data class DictionaryFormState(
    val editingDictionaryId: Long? = null,     // null = создание, не null = редактирование
    val name: String = "",
    val isLanguageBound: Boolean = false,       // чекбокс "Привязать к языку"
    val selectedLanguage: LanguageItem? = null,  // выбранный язык
    val availableFlags: List<CountryFlagItem> = listOf(), // флаги для выбранного языка
    val selectedFlag: CountryFlagItem? = null,   // выбранный флаг
    val saveButtonEnabled: Boolean = false,       // активна ли кнопка Создать/Сохранить
)
```

- `editingDictionaryId` — определяет режим: null → "Создать", не null → "Сохранить"
- `name` — текст в поле ввода
- `isLanguageBound` — показывать ли секции язык/флаг
- `selectedLanguage` — выбранный из bottom sheet
- `availableFlags` — загружаются после выбора языка
- `selectedFlag` — тап на флаг в grid
- `saveButtonEnabled` — `true` когда `name.isNotBlank()`

### DeleteDialogState

```kotlin
@Immutable
data class DeleteDialogState(
    val show: Boolean = false,
    val dictionaryId: Long = 0,
    val dictionaryName: String = "",
)
```

### LanguagePickerState

```kotlin
@Immutable
data class LanguagePickerState(
    val show: Boolean = false,
    val query: String = "",
    val languages: List<LanguageItem> = listOf(),        // полный список
    val filteredLanguages: List<LanguageItem> = listOf(), // отфильтрованный по query
)
```

- `show` — открыт ли bottom sheet
- `query` — текст поиска
- `languages` — загружается один раз при инициализации
- `filteredLanguages` — пересчитывается при изменении query

### Вспомогательные модели (не стейт, но используются в нём)

```kotlin
data class LanguageItem(
    val code: String,          // "es"
    val displayName: String,   // "Испанский"
)

data class CountryFlagItem(
    val numericCode: Int,
    val countryName: String,
    val flagRes: Int,
)
```

---

## Extension-функции

По конвенции — в `State.kt`, рядом со стейт-классами.

```kotlin
// Переключение режимов
fun DictionaryState.showList() = copy(screenMode = ScreenMode.LIST)
fun DictionaryState.showForm() = copy(screenMode = ScreenMode.FORM)

// Список
fun DictionaryState.updateDictionaries(list: List<DictionaryListItem>) = copy(
    listState = listState.copy(dictionaries = list)
)
fun DictionaryState.showContinue() = copy(
    listState = listState.copy(showContinueButton = true)
)

// Форма — создание
fun DictionaryState.openNewForm() = copy(
    screenMode = ScreenMode.FORM,
    formState = DictionaryFormState(),  // чистая форма
)

// Форма — редактирование
fun DictionaryState.openEditForm(item: DictionaryListItem) = copy(
    screenMode = ScreenMode.FORM,
    formState = DictionaryFormState(
        editingDictionaryId = item.id,
        name = item.name,
        isLanguageBound = item.flagRes != null,
        // selectedLanguage и selectedFlag загрузятся из эффекта
    ),
)

// Форма — изменения полей
fun DictionaryState.updateName(value: String) = copy(
    formState = formState.copy(
        name = value,
        saveButtonEnabled = value.isNotBlank(),
    )
)

fun DictionaryState.toggleLanguageBound() = copy(
    formState = formState.copy(
        isLanguageBound = !formState.isLanguageBound,
        selectedLanguage = if (formState.isLanguageBound) null else formState.selectedLanguage,
        availableFlags = if (formState.isLanguageBound) listOf() else formState.availableFlags,
        selectedFlag = if (formState.isLanguageBound) null else formState.selectedFlag,
    )
)

fun DictionaryState.selectLanguage(language: LanguageItem) = copy(
    formState = formState.copy(selectedLanguage = language),
    languagePickerState = languagePickerState.copy(show = false, query = ""),
)

fun DictionaryState.selectFlag(flag: CountryFlagItem) = copy(
    formState = formState.copy(selectedFlag = flag),
)

// Диалог удаления
fun DictionaryState.showDeleteDialog(id: Long, name: String) = copy(
    deleteDialogState = DeleteDialogState(show = true, dictionaryId = id, dictionaryName = name),
)
fun DictionaryState.hideDeleteDialog() = copy(
    deleteDialogState = DeleteDialogState(),
)

// Bottom sheet языков
fun DictionaryState.showLanguagePicker() = copy(
    languagePickerState = languagePickerState.copy(show = true),
)
fun DictionaryState.hideLanguagePicker() = copy(
    languagePickerState = languagePickerState.copy(show = false, query = ""),
)
fun DictionaryState.filterLanguages(query: String) = copy(
    languagePickerState = languagePickerState.copy(
        query = query,
        filteredLanguages = languagePickerState.languages.filter {
            it.displayName.contains(query, ignoreCase = true)
        },
    ),
)
```

---

## Связь стейт → UI

| Поле стейта | UI элемент |
|-------------|-----------|
| `screenMode` | `AnimatedContent` — LIST или FORM |
| `listState.dictionaries` | `LazyColumn` с `DictionaryListItemWidget` |
| `listState.showContinueButton` | Кнопка "Далее" — видимость |
| `formState.name` | `LexemeTextFieldWidget` — value |
| `formState.editingDictionaryId` | Текст кнопки "Создать" vs "Сохранить" |
| `formState.isLanguageBound` | `Checkbox` — checked |
| `formState.selectedLanguage` | Поле язык — отображаемый текст |
| `formState.availableFlags` | `FlagGridWidget` — данные |
| `formState.selectedFlag` | `FlagGridWidget` — выделение |
| `formState.saveButtonEnabled` | `PrimaryFullButtonWidget` — enabled |
| `deleteDialogState.show` | `ConfirmDeleteDictionaryWidget` — видимость |
| `languagePickerState.show` | `LanguagePickerBottomSheet` — видимость |
| `languagePickerState.filteredLanguages` | `LazyColumn` внутри bottom sheet |
| `languagePickerState.query` | Поле поиска в bottom sheet |
