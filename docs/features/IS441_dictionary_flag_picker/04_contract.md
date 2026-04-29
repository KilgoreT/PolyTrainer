# Contract

## State

### DictionaryFormScreenState

```kotlin
data class DictionaryFormScreenState(
    val editingDictionaryId: Long? = null,
    val name: String = "",
    val flagFilter: String = "",              // NEW
    val flags: List<CountryFlagItem> = emptyList(),  // NEW (заменяет availableFlags)
    val selectedFlag: CountryFlagItem? = null,
    val saveButtonEnabled: Boolean = false,
    // REMOVED: needClose, allFlags, isLanguageBound, selectedLanguage, languagePickerState
)
```

#### editingDictionaryId: `Long?`
- Дефолт: `null`

ID словаря для редактирования. null = режим создания, не null = режим редактирования

---

#### name: `String`
- Дефолт: `""`

Текст в поле названия словаря. Привязан к TextField

---

#### flagFilter: `String`
- Дефолт: `""`

Текст в поле фильтрации флагов. Привязан к TextField фильтра

---

#### flags: `List<CountryFlagItem>`
- Дефолт: `emptyList()`

Отфильтрованный список флагов для отображения в grid. Вычисляется из allFlags + flagFilter при каждом изменении фильтра

---

#### selectedFlag: `CountryFlagItem?`
- Дефолт: `null`

Выбранный флаг. null = не выбран (placeholder). Отображается рядом с полем названия и с BorderStroke в grid

---

#### saveButtonEnabled: `Boolean`
- Дефолт: `false`

Активность кнопки "Создать"/"Сохранить". Вычисляется из name.isNotBlank() в редьюсере

---

### Ключевые инварианты
- saveButtonEnabled = name.isNotBlank() (синхронизируется при каждом updateName)
- editingDictionaryId не меняется после инициализации
- flags обновляется реактивно через FlowHandler при изменении flagFilter
- allFlags хранится в UseCase, не в State

### CountryFlagItem (существующая модель, расширяется)

```kotlin
data class CountryFlagItem(
    val numericCode: Int,
    val countryName: String,
    val flagRes: Int,
    val languages: List<String> = listOf(),  // NEW
)
```

#### numericCode: `Int`

Числовой код страны (ISO 3166-1 numeric)

---

#### countryName: `String`

Название страны

---

#### flagRes: `Int`

Ресурс иконки флага

---

#### languages: `List<String>`
- Дефолт: `listOf()`

Список языков страны для фильтрации. Новое поле. Заполняется в UseCase через `countryProvider.getLanguagesForCountry(numericCode)`

## Extensions

#### updateName

```kotlin
fun DictionaryFormScreenState.updateName(value: String)
```

Обновляет name и пересчитывает saveButtonEnabled = value.isNotBlank()
Затрагивает: name, saveButtonEnabled

---

#### selectFlag

```kotlin
fun DictionaryFormScreenState.selectFlag(flag: CountryFlagItem)
```

Устанавливает selectedFlag
Затрагивает: selectedFlag

---

#### deselectFlag

```kotlin
fun DictionaryFormScreenState.deselectFlag()
```

Сбрасывает selectedFlag в null
Затрагивает: selectedFlag

---

#### updateFlagFilter

```kotlin
fun DictionaryFormScreenState.updateFlagFilter(query: String)
```

Обновляет только flagFilter. Фильтрация происходит в UseCase через FlowHandler (подписка на flagFilter с debounce)
Затрагивает: flagFilter

---

#### updateFlags

```kotlin
fun DictionaryFormScreenState.updateFlags(list: List<CountryFlagItem>)
```

Обновляет flags из результата фильтрации UseCase
Затрагивает: flags

---

#### prefillForEdit

```kotlin
fun DictionaryFormScreenState.prefillForEdit(name: String, flag: CountryFlagItem?)
```

Заполняет форму данными словаря для редактирования. Устанавливает name, selectedFlag, saveButtonEnabled = name.isNotBlank()
Затрагивает: name, selectedFlag, saveButtonEnabled

---

### Удаляемые поля и extensions

- needClose, isLanguageBound, selectedLanguage, languagePickerState — удаляются
- close(), toggleLanguageBound(), selectLanguage(), showLanguagePicker(), hideLanguagePicker(), filterLanguages(), setLanguages() — удаляются
- старая updateFlags() — удаляется (новая updateFlags с другой логикой описана в Extensions)

## UI Messages

### NameChanged(value: String)
- Trigger: пользователь вводит текст в поле названия словаря
- State changes: name, saveButtonEnabled -- через updateName(value)
- Effects: нет

### FlagFilterChanged(query: String)
- Trigger: пользователь вводит текст в поле фильтрации флагов
- State changes: flagFilter — через updateFlagFilter(query)
- Effects: Effect.FilterFlags(query)
- Примечание: Effect идёт в FlowHandler.runEffect() → useCase.updateFilter(query) → UseCase Flow эмитит → FlowHandler → FlagsUpdated

### SelectFlag(item: CountryFlagItem)
- Trigger: пользователь тапает на флаг в grid
- State changes: зависит от текущего selectedFlag
  - Если item == selectedFlag -> deselectFlag() (toggle off)
  - Если item != selectedFlag -> selectFlag(item) (toggle on / switch)
- Effects: нет
- Примечание: один message обрабатывает и выбор, и снятие выбора (toggle-семантика)

