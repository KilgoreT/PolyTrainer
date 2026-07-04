# Built-in типы: стратегия разделения

Проработка фундаментального вопроса — **как разделять built-in и user-defined компоненты в данных**. Возник в Architecture finding #3 [`03_database_design_review.md`](03_database_design_review.md): nullable `dictionary_id` как маркер scope замазывал архитектуру.

Решение принято: built-in — **полноценный компонент** в единой таблице `component_types`. Семантика разделения через **две независимые ортогональные оси** (см. ниже).

---

## ✅ Чеклист принятых решений (для верификации переноса)

Сводный список ключевых решений из этого документа. При переносе в `03_database_design.md` / `02_design_sketch.md` / реализацию — пройти по чеклисту и убедиться что каждое решение отражено.

### Схема таблицы `component_types`

- [ ] Поле `system_key: String?` UNIQUE — маркер built-in (`"translation"`). NULL = user-defined. **`"definition"` НЕ built-in** (AGG-1: definition мигрирует в user-defined per-dictionary).
- [ ] Поле `name: String?` — литерал (всегда литерал, никогда ключ ресурса). Опциональный override для built-in.
- [ ] CHECK constraint: `name IS NOT NULL OR system_key IS NOT NULL`.
- [ ] Поле `dictionary_id: Long?` FK → `dictionaries.id` ON DELETE CASCADE — где применим (NULL = global).
- [ ] Поле `template_key: String` — ключ шаблона (`"text"` / `"long_text"` / `"image"`).
- [ ] Поле `position: Int` — порядок отображения. Built-in идут раньше user-defined через `ORDER BY (system_key IS NULL) ASC, position ASC`.
- [ ] Поле `remove_date: Date?` — soft-delete (только для user-defined; built-in защищены отдельно).
- [ ] **Удалить** поле `is_system` (если оставалось в 03) — заменено на `system_key`.

### Индексы / constraints

- [ ] `@Index(unique=true)` на `system_key` — защита от дублей built-in (Room генерирует).
- [ ] `@Index(unique=true)` на `(dictionary_id, name)` — защита от дублей user-defined per-dictionary.
- [ ] **Partial UNIQUE index** для 4-й комбинации (user-defined global) — вручную в migration через `connection.execSQL` (extension `androidx.sqlite.execSQL`): `CREATE UNIQUE INDEX index_component_types_global_userdef_name ON component_types(name) WHERE dictionary_id IS NULL AND system_key IS NULL`.
- [ ] `INDEX(dictionary_id)` — FK.
- [ ] **Удалить** избыточный `Index("lexeme_id")` из `ComponentValueDb` (покрывается композитным UNIQUE).
- [ ] **Колонка `value`** (не `payload`) в `component_values` — конвенция проекта (`WordDb.value`, `SampleDb.value`, `HintDb.value`). `@ColumnInfo(name = "value")`.
- [ ] **Sealed `ComponentValueData`**: варианты `TextValue` / `LongTextValue` / `ImageValue` (с суффиксом `Value`), не `Text` / `LongText` / `Image` — избегаем конфликта с `androidx.compose.material3.Text`. Поля внутри остаются `text: String` / `uri: String`.
- [ ] **Multi-level @Relation** для чтения Lexeme + components + types за batched 3 SELECT (избегаем N+1). Промежуточный `ComponentValueWithType` (`@Embedded` `ComponentValueDb` + `@Relation` на `ComponentTypeDb`). `LexemeDbEntity` ссылается через `entity = ComponentValueDb::class`. Паттерн проверен в проекте (`TermDbEntity` → `LexemeDbEntity` → `SampleDb`).
- [ ] **Naming-конвенция (R-N-011)**: domain enum `BuiltInComponent` (без суффикса `ApiEntity`). API и domain reuse один и тот же enum из `modules/domain/lexeme`, не дублируют. Аналогично `ComponentTemplate`. Data class сохраняют суффиксы: `ComponentTypeApiEntity` (API), `ComponentType` (domain).

### Seed и миграция

- [ ] Общая функция `seedBuiltIns(connection: SQLiteConnection)` с `INSERT OR IGNORE` — один источник истины для INSERT built-in. Использует `connection.execSQL(...)` через extension `androidx.sqlite.execSQL`.
- [ ] `RoomDatabase.Callback.onCreate(connection)` (НЕ legacy `onCreate(db: SupportSQLiteDatabase)`) вызывает `seedBuiltIns(connection)` + создаёт partial UNIQUE index (для fresh install).
- [ ] `Migration v11→v12` вызывает `seedBuiltIns(connection)` в конце + создаёт partial UNIQUE index (для upgrade).
- [ ] При INSERT built-in в migration / seed: `name = NULL` (default из ресурса), не литерал.
- [ ] Миграция мигрирует существующие `lexemes.translation` / `.definition` в `component_values` со ссылкой на built-in через `system_key`, не по id.

