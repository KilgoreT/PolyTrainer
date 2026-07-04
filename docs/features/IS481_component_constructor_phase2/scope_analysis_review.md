
## Итерация 1 (2026-06-22T16:44:45-06:00)

### F001 [architect] minor

**Description:** `EditComponentOutcome` planned как отдельный файл `entity/EditComponentOutcome.kt`, но existing convention в `core-db-api/entity/` держит все component outcomes в одном файле `ComponentOutcomeApiEntity.kt` (Create/Rename/SoftDelete вместе) — нарушает file-locality convention.

**Status:** approved

**Verdict:** ComponentOutcomeApiEntity.kt действительно держит Create/Rename/SoftDelete вместе, и план «NEW entity/EditComponentOutcome.kt» нарушает эту file-locality.

### F002 [architect] minor

**Description:** Формулировка "наполнение пустого `:modules:widget:component_widgets/build.gradle.kts`" в sub-flow Infrastructure фактически неверна — build.gradle.kts уже содержит android{} конфиг и deps на theme/ui/lexeme/core-resources; "наполнение" сводится к опциональному добавлению compose-tooling для preview, что стоит явно отразить.

**Status:** approved

**Verdict:** build.gradle.kts уже содержит android{} и deps на theme/ui/lexeme/core-resources, поэтому формулировка «наполнение пустого build.gradle.kts» в § Infrastructure фактически неверна.

### F003 [architect] minor

**Description:** Open question 3 предлагает положить `EditOutcome` в `modules/domain/lexeme` как best-guess, но затронутые файлы дублируют это решение как факт ("**NEW** `EditOutcome.kt`") — open question с best-guess vs уже зафиксированный NEW-файл создают двойную истину; либо снять Q3, либо пометить файл как «контингентный».

**Status:** approved

**Verdict:** Open Q3 ставит вопрос с best-guess о месте EditOutcome, но в § Затронутые файлы уже зафиксировано NEW EditOutcome.kt в modules/domain/lexeme — двойная истина.

### F004 [architect] minor

**Description:** `RenameOutcome` / `DeleteOutcome` получают новый `Removed` variant, но `CreateOutcome` — нет; обоснование («Create не оперирует existing types, Removed недостижим») семантически верно, однако в артефакте это нигде не зафиксировано — будущий читатель спросит «почему асимметрия». Стоит явный one-liner rationale в Замысле задачи либо как Open Q.

**Status:** approved

**Verdict:** Removed добавлен в Rename/Delete но не в Create, и в артефакте нет one-liner rationale про асимметрию (Create не работает с existing type).

### F005 [qa_engineer] critical

**Description:** Cardinality downgrade SELECT в § Data использует `removed_at IS NOT NULL` фильтр на `component_values` — не описано, существует ли вообще колонка `removed_at` в `component_values` после M12→M13; если её нет, SQL упадёт в runtime, а тест-сценария на это нет.

**Status:** rejected

**Verdict:** removed_at в component_values существует с момента M12→M13 (рабочая база на M13, не меняется в phase 2), что фиксирует сам task.md в п.5 («add component_values.removed_at» в Migration_012_to_013 logs).

### F006 [qa_engineer] critical

**Description:** В Open Q5 push через FlowHandler выбран best-guess, но не описан edge case «диалог открыт + dictionary удалён out-of-band, причём он уже выбран в chip-list» — что с `Scope.PerDictionaries([deletedId])` на submit? Submit с stale id → silent corruption или error? Тест-сценарий отсутствует.

**Status:** approved

**Verdict:** Open Q5 (push subscribe) и Q9 (submit disabled) не описывают edge case «выбранный chip dictionary удалён out-of-band» — реальный race на submit/state-cleanup не зафиксирован.

### F007 [qa_engineer] critical

**Description:** Edit с одновременной soft-delete того же компонента (race) — `EditOutcome.Removed` описан как возврат, но не описан сценарий: пользователь открыл EditDialog, в параллельном tab удалили компонент, нажал Submit → ожидание UI? Snackbar + close + удаление из списка? Reducer-тест не сформулирован.

**Status:** approved

