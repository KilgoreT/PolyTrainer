# IS481 alignment pass — decisions log

Решения по gap'ам из readiness анализа. Применяются ВМЕСТЕ одним проходом после прохождения всех пунктов.

## Decisions

### Gap-0 [решено]: `category` поле в Lexeme

**Решение:** УДАЛИТЬ упоминания из 02.md и 06.md (Domain shape, mapper). Поле было заготовкой, в БД отсутствует, никогда не использовалось.

### Gap-1 [решено]: устаревшие locations `modules/screen/wordcard/entity/` → `modules/domain/lexeme/`

**Решение:** заменить все упоминания locations в 04.md (строки 151, 278) и 06.md (строки 187, 216, 242) на `modules/domain/lexeme/`. Domain-классы (Lexeme, ComponentType, ComponentValue, LexemeBuiltInExt) живут в общем модуле.

### Gap-2 [no-op / false alarm]: `wordId` в generic API методах

**Проверено реальным кодом:** `CoreDbApi.LexemeApi.addLexemeWithTranslation(wordId: Long, dictionaryId: Long, translation): Long` — `wordId` остался как **параметр метода**. Удалён только из data class `LexemeApiEntity`. Контракт API совместим со scope IS481.

**Решение:** generic метод `addLexemeWithBuiltInComponent(wordId: Long, dictionaryId: Long, systemKey: BuiltInComponent, data: ComponentValueData): Long` — сохранить `wordId` и добавить `dictionaryId` для соответствия существующим атомарным методам. В 02.md / 05.md обновить сигнатуры если они показаны без `dictionaryId`.

### Gap-3 [решено]: `ComponentValue.type` vs `componentTypeId` противоречие

**Решение:** `ComponentValue` хранит полный `type: ComponentType` (не `componentTypeId: ComponentTypeId`). Аналогично `ComponentValueApiEntity.type: ComponentTypeApiEntity`. Multi-level `@Relation` в DAO даёт `ComponentValueWithType` (Embedded + Relation), маппинг пробрасывает оба слоя. Lookup `it.type.systemKey == key` работает напрямую.

Затрагивает: 06.md (`ComponentValueApiEntity.toDomain`, domain `ComponentValue` shape), 04.md (built-in lookup formulation), 03.md (ApiEntity shape, маппер DAO → API).

### Gap-5 [решено]: 02.md § Зависимости и порядок работ устарел

