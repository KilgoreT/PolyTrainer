# Business walkthrough — IS481 component_constructor phase 2

Факт-чек реального кода в 5 областях phase 2. Решений нет — только `file:line` ссылки. Все пути относительно корня репозитория PolyTrainer.

## 1. Edit component flow — текущий CRUD (Create / Rename / Delete)

### Domain outcomes (`modules/domain/lexeme/`)

Все sealed outcomes для CRUD по user-defined component_types живут в `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/`:

- `CreateOutcome.kt:10-16` — `sealed interface CreateOutcome { Success(List<ComponentType>) / NameEmpty / SameScopeCollision / CrossScopeCollision / Failure(cause) }`.
- `RenameOutcome.kt:9-16` — `Success(ComponentType) / NameEmpty / SameScopeCollision / CrossScopeCollision / BuiltInProtected / Failure(cause)`. `Removed` варианта **нет** — single axis BuiltInProtected перегружает soft-deleted кейс.
- `DeleteOutcome.kt:9-13` — `Success(DeletionImpact) / BuiltInProtected / Failure(cause)`. `Removed` **нет** — повторный soft-delete уже-removed type возвращает `BuiltInProtected` (см. `CoreDbApiImpl.kt:613-615` — type получается через `getById` без фильтра removed_at, но system_key check возвращает BuiltInProtected; для already-removed soft-delete фактически no-op).

**EditOutcome / EditComponentOutcome** — отсутствуют. Аналогов нет.

### Data-layer outcomes (`core/core-db-api/`)

`core/core-db-api/src/main/java/me/apomazkin/core_db_api/entity/ComponentOutcomeApiEntity.kt:13-33`:
- `CreateComponentOutcome { Success(List<ComponentTypeApiEntity>) / SameScopeCollision / CrossScopeCollision }` — без NameEmpty / Failure (валидация и try-catch — на UseCaseImpl).
- `RenameComponentOutcome { Success(ComponentTypeApiEntity) / SameScopeCollision / CrossScopeCollision / BuiltInProtected }` — без Removed.
- `SoftDeleteComponentOutcome { Success(DeletionImpact) / BuiltInProtected }` — без Removed.

### UseCase interfaces (screen modules)

`modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/deps/ComponentsManagerUseCase.kt:20-65`:
- `flowAllUserDefinedTypes()` (line 26)
- `createUserDefinedComponent(name, template, isMulti, scope): CreateOutcome` (line 37-42)
- `renameComponent(typeId, newName): RenameOutcome` (line 48-51)
- `previewDeletionImpact(typeId): DeletionImpact?` (line 57)
- `softDeleteComponent(typeId): DeleteOutcome` (line 64)
- **`editComponent` — отсутствует. `flowDictionaries` — отсутствует.**

`modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/deps/PerDictionaryComponentsUseCase.kt:22-46`:
- зеркало (line 31-45) + `flowComponentsForDictionary(dictionaryId): Flow<PerDictionarySnapshot>` (line 29).
- `editComponent` — отсутствует.

### UseCaseImpl (`app/.../di/module/`)

`app/src/main/java/me/apomazkin/polytrainer/di/module/componentsmanager/ComponentsManagerUseCaseImpl.kt`:
- `flowAllUserDefinedTypes` (line 48-58) — map snapshot API → domain (`it.toDomain()`).
- `createUserDefinedComponent` (line 60-81) — pattern: `trim + isBlank → NameEmpty` → `try { when (val r = lexemeApi.createUserDefinedComponent(...)) { ... } } catch (CancellationException) { throw } catch (Exception) { logger.e; Failure(e) }`. `when` exhaustive на `CreateComponentOutcome.{Success/SameScopeCollision/CrossScopeCollision}` (line 70-74).
- `renameComponent` (line 83-101) — тот же pattern + `BuiltInProtected` branch (line 93).
- `previewDeletionImpact` (line 103-112) — без `when` (read-only).
- `softDeleteComponent` (line 114-135) — особенность F127: prefs reset вынесен за outer try/catch (line 117-119, 128-134).

