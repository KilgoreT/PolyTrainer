# Стиль кода

## Общие правила

- Максимальная длина строки: **120 символов**
- Язык комментариев и документации: **английский**
- Язык общения в чате: **русский**
- Одна ответственность на функцию
- Комментарии — только для сложной бизнес-логики

## Именование

### Пакеты

```
me.apomazkin.<module>
me.apomazkin.<module>.logic    — State, Message, Reducer
me.apomazkin.<module>.deps     — UseCase интерфейсы
me.apomazkin.<module>.ui       — Screen, Widget
me.apomazkin.<module>.entity   — Domain модели
```

### Файлы

| Тип | Формат | Пример |
|-----|--------|--------|
| Экран | `*Screen.kt` | `WordCardScreen.kt` |
| ViewModel | `*ViewModel.kt` | `ChatViewModel.kt` |
| Виджет | `*Widget.kt` | `TopBarWidget.kt`, `LexemeItemWidget.kt` |
| Редьюсер | `*Reducer.kt` | `ChatReducer.kt` |
| Стейт | `State.kt` | `State.kt` (одинаково во всех модулях) |
| Сообщения | `Message.kt` | `Message.kt` |
| Эффект-хендлер | `*EffectHandler.kt` | `DatasourceEffectHandler.kt` |
| UseCase | `*UseCase.kt` / `*UseCaseImpl.kt` | `WordCardUseCase.kt` |
| Тесты | `*Test.kt` | `OpenTopBarMenuTest.kt` |
| Тесты расширений | `*ExtTest.kt` в папке `ext/` | `TopBarExtTest.kt` |

### Классы и интерфейсы

- Стейт: `*State` — `WordCardState`, `TopBarState`, `ChatScreenState`
- Сообщения: sealed interface `Msg` (всегда `Msg`)
- Эффекты: `DatasourceEffect`, `UiEffect` (sealed interface extends `Effect`)
- UseCases: интерфейс `*UseCase`, реализация `*UseCaseImpl`

### Логирование

См. отдельный гайд: [logging.md](logging.md)

### Принцип именования

Имена должны быть **лаконичными и понятными**. Короткое имя лучше длинного при равной ясности. Если имя можно сократить без потери смысла — сократи. При ревью всегда проверяй: можно ли переименовать короче?

- `FlagsUpdated` лучше чем `FilteredFlagsLoaded`
- `updateFlags` лучше чем `updateFilteredFlags`
- `flags` лучше чем `filteredFlagsList`

## Форматирование

### Перенос длинных строк

```kotlin
// Хорошо: перенос на логических точках
val userProfile = UserProfile(
    id = userId,
    name = "John Doe",
    email = "john.doe@example.com"
)

val result = someService
    .getUserData(userId)
    .filter { it.isActive }
    .map { it.toUserProfile() }

// Плохо: всё в одну строку
val userProfile = UserProfile(id = userId, name = "John Doe", email = "john.doe@example.com", phoneNumber = "+1234567890")
```

### Extension chain в редьюсере

```kotlin
// Хорошо: каждый вызов на новой строке
is Msg.UserAttempt -> state
    .userTextEnter()
    .clearUserInput()
    .hideUserActions()
    .disableUserInput() to setOf(
        DatasourceEffect.CheckAnswer(message.value)
    )

// Плохо: всё в одну строку
is Msg.UserAttempt -> state.userTextEnter().clearUserInput().hideUserActions().disableUserInput() to setOf(DatasourceEffect.CheckAnswer(message.value))
```

## Импорты

Группировка:
1. Android / AndroidX
2. Kotlin / Kotlinx
3. Проектные модули

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

import kotlinx.coroutines.flow.*

import me.apomazkin.mate.*
import me.apomazkin.theme.*
import me.apomazkin.ui.*
```

## Git конвенции

### Ветки

```
IS<номер>-<описание>
```

Примеры: `IS436-word-card-refactor`, `IS378-vocabulary-tab-tests`

### Коммиты

```
IS<номер>. <описание на английском>.
```

Примеры:
- `IS436. WordCard refactor.`
- `IS378. Added vocabulary tab tests.`
- `IS431. Added extension unit tests for dictionary tab feature.`

## Gradle конвенции

- Java target: **17**
- compileSdk / targetSdk: **35**
- minSdk: **23**
- Version catalogs в `deps/*.versions.toml`
- Namespace = lowercase package: `me.apomazkin.dictionarytab`
- Плагины: `com.android.library` + `org.jetbrains.kotlin.android` + `org.jetbrains.kotlin.plugin.compose`
