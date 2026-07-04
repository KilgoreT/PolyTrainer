# IS481 Component Constructor — Phase 2

## Задача

Phase 1 (`docs/features/IS481_component_constructor/`) реализовал базовый CRUD user-defined компонентов: create / rename / soft-delete + миграция M12→M13 + два экрана (`ComponentsManagerScreen` aggregated + `PerDictionaryComponentsScreen` scoped) + UI entry-points (Settings drill-in + DictionaryAppBar «молоток»).

Релиз phase 1 — code-complete, quality gates PASS (lint / test / build), но **не закрывает 5 пунктов** по сравнению с концептом + найденные в ходе flow technical debts. Phase 2 закрывает их одним flow.

### Что доделать (5 пунктов)

#### 1. Edit component + cardinality downgrade (concept `template_model.md` § Open Q9; checklist scenario 6)

**Что:** добавить редактирование существующего user-defined компонента — изменение `name` (есть отдельно через rename, но через единый Edit-flow), `template`, `isMulti`. Включает downgrade guard для `is_multi: true → false` при наличии лексем с count > 1.

**Acceptance:**
- Новый Msg `Msg.OpenEditDialog(typeId)` + `Msg.EditNameChange / EditTemplateChange / EditMultiToggle / SubmitEdit / CloseEditDialog` + `Msg.EditResult(epochId, outcome)`.
- Новый UseCase метод `editComponent(typeId, name, template, isMulti): EditOutcome`.
- Новый sealed `EditOutcome { Success, NameEmpty, SameScopeCollision, CrossScopeCollision, CardinalityDowngradeBlocked(impactedLexemeIds: List<Long>), Failure(cause) }`.
- Reducer: ветка `Msg.OpenEditDialog` — load type details + open dialog. Ветка `Msg.SubmitEdit` — emit `DatasourceEffect.EditComponent(epoch, typeId, name, template, isMulti)` с epoch correlation guard (parity F124/F136).
- Reducer: при `EditResult.CardinalityDowngradeBlocked(impactedLexemes)` — UI показывает preview списка проблемных лексем (top-3 inline + «Показать все» drill-in либо bottom-sheet).
- UseCase impl: SQL `SELECT type.id, lexeme_id, COUNT(*) FROM component_values WHERE component_type_id = :typeId AND removed_at IS NULL GROUP BY lexeme_id HAVING COUNT(*) > 1` для downgrade check (запускать только при `isMulti=false` И existing `type.isMulti=true`).
- Если `name` изменился — cascade `quiz_configs.component_refs` (parity с rename pattern).
- Edit `template` — **запрещён** по `template_model.md § Open Q10` (template immutable после создания). UI скрывает / disable выбор template'а; если попытка отправить changed template — UseCase возвращает `EditOutcome.TemplateImmutable`.
- `UserDefinedRowWidget` / `PerDictRowWidget` получают action Edit (icon-кнопка либо пункт меню).
- Unit-тесты UseCase: каждая ветка `EditOutcome` (Success / NameEmpty / collisions / CardinalityDowngradeBlocked / TemplateImmutable / Failure).
- Reducer-тесты: race conditions (stale epoch / closed dialog) аналогично Create/Rename (F101, F124, F136).

#### 2. Multi-dict picker scope=PerDictionaries multi-select (concept `ui_placement.md` § «Создать новый»)

**Что:** в Create-диалоге Manager-экрана юзер выбирает scope — radio «На все словари» (Global) / «На конкретные» (PerDictionaries). При выборе «На конкретные» — chip-list со словарями для multi-select. Сейчас scope hardcoded по entry-point'у (Settings → Global only, AppBar → current dict only).

**Acceptance:**
- В `ComponentsManagerScreenState` (Manager-экран) добавить `availableDictionaries: List<DictionaryListEntry>` (`id: Long`, `name: String`). Reducer infrastruct — initial load через `DatasourceEffect.LoadDictionaries` либо subscribe pattern через `FlowHandler`.
- В Msg добавить `Msg.DictionariesLoaded(list)` либо subscribe-driven обновление.
- В `CreateDialogState`: расширить `scope: Scope` поле (уже есть в state, но reducer не реагирует на UI change) — теперь `Msg.CreateScopeChange(scope)` живой, не dead.
- В `ComponentsManagerUseCase`: добавить `flowDictionaries(): Flow<List<DictionaryEntry>>` метод (либо через existing `DictionaryApi` extension).
- В `CreateComponentDialog` UI: radio «На все / На конкретные» + при выбранном «На конкретные» — chip-list dictionaries с multi-select toggle.
- При submit scope = `Scope.Global` либо `Scope.PerDictionaries(selectedIds)` — UseCase создаёт N rows (по одной per dictionary).
- В PerDict-экране (`PerDictionaryComponentsScreen`) — **scope hardcoded** в `PerDictionaries([currentDict])` остаётся как есть (single-dict context, не multi-select).
- UX edge: если выбрано «На конкретные» но 0 dicts selected — submit-кнопка disabled либо `CreateOutcome.NameEmpty`-like ошибка («Выберите хотя бы один словарь»).
- Unit-тесты: scope-change Msg → state update; submit с empty PerDictionaries → ошибка; submit с N dicts → N rows в БД (mock).

