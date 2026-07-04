# Template model — domain / БД / миграция

> **Этот документ описывает target state ПОСЛЕ миграции M13.** Текущее состояние M12 — другое (`ComponentValueData` sealed с `TextValue/LongTextValue/ImageValue`, `ComponentTemplate` enum с `LONG_TEXT` и non-null `fromKey`, нет `Field/Primitive/TemplateValues/fields/is_multi`, soft-delete-колонка называется `remove_date` только в `component_types`). Здесь описано **target state после M13**: новые типы, поля, переименования. Конкретный M12 → M13 переход — в разделе § Миграция M12→M13.

Архитектура шаблона компонента и его данных. Решения зафиксированные в обсуждении.

Type safety и раскладка domain ↔ data ↔ UI — см. отдельный документ [`typed_views.md`](typed_views.md). Этот файл фокусируется на схеме template, миграции и БД.

## Принципы реализации (type safety)

Сквозное требование к фиче — **максимально безопасная работа с типами на всех границах** (БД ↔ domain ↔ UI):

1. **Domain типы строго типизированы.** `ComponentTemplate` — enum, `Primitive` — sealed, `Field.type: PrimitiveType` — enum. Никаких `String` / `Int` / `Any` в API доменных функций. Stringly-typed API запрещены.
2. **Парсинг JSON → domain — fail-soft с логом.** Любое расхождение (unknown template, unknown primitive type, type mismatch со schema, отсутствие ожидаемого field) → не fallback, не crash, а **null/skip + Crashlytics-лог error**. Никакого silent fallback на «пустой Text» — это маскирует баги.
3. **Schema validation на чтении value.** При парсинге `component_values.value` JSON парсер сверяется со `ComponentTemplate.fields` родителя. Поле в JSON но не в schema → лог + skip поля. Поле в schema но не в JSON → лог + null (как «незаполнено»).
4. **UI читает только через mapper.** Composable никогда не работает с raw JSON / String / Map. Всё проходит через DAO → JSON parser (в `core-db-impl`) → typed `TemplateValues` (domain). UI ничего не знает про JSON-формат. См. [`typed_views.md`](typed_views.md).
5. **Mapper unit tests.** На каждый известный template — позитивный тест round-trip (encode → decode). На граничные случаи — unknown template, unknown primitive, type mismatch, отсутствующий field, malformed JSON, escape-символы в строках — отдельные негативные тесты.
6. **Compile-time gates.** `when` по `ComponentTemplate` и `Primitive` — exhaustive (kotlin требует ветви для каждого entry). Добавление нового template / primitive ломает компиляцию до тех пор, пока не обработаны все callsite'ы. Это намеренно — sealed/enum заставляет ревизию.

## Концепт

**Template = schema из именованных полей.** Каждое поле — один примитив. Один template ↔ один Compose composable (1:1), composable хардкодит UI рендеринг (стили, шрифты, layout). UI generic-рендер не используется → field role / display style — НЕ нужны.

User видит UI-предпросмотр template'а при создании компонента и даёт компоненту **своё** имя. Enum-key template'а в БД — implementation detail, юзер его никогда не видит.

## Domain types

### Primitive

```kotlin
sealed interface Primitive {
    data class Text(val value: String) : Primitive
    data class Image(val uri: String) : Primitive
    data class Color(val hex: String) : Primitive
}
```

Только эти три. `LONG_TEXT` упразднён — composable per template сам решает single-line / multiline / textarea (на уровне данных всё `String`). При появлении реальной нужды (например БД-индексация коротких vs длинных) — вернём.

### Field

```kotlin
data class Field(
    val name: String,             // "quote", "source", "value", "background_color"
    val type: PrimitiveType,      // TEXT / IMAGE / COLOR
)
```

`PrimitiveType` — enum зеркало `Primitive` вариантов.

### ComponentTemplate

