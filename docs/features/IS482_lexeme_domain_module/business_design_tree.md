# Business design tree: IS482 — Lexeme domain unification

> DAG файлов для реализации **business-слоя**: создание domain-типов в `modules/domain/lexeme`, общего mapper'а в `app/`, миграция трёх feature-модулей (wordcard, quiz/chat, dictionaryTab) и трёх `UseCaseImpl` на унифицированный контракт.
>
> Пометки: `[+]` создание, `[~]` изменение, `[-]` удаление.
>
> Контекст: infra-слой уже отработал (см. `business_walkthrough.md` § 7) — модуль `modules/domain/lexeme` существует с пустым `Lexeme.kt` placeholder'ом, `settings.gradle.kts` подключает модуль, dependency wiring в `build.gradle.kts` потребителей — задача infra sub-flow IS482. Текущий граф **не включает infra-узлы** (gradle-файлы), только business.

---

## Design-choice: расположение value-классов

**Решение:** все четыре domain-типа в одном файле `Lexeme.kt` (рядом с `data class Lexeme`).

**Почему:**

1. **Совместное использование.** `LexemeId`, `Translation`, `Definition` существуют только как поля `Lexeme` — нет ни одного use-case'а где value-класс используется без `Lexeme`. Разнос по файлам создаёт ложное впечатление независимости.
2. **Соответствие текущему стилю проекта.** В `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/entity/Lexeme.kt` сейчас все четыре типа в одном файле (`business_walkthrough.md` § 1.1). Аналогично в `quiz/chat.entity.Lexeme.kt` (§ 1.2). Унификация — повод сохранить эту структуру в общем модуле, а не ломать её.
3. **Малый размер.** Четыре `@JvmInline value class` + один `data class` — суммарно ~25 строк. Разнос на 4 файла дробит читаемость без выигрыша.
4. **`naming.md` § «Файлы»** — не запрещает несколько типов в одном файле когда они тесно связаны.

**Альтернатива (отклонена):** отдельные файлы `LexemeId.kt`, `Translation.kt`, `Definition.kt`, `Lexeme.kt`. Минусы — четыре файла вместо одного, value-классы оторваны от data class.

> 📎 guide: docs/guides/naming.md — "Имена короче лучше длиннее при равной ясности" (применяется и к файловой структуре — меньше файлов лучше при равной ясности).

## Design-choice: расположение `Lexeme.toUiItem(): LexemeUiItem` extension

**Решение:** extension живёт в `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/entity/LexemeUiItem.kt` (рядом с `LexemeUiItem` data class).

**Почему:**

1. **UI-маппинг — забота UI-слоя.** `LexemeUiItem` — это UI-модель dictionaryTab (см. scope § «Аспекты», аспект `ui_layer_dictionarytab`). Mapping `domain → UI` — это граница domain ↔ UI, и по `naming.md` § «Сущности по слоям» extension должен жить там же где UI-тип.
2. **Симметрия с общим mapper'ом.** Общий API → domain mapper (`LexemeApiEntity.toDomain()`) живёт рядом с UseCase-модулем (`app/`). Локальный domain → UI mapper (`Lexeme.toUiItem()`) живёт рядом с UI-моделью (`dictionaryTab/entity/`). Это два разных слоёвых перехода — у каждого своё место.
3. **Альтернатива (a) — extension внутри `DictionaryTabUseCaseImpl`** — отклонена: смешение UI-маппинга с UseCase-реализацией ухудшает читаемость, тянет UI-знания в `app/`.
4. **Альтернатива (b) — отдельный файл `LexemeUiMapper.kt`** — отклонена: один extension не оправдывает отдельный файл; в будущем сюда же лягут `TermUiItem`-маппер и пр., тогда — можно вынести.

> 📎 guide: docs/guides/data-layer.md — "Три слоя маппинга: DB → API → Domain. Каждый через extension-функцию."
> 📎 guide: docs/guides/naming.md — "UI data class → префикс `Ui` + тип" — маппинг в UI-слое живёт рядом с UI-моделями.

