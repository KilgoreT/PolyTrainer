# Design sketch: рефакторинг lexeme components

Обзорный документ изменений по слоям после полного review и triage findings. Заменяет предыдущую версию скетча.

Скоуп IS481 значительно сужен по сравнению с первоначальным планом — **mate / UI / большинство кода не меняются** (через `@Deprecated` обёртки и computed extensions). Фактически IS481 = **data-слой + API совместимости**.

Детали — в специализированных документах: [`03_database_design.md`](03_database_design.md), [`04_builtin_strategy.md`](04_builtin_strategy.md), [`05_migration_strategy.md`](05_migration_strategy.md), [`06_mapping_design.md`](06_mapping_design.md). Quiz — [`07_quiz_strategy.md`](07_quiz_strategy.md).

---

## Обзор изменений по слоям

| Слой | Что меняется | Объём | Документ |
|---|---|---|---|
| `core-db-impl` | Новые Entity (`ComponentTypeDb`, `ComponentValueDb`, `ComponentValueWithType`, **`QuizConfigDb`**), Multi-level @Relation в `LexemeDbEntity`, удаление колонок translation/definition (ALTER DROP COLUMN), DAO (включая **`QuizConfigDao`**), миграция v11→v12 (одна `Migration_011_to_012.kt` directly под `SQLiteConnection` API — AGG-12, без compat layer; см. `05.md`), JSON helper **`ComponentTypeRefJson`** | **большой** | `03` + `05` |
| `core-db-api` | Новые API DTO (`ComponentTypeApiEntity`, `ComponentValueApiEntity`, **`QuizConfigApiEntity`**), новые generic методы + старые `@Deprecated` обёртки. Начинает зависеть от `modules/domain/lexeme` (новая Gradle dep edge). | **большой** | `03` + `06` |
| `modules/domain/lexeme` (создан в IS482, расширяется в IS481) | + `ComponentValue`, `ComponentType`, `ComponentTypeId`, `ComponentValueId`, enum `BuiltInComponent`, enum `ComponentTemplate`, sealed `ComponentValueData`, computed extensions, **sealed `ComponentTypeRef` (BuiltIn / UserDefined)**, **data class `QuizConfig`** | **средний** | этот документ + `06` |
| `modules/screen/wordcard` (mate / UI / UseCase) | **Точечно тронут** (AGG-6 / MIN-9): `WordCardUseCase` interface — удалены `addLexemeDefinition` / `deleteLexemeDefinition`, добавлены generic; `WordCardUseCaseImpl` / `DatasourceEffectHandler` / `WordCardUseCaseImpl.restoreLexeme` — переписаны на generic component API; `WordCardState.hasDefinitionComponent: Boolean` (новый флаг); chip «Определение» в UI скрывается если флаг false. Translation flow остаётся через @Deprecated shim. | средний | `05` § AGG-6 чеклист |
| `modules/screen/quiz/chat` | **Частично трогается:** `QuizGameImpl.toQuizItem()` переписывается на lookup через `QuizConfig.componentRefs` + graceful skip (`null` вместо `error()`); fetch `QuizConfig` один раз в начале quiz session. См. `07.md` § Lookup в quiz. | минимальный | `07` |
| `modules/screen/dictionaryTab` | **Не меняется в IS481**, кроме одной строки в `LexemeUiItem.toUiItem()` (computed shim теперь `String?`) | минимальный | `05` § API совместимости |
| `app` (RoomModule, proguard) | ✅ **выполнено в prereq фиче** [`IS481_..._vPrepared`](../../IS481_lexeme_component_constructor_vPrepared/): bundled SQLite driver, KMP-builder Room 2.7+, `fallbackToDestructiveMigration(dropAllTables=true)` + `onDestructiveMigration` → Crashlytics, ProGuard keep-rules, 10 historical миграций v1→v11 дропнуты. В IS481 main только подключить `.addMigrations(Migration_011_to_012)` к existing builder. | минимальный | `05` § AGG-12 |

---

## Data-слой (`core-db-impl`)

См. полную схему / Entity / DAO / Database / Migration — в `03_database_design.md` и `05_migration_strategy.md`.

**Сводка:**
- Две новые таблицы: `component_types`, `component_values`.
- Изменение `lexemes`: удаляются колонки `translation`, `definition` (через `ALTER TABLE DROP COLUMN` на bundled SQLite 3.45+).
- Multi-level @Relation в `LexemeDbEntity` — Room автоматически делает batched JOIN.
- Миграция через `json_object()` для эскейпа payload (JSON1 в bundled SQLite).
- Seed built-in через `Callback.onCreate` + общую функцию `seedBuiltIns(db)`.

