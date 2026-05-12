# Список словарей — Спецификация

Экран просмотра, удаления словарей и навигации к созданию/редактированию.

---

## Точки входа

- Из настроек (`SettingsTab.onLangManagementClick`) → `RootPoint.DICTIONARY_LIST`
- AppBar показывается, когда в списке есть хотя бы один словарь
- Системная back при пустом списке закрывает приложение (`navigator.exit()`)

`DICTIONARY_SETUP` и `DICTIONARY_CREATE` route — это **другой экран** (`DictionaryFormScreen`), не этот.

---

## Список

- Отображает все словари пользователя
- Каждый элемент: флаг (или placeholder) + название + иконка удаления
- Тап на элемент → `Msg.EditDictionary(id)` → `ListNavigationEffect.OpenEdit(id)` → форма редактирования
- Тап на иконку удаления → диалог подтверждения
- Кнопка "Новый словарь" внизу → `Msg.OpenNewDictionary` → `ListNavigationEffect.OpenCreate` → форма создания

### Пустое состояние

Если словарей нет — "Нет словарей / Создайте первый!" по центру + кнопка "Новый словарь". AppBar не показывается.

---

## Удаление

1. Тап на иконку удаления → `Msg.RequestDelete(id, name)` → диалог `ConfirmDeleteDictionaryWidget`
2. "Удалить" → `Msg.ConfirmDelete` → `DictionaryListEffect.DeleteDictionary(id)` → CASCADE delete (словарь → words → lexemes → write_quiz)
3. Если удалённый был текущим → useCase переключает на первый оставшийся
4. Если словарей не осталось → пустое состояние

---

## Обновление списка

Реактивное через `flowDictionaryList()` (Room Flow → `DictionaryListFlowHandler.subscribe()` → `Msg.DictionariesLoaded` → reducer обновляет state).

---

## Навигация назад

Conditional в reducer:

```kotlin
is Msg.RequestBack -> if (state.dictionaries.isEmpty()) {
    state to setOf(ListNavigationEffect.ExitApp)
} else {
    state to setOf(NavigationEffect.Back)
}
```

- Пустой список → `ListNavigationEffect.ExitApp` → `ListNavigator.exit()` → `ListNavigatorImpl.onExit()` → `activity.finish()`
- Есть словари → `NavigationEffect.Back` → `Navigator.back()` → `navController.popBackStack()`

Composable отправляет один `Msg.RequestBack` через `BackHandler` и через `AppBar.onBackPress`.

---

## Messages

| Message | Действие |
|---------|----------|
| `RequestBack` | Системная back или AppBar — reducer решает Back vs ExitApp |
| `OpenNewDictionary` | Тап "Новый словарь" — переход на форму создания |
| `EditDictionary(id)` | Тап на элемент списка — переход на форму редактирования |
| `RequestDelete(id, name)` | Показать диалог удаления |
| `ConfirmDelete` | Подтвердить удаление |
| `DismissDelete` | Закрыть диалог |
| `DictionariesLoaded(list)` | Обновление списка из FlowHandler |
| `DictionaryDeleted` | Подтверждение удаления из EffectHandler |
| `Empty` | No-op |

---

## Effects

| Effect | Действие |
|--------|----------|
| `DictionaryListEffect.DeleteDictionary(id)` | CASCADE delete в Room |
| `ListNavigationEffect.ExitApp` | `navigator.exit()` → `activity.finish()` |
| `ListNavigationEffect.OpenCreate` | `navigator.openCreate()` → переход на форму создания |
| `ListNavigationEffect.OpenEdit(id)` | `navigator.openEdit(id)` → переход на форму редактирования |
| `NavigationEffect.Back` | `navigator.back()` → popBackStack |

---

## Navigator

```kotlin
interface ListNavigator : Navigator {
    fun exit()
    fun openEdit(id: Long)
    fun openCreate()
}
```

Реализация в `app/.../navigator/ListNavigatorImpl.kt`. Принимает `navController` + `onExit: () -> Unit` callback (из RootRouter, который дёргает `activity.finish()`).

---

## Порядок словарей

По дате добавления (Room default ordering).

---

## UI

### AppBar

Показывается только если `state.dictionaries.isNotEmpty()`. На пустом списке — без AppBar.

### Элемент списка

`Row`: ImageFlagWidget (или placeholder) + название (secondary color) + IconBoxed удаления

### Диалог удаления

`ConfirmDeleteDictionaryWidget`: "Удалить словарь? Все слова, определения и результаты квизов будут удалены."

---

## Зависимости

- `DictionaryUseCase` — CRUD словарей + переключение текущего
- `ListNavigator` — через `@Assisted` в ViewModel
- `LexemeLogger` — логирование
