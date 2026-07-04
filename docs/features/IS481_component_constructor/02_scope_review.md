# Review: 02_scope.md

## Итерация 1 (2026-06-15T03:53:25-06:00)

### F001 [architect] critical

**Description:** M12 JSON формат в § Миграция M12→M13 указан неверно — реальный M12 `{"v":1,"text":"..."}` / `{"v":1,"uri":"..."}` (см. `ComponentValueDataJson.kt:20-29`), а не `{"type":"text","text":"foo"}`. Migration написанная по этому scope покорраптит existing rows.

**Status:** approved

**Verdict:** Реальный M12 формат — `{"v":1,"text":"..."}` / `{"v":1,"uri":"..."}`; scope указывает несуществующий `{"type":"text","text":"foo"}` — миграция по такому описанию покорраптит rows.

### F002 [architect] critical

**Description:** Переименование `ComponentType.removeDate → removedAt` имеет cross-cutting impact на не упомянутые консьюмеры: `app/mapper/LexemeMapper.kt`, `core-db-api/.../SampleApiEntity.kt`, `WordApiEntity.kt` тоже имеют `removeDate`. Scope не объявил scope rename.

**Status:** approved

**Verdict:** Пропущенный файл/контекст rename'а критичен для классификации затронутых файлов.

### F003 [architect] minor

**Description:** После DROP UNIQUE на `component_values` нужно добавить отдельный `Index("lexeme_id")` иначе `flowValuesByLexeme` = full-table scan.

**Status:** rejected

**Verdict:** Конкретные индексы — это design_tree/implement detail; решается на data_design_tree.

### F004 [architect] minor

**Description:** «новый `ComponentApi` (или расширение)» — open question не зафиксирован в `## Open questions`, оставлен полу-решённым в Aspects.

**Status:** approved

**Verdict:** Аспект явно содержит open phrase «(или расширение)» — это шов scope_analysis, должен быть в Open questions.

### F005 [architect] minor

**Description:** «data-layer.md описывает M10-state» — unverified claim в § Релевантные спеки.

**Status:** rejected

**Verdict:** Утверждение приведено самим scope'ом как явная пометка о состоянии гайда — не unverified claim.

### F006 [architect] minor

**Description:** `insertSingleSafe @Transaction` механика (JOIN между component_types и component_values для is_multi check) не отражена.

**Status:** rejected

**Verdict:** Механика @Transaction — implementation detail на business_contract/design_tree.

### F007 [qa_engineer] critical

**Description:** `needs_migration_tests: true` есть, но в списке файлов нет `Migration_012_to_013Test.kt`.

**Status:** rejected

**Verdict:** Конкретные test file paths — работа test/design_tree шага; флаг `needs_migration_tests: true` достаточен.

### F008 [qa_engineer] critical

**Description:** Edge case миграции JSON rewrite (malformed/unknown/null/empty existing rows) — стратегия не определена.

**Status:** approved

**Verdict:** Стратегия edge case rewrite затрагивает классификацию `db_migration` аспекта и должна быть зафиксирована либо явно открыта; концепт её не определяет.

### F009 [qa_engineer] critical

**Description:** Backfill timestamps на existing rows — `created_at`/`updated_at NOT NULL` без `DEFAULT` приведёт к падению миграции.

**Status:** approved

**Verdict:** NOT NULL без DEFAULT для существующих rows — базовая корректность стратегии миграции на уровне аспекта `db_migration`.

### F010 [qa_engineer] major

**Description:** Cascade поведение soft-delete component_type на component_values не зафиксировано в аспектах scope.

**Status:** rejected

**Verdict:** Cascade зафиксирован в § Аспекты как `dao_convention` (JOIN-фильтр на parent.removed_at), соответствует deletion_concept.md.

### F011 [qa_engineer] major

**Description:** Concurrent / повторный клик create / delete + race между previewDeletionImpact snapshot и softDeleteComponent — не покрыт.

**Status:** rejected

**Verdict:** Idempotency / race conditions — это business_contract/design_tree work, не scope-классификация.

### F012 [qa_engineer] major

**Description:** Error contract для `insertSingleSafe @Transaction` cardinality reject не определён.

**Status:** rejected

**Verdict:** Error contract для DAO — это цель business_contract шага.

### F013 [qa_engineer] minor

**Description:** `mapper_golden_fixtures` без негативных fixtures.

**Status:** rejected

