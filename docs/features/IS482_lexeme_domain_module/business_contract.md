# Business contract: IS482 — Lexeme domain unification

> Spec business-слоя для рефакторинга unification `Lexeme` в `modules/domain/lexeme`.
> Только сигнатуры и решения. Реализация — на следующих шагах.

---

## State

**N/A для IS482** — фича рефакторинговая (унификация domain-типа `Lexeme`), не feature с новым UI/reducer. Mate-слои `wordcard`, `quiz/chat`, `dictionaryTab` не трогаются: `LexemeState` / `WordCardState` / `TextValueState` / dictionaryTab `State` (см. `02_scope.md` § «UI — нет», бриф «никаких изменений UI/UX») остаются без изменений. UI-state продолжает работать через те же поля (имена value-классов `translation.value` / `definition.value` сохраняются после унификации; обращение `lexemeId.id` тоже сохраняется в wordcard, а в quiz/chat появляется как замена `lexeme.id` — это правка консьюмеров, не правка state-структуры).

> 📎 guide: docs/guides/naming.md — "TEA-state (в `State.kt` редьюсера) → без `Ui` префикса (`LexemeState`, `WordCardState`). TEA-state — внутренняя структура редьюсера, не UI-data."
>
> 📎 guide: docs/guides/naming.md — "Колонка со «значением» сущности (текст / число / payload) — `value`."

---

## Msg

**N/A для IS482** — см. State. `Msg` иерархии wordcard / quiz/chat / dictionaryTab не трогаются. `wordcard.mate.Message.Msg.RefreshLexemeList(val lexemes: List<Lexeme>)` (`business_walkthrough.md` § 5.3) сохраняет ту же сигнатуру — меняется только источник импорта типа `Lexeme` (с `me.apomazkin.wordcard.entity.Lexeme` на `me.apomazkin.lexeme.Lexeme`). Это смена пакета, не семантики сообщения.

> 📎 guide: docs/guides/naming.md — "Сообщения: sealed interface `Msg` (всегда `Msg`)"

---

## Effect/IO

**N/A для IS482** — см. State. EffectHandler'ы трёх feature-модулей не трогаются. `DatasourceEffectHandler` wordcard продолжает звать `WordCardUseCase` с теми же сигнатурами эффектов; поведение IO не меняется. Меняется только тип возврата UseCase (см. § UseCase ниже), но это контрактное изменение depend-стороны, не пересмотр Effect-протокола.

> 📎 guide: docs/guides/naming.md — "Эффекты: `DatasourceEffect`, `UiEffect` (sealed interface extends `Effect`)"

---

## Domain shape

Финальные классы в `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt`.

> 📎 guide: docs/guides/naming.md — "Feature domain — `modules/screen/<feature>/entity/`, `modules/domain/<name>/` — Без суффикса: `Word`, `Lexeme`, `Term`, `ComponentType`"
>
> 📎 guide: docs/guides/naming.md — "Domain (feature `modules/screen/.../entity/`, общий `modules/domain/<name>/`) → без суффикса (`Word`, `Lexeme`)."
>
> 📎 guide: docs/guides/state-modeling.md — "Внутрь можно, наружу — нельзя. Feature знает о Domain и Library. Domain знает о Library. Library не знает ни о чём специфичном."

### `LexemeId`

```kotlin
@JvmInline
value class LexemeId(val id: Long)
```

- Берём из wordcard.entity.Lexeme как есть (`business_walkthrough.md` § 1.1).
- Wrapping `Long` через `@JvmInline value class` — нулевой runtime-overhead, типобезопасность.
- quiz/chat и dictionaryTab сейчас используют сырой `Long` — после унификации переходят на `LexemeId` (правка `lexeme.id` → `lexeme.lexemeId.id` в `QuizGameImpl.kt:511`, аспект `lexemeid_for_quiz_dictionarytab` в scope).

> 📎 guide: docs/guides/naming.md — "Имена короче лучше длиннее при равной ясности (R-N-013)"

### `Translation`

```kotlin
@JvmInline
value class Translation(val value: String)
```

