# IS493 / Э3 — План реализации: создание групп (корневой уровень)

Статус: **v2** (2026-08-12, после ревью 5 агентов и триажа — 39 находок;
принятое внесено, отклонённое с аргументами — в
[stage3_design_tree.md](stage3_design_tree.md) §«Отклонённые находки»).
v1 — 2026-08-11. Решения В1–В3 + уточнения v2 — см. design tree (D12–D16).
Основа: [rollout_stages.md](rollout_stages.md) Э3,
[architecture.md](architecture.md) §2.3–2.5, §3.2.

**TDD:** unit — честный red прогоном; androidTest — red = ошибка компиляции
до появления API (зафиксировано осознанно, ревью T-3): тесты пишутся
первыми, прогон на девайсе — сразу после зелёной компиляции фазы, факт
прогона фиксируется в отчёте фазы.

**Скоуп:** группы Э3 пустые (слова — Э5, подгруппы — Э4): счётчик «0»,
раскрытие — заглушка. БД не трогается (миграций нет). androidTest — руками
на девайсе (CI не гоняет).

## Коммит-нарезка

1. Фаза 1 «domain: валидации + DisplayTree с группами» — default'ы
   сохраняют Э2-вызовы зелёными (T-6).
2. Фаза 2 «данные: dep в core-db-api + транзакционные мутации + groupTree»
   (+androidTest, прогон на девайсе).
3. Фаза 3 «groupstab: handle + шторка/фильтр/kebab/конфирм + мост».
4. Фаза 4 «верификация + мануал + док-синк» — отдельный док-коммит (T-7).

---

## Фаза 1. Domain: валидации + DisplayTree с группами

- [ ] **ТЕСТЫ СНАЧАЛА (red прогоном)** — `modules/domain/group`:
  - normalize: trim; NFC на конкретных кодпоинтах (T-4): «й» =
    U+0438+U+0306 → U+0439 — assert: входы различны посимвольно, после
    normalize равны; ⚠️ «е́» непригодно (нет композитной формы);
  - validateGroupName: пустое/пробельное → EmptyName; валидное →
    `Valid(normalizedName)` — нормализованное имя ИЗ валидатора (D12.1);
  - дубликат живых сиблингов case-insensitive («Дом» vs «дом») →
    DuplicateSibling;
  - **self-exclusion (D-1/T-1):** валидатор с `excludeName`/списком без
    self — смена регистра своего имени → Valid; no-op rename → Valid;
  - резерв из set («Все»/«All») → ReservedName;
  - buildDisplayTree: группы отсортированы компаратором — **кейс
    кириллицы** («Ёлка»/«Яма»/«Аист» — фиксирует Collator, A-E);
    subtreeWordCount по groupId (0 для пустых); AllWords первым;
    **membership с неизвестным groupId → drop, слово остаётся в «Все»
    (контракт combine-skew, A-D)**; fail-soft битого parent.
- [ ] Код:
  - `normalizeGroupName`; `validateGroupName(raw, livingSiblingNames,
    reservedNames, locale): NameCheck` → `Valid(normalized)` | ошибки;
  - outcomes (D12.2): CreateGroupOutcome (+ **ParentHasWords — задел Э6**,
    A-C), RenameGroupOutcome, DeleteGroupOutcome;
  - `GroupNode(id, parentGroupId, name)`;
  - `buildDisplayTree(slice, groups = emptyList(), comparator =
    naturalOrder())` — default'ы сохраняются (T-6), риск Collator закрыт
    тестом + мутацией.

## Фаза 2. Данные: dep + транзакционные мутации + groupTree

- [ ] **`core-db-api/build.gradle.kts`: `api(project(":modules:domain:group"))`**
  — по прецеденту lexeme (`core-db-api:35`); impl видит транзитивно;
  в core-db-impl НИЧЕГО не добавляется (исправление F-1/A-A).
- [ ] **androidTest СНАЧАЛА** (`GroupMutationsTest`; инстанцирование —
  ручная сборка `CoreDbApiImpl.GroupApiImpl` по образцу
  `Is486DataLayerTest`, БЕЗ Dagger — T-5):
  - createGroup Success → строка в БД, имя НОРМАЛИЗОВАННОЕ;
  - createGroup(" Дом ") при живой «Дом» → DuplicateSibling (нормализация
    на API-уровне, D-6в);
  - NFC на БД: create разложенного «й...», затем композитного →
    DuplicateSibling; хранимая форма композитная (T-4);
  - дубль поверх soft-deleted имени → Success;
  - резерв → ReservedName; **rename в резерв → ReservedName (T-5)**;
  - **rename: смена регистра своего имени → Success (self-exclusion,
    D-1/T-1)**; rename на занятое чужое → DuplicateSibling;
    rename несуществующей → NotFound; **rename soft-deleted → NotFound
    (liveness, T-5)**;
  - **create имени, освобождённого rename'ом (A→B, create A) → Success
    (T-5)**;
  - deleteGroup Success → removed_at; его word_groups hard-deleted,
    **строка ДРУГОЙ группы переживает (скоуп, D-6б)**; повторный delete →
    NotFound;
  - groupTree: переэмит ПОСЛЕ createGroup (инвалидация insert'ом —
    D-6а) и после deleteGroup (без удалённой);
  - membershipSlice после deleteGroup: слово мёртвой группы → (wordId, null).
