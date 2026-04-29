# Design Tree

## Часть 1: Граф

```yaml
- id: 0
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/model/CountryFlagItem.kt
  action: "~"
  depends: []

- id: 1
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/model/DictionaryItem.kt
  action: "+"
  depends: []

- id: 2
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/DictionaryUseCase.kt
  action: "~"
  depends: [0, 1]

- id: 3
  file: app/src/main/java/me/apomazkin/polytrainer/di/module/dictionary/DictionaryUseCaseImpl.kt
  action: "~"
  depends: [2]

- id: 4
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/DictionaryFormState.kt
  action: "~"
  depends: [0]

- id: 5
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/DictionaryFormMsg.kt
  action: "~"
  depends: [0]

- id: 6
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/DictionaryFormEffectHandler.kt
  action: "~"
  depends: [2, 5]

- id: 7
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/FlagFilterFlowHandler.kt
  action: "+"
  depends: [2, 5, 6]

- id: 8
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/NavigationEffectHandler.kt
  action: "+"
  depends: [5, 6]

- id: 9
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/DictionaryFormReducer.kt
  action: "~"
  depends: [4, 5, 6]

- id: 10
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/DictionaryFormViewModel.kt
  action: "~"
  depends: [6, 7, 8, 9]

- id: 11
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/FlagGridWidget.kt
  action: "~"
  depends: [0]

- id: 12
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/DictionaryFormWidget.kt
  action: "~"
  depends: [4, 5, 11]

- id: 13
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/DictionaryFormScreen.kt
  action: "~"
  depends: [4, 5, 10, 12]

- id: 14
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/LanguagePickerBottomSheet.kt
  action: "-"
  depends: [12, 13]

- id: 15
  file: modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/model/LanguageItem.kt
  action: "-"
  depends: [2, 3, 4, 5, 13, 14, 16, 17, 18, 19]

- id: 16
  file: modules/screen/dictionary/src/test/java/me/apomazkin/dictionary/form/ext/LanguagePickerExtTest.kt
  action: "-"
  depends: [4]

- id: 17
  file: modules/screen/dictionary/src/test/java/me/apomazkin/dictionary/form/ext/FormFieldsExtTest.kt
  action: "~"
  depends: [0, 4]

- id: 18
  file: modules/screen/dictionary/src/test/java/me/apomazkin/dictionary/form/reducer/FormActionsTest.kt
  action: "~"
  depends: [0, 4, 5, 6, 9]

- id: 19
  file: modules/screen/dictionary/src/test/java/me/apomazkin/dictionary/form/reducer/FormDataLoadingTest.kt
  action: "~"
  depends: [0, 4, 5, 6, 9]
```

## Часть 2: Детали изменений

### #0 CountryFlagItem.kt [~]

**Было:**
```kotlin
data class CountryFlagItem(
    val numericCode: Int,
    val countryName: String,
    val flagRes: Int,
)
```

**Стало:**
```kotlin
data class CountryFlagItem(
    val numericCode: Int,
    val countryName: String,
    val flagRes: Int,
    val languages: List<String> = listOf(),
)
```

Добавляется поле `languages` для фильтрации по языкам страны. Дефолт `listOf()` -- обратная совместимость с существующими вызовами.

---

### #1 DictionaryItem.kt [+]

Модель словаря для предзаполнения формы при редактировании.

```kotlin
package me.apomazkin.dictionary.model

data class DictionaryItem(
    val id: Long,
    val name: String,
    val numericCode: Int?,
)
```

---

### #2 DictionaryUseCase.kt [~]

**Было:**
```kotlin
interface DictionaryUseCase {
    suspend fun getDictionaryList(): List<DictionaryListItem>
    fun flowDictionaryList(): Flow<List<DictionaryListItem>>
    suspend fun addDictionary(name: String, numericCode: Int?): Long
    suspend fun updateDictionary(id: Long, name: String, numericCode: Int?)
    suspend fun deleteDictionary(id: Long)
    suspend fun setCurrentDictionary(id: Long)
    fun getAvailableLanguages(): List<LanguageItem>
    suspend fun getCountriesForLanguage(languageCode: String): List<CountryFlagItem>
}
```

