# Preferences (DataStore)

## Модуль

`modules/datasource/prefs` — пакет `me.apomazkin.prefs`.

Единственный entry point для работы с локальными настройками. Обёртка над Jetpack DataStore.

## Архитектура

```
PrefsProvider (class)
    └── DataStore<Preferences> (delegate property)
            └── файл: lexemePrefStore (на устройстве)
```

```kotlin
class PrefsProvider(private val context: Context) {
    private val Context.dataStore: DataStore<Preferences> 
        by preferencesDataStore(name = "lexemePrefStore")
}
```

- Один файл на всё приложение: `lexemePrefStore`
- Провайдится как `@Singleton` через `PrefsProviderModule` в AppComponent
- Не создавать вручную — только через DI

## DI

```kotlin
// PrefsProviderModule.kt (app module)
@Module
class PrefsProviderModule {
    @Singleton
    @Provides
    fun providePrefsProvider(context: Context): PrefsProvider = PrefsProvider(context)
}
```

Доступен во всех UseCase'ах через constructor injection.

## API

### Flow-подписки (реактивные)

Используются для реактивных подписок в UseCase'ах и FlowHandler'ах. Эмитят при каждом изменении значения.

```kotlin
fun getLongFlow(prefKey: PrefKey): Flow<Long?>     // nullable — ключ может не существовать
fun getIntFlow(prefKey: PrefKey): Flow<Int?>       // nullable — ключ может не существовать
fun getBooleanFlow(prefKey: PrefKey): Flow<Boolean> // не nullable — дефолт false
```

### Одноразовые чтения (suspend)

Используются для разовых проверок в EffectHandler'ах.

```kotlin
suspend fun getLong(prefKey: PrefKey): Long?       // null = ключ не существует
suspend fun getInt(prefKey: PrefKey): Int?         // null = ключ не существует
suspend fun getBoolean(prefKey: PrefKey): Boolean? // null = ключ не существует
```

### Запись (suspend)

```kotlin
suspend fun setLong(prefKey: PrefKey, value: Long)
suspend fun setInt(prefKey: PrefKey, value: Int)
suspend fun setBoolean(prefKey: PrefKey, value: Boolean)
```

## Ключи

```kotlin
enum class PrefKey(val value: String) {
    CURRENT_DICTIONARY_ID_LONG("LONG_currentDictionaryId"),
    CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN("Boolean_chatEarliestReviewedStatus"),
    CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN("Boolean_chatFrequentMistakesStatus"),
    CHAT_DEBUG_STATUS_BOOLEAN("Boolean_chatDebugStatus")
}
```

### Конвенция именования

`TYPE_camelCaseName` — тип значения в префиксе:
- `LONG_` для Long
- `Boolean_` для Boolean
- `INT_` для Int (пока не используется)

### Кто использует какие ключи

| Ключ | Чтение (Flow) | Чтение (suspend) | Запись |
|------|--------------|-------------------|-------|
| `CURRENT_DICTIONARY_ID_LONG` | DictionaryAppBarUseCase, DictionaryTabUseCase, StatisticUseCase | WordCardUseCase, QuizChatUseCase, DictionaryUseCase, DictionaryTabUseCase | DictionaryAppBarUseCase, QuizChatUseCase, DictionaryUseCase, DictionaryTabUseCase |
| `CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN` | AppBarFlowHandler | QuizChatUseCase | DatasourceEffectHandler (chat) |
| `CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN` | AppBarFlowHandler | QuizChatUseCase | DatasourceEffectHandler (chat) |
| `CHAT_DEBUG_STATUS_BOOLEAN` | AppBarFlowHandler | QuizGameImpl | DatasourceEffectHandler (chat) |

## Правила

### 1. Nullable Flow — ОБЯЗАТЕЛЬНАЯ обработка null

`getLongFlow()` и `getIntFlow()` возвращают nullable. Ключ может не существовать при:
- Первом запуске приложения
- Очистке данных приложения
- Race condition (UI подписался раньше чем splash записал ключ)

