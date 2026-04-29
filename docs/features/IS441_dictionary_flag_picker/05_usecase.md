# UseCase

## interface DictionaryUseCase

### Остающиеся методы

- `suspend fun getDictionaryList(): List<DictionaryListItem>` — возвращает список словарей. Используется экраном списка словарей. Effect: не связан с формой
- `fun flowDictionaryList(): Flow<List<DictionaryListItem>>` — реактивный поток списка словарей. Используется экраном списка словарей. Effect: не связан с формой
- `suspend fun addDictionary(name: String, numericCode: Int?): Long` — создаёт словарь в Room, возвращает ID. Effect: SaveDictionary
- `suspend fun updateDictionary(id: Long, name: String, numericCode: Int?)` — обновляет словарь в Room. Effect: UpdateDictionary
- `suspend fun deleteDictionary(id: Long)` — удаляет словарь. Используется экраном списка словарей. Effect: не связан с формой
- `suspend fun setCurrentDictionary(id: Long)` — устанавливает текущий словарь. Вызывается после addDictionary в цепочке SaveDictionary. Effect: SaveDictionary (часть цепочки)

### Удаляемые методы

- `fun getAvailableLanguages(): List<LanguageItem>` — удаляется. Привязка к языку убрана из формы. LanguageItem больше не используется формой
- `suspend fun getCountriesForLanguage(languageCode: String): List<CountryFlagItem>` — удаляется. Флаги загружаются все сразу через getAllCountryFlags(), фильтрация через поле фильтрации

### Новые методы интерфейса

- `fun updateFilter(query: String)` — обновляет текущий фильтр. Внутри UseCase хранится `MutableStateFlow<String>` для query. При вызове эмитит новое значение в filterQuery flow. Фильтрация происходит реактивно: `flagsFlow` комбинирует allFlags и filterQuery, применяет debounce ~300ms, фильтрует по countryName и languages (case-insensitive substring). Effect: FilterFlags(query) -> FlowHandler.runEffect -> useCase.updateFilter(query)
- `fun flagsFlow(): Flow<List<CountryFlagItem>>` — реактивный поток отфильтрованных флагов. Комбинирует allFlags и filterQuery с debounce. При пустом query эмитит все флаги. FlagFilterFlowHandler подписывается на этот flow и всегда эмитит FlagsUpdated
- `suspend fun getDictionary(id: Long): DictionaryItem` — загружает словарь по ID для предзаполнения формы редактирования. Effect: LoadDictionary(id)
- `fun findFlag(numericCode: Int): CountryFlagItem?` — ищет флаг в allFlags по numericCode. Вызывается EffectHandler при LoadDictionary, не reducer'ом

### DictionaryItem

```kotlin
data class DictionaryItem(
    val id: Long,
    val name: String,
    val numericCode: Int?,
)
```

Модель в `modules/screen/dictionary/model/`. Используется только для предзаполнения формы при редактировании.

### Приватные методы реализации (impl)

- `private fun loadAllFlags(): List<CountryFlagItem>` — загружает все флаги через `World.getAllCountries()`, обогащает языками через `countryProvider.getLanguagesForCountry(numericCode)`. Вызывается лениво при первом обращении к flagsFlow(). Синхронный (countryProvider in-memory). Результат кэшируется в `allFlags`. НЕ входит в интерфейс DictionaryUseCase

### Внутреннее состояние UseCase

UseCase хранит внутри:
- `allFlags: List<CountryFlagItem>` — полный список флагов, загруженный один раз через getAllCountryFlags(). Обогащён языками
- `filterQuery: MutableStateFlow<String>` — текущий текст фильтра. Начальное значение `""` (все флаги)

`flagsFlow()` реализуется как: filterQuery с debounce ~300ms -> map { query -> filterFlags(allFlags, query) }. Debounce ТОЛЬКО здесь — FlowHandler не добавляет свой debounce.

Логика фильтрации:
```kotlin
fun filterFlags(allFlags: List<CountryFlagItem>, query: String): List<CountryFlagItem> {
    if (query.isBlank()) return allFlags
    val q = query.trim().lowercase()
    return allFlags.filter { flag ->
        flag.countryName.lowercase().contains(q) ||
        flag.languages.any { lang -> lang.lowercase().contains(q) }
    }
}
```

### Связь Effect -> UseCase метод

| Effect | UseCase метод | Что происходит |
|--------|--------------|----------------|
| FilterFlags(query) | updateFilter(query) | Обновляет filterQuery -> flagsFlow эмитит -> FlagsUpdated |
| LoadDictionary(id) | getDictionary(id) + findFlag(numericCode) | Загружает словарь, резолвит флаг -> DictionaryLoaded(name, flag) |
| SaveDictionary(name, numericCode) | addDictionary(name, numericCode) + setCurrentDictionary(id) | Создаёт словарь, устанавливает текущим -> DictionarySaved |
| UpdateDictionary(id, name, numericCode) | updateDictionary(id, name, numericCode) | Обновляет словарь -> DictionarySaved |
| Close | — (NavigationEffectHandler) | onClose() |
| Back | — (NavigationEffectHandler) | onBackPress() |

_model: claude-opus-4-6[1m]_