#### 3. `:modules:widget:component_widgets/` наполнение + shared dialogs (concept `typed_views.md` § «Где жить»)

**Что:** наполнить Tier 2 widget-модуль (сейчас пустой scaffold). Вынести 6 дублированных widgets из обоих screen-модулей. Реализовать per-template architecture (`TextWidget`, `ComponentBlock`, template resolver) — даже для MVP с одним template'ом, как фундамент под будущие composite templates.

**Acceptance:**
- В `modules/widget/component_widgets/src/main/java/me/apomazkin/component_widgets/`:
  - `dialogs/CreateComponentDialog.kt` — принимает **примитивы + callbacks** (name, isMulti, scope, availableDictionaries, nameError, isCreating, onNameChange, onMultiToggle, onScopeChange, onSubmit, onDismiss). НЕТ зависимости на mate-state.
  - `dialogs/RenameComponentDialog.kt` — аналогично (name, nameError, isRenaming, onNameChange, onSubmit, onDismiss).
  - `dialogs/DeleteComponentConfirmDialog.kt` — аналогично (impact, isLoadingImpact, isDeleting, onConfirm, onDismiss).
  - `dialogs/EditComponentDialog.kt` — новый для пункта 1.
  - `widgets/UserDefinedRowWidget.kt` (либо `ComponentRowWidget.kt` с параметризацией) + `PerDictRowWidget.kt` — вынести.
  - `widgets/ComponentsEmptyStateWidget.kt` + `widgets/CreateComponentFab.kt` — вынести.
  - `templates/TextWidget.kt` — `@Composable fun TextWidget(values: TextValues)` per-template composable.
  - `templates/ComponentBlock.kt` — `@Composable fun ComponentBlock(name: String, content: @Composable () -> Unit)` wrapper.
  - `templates/ComponentByTemplate.kt` — resolver `@Composable fun ComponentByTemplate(template: ComponentTemplate, values: TemplateValues)` с exhaustive `when`.
- В `components_manager` и `per_dictionary_components` — удалить дублированные widgets, импортить из `:modules:widget:component_widgets`.
- В `app/build.gradle.kts` + screen-модулях — добавить `implementation(project(":modules:widget:component_widgets"))`.
- Compose preview для каждого вынесенного widget'а — есть либо у себя, либо в `:modules:core:ui` patterns.
- Build PASS, все existing tests PASS без изменений (только перенос файлов + import paths).

#### 4. `RenameOutcome.BuiltInProtected` conflation для soft-deleted

**Что:** `renameComponent(typeId)` для soft-deleted типа возвращает `RenameOutcome.BuiltInProtected` — misleading. Тот же баг возможен в `DeleteOutcome` и (новый) `EditOutcome`.

**Acceptance:**
- В `RenameOutcome` добавить variant `Removed` (либо `NotFound` — выбрать на business_contract). UseCase impl: `type.systemKey != null → BuiltInProtected`; `type.removedAt != null → Removed`; иначе обычный rename.
- В `DeleteOutcome` проверить аналогичную ветку — soft-delete уже удалённого типа должно возвращать `Removed`, не `BuiltInProtected`.
- В новом `EditOutcome` (см. пункт 1) — изначально различать `BuiltInProtected` vs `Removed`.
- Reducer ветки: `Msg.RenameResult(Removed)` / `Msg.DeleteResult(Removed)` / `Msg.EditResult(Removed)` — UI snackbar «Компонент удалён» + close dialog (не показывать как «нельзя редактировать встроенный»).
- Unit-тесты: для каждого outcome — case с soft-deleted type → `Removed`.

#### 5. Feature-scoped tag `###ComponentConstructor###` + логи в Migration_012_to_013 + DAO cascade

**Что:** добавить feature-tag для adb logcat фильтрации фича-событий (manual smoke verify). Добавить логи в Migration_012_to_013 (счётчики per step) и DAO cascade (`QuizConfigDao.updateComponentRefs`, prefs reset).