---

## Часть 1: Граф (YAML, полные пути)

```yaml
nodes:
  # ============================================================
  # ЛИСТЬЯ — domain-типы (без зависимостей от другого кода IS482)
  # ============================================================

  - id: 1
    path: modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt
    mark: "[~]"
    desc: >
      Placeholder-файл (`business_walkthrough.md` § 7) переписывается финальным содержимым.
      Содержит четыре типа: `Lexeme` (data class), `LexemeId` (value class), `Translation`, `Definition`.
      Package — `me.apomazkin.lexeme` (без `.entity`, см. scope § Аспекты `package_path_drift_rule`).
    depends_on: []

  # ============================================================
  # СЛОЙ 2 — общий API → domain mapper
  # ============================================================

  - id: 2
    path: app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt
    mark: "[+]"
    desc: >
      Общая top-level extension `LexemeApiEntity.toDomain(): Lexeme`.
      Single-expression body. Не пробрасывает `wordClass` / `options`.
    depends_on: [1]

  # ============================================================
  # СЛОЙ 3 — domain → UI mapper (только для dictionaryTab)
  # ============================================================

  - id: 3
    path: modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/entity/LexemeUiItem.kt
    mark: "[~]"
    desc: >
      Добавляется extension `Lexeme.toUiItem(): LexemeUiItem`.
      Сам data class `LexemeUiItem` + value-классы `TranslationUiEntity` / `DefinitionUiEntity` остаются без изменений.
    depends_on: [1]

  # ============================================================
  # СЛОЙ 4 — миграция wordcard на общий domain
  # ============================================================

  - id: 4
    path: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/entity/Lexeme.kt
    mark: "[-]"
    desc: >
      Удаление старого domain-файла wordcard (4 типа: `Lexeme`, `LexemeId`, `Translation`, `Definition`).
      Все консьюмеры переходят на `me.apomazkin.lexeme.*`.
    depends_on: [1]

  - id: 5
    path: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/entity/Term.kt
    mark: "[~]"
    desc: >
      Замена import `wordcard.entity.Lexeme` → `me.apomazkin.lexeme.Lexeme`.
      Поле `lexemeList: List<Lexeme>` — generic-параметр меняет источник, сигнатура та же.
    depends_on: [1, 4]

  - id: 6
    path: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/deps/WordCardUseCase.kt
    mark: "[~]"
    desc: >
      Замена import на общий domain. 7 точек изменения типа (см. `business_contract_spec.md` § WordCardUseCase):
      `getTermById`, `deleteLexeme`, `addLexemeTranslation`, `addLexemeDefinition`, `restoreLexeme`,
      `RemoveTranslationResult.TranslationRemoved`, `RemoveDefinitionResult.DefinitionRemoved`.
      `Term` остаётся wordcard-локальным (импортируется из `wordcard.entity.Term`, см. узел 5).
    depends_on: [1, 4]

  - id: 7
    path: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/State.kt
    mark: "[~]"
    desc: >
      Замена import `wordcard.entity.Lexeme` → `me.apomazkin.lexeme.Lexeme`.
      Маппер `Lexeme.toLexemeState()` (`business_walkthrough.md` § 5.2, строки 245-261) сохраняет логику —
      обращения `lexemeId.id`, `translation?.value`, `definition?.value` работают как раньше (имена полей идентичны).
    depends_on: [1, 4]

  - id: 8
    path: modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/Message.kt
    mark: "[~]"
    desc: >
      Замена import `wordcard.entity.Lexeme` → `me.apomazkin.lexeme.Lexeme`.
      `Msg.RefreshLexemeList(val lexemes: List<Lexeme>)` (`business_walkthrough.md` § 5.3) — сигнатура без изменений.
    depends_on: [1, 4]

  - id: 9
    path: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/LexemeManagementTest.kt
    mark: "[~]"
    desc: >
      Замена 4 импортов: `Lexeme`, `LexemeId`, `Translation`, `Definition` → `me.apomazkin.lexeme.*`.
      Логика тестов без изменений.
    depends_on: [1, 4]

  - id: 10
    path: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/WordLoadedTest.kt
    mark: "[~]"
    desc: >
      Замена 4 импортов (`Lexeme`, `LexemeId`, `Translation`, `Definition`) → `me.apomazkin.lexeme.*`.
      `Term`, `Word`, `WordId` — остаются из `wordcard.entity.*`.
    depends_on: [1, 4]

  - id: 11
    path: modules/screen/wordcard/src/test/java/me/apomazkin/wordcard/mate/DatasourceEffectHandlerTest.kt
    mark: "[~]"
    desc: >
      Замена 4 импортов (`Lexeme`, `LexemeId`, `Translation`, `Definition`) → `me.apomazkin.lexeme.*`.
    depends_on: [1, 4]

  # ============================================================
  # СЛОЙ 5 — миграция quiz/chat на общий domain
  # ============================================================

  - id: 12
    path: modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/entity/Lexeme.kt
    mark: "[-]"
    desc: >
      Удаление старого domain-файла quiz/chat (3 типа: `Lexeme`, `Translation`, `Definition`).
      Не было `LexemeId` — теперь добавляется через общий domain (правка консьюмера в `QuizGameImpl.kt`).
    depends_on: [1]

  - id: 13
    path: modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/entity/WriteQuiz.kt
    mark: "[~]"
    desc: >
      Замена import `quiz.chat.entity.Lexeme` → `me.apomazkin.lexeme.Lexeme`.
      Поле `val lexeme: Lexeme` (`business_walkthrough.md` § 5.4) — сигнатура та же, источник типа другой.
    depends_on: [1, 12]

  - id: 14
    path: modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/deps/QuizChatUseCase.kt
    mark: "[~]"
    desc: >
      Замена import (если использовался) на общий domain. `Lexeme` напрямую в сигнатурах нет —
      только транзитивно через `WriteQuiz` (узел 13). Может потребоваться лишь убрать неиспользуемый import.
    depends_on: [1, 12, 13]

  - id: 15
    path: modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/quiz/QuizGameImpl.kt
    mark: "[~]"
    desc: >
      Замена import на общий domain + правка `lexeme.id` → `lexeme.lexemeId.id` на строке :511
      (`business_walkthrough.md` § 5.5). Остальные обращения (`lexeme.translation`, `lexeme.definition`,
      `.value`) работают как раньше — имена полей и value-классов идентичны.
    depends_on: [1, 12, 13]

  # ============================================================
  # СЛОЙ 6 — dictionaryTab миграция (UI-контракт UseCase не меняется)
  # ============================================================

  # (Публичный контракт DictionaryTabUseCase не меняется — UI-узлы (LexemeUiItem, TermUiItem,
  # composables, mate.State) не входят в этот DAG. Меняется только UseCaseImpl и маппер UI — узел 3.)

  # ============================================================
  # СЛОЙ 7 — миграция UseCaseImpl в app/ на общий mapper
  # ============================================================

  - id: 16
    path: app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt
    mark: "[~]"
    desc: >
      Удалить локальный `LexemeApiEntity.toDomainEntity()` (строки 216-225, `business_walkthrough.md` § 2.1).
      Заменить вызовы `.toDomainEntity()` для `LexemeApiEntity` → `.toDomain()` на 7 точках
      (`:49, :68, :78, :101, :111, :153, :212`). Импорт общего `LexemeMapper`.
      Импорт `Lexeme` / `LexemeId` / `Translation` / `Definition` — из `me.apomazkin.lexeme.*`.
      Уточнение: `TermApiEntity.toDomainEntity()` (определён на `:202`) **остаётся как есть** —
      Term не унифицируется в IS482. Не путать с `LexemeApiEntity.toDomainEntity()` (на `:216`).
    depends_on: [1, 2, 4, 6]

  - id: 17
    path: app/src/main/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImpl.kt
    mark: "[~]"
    desc: >
      Удалить локальный `LexemeApiEntity.toDomainEntity()` (строки 117-123, `business_walkthrough.md` § 2.2).
      Импортировать общий mapper. Правка вызова `.toDomainEntity()` → `.toDomain()` в строке :138
      (`WriteQuizComplexEntity.toDomainEntity`). Конструктор `Lexeme(...)` теперь принимает `LexemeId(...)`,
      а не сырой `id` — но это уже инкапсулировано в общий mapper.
    depends_on: [1, 2, 12, 13, 14]

  - id: 18
    path: app/src/main/java/me/apomazkin/polytrainer/di/module/dictionarytab/DictionaryTabUseCaseImpl.kt
    mark: "[~]"
    desc: >
      Заменить inline-маппинг `LexemeApiEntity → LexemeUiItem` (строки 108-121 в `getWordList`, 140-153
      в `searchTerms`, `business_walkthrough.md` § 2.3) на двойной маппинг:
      `LexemeApiEntity.toDomain() → Lexeme.toUiItem()`. Импорт общего mapper'а и
      `me.apomazkin.dictionarytab.entity.toUiItem`.
    depends_on: [1, 2, 3]

  # ============================================================
  # СЛОЙ 8 — app/ tests (sanity)
  # ============================================================

  - id: 19
    path: app/src/test/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImplTest.kt
    mark: "[~]"
    desc: >
      Проверка компиляции после удаления локального `toDomainEntity` в `QuizChatUseCaseImpl` (узел 17).
      Тест использует `LexemeApiEntity` напрямую (`business_walkthrough.md` § 6.4) — domain `Lexeme` не импортирует.
      Правка не требуется, но узел в графе для подтверждения проверки.
    depends_on: [17]
```

