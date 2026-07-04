# Quiz strategy: dictionary-level config of source components (per quiz mode)

## Контекст (что было)

**До IS481:**
- `QuizGameImpl.kt:441-507` — quiz рендерит вопрос: сначала пытается `lexeme.translation`, потом `lexeme.definition`, fallback `else throw IllegalArgumentException("No translation or definition")`.
- Defensive guard — теоретически не должен срабатывать (data validation на уровне приложения требует наличия translation или definition).

**После IS481 (по плану до этого решения):**
- `translation` мигрирует в **built-in** `system_key="translation"`. Доступ через computed extension `lexeme.translation` сохраняется (читает компонент с этим system_key).
- `definition` мигрирует в **user-defined per-dictionary** (тип с `name="Definition"`, `system_key=NULL`). Computed extension `lexeme.definition` напрямую не работает — нет built-in для definition.

**Проблема:**
- Лексемы только с definition (translation IS NULL в v11) — после миграции в новой БД имеют только user-defined компонент.
- Quiz пытается `lexeme.translation` → NULL → `lexeme.definition` → NULL → `else throw` → **crash**.
- Quiz не имеет контракта «какой user-defined использовать» — поиск по имени `"Definition"` ломается если пользователь переименует тип (когда появится UI редактирования) или создаст в другой локали.

---

## Решение

Отдельная таблица `quiz_configs(dictionary_id, quiz_mode, component_refs)`. Каждый row — config для конкретного режима конкретного словаря. В IS481 — только `quiz_mode='write'`. Future-режимы (`card`, `recall`, ...) добавляются INSERT'ом row'ов без миграции схемы.

`component_refs` — JSON array объектов `ComponentTypeRef` (sealed, два варианта):
- `BuiltIn(key: BuiltInComponent)` — ссылка через stable enum-key (для built-in компонентов).
- `UserDefined(name: String)` — ссылка через имя (для user-defined per-dictionary; уникальность гарантирована UNIQUE `(dictionary_id, name)`, см. 03.md).

Stable keys → backup/restore через export/import работает (не ломается на ID re-create после wipe). Quiz config обособлен от dictionary metadata — конфиг каждого режима читается / меняется атомарно.

---

## Технический дизайн

### Storage — таблица `quiz_configs`

```sql
CREATE TABLE quiz_configs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dictionary_id INTEGER NOT NULL,
    quiz_mode TEXT NOT NULL,           -- "write", "card", "recall", ...
    component_refs TEXT NOT NULL,      -- JSON array of ComponentTypeRef
    FOREIGN KEY (dictionary_id) REFERENCES dictionaries(id) ON DELETE CASCADE,
    UNIQUE(dictionary_id, quiz_mode)
);
CREATE INDEX index_quiz_configs_dictionary_id ON quiz_configs(dictionary_id);
```

