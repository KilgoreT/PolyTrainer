# Business walkthrough: IS482

> Discovery business-слоя для унификации `Lexeme` в `modules/domain/lexeme`.
> Только факты, ссылки `file:line`. Дизайн-решения — на следующем шаге.

---

## 1. Три формы `Lexeme` (текущие сигнатуры)

### 1.1 `wordcard`

Файл: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/entity/Lexeme.kt:1-21`

```kotlin
package me.apomazkin.wordcard.entity

import java.util.Date

@JvmInline
value class LexemeId(val id: Long)                       // :5-6

@JvmInline
value class Translation(val value: String)               // :8-9

@JvmInline
value class Definition(val value: String)                // :11-12

data class Lexeme(                                       // :14-21
    val lexemeId: LexemeId,
    val translation: Translation?,
    val definition: Definition?,
    val category: String?,
    val addDate: Date,
    val changeDate: Date? = null,
)
```

- `LexemeId` — есть, `@JvmInline value class`, оборачивает `Long`.
- `category: String?` — есть, nullable, **в маппере всегда `null`** (см. § 2.1).
- `addDate`/`changeDate` — `java.util.Date`, `changeDate` nullable.

### 1.2 `quiz/chat`

Файл: `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/entity/Lexeme.kt:1-17`

```kotlin
package me.apomazkin.quiz.chat.entity

import java.util.Date

@JvmInline
value class Translation(val value: String)               // :5-6

@JvmInline
value class Definition(val value: String)                // :8-9

data class Lexeme(                                       // :11-17
    val id: Long,
    val translation: Translation? = null,
    val definition: Definition? = null,
    val addDate: Date,
    val changeDate: Date? = null,
)
```

- `id: Long` — **сырой `Long`**, нет `LexemeId` value-class.
- `category` — **нет** поля.
- Имена value-классов `Translation` / `Definition` совпадают с wordcard, но это **другие классы** (разные пакеты).
- Поля `translation` / `definition` имеют дефолт `null` (в wordcard — без дефолта).

### 1.3 `dictionaryTab` (UI-слой)

Файл: `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/entity/LexemeUiItem.kt:1-20`

```kotlin
package me.apomazkin.dictionarytab.entity

import java.util.Date

@JvmInline
value class TranslationUiEntity(val value: String)       // :6-7

@JvmInline
value class DefinitionUiEntity(val value: String)        // :9-10