### Топологический порядок исполнения

```
1 → (2, 3, 4, 12)
4 → (5, 6, 7, 8, 9, 10, 11)
12 → (13, 14, 15)
13 → (14, 15)
(2, 4, 6) → 16
(2, 12, 13, 14) → 17
(2, 3) → 18
17 → 19
```

DAG проверен — циклов нет.

---

## Часть 2: Детали по каждому узлу

### Узел 1 — `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt` `[~]`

**Назначение.** Единый источник domain-типов лексемы для всего приложения.

**Сейчас (placeholder).**

```kotlin
package me.apomazkin.lexeme
// Placeholder — will be populated by business sub-flow (IS482).
```

**Стало (псевдокод).**

```kotlin
package me.apomazkin.lexeme

import java.util.Date

@JvmInline
value class LexemeId(val id: Long)

@JvmInline
value class Translation(val value: String)

@JvmInline
value class Definition(val value: String)

data class Lexeme(
    val lexemeId: LexemeId,
    val wordId: Long,
    val translation: Translation?,
    val definition: Definition?,
    val addDate: Date,
    val changeDate: Date? = null,
)
```

Поля без `wordClass` / `options` / `category` (исключены, обоснование — `business_contract.md` § Domain shape).

---

### Узел 2 — `app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt` `[+]`

