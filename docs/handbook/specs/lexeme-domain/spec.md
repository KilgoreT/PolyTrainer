# Lexeme Domain

> **Пометка (IS486, 2026-07-20).** Доменная модель существенно расширена
> фичей IS486: `ComponentType` получил `core` / `enabled` / `dependsOn`
> (иерархия зависимостей), добавлены `ComponentOption`, `DependencyTarget`,
> шаблон `CHOICE` (`ChoiceValues`), чистые модули иерархии
> (`HierarchyChecks`, `CascadePlan`) и outcome-типы конструктора.
> Актуальный источник контрактов доменной модели —
> **[docs/features/IS486_choice_component/spec.md](../../../features/IS486_choice_component/spec.md)** (§5).
> Ниже — исходная спека выноса модуля (IS482), исторически валидная.

## Бизнес-описание

Модуль `modules/domain/lexeme` — единый источник доменной сущности `Lexeme` для всего приложения. Сейчас `Lexeme` дублируется в трёх местах: domain-классы в `wordcard` и `quiz/chat`, а в `dictionaryTab` domain-слоя нет вовсе — там сразу UI-модель `LexemeUiItem`. Маппинг `LexemeApiEntity → Lexeme` повторяется в трёх `UseCaseImpl` в `app/`.

Фича вводит новую категорию Gradle-модулей `modules/domain/` (наряду с `core/`, `screen/`, `widget/`, `library/`, `datasource/`) и в ней первый житель — `lexeme` (pure-Kotlin, без Android-зависимостей). Цель — устранить тройное дублирование domain-типа и подготовить почву для IS481 (компонентный конструктор лексемы), где иначе пришлось бы плодить `@Deprecated` computed extensions в трёх местах вместо одного. Изменений UI/UX и БД-схемы нет — это чистый рефакторинг архитектуры.

## User Stories

Фича рефакторинговая, stories developer-facing:

- Как разработчик, я хочу единое определение `Lexeme` в `modules/domain/lexeme`, чтобы изменения доменной модели применялись в одном месте, а не размножались по трём feature-модулям.
- Как разработчик, я хочу один общий mapper `LexemeApiEntity → Lexeme` в `app/`, чтобы устранить тройное дублирование маппинга в `WordCardUseCaseImpl` / `QuizChatUseCaseImpl` / `DictionaryTabUseCaseImpl`.
- Как разработчик `dictionaryTab`, я хочу, чтобы маппинг `domain.Lexeme → LexemeUiItem` жил внутри слоёв dictionaryTab, чтобы UI-слой не зависел от data-API напрямую (`LexemeApiEntity → LexemeUiItem` inline исчезает).
- Как разработчик `quiz/chat` и `dictionaryTab`, я хочу типобезопасный `LexemeId(Long)` value-class вместо сырого `Long`, чтобы случайно не перепутать id лексемы с id слова или пакета.
- Как разработчик, готовящий IS481, я хочу заранее иметь единый domain-модуль, чтобы добавление `components: List<ComponentValue>` и compatibility-shim `Lexeme.translation` / `Lexeme.definition` делалось в одном месте.

## State

**N/A для IS482** — фича рефакторинговая. Mate-state (`LexemeState`, `WordCardState`, `TextValueState` в wordcard; `State` в quiz/chat; `State` с `termListMap: Map<String, Flow<PagingData<TermUiItem>>>` в dictionaryTab) — без изменений. Поля и подписи остаются. Меняется только источник импорта типа `Lexeme` (с feature-локального на общий `me.apomazkin.lexeme.Lexeme`).

## UI Messages

**N/A для IS482** — `Msg.*` иерархии wordcard / quiz.chat / dictionaryTab без изменений. `wordcard.mate.Message.Msg.RefreshLexemeList(val lexemes: List<Lexeme>)` сохраняет сигнатуру; меняется только пакет импорта `Lexeme`.

## IO

**N/A для IS482** — `DatasourceEffect` / `UiEffect` без изменений. EffectHandler'ы трёх feature-модулей продолжают звать UseCase с теми же эффектами; поведение IO не меняется. Контрактное изменение касается только типа возврата UseCase (см. § UseCase).

## Domain shape