data class LexemeUiItem(                                 // :12-20
    val id: Long,
    val wordId: Long,
    val translation: TranslationUiEntity?,
    val definition: DefinitionUiEntity?,
    val addDate: Date,
    val changeDate: Date? = null,
//    val category: LexemeLabel,
)
```

- `id: Long` — сырой `Long`.
- `wordId: Long` — **есть**, чего нет ни в wordcard, ни в quiz/chat.
- Value-классы: `TranslationUiEntity` / `DefinitionUiEntity` (UI-обёртки, отличаются от `wordcard`/`quiz/chat`).
- Закомментирован `category: LexemeLabel` — потенциальная связь с `LexemeLabel` enum (см. § 1.4).
- Это **UI-модель** — composable работает с ней напрямую. По scope (§ Замечание) — **не удаляется**.

### 1.4 `LexemeLabel` enum (не путать с domain `Lexeme`)

Файл: `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/entity/Lexeme.kt:1-58`

- Имя файла случайно совпадает с фичей IS482.
- Содержит `enum class LexemeLabel` (`:10-26`) — лексическая категория слова: `NOUN`, `VERB`, `ADJ`, `ADV`, `PRON`, `NUMERAL`, `PREP`, `CONJ`, `PHRASE`, `OTHER`, `UNDEFINED` — со `stringValue` и `@StringRes valueRes`/`shortValueRes`.
- Реализует `ChipValue` из `me.apomazkin.chippicker`.
- Хелперы: `String.toLexemeLabel()` (`:28-40`), `LexemeLabel.toChipPicker()` (`:42-45`), `val lexicalCategory: List<LexemeLabel>` (`:47-58`).
- Помечен `@Deprecated("Old as ass hole")` (`:9`) с TODO о переименовании в `LexicalCategoryLabel`.
- **Не относится к domain `Lexeme`** — это UI-категоризация для chip-picker.
- По scope (§ Не трогаем) — out of scope IS482.

---

## 2. Маппинг `LexemeApiEntity → <feature>.Lexeme` (три копии в `app/`)

### 2.1 `WordCardUseCaseImpl.toDomainEntity()`

Файл: `app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt:216-225`

```kotlin
fun LexemeApiEntity.toDomainEntity(): Lexeme {
    return Lexeme(
        lexemeId = LexemeId(id),
        translation = translation?.let { Translation(it.value) },
        definition = definition?.let { Definition(it.value) },
        category = null,                                  // :221
        addDate = addDate,
        changeDate = changeDate,
    )
}
```

- Top-level extension, **`category` всегда `null`** (`:221`) — поле есть, но не используется.
- Использования внутри файла: `:36, :49, :68, :78, :101, :111, :153, :212` (через `TermApiEntity.toDomainEntity()` `:202-214`, который мапит `lexemes` через `it.toDomainEntity()`).

### 2.2 `QuizChatUseCaseImpl.toDomainEntity()`

Файл: `app/src/main/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImpl.kt:117-123`

```kotlin
fun LexemeApiEntity.toDomainEntity() = Lexeme(
    id = id,
    translation = translation?.let { Translation(it.value) },
    definition = definition?.let { Definition(it.value) },
    addDate = addDate,
    changeDate = changeDate,
)
```

- Top-level extension. Single-expression вместо `return`-блока (стилистика отличается от wordcard).
- `LexemeApiEntity.wordId` и `wordClass`/`options` игнорируются — поля не пробрасываются.
- Использование внутри файла: вызывается через `WriteQuizComplexEntity.toDomainEntity()` (`:130-141`), строка `:138` (`lexeme = lexemeData.toDomainEntity()`).

### 2.3 `DictionaryTabUseCaseImpl` — inline-маппинг (НЕ extension)

Файл: `app/src/main/java/me/apomazkin/polytrainer/di/module/dictionarytab/DictionaryTabUseCaseImpl.kt`

**Первое место — `getWordList()` `:108-121`:**

```kotlin
lexemeList = term.lexemes.map { defMate ->
    LexemeUiItem(
        id = defMate.id,
        wordId = defMate.wordId,
        translation = defMate.translation?.let {
            TranslationUiEntity(
                it.value
            )
        },
        definition = defMate.definition?.let { DefinitionUiEntity(it.value) },
        addDate = defMate.addDate,
        changeDate = defMate.changeDate,
    )
}
```

**Второе место — `searchTerms(...)` `:140-153`:** идентичный inline-блок (дубль).

- **НЕТ** `toDomainEntity` extension — маппинг сделан inline в двух местах, оба раза `LexemeApiEntity → LexemeUiItem` напрямую (минуя domain).
- `wordId` пробрасывается из `LexemeApiEntity.wordId`.
- `wordClass`/`options`/`category` игнорируются.

---

## 3. UseCase interfaces (`deps/`)

### 3.1 `WordCardUseCase`

Файл: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/deps/WordCardUseCase.kt:1-36`

Возвращаемые типы упоминают `Lexeme`:
- `deleteLexeme(...): List<Lexeme>?` (`:10`)
- `addLexemeTranslation(...): Lexeme?` (`:11`)
- `addLexemeDefinition(...): Lexeme?` (`:13`)
- `restoreLexeme(...): List<Lexeme>?` (`:21-25`)
- `RemoveTranslationResult.TranslationRemoved(val lexeme: Lexeme)` (`:29`)
- `RemoveDefinitionResult.DefinitionRemoved(val lexeme: Lexeme)` (`:34`)
- `getTermById(...): Term?` (`:7`) — `Term` содержит `lexemeList: List<Lexeme>` (см. § 5).

`Translation` / `Definition` в сигнатурах **не упоминаются** — UseCase принимает `String` (`translation: String`, `definition: String`), domain value-классы оборачивают только внутри.

### 3.2 `QuizChatUseCase`

Файл: `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/deps/QuizChatUseCase.kt:1-14`