**Назначение.** Общая top-level extension для маппинга API → domain. Заменяет три копии (`toDomainEntity` в wordcard/quiz-chat UseCaseImpl + inline в dictionaryTab).

**Сейчас.** Не существует.

**Стало (псевдокод).**

```kotlin
package me.apomazkin.polytrainer.mapper

import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.lexeme.Definition
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.lexeme.LexemeId
import me.apomazkin.lexeme.Translation

fun LexemeApiEntity.toDomain(): Lexeme = Lexeme(
    lexemeId = LexemeId(id),
    wordId = wordId,
    translation = translation?.let { Translation(it.value) },
    definition = definition?.let { Definition(it.value) },
    addDate = addDate,
    changeDate = changeDate,
)
```

Single-expression body. `wordClass` / `options` не пробрасываются.

---

### Узел 3 — `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/entity/LexemeUiItem.kt` `[~]`

**Назначение.** Добавить extension `Lexeme.toUiItem(): LexemeUiItem` для домен → UI маппинга в dictionaryTab.

**Сейчас.** Только data class + два value-класса (`business_walkthrough.md` § 1.3).

**Стало (псевдокод, дополнение).**

```kotlin
// Существующее содержимое не меняется. Добавляется extension:

import me.apomazkin.lexeme.Lexeme

fun Lexeme.toUiItem(): LexemeUiItem = LexemeUiItem(
    id = lexemeId.id,
    wordId = wordId,
    translation = translation?.let { TranslationUiEntity(it.value) },
    definition = definition?.let { DefinitionUiEntity(it.value) },
    addDate = addDate,
    changeDate = changeDate,
)
```