`app/src/main/java/me/apomazkin/polytrainer/di/module/perdictionarycomponents/PerDictionaryComponentsUseCaseImpl.kt:29-65`:
- write-методы (line 46-64) **делегируют** на `sharedCrud: ComponentsManagerUseCase` (line 31, инжект DIP через `@Binds` в `ComponentsManagerModule`, см. line 26-28).
- `flowComponentsForDictionary` (line 34-44) — собственный subscribe + mapping.

### API→domain mapping pattern

Везде одинаково: `when (val r = lexemeApi.xxx(...)) { is XxxComponentOutcome.Success -> XxxOutcome.Success(...); XxxComponentOutcome.SameScopeCollision -> XxxOutcome.SameScopeCollision; ... }` — exhaustive enumeration. `Failure` строится в catch блоке (НЕ из API-варианта). См. CMUseCaseImpl `:69-74`, `:89-94`, `:128-134`.

### Reducer Create/Rename/Delete-ветки (`modules/screen/components_manager/.../mate/Reducer.kt`)

- Open\*Dialog ветки с epoch bump + mutual-exclusion закрытия других диалогов: `Reducer.kt:50-63` (Create), `:172-194` (Rename), `:285-311` (Delete).
- Submit\*: guard on `is*ing` + dialog null + name.isBlank() → emit DatasourceEffect: `:94-114` (SubmitCreate), `:207-225` (SubmitRename), `:341-355` (ConfirmDelete).
- \*Result handling: stale epoch check (F136) + per-variant snackbar/dialog mutation + dialog-null fallback snackbar (F101 race close-during-flight): `:116-169` (CreateResult), `:227-282` (RenameResult), `:357-375` (DeleteResult).

`OpenCreateDialog` создаёт `CreateDialogState(epochId = newEpoch)` (default name="" / TEXT template / isMulti=false / **scope=Global**) — `Reducer.kt:54-62`, `State.kt:60-67`.

PerDictionary mirror в `modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/mate/Reducer.kt:59-75` — отличие: `scope = Scope.PerDictionaries(listOf(state.dictionaryId))` preselect.

### Msg families

`modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/Msg.kt:18-79`:
- Create: `OpenCreateDialog/CloseCreateDialog/CreateNameChange/CreateTemplateChange/CreateMultiToggle/CreateScopeChange/SubmitCreate/CreateResult(epochId, outcome)` (line 26-37).
- Rename: `OpenRenameDialog(typeId)/CloseRenameDialog/RenameTextChange/SubmitRename/RenameResult(epochId, outcome)` (line 40-44).
- Delete: `OpenDeleteConfirm(typeId)/CloseDeleteConfirm/ImpactPreviewLoaded/ImpactPreviewFailed/ConfirmDelete/DeleteResult(epochId, outcome)` (line 47-61).
- `UiMsg.Snackbar(text)` (line 88).

`Edit*` family Msg — **отсутствует**. `DictionariesLoaded` Msg — **отсутствует**.

### DatasourceEffect

`modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/DatasourceEffect.kt:18-48`:
- `CreateComponent(epochId, name, template, isMulti, scope)` (line 20-26).
- `RenameComponent(epochId, typeId, newName)` (line 28-32).
- `LoadImpact(typeId)` (line 34).
- `SoftDeleteComponent(epochId, typeId)` (line 36-39).
- `LoadAllUserDefinedTypes` (line 47, F163 re-subscribe trigger).

`EditComponent` effect — **отсутствует**. `SubscribeDictionaries` — **отсутствует**.

### DatasourceEffectHandler

`modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/DatasourceEffectHandler.kt:42-109`:
- Pattern: `withContext(Dispatchers.IO) { try { when (effect) { ... } } catch (Cancellation) { throw } catch (Exception) { logger.e; build Failure outcome msg } }` (line 43-106).
- Каждый case строит `Msg.XxxResult(epochId, outcome = useCase.xxx(...))`.
- На catch — отдельный exhaustive `when (effect)` (line 91-105) который строит Failure-typed Result msg.

## 2. Multi-dict scope picker — текущий Scope в Create-flow

### Scope sealed (`modules/domain/lexeme/Scope.kt`)

`modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Scope.kt:12-15`:
```
sealed interface Scope {
    data object Global : Scope
    data class PerDictionaries(val ids: List<Long>) : Scope
}
```