**Verdict:** Состав test fixtures — test/design_tree work.

### F014 [qa_engineer] minor

**Description:** Open question «один модуль vs два» без acceptance-критерия для финализации.

**Status:** rejected

**Verdict:** Acceptance-критерий — процессуальное замечание, не корректность классификации; open question помечен «финализировать на business_walkthrough».

### F015 [qa_engineer] minor

**Description:** Unit-тест на nullable `ComponentTemplate.fromKey` callsite'ы не упомянут.

**Status:** rejected

**Verdict:** Конкретный unit-test — test шаг; scope уже фиксирует аспект `forward_compat_unknown`.

---

## Итоги итерации 1

- **Approved:** 5 (F001, F002, F004, F008, F009) — 4 critical + 1 minor.
- **Rejected:** 10.
- **Решение:** repeat (есть approved critical).

---

## Итерация 2 (2026-06-15T04:30:00-06:00) — pending inquisitor

Raw findings (11). Inquisitor verdicts будут выставлены при resume сессии — контекст текущей conversation исчерпан перед запуском inquisitor.

### F016 [architect] critical

**Description:** `core/core-db-impl/.../entity/ComponentValueWithType.kt` отсутствует в списке affected files (раздел Data layer), но содержит mapper `toApiEntity()` который зовёт `toComponentValueData` и должен быть переписан под typed `TemplateValues` — без него M13 не скомпилируется.

**Status:** approved

**Verdict:** ComponentValueWithType.kt напрямую импортирует toComponentValueData и собирает ComponentValueApiEntity — упразднение mapper'а без правки этого файла = compile fail; в "Затронутые файлы" он не указан.

### F017 [architect] critical

**Description:** `core/core-db-impl/.../CoreDbApiImpl.kt` не выделен явно в affected files, но содержит ~10 callsite'ов `ComponentValueData` (включая legacy `addLexemeWithTranslation` shim), все требуют rebind на `TemplateValues` — упомянут лишь обобщённо как "Storage слой".

**Status:** approved

**Verdict:** CoreDbApiImpl.kt имеет ~10 callsite'ов ComponentValueData (addLexemeWith*, addComponentValue, updateComponentValue, deprecated shims) и переходит на typed boundary — обобщённая фраза «Storage слой» в scope конкретный файл не фиксирует.

### F018 [architect] critical

**Description:** UNIQUE `Index(["dictionary_id","name"], unique=true)` на `component_types` конфликтует с soft-delete семантикой (повторный create user-defined компонента с именем удалённого ранее → constraint violation); ни scope, ни template_model.md не обсуждают partial-unique либо смену стратегии (этот partial-unique вопрос обсуждён только для `component_values`).

**Status:** approved

**Verdict:** ComponentTypeDb действительно объявляет Index(["dictionary_id","name"], unique=true); scope не обсуждает конфликт UNIQUE с soft-delete и не выносит выбор стратегии (partial-unique / append suffix / hard-delete soft-deleted при коллизии) в Open questions.

### F019 [architect] critical

**Description:** Переход `ComponentTemplate.fromKey` → nullable каскадно меняет публичный контракт `ComponentTypeApiEntity.template: ComponentTemplate` (либо поле становится nullable, либо `toApiEntity()` возвращает nullable, либо unknown template-row фильтруется на DAO-границе) — это решение классификационного уровня, в scope не зафиксировано в `public_contract_change` и не вынесено в Open questions.

**Status:** approved

**Verdict:** ComponentTypeApiEntity.template — non-nullable + toApiEntity делает прямой вызов fromKey; nullable-каскад затрагивает public API и DAO-границу, но в § public_contract_change и Open questions не зафиксирован.

### F020 [architect] minor

**Description:** `app/src/main/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImpl.kt` и его тест указаны в Infrastructure, но не явно в "Consumers domain rewrite" — асимметрия с `WordCardUseCaseImpl`; классификация size непоследовательна.

**Status:** rejected

**Verdict:** Симметрия в scope сохранена — оба UseCase'а указаны в Infrastructure, оба теста в Consumers tests; QuizChatUseCaseImpl не содержит refs ComponentValueData, его rewrite сводится к ComponentType.removedAt rename.

### F021 [qa_engineer] critical