**Verdict:** EditOutcome.Removed описан как возможный результат, но scenario «параллельная soft-delete пока EditDialog открыт» и UI-реакция (close dialog + snackbar?) явно не прописан, хотя такой же случай для Rename/Delete упомянут.

### F008 [qa_engineer] critical

**Description:** Тест-список § Tests не включает Reducer-тесты на mutual exclusion Edit ↔ Create / Edit ↔ Rename / Edit ↔ Delete (F138 invariant), несмотря на упоминание «mutual-exclusion с Create/Rename/Delete (F138)» в Reducer-аспекте — пропущенный coverage.

**Status:** approved

**Verdict:** Reducer-аспект явно упоминает «mutual-exclusion с Create/Rename/Delete (F138)», но § Tests не содержит соответствующих кейсов Edit↔Create/Rename/Delete.

### F009 [qa_engineer] minor

**Description:** «**DELETE** ... в обоих модулях (после переноса в shared)» — нет acceptance/тест-критерия «нет orphan-import'ов и нет дублей composables после удаления». Воспроизводимость cleanup-шага не проверяема детерминированно (build pass ≠ нет stale файлов в git).

**Status:** rejected

**Verdict:** Шаг scope_analysis — классификация затронутости, а acceptance-критерии cleanup-step и orphan-import чек — задача sub-flow / business-contract / тестового шага, не scope.

### F010 [qa_engineer] minor

**Description:** Cardinality downgrade preview «top-3 impacted lexemes inline + "Показать все"» — не указано детерминированное правило сортировки top-3 (по id / по последнему updated / по имени lexeme?); UI-тест на конкретные «top-3» будет flaky без зафиксированного ORDER BY.

**Status:** approved

**Verdict:** Артефакт фиксирует top-3 preview как UI-фичу без правила сортировки, что делает поведение нестабильным для future test/spec — open question 7 это место упустил.

### F011 [qa_engineer] minor

**Description:** Open Q9 «submit disabled при `PerDictionaries(emptyList())`» — не описан переход Global → PerDictionaries (мгновенно disabled до первого chip-выбора?) и обратно; тест-сценарий state-transition не зафиксирован.

**Status:** rejected

**Verdict:** State-transition Global↔PerDictionaries — это design detail UI-sub-flow; scope_analysis уже зафиксировал аспект multi_dict_scope_picker и Open Q9 как открытый — глубже не его уровень.

### F012 [qa_engineer] minor

**Description:** § Tests не упоминает тест на маппинг API `Removed` (новые ветки в `RenameComponentOutcome` / `SoftDeleteComponentOutcome`) → domain `RenameOutcome.Removed` / `DeleteOutcome.Removed` в UseCaseImpl; только `editComponent` ветки и «`Removed` в rename / softDelete» одной строкой без явных acceptance-условий.

**Status:** approved

**Verdict:** § Tests перечисляет домейн-ветки Edit, но маппинг API Removed → domain Removed в UseCaseImpl для Rename/SoftDelete явно не упомянут, хотя сам маппинг указан как затронутый код.

## Итерация 2 (2026-06-22T17:10:35-06:00)

### F013 [architect] critical

**Description:** § Data impl не упоминает `core/core-db-impl/CoreDbApiImpl.kt` (где живёт `LexemeApiImpl.renameComponentType` с collision checks + cascade + transaction), а перечисляет только `ComponentTypeDao.kt` / `QuizConfigDao.kt` / Migration — оркестрация `editComponentType` (downgrade SELECT + collision + cascade + soft-deleted check + withTransaction) должна жить там же, иначе слой пропущен.

**Status:** approved

**Verdict:** LexemeApiImpl.renameComponentType в CoreDbApiImpl.kt — реальное место orchestration (collision/cascade/transaction), § Data impl его не упоминает, только DAO + Migration.

### F014 [architect] minor

**Description:** Open Q5 (где положить `LogTags.COMPONENT_CONSTRUCTOR`) и § Logger в § Затронутые файлы одновременно фиксируют файл как NEW и оставляют best-guess по локации — та же двойная истина, что F003 закрыл для `EditOutcome` (либо снять Q5, либо пометить файл как «контингентный»).

