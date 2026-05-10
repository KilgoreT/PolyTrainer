# Task

## IS474. Crash: PrefsProvider throws when PrefKey not found

Краш в продакшне. Версия 0.1.3.

## Стектрейс

```
java.lang.IllegalStateException: PrefKey CURRENT_DICTIONARY_ID_LONG not found
    at me.apomazkin.prefs.PrefsProvider$getLongFlow$$inlined$map$1$2.emit
    at androidx.datastore.core.DataStoreImpl$data$1.invokeSuspend
```

## Суть

`PrefsProvider.getLongFlow()` (строка 42) бросает `IllegalStateException` когда ключ не найден в DataStore:

```kotlin
fun getLongFlow(prefKey: PrefKey): Flow<Long> {
    return context.dataStore.data
        .map {
            it[longPreferencesKey(prefKey.value)]
                ?: throw IllegalStateException("PrefKey $prefKey not found")
        }
}
```

Происходит при первом запуске или чистых данных — ключ `CURRENT_DICTIONARY_ID_LONG` ещё не записан.

## Непоследовательность

| Метод | Поведение при отсутствии ключа |
|-------|-------------------------------|
| `getIntFlow()` | `throw IllegalStateException` ← краш |
| `getLongFlow()` | `throw IllegalStateException` ← краш |
| `getBooleanFlow()` | `?: false` ← дефолт, безопасно |
| `getInt()` | `return null` ← безопасно |
| `getLong()` | `return null` ← безопасно |
| `getBoolean()` | `return null` ← безопасно |

Flow-версии Int и Long крашат, все остальные — безопасны.
