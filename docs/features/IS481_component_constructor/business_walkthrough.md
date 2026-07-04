# Business walkthrough: IS481 component_constructor

Факты о текущем состоянии бизнес-слоя (UseCase, domain entities, API contracts) для последующего шага `business_contract`. Только что есть в коде на момент M12.

## 1. Где живут типы компонента сейчас

После IS482 lexeme-domain unification — всё в `modules/domain/lexeme/`:

- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentTemplate.kt:7-16` — enum `ComponentTemplate(val key: String)` с тремя entries: `TEXT("text")`, `LONG_TEXT("long_text")`, `IMAGE("image")`. `fromKey(key: String): ComponentTemplate` — non-null с fallback на `TEXT` (`:14`).
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentType.kt:16-24` — data class с полями `id: ComponentTypeId`, `systemKey: BuiltInComponent?`, `dictionaryId: Long?`, `name: String?`, `template: ComponentTemplate`, `position: Int`, `removeDate: Date? = null`. Inline value class `ComponentTypeId(val id: Long)` (`:6`).
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentValueData.kt:9-13` — sealed M12-формы: `TextValue(text)`, `LongTextValue(text)`, `ImageValue(uri)`. Variant выбирается по `template` parent-`ComponentType` (`:5-6`).
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentValue.kt:11-16` — data class с `id: ComponentValueId`, `lexemeId: LexemeId`, `type: ComponentType` (full embedded), `data: ComponentValueData`.
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/BuiltInComponent.kt:11-18` — enum с единственным entry `TRANSLATION("translation")`. `fromKey` — nullable (`:16`). После AGG-1 `definition` мигрировал в user-defined per-dictionary тип `name="Definition"` (`:7-8`).
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentTypeRef.kt:15-21` — sealed: `BuiltIn(val key: BuiltInComponent)`, `UserDefined(val name: String)`. Используется в `QuizConfig.componentRefs`. `ComponentType.toRef()` (`:32-35`) — error при user-defined без name.
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/QuizConfig.kt:13-17` — domain config квиза, упорядоченный список `ComponentTypeRef`.
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt:28-35` — основная entity. `components: List<ComponentValue>` — source of truth (IS481, AGG-6).
- `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/LexemeBuiltInExt.kt:7-8` — `Lexeme.builtIn(key: BuiltInComponent): ComponentValue?` lookup helper.

**Отсутствуют в текущем коде** (всё новое для IS481):

- `Primitive` sealed / `Field` / `PrimitiveType` enum / `TemplateValues` sealed — нигде нет (`grep` пусто).
- `isMulti` / `is_multi` field — нигде нет (`grep` пусто).
- M13 типы (verified отсутствие).

## 2. Какой data-API сейчас обслуживает компоненты

Единый интерфейс `CoreDbApi.LexemeApi` в `core/core-db-api/src/main/java/me/apomazkin/core_db_api/CoreDbApi.kt:81-164`. Отдельного `ComponentApi` interface нет. Компонент-методы (текущие, M12):

| Метод | Сигнатура (file:line) | Назначение |
|---|---|---|
| `addLexemeWithBuiltInComponent` | `CoreDbApi.kt:93-98` | Atomic INSERT lexeme + write_quiz + первый built-in component_value |
| `addLexemeWithUserDefinedComponent` | `CoreDbApi.kt:105-110` | Atomic INSERT lexeme + write_quiz + первый user-defined component_value (lookup по `(dict, name, system_key=NULL)`); null если type не найден |
| `addLexemeWithComponents` | `CoreDbApi.kt:117-121` | Atomic compound INSERT lexeme + write_quiz + N component_values (MIN-9; restore-path) |
| `addComponentValue` | `CoreDbApi.kt:123-127` | INSERT single component_value (existing lexeme) |
| `updateComponentValue` | `CoreDbApi.kt:129-132` | UPDATE existing value |
| `deleteComponentValue` | `CoreDbApi.kt:138` | Hard DELETE, возвращает количество оставшихся components у лексемы (caller cascade lexeme delete при 0) |
| `getComponentTypes(dictionaryId)` | `CoreDbApi.kt:140` | Suspend list всех типов для словаря (built-in global + per-dict user-defined) |
| `getQuizConfig(dictionaryId, quizMode)` | `CoreDbApi.kt:142-145` | Конфиг квиза |
| `addLexemeWithTranslation` | `CoreDbApi.kt:150-154` (`@Deprecated`) | Shim на `addLexemeWithBuiltInComponent` |
| `updateLexemeTranslation` | `CoreDbApi.kt:157-160` (`@Deprecated`) | Legacy translation update shim |