**Status:** approved

**Verdict:** § Logger фиксирует файл как NEW, а Open Q5 одновременно держит best-guess о локации — та же double-truth, что F003 закрыл для EditOutcome.

### F015 [architect] minor

**Description:** API entity `EditComponentOutcome` в § Затронутые файлы перечисляет только `CardinalityDowngradeBlocked` явно, остальные варианты (Success / NameEmpty / SameScopeCollision / CrossScopeCollision / TemplateImmutable / BuiltInProtected / Removed / Failure) даны только на domain-уровне — для file-locality (F001) и mapping-тестов F012 явный перечень вариантов API outcome нужен.

**Status:** approved

**Verdict:** API outcome EditComponentOutcome в § Затронутые файлы перечисляет только CardinalityDowngradeBlocked, остальные варианты только на domain — для file-locality (F001) и mapping-тестов нужен явный API перечень.

### F016 [architect] minor

**Description:** EditDialog widget API в § UI / Widgets не фиксирует scope-immutability invariant (§ Аспекты говорит «в MVP Edit не меняет scope»), что важно для callbacks-набора shared dialog — пропуск scope-callback должен быть явно отражён в списке callbacks, иначе sub-flow может случайно завести scope в shared API.

**Status:** rejected

**Verdict:** scope-immutability уже зафиксирован в аспекте dictionary_chip_staleness ("в MVP Edit не меняет scope — пункт фиксирует invariant"); перечисление того каких callback'ов НЕТ — sub-flow design detail.

### F017 [qa_engineer] critical

**Description:** Артефакт убрал `template` из Edit-сигнатур (`editComponentType(typeId, name, isMulti)` и Msg family без `EditTemplateChange`), хотя task.md acceptance §1 явно требует `editComponent(typeId, name, template, isMulti)` + `Msg.EditTemplateChange` + `EditOutcome.TemplateImmutable` — теперь невозможно покрыть тест-кейс `TemplateImmutable` (Submit с changed template), который сам же артефакт включает в § Tests.

**Status:** approved

**Verdict:** артефакт убрал template из editComponent + EditTemplateChange Msg, противоречит task.md §1 acceptance (template-параметр + EditTemplateChange + TemplateImmutable outcome) — делает TemplateImmutable недостижимым.

### F018 [qa_engineer] critical

**Description:** `cardinality_downgrade_guard` аспект не фиксирует precondition «check запускается ТОЛЬКО при actual downgrade transition (`new.isMulti=false AND current.isMulti=true`)» — task.md §1 acceptance это требует. Без этой формулировки тестируемость провалена: нет негативных кейсов «edit isMulti false→true НЕ триггерит downgrade SELECT», «edit только name НЕ триггерит downgrade SELECT».

**Status:** approved

**Verdict:** формулировка "is_multi: true → false blocked при наличии..." имплицитна; task.md §1 explicitly требует "запускать только при isMulti=false И existing type.isMulti=true" — для тестируемости precondition должен быть явным.

### F019 [qa_engineer] minor

**Description:** Тесты на маппинг `Removed` → domain для Rename/SoftDelete добавлены (F012 закрыт), но симметричные reducer-тесты `whenRenameResultRemoved_thenDialogClosed_andRemovedSnackbarEmitted` / `whenDeleteResultRemoved_...` отсутствуют — есть только parity-тест для Edit (F007). UI-реакция на новый Rename/Delete `Removed` variant остаётся непокрытой на уровне reducer.

**Status:** approved

**Verdict:** parity-тест на close-on-Removed есть только для Edit; симметричные whenRenameResultRemoved_/whenDeleteResultRemoved_ для Rename/Delete (где Removed добавлен F004) отсутствуют.

### F020 [qa_engineer] minor