**Description:** Soft-delete UNIQUE collision не классифицирован — существующий `Index(value = ["dictionary_id", "name"], unique = true)` на `component_types` после soft-delete блокирует пересоздание компонента с тем же именем в словаре; aspect отсутствует, стратегия (partial unique с `WHERE removed_at IS NULL` / business-rule / иное) не выбрана. (Дубль F018 от другого ревьювера.)

**Status:** approved

**Verdict:** Дубль F018 — реальный архитектурный пробел в scope.

### F022 [qa_engineer] critical

**Description:** Cleanup `quiz_configs.component_refs` при soft-delete отсутствует как aspect — `deletion_concept.md` § «Что удалять» явно требует «убрать ref сразу», но scope не классифицирует ни на `dao_convention`, ни на UseCase, ни на migration; квиз сломается при попытке загрузить удалённый компонент.

**Status:** approved

**Verdict:** deletion_concept.md § «Что удалять» прямо требует cleanup quiz_configs.component_refs при soft-delete; scope не классифицирует этот шаг ни в dao_convention, ни в UseCase aspects, ни в migration.

### F023 [qa_engineer] critical

**Description:** `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/room/SeedBuiltIns.kt` не в списке затронутых файлов — содержит SQL `INSERT INTO component_types (... remove_date)`, вызывается из `RoomDatabase.Callback.onCreate` (fresh install); после rename без правки SeedBuiltIns создание новой БД упадёт.

**Status:** approved

**Verdict:** SeedBuiltIns.kt действительно содержит INSERT с literal `remove_date`, вызывается из RoomDatabase.Callback.onCreate; rename column без правки этого SQL = fresh install падает с unknown column.

### F024 [qa_engineer] major

**Description:** `previewDeletionImpact(typeId)` описан как «счётчик values», но не учитывает второй вид impact'а — удаление ref'а из `quiz_configs.component_refs`; превью обязано показать full impact (values + quiz configs), иначе юзер удаляет «по-тихому» ломая активные quiz-конфиги.

**Status:** approved

**Verdict:** previewDeletionImpact должен возвращать full impact (values + quiz configs); иначе UI confirm-dialog не покажет полный warning.

### F025 [qa_engineer] minor

**Description:** Aspect `forward_compat_unknown` ограничивает scope только `ComponentTemplate.fromKey`, но согласно `template_model.md` § Open Q4 нужна симметричная обработка unknown primitive `type` в JSON и type-mismatch со schema; парсер `parseTemplateValues` должен это поддерживать — отдельным aspect'ом не вынесено.

**Status:** approved

**Verdict:** template_model Open Q4 описывает unknown primitive type / type mismatch со schema; scope-aspect forward_compat_unknown сужен только до fromKey.

### F026 [qa_engineer] minor

**Description:** Open question «SQL `json_*` vs Kotlin loop» опирается на ложное допущение «rows немного (~2k-25k)» — per-row update в Kotlin loop через `SupportSQLiteDatabase.execSQL` на 25k rows = десятки секунд; финализировать на `data_design_tree` без QA-критерия (acceptable migration time) рискованно.

**Status:** approved

**Verdict:** 25k rows × парсинг JSON в Kotlin loop на старых девайсах = десятки секунд; QA-критерий «acceptable migration time» нужен в Open question.

---

## Итоги итерации 2

- **Approved:** 10 (F016, F017, F018, F019, F021, F022, F023, F024, F025, F026) — 7 critical + 1 major + 2 minor.
- **Rejected:** 1 (F020 — асимметрия не подтвердилась).
- **Решение:** repeat (есть approved critical).

---

## Итерация 3 (2026-06-15T05:30:00-06:00)

### F027 [architect] critical
**Description:** Aspect rename_propagation отсутствует — `renameComponent` обязан каскадно обновлять `quiz_configs.component_refs` (`ComponentTypeRef.UserDefined(name)` идентифицируется по имени); без cascade UPDATE → silent quiz breakage; aspect `quiz_configs_cleanup` покрывает только soft-delete.
**Status:** approved
**Verdict:** UserDefined идентифицируется по name (verified `ComponentTypeRef.kt`, `QuizGameImpl.matchesRef`); rename без cascade UPDATE → silent quiz breakage.

### F028 [architect] critical
**Description:** Best-guess (B) для F018/F021 «убрать UNIQUE из Room index» на `component_types(dictionary_id, name)` несовместим со списком миграции — DROP UNIQUE упомянут только на `component_values`.
**Status:** approved
**Verdict:** Open question best-guess (B) и db_migration aspect рассогласованы — реальная дыра в классификации.

