# Что не доделано — IS481 phase 2

Срез того, что осталось не реализовано или не закрыто после прохождения adaptive flow IS481 phase 2 (infra / business / data / ui sub-flows) и code review (REVIEW.md, 28 findings). Все претензии о коде верифицированы Read/Grep на момент 2026-06-23.

---

## 1. Critical undone (блокер релиза)

### 1.1 Multi-dict picker полностью сломан (A1 / B1)

**Что:** один из 4 главных пунктов phase 2 — «scope=PerDictionaries multi-select в CreateDialog Manager-экрана» — нерабоч на runtime.

**Конкретно:**
- `DictionariesFlowHandler` создан как `MateFlowHandler` и инжектится в `DatasourceEffectHandler`, но **не зарегистрирован в `effectHandlerSet`** ViewModel. `Mate.subscribeToLongRunningFlows()` подписывает только handler'ы из `effectHandlerSet`, поэтому подписка не стартует.
- `DatasourceEffect.SubscribeDictionaries` **нигде не эмитится Reducer'ом** (`grep -c "SubscribeDictionaries" Reducer.kt → 0`).
- Итог: `state.availableDictionaries` навсегда `emptyList()` → chip-list пустой → submit с `Scope.PerDictionaries` недоступен. Пользователь не может создать компонент в подмножестве словарей через Manager — фича существует на бумаге, но в UI её нет.

**Verify:**
- `Read /modules/screen/components_manager/.../ComponentsManagerViewModel.kt:38-47` — `effectHandlerSet = setOf(datasourceHandler, flowHandler, uiHandler, navHandlerFactory.create(navigator))` — без `dictionariesFlowHandler`.
- `Grep "SubscribeDictionaries" Reducer.kt → 0 матчей`.

**Что нужно:** добавить `dictionariesFlowHandler` в `effectHandlerSet` ViewModel; либо удалить `SubscribeDictionaries` как мёртвый код (FlowHandler автоподписывается через `subscribe()` при init Mate), либо honest-emit его в init Reducer.

### 1.2 Template-immutability gate отсутствует в UseCase (A2 / Y9)

**Что:** business contract + KDoc + `02_scope.md` декларируют: «UseCaseImpl сравнивает template параметра с current → `TemplateImmutable` без обращения к data API (F017)». Реальный код — только `trim+isBlank`, далее сразу делегирует в API.

**Где:** `app/.../componentsmanager/ComponentsManagerUseCaseImpl.kt:144-172`.

**Verify:**
- `Read` строки 144-172 — нет `current.template` lookup, прямой делегат `lexemeApi.editComponentType(typeId.id, trimmed, template, isMulti)`.
- Defense-in-depth есть на data (`CoreDbApiImpl:582`), но это **fallback**, не основной gate.

**Что нужно:** либо реализовать gate в UseCase (нужен новый `getById` API method или передавать current через State), либо явно отказаться от F017 в контракте/KDoc и оставить только data check. Сейчас худшее из миров — контракт говорит одно, код другое; тест `whenSubmitEditWithChangedTemplate_thenTemplateImmutable_andDataApiNotCalled` отсутствует.

### 1.3 Inconsistency порядка защит editComponentType vs rename/softDelete (A3)

**Что:** edit использует обратный порядок защит относительно rename/softDelete. Sibling CRUD методы non-uniform; data_summary.md ложно утверждает parity.

**Verify (`core/core-db-impl/.../CoreDbApiImpl.kt`):**
- `editComponentType:573-574` → `if (systemKey != null) BuiltInProtected; if (removedAt != null) Removed` (BuiltIn первым).
- `renameComponentType:532-533` → `if (removedAt != null) Removed; if (systemKey != null) BuiltInProtected` (Removed первым).
- `softDeleteComponentType:690-691` → тот же порядок что rename (Removed первым).

Для built-in + soft-deleted типа edit вернёт `BuiltInProtected`, rename/softDelete — `Removed`. UI показывает разные snackbar для одной реальности.

**Что нужно:** swap строк 573-574 в editComponentType (Removed → BuiltInProtected), исправить `data_summary.md` (см. §5 doc-debt).

---

## 2. Не реализовано из brief acceptance

### Пункт 1 brief (Edit component + cardinality downgrade)

**Что в task.md (§ 1 Acceptance):**
- «UI показывает preview списка проблемных лексем (top-3 inline + «Показать все» drill-in либо bottom-sheet)».
- **Реализовано:** preview top-3 inline есть; «Показать все» **кнопка** есть в `CardinalityDowngradePreviewWidget`, но callback `onShowAllImpacted` — **no-op TBD comment** в обоих screens.
- **Gap:** drill-in destination (bottom-sheet или separate screen) не реализован. Y12 в triage помечен «→ закрыть в фиче» (скрыть кнопку до реализации), но и это не сделано — кнопка видна, тыкаешь — ничего не происходит.
- **Verify:** `Grep "onShowAllImpacted" ComponentsManagerScreen.kt:246 → "{ /* TBD: drill-in destination — backlog (bottom-sheet/screen) */ }"`.