**Description:** `migration_logging` аспект перечисляет лог-точки в Migration_012_to_013 как 5 групп, но реальная миграция имеет 9 атомарных шагов (renameComponentTypesRemoveDate / addComponentTypesNewColumns / dropUniqueComponentTypesDictName / addComponentValuesNewColumns / dropUniqueComponentValuesLexemeType / createComponentValuesLexemeIdIndex / consolidateLongTextTemplateKey / rewriteTextJson / rewriteImageJson). Tests/smoke verify по task.md §5 acceptance перечисляют 7 точек — coverage неоднозначен.

**Status:** approved

**Verdict:** артефакт группирует логи в 5 групп ("rename, ADD columns, DROP indices, JSON rewrite text/image, long_text"), task.md §5 acceptance требует per-step coverage 9 шагов — coverage неоднозначен.

### F021 [qa_engineer] minor

**Description:** `dictionary_chip_staleness` аспект покрывает только удаление dictionary out-of-band, но не rename: если выбранный chip dict переименован пока CreateDialog открыт — `Msg.DictionariesLoaded(updated)` принесёт новый name для того же id, и chip должен отрендериться с новым именем. Reducer-тест на «whenChipDictionaryRenamedOutOfBand_thenChipLabelUpdated» отсутствует.

**Status:** rejected

**Verdict:** аспект dictionary_chip_staleness покрывает stale selection (id отсутствует); rename — это display-label concern, автоматически решается push-subscribe pattern (Flow обновляет name) — sub-flow design detail.

## Итерация 3 (2026-06-22T17:31:31-06:00)

### PASS [architect]

### F022 [qa_engineer] critical

**Description:** F018 negative test cases (`whenEditUpgradesIsMulti_thenNoDowngradeSelect`, `whenEditOnlyName_thenNoDowngradeSelect`) размещены в § Tests под `ComponentsManagerReducerTest` / `PerDictionaryComponentsReducerTest`. Однако precondition «downgrade SELECT не запускается» — это инвариант на UseCaseImpl/data-уровне (task.md: «запускать только при isMulti=false И existing type.isMulti=true» в UseCase impl SQL). Reducer эмитит `DatasourceEffect.EditComponent`, никаких SELECT не делает. Тесты «handler не вызывает downgrade SELECT» по определению UseCaseImplTest, а не ReducerTest — тестируемость нарушена.

**Status:** approved

**Verdict:** precondition «не вызывает downgrade SELECT» — handler/UseCaseImpl-инвариант; Reducer его проверить не может (эмитит DatasourceEffect), тесты ошибочно размещены в ReducerTest вместо UseCaseImplTest.

### F023 [qa_engineer] minor

**Description:** § Аспекты `cardinality_downgrade_guard` фиксирует ORDER BY `component_values.updated_at DESC, lexeme_id ASC` для deterministic top-3 preview, но не указывает обработку случая когда `impactedLexemeIds.size <= 3` (LIMIT не нужен / «Показать все» drill-in кнопка не показывается). Тест-coverage упоминает «cardinality preview rendering» одной строкой без edge-кейсов: 0/1-3/>3 impacted (drill-in видна/скрыта).

**Status:** approved

**Verdict:** ORDER BY + LIMIT 3 зафиксирован, но edge-cases 0/1-3/>3 impacted (drill-in скрыт/видна) не указаны — для тестируемости и UI-спеки edge-coverage должен быть явным.

### F024 [qa_engineer] minor

**Description:** F006 тест-кейс `whenChipDictionaryRemovedOutOfBand_thenSelectionFiltered_andSubmitDisabledIfEmpty` (одна assertion на два эффекта). Не покрывает случай `selectionFilteredButNotEmpty`. Стоит разделить на 2 кейса: `whenChipDictionaryRemovedOutOfBand_andSomeRemain_thenSelectionFiltered_andSubmitStillEnabled` + `whenChipDictionaryRemovedOutOfBand_andAllRemoved_thenSubmitDisabled`.

**Status:** rejected

**Verdict:** разбиение «filtered to empty» vs «filtered but not empty» — sub-flow design detail на уровне тест-гранулярности; в scope этот сценарий уже зафиксирован одним кейсом (F006), детализация — задача business_contract / design_tree.

## Итерация 4 (2026-06-22T17:45:04-06:00)

### F025 [architect] minor

