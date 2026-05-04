# IS445 — Устаревшая иконка словаря в appbar picker

## Проблема

После смены флага словаря в форме редактирования (DictionaryForm) иконка текущего словаря в DictionaryAppBar (picker на вкладках quiz/dictionary tab) не обновляется. Список словарей (DictionaryList) обновляется корректно.

## Корневая причина

`DictionaryAppBarUseCaseImpl.flowCurrentDict()` подписан на `prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)`. Этот Flow эмитит только при смене **ID текущего словаря** (переключение между словарями). Внутри `map` выполняется одноразовый `getDictionaryById(id)` — не реактивный.

Когда пользователь меняет флаг текущего словаря, ID не меняется → prefs Flow не эмитит → appbar не обновляется. Пользователь видит устаревший placeholder (букву на сером фоне) вместо флага.

Для сравнения: `flowAvailableDict()` подписан на `dictionaryApi.flowDictionaryList()` (Room Flow) — он реактивно получает обновления при любых изменениях в таблице словарей. Поэтому список словарей в picker (dropdown) обновляется, а иконка текущего — нет.

## Что остаётся без изменений

- `DictionaryAppBarState` — структура state не меняется
- `DictionaryAppBarReducer` — reducer messages не меняются
- `DictionaryAppBar` composable — UI не меняется
- `DictDropDownWidget` — отображение picker не меняется
- `DictionaryAppBarUseCase` интерфейс — сигнатуры сохраняются
- `DatasourceEffectHandler` — subscribe/runEffect логика не меняется
- `DictUiEntity` — модель не меняется

## Что меняется

### `DictionaryAppBarUseCaseImpl.flowCurrentDict()`

**Было:** подписка на prefs Flow → одноразовый `getDictionaryById(id)` при смене ID.

**Стало:** `flowCurrentDict()` должен реагировать как на смену ID словаря (prefs), так и на изменение данных самого словаря (Room). Конкретный механизм — `combine` или `flatMapLatest` на prefs Flow + Room Flow словаря.

Варианты реализации:

1. **combine + flowDictionaryList**: `combine(prefsFlow, dictionaryApi.flowDictionaryList())` → найти текущий словарь в списке. Простое, но emits при изменении любого словаря.

2. **flatMapLatest + flowDictionaryById**: prefs Flow `flatMapLatest { id -> dictionaryApi.flowDictionaryById(id) }`. Точечно реагирует только на изменения текущего словаря. Требует добавления `flowDictionaryById(id)` в `DictionaryApi` (если его нет).

3. **combine + availableDict**: переиспользовать `flowAvailableDict()` внутри `flowCurrentDict()` — совместить с prefs Flow.

### Возможные изменения в `CoreDbApi.DictionaryApi`

Если выбран вариант 2 — нужен `flowDictionaryById(id: Long): Flow<DictionaryApiEntity?>` в API (Room DAO + реализация). Если его нет — добавить.

## Затронутые модули

| Модуль | Изменение |
|--------|-----------|
| `app` (DictionaryAppBarUseCaseImpl) | Основной фикс: реактивный `flowCurrentDict()` |
| `core-db-api` / `core-db-impl` | Возможно: `flowDictionaryById()` в DAO (если вариант 2) |
| `widget/dictionaryappbar` | Без изменений кода. Автоматически получит обновления через существующий Flow |

## Сценарий воспроизведения (было)

1. Открыть список словарей
2. Видеть словарь без флага — placeholder (буква на сером)
3. Тап на словарь → форма → выбрать флаг → сохранить
4. Вернуться в список словарей → флаг виден (Room Flow обновил)
5. Переключиться на вкладку с appbar picker → иконка устаревшая (placeholder)

## Ожидаемое поведение (стало)

Шаг 5: appbar picker показывает актуальный флаг словаря сразу, без необходимости переключения словарей.

_model: claude-opus-4-6_