**Что в task.md (§ 1 Acceptance):**
- «Реальный lexemeLabel resolve для preview».
- **Реализовано:** `lexemeLabel: (Long) -> String` callback резолвится через `context.getString(R.string.components_edit_lexeme_label, id)` = `"Lexeme #N"` placeholder.
- **Gap:** реальный label через UseCase query (`getLexemesByIds`) не сделан. ui_summary прямо называет это backlog.
- **Verify:** `Read strings.xml:282 → <string name="components_edit_lexeme_label">Lexeme #%1$d</string>`.

### Пункт 2 brief (Multi-dict picker)

**Что в task.md (§ 2 Acceptance):**
- «scope picker chip-list dictionaries с multi-select toggle» + «при submit `Scope.PerDictionaries(selectedIds)` UseCase создаёт N rows».
- **Реализовано:** State/Msg/Reducer/UI расширения сделаны; `availableDictionaries` поле есть; `Msg.CreateDictionaryToggle` есть; `DictionariesFlowHandler` создан; `flowDictionaries()` в UseCase есть; chip-list FlowRow+FilterChip в CreateDialog отрисован под `HostVariant.Manager`.
- **Gap:** см. §1.1 — handler не подписан, chip-list **всегда пустой** на runtime. Юзер не может выбрать словари → submit с `Scope.PerDictionaries(ids)` физически недоступен через UI.

### Пункт 3 brief (`:modules:widget:component_widgets/` shared)

**Что в task.md (§ 3 Acceptance):**
- «14 NEW source files (dialogs, widgets, templates) вынесены; 16 файлов в screen-модулях удалены; build PASS, все tests PASS без изменений».
- **Реализовано полностью:** 14 NEW + 16 deletions + `HostVariant` pattern + per-template architecture (TextWidget / ComponentBlock / ComponentByTemplate resolver). Build + tests PASS.
- **Gap:** **нет** в части concept compliance; есть minor backlog'и (LexemeStyle.LabelM отсутствует → fallback на BodyS; `PrimaryTextButtonWidget` без vararg overload; unified `ComponentRowWidget`; `HostVariant`/`DictionaryRef`/`DeletionImpactRef` declared inline вместо `shared/types.kt`) — все вынесены в ui_summary backlog.

### Пункт 4 brief (`RenameOutcome.BuiltInProtected` conflation)

**Что в task.md (§ 4 Acceptance):**
- «Добавить `Removed` variant в `RenameOutcome` / `DeleteOutcome` / `EditOutcome`; UseCase impl различает; Reducer ветки + snackbar «Компонент удалён»; unit-тесты».
- **Реализовано полностью:** `Removed` variant добавлен во все три sealed. Bug-fix #0 и #1 в data layer применены (см. data_summary §Bug-fixes). Reducer обрабатывает `Removed` ветку с snackbar + close dialog. Tests добавлены.
- **Gap:** только inconsistency порядка защит в editComponentType (см. §1.3 A3) — это **частный случай той же баги**, который остался не закрыт.

### Пункт 5 brief (Feature-tag + Migration logs + DAO cascade)

**Что в task.md (§ 5 Acceptance):**
- «Migration_012_to_013.kt — каждый из 9 шагов пишет лог: rows affected per step».
- **Реализовано:** 9 Log.d вызовов в `migrateImpl()` под `LogTags.COMPONENT_CONSTRUCTOR`, **формат `"M12→M13 step N <name>: ok"` — без `rowsAffected` counters**.
- **Gap:** acceptance явно перечисляет «rows affected (rename `remove_date → removed_at`) / rows backfilled (`is_multi` / `created_at` / `updated_at`) / rows count (`component_values.removed_at`) / success/failure (drop indices) / rows rewritten / skipped (JSON text + image) / rows affected (long_text → text)». Реальные counters требуют `SELECT changes()` после каждого `execSQL` — не сделано. Infra summary прямо называет это «MVP-минимум, backlog для QA».
- **Verify:** `Grep "Log.d|step " Migration_012_to_013.kt → 9 ok-stub строк`.

Остальные acceptance (`LogTags.COMPONENT_CONSTRUCTOR` в shared logger; entry/exit логи в UseCase методах; cascade rename/soft-delete с before/after refs count; prefs reset start/per-pref/done double-tag) — **реализованы**.

---

## 3. Технический долг → backlog

14 findings из REVIEW.md triage «→ backlog» (не блокеры, но грязь):