Финальные классы в `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt`. Пакет — `me.apomazkin.lexeme` (без `.entity`, drift зафиксирован в scope: модуль = домен, доменные типы лежат в корне пакета).

### `LexemeId`

```kotlin
@JvmInline
value class LexemeId(val id: Long)
```

- Что: типобезопасная обёртка над `Long` id лексемы.
- Почему: zero-overhead value-class; берём решение wordcard, на которое мигрируют quiz/chat и dictionaryTab (сейчас у них сырой `Long`).

### `Translation`

```kotlin
@JvmInline
value class Translation(val value: String)
```

- Что: domain-обёртка над строкой перевода.
- Почему: имя без суффикса по `naming.md` § «Сущности по слоям» (domain-слой). Совпадает по форме с локальными `wordcard.entity.Translation` и `quiz.chat.entity.Translation`. UI-обёртка `TranslationUiEntity` в dictionaryTab — отдельный UI-слой, не трогаем.

### `Definition`

```kotlin
@JvmInline
value class Definition(val value: String)
```

- Что: domain-обёртка над строкой определения.
- Почему: симметрично `Translation`. Имя без суффикса.

### `Lexeme`

```kotlin
package me.apomazkin.lexeme

import java.util.Date

data class Lexeme(
    val lexemeId: LexemeId,
    val wordId: Long,
    val translation: Translation?,
    val definition: Definition?,
    val addDate: Date,
    val changeDate: Date? = null,
)
```

#### Состав полей

| Поле | Что | Почему |
|---|---|---|
| `lexemeId: LexemeId` | Идентификатор лексемы. | Типобезопасность; берём решение wordcard. quiz/chat и dictionaryTab переходят с сырого `Long` (правка `lexeme.id` → `lexeme.lexemeId.id`). |
| `wordId: Long` | Id слова-родителя. Non-null. | `LexemeApiEntity.wordId: Long` всегда присутствует. `DictionaryTabUseCaseImpl` использует `wordId` при маппинге в `LexemeUiItem` — без поля пришлось бы вводить `?: error()` в маппере. Wordcard / quiz/chat поле не читают, но участвие в `equals/hashCode` безвредно. |
| `translation: Translation?` | Перевод. Nullable. | Все три текущих формы и API имеют translation nullable. |
| `definition: Definition?` | Определение. Nullable. | Симметрично translation. |
| `addDate: Date` | Дата добавления. Non-null. | Соответствует `LexemeApiEntity.addDate: Date` (без `?`). |
| `changeDate: Date? = null` | Дата изменения. Nullable, default `null`. | Везде nullable. Единственный default-параметр — упрощает конструктор для случая «только что созданная лексема». |

#### Исключённые поля

- `category: String?` (есть в `wordcard.entity.Lexeme`) — **не включено**: в маппере `WordCardUseCaseImpl:221` всегда `null`, ни один потребитель не читает. Мёртвый код, YAGNI. Если потребуется в IS481 — добавим тогда.
- `wordClass: String?` (есть в `LexemeApiEntity`) — **не включено**: маппер игнорирует. Бриф запрещает расширение scope.
- `options: Long` (есть в `LexemeApiEntity`) — **не включено**: аналогично, не используется маппером, out of scope.

#### Порядок полей

`lexemeId` → `wordId` → `translation` → `definition` → `addDate` → `changeDate`. Identity → семантические value → даты. Соответствует порядку `LexemeApiEntity` (за вычетом `wordClass`/`options`).

## Mapper

**Файл:** `app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt`

**Пакет:** `me.apomazkin.polytrainer.mapper`

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

- Расположение: top-level extension в пакете `mapper/` (рядом с будущими аналогами `TermApiEntity → Term` и пр.), **не** в `di/module/common/` — маппер не DI-bean.
- Имя метода `toDomain()` (не `toDomainEntity()`) — domain-классы без суффикса `Entity` по `naming.md`.
- Поля `wordClass`/`options` из API не пробрасываются (исключены из domain).
- Single-expression body.
- Три `UseCaseImpl` (`WordCardUseCaseImpl`, `QuizChatUseCaseImpl`, `DictionaryTabUseCaseImpl`) импортируют этот mapper. Локальные `toDomainEntity()` и inline-маппинги удаляются.

