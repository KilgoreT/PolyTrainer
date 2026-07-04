# Review: 06_mapping_design.md

Параллельный обзор: 2 subagent'а — Architecture & Correctness / Performance & Edge cases. Каждый finding — `Verify:` через встроенные Claude tools.

## Сводка

| Направление | Critical | Minor |
|---|---|---|
| Architecture & Correctness | 5 | 4 |
| Performance & Edge cases | 2 | 5 |
| **Итого** | **7** | **9** |

**Главные сигналы:**

1. **Naming рассинхрон** — `BuiltInComponentType` vs `BuiltInComponent` (конвенция R-N-011 принята, но не применена в 06 и в 04). Также `ComponentValueData.Text` остался в § «Чтение в коде» `04`.
2. **kotlinx.serialization не подключён в проекте** — нет зависимости, плагина, proguard rules. Реализация не соберётся.
3. **Стиль мапперов** — в проекте extension в файле Entity (см. `WordDb.kt:32-40`). Документ предлагает отдельные `*Mapper.kt` — расходится со стилем.
4. **R-N-009 нарушение vs обоснование** — sealed → String с ручным парсингом. Нужно либо обоснование (TypeConverter не имеет доступа к `template`), либо обновление правила.
5. **Edge: unknown templateKey → fallback TEXT крашит** парсинг image-payload как TextValue → весь list-запрос падает.
6. **LexemeApiEntity.toDomain несовместим** с текущим Lexeme domain (поля translation/definition не удалены явно).
7. **Производительность JSON** — 3000 объектов × 5-20 мкс = 15-60ms. Близко к одному кадру. Нужно `flowOn(Dispatchers.Default)`.

---

## Architecture & Correctness Review

### 🔒 [critical] Enum naming mismatch — `BuiltInComponentType` vs `BuiltInComponent`
**Triage:** → закрыто. Массовый replace `BuiltInComponentType` → `BuiltInComponent` сделан в `02`, `04`, `05`, `06`. Дополнительно `ComponentValueData.Text` → `ComponentValueData.TextValue` (см. A&C #2). Review-документы оставлены с историческими именами как контекст findings.

### [critical] Sealed `ComponentValueData.Text` всё ещё в `04` § «Чтение в коде»
**Где:** `04` строки 464, 467 — `(builtIn(BuiltInComponentType.TRANSLATION)?.data as? ComponentValueData.Text)?.text`.
**Что не так:** Чеклист `04` принял `TextValue/LongTextValue/ImageValue`, но в compatibility shim секции остался `ComponentValueData.Text`. Будущий compile-error.
**Предложение:** В `04` строки 464, 467 заменить на `ComponentValueData.TextValue`.

### 🔒 [critical] `LexemeApiEntity.toDomain` несовместим с текущим Domain `Lexeme`
**Triage:** → закрыто. В `06` добавлен раздел «Изменения Domain `Lexeme`»: показано что value-классы `Translation` / `Definition` удаляются, поля заменяются на `components`, computed accessors `builtIn() / translation / definition` живут как extension в отдельном файле `LexemeBuiltInExt.kt`.

### 🔒 [critical] R-N-009 нарушен — sealed хранится как String с ручным парсингом
**Triage:** → закрыто. (1) В `naming.md` R-N-009 добавлено **исключение** для polymorphic payload (где выбор sealed зависит от контекста parent). (2) В `06_mapping_design.md` § «ComponentValueData» добавлен disclaimer со ссылкой на правило. Ручной парсер в маппере остаётся (TypeConverter технически невозможен — нет доступа к parent.template).

### 🔒 [critical] Стиль мапперов — extension в файле Entity, не отдельный `*Mapper.kt`
**Triage:** → закрыто. Пути в `06` обновлены: extension рядом с data class (`ComponentTypeDb.kt`, `ComponentValueDb.kt`, `ComponentValueWithType.kt`; domain — `ComponentType.kt`, `ComponentValue.kt`). JSON helper остаётся отдельным файлом `ComponentValueDataJson.kt` (изоляция kotlinx.serialization). Стиль соответствует существующим Entity проекта.

### ❌ [minor] Edge case orphan — `Multi-level @Relation вернёт пустой type` некорректно
**Triage:** → rejected. Orphan невозможен — FK ON DELETE CASCADE. Теоретический сценарий.

### 🔒 [minor] Multi-level @Relation — пояснить механику batched JOIN
**Triage:** → закрыто. В `06` § «ComponentValueWithType.toApiEntity()» добавлено пояснение про 3 batched SELECT'a с `WHERE ... IN (...)`.

### 🔒 [minor] `@SerialName` избыточен в variants
**Triage:** → закрыто. Убран `@SerialName` из всех 3 variants в `06`. Чеклист обновлён.

### 🔒 [minor] Эволюционная устойчивость формата
**Triage:** → закрыто. В `06` после блока сериализации добавлено правило эволюции: новые поля variants обязаны иметь default value, иначе старые записи падают при парсинге.

### [minor] Обратные мапперы неполные
**Где:** 06 строки 31-35 (чеклист) vs 229-258 (реализация).
**Что не так:** Нет `ComponentValueApiEntity.toDb()`. Нет domain→API (`ComponentType.toApi()`, `ComponentValue.toApi()`). Имена расходятся (`toDbValue` в чеклисте, `toJson` в коде).
**Предложение:** Дополнить: `ComponentValueApiEntity.toDb()`, domain→API мапперы. Унифицировать имя.

---

## Performance & Edge Cases Review

### 🔒 [critical] kotlinx.serialization не подключён в проекте
**Triage:** → закрыто через выбор **`org.json.JSONObject`** вместо kotlinx.serialization. Встроен в Android SDK, без зависимостей / плагинов / proguard rules. Парсер ручной (5-10 строк), достаточен для малых sealed-payload. В `06` § «Сериализация» переписан под JSONObject. Если в будущем payload усложнится (audio / video / table) — переключимся на kotlinx.serialization тогда.

### 🔒 [critical] Edge: unknown templateKey → fallback TEXT крашит парсинг image
**Triage:** → закрыто через правило в гайде. Реальные сценарии: (1) откат версии — dev-only; (2) опечатка в миграции — ловится тестом; (3) удаление варианта enum — критичная ошибка разработчика. В `naming.md` добавлено **R-N-012**: «варианты sealed/enum связанных с БД — только добавлять, никогда не удалять» (защита от (3)). Runtime-защиту не делаем (YAGNI: в prod риск=0 при соблюдении правила).

### 🔒 [minor] JSON-парсинг 2000-3000 раз на DictionaryTab
**Triage:** → закрыто. В чеклист `06` (раздел «DB → API мапперы») добавлен пункт: «Thread: в репозитории Flow с маппингом обернуть в `.flowOn(Dispatchers.Default)` или `withContext(Dispatchers.IO)` для парсинга JSON на background».

### ❌ [minor] `private val json` top-level singleton
**Triage:** → rejected. Неактуально — перешли на `JSONObject`, top-level `Json` instance не используется.

### ❌ [minor] `fromKey()` linear scan
**Triage:** → rejected. YAGNI на малом enum (2-5 вариантов). Optimization если профайлинг покажет hot path.

### ❌ [minor] Edge orphan — NPE на non-null type
**Triage:** → rejected (дубль с A&C #6).

### ❌ [minor] Мапперы — обычные функции, не suspend
**Triage:** → not-an-issue. Соответствует конвенции проекта.

### ❌ [minor] Объём кода маппинга — оправдан
**Triage:** → not-an-issue.

---

## Triage

Заполняется conductor'ом с пользователем.
