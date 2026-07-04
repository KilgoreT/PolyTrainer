# Checklist

- [ ] Пользователь редактирует user-defined компонент (name / isMulti) через EditDialog → изменения сохраняются, list перерисовывается, при downgrade isMulli true→false с конфликтом — показывается preview impacted lexemes; template menu disabled/immutable [scope §Аспекты edit_component, cardinality_downgrade_guard; task §1]
  - ✅ Лог: ###ComponentConstructor### editComponent entry typeId=<id> name=<name> isMulti=<bool> [scope §Логгер; task §5] (UseCaseImpl entry/exit под FeatureLogTags.COMPONENT_CONSTRUCTOR)
  - ✅ Лог: ###ComponentConstructor### editComponent exit outcome=<Success|TemplateImmutable|CardinalityDowngradeBlocked|BuiltInProtected|Removed|...> [scope §Логгер; task §5] (9 веток EditOutcome, exhaustive when в UseCaseImpl)

- [ ] Пользователь выбирает multi-dict scope (radio Global / PerDictionaries + chip-list dictionaries) в Create-диалоге Manager-экрана → при PerDictionaries с N selected dictionaries создаётся N rows; при 0 selected — submit disabled; stale chip filter работает при out-of-band удалении dict [scope §Аспекты multi_dict_scope_picker, dictionary_chip_staleness; task §2]
  - ✅ Лог: ###ComponentConstructor### createUserDefinedComponent scope=<Global|PerDictionaries(ids)> rowsCreated=<N> [scope §Логгер; task §5] (existing phase 1 entry/exit логи покрывают; UI mounts FlowRow+FilterChip через HostVariant.Manager)

- [ ] Shared widget module `:modules:widget:component_widgets` наполнен: 8+ widgets (CreateComponentDialog, RenameComponentDialog, DeleteComponentConfirmDialog, EditComponentDialog, UserDefinedRowWidget, ComponentsEmptyStateWidget, CreateComponentFab, ComponentTemplateLabel, NameErrorLabel) + per-template architecture (TextWidget / ComponentBlock / ComponentByTemplate resolver) вынесены и переиспользуются из обоих screen-модулей; дубликаты удалены [scope §UI/Widgets, §UI cleanup; task §3]
  - ✅ Лог: не требуется — pure UI рефакторинг, без runtime-событий [scope §UI/Widgets] (14 NEW source files в widget module, 16 удалённых файлов в screen widget/ директориях)

- [ ] Soft-deleted user-defined компонент при попытке rename / edit / delete возвращает `Removed` outcome (не `BuiltInProtected`) → UI закрывает диалог + snackbar «Компонент удалён»; list перерисовывается без removed item [scope §Аспекты soft_deleted_removed_outcome, edit_race_with_delete; task §4]
  - ✅ Лог: ###ComponentConstructor### renameComponent exit outcome=Removed typeId=<id> [scope §Логгер; task §5] (LexemeApiImpl.renameComponentType bug #0 fixed: Removed check ПЕРЕД BuiltInProtected, UseCaseImpl mapping Removed→RenameOutcome.Removed)
  - ✅ Лог: ###ComponentConstructor### softDeleteComponent exit outcome=Removed typeId=<id> [scope §Логгер; task §5] (LexemeApiImpl.softDeleteComponentType bug #1 fixed: Removed check добавлен между lookup и system_key check, UseCaseImpl mapping Removed→DeleteOutcome.Removed)
  - ✅ Лог: ###ComponentConstructor### editComponent exit outcome=Removed typeId=<id> [scope §Логгер; task §5] (LexemeApiImpl.editComponentType:572-573 native Removed branch, Reducer EditResult.Removed → snackbar + close dialog)