Инвариант `global ⊥ per-dict` зафиксирован комментарием (line 8-11).

### Передача scope в Create-flow

**Manager-экран** (`ComponentsManagerScreen`):
- `CreateDialogState.scope: Scope = Scope.Global` — default (`State.kt:65`).
- `Msg.CreateScopeChange(val scope: Scope)` — `Msg.kt:31`. Reducer ветка `:88-92`: `state.copy(createDialog = dlg.copy(scope = message.scope))`.
- `SubmitCreate` упаковывает `scope` в `DatasourceEffect.CreateComponent(... scope = dlg.scope)` — `Reducer.kt:108-110`.
- UI слой: единичный `Scope` value на dialog state. **Multi-select chip-list для PerDictionaries(N)** в текущем UI/state — отсутствует. `availableDictionaries: List<DictionaryApiEntity>` поле в state — отсутствует.

**Per-dict экран** (`PerDictionaryComponentsScreen`):
- `OpenCreateDialog` preselect: `scope = Scope.PerDictionaries(listOf(state.dictionaryId))` — `per_dictionary_components/.../mate/Reducer.kt:65-67`. Только single dictionary id, по сути single-select.

### Data-layer Scope handling

`CoreDbApiImpl.kt:465-522` — `createUserDefinedComponent`:
- two-prong SELECT на `Scope.Global` / `Scope.PerDictionaries` (line 471-484).
- INSERT N rows: `Scope.PerDictionaries -> scope.ids.map { ... }` (line 502-513), wrapped in `database.withTransaction { ... }` (line 515-520).
- N-rows API уже работает — но triggers только если caller передал `scope.ids` с N>1 элементов. **Никто в коде сейчас не передаёт N>1** (Manager: scope=Global default + ChangeScope без UI-multi-select; PerDict: hardcoded `listOf(dictionaryId)`).

### flowDictionaries pattern — где использовать

`DictionaryApi.flowDictionaryList(): Flow<List<DictionaryApiEntity>>` — `core/core-db-api/.../CoreDbApi.kt:65`. Возвращает `DictionaryApiEntity` (`core/core-db-api/.../entity/DictionaryApiEntity.kt:5-12`: id, numericCode, name, addDate, changeDate, deleteDate).

Existing callers `flowDictionaryList()` (нужны как pattern):
- `app/.../di/module/splash/SplashUseCaseImpl.kt:13` — `dictionaryApi.flowDictionaryList().transform { ... }`.
- `app/.../di/module/widget/DictionaryAppBarUseCaseImpl.kt:19, 34` — два места.
- `app/.../di/module/dictionary/DictionaryUseCaseImpl.kt:38-39` — `flowDictionaryList(): Flow<List<DictionaryListItem>>` + delegate + map.

Components-manager сейчас `DictionaryApi` **не инжектит**: `ComponentsManagerUseCaseImpl` (`:42-46`) принимает только `lexemeApi`, `prefsProvider`, `logger`. Аналогично `PerDictionaryComponentsUseCaseImpl` (`:29-32`) — только `lexemeApi` + `sharedCrud`. Аналогов «нового FlowHandler с DictionaryApi для component-screens» в коде нет.

## 3. Removed semantics — где проверка `removed_at IS NOT NULL`

### Data layer (`CoreDbApiImpl`)

`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt`:

**`renameComponentType` (line 524-561):**
- `existing = componentTypeDao.getById(typeId) ?: return BuiltInProtected` (line 528-529) — null returns BuiltInProtected (collapsed semantics).
- `if (existing.systemKey != null) return BuiltInProtected` (line 530).
- `if (existing.removedAt != null) return BuiltInProtected` (line 531) — **soft-deleted ловится через BuiltInProtected**, отдельного Removed варианта нет.
- Collision check (line 535-550).
- UPDATE в transaction (line 553-556).

**`softDeleteComponentType` (line 610-639):**
- `getById` → null → BuiltInProtected (line 613-614).
- `systemKey != null → BuiltInProtected` (line 615).
- **`removedAt` check отсутствует** — повторный soft-delete уже-removed type выполнит UPDATE через `componentTypeDao.softDelete` (DAO-level SQL: `WHERE id = :id AND system_key IS NULL` — line 68 в `ComponentTypeDao.kt`; removed_at просто перезапишется новой `now`). `SoftDeleteComponentOutcome.Success(impact)` вернётся.