Маппинг тривиален — выкрутки value-классов из domain в UI-обёртки.

---

### Узел 4 — `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/entity/Lexeme.kt` `[-]`

**Назначение.** Удалить старый domain-файл wordcard. Все 4 типа (`Lexeme`, `LexemeId`, `Translation`, `Definition`) переехали в общий модуль (узел 1).

**Сейчас.** Файл существует, содержит 4 типа (`business_walkthrough.md` § 1.1).

**Стало.** Файла нет. Все консьюмеры импортируют `me.apomazkin.lexeme.*`.

---

### Узел 5 — `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/entity/Term.kt` `[~]`

**Назначение.** Замена import. `Term` сам остаётся wordcard-локальным (IS482 unification касается только `Lexeme` & co).

**Сейчас.**

```kotlin
package me.apomazkin.wordcard.entity

data class Term(... val lexemeList: List<Lexeme>)  // Lexeme из wordcard.entity
```

**Стало.**

```kotlin
package me.apomazkin.wordcard.entity

import me.apomazkin.lexeme.Lexeme

data class Term(... val lexemeList: List<Lexeme>)
```

---

### Узел 6 — `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/deps/WordCardUseCase.kt` `[~]`

**Назначение.** Замена import + изменение 7 сигнатур (источник типа `Lexeme` смещается).

**Сейчас (выборка).**

```kotlin
import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.Term

interface WordCardUseCase {
    suspend fun getTermById(id: Long): Term?
    suspend fun deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>?
    suspend fun addLexemeTranslation(...): Lexeme?
    suspend fun addLexemeDefinition(...): Lexeme?
    suspend fun restoreLexeme(...): List<Lexeme>?

    sealed interface RemoveTranslationResult {
        data class TranslationRemoved(val lexeme: Lexeme) : RemoveTranslationResult
        ...
    }
    sealed interface RemoveDefinitionResult {
        data class DefinitionRemoved(val lexeme: Lexeme) : RemoveDefinitionResult
        ...
    }
}
```

**Стало (выборка).**

```kotlin
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.wordcard.entity.Term

interface WordCardUseCase {
    // Сигнатуры идентичны, источник Lexeme — me.apomazkin.lexeme.Lexeme.
    suspend fun getTermById(id: Long): Term?
    suspend fun deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>?
    suspend fun addLexemeTranslation(...): Lexeme?
    suspend fun addLexemeDefinition(...): Lexeme?
    suspend fun restoreLexeme(...): List<Lexeme>?

    sealed interface RemoveTranslationResult {
        data class TranslationRemoved(val lexeme: Lexeme) : RemoveTranslationResult
        ...
    }
    sealed interface RemoveDefinitionResult {
        data class DefinitionRemoved(val lexeme: Lexeme) : RemoveDefinitionResult
        ...
    }
}
```

---

### Узел 7 — `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/State.kt` `[~]`

**Назначение.** Замена import. `Lexeme.toLexemeState()` extension работает без изменений (имена полей `lexemeId.id`, `translation.value`, `definition.value` — те же).

**Сейчас (выборка).**

