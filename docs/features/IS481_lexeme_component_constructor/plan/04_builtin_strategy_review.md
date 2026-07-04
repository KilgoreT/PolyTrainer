# Review: 04_builtin_strategy.md

Параллельный обзор решения по разделению built-in / user-defined компонентов лексемы. 3 subagent'а: Architecture & System Design / Schema Correctness / Edge Cases & Consistency. Каждый finding содержит `Verify:` через встроенные Claude tools.

## Сводка

| Направление | Critical | Minor |
|---|---|---|
| Architecture & System Design | 4 | 2 |
| Schema Correctness | 4 | 4 |
| Edge Cases & Consistency | 7 | 2 |
| **Итого** | **15** | **8** |

**Главные сигналы (повторяются между ревьюерами):**

1. **Двойная семантика `name` для built-in** — Architecture #1 + Edge #2. Поле `name` для built-in служит и ключом ресурса, и литералом для display fallback → конфликты с user-defined names, ложная UNIQUE-защита.
2. **`@StringRes` в enum нарушает слойность** — Architecture #2. Если enum в `core-db-api`, нельзя тащить туда `R.string.*`. Резолв ресурса должен быть в UI/feature-модуле.
3. **TypeConverter не работает в migrate()** — Schema #3 + Edge #3. INSERT в миграции должен быть literal-строкой, не enum. Контракт `enum.key == literal в миграции` нужен явно + unit-тест.
4. **TypeConverter не зарегистрирован** — Schema #1. `@TypeConverters(DateTimeConverter::class)` нужно расширить, иначе build break.
5. **Fresh install — built-in не вставляются** — Edge #6. Миграция работает только на upgrade. На чистой установке нужен seed через `RoomDatabase.Callback.onCreate()`. Не описано.
6. **Unknown system_key из будущей версии** — Edge #4. TypeConverter молча возвращает null → built-in воспринимается как user-defined → защита от удаления слетает.
7. **Эволюция template без миграции payload** — Edge #5. «UPDATE template» в коде не мигрирует существующие payload. Может быть фатально.

---

## Architecture & System Design Review

### 🔒 [critical] Двойная семантика `name` для built-in
**Где:** § «Схема таблицы», § «Открытые вопросы» п.2.
**Что не так:** Заявлено что `system_key` несёт одну семантику, `name` для built-in резолвится через enum. Но open question 2 говорит «переиспользуем `name` как ключ ресурса». Значит `name` для built-in == дубль `system_key` (оба содержат ключ).
**Предложение:** `name IS NULL when system_key IS NOT NULL` с CHECK constraint.
**Verify:** Read `04_builtin_strategy.md:132, 188`.
**Triage:** → закрыто в дизайне (с расширением). `name` стал **опциональным user-override** для built-in: NULL → fallback на enum.nameRes, не-NULL → user переименовал. CHECK: `name IS NOT NULL OR system_key IS NOT NULL`. Закрывает кейс «румын учит французский на en/ru UI» — может переименовать built-in под себя. Зафиксировано в § «Резолв display name» в `04_builtin_strategy.md`. Open question 2 закрыт.

### 🔒 [critical] Enum `BuiltInComponentType` с `@StringRes` в `core-db-api` нарушает слойность
**Где:** § «Маппинг через enum», § «Открытые вопросы» п.1.
**Что не так:** Enum с `@StringRes nameRes: Int` тащит Android resources в чистый БД-API.
**Предложение:** Разделить enum (без StringRes) и UI-маппер.
**Verify:** Read `core-db-api/.../entity/` → API без @StringRes. Read `04_builtin_strategy.md:81-95`.
**Triage:** → закрыто в дизайне. (1) Enum `BuiltInComponentType` в `core-db-api` — только `key: String`, без Android. (2) Room TypeConverter **не нужен** — `system_key` хранится в Room как `String?`, маппинг в enum происходит в обычном маппере `ComponentTypeDb.toApiEntity()` в `core-db-impl`. (3) `nameRes()` extension живёт в UI/feature-модуле (`modules/screen/wordcard/widget/internal/`). Полный раздел «Слойность: где живёт enum, где маппер, где display name» добавлен в `04_builtin_strategy.md` с конкретным кодом и примером строки таблицы на 4 слоях.

### 🔒 [critical] 4-я комбинация (user-defined global) — UNIQUE-асимметрия
**Где:** § «Четыре комбинации».
**Что не так:** Built-in защищён UNIQUE(system_key), user-defined — нет. На 4-й комбинации `(NULL, NULL, name)` SQLite разрешит дубли.
**Предложение:** `UNIQUE(COALESCE(dictionary_id,-1), name)` или явное «дубли допустимы».
**Verify:** Read `04_builtin_strategy.md:38-50, 125-135`.
**Triage:** → закрыто в дизайне. Три уровня защиты:
1. `UNIQUE(system_key)` через `@Index(unique=true)` — built-in (Room генерирует).
2. `UNIQUE(dictionary_id, name)` через `@Index(unique=true)` — user-defined per-dictionary (Room генерирует).
3. **Partial UNIQUE index в migration** для 4-й комбинации: `CREATE UNIQUE INDEX ... ON component_types(name) WHERE dictionary_id IS NULL AND system_key IS NULL`. Partial index с SQLite 3.8+ (minSdk 23 = SQLite 3.8.10, гарантировано). Room не валидирует custom-индексы. Миграционный тест должен явно проверить наличие после миграции.

