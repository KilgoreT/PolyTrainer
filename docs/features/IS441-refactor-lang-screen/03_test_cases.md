# IS441. Задача 3 — Тест-кейсы

---

## Часть 1: Extension-функции стейта

Формат: extension → что меняет → что НЕ должно измениться → сигнатуры.

---

### ext/ScreenModeExtTest.kt

#### showList()

| Поле | До | После |
|------|----|-------|
| `screenMode` | FORM | **LIST** |
| `listState` | {dictionaries=[...]} | не меняется |
| `formState` | {name="Eng"} | не меняется |
| `deleteDialogState` | {} | не меняется |
| `languagePickerState` | {} | не меняется |

#### showForm()

| Поле | До | После |
|------|----|-------|
| `screenMode` | LIST | **FORM** |
| остальные | * | не меняются |

```kotlin
fun `should set screenMode to LIST when showList`()
fun `should set screenMode to FORM when showForm`()
fun `should preserve all other fields when showList`()
fun `should preserve all other fields when showForm`()
```

---

### ext/DictionaryListExtTest.kt

#### updateDictionaries(list)

| Поле | До | После |
|------|----|-------|
| `listState.dictionaries` | [] | **[item1, item2]** |
| `listState.showContinueButton` | false | не меняется |
| `screenMode` | LIST | не меняется |
| `formState` | {} | не меняется |

#### showContinue()

| Поле | До | После |
|------|----|-------|
| `listState.showContinueButton` | false | **true** |
| `listState.dictionaries` | [item1] | не меняется |
| `formState` | {} | не меняется |

```kotlin
fun `should set dictionaries when updateDictionaries`()
fun `should preserve other fields when updateDictionaries`()
fun `should set showContinueButton when showContinue`()
fun `should preserve dictionaries when showContinue`()
```

---

### ext/FormOpenExtTest.kt

#### openNewForm()

| Поле | До | После |
|------|----|-------|
| `screenMode` | LIST | **FORM** |
| `formState.editingDictionaryId` | * | **null** |
| `formState.name` | * | **""** |
| `formState.isLanguageBound` | * | **false** |
| `formState.selectedLanguage` | * | **null** |
| `formState.selectedFlag` | * | **null** |
| `formState.saveButtonEnabled` | * | **false** |
| `listState` | {dictionaries=[...]} | не меняе��ся |

#### openEditForm(item с флагом)

`item = DictionaryListItem(id=1, name="English", flagRes=123, languageName="English")`

| Поле | До | После |
|------|----|-------|
| `screenMode` | LIST | **FORM** |
| `formState.editingDictionaryId` | null | **1** |
| `formState.name` | "" | **"English"** |
| `formState.isLanguageBound` | false | **true** (flagRes != null) |
| `listState` | * | не меняется |

#### openEditForm(item без флага)

`item = DictionaryListItem(id=2, name="Биология", flagRes=null, languageName=null)`

| Поле | До | После |
|------|----|-------|
| `formState.editingDictionaryId` | null | **2** |
| `formState.name` | "" | **"Биология"** |
| `formState.isLanguageBound` | false | **false** (flagRes == null) |

```kotlin
fun `should clear form and switch to FORM when openNewForm`()
fun `should preserve listState when openNewForm`()
fun `should fill form from flagged item when openEditForm`()
fun `should fill form from no-flag item when openEditForm`()
fun `should preserve listState when openEditForm`()
```

---

### ext/FormFieldsExtTest.kt

#### updateName(value)

| value | name → | saveButtonEnabled → |
|-------|--------|---------------------|
| `"English"` | "English" | **true** |
| `""` | "" | **false** |
| `"   "` | "   " | **false** |

Не меняются: `editingDictionaryId`, `isLanguageBound`, `selectedLanguage`, `selectedFlag`

#### toggleLanguageBound()

**Включение** (was false):

| Поле | До | После |
|------|----|-------|
| `isLanguageBound` | false | **true** |
| `selectedLanguage` | null | не меняется (null) |
| `name` | "Eng" | не меняется |

**Выключение** (was true, с данными):

| Поле | До | После |
|------|----|-------|
| `isLanguageBound` | true | **false** |
| `selectedLanguage` | LanguageItem("es",...) | **null** |
| `availableFlags` | [Spain, Mexico] | **[]** |
| `selectedFlag` | Spain | **null** |
| `name` | "Eng" | не меняется |

