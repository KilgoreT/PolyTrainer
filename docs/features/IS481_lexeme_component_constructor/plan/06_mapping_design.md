# Mapping design: DB ↔ API ↔ Domain

Детальное описание маппинга `ComponentType` / `ComponentValue` между слоями.

**Контекст слоёв** — см. [`04_builtin_strategy.md`](04_builtin_strategy.md) § «Слойность». Здесь — конкретный код мапперов и парсинг JSON-payload.

---

## ✅ Чеклист задач маппинга

При реализации пройти каждый пункт.

### Сериализация ComponentValueData

- [ ] Sealed `ComponentValueData` — domain shape, живёт в `modules/domain/lexeme/.../` с вариантами `TextValue` / `LongTextValue` / `ImageValue`. **Без kotlinx.serialization** — используем встроенный `org.json.JSONObject`.
- [ ] Helper `ComponentValueData.toJson(): String` + `String.toComponentValueData(template: ComponentTemplate): ComponentValueData` — файл `ComponentValueDataJson.kt` в **`core-db-impl`** (Android library, `org.json.JSONObject` доступен). Domain-модуль остаётся без Android-зависимостей.

### DB → API мапперы (в `core-db-impl`)

- [ ] `ComponentTypeDb.toApiEntity(): ComponentTypeApiEntity` — конверсия `String? → BuiltInComponent?`, `String → ComponentTemplate`.
- [ ] `ComponentValueDb.toApiEntity(type: ComponentType): ComponentValueApiEntity` — парсинг JSON `value` через `String.toComponentValueData(template)`.
- [ ] `ComponentValueWithType.toApiEntity()` — комбинированный маппер (использует тип внутри для парсинга value).
- [ ] Обновлённый `LexemeDbEntity.toApiEntity()` — теперь содержит список `ComponentValueApiEntity` через Multi-level @Relation.
- [ ] **Thread**: в репозитории Flow с маппингом обернуть в `.flowOn(Dispatchers.Default)` или `withContext(Dispatchers.IO)` для парсинга JSON на background — на большом DictionaryTab (1000 лексем) парсинг 3000 объектов занимает ~60ms (близко к кадру), нельзя на main thread.

### API → Domain (в `app/`)

**Location** (AGG-2): мапперы API → Domain живут в `app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt` (паттерн IS482 — файл уже существует). **НЕ в `modules/domain/lexeme`** — это создаст циклическую Gradle dep (`core-db-api` зависит от domain по A1; если mapper в domain — domain должен знать про `LexemeApiEntity` → цикл). `app/` видит оба слоя, циклов нет.

- [ ] `ComponentTypeApiEntity.toDomain(): ComponentType` — value class wrapping (`ComponentTypeId(id)`).
- [ ] `ComponentValueApiEntity.toDomain(): ComponentValue` — то же.
- [ ] Обновлённый `LexemeApiEntity.toDomain(): Lexeme` — теперь содержит `components: List<ComponentValue>` + **shim translation / definition** (B4/C2):
  ```kotlin
  fun LexemeApiEntity.toDomain(): Lexeme = Lexeme(
      lexemeId = LexemeId(id),
      components = components.map { it.toDomain() },
      translation = components.firstOrNull { it.type.systemKey == BuiltInComponent.TRANSLATION }
          ?.data?.let { (it as? ComponentValueData.TextValue)?.text }?.let { Translation(it) },
      // AGG-1: definition shim через user-defined lookup, НЕ built-in:
      definition = components.firstOrNull { it.type.systemKey == null && it.type.name == "Definition" }
          ?.data?.let { (it as? ComponentValueData.TextValue)?.text }?.let { Definition(it) },
      addDate = addDate,
      changeDate = changeDate,
  )
  ```

### Обратные мапперы (для INSERT/UPDATE)

- [ ] `ComponentTypeApiEntity.toDb(): ComponentTypeDb` — конверсия enum → String через `.key`.
- [ ] `ComponentValueData.toDbValue(): String` — JSON-сериализация для INSERT/UPDATE через DAO.

### Quiz config (per OQ-1)