**`updateComponentValue` (line 397-408):**
- `check(type.removedAt == null) { "Cannot update ComponentValue for soft-deleted type ${...}" }` (line 400-402) — runtime crash на write через generic component API. Это **assertion**, не typed outcome.

**`previewDeletionImpact` (line 581-608):**
- `if (type.systemKey != null) return null` (line 583). **removed_at NOT checked** — preview работает на soft-deleted type (но `valueCount` посчитан на active values: `countActiveByTypeId(typeId)` line 586, `dictionaryIdsForTypeId(typeId)` line 587 — оба считают active, но active не предполагает removed parent type).

### DAO-level

`ComponentTypeDao.kt`:
- `getById` (line 54-55) — **БЕЗ фильтра** removed_at (explicit comment line 17-21: read-only views / lookup; write paths должны explicit-проверять).
- `softDelete` (line 68-69) — `WHERE id = :id AND system_key IS NULL`, без removed_at guard.
- `renameUserDefined` (line 75-76) — `WHERE id = :id AND system_key IS NULL`, без removed_at guard.
- Все `findActive*` / `countActive*` / `flow*` методы добавляют `removed_at IS NULL` (line 32, 42, 48, 51, 87, 101, 114, 125, 139).

### API→domain mapping pattern для outcome variants

`ComponentsManagerUseCaseImpl.kt` — exhaustive `when (api) { ... }` без default ветки:
- `createUserDefinedComponent` (line 69-74) — 3 ветки → 3 domain.
- `renameComponent` (line 89-94) — 4 ветки (включая BuiltInProtected) → 4 domain.
- `softDeleteComponent` (line 128-134) — 2 ветки → 2 domain (+ side-effect `resetQuizPickerPrefsBestEffort` на Success).

`Removed` ветки в маппинге сейчас **нет** — API outcomes не содержат `Removed` variant, и domain outcomes не содержат `Removed`.

## 4. flowDictionaries / AllUserDefinedTypesFlowHandler template

### Существующий FlowHandler pattern (template для нового DictionariesFlowHandler)

`modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/AllUserDefinedTypesFlowHandler.kt:22-59`:
- Класс: `@Inject constructor(private val useCase: ComponentsManagerUseCase, private val logger: LexemeLogger) : MateFlowHandler<Msg, Effect>`.
- `override var job: Job? = null` (line 27).
- `subscribe(scope, send)` (line 42-58): сохраняет scope/send (F163 для re-subscribe), `launch { useCase.flowAllUserDefinedTypes().catch { send(Msg.TypesLoadFailed(e)) }.collectLatest { send(Msg.TypesLoaded(snapshot)) } }`.
- `runEffect(effect, consumer)` (line 33-40): обрабатывает `DatasourceEffect.LoadAllUserDefinedTypes` (re-subscribe trigger F163) — `unsubscribe(); subscribe(...)`.

Assisted-вариант с `dictionaryId` параметром (для PerDict): `modules/screen/per_dictionary_components/.../mate/ComponentsForDictionaryFlowHandler.kt:27-70`:
- `@AssistedInject` + `@Assisted private val dictionaryId: Long` (line 27-28).
- `@AssistedFactory interface Factory { fun create(dictionaryId: Long): ComponentsForDictionaryFlowHandler }` (line 66-69).

Альтернативный простой pattern (без F163): `modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/list/DictionaryListFlowHandler.kt:12-33` — `dictionaryUseCase.flowDictionaryList().collectLatest { send(DictionaryListMsg.DictionariesLoaded(list)) }` (line 18-23).

### DictionaryApi inject pattern

Параметр в UseCaseImpl constructor:
- `app/.../di/module/widget/DictionaryAppBarUseCaseImpl.kt:15` — `private val dictionaryApi: CoreDbApi.DictionaryApi`.
- `app/.../di/module/dictionary/DictionaryUseCaseImpl.kt:20` — same.
- `app/.../di/module/splash/SplashUseCaseImpl.kt:10` — same.
- `app/.../di/module/quiztab/QuizTabUseCaseImpl.kt:10` — same.

