# IS445 — UseCase: Устаревшая иконка словаря в appbar picker

## UseCase

### interface DictionaryAppBarUseCase

> 📎 guide: docs/guides/effect-handlers.md — "FlowHandler останавливает Flow — дальше только Messages"

Интерфейс **не меняется**. Сигнатуры остаются прежними. Меняется только реализация `flowCurrentDict()`.

- `fun flowAvailableDict(): Flow<List<DictUiEntity>>` — реактивная подписка на список всех словарей. Room Flow, эмитит при любом изменении таблицы словарей. Effect: подписка в DatasourceEffectHandler (FlowHandler). **Без изменений.**

- `fun flowCurrentDict(): Flow<DictUiEntity>` — реактивная подписка на текущий словарь. Effect: подписка в DatasourceEffectHandler (FlowHandler) → `Msg.CurrentDict(current)`.
  - **Было:** `prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG).map { id -> getDictionaryById(id) }` — одноразовый suspend, реагирует только на смену ID.
  - **Стало:** `combine(prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG), dictionaryApi.flowDictionaryList()) { id, list -> ... }` — реактивный, реагирует на смену ID (prefs) И на изменение данных словаря (Room).
  - Fallback: если словарь с текущим ID не найден → первый из списка → если список пуст → `DictionaryNotFoundException`.

- `suspend fun changeDict(id: Long)` — записывает ID выбранного словаря в SharedPreferences. Effect: `DatasourceEffect.ChangeDict`. **Без изменений.**

### Изменение реализации: DictionaryAppBarUseCaseImpl.flowCurrentDict()

Единственное изменение — замена `map` на `combine`:

```kotlin
// БЫЛО:
override fun flowCurrentDict(): Flow<DictUiEntity> {
    return prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)
        .map { id: Long ->
            val dict = (dictionaryApi.getDictionaryById(id)
                ?: dictionaryApi.getDictionaryList().firstOrNull())
                ?.let { dict ->
                    DictUiEntity(
                        id = dict.id,
                        flagRes = dict.numericCode?.let { countryProvider.getFlagRes(it) } ?: 0,
                        title = dict.name,
                        numericCode = dict.numericCode ?: 0,
                    )
                }
            dict ?: throw DictionaryNotFoundException()
        }
}

// СТАЛО:
override fun flowCurrentDict(): Flow<DictUiEntity> {
    return combine(
        prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG),
        dictionaryApi.flowDictionaryList()
    ) { id, list ->
        list.find { it.id == id }?.let { dict ->
            DictUiEntity(
                id = dict.id,
                flagRes = dict.numericCode?.let { countryProvider.getFlagRes(it) } ?: 0,
                title = dict.name,
                numericCode = dict.numericCode ?: 0,
            )
        }
        ?: list.firstOrNull()?.let { dict ->
            DictUiEntity(
                id = dict.id,
                flagRes = dict.numericCode?.let { countryProvider.getFlagRes(it) } ?: 0,
                title = dict.name,
                numericCode = dict.numericCode ?: 0,
            )
        }
        ?: throw DictionaryNotFoundException()
    }
}
```

### Зависимости UseCase

| Зависимость | Тип | Что используется |
|-------------|-----|-----------------|
| `CoreDbApi.DictionaryApi` | interface | `flowDictionaryList()`, без нового DAO-метода |
| `PrefsProvider` | interface | `getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)`, `setLong(...)` |
| `CountryProvider` | interface | `getFlagRes(numericCode)` |

Новых зависимостей нет. Новых DAO-методов не требуется.

### Маппинг Effect → UseCase method

| Effect / FlowHandler подписка | UseCase метод | Направление |
|-------------------------------|---------------|-------------|
| FlowHandler подписка 1 (Room) | `flowAvailableDict()` | данные → UI |
| FlowHandler подписка 2 (prefs + Room) | `flowCurrentDict()` | данные → UI |
| `DatasourceEffect.ChangeDict` | `changeDict(id)` | UI → данные |

## log_messages
- UseCase интерфейс DictionaryAppBarUseCase не меняется, фикс замкнут в реализации flowCurrentDict()
- Замена map на combine(prefsFlow, flowDictionaryList) обеспечивает реактивность при смене флага словаря
- Новых DAO-методов не требуется — переиспользуется существующий flowDictionaryList()

## checklist_items
- root: "Пользователь меняет флаг словаря в форме → appbar picker показывает актуальный флаг без переключения словарей"
  items:
    - UseCase интерфейс DictionaryAppBarUseCase сохраняет прежние сигнатуры (3 метода)
    - DictionaryAppBarUseCaseImpl.flowCurrentDict() использует combine вместо map
    - Новых зависимостей и DAO-методов не добавляется

_model: claude-opus-4-6_