**Решение:** в 02.md § «Зависимости и порядок работ» и § «Следующие шаги» переписать упоминание «вынести в `modules/domain/lexeme` до IS481 (backlog § Архитектура)» на «модуль `modules/domain/lexeme` создан в IS482 (PR #483, merged `6d3499c`); IS481 его расширяет — добавляет `ComponentValue`, `ComponentType`, `BuiltInComponent`, `ComponentTemplate`, computed extensions».

### Gap-6 [deferred]: Quiz strategy 07 (Variant A/B/C/D)

Решение отложено пользователем «в самую последнюю очередь». До решения **blocked Migration step** (зависит от Variant A vs B/C/D). Связанные расхождения 04.md/05.md по статусу definition откладываются вместе. Возвращаемся к этому в конце alignment pass.

### Gap-7 [решено]: тесты `Lexeme.builtIn` extension и computed shim

**Решение:** добавить в 06.md § «Тестирование» новый тест-файл `LexemeBuiltInExtTest.kt` (в `modules/domain/lexeme/src/test/...`) с 6-8 кейсами:

- `builtIn(TRANSLATION)` возвращает correct ComponentValue когда translation present.
- `builtIn(TRANSLATION)` возвращает null когда отсутствует.
- `builtIn(DEFINITION)` — то же.
- `Lexeme.translation: String?` — возвращает text из ComponentValueData.TextValue, null если нет built-in.
- `Lexeme.definition: String?` — то же.
- Multiple components (built-in + user-defined) — built-in lookup игнорирует user-defined.
- Empty components → builtIn возвращает null.

### Gap-8 [решено]: business_contract.md не создаём заранее

**Решение:** не генерировать `business_contract.md` отдельно. При запуске adaptive flow conductor подаёт `02_design_sketch.md` как input в business_walkthrough → business_contract шаг; sub-agent сам собирает финальный contract (как было в IS482, там тоже не было исходного contract, agent его собрал из contract_spec + walkthrough). 02_design_sketch.md остаётся как высокоуровневый guide.

### A1 [решено]: domain types живут в `modules/domain/lexeme`, `core-db-api` зависит от domain

**Решение по Clean / Dependency Rule:**

1. Переместить domain concepts в `modules/domain/lexeme`:
   - `BuiltInComponent` (enum) — это domain concept (доменное знание о built-in типах компонентов).
   - `ComponentTemplate` (enum) — domain concept (тип содержимого компонента).
   - `ComponentValueData` (sealed: `TextValue` / `LongTextValue` / `ImageValue`) — domain shape.
   - `ComponentType` / `ComponentValue` / `ComponentTypeId` / `ComponentValueId` — domain entities.
   - `LexemeBuiltInExt.kt` (computed extensions `Lexeme.builtIn`, `.translation`, `.definition`).

2. `core-db-api` начинает зависеть от `modules/domain/lexeme` (новая Gradle dep edge). `ComponentValueApiEntity` имеет поле `data: ComponentValueData` (тип из domain).

3. `ComponentValueDataJson.kt` (JSON helper c `org.json.JSONObject`) — в `core-db-impl` (Android library, доступен `JSONObject`). Парсит API entity ↔ domain `ComponentValueData`.

4. Затрагивает обновления docs:
   - 04.md § «Слойность» — переписать location enum/sealed.
   - 06.md § «Сериализация ComponentValueData» — JSON helper в `core-db-impl`, не в `core-db-api/entity/`.
   - 03.md — `ComponentValueDb.toApiEntity()` маппер использует JSON helper (тот же модуль `core-db-impl`).

Это инверсия Dependency Rule — данные знают domain, не наоборот.

### A3 [решено]: `@Deprecated` обёртка сохраняет реальную сигнатуру (TranslationApiEntity)

**Решение:** drop-in замена реальной сигнатуры. В 02.md / 05.md скетчи обёрток поправить на:

```kotlin
@Deprecated("Use addLexemeWithBuiltInComponent")
suspend fun addLexemeWithTranslation(
    wordId: Long,
    dictionaryId: Long,
    translation: TranslationApiEntity,    // НЕ String!
): Long = addLexemeWithBuiltInComponent(
    wordId, dictionaryId, BuiltInComponent.TRANSLATION,
    ComponentValueData.TextValue(translation.value),
)
```

Аналогично для `addLexemeWithDefinition(..., definition: DefinitionApiEntity)`. Mate / тесты / wordcard UseCaseImpl не трогаются.

### B4 / C2 [решено]: shim в IS481, mate refactor отдельной фичей

**Решение:** в `modules/domain/lexeme/Lexeme.kt` оставить поля `translation: Translation?` и `definition: Definition?` как `@Deprecated`, value-classes `Translation`/`Definition` тоже `@Deprecated`. Маппер `LexemeApiEntity.toDomain()` заполняет эти поля **из `components`** через built-in lookup:

```kotlin
data class Lexeme(
    val lexemeId: LexemeId,
    val components: List<ComponentValue>,
    @Deprecated("Use components") val translation: Translation? = null,
    @Deprecated("Use components") val definition: Definition? = null,
    val addDate: Date,
    val changeDate: Date? = null,
)
```

Mate использует поля без правок. `copy(translation = X)` создаёт рассинхрон с `components` (временно, ловится при mate refactor).

**Trade-off:** дублирование данных + риск рассинхрона на mutations. Цена за неизменность mate. После mate refactor (backlog «Wordcard mate refactor: generic компоненты») — shim удаляется.

Затрагивает:
- 02.md / 06.md — обновить shape `Lexeme` (поля + Deprecated).
- 06.md § Mapper `LexemeApiEntity.toDomain()` — заполняет translation/definition из components.
- backlog запись «mate refactor» получает явное «триггер: shim удаляется после рефактора».

### B1 [решено]: `Callback.onCreate(connection: SQLiteConnection)`

**Решение:** в чеклист `05_migration_strategy.md` § «Bundled SQLite» добавить пункт: переопределить `Database.Callback.onCreate(connection: SQLiteConnection)`, не `onCreate(db: SupportSQLiteDatabase)` (последний молча игнорируется под bundled driver). `seedBuiltIns(connection: SQLiteConnection)` использует `connection.execSQL(...)` через extension `androidx.sqlite.execSQL`.

### B2 [obsolete — миграции удалены целиком, см. AGG-12]: переписать все 10 существующих миграций под bundled driver

**ОТМЕНЕНО.** После prereq фичи `IS481_lexeme_component_constructor_vPrepared` принято решение **удалить все 10 historical migrations + migration test infrastructure** (никто из internal testers не имеет БД < v11, проверено через git tags 0.1.0 / 0.1.5). Переписывать нечего — кода больше нет.

Актуальное решение про bundled driver + migration handling — **AGG-12** ниже.

См. `docs/db-migrations-history.md` — retrospective timeline удалённых миграций.

### B3 [решено]: `SQLiteConnection.execSQL` через extension `androidx.sqlite.execSQL`

**Решение:** в 04.md / 05.md SQL-сниппеты использовать `connection.execSQL("...")` с import `androidx.sqlite.execSQL`. В чеклисте 05 явно зафиксировать что extension импортируется во всех migrations / seedBuiltIns.

### C4 [rejected]: Partial UNIQUE для user-defined global

**Rejected.** Index закладывается как schema-readiness под будущий UI создания user-defined global. Не YAGNI — defensive schema на дешёвом этапе создания таблицы.

### C5–C10, B6, B9, B10 [rejected/closed batch]: schema-readiness / on-future / уже учтено

- **C5** `remove_date` soft-delete для ComponentType — schema-readiness под UI удаления.
- **C6** `BuiltInComponent` enum при одном значении — на будущее (pronunciation / transcription / etc).
- **C7** `ComponentTemplate.IMAGE` / `ImageValue` — schema-readiness под будущий image-компонент.
- **C8** JSON `v: 1` payload version — schema-readiness под breaking changes payload.
- **C9** `canRemoveComponent` — учтён в чеклисте 05 (closed).
- **C10** Multi-level @Relation без бенчмарка — TBD test-time, не блокер плана.
- **B6** existing user-defined «Definition» при upgrade — `INSERT OR IGNORE` уже в чеклисте.
- **B9** partial UNIQUE not validated Room — упомянуто явно в 04, не сюрприз.
- **B10** `INSERT OR IGNORE` deps on UNIQUE — учтено через `@Index(unique=true)` в Entity.

### A2 / B5 [решено]: убрать `wordId` из маппера в 06.md

**Решение:** в `06_mapping_design.md` § `LexemeDbEntity.toApiEntity()` (около строки 170) удалить строку `wordId = lexemeDb.wordId`. Реальный `LexemeApiEntity` после IS482 не имеет этого поля. Alignment pass это пропустил.

### A5 [решено]: разделить слои @Deprecated wrappers в чеклисте 05

**Решение:** `05_migration_strategy.md:58` — разделить на два пункта:
- `CoreDbApi.LexemeApi @Deprecated`: `addLexemeWithTranslation` / `addLexemeWithDefinition` / `updateLexemeTranslation` / `updateLexemeDefinition`.
- `WordCardUseCase @Deprecated`: `deleteLexemeTranslation` / `deleteLexemeDefinition`.

Разные слои — implement не запутается куда что класть.

### Test gaps batch (B7 / B8+C11 / C12 / C13) [решено]

**Решение:** в `06_mapping_design.md` § «Тестирование» добавить 4 кейса:
- **orphan lexeme** — после миграции `components.isEmpty()`, `lexeme.translation == null`.
- **invalid JSON** в `value` — `String.toComponentValueData()` обернуть в try-catch с fallback / явный exception.
- **rollback атомарности** `addLexemeWithBuiltInComponent` — FK violation → `lexemes` + `write_quiz` без новых строк (упомянут в чеклисте 05, продублировать в § Тестирование 06).
- **`LexemeApiEntity.toDomain()` с components** — кейсы: translation-only, translation+user-defined, orphan, несколько user-defined.

### Mini-cosmetic batch (A6 / A7 / A8 / A9) [решено]

**Решение:** одной правкой при apply:
- **A6** 04.md Open questions: «sealed `BuiltInComponent`» → «enum `BuiltInComponent`».
- **A7** 04.md: «(без суффикса, не `BuiltInComponent`)» → «(без суффикса `ApiEntity`)».
- **A8** 02.md / 06.md: добавить декларации `@JvmInline value class ComponentTypeId(val id: Long)` / `ComponentValueId(val id: Long)` (по аналогии с `LexemeId`).
- **A9** 03.md `LexemeDbEntity`: комментарий «`entityColumn = "lexemeId"` — camelCase т.к. SampleDb legacy без @ColumnInfo; ComponentValueDb уже использует snake_case через @ColumnInfo».

### Shim consistency invariant [решено]

**Решение:** добавить в 06.md:

1. **§ Mapper** — debug-assertion в `LexemeApiEntity.toDomain()`: после построения shim-полей проверять что они согласованы с `components` built-in lookup. В release no-op. Пример:
   ```kotlin
   if (BuildConfig.DEBUG) {
       val expectedT = components.firstOrNull { it.type.systemKey == BuiltInComponent.TRANSLATION }
           ?.data?.let { (it as? ComponentValueData.TextValue)?.text }
       check(translation?.value == expectedT) { "Shim/components rassinhron: $translation vs $expectedT" }
   }
   ```
2. **§ Тестирование** — добавить тест **invariant `mapper output: shim consistent with components built-in`**: property-based / параметризованный тест на нескольких комбинациях (translation-only, translation+definition, user-defined-only, empty) проверяет что `lex.translation?.value == lex.components.firstOrNull{TRANSLATION}?.text` после маппера. Ловит регрессии mapper'а до prod.

Не закрывает `lexeme.copy(translation = X)` mutation в mate (рассинхрон создаётся **после** маппера). Это **known trade-off** — закрывается при mate refactor.

### OQ-1 [решено]: QuizConfig — отдельная entity (B), без Dictionary префикса

**Решение:** quiz-config как **отдельная domain entity**, не встроенно в Dictionary. Dictionary остаётся без quiz-полей (separation of concerns — словарь и quiz это разные tabs, разные модули).

```kotlin
data class QuizConfig(
    val dictionaryId: Long,
    val quizMode: String,
    val componentRefs: List<ComponentTypeRef>,
)
```

UseCase: `getQuizConfig(dictionaryId: Long, quizMode: String = "write"): QuizConfig?`.
Таблица: `quiz_configs(id, dictionary_id, quiz_mode, component_refs)`.

**Naming:** без префикса `Dictionary` — связь через FK column `dictionary_id`, не через имя сущности.

В 07 переписать § «Dictionary.quizConfigs или отдельная domain entity?» — убрать soft-hint, оставить только вариант B как принятое решение. Imена `dictionary_quiz_configs` → `quiz_configs` (table), `DictionaryQuizConfigDb`/`...ApiEntity` → `QuizConfigDb`/`QuizConfigApiEntity` (entities).

### AGG-1 [решено]: definition = user-defined per-dictionary, BuiltInComponent.DEFINITION удалить

**Решение:**
- В enum `BuiltInComponent` оставить только `TRANSLATION`. **Удалить** `DEFINITION` value.
- Mapper `LexemeApiEntity.toDomain()` заполняет shim `Lexeme.definition` через user-defined lookup:
  ```kotlin
  definition = components
      .firstOrNull { it.type.systemKey == null && it.type.name == "Definition" }
      ?.data
      ?.let { (it as? ComponentValueData.TextValue)?.text }
      ?.let { Definition(it) }
  ```
- Mate / dictionaryTab / quiz **не трогаем** — продолжают читать `lexeme.definition?.value` через shim-поле.

Затрагивает: 04.md (seedBuiltIns только translation, удалить definition из enum/seed), 05.md (migration definition → user-defined per-dictionary, уже зафиксировано), 06.md (mapper переписать на user-defined lookup, не `BuiltInComponent.DEFINITION`).

**Зависимость:** корректность работает только если AGG-6 тоже закрыто (обёртки `addLexemeWithDefinition` создают user-defined "Definition" тип в словаре если ещё нет).

**Cross-ref:** AGG-4/AGG-5 (реверс) — quiz wire возвращён в IS481, иначе после удаления `BuiltInComponent.DEFINITION` existing definitions становятся сиротами в квизе. Это противоречило бы цели фичи (сохранить пользовательские данные).

### AGG-6 [решено]: удалить definition-deprecated wrappers, переписать на generic + UI блок

**Решение:** definition мигрирует в user-defined → built-in API для неё больше не семантичен. Подход:

1. **Удалить @Deprecated definition обёртки:**
   - `CoreDbApi.LexemeApi`: `addLexemeWithDefinition`, `updateLexemeDefinition`, `deleteLexemeDefinition`.
   - `WordCardUseCase` interface: `addLexemeDefinition`, `deleteLexemeDefinition`.
2. **Generic API для definition:**
   - `addLexemeWithUserDefinedComponent(wordId, dictionaryId, name="Definition", data)` (атомарный INSERT lexeme + write_quiz + первый component_value). Lookup component_type по `(dictionary_id, name="Definition", system_key=NULL)`.
   - Update / delete — через существующие `updateComponentValue(id, data)` / `deleteComponentValue(id)`.
3. **Переписать callsite'ы (расширение scope IS481):**
   - `WordCardUseCaseImpl` — переписать на generic.
   - `DatasourceEffectHandler` (2 точки) — переписать.
   - 2 теста (`DatasourceEffectHandlerTest`, `SpecializedLexemeExtTest` если задевает) — переписать.
4. **UI блок:** chip «Определение» показывается только если в словаре есть user-defined тип `name="Definition"`. Добавить:
   - `WordCardState.hasDefinitionComponent: Boolean` (новый флаг).
   - При load wordcard грузить `component_types` словаря, проверять наличие `name="Definition" AND system_key=NULL`.
   - Composable `AddLexemeMeaningRow` / `LexemeMeaningField` скрывает chip definition если false.
5. **State / Reducer / Shim:** Domain `Lexeme.definition?` shim продолжает работать (заполняется маппером по AGG-1). `LexemeState.definition` field остаётся. Mate / wordcard reducer / `updateLexemeDefinitionText` extension — НЕ трогаем (state mutation через shim).
6. **Translation wrappers остаются** как @Deprecated (translation = built-in).

Auto-create типа `name="Definition"` при первом INSERT в новом словаре — **не нужен**, UI блокирует ввод до явного создания типа через future UI редактирования компонентов.

Translation-wrappers и translation-flow в IS481 остаются работающими через @Deprecated shim. Symmetric asymmetry: translation built-in → обёртки работают, definition user-defined → нет.

### AGG-2 [решено]: маппер в `app/`, не в `modules/domain/lexeme`

**Решение:** `LexemeApiEntity.toDomain()` декларируется в `app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt` (паттерн IS482 — файл уже существует).

**Почему:** `core-db-api` (где `LexemeApiEntity`) depends on `modules/domain/lexeme` по A1. Если маппер в domain — domain должен знать про ApiEntity → циклическая Gradle dep. `app/` видит оба слоя, циклов нет.

**Затрагивает:** `06_mapping_design.md` — переписать раздел про местоположение маппера, убрать упоминания «маппер в `modules/domain/lexeme`», указать `app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt`.

### AGG-3 [решено]: 07.md догнать OQ-1 (`dictionary_quiz_configs` → `quiz_configs`)

**Решение:** в `07_quiz_strategy.md` применить наименования OQ-1:
- SQL table: `dictionary_quiz_configs` → `quiz_configs`.
- Room entity: `DictionaryQuizConfigDb` → `QuizConfigDb`.
- Api entity: `DictionaryQuizConfigApiEntity` → `QuizConfigApiEntity`.
- В § «Dictionary.quizConfigs или отдельная domain entity?» убрать soft-hint про варианты, оставить только принятый вариант B.

Alignment v3 не дотронул 07.md — alignment v4 закроет.

### AGG-4 [решено — реверс]: новый dictionary auto-INSERT default `[translation]`

**ПРЕДЫДУЩЕЕ РЕШЕНИЕ (отменено):** «всё quiz wire в backlog». Было ошибкой триажа — пользователь говорил «UI configurator в backlog», а я расширил scope до всей wire-функциональности. Без wire в IS481 удаление `BuiltInComponent.DEFINITION` (AGG-1) делает existing definitions сиротами в квизе — это противоречит цели фичи (сохранение пользовательских данных).

**Новое решение (в IS481):**
1. **`addDictionary` auto-INSERT** default row в `quiz_configs(dictionary_id, quiz_mode, component_refs=[BuiltIn(TRANSLATION)])` для каждого `quiz_mode`. Новый словарь сразу квизит translation. Атомарность через единый transaction.
2. **Migration existing словарей** в Migration_11_12: для каждого dictionary INSERT `quiz_configs` row по факту имеющихся типов:
   - всегда translation;
   - если есть user-defined Definition (после транзита из старой `lexemes.definition` колонки, см. AGG-1) — также `UserDefined("Definition")`.
3. **Quiz runtime использует config** — wire см. AGG-5 (реверс там же).

**В backlog остаётся:** UI configurator (экран где пользователь меняет какие компоненты квиз использует). Без UI юзер не может конфиг править, дефолт `[translation]` фиксированный для новых словарей.

**Мотивация (пользовательская формулировка):** «definition мы тащим в IS481 чтобы не терять данные, уже вбитые в приложение. Для новых словарей пока только translation — UI создания компонентов всё равно нет».

### AGG-5 [решено — реверс]: `QuizConfig` lookup wire в IS481

**ПРЕДЫДУЩЕЕ РЕШЕНИЕ (отменено):** «wire в backlog вместе с AGG-4». Было следствием той же ошибки триажа — wire не отделим от цели IS481 (сохранение definitions в квизе).

**Новое решение (в IS481):**
- `QuizConfig` lookup pre-fetch в `QuizChatUseCaseImpl` / `QuizGameImpl.fetchData`. Запрос по `(dictionary_id, quiz_mode)` возвращает `component_refs`.
- Quiz session сборка: для каждого `ComponentTypeRef` из `component_refs` — резолв в `ComponentType` (через `component_types` table), потом подтянуть `component_values` лексем по этому типу. `toQuizItem(quizComponents, ...): QuizItem?` фильтрует лексемы где требуемые компоненты заполнены (graceful skip = null).
- **Контракт quiz session:** Если в `component_refs` указан тип, который в словаре отсутствует (например, конфиг ссылается на удалённый user-defined тип) — graceful skip, не crash. Это **корректность по дизайну**, не баг.

**Затрагивает:** `07_quiz_strategy.md` — выписать алгоритм lookup + сборка session явно. `06_mapping_design.md` — mapper `QuizConfigDb` → `QuizConfig` domain.

### AGG-7 [obsolete — опровергнут через verify Room source, см. AGG-11]: bundled driver подключаем точечно в IS481, 10 миграций не переписываем

**ОТМЕНЕНО.** Гипотеза «Room 2.8 имеет compat layer для `SupportSQLiteDatabase` API под bundled driver» была **не verified через source**, только через documentation reading. В prereq фиче `IS481_lexeme_component_constructor_vPrepared` sub-agent (после permission читать библиотеки) прочитал реальный Room 2.8.4 source и опроверг гипотезу: ctors mutually exclusive, compat layer для миграций под bundled driver **не работает**.

См. FlowBacklog IS481-F5 (root finding), IS481-F11 (systemic — главный systemic проёб проекта: evasion вместо `Read` real source при design-decisions про library API contract).

**Действующее решение:** **B2 восстановлен** (см. выше) — переписать все 10 миграций + migration harness. AGG-11 ниже фиксирует canonical decision.

---

### AGG-11 [obsolete — superseded by AGG-12]: переписать все 10 миграций + migration harness под bundled driver

**ОТМЕНЕНО.** AGG-11 предписывал переписать 10 existing миграций + harness под bundled driver. После реализации prereq фичи `IS481_lexeme_component_constructor_vPrepared` принято кардинальное решение **drop'нуть весь legacy migration code целиком** (никто из internal testers не имеет БД с user_version < 11, проверено через tags 0.1.0 / 0.1.5).

Актуальное решение — **AGG-12**.

См. `docs/db-migrations-history.md` для retrospective timeline.

---

### AGG-12 [решено]: existing migrations dropped, новая M11→M12 пишется directly под bundled driver

**Verify:** prereq `IS481_lexeme_component_constructor_vPrepared` infra_implement:
- 10 production migration objects (`Migration_NNN_to_MMM.kt`) **удалены**.
- Migration test infrastructure (`BaseMigration.kt`, `Schemable.kt`, `DataProvider.kt`, `MigrationFromNNtoMM.kt`, `Schema.kt`, schemable/* old entity snapshots, utils) **удалены целиком**.
- `RoomModule.provideDatabase` — `.addMigrations(...)` блок убран; добавлен `.fallbackToDestructiveMigration(dropAllTables = true)` + `Database.Callback.onDestructiveMigration(connection)` который логирует через `LexemeLogger.e` → автоматически в Crashlytics через `CrashlyticsSink` (для edge case pre-0.1.0 install).
- Verify через `tags 0.1.0` и `0.1.5` — оба имеют `database version = 11` — никакой инкремент DB version в production истории.

**Решение для IS481 main миграции M11→M12:**
- **Migration object:** написать **один** `Migration_011_to_012.kt` с `override fun migrate(connection: SQLiteConnection)` + `connection.execSQL(...)` через `androidx.sqlite.execSQL` extension. Никакого legacy `SupportSQLiteDatabase` API — directly на new API.
- **Migration test:** написать **один** `MigrationFrom11to12.kt` с `MigrationTestHelper(driver = BundledSQLiteDriver(), ...)`. Возможно потребуется минимальная version `BaseMigration.kt` helper если нужен shared setup.
- **Schema snapshots:** Room автогенерирует `schemas/me.apomazkin.core_db_impl.room.Database/12.json` через KSP при обновлении `@Database(version = 12)`.
- **Schemable / DataProvider** — не восстанавливаем. Если нужны для нового M11→M12 test — пишутся от scratch только для v11/v12 entity snapshots, не для всей истории.

**Затрагивает:** `05_migration_strategy.md` — переписать § Bundled SQLite и § Migration steps в свете «нет existing migrations, одна новая M11→M12». Убрать пункт «переписать 10 миграций». Добавить пункт «новая migration пишется directly на SQLiteConnection API».

**Scope impact для IS481 main:** **существенно сокращён** vs AGG-11. Вместо переписки 10 + harness — одна новая миграция + один тест к ней. Plus миграция M11→M12 = реальный core IS481 work (добавление `component_types` / `component_values` / `quiz_configs` таблиц + transform translation/definition).

---

### AGG-7 obsolete-приложение

Ниже сохранён оригинальный текст AGG-7 для аудита истории решений.

~~**Решение:** prereq-шаг внутри IS481 — подключить `BundledSQLiteDriver` (gradle dep `androidx.sqlite:sqlite-bundled` + `.setDriver(BundledSQLiteDriver())` в `RoomModule.kt:37`). Существующие 10 миграций **не трогаем** — Room 2.8 имеет compat layer для `SupportSQLiteDatabase` API. IS481 миграция M11→12 пишется уже на новом API (`SQLiteConnection.execSQL` extension).~~

~~**Verify procedure (защита вместо гарантии):**~~
~~1. Подключить bundled driver.~~
~~2. Прогнать существующие migration tests (`core-db-impl/androidTest`).~~
~~3. Если падают — точечная правка падающих миграций (не блок-перепиской всех).~~
~~4. Только после зелёных тестов — IS481 миграция.~~

~~**Затрагивает:** `05_migration_strategy.md` — убрать пункт «переписать 10 миграций», добавить prereq-шаг bundled driver + verify procedure.~~

### AGG-8 [решено]: verify `json_insert(..., '$[#]', ...)` синтаксиса перед M11→12

**Решение:** не доверяем плану на слово — добавляем verify step. После подключения bundled driver (AGG-7 prereq) — написать smoke test для `json_insert($, '$[#]', json_object(...))` против реальной БД на bundled SQLite. Если работает — оставляем M11→12 как в плане. Если нет — fallback на Kotlin string-building в миграции.

**Затрагивает:** `05_migration_strategy.md` — после prereq bundled driver добавить verify step «smoke test json_insert append синтаксиса» перед M11→12.

### AGG-9 [решено]: явный порядок шагов внутри Migration_11_12

**Решение:** в `05_migration_strategy.md` зафиксировать порядок шагов Migration_11_12 нумерованным списком с rationale «FK violation risk»:

1. `CREATE TABLE component_types` (с FK на dictionaries).
2. `CREATE TABLE component_values` (с FK на lexemes + component_types).
3. `seedBuiltIns(connection)` — INSERT `TRANSLATION` built-in типа для каждого существующего dictionary (FK ready после шага 1).
4. Для каждого dictionary с пользовательскими definitions: INSERT user-defined `Definition` тип в `component_types` (FK ready).
5. Transform: INSERT в `component_values` строки из старого `lexemes.definition` колонки (FK ready после шагов 3-4).
6. `ALTER TABLE lexemes DROP COLUMN definition` (если bundled SQLite поддерживает — иначе recreate-table pattern).

Шаги 3 и 4 ДОЛЖНЫ быть до шага 5, иначе FK violation на `component_values.component_type_id`.

### AGG-10 [решено]: QuizConfig в `modules/domain/lexeme` — trade-off с явным TODO на вынос

**Решение:** `QuizConfig` / `ComponentTypeRef` (sealed) остаются в `modules/domain/lexeme` для IS481 (полный stack DB → ApiEntity → Domain как schema-readiness). НО на каждом типе KDoc:

```kotlin
/**
 * TODO: вынести в `modules/domain/quiz` в рамках backlog-фичи
 * «Quiz config UX» (см. docs/Backlog.md § Срочное).
 * Сейчас живёт в lexeme domain как trade-off — не плодим
 * второй domain модуль ради 2 типов.
 */
```

Trade-off accepted: один domain module проще двух, SRP violation временный.

**После реверса AGG-4/5:** `QuizConfig` теперь **используется runtime'но** в IS481 (lookup в quiz UseCase'ах), не просто schema-readiness. Это **усиливает** обоснование trade-off (типы реально работают, не лежат мёртвым грузом), но также делает вынос в `modules/domain/quiz` более востребованным в backlog-фиче. TODO-комменты остаются.

**Затрагивает:** код IS481 — добавить KDoc на `QuizConfig`, `ComponentTypeRef`, `ComponentTypeRef.BuiltIn`, `ComponentTypeRef.UserDefined`. В backlog entry «Quiz config UX» уже надо упомянуть «создать `modules/domain/quiz` + перенести `QuizConfig`/`ComponentTypeRef` из lexeme».

### MIN-2 [решено]: чеклист — `implementation(project(":modules:domain:lexeme"))` в `core-db-api`

**Решение:** в `05_migration_strategy.md` § «API совместимости» добавить явный пункт чеклиста: «добавить `implementation(project(":modules:domain:lexeme"))` в `core/core-db-api/build.gradle.kts`». Без этого dep IS481 не соберётся (ApiEntity будет использовать `BuiltInComponent`/`ComponentTemplate`/`ComponentValueData` из domain).

Контекст: после IS482 dep не было — `LexemeApiEntity` не использовал domain типы. IS481 расширяет ApiEntity и добавляет необходимость dep edge.

### MIN-3 [решено]: явный блок про double cascade pathway в 03.md

**Решение:** в `03_database_design.md` после секции CASCADE-связей добавить:

> **Double cascade pathway:** при delete dictionary `component_values` удаляются по двум путям — через `component_types` (FK `dictionary_id → dictionaries.id` ON DELETE CASCADE → каскад на `component_type_id`) и через `words → lexemes` (FK `lexeme_id → lexemes.id` ON DELETE CASCADE). Оба пути намеренны: первый покрывает delete отдельной lexeme, второй — delete всего словаря. SQLite обрабатывает idempotent: повторное удаление одной и той же строки безопасно.

Защита: без явного комментария будущий читатель может предложить «убрать один CASCADE» как cleanup и сломать сценарий.

### MIN-4 [решено]: integration test cascade chain после `ALTER TABLE DROP COLUMN`

**Решение:** в `05_migration_strategy.md` § «Тестовый план» добавить test case:

- **Setup:** создать dictionary, word, lexeme (со старым `definition` колонкой v11), мигрировать БД на v12, добавить `component_value` через новый DAO.
- **Action:** `DELETE FROM dictionaries WHERE id = X`.
- **Assert:** `words`, `lexemes`, `component_values`, `component_types` (с FK на этот dictionary) — все удалены каскадом. Counts проверить `SELECT COUNT(*)` для каждой таблицы.

Rationale: `ALTER TABLE DROP COLUMN` либо recreate-table pattern иногда теряют FK constraints на новой таблице — без integration test упускается.

### MIN-6 [решено]: thread policy `quiz_configs.component_refs` парсинга — inline, без flowOn/cache

**Решение:** в `06_mapping_design.md` около описания mapper `QuizConfigDb.toApiEntity()` (стр. 43) добавить one-liner:

> **Thread policy:** inline в mapper, без `flowOn`/cache. Объём малый (1-5 row × несколько `ComponentTypeRef`), читается раз при quiz session start. Не путать с `ComponentValueData` thread policy для DictionaryTab (см. § Сериализация ComponentValueData) — там 3000 объектов на крупном словаре, тут на порядки меньше.

Защита от copy-paste thread policy между разными мапперами имплементором.

### MIN-7 [решено]: явный раздел Invariants в 07.md

**Решение:** в `07_quiz_strategy.md` около стр. 55 (после описания UNIQUE constraint) добавить раздел «**Invariants**»:

> **F1 — полнота config:** для каждого `dictionary_id` × каждый registered `quiz_mode` существует ровно один row в `quiz_configs`. При добавлении нового `quiz_mode` (`card`, `recall`, ...) — миграция / data layer обязан INSERT'ить default config row для всех existing dictionaries.
>
> **F5 — no N+1:** `quizComponents` подгружается **один раз** на quiz session (`getDictionaryQuizConfig(dictionaryId, quiz_mode)`) и передаётся в `toQuizItem` для каждой лексемы. Не lookup-per-lexeme.

Rationale: F1 — процедурный invariant (поведение data layer при добавлении новых режимов), DDL `UNIQUE(dictionary_id, quiz_mode)` не покрывает требование «row должен существовать», только «не больше одного». Без явного раздела имплементор может забыть полноту при добавлении `card`/`recall`.

### MIN-8 [решено]: write-mapper `QuizConfig.toApiEntity()` + DAO `upsertQuizConfig` — YAGNI в IS481

**Решение:** убрать из плана IS481:
- `QuizConfig.toApiEntity()` mapper (Domain → ApiEntity).
- `QuizConfigApiEntity.toDb()` mapper (ApiEntity → DB).
- DAO `upsertQuizConfig(...)` через mapper.

**Что остаётся в IS481:**
- **Read-mapper** `QuizConfigDb → QuizConfig` (через ApiEntity) — нужен для quiz lookup (AGG-5 wire).
- **DAO `insertDefaultQuizConfig(dictionaryId: Long, quizMode: String)`** — простой method, пишет hardcoded JSON `'[{"type":"builtin","key":"translation"}]'`. Без передачи доменного объекта.
- **Migration_11_12** — direct SQL `INSERT INTO quiz_configs ...` (без mapper).
- **`addDictionary`** атомарно: INSERT в `dictionaries` + для каждого зарегистрированного `quiz_mode` вызов `insertDefaultQuizConfig(newId, mode)` в одной транзакции.

Write-mapper появится в backlog-фиче «Quiz config UX» — когда UI configurator будет писать произвольные `QuizConfig` объекты.

**Затрагивает:** `06_mapping_design.md` — удалить `QuizConfig.toApiEntity()` / `QuizConfigApiEntity.toDb()` пункты из чеклиста, добавить пункт DAO `insertDefaultQuizConfig`. `07_quiz_strategy.md` — `addDictionary` транзакция уточнить.

### MIN-9 [решено]: `WordCardUseCaseImpl.restoreLexeme` переписать impl на generic

**Решение:** в `05_migration_strategy.md` § AGG-6 чеклист переписки callsite'ов definition wrappers добавить пункт:

> **`WordCardUseCaseImpl.restoreLexeme`** — переписать impl на generic component INSERT. Сигнатура mate API (`restoreLexeme(wordId, translation: String?, definition: String?): List<Lexeme>?`) остаётся, меняется только внутренняя реализация: INSERT lexeme + INSERT component_value translation (built-in) + если `definition != null` — INSERT component_value через user-defined "Definition" тип словаря.

Без этой точки IS481 не соберётся (compile error на удалённый `addLexemeDefinition`).

### MIN-10 [решено]: test cascade `component_types → component_values`

**Решение:** в `05_migration_strategy.md` § «Тестовый план» добавить test case:

- **Setup:** создать `component_type` (например user-defined Definition) + 2 `component_values` ссылающихся на этот type через `component_type_id`.
- **Action:** `DELETE FROM component_types WHERE id = X`.
- **Assert:** `SELECT COUNT(*) FROM component_values WHERE component_type_id = X` → 0.

Прямой CASCADE test отдельно от MIN-4 (где цепочка через dictionary). Нужен для сценария ручного DELETE component_type — например через UI редактирования компонентов (будущая фича).

### MIN-11 [решено]: synchronous cleanup `quiz_configs.component_refs` при DELETE component_type

**Решение:** оставляем JSON-storage `quiz_configs.component_refs` (обсуждали normalized альтернативу — over-engineering для текущего scope; evolution `ComponentTypeRef` не предвидится; квизов будет немного, прогнать JSON через UPDATE — приемлемо).

**F6 — referential consistency:** в `07_quiz_strategy.md` § Invariants добавить:

> **F6 — referential consistency:** после DELETE component_type, ни в одном `quiz_configs.component_refs` не остаётся ссылок на удалённый тип. Cleanup синхронный, в DAO `deleteComponentType` (одна транзакция):
> 1. SELECT все `quiz_configs` WHERE `component_refs` содержит ref на удаляемый type (через `json_each` либо в Kotlin после SELECT).
> 2. UPDATE каждого row — `json_remove(component_refs, '$[index]')` либо собрать новый JSON в Kotlin.
> 3. DELETE component_type (CASCADE на `component_values` автомат).

**В IS481 операция `deleteComponentType` отсутствует** (нет UI триггера, миграция только создаёт типы). Invariant и cleanup-контракт документируются для backlog-фичи «Quiz config UX». Не реализуем cleanup в IS481 — добавится с UI configurator.

**Затрагивает:** `07_quiz_strategy.md` — F6 в § Invariants. `Backlog.md` entry «Quiz config UX» — пункт «DAO `deleteComponentType` атомарно cleanup'ит `quiz_configs.component_refs`».

### MIN-12 [решено]: три подвопроса — UX / F4 / INSERT OR IGNORE

**MIN-12a — Empty quiz session UX:** не меняем в IS481. Существующее поведение «quiz сессия пустая → текущий fallback» сохраняется. Улучшение UX (различение «словарь пуст» vs «лексемы есть, но не подходят под config») — в backlog UI configurator.

**MIN-12b — F4 contract:** order компонентов в quiz session = позиция в JSON-массиве `quiz_configs.component_refs`. В `07_quiz_strategy.md` § Invariants добавить:

> **F4 — display order:** позиция `ComponentTypeRef` в JSON-массиве `component_refs` определяет порядок отображения компонента в quiz session. При INSERT/UPDATE сохранять order. Mapper `QuizConfigDb → QuizConfig` сохраняет порядок (JSON-array → List).

**MIN-12c — Migration idempotency `INSERT OR IGNORE`:** отклонено как FP. Room оборачивает каждую `Migration.migrate(...)` в транзакцию по умолчанию → падение даёт rollback → INSERT'ы не остаются → UNIQUE violation при повторном запуске невозможен. Defensive `OR IGNORE` без реального кейса.
