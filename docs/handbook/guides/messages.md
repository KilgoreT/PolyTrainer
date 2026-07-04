# Дизайн сообщений

## Структура

Все сообщения фичи определяются как единый sealed interface `Msg`:

```kotlin
sealed interface Msg {
    // Сообщения от UI
    data object ShowDropdownMenu : Msg
    data object HideDropdownMenu : Msg
    data class ChangeWordValue(val value: String) : Msg
    data class DeleteWord(val wordId: Long) : Msg

    // Результаты эффектов
    data class TermLoaded(val term: Term) : Msg
    data class LexemeUpdate(val lexeme: Lexeme) : Msg

    // No-op
    data object Empty : Msg
}
```

## Внутренние сообщения (UiMsg)

Сообщения, генерируемые UI-эффект-хендлерами. Маркируются `internal`:

```kotlin
internal sealed interface UiMsg : Msg {
    data class Snackbar(val text: String, val show: Boolean) : UiMsg
}
```

## Категории сообщений

| Категория | Источник | Примеры |
|-----------|----------|---------|
| Действия пользователя | UI клик/ввод | `ShowMenu`, `UserTextChange`, `UserAttempt` |
| Навигация | UI действие или системная back | `RequestBack`, `OpenWordCard(id)`, `OpenNewDictionary` |
| Результаты данных | DatasourceEffectHandler | `TermLoaded`, `QuizLoaded`, `LexemeUpdate` |
| UI обратная связь | UiEffectHandler | `UiMsg.Snackbar` |
| Переключатели | UI switch/checkbox | `EarliestOn`, `EarliestOff`, `DebugOn` |
| No-op | Фоллбэк | `Empty` |

## Конвенции именования

### Действия пользователя
Императивный глагол, описывающий намерение:
- `Show*` / `Hide*` — переключение видимости UI-элемента (меню, диалог)
- `Open*` — намерение открыть другой экран (`OpenWordCard(id)`, `OpenNewDictionary`). Reducer возвращает соответствующий `NavigationEffect`
- `Close*` — закрыть локальный UI-элемент (диалог, bottom sheet). НЕ для закрытия экрана — используется `RequestBack`
- `RequestBack` — намерение вернуться назад. Reducer решает: обычный `NavigationEffect.Back` или per-screen `ExitApp` (зависит от state)
- `Add*` / `Delete*` — CRUD
- `Save*` — сохранение изменений
- `*TextChange` — изменение ввода

### Навигационные сообщения
Навигация на другой экран и системная back-кнопка идут через Msg, не через прямые callback. UI отправляет Msg → Reducer возвращает `NavigationEffect` → NavigationEffectHandler дёргает `Navigator`.

```kotlin
// Composable
BackHandler { viewModel.accept(Msg.RequestBack) }

onClick = { viewModel.accept(Msg.OpenWordCard(wordId = item.id)) }

// Reducer
is Msg.RequestBack -> if (state.dictionaries.isEmpty()) {
    state to setOf(ListNavigationEffect.ExitApp)
} else {
    state to setOf(NavigationEffect.Back)
}
is Msg.OpenWordCard -> state to setOf(VocabularyNavigationEffect.OpenWordCard(message.wordId))
```

State навигационные Msg **не модифицируют** — только порождают эффект.

### Результаты эффектов
Прошедшее время или существительное:
- `*Loaded` — данные получены
- `*Update` — сущность обновлена
- `*Skipped` — действие завершено
- `Assessment` — результат оценки

### Toggle-пары
Явные on/off, не булев параметр:
```kotlin
// Предпочтительно: явные сообщения
data object EarliestOn : Msg
data object EarliestOff : Msg

// Также допустимо: булев параметр
data class AddLexemeBottomTranslation(val isAdded: Boolean) : Msg
```

## Данные в сообщениях

### data object (без данных)
Для действий без дополнительной информации:
```kotlin
data object ShowDropdownMenu : Msg
data object Start : Msg
data object Skip : Msg
```

### data class (с данными)
Для действий с параметрами от UI или эффектов:
```kotlin
data class ChangeWordValue(val value: String) : Msg
data class DeleteWord(val wordId: Long) : Msg
data class TermLoaded(val term: Term) : Msg
data class UserAttempt(val value: String) : Msg
```

## Empty Message

В каждой фиче есть `Msg.Empty` как no-op:

```kotlin
data object Empty : Msg
```

Используется handlers для "своих" эффектов без полезного результата (например, после успешного сохранения, когда state не меняется). Для чужих эффектов consumer не вызывается вообще — фильтрация в `MateTypedEffectHandler.filter()`.

В редьюсере:
```kotlin
is Msg.Empty -> state to emptySet()
```

## Exhaustiveness

Sealed interface гарантирует, что `when` в редьюсере исчерпывающий — каждое сообщение должно быть обработано. Добавление нового типа вызывает ошибку компиляции если не обработан.

```kotlin
return when (message) {
    is Msg.ShowDropdownMenu -> ...
    is Msg.HideDropdownMenu -> ...
    is Msg.TermLoaded -> ...
    is UiMsg.Snackbar -> ...    // Внутренние сообщения тоже обрабатываются
    is Msg.Empty -> state to emptySet()
}
```
