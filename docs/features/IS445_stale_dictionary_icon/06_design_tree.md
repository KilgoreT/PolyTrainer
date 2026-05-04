# IS445 — Design Tree: Устаревшая иконка словаря в appbar picker

## Граф

```yaml
- id: 0
  file: app/src/main/java/me/apomazkin/polytrainer/di/module/widget/DictionaryAppBarUseCaseImpl.kt
  action: "~"
  depends: []
```

## Детали

### #0 DictionaryAppBarUseCaseImpl.kt [~]

**Было:**
```kotlin
override fun flowCurrentDict(): Flow<DictUiEntity> {
    return prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)
        .map { id: Long ->
            val dict = (dictionaryApi.getDictionaryById(id)
                ?: dictionaryApi.getDictionaryList().firstOrNull())
                ?.let { dict -> DictUiEntity(...) }
            dict ?: throw DictionaryNotFoundException()
        }
}
```

**Стало:**
```kotlin
override fun flowCurrentDict(): Flow<DictUiEntity> {
    return combine(
        prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG),
        dictionaryApi.flowDictionaryList()
    ) { id, list ->
        val found = list.find { it.id == id } ?: list.firstOrNull()
        found?.let { dict -> DictUiEntity(id, flagRes, title, numericCode) }
            ?: throw DictionaryNotFoundException()
    }
}
```

Замена `map` на `combine(prefsFlow, flowDictionaryList())`. Импорт `kotlinx.coroutines.flow.combine` вместо неиспользуемого `map`. Удаление suspend-вызовов `getDictionaryById` и `getDictionaryList` — вместо них реактивный `flowDictionaryList()`.

## log_messages
- Design tree содержит 1 узел: изменение flowCurrentDict() в DictionaryAppBarUseCaseImpl
- Граф тривиален — один файл, нет зависимостей

## checklist_items
Существующие пункты чеклиста покрывают design tree полностью. Новых пунктов не требуется.

_model: claude-opus-4-6_