### Save
- Trigger: пользователь нажимает кнопку "Создать" или "Сохранить"
- State changes: нет немедленных (state обновится через DatasourceMsg)
- Effects: зависит от editingDictionaryId
  - null -> SaveDictionary(name, numericCode)
  - не null -> UpdateDictionary(id, name, numericCode)
- Примечание: numericCode = selectedFlag?.numericCode

### Back
- Trigger: пользователь нажимает кнопку "Назад" (AppBar или системная)
- State changes: нет
- Effects: Effect.Back

---

### Empty
- No-op fallback. Не меняет State, не порождает Effects

## Effects

### FilterFlags(query: String)
- Source: Msg.FlagFilterChanged
- Handler: FlagFilterFlowHandler (через runEffect)
- Action: useCase.updateFilter(query) → UseCase Flow пересчитывает → FlowHandler эмитит FlagsUpdated
- Результат: косвенный — через Flow подписку

---

### LoadDictionary(id: Long)
- Source: initEffects (при editingDictionaryId != null)
- Action: useCase.getDictionary(id) → получает DictionaryItem. useCase.findFlag(numericCode) → резолвит флаг из allFlags
- Результат: Msg.DictionaryLoaded(name, flag: CountryFlagItem?)

### SaveDictionary(name: String, numericCode: Int?)
- Source: Msg.Save (при editingDictionaryId == null)
- Action: DictionaryUseCase.addDictionary(name, numericCode) -> setCurrentDictionary(id)
- Результат: Msg.DictionarySaved

### UpdateDictionary(id: Long, name: String, numericCode: Int?)
- Source: Msg.Save (при editingDictionaryId != null)
- Action: DictionaryUseCase.updateDictionary(id, name, numericCode)
- Результат: Msg.DictionarySaved

### Close
- Source: Msg.DictionarySaved
- Handler: NavigationEffectHandler
- Action: вызывает onClose()
- Результат: Msg.Empty

---

### Back
- Source: Msg.Back
- Handler: NavigationEffectHandler
- Action: вызывает onBackPress()
- Результат: Msg.Empty

## Datasource Messages

### FlagsUpdated(list: List\<CountryFlagItem>)
- Source: FlagFilterFlowHandler — все emit'ы (инициализация и фильтрация)
- State changes: flags — через updateFlags(list)
- Effects: нет

---

### DictionaryLoaded(name: String, flag: CountryFlagItem?)
- Source: Effect.LoadDictionary
- State changes: name, selectedFlag, saveButtonEnabled — через prefillForEdit(name, flag)
- Effects: нет
- Примечание: flag резолвится в EffectHandler через useCase.findFlag(numericCode), не из state.flags. Независим от FlagsUpdated

### DictionarySaved
- Source: Effect.SaveDictionary, Effect.UpdateDictionary
- State changes: нет
- Effects: Effect.Close

**Удаляемые messages и effects (из текущего кода):**
- ToggleLanguageBound, OpenLanguagePicker, CloseLanguagePicker, LanguageQueryChanged, SelectLanguage -- удаляются
- LanguagesLoaded -- удаляется
- LoadFlagsForLanguage -- удаляется (флаги загружаются все сразу, без привязки к языку)

**Новые messages:**
- FlagFilterChanged -- ввод текста в поле фильтрации
- FlagsUpdated -- все emit'ы FlowHandler (инициализация и фильтрация)
- DictionaryLoaded -- результат загрузки словаря для редактирования

**Новые effects:**
- LoadDictionary -- загрузка словаря для предзаполнения
- Close -- навигация назад (вместо needClose в State)

**Новые handlers:**
- NavigationEffectHandler -- обрабатывает Effect.Close, вызывает onClose callback
- FlagFilterFlowHandler -- subscribe() подписан на useCase.flagsFlow(), всегда эмитит FlagsUpdated. runEffect() обрабатывает FilterFlags(query) — вызывает useCase.updateFilter(query). Debounce в UseCase. При старте эмитит все флаги (query пуст)

**UseCase (новый метод):**
- `fun updateFilter(query: String)` — обновляет фильтр, UseCase хранит allFlags внутри, Flow эмитит отфильтрованный список. Фильтрация по countryName и languages, case-insensitive substring, debounce 300ms

**Маршрутизация Effect → Handler:**

| Effect | Handler | Остальные handlers |
|--------|---------|-------------------|
| FilterFlags | FlagFilterFlowHandler | игнорируют |
| LoadDictionary | DatasourceEffectHandler | иг��орируют |
| SaveDictionary | DatasourceEffectHandler | игнорируют |
| UpdateDictionary | DatasourceEffectHandler | игнорируют |
| Close | NavigationEffectHandler | игнорируют |
| Back | NavigationEffectHandler | игнорируют |

**Цепочки:**
1. Создание: FlowHandler подписка → FlagsUpdated (все флаги) → конечная
2. Редактирование: initEffects → LoadDictionary(id) → handler резолвит флаг из UseCase → DictionaryLoaded(name, flag) → prefillForEdit. Параллельно FlowHandler → FlagsUpdated. Независимы
3. Фильтрация: FlagFilterChanged → Effect.FilterFlags(query) → UseCase.updateFilter → Flow emit → FlagsUpdated (реактивная)
4. Сохранение: Save → SaveDictionary/UpdateDictionary → DictionarySaved → Effect.Close
5. Назад: Back → Effect.Back

