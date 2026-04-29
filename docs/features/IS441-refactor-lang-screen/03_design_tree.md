# IS441. Задача 3 — Design Tree

**Input:**
- `03_tasks.md` — требования
- `03_design_ui.md` — UI схема
- `03_design_compose.md` — Compose компоненты
- `03_design_data.md` — UseCase, API, данные
- `03_design_state.md` — стейт, extensions

---

## Граф

```yaml
# === СЛОЙ 0: DB — новые методы ===

- id: 0
  file: core/core-db-impl/src/main/java/.../room/WordDao.kt
  action: "~"
  depends: []

- id: 1
  file: core/core-db-api/src/main/java/.../CoreDbApi.kt
  action: "~"
  depends: []

- id: 2
  file: core/core-db-impl/src/main/java/.../CoreDbApiImpl.kt
  action: "~"
  depends: [0, 1]

# === СЛОЙ 0.5: FlagProvider — расширение ===

- id: 26
  file: modules/library/flags/src/main/java/.../FlagProviderImpl.kt
  action: "~"
  depends: []

# === СЛОЙ 1: UseCase ===

- id: 3
  file: modules/screen/dictionary/src/main/java/.../DictionaryViewModel.kt (interface DictionaryUseCase)
  action: "~"
  depends: [1]

- id: 4
  file: app/src/main/java/.../di/module/dictionary/DictionaryUseCaseImpl.kt
  action: "~"
  depends: [2, 3, 26]

# === СЛОЙ 2: Стейт (полная замена) ===

- id: 5
  file: modules/screen/dictionary/src/main/java/.../logic/State.kt
  action: "~"
  depends: []

# === СЛОЙ 3: Messages ===

- id: 6
  file: modules/screen/dictionary/src/main/java/.../logic/Message.kt
  action: "~"
  depends: [5]

# === СЛОЙ 4: Reducer ===

- id: 7
  file: modules/screen/dictionary/src/main/java/.../logic/DictionaryReducer.kt
  action: "~"
  depends: [5, 6]

# === СЛОЙ 5: Effects + Handler ===

- id: 8
  file: modules/screen/dictionary/src/main/java/.../logic/DatasourceEffectHandler.kt
  action: "~"
  depends: [3, 6]

# === СЛОЙ 6: Новые виджеты ===

- id: 9
  file: modules/screen/dictionary/src/main/java/.../widget/DictionaryListItemWidget.kt
  action: "+"
  depends: [5]

- id: 10
  file: modules/screen/dictionary/src/main/java/.../widget/DictionaryFormWidget.kt
  action: "+"
  depends: [5, 6]

- id: 11
  file: modules/screen/dictionary/src/main/java/.../widget/LanguagePickerBottomSheet.kt
  action: "+"
  depends: [5, 6]

- id: 12
  file: modules/screen/dictionary/src/main/java/.../widget/FlagGridWidget.kt
  action: "+"
  depends: [5, 6]

- id: 13
  file: modules/screen/dictionary/src/main/java/.../widget/ConfirmDeleteDictionaryWidget.kt
  action: "+"
  depends: [6]

- id: 14
  file: modules/screen/dictionary/src/main/java/.../widget/EmptyDictionaryWidget.kt
  action: "+"
  depends: []

# === СЛОЙ 7: Экран (собирает всё) ===

- id: 15
  file: modules/screen/dictionary/src/main/java/.../DictionaryScreen.kt
  action: "~"
  depends: [5, 6, 9, 10, 11, 12, 13, 14]

# === СЛОЙ 8: ViewModel ===

- id: 16
  file: modules/screen/dictionary/src/main/java/.../DictionaryViewModel.kt
  action: "~"
  depends: [3, 5, 7, 8]

# === СЛОЙ 9: Удалить старые файлы ===

- id: 17
  file: modules/screen/dictionary/src/main/java/.../DictionaryData.kt
  action: "-"
  depends: [15]

- id: 18
  file: modules/screen/dictionary/src/main/java/.../entity/PresetDictionaryUi.kt
  action: "-"
  depends: [15]

- id: 19
  file: modules/screen/dictionary/src/main/java/.../entity/DictionaryUpdateUi.kt
  action: "-"
  depends: [15]

- id: 20
  file: modules/screen/dictionary/src/main/java/.../widget/DictionaryPickerWidget.kt
  action: "-"
  depends: [15]

- id: 21
  file: modules/screen/dictionary/src/main/java/.../widget/DictionaryListWidget.kt
  action: "-"
  depends: [15]

- id: 22
  file: modules/screen/dictionary/src/main/java/.../widget/DictionaryItemWidget.kt
  action: "-"
  depends: [15]

- id: 23
  file: modules/screen/dictionary/src/main/java/.../widget/ListHeaderWidget.kt
  action: "-"
  depends: [15]

# === СЛОЙ 10: Строковые ресурсы ===

- id: 24
  file: core/core-resources/src/main/res/values/strings.xml
  action: "~"
  depends: []

- id: 25
  file: core/core-resources/src/main/res/values-ru-rRU/strings.xml
  action: "~"
  depends: []
```

