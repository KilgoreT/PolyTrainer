# Test Plan

Тесты написаны по контракту и спеке, до реализации. Могут не компилироваться — реализация адаптируется к тестам.

---

## Extension тесты

Файл: `modules/screen/dictionary/src/test/java/me/apomazkin/dictionary/form/ext/FormFieldsExtTest.kt` (перезаписывается полностью — старые extensions удалены)

Тестовые данные:

```kotlin
private val spainFlag = CountryFlagItem(
    numericCode = 724, countryName = "Spain", flagRes = 100,
    languages = listOf("Spanish", "Catalan")
)
private val mexicoFlag = CountryFlagItem(
    numericCode = 484, countryName = "Mexico", flagRes = 101,
    languages = listOf("Spanish")
)
private val flags = listOf(spainFlag, mexicoFlag)
```

---

### updateName(value)

#### Таблица "до -> после"

| value | name -> | saveButtonEnabled -> |
|-------|---------|----------------------|
| `"English"` | "English" | **true** |
| `""` | "" | **false** |
| `"   "` | "   " | **false** |

Не меняются: `editingDictionaryId`, `flagFilter`, `flags`, `selectedFlag`

#### Тест-кейсы

| # | Тип | Описание |
|---|-----|----------|
| 1 | Boundary | updateName with empty string disables save |
| 2 | Standard | updateName with text sets name and enables save |
| 3 | Edge | updateName with whitespace only disables save |
| 4 | Standard | updateName preserves all other state fields |

#### Сигнатуры

```kotlin
fun `should disable save when updateName with empty string`()
fun `should set name and enable save when updateName with text`()
fun `should disable save when updateName with whitespace only`()
fun `should preserve other fields when updateName`()
```

---

### selectFlag(flag)

#### Таблица "до -> после"

| Поле | До | После |
|------|----|-------|
| `selectedFlag` | null | **spainFlag** |
| `name` | * | не меняется |
| `editingDictionaryId` | * | не меняется |
| `flagFilter` | * | не меняется |
| `flags` | * | не меняется |
| `saveButtonEnabled` | * | не меняется |

#### Тест-кейсы

| # | Тип | Описание |
|---|-----|----------|
| 5 | Boundary | selectFlag on default state sets selectedFlag |
| 6 | Standard | selectFlag replaces previously selected flag |
| 7 | Standard | selectFlag preserves all other state fields |

#### Сигнатуры

```kotlin
fun `should set selectedFlag when selectFlag on default state`()
fun `should replace flag when selectFlag with different flag`()
fun `should preserve other fields when selectFlag`()
```

---

### deselectFlag()

#### Таблица "до -> после"

| Поле | До | После |
|------|----|-------|
| `selectedFlag` | spainFlag | **null** |
| `name` | * | не меняется |
| `editingDictionaryId` | * | не меняется |
| `flagFilter` | * | не меняется |
| `flags` | * | не меняется |
| `saveButtonEnabled` | * | не меняется |

#### Тест-кейсы

| # | Тип | Описание |
|---|-----|----------|
| 8 | Boundary | deselectFlag on default state (selectedFlag already null) |
| 9 | Standard | deselectFlag clears selectedFlag |
| 10 | Standard | deselectFlag preserves all other state fields |

#### Сигнатуры

```kotlin
fun `should remain null when deselectFlag on default state`()
fun `should clear selectedFlag when deselectFlag`()
fun `should preserve other fields when deselectFlag`()
```

---

### updateFlagFilter(query)

#### Таблица "до -> после"

| query | flagFilter -> |
|-------|---------------|
| `"spa"` | "spa" |
| `""` | "" |
| `"   "` | "   " |

Не меняются: `editingDictionaryId`, `name`, `flags`, `selectedFlag`, `saveButtonEnabled`

#### Тест-кейсы