```kotlin
enum class ComponentTemplate(val key: String) {
    TEXT("text"),                                  // существующий (M12)
    IMAGE("image"),                                // существующий (M12)
    QUOTE_WITH_SOURCE("quote_with_source"),        // пример нового composite
    IMAGE_WITH_CAPTION("image_with_caption"),      // пример нового composite
    // ...
    ;
    
    val fields: List<Field> get() = when (this) {
        TEXT -> listOf(
            Field("value", PrimitiveType.TEXT),
        )
        IMAGE -> listOf(
            Field("value", PrimitiveType.IMAGE),
        )
        QUOTE_WITH_SOURCE -> listOf(
            Field("quote",  PrimitiveType.TEXT),
            Field("source", PrimitiveType.TEXT),
        )
        IMAGE_WITH_CAPTION -> listOf(
            Field("image",   PrimitiveType.IMAGE),
            Field("caption", PrimitiveType.TEXT),
        )
        // ...
    }
}
```

`LONG_TEXT` (существовал в M12) — упраздняется в M13 (см. § Миграция).

**Schema живёт в коде.** Поскольку мы выбрали 1:1 связь template ↔ composable (см. § Концепт), композит для каждого template всё равно пишется в коде — естественно держать там же и его schema (список fields). БД хранит только enum key (`component_types.template = "quote_with_source"`); сама структура полей не дублируется в БД.

### TemplateValues

Значения компонента в domain — sealed-иерархия typed views, по одной data class per template:

```kotlin
sealed interface TemplateValues

data class TextValues(
    val value: Primitive.Text,
) : TemplateValues

data class QuoteWithSourceValues(
    val quote: Primitive.Text,
    val source: Primitive.Text,
) : TemplateValues

data class ImageWithCaptionValues(
    val image: Primitive.Image,
    val caption: Primitive.Text,
) : TemplateValues
// ... по одному data class на каждое entry ComponentTemplate
```

Старая sealed-иерархия `TextValue / LongTextValue / ImageValue` (M12) **упраздняется** в M13. Промежуточный `Map<String, Primitive>` в domain **не существует** — парсер JSON в `core-db-impl` сразу собирает typed view. См. [`typed_views.md`](typed_views.md) для полного дизайна (раскладка по слоям, парсер, UI).

## БД

### `component_values` (уже есть)

