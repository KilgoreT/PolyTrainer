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
| Навигация | UI действие | `CloseScreen`, `Exit` |
| Результаты данных | DatasourceEffectHandler | `TermLoaded`, `QuizLoaded`, `LexemeUpdate` |
| UI обратная связь | UiEffectHandler | `UiMsg.Snackbar` |
| Переключатели | UI switch/checkbox | `EarliestOn`, `EarliestOff`, `DebugOn` |
| No-op | Фоллбэк | `Empty` |

## Конвенции именования

### Действия пользователя
Императивный глагол, описывающий намерение:
- `Show*` / `Hide*` — переключение видимости
- `Open*` / `Close*` — вход/выход из режима
- `Add*` / `Delete*` — CRUD
- `Save*` — сохранение изменений
- `*TextChange` — изменение ввода

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

Используется хендлерами, когда тип эффекта не совпадает:
```kotlin
null -> Msg.Empty
```

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
