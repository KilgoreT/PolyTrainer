# IS441. Задача 3 — Данные и UseCase

## Текущий UseCase

```kotlin
interface DictionaryUseCase {
    suspend fun getFlagRes(numericCode: Int): Int
    suspend fun addDictionary(numericCode: Int, name: String): Long
    suspend fun saveCurrentDictionary(numericCode: Int)
}
```

Мало — умеет только создавать и получать флаг. Нужно расширить.

---

## Новый UseCase (расширенный)

```kotlin
interface DictionaryUseCase {
    // === Словари ===
    suspend fun getDictionaryList(): List<DictionaryItem>
    suspend fun addDictionary(name: String, numericCode: Int?, description: String?): Long
    suspend fun updateDictionary(id: Long, name: String, numericCode: Int?, description: String?)
    suspend fun deleteDictionary(id: Long)
    suspend fun saveCurrentDictionary(id: Long)

    // === Языки (для пикера) ===
    fun getAvailableLanguages(): List<LanguageItem>

    // === Флаги (для пикера) ===
    suspend fun getCountriesForLanguage(languageCode: String): List<CountryFlagItem>
}
```

---

## Операции по одной

### 1. Получить список словарей

**Откуда:** `CoreDbApi.DictionaryApi.getDictionaryList()` → `List<DictionaryApiEntity>`

**Маппинг:**
```kotlin
data class DictionaryItem(
    val id: Long,
    val name: String,
    val numericCode: Int?,       // для получения флага
    val flagRes: Int?,           // drawable флага (null если нет numericCode)
    val languageName: String?,   // отображаемое название языка (null если не привязан)
)
```

**Логика в UseCaseImpl:**
```kotlin
override suspend fun getDictionaryList(): List<DictionaryItem> {
    return dictionaryApi.getDictionaryList().map { entity ->
        DictionaryItem(
            id = entity.id,
            name = entity.name,
            numericCode = entity.numericCode,
            flagRes = entity.numericCode?.let { flagProvider.getFlagRes(it) },
            languageName = entity.numericCode?.let { resolveLanguageName(it) },
        )
    }
}
```

`resolveLanguageName(numericCode)` — через blongho `World.getCountryFrom(numericCode)` → `World.getLanguagesFrom(numericCode)` → первый язык. Или из Locale.

### 2. Создать словарь

**Откуда:** `CoreDbApi.DictionaryApi.addDictionary(numericCode, name)` → `Long` (id)

**Текущий API проблема:** `addDictionary(numericCode: Int, name: String)` — numericCode обязательный Int. Нужно сделать nullable:

```kotlin
// CoreDbApi.DictionaryApi — нужно изменить:
suspend fun addDictionary(name: String, numericCode: Int? = null): Long
```

**После создания:**
```kotlin
override suspend fun addDictionary(name: String, numericCode: Int?, description: String?): Long {
    val id = dictionaryApi.addDictionary(name, numericCode)
    saveCurrentDictionary(id)  // сделать текущим
    return id
}
```

### 3. Обновить словарь

**Нет в текущем API.** Нужно добавить:

```kotlin
// CoreDbApi.DictionaryApi:
suspend fun updateDictionary(id: Long, name: String, numericCode: Int?)

// WordDao:
@Query("UPDATE dictionaries SET name = :name, numericCode = :numericCode, changeDate = :changeDate WHERE id = :id")
suspend fun updateDictionary(id: Long, name: String, numericCode: Int?, changeDate: Long)
```

### 4. Удалить словарь

**Нет в текущем API.** Нужно добавить:

```kotlin
// CoreDbApi.DictionaryApi:
suspend fun deleteDictionary(id: Long)

// WordDao:
@Query("DELETE FROM dictionaries WHERE id = :id")
suspend fun deleteDictionary(id: Long)
```

FK CASCADE удалит words → lexemes → write_quiz автоматически.

**После удаления:**
```kotlin
override suspend fun deleteDictionary(id: Long) {
    dictionaryApi.deleteDictionary(id)
    // Если удалённый был текущим — переключиться на первый оставшийся
    val currentId = prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)
    if (currentId == id) {
        val remaining = dictionaryApi.getDictionaryList().firstOrNull()
        remaining?.let { saveCurrentDictionary(it.id) }
    }
}
```

### 5. Сохранить текущий словарь