- Имя без суффикса по `naming.md` § «Сущности по слоям» — domain-слой не использует `*Entity` / `*UiEntity`.
- Совпадает по форме с wordcard.entity.Translation и quiz.chat.entity.Translation (`business_walkthrough.md` § 1.1, § 1.2).
- UI-обёртка `TranslationUiEntity` в dictionaryTab остаётся (UI-слой, не трогаем).

> 📎 guide: docs/guides/naming.md — "Колонка со «значением» сущности (текст / число / payload) — `value`. Не `payload` / `data` / `content` — единая конвенция `value`."
>
> 📎 guide: docs/guides/naming.md — "UI data class → префикс `Ui` + тип: `*UiEntity` (обёртка одного домен-объекта), `*UiItem` (элемент списка), `*UiList` (коллекция с UI-метаданными)."

### `Definition`

```kotlin
@JvmInline
value class Definition(val value: String)
```

- Симметрично `Translation`. Имя без суффикса.

> 📎 guide: docs/guides/naming.md — "Domain (feature `modules/screen/.../entity/`, общий `modules/domain/<name>/`) → без суффикса (`Word`, `Lexeme`)."

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

> 📎 guide: docs/guides/state-modeling.md — "State — это данные, не объекты. Нет поведения, нет identity, нет наследования. Только immutable факты."
>
> 📎 guide: docs/guides/state-modeling.md — "`data class` (product) для независимых полей"

#### Обоснование по полям

| Поле | Решение | Обоснование |
|---|---|---|
| `lexemeId: LexemeId` | **включено** | Типобезопасность; берём решение wordcard. quiz/chat и dictionaryTab мигрируют с сырого `Long`. |
| `wordId: Long` | **включено** (обязательное, не nullable) | dictionaryTab при маппинге `domain.Lexeme → LexemeUiItem` требует `wordId` (`DictionaryTabUseCaseImpl:108-121, :140-153`). Источник `LexemeApiEntity.wordId: Long` (`core-db-api`, § 4 walkthrough) — всегда присутствует. Если бы было nullable — пришлось бы добавлять `?: error()` в маппере dictionaryTab, что обозначало бы потерю информации без причины. wordcard и quiz/chat не используют `wordId` — для них поле «инертно» (просто присутствует в data class, при `equals/hashCode` участвует, но никто не читает). Это не проблема: domain-shape по принципу union обязан удовлетворить всех потребителей, и dictionaryTab — реальный потребитель. |
| `category: String?` | **НЕ включено** | В wordcard.entity.Lexeme поле есть, но **в маппере всегда `null`** (`WordCardUseCaseImpl:221`). Реально не используется ни одним потребителем. Включать в union — тянуть мёртвый код в общий domain. По принципу YAGNI оставляем за бортом. Если IS481 (компонентный конструктор) или будущая фича потребует — добавим тогда. Связь с `wordClass: String?` из `LexemeApiEntity` — не материальна для IS482 (`wordClass` сейчас тоже игнорируется маппером). |
| `wordClass: String?` | **НЕ включено** | Из walkthrough «кандидат для union», но бриф **запрещает расширение scope** («без изменений поведения»). Сейчас `LexemeApiEntity.wordClass` не используется ни одним маппером — добавление в domain == введение нового семантического поля, что выходит за рамки рефакторинга. Решение out-of-scope, см. backlog (если потребуется — заведём отдельную задачу). |
| `options: Long` | **НЕ включено** | Аналогично `wordClass` — есть в API (`LexemeApiEntity.options: Long = 0`), не используется маппером, бриф запрещает расширение scope. |
| `translation: Translation?` | **включено, nullable** | Все три текущих формы и API имеют translation как nullable. Сохраняем. |
| `definition: Definition?` | **включено, nullable** | Симметрично translation. |
| `addDate: Date` | **включено, non-null** | Есть везде (wordcard, quiz/chat, dictionaryTab, API). non-null соответствует `LexemeApiEntity.addDate: Date` (без `?`). |
| `changeDate: Date?` | **включено, nullable** | Есть везде, везде nullable (`Date? = null`). |