- [ ] Feature-tag `###ComponentConstructor###` фильтрует фича-события в adb logcat: shared `LogTags.COMPONENT_CONSTRUCTOR` константа в `:modules:core:logger/LogTags.kt`; все public UseCase методы (create / rename / softDelete / previewDeletionImpact / editComponent) пишут entry/exit логи с feature-tag; Migration_012_to_013 логирует per-step counters (9 шагов); QuizConfigDao.updateComponentRefs cascade и prefs reset логируют before/after [scope §Аспекты feature_log_tag, migration_logging; task §5]
  - ✅ Лог: ###ComponentConstructor### Migration_012_to_013 step=<1..9 name> rowsAffected=<N> [scope §Аспекты migration_logging; task §5] (infra sub-flow узел 4: 9 Log.d call'ов в Migration_012_to_013.migrateImpl, MVP-формат "M12→M13 step N <name>: ok" без rowsAffected counters — backlog для QA)
  - ✅ Лог: ###ComponentConstructor### QuizConfigDao.updateComponentRefs typeId=<id> dictId=<id> before=<N> after=<M> [scope §Аспекты migration_logging; task §5] (data sub-flow #3/#4: cascade rename + cascade soft-delete логи в LexemeApiImpl:641-660 и :705-715, формат "cascade rename: configId=… refs=N→M write=… oldName=… newName=…")
  - ✅ Лог: ###ComponentConstructor### prefs reset outcome=<ok|failure cause=...> [scope §Аспекты migration_logging; task §5] (data sub-flow #5: double-tag start/per-pref ok/per-pref fail/done в ComponentsManagerUseCaseImpl.resetQuizPickerPrefsBestEffort:185-216)

## Ручное тестирование

### Сценарий 1: Edit user-defined компонента (name + isMulti) без downgrade
1. Открыть Settings → Components Manager → выбрать user-defined компонент (isMulti=false) → нажать Edit icon/menu.
2. В EditDialog изменить name → «NewName», isMulti оставить false, template menu — disabled (immutable).
3. Submit.
4. Ожидание: dialog закрывается, list перерисовывается с новым именем; в quiz_configs cascade обновил refs (если name изменился).
5. Логи:
   - `###ComponentConstructor### editComponent entry typeId=<id> name=NewName isMulti=false`
   - `###ComponentConstructor### editComponent exit outcome=Success`
   - `###ComponentConstructor### cascade rename: configId=<id> refs=N→N write=<bool> oldName=<old> newName=NewName` (per затронутый quiz_config)