**Стало:**
```kotlin
interface DictionaryUseCase {
    // Остаются без изменений:
    suspend fun getDictionaryList(): List<DictionaryListItem>
    fun flowDictionaryList(): Flow<List<DictionaryListItem>>
    suspend fun addDictionary(name: String, numericCode: Int?): Long
    suspend fun updateDictionary(id: Long, name: String, numericCode: Int?)
    suspend fun deleteDictionary(id: Long)
    suspend fun setCurrentDictionary(id: Long)

    // Новые:
    fun updateFilter(query: String)
    fun flagsFlow(): Flow<List<CountryFlagItem>>
    suspend fun getDictionary(id: Long): DictionaryItem
    fun findFlag(numericCode: Int): CountryFlagItem?

    // Удаляются:
    // fun getAvailableLanguages(): List<LanguageItem>
    // suspend fun getCountriesForLanguage(languageCode: String): List<CountryFlagItem>
}
```

Удалён импорт `LanguageItem`. Добавлен импорт `DictionaryItem`.

---

### #3 DictionaryUseCaseImpl.kt [~]

**Было:**
```kotlin
class DictionaryUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val countryProvider: CountryProvider,
    private val prefsProvider: PrefsProvider,
) : DictionaryUseCase {
    // getAvailableLanguages() -- перебор Locale.getISOLanguages()
    // getCountriesForLanguage(code) -- фильтрация стран по языку
}
```

**Стало:**
```kotlin
class DictionaryUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val countryProvider: CountryProvider,
    private val prefsProvider: PrefsProvider,
) : DictionaryUseCase {
    // Внутреннее состояние:
    private val allFlags: List<CountryFlagItem> by lazy { loadAllFlags() }
    private val filterQuery = MutableStateFlow("")

    override fun updateFilter(query: String) {
        // эмитит query в filterQuery
    }

    override fun flagsFlow(): Flow<List<CountryFlagItem>> {
        // filterQuery.debounce(300).map { filterFlags(allFlags, it) }
        // при пустом query -- все флаги
    }

    override suspend fun getDictionary(id: Long): DictionaryItem {
        // dictionaryApi.getDictionaryById(id) → маппинг в DictionaryItem(id, name, numericCode)
    }

    override fun findFlag(numericCode: Int): CountryFlagItem? {
        // allFlags.firstOrNull { it.numericCode == numericCode }
    }

    private fun loadAllFlags(): List<CountryFlagItem> {
        // countryProvider.getAllCountries() → map { CountryFlagItem(numericCode, name, flagRes, languages) }
        // languages = countryProvider.getLanguagesForCountry(numericCode)
        // getFlagRes -- suspend, нужен runBlocking или синхронная версия
    }

    private fun filterFlags(allFlags: List<CountryFlagItem>, query: String): List<CountryFlagItem> {
        // if query.isBlank() → allFlags
        // иначе фильтрация по countryName и languages, case-insensitive substring
    }

    // Удаляются: getAvailableLanguages(), getCountriesForLanguage()
    // Удаляется импорт LanguageItem
}
```

**Замечание:** `CountryProvider.getFlagRes()` -- suspend. В текущей реализации `getCountriesForLanguage` вызывается из suspend-контекста. Для `loadAllFlags()` (lazy, вызывается из non-suspend `flagsFlow()`) нужно решение: либо сделать `getFlagRes` синхронным в CountryProvider (World.getFlagOf -- синхронный вызов под капотом), либо использовать `runBlocking`. Предпочтительно: добавить синхронный `getFlagResSync(numericCode: Int): Int` в CountryProvider.

---

### #4 DictionaryFormState.kt [~]

**Было:**
```kotlin
data class DictionaryFormScreenState(
    val needClose: Boolean = false,
    val editingDictionaryId: Long? = null,
    val name: String = "",
    val isLanguageBound: Boolean = false,
    val selectedLanguage: LanguageItem? = null,
    val availableFlags: List<CountryFlagItem> = listOf(),
    val selectedFlag: CountryFlagItem? = null,
    val saveButtonEnabled: Boolean = false,
    val languagePickerState: LanguagePickerState = LanguagePickerState(),
)
// + LanguagePickerState data class
// + extensions: toggleLanguageBound, selectLanguage, showLanguagePicker,
//   hideLanguagePicker, filterLanguages, setLanguages, close, updateFlags (старая)
```