### Защита built-in от модификаций

- [ ] **Immutable system_key:** DAO нет метода для UPDATE system_key + UseCase валидация в общем `update(type)` (`old.systemKey == new.systemKey`) + комментарий в Entity.
- [ ] **Not deletable:** DAO `softDelete` имеет SQL-фильтр `WHERE id = :id AND system_key IS NULL` + UseCase явная проверка.

### Слойность

- [ ] **Room Entity** (`core-db-impl/.../ComponentTypeDb.kt`): `systemKey: String?`, `templateKey: String` — нативные SQLite типы. **Никаких TypeConverter'ов.**
- [ ] **Enum `BuiltInComponent`** в `modules/domain/lexeme/.../` — domain concept, только `key: String`, без `@StringRes` / Android-зависимостей. `fromKey(key): BuiltInComponent?` — nullable.
- [ ] **Enum `ComponentTemplate`** в `modules/domain/lexeme/.../` — domain concept, `fromKey(key): ComponentTemplate` — **non-null с дефолтом TEXT** внутри (forward-compat).
- [ ] **Sealed `ComponentValueData`** в `modules/domain/lexeme/.../` — domain shape (`TextValue` / `LongTextValue` / `ImageValue`). JSON helper `ComponentValueDataJson.kt` (с `org.json.JSONObject`) — в `core-db-impl` (Android library, доступен `JSONObject`).
- [ ] **`core-db-api` зависит от `modules/domain/lexeme`** (новая Gradle dep edge). `ComponentValueApiEntity.data: ComponentValueData` — поле из domain (пример «data знает domain»).
- [ ] **API Entity** (`ComponentTypeApiEntity`): `id: Long` (без value class — конвенция API), `systemKey: BuiltInComponent?` (enum из domain), `template: ComponentTemplate` (enum из domain).
- [ ] **Маппер** `ComponentTypeDb.toApiEntity()` в `core-db-impl` — конвертация String → enum.
- [ ] **UI extension** `BuiltInComponent.nameRes(): Int` в feature/UI-модуле (`modules/screen/wordcard/widget/internal/`), с `R.string.*`. **Не в core-db-api / не в domain.**

### Чтение в коде

- [ ] Extension `Lexeme.builtIn(key: BuiltInComponent): ComponentValue?` — один источник lookup для built-in. Type-safe через enum.
- [ ] Computed `Lexeme.translation` / `Lexeme.definition` через extension, с TODO «compatibility shim — удалить после миграции quiz/search/DictionaryTab на `components`».
- [ ] Display name: `type.name ?: type.systemKey?.let { resources.getString(it.nameRes()) } ?: error(...)`.
- [ ] **Запрет магических строк** — везде `BuiltInComponent.TRANSLATION` вместо `"translation"`.

### Open questions (не решены здесь)

- [ ] Куда вписать enum `BuiltInComponent` физически внутри `modules/domain/lexeme` — корневой пакет рядом с `Lexeme.kt` или отдельный sub-пакет (на этапе реализации).
- [ ] Где живёт `seedCallback` (RoomModule? Database companion? Initializer?) — на этапе реализации.
- [ ] Retention period для cleanup soft-deleted user-defined — TBD при появлении UI удаления.
- [ ] Расширение scope-объявления (user-defined global / per-tag / иерархия) — TBD при появлении user-defined UI.

---

## Контекст

В фиче IS481 появляются:
- **Built-in типы** — только `translation` (в будущем — example / note). Известны коду заранее. Имеют жёсткую семантику (имя — ресурс, шаблон по умолчанию определённый, нельзя удалить, нельзя переименовать). **`definition` НЕ built-in** — мигрирует в user-defined per-dictionary тип (AGG-1).
- **User-defined типы** — создаются пользователем (на старте — UI нет, но схема готова). Произвольное имя, выбранный шаблон, привязка к словарю, можно удалить. После миграции существующие словари с definition data получают user-defined тип `name="Definition"`.

Вопрос: **как унифицировать оба класса в одной модели без хардкода id и без замазывания scope через nullable**?

---

## Как сейчас (до фичи)

В текущей схеме БД (v11) **нет понятия «тип компонента»** вообще. Translation и definition — это **прямые колонки** таблицы `lexemes`:

- `lexemes.translation: String?` — хранит текст перевода (NULL если отсутствует).
- `lexemes.definition: String?` — хранит текст определения (NULL если отсутствует).

Других «компонентов» лексемы в БД нет. Если бы потребовалось добавить, например, `example` или `note` — пришлось бы добавить новые колонки в `lexemes` и сопровождающую логику в код (State / Msg / Effect / UseCase / UI знают про каждый тип отдельно).

User-defined типов нет ни в коде, ни в БД — пользователь видит только то, что заложено хардкодом.

То есть «компонент» как абстракция — это **исключительно то, что фича IS481 вводит**. До неё компоненты = два жёстко знаемых атрибута лексемы.

---

## Принятое решение

Built-in типы хранятся как **полноценные строки** в таблице `component_types` — наравне с user-defined. Различение по **двум независимым ортогональным осям**:

### Ось 1 — `system_key: String?` (UNIQUE)

Отвечает на вопрос «**это built-in или user-defined**».
- `system_key != null` (например `"translation"` / `"definition"`) → built-in. Имя резолвится из ресурсов по этому ключу. Защищён от удаления и переименования. Domain-код находит built-in через `dao.getBySystemKey(key)`.
- `system_key = null` → user-defined. Имя — литеральная строка из поля `name`.

`UNIQUE(system_key)` защищает **только built-in** от дублей по ключу: две строки с `system_key="translation"` не пройдут.

⚠ **Важно:** SQLite разрешает множественные NULL в UNIQUE-индексе — значит user-defined строк (где `system_key=NULL`) может быть сколько угодно. Это **по дизайну** (NULL = «не built-in»). Защита от дублей user-defined — отдельные индексы: `UNIQUE(dictionary_id, name)` для per-dictionary + partial index для 4-й комбинации (см. раздел «Индексы / constraints» ниже).

### Ось 2 — `dictionary_id: Long?` (FK на `dictionaries.id`)

Отвечает на вопрос «**где применим**».
- `dictionary_id = null` → global, применим во всех словарях.
- `dictionary_id = N` → применим только в словаре N.

### Четыре комбинации

Оси независимы — потенциально допустимы все 4 комбинации:

| `system_key` | `dictionary_id` | Кейс | Релевантность сейчас |
|---|---|---|---|
| not null | null | built-in global (translation, definition) | основной кейс built-in |
| not null | not null | built-in для конкретного словаря | странный, но допустимый |
| null | null | user-defined global (для всех словарей) | future — когда появится концепция учётки |
| null | not null | user-defined per-dictionary | основной кейс user-defined |

Нет CHECK constraint типа «обязательно одно из NULL» — каждая ось имеет независимый смысл, NULL в обеих обозначает легитимные кейсы.

### Принципиальное отличие от ранних вариантов

Это **не** возврат к «nullable dictionary_id как замаскированному scope-флагу». Здесь:
- `dictionary_id` несёт **одну** семантику — «где применим». NULL = «везде». Это нормальная семантика nullable FK.
- `system_key` несёт **одну** семантику — «системный это тип или пользовательский». NULL = «пользовательский».
- Они не дублируют и не противоречат друг другу.

---

## Слойность: где живёт enum, где маппер, где display name

`system_key` — это строка в БД, enum в коде, локализованная строка в UI. Каждый слой делает свою работу.

### Где что лежит

**Принципиально:** `BuiltInComponent`, `ComponentTemplate`, `ComponentValueData` — это **domain concepts** (доменное знание о built-in типах / шаблонах содержимого / shape данных). По Clean Architecture / Dependency Rule они живут в `modules/domain/lexeme`. `core-db-api` зависит от `modules/domain/lexeme` (новая Gradle dep edge — данные знают domain, не наоборот).

| Слой | Модуль | Тип `systemKey` |
|---|---|---|
| Room Entity | `core-db-impl/entity/ComponentTypeDb.kt` | `String?` (raw column) |
| API DTO | `core-db-api/entity/ComponentTypeApiEntity.kt` | `BuiltInComponent?` (enum из domain) |
| Domain | `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentType.kt` | `BuiltInComponent?` (тот же enum) |
| UI | `modules/screen/wordcard/widget/*` | резолвится в `String` через `resources.getString(systemKey.nameRes())` |

### Room Entity в core-db-impl