| # | Тип | Описание |
|---|-----|----------|
| 11 | Boundary | updateFlagFilter with empty string sets empty flagFilter |
| 12 | Standard | updateFlagFilter with text sets flagFilter |
| 13 | Standard | updateFlagFilter preserves all other state fields |

#### Сигнатуры

```kotlin
fun `should set empty flagFilter when updateFlagFilter with empty`()
fun `should set flagFilter when updateFlagFilter with text`()
fun `should set flagFilter when updateFlagFilter with whitespace`()
fun `should preserve other fields when updateFlagFilter`()
```

---

### updateFlags(list)

#### Таблица "до -> после"

| list | flags -> |
|------|----------|
| `listOf(spainFlag)` | [spainFlag] |
| `emptyList()` | [] |
| `flags` (2 items) | [spainFlag, mexicoFlag] |

Не меняются: `editingDictionaryId`, `name`, `flagFilter`, `selectedFlag`, `saveButtonEnabled`

#### Тест-кейсы

| # | Тип | Описание |
|---|-----|----------|
| 14 | Boundary | updateFlags with empty list sets empty flags |
| 15 | Standard | updateFlags with list sets flags |
| 16 | Standard | updateFlags preserves all other state fields |

#### Сигнатуры

```kotlin
fun `should set empty flags when updateFlags with empty list`()
fun `should set flags when updateFlags with list`()
fun `should preserve other fields when updateFlags`()
```

---

### prefillForEdit(name, flag)

#### Таблица "до -> после"

| name | flag | name -> | selectedFlag -> | saveButtonEnabled -> |
|------|------|---------|-----------------|----------------------|
| `"English"` | spainFlag | "English" | spainFlag | **true** |
| `"English"` | null | "English" | null | **true** |
| `""` | spainFlag | "" | spainFlag | **false** |

Не меняются: `editingDictionaryId`, `flagFilter`, `flags`

#### Тест-кейсы

| # | Тип | Описание |
|---|-----|----------|
| 17 | Standard | prefillForEdit with name and flag sets all three fields |
| 18 | Standard | prefillForEdit with null flag sets name and null selectedFlag |
| 19 | Edge | prefillForEdit with empty name disables save |
| 20 | Standard | prefillForEdit preserves editingDictionaryId, flagFilter, flags |

#### Сигнатуры

```kotlin
fun `should set name flag and enable save when prefillForEdit`()
fun `should set name and null flag when prefillForEdit without flag`()
fun `should disable save when prefillForEdit with empty name`()
fun `should preserve other fields when prefillForEdit`()
```

---

## Reducer тесты

### Файл 1: FormActionsTest.kt (перезаписывается)

Путь: `modules/screen/dictionary/src/test/java/me/apomazkin/dictionary/form/reducer/FormActionsTest.kt`

Тестовые данные:

```kotlin
private val reducer = DictionaryFormReducer()
private val spainFlag = CountryFlagItem(
    numericCode = 724, countryName = "Spain", flagRes = 100,
    languages = listOf("Spanish", "Catalan")
)
private val mexicoFlag = CountryFlagItem(
    numericCode = 484, countryName = "Mexico", flagRes = 101,
    languages = listOf("Spanish")
)
```

---

#### Msg.NameChanged(value)

| # | Тип | Начальный стейт | Ожидание (state) | Effects |
|---|-----|----------------|------------------|---------|
| 1 | Boundary | default | name="", saveButtonEnabled=false | нет |
| 2 | Standard | default | name="English", saveButtonEnabled=true | нет |
| 3 | Edge | default | name="   ", saveButtonEnabled=false | нет |
| 4 | Standard | editingDictionaryId=5, selectedFlag=spainFlag | editingDictionaryId=5, selectedFlag=spainFlag (не изменились) | нет |

Сигнатуры:

```kotlin
fun `should disable save when NameChanged with empty`()
fun `should set name and enable save when NameChanged with text`()
fun `should disable save when NameChanged with whitespace`()
fun `should preserve other fields when NameChanged`()
```