**Есть, но неправильная сигнатура.** Сейчас принимает `numericCode: Int`, нужно `id: Long`:

```kotlin
// Сейчас:
override suspend fun saveCurrentDictionary(numericCode: Int) {
    prefsProvider.setInt(PrefKey.CURRENT_DICTIONARY_ID_LONG, numericCode)
}

// Нужно:
override suspend fun saveCurrentDictionary(id: Long) {
    prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, id)
}
```

**Возражение:** это баг из задачи 1 — переименовали ключ но не поменяли тип (Int → Long) и семантику (numericCode → id). Нужно починить.

### 6. Получить список языков (для пикера)

**Источник:** `java.util.Locale` (встроено в JDK, без БД).

```kotlin
data class LanguageItem(
    val code: String,          // ISO 639: "es", "en", "fr"
    val displayName: String,   // на языке устройства: "Испанский", "English"
)

override fun getAvailableLanguages(): List<LanguageItem> {
    val currentLocale = Locale.getDefault()
    return Locale.getISOLanguages()
        .map { code -> Locale(code) }
        .map { locale ->
            LanguageItem(
                code = locale.language,
                displayName = locale.getDisplayLanguage(currentLocale)
                    .replaceFirstChar { it.uppercase() },
            )
        }
        .filter { it.displayName.isNotBlank() }
        .sortedBy { it.displayName }
}
```

Фильтрация в UI: `languages.filter { it.displayName.contains(query, ignoreCase = true) }`

### 7. Получить флаги стран для языка

**Источник:** `blongho World.getAllCountries()` + `World.getLanguagesFrom()` + `World.getFlagOf()`

```kotlin
data class CountryFlagItem(
    val numericCode: Int,
    val countryName: String,
    val flagRes: Int,
)

override suspend fun getCountriesForLanguage(languageCode: String): List<CountryFlagItem> {
    val languageName = Locale(languageCode)
        .getDisplayLanguage(Locale.ENGLISH)
        .lowercase()

    return World.getAllCountries()
        .filter { country ->
            World.getLanguagesFrom(country.id)
                .any { it.lowercase().contains(languageName) }
        }
        .map { country ->
            CountryFlagItem(
                numericCode = country.id,
                countryName = country.name,
                flagRes = World.getFlagOf(country.id),
            )
        }
}
```

---

## Изменения в API слоях

### CoreDbApi.DictionaryApi — добавить методы

```kotlin
interface DictionaryApi {
    suspend fun addDictionary(name: String, numericCode: Int? = null): Long  // изменить сигнатуру
    suspend fun getDictionary(id: Long): DictionaryApiEntity?                // добавить по id
    suspend fun getDictionaryList(): List<DictionaryApiEntity>               // есть
    suspend fun updateDictionary(id: Long, name: String, numericCode: Int?) // добавить
    suspend fun deleteDictionary(id: Long)                                   // добавить
    fun flowDictionaryList(): Flow<List<DictionaryApiEntity>>               // есть
}
```

### WordDao — добавить методы

```kotlin
@Query("UPDATE dictionaries SET name = :name, numericCode = :numericCode, changeDate = :changeDate WHERE id = :id")
suspend fun updateDictionary(id: Long, name: String, numericCode: Int?, changeDate: Long)

@Query("DELETE FROM dictionaries WHERE id = :id")
suspend fun deleteDictionary(id: Long)

@Query("SELECT * FROM dictionaries WHERE id = :id")
suspend fun getDictionaryById(id: Long): DictionaryDb?
```

### FlagProvider — расширить

```kotlin
interface FlagProvider {
    suspend fun getFlagRes(numericCode: Int): Int                // есть
    fun getAllCountries(): List<Country>                          // добавить
    fun getLanguagesForCountry(numericCode: Int): List<String>   // добавить
}
```

Или вынести работу с `World` напрямую в UseCaseImpl (через `World.getAllCountries()` static-вызовы). FlagProvider тогда не расширяем — вызываем World напрямую в UseCase.

---

## Баги для починки попутно

1. **`saveCurrentDictionary(numericCode: Int)`** → нужно `saveCurrentDictionary(id: Long)` + `setLong` вместо `setInt`
2. **`addDictionary(numericCode: Int, name: String)`** → сигнатура: name первый, numericCode nullable
3. **`getDictionary(numericCode: Int)`** → нужен ещё `getDictionary(id: Long)` — поиск по id, не по numericCode
