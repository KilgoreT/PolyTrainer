# Dictionary AppBar — Спецификация

Виджет выбора текущего словаря, встраивается в AppBar вкладок (vocabulary tab, quiz tab, statistic tab).

---

## Назначение

Позволяет пользователю видеть текущий словарь (иконка флага или placeholder) и переключаться между словарями через dropdown-меню.

---

## Состояние (State)

- `isLoading` — начальная загрузка данных
- `currentDict` — текущий словарь (`DictUiEntity?`). `null` — валидное состояние: словарей в системе нет.
- `availableDictList` — список всех словарей (может быть пустым)
- `isDropDownMenuOpen` — развёрнуто ли dropdown-меню

---

## Иконка текущего словаря

- Если у словаря задан `flagRes` (ненулевой) — `ImageFlagWidget` с флагом страны
- Если флаг не задан — `FlagPlaceholderWidget` (первая буква названия на сером фоне)
- Если `currentDict == null` — иконка не отображается (виджет в этом состоянии не рендерится хост-экраном; см. «Пустой список словарей»)

Иконка **реактивно обновляется** при изменении данных словаря (смена флага, переименование). Источник: реактивный Flow, подписанный на изменения в БД.

---

## Реактивность данных

### Список словарей

`flowAvailableDict()` — Room Flow на таблицу словарей. Автоматически обновляется при любых CRUD-операциях со словарями. Может эмитить пустой список — это валидное состояние.

### Текущий словарь

`flowCurrentDict(): Flow<DictUiEntity?>` — реактивный Flow с nullable-эмиссией.

Контракт эмиссий:

1. Список словарей непуст, в prefs записан id существующего словаря → эмит `DictUiEntity` найденного словаря.
2. Список словарей непуст, в prefs записан id несуществующего словаря (orphaned reference) или pref отсутствует → эмит `DictUiEntity` первого словаря в списке (fallback).
3. **Список словарей пуст** → эмит `null`.

Эмиссии происходят при:

- смене id текущего словаря в prefs (переключение через dropdown);
- изменении данных текущего словаря в БД (смена флага, переименование);
- появлении / исчезновении словарей в БД (включая удаление последнего).

Реализация — комбинация Flow из SharedPreferences (`Long?`) и Flow из Room (`List<DictionaryItem>`). Пустой список словарей — валидное доменное состояние, моделируется `null`, а не исключением.

---

## Пустой список словарей

`state.currentDict == null && state.availableDictList.isEmpty()` — валидное состояние виджета.

Поведение: виджет **не рендерится** хост-экраном — на пустом списке словарей хост (например, экран списка словарей) скрывает AppBar и показывает собственное пустое состояние / редирект. На остальных табах (vocabulary / quiz / stats) при пустом списке хост-таб отвечает за пустое состояние; AppBar в составе таба корректно отрабатывает `null` без креша.

Если AppBar всё же рендерится с `currentDict == null` (например, гонка во время удаления):

- Иконка флага не показывается (или скрыта `FlagPlaceholderWidget` с пустой буквой).
- Dropdown-меню содержит только пункт «Создать словарь».
- `ChangeDict` недоступен, потому что список словарей пуст.

---

## Dropdown-меню

- Тап на иконку → раскрытие меню
- Элементы: список словарей (флаг + название, выделение текущего). При пустом списке — пункты отсутствуют.
- Разделитель
- "Создать словарь" → эмитит `Msg.OpenDictionaryCreate` → `DictionaryAppBarNavigationEffect.OpenDictionaryCreate` → `Navigator.openDictionaryCreate()`. Доступно всегда, включая пустой список.
- Выбор словаря → переключение текущего → меню закрывается

---

## Messages

| Message | Действие |
|---------|----------|
| `AvailableDict(list)` | Обновить список словарей, скрыть loading |
| `CurrentDict(current: DictUiEntity?)` | Обновить текущий словарь; `null` — состояние «нет словарей» |
| `ChangeDict(dict)` | Закрыть меню, отправить эффект смены словаря |
| `DictMenuOn` | Открыть dropdown |
| `DictMenuOff` | Закрыть dropdown |
| `OpenDictionaryCreate` | Эмитить навигационный эффект на экран создания |
| `Empty` | No-op |

---

## Effects

| Effect | Действие |
|--------|----------|
| `DatasourceEffect.ChangeDict(dict)` | Записать новый ID в SharedPreferences |
| `DictionaryAppBarNavigationEffect.OpenDictionaryCreate` | Дёрнуть `DictionaryAppBarNavigator.openDictionaryCreate()` |
| `NavigationEffect.Back` | Базовый — для shared widget no-op |

---

## Navigator

Widget — shared между 3 табами с одинаковой навигацией. Один интерфейс на все табы:

```kotlin
interface DictionaryAppBarNavigator : Navigator {
    fun openDictionaryCreate()
}
```

`back()` для shared widget — no-op (управление back-кнопкой делает хост-экран). Реализация `DictionaryAppBarNavigatorImpl` в `app/.../navigator/` — получает `onOpenDictionaryCreate: () -> Unit` callback от RootRouter и дёргает его в `openDictionaryCreate()`. В `CompositionRootImpl` навигатор создаётся для каждого таба отдельно через `remember(openDictionaryCreate)`.

---

## Зависимости

- `DictionaryAppBarUseCase` — интерфейс доступа к данным
- `DictionaryAppBarNavigator` — навигация (через `@Assisted` в ViewModel)
- `LexemeLogger` — логирование
- `CountryProvider` — резолв `numericCode → flagRes`
- `PrefsProvider` — хранение ID текущего словаря
- `CoreDbApi.DictionaryApi` — доступ к словарям (Room)