## UseCase

Меняется только источник типа `Lexeme` в сигнатурах (с `<feature>.Lexeme` на `me.apomazkin.lexeme.Lexeme`). Состав методов и семантика сохраняются.

### `WordCardUseCase`

**Файл:** `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/deps/WordCardUseCase.kt`

| Метод / тип | Старый источник `Lexeme` | Новый источник `Lexeme` |
|---|---|---|
| `getTermById(id: Long): Term?` (через `Term.lexemeList: List<Lexeme>`) | `wordcard.entity.Lexeme` | `me.apomazkin.lexeme.Lexeme` |
| `deleteLexeme(wordId: Long, lexemeId: Long): List<Lexeme>?` | `wordcard.entity.Lexeme` | `me.apomazkin.lexeme.Lexeme` |
| `addLexemeTranslation(...): Lexeme?` | `wordcard.entity.Lexeme` | `me.apomazkin.lexeme.Lexeme` |
| `addLexemeDefinition(...): Lexeme?` | `wordcard.entity.Lexeme` | `me.apomazkin.lexeme.Lexeme` |
| `restoreLexeme(...): List<Lexeme>?` | `wordcard.entity.Lexeme` | `me.apomazkin.lexeme.Lexeme` |
| `RemoveTranslationResult.TranslationRemoved(val lexeme: Lexeme)` | `wordcard.entity.Lexeme` | `me.apomazkin.lexeme.Lexeme` |
| `RemoveDefinitionResult.DefinitionRemoved(val lexeme: Lexeme)` | `wordcard.entity.Lexeme` | `me.apomazkin.lexeme.Lexeme` |

`Term` остаётся в feature-модуле wordcard (не входит в IS482-scope). Параметры с типом `String` (тексты translation/definition) не меняются.

### `QuizChatUseCase`

**Файл:** `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/deps/QuizChatUseCase.kt`

`Lexeme` напрямую в сигнатурах не встречается — `getRandomWriteQuizList(limit: Int, maxGrade: Int, dictionaryId: Long): List<WriteQuiz>`. Изменение транзитивное через `WriteQuiz`:

| Файл | Поле | Старый тип | Новый тип |
|---|---|---|---|
| `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/entity/WriteQuiz.kt` | `val lexeme: Lexeme` | `quiz.chat.entity.Lexeme` | `me.apomazkin.lexeme.Lexeme` |

`WriteQuiz` остаётся в feature-модуле quiz/chat.

### `DictionaryTabUseCase`

**Файл:** `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/deps/DictionaryTabUseCase.kt`

**Публичный контракт без изменений.** UseCase продолжает возвращать UI-модели (`TermUiItem` / `LexemeUiItem`).

| Метод | Тип возврата | Изменение |
|---|---|---|
| `getWordList(): List<TermUiItem>` | `List<TermUiItem>` (содержит `LexemeUiItem`) | без изменений в публичном контракте |
| `searchTerms(...): Flow<PagingData<TermUiItem>>` | `Flow<PagingData<TermUiItem>>` | без изменений в публичном контракте |

`Lexeme` в публичной сигнатуре `DictionaryTabUseCase` не появляется.

**Внутри `DictionaryTabUseCaseImpl`** (это не часть публичного контракта, но релевантно бизнес-слою): inline-маппинг `LexemeApiEntity → LexemeUiItem` заменяется двойным маппингом `Api → domain (общий mapper в app/) → UI (локальный mapper в dictionaryTab)`. Точное расположение второго маппера (extension рядом с `LexemeUiItem` в dictionaryTab или внутри `DictionaryTabUseCaseImpl`) — design-решение, рекомендация: `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/entity/LexemeUiItem.kt` (UI-маппинг — забота UI-слоя).

Обоснование сохранения UI-контракта: смена возврата на domain потребовала бы переписки всех консьюмеров в mate dictionaryTab (`State.termListMap`, reducer'ы, EffectHandler'ы) — это out of scope IS482 (бриф запрещает изменения UI/UX и расширение scope). dictionaryTab tech-debt отсутствия domain-слоя — отдельная задача.

_model: Opus 4.7 (1M context)_
