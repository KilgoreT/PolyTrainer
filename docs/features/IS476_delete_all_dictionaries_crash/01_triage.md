# Triage — IS476 / delete_all_dictionaries_crash

## 1. Корневая причина

### Где падает

`DictionaryAppBarUseCaseImpl.flowCurrentDict()` —
`app/src/main/java/me/apomazkin/polytrainer/di/module/widget/DictionaryAppBarUseCaseImpl.kt:46`

```kotlin
override fun flowCurrentDict(): Flow<DictUiEntity> {
    return combine(
            prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG),
            dictionaryApi.flowDictionaryList()
    ) { id, list ->
        (list.find { it.id == id } ?: list.firstOrNull())
                ?.let { dict -> DictUiEntity(...) }
                ?: throw DictionaryNotFoundException()   // <-- падение здесь
    }
}
```

Контракт типа `Flow<DictUiEntity>` non-null — поэтому при пустом `list` код кидает `DictionaryNotFoundException`. Исключение пробрасывается из reactive-flow и краш приложения.

> 📎 guide: docs/guides/prefs-datastore.md — "ЗАПРЕЩЕНО throw на null при чтении из Flow — приводит к крашу при первом запуске / очистке данных / race condition; вместо исключения использовать nullable Flow с fallback или filterNotNull"
> 📎 guide: docs/guides/prefs-datastore.md — "Nullable Flow обязывает к обработке null: ключ может не существовать (первый запуск, очистка данных, race condition) — fallback через `?:` или filterNotNull, никогда `!!` и никогда throw"

### Data flow от триггера до краша

1. Пользователь на экране `DictionaryListScreen` удаляет последний словарь.
2. `DictionaryListReducer` → `DictionaryListEffect.DeleteDictionary(id)`
   (`modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/list/DictionaryListReducer.kt:30`).
3. `DictionaryListEffectHandler.onEffect()` → `dictionaryUseCase.deleteDictionary(id)`
   (`modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/list/DictionaryListEffectHandler.kt:25-31`).
   CASCADE delete в Room.
4. Room emits empty `flowDictionaryList()` в обоих подписчиках:
   - `DictionaryAppBar.DatasourceEffectHandler.subscribe()` подписан на `useCase.flowCurrentDict()`
     (`modules/widget/dictionaryAppBar/src/main/java/me/apomazkin/dictionaryappbar/mate/DatasourceEffectHandler.kt:34-37`).
   - `DictionaryTab.DatasourceEffectHandler.subscribe()` подписан на `dictionaryTabUseCase.flowCurrentDict()`
     (`modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/logic/DatasourceEffectHandler.kt:42-46`).
5. `DictionaryAppBarUseCaseImpl.flowCurrentDict()`:
   `list.find { it.id == id } ?: list.firstOrNull()` → `null` (список пуст) → `throw DictionaryNotFoundException()`
   (`DictionaryAppBarUseCaseImpl.kt:46`).
6. Исключение прорывается через `collectLatest` корутины, не ловится и роняет процесс. Стектрейс совпадает с приведённым в задаче.

> 📎 guide: docs/guides/effect-handlers.md — "Flow останавливается в FlowHandler — дальше только Messages; Flow-операторы (debounce, distinctUntilChanged и др.) в UseCase или FlowHandler, не в Reducer и не в UI — соответственно ошибки реактивного источника обязан обрабатывать UseCase/FlowHandler, не пропуская исключения наружу"

### Почему именно AppBar (не DictionaryTab)

`DictionaryTabUseCaseImpl.flowCurrentDict()` кидает то же исключение
(`DictionaryTabUseCaseImpl.kt:69`), но во время удаления словарей пользователь
находится на `DictionaryListScreen` (точка входа из SettingsTab → DICTIONARY_LIST).
DictionaryTab при этом не активен, его подписки или не запущены, или его таб
прячется до того, как состояние обновится. Виджет `DictionaryAppBar` тоже не должен
быть виден на пустом списке (см. dictionary-list spec — "AppBar не показывается"),
но `DatasourceEffectHandler` AppBar'а в памяти остаётся живой как часть ViewModel
таба (vocabulary/quiz/statistic), который ранее был хост-экраном.