### F029 [architect] major
**Description:** `public_contract_change` aspect фиксирует только «добавить новые методы», но упразднение `ComponentValueData` меняет сигнатуры существующих публичных методов (`addLexemeWithBuiltInComponent`, `addLexemeWithUserDefinedComponent`, `addLexemeWithComponents`, `addComponentValue`, `updateComponentValue`, `addLexemeWithTranslation`) — breaking change не классифицирован.
**Status:** approved
**Verdict:** Breaking signature change существующих publicAPI методов не в aspect.

### F030 [architect] minor
**Description:** Open question про `ComponentApi` vs `LexemeApi` обосновывает best-guess неверным утверждением «LexemeApi уже содержит `flowComponentTypes`» — в реальном API только `getComponentTypes` (suspend), Flow-варианта нет.
**Status:** approved
**Verdict:** Обоснование шага должно опираться на реальное API.

### F031 [qa_engineer] critical
**Description:** `LexemeDbEntity.componentValueListDb` использует Room `@Relation` без WHERE-фильтра — после M13 будет подтягивать soft-deleted `component_values`. `@Relation` не поддерживает WHERE — нужен post-load filter в `toApiEntity()` либо custom `@Query`. `LexemeDbEntity.kt` отсутствует в Затронутых файлах.
**Status:** approved
**Verdict:** @Relation без WHERE = soft-deleted values утекают в UI/quiz; LexemeDbEntity.kt отсутствует в files, стратегия не зафиксирована.

### F032 [qa_engineer] critical
**Description:** Стратегия uniqueness в `soft_delete_unique_collision` (`SELECT WHERE dictionary_id=? AND name=? AND removed_at IS NULL`) ломается для global user-defined (`dictionaryId=null` per ui_placement.md). SQL `dictionary_id = NULL` всегда UNKNOWN — два global с одним именем создаются молча.
**Status:** approved
**Verdict:** Nullability branch обязательна (`dictionary_id IS NULL`) либо явное «user-defined ВСЕГДА per-dict».

### F033 [qa_engineer] critical
**Description:** Cleanup pref `quiz_picker_dict_<id>` (per-dictionary quiz picker selection из IS481 предыдущей фичи) при soft-delete не классифицирован. После soft-delete pref остаётся stale → следующая quiz session попытается восстановить выбор по удалённому ref.
**Status:** approved
**Verdict:** Pref cleanup при soft-delete component_type не классифицирован; quiz_configs_cleanup покрывает только quiz_configs.component_refs.

### F034 [qa_engineer] major
**Description:** DROP UNIQUE `Index(["dictionary_id","name"])` на `component_types` (нужен стратегией B из Open question F018/F021) не зафиксирован в `db_migration` — только DROP UNIQUE на `component_values`. (Дубль F028.)
**Status:** approved
**Verdict:** Дубль F028, та же дыра в классификации.

### F035 [qa_engineer] minor
**Description:** Aspect `cardinality_safety` (insertSingleSafe @Transaction) не уточняет что SELECT должен фильтровать `removed_at IS NULL` — иначе soft-deleted value на `is_multi=false` блокирует пересоздание.
**Status:** approved
**Verdict:** SELECT фильтр removed_at IS NULL обязателен; scope-aspect не сшит с dao_convention.

## Итоги итерации 3

- **Approved:** 9 (все 9) — 7 critical + 2 minor (F029, F034 — major).
- **Rejected:** 0.
- **Решение:** repeat (есть approved critical).

---

## Итерация 4 (2026-06-15T06:30:00-06:00)

### F036 [architect] minor
**Description:** scope утверждает 6 методов меняют сигнатуру `ComponentValueData → TemplateValues`, но `addLexemeWithTranslation` принимает `TranslationApiEntity`, не `ComponentValueData` — реальное число = 5.
**Status:** approved
**Verdict:** addLexemeWithTranslation принимает TranslationApiEntity, реальное число breaking-signature = 5.

### F037 [architect] major
**Description:** `QuizConfigDao.kt` отсутствует в Затронутых файлах, хотя aspect `quiz_configs_cleanup` требует новых query или использования update(config) — файл затронут в любой стратегии.
**Status:** approved
**Verdict:** QuizConfigDao.kt затронут в любой реализационной стратегии quiz_configs_cleanup.