DI-wiring `CoreDbApi.DictionaryApi` идёт через провайдер; уже работает в проекте — добавить аналогичный constructor-param в `ComponentsManagerUseCaseImpl` тривиально.

`DictionariesFlowHandler` — **аналогов нет** (в Components-flow), но template = `AllUserDefinedTypesFlowHandler` + DictionaryApi+UseCase delegate + новый Msg вариант `Msg.DictionariesLoaded(list)`.

## 5. F138 mutual exclusion — где invariant зафиксирован в Reducer

### Manager Reducer

`modules/screen/components_manager/src/main/java/me/apomazkin/components_manager/mate/Reducer.kt`:

**Epoch correlation:** `state.nextEpoch + 1` инкремент на каждый Open\*Dialog: line 53 (Create), 178 (Rename), 294 (Delete). Stale Result check: `if (dlg != null && dlg.epochId != message.epochId) state to emptySet()` — line 118-120 (CreateResult), 229-230 (RenameResult), 359-360 (DeleteResult). См. `Msg.kt:34-37` (CreateResult несёт `val epochId: Long`), `:44` (RenameResult), `:61` (DeleteResult). Effect-уровень correlation: `DatasourceEffect.kt:20-39` — все write effects несут epochId.

**Mutual exclusion (F138)** — каждая Open\*Dialog ветка явно обнуляет другие диалоги + сбрасывает in-flight флаги (F140):
- `OpenCreateDialog` (`Reducer.kt:50-63`): `createDialog = CreateDialogState(...), renameDialog = null, deleteConfirm = null, isCreating = false, isRenaming = false, isDeleting = false`.
- `OpenRenameDialog` (`Reducer.kt:172-194`): `renameDialog = RenameDialogState(...), createDialog = null, deleteConfirm = null, isCreating = false, isRenaming = false, isDeleting = false`.
- `OpenDeleteConfirm` (`Reducer.kt:285-311`): `deleteConfirm = DeleteConfirmState(...), createDialog = null, renameDialog = null, isCreating = false, isRenaming = false, isDeleting = false`. Дополнительный narrow F132: `state.deleteConfirm?.typeId == message.typeId → no-op` (line 289-291).

Инвариант `[shape]` зафиксирован в State.kt KDoc `:16-23`: `createDialog/renameDialog/deleteConfirm: одновременно открыт не более одного диалога (enforced в Reducer через mutual-exclusion в Open*Dialog ветках — F138)`.

**Reducer-тесты F138** (`modules/screen/components_manager/src/test/java/me/apomazkin/components_manager/mate/ComponentsManagerReducerTest.kt`):
- `OpenCreateDialog closes other dialogs (F138)` — line 234-253.
- `OpenRenameDialog closes other dialogs (F138)` — line 513-531.
- `OpenDeleteConfirm closes other dialogs (F138)` — line 723-741.

### Per-dict Reducer (зеркало)

`modules/screen/per_dictionary_components/src/main/java/me/apomazkin/per_dictionary_components/mate/Reducer.kt`:
- `OpenCreateDialog` mutual exclusion: line 59-75 (с preselect scope).
- `OpenRenameDialog`: line 178-199.
- `OpenDeleteConfirm`: line 286-311.

Pattern идентичен Manager Reducer; те же F138/F140 invariants. KDoc invariants в `per_dictionary_components/.../mate/State.kt:23-29`.

### Гард ветки для async Result

Помимо stale-epoch (F136), все \*Result имеют race close-during-flight (F101) fallback: `if (dlg == null) snackbar fallback`. Примеры — `Reducer.kt:127-137` (CreateResult.NameEmpty с dlg==null fallback). См. также F124 stale-typeId для `ImpactPreviewLoaded/Failed` (line 316-339).

`Edit-ветка` для F138 invariant — **отсутствует** (нет EditDialog в State, нет OpenEditDialog Msg, нет mutual-exclusion закрытия из других Open\*).

## Дополнительные факты (полнота контекста)