---

## API-слой (`core-db-api`)

### Новые сущности

В `core-db-api/entity/`:

- **`ComponentTypeApiEntity`** — DTO для типа компонента (id: Long, systemKey: BuiltInComponent?, dictionaryId: Long?, name: String?, template: ComponentTemplate, position: Int, removeDate: Date?). Типы `BuiltInComponent` / `ComponentTemplate` импортируются из `modules/domain/lexeme` (новая dep edge).
- **`ComponentValueApiEntity`** — DTO для значения компонента (id: Long, lexemeId: Long, type: ComponentTypeApiEntity, data: ComponentValueData). Поле `data: ComponentValueData` — из domain (пример «data знает domain»). Полный embedded `type` (не только id) — Multi-level @Relation подгружает `ComponentTypeDb` вместе с `ComponentValueDb` через `ComponentValueWithType`.

В `modules/domain/lexeme/`:

- **`BuiltInComponent`** — enum c **единственным значением `TRANSLATION`** (AGG-1: `DEFINITION` удалён, definition мигрирует в user-defined per-dictionary тип). Domain concept, без Android-зависимостей, без `@StringRes`.
- **`ComponentTemplate`** — enum (`TEXT`, `LONG_TEXT`, `IMAGE`). Domain concept.
- **`ComponentValueData`** — sealed interface (`TextValue`, `LongTextValue`, `ImageValue`). Domain shape. JSON helper `ComponentValueDataJson.kt` (через `org.json.JSONObject`) лежит **в `core-db-impl`** (Android library), не в domain — domain остаётся без Android-зависимостей.
- **`@JvmInline value class ComponentTypeId(val id: Long)`** и **`@JvmInline value class ComponentValueId(val id: Long)`** — value-классы domain id (по аналогии с `LexemeId`).

### Изменения существующих сущностей

- **`LexemeApiEntity`** — добавляется `val components: List<ComponentValueApiEntity>`. Поля `translation: TranslationApiEntity?` / `definition: DefinitionApiEntity?` **удаляются из API DTO** (DB ↔ API маппер уже не пробрасывает их — они переехали в `component_values`). Их computed-эквиваленты живут на domain-уровне как shim-поля `Lexeme.translation` / `.definition` (см. § Domain).
- Value-классы `TranslationApiEntity` / `DefinitionApiEntity` — остаются в `core-db-api` как `@Deprecated` (используются в сигнатурах `@Deprecated`-обёрток API методов).

### CoreDbApi — новые и старые методы

**Новые generic методы:**
```kotlin
suspend fun getComponentTypes(dictionaryId: Long): List<ComponentTypeApiEntity>
suspend fun getBuiltInComponentTypes(): List<ComponentTypeApiEntity>
suspend fun addLexemeWithBuiltInComponent(wordId: Long, dictionaryId: Long, systemKey: BuiltInComponent, data: ComponentValueData): Long
suspend fun addComponentValue(lexemeId: Long, typeId: Long, data: ComponentValueData): Long
suspend fun updateComponentValue(id: Long, data: ComponentValueData)
suspend fun deleteComponentValue(id: Long): DeleteComponentResult
```

**Translation-обёртки остаются как `@Deprecated`** (translation = built-in, drop-in shim):

```kotlin
@Deprecated("Use addLexemeWithBuiltInComponent")
suspend fun addLexemeWithTranslation(
    wordId: Long,
    dictionaryId: Long,
    translation: TranslationApiEntity,
): Long = addLexemeWithBuiltInComponent(
    wordId, dictionaryId, BuiltInComponent.TRANSLATION,
    ComponentValueData.TextValue(translation.value),
)
// Аналогично updateLexemeTranslation, deleteLexemeTranslation.
```

**Definition-обёртки удалены целиком** (AGG-6: `BuiltInComponent.DEFINITION` нет, definition теперь user-defined тип). Из `CoreDbApi.LexemeApi` убираются `addLexemeWithDefinition`, `updateLexemeDefinition`, `deleteLexemeDefinition`. Все callsite'ы переписаны на generic component API + UI блок (chip definition скрывается если в словаре нет user-defined типа Definition). См. `_alignment_decisions.md` § AGG-6.

Translation shim **критичен** — без обёрток ломаются wordcard / quiz / dictionaryTab UseCase'ы и mate-слой по translation flow.

---

## Domain-слой

### Зависимость: `modules/domain/lexeme`