- [ ] Sealed `ComponentTypeRef` в `modules/domain/lexeme` — варианты `BuiltIn(key: BuiltInComponent)` и `UserDefined(name: String)`. **KDoc-TODO** (AGG-10): «вынести в `modules/domain/quiz` в рамках backlog-фичи Quiz config UX».
- [ ] Data class `QuizConfig` в `modules/domain/lexeme` (живёт вместе с lexeme types; future — переедет в `modules/domain/quiz` в backlog-фиче). **KDoc-TODO** на вынос.
- [ ] Data class `QuizConfigApiEntity` в `core-db-api/entity/` — DTO для API-слоя.
- [ ] Helper `ComponentTypeRefJson.kt` в `core-db-impl` (Android-зависимый `org.json.JSONObject`) — sealed `List<ComponentTypeRef>` ↔ JSON array со discriminator (`type: "builtin" | "user"`).
- [ ] **Read-mapper** `QuizConfigDb.toApiEntity(): QuizConfigApiEntity` — парсит JSON `component_refs` в `List<ComponentTypeRef>`. **Thread policy** (MIN-6): inline в mapper, без `flowOn` / cache. Объём малый (1-5 row × несколько `ComponentTypeRef`), читается раз при quiz session start. **Не путать с `ComponentValueData` thread policy для DictionaryTab** (см. § Сериализация ComponentValueData) — там 3000 объектов на крупном словаре, тут на порядки меньше.
- [ ] Маппер `QuizConfigApiEntity.toDomain(): QuizConfig` — value class wrap для ID не нужен (по OQ-1 — `dictionaryId: Long` без value class).
- [ ] **Write-mapper УДАЛЁН** (MIN-8). `QuizConfig.toApiEntity()` и `QuizConfigApiEntity.toDb()` НЕ нужны в IS481 (нет потребителя — UI configurator в backlog). Вместо них:
  - **DAO `insertDefaultQuizConfig(dictionaryId: Long, quizMode: String)`** — простой method, пишет hardcoded JSON `'[{"type":"builtin","key":"translation"}]'` через `connection.execSQL`. Без mapper'а / без передачи доменного `QuizConfig`.
  - **Migration_11_12** — direct SQL `INSERT INTO quiz_configs ...` (без mapper'а).
  - Write-mapper появится в backlog-фиче «Quiz config UX» когда UI configurator будет писать произвольные `QuizConfig` объекты.

---

## Обзор слоёв

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ Layer       │ Entity                  │ systemKey      │ value             │
├─────────────────────────────────────────────────────────────────────────────┤
│ Room (impl) │ ComponentTypeDb         │ String?        │ —                 │
│             │ ComponentValueDb        │ —              │ String (JSON)     │
├─────────────────────────────────────────────────────────────────────────────┤
│ API (DTO)   │ ComponentTypeApiEntity  │ BuiltInComponent? (enum из domain) │
│             │ ComponentValueApiEntity │ —              │ ComponentValueData │
│             │                         │                │  (sealed из domain)│
├─────────────────────────────────────────────────────────────────────────────┤
│ Domain      │ ComponentType           │ BuiltInComponent? (source)     │
│ (lexeme)    │ ComponentValue          │ —              │ ComponentValueData │
│             │ BuiltInComponent enum   │                │ (source)          │
│             │ ComponentTemplate enum  │                │                   │
│             │ ComponentValueData sealed│               │                   │
└─────────────────────────────────────────────────────────────────────────────┘
```

`BuiltInComponent`, `ComponentTemplate`, `ComponentValueData` — domain concepts, живут в `modules/domain/lexeme`. `core-db-api` зависит от `modules/domain/lexeme` (новая dep edge) и переиспользует эти типы (data знает domain — инверсия Dependency Rule).

Маппинг enum ↔ string — на границе **Room → API**. Маппер в `core-db-impl`. Sealed `ComponentValueData` парсится из JSON-строки с использованием `template` (из родительского `ComponentType`) для определения формата — helper `ComponentValueDataJson.kt` тоже в `core-db-impl` (нужен `org.json.JSONObject` — Android-зависимость).

---

## ComponentValueData — sealed + сериализация

> **Disclaimer по R-N-009.** Правило R-N-009 в `naming.md` («sealed → TEXT через TypeConverter, не String + ручной парсинг») в этом случае **неприменимо** — это polymorphic payload, где выбор sealed-варианта зависит от `templateKey` parent-сущности `ComponentType`. TypeConverter получает только value-колонку, не имеет доступа к parent → не может выбрать вариант. Ручной парсер на уровне маппера (где есть контекст через @Relation) — единственно правильный путь. Исключение зафиксировано в R-N-009.

### Определение

```kotlin
// modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentValueData.kt

sealed interface ComponentValueData {
    data class TextValue(val text: String) : ComponentValueData
    data class LongTextValue(val text: String) : ComponentValueData
    data class ImageValue(val uri: String) : ComponentValueData
}
```

Никаких JSON-аннотаций — мы используем встроенный `org.json.JSONObject` (Android SDK), без kotlinx.serialization / Gson. Парсер и сериализатор ручные, 5-10 строк (см. ниже) — лежат в `core-db-impl`, где доступна Android-зависимость `org.json`.

### JSON-формат (хранение в БД)

JSON соответствует тому что генерирует `json_object()` в SQL миграции:

```json
// template = "text"      → {"v":1,"text":"слово"}
// template = "long_text" → {"v":1,"text":"длинный\nтекст"}
// template = "image"     → {"v":1,"uri":"content://media/..."}
```

Дискриминатор типа в JSON **не нужен** — template из `ComponentType` определяет какой sealed-вариант парсить. Поле **`v`** (payload schema version) присутствует с самого начала — страховка от будущих расширений `ComponentValueData` (см. § «Правило эволюции» ниже). Стартовая версия — `1`.

### Сериализация / десериализация

```kotlin
// core-db-impl/src/main/java/me/apomazkin/core_db_impl/mapper/ComponentValueDataJson.kt

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentValueData
import org.json.JSONObject

private const val PAYLOAD_VERSION = 1

fun ComponentValueData.toJson(): String = when (this) {
    is ComponentValueData.TextValue     -> JSONObject().put("v", PAYLOAD_VERSION).put("text", text).toString()
    is ComponentValueData.LongTextValue -> JSONObject().put("v", PAYLOAD_VERSION).put("text", text).toString()
    is ComponentValueData.ImageValue    -> JSONObject().put("v", PAYLOAD_VERSION).put("uri", uri).toString()
}

fun String.toComponentValueData(template: ComponentTemplate): ComponentValueData {
    val json = JSONObject(this)
    // val v = json.optInt("v", 1)  // version read для будущих миграций payload — на старте всегда 1
    return when (template) {
        ComponentTemplate.TEXT      -> ComponentValueData.TextValue(json.getString("text"))
        ComponentTemplate.LONG_TEXT -> ComponentValueData.LongTextValue(json.getString("text"))
        ComponentTemplate.IMAGE     -> ComponentValueData.ImageValue(json.getString("uri"))
    }
}
```

Парсер выбирает sealed-вариант по `template`, а не по дискриминатору в JSON. `JSONObject` обрабатывает экранирование автоматически (symmetric с SQL `json_object()`). Никаких зависимостей / плагинов / proguard-rules — `org.json` встроен в Android SDK.

---

## DB → API мапперы

### `ComponentTypeDb.toApiEntity()`

```kotlin
// core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/ComponentTypeDb.kt
// (extension рядом с data class — конвенция проекта, см. WordDb.kt)

import me.apomazkin.core_db_api.entity.ComponentTypeApiEntity
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate

fun ComponentTypeDb.toApiEntity(): ComponentTypeApiEntity = ComponentTypeApiEntity(
    id = id,
    systemKey = systemKey?.let(BuiltInComponent::fromKey),  // String? → enum?
    dictionaryId = dictionaryId,
    name = name,
    template = ComponentTemplate.fromKey(templateKey),           // String → enum (non-null, fallback TEXT)
    position = position,
    removeDate = removeDate,
)
```

**Тонкости:**
- `systemKey?.let(...)` — для NULL остаётся NULL (user-defined). Для строки — конверсия через `fromKey()` (тоже nullable, см. § «Unknown system_key» в `04_builtin_strategy_review.md` — отвергнут, поэтому здесь null от неизвестного ключа возможен; trade-off accepted).
- `ComponentTemplate.fromKey()` — non-null с fallback TEXT внутри.

### `ComponentValueWithType.toApiEntity()`

```kotlin
fun ComponentValueWithType.toApiEntity(): ComponentValueApiEntity {
    val typeApi = type.toApiEntity()
    val data = value.value.toComponentValueData(typeApi.template)  // парсинг JSON
    return ComponentValueApiEntity(
        id = value.id,
        lexemeId = value.lexemeId,
        type = typeApi,                                            // full embedded — не только id
        data = data,
    )
}
```

Multi-level @Relation подгружает `type` вместе с `value`. Room генерирует **3 batched SELECT'a** (по одному на каждый уровень, с `WHERE ... IN (...)`): `lexemes` → `component_values WHERE lexeme_id IN (...)` → `component_types WHERE id IN (...)`. N+1 не возникает на любом количестве лексем.

### Обновлённый `LexemeDbEntity.toApiEntity()`

```kotlin
fun LexemeDbEntity.toApiEntity() = LexemeApiEntity(
    id = lexemeDb.id,
    components = componentValueListDb.map { it.toApiEntity() },  // ⬅ новое
    wordClass = lexemeDb.wordClass,
    options = lexemeDb.options,
    addDate = lexemeDb.addDate,
    changeDate = lexemeDb.changeDate,
    // translation / definition удалены из API DTO — доступ через shim-поля domain Lexeme (заполняются маппером .toDomain() из components).
    // wordId не пробрасывается — после IS482 LexemeApiEntity не содержит этого поля.
)
```

---

## API → Domain мапперы

**AGG-2:** все API→Domain мапперы (`*ApiEntity.toDomain()` + value-class declarations нужные мапперу) живут в `app/...mapper/LexemeMapper.kt`, **не** в `modules/domain/lexeme`. Причина — Dependency Rule: domain — вершина графа (никаких dep на `core-db-api`), `core-db-api` не должен импортировать `app`. Map-функции работают через два модуля → должны жить в общем downstream — `app`.

### `ComponentTypeApiEntity.toDomain()`

```kotlin
// app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt
// (все API→Domain мапперы IS481 — в одном файле)

import me.apomazkin.core_db_api.entity.ComponentTypeApiEntity
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId

@JvmInline value class ComponentTypeId(val id: Long)   // объявлен в modules/domain/lexeme

fun ComponentTypeApiEntity.toDomain(): ComponentType = ComponentType(
    id = ComponentTypeId(id),                  // value class в domain
    systemKey = systemKey,                     // enum проброшен как есть
    dictionaryId = dictionaryId,
    name = name,
    template = template,                       // enum проброшен
    position = position,
    removeDate = removeDate,
)
```

### `ComponentValueApiEntity.toDomain()`

```kotlin
// app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt

@JvmInline value class ComponentValueId(val id: Long)   // объявлен в modules/domain/lexeme

fun ComponentValueApiEntity.toDomain(): ComponentValue = ComponentValue(
    id = ComponentValueId(id),
    lexemeId = LexemeId(lexemeId),
    type = type.toDomain(),                    // full embedded ComponentType
    data = data,                               // sealed проброшен как есть
)
```

### Изменения Domain `Lexeme`

Текущий `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt`:
```kotlin
data class Lexeme(
    val lexemeId: LexemeId,
    val translation: Translation?,
    val definition: Definition?,
    val addDate: Date,
    val changeDate: Date? = null,
)
@JvmInline value class Translation(val value: String)
@JvmInline value class Definition(val value: String)
```

После рефакторинга (B4/C2 — shim в IS481, mate refactor отдельной фичей):
```kotlin
data class Lexeme(
    val lexemeId: LexemeId,
    val components: List<ComponentValue>,                                // ← новое
    @Deprecated("Use components") val translation: Translation? = null,  // ← shim, заполняется маппером из components
    @Deprecated("Use components") val definition: Definition? = null,    // ← shim, заполняется маппером из components
    val addDate: Date,
    val changeDate: Date? = null,
)
// Value-классы Translation / Definition ОСТАЮТСЯ в modules/domain/lexeme как @Deprecated.
// Не удаляются в IS481 — нужны для drop-in shim, mate / wordcard reducer tests
// продолжают работать без правок. Удаляются после mate refactor (backlog).
@Deprecated("Use ComponentValueData.TextValue")
@JvmInline value class Translation(val value: String)
@Deprecated("Use ComponentValueData.TextValue")
@JvmInline value class Definition(val value: String)
```

Built-in lookup — extension в `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/LexemeBuiltInExt.kt`:
```kotlin
fun Lexeme.builtIn(key: BuiltInComponent): ComponentValue? =
    components.firstOrNull { it.type.systemKey == key }
// type — full embedded ComponentType (не только id), подтянут Multi-level @Relation в ComponentValueWithType.
```

Поля `Lexeme.translation` / `.definition` оставлены как shim-поля data class (а не computed extensions), чтобы mate / wordcard reducer tests с массовыми `.translation` / `.definition` чтениями работали без правок. Заполняются маппером `LexemeApiEntity.toDomain()` ниже.

### Обновлённый `LexemeApiEntity.toDomain()`

```kotlin
fun LexemeApiEntity.toDomain(): Lexeme {
    val components = components.map { it.toDomain() }
    return Lexeme(
        lexemeId = LexemeId(id),
        components = components,
        translation = components
            .firstOrNull { it.type.systemKey == BuiltInComponent.TRANSLATION }
            ?.data
            ?.let { (it as? ComponentValueData.TextValue)?.text }
            ?.let { Translation(it) },
        // AGG-1: definition shim через user-defined lookup, НЕ built-in (BuiltInComponent.DEFINITION удалён).
        definition = components
            .firstOrNull { it.type.systemKey == null && it.type.name == "Definition" }
            ?.data
            ?.let { (it as? ComponentValueData.TextValue)?.text }
            ?.let { Definition(it) },
        addDate = addDate,
        changeDate = changeDate,
    )
}
```

Маппер заполняет shim-поля `translation` / `definition` из `components` через built-in lookup. Mate читает `lexeme.translation?.value` как раньше — без правок. После mate refactor (backlog) маппер сократится: shim-поля и value-классы `Translation` / `Definition` будут удалены.

**Debug-assertion shim consistency:** в `BuildConfig.DEBUG` маппер проверяет что shim-поля согласованы с `components` built-in lookup — иначе ошибка в самом маппере детектируется до prod:

```kotlin
if (BuildConfig.DEBUG) {
    val expectedT = components
        .firstOrNull { it.type.systemKey == BuiltInComponent.TRANSLATION }
        ?.data?.let { (it as? ComponentValueData.TextValue)?.text }
    check(translation?.value == expectedT) {
        "Shim/components рассинхрон translation: shim=$translation vs components=$expectedT"
    }
    // аналогично для definition
}
```

В release сборке `BuildConfig.DEBUG == false`, проверка no-op. **Не закрывает** `lexeme.copy(translation = X)` mutation в mate — рассинхрон создаётся **после** маппера. Это known trade-off, закрывается при mate refactor.

---

## Обратные мапперы (для INSERT/UPDATE)

### `ComponentTypeApiEntity.toDb()`

```kotlin
fun ComponentTypeApiEntity.toDb(): ComponentTypeDb = ComponentTypeDb(
    id = id,
    systemKey = systemKey?.key,                // enum → String через .key
    dictionaryId = dictionaryId,
    name = name,
    templateKey = template.key,                // enum → String через .key
    position = position,
    removeDate = removeDate,
)
```

### Сериализация `ComponentValueData` для INSERT

```kotlin
// При INSERT через DAO:
val valueDb = ComponentValueDb(
    lexemeId = lexemeId,
    componentTypeId = componentTypeId,
    value = data.toJson(),                     // sealed → JSON string
)
componentValueDao.insert(valueDb)
```

Парсинг (`toComponentValueData`) симметричен сериализации — формат стабилен.

---

## QuizConfig + ComponentTypeRef (per OQ-1)

Контекст: [`07_quiz_strategy.md`](07_quiz_strategy.md), решение [`_alignment_decisions.md`](_alignment_decisions.md) § OQ-1.

### ComponentTypeRef — sealed (domain)

```kotlin
// modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentTypeRef.kt

sealed interface ComponentTypeRef {
    @JvmInline value class BuiltIn(val key: BuiltInComponent) : ComponentTypeRef
    @JvmInline value class UserDefined(val name: String) : ComponentTypeRef
}
```

Sealed с двумя variants:
- `BuiltIn(key)` — ссылка на built-in компонент через stable enum-key. Стабилен через export/import (`BuiltInComponent.key` — стабильный системный ключ, не БД id).
- `UserDefined(name)` — ссылка на user-defined компонент per-dictionary через имя. Уникальность гарантирована `UNIQUE(dictionary_id, name)` в `component_types` (см. `03.md`).

Локация: `modules/domain/lexeme` (общий domain module для всех component-related types по A1 в `_alignment_decisions.md`).

### QuizConfig — domain entity

```kotlin
// modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/QuizConfig.kt

data class QuizConfig(
    val dictionaryId: Long,
    val quizMode: String,
    val componentRefs: List<ComponentTypeRef>,
)
```

Лежит в `modules/domain/lexeme` вместе с lexeme types — на старте feature нет отдельного quiz domain module, и тащить новый модуль ради одного типа избыточно. **Future:** при появлении более сложной quiz domain (несколько режимов, отдельный quiz settings UI) — `QuizConfig` + `ComponentTypeRef` могут переехать в `modules/domain/quiz` без breaking changes (только Gradle dep edge перевешивается).

### QuizConfigApiEntity — DTO

```kotlin
// core-db-api/src/main/java/me/apomazkin/core_db_api/entity/QuizConfigApiEntity.kt

data class QuizConfigApiEntity(
    val id: Long,
    val dictionaryId: Long,
    val quizMode: String,
    val componentRefs: List<ComponentTypeRef>,    // domain type — `core-db-api` уже зависит от `modules/domain/lexeme` (A1)
)
```

### JSON-формат `component_refs`

JSON array объектов с discriminator-полем `type`:

```json
// BuiltIn(BuiltInComponent.TRANSLATION) → {"type":"builtin","key":"translation"}
// UserDefined("Definition")             → {"type":"user","name":"Definition"}

// Полный config dictionary с definition:
[
    {"type": "builtin", "key": "translation"},
    {"type": "user",    "name": "Definition"}
]
```

Discriminator (`type: "builtin" | "user"`) необходим — в отличие от `ComponentValueData` (где variant выбирается по `template` parent-сущности), здесь нет внешнего ключа выбора variant; используется явный discriminator в payload.

### Сериализация / десериализация

```kotlin
// core-db-impl/src/main/java/me/apomazkin/core_db_impl/mapper/ComponentTypeRefJson.kt

import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTypeRef
import org.json.JSONArray
import org.json.JSONObject

fun List<ComponentTypeRef>.toJson(): String {
    val arr = JSONArray()
    forEach { ref ->
        val obj = JSONObject()
        when (ref) {
            is ComponentTypeRef.BuiltIn -> {
                obj.put("type", "builtin")
                obj.put("key", ref.key.key)
            }
            is ComponentTypeRef.UserDefined -> {
                obj.put("type", "user")
                obj.put("name", ref.name)
            }
        }
        arr.put(obj)
    }
    return arr.toString()
}

/**
 * Defensive parser: при corrupt JSON / unknown discriminator / unknown enum key
 * возвращает пустой List вместо crash. Quiz UseCase реагирует на пустой config —
 * показывает сообщение «Конфиг квиза повреждён», не падает.
 *
 * Защита от: прямой правки БД, runtime corruption после backup/restore, ошибки миграции.
 */
fun String.toComponentTypeRefList(): List<ComponentTypeRef> = try {
    val arr = JSONArray(this)
    List(arr.length()) { i ->
        val obj = arr.getJSONObject(i)
        when (val type = obj.getString("type")) {
            "builtin" -> {
                val key = BuiltInComponent.fromKey(obj.getString("key"))
                    ?: error("Unknown BuiltInComponent key: ${obj.getString("key")}")
                ComponentTypeRef.BuiltIn(key)
            }
            "user" -> ComponentTypeRef.UserDefined(obj.getString("name"))
            else -> error("Unknown ComponentTypeRef discriminator: $type")
        }
    }
} catch (e: Exception) {
    logger.e(tag = "ComponentTypeRefJson", message = "Corrupt component_refs JSON: $this", throwable = e)
    emptyList()
}
```

Helper лежит в `core-db-impl` по принципу A1 — Android-зависимые helpers (`org.json.JSONObject` / `JSONArray`) в impl-модуле.

**Контракт обработки corrupt JSON в quiz UseCase:** если `getQuizConfig(...)` возвращает `QuizConfig` с пустым `componentRefs` (либо после corrupt-парсинга, либо после полной cleanup'а по F6) — quiz UseCase возвращает «empty session» статус (UX: «Конфиг квиза повреждён или пуст. Настройте через UI редактирования компонентов.» — в backlog UI configurator). Crash недопустим.

### Мапперы QuizConfig (MIN-8: только read-маппер в IS481)

**MIN-8:** в IS481 нужен **только** read-маппер `QuizConfigDb.toApiEntity()` (для `getQuizConfig` lookup в quiz UseCase). Write-маппер `QuizConfigApiEntity.toDb()` и `QuizConfig.toApiEntity()` **не нужны** — IS481 не пишет `QuizConfig` объекты через UseCase. INSERT в `quiz_configs` происходит двумя путями, оба обходят domain-mapper:
- **Миграция M11→M12** — прямой SQL INSERT/UPDATE с hardcoded JSON через bundled SQLite JSON1 функции (см. `05.md` § 7.2/7.3).
- **`addDictionary` auto-INSERT** (AGG-4 реверс) — DAO method `insertDefaultQuizConfig(dictionaryId, quizMode)` пишет hardcoded JSON `'[{"type":"builtin","key":"translation"}]'`. Не принимает доменный `QuizConfig` объект — никакой write-mapping не нужен.

```kotlin
// core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/QuizConfigDb.kt

fun QuizConfigDb.toApiEntity(): QuizConfigApiEntity = QuizConfigApiEntity(
    id = id,
    dictionaryId = dictionaryId,
    quizMode = quizMode,
    componentRefs = componentRefs.toComponentTypeRefList(),
)
```

```kotlin
// app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt
// (общий файл с остальными API→Domain мапперами IS481, AGG-2)

import me.apomazkin.core_db_api.entity.QuizConfigApiEntity
import me.apomazkin.lexeme.QuizConfig

fun QuizConfigApiEntity.toDomain(): QuizConfig = QuizConfig(
    dictionaryId = dictionaryId,
    quizMode = quizMode,
    componentRefs = componentRefs,    // domain тип уже — копирование как есть
)
```

Маппер API → Domain — почти no-op (типы `componentRefs` те же `List<ComponentTypeRef>`). Существует для слойной строгости: domain не импортирует `core-db-api` напрямую.

**Future:** write-mapper `QuizConfig.toApiEntity()` + UseCase `upsertQuizConfig(config)` появятся в backlog-фиче «Quiz config UX: UI редактор `quiz_configs.component_refs`» (см. `docs/Backlog.md`), когда появится UI для редактирования config'а.

### UseCase contract (IS481 scope)

```kotlin
// core-db-api / wordcard-api или quiz-api — финально на этапе implement.
// IS481 нужен ТОЛЬКО read API + DAO для seed default.
suspend fun getQuizConfig(dictionaryId: Long, quizMode: String = "write"): QuizConfigApiEntity?
```

**`upsertQuizConfig(config: QuizConfigApiEntity)` — backlog**, не IS481. См. § «Мапперы QuizConfig» выше + entry «Quiz config UX» в `docs/Backlog.md`.

`insertDefaultQuizConfig(dictionaryId, quizMode)` живёт **только** в DAO (`core-db-impl`), вызывается из `addDictionary` UseCase impl внутри транзакции (AGG-4 реверс). Не выставлен наружу через `core-db-api` UseCase contract — это implementation detail атомарности `addDictionary`.

Lookup в quiz session — один раз в начале session (см. `07.md` § «No N+1»), не per-lexeme.

---

⚠ **Правило эволюции:** payload содержит поле `v` (schema version) — на старте `1`. Стратегия:
- **Backward-compatible изменение** (новое опциональное поле, например `language` в `TextValue`) — поле должно иметь default value (nullable / дефолтное значение). В парсере — `json.optString("language", null)`. Версия `v` остаётся `1` (старые и новые payload совместимы).
- **Breaking изменение** (изменение семантики существующих полей, обязательное новое поле без дефолта) — увеличить `PAYLOAD_VERSION` до `2`, в парсере `when (v) { 1 -> ...; 2 -> ... }` или явная миграция payload (UPDATE по всем строкам этого template).
- Без поля `v` (легаси с предыдущих версий миграции) — `optInt("v", 1)` даст дефолт `1`, читается как старый формат. На старте такого случая нет — миграция v11→v12 пишет `v=1` сразу.

---

## Edge cases

1. **Unknown systemKey при чтении.** `BuiltInComponent.fromKey("unknown")` → `null`. Маппер возвращает `systemKey = null` → API-потребитель видит row как user-defined. Trade-off accepted (см. Edge #4 в `04_builtin_strategy_review.md`, → rejected как dev-only сценарий).

2. **Unknown templateKey при чтении.** `ComponentTemplate.fromKey("unknown")` → fallback `TEXT`. Парсинг JSON через `TextValue` — если структура совпадает (поле `text`), читается; если нет — `SerializationException`. Маловероятно (template только меняется миграцией).

3. **Невалидный JSON в `value`.** Например после прямого вмешательства в БД. `JSONObject(badJson)` бросает `org.json.JSONException` — нужно обернуть `String.toComponentValueData(template)` в try-catch или возвращать sealed `Result<ComponentValueData>` на уровне маппера. UseCase / репозиторий решает: пропустить компонент (skip) или пробросить ошибку дальше. Покрыто отдельным test-кейсом в `ComponentValueDataJsonTest`.

4. **Component без типа в БД (orphan).** Невозможен — FK CASCADE удаляет values при удалении типа. Если как-то возник (например через прямой SQL) — Multi-level @Relation вернёт пустой `type` (Room не падает, просто пропускает).

---

## Тестирование

В `core/core-db-impl/src/test/`:

- `ComponentTypeDbMapperTest` — проверка конверсии Db ↔ Api для всех 4 комбинаций (built-in default / built-in override / user-defined per-dictionary / user-defined global).
- `ComponentValueDataJsonTest` — проверка сериализации / десериализации для всех вариантов sealed + спецсимволов (эмодзи, переносы строк, кавычки).
  - **Invalid JSON в `value` колонке** — `String.toComponentValueData(template)` обернуть в try-catch или возвращать sealed Result; добавить кейс с битой строкой (`"not json"`, `"{}"` без обязательного `text`/`uri`) → exception path. Validation pathway проверить явно.
- `LexemeDbEntityMapperTest` — проверка интеграции Multi-level @Relation + маппинга.
- `ComponentTypeRefJsonTest` — round-trip сериализация/десериализация `List<ComponentTypeRef>`:
  - Пустой список (`[]`) → пустой JSON array → пустой список.
  - Только `BuiltIn(TRANSLATION)` → `[{"type":"builtin","key":"translation"}]` → обратно.
  - `[BuiltIn(TRANSLATION), UserDefined("Definition")]` → round-trip с сохранением порядка.
  - Несколько UserDefined со спецсимволами в name (кавычки, эмодзи) — корректно эскейпятся через `JSONObject`.
  - **Invalid JSON** (битая строка / неизвестный discriminator / unknown BuiltInComponent.key) → exception path.
- `QuizConfigMapperTest` — проверка маппинга `QuizConfigDb ↔ QuizConfigApiEntity ↔ QuizConfig`:
  - `QuizConfigDb` с пустым `component_refs = "[]"` → `QuizConfigApiEntity.componentRefs.isEmpty()`.
  - `QuizConfigDb` с default-config (только translation) → парсится в `[BuiltIn(TRANSLATION)]`.
  - `QuizConfigDb` с config из миграции (`[BuiltIn(TRANSLATION), UserDefined("Definition")]`) → парсится корректно, порядок сохранён.
  - Round-trip `QuizConfig.toApiEntity().toDb().toApiEntity().toDomain()` — данные не теряются.

В `modules/domain/lexeme/src/test/`:

- `ComponentTypeMapperTest` — Api ↔ Domain.
- `ComponentValueMapperTest` — то же.
- `LexemeMapperTest` — обновлённый, с `components`.
- `modules/domain/lexeme/src/test/java/me/apomazkin/lexeme/LexemeBuiltInExtTest.kt` — 6-8 кейсов на extension `Lexeme.builtIn(...)` и computed `Lexeme.translation` / `Lexeme.definition`:
  - `Lexeme.builtIn(BuiltInComponent.TRANSLATION)` возвращает correct `ComponentValue` когда built-in translation present.
  - `Lexeme.builtIn(BuiltInComponent.TRANSLATION)` возвращает `null` когда отсутствует.
  - `Lexeme.translation: String?` возвращает text из `ComponentValueData.TextValue` когда built-in translation present.
  - `Lexeme.translation: String?` возвращает `null` когда built-in не найден.
  - `Lexeme.definition: String?` возвращает text когда в lexeme есть user-defined компонент `name="Definition"`; `null` когда отсутствует (AGG-1: definition теперь user-defined, не built-in).
  - Multiple components (built-in + user-defined в одной лексеме) — `builtIn(...)` игнорирует user-defined, возвращает только built-in match.
  - Empty `components` → `builtIn(...)` возвращает `null`, computed `translation` / `definition` тоже `null`.

### Дополнительные кейсы (test gaps batch)

- **Orphan lexeme после миграции.** Lexeme без translation и без definition в v11 → после миграции `lexeme.components.isEmpty()`, `lexeme.translation == null`, `lexeme.definition == null`. Маппер `LexemeApiEntity.toDomain()` не падает на пустом списке, shim-поля остаются `null`.
- **Rollback атомарности `addLexemeWithBuiltInComponent`.** FK-violation (несуществующий `componentTypeId`) при вызове generic метода → assert: новых строк ни в `lexemes`, ни в `write_quiz`, ни в `component_values` не появилось (rollback всей транзакции). Этот кейс упомянут в чеклисте `05.md` § «Атомарность» — здесь дублируется ссылкой как mapper-side integration test.
- **`LexemeApiEntity.toDomain()` с components — явные кейсы:**
  - translation-only (один component с `systemKey = TRANSLATION`) → `lexeme.translation?.value == "..."`, `lexeme.definition == null`.
  - translation + user-defined (один built-in + один user-defined) → `lexeme.translation?.value == "..."`, shim не зависит от user-defined.
  - orphan (`components.isEmpty()`) → оба shim-поля `null`.
  - несколько user-defined без built-in → оба shim-поля `null`, `components.size > 0`.
- **Shim consistency invariant.** Параметризованный тест: для каждой комбинации components (translation-only / translation+definition / user-defined-only / empty / built-in+user-defined) после `LexemeApiEntity.toDomain()` инвариант `lex.translation?.value == lex.components.firstOrNull { it.type.systemKey == BuiltInComponent.TRANSLATION }?.data?.let { (it as? TextValue)?.text }`. Аналогично для `definition`. Ловит регрессии маппера до prod (рассинхрон shim-полей с `components`).
