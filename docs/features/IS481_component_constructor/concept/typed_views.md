# Typed views — design

**Статус: применено в template_model.md, реализовать на этапе implementation.**

Документ описывает финальный дизайн typed views для значений компонента: чистая type-safe domain-модель без промежуточного `Map<String, Primitive>`. Парсер JSON в data-слое сразу собирает typed view, UI работает только с типизированными data class'ами.

Это решение F-N2 из дизайн-ревью (architect+senior+qa): «type-safety регрессия с `Map<String, Primitive>`» — string-key + runtime `as?` ломают compile-time gates (опечатка `data.fields["qoute"]` пропускается компилятором, schema change даёт silent null). Решено убрать Map из domain вообще.

## Раскладка по слоям

```
UI               ┐
                 ├─→  TemplateValues (sealed)
Composable       ┘     QuoteWithSourceValues / TextValues / ...
                       Compile-time проверка полей и типов

────────────────  domain boundary ────────────────

Domain           ┐
                 ├─→  TemplateValues + Primitive + ComponentTemplate +
                 │     Field + ComponentType
                 │     Никаких Map<String, Primitive> и string-keys.

────────────────  data boundary ──────────────────

Data parser      ┐
(core-db-impl)   ├─→  JsonString + ComponentTemplate → TemplateValues?
                 │     Единственная точка с runtime-check.
                 │     Знает JSON-формат и schema (template.fields).

Storage          ┐
(Room)           ├─→  ComponentValueDb.value : String (JSON, как есть)
                 ┘
```

Map не существует как промежуточный тип в коде; storage держит JSON-строку, парсер сразу собирает typed view.

## Domain (`modules/domain/lexeme/`)

`sealed interface TemplateValues` + конкретные data classes per template:

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
```

Также в domain остаются (как раньше): `Primitive` (sealed), `Field`, `ComponentTemplate` (enum + `fields: List<Field>`), `ComponentType`.

## Data parser (`core/core-db-impl/.../mapper/`)

Парсер читает `ComponentValueDb.value` (JSON-строка) **+ знает schema** из `ComponentTemplate.fields` родителя → выдаёт `TemplateValues?`. Это единственная точка где есть runtime check (отсутствует field / неверный тип → null + лог в Crashlytics).

Pseudocode:

```kotlin
fun parseTemplateValues(json: String, template: ComponentTemplate): TemplateValues? {
    val root = JSONObject(json)
    val fields = root.getJSONObject("fields")
    return when (template) {
        ComponentTemplate.TEXT -> TextValues(
            value = parsePrimitive<Primitive.Text>(fields, "value") ?: return null
        )
        ComponentTemplate.QUOTE_WITH_SOURCE -> QuoteWithSourceValues(
            quote  = parsePrimitive<Primitive.Text>(fields, "quote")  ?: return null,
            source = parsePrimitive<Primitive.Text>(fields, "source") ?: return null,
        )
        ComponentTemplate.IMAGE_WITH_CAPTION -> ImageWithCaptionValues(
            image   = parsePrimitive<Primitive.Image>(fields, "image")   ?: return null,
            caption = parsePrimitive<Primitive.Text>(fields,  "caption") ?: return null,
        )
        // exhaustive — добавление нового template ломает компиляцию.
    }
}
```

Обратный путь (для save): `TemplateValues → JsonString` — extension рядом, симметрично.

## UI composable работает с typed

```kotlin
@Composable
fun QuoteWithSourceWidget(values: QuoteWithSourceValues) {
    Text(values.quote.value)
    Text(values.source.value)
}
```

Composable знает только domain types. Никаких Map / string-key / `as?`.

## Где жить (по модулям)

- **`modules/domain/lexeme/`** (pure JVM, domain semantics):
  - `Primitive` (sealed Text/Image/Color), `Field`, `ComponentTemplate` (enum + schema), `ComponentType` — как раньше.
  - **Новое:** `TemplateValues` (sealed interface) + конкретные data classes (`TextValues`, `QuoteWithSourceValues`, ...).
- **`core/core-db-impl/.../mapper/`** (data-слой, знает storage format):
  - JSON parser `parseTemplateValues(json, template): TemplateValues?` — собирает typed view сразу.
  - JSON serializer обратно `TemplateValues.toJson(): String` — симметрично.
- **`modules/widget/component_widgets/`** (новый widget-модуль, Tier 2):
  - Composable widgets per template (`TextWidget`, `QuoteWithSourceWidget`, ...). Используют domain types.
- **Wrapper `ComponentBlock(name, content)`** — либо в `modules/widget/component_widgets/` (Tier 2), либо в `core/ui/` (Tier 1). Решение на UI-этапе.

## Что улучшается

- **Опечатка → compile error.** `values.qoute` не компилируется.
- **Schema change ломает компиляцию.** Переименовали `quote → quotation` в `ComponentTemplate.fields` + в `QuoteWithSourceValues` — все widgets потребуют правки, компилятор протащит за руку.
- **Type swap ломает компиляцию.** Заменили `Field("quote", TEXT)` на `Field("quote", IMAGE)` — `QuoteWithSourceValues.quote: Primitive.Text` уже не соответствует, mapper не компилируется.
- **`when` exhaustive на `ComponentTemplate`** — добавили новый template, забыли typed view → mapper не компилируется.
- **Map остаётся только в storage / mapper layer.** UI не видит string-keys и `as?`.

## Цена

- На каждый template — один typed `data class *Values` + одна ветка в `parseTemplateValues` mapper.
- Линейный бойлерплейт с количеством templates. Для MVP (только `TEXT`) — один `TextValues` + одна ветка mapper'а.
- Один дополнительный читальный шаг на render (parse JSON → typed). Цена — O(N полей), N≤5 типично. Не проблема.