До IS482 Lexeme дублировался в 3 модулях (`wordcard/entity/Lexeme.kt`, `quiz/chat/entity/Lexeme.kt`, `dictionaryTab/.../LexemeUiItem.kt`), каждый со своим `Translation` / `Definition` value class.

**Сделано в IS482** (PR #483, merged `6d3499c`): создан общий модуль `modules/domain/lexeme` с unified `Lexeme`, `Translation`, `Definition`, `LexemeId`, computed extensions. IS481 этот модуль **расширяет** — добавляет `ComponentValue`, `ComponentType`, `ComponentTypeId`, `ComponentValueId`, enum `BuiltInComponent`, enum `ComponentTemplate`, sealed `ComponentValueData` и extension `Lexeme.builtIn(key)` через built-in lookup.

`core-db-api` начинает зависеть от `modules/domain/lexeme` (новая Gradle dep edge) — DTO `ComponentTypeApiEntity` / `ComponentValueApiEntity` импортируют enum / sealed из domain.

### Новые типы (в `modules/domain/lexeme`)

```kotlin
@JvmInline value class ComponentTypeId(val id: Long)
@JvmInline value class ComponentValueId(val id: Long)

data class ComponentType(
    val id: ComponentTypeId,
    val systemKey: BuiltInComponent?,
    val dictionaryId: Long?,
    val name: String?,
    val template: ComponentTemplate,
    val position: Int,
)

data class ComponentValue(
    val id: ComponentValueId,
    val lexemeId: LexemeId,
    val type: ComponentType,        // full embedded — не только id
    val data: ComponentValueData,
)
```

### Изменения Lexeme

```kotlin
data class Lexeme(
    val lexemeId: LexemeId,
    val components: List<ComponentValue>,                                // новое
    @Deprecated("Use components") val translation: Translation? = null,  // shim — заполняется маппером из components
    @Deprecated("Use components") val definition: Definition? = null,    // shim — заполняется маппером из components
    val addDate: Date,
    val changeDate: Date? = null,
)
```

**Shim `translation` / `definition` остаются полями data class** (а не computed extensions) — чтобы mate / wordcard reducer tests НЕ ломались на mass `.translation` / `.definition` чтениях. Маппер `LexemeApiEntity.toDomain()` заполняет эти поля **из `components`** через built-in lookup (см. `06.md` § Mapper).

Value-классы `Translation` / `Definition` — **остаются в `modules/domain/lexeme`** как `@Deprecated`. Не удаляются в IS481 (нужны для drop-in shim).

**Trade-off:** дублирование данных + риск рассинхрона на `copy(translation = X)` (создаёт mismatch с `components`). Цена за неизменность mate. После mate refactor (backlog «Wordcard mate refactor: generic компоненты») — shim-поля + value-classes удаляются.

### Backward-compat extensions

```kotlin
// modules/domain/lexeme/LexemeBuiltInExt.kt

fun Lexeme.builtIn(key: BuiltInComponent): ComponentValue? =
    components.firstOrNull { it.type.systemKey == key }
// type — full embedded ComponentType (не только id), подтянут через Multi-level @Relation
```

Поля `Lexeme.translation: Translation?` и `Lexeme.definition: Definition?` — **не extensions**, а shim-поля в `Lexeme` data class (см. § «Изменения Lexeme»). Заполняются маппером `LexemeApiEntity.toDomain()` из `components` через built-in lookup. Mate / UI / UseCase wordcard продолжают читать `lexeme.translation?.value` без изменений.

---

## Quiz config (новое в IS481)

Решение и техдизайн — [`07_quiz_strategy.md`](07_quiz_strategy.md), naming решение — `_alignment_decisions.md` § OQ-1.

### Где живёт

| Слой | Сущность | Модуль |
|---|---|---|
| Room Entity | `QuizConfigDb` (table `quiz_configs`) | `core-db-impl/entity/` |
| DAO | `QuizConfigDao` (`getByDictionaryAndMode`, `insert`, `update`) | `core-db-impl/room/dao/` |
| API DTO | `QuizConfigApiEntity` | `core-db-api/entity/` |
| Domain | `QuizConfig` + sealed `ComponentTypeRef` (`BuiltIn` / `UserDefined`) | `modules/domain/lexeme/` |
| JSON helper | `ComponentTypeRefJson.kt` (`org.json.JSONObject`) | `core-db-impl/mapper/` |

`QuizConfig` и `ComponentTypeRef` (sealed) лежат в `modules/domain/lexeme` рядом с lexeme types — на старте IS481 отдельный quiz domain module избыточен (AGG-10 trade-off). **KDoc-TODO на каждом из этих типов** указывает на будущий вынос в `modules/domain/quiz` в рамках backlog-фичи «Quiz config UX» (см. `docs/Backlog.md` § Срочное).

### Как loads

В начале quiz session:
```kotlin
val config = useCase.getQuizConfig(dictionaryId, quizMode = "write")
    ?: error("QuizConfig missing for dictionary $dictionaryId") // не должно быть после миграции
```

Один SQL `SELECT` через `QuizConfigDao.getByDictionaryAndMode(...)`. JSON парсится в `List<ComponentTypeRef>` через `ComponentTypeRefJson` helper. Затем `config.componentRefs` передаётся в `toQuizItem(...)` для каждой лексемы — **lookup один раз на session**, не per-lexeme (F5 — no N+1).

### Lookup contract

```kotlin
fun WriteQuiz.toQuizItem(
    quizComponents: List<ComponentTypeRef>,
    resourceManager: ResourceManager,
    isDebugOn: Boolean,
): QuizItem? {
    val source = quizComponents.firstNotNullOfOrNull { ref ->
        lexeme.components.firstOrNull { it.matchesRef(ref) }
    } ?: return null    // graceful skip — не error()
    // ...build QuizItem из source.data
}

fun ComponentValue.matchesRef(ref: ComponentTypeRef): Boolean = when (ref) {
    is ComponentTypeRef.BuiltIn -> type.systemKey == ref.key
    is ComponentTypeRef.UserDefined -> type.systemKey == null && type.name == ref.name
}
```

Семантика:
- **Order matters** — `firstNotNullOfOrNull` берёт первый match по порядку config. Config `[BuiltIn(TRANSLATION), UserDefined("Definition")]` для лексемы с обоими компонентами → translation (приоритет).
- **Graceful skip** — лексема без любого компонента из config → `null` (skip), не crash. Filter null'ов на уровне session — `.mapNotNull { it.toQuizItem(...) }`.

После миграции каждый existing dictionary получает default config `[BuiltIn(TRANSLATION)]` + опциональный `UserDefined("Definition")` если в словаре есть definition data (AGG-4 реверс). Backward-compat автоматический.

**Для новых словарей:** `addDictionary` атомарно (одна транзакция) INSERT'ит row в `dictionaries` + для каждого зарегистрированного `quiz_mode` вызывает DAO `insertDefaultQuizConfig(newDictId, mode)`, который пишет hardcoded JSON `'[{"type":"builtin","key":"translation"}]'`. Без UI configurator (backlog) юзер не может менять default для новых словарей — пока только translation.

---

## UseCase

### Скоуп IS481 — добавление generic, сохранение старых

В `wordcard/usecase/WordCardUseCase.kt` и аналогичных:

**Добавляются** новые generic методы (`addComponentValue`, `updateComponentValue`, `deleteComponentValue`, `addLexemeWithBuiltInComponent`, `addLexemeWithUserDefinedComponent`).

**Translation flow сохраняется** через `@Deprecated` обёртки над generic. Drop-in замена реальной сигнатуры (`translation: TranslationApiEntity`, не `String`):

```kotlin
@Deprecated("Use addLexemeWithBuiltInComponent")
suspend fun addLexemeWithTranslation(
    wordId: Long,
    dictionaryId: Long,
    translation: TranslationApiEntity,
): Long = addLexemeWithBuiltInComponent(
    wordId, dictionaryId, BuiltInComponent.TRANSLATION,
    ComponentValueData.TextValue(translation.value),
)
```

**Definition flow переписан** (AGG-6): из `WordCardUseCase` interface удалены `addLexemeDefinition` / `deleteLexemeDefinition`. `WordCardUseCaseImpl` / `DatasourceEffectHandler` / `WordCardUseCaseImpl.restoreLexeme` (MIN-9) переписаны на generic `addLexemeWithUserDefinedComponent(wordId, dictionaryId, name="Definition", data)` / `updateComponentValue(id, data)` / `deleteComponentValue(id)`. UI блок: chip «Определение» скрывается если `WordCardState.hasDefinitionComponent == false` (флаг грузится при load wordcard по наличию user-defined типа `name="Definition"`).

**Атомарность** `addLexemeWithTranslation` (domain-инвариант — lexeme + write_quiz + первый компонент в транзакции) сохраняется внутри новой `addLexemeWithBuiltInComponent` и `addLexemeWithUserDefinedComponent`.

---

## Не затрагивается в IS481

- **mate-слой wordcard** (`State`, `Msg`, `Effect`, `Reducer`) — **в основном** работает как раньше благодаря shim-полям `translation` / `definition` в `Lexeme` data class + value-classes `Translation` / `Definition` в `modules/domain/lexeme`. Маппер `LexemeApiEntity.toDomain()` заполняет shim из `components` (translation — через built-in lookup; definition — через user-defined `name="Definition"` lookup). После mate refactor (backlog) — shim удаляется.
- **Точечно тронут** (AGG-6 / MIN-9): добавлен `WordCardState.hasDefinitionComponent: Boolean` флаг + соответствующий load в effect. `Reducer` / `extension updateLexemeDefinitionText` — НЕ трогаем (state mutation через shim продолжает работать).
- **UI wordcard** (`LexemeMeaningField`, `AddLexemeMeaningRow`, и др.) — точечно: chip «Определение» скрывается если `state.hasDefinitionComponent == false`. Translation chip без изменений.
- **wordcard reducer tests** (~14 файлов с `.translation` / `.definition` assertions) — продолжают работать через shim-поля.
- **Search** (`WordDao.searchTerms*`) — оперирует только `words.value`, миграция не задевает.
- **dictionaryTab** — через shim-поля работает как было. Маппинг `LexemeUiItem.toUiItem()` остаётся без изменений (читает `lexeme.translation?.value` через value class — сохранено).
- **quiz/chat** — **частично трогается** (это исключение из «не меняется в IS481»):
  - `QuizGameImpl.toQuizItem(writeQuiz)` переписывается на lookup через `QuizConfig.componentRefs` + graceful skip (см. `07.md` § Lookup в quiz). Старая логика `lexeme.translation` → `lexeme.definition` → `error()` удаляется.
  - В начале quiz session — fetch `QuizConfig` через `getQuizConfig(dictionaryId, "write")` (см. `07.md` § «No N+1»).
  - Definition-only лексемы рендерятся корректно через `UserDefined("Definition")` в config.
  - Lexeme без любого компонента из config → `toQuizItem` возвращает `null` (skip, не crash).

Refactor wordcard mate на generic компоненты — отдельная фича в backlog (`docs/Backlog.md` § Архитектура). Триггер на выпиливание shim — описан в backlog-записи.

---

## Зависимости и порядок работ

1. **Сделано (IS482):** модуль `modules/domain/lexeme` создан (PR #483, merged `6d3499c`) с unified `Lexeme` / `Translation` / `Definition` / `LexemeId`. IS481 этот модуль расширяет — добавляет `ComponentValue`, `ComponentType`, `BuiltInComponent`, `ComponentTemplate`, computed extensions.
2. **Текущая фича:** IS481 (data-слой + API совместимости).
3. **Параллельно (или после):** решение по `07_quiz_strategy.md`.
4. **Позже:** Wordcard mate refactor на generic (отдельная фича в backlog).

---

## Что НЕ затрагивается на старте

- **UI настройки типов** (создание / редактирование / удаление user-defined типов) — out of scope.
- **Шаблон `image`** — sealed подкласс `ImageValue` определён, активного использования нет.
- **Soft-delete cleanup** — поле `remove_date` есть, retention-job не реализован (не нужен пока user-defined типы не создаются через UI).
- **TBD scope-объявления** (см. `01_research.md` § 1) — остаёмся с nullable `dictionaryId`.
- **Композирование составных шаблонов** (table, structured) — за рамками.

---

## Риски

См. также:
- `03_database_design_review.md` — закрытые findings по БД-дизайну.
- `05_migration_strategy_review.md` — закрытые findings по миграции.
- `06_mapping_design_review.md` — закрытые findings по маппингу.

Главные оставшиеся:
1. **Bundled SQLite** через новый KMP-builder Room 2.7+ — без перехода `.setDriver()` не активируется. Блокер миграции. См. `05` чеклист «Bundled SQLite».
2. ~~**Quiz strategy** для definition-only лексем~~ — **решено**: `QuizConfig` per `(dictionary_id, quiz_mode)` + graceful skip. См. `07_quiz_strategy.md`.

---

## Следующие шаги

1. ~~**Lexeme domain unification** (`modules/domain/lexeme`)~~ — **сделано в IS482** (PR #483, merged `6d3499c`).
2. ~~**Решение по quiz strategy**~~ — **решено** в `07_quiz_strategy.md` (отдельная таблица `quiz_configs` + `QuizConfig` domain entity по OQ-1).
3. **Запуск IS481** через адаптивный flow — alignment pass завершён, документы синхронизированы.