#### selectLanguage(item)

`item = LanguageItem(code="es", displayName="Испанский")`

| Поле | До | После |
|------|----|-------|
| `formState.selectedLanguage` | null | **item** |
| `languagePickerState.show` | true | **false** |
| `languagePickerState.query` | "исп" | **""** |
| `formState.name` | "Eng" | не меняется |

#### selectFlag(item)

`item = CountryFlagItem(numericCode=724, countryName="Spain", flagRes=123)`

| Поле | До | После |
|------|----|-------|
| `formState.selectedFlag` | null | **item** |
| `formState.selectedLanguage` | LanguageItem | не меняется |
| `formState.name` | "Eng" | не меняется |

```kotlin
fun `should set name and enable save when updateName with text`()
fun `should disable save when updateName with empty`()
fun `should disable save when updateName with whitespace only`()
fun `should preserve other form fields when updateName`()
fun `should enable when toggleLanguageBound from disabled`()
fun `should disable and clear all when toggleLanguageBound from enabled`()
fun `should preserve name when toggleLanguageBound`()
fun `should select language and close picker when selectLanguage`()
fun `should preserve name when selectLanguage`()
fun `should set selectedFlag when selectFlag`()
fun `should preserve other fields when selectFlag`()
```

---

### ext/DeleteDialogExtTest.kt

#### showDeleteDialog(id=3, name="English")

| Поле | До | После |
|------|----|-------|
| `deleteDialogState.show` | false | **true** |
| `deleteDialogState.dictionaryId` | 0 | **3** |
| `deleteDialogState.dictionaryName` | "" | **"English"** |
| `listState` | * | не меняется |
| `formState` | * | не меняется |

#### hideDeleteDialog()

| Поле | До | После |
|------|----|-------|
| `deleteDialogState` | {show=true, id=3, name="English"} | **{show=false, id=0, name=""}** |
| `listState` | * | не меняется |

```kotlin
fun `should show dialog with data when showDeleteDialog`()
fun `should preserve other fields when showDeleteDialog`()
fun `should reset dialog when hideDeleteDialog`()
fun `should preserve other fields when hideDeleteDialog`()
```

---

### ext/LanguagePickerExtTest.kt

#### showLanguagePicker()

| Поле | До | После |
|------|----|-------|
| `languagePickerState.show` | false | **true** |
| `languagePickerState.languages` | [en, es, fr] | не меняется |

#### hideLanguagePicker()

| Поле | До | После |
|------|----|-------|
| `languagePickerState.show` | true | **false** |
| `languagePickerState.query` | "исп" | **""** |
| `languagePickerState.languages` | [en, es, fr] | не меняется |

#### filterLanguages(query)

Начальный `languages`: `[LanguageItem("en","English"), LanguageItem("es","Испанский"), LanguageItem("fr","Français")]`

| query | filteredLanguages → | query → |
|-------|---------------------|---------|
| `"исп"` | **[Испанский]** | "исп" |
| `""` | **[все 3]** | "" |
| `"xyz"` | **[]** | "xyz" |
| `"eng"` | **[English]** (case-insensitive) | "eng" |

Не меняется: `show`, `languages` (полный список)

```kotlin
fun `should show picker when showLanguagePicker`()
fun `should preserve languages when showLanguagePicker`()
fun `should hide and clear query when hideLanguagePicker`()
fun `should preserve languages when hideLanguagePicker`()
fun `should filter by query when filterLanguages`()
fun `should return all when filterLanguages with empty query`()
fun `should return empty when filterLanguages with no match`()
fun `should preserve show state when filterLanguages`()
```

---

## Часть 2: Reducer — обработка сообщений

Формат: `Msg` → структура сообщения → варианты данных → тесты.

---

### reducer/NavigationTest.kt

#### Msg.OpenNewForm

```kotlin
data object OpenNewForm : Msg
```

Нет данных. Всегда одинаковое поведение.

| # | Кейс | Начальный стейт | Ожидание (state) | Ожидание (effects) |
|---|------|----------------|------------------|-------------------|
| 1 | Standard | screenMode=LIST, form с данными | screenMode=FORM, form очищена (editingId=null, name="", isLanguageBound=false) | нет |
| 2 | Standard | screenMode=LIST | listState не изменился | нет |