**Description:** Line 308 содержит typo `whenEditUpgradesIsMulli_*` (должно быть `whenEditUpgradesIsMulti_*`). В § Tests (line 278) тот же тест корректно назван `whenEditUpgradesIsMulti_thenDowngradeSelectNotCalled`. Cross-reference в Note F022 расходится с canonical именем — copy-paste-ошибка iter 4.

**Status:** approved

**Verdict:** реальный typo в Note F022 (whenEditUpgradesIsMulli vs canonical whenEditUpgradesIsMulti) — cross-reference расходится, проёб iter 4.

### F026 [architect] minor

**Description:** Тип `DictionaryEntry` упоминается в State.kt (`availableDictionaries: List<DictionaryEntry>`) и UseCase сигнатуре (`flowDictionaries(): Flow<List<DictionaryEntry>>`), но **не существует** ни в `:modules:domain:lexeme`, ни в `core-db-api`. Существующий API тип — `DictionaryApiEntity` (`flowDictionaryList(): Flow<List<DictionaryApiEntity>>`). Либо использовать существующий `DictionaryApiEntity`, либо зафиксировать что вводится новый domain тип (с placement / mapping).

**Status:** approved

**Verdict:** Тип DictionaryEntry упоминается в State/UseCase сигнатурах, но не существует в domain/API — реальная неувязка scope, нужна фиксация.

### F027 [qa_engineer] critical

**Description:** API-level `EditComponentOutcome` variant list включает `NameEmpty` и `Failure(cause: Throwable)`, но existing `RenameComponentOutcome` / `SoftDeleteComponentOutcome` / `CreateComponentOutcome` НЕ имеют ни `NameEmpty`, ни `Failure` (`NameEmpty` валидируется в UseCaseImpl, исключения мапятся в `Failure` на UseCase-уровне). Расхождение с existing pattern + внутреннее противоречие scope (immutability check на UseCase-уровне) — implementer не знает по какому контракту писать API outcome.

**Status:** approved

**Verdict:** Расхождение API-level outcome list с existing pattern (NameEmpty/Failure обычно на UseCase-уровне) — implementer не знает по какому контракту писать, реальная проблема scope.

### F028 [qa_engineer] critical

**Description:** § Tests Reducer block НЕ содержит теста на `EditResult.Failure` — негативный сценарий generic IO/DB ошибки. Reducer-реакция на `EditOutcome.Failure` (close dialog? show generic error snackbar? retain dialog state?) не специфицирована и не тестируется. Пропущенный важный сценарий (IO/DB error — типичная негативная ветка).

**Status:** approved

**Verdict:** Reducer-реакция на EditResult.Failure не специфицирована в § Tests — реальный пробел test coverage в области ответственности шага.

### F029 [qa_engineer] minor

**Description:** § Аспекты `edit_component` / § Затронутые файлы НЕ описывают edge-case «no-op Submit»: user открыл EditDialog, ничего не менял и нажал Submit. Должен ли UseCase делать short-circuit `Success` без UPDATE (skip `updated_at` bump + skip cascade) или прогонять полный путь — не специфицировано. Отсутствует тест `whenEditSubmitNoOp_thenSuccessWithoutUpdate` / `_thenSuccessWithUpdate`.

**Status:** rejected

**Verdict:** «No-op Submit» — sub-flow design detail (business decision short-circuit vs UPDATE), не классификация scope.

### F030 [qa_engineer] minor

**Description:** § Аспекты `dictionary_chip_staleness` упоминает «Аналогично EditDialog если он будет включать dictionary-scope (в MVP Edit не меняет scope — пункт фиксирует invariant)», но в § Tests НЕТ теста-инварианта `whenEditDialogOpen_thenDictionariesLoaded_doesNotMutateEditState`. Без этого инвариант декларативный — регрессия (общая Reducer-ветка случайно применит фильтр к EditDialogState) пройдёт незамеченной.

**Status:** approved

**Verdict:** invariant заявлен в § Аспекты но не покрыт тестом — реальный gap consistency между аспектом и § Tests, в области ответственности шага.

(end of scope_analysis review — user-accepted after iter 5)