**Стало:**
```kotlin
data class DictionaryFormScreenState(
    val editingDictionaryId: Long? = null,
    val name: String = "",
    val flagFilter: String = "",
    val flags: List<CountryFlagItem> = emptyList(),
    val selectedFlag: CountryFlagItem? = null,
    val saveButtonEnabled: Boolean = false,
)

// Extensions:
fun DictionaryFormScreenState.updateName(value: String)
    // copy(name = value, saveButtonEnabled = value.isNotBlank())

fun DictionaryFormScreenState.selectFlag(flag: CountryFlagItem)
    // copy(selectedFlag = flag)

fun DictionaryFormScreenState.deselectFlag()
    // copy(selectedFlag = null)

fun DictionaryFormScreenState.updateFlagFilter(query: String)
    // copy(flagFilter = query)

fun DictionaryFormScreenState.updateFlags(list: List<CountryFlagItem>)
    // copy(flags = list)

fun DictionaryFormScreenState.prefillForEdit(name: String, flag: CountryFlagItem?)
    // copy(name = name, selectedFlag = flag, saveButtonEnabled = name.isNotBlank())

// Удаляются: LanguagePickerState, needClose, isLanguageBound, selectedLanguage,
//   availableFlags, languagePickerState
// Удаляются extensions: toggleLanguageBound, selectLanguage, showLanguagePicker,
//   hideLanguagePicker, filterLanguages, setLanguages, close, старая updateFlags
// Удаляется импорт LanguageItem
```

---

### #5 DictionaryFormMsg.kt [~]

**Было:**
```kotlin
sealed interface DictionaryFormMsg {
    data class NameChanged(val value: String)
    data object ToggleLanguageBound
    data object Save
    data object OpenLanguagePicker
    data object CloseLanguagePicker
    data class LanguageQueryChanged(val query: String)
    data class SelectLanguage(val item: LanguageItem)
    data class SelectFlag(val item: CountryFlagItem)
    data class LanguagesLoaded(val list: List<LanguageItem>)
    data class FlagsLoaded(val list: List<CountryFlagItem>)
    data object DictionarySaved
    data object Empty
}
```

**Стало:**
```kotlin
sealed interface DictionaryFormMsg {
    // UI messages:
    data class NameChanged(val value: String)
    data class FlagFilterChanged(val query: String)
    data class SelectFlag(val item: CountryFlagItem)
    data object Save
    data object Back

    // Datasource messages:
    data class FlagsUpdated(val list: List<CountryFlagItem>)
    data class DictionaryLoaded(val name: String, val flag: CountryFlagItem?)
    data object DictionarySaved

    data object Empty
}

// Удаляются: ToggleLanguageBound, OpenLanguagePicker, CloseLanguagePicker,
//   LanguageQueryChanged, SelectLanguage, LanguagesLoaded, FlagsLoaded
// Удаляется импорт LanguageItem
```

---

### #6 DictionaryFormEffectHandler.kt [~]

**Было:**
```kotlin
sealed interface DictionaryFormEffect : Effect {
    data object LoadLanguages
    data class LoadFlagsForLanguage(val languageCode: String)
    data class SaveDictionary(val name: String, val numericCode: Int?)
    data class UpdateDictionary(val id: Long, val name: String, val numericCode: Int?)
}

class DictionaryFormEffectHandler(dictionaryUseCase) : MateEffectHandler {
    // runEffect: LoadLanguages, LoadFlagsForLanguage, SaveDictionary, UpdateDictionary
}
```

**Стало:**
```kotlin
sealed interface DictionaryFormEffect : Effect {
    data class FilterFlags(val query: String)
    data class LoadDictionary(val id: Long)
    data class SaveDictionary(val name: String, val numericCode: Int?)
    data class UpdateDictionary(val id: Long, val name: String, val numericCode: Int?)
    data object Close
    data object Back
}

class DictionaryFormEffectHandler(dictionaryUseCase) : MateEffectHandler {
    // runEffect: safe cast as? DictionaryFormEffect
    // LoadDictionary → dictionaryUseCase.getDictionary(id) + findFlag(numericCode) → DictionaryLoaded
    // SaveDictionary → addDictionary + setCurrentDictionary → DictionarySaved
    // UpdateDictionary → updateDictionary → DictionarySaved
    // FilterFlags, Close, Back → Msg.Empty (обрабатываются другими handlers)
}

// Удаляются: LoadLanguages, LoadFlagsForLanguage
// Удаляется импорт LanguageItem
```

