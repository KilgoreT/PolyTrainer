# Design Tree: IS474 — PrefsProvider nullable Flow

## Graph

```yaml
- id: 0
  file: modules/datasource/prefs/src/main/java/me/apomazkin/prefs/PrefsProvider.kt
  action: "~"
  depends: []

- id: 1
  file: app/src/main/java/me/apomazkin/polytrainer/di/module/widget/DictionaryAppBarUseCaseImpl.kt
  action: "~"
  depends: [0]

- id: 2
  file: app/src/main/java/me/apomazkin/polytrainer/di/module/dictionarytab/DictionaryTabUseCaseImpl.kt
  action: "~"
  depends: [0]

- id: 3
  file: app/src/main/java/me/apomazkin/polytrainer/di/module/statistictab/StatisticUseCaseImpl.kt
  action: "~"
  depends: [0]

- id: 4
  file: app/src/test/java/me/apomazkin/polytrainer/di/module/widget/DictionaryAppBarUseCaseImplTest.kt
  action: "~"
  depends: [0]
```

## Details

### #0 PrefsProvider.kt [~]

Убрать `throw IllegalStateException` из `getLongFlow()` и `getIntFlow()`. Вернуть nullable:

**getLongFlow — было:**
```kotlin
fun getLongFlow(prefKey: PrefKey): Flow<Long> {
    return context.dataStore.data
        .map {
            it[longPreferencesKey(prefKey.value)]
                ?: throw IllegalStateException("PrefKey $prefKey not found")
        }
}
```

**getLongFlow — стало:**
```kotlin
fun getLongFlow(prefKey: PrefKey): Flow<Long?> {
    return context.dataStore.data
        .map { it[longPreferencesKey(prefKey.value)] }
}
```

**getIntFlow — было:**
```kotlin
fun getIntFlow(prefKey: PrefKey): Flow<Int> {
    return context.dataStore.data
        .map {
            it[intPreferencesKey(prefKey.value)]
                ?: throw IllegalStateException("PrefKey $prefKey not found")
        }
}
```

**getIntFlow — стало:**
```kotlin
fun getIntFlow(prefKey: PrefKey): Flow<Int?> {
    return context.dataStore.data
        .map { it[intPreferencesKey(prefKey.value)] }
}
```

`getBooleanFlow` — не трогаем, уже безопасный (`?: false`).

### #1 DictionaryAppBarUseCaseImpl.kt [~]

Строка 34: `combine(prefsProvider.getLongFlow(...), dictionaryListFlow) { id, list -> ... }`

`id` теперь `Long?`. Логика fallback уже есть на строке 37:
```kotlin
(list.find { it.id == id } ?: list.firstOrNull())
```

Когда `id == null` — `list.find { it.id == null }` не найдёт → fallback на `list.firstOrNull()`. Корректно — при null id берём первый словарь.

**Изменение:** только тип в лямбде `{ id: Long?, list -> ... }`. Логика не меняется — fallback работает.

### #2 DictionaryTabUseCaseImpl.kt [~]

Строка 57-58:
```kotlin
prefsProvider.getLongFlow(...)
    .map { id: Long -> ... }
```

`id` теперь `Long?`. Внутри map вызывается `dictionaryApi.getDictionaryById(id)` — `id` nullable.

**Было:**
```kotlin
.map { id: Long ->
    val dict = (dictionaryApi.getDictionaryById(id) ?: dictionaryApi.getDictionaryList().firstOrNull())
```

**Стало:**
```kotlin
.map { id: Long? ->
    val dict = (id?.let { dictionaryApi.getDictionaryById(it) } ?: dictionaryApi.getDictionaryList().firstOrNull())
```

При `id == null` → пропускаем `getDictionaryById`, сразу fallback на первый словарь.

### #3 StatisticUseCaseImpl.kt [~]

Строки 23, 39, 55 — три метода с одинаковым паттерном:
```kotlin
prefsProvider.getLongFlow(...)
    .mapLatest { id ->
        val dictionaryId = dictionaryApi.getDictionaryById(id)?.id?.toInt()
            ?: dictionaryApi.getDictionaryList().firstOrNull()?.id?.toInt()
        dictionaryId
    }
```

`id` теперь `Long?`. `getDictionaryById(id)` — `id` nullable.

**Было:**
```kotlin
.mapLatest { id ->
    val dictionaryId = dictionaryApi
            .getDictionaryById(id)?.id?.toInt()
            ?: dictionaryApi.getDictionaryList().firstOrNull()?.id?.toInt()
    dictionaryId
}
```

**Стало:**
```kotlin
.mapLatest { id ->
    val dictionaryId = id?.let { dictionaryApi.getDictionaryById(it)?.id?.toInt() }
            ?: dictionaryApi.getDictionaryList().firstOrNull()?.id?.toInt()
    dictionaryId
}
```

При `id == null` → пропускаем `getDictionaryById`, сразу fallback. Три метода — одинаковое изменение.

### #4 DictionaryAppBarUseCaseImplTest.kt [~]

Файл: `app/src/test/java/me/apomazkin/polytrainer/di/module/widget/DictionaryAppBarUseCaseImplTest.kt`

Мок `prefsProvider.getLongFlow()` возвращает `MutableStateFlow<Long>` — после изменения нужен `MutableStateFlow<Long?>`.

**Было:**
```kotlin
val prefsFlow = MutableStateFlow<Long>(...)
coEvery { prefsProvider.getLongFlow(any()) } returns prefsFlow
```

**Стало:**
```kotlin
val prefsFlow = MutableStateFlow<Long?>(...)
coEvery { prefsProvider.getLongFlow(any()) } returns prefsFlow
```

Добавить тест-кейс: "flowCurrentDict falls back to first dict when prefs ID is null" — `prefsFlow.value = null`.

## Notes

- `getIntFlow` фиксится аналогично, но сейчас нигде не вызывается — превентивный фикс
- `getBooleanFlow` уже безопасный — не трогаем
- Все потребители уже имеют fallback логику (первый словарь из списка) — null-обработка минимальна