- [ ] DAO (`GroupDao`): `getLivingGroupById(id)` (A-F),
  `livingGroups(dictionaryId)`, `flowLivingGroups(dictionaryId)`,
  `updateGroupName(id, name, updatedAt): Int` — `WHERE id = :id AND
  removed_at IS NULL`, rowcount 0 → NotFound (D-5);
  `softDeleteGroups(ids, removedAt)` / `hardDeleteWordGroupsByGroups(ids)`
  — **list-формы** (задел каскада Э4, D-4).
- [ ] `CoreDbApi.GroupApi`: createGroup/renameGroup/deleteGroup +
  `groupTree(dictionaryId): Flow<List<GroupApiEntity>>`;
  `GroupApiEntity(id, dictionaryId, parentGroupId, name)`. Без
  reservedNames/locale в сигнатурах (D12.3).
- [ ] `CoreDbApiImpl.GroupApiImpl`: + `database: Database` в конструктор
  (F-5) + `reservedNames: Set<String>` (конструкторная инъекция D12.3 —
  app провайдит при сборке CoreDbProvider-компонента из ресурсов всех
  локалей; точку @Provides определить по факту сборки компонента);
  мутации целиком в `useWriterConnection { immediateTransaction }`;
  rename: `getLivingGroupById` → сиблинги без self → validate → UPDATE.
  Locale — `Locale.getDefault()` (единый источник, D12.3).

## Фаза 3. Groupstab: handle + шторка/фильтр/kebab/конфирм + мост

### 3.1 Виджеты

- [ ] `GroupNodeWidget`: trailing-слот `actions: @Composable (() -> Unit)?
  = null`; порядок: title(weight) → счётчик → шеврон → kebab; высоту с
  48dp IconButton сверить в Preview (U-8).
- [ ] Kebab: пункты через `MenuItem.withIcon(EditIcon, «Переименовать»,
  ...)` / `(DeleteIcon, «Удалить», ...)` — готовых пунктов в iconDropDowned
  НЕТ, только иконки (F-2); образец сборки — `DeleteWordMenuItem`
  (wordcard).
- [ ] `GroupBottomSheetWidget`: state-driven ModalBottomSheet (рендер по
  `sheet != null`), **`scrimColor = Transparent`** (В1 v2), поле + submit,
  inline-ошибка, дизейбл при isSubmitting.
- [ ] Конфирм по образцу `ConfirmDeleteWordWidget`/`AlarmDialogWidget`.

### 3.2 Mate-цикл (ТЕСТЫ СНАЧАЛА, red прогоном)

- [ ] Reducer-тесты (дополнено T-2/U-3):
  - `OpenCreateSheet` → sheet Create, input пуст; при открытом
    confirmDelete → игнор (или закрыть конфирм — выбрать и зафиксировать);
  - `OpenRenameSheet(id)` → sheet Rename, input предзаполнен;
    **фильтр НЕ активен в Rename (T-10)**;
  - `SheetInputChanged` (Create) → visibleGroups отфильтрованы (префикс,
    case-insensitive; «Все» вне фильтра); пустой ввод → все;
  - `SubmitSheet` → эффект + isSubmitting; повторный submit → игнор;
    ввод «только пробелы» — уходит в effect, вернётся EmptyName
    (второго валидатора в reducer НЕТ — зафиксировано, T-2);
  - `SheetOutcome(Success)` → шторка закрыта, фильтр сброшен;
    ошибки → шторка открыта, error, isSubmitting снят;
    **`SheetOutcome` при `sheet == null` → no-op (гонка dismiss, U-3)**;
    `RenameGroupOutcome.NotFound` → шторка закрыта (D15.2);
  - **`DismissSheet` → sheet null, фильтр сброшен, visibleGroups
    восстановлены (T-2)**;
  - `RequestDelete`/`DismissDelete`/`ConfirmDelete` → эффект, диалог
    закрыт; `DeleteOutcome(Success|NotFound)` → no-op state;
  - **`SliceLoaded` вычищает из `expandedGroups` id, отсутствующие в
    живых группах (T-2)**; groups в state отсортированы (из DisplayTree);
    активный фильтр применён к новому списку;
  - `OpenKebab(id)`/`DismissKebab` → `openMenuGroupId` (U-2/F-3);
  - `ToggleGroup(id)` → expandedGroups toggle; содержимое — заглушка;
  - смена словаря → sheet/конфирм/фильтр/expand/kebab сброшены.