**CRUD методов для самого `ComponentType` в `CoreDbApi.LexemeApi` НЕТ.** Read-only: только `getComponentTypes`. Нет `createUserDefinedComponent`, `renameComponent`, `softDeleteComponent`, `previewDeletionImpact`. Нет `flowUserDefinedTypes` / `flowTypesForDictionary` на API-уровне (хотя на DAO-уровне есть — см. § 5).

Imports на API-уровне (`CoreDbApi.kt:5-16`) — `ComponentTypeApiEntity`, `ComponentValueData`, `ComponentTypeRef`, `BuiltInComponent`. Domain enum-ы пробрасываются прямо через API (по A1/MIN-2 data знает domain).

## 3. API DTO entities

- `core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/ComponentTypeApiEntity.kt:13-21` — `id: Long`, `systemKey: BuiltInComponent?`, `dictionaryId: Long?`, `name: String?`, `template: ComponentTemplate`, `position: Int`, `removeDate: Date? = null`. Поля 1:1 с `ComponentType` (domain).
- `core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/ComponentValueApiEntity.kt:11-16` — `id`, `lexemeId`, `type: ComponentTypeApiEntity` (full embedded), `data: ComponentValueData`.
- `core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/QuizConfigApiEntity.kt:8-13` — `id`, `dictionaryId`, `quizMode`, `componentRefs: List<ComponentTypeRef>`.
- `core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/LexemeApiEntity.kt:22-29` — `id`, `components: List<ComponentValueApiEntity>`, `wordClass`, `options`, `addDate`, `changeDate`. `translation` / `definition` поля удалены (AGG-6, `:17-21`).

## 4. CoreDbApiImpl — реальный data path

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt`. Релевантные методы в `LexemeApiImpl` (`:209-400`):

- `addLexemeWithBuiltInComponent` (`:237-251`) — lookup `componentTypeDao.getBySystemKey(systemKey.key)` (`:243`), error если не найден; затем `wordDao.addLexemeWithComponents(...)` атомарно.
- `addLexemeWithUserDefinedComponent` (`:253-274`) — lookup в `getTypesForDictionary(dictionaryId)` по `systemKey == null && name == name` (`:260`); `null` если не найден (с error log).
- `addLexemeWithComponents` (`:276-306`) — resolve каждого `ComponentTypeRef` в `typeDb.id`, потом atomic insert.
- `addComponentValue` (`:308-320`) — простой `componentValueDao.insert(ComponentValueDb(...))`.
- `updateComponentValue` (`:322-329`) — fetch by id + `componentValueDao.update(existing.copy(value = data.toJson()))`. Без timestamps.
- `deleteComponentValue` (`:331-336`) — **hard delete** `componentValueDao.delete(...)` + `countForLexeme(lexemeId)` для caller-cascade.
- `getComponentTypes` (`:338-340`) — `componentTypeDao.getTypesForDictionary(dictionaryId).map { it.toApiEntity() }`.

**`@Deprecated` shim** для translation:
- `addLexemeWithTranslation` (`:351-361`) — делегирует в built-in.
- `updateLexemeTranslation` (`:363-399`) — legacy lookup-then-{delete|insert|update} branch (`:373-398`).

## 5. DAO — `ComponentTypeDao` / `ComponentValueDao`

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/dao/ComponentTypeDao.kt:18-64`:

| Метод | Подпись |
|---|---|
| `flowTypesForDictionary(dictId): Flow<List<ComponentTypeDb>>` | `:31`. Active+global+per-dict. **`WHERE remove_date IS NULL` уже фильтрует** (`:28`). ORDER `system_key NULL ASC, position ASC` (built-in first). |
| `getTypesForDictionary(dictId): List<...>` | `:41`. Suspend. Тот же query (`:38`). |
| `getBuiltInTypes(): List<...>` | `:44`. `WHERE system_key IS NOT NULL AND remove_date IS NULL` (`:43`). |
| `getBySystemKey(key): ComponentTypeDb?` | `:47`. `WHERE system_key = ? AND remove_date IS NULL` (`:46`). |
| `getById(id): ComponentTypeDb?` | `:50`. **БЕЗ фильтра `remove_date IS NULL`** (`:49`) — single-row lookup. |
| `insert(type): Long` | `@Insert`, `:53`. |
| `update(type)` | `@Update`, `:56`. |
| `softDelete(id, now): Int` | `:63`. `UPDATE ... SET remove_date = :now WHERE id = :id AND system_key IS NULL` — защита от soft-delete'а built-in типов SQL-уровнем. |

**`softDelete` существует, но не вызывается ни одним UseCase** (`grep softDelete` — только DAO + placeholder ComponentsManagerUseCaseImpl и заглушка в ComponentsManagerUseCase). Orphan API.

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/dao/ComponentValueDao.kt:13-43`:

| Метод | Подпись |
|---|---|
| `getForLexeme(lexemeId): List<...>` | `:16`. БЕЗ фильтра `removed_at` (колонки на M12 нет). |
| `getById(id): ...?` | `:19`. |
| `getForLexemeAndType(lexemeId, typeId): ...?` | `:24-27`. |
| `insert(value): Long` | `@Insert`, `:30`. |
| `update(value)` | `@Update`, `:33`. |
| `delete(id): Int` | **hard** `DELETE WHERE id = :id` (`:36`). |
| `deleteByLexemeAndType(lexemeId, typeId): Int` | **hard** (`:38-39`). |
| `countForLexeme(lexemeId): Int` | `:41-42`. |

`QuizConfigDao` — `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/dao/QuizConfigDao.kt:18-44`. Методы: `getByDictionaryAndMode` (`:20`), `getByDictionary` (`:23`), `insert` (`:26`), `update` (`:29`), `insertDefaultQuizConfig` (`:36-43`, hardcoded JSON `[{"type":"builtin","key":"translation"}]`). **Нет точечного `componentRefs`-UPDATE** для cleanup/cascade.

## 6. Soft-delete: какие колонки, какие фильтры

`removeDate` объявлен в трёх Room entity, активно используется только в одной:

| Entity | Файл / line | Тип колонки | Активно используется |
|---|---|---|---|
| `ComponentTypeDb.removeDate` | `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/ComponentTypeDb.kt:48` | `Date?` (`@ColumnInfo("remove_date")`) | **да** — `softDelete` DAO method + `WHERE remove_date IS NULL` фильтр на всех active read-queries в DAO. |
| `SampleDb.removeDate` | `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/SampleDb.kt:18` | `Date?` с TODO «удалить?» | **нет** — нигде не пишется и не читается (orphan field). |
| `HintDb.removeDate` | `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/HintDb.kt:15` | `Date?` | **нет** — `hints` таблица полностью dead branch (нет DAO методов в проекте). |

**`ComponentValueDb` не имеет `removed_at` / `removeDate` колонки.** Файл `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/entity/ComponentValueDb.kt:42-48` — только `id`, `lexemeId`, `componentTypeId`, `value`. Удаление values — только hard DELETE.

Convention `WHERE remove_date IS NULL` применяется в `ComponentTypeDao` queries (`:28`, `:38`, `:43`, `:46`), но НЕ применяется в `getById` (`:49`).

## 7. Mapper layer — текущий формат JSON

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/mapper/ComponentValueDataJson.kt`:

- M12-формат (`:18-29`): `{"v":1,"text":"..."}` для `TextValue` / `LongTextValue`, `{"v":1,"uri":"..."}` для `ImageValue`. Дискриминатора нет — variant выбирается по `template` parent-`ComponentType`.
- Парсер `String.toComponentValueData(template)` (`:37-44`) — exhaustive `when` по `ComponentTemplate`, **throws `JSONException`** при невалидном JSON / отсутствующих ключах (без fail-soft).

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/mapper/ComponentTypeRefJson.kt:21-59` — JSON для `componentRefs`. `BuiltIn` → `{"type":"builtin","key":"..."}`; `UserDefined` → `{"type":"user","name":"..."}`. Defensive parser возвращает `emptyList()` при corrupt JSON (`:42-58`).

Mapping entity → API:
- `ComponentTypeDb.toApiEntity()` — `ComponentTypeDb.kt:51-59` — straight rebind; `templateKey` через `ComponentTemplate.fromKey` (non-null fallback на TEXT).
- `ComponentValueWithType.toApiEntity()` — `ComponentValueWithType.kt:20-29` — straight rebind, calls `value.value.toComponentValueData(typeApi.template)` для JSON parse.
- `LexemeDbEntity.toApiEntity()` — `LexemeDbEntity.kt:34-41` — `componentValueListDb.map { it.toApiEntity() }`. **Без фильтрации `removedAt`** (колонки нет).

## 8. Существующие UseCase, использующие компонент-API

Найдены через `grep -rn UseCase`:

### Реальные UseCase (с DI-injected `CoreDbApi.LexemeApi`)

- **`WordCardUseCaseImpl`** — `app/src/main/java/me/apomazkin/polytrainer/di/module/wordCard/WordCardUseCaseImpl.kt`. Полный CRUD на компоненты лексемы. Релевантные методы (file:line):
  - `addLexemeWithBuiltInComponent` (`:82-126`) — branch new-lexeme vs existing-lexeme (`:88-121`); resolveCurrentDictionaryId через `prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)` (`:304-318`); existing-lexeme path делает UPDATE-or-INSERT в зависимости от наличия value по type (`:115-119`).
  - `addLexemeWithUserDefinedComponent` (`:128-176`) — аналогичная логика. Lookup type по `name` (`:151`).
  - `addComponentValue` (`:178-188`).
  - `updateComponentValue` (`:190-209`) — TODO про DAO-level `getLexemeIdByComponentValueId` (`:198-203`), возвращает `null` потому что нет lookup.
  - `deleteComponentValue` (`:211-231`) — branch `remaining > 0` (`null`, TODO) vs `LexemeCascadeRemoved` (`:215-227`).
  - `deleteDefinitionComponent` (`:233-246`) — shim для user-defined "Definition" компонента.
  - `restoreLexeme` (`:257-300`) — atomic compound restore через `addLexemeWithComponents`.
  - Helper `deleteLexemeComponentBy` (`:345-370`) — универсальный delete-with-cascade pattern.
  - Error handling pattern: `try { ... } catch (e: Exception) { logger.e(...); null }` (`:42-52`, `:67-78`, `:122-126`, etc.).
  - Suspend (`suspend fun`) — да, все методы.
  - Return пэттерн: `T?` либо `sealed Result` (`RemoveComponentResult`, `RemoveTranslationResult`). Не `Flow`.

- **`WordCardUseCase`** interface — `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/deps/WordCardUseCase.kt:11-79`. Чисто component-CRUD методы перечислены `:38-78`. Sealed result types `RemoveTranslationResult` (`:81-84`) и `RemoveComponentResult` (`:86-89`).

### Placeholder UseCase для конструктора

Уже созданы как stubs в infra-фазе IS481:

- `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/deps/ComponentsManagerUseCase.kt:10` — `interface ComponentsManagerUseCase` — **пустой** (комментарий «реальные методы финализируются на business_contract»).
- `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/deps/PerDictionaryComponentsUseCase.kt:9` — `interface PerDictionaryComponentsUseCase` — **пустой**.
- `app/src/main/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerUseCaseImpl.kt:13-15` — `class ComponentsManagerUseCaseImpl @Inject constructor()` — пустой constructor, нет dependencies.
- `app/src/main/java/me/apomazkin/polytrainer/di/module/perdictionarycomponents/PerDictionaryComponentsUseCaseImpl.kt:11-13` — пустой constructor.

Screen files тоже placeholders:
- `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/ComponentsManagerScreen.kt:25-31` — `Box { Text("TODO: UI in ui_implement") }`.
- `modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/ComponentsManagerViewModel.kt:13-23` — пустой `AssistedInject` ViewModel без `MateReducer`.

### Другие UseCase (не component-CRUD, но релевантны как UseCase convention)

- `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/deps/SettingsTabUseCase.kt:5-8` — пример минималистичного UseCase.
- `modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/deps/DictionaryAppBarUseCase.kt:6-10` — пример с `Flow<...>` (реактивный) и suspend `changeDict`.

## 9. UseCase conventions (вытащено из существующих)

Из `WordCardUseCaseImpl`:

- `suspend fun` для всех методов CRUD; не `Flow` (Flow только в DictionaryAppBarUseCase).
- Возврат `T?` — `null` означает ошибку (логируется через `logger.e(tag, message)`).
- Sealed `Result` иерархия для сложных ветвлений (cascade vs partial delete).
- `try { ... } catch (e: Exception) { logger.e(...); null }` — exception wrapping pattern везде.
- Dependency injection через primary constructor: `CoreDbApi.WordApi`, `CoreDbApi.DictionaryApi`, `CoreDbApi.TermApi`, `CoreDbApi.LexemeApi`, `PrefsProvider`, `LexemeLogger` (`:25-32`).
- Logger через `me.apomazkin.logger.LexemeLogger`, теги в локальном `LogTags` объекте per-module.
- Mapping DB API → domain через `toDomain()` extension (`me.apomazkin.polytrainer.mapper.toDomain`).
- Helper-методы для повторяющихся patterns (`deleteLexemeComponentBy`, `:345-370`).

## 10. Migration / seed infrastructure (relevant context)

- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/Database.kt:32` — `version = 12`.
- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/di/module/RoomModule.kt:50` — `.addMigrations(Migration_011_to_012)`. `fallbackToDestructiveMigration(dropAllTables = true)` (`:51`). `Callback.onCreate` вызывает `seedBuiltIns(connection)` (`:53-57`).
- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/SeedBuiltIns.kt:23-31` — INSERT OR IGNORE для built-in TRANSLATION. SQL: `(system_key, dictionary_id, name, template_key, position, remove_date) VALUES ('translation', NULL, NULL, 'text', 0, NULL)`.
- `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/migrations/Migration_011_to_012.kt:31-247` — M11→M12. Создаёт `component_types`, `component_values`, `quiz_configs`. Поля `component_types`: `id`, `system_key`, `dictionary_id`, `name`, `template_key`, `position`, `remove_date` (без `is_multi`, без `created_at`/`updated_at`).