```kotlin
import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.LexemeId
import me.apomazkin.wordcard.entity.Translation
import me.apomazkin.wordcard.entity.Definition

// LexemeState (UI-state) — без изменений.
// fun Lexeme.toLexemeState(): LexemeState — обращается к this.lexemeId.id / this.translation?.value / this.definition?.value.
```

**Стало (выборка).**

```kotlin
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.lexeme.LexemeId
import me.apomazkin.lexeme.Translation
import me.apomazkin.lexeme.Definition

// LexemeState — без изменений.
// fun Lexeme.toLexemeState(): LexemeState — без изменений (обращения работают как раньше).
```

---

### Узел 8 — `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/Message.kt` `[~]`

**Назначение.** Замена import. `Msg.RefreshLexemeList(val lexemes: List<Lexeme>)` без изменений.

**Сейчас.**

```kotlin
import me.apomazkin.wordcard.entity.Lexeme

sealed interface Msg {
    data class RefreshLexemeList(val lexemes: List<Lexeme>) : Msg
    ...
}
```

**Стало.**

```kotlin
import me.apomazkin.lexeme.Lexeme

sealed interface Msg {
    data class RefreshLexemeList(val lexemes: List<Lexeme>) : Msg
    ...
}
```

---

### Узлы 9, 10, 11 — wordcard test-файлы `[~]`

**Назначение.** Замена 4 импортов в трёх тест-файлах.

**Сейчас (общая часть в каждом).**

```kotlin
import me.apomazkin.wordcard.entity.Definition
import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.LexemeId
import me.apomazkin.wordcard.entity.Translation
```

**Стало.**

```kotlin
import me.apomazkin.lexeme.Definition
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.lexeme.LexemeId
import me.apomazkin.lexeme.Translation
```

Логика тестов не меняется — value-классы конструируются и сравниваются идентично.

---

### Узел 12 — `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/entity/Lexeme.kt` `[-]`

**Назначение.** Удалить старый domain-файл quiz/chat (3 типа: `Lexeme`, `Translation`, `Definition`).

**Сейчас.** Файл существует (`business_walkthrough.md` § 1.2). Поле `id: Long` (сырой), без `LexemeId`.

**Стало.** Файла нет. Тип `Lexeme` импортируется из общего модуля, теперь с `LexemeId(Long)`.

---

### Узел 13 — `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/entity/WriteQuiz.kt` `[~]`

**Назначение.** Замена import — `lexeme: Lexeme` теперь общий domain (с `LexemeId`).

**Сейчас.**

```kotlin
import me.apomazkin.quiz.chat.entity.Lexeme

data class WriteQuiz(... val lexeme: Lexeme, ...)
```

**Стало.**

```kotlin
import me.apomazkin.lexeme.Lexeme

data class WriteQuiz(... val lexeme: Lexeme, ...)
```

---

### Узел 14 — `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/deps/QuizChatUseCase.kt` `[~]`

**Назначение.** Подчистить неиспользуемые импорты (если есть). `Lexeme` в публичных сигнатурах не появляется (`business_walkthrough.md` § 3.2) — изменение транзитивное через `WriteQuiz`.

**Сейчас.** Возможен импорт `quiz.chat.entity.Lexeme` — если есть, убирается; если использовался только в `WriteQuiz` (что в другом файле), то правка тривиальна.

**Стало.** Импорт `me.apomazkin.lexeme.Lexeme` появляется только если нужен; иначе — нет правок в типах.

---

### Узел 15 — `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/quiz/QuizGameImpl.kt` `[~]`

**Назначение.** Замена import + правка `lexeme.id` → `lexeme.lexemeId.id` (миграция с сырого `Long` на `LexemeId(Long)`).

**Сейчас (выборка).**

```kotlin
import me.apomazkin.quiz.chat.entity.Lexeme

// :511
QuizItem.QuizInfo(lexemeId = lexeme.id, ...)
// Остальные обращения работают как раньше.
```

**Стало (выборка).**