```kotlin
fun `should switch to FORM and clear form when OpenNewForm`()
fun `should preserve listState when OpenNewForm`()
```

#### Msg.OpenEditForm

```kotlin
data class OpenEditForm(val item: DictionaryListItem) : Msg
```

Варианты `item`:
- С флагом: `DictionaryListItem(id=1, name="English", flagRes=123, languageName="English")`
- Без флага: `DictionaryListItem(id=2, name="Биология", flagRes=null, languageName=null)`

| # | Кейс | item | Ожидание (state) | Effects |
|---|------|------|------------------|---------|
| 3 | Standard: с флагом | flagRes=123 | screenMode=FORM, editingId=1, name="English", isLanguageBound=true | нет (или LoadFlagsForLanguage) |
| 4 | Standard: без флага | flagRes=null | screenMode=FORM, editingId=2, name="Биология", isLanguageBound=false | нет |
| 5 | Standard | любой | listState не изменился | нет |

```kotlin
fun `should fill form with flag data when OpenEditForm with flagged item`()
fun `should fill form without language when OpenEditForm with no flag item`()
fun `should preserve listState when OpenEditForm`()
```

#### Msg.BackToList

```kotlin
data object BackToList : Msg
```

| # | Кейс | Начальный стейт | Ожидание | Effects |
|---|------|----------------|----------|---------|
| 6 | Standard | screenMode=FORM, form заполнена | screenMode=LIST, form сброшена | нет |
| 7 | Standard | любой | listState, deleteDialogState не изменились | нет |

```kotlin
fun `should switch to LIST and clear form when BackToList`()
fun `should preserve listState when BackToList`()
```

#### Msg.Continue

```kotlin
data object Continue : Msg
```

| # | Кейс | Начальный стейт | Ожидание | Effects |
|---|------|----------------|----------|---------|
| 8 | Standard | список из 2 словарей | needClose=true | SetCurrentDictionary(lastCreatedId) |
| 9 | Edge | список из 1 словаря | needClose=true | SetCurrentDictionary(id) |

```kotlin
fun `should set needClose and trigger SetCurrentDictionary when Continue`()
fun `should use first dictionary id when Continue with single dictionary`()
```

---

### reducer/FormActionsTest.kt

#### Msg.NameChanged

```kotlin
data class NameChanged(val value: String) : Msg
```

Варианты `value`:
- `"English"` — обычный текст
- `""` — пустая строка
- `"   "` — только пробелы
- `"A"` — минимальный

| # | Кейс | value | Ожидание (formState) | Effects |
|---|------|-------|---------------------|---------|
| 1 | Standard | "English" | name="English", saveButtonEnabled=true | нет |
| 2 | Boundary | "" | name="", saveButtonEnabled=false | нет |
| 3 | Edge | "   " | name="   ", saveButtonEnabled=false | нет |
| 4 | Standard | любой | editingDictionaryId, isLanguageBound не изменились | нет |

```kotlin
fun `should set name and enable save when NameChanged with text`()
fun `should disable save when NameChanged with empty`()
fun `should disable save when NameChanged with whitespace only`()
fun `should preserve other form fields when NameChanged`()
```

#### Msg.ToggleLanguageBound

```kotlin
data object ToggleLanguageBound : Msg
```

| # | Кейс | Начальный formState | Ожидание | Effects |
|---|------|--------------------|---------|---------| 
| 5 | Standard: включение | isLanguageBound=false | isLanguageBound=true | нет |
| 6 | Standard: выключение | isLanguageBound=true, selectedLanguage!=null, selectedFlag!=null | isLanguageBound=false, selectedLanguage=null, availableFlags=[], selectedFlag=null | нет |
| 7 | Standard | любой | name не изменился | нет |

```kotlin
fun `should enable language bound when ToggleLanguageBound from disabled`()
fun `should disable and clear language and flags when ToggleLanguageBound from enabled`()
fun `should preserve name when ToggleLanguageBound`()
```

#### Msg.OpenLanguagePicker / CloseLanguagePicker

```kotlin
data object OpenLanguagePicker : Msg
data object CloseLanguagePicker : Msg
```