Источник: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/ComponentValueDb.kt:42-48`.

Структура (упрощённо):

- `id: Long` — PK.
- `lexeme_id: Long` — FK CASCADE на `lexemes.id`.
- `component_type_id: Long` — FK CASCADE на `component_types.id`.
- `value: String` — JSON значений компонента (формат см. ниже). Парсер в `core-db-impl` читает эту строку + знает schema из `ComponentTemplate.fields` родителя → сразу выдаёт typed `TemplateValues`. Промежуточного Map в коде нет.

### Формат JSON в `value`

Корневой объект — `{"fields": ...}`, где значение `fields` — map «имя поля → примитив». Каждый примитив = объект с дискриминатором `type`:

- `Text`  → `{ "type": "text",  "value": "<строка>" }`
- `Image` → `{ "type": "image", "uri":   "<uri>" }`
- `Color` → `{ "type": "color", "hex":   "<hex>" }`

Пример. Template `title_with_description` со схемой `[Field("title", TEXT), Field("description", TEXT)]`:

```json
{
  "fields": {
    "title":       { "type": "text", "value": "Hello" },
    "description": { "type": "text", "value": "World, this is description" }
  }
}
```

Парсер при чтении row сверяет ключи `fields` с `ComponentTemplate.fields` родителя (`component_types.template`) — отсутствие ожидаемого ключа = пустое значение (см. § Открытые вопросы про required/optional). Выход парсера — typed `TemplateValues`, не Map (см. [`typed_views.md`](typed_views.md)).

Storage — JSON-blob в `value`, parser по `ComponentType.template` родителя. Storage уже выбран в IS481 main, **не меняем**.

**Cardinality (М13):** UNIQUE `(lexeme_id, component_type_id)` из M12 — **снимается**. Вместо этого:

- Новое поле `component_types.is_multi: Boolean` — юзер решает при создании компонента.
- `is_multi = false` — у лексемы допустимо одно значение этого компонента.
- `is_multi = true` — у лексемы может быть N значений этого компонента (несколько rows в `component_values`).
- Порядок multi-значений — по `created_at` ASC (см. § Timestamps). Отдельная `position` колонка пока **не вводится**; добавим если появится UI drag-n-drop reorder.

**Защита cardinality на INSERT (вместо БД-constraint):**

DAO-метод `insertSingleSafe` помечается `@Transaction`. Внутри — три шага атомарно:
1. SELECT — посчитать активные (`removed_at IS NULL`) значения данного `component_type` у лексемы.
2. Если `is_multi = false` и count > 0 → отказ (typed error / exception).
3. INSERT новой row.

Дополнительно UI блокирует save-кнопку пока операция в полёте — снимает риск двойного тапа.

**Решения отвергнуты (и почему):**
- Partial UNIQUE `(lexeme_id, component_type_id) WHERE is_multi=0 AND removed_at IS NULL` — SQLite поддерживает, но Room не видит partial index в schema-export → может «потеряться» при recreate таблицы в следующей миграции. Сюрприз без явного предупреждения.
- Application-level `Mutex` в UseCase — перебор для local single-user app (нет concurrent writers).

Примеры реальных кейсов:
- Чипс «часть речи» — `is_multi=false` (у слова одна часть речи).
- Цитаты — `is_multi=true` (у слова может быть 2-3 цитаты).

### `component_types` (уже есть)

`component_types.template` — string-key enum'а `ComponentTemplate`. Сохраняется как есть; добавляем новые keys (`quote_with_source`, `image_with_caption`, …) без изменения структуры таблицы.

## Миграция M12→M13

Существующие values переписать под composite формат:

**`component_values.value` JSON:**

- `{"type": "text", "text": "foo"}` → `{"fields": {"value": {"type": "text", "value": "foo"}}}`
- `{"type": "long_text", "text": "bar"}` → `{"fields": {"value": {"type": "text", "value": "bar"}}}` (long_text упразднён)
- `{"type": "image", "uri": "u"}` → `{"fields": {"value": {"type": "image", "uri": "u"}}}`

**`component_types.template` key:**

- `text` (M12) → `text` (M13, неизменно; schema = `[Field("value", TEXT)]`).
- `long_text` (M12) → `text` (M13; потеря semantic «long» допустима — UI делается per template).
- `image` (M12) → `image` (M13, неизменно; schema = `[Field("value", IMAGE)]`).

**`component_types`:**

- Добавить колонку `is_multi INTEGER NOT NULL DEFAULT 0 CHECK (is_multi IN (0, 1))` — Boolean с CHECK для defense-in-depth (защита от corrupt data при raw SQL / ADB). Migration: existing rows получают `is_multi=0` (single, соответствует прежнему UNIQUE поведению).
- Добавить timestamp-колонки `created_at` / `updated_at` (см. § Timestamps).
- Переименовать `remove_date` → `removed_at` для consistency с timestamp-convention (имя теперь suffix `_at`, тип INTEGER Unix ms). Затрагивает Kotlin domain (`ComponentType.removeDate` → `removedAt`) + Room `@ColumnInfo` + все callsite'ы.

**`component_values`:**

- Снять UNIQUE `(lexeme_id, component_type_id)` (DROP INDEX в M13).
- Добавить timestamp-колонки (см. § Timestamps): `created_at`, `updated_at`, `removed_at`. Существующим rows: `created_at = updated_at = NOW()` на момент миграции, `removed_at = NULL`.

## Timestamps (convention)

Для всех таблиц с пользовательскими данными — стандартный набор колонок:

- `created_at: INTEGER NOT NULL` — Unix ms. Заполняется при INSERT.
- `updated_at: INTEGER NOT NULL` — Unix ms. Обновляется при каждом UPDATE (через DAO/repository, не БД-trigger).
- `removed_at: INTEGER NULL` — Unix ms. Soft-delete marker (`NULL` = активная запись).

**В M13 — добавить в `component_values`** (раньше их не было).

**В M13 — добавить `created_at` / `updated_at` в `component_types`** + **переименовать `remove_date` → `removed_at`** (раньше отсутствовали; `remove_date` существовал с M12, имя приводится к новой convention).

Side benefit: сортировка multi-values по `created_at` ASC = порядок добавления.

Миграция атомарная: `BEGIN TRANSACTION` → переписать всё `component_values.value` через `json_*` функции SQLite либо собрать новый JSON в Kotlin → COMMIT. Rollback при FK violation.

## UI композит per template

1:1 связь `ComponentTemplate` ↔ Compose composable. Composable принимает typed view (свой `*Values` data class per template):

- `TEXT` → `TextWidget(values: TextValues)` — читает `values.value.value`.
- `QUOTE_WITH_SOURCE` → `QuoteWithSourceWidget(values: QuoteWithSourceValues)` — читает `values.quote.value`, `values.source.value`.
- `IMAGE_WITH_CAPTION` → `ImageWithCaptionWidget(values: ImageWithCaptionValues)` — читает `values.image.uri`, `values.caption.value`.

UI хардкодит layout / стили / шрифты per template. Поля typed view — compile-checked: опечатка `values.qoute` не компилируется (см. [`typed_views.md`](typed_views.md)).

Resolver `ComponentTemplate -> @Composable (TemplateValues) -> Unit` — централизованный mapping в widget-модуле `modules/widget/component_widgets/`. Расположение обусловлено Dependency Rule: domain про Compose не знает (правильно), UI знает про domain (правильно), resolver — UI-side mapper из domain enum в UI composable. Централизация в widget модуле — чтобы не дублировать `when` в каждом screen-потребителе (wordcard, quiz chat, конструктор). Реализация — exhaustive `when` через sealed `TemplateValues`; конкретный API (interface vs top-level function) выбирается на implementation phase.

## Открытые вопросы

1. **Required vs optional fields.** Решено: per-field required/optional пока **не enforce**. Правило валидации — **хотя бы один** примитив должен быть заполнен; всё пустое = в БД не записывается (UI **не блокирует** submit, юзер свободно может закрыть). При следующем открытии лексемы такого компонента просто не будет — данные берутся из БД, в БД ничего нет. Per-field уточнения — смотрим на практике, после первых реальных widget'ов.
2. **Default values per field.** Решено: per-field default в БД не вводим. Пустое поле = `null` в JSON, UI рисует placeholder. Если widget'у нужен «дефолтный цвет / иконка» при пустом значении — это **хардкод в composable** шаблона, не БД-default. Для будущих примитивов (`Number`, `Boolean`, `Date`) пересмотрим отдельно.
3. **Forward-compat unknown template.** Решено: **skip** — если в БД лежит template которого нет в текущем enum, компонент **не рендерится** в UI лексемы. Данные в БД сохраняются (не теряются и не портятся accidentally). При встрече unknown template — **лог в Crashlytics** (или общий logger) с уровнем error: ключ template'а + `component_type_id` + контекст. Это сигнал команде что или появилась нерелизнутая версия в проде, или dev-leak. Когда / если появится sync между устройствами — пересмотреть на «заглушка с CTA Обновите приложение». `ComponentTemplate.fromKey` существующий fallback на TEXT → заменить на nullable return (`fromKey(key: String): ComponentTemplate?`), callsite'ы обрабатывают `null` как skip.
4. **Forward-compat unknown primitive type в JSON.** Решено: **skip per-field + лог** (симметрично пункту 3). Покрывает два случая: (a) primitive type которого нет в текущем sealed (`{"type": "video", ...}`); (b) type mismatch со schema — поле в schema ожидает `TEXT`, JSON содержит `IMAGE` (data corruption). В обоих — соответствующее поле в composable получает `null`, composable рендерит как «отсутствует». Запись в Crashlytics с уровнем error: ключ primitive type'а / расхождение со schema + `component_value_id` + контекст.
5. **Storage эффективность.** Решено: JSON-blob парсинг приемлем для реалистичных объёмов. Активный изучаемый словарь — 1-5k лексем × 2-5 компонентов = 2k-25k `component_values` per словарь. Даже на 10 словарей — десятки тысяч rows, не сотни. Перфоманс JSON-парсинга при таких объёмах не проблема. Пересмотр только если появится сценарий типа полнотекстового поиска по миллионам values (не предвидится).
6. **Список начальных templates для MVP.** Решено: только `TEXT` в MVP конструктора (schema = `[Field("value", TEXT)]`, typed view = `TextValues(value: Primitive.Text)`). UI widget = переиспользуется существующий composable для translation/definition. **Переход с legacy sealed (`TextValue / LongTextValue / ImageValue`) на typed views per template делаем сразу в M13** — архитектура готова к будущим composite templates без дополнительной миграции. Конкретные composite (`quote_with_source`, `image_with_caption`) — отдельные фичи позже.
7. **Timestamps в остальных таблицах** (`lexemes`, `terms`, `dictionaries`, `samples`, `hints` и т.д.). Решено: **отдельная миграция M14**, не в M13. Причины split — `[F-N3]` (см. `Backlog.md`): repository-wide rename `addDate`/`modifiedDate` — это convention refactor, не блокирующий конструктор; смешивать с composite-rewrite в одной миграции = риск destructive fallback из-за пропущенного `@Query` SQL литерала. M14 запускается отдельной фичей после M13.

8. **Soft-delete scope — критерии каких таблиц затрагивает** `[F-N4]`. Закрыто, см. `deletion_concept.md`:
   - **(a) Каким таблицам нужна `removed_at`:** в этой фиче — только `component_types` (уже есть, rename `remove_date` → `removed_at`) и `component_values` (новая колонка). Остальные (`dictionaries` / `words` / `lexemes`) — отдельной фичей позже. `samples` / `hints` отложены до миграции в template'ы. `quiz_configs` / `write_quiz` — hard-delete достаточно навсегда.
   - **(b) DAO convention:** все active-data queries для `component_types` / `component_values` обязаны иметь `WHERE removed_at IS NULL` (либо JOIN с тем же фильтром на родителя). Audit DAO methods на implementation phase.
   - **(c) Cascade при soft-delete родительской сущности:** JOIN-based hiding (без UPDATE values). При `component_type.removed_at != NULL` — связанные `component_values` остаются с `removed_at = NULL`, но active queries фильтруют через JOIN на parent's `removed_at IS NULL`. При hard-delete родителя (если когда-то будет) — FK CASCADE снесёт values автоматически.

9. **Multi → single downgrade — жёсткий запрет при наличии лексем с count > 1** `[F-N5a]`. UseCase при попытке `is_multi: true → false` делает `SELECT MAX(count) per lexeme_id` среди активных values этого component_type. Если `max <= 1` — toggle разрешён. Если `max > 1` — блок. UI показывает: общий count проблемных лексем + preview первых ~3 (clickable, ведут к лексеме); кнопка «Показать все» открывает отдельный экран со списком + поиск. Юзер сам идёт удалять лишние, возвращается, re-check, либо разрешено, либо ещё проблемные. Bulk-actions «удалить лишние везде» **запрещены** — это эквивалент произвольного выбора что удалить (приложение само решает), что небезопасно. Если у юзера 1000 проблемных лексем — фактически downgrade невозможен; рекомендованный путь — создать новый single-компонент.

10. **Template immutable после релиза** `[F-N5b обновлён]`. Различение двух уровней rename:
    - **`ComponentType.name`** (имя компонента которое юзер задал в конструкторе, например «Моя цитата») — это **user-data в БД**, юзер свободно переименовывает в любой момент. Никаких ограничений.
    - **`Field.name`** в `ComponentTemplate.fields` (структурное имя поля schema — `"quote"`, `"source"`) и **состав `fields`** (имена / типы / порядок) — **immutable** после релиза. Какие template'ы завели — такие и остаются. Нужны другие поля / иная структура → создаётся **новый template** (новое entry в enum, новый widget, новый `*Values` data class). Старые template'ы продолжают работать как были. Причина: existing JSON в `component_values.value` зашит на текущую schema; любое изменение = schema-mismatch / silent breakage / Crashlytics шум на все existing rows.

    **Защита механизмом:**
    - **Test fixture round-trip** в `core-db-impl` (`fixtures/component_values/*.json` — golden JSON per known template). Тест: парсит fixture → сравнивает с expected typed view. Любое изменение existing template (rename Field / add field / remove field / type change) → expected `QuoteWithSourceValues(...)`, actual `null` или несовпадение → CI **fail**. Hard block на merge.
    - **Convention в `docs/guides/data-layer.md`** — текстовое правило. При красном CI разраб грепает «golden fixture» → находит правило → понимает что existing template нельзя менять.

11. **Атомарность save composite-лексемы** `[F-N8]`. Repository-метод сохранения лексемы оборачивается в `@Transaction` DAO. Внутри: INSERT row в `lexemes` + N INSERT в `component_values`. Любая ошибка на любом шаге → rollback всей транзакции, в БД ничего не остаётся. Без транзакции — риск partial state (лексема с половиной компонентов). Unit-test на rollback при FK violation.
