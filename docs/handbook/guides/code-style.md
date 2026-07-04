# Стиль кода

## Общие правила

- Максимальная длина строки: **120 символов**
- Язык комментариев и документации: **английский**
- Язык общения в чате: **русский**
- Одна ответственность на функцию
- Комментарии — только для сложной бизнес-логики

## Nullability и `!!`

Force-unwrap `!!` в production-коде **запрещён**. Безмолвный NPE без сообщения = плохая диагностика; в перспективе computed property / cross-module smart-cast = риск регрессии (значение между двумя `!!` может оказаться разным).

**Допустимые исключения** (комментарий с обоснованием обязателен):
- Cross-module smart-cast: компилятор не выводит non-null для public property из другого модуля, хотя null-check уже сделан.
- Гарантия через билдер / контракт (`require`/`check` выкинул раньше).

В обоих случаях — **используй локальную переменную, `?.let`, `?:` или `requireNotNull`**, не `!!`. Тесты — исключение (setup-данные контролируешь).

### Антипаттерн (на реальном кейсе из QuizGameImpl)

```kotlin
when {
    lexeme.translation != null -> {
        append(lexeme.translation!!.value)
        someAction(lexeme.translation!!.value.length)
    }
}
```

### Корректно

```kotlin
// Локальная переменная — smart-cast возвращается
when {
    lexeme.translation != null -> {
        val translation = lexeme.translation
        append(translation.value)
        someAction(translation.value.length)
    }
}

// Идиоматично через ?.let
lexeme.translation?.let { translation ->
    append(translation.value)
    someAction(translation.value.length)
}

// Контракт через requireNotNull — кидает IllegalArgumentException с сообщением
val translation = requireNotNull(lexeme.translation) {
    "lexeme.translation must be present in QuizPlay state"
}
append(translation.value)
```

---

## Rules — машинно-проверяемые правила

### R-CS-001. Запрет `!!` в production-коде

- **Severity:** critical.
- **Applies to:** все production `.kt` файлы (не `src/test/`).
- **Check:** `Grep "!!"` (или `\!\!`) по `src/main/` — не должно быть. Исключения допустимы только с комментарием рядом, объясняющим **почему именно `!!`** (cross-module smart-cast, contract-guarantee, и т.п.). Тесты `src/test/` — разрешены.
- **Альтернативы:** локальная переменная + `?.let`/`?:`, либо `requireNotNull(x) { "explicit message" }`.
- **Зачем:** silent NPE без сообщения = плохая диагностика; cross-module/computed property поля могут вернуть разные значения между двумя `!!` обращениями — атомарность теряется. См. § «Nullability и `!!`».

### R-CS-002. Cross-layer compile-shim обязан иметь `// TODO(shim, <next-subflow>)` маркер

- **Severity:** critical.
- **Applies to:** любая «заглушка» в коде на границе между sub-flow слоями (типично business sub-flow дописывает minimal impl в data слой чтобы код скомпилировался, реальную реализацию доделает data sub-flow). Признаки: пустые `List`/`emptySet()` вместо реального SQL, hardcoded `false`/`null` вместо реальной проверки, `TODO()` без описания, conservative approximation вместо честного computation.
- **Check:** `Grep "TODO\\(shim" --include="*.kt"` после business sub-flow — должен совпадать с реально сделанными shim'ами. На finalize data sub-flow (или любого downstream который должен доделать) — `Grep "TODO\\(shim" --include="*.kt"` должен вернуть **пусто**. Если остаются `TODO(shim, …)` — downstream sub-flow не доделал, релиз блокируется.
- **Зачем:** без явного маркера shim превращается в timed bomb. Тесты mock'аются и проходят (mock не знает что реализация заглушка), assemble компилируется, manual smoke может не задеть edge case. В IS481 phase 2 conservative cardinality approximation в `LexemeApiImpl.editComponentType` была без TODO маркера — выжила только потому что data sub-flow пришёл и переписал. При пропуске downstream — silent invariant violation в production.
- **Формат:** `// TODO(shim, data): real per-lexeme SELECT in data_implement` либо аналогичная формулировка с указанием **какого sub-flow** ждём.
- **Альтернатива:** не делать shim вовсе — если business sub-flow не может скомпилировать без реального impl, явно зафиксировать что upstream design tree должен дождаться downstream результата (явная зависимость в DAG).

---

## Минимализм API и комментарии (YAGNI)

Правила направлены против раздувания кода "на будущее" и "для контекста". Применяются ко всем виджетам, классам, функциям.

### Параметры функций / composable'ов

- **Не добавлять параметр "на будущее".** Понадобится — добавим тогда. До этого момента — мусор.
- `@Suppress("UNUSED_PARAMETER")` — **запрещён**. Если параметр не используется внутри функции, удалить.
- Dead callback (`onSomething: () -> Unit` который никем не вызывается) — удалить. Включая dead defaults (`@DrawableRes iconConfirm: Int? = ...` если никем не передаётся не-null значение).
- Default value параметра, который нигде не переопределяется → удалить параметр целиком, оставить hardcoded значение или удалить совсем.

### KDoc виджетов / функций

- **Одна фраза** о назначении + `@param` на каждый параметр.
- KDoc виджета **НЕ описывает контекст использования** (где живёт, в каком layout, как взаимодействует с другими виджетами). Виджет не должен "знать" где он используется.
- KDoc **НЕ ссылается на project decisions, design rationale, спеку фичи**. Это живёт в `docs/handbook/specs/`, `docs/features/<feature>/`, не в коде.
- KDoc **НЕ описывает состояния за пределами параметров** ("в FlowRow используется так, в заголовке — иначе"). Поведение задаётся параметрами — описание параметров достаточно.

**Антипаттерн:**
```kotlin
/**
 * Chip-метка субсущности (Translation/Definition).
 *
 * Используется в FlowRow (как активатор) и в заголовке LexemeMeaningField
 * (как метка активной субсущности с возможностью удалить). Различается только
 * иконка и callback. Project decisions: pill в обоих режимах ...
 *
 * @param ...
 */
```

**Корректно:**
```kotlin
/**
 * Округлый чип с подписью и иконкой справа.
 *
 * @param labelRes подпись.
 * @param iconRes иконка справа.
 * @param enabled блокирует клик.
 * @param onClick тап по чипу.
 */
```

### Имена параметров

- **Точные, без избыточных префиксов.** `trailing`/`leading`/`start`/`end` — только когда есть выбор между позициями. У виджета с одной иконкой → `iconRes`, не `trailingIconRes`.
- **Без слов-помощников** (`Helper`, `Manager`, `Util`, `Internal`) если они не несут смысла за пределами организационного.
- Имена отражают **роль/намерение**, не реализацию (`canAddTranslation` лучше чем `hasTranslationPlaceholder` — описывает domain-возможность, не UI-эффект).

## Именование

См. отдельный гайд: **[naming.md](naming.md)**. Там — пакеты, файлы, классы, БД (таблицы / колонки / FK), enum-значения, resources, тесты + раздел `## Rules` с machine-checkable правилами.

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