| # | Кейс | Msg | Ожидание (languagePickerState) | Effects |
|---|------|-----|-------------------------------|---------|
| 8 | Standard | OpenLanguagePicker | show=true | нет |
| 9 | Standard | CloseLanguagePicker | show=false, query="" | нет |

```kotlin
fun `should show picker when OpenLanguagePicker`()
fun `should hide picker and clear query when CloseLanguagePicker`()
```

#### Msg.LanguageQueryChanged

```kotlin
data class LanguageQueryChanged(val query: String) : Msg
```

Варианты `query`:
- `"исп"` — частичное совпадение
- `""` — пустой (все языки)
- `"xyz"` — нет совпадений

Начальный `languages`: `[LanguageItem("en","English"), LanguageItem("es","Испанский"), LanguageItem("fr","Français")]`

| # | Кейс | query | Ожидание (filteredLanguages) | Effects |
|---|------|-------|-----------------------------|---------|
| 10 | Standard | "исп" | [Испанский] | нет |
| 11 | Boundary | "" | [все 3] | нет |
| 12 | Edge | "xyz" | [] | нет |

```kotlin
fun `should filter languages by query when LanguageQueryChanged`()
fun `should show all languages when LanguageQueryChanged with empty`()
fun `should show empty when LanguageQueryChanged with no match`()
```

#### Msg.SelectLanguage

```kotlin
data class SelectLanguage(val item: LanguageItem) : Msg
```

`item`: `LanguageItem(code="es", displayName="Испанский")`

| # | Кейс | Ожидание | Effects |
|---|------|----------|---------|
| 13 | Standard | formState.selectedLanguage=item, picker closed, query="" | LoadFlagsForLanguage("es") |
| 14 | Standard | name, editingDictionaryId не изменились | — |

```kotlin
fun `should select language close picker and trigger LoadFlags when SelectLanguage`()
fun `should preserve form name when SelectLanguage`()
```

#### Msg.SelectFlag

```kotlin
data class SelectFlag(val item: CountryFlagItem) : Msg
```

`item`: `CountryFlagItem(numericCode=724, countryName="Spain", flagRes=R.drawable.flag_es)`

| # | Кейс | Ожидание | Effects |
|---|------|----------|---------|
| 15 | Standard | formState.selectedFlag=item | нет |
| 16 | Standard | selectedLanguage, name не изменились | нет |

```kotlin
fun `should set selectedFlag when SelectFlag`()
fun `should preserve selectedLanguage when SelectFlag`()
```

#### Msg.Save

```kotlin
data object Save : Msg
```

| # | Кейс | Начальный formState | Ожидание | Effects |
|---|------|--------------------|---------|---------| 
| 17 | Standard: создание | editingId=null, name="Bio", selectedFlag=null | — | SaveDictionary(name="Bio", numericCode=null) |
| 18 | Standard: создание с флагом | editingId=null, name="Eng", selectedFlag(724) | — | SaveDictionary(name="Eng", numericCode=724) |
| 19 | Standard: редактирование | editingId=5, name="Updated", selectedFlag(724) | — | UpdateDictionary(id=5, name="Updated", numericCode=724) |
| 20 | Edge: редактирование со снятым языком | editingId=5, name="Bio", isLanguageBound=false | — | UpdateDictionary(id=5, name="Bio", numericCode=null) |

```kotlin
fun `should trigger SaveDictionary without flag when Save in create mode`()
fun `should trigger SaveDictionary with flag when Save in create mode with flag`()
fun `should trigger UpdateDictionary when Save in edit mode`()
fun `should trigger UpdateDictionary with null numericCode when language unbound`()
```

---

### reducer/DeleteFlowTest.kt

#### Msg.RequestDelete

```kotlin
data class RequestDelete(val id: Long, val name: String) : Msg
```

`id=3, name="English"`

| # | Кейс | Ожидание | Effects |
|---|------|----------|---------|
| 1 | Standard | deleteDialogState: show=true, id=3, name="English" | нет |
| 2 | Standard | listState не изменился | нет |

```kotlin
fun `should show delete dialog with id and name when RequestDelete`()
fun `should preserve listState when RequestDelete`()
```

#### Msg.ConfirmDelete

```kotlin
data object ConfirmDelete : Msg
```

Начальный: `deleteDialogState(show=true, dictionaryId=3)`