### Архитектурная первопричина

`flowCurrentDict()` спроектирован под молчаливое предположение «хотя бы один словарь
всегда есть». Реальность другая: пустой список — валидное состояние домена
(пустой стейт описан в `dictionary-list.md` явно). Контракт типа
`Flow<DictUiEntity>` не пропускает «нет словаря» как валидное событие, поэтому
эмиссия моделируется исключением — антипаттерн в реактивных стримах.

> 📎 guide: docs/guides/prefs-datastore.md — "Пример правильной обработки nullable id из prefs: `id?.let { dictionaryApi.getDictionaryById(it) } ?: dictionaryApi.getDictionaryList().firstOrNull()` — fallback через ?:, результат остаётся nullable вместо исключения"
> 📎 guide: docs/guides/state-and-extensions.md — "State = только отображаемое; явные поля для каждого UI-элемента — если данные могут отсутствовать (пустой список словарей), это валидное состояние UI и должно быть выражено nullable полем, а не моделироваться исключением"

## 2. Тип бага

**spec.**

Обоснование:
- Затрагивает минимум 2 файла (`DictionaryAppBarUseCaseImpl`, `DictionaryTabUseCaseImpl`),
  плюс интерфейсы `DictionaryAppBarUseCase`/`DictionaryTabUseCase`, плюс подписчики
  (FlowHandler'ы AppBar и DictionaryTab) и их редьюсеры/стейты (поле `currentDict`
  уже nullable в `DictionaryAppBarState`, но в `DictionaryTab` контракт `getCurrentDict()`
  non-null и используется в `DatasourceEffect.LoadTermFlow`).
- Поведение в коде расходится со спекой: `dictionary-list.md` явно описывает
  пустое состояние и заявляет «если удалённый был текущим → useCase переключает
  на первый оставшийся» — но не описывает, что должен делать `flowCurrentDict()`,
  когда «первого оставшегося» нет. `dictionary-appbar.md` тоже молчит про пустой
  список (state.currentDict уже `DictUiEntity?`, но Flow контракт non-null).
- Фикс требует обновления спек `dictionary-appbar.md` и `dictionary-list.md`
  (плюс, возможно, спеки DictionaryTab, когда она появится), чтобы зафиксировать
  поведение реактивного контракта при пустом списке словарей: эмиссия `null` /
  переход на пустое состояние вместо исключения.

`needs_spec_update = true`

## 3. Затронутые спеки

- `docs/features-spec/dictionary-appbar.md` — описывает виджет `DictionaryAppBar`
  и его реактивный контракт `flowCurrentDict()`. Прямо относится к багу: именно
  этот use case кидает исключение. Спека описывает «комбинацию Flow из
  SharedPreferences и Flow из Room», но не описывает пустой случай.
  Поле `state.currentDict` уже nullable (`DictUiEntity?`), что подсказывает
  направление фикса — пропускать `null` через Flow вместо исключения.

- `docs/features-spec/dictionary-list.md` — описывает экран удаления словарей и
  пустое состояние. Триггер бага — именно удаление последнего словаря отсюда.
  Текст «если удалённый был текущим → useCase переключает на первый оставшийся»
  неполный: не покрывает кейс «нет оставшегося». Нужно дополнить.

- Спека для `DictionaryTab` / `VocabularyTab` отсутствует
  (`docs/features-spec/README.md` упоминает «Словарь (DictionaryTab / VocabularyTab)»
  без ссылки). `DictionaryTabUseCaseImpl.flowCurrentDict()` страдает тем же
  дефектом, что и AppBar (`DictionaryTabUseCaseImpl.kt:69`). При фиксе через спеку
  стоит решить, фиксировать ли поведение DictionaryTab отдельной спекой или в
  README/общем разделе.

_model: claude-opus-4-7[1m]_