| ID | Severity | Описание | Почему отложен |
|---|---|---|---|
| **B2** | critical | `flowAllUserDefinedTypesWithUsage` и `flowUserDefinedTypesForDictionary` НЕ реактивны на `component_values` changes (`countActiveValueByType` / `aggregatedValueCountPerType` — suspend, не Flow). Пользователь добавил 5 переводов с custom типом — на Manager `0 · —` остаётся stale. | Крупная архитектурная задача — Flow-варианты в DAO через Room `@Query`. Отдельный тикет. |
| **Y2** | critical | `addLexemeWithTranslation` / `updateLexemeTranslation` — `@Deprecated` shims, 0 callers (~52 строки в CoreDbApi + impl). Verify: оба видны в `CoreDbApi.kt:243-251`. | Отдельный clean-up commit; затронет `TranslationApiEntity` (возможно тоже мёртв). |
| **Y3** | critical | `WordCardUseCase` generic `addComponentValue/updateComponentValue/deleteComponentValue` — без callers; impl сам признаёт «generic path не имеет caller'ов в IS481». Verify: `WordCardUseCase.kt:52-65`. | Удалить до момента реального caller'а. |
| **Y4** | critical | `Field` / `PrimitiveType` / `ComponentTemplate.fields` — speculative future API, 0 imports. Verify: `ls modules/domain/lexeme/.../` → `Field.kt`, `PrimitiveType.kt` живы. | Удалить, заведут заново когда composite templates появятся. |
| **Y5** | critical | `Primitive.Color` + `"color"` JSON branch — reserved-but-unused. Verify: `Primitive.kt:17 → data class Color(val hex: String) : Primitive`. | sealed → расширение non-breaking; удалить. |
| **A5** | minor | Дублирование `EditDialogState` / `EditNameError` / `ImpactedLexemesPreview` между двумя screen-mate (~80 строк дублей). | Вынести в shared module либо принять дублирование как осознанный choice. |
| **B5** | minor | `previewDeletionImpact.affectedPrefs` покрывает только `affectedConfigs.dictionaryId`, не все `dictionariesWithValues`. Edge case UX. | Расширить `affectedPrefs = (affectedConfigs.map { it.dictionaryId } + dictionariesWithValues).distinct()`. |
| **B6** | minor | `getById` returns null → fallback `BuiltInProtected` (confusing для hard-deleted). | Изменить fallback на `NotFound` или `Removed`. Diagnostic improvement. |
| **B7** | minor | `WordCardUseCaseImpl catch (e: Exception)` глотает `CancellationException` (10 sites: 51, 77, 125, 175, 187, 208, 230, 245, 252, 298). Pre-existing, но phase 2 их трогал. | Добавить `} catch (e: CancellationException) { throw e } catch (e: Exception) { ... }` всюду. |
| **B8** | minor | Tests не покрывают bug A1/B1: 75 tests pass дают ложную уверенность. | Интеграционный test ViewModel: мокировать `flowDictionaries()` → assert `availableDictionaries.isNotEmpty()`. |
| **Y7** | minor | `PerDictionaryComponentsUseCaseImpl` 5/6 методов — pass-through delegate. | Reducer берёт `ComponentsManagerUseCase` напрямую, либо в PerDict interface оставить только `flowComponentsForDictionary`. |
| **Y10** | minor | `insertDefaultQuizConfig` DAO-метод dead-code; hardcoded JSON inline в `WordDao` (дубль). | Либо удалить метод, либо использовать в WordDao вместо inline JSON. |
| **Y13** | minor | `Lexeme.builtIn(key)` extension — нет production callers (только тесты). | Использовать в `LexemeMapper.kt:76,81` или удалить. |
| **Y15** | minor | Magic-string `"Definition"` в production-коде (4 occurrences: `WordCardUseCaseImpl.kt:241, 281`, `DatasourceEffectHandler.kt:141`, `LexemeMapper.kt:81`). | `const val DEFINITION_USER_DEFINED_NAME = "Definition"` либо `WellKnownUserDefined` enum. |

---

## 4. Manual test scenarios — не запущены

`checklist.md` имеет 5 root items в статусе `[ ]` (требуют ручного теста на эмуляторе/девайсе). Ни один не прогонялся:

- **[ ] root item 1** — Edit user-defined компонента через EditDialog (name / isMulti / template menu disabled / cardinality downgrade с preview). Покрывает scenarios 1-3 в `checklist.md`.
- **[ ] root item 2** — Multi-dict scope picker (PerDictionaries N selected / 0 selected disabled / chip staleness при out-of-band удалении dict). Scenarios 4-6. **Гарантированно упадёт из-за §1.1.**
- **[ ] root item 3** — Shared widget module visual parity (Manager + PerDict оба показывают одинаковые dialogs/rows/empty/fab из shared). Scenario 7.
- **[ ] root item 4** — Soft-deleted Rename / Edit / Delete возвращают `Removed` (race with delete; не misleading `BuiltInProtected`). Scenarios 8-10.
- **[ ] root item 5** — Feature-tag `###ComponentConstructor###` фильтрует фича-события в adb logcat (CRUD operations / Migration per-step / cascade / prefs reset). Scenarios 11-14.

