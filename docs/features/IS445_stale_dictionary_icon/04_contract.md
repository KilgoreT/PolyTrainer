# IS445 — Контракт: Устаревшая иконка словаря в appbar picker

## State

> 📎 guide: docs/guides/state-and-extensions.md — "State = только отображаемое. Никаких навигационных флагов. Никаких кэшей данных для вычислений"

Структура `DictionaryAppBarState` **не меняется**. Баг находится на уровне UseCase (реактивность `flowCurrentDict()`), а не на уровне State.

```kotlin
@Immutable
data class DictionaryAppBarState(
    val isLoading: Boolean = true,
    val currentDict: DictUiEntity? = null,
    val availableDictList: List<DictUiEntity> = emptyList(),
    val isDropDownMenuOpen: Boolean = false,
)
```

#### isLoading: `Boolean`
- Дефолт: `true`

Индикатор загрузки. Скрывается после первого получения списка словарей.

---

#### currentDict: `DictUiEntity?`
- Дефолт: `null`

Текущий выбранный словарь. Отображается в appbar picker (иконка флага или placeholder). **Поле существующее — без изменений.** После фикса будет обновляться реактивно при смене флага словаря (ранее обновлялось только при смене ID словаря).

---

#### availableDictList: `List<DictUiEntity>`
- Дефолт: `emptyList()`

Список доступных словарей для выпадающего меню picker. Уже реактивный через Room Flow.

---

#### isDropDownMenuOpen: `Boolean`
- Дефолт: `false`

Состояние выпадающего меню выбора словаря.

---

### Ключевые инварианты

- `currentDict` всегда содержит актуальные данные словаря (включая `flagRes`) — обеспечивается реактивной подпиской в UseCase
- Если `currentDict` не найден в БД — fallback на первый словарь из списка, если и его нет — `DictionaryNotFoundException`
- `currentDict` обновляется как при смене ID (переключение словаря), так и при изменении данных текущего словаря (смена флага)

### Extensions

Все extension-функции **без изменений**:

#### showLoading
```kotlin
fun DictionaryAppBarState.showLoading(): DictionaryAppBarState
```
Установить `isLoading = true`.

---

#### hideLoading
```kotlin
fun DictionaryAppBarState.hideLoading(): DictionaryAppBarState
```
Установить `isLoading = false`.

---

#### availableDictList
```kotlin
fun DictionaryAppBarState.availableDictList(list: List<DictUiEntity>): DictionaryAppBarState
```
Обновить список доступных словарей.

---

#### currentDict
```kotlin
fun DictionaryAppBarState.currentDict(current: DictUiEntity): DictionaryAppBarState
```
Обновить текущий словарь.
Затрагивает: currentDict

---

#### dictMenuOn
```kotlin
fun DictionaryAppBarState.dictMenuOn(): DictionaryAppBarState
```
Открыть выпадающее меню.

---

#### dictMenuOff
```kotlin
fun DictionaryAppBarState.dictMenuOff(): DictionaryAppBarState
```
Закрыть выпадающее меню.

### Удаляемые поля и extensions

Нет удалений.

## UI Messages

> 📎 guide: docs/guides/messages.md — "Sealed interface гарантирует exhaustive when в редьюсере"

Все UI Messages **без изменений**. Reducer не затрагивается. Баг на уровне UseCase, а не на уровне сообщений.

### AvailableDict(list)
- Trigger: FlowHandler подписан на `useCase.flowAvailableDict()` — Room Flow эмитит при любом изменении таблицы словарей
- State changes: `hideLoading()`, `availableDictList(list)`
- Effects: нет

---

### CurrentDict(current)
- Trigger: FlowHandler подписан на `useCase.flowCurrentDict()` — **после фикса** будет эмитить как при смене ID словаря (prefs), так и при изменении данных текущего словаря (Room)
- State changes: `currentDict(current)`
- Effects: нет
- Примечание: именно этот Msg будет приходить чаще после фикса — при любом изменении текущего словаря, а не только при переключении

---

### ChangeDict(dict)
- Trigger: пользователь выбирает другой словарь в dropdown picker
- State changes: `dictMenuOff()`
- Effects: `DatasourceEffect.ChangeDict(dict)` — записывает новый ID в prefs

---

### DictMenuOn
- Trigger: пользователь нажимает на иконку словаря в appbar
- State changes: `dictMenuOn()`
- Effects: нет

---

### DictMenuOff
- Trigger: пользователь закрывает dropdown (dismiss)
- State changes: `dictMenuOff()`
- Effects: н��т

---