---

### #7 FlagFilterFlowHandler.kt [+]

FlowHandler для реактивной подписки на flagsFlow() и обработки эффекта FilterFlags.

```kotlin
package me.apomazkin.dictionary.form

class FlagFilterFlowHandler(
    private val dictionaryUseCase: DictionaryUseCase,
) : MateFlowHandler<DictionaryFormMsg, Effect> {

    override var job: Job? = null

    override fun subscribe(scope: CoroutineScope, send: (DictionaryFormMsg) -> Unit) {
        // job = scope.launch { dictionaryUseCase.flagsFlow().collectLatest { send(FlagsUpdated(it)) } }
    }

    override suspend fun runEffect(effect: Effect, consumer: (DictionaryFormMsg) -> Unit) {
        // val e = effect as? DictionaryFormEffect.FilterFlags ?: return
        // dictionaryUseCase.updateFilter(e.query)
        // (Flow подписка эмитит FlagsUpdated сама)
    }
}
```

---

### #8 NavigationEffectHandler.kt [+]

Обрабатывает навигационные эффекты Close и Back.

```kotlin
package me.apomazkin.dictionary.form

class NavigationEffectHandler(
    private val onClose: () -> Unit,
    private val onBackPress: (() -> Unit)?,
) : MateEffectHandler<DictionaryFormMsg, Effect> {

    override suspend fun runEffect(effect: Effect, consumer: (DictionaryFormMsg) -> Unit) {
        // val e = effect as? DictionaryFormEffect ?: return
        // when (e) {
        //     DictionaryFormEffect.Close → onClose()
        //     DictionaryFormEffect.Back → onBackPress?.invoke()
        //     else → return
        // }
        // consumer(DictionaryFormMsg.Empty)
    }
}
```

---

### #9 DictionaryFormReducer.kt [~]

**Было:**
```kotlin
when (message) {
    NameChanged → updateName
    ToggleLanguageBound → toggleLanguageBound
    Save → SaveDictionary / UpdateDictionary
    OpenLanguagePicker → showLanguagePicker
    CloseLanguagePicker → hideLanguagePicker
    LanguageQueryChanged → filterLanguages
    SelectLanguage → selectLanguage + LoadFlagsForLanguage
    SelectFlag → selectFlag
    LanguagesLoaded → setLanguages
    FlagsLoaded → updateFlags
    DictionarySaved → close()
    Empty → no-op
}
```

**Стало:**
```kotlin
when (message) {
    NameChanged → state.updateName(value) to emptySet()
    FlagFilterChanged → state.updateFlagFilter(query) to setOf(Effect.FilterFlags(query))
    SelectFlag → if (item == state.selectedFlag) state.deselectFlag() else state.selectFlag(item)
                 // оба → to emptySet()
    Save → if (editingDictionaryId != null) UpdateDictionary else SaveDictionary
    Back → state to setOf(Effect.Back)
    FlagsUpdated → state.updateFlags(list) to emptySet()
    DictionaryLoaded → state.prefillForEdit(name, flag) to emptySet()
    DictionarySaved → state to setOf(Effect.Close)
    Empty → state to emptySet()
}

// Удаляются ветки: ToggleLanguageBound, OpenLanguagePicker, CloseLanguagePicker,
//   LanguageQueryChanged, SelectLanguage, LanguagesLoaded, FlagsLoaded
```

---

### #10 DictionaryFormViewModel.kt [~]

**Было:**
```kotlin
class DictionaryFormViewModel(
    dictionaryUseCase, editingDictionaryId, editingName, editingHasFlag
) {
    // initState: editingDictionaryId, name, isLanguageBound, saveButtonEnabled
    // initEffects: setOf(LoadLanguages)
    // effectHandlerSet: setOf(DictionaryFormEffectHandler)
}
```