### Сценарий 2: Edit cardinality downgrade isMulti true→false с конфликтом
1. Подготовка: существует user-defined компонент с isMulti=true; есть лексема с 2+ component_values на этом type.
2. Открыть EditDialog → toggle isMulti на false → Submit.
3. Ожидание: dialog НЕ закрывается, показывается preview impacted lexemes (top-3 inline + «Показать все» если >3); если ≤3 — все inline, drill-in скрыт.
4. Edge case: legitimate downgrade (один lexeme с одним value) НЕ должен блокироваться (data bug #2 fixed: real per-lexeme SELECT с deterministic ORDER `MAX(updated_at) DESC, lexeme_id ASC`).
5. Логи:
   - `###ComponentConstructor### editComponent entry typeId=<id> name=<name> isMulti=false`
   - `###ComponentConstructor### editComponent exit outcome=CardinalityDowngradeBlocked impactedSize=<N>` (real List<Long> ids, не пустой как conservative approximation)

### Сценарий 3: Edit template (immutable)
1. Открыть EditDialog для существующего user-defined компонента.
2. Ожидание: template поле disabled/read-only (UI блокирует выбор); попытка submit с unchanged template проходит, с changed template (если caller сможет послать) — `EditOutcome.TemplateImmutable` без обращения к data API (gate на UseCaseImpl ДО `lexemeApi.editComponentType(...)`).
3. Логи:
   - `###ComponentConstructor### editComponent exit outcome=TemplateImmutable` (если попытка изменить template)

### Сценарий 4: Multi-dict scope picker (PerDictionaries N selected)
1. Открыть Settings → Components Manager → FAB «Create».
2. В CreateDialog: выбрать radio «На конкретные» (PerDictionaries) → отметить 2 из 3 dictionaries в chip-list (FlowRow+FilterChip) → ввести name «test_component» → Submit.
3. Ожидание: dialog закрывается; в БД создаётся 2 rows component_types (по одной на каждый выбранный dict).
4. Логи:
   - `###ComponentConstructor### createUserDefinedComponent scope=PerDictionaries(ids=[1,2]) rowsCreated=2`

### Сценарий 5: Multi-dict scope picker — 0 dicts selected
1. В CreateDialog: выбрать radio «На конкретные» → НЕ отмечать ни одного dictionary → ввести name.
2. Ожидание: submit-кнопка disabled (preventive UX через `canSubmit` extension val: Global → не пустой trim'нутый name; PerDictionaries → дополнительно `selectedDictionaryIds.isNotEmpty()`).
3. Логи: не требуется (UI-level guard).

### Сценарий 6: Dictionary chip staleness
1. Открыть CreateDialog в Manager → выбрать PerDictionaries → отметить dict_A + dict_B.
2. В другом контексте (другой экран) удалить dict_A.
3. Ожидание: `Msg.DictionariesLoaded(updated)` фильтрует selected (`createDialog.selectedDictionaryIds ∩ list.ids`) — dict_A исчезает из chip selection; если оба удалены — submit disabled.
4. Invariant: `editDialog` поля НИКОГДА не мутируются на DictionariesLoaded (F030 invariant, dedicated test `whenEditDialogOpen_thenDictionariesLoaded_doesNotMutateEditState`).
5. Логи: не требуется (Reducer-level filter).

### Сценарий 7: Shared widget module — visual parity
1. Открыть ComponentsManagerScreen (Settings drill-in) и PerDictionaryComponentsScreen (AppBar «молоток»).
2. Ожидание: оба экрана отображают одинаковые dialogs (Create / Rename / Delete / Edit) и rows / empty state / fab — из shared `:modules:widget:component_widgets`; нет дубликатов в screen-модулях (16 файлов удалено).
3. Особенности: CreateDialog рендерится через `HostVariant.Manager | PerDict` (PerDict скрывает scope picker, передаёт emptyList/no-op callbacks).
4. Логи: не требуется.

### Сценарий 8: Soft-deleted Rename returns Removed
1. Подготовка: user-defined компонент soft-deleted (removed_at IS NOT NULL).
2. Через debug-action либо race condition (другой контекст удалил пока диалог открыт) — попытаться rename удалённого type.
3. Ожидание: snackbar «Компонент удалён», dialog закрывается, list перерисовывается.
4. Bug fix #0: `renameComponentType` теперь возвращает `Removed` (не silent `BuiltInProtected`); порядок Removed → BuiltInProtected.
5. Логи:
   - `###ComponentConstructor### renameComponent exit outcome=Removed typeId=<id>`

### Сценарий 9: Soft-deleted Edit returns Removed (race with delete)
1. Подготовка: открыть EditDialog для type X.
2. В другом контексте soft-delete type X (removed_at = now()).
3. Submit Edit.
4. Ожидание: snackbar «Компонент удалён», dialog закрывается, list перерисовывается без removed item.
5. Логи:
   - `###ComponentConstructor### editComponent exit outcome=Removed typeId=<id>`

### Сценарий 10: Soft-deleted Delete returns Removed
1. Подготовка: open DeleteConfirmDialog для type X.
2. В другом контексте soft-delete type X.
3. Confirm Delete.
4. Ожидание: snackbar «Компонент удалён», dialog закрывается.
5. Bug fix #1: `softDeleteComponentType` теперь возвращает `Removed` (не silent `Success(impact.valueCount=0)`); проверка добавлена между lookup и system_key check.
6. Логи:
   - `###ComponentConstructor### softDeleteComponent exit outcome=Removed typeId=<id>`

### Сценарий 11: Feature-tag adb logcat фильтрация — CRUD operations
1. Запустить app на emulator/device.
2. Выполнить: create / rename / softDelete / edit user-defined компонента.
3. `adb logcat | grep '###ComponentConstructor###'` — видим stream entry/exit логов на каждый CRUD.
4. Ожидание: каждый public UseCase метод пишет entry + exit с outcome.
5. Логи (пример):
   - `###ComponentConstructor### createUserDefinedComponent entry name=foo scope=Global`
   - `###ComponentConstructor### createUserDefinedComponent exit outcome=Success`
   - `###ComponentConstructor### renameComponent entry typeId=<id> newName=bar`
   - `###ComponentConstructor### renameComponent exit outcome=Success`

### Сценарий 12: Feature-tag в Migration_012_to_013 per-step counters
1. Установить app с pre-M13 БД (либо инструмент имитации фрэш миграции; либо проверить логи существующей миграции при первом запуске).
2. `adb logcat | grep '###ComponentConstructor###'` во время миграции.
3. Ожидание: 9 per-step логов в формате `"M12→M13 step N <name>: ok"` (MVP-минимум, без `SELECT changes()` affected rows — backlog).
4. Логи (формат implement):
   - `###ComponentConstructor### M12→M13 step 1 renameComponentTypesRemoveDate: ok`
   - `###ComponentConstructor### M12→M13 step 2 addComponentTypesNewColumns: ok`
   - `###ComponentConstructor### M12→M13 step 3 dropUniqueComponentTypesDictName: ok`
   - `###ComponentConstructor### M12→M13 step 4 addComponentValuesNewColumns: ok`
   - `###ComponentConstructor### M12→M13 step 5 dropUniqueComponentValuesLexemeType: ok`
   - `###ComponentConstructor### M12→M13 step 6 createComponentValuesLexemeIdIndex: ok`
   - `###ComponentConstructor### M12→M13 step 7 consolidateLongTextTemplateKey: ok`
   - `###ComponentConstructor### M12→M13 step 8 rewriteTextJson: ok`
   - `###ComponentConstructor### M12→M13 step 9 rewriteImageJson: ok`

### Сценарий 13: Feature-tag в QuizConfigDao.updateComponentRefs cascade
1. Rename user-defined компонента который используется в quiz_configs.component_refs.
2. `adb logcat | grep '###ComponentConstructor###'`.
3. Ожидание: cascade лог с before/after refs count per затронутый config.
4. Параллельный кейс: soft-delete user-defined компонента → cascade soft-delete лог (узел #4).
5. Логи (формат implement):
   - `###ComponentConstructor### cascade rename: configId=<id> refs=N→M write=<bool> oldName=<old> newName=<new>` (per for-each iteration)
   - `###ComponentConstructor### cascade soft-delete: configId=<id> refs=N→M write=<bool> removedName=<name>` (при soft-delete)

### Сценарий 14: Feature-tag в prefs reset (best-effort)
1. Soft-delete user-defined компонента который используется в quiz_picker prefs.
2. `adb logcat | grep '###ComponentConstructor###'`.
3. Ожидание: prefs reset лог (start / per-pref ok / per-pref fail / done) под double-tag (feature-tag + module-tag `COMPONENTS_MANAGER`).
4. Логи:
   - `###ComponentConstructor### prefs reset start typeId=<id>`
   - `###ComponentConstructor### prefs reset pref=<key> outcome=ok` (per-pref)
   - `###ComponentConstructor### prefs reset pref=<key> outcome=failure cause=<...>` (per-pref failure)
   - `###ComponentConstructor### prefs reset done typeId=<id>`

## log_messages

- checklist.md обновлён: подпункты ✅ (10 sub-items с logging confirmed по infra/business/data/ui summaries), root items оставлены [ ] (требуют ручного теста на девайсе).
- Manual scenarios 1-14 уточнены: добавлены edge cases из implementation (data bug #0/#1/#2 fixes, HostVariant pattern, MVP migration log format "M12→M13 step N: ok", double-tag prefs reset, cascade soft-delete параллельный кейс, real per-lexeme SELECT для CardinalityDowngrade).

_model: claude-opus-4-7[1m]_