## 11. Picker / Prefs (cleanup context для soft-delete)

- `modules/datasource/prefs/src/main/java/me/apomazkin/prefs/QuizPickerPrefKey.kt:10` — helper `quizPickerPrefKey(dictionaryId: Long): String = "quiz_picker_dict_$dictionaryId"`.
- `modules/datasource/prefs/src/main/java/me/apomazkin/prefs/PrefsProvider.kt:78-96` — методы `getStringByRawKey(key: String): String?` (`:78`), `getStringFlowByRawKey(key: String): Flow<String?>` (`:82`), `setStringByRawKey(key: String, value: String?)` (`:87`). **Метода `findKeysWithPrefix(prefix)` НЕТ** — для iteration через все pref-ключи требуется внешний source dictionary IDs.

## 12. quiz_configs.component_refs — формат и матчинг

- Storage (JSON массив): `[{"type":"builtin","key":"translation"},{"type":"user","name":"Definition"}]`. Encode/decode — `ComponentTypeRefJson.kt:21-59`.
- Matching на quiz-сессии: `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/quiz/QuizGameImpl.kt:527-530`:
  ```kotlin
  private fun ComponentValue.matchesRef(ref: ComponentTypeRef): Boolean = when (ref) {
      is ComponentTypeRef.BuiltIn -> type.systemKey == ref.key
      is ComponentTypeRef.UserDefined -> type.systemKey == null && type.name == ref.name
  }
  ```
  Идентификация user-defined компонента только по `name`. При rename component_type cascade в `quiz_configs.component_refs` обязателен — иначе silent quiz breakage.

## 13. Settings tab — навигационная точка для конструктора (для контекста)