```kotlin
import me.apomazkin.lexeme.Lexeme

// :511
QuizItem.QuizInfo(lexemeId = lexeme.lexemeId.id, ...)
```

Остальные обращения (`lexeme.translation`, `.value`, `lexeme.definition`, `.value` на строках :442, :463, :468, :492, :500-503) — без изменений, имена идентичны.

---

### Узел 16 — `app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt` `[~]`

**Назначение.** Удалить локальный `toDomainEntity` и переключиться на общий mapper.

**Сейчас (выборка).**

```kotlin
import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.LexemeId
import me.apomazkin.wordcard.entity.Translation
import me.apomazkin.wordcard.entity.Definition

// :216-225
fun LexemeApiEntity.toDomainEntity(): Lexeme {
    return Lexeme(
        lexemeId = LexemeId(id),
        translation = translation?.let { Translation(it.value) },
        definition = definition?.let { Definition(it.value) },
        category = null,
        addDate = addDate,
        changeDate = changeDate,
    )
}

// :202-214 — TermApiEntity.toDomainEntity() → вызывает it.toDomainEntity() для lexemes (:212)
// Вызовы .toDomainEntity() для LexemeApiEntity на строках :49, :68, :78, :101, :111, :153, :212
// (`:36` — это вызов TermApiEntity.toDomainEntity(), не относится к Lexeme-унификации)
```

**Стало (выборка).**

```kotlin
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.polytrainer.mapper.toDomain

// Локальная toDomainEntity для LexemeApiEntity — удалена.
// TermApiEntity.toDomainEntity (если останется в этом файле) — теперь вызывает .toDomain() для lexemes.
// Все ранее `.toDomainEntity()` для LexemeApiEntity → теперь `.toDomain()` (но `TermApiEntity.toDomainEntity` остаётся — это другой extension, не связан с unification).
```

Замечание: `WordCardUseCaseImpl.toDomainEntity()` есть и для `TermApiEntity → Term` (`business_walkthrough.md` § 2.1, `:202-214`) — этот mapper остаётся локальным в `WordCardUseCaseImpl` (Term не унифицируется в IS482, см. scope). Только `LexemeApiEntity.toDomainEntity` удаляется, заменяется на `LexemeMapper.toDomain`.

---

### Узел 17 — `app/src/main/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImpl.kt` `[~]`

**Назначение.** Удалить локальный `toDomainEntity`, переключиться на общий mapper.

**Сейчас (выборка).**

```kotlin
import me.apomazkin.quiz.chat.entity.Lexeme
import me.apomazkin.quiz.chat.entity.Translation
import me.apomazkin.quiz.chat.entity.Definition

// :117-123
fun LexemeApiEntity.toDomainEntity() = Lexeme(
    id = id,
    translation = translation?.let { Translation(it.value) },
    definition = definition?.let { Definition(it.value) },
    addDate = addDate,
    changeDate = changeDate,
)

// :130-141 — WriteQuizComplexEntity.toDomainEntity() с lexeme = lexemeData.toDomainEntity() на :138
```

**Стало (выборка).**

```kotlin
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.polytrainer.mapper.toDomain

// Локальная toDomainEntity для LexemeApiEntity — удалена.
// WriteQuizComplexEntity.toDomainEntity на :138 → lexeme = lexemeData.toDomain()
```

---

### Узел 18 — `app/src/main/java/me/apomazkin/polytrainer/di/module/dictionarytab/DictionaryTabUseCaseImpl.kt` `[~]`

**Назначение.** Заменить inline-маппинг API → UI на двойной маппинг API → domain → UI.

**Сейчас (выборка, `:108-121` и `:140-153`).**

```kotlin
// Inline-маппинг в getWordList() и searchTerms()
lexemeList = term.lexemes.map { defMate ->
    LexemeUiItem(
        id = defMate.id,
        wordId = defMate.wordId,
        translation = defMate.translation?.let { TranslationUiEntity(it.value) },
        definition = defMate.definition?.let { DefinitionUiEntity(it.value) },
        addDate = defMate.addDate,
        changeDate = defMate.changeDate,
    )
}
```

