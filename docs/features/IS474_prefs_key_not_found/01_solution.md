# Solution: IS474 — PrefsProvider throws on missing key

## Анализ вызовов

`getLongFlow(CURRENT_DICTIONARY_ID_LONG)` вызывается в:

| Файл | Использование |
|------|--------------|
| DictionaryAppBarUseCaseImpl:34 | `combine(prefsProvider.getLongFlow(...), ...)` — реактивное обновление AppBar |
| DictionaryTabUseCaseImpl:57 | `.getLongFlow(...)` — текущий словарь для списка |
| StatisticUseCaseImpl:23,39,55 | `.getLongFlow(...)` — текущий словарь для статистики |

Все подписываются через `combine`/`flatMapLatest`. При первом запуске ключ не существует → exception → краш.

`getIntFlow()` нигде не вызывается — но содержит ту же проблему.

## Варианты

### Вариант A: Nullable Flow

```kotlin
fun getLongFlow(prefKey: PrefKey): Flow<Long?> {
    return context.dataStore.data
        .map { it[longPreferencesKey(prefKey.value)] }
}
```

Flow эмитит `null` когда ключ отсутствует. Потребители сами решают что делать.

| | |
|---|---|
| Плюсы | Честный API — ключ может не существовать, и это ок |
| Минусы | Все 5 call sites нужно обновить — обработать null. Ломает текущий контракт `Flow<Long>` → `Flow<Long?>` |
| Сложность | MEDIUM |

### Вариант B: Default value

```kotlin
fun getLongFlow(prefKey: PrefKey, default: Long = -1L): Flow<Long> {
    return context.dataStore.data
        .map { it[longPreferencesKey(prefKey.value)] ?: default }
}
```

Возвращает дефолт когда ключ отсутствует. Аналогично `getBooleanFlow` (уже использует `?: false`).

| | |
|---|---|
| Плюсы | Не ломает контракт `Flow<Long>`. Потребители не меняются если дефолт подходит |
| Минусы | Magic value `-1L` — неочевидно. Для dictionary ID нет "правильного" дефолта |
| Сложность | LOW |

### Вариант C: filterNotNull

```kotlin
fun getLongFlow(prefKey: PrefKey): Flow<Long> {
    return context.dataStore.data
        .map { it[longPreferencesKey(prefKey.value)] }
        .filterNotNull()
}
```

Flow не эмитит пока ключ не записан. Первый emit — когда ключ появится.

| | |
|---|---|
| Плюсы | Не ломает контракт. Не крашит. Нет magic value. Потребители просто "ждут" данных |
| Минусы | При первом запуске UI может висеть без данных пока ключ не появится. Но SplashScreen уже устанавливает dictionary — к моменту подписки ключ обычно есть |
| Сложность | LOW |

## Рекомендация: Вариант A (Nullable Flow)

Обоснование:

1. **Честный API** — ключ может не существовать, и Flow это отражает через `null`
2. **Потребители явно обрабатывают null** — fallback на первый словарь, не тихое зависание UI
3. **Вариант C отклонён** — filterNotNull() маскирует отсутствие ключа, UI зависает без данных и без индикации
4. **Одинаковый фикс для Int и Long** — `getIntFlow()` фиксится так же

Также: исправить `getIntFlow()` аналогично (та же проблема, строка 24).

## Фикс

```kotlin
fun getLongFlow(prefKey: PrefKey): Flow<Long?> {
    return context.dataStore.data
        .map { it[longPreferencesKey(prefKey.value)] }
}

fun getIntFlow(prefKey: PrefKey): Flow<Int?> {
    return context.dataStore.data
        .map { it[intPreferencesKey(prefKey.value)] }
}
```

Удалить `throw IllegalStateException` из обоих методов. Потребители обрабатывают null с fallback.