**Acceptance:**
- В shared logger (либо новый `:modules:core:logger` extension) — добавить tag constant `LogTags.COMPONENT_CONSTRUCTOR = "###ComponentConstructor###"`.
- В `ComponentsManagerUseCaseImpl` / `PerDictionaryComponentsUseCaseImpl` — каждый public метод (`createUserDefinedComponent`, `renameComponent`, `softDeleteComponent`, `previewDeletionImpact`, `editComponent`) пишет лог с tag = `###ComponentConstructor###` + scope + result outcome.
- В `Migration_012_to_013.kt` — каждый из 9 шагов пишет лог:
  - rename `remove_date → removed_at` (rows affected).
  - add `is_multi` / `created_at` / `updated_at` (rows backfilled).
  - add `component_values.removed_at` (rows count).
  - drop indices (success/failure).
  - JSON rewrite text (rows rewritten / skipped).
  - JSON rewrite image (rows rewritten / skipped).
  - long_text → text consolidation (rows affected).
- В `QuizConfigDao.updateComponentRefs` — лог при rename/soft-delete cascade (component_type id + dictionary id + before/after refs count).
- В UseCase prefs reset (`resetQuizPickerPrefsBestEffort`) — лог при reset либо при ошибке reset.
- **Решить** на этапе scope_analysis / business_contract: оставить параллельно module-scoped tags (`ComponentsManager` / `PerDictComponents`) либо снести в пользу `###ComponentConstructor###` only. Recommended: сохранить module-tags для debug-логов, добавить `###ComponentConstructor###` как фича-маркер для smoke (две оси: что и зачем).
- Manual verify (вне scope flow): `adb logcat | grep '###ComponentConstructor###'` даёт stream фича-событий после миграции и при CRUD operations.

### Связи между пунктами (порядок реализации)

Рекомендуемый порядок внутри flow:
1. **Сначала пункт 3** (refactor + widget module fundament) — потому что пункты 1 и 2 добавляют новые UI (Edit dialog, multi-dict picker) которые должны лечь в shared module, не в дубликаты.
2. **Затем пункты 1 и 4 совместно** — Edit-flow + `Removed` variant в sealed (тесно связаны на UseCase / Reducer / sealed уровне).
3. **Затем пункт 2** — multi-dict picker в Create dialog (уже shared).
4. **В конце пункт 5** — logger pass проходит по всем UseCase методам + DAO, естественно делается после того как методы добавлены.

Это hint для scope_analysis / sub-flow ordering. flow conductor сам решит финальную последовательность.

## Контекст

### Концептуальная база (не меняется)

`docs/features/IS481_component_constructor/concept/`:
- `ui_placement.md` — UI entry-points + Create-form требования.
- `template_model.md` — domain types + миграция + cardinality / soft-delete.
- `typed_views.md` — modules layout (Tier 2 widget module).
- `deletion_concept.md` — soft-delete cascade.

### Связанные документы

- **Полный список расхождений phase 1 vs concept** — `docs/features/IS481_component_constructor/__plan_vs_reality.md`.
- **Phase 1 фича** — `docs/features/IS481_component_constructor/`.

### Критерии готовности всей фичи

1. Все 5 пунктов done — concept compliance gaps закрыты, technical debts закрыты.
2. `:app:lintDebug` PASS.
3. `:app:testDebugUnitTest` PASS (включая новые тесты пунктов 1, 2, 3, 4).
4. `:app:assembleDebug` PASS.
5. Manual smoke на эмуляторе (или device):
   - Создать global компонент из Settings → видим во всех словарях.
   - Создать per-dict компонент (выбор 2 из 3 словарей) из Settings → видим в выбранных 2.
   - Edit existing per-dict компонента — name + isMulti — без downgrade.
   - Edit existing component is_multi=true → false при наличии лексемы с 2 values — блокировка с показом impactedLexemes.
   - Edit existing component template — UI блокирует выбор (immutable).
   - Soft-delete + recreate с тем же именем (после soft-delete) — Success.
   - Soft-delete и попытка rename удалённого — `Removed` (не misleading `BuiltInProtected`).
   - `adb logcat | grep '###ComponentConstructor###'` — видим стрим фича-событий.
6. Instrumented migration tests запускаются: `MigrationFrom12to13` (если ещё не запускался в phase 1) + новые scenarios если phase 2 трогает миграцию (вряд ли).
7. Published spec `docs/features-spec/component-constructor.md` обновлён — отражает Edit / multi-dict / shared widget module / новые outcomes.
8. Backlog records на 5 пунктов закрыты в `docs/Backlog.md` (либо отмечены как done).

### Out of scope phase 2

- Recovery UI (корзина / архив для soft-deleted компонентов) — отдельная фича.
- Background TTL hard-delete — отдельная фича.
- Per-dictionary disable built-in (например словарь без translation) — отдельная фича.
- Composite templates (`QuoteWithSourceValues`, `ImageWithCaptionValues`, etc.) — отдельная фича, но фундамент (`ComponentBlock`, resolver) — в пункте 3 текущей фазы.
- Soft-delete для `dictionaries` / `words` / `lexemes` — отдельные фичи (см. `concept/deletion_concept.md`).

_model: claude-opus-4-7[1m]_