Room-сторона:
- Entity `QuizConfigDb` в `core-db-impl/entity/` — по convention `naming.md` R-N-001-005 (DTO в core-db-impl, имя в Db-суффиксе).
- Аналогичный публичный API entity `QuizConfigApiEntity` в `core-db-api/entity/`.
- DAO: `QuizConfigDao` с методами `getConfig(dictionaryId, quizMode)`, `insertDefaultQuizConfig(dictionaryId, quizMode)` (простой INSERT с hardcoded JSON `'[{"type":"builtin","key":"translation"}]'`, без mapper'а), `deleteByDictionary(dictionaryId)` (для CASCADE-чистоты на уровне Kotlin, FK даёт автомат). **Write через mapper** (upsert произвольного `QuizConfig`) — НЕ в IS481 (MIN-8), появится в backlog UI configurator.

CASCADE: `ON DELETE CASCADE` на FK → удаление dictionary автоматически чистит все его configs.

UNIQUE `(dictionary_id, quiz_mode)` → один config на пару (словарь, режим). Конфликт при INSERT → upsert либо явная ошибка (решит DAO-контракт).

### Invariants

**F1 — полнота config:** для каждого `dictionary_id` × каждый registered `quiz_mode` существует ровно один row в `quiz_configs`. При добавлении нового `quiz_mode` (`card`, `recall`, ...) — миграция / data layer обязан INSERT'ить default config row для всех existing dictionaries. DDL `UNIQUE(dictionary_id, quiz_mode)` не покрывает требование «row должен существовать», только «не больше одного» — этот invariant процедурный, держится миграцией + `addDictionary` транзакцией (AGG-4 реверс).

**F4 — display order:** позиция `ComponentTypeRef` в JSON-массиве `component_refs` определяет порядок отображения компонента в quiz session. При INSERT/UPDATE сохранять order. Mapper `QuizConfigDb → QuizConfig` сохраняет порядок (JSON-array → List).

**F5 — no N+1:** `quizComponents` подгружается **один раз** на quiz session через `getQuizConfig(dictionaryId, quiz_mode)` и передаётся в `toQuizItem` для каждой лексемы. Не lookup-per-lexeme.

**F6 — referential consistency:** после DELETE component_type, ни в одном `quiz_configs.component_refs` не остаётся ссылок на удалённый тип. Cleanup синхронный, в DAO `deleteComponentType` (одна транзакция):

1. SELECT все `quiz_configs` где `component_refs` содержит ref на удаляемый type (через `json_each` либо в Kotlin после SELECT).
2. UPDATE каждого row — `json_remove(component_refs, '$[index]')` либо собрать новый JSON в Kotlin.
3. DELETE component_type (CASCADE на `component_values` автомат).

**F6 покрывает только DELETE.** Rename component_type — отдельная атомарная операция (UPDATE `component_types.name` + UPDATE всех `quiz_configs.component_refs` через `json_replace`); без cleanup ссылок rename вызывает graceful skip в квизе → UX-регрессия. Rename поддержка — в backlog-фиче «Quiz config UX» (см. `docs/Backlog.md`).

**В IS481 операции `deleteComponentType` и rename отсутствуют** (нет UI триггера, миграция только создаёт типы; component_types в IS481 immutable после миграции). Invariant и cleanup-контракт документируются для backlog-фичи «Quiz config UX».

### `ComponentTypeRef` sealed

```kotlin
// modules/domain/lexeme/...

sealed interface ComponentTypeRef {
    @JvmInline value class BuiltIn(val key: BuiltInComponent) : ComponentTypeRef
    @JvmInline value class UserDefined(val name: String) : ComponentTypeRef
}
```

Локация: `modules/domain/lexeme` (domain — единое место для всех component-related domain types по A1 решению из `_alignment_decisions.md`).

### JSON-serialization (discriminator)

```kotlin
// Сериализация
fun ComponentTypeRef.toJson(): String = when (this) {
    is BuiltIn -> """{"type":"builtin","key":"${key.key}"}"""
    is UserDefined -> """{"type":"user","name":"$name"}"""
}

// Десериализация
fun String.toComponentTypeRef(): ComponentTypeRef = TODO("parse JSON via org.json.JSONObject")
```

Storage — колонка `component_refs` хранит JSON array:
```json
[
    {"type": "builtin", "key": "translation"},
    {"type": "user", "name": "Definition"}
]
```

JSON helper — в `core-db-impl` (Android library, `org.json.JSONObject` доступен).

### `QuizConfig` — отдельная domain entity (OQ-1)

Два варианта (выбор — в business sub-flow при implement):

По OQ-1 — отдельная domain entity `QuizConfig(dictionaryId, quizMode, componentRefs)` без префикса `Dictionary`. Lookup через `GetQuizConfigUseCase(dictionaryId, quizMode)` — атомарный lookup только нужного режима, без over-fetch.

Предварительный лидер — **(B)**: quiz session дёргает один режим, не вся карточка dictionary. Меньше mapping work в `DictionaryDao`. Финальный выбор — business sub-flow.

### Lookup в quiz

`QuizGameImpl.toQuizItem(writeQuiz)` — переписать:

```kotlin
fun WriteQuiz.toQuizItem(
    quizComponents: List<ComponentTypeRef>,   // pre-fetched per session
    resourceManager: ResourceManager,
    isDebugOn: Boolean,
): QuizItem? {                                 // nullable — skip при mismatch
    val source = quizComponents.firstNotNullOfOrNull { ref ->
        lexeme.components.firstOrNull { it.matchesRef(ref) }
    } ?: return null                           // graceful skip, не error()

    val text = (source.data as? ComponentValueData.TextValue)?.text
        ?: (source.data as? ComponentValueData.LongTextValue)?.text
        ?: return null
    return TODO("остальной build QuizItem — answer/question через text + lexeme.lexemeId")
}

fun ComponentValue.matchesRef(ref: ComponentTypeRef): Boolean = when (ref) {
    is ComponentTypeRef.BuiltIn -> type.systemKey == ref.key
    is ComponentTypeRef.UserDefined -> type.systemKey == null && type.name == ref.name
}
```

**F2 — graceful skip:** возвращаем `null` (skip) вместо `error()`. Вызывающий код фильтрует null'ы (`.mapNotNull { it.toQuizItem(...) }`). Lexeme без quiz-source не попадает в session — нет crash.

**F5 — no N+1:** `quizComponents` подгружается **один раз** в начале quiz session через `getQuizConfig(dictionaryId, quiz_mode="write")` и передаётся в `toQuizItem` для каждой лексемы. Не lookup-per-lexeme.

### Order semantics (F4)

`firstNotNullOfOrNull` берёт **первый match по порядку config**.

Для config `[BuiltIn(TRANSLATION), UserDefined("Definition")]`:
- Лексема с обоими компонентами → translation (первый в config).
- Лексема только с definition → definition (первый успешный match).
- Лексема без обоих → `null` (skip).

**Это контракт, не временно.** Семантика: «приоритет компонентов в config определяет приоритет рендеринга». Будущий UI настроек dictionary позволит пользователю менять порядок компонентов в config — это семантически = менять приоритет.

---

## Migration step (v11 → v12)

После шагов 1-5 из 05.md (создание таблиц component_types/component_values + миграция данных translation/definition) — **новый шаг 6**:

```sql
-- 6.1: создаём таблицу + индекс
CREATE TABLE quiz_configs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    dictionary_id INTEGER NOT NULL,
    quiz_mode TEXT NOT NULL,
    component_refs TEXT NOT NULL,
    FOREIGN KEY (dictionary_id) REFERENCES dictionaries(id) ON DELETE CASCADE,
    UNIQUE(dictionary_id, quiz_mode)
);
CREATE INDEX index_quiz_configs_dictionary_id
    ON quiz_configs(dictionary_id);

-- 6.2: Заполнить config quiz_mode="write" для ВСЕХ словарей.
--      БЕЗ фильтра WHERE translation IS NOT NULL — даже пустой dictionary
--      получает default [BuiltIn(TRANSLATION)]. (F1 closed.)
INSERT INTO quiz_configs (dictionary_id, quiz_mode, component_refs)
SELECT id, 'write', json_array(
    json_object('type', 'builtin', 'key', 'translation')
)
FROM dictionaries;

-- 6.3: Для словарей с definition хотя бы у одной лексемы — добавить
--      UserDefined("Definition") в config.
--      AGG-8: verify синтаксиса '$[#]' под bundled SQLite перед IS481;
--      если не работает — fallback на собирать JSON в Kotlin перед UPDATE.
UPDATE quiz_configs
SET component_refs = json_insert(
    component_refs,
    '$[#]',
    json_object('type', 'user', 'name', 'Definition')
)
WHERE quiz_mode = 'write'
  AND dictionary_id IN (
    SELECT DISTINCT w.dictionary_id
    FROM words w JOIN lexemes l ON l.word_id = w.id
    WHERE l.definition IS NOT NULL
  );
```

(Точная SQL-форма зависит от bundled SQLite json1 функций; альтернатива — Kotlin migration code с прямыми INSERT/UPDATE на каждый dictionary.)

Правила автомиграции:
- Каждый dictionary получает row `(dictionary_id, 'write', '[{"type":"builtin","key":"translation"}]')` — **default для всех**, даже без лексем (F1 closed).
- Если в dictionary есть хоть одна лексема с `definition IS NOT NULL` → к существующему config добавляется `{"type":"user","name":"Definition"}`.

Постмиграция: backward-compat автоматический — все existing лексемы продолжают рендериться в quiz как раньше.

---

## Тестовые сценарии

1. **Default config (empty deps):** dictionary без лексем → `quiz_mode='write'` config `[BuiltIn(TRANSLATION)]` всё равно записан (F1 closed — даже пустой dictionary).
2. **Default config (translation-only):** dictionary с лексемами только-translation → config `[BuiltIn(TRANSLATION)]`.
3. **Definition present:** dictionary где хотя бы одна лексема с definition → config `[BuiltIn(TRANSLATION), UserDefined("Definition")]`.
4. **Definition-only dictionary:** все лексемы только с definition (translation IS NULL) → config `[BuiltIn(TRANSLATION), UserDefined("Definition")]`. Новая translation-лексема, добавленная после миграции, находит translation OK (BuiltIn в config есть).
5. **Quiz lookup translation-only lexeme:** `toQuizItem` возвращает translation.
6. **Quiz lookup definition-only lexeme:** `toQuizItem` возвращает definition (через UserDefined("Definition") в config).
7. **Quiz lookup priority:** lexeme с обоими компонентами → translation (config order — BuiltIn первый).
8. **Quiz lookup mismatch:** lexeme без любого компонента из config → `toQuizItem` возвращает `null` (skip, не crash). F2 closed.
9. **Corrupt JSON in `component_refs`:** парсер throws → quiz session fails gracefully (явная ошибка для bug report, не silent corruption).
10. **Backup/restore:** export → wipe → import → `quiz_configs` восстанавливается, quiz продолжает работать (stable keys).
11. **CASCADE on dictionary delete:** удаление dictionary → все его rows в `quiz_configs` удалены автоматически (FK CASCADE).
12. **addDictionary auto-INSERT** (AGG-4 реверс): создание нового словаря → атомарно INSERT в `dictionaries` + `insertDefaultQuizConfig(newId, 'write')` пишет default `[BuiltIn(TRANSLATION)]`. Новый словарь сразу квизит translation.
13. **Empty quiz session (MIN-12a):** если для словаря config есть, но ни одна лексема не удовлетворяет (нет component_values нужных типов) — quiz возвращает пустую session. UX оставлен как сейчас (MIN-12a, улучшение — в backlog UI configurator).
12. **Future quiz_mode extensibility:** INSERT row `(dictionary_id, 'card', '[...]')` не требует миграции схемы — таблица поддерживает любой `quiz_mode` строкой.

---

## Future direction

- **UI настроек dictionary** — отдельная фича в backlog «Quiz config UX» (`docs/Backlog.md` § Срочное). Пользователь редактирует `component_refs` для каждого режима через UI. В рамках той же фичи: создание `modules/domain/quiz` модуля + перенос `QuizConfig` / `ComponentTypeRef` из `modules/domain/lexeme` (AGG-10 TODO). Также: DAO `deleteComponentType` атомарный cleanup `quiz_configs.component_refs` через `json_remove` (F6 invariant, MIN-11). Write-mapper `QuizConfig.toApiEntity()` появится здесь (в IS481 убран по MIN-8).
- **Множественные quiz_mode** — `card`, `recall`, и т.д. добавляются INSERT'ом row'ов `(dictionary_id, '<mode>', '<refs>')`. Без миграции схемы. **При добавлении нового `quiz_mode` обязательно INSERT default config для всех existing dictionaries** (F1 invariant полноты).

---

## Rejected alternatives (history)

Раньше рассматривались 4 варианта:
- **A** Definition остаётся built-in — отвергнут: definition не universal (не все словари нуждаются), built-in для всех — over-promote.
- **B** Quiz skip definition-only — UX-регресс (definition-only лексемы исчезают из quiz без визуального fallback).
- **C** Quiz ищет по имени `"Definition"` (hack) — magic string, локализация / переименование ломает.
- **D** Generic quiz с UI выбора — слишком large scope для IS481, требует UI работы.

Также рассматривался промежуточный вариант:
- **JSON-колонка `dictionaries.quiz_components: TEXT`** — отвергнут в пользу отдельной таблицы. Причины: (1) добавление quiz_mode (`card`, `recall`, ...) требовало бы либо вложенного JSON `{"write": [...], "card": [...]}` (двойная сериализация, грязная схема), либо новых колонок (`card_components`, `recall_components` — anti-pattern). (2) Атомарность изменений: запись config для одного режима через UPDATE на dictionaries.quiz_components пере-сериализует весь JSON для всех режимов — race conditions при будущем concurrent UI редактировании разных режимов. (3) Read избирательность: quiz session нуждается в одном режиме — отдельная таблица даёт точечный lookup без deserialize всех режимов.

Финальное решение (отдельная таблица + автомиграция) объединяет лучшее из A (built-in для всех словарей через config, не enum), D (generic подход) и обеспечивает scalability по quiz_mode без UI работы в IS481.

_model: claude-opus-4-7[1m]_