### Shared LogTags.COMPONENT_CONSTRUCTOR
`modules/core/logger/src/main/java/me/apomazkin/logger/LogTags.kt:3-5` — `object LogTags { const val COMPONENT_CONSTRUCTOR: String = "###ComponentConstructor###" }`. Уже создан, но **в коде не используется** (grep по `LogTags.COMPONENT_CONSTRUCTOR` за пределами самого файла даёт 0 callsites в текущем impl-коде; per-module tags `ComponentsManager` / `PerDictComponents` используются в UseCaseImpl и FlowHandlers).

### Per-module LogTags
- `modules/screen/components_manager/.../LogTags.kt:10-12` — `COMPONENTS_MANAGER = "ComponentsManager"`.
- `modules/screen/per_dictionary_components/.../LogTags.kt:9-11` — `PER_DICT_COMPONENTS = "PerDictComponents"`.

### ComponentTemplate (`fromKey` fail-soft)
`modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/ComponentTemplate.kt:11-31`:
```
enum class ComponentTemplate(val key: String) {
    TEXT("text"), IMAGE("image"),
    ...
    companion object { fun fromKey(key: String): ComponentTemplate? = entries.firstOrNull { it.key == key } }
}
```

### Migration M12→M13 (для будущего logging)
`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/migrations/Migration_012_to_013.kt` — существует (не читал содержимое, но `02_scope.md § migration_logging` фиксирует 9 атомарных шагов F020).

### QuizConfigDao.updateComponentRefs
`core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/dao/QuizConfigDao.kt:48-49`:
```
@Query("UPDATE quiz_configs SET component_refs = :newRefs WHERE id = :id")
suspend fun updateComponentRefs(id: Long, newRefs: String)
```
Используется внутри cascade-helper в `CoreDbApiImpl.cascadeRenameInQuizConfigs` (line 567-579) — приватный helper рядом с `renameComponentType`. Зеркальная cascade-логика для soft-delete inline в `softDeleteComponentType` (line 626-635) — **дублирование** (filtered-refs JSON-collect; не extract'нут в shared helper).

### DictionaryApi не инжектится в Components-flow
`ComponentsManagerUseCaseImpl.kt:42-46` constructor: `lexemeApi: CoreDbApi.LexemeApi, prefsProvider: PrefsProvider, logger: LexemeLogger`. Для phase 2 потребуется добавить `dictionaryApi: CoreDbApi.DictionaryApi`.

### Reducer extension-функции (snapshot mapping)
`modules/screen/components_manager/.../mate/State.kt:109-126` — `internal fun UserDefinedTypesSnapshot.toRows(): List<UserDefinedRow>`. Аналогично per-dict `State.kt:117-127` — `toPerDictRows()`. Шаблон mapping snapshot → UI rows.

## Вердикт

Аналог: **частично найден**.

- Create/Rename/Delete-ветки — **полный аналог** (Reducer / Msg / Effect / EffectHandler / UseCase / UseCaseImpl / outcome-mapping pattern). Edit-ветку шить по шаблону Rename + добавить cardinality-downgrade preview state ветку.
- F138 mutual exclusion — **полный аналог** в трёх Open\*Dialog ветках; добавить `OpenEditDialog` в эту же серию (включить `editDialog = null` во все три существующих Open\*, добавить новую Open ветку с симметричной reset-другими-диалогами + `isEditing = false`).
- Scope picker — **частичный аналог**: `Scope` sealed + `CreateScopeChange` Msg + data-layer N-rows транзакция уже работают; **multi-select UI state / chip-list / `availableDictionaries` поле — отсутствуют**. `flowDictionaries` API в `DictionaryApi.flowDictionaryList()` есть, но в Components-UseCase не подключен.
- Removed semantics — **не выделена**. В коде сейчас soft-deleted type на rename ловится через `BuiltInProtected` (`CoreDbApiImpl.kt:531`); softDelete для already-removed работает no-op через DAO `WHERE system_key IS NULL` (без removed_at guard); generic `updateComponentValue` крашит на `check()` (line 400-402). Domain/API sealed outcomes `Removed` variant — отсутствует. Маппинг `removed_at IS NOT NULL → Removed` — отсутствует.
- DictionariesFlowHandler — **не существует** (для Components-flow); template для копирования: `AllUserDefinedTypesFlowHandler` (subscribe + collect + F163 re-subscribe).

_model: claude-opus-4-7[1m]_