Visible-label collision (built-in override "Traducere" vs user-defined "Traducere" в словаре) — валидация на уровне UseCase. Раздел «Индексы / constraints» добавлен в § «Схема таблицы» в `04_builtin_strategy.md`.

### 🔒 [critical] Computed `Lexeme.translation` с магической строкой
**Где:** § «Чтение в коде».
**Что не так:** магическая строка `"translation"` вместо enum, linear scan.
**Предложение:** enum + Map-индекс + TODO.
**Verify:** Read `04_builtin_strategy.md:156-159, 81-89`.
**Triage:** → закрыто в дизайне частично.
1. ✅ Магическая строка заменена на `BuiltInComponentType.TRANSLATION` (enum). Type-safe.
2. ✅ Добавлен extension `Lexeme.builtIn(key: BuiltInComponentType)` — один источник lookup для built-in. Computed properties (`translation` / `definition`) используют его.
3. ✅ TODO-комментарий «compatibility shim для quiz / search / DictionaryTab; удалить после миграции».
4. ❌ Map-индекс **отвергнут** (YAGNI): N компонентов мала (2-5), 1000 lexemes × 2 lookup = ~10k простейших сравнений = <1ms. Реальные bottleneck'и — БД и UI render. Если профайл покажет — оптимизация делается **внутри extension** `builtIn()`, без правки API Lexeme.

### 🔒 [minor] «built-in → user-defined через UPDATE» опасен
**Где:** § «Эволюция».
**Что не так:** «Один UPDATE» скрывает сложность.
**Предложение:** удалить сценарий или расписать.
**Verify:** Read `04_builtin_strategy.md:181`.
**Triage:** → закрыто в дизайне. **Built-in immutable** — превращение в user-defined запрещено. Защита на 3 уровнях:
1. DAO — нет метода для UPDATE `system_key`.
2. UseCase валидация: при `update(type)` проверять `old.systemKey == new.systemKey`. Нарушение — `IllegalArgumentException` / `Error.ImmutableSystemKey`.
3. Комментарий в Entity `// system_key is IMMUTABLE after creation`.

TRIGGER в БД не делаем (overkill). Если в будущем понадобится — lightweight refactor (добавить новый специальный метод DAO + UseCase). Зафиксировано в § «Эволюция» в `04_builtin_strategy.md`.

### 🔒 [minor] Рассинхрон с research/sketch без отметки «overrides previous decision»
**Где:** `04_builtin_strategy.md` vs `01_research.md:50`, `02_design_sketch.md:39`.
**Что не так:** Research / sketch без отметки «решение пересмотрено».
**Предложение:** Добавить пометки.
**Triage:** → закрыто планом. Согласно плану «04 → 03 → 02» (закончим 04, потом обновим 03 и 02 с учётом всех решений 04). Рассинхрон устранится естественно при обновлении этих документов.

---

## Schema Correctness Review

### 🔒 [critical] TypeConverter не зарегистрирован — Room не скомпилируется
**Где:** § «Маппинг system_key», `03_database_design.md:277`.
**Что не так:** TypeConverter не зарегистрирован.
**Предложение:** Регистрация в `@TypeConverters` Database.
**Verify:** Read `Database.kt:25`.
**Triage:** → не применимо. После Architecture #2 (enum в `core-db-api`, не в Room) — TypeConverter **не существует**. Room Entity хранит `system_key: String?` и `template_key: String` напрямую как нативные SQLite типы. Маппинг enum ↔ string идёт в обычной extension-функции `ComponentTypeDb.toApiEntity()`. Никаких `@TypeConverters` регистрировать не надо.

### 🔒 [critical] UNIQUE на nullable `system_key` — пояснить семантику NULL-collision
**Где:** § «Ось 1».
**Что не так:** Формулировка вводила в заблуждение.
**Предложение:** Явно зафиксировать семантику.
**Triage:** → закрыто. Переформулировано в § «Ось 1»: «`UNIQUE(system_key)` защищает только built-in от дублей по ключу; множественные NULL для user-defined допустимы по дизайну; защита user-defined через `UNIQUE(dictionary_id, name)` + partial index». Никаких изменений кода, только текст.