```kotlin
// core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/ComponentTypeDb.kt

@Entity(
    tableName = "component_types",
    foreignKeys = [
        ForeignKey(
            entity = DictionaryDb::class,
            parentColumns = ["id"],
            childColumns = ["dictionary_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("dictionary_id"),
        Index(value = ["system_key"], unique = true),
    ],
)
data class ComponentTypeDb(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "system_key") val systemKey: String?,    // raw string, не enum
    @ColumnInfo(name = "dictionary_id") val dictionaryId: Long?,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "template_key") val templateKey: String, // raw string, не enum
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "remove_date") val removeDate: Date? = null,
)
```

Никаких `TypeConverter`'ов — нативные SQLite типы (`TEXT`, `INTEGER`). Enum-конверсия — на следующем слое в маппере `toApiEntity()`.

**Примеры строк** (одна таблица для built-in и user-defined):

```kotlin
// built-in без override (default name из ресурса)
ComponentTypeDb(
    id = 1,
    systemKey = "translation",
    dictionaryId = null,           // global
    name = null,                   // → display через R.string.component_translation_name
    templateKey = "text",
    position = 0,
)

// built-in с user-override (например румын переименовал)
ComponentTypeDb(
    id = 2,
    systemKey = "translation",
    dictionaryId = null,
    name = "Traducere",            // → display "Traducere" вместо локализованной строки
    templateKey = "text",
    position = 0,
)

// user-defined в словаре 5
ComponentTypeDb(
    id = 10,
    systemKey = null,              // не built-in
    dictionaryId = 5,              // привязан к словарю 5
    name = "Произношение",         // обязательно для user-defined
    templateKey = "text",
    position = 100,
)
```

Та же `ComponentTypeDb` хранит все три. Различия только в значениях полей — структура одна.

### Enum'ы в domain (без Android-зависимостей)

```kotlin
// modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/BuiltInComponent.kt

enum class BuiltInComponent(val key: String) {
    TRANSLATION("translation");
    // AGG-1: DEFINITION удалён, definition теперь user-defined per-dictionary тип.

    companion object {
        fun fromKey(key: String): BuiltInComponent? =
            values().firstOrNull { it.key == key }
    }
}
```