- `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/logic/Message.kt:10` — `Msg.OpenComponentsManager : Msg` (уже добавлен).
- `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/SettingsNavigationEffect.kt:9` — `data object OpenComponentsManager : SettingsNavigationEffect` (уже добавлен).
- `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/SettingsNavigator.kt:9` — `fun openComponentsManager()` (уже добавлен).
- `modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/logic/SettingsTabReducer.kt:34-36` — обработка `Msg.OpenComponentsManager → SettingsNavigationEffect.OpenComponentsManager`.

Навигационная инфраструктура для главного экрана конструктора уже подведена; UI-точка нажатия (виджет в Settings tab) — не проверял в этом walkthrough.

## Вердикт

Аналог: **частично найден**

В проекте есть:

1. **Domain entity ComponentType + ComponentTypeRef + Lexeme.components** (M12 после IS482 unification) — структура хранения user-defined компонентов уже существует, рабочая, покрыта тестами и используется в WordCard / Quiz chat.
2. **DAO-уровень CRUD для `component_types`** — `insert`, `update`, `softDelete(id, now)`, `flowTypesForDictionary`, `getTypesForDictionary`. Soft-delete protected от built-in на SQL-уровне (`WHERE system_key IS NULL`). Active queries уже фильтруют `WHERE remove_date IS NULL`.
3. **Migration `createUserDefinedDefinitionTypes`** в `Migration_011_to_012.kt:166-179` создаёт первый user-defined ComponentType "Definition" для словарей с definition data — паттерн программного INSERT в `component_types` существует и работает.
4. **WordCardUseCaseImpl** — полноценный UseCase, использующий component-API: `addLexemeWith*Component`, `addComponentValue`, `updateComponentValue`, `deleteComponentValue`. Demonstrates convention (suspend / `T?` return / try-catch-log / sealed Result).
5. **`@Deprecated` shim API** для translation — образец постепенного перехода CoreDbApi контракта (как делать domain rewrite через shim в одной фиче).

В проекте **отсутствует**:

1. **CRUD user-defined ComponentType** на UseCase / API уровне — нет `createUserDefinedComponent(name, template, isMulti, scope)`, `renameComponent(typeId, newName)`, `softDeleteComponent(typeId)`, `previewDeletionImpact`. DAO `softDelete` — orphan (не вызывается ни одним UseCase).
2. **`is_multi` концепция и `insertSingleSafe` cardinality protection** — M12 имеет жёсткий UNIQUE `(lexeme_id, component_type_id)` для всех типов (`ComponentValueDb.kt:39`), переход к мягкой cardinality (`is_multi=false` через UseCase) не реализован.
3. **`TemplateValues` sealed + typed views per template** — есть только M12 `ComponentValueData.TextValue/LongTextValue/ImageValue`. `Primitive`, `Field`, `PrimitiveType` — отсутствуют.
4. **Schema-aware JSON parser** — `String.toComponentValueData(template)` (`ComponentValueDataJson.kt:37-44`) throw'ит `JSONException`, fail-soft через Crashlytics + null return — отсутствует.
5. **Soft-delete для `component_values`** — нет колонки `removed_at` в `ComponentValueDb`, hard DELETE везде. Каскад «soft-delete component_type → JOIN-фильтр на `parent.removed_at IS NULL` для values» — отсутствует (только FK CASCADE на hard delete).
6. **`quiz_configs.component_refs` cleanup / rename cascade** — нет точечного UPDATE метода в `QuizConfigDao`. При soft-delete или rename component_type silent quiz breakage.
7. **Cross-scope uniqueness invariant** — `(dictionary_id, name)` UNIQUE на M12 защищает только same-scope коллизию. Активные global "X" + per-dict "X" в одном словаре могут существовать одновременно — identity-ambiguity для `ComponentTypeRef.UserDefined`.
8. **Prefs cleanup при soft-delete component_type** — нет логики reset `quiz_picker_dict_<id>` ссылок на удалённый ref. `PrefsProvider` не имеет scan-by-prefix API.

**Резюме:** структура хранения и DAO готовы примерно на 50%; **бизнес-слой (UseCase / API CRUD) — отсутствует**. Существующие UseCase (`WordCardUseCaseImpl`) дают шаблон conventions (suspend, T?, try-catch-log, sealed Result). Placeholder-классы `ComponentsManagerUseCase` / `PerDictionaryComponentsUseCase` уже заведены — туда писать новые методы.