```kotlin
// ПРАВИЛЬНО — fallback при null:
prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)
    .map { id: Long? ->
        id?.let { dictionaryApi.getDictionaryById(it) }
            ?: dictionaryApi.getDictionaryList().firstOrNull()
    }

// ПРАВИЛЬНО — filterNotNull если null невозможен по логике:
prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)
    .filterNotNull()
    .flatMapLatest { id -> ... }

// ЗАПРЕЩЕНО — force unwrap:
prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)
    .map { id -> dictionaryApi.getDictionaryById(id!!) }  // ← NPE

// ЗАПРЕЩЕНО — throw на null:
it[longPreferencesKey(prefKey.value)]
    ?: throw IllegalStateException("not found")  // ← краш при первом запуске
```

### 2. Boolean Flow — дефолт false

`getBooleanFlow()` возвращает `Flow<Boolean>` (не nullable) с дефолтом `false`. Toggle-флаги по умолчанию выключены.

### 3. Запись перед чтением

Для ключей, от которых зависит UI (например `CURRENT_DICTIONARY_ID_LONG`):
- Запись происходит на **SplashScreen** при первом запуске
- К моменту подписки ключ **обычно** уже есть
- Nullable Flow — **страховка** на race condition

### 4. Не кешировать

```kotlin
// ЗАПРЕЩЕНО — кеш в переменную:
val currentDictId = prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)
// ... позже используется устаревшее значение

// ПРАВИЛЬНО — подписка через Flow:
prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)
    .collect { id -> /* всегда актуальное */ }
```

DataStore сам нотифицирует об изменениях через Flow.

### 5. Не использовать DataStore напрямую

Весь доступ — через `PrefsProvider`. Не создавать второй DataStore, не обращаться к `context.dataStore` из других мест.

### 6. Новый ключ — чеклист

1. Добавить в `PrefKey` enum с конвенцией `TYPE_camelCaseName`
2. Определить: нужен ли Flow или только suspend?
3. Определить: кто пишет? кто читает?
4. Если Flow — обработать null для Long/Int
5. Обновить таблицу "Кто использует какие ключи" в этом гайде

## Потоки данных

### CURRENT_DICTIONARY_ID_LONG — жизненный цикл

```
1. SplashScreen → checkIfNeedAddDictionary()
   └── нет словарей → создать → setLong(CURRENT_DICTIONARY_ID_LONG, id)
   └── есть словари → setLong(CURRENT_DICTIONARY_ID_LONG, firstDict.id)

2. DictionaryAppBarUseCase → changeDict(id)
   └── setLong(CURRENT_DICTIONARY_ID_LONG, id)

3. DictionaryTabUseCase → changeDictionary(id) / deleteDictionary(id)
   └── setLong(CURRENT_DICTIONARY_ID_LONG, newId)

4. Подписчики (getLongFlow):
   ├── DictionaryAppBarUseCase.flowCurrentDict()
   ├── DictionaryTabUseCase.flowCurrentDict()
   └── StatisticUseCase.flowWordCount/flowLexemeCount/flowQuizStat()
```

### Boolean ключи — жизненный цикл

```
1. DatasourceEffectHandler (chat) → toggle on/off
   └── setBoolean(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN, true/false)

2. Подписчики (getBooleanFlow):
   └── AppBarFlowHandler → combine трёх boolean Flow → обновление AppBar состояния
```

## Ограничения DataStore

- **Один файл** — все preferences в одном файле `lexemePrefStore`. При большом количестве ключей — все подписчики нотифицируются при любом изменении.
- **Main thread safe** — DataStore работает на IO dispatcher внутри. Но `edit {}` — suspend, вызывать из корутины.
- **Нет миграции** — при добавлении/удалении ключей миграция не нужна (key-value store). Старые ключи просто игнорируются.
- **Нет типизации на уровне компиляции** — `PrefKey` enum не привязан к типу значения. Можно вызвать `getLong(BOOLEAN_KEY)` — компилятор не поймает. Конвенция именования (`TYPE_name`) — единственная защита.