> 📎 guide: docs/guides/state-modeling.md — "Не плодить классы — научиться считать варианты. T? = |T| + 1; product = умножение, sum = сложение."
>
> 📎 guide: docs/guides/state-modeling.md — "Соблюдать Dependency Rule моделей. Feature → Domain → Library, наружу нельзя. Не лепить feature-флаги в domain model."

#### Порядок полей

`lexemeId` → `wordId` → `translation` → `definition` → `addDate` → `changeDate`. Логический порядок: identity-поля → семантические value → даты. Совпадает с порядком в `LexemeApiEntity` за исключением выноса `wordClass`/`options`.

#### Default values

`changeDate: Date? = null` — единственный default. Остальные поля без дефолтов: маппер передаёт все значения явно, дефолты помешают понимать source данных при чтении кода маппера.

---

## Mapper signature

### Расположение

**Файл:** `app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt`

**Пакет:** `me.apomazkin.polytrainer.mapper`

### Обоснование локации

Между двумя вариантами из scope (`02_scope.md:43`):

1. `me.apomazkin.polytrainer.mapper.LexemeMapper.kt` — отдельный пакет `mapper` рядом с другими top-level extensions общего назначения.
2. `me.apomazkin.polytrainer.di.module.common.LexemeMapper.kt` — внутри `di/module/common/`, рядом с DI-модулями.

**Выбран вариант 1**, потому что:

- Mapper `LexemeApiEntity → Lexeme` — это **не DI-bean и не модуль Dagger'а**, это top-level extension. Пакет `di/module/...` зарезервирован за DI-конфигурацией (`@Module`, `@Provides`, `*ProviderImpl`); смешение бизнес-логики маппинга с DI-инфрой ухудшает читаемость.
- Пакет `mapper/` явно сигнализирует «здесь живут мапперы границ слоёв» — соответствует `data-layer.md` § «Маппинг сущностей» («API → Domain в UseCase модуле», в нашей архитектуре `app/` = UseCase-модуль).
- В будущем сюда же лягут другие top-level мапперы (например `TermApiEntity → Term` после аналогичной унификации) — пакет станет накопителем мапперов границы API↔Domain.

> 📎 guide: docs/guides/data-layer.md — "API → Domain (в UseCase модуле)"
>
> 📎 guide: docs/guides/data-layer.md — "Три слоя маппинга: DB → API → Domain. Каждый через extension-функцию."

### Сигнатура

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

### Замечания

- Имя метода — `toDomain()` (не `toDomainEntity()`). По `naming.md` § «Сущности по слоям» domain-классы без суффикса `Entity` — соответственно и маппер именуется `toDomain`, не `toDomainEntity`. Это правка стиля относительно старых `WordCardUseCaseImpl.toDomainEntity()` / `QuizChatUseCaseImpl.toDomainEntity()`.
- `wordClass` и `options` из `LexemeApiEntity` **не пробрасываются** — соответствует domain-shape (см. § Domain shape).
- `category` поле в domain отсутствует — обоснование в § Domain shape.
- Single-expression body (`= Lexeme(...)`) вместо `return`-блока — короче и идиоматичнее для маппера-однострочника.

> 📎 guide: docs/guides/naming.md — "Имена короче лучше длиннее при равной ясности. Перед фиксацией имени — задай себе вопрос «можно ли сократить без потери смысла?»"
>
> 📎 guide: docs/guides/naming.md — "Суффиксы по слою — Db / DbEntity / ApiEntity / без суффикса (R-N-011)"

---

## UseCase

Изменения трёх UseCase-интерфейсов в feature-модулях. Меняется только источник типа `Lexeme` в сигнатурах (с `<feature>.Lexeme` на `me.apomazkin.lexeme.Lexeme`). Состав методов и их семантика сохраняются.

> 📎 guide: docs/guides/data-layer.md — "UseCase интерфейс в feature модуле, реализация в app модуле."
>
> 📎 guide: docs/guides/naming.md — "UseCases: интерфейс `*UseCase`, реализация `*UseCaseImpl`"

### `WordCardUseCase`