---

#### Msg.FlagFilterChanged(query)

| # | Тип | Начальный стейт | Ожидание (state) | Effects |
|---|-----|----------------|------------------|---------|
| 5 | Boundary | default | flagFilter="" | FilterFlags("") |
| 6 | Standard | default | flagFilter="spa" | FilterFlags("spa") |
| 7 | Standard | name="Eng", selectedFlag=spainFlag | name="Eng", selectedFlag=spainFlag (не изменились) | FilterFlags("spa") |

Сигнатуры:

```kotlin
fun `should set empty flagFilter and emit FilterFlags when FlagFilterChanged empty`()
fun `should set flagFilter and emit FilterFlags when FlagFilterChanged with text`()
fun `should preserve other fields when FlagFilterChanged`()
```

---

#### Msg.SelectFlag(item)

| # | Тип | Начальный стейт | Ожидание (state) | Effects |
|---|-----|----------------|------------------|---------|
| 8 | Boundary | selectedFlag=null | selectedFlag=spainFlag | нет |
| 9 | Standard | selectedFlag=spainFlag | selectedFlag=mexicoFlag (switch) | нет |
| 10 | Standard | selectedFlag=spainFlag, item=spainFlag | selectedFlag=null (toggle off) | нет |
| 11 | Standard | name="Eng", flags=[...] | name="Eng", flags=[...] (не изменились) | нет |

Сигнатуры:

```kotlin
fun `should select flag when SelectFlag on empty selection`()
fun `should switch flag when SelectFlag with different flag`()
fun `should deselect flag when SelectFlag with already selected flag`()
fun `should preserve other fields when SelectFlag`()
```

---

#### Msg.Save

| # | Тип | Начальный стейт | Ожидание (state) | Effects |
|---|-----|----------------|------------------|---------|
| 12 | Standard | editingDictionaryId=null, name="Bio", selectedFlag=null | state не меняется | SaveDictionary("Bio", null) |
| 13 | Standard | editingDictionaryId=null, name="Eng", selectedFlag=spainFlag | state не меняется | SaveDictionary("Eng", 724) |
| 14 | Standard | editingDictionaryId=5, name="Updated", selectedFlag=spainFlag | state не меняется | UpdateDictionary(5, "Updated", 724) |
| 15 | Edge | editingDictionaryId=5, name="Bio", selectedFlag=null | state не меняется | UpdateDictionary(5, "Bio", null) |

Сигнатуры:

```kotlin
fun `should emit SaveDictionary without flag when Save in create mode`()
fun `should emit SaveDictionary with flag when Save in create mode with flag`()
fun `should emit UpdateDictionary when Save in edit mode`()
fun `should emit UpdateDictionary with null numericCode when Save edit without flag`()
```

---

#### Msg.Back

| # | Тип | Начальный стейт | Ожидание (state) | Effects |
|---|-----|----------------|------------------|---------|
| 16 | Standard | default | state не меняется | Back |
| 17 | Standard | name="Eng", selectedFlag=spainFlag | state не меняется | Back |

Сигнатуры:

```kotlin
fun `should emit Back effect when Back on default state`()
fun `should preserve state and emit Back when Back with data`()
```

---

#### Msg.Empty

| # | Тип | Начальный стейт | Ожидание (state) | Effects |
|---|-----|----------------|------------------|---------|
| 18 | Boundary | default | state не меняется | нет |

Сигнатура:

```kotlin
fun `should not change state and produce no effects when Empty`()
```

---

### Файл 2: FormDataLoadingTest.kt (перезаписывается)

Путь: `modules/screen/dictionary/src/test/java/me/apomazkin/dictionary/form/reducer/FormDataLoadingTest.kt`

Тестовые данные:

