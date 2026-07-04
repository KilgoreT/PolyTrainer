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
3. Если удалённый был текущим:
   - есть оставшиеся словари → `useCase` записывает в prefs id первого оставшегося (`setCurrentDictionary(remaining.id)`);
   - оставшихся нет → `useCase` **очищает** pref `CURRENT_DICTIONARY_ID_LONG` (`prefsProvider.setLong(CURRENT_DICTIONARY_ID_LONG, null)`).
4. Если словарей не осталось → экран переходит в пустое состояние; все активные подписки `flowCurrentDict()` (AppBar, DictionaryTab) эмитят `null`, без исключений.

---

## Инварианты pref'а текущего словаря

`CURRENT_DICTIONARY_ID_LONG` (Long? в DataStore) может находиться в одном из состояний:

- **не записан** (первый запуск, очистка данных, после удаления последнего словаря) — `null`;
- **содержит id существующего словаря** — happy path;
- **содержит id удалённого словаря** (orphaned reference) — допустимое переходное состояние.

Все читатели pref'а обязаны трансформировать «нет словаря» в безопасный fallback и **не бросать исключение**:

- `DictionaryAppBarUseCase.flowCurrentDict(): Flow<DictUiEntity?>` — при orphaned id или отсутствующем pref fallback на первый словарь из списка; при пустом списке эмитит `null`.
- `DictionaryTabUseCase.flowCurrentDict(): Flow<DictUiEntity?>` и `getCurrentDict(): DictUiEntity?` — то же поведение; при отсутствии словаря таб переходит в пустое состояние (`hasNoDictionary = true`).
- `QuizChatUseCase.getCurrentDictionaryId(): Long?` — возвращает `null` при отсутствии словаря; вызывающая сторона (`QuizGameImpl`) обязана корректно обработать `null` (логирование + пустой quiz-list).

**Инвариант:** удаление любого словаря, включая последний, не должно приводить к необработанному исключению ни в одной активной подписке на текущий словарь.

---

## Обновление списка

Реактивное через `flowDictionaryList()` (Room Flow → `DictionaryListFlowHandler.subscribe()` → `Msg.DictionariesLoaded` → reducer обновляет state).

---

## Навигация назад и user-journey пустого списка

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

### User-journey: удаление всех словарей

Conditional `RequestBack` — это не просто UX-удобство, а часть продуктового user-journey:

1. Пользователь удалил все словари → список пуст → AppBar скрыт, экран в пустом состоянии («Нет словарей / Создайте первый!»).
2. Системная back / тап «назад» → `Msg.RequestBack` → reducer → `ListNavigationEffect.ExitApp` → `ListNavigator.exit()` → `activity.finish()`. Приложение закрыто.
3. **Следующий запуск** приложения эквивалентен онбордингу создания словаря: `SplashScreen` видит пустую БД → `openDictionarySetup()` → форма создания первого словаря в режиме `DICTIONARY_SETUP` (без AppBar). См. `dictionary-create.md`.

Удаление последнего словаря не закрывает пользователю доступ в приложение — оно возвращает его в стартовую точку онбординга при следующем запуске. Это намеренная связка двух механизмов: `ExitApp` при пустом списке + `SplashScreen → openDictionarySetup()` при отсутствии словарей.

---

## Системное ограничение: QuizChat

`QuizChatUseCase.getCurrentDictionaryId(): Long?` возвращает `null` при отсутствии словарей. `QuizGameImpl` при `null` логирует warning (`LogTags.CHAT`) и возвращает пустой список вопросов — экран квиз-чата в этом случае показывает пустое состояние без креша.

В норме пользователь физически не попадает в квиз-чат без словарей: после удаления последнего словаря приложение закрывается, а при следующем запуске SplashScreen перенаправляет в `DICTIONARY_SETUP` (см. user-journey выше). Защита в `QuizGameImpl` существует как страховка от orphaned pref / гонок и формальный контракт «удаление всех словарей не крашит ни один модуль».

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

- `DictionaryUseCase` — CRUD словарей + переключение текущего + чистка orphaned pref при удалении последнего словаря
- `ListNavigator` — через `@Assisted` в ViewModel
- `LexemeLogger` — логирование