**Файл:** `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/deps/WordCardUseCase.kt`

> 📎 guide: docs/guides/naming.md — "UseCase интерфейсы — `me.apomazkin.<module>.deps`"

Методы с изменением типа возврата (`business_walkthrough.md` § 3.1):

| Метод | Старый тип | Новый тип |
|---|---|---|
| `getTermById(id: Long): Term?` | `Term?` (с `lexemeList: List<wordcard.entity.Lexeme>`) | `Term?` (с `lexemeList: List<me.apomazkin.lexeme.Lexeme>`) |
| `deleteLexeme(lexemeId: Long): List<Lexeme>?` | `List<wordcard.entity.Lexeme>?` | `List<me.apomazkin.lexeme.Lexeme>?` |
| `addLexemeTranslation(...): Lexeme?` | `wordcard.entity.Lexeme?` | `me.apomazkin.lexeme.Lexeme?` |
| `addLexemeDefinition(...): Lexeme?` | `wordcard.entity.Lexeme?` | `me.apomazkin.lexeme.Lexeme?` |
| `restoreLexeme(...): List<Lexeme>?` | `List<wordcard.entity.Lexeme>?` | `List<me.apomazkin.lexeme.Lexeme>?` |
| `RemoveTranslationResult.TranslationRemoved(val lexeme: Lexeme)` | `wordcard.entity.Lexeme` | `me.apomazkin.lexeme.Lexeme` |
| `RemoveDefinitionResult.DefinitionRemoved(val lexeme: Lexeme)` | `wordcard.entity.Lexeme` | `me.apomazkin.lexeme.Lexeme` |

`Term` (`wordcard.entity.Term`) сам остаётся в feature-модуле wordcard (он не входит в IS482-scope унификации, только `Lexeme` / `LexemeId` / `Translation` / `Definition`). У `Term.lexemeList` меняется generic-параметр на новый `me.apomazkin.lexeme.Lexeme`.

Параметры с типом `String` (translation/definition тексты) не меняются.

> 📎 guide: docs/guides/data-layer.md — "Нормализация текстового ввода (trim). Любая строка, уходящая в БД или в сеть, нормализуется через `.trim()` в UseCaseImpl."

### `QuizChatUseCase`

**Файл:** `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/deps/QuizChatUseCase.kt`

`Lexeme` напрямую в сигнатурах не упоминается (`business_walkthrough.md` § 3.2) — возвращает `List<WriteQuiz>`. Изменение распространяется транзитивно через `WriteQuiz`:

| Файл | Поле | Старый тип | Новый тип |
|---|---|---|---|
| `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/entity/WriteQuiz.kt` | `val lexeme: Lexeme` | `quiz.chat.entity.Lexeme` | `me.apomazkin.lexeme.Lexeme` |

Сигнатура `getWriteQuiz(...): List<WriteQuiz>` остаётся, но тип `Lexeme` внутри `WriteQuiz` теперь общий domain.

`WriteQuiz` сам остаётся в feature-модуле quiz/chat — он не входит в IS482-scope.

> 📎 guide: docs/guides/state-modeling.md — "Domain модели переиспользуются между фичами. Изменение в одной фиче не должно ломать domain."

### `DictionaryTabUseCase`

**Файл:** `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/deps/DictionaryTabUseCase.kt`

**Decision: UseCase продолжает возвращать UI-модели** (`TermUiItem` / `LexemeUiItem`), **не** domain.

#### Контракт после IS482

| Метод | Тип возврата | Изменение |
|---|---|---|
| `getWordList(): List<TermUiItem>` | `List<TermUiItem>` (содержит `LexemeUiItem`) | **без изменений** в публичном контракте |
| `searchTerms(...): Flow<PagingData<TermUiItem>>` | `Flow<PagingData<TermUiItem>>` | **без изменений** в публичном контракте |

`Lexeme` в публичной сигнатуре `DictionaryTabUseCase` **не появляется**. Изменения локализованы в `DictionaryTabUseCaseImpl` (см. ниже).

#### Обоснование

Два рассмотренных варианта:

(a) **UseCase возвращает domain `Lexeme` / `Term`**, mate dictionaryTab сам маппит в `LexemeUiItem` / `TermUiItem` в reducer'е / EffectHandler'е.

(b) **UseCase возвращает UI `LexemeUiItem` / `TermUiItem`**, маппинг `domain → UI` живёт внутри `DictionaryTabUseCaseImpl`.

**Выбран (b)**, потому что:

1. **Соответствие текущему стилю модуля.** `DictionaryTabUseCase` сейчас возвращает `TermUiItem` (UI-модель) — изменение этого контракта потребует переписки всех консьюмеров в mate-слое dictionaryTab (`State.termListMap: Map<String, Flow<PagingData<TermUiItem>>>`, reducer'ы, EffectHandler'ы). Это **out of scope IS482** по бризу («без изменений UI/UX») и по scope (§ «UI — нет»).
2. **`data-layer.md` § «UseCase»** не предписывает что именно — domain или UI — должен возвращать UseCase. Это design-decision per feature. dictionaryTab исторически выбрал UI (см. `TODO kilg 29.06.2025` в `DictionaryTabUseCase.kt:9` — фиксирует осознание отсутствия domain-слоя). IS482 не решает этот tech-debt, только устраняет дублирование domain-типа `Lexeme`.
3. **Симметрия проста.** Внутри `DictionaryTabUseCaseImpl` поток данных становится: `LexemeApiEntity → (общий mapper) → domain.Lexeme → (локальный mapper в app/dictionarytab) → LexemeUiItem`. Двойной маппинг — цена соответствия `data-layer.md` (общий API→Domain mapper) при сохранении UI-контракта UseCase.

> 📎 guide: docs/guides/data-layer.md — "Три слоя маппинга: DB → API → Domain. Каждый через extension-функцию."
>
> 📎 guide: docs/guides/naming.md — "UI data class → префикс `Ui` + тип: `*UiEntity` (обёртка одного домен-объекта), `*UiItem` (элемент списка)"

#### Изменения внутри `DictionaryTabUseCaseImpl`

(Это не часть публичного контракта UseCase, но релевантно для business-слоя.)

**Файл:** `app/src/main/java/me/apomazkin/polytrainer/di/module/dictionarytab/DictionaryTabUseCaseImpl.kt`

- Inline-маппинг `LexemeApiEntity → LexemeUiItem` (строки `:108-121`, `:140-153`) заменяется двойным маппингом:
  ```kotlin
  // pseudo-signature, не реализация
  lexemeList = term.lexemes
      .map { it.toDomain() }       // общий mapper из app/.../mapper/LexemeMapper.kt
      .map { it.toUiItem() }       // локальный mapper domain.Lexeme → LexemeUiItem (расположение — design шаг)
  ```
- Второй маппер (`Lexeme.toUiItem(): LexemeUiItem`) — конкретное расположение (extension в `dictionaryTab/entity/` или внутри `DictionaryTabUseCaseImpl`) **business sub-flow не фиксирует жёстко** — выбор делает design / реализация. Рекомендация: extension рядом с `LexemeUiItem` в dictionaryTab (`modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/entity/LexemeUiItem.kt`), т.к. UI-маппинг — забота UI-слоя.

> 📎 guide: docs/guides/data-layer.md — "API → Domain (в UseCase модуле)"
>
> 📎 guide: docs/guides/state-modeling.md — "Feature → Domain → Library, наружу нельзя. Не лепить feature-флаги в domain model."

---

## log_messages

- Зафиксирован union-shape `Lexeme` (`lexemeId: LexemeId`, `wordId: Long`, `translation`/`definition` nullable, `addDate`, `changeDate?`); `category`/`wordClass`/`options` исключены с обоснованием.
- Mapper-локация выбрана: `app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt` (метод `toDomain()`).
- `DictionaryTabUseCase` контракт сохраняет возврат UI-моделей; двойной маппинг `Api → domain → UI` внутри Impl. Wordcard/QuizChat UseCase меняют только источник `Lexeme` в типах возврата.

_model: Opus 4.7 (1M context)_
