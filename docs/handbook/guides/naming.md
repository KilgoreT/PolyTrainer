# Naming — нейминг в проекте

Единый гайд для именования всего: Kotlin-кода (пакеты, файлы, классы, методы), сущностей БД (таблицы, колонки, FK), значений enum в БД, ресурсов и т.д.

Раздел [Rules](#rules--машинно-проверяемые-правила) — машинно-проверяемые правила для прогона ревьюером (формат R-N-NNN).

---

## Принцип

Имена должны быть **лаконичными и понятными**. Короткое имя лучше длинного при равной ясности. Если имя можно сократить без потери смысла — сократи. При ревью всегда проверяй: можно ли переименовать короче?

- `FlagsUpdated` лучше чем `FilteredFlagsLoaded`
- `updateFlags` лучше чем `updateFilteredFlags`
- `flags` лучше чем `filteredFlagsList`

---

## Kotlin

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

См. отдельный гайд: [logging.md](logging.md).

---

## Database

Все правила выведены из существующих Entity проекта (`core/core-db-impl/.../entity/`). Если есть расхождение между правилом и существующим Entity — это **legacy**, выравнивается отдельной задачей.

### Имена таблиц

- Множественное число (`dictionaries`, `words`, `lexemes`, `samples`, `hints`).
- snake_case.
- Если таблица относится к domain-сущности — имя совпадает с сущностью (например, `lexemes` для `LexemeDb`).
- Для составных доменов или namespace-clarity — префикс по родителю (`write_quiz`).

### Имена колонок

- snake_case через `@ColumnInfo(name = "...")` явно.
- PK всегда `id` (`@ColumnInfo(name = "id")`).
- Foreign key: `<parent_singular>_id` — например `dictionary_id`, `lexeme_id`, `word_id`.
- Boolean — без префикса `is_` (`built_in`, не `is_built_in`). Контекст таблицы делает префикс избыточным.
- Дата/время — `<event>_date` (`add_date`, `change_date`, `remove_date`). При soft-delete — single field `remove_date: Date?` (NULL = активный), не отдельный boolean.

### Значения enum в TEXT-колонках

- lowercase snake_case: `"noun"` / `"verb"` / `"adjective"` (как в `word_class`).
- Не UPPER_SNAKE. Не camelCase.
- При sealed-классе в Kotlin: `key: String` свойство, `companion object { fun fromKey(...) }` для парсинга.

### Имена value-полей

- Колонка со «значением» сущности (текст / число / payload) — `value`.
  Пример: `WordDb.value` (text слова), `SampleDb.value` (текст примера), `HintDb.value` (текст подсказки).
- Не `payload` / `data` / `content` — единая конвенция `value`.

### Индексы

- Не дублировать индекс на leading column композитного UNIQUE-индекса — он покрывает запросы с фильтром по этой колонке.
- На FK-колонки обязательно индекс (`Index("dictionary_id")`).

### Типы и TypeConverter

- Sealed-enum → TEXT через TypeConverter (как `DateTimeConverter` для Date). Не строковая колонка с ручным парсингом в маппере.

---

## Сущности по слоям

Конвенция имён в зависимости от слоя где живёт сущность:

| Слой | Расположение | Data class (DTO/Entity) | Enum / sealed |
|---|---|---|---|
| Room Entity (`@Entity`) | `core-db-impl/entity/` | Суффикс `Db`: `WordDb`, `LexemeDb`, `ComponentTypeDb` | Не используются (Room хранит как TEXT/INTEGER) |
| Room комбинированные (`@Embedded` + `@Relation`) | `core-db-impl/entity/` | Суффикс `DbEntity`: `LexemeDbEntity`, `TermDbEntity` | — |
| API DTO (контракт data ↔ feature) | `core-db-api/entity/` | Суффикс `ApiEntity`: `WordApiEntity`, `LexemeApiEntity`, `ComponentTypeApiEntity` | **Без суффикса**: `Grade`, `BuiltInComponent`, `ComponentTemplate` (как plain value type) |
| Feature domain | `modules/screen/<feature>/entity/`, `modules/domain/<name>/` | Без суффикса: `Word`, `Lexeme`, `Term`, `ComponentType` | **Reuse** enum из API (не дублируем) |
| UI (composable / списки / виджеты) | `modules/screen/<feature>/entity/`, `modules/widget/<name>/entity/` | Префикс `Ui` + тип: `*UiEntity` (одиночная обёртка над domain), `*UiItem` (элемент `LazyColumn`/`LazyRow`), `*UiList` (коллекция с UI-метаданными). Новые `Ui<Тип>` (`UiAction`, `UiField` и т.п.) — по смыслу, конвенция не закрытая. | — |
| DataStore Prefs | `modules/datasource/prefs/` | — (key-value) | Суффикс `Key`: `PrefKey` |

**Логика:**
- `data class` (data, api, domain, UI) — у каждого слоя свой, чтобы границы были чёткими. UI не работает с domain напрямую — ViewModel/Reducer маппит `Domain → Ui*`.
- `enum` / `sealed` — общий между API и domain (reuse из API). Enum это plain value type без data-логики; домену нечего добавить.
- **TEA-state** (`State.kt` редьюсера, `LexemeState`, `WordCardState`, `TextValueState`) — **без** префикса `Ui`. Это не UI-data class для composable, а внутренняя структура редьюсера.

---

## Resources (Android)

- Strings: `<screen>_<element>_<state>` — `word_card_bottom_translation`.
- Drawables: `ic_<name>` для иконок (`ic_close`, `ic_confirm`).
- Цвета: токены через `MaterialTheme.colorScheme.*` или `me.apomazkin.theme.*Color`.

---

## Tests

- Имя класса: `*Test.kt`.
- Имя метода теста: backtick-string в свободной форме («что проверяем»), например `` `removes lexeme when confirmed`() ``.
- Для extension-тестов — отдельная папка `ext/`, имя `*ExtTest.kt`.

---

## Rules — машинно-проверяемые правила

Формализованные правила для прогона ревьюером (см. F9-решение в `docs/FlowBacklog.md`). Формат: `R-N-NNN` / Severity / Applies to / Check.

### R-N-001. snake_case для имён таблиц БД

- **Severity:** critical.
- **Applies to:** новые Entity (`@Entity(tableName = "...")`).
- **Check:** `tableName` — snake_case в множественном числе. Не camelCase, не PascalCase, не singular.
- **Пример:** `tableName = "component_types"` ✓, `tableName = "componentTypes"` ✗, `tableName = "component_type"` ✗.

### R-N-002. snake_case для имён колонок БД через @ColumnInfo

- **Severity:** critical.
- **Applies to:** новые поля Room Entity.
- **Check:** Каждое поле в Entity имеет явный `@ColumnInfo(name = "snake_case_name")`. Не полагаться на Kotlin camelCase имя поля (тогда Room возьмёт его как-есть и создаст camelCase-колонку — это legacy bug в `SampleDb` / `HintDb`).

### R-N-003. PK всегда `id`

- **Severity:** critical.
- **Applies to:** новые Entity.
- **Check:** PK-колонка — `@ColumnInfo(name = "id") val id: Long`. Не `<table>_id`, не `pk_id`.

### R-N-004. Foreign key именуется `<parent_singular>_id`

- **Severity:** critical.
- **Applies to:** новые Entity со связями.
- **Check:** FK-колонка — `<parent_table_singular>_id`. `dictionary_id` (parent `dictionaries`), `lexeme_id` (parent `lexemes`), `word_id` (parent `words`).

### R-N-005. Enum-значения в TEXT-колонках — lowercase snake_case

- **Severity:** critical.
- **Applies to:** значения для колонок типа `word_class`, `source`, `template_key` и т.п.
- **Check:** Все литералы — lowercase snake_case (`"noun"` / `"verb"` / `"long_text"`). Не UPPER_SNAKE, не camelCase.

### R-N-006. Колонка значения сущности — `value`

- **Severity:** minor.
- **Applies to:** новые Entity с одной «основной» колонкой данных (текст / число / payload).
- **Check:** Колонка называется `value` (по конвенции `WordDb`, `SampleDb`, `HintDb`). Не `payload` / `data` / `content`.

### R-N-007. Boolean-колонки без `is_` префикса

- **Severity:** minor.
- **Applies to:** новые Boolean-колонки.
- **Check:** `built_in` ✓, `deleted` ✓, `is_built_in` ✗, `is_deleted` ✗. Контекст таблицы делает префикс избыточным.

### R-N-008. Soft-delete — single field `remove_date: Date?`

- **Severity:** minor.
- **Applies to:** новые Entity с soft-delete семантикой.
- **Check:** Одна колонка `remove_date: Date?` (NULL = активный). Не `deleted: Boolean` + `deleted_at: Date?` (избыточно, разрешает невалидное `deleted=true && deleted_at=null`).

### R-N-009. Sealed → TEXT через TypeConverter, не String + ручной парсинг

- **Severity:** minor.
- **Applies to:** новые поля Entity с sealed-типом в коде.
- **Check:** Если поле в Entity маппится из sealed-класса (`ComponentTemplate` / `Source` / ...) — использовать `@TypeConverter` (как `DateTimeConverter` для Date). Не хранить как `String` с ручным парсингом в маппере.
- **Исключение — polymorphic payload.** Если выбор sealed-варианта зависит от контекста другого поля / другой таблицы (например, JSON-payload где формат определяется родительским `templateKey`), TypeConverter технически не подходит — он получает только value-колонку без контекста parent. В таких случаях ручной парсер на уровне маппера допустим (контекст доступен через @Relation / JOIN). Зафиксировать disclaimer в дизайн-документе фичи.

### R-N-010. Имена Kotlin-классов следуют конвенции файлов

- **Severity:** minor.
- **Applies to:** новые классы.
- **Check:** Имя класса совпадает с именем файла (`WordCardScreen.kt` — `class WordCardScreen`). Reducer — `*Reducer`, State — `*State`, Msg — sealed interface `Msg` (без префикса), UseCase — `*UseCase` / `*UseCaseImpl`.

### R-N-011. Суффиксы по слою — Db / DbEntity / ApiEntity / без суффикса

- **Severity:** minor.
- **Applies to:** новые сущности проекта (data class + enum).
- **Check:**
  - Room `@Entity` → суффикс `Db` (`WordDb`).
  - Room `@Embedded` + `@Relation` комбинированные → суффикс `DbEntity` (`LexemeDbEntity`).
  - API DTO data class → суффикс `ApiEntity` (`WordApiEntity`).
  - API enum → без суффикса (`Grade`, `BuiltInComponent`).
  - Domain (feature `modules/screen/.../entity/`, общий `modules/domain/<name>/`) → без суффикса (`Word`, `Lexeme`).
  - Domain enum → reuse из API, не дублировать.
  - UI data class → префикс `Ui` + тип: `*UiEntity` (обёртка одного домен-объекта), `*UiItem` (элемент списка), `*UiList` (коллекция с UI-метаданными). Новые `Ui<Тип>` суффиксы (`UiAction`, `UiField` и т.п.) — по смыслу, конвенция не закрытая.
  - TEA-state (в `State.kt` редьюсера) → без `Ui` префикса (`LexemeState`, `WordCardState`). TEA-state — внутренняя структура редьюсера, не UI-data.
- См. § «Сущности по слоям» выше.

### R-N-012. Варианты sealed/enum связанных с БД — только добавлять, никогда не удалять

- **Severity:** critical.
- **Applies to:** sealed-классы и enum, чьи варианты сериализуются в БД как строковый ключ (`ComponentTemplate`, `BuiltInComponent`, `Source`, `Grade` и т.п.).
- **Check:** при изменении такого sealed/enum — варианты можно только **добавлять**. Удаление варианта = ломает всех существующих пользователей у которых в БД есть rows с этим key (парсер падает / fallback приводит к багу). Если нужно «убрать» вариант — пометить `@Deprecated` + миграция данных (UPDATE rows на другой key), но не удалять сам enum-вариант пока есть risk что rows остались.
- **Зачем:** sealed-вариант существует не только в коде, но и как контракт хранимых данных. Удаление = breaking change для БД.

### R-N-013. Имена короче лучше длиннее при равной ясности

- **Severity:** minor.
- **Applies to:** любое именование (классы / методы / переменные / параметры).
- **Check:** Перед фиксацией имени — задай себе вопрос «можно ли сократить без потери смысла?». `flags` лучше `filteredFlagsList`, `updateFlags` лучше `updateFilteredFlags`.