- `Lexeme` в сигнатурах **не упоминается напрямую** — возвращает `List<WriteQuiz>` (`:10-13`).
- `WriteQuiz` содержит поле `val lexeme: Lexeme` (см. § 5).
- Связь с domain через transitivity.

### 3.3 `DictionaryTabUseCase`

Файл: `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/deps/DictionaryTabUseCase.kt:1-23`

- Содержит `//TODO kilg 29.06.2025 21:33 завести слой доменных сущностей.` (`:9`) — **прямое указание на отсутствие domain-слоя**.
- Возвращает **UI-модели** `DictUiEntity` / `TermUiItem` (UI-обёртку, не domain).
- `getWordList(): List<TermUiItem>` (`:14`), `searchTerms(...): Flow<PagingData<TermUiItem>>` (`:15-18`).
- **Текущий стиль:** UseCase в dictionaryTab возвращает UI, а не domain — отличается от wordcard / quiz/chat.

---

## 4. `LexemeApiEntity` (контракт data)

Файл: `core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/LexemeApiEntity.kt:1-23`

```kotlin
package me.apomazkin.core_db_api.entity

import java.util.Date

@JvmInline
value class TranslationApiEntity(val value: String)      // :6-7

@JvmInline
value class DefinitionApiEntity(val value: String)       // :9-10

data class LexemeApiEntity(                              // :11-20
    val id: Long,
    val wordId: Long,
    val translation: TranslationApiEntity? = null,
    val definition: DefinitionApiEntity? = null,
    val wordClass: String? = null,
    val options: Long = 0,
    val addDate: Date,
    val changeDate: Date? = null,
)

fun LexemeApiEntity.canRemoveTranslation() = definition != null   // :22
fun LexemeApiEntity.canRemoveDefinition() = translation != null   // :23
```

Поля доступные общему mapper'у:
- `id: Long`
- `wordId: Long`  — есть, используется только dictionaryTab inline.
- `translation: TranslationApiEntity?` (value-class над `String`)
- `definition: DefinitionApiEntity?` (value-class над `String`)
- `wordClass: String?` — **не используется** ни одним из трёх мапперов (потенциальный source для `category`).
- `options: Long = 0` — **не используется** мапперами.
- `addDate: Date` / `changeDate: Date?`

Hint-функции `canRemoveTranslation()` / `canRemoveDefinition()` работают на API-уровне — используются в `WordCardUseCaseImpl:76, :109`.

---

## 5. Использования `Lexeme` в коде (контекст для union-shape)

### 5.1 `wordcard.entity.Term`

Файл: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/entity/Term.kt:1-18`

- `data class Term(... val lexemeList: List<Lexeme>)` (`:17`).
- `WordId` / `Word` value-классы (`:6, :9`).
- Создаётся в `WordCardUseCaseImpl.toDomainEntity()` (TermApiEntity → Term) (`app/.../WordCardUseCaseImpl.kt:202-214`).

### 5.2 `wordcard.mate.State` — потребитель domain `Lexeme`

Файл: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/State.kt`

- `import me.apomazkin.wordcard.entity.Lexeme` (`:5`).
- `LexemeState` (UI-state) — содержит `id: Long`, `translation: TextValueState?`, `definition: TextValueState?` (`:52-59`).
- **Маппинг `Lexeme → LexemeState`**: `Lexeme.toLexemeState()` extension (`:245-261`):
  - `this.lexemeId.id` (`:246`) — обращение к `LexemeId.id`.
  - `this.translation?.value` (`:247-249`) — обращение к `Translation.value`.
  - `this.definition?.value` (`:254-256`) — обращение к `Definition.value`.

### 5.3 `wordcard.mate.Message`