### F038 [architect] minor
**Description:** scope упоминает «Поле removeDate в WordDb НЕ трогать», но `WordDb` не имеет такой колонки — только id/dictionary_id/value/add_date/change_date.
**Status:** approved
**Verdict:** WordDb без removeDate; поле живёт только в WordApiEntity (dead field).

### F039 [qa_engineer] critical
**Description:** DROP UNIQUE `(dictionary_id, name)` на `component_types` ломает identity-инвариант `ComponentTypeRef.UserDefined(name)` — две независимые ветки SELECT разрешают active global "Foo" + per-dict "Foo" одновременно; `matchesRef` и cascade rename / prefs cleanup становятся неоднозначны.
**Status:** approved
**Verdict:** Cross-scope коллизия имён не классифицирована; soft_delete_unique_collision разветвляет SELECT по scope, но не предотвращает кросс-scope дубль.

## Итоги итерации 4

- **Approved:** 4 (все 4) — 1 critical + 1 major + 2 minor.
- **Rejected:** 0.
- **Решение:** repeat (есть approved critical).

---

## Итерация 5 (2026-06-15T07:30:00-06:00)

### F040 [qa_engineer] critical
**Description:** `RoomModule.kt` отсутствует в Затронутых файлах — точка регистрации миграций; без обновления `.addMigrations(...)` upgrade 12→13 уйдёт в destructive fallback → data loss.
**Status:** approved
**Verdict:** RoomModule.kt содержит реальный `.addMigrations(Migration_011_to_012)` + destructive fallback callback; без M13 в этом списке миграция не подключится.

### F041 [qa_engineer] critical
**Description:** Routes регистрируются НЕ в `MainActivity.kt`/`Constants.kt`/`NavEntity.kt` (там только LOG_TAG / data class) — реальная регистрация в `modules/screen/main/Settings.kt`/`Vocabulary.kt`/`Quiz.kt`. Scope mis-classified навигационные файлы.
**Status:** approved
**Verdict:** MainActivity содержит `RootRouter(...)` без composable; реальные NavGraphBuilder extensions живут в `modules/screen/main/`. Mis-classification.

### F042 [qa_engineer] critical
**Description:** `CompositionRoot.kt` + `CompositionRootImpl.kt` отсутствуют в Затронутых файлах — конвенция требует `ComponentsManagerScreenDep` / `PerDictionaryComponentsScreenDep` методов; без них экраны не получат DI.
**Status:** approved
**Verdict:** Per-screen *ScreenDep convention реально в коде; без новых dep-методов экраны не подключатся.

## Итоги итерации 5

- **Approved:** 3 (все 3 critical, от qa). Architect выдал PASS.
- **Rejected:** 0.
- **Решение:** repeat (есть approved critical).

---

## Итерация 6 (2026-06-15T08:30:00-06:00)

### F043 [architect] critical
**Description:** DictionaryAppBar рендерится в 3 табах (Vocabulary/Quiz/Statistic) через CompositionRootImpl; артефакт описывает icon-button «молоток» так, будто только в dictionary-tab. Хаммер появится во всех трёх табах — wiring lambda нужен для каждого host'а; артефакт умалчивает.
**Status:** approved
**Verdict:** Shared widget multi-instance wiring не классифицирован; concept фиксирует «на всех трёх табах» — scope этот мульти-инстанс не отражает.

### F044 [architect] critical
**Description:** Migration_011_to_012 вызывает seedBuiltIns(connection). Артефакт обновляет SeedBuiltIns под M13-схему, но user_version=11 → M11→M12 → обновлённый seed → runtime fail «no such column». Стратегия (frozen-копия / schema-aware / перенос в M13) не указана.
**Status:** approved
**Verdict:** Aspect db_migration не фиксирует strategy для fresh-install/upgrade-path conflict с seedBuiltIns.

### F045 [architect] critical
**Description:** SettingsTab wiring chain неполный — не упомянуты Message.kt / SettingsNavigationEffect.kt / SettingsTabReducer.kt. Для DictionaryAppBar — DictionaryAppBarNavigationEffect.kt пропущен.
**Status:** approved
**Verdict:** File classification gap — без этих файлов handler не получит effect и navigator не вызовется.

### F046 [architect] critical
**Description:** «template-key long_text → text consolidation» как goal в aspects, но в migration SQL списке отсутствует UPDATE existing component_types rows. Без него existing long_text компоненты станут невидимы (fromKey=null) — data loss.
**Status:** approved
**Verdict:** Classification gap aspect→file mapping; без UPDATE rows существующие long_text молча исчезают.