### Empty
- No-op fallback для хендлеров

---

### Удаляемые messages

Н��т удалений.

### Новые messages

Нет новых messages. Фикс на уровне UseCase — существующий `Msg.CurrentDict` будет получать обновления чаще.

## Effects

> 📎 guide: docs/guides/effect-handlers.md — "FlowHandler для подписок, обычный хендлер для одноразовых эффектов"

Все Effects **без изменений**. Единственный DatasourceEffect остаётся прежним.

### DatasourceEffect.ChangeDict(dict)
- Source: `Msg.ChangeDict` — пользователь выбирает другой словарь в dropdown
- Handler: `DatasourceEffectHandler.runEffect()`
- Action: `useCase.changeDict(id)` — записывает ID в SharedPreferences
- Результат: `Msg.Empty`

---

## Datasource Messages

Нет отдельных Datasource Messages. Данные приходят через FlowHandler подписки (`Msg.AvailableDict`, `Msg.CurrentDict`), а не через эффекты.

---

## FlowHandler

> 📎 guide: docs/guides/effect-handlers.md — "FlowHandler останавливает Flow — дальше только Messages"

### DatasourceEffectHandler (является MateFlowHandler)

Совмещает FlowHandler (подписки) и EffectHandler (одноразовые эффекты) в одном классе.

- **Подписка 1:** `useCase.flowAvailableDict()` → `Msg.AvailableDict(list)` — Room Flow, эмитит при любом изменении таблицы словарей. **Без изменений.**

- **Подписка 2:** `useCase.flowCurrentDict()` → `Msg.CurrentDict(current)` — **после фикса** будет реактивной:
  - **Было:** `prefsFlow.map { getDictionaryById(id) }` — одноразовый suspend-вызов, реагирует только на смену ID словаря в prefs
  - **Стало:** `combine(prefsFlow, flowDictionaryList()) { id, list -> list.find(id) }` — реактивный, реагирует на смену ID (prefs) И на изменение данных словаря (Room)

- **Эффекты:** `DatasourceEffect.ChangeDict` → `useCase.changeDict(id)` → `Msg.Empty`. **Без изменений.**

---

### Изменение в UseCase (не в FlowHandler)

Структура `DatasourceEffectHandler` **не меняется**. Фикс целиком в `DictionaryAppBarUseCaseImpl.flowCurrentDict()`:

**Было:**
```kotlin
fun flowCurrentDict(): Flow<DictUiEntity> =
    prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)
        .map { id ->
            dictionaryApi.getDictionaryById(id)  // одноразовый suspend
                ?.toDictUiEntity()
                ?: throw DictionaryNotFoundException()
        }
```

**Стало (вариант 1 — combine + flowDictionaryList):**
```kotlin
fun flowCurrentDict(): Flow<DictUiEntity> =
    combine(
        prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG),
        dictionaryApi.flowDictionaryList()
    ) { id, list ->
        list.find { it.id == id }?.toDictUiEntity()
            ?: list.firstOrNull()?.toDictUiEntity()
            ?: throw DictionaryNotFoundException()
    }
```

> Выбран вариант 1 (combine + flowDictionaryList) как наиболее простой: не требует нового DAO-метода, переиспользует существующий `flowDictionaryList()`. Недостаток — эмит при изменении **любого** словаря, не только текущего. Для виджета appbar с малым числом словарей это несущественно: distinctUntilChanged на уровне State отсечёт лишние recomposition.

### Удаляемые messages и effects

Нет удалений.

### Новые messages, effects, handlers

Нет новых. Фикс замкнут внутри `DictionaryAppBarUseCaseImpl.flowCurrentDict()`.

### Цепочки

1. **Смена флага словаря (фикс):**
   Пользователь меняет флаг в DictionaryForm → Room UPDATE → `flowDictionaryList()` эмитит → `combine` в `flowCurrentDict()` пересчитывает → FlowHandler получает новый `DictUiEntity` → `Msg.CurrentDict(updated)` → Reducer: `state.currentDict(updated)` → UI показывает актуальный флаг

2. **Переключение словаря (существующее):**
   Пользователь выбирает словарь в dropdown → `Msg.ChangeDict(dict)` → Reducer: `dictMenuOff()` + `DatasourceEffect.ChangeDict` → Handler: `useCase.changeDict(id)` → prefs пишет новый ID → `getLongFlow()` эмитит → `combine` в `flowCurrentDict()` пересчитывает → FlowHandler: `Msg.CurrentDict(newDict)` → Reducer: `state.currentDict(newDict)` → UI показывает иконку нового словаря