---

## Детали

### #26 FlagProviderImpl.kt [~]

Расширить интерфейс и реализацию:
```kotlin
interface FlagProvider {
    suspend fun getFlagRes(numericCode: Int): Int             // есть
    fun getAllCountries(): List<CountryInfo>                   // добав��ть
    fun getLanguagesForCountry(numericCode: Int): List<String> // добавить
}

data class CountryInfo(
    val numericCode: Int,
    val name: String,
)
```

Реализация — делегирует в `World.getAllCountries()` и `World.getLanguagesFrom()`.

### #0 WordDao.kt [~]

Добавить:
```kotlin
@Query("UPDATE dictionaries SET name = :name, numericCode = :numericCode, changeDate = :changeDate WHERE id = :id")
suspend fun updateDictionary(id: Long, name: String, numericCode: Int?, changeDate: Long)

@Query("DELETE FROM dictionaries WHERE id = :id")
suspend fun deleteDictionary(id: Long)

@Query("SELECT * FROM dictionaries WHERE id = :id")
suspend fun getDictionaryById(id: Long): DictionaryDb?
```

### #1 CoreDbApi.kt [~]

DictionaryApi — добавить:
```kotlin
suspend fun getDictionary(id: Long): DictionaryApiEntity?
suspend fun updateDictionary(id: Long, name: String, numericCode: Int?)
suspend fun deleteDictionary(id: Long)
```
Изменить `addDictionary`: `numericCode: Int` → `numericCode: Int? = null`, name первым параметром.

### #2 CoreDbApiImpl.kt [~]

Реализовать новые методы DictionaryApi.

### #3 DictionaryUseCase (в DictionaryViewModel.kt) [~]

Полностью переписать интерфейс:
```kotlin
interface DictionaryUseCase {
    suspend fun getDictionaryList(): List<DictionaryListItem>
    suspend fun addDictionary(name: String, numericCode: Int?): Long
    suspend fun updateDictionary(id: Long, name: String, numericCode: Int?)
    suspend fun deleteDictionary(id: Long)
    suspend fun setCurrentDictionary(id: Long)
    fun getAvailableLanguages(): List<LanguageItem>
    suspend fun getCountriesForLanguage(languageCode: String): List<CountryFlagItem>
}
```

### #4 DictionaryUseCaseImpl.kt [~]

Реализовать все новые методы. Починить `saveCurrentDictionary` → `setCurrentDictionary(id: Long)` с `setLong`.

### #5 State.kt [~]

Полная замена. Новый стейт из `03_design_state.md`.

### #6 Message.kt [~]

Полная замена. Новые сообщения:
- UI: `OpenNewForm`, `OpenEditForm(id)`, `BackToList`, `NameChanged(value)`, `ToggleLanguageBound`, `OpenLanguagePicker`, `CloseLanguagePicker`, `LanguageQueryChanged(query)`, `SelectLanguage(item)`, `SelectFlag(item)`, `Save`, `Continue`
- Удаление: `RequestDelete(id, name)`, `ConfirmDelete`, `DismissDelete`
- Данные: `DictionariesLoaded(list)`, `FlagsLoaded(list)`, `LanguagesLoaded(list)`, `DictionarySaved`, `DictionaryDeleted`
- `Empty`

### #7 DictionaryReducer.kt [~]

Полная замена. Обработка всех новых сообщений через extension-функции стейта.

### #8 DatasourceEffectHandler.kt [~]

Полная замена. Новые эффекты:
- `LoadDictionaries`, `SaveDictionary(name, numericCode)`, `UpdateDictionary(id, name, numericCode)`, `DeleteDictionary(id)`, `SetCurrentDictionary(id)`, `LoadLanguages`, `LoadFlagsForLanguage(code)`

### #9-14 Новые виджеты [+]

По `03_design_compose.md`.

### #5 State.kt [~]

Полная замена стейта. Entity классы `DictionaryListItem`, `LanguageItem`, `CountryFlagItem` определяются здесь же (вспомогательные модели рядом со стейт-классами, по конвенции).

### #15 DictionaryScreen.kt [~]

Полная переработка: `AnimatedContent` с двумя ветками (LIST / FORM), условный AppBar, интеграция всех виджетов. Использует `DictionaryAppBar` из задачи 2 (уже существует, не меняется).

### #17-23 Удаление старых файлов [-]

`DictionaryData`, `PresetDictionaryUi`, `DictionaryUpdateUi`, `DictionaryPickerWidget`, `DictionaryListWidget`, `DictionaryItemWidget`, `ListHeaderWidget` — заменены новой архитектурой. `LoadingWidget` оставляем.

### #24-25 Строковые ресурсы [~]

10 новых строк (en + ru) из `03_design_compose.md`.

---

## Статистика

| Действие | Кол-во |
|----------|--------|
| [+] создание | 6 |
| [~] изменение | 14 |
| [-] удаление | 7 |
| **Всего** | **27** |