- [ ] State: + `groups`, `visibleGroups`, `sheet`, `confirmDelete`,
  `openMenuGroupId`, `expandedGroups` — все явные (D15.4).
- [ ] Handler: slice-ветка → `combine(membershipSlice, groupTree)` с
  **`distinctUntilChanged()` на обоих источниках (D-7)** →
  `buildDisplayTree(slice, groups, comparator)` — оба аргумента явно;
  comparator — конструктор handler'а (маршрут: мост → VM factory →
  handler, F-4); мутации → runCatching → outcome-Msg.

### 3.3 Handle + UI + мост

- [ ] **`rememberGroupsTabHandle(factory, navigator): GroupsTabHandle`**
  (U-1 — отдельный пункт работ, рефакторинг Э2-кода): VM переезжает из
  дефолт-параметра `GroupsTabScreen` в handle (по образцу
  `rememberWordsTabHandle`); наружу: `isFabVisible =
  derivedStateOf { sheet == null && confirmDelete == null }`, `onFabClick`,
  `Content(dictionaryId, isDictResolved)` — LaunchedEffect-проводка
  словаря остаётся внутри Content. Проверить VM-owner (тот же
  NavBackStackEntry — прецедент words).
- [ ] UI: список = «Все» (Э2 не тронут) + visibleGroups строками
  GroupNodeWidget (kebab в trailing, у «Все» null) + заглушка «пусто» под
  раскрытой группой; ключи: **`"node:group:{id}"`, `"empty:group:{id}"`**
  (U-4; Э2 занял "node:all"/"all:{id}"/"footer:all"); шторка; конфирм.
- [ ] Строки core-resources (values + values-ru-rRU): титулы шторки,
  hint, «Переименовать» (новая — в iconDropDowned её нет, F-2),
  «Удалить группу?», ошибки (пустое имя/занято/зарезервировано), «пусто».
- [ ] Мост: `tabs = remember(words, groups)`; groups-FabSpec:
  `iconRes = core_resources.R.drawable.ic_add` (F-6), visible/onClick из
  handle. Спеки стабильны (D1.3).
- [ ] `GroupsTabUseCaseImpl` (app): + мутации (тонкая делегация без
  reservedNames — те в DI core-db), `groupTree` → `GroupNode`; unit-тест.

## Фаза 4. Верификация + мануал + док-синк (отдельный коммит)

- [ ] Тесты последовательно: domain:group, groupstab, app; build; lint.
- [ ] androidTest (GroupMutationsTest + регресс Э2-сюит) — на девайсе,
  факт прогона в отчёт.
- [ ] Мутационная проверка (расширено T-8): валидатор (резерв/дубль/self),
  фильтр, guard isSubmitting, **normalize (убрать NFC → тест падает)**,
  **транзакция delete (убрать hardDeleteWordGroups → androidTest падает)**,
  **combine (убрать groupTree → SliceLoaded-тест падает)**,
  компаратор (naturalOrder вместо Collator → кириллица-тест падает).
- [ ] `stage3_manual_test.md` по формату гайда: создание/переименование/
  удаление; дубль «Дом»/«дом»; резерв — **два шага со сменой локали
  устройства (ru → «Все», en → «All»; T-9)**; фильтр при вводе (виден
  сквозь прозрачный скрим — U-5); «Все» первая + алфавит; kebab
  отсутствует у «Все»; конфирм; пустая группа → заглушка; рестарт;
  смена словаря; rename-скачок позиции (известное поведение А8);
  FAB скрыт под шторкой/конфирмом.
- [ ] Док-синк: rollout_stages Э3 (пометки В1–В3 + **snackbar → inline**,
  A-H); architecture §2.5 (**+ ParentHasWords в createGroup — синхрон с
  А18**, A-C; NotFound = отсутствует ∪ soft-deleted для rename/delete,
  D-5).

## Вне скоупа Э3

- Подгруппы, каскад, deleteImpact — Э4 (list-DAO уже готовы).
- Слова в группах, контент листа (JOIN-окно), счётчики > 0 — Э5.
- Инвариант «папка/лист» в транзакциях — Э6 (ветка outcome — задел есть).
- Reparent — вне v1.