### 🔒 [critical] TypeConverter НЕ работает внутри `migrate()`
**Где:** § «Миграция данных», `03_database_design.md:467-474`.
**Что не так:** Migration без TypeConverter, INSERT literal-строкой.
**Предложение:** Unit-тест контракта `assertEquals("translation", BuiltInComponentType.TRANSLATION.key)`.
**Triage:** → не применимо для TypeConverter (после Architecture #2 его нет — enum в `core-db-api`, в Room String). Миграция и так пишет literal `'translation'`. Контракт «`enum.key` совпадает со строкой в миграции» остаётся — защита через unit-тест: `assertEquals("translation", BuiltInComponentType.TRANSLATION.key)` + `assertEquals("definition", BuiltInComponentType.DEFINITION.key)`. Записать в план реализации (`testing-extensions.md` стиль).

### 🔒 [critical] FK CASCADE на dictionaries безопасен для built-in
**Где:** § «Открытые вопросы» п.3.
**Что не так:** Документация недостаточно явная.
**Предложение:** Переформулировать + миграционный тест.
**Triage:** → закрыто. Формулировка в § «Открытые вопросы» п.3 переписана с явным описанием поведения SQLite (FK CASCADE срабатывает только при non-NULL child-FK; PRAGMA foreign_keys включён в runtime, отключён в migrate). Миграционный тест «после удаления всех словарей `getBySystemKey("translation")` всё ещё возвращает row» — в план реализации.

### 🔒 [minor] Нарушение R-N-007 — Boolean `is_system` (если остаётся)
**Где:** `03_database_design.md`.
**Triage:** → закрыто через план «04 → 03 → 02». При обновлении 03 — `is_system` уходит, заменяется на `system_key` (и ORDER BY `(system_key IS NULL) ASC, position ASC`).

### 🔒 [minor] Избыточный `INDEX(lexeme_id)` для component_values
**Где:** `03_database_design.md`.
**Triage:** → закрыто через план. Дубль с Performance finding из `03_database_design_review.md`. При обновлении 03 — удалить `Index("lexeme_id")` из `ComponentValueDb`.

### 🔒 [minor] Affinity TypeConverter для enum — nullable обязателен
**Где:** § «Маппинг system_key».
**Triage:** → не применимо. После Architecture #2 enum не в Room (там `String?`). TypeConverter не существует. Поле `systemKey: String?` в `ComponentTypeDb` уже nullable — это явно показано в коде Entity в § «Room Entity в core-db-impl».

### 🔒 [minor] Защита built-in от soft-delete в SQL
**Где:** § «Открытые вопросы» п.4.
**Triage:** → закрыто. В § «Эволюция» добавлен пункт «Built-in не удаляется (soft-delete заблокирован)» с защитой на 2 уровнях: (1) DAO `softDelete` имеет SQL-фильтр `WHERE id = :id AND system_key IS NULL`; (2) UseCase явная проверка перед вызовом DAO.

---

## Edge Cases & Consistency Review

### ❌ [critical] «Built-in для конкретного словаря» — комбинация без правил поведения
**Где:** § «Четыре комбинации».
**Triage:** → rejected. Кейс не реальный. CHECK constraint заблокировал бы будущие осознанные использования. Описывать правила priority впрок — нет реальных правил. Комбинация остаётся «странной, но допустимой»; если когда-то понадобится осознанное применение — придумаем правила тогда.

### 🔒 [critical] Конфликт user-defined name с built-in name
**Где:** § «Чтение в коде».
**Triage:** → закрыто другими решениями. Architecture #1: `name` для built-in NULL (имя из ресурса через enum) — нет литерала «translation» в БД. Architecture #3: UseCase-валидация при создании user-defined проверяет visible-label collision со всеми built-in resolved-именами. Двух «Translation» в словаре больше не получится.

### 🔒 [critical] Превращение built-in → user-defined ломает идемпотентность миграций
**Где:** § «Эволюция».
**Triage:** → закрыто через Architecture #5. Built-in immutable, превращение запрещено на 3 уровнях (DAO / UseCase / комментарий). Проблема идемпотентности миграций исчезает — system_key не может стать NULL для built-in.

### ❌ [critical] Unknown system_key из будущей версии — null маскирует built-in как user-defined
**Где:** § «Маппинг», метод `toEnum`.
**Triage:** → rejected.

### ❌ [critical] Эволюция template built-in: payload не мигрирует
**Где:** § «Эволюция».
**Triage:** → rejected. Сценарий гипотетический, не будет. Пункт «Поменять template built-in» удалён из § «Эволюция» в `04_builtin_strategy.md` — не обещаем то чего не делаем.

### 🔒 [critical] Чистая установка v12 — built-in НЕ вставляются
**Где:** Не упомянуто.
**Triage:** → закрыто в дизайне. В `04_builtin_strategy.md` § «Миграция данных и seed built-in» добавлено: (1) общая функция `seedBuiltIns(db)` с `INSERT OR IGNORE`; (2) Callback.onCreate для fresh install; (3) Migration вызывает ту же функцию для upgrade; (4) Partial UNIQUE index создаётся в обоих местах. Таблица покрытых сценариев (8 кейсов) в документе.

### ❌ [critical] Откат с v12 на v11 — Room упадёт без fallback
**Где:** Не упомянуто.
**Triage:** → rejected. Администрирование, не архитектура.

### ❌ [minor] Concurrency: create user-defined + delete dictionary одновременно
**Triage:** → rejected.

### ❌ [minor] CASCADE через два пути при удалении dictionary
**Triage:** → rejected.

---

## Triage

Заполняется conductor'ом вместе с пользователем. Для каждого finding: `→ закрыть в дизайне сейчас` / `→ backlog` / `→ rejected`.
