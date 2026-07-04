# Слой Data — план изменений

> **Гайды:** `docs/guides/data-layer.md` (CoreDbApi/DAO конвенции: `flow*` для реактивных, `suspend` для разовых, nullable для «может не быть», trim-нормализация в UseCase). `naming.md` для имён методов.

> **A1 (упрощение, применено).** Reverse-lookup `getLexemeIdByComponentValueId` / DAO `selectLexemeIdById` **удалён из плана**: update-путь уже несёт `lexemeId` (в `Effect.UpsertComponentValue.lexemeId` и в `Msg.CommitComponentValueEdit`). Reverse-lookup решал проблему, которой в новом дизайне нет. UseCase-форма update: `updateComponentValue(componentValueId, lexemeId, data)`, где `lexemeId` нужен **не для самой записи** (DB-запись идёт по `componentValueId`), а только для re-read обновлённой лексемы через `getLexemeById(lexemeId)` после успешной записи. (NB: DB-метод `deleteComponentValue` принимает только `componentValueId` — никакой «симметрии по lexemeId» на уровне API нет.)

Объём минимальный: схема БД готова (M13), generic component CRUD в API уже есть, маппер `LexemeApiEntity.toDomain()` уже наполняет `components`. Нужно достроить ровно **одну** дыру в API + Impl:

1. **`flowTypesForDictionary`** — реактивная подписка на все active типы для словаря (built-in + user-defined). DAO query существует — нужно поднять через API.

Плюс **Domain helpers** — два мелких extension на `TemplateValues`, без зависимостей.

---

## 1. Domain (`:modules:domain:lexeme`)

### 1.1 NEW — `TemplateValues.kt` (extend)

Реальное содержимое сейчас (EXISTING, не трогать):
```kotlin
sealed interface TemplateValues
data class TextValues(val value: Primitive.Text) : TemplateValues
data class ImageValues(val value: Primitive.Image) : TemplateValues
```

Добавляются на уровне файла (NEW):
```kotlin
fun TemplateValues.asText(): String? = when (this) {
    is TextValues -> value.value
    is ImageValues -> null
}

fun textValuesOf(text: String): TemplateValues =
    TextValues(Primitive.Text(text))
```

Использование: UI рендер inline-edit поля + reducer commit; UseCase для построения payload `addComponentValue(... data = textValuesOf(text))`.

### 1.2 NO-OP

- `ComponentType.toRef()` уже существует и корректен — переиспользуем.
- `ComponentTemplate.fromKey(...)` — fail-soft, используем как есть.
- `BuiltInComponent`, `Primitive`, `ComponentType`, `ComponentValue`, `Lexeme`, `LexemeBuiltInExt` — без изменений.

### 1.3 Запрет

- `ComponentType.displayLabel(): String` с hardcoded `"Перевод"` — **не вводится**. Локализация в UI слое.

---

## 2. Data API (`:core:core-db-api`)

### 2.1 MODIFY — `CoreDbApi.kt`

Внутри `interface LexemeApi` добавляется **один** метод (в одной коммит-точке с Impl, иначе сборка ломается):

```kotlin
/**
 * Реактивная подписка на все active component types для словаря
 * (built-in + user-defined per-dict + global).
 * Сорт: built-in first (system_key IS NULL ASC), затем position ASC.
 */
fun flowTypesForDictionary(dictionaryId: Long): Flow<List<ComponentTypeApiEntity>>
```

**Existing `updateComponentValue` / `deleteComponentValue` (DB write) — без изменений:** оба пишут по `componentValueId`, `lexemeId` для самой записи не нужен. `lexemeId` используется только на UseCase-уровне для re-read обновлённой `Lexeme` (см. §4).

### 2.2 NO-OP

- Все остальные методы `LexemeApi` (см. реальный файл `CoreDbApi.kt`) — без изменений.
- Entity `ComponentValueApiEntity`, `ComponentTypeApiEntity`, `LexemeApiEntity` — без изменений.

---

## 3. Data Impl (`:core:core-db-impl`)

### 3.1 NO-OP — `room/dao/ComponentValueDao.kt`

Новых query нет. `selectLexemeIdById` **не добавляется** (A1 — reverse-lookup удалён).

### 3.2 NO-OP — `room/dao/ComponentTypeDao.kt`

`flowTypesForDictionary(dictionaryId: Long): Flow<List<ComponentTypeDb>>` — уже существует. Используется напрямую из Impl.

### 3.3 MODIFY — `CoreDbApiImpl.kt` (`LexemeApiImpl`)

Добавляется **одна** override-функция в существующий класс:

```kotlin
override fun flowTypesForDictionary(
    dictionaryId: Long,
): Flow<List<ComponentTypeApiEntity>> =
    componentTypeDao.flowTypesForDictionary(dictionaryId).map { list ->
        list.mapNotNull { it.toApiEntity() }
    }
```