Подробные шаги — `checklist.md § Ручное тестирование` сценарии 1-14.

---

## 5. Doc-debt

### 5.1 `data_summary.md` ложное parity-утверждение (A3)

- **Строка 13:** «порядок swap'нут на Removed → BuiltInProtected (parity с `editComponentType:572-573`)».
- **Строка 27:** «в `renameComponentType` swap не только bug-fix mapping, но и порядок (parity с `editComponentType:572-573` и post-#1 `softDelete`)».
- **Реальность:** `editComponentType:573-574` — `BuiltInProtected` ПЕРЕД `Removed` (обратный порядок). Документ врёт. Надо исправить и (вариант 1) применить swap к edit для honest parity, либо (вариант 2) переписать data_summary как «edit использует обратный порядок — известное несоответствие → backlog».

### 5.2 business_contract typeId Long vs codebase `ComponentTypeId`

- **Описано в `business_contract_spec_review.md` F-BCSR1**:
  > Рассинхрон спека↔контракт по типу `typeId`. В contract.md `OpenEditDialog(val typeId: Long)`, `EditDialogState.typeId: Long`, `useCase.editComponent(typeId: Long)`, `DatasourceEffect.EditComponent(val typeId: Long)`. В спеке те же сигнатуры — с `ComponentTypeId`. Спека consistent с существующей codebase.
- **Принято как-is:** «implement-шаг приведёт к финальному код = ComponentTypeId; контракт остаётся с Long как historical inaccuracy».
- **Реальность:** код правильный (`ComponentTypeId`), `business_contract.md` так и не приведён.

### 5.3 business_contract `ComponentsManagerState` vs codebase `ComponentsManagerScreenState`

- **`business_contract_spec_review.md` F-BCSR2**:
  > Рассинхрон имени state-класса. Контракт `ComponentsManagerState`; спека `ComponentsManagerScreenState`. Base codebase использует `ComponentsManagerScreenState` (`mate/State.kt:25`).
- **Принято как-is:** «implement-шаг использует existing; контракт inaccurate, не блокирующий».
- **Реальность:** код правильный, контракт так и не приведён.

---

## 6. FF self-improvements в FlowBacklog

### Уже добавлены во время flow

- **IS481p2-F1** — `check_criteria` не поддерживает условные `output_criteria` (`figma_dump` валит при Case A `feature_has_figma=false`). Workaround #3 (conductor создаёт stub `figma_dump.json`). Системный фикс (#1: расширить runner `is_mechanical` на `«если <var>=<value> — ...»`) — open.
- **IS481p2-F2** — рассинхрон имени артефакта: `task.md` frontmatter `output: task.md` vs `scope_analysis.md` input_criteria `00_task.md существует`. Применён локальный fix #1 (overlay edit), системный fix (`{step.output}` interpolation либо align base task.md frontmatter) — open.

### Возможные добавления (из постмортем quick wins, ещё не записаны)

- **condition в output_criteria** — generalization IS481p2-F1: позволить step.md `output_criteria` использовать synтаксис `if <var>=<value>: <criterion>` либо `unless <var>=<value>: <criterion>`. Runner парсит prefix, резолвит var из `plan.context`, применяет либо skip.
- **alias-step frontmatter override** — generalization IS482-F7: позволить inline-step в flow переопределять отдельные frontmatter поля step-файла без копии целого промпта. Пример: `step: design_tree` + `name: ui_design_tree` + `overrides: { description: "UI design tree subset" }`.

---

## Итог

- **3 блокера** до релиза (A1/B1 multi-dict broken, A2 template gate missing, A3 inconsistency).
- **3+ пропущенных acceptance** (drill-in `onShowAllImpacted` no-op; lexemeLabel placeholder; migration `rowsAffected` counters stub; multi-dict picker блокирован A1).
- **14 в техдолге** (B2 / Y2-Y5 / A5 / B5-B8 / Y7 / Y10 / Y13 / Y15).
- **5 root items ручных тестов** ждут запуска (scenarios 1-14 в `checklist.md`).
- **3 doc-debt** (data_summary parity, business_contract typeId Long, business_contract state name).
- **2 FF finding-а** добавлены (IS481p2-F1/F2); 2 quick win'а предложены.

Phase 2 — **не code-complete для релиза**. Минимум — закрыть 3 блокера в §1, прогнать §4 manual tests, исправить §5.1 doc-debt.

_model: claude-opus-4-7[1m]_