| # | Кейс | Ожидание | Effects |
|---|------|----------|---------|
| 3 | Standard | deleteDialogState сброшен | DeleteDictionary(id=3) |

```kotlin
fun `should hide dialog and trigger DeleteDictionary when ConfirmDelete`()
```

#### Msg.DismissDelete

```kotlin
data object DismissDelete : Msg
```

| # | Кейс | Ожидание | Effects |
|---|------|----------|---------|
| 4 | Standard | deleteDialogState сброшен | нет |

```kotlin
fun `should hide dialog when DismissDelete`()
```

#### Msg.DictionaryDeleted

```kotlin
data object DictionaryDeleted : Msg
```

| # | Кейс | Начальный listState | Ожидание | Effects |
|---|------|--------------------|---------|---------| 
| 5 | Standard | 2 словаря | — | LoadDictionaries |
| 6 | Edge | 1 словарь (стал 0 после удаления) | showContinueButton=false | LoadDictionaries |

```kotlin
fun `should trigger LoadDictionaries when DictionaryDeleted`()
fun `should hide continue button when DictionaryDeleted results in empty list`()
```

---

### reducer/DataLoadingTest.kt

#### Msg.DictionariesLoaded

```kotlin
data class DictionariesLoaded(val list: List<DictionaryListItem>) : Msg
```

Варианты `list`:
- 2 элемента: `[DictionaryListItem(1,"English",flagRes=123), DictionaryListItem(2,"Bio",flagRes=null)]`
- Пустой: `[]`

| # | Кейс | list | Ожидание | Effects |
|---|------|------|----------|---------|
| 1 | Standard | 2 элемента | dictionaries=list, isLoading=false | нет |
| 2 | Boundary | [] | dictionaries=[], isLoading=false | нет |
| 3 | Standard | любой | formState, screenMode не изменились | нет |

```kotlin
fun `should update dictionaries and stop loading when DictionariesLoaded`()
fun `should handle empty list when DictionariesLoaded`()
fun `should preserve formState when DictionariesLoaded`()
```

#### Msg.LanguagesLoaded

```kotlin
data class LanguagesLoaded(val list: List<LanguageItem>) : Msg
```

| # | Кейс | Ожидание | Effects |
|---|------|----------|---------|
| 4 | Standard | languages=list, filteredLanguages=list | нет |

```kotlin
fun `should set languages and filteredLanguages when LanguagesLoaded`()
```

#### Msg.FlagsLoaded

```kotlin
data class FlagsLoaded(val list: List<CountryFlagItem>) : Msg
```

Варианты:
- 5 флагов: `[Spain, Mexico, Argentina, Colombia, Chile]`
- 1 флаг: `[Japan]`

| # | Кейс | list | Ожидание | Effects |
|---|------|------|----------|---------|
| 5 | Standard: несколько | 5 флагов | availableFlags=list, selectedFlag=null | нет |
| 6 | Standard: один | 1 флаг | availableFlags=list, selectedFlag=Japan (автовыбор) | нет |

```kotlin
fun `should set availableFlags when FlagsLoaded with multiple`()
fun `should auto-select flag when FlagsLoaded with single item`()
```

#### Msg.DictionarySaved

```kotlin
data object DictionarySaved : Msg
```

| # | Кейс | Ожидание | Effects |
|---|------|----------|---------|
| 7 | Standard | screenMode=LIST | LoadDictionaries |

```kotlin
fun `should switch to LIST and trigger LoadDictionaries when DictionarySaved`()
```

---

## Сводка

| Файл | Кейсов | Тестовых функций |
|------|--------|------------------|
| ext/ScreenModeExtTest.kt | 4 | 6 |
| ext/DictionaryListExtTest.kt | 6 | 7 |
| ext/FormOpenExtTest.kt | 11 | 11 |
| ext/FormFieldsExtTest.kt | 16 | 18 |
| ext/DeleteDialogExtTest.kt | 5 | 6 |
| ext/LanguagePickerExtTest.kt | 9 | 10 |
| reducer/NavigationTest.kt | 4 | 4 |
| reducer/FormActionsTest.kt | 9 | 9 |
| reducer/DeleteFlowTest.kt | 5 | 5 |
| reducer/DataLoadingTest.kt | 6 | 6 |
| **Итого** | **75** | **82** |