### F047 [architect] major
**Description:** WordCardUseCase / QuizChatUseCase deps boundaries не в aspect public_contract_change — только CoreDbApi.LexemeApi.
**Status:** rejected
**Verdict:** Aspect domain_rewrite фиксирует «Breaking change для всех callsite (wordcard, quiz/chat)»; aspect public_contract_change резонно ограничен публичной библиотечной границей CoreDbApi.

### F048 [architect] critical
**Description:** Statistic tab (StatisticTabScreen) делит DictionaryAppBar — не упомянут в § Затронутые файлы. После правки widget'а StatisticTabScreenDep тоже получит новый lambda parameter либо нужно скрытие icon. Scope-решения нет.
**Status:** approved
**Verdict:** Classification gap — concept ui_placement.md фиксирует «на всех трёх табах», но scope не упоминает statTab DI integration.

## Итоги итерации 6

- **Approved:** 5 (F043, F044, F045, F046, F048) — 4 critical + 1 major.
- **Rejected:** 1 (F047).
- **Решение:** repeat (есть approved critical). Это **iter 7 — последняя итерация** по max=7. Если в iter 7 опять approved critical — `on_max: escalate`.

---

## Итерация 7 (2026-06-15T09:30:00-06:00) — last by max=7

### F049 [architect] critical
**Description:** Aspect `prefs_cleanup_on_soft_delete` (F033) описывает cleanup quiz_picker_dict_<id> при soft-delete, но `modules/datasource/prefs/` (`PrefsProvider.kt`, `QuizPickerPrefKey.kt`) и `QuizPickerFlowHandler.kt` отсутствуют в § Затронутые файлы. Симметрично F037 (QuizConfigDao gap).
**Status:** approved
**Verdict:** Classification gap — aspect документирован, но file list неполон. PrefsProvider не имеет scan-by-prefix → требуется или новый метод, или UseCase-композиция через DictionaryApi.getAllDictIds().

QA на iter 7: PASS.

## Итоги итерации 7

- **Approved:** 1 critical (F049). Architect нашёл, qa PASS.
- **Решение:** max=7 исчерпан → `on_max: escalate`. Пользователь в autonomy сказал «продолжай хуила» = «да» на escalate prompt → execute_repeat recurse iter 8.

---

## Итерация 8 (2026-06-15T10:00:00-06:00) — post-max recurse

### F050 [qa_engineer] minor
**Description:** Open question F049 (best-guess B) и описание PrefsProvider.kt ссылаются на несуществующий `DictionaryApi.getAllDictIds()`. Реальный API — `getDictionaryList(): List<DictionaryApiEntity>` либо `flowDictionaryList()` Flow-вариант. Симметрично F030.
**Status:** approved
**Verdict:** Метод getAllDictIds() не существует; обоснование Open question на ложном API.

Architect на iter 8: PASS.

## Итоги итерации 8

- **Approved:** 1 minor (F050). Critical = 0.
- **Решение:** minor-only итерация. `step.minor_only_streak = 1`. По review module — repeat (streak < 2). Если iter 9 опять только minor → streak=2 → PASS with tech debt.

---

## Итерация 9 (2026-06-15T10:30:00-06:00)

Оба ревьюера: **PASS** (нет findings).

## Итоги итерации 9

- **Approved:** 0.
- **review_passed = true.** Но `require_clean_iteration=true` и iter 9 changes_made=true (фикс F050) → нужна iter 10 без изменений артефакта для clean exit.

---

## Итерация 10 (2026-06-15T10:45:00-06:00) — clean iteration check

Sub-agent не вносил изменений. Оба ревьюера: **PASS**.

## Итоги итерации 10

- **Approved:** 0.
- **review_passed = true.** changes_made = false → exit с execute_repeat.

**scope_analysis ЗАКРЫТ как done.** Финальный артефакт — `02_scope.md`. Все 8 context-переменных установлены.

Всего по шагу: 10 итераций, 38 approved findings (5+10+9+4+3+5+1+1+0+0 после F047 rejected), 1 на review iter 8 minor + 1 clean check. После rejected (F003/F005/F006/F007/F010/F011/F012/F013/F014/F015/F020/F047 = 12) — итог 38 закрытых правок в scope.