```kotlin
private val reducer = DictionaryFormReducer()
private val spainFlag = CountryFlagItem(
    numericCode = 724, countryName = "Spain", flagRes = 100,
    languages = listOf("Spanish", "Catalan")
)
private val mexicoFlag = CountryFlagItem(
    numericCode = 484, countryName = "Mexico", flagRes = 101,
    languages = listOf("Spanish")
)
private val flags = listOf(spainFlag, mexicoFlag)
```

---

#### initEffects (ViewModel)

| # | Тип | editingDictionaryId | Ожидание (initEffects) |
|---|-----|--------------------|-----------------------|
| 0a | Boundary | null (создание) | emptySet() |
| 0b | Standard | 42L (редактирование) | setOf(Effect.LoadDictionary(42)) |

Сигнатуры:

```kotlin
fun `should have empty initEffects when creating`()
fun `should have LoadDictionary in initEffects when editing`()
```

---

#### Msg.FlagsUpdated(list)

| # | Тип | Начальный стейт | Ожидание (state) | Effects |
|---|-----|----------------|------------------|---------|
| 1 | Boundary | default | flags=emptyList() | нет |
| 2 | Standard | default | flags=[spainFlag, mexicoFlag] | нет |
| 3 | Standard | name="Eng", selectedFlag=spainFlag | name, selectedFlag не изменились | нет |

Сигнатуры:

```kotlin
fun `should set empty flags when FlagsUpdated with empty list`()
fun `should set flags when FlagsUpdated with list`()
fun `should preserve other fields when FlagsUpdated`()
```

---

#### Msg.DictionaryLoaded(name, flag)

| # | Тип | Начальный стейт | Ожидание (state) | Effects |
|---|-----|----------------|------------------|---------|
| 4 | Standard | editingDictionaryId=5 | name="English", selectedFlag=spainFlag, saveButtonEnabled=true | нет |
| 5 | Standard | editingDictionaryId=5 | name="English", selectedFlag=null, saveButtonEnabled=true | нет |
| 6 | Edge | editingDictionaryId=5 | name="", saveButtonEnabled=false | нет |
| 7 | Standard | editingDictionaryId=5, flagFilter="spa" | flagFilter="spa" (не изменился), editingDictionaryId=5 (не изменился) | нет |

Сигнатуры:

```kotlin
fun `should prefill name and flag when DictionaryLoaded`()
fun `should prefill name with null flag when DictionaryLoaded without flag`()
fun `should disable save when DictionaryLoaded with empty name`()
fun `should preserve other fields when DictionaryLoaded`()
```

---

#### Msg.DictionarySaved

| # | Тип | Начальный стейт | Ожидание (state) | Effects |
|---|-----|----------------|------------------|---------|
| 8 | Standard | default | state не меняется | Close |
| 9 | Standard | name="Eng", selectedFlag=spainFlag | state не меняется | Close |

Сигнатуры:

```kotlin
fun `should emit Close effect when DictionarySaved`()
fun `should preserve state when DictionarySaved`()
```

---

## Verification дополнения

- [ ] [test] Msg.SelectFlag реализует toggle-семантику: тап на выбранный флаг снимает выбор (deselectFlag), тап на другой — переключает (selectFlag)
  - подпункт к корневым #4 и #5 (тап на флаг / тап на выбранный)
- [ ] [test] Msg.DictionarySaved порождает Effect.Close (не needClose в State), state не меняется
  - подпункт к корневым #7 и #8 (создание/сохранение)
- [ ] [test] Msg.FlagFilterChanged порождает Effect.FilterFlags(query), state обновляет только flagFilter
  - подпункт к корневому #6 (фильтрация)
- [ ] [test] prefillForEdit с пустым name → saveButtonEnabled=false (edge case редактирования)
  - подпункт к корневому #2 (редактирование)
- [ ] [test] Msg.Back порождает Effect.Back, state не меняется
  - подпункт к корневому #9 (back press)

_model: claude-opus-4-6[1m]_