Файл: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/Message.kt:4`

- `import me.apomazkin.wordcard.entity.Lexeme`
- `Msg.RefreshLexemeList(val lexemes: List<Lexeme>)` (`:52`).

### 5.4 `quiz/chat.entity.WriteQuiz`

Файл: `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/entity/WriteQuiz.kt:5-16`

- `data class WriteQuiz(... val lexeme: Lexeme, val word: Word, ...)` (`:13`).
- `lexeme: Lexeme` напрямую вложен.

### 5.5 `quiz/chat.quiz.QuizGameImpl` — field access на `Lexeme`

Файл: `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/quiz/QuizGameImpl.kt`

- `lexeme.translation` field access (`:442, :463, :500, :501`).
- `lexeme.translation.value` (`:463, :501`).
- `lexeme.definition` (`:468, :492, :502, :503`).
- `lexeme.definition.value` (`:492, :503`).
- `lexeme.id` (`:511` — `lexemeId = lexeme.id` для `QuizItem.QuizInfo`).
- **Важно:** quiz/chat использует **сырой `lexeme.id`**. После унификации на `LexemeId(Long)` потребуется правка `lexeme.id` → `lexeme.lexemeId.id`.

### 5.6 `dictionaryTab` UI — потребитель `LexemeUiItem`

Файл: `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/entity/TermUiItem.kt:1-15`

- `data class TermUiItem(... val lexemeList: List<LexemeUiItem> = listOf(), ...)` (`:12`).

Файл: `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/logic/State.kt:23`

- `val termListMap: Map<String, Flow<PagingData<TermUiItem>>> = emptyMap()` — UI-state работает с `TermUiItem` (содержит `LexemeUiItem`).
- Composables читают `LexemeUiItem` напрямую (не меняем по scope).

---

## 6. Tests (импорты domain `Lexeme`)

### 6.1 `wordcard` тесты — 3 файла с импортами

Все три импортируют из `me.apomazkin.wordcard.entity.{Lexeme, LexemeId, Translation, Definition}`:

- `modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/LexemeManagementTest.kt:7-10`:
  ```kotlin
  import me.apomazkin.wordcard.entity.Definition
  import me.apomazkin.wordcard.entity.Lexeme
  import me.apomazkin.wordcard.entity.LexemeId
  import me.apomazkin.wordcard.entity.Translation
  ```
- `modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/WordLoadedTest.kt:8-13` — те же 4 импорта + `Term`, `Word`, `WordId`.
- `modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/DatasourceEffectHandlerTest.kt:9-15` — те же 4 импорта + `Term`, `Word`, `WordId`.

### 6.2 `quiz/chat` тесты — нет импортов domain `Lexeme`

Найдено два test-файла:
- `modules/screen/quiz/chat/src/test/java/me/apomazkin/quiz/chat/ExampleUnitTest.kt`
- `modules/screen/quiz/chat/src/test/java/me/apomazkin/quiz/chat/quiz/QuizGameImplEmptyListTest.kt` — мокает `QuizChatUseCase`, **не импортирует `Lexeme`**.

### 6.3 `dictionaryTab` тесты — нет test-файлов

Через `find` обнаружено: `modules/screen/dictionaryTab/src/test/java/` существует, но **`.kt` файлов нет**.

### 6.4 `app/` тесты на UseCaseImpl

- `app/src/test/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImplTest.kt` — упомянут в scope (`02_scope.md:157`), использует `LexemeApiEntity`, не domain `Lexeme`. После удаления локального `toDomainEntity` в `QuizChatUseCaseImpl` — нужно убедиться что компилируется.

---

## 7. Infra placeholder

Файл: `modules/domain/lexeme/build.gradle.kts:1-13`

```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}
```

- pure-Kotlin, без Android, без зависимостей. Готов принять domain-классы.

Файл: `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt:1-3`

```kotlin
package me.apomazkin.lexeme

// Placeholder — will be populated by business sub-flow (IS482).
```

Файл: `settings.gradle.kts:63-64`

```kotlin
//Domain
include(":modules:domain:lexeme")
```

— infra-шаг уже отработал, категория `Domain` присутствует.

---

## 8. Сравнительная матрица полей

| Поле | wordcard.Lexeme | quiz/chat.Lexeme | dictionaryTab.LexemeUiItem | LexemeApiEntity |
|---|---|---|---|---|
| id-тип | `LexemeId(Long)` | `Long` | `Long` | `Long` |
| `wordId: Long` | — | — | **есть** | **есть** |
| `translation` | `Translation?` | `Translation? = null` | `TranslationUiEntity?` | `TranslationApiEntity? = null` |
| `definition` | `Definition?` | `Definition? = null` | `DefinitionUiEntity?` | `DefinitionApiEntity? = null` |
| `category` / `wordClass` | `category: String?` (всегда `null` в маппере) | — | `// val category: LexemeLabel` (закомм.) | `wordClass: String? = null` |
| `options` | — | — | — | `Long = 0` |
| `addDate` | `Date` | `Date` | `Date` | `Date` |
| `changeDate` | `Date? = null` | `Date? = null` | `Date? = null` | `Date? = null` |