**Стало:**
```kotlin
class DictionaryFormViewModel(
    dictionaryUseCase, editingDictionaryId, onClose, onBackPress
) {
    // initState: DictionaryFormScreenState(editingDictionaryId = editingDictionaryId)
    // initEffects: if (editingDictionaryId != null) setOf(LoadDictionary(id)) else emptySet()
    // effectHandlerSet: setOf(
    //     DictionaryFormEffectHandler(dictionaryUseCase),
    //     FlagFilterFlowHandler(dictionaryUseCase),
    //     NavigationEffectHandler(onClose, onBackPress),
    // )
    // Factory: dictionaryUseCase, editingDictionaryId, onClose, onBackPress
    // Убираются: editingName, editingHasFlag (предзаполнение через LoadDictionary)
}
```

---

### #11 FlagGridWidget.kt [~]

**Было:**
```kotlin
// LazyRow с горизонтальной прокруткой
// Каждый элемент: Surface + ImageFlagWidget(48dp) + Text(countryName)
```

**Стало:**
```kotlin
// LazyVerticalGrid(columns = Fixed(5)) вместо LazyRow
// Modifier.weight(1f) -- занимает всё оставшееся пространство
// Каждый элемент: Surface + ImageFlagWidget(48dp), без текста страны
// BorderStroke для выбранного флага (уже есть)
```

---

### #12 DictionaryFormWidget.kt [~]

**Было:**
```kotlin
// Column: LexemeTextField(name) → Checkbox(isLanguageBound) → OutlinedTextField(selectedLanguage)
//   → FlagGridWidget(availableFlags) → PrimaryFullButtonWidget
```

**Стало:**
```kotlin
// Column:
//   Row: ImageFlagWidget/placeholder (48dp) + LexemeTextField(name) -- фиксирован сверху
//   OutlinedTextField(flagFilter) с иконкой поиска и кнопкой очистки -- фиксирован
//   FlagGridWidget(flags, selectedFlag) с Modifier.weight(1f) -- скроллится
//   PrimaryFullButtonWidget -- фиксирован снизу
// Msg: NameChanged, FlagFilterChanged, SelectFlag, Save
// Убирается: Checkbox, OutlinedTextField(language), ToggleLanguageBound, OpenLanguagePicker
```

---

### #13 DictionaryFormScreen.kt [~]

**Было:**
```kotlin
// Публичная: DictionaryFormScreen(dictionaryUseCase, editingDictionaryId, editingName,
//   editingHasFlag, onClose, onBackPress)
// Внутренняя: if (state.needClose) onClose.invoke()
//   DictionaryFormWidget + LanguagePickerBottomSheet
```

**Стало:**
```kotlin
// Публичная: DictionaryFormScreen(dictionaryUseCase, editingDictionaryId, onClose, onBackPress)
//   ViewModel получает onClose и onBackPress для NavigationEffectHandler
// Внутренняя: BackHandler { sendMsg(Msg.Back) }
//   DictionaryFormWidget (без LanguagePickerBottomSheet)
// Убирается: needClose проверка, LanguagePickerBottomSheet, editingName, editingHasFlag параметры
```

---

### #14 LanguagePickerBottomSheet.kt [-]

Полностью удаляется. Привязка к языку убрана из формы. Никто не импортирует после удаления из DictionaryFormScreen.

---

### #15 LanguageItem.kt [-]

Удаляется после того как все зависимости (#3, #13) перестанут его использовать. Модель заменена прямой фильтрацией через `CountryFlagItem.languages`. Импорты из `DictionaryFormState`, `DictionaryFormMsg`, `DictionaryUseCaseImpl` уже удалены на предыдущих шагах.

---

## Verification дополнения

- [ ] [design_tree] Пользователь открывает форму создания словаря → FlagFilterFlowHandler подписан, flagsFlow() эмитит все флаги через FlagsUpdated
  - подпункт к корневому #1 (открытие формы создания)
- [ ] [design_tree] Пользователь открывает форму редактирования → initEffects содержит LoadDictionary(id), DictionaryFormEffectHandler вызывает getDictionary + findFlag → DictionaryLoaded
  - подпункт к корневому #2 (открытие формы редактирования)
- [ ] [design_tree] Пользователь нажимает "Назад" → NavigationEffectHandler вызывает onBackPress
  - подпункт к корневому #9 (back press)
- [ ] [design_tree] DictionarySaved → Reducer порождает Effect.Close → NavigationEffectHandler вызывает onClose
  - подпункт к корневым #7, #8 (создание/сохранение)

_model: claude-opus-4-6[1m]_