Без транзакций (read-only).

### 3.4 NO-OP — Mapper

`LexemeApiEntity.toDomain()` в `app/.../mapper/LexemeMapper.kt` — уже маппит `components`. Без изменений. Shim'ы `translation` / `definition` остаются.

---

## 4. Связи с другими слоями

- **UseCase (Business)** — потребитель нового API + перестроенного update-пути:
  - `WordCardUseCase.flowAvailableComponentTypes(dictionaryId): Flow<List<ComponentType>>` → `lexemeApi.flowTypesForDictionary(dictId).map { it.map { it.toDomain() } }`.
  - `WordCardUseCase.updateComponentValue(componentValueId, lexemeId, data): Lexeme?` → после успешного `lexemeApi.updateComponentValue(componentValueId, data)` делает re-read через `getLexemeById(lexemeId)` (lexemeId передан из Effect/Msg, **не** ищется по cv). `lexemeId` нужен только для re-read, не для DB-записи (запись по `componentValueId`).

### 4.1 ВАЖНО — `updateComponentValue` для soft-deleted типа (B1.5)

Реальный `CoreDbApiImpl.LexemeApiImpl.updateComponentValue` (line ~412) делает внутри транзакции:
```kotlin
check(type.removedAt == null) { "Cannot update ComponentValue for soft-deleted type ..." }
```
→ **бросает `IllegalStateException`** для значения, чей тип soft-deleted в Manager. План раньше предполагал «return 0/null» — это НЕВЕРНО.

**Корректное поведение (минимальная правка, импл НЕ трогаем):** `WordCardUseCaseImpl.updateComponentValue` оборачивает вызов в `try/catch` (он там уже есть) → исключение ловится → метод возвращает `null`. Это **защита data-слоя**, а НЕ пользовательский сценарий: в потоке WordCard такой вызов **недостижим**. Удаление типа в Manager каскадно soft-удаляет все его значения (`componentValueDao.softDeleteByTypeId`), а маппер загрузки фильтрует `value.removedAt==null && type.removedAt==null` → значение с удалённым типом на карточку не грузится (см. 09 A9; Android single-screen, гонок нет). Orphan-значения, переживающего удаление типа, не существует. `deleteComponentValue` (line ~423) при этом НЕ делает `check(removedAt)` — это просто отличие DB-метода (контраст с update), не сценарий. Менять impl/убирать `check` — out of scope.
- **FlowHandler (Business)** — `AvailableComponentTypesFlowHandler` collect'ит `WordCardUseCase.flowAvailableComponentTypes(...)`.

---

## 5. Тесты

### 5.1 NO-OP — `:core:core-db-impl` DAO-тесты

Новых DAO-методов нет (A1 — `selectLexemeIdById` удалён) → **новый `ComponentValueDaoTest` не нужен**. Instrumented-стадия в CI не требуется. `flowTypesForDictionary` (`ComponentTypeDao`) уже существует и покрыт интеграционными UseCase-тестами (см. §5.2).

### 5.2 EXISTING — без правок

- `ComponentTypeDao.flowTypesForDictionary` — покрыт через интеграционные UseCase-тесты в `:app` (см. `PerDictionaryComponentsUseCaseImplTest` использует `flowUserDefinedTypesForDictionary` — паттерн идентичен).

---

## 6. Acceptance

1. `./scripts/cc-build.sh :core:core-db-api:assembleDebug` — зелёный.
2. `./scripts/cc-build.sh :core:core-db-impl:assembleDebug` — зелёный.
3. `./scripts/cc-build.sh :core:core-db-impl:testDebugUnitTest` — зелёный (unit).
4. Полный rebuild `./scripts/cc-build.sh :app:assembleDebug` — зелёный (interface signature change в API → no compile errors в consumer'ах: `CoreDbApiImpl` уже добавляет override, остальные модули не вызывают новый метод).

---

## 7. Риски

- **Bin-compat для consumers `CoreDbApi.LexemeApi`.** Любой `: CoreDbApi.LexemeApi` реализатор перестанет компилироваться при добавлении `flowTypesForDictionary`. Реальный реализатор (единственный) — `LexemeApiImpl`. **Mock-тесты НЕ ломаются:** новый метод не требует стабов в существующих mockk-тестах (non-relaxed mockk не требует стабов для невызываемых методов). Grep'ом на этапе imp проверить только что иных живых имплементоров интерфейса нет.
- **`flowTypesForDictionary` отдаёт built-in.** Reducer / UI должны рендерить chip для `TRANSLATION` (label через UI-resolver). Проверить fixture'ой в reducer-тесте `WordLoaded → ComponentTypesLoaded c TRANSLATION в списке → chip rendered`.