**Стало (выборка).**

```kotlin
import me.apomazkin.polytrainer.mapper.toDomain
import me.apomazkin.dictionarytab.entity.toUiItem

// В getWordList() и searchTerms()
lexemeList = term.lexemes
    .map { it.toDomain() }    // LexemeApiEntity → Lexeme (общий mapper, узел 2)
    .map { it.toUiItem() }    // Lexeme → LexemeUiItem (локальный mapper, узел 3)
```

Контракт `DictionaryTabUseCase` (возврат `List<TermUiItem>` и `Flow<PagingData<TermUiItem>>`) не меняется.

---

### Узел 19 — `app/src/test/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImplTest.kt` `[~]`

**Назначение.** Подтвердить компиляцию после удаления локального `toDomainEntity` в `QuizChatUseCaseImpl`.

**Сейчас.** Тест использует `LexemeApiEntity` напрямую (`business_walkthrough.md` § 6.4). Не импортирует domain `Lexeme`.

**Стало.** Содержательных правок не требуется. Узел в графе для явной фиксации шага «проверить компиляцию `app/` тестов после удаления локального mapper'а».

Если компиляция падает (например тест мокал `toDomainEntity` extension) — добавить импорт `import me.apomazkin.polytrainer.mapper.toDomain` и заменить вызов. Маловероятно — extension'ы обычно не мокают.

---

## Сводка

- **`[+]` создание:** 1 файл (общий mapper `LexemeMapper.kt`).
- **`[~]` изменение:** 15 файлов (1 domain placeholder → финальный; 1 UI mapper в dictionaryTab; 9 wordcard + 3 quiz/chat файлов миграции; 3 app/UseCaseImpl + 1 app/test).

  Точнее по группам:
  - Domain: 1 (узел 1, переписать placeholder).
  - dictionaryTab UI mapper: 1 (узел 3).
  - wordcard main: 4 (узлы 5, 6, 7, 8).
  - wordcard tests: 3 (узлы 9, 10, 11).
  - quiz/chat: 3 (узлы 13, 14, 15).
  - app/ UseCaseImpl: 3 (узлы 16, 17, 18).
  - app/ tests: 1 (узел 19).

  Итого `[~]`: 16 (1 + 1 + 4 + 3 + 3 + 3 + 1).

- **`[-]` удаление:** 2 файла (старые `wordcard/entity/Lexeme.kt`, `quiz/chat/entity/Lexeme.kt`).

Циклов в DAG нет. Топологический порядок:
1. Domain (узел 1)
2. Параллельно: общий mapper (2), dictionaryTab UI mapper (3), удаление wordcard.Lexeme (4), удаление quiz/chat.Lexeme (12)
3. Параллельно wordcard миграция (5, 6, 7, 8, 9, 10, 11) и quiz/chat миграция (13, 14, 15)
4. app/UseCaseImpl миграция (16, 17, 18) — после соответствующих feature-узлов
5. app/test (19) — после узла 17

---

## log_messages

- DAG из 19 узлов: 1 создание (mapper), 16 изменений (domain placeholder, UI mapper, wordcard×7, quiz-chat×3, app UseCaseImpl×3, app test×1), 2 удаления (старые feature-локальные `Lexeme.kt`).
- Зафиксированы два design-choice: (1) все 4 domain-типа в одном файле `Lexeme.kt` (совместное использование + текущий стиль проекта); (2) `Lexeme.toUiItem()` extension в `dictionaryTab/entity/LexemeUiItem.kt` (UI-маппинг — забота UI-слоя, симметрия с расположением общего API→domain mapper'а).
- Топологический порядок: domain (1) → mapper/UI-mapper/удаления (2,3,4,12) → миграция консьюмеров (5-11, 13-15) → UseCaseImpl (16-18) → tests (19). Циклов нет.

_model: Opus 4.7 (1M context)_