Возвращает nullable: для built-in строка известна, для user-defined в БД будет NULL (маппер вызывает `key?.let(BuiltInComponent::fromKey)`). Unknown ключ из будущей версии БД — отдельный edge case (см. Edge #4 review).

```kotlin
// modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentTemplate.kt

enum class ComponentTemplate(val key: String) {
    TEXT("text"),
    LONG_TEXT("long_text"),
    IMAGE("image");

    companion object {
        /** Unknown key → fallback на TEXT (forward-compat при откате версии БД). */
        fun fromKey(key: String): ComponentTemplate =
            values().firstOrNull { it.key == key } ?: TEXT
    }
}
```

`fromKey` для template — **non-null** с дефолтом TEXT внутри. Маппер пишет просто `template = ComponentTemplate.fromKey(templateKey)` без `?:` снаружи. Один источник истины для fallback'а.

Никаких `R.string.*`, `@StringRes` — ни `core-db-api`, ни `modules/domain/lexeme` не зависят от Android resources / UI.

### Полная API Entity

```kotlin
// core-db-api/src/main/java/me/apomazkin/core_db_api/entity/ComponentTypeApiEntity.kt

data class ComponentTypeApiEntity(
    val id: Long,                           // raw Long в API (value class — только в domain)
    val systemKey: BuiltInComponent?,   // enum, типизирован уже здесь
    val dictionaryId: Long?,
    val name: String?,                      // nullable — см. § «Резолв display name»
    val template: ComponentTemplate,        // тоже enum (как BuiltInComponent, через key)
    val position: Int,
    val removeDate: Date? = null,
)
```

Конвенция проекта: `core-db-api` использует raw `Long` для id (см. `LexemeApiEntity.id: Long`). Value-классы (`@JvmInline value class XxxId(val id: Long)`) живут в **domain слое** (`modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/`), где они дают type-safety при работе с UseCase / Reducer.

### Маппинг enum ↔ string на границе слоёв

**core-db-impl** делает преобразование (никакого Room TypeConverter — раз enum в api, маппинг в коде маппера):

```kotlin
fun ComponentTypeDb.toApiEntity(): ComponentTypeApiEntity = ComponentTypeApiEntity(
    id = id,
    systemKey = systemKey?.let(BuiltInComponent::fromKey),
    dictionaryId = dictionaryId,
    name = name,
    template = ComponentTemplate.fromKey(templateKey),
    position = position,
    removeDate = removeDate,
)
```

В Room Entity `systemKey: String?` хранится как есть — никакого `@TypeConverter` не нужно.

### UI-резолв display name (отдельно от enum)

```kotlin
// modules/screen/wordcard/widget/internal/ComponentTypeUiExt.kt (UI-слой)

fun BuiltInComponent.nameRes(): Int = when (this) {
    BuiltInComponent.TRANSLATION -> R.string.component_translation_name
}

@Composable
fun displayName(type: ComponentType): String =
    type.name
        ?: type.systemKey?.let { stringResource(it.nameRes()) }
        ?: error("Invalid component_type: both name and systemKey are NULL")
```

`R.string.*` живёт **только** в feature/UI модуле. `core-db-api` ничего не знает про ресурсы.

### Пример: одна строка таблицы на всех слоях

В БД лежит:
```
id=1, system_key='translation', dictionary_id=NULL, name=NULL, template='text', position=0
```

Чтение по слоям:

1. **Room** (`core-db-impl`) — `ComponentTypeDb`:
   ```kotlin
   ComponentTypeDb(id=1, systemKey="translation", name=null, templateKey="text", ...)
   ```

2. **API DTO** (`core-db-api`) — `ComponentTypeApiEntity` после `toApiEntity()`:
   ```kotlin
   ComponentTypeApiEntity(
       id = 1L,
       systemKey = BuiltInComponent.TRANSLATION,   // ← string → enum
       name = null,
       template = ComponentTemplate.TEXT,
       ...
   )
   ```

3. **Domain** (`wordcard`): аналогично — enum проброшен дальше.

4. **UI** (Composable):
   ```kotlin
   val label = displayName(type)
   // type.name == null → fallback в enum.nameRes() → stringResource(R.string.component_translation_name)
   // → "Translation" / "Перевод" в зависимости от локали
   ```

### Преимущества разделения

- **`core-db-api` и `modules/domain/lexeme` нейтральны** — нет Android-зависимостей, могут использоваться в тестах / future server-sync.
- **Enum в одном месте** (domain), известен всем потребителям, type-safe. `core-db-api` reuse этот enum, не дублирует.
- **R.string живёт в UI** — единственное место где нужны Android resources.
- **Маппер enum ↔ string на границе DB↔API** — одно место конвертации (`toApiEntity()` в impl).

### Добавить новый built-in (например `example`)

1. В `modules/domain/lexeme/BuiltInComponent.kt`: `EXAMPLE("example")`.
2. В UI-extension `nameRes()`: `EXAMPLE -> R.string.component_example_name`.
3. Строковый ресурс + локализации.
4. Миграция INSERT в `component_types(system_key='example', ...)`.

Domain-код подхватывает автоматически через `fromKey`.

---

## Схема таблицы `component_types`

(словесное описание, SQL — в обновлённом [`03_database_design.md`](03_database_design.md))

- `id` — PK, autoincrement (без хардкода).
- `system_key: String?` UNIQUE — ключ built-in (`"translation"` / `"definition"` и т.п.) или NULL для user-defined. На уровне Room хранится строкой; маппится в enum `BuiltInComponent` в `core-db-api`. См. раздел выше «Слойность: где живёт enum».
- `dictionary_id: Long?` FK → `dictionaries.id`, ON DELETE CASCADE — где применим.
- `name: String?` — литеральное имя (всегда литерал, никогда не ключ ресурса). Семантика по комбинации с `system_key` — см. раздел «Резолв display name» ниже. CHECK constraint: `name IS NOT NULL OR system_key IS NOT NULL` (хотя бы одно).
- `template_key: String` — ключ шаблона (`"text"` / `"long_text"` / `"image"`). На уровне Room — строка; маппится в enum `ComponentTemplate` в `core-db-api` через `fromKey()`. Можно менять миграцией в будущем (например translation: `text` → `long_text`).
- `position: Int` — порядок отображения. Built-in идут раньше user-defined.
- `remove_date: Date?` — soft-delete для user-defined (NULL = активный). Built-in не удаляются — попытка soft-delete блокируется на уровне UseCase.

**Индексы / constraints:**
- `INDEX(dictionary_id)` — FK.
- `UNIQUE(system_key)` через `@Index(unique=true)` — защита от дублей built-in.
- `UNIQUE(dictionary_id, name)` через `@Index(unique=true)` — защита от дублей имён user-defined per-dictionary (комбинация `dictionary_id != NULL`).
- **Partial UNIQUE index для 4-й комбинации** — создаётся вручную в migration через `connection.execSQL` (extension `androidx.sqlite.execSQL`):
  ```sql
  CREATE UNIQUE INDEX index_component_types_global_userdef_name
  ON component_types (name)
  WHERE dictionary_id IS NULL AND system_key IS NULL
  ```
  Покрывает только user-defined global (`system_key=NULL, dictionary_id=NULL`). Без этого SQLite пропустит дубли (NULL collision в обычном UNIQUE). Partial index поддерживается SQLite 3.8+ (minSdk 23 = SQLite 3.8.10, гарантированно). Room его не валидирует (custom-индексы созданные через `connection.execSQL` игнорируются Room validation). Миграционный тест должен явно проверить наличие индекса после миграции.
- Дополнительная валидация на уровне UseCase: при создании user-defined проверять все resolved-имена в этом словаре (built-in default + built-in override + user-defined global + user-defined per-dictionary) на visible-label collision.

---

## Резолв display name

`name` — всегда **литерал для UI** (никогда не ключ ресурса). Для built-in он опциональный: если NULL — fallback на локализованную строку из enum `BuiltInComponent.nameRes`.

### Четыре комбинации `name` × `system_key`

| `system_key` | `name` | Кейс | Что показывать |
|---|---|---|---|
| not null | NULL | built-in без override (стандарт) | локализованная строка из `enum.nameRes` |
| not null | not null | built-in с user-override | литерал из `name` |
| NULL | not null | user-defined | литерал из `name` |
| NULL | NULL | невалидно | — (запрещено CHECK constraint'ом) |

### Зачем user-override для built-in

Пример: румын учит французский, интерфейс приложения на английском или русском. Built-in `translation` отображается как «Translation» / «Перевод» — не комфортно. Пользователь переименовывает в «Traducere» — записывается в `name`. С этого момента UI показывает «Traducere» для этого built-in в этой БД.

UI «переименовать built-in» появится позже (out of scope первой итерации). На старте все built-in идут с `name = NULL` → display из ресурсов.

### Чтение в коде

```kotlin
val displayName: String = type.name
    ?: type.systemKey?.let { resources.getString(it.nameRes) }
    ?: error("Invalid component_type: both name and system_key are NULL")
```

CHECK constraint в БД гарантирует что error-ветка недостижима — но defensive `error()` страхует от багов миграции.

### Преимущества схемы

- **Одна семантика на колонку.** `name` всегда литерал. `system_key` всегда enum-ключ. Не нужно смотреть на другую колонку чтобы понять как трактовать.
- **Override бесплатный.** Изначально не реализуем UI — но схема готова, добавится без миграций.
- **Не ломается локализация.** Пока `name = NULL` — пользователь видит локализованный built-in на текущей локали. Override — осознанное действие пользователя.

---

## Чтение в коде

**Единый путь — все компоненты одинаковы.**

```
Lexeme.components: List<ComponentValue>
  // содержит и built-in (translation, definition), и user-defined в одной коллекции
  // упорядочены по ComponentType.position (built-in — первыми)
```

`ComponentValue` ссылается на `ComponentType` по `component_type_id` (один FK, без двух альтернативных колонок). При отображении:
- Если `type.systemKey != null` → имя резолвится из ресурсов (`resources.getString(systemKeyToResId(type.systemKey))`).
- Если `type.systemKey == null` → имя из `type.name` как литерал.

Поиск / DictionaryTab переписываются на чтение через `Lexeme.components` (с фильтром по `systemKey` или конкретному ключу). **Quiz** — отдельный путь: читает через `QuizConfig.componentRefs` + lookup `ComponentTypeRef` против `lexeme.components` (см. `07_quiz_strategy.md`). Не использует `BuiltInComponent.TRANSLATION` напрямую как enum-константу в `firstOrNull { systemKey == TRANSLATION }`.

**Extension для built-in lookup** — один источник истины, type-safe через enum:

```kotlin
fun Lexeme.builtIn(key: BuiltInComponent): ComponentValue? =
    components.firstOrNull { it.type.systemKey == key }
```

`O(N)` где `N` — число компонентов лексемы (типично 2-5). При необходимости оптимизации (профайлинг покажет bottleneck) — реверсивно меняется внутри extension без правки API.

**Computed properties для backward-compat** (временные shim'ы):

**Shim'ы для translation / definition НЕ являются computed extensions** (B4/C2 решение). Это полноценные поля в `Lexeme` data class, заполняются маппером `LexemeApiEntity.toDomain()`:

```kotlin
// modules/domain/lexeme/Lexeme.kt — shim поля в data class
data class Lexeme(
    val lexemeId: LexemeId,
    val components: List<ComponentValue>,
    @Deprecated("Use components") val translation: Translation? = null,  // shim
    @Deprecated("Use components") val definition: Definition? = null,    // shim
    ...
)

// Маппер заполняет shim:
fun LexemeApiEntity.toDomain(): Lexeme = Lexeme(
    ...,
    components = components.map { it.toDomain() },
    translation = components.firstOrNull { it.type.systemKey == BuiltInComponent.TRANSLATION }
        ?.data?.let { (it as? ComponentValueData.TextValue)?.text }?.let { Translation(it) },
    // AGG-1: definition shim заполняется user-defined lookup, НЕ built-in:
    definition = components.firstOrNull { it.type.systemKey == null && it.type.name == "Definition" }
        ?.data?.let { (it as? ComponentValueData.TextValue)?.text }?.let { Definition(it) },
)
```

Mate / wordcard reducer / dictionaryTab / quiz UseCase'ы продолжают читать `lexeme.translation?.value` / `lexeme.definition?.value` без изменений.

Никаких магических строк — только enum. Любая опечатка / переименование ключа `BuiltInComponent` — пойманы компилятором.

---

## Миграция данных и seed built-in

Built-in типы попадают в БД двумя путями в зависимости от сценария установки:

### A. Upgrade с v11 → v12 (через Migration_11_12)

**Явный порядок шагов** (AGG-9 — критично для FK consistency):

1. CREATE TABLE `component_types` (FK на dictionaries).
2. CREATE TABLE `component_values` (FK на lexemes + component_types).
3. CREATE TABLE `quiz_configs` (FK на dictionaries, см. `07.md`).
4. **Вызов общей seed-функции** `seedBuiltIns(connection)` — INSERT built-in `translation` строки в `component_types` (`system_key='translation'`, `name=NULL` — display из ресурса). **FK готов после шага 1.**
5. Для каждого dictionary где есть `lexemes.definition IS NOT NULL`: INSERT user-defined тип `(system_key=NULL, dictionary_id=X, name='Definition', template_key='text', position=...)` в `component_types`. **FK готов после шага 1.**
6. INSERT в `component_values` translation data из `lexemes.translation` со ссылкой на built-in (lookup по `system_key='translation'`). **FK готов после шагов 1+4.**
7. INSERT в `component_values` definition data из `lexemes.definition` со ссылкой на user-defined Definition тип в соответствующем словаре. **FK готов после шагов 1+5.**
8. INSERT в `quiz_configs` row для каждого dictionary `(dictionary_id, 'write', component_refs=…)` — `component_refs` JSON содержит `[{"type":"builtin","key":"translation"}]` + опционально `{"type":"user","name":"Definition"}` если словарь имел definition data (AGG-4 реверс).
9. Удалить колонки `translation` / `definition` из `lexemes` (через `ALTER TABLE DROP COLUMN` — bundled SQLite 3.45+, см. § 03_database_design.md).
10. Создать partial UNIQUE index для user-defined global (см. § «Индексы / constraints»).

**Критично:** шаги 4 и 5 (создание типов) ДОЛЖНЫ быть до шагов 6 и 7 (INSERT в `component_values`), иначе FK violation на `component_type_id`.

### B. Fresh install v12 (через Callback.onCreate)

Room вызывает `Migration` **только при upgrade**. На чистой установке миграция не срабатывает, built-in остались бы пустыми. Решение — `RoomDatabase.Callback.onCreate(connection: SQLiteConnection)`, вызывается **один раз** при первом создании БД:

```kotlin
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val seedCallback = object : RoomDatabase.Callback() {
    // ВАЖНО: под bundled driver переопределяем onCreate(connection: SQLiteConnection),
    // НЕ legacy onCreate(db: SupportSQLiteDatabase) — последний молча игнорируется.
    override fun onCreate(connection: SQLiteConnection) {
        seedBuiltIns(connection)
        // partial UNIQUE index для user-defined global тоже создаём здесь
        connection.execSQL("CREATE UNIQUE INDEX index_component_types_global_userdef_name ON component_types(name) WHERE dictionary_id IS NULL AND system_key IS NULL")
    }
}

Room.databaseBuilder(...)
    .addMigrations(Migration11To12)
    .addCallback(seedCallback)
    .build()
```

### Общая seed-функция

Один источник истины для INSERT built-in:

```kotlin
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

fun seedBuiltIns(connection: SQLiteConnection) {
    connection.execSQL("INSERT OR IGNORE INTO component_types (system_key, dictionary_id, name, template_key, position) VALUES ('translation', NULL, NULL, 'text', 0)")
    // AGG-1: definition built-in удалён, не вставляется. Existing definition данные мигрируют в user-defined тип per-dictionary (шаги 5+7 миграции).
}
```

`INSERT OR IGNORE` — идемпотентно через `UNIQUE(system_key)`. Можно безопасно вызвать дважды (например на test-сценариях / при повторной инициализации).

### Покрытые сценарии

| Сценарий | Что срабатывает |
|---|---|
| Fresh install v12 | `Callback.onCreate` → `seedBuiltIns` ✅ |
| Upgrade v11 → v12 | `Migration` → `seedBuiltIns` + миграция данных ✅ |
| Upgrade v10 → v12 (по цепочке) | Все промежуточные Migration'ы по очереди ✅ |
| Reinstall / clear app data | Fresh install заново ✅ |
| Auto-backup restore (та же версия) | БД восстановлена с built-in, ничего не нужно ✅ |
| Restore из старого backup | Migration отработает ✅ |
| Future v13 с новым built-in | Новый INSERT в `seedBuiltIns` + Migration_12_13 вызывает её — оба пути покрыты ✅ |

После миграции / seed domain-код находит built-in через `dao.getBySystemKey("translation")`, никакого хардкода id.

---

## Эволюция в будущем

- **Добавить новый built-in.** Миграция: INSERT новой строки с `system_key='<key>'`. Плюс sealed-вариант в Kotlin для resource resolution. Без новых колонок в `lexemes`.
- **User-defined global** (когда появится концепция учётки). Просто разрешить INSERT с `system_key=NULL, dictionary_id=NULL`. Никаких изменений схемы.
- **Built-in immutable — превращение в user-defined запрещено.** `system_key` после INSERT не меняется. Если в будущем понадобится — рефакторинг lightweight: добавить новый DAO-метод `convertBuiltInToUserDefined()` + UseCase с атомарной транзакцией (UPDATE system_key + name + dictionary_id + проверка коллизий). Сейчас защита на 3 уровнях:
  1. **DAO** — нет метода для UPDATE `system_key`. Общий `update(type)` существует, но в UseCase проверяется (см. п.2).
  2. **UseCase валидация** — при `update(type)` проверять `old.systemKey == new.systemKey`. Нарушение — `IllegalArgumentException` / sealed `Error.ImmutableSystemKey`.
  3. **Комментарий в Entity** — `// system_key is IMMUTABLE after creation` рядом с полем.

  TRIGGER в БД не делаем (overkill для текущего объёма).

- **Built-in не удаляется (soft-delete заблокирован).** Защита на 2 уровнях:
  1. **DAO** — `softDelete` имеет встроенный фильтр в SQL: `UPDATE component_types SET remove_date = :now WHERE id = :id AND system_key IS NULL`. Built-in (`system_key != NULL`) физически не получает `remove_date` даже при ошибке вызова.
  2. **UseCase** — на уровне выше явная проверка: `if (type.systemKey != null) error("Cannot delete built-in")`. Возвращает понятный error раньше чем дойдёт до DAO.

---

## Открытые вопросы

1. **Куда вписать enum `BuiltInComponent` в коде.** Решено: в `modules/domain/lexeme` рядом с `ComponentTemplate` (это domain concept; `core-db-api` зависит от domain и переиспользует enum). Конкретный пакет внутри domain-модуля — уточнить на этапе реализации.
2. ~~**Локализация имён built-in.**~~ **Решено** — см. § «Резолв display name». Имя резолвится из enum `BuiltInComponent.nameRes` через ResourceManager. Поле `name` для built-in — опциональный user-override (NULL = default из ресурса).
3. **Удаление словаря — каскад на built-in?** Built-in (`dictionary_id IS NULL`) гарантированно **не задеваются** при `DELETE FROM dictionaries`. Это поведение SQLite: FK CASCADE срабатывает только при non-NULL child-FK. CASCADE работает при `PRAGMA foreign_keys = ON` (Room включает по умолчанию; на время `migrate()` отключает — для миграций это OK, в runtime — включено). Покрыть миграционным тестом: «после удаления всех словарей `getBySystemKey("translation")` всё ещё возвращает row».
4. **Soft-delete для built-in.** Built-in не удаляются — UseCase блокирует. Колонка `remove_date` для built-in всегда NULL. CHECK constraint избыточен (защита на уровне UseCase достаточна).
5. **Position для built-in.** Built-in идут первыми (см. F7 решение в FlowBacklog). Конкретные значения position для built-in — захардкодены в миграции (`translation=0, definition=1`), или динамические? Это эволюция-вопрос: если в будущем хочется переставить — нужен механизм. На старте — захардкоженные значения, заменим если понадобится.