---

## 9. Сводка факт-точек для следующего шага

1. **Три data class `Lexeme`** дублируются: `wordcard.entity.Lexeme`, `quiz.chat.entity.Lexeme`, `dictionarytab.entity.LexemeUiItem`. Точные сигнатуры — § 1.
2. **Mapper в трёх местах:** два top-level `LexemeApiEntity.toDomainEntity()` extensions (wordcard / quiz/chat) + inline-маппинг в `DictionaryTabUseCaseImpl` (дублирован в `getWordList` и `searchTerms`). § 2.
3. **Только wordcard** имеет `LexemeId` value-class и поле `category` (всегда `null`). § 1.1.
4. **Только dictionaryTab/Api** имеют `wordId`. § 1.3, § 4.
5. **API имеет неиспользуемые поля:** `wordClass: String?`, `options: Long` — потенциальные кандидаты для будущих union-полей. § 4.
6. **Field access на `Lexeme`** в quiz/chat использует `lexeme.id` (`QuizGameImpl.kt:511`) — при переходе на `LexemeId(Long)` потребуется правка. § 5.5.
7. **wordcard.State** содержит маппер `Lexeme.toLexemeState()` (`State.kt:245-261`) — обращается к `lexemeId.id`, `translation.value`, `definition.value`. Сохранится при unification (имена полей те же). § 5.2.
8. **dictionaryTab UseCase** возвращает UI-модели, не domain — отличается от wordcard/quiz/chat (`TODO` в `DictionaryTabUseCase.kt:9` фиксирует это). § 3.3.
9. **Tests:** ровно три wordcard-test-файла импортируют domain (`Lexeme`/`LexemeId`/`Translation`/`Definition`). quiz/chat и dictionaryTab тестов на domain нет. § 6.
10. **infra готова:** module `modules/domain/lexeme` существует, placeholder `Lexeme.kt` пустой, dependency wiring `settings.gradle.kts:63-64` на месте. § 7.

---

## Вердикт

Аналог: **частично найден**

Domain-форма `Lexeme` существует в трёх экземплярах (`wordcard.entity.Lexeme`, `quiz.chat.entity.Lexeme`, `dictionarytab.entity.LexemeUiItem`) с разными shape: `LexemeId` value-class только у wordcard; `wordId` только у dictionaryTab/Api; `category` только у wordcard (всегда `null`). Mapper `LexemeApiEntity → <feature>.Lexeme` дублируется трижды: два top-level `toDomainEntity` extensions (`WordCardUseCaseImpl.kt:216-225`, `QuizChatUseCaseImpl.kt:117-123`) + inline-маппинг в двух местах `DictionaryTabUseCaseImpl` (`:108-121`, `:140-153`, причём напрямую `Api → UI`, минуя domain). Единого mapper'а нет. Placeholder `Lexeme.kt` в `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/` создан в infra-шаге, dependency wiring (`settings.gradle.kts:63-64`) присутствует — модуль готов принять unified shape.

---

## log_messages

- Прочитаны три формы Lexeme (`wordcard`, `quiz/chat`, `dictionaryTab.UiItem`) и `LexemeApiEntity`, зафиксированы точные сигнатуры с file:line.
- Найдены три места маппинга: `WordCardUseCaseImpl:216-225`, `QuizChatUseCaseImpl:117-123`, inline `DictionaryTabUseCaseImpl:108-121` и `:140-153`.
- Подтверждено: 3 wordcard-test-файла импортируют domain `Lexeme`; quiz/chat и dictionaryTab тестов на domain нет; placeholder `modules/domain/lexeme/Lexeme.kt` пустой.

_model: Opus 4.7 (1M context)_
