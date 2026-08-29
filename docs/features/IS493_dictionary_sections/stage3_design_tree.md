# IS493 / Э3 — Design tree

Статус: **v2** (2026-08-12, после ревью 5 агентов: архитектура A, данные D,
UI U, факты F, тесты T — 39 находок, триаж оркестратора; принятое внесено в
узлы, отклонённое — в конце). v1 — 2026-08-11. Узлы D12–D16 (сквозная
нумерация Э1–Э3). Решения пользователя В1–В3 (2026-08-11). Основа:
[rollout_stages.md](rollout_stages.md) Э3, [architecture.md](architecture.md)
§2.3–2.5, §3.2, А3/А4/А7/А8/А14/А17; наработки Э2 —
[stage2_design_tree.md](stage2_design_tree.md).

**Скоуп-уточнение:** в Э3 слова в группы НЕ попадают (пикер — Э5) — все
группы пустые: счётчик «0», раскрытие — строка-заглушка «пусто». Живой
контент листа (JOIN-окно по образцу D10.1 v3) — Э5. Подгрупп нет (Э4) —
все создаваемые группы корневые, `parentGroupId = null`.

## Решения пользователя (В1–В3, 2026-08-11)

- **В1.** Создание/переименование — ШТОРКА снизу (образец
  `AddWordBottomSheetWidget`); при вводе имени список групп позади
  live-фильтруется (как слова на «Словах»). Фильтр — префиксный,
  case-insensitive, in-memory. Уточнения v2: фильтр активен ТОЛЬКО в
  Create-режиме (в Rename предзаполненное имя схлопнуло бы список до самой
  группы — ревью T-10/U-5); скрим шторки групп — прозрачный
  (`scrimColor = Transparent`, параметр M3): затемнение дефолтного скрима
  гасит смысл В1 «видеть похожие/дубли» (ревью U-5).
- **В2.** Конфирм удаления — ВСЕГДА, начиная с Э3 (простой «Удалить
  группу?»). Задел формулировки для непустых (Э4/Э6): «в группе были
  слова — они останутся в общем списке, группа удалится».
- **В3.** Kebab (⋮) — виджет `iconDropDowned`. Уточнение v2 (ревью F-2/F-3):
  «готовые» там только ИКОНКИ (`DeleteIcon`/`EditIcon`); пункты собирает
  потребитель через `MenuItem.withIcon(icon, title, onClick)` (образец —
  `wordcard/widget/DeleteWordMenuItem.kt`; боевые потребители — wordcard
  TopBarWidget и quiz/chat ActionsWidget, НЕ ActionTopBar words). Строка
  «Переименовать» — новая в core-resources.

```
Э3. Создание групп: корневой уровень
├── D12. domain/group: валидации + outcomes
│   ├── D12.1 normalize: Valid(normalized) — одна точка нормализации
│   ├── D12.2 Outcomes: + задел ParentHasWords (Э6); «финальность» честная
│   └── D12.3 Резерв имён и locale: конструкторная инъекция в impl
├── D13. Данные: транзакционные мутации + groupTree
│   ├── D13.1 §2.4: read→validate→write; rename исключает self
│   ├── D13.2 Зависимость: core-db-api → domain/group (api), impl транзитивно
│   ├── D13.3 GroupApi: мутации без reservedNames в сигнатурах + groupTree
│   └── D13.4 Удаление: list-формы DAO (задел каскада Э4), rowcount-NotFound
├── D14. DisplayTree с группами
│   ├── D14.1 buildDisplayTree(+groups); контракт неизвестного groupId
│   └── D14.2 Компаратор/locale: Collator/Locale.getDefault() — явно
├── D15. Groupstab: handle, шторка, фильтр, kebab, конфирм
│   ├── D15.1 Шторка — общий InputBottomSheetWidget; фильтр Create-only
│   ├── D15.2 Ошибки — inline под полем; снекбар только для гонки (v5)
│   ├── D15.3 Kebab: openMenuGroupId в state; конфирм всегда
│   └── D15.4 State/Msg/Reducer: расширение Э2-цикла; ключи LazyColumn
└── D16. Host/мост: rememberGroupsTabHandle, FAB групп
```

---

## D12. domain/group: валидации + outcomes

### D12.1 normalize — одна точка

- **Решение (уточнено ревью D-3):** `validateGroupName(raw,
  livingSiblingNames, reservedNames, locale)` возвращает
  `Valid(normalizedName)` | ошибку — нормализованное имя рождается В
  валидаторе, impl пишет в БД ровно его. Двух независимых точек normalize
  (сравнение отдельно, запись отдельно) не существует — рассинхрон
  хранения и сравнения исключён конструктивно.
- normalize = trim + NFC; сравнение — lowercase(locale) обеих сторон;
  хранится нормализованное. Та же функция — для импорта публикации (А14).
- NFC-тест — на конкретных кодпоинтах (ревью T-4): «й» = U+0438+U+0306 →
  U+0439 (или «ё»); кириллическое «е́» НЕ имеет композитной формы — для
  теста непригодно. Assert: входы различны посимвольно, после normalize
  равны; хранимая форма — композитная.

### D12.2 Outcomes — честная финальность

- **Решение (уточнено ревью A-C):**
  - `CreateGroupOutcome`: Success(groupId) | EmptyName | DuplicateSibling |
    ReservedName | ParentNotFound (задел Э4) | **ParentHasWords (задел Э6,
    А18)** — ветка добавляется СЕЙЧАС, чтобы when-потребители не
    переписывались в Э6;
  - `RenameGroupOutcome`: Success | EmptyName | DuplicateSibling |
    ReservedName | NotFound;
  - `DeleteGroupOutcome`: Success | NotFound.
- `NotFound` = отсутствует ∪ soft-deleted (распространение А17 на
  rename/delete — ревью D-5).
- `DuplicateSibling` — уникальность среди ЖИВЫХ сиблингов; **при rename
  из сиблингов исключается сама переименовываемая группа** (ревью
  D-1/T-1 — иначе смена регистра «дом»→«Дом» и no-op rename упрутся в
  self-collision). Оба кейса — обязательные тесты.
- Синхронизация architecture §2.5 (там createGroup без ParentHasWords —
  рассинхрон с А18) — док-синк фазы 4.

### D12.3 Резерв имён и locale — конструкторная инъекция

- **Решение (пересмотр v1 по ревью A-B/D-2):** `reservedNames` и `locale` —
  НЕ параметры каждого вызова GroupApi. Per-call параметр делает инвариант
  «резерв занят» заботой каждого вызывающего: второй клиент (Э7 — пикер
  карточки) обязан продублировать проводку, забыл → инвариант молча
  выключен. Вместо этого — конструкторная инъекция в
  `CoreDbApiImpl.GroupApiImpl` через DI-граф core-db: app при сборке
  CoreDbProvider-компонента провайдит `Set<String>` (из ресурсов
  `group_all_title` всех локалей) — Р7 соблюдён по духу (набор перечисляет
  UI-слой, в валидатор внутри транзакции он попадает параметром).
- **Locale — единый источник, зафиксирован явно** (ревью D-2/U-6):
  `Locale.getDefault()` / `Collator.getInstance()` на всех точках вызова
  (impl-транзакции, reducer-фильтр, компаратор дерева). Domain-функции
  остаются zero-deps (locale — параметр). Разные locale на разных слоях —
  расхождение compare-семантик (турецкая i) — исключены единым источником.

## D13. Данные: транзакционные мутации + groupTree

### D13.1 Транзакции §2.4

- **Решение (форма подтверждена ревью по 12 боевым блокам
  `CoreDbApiImpl`):** каждая мутация — `database.useWriterConnection {
  it.immediateTransaction { SELECT живых → validateGroupName (domain) →
  запись } }`; suspend DAO-вызовы внутри транзакции — обкатанный прецедент
  IS481/IS486. `GroupApiImpl` дополнительно инжектит `database: Database`
  (сейчас только groupDao — ревью F-5; прецедент — LexemeApiImpl).
- Rename: SELECT сиблингов `WHERE id != :groupId` (self-exclusion, D12.2);
  предварительно `getLivingGroupById(id)` — резолв dictionaryId и
  liveness (ревью A-F).
- Use case-слой groupstab — тонкая делегация. Конкурентность тестами не
  гоняем (осознанное ограничение §2.4).

### D13.2 Зависимость — в core-db-api

- **Решение (исправлено по ревью A-A/F-1):** «прецедент core-db-impl →
  domain/lexeme» БЫЛ ЛОЖНЫМ — реальный прецедент:
  `core-db-api/build.gradle.kts:35` — `api(project(":modules:domain:lexeme"))`,
  impl видит типы транзитивно. Outcomes domain/group входят в сигнатуры
  `CoreDbApi.GroupApi` → dep добавляется в **core-db-api** как
  `api(project(":modules:domain:group"))`; core-db-impl — ничего.

### D13.3 GroupApi

- **Решение:** расширение `CoreDbApi.GroupApi`:
  - `suspend createGroup(dictionaryId, parentId: Long? = null, name):
    CreateGroupOutcome`;
  - `suspend renameGroup(groupId, name): RenameGroupOutcome`;
  - `suspend deleteGroup(groupId): DeleteGroupOutcome`;
  - `groupTree(dictionaryId): Flow<List<GroupApiEntity>>` — живые группы.
  Без reservedNames/locale в сигнатурах (D12.3).
- `GroupApiEntity(id, dictionaryId, parentGroupId, name)` — audit-поля не
  нужны (deleteImpact Э4 идёт DAO-SQL).
- DAO: `getLivingGroupById(id)`, `livingGroups(dictionaryId)` suspend,
  `flowLivingGroups(dictionaryId)`, `updateGroupName(id, name, updatedAt)`
  — UPDATE c `WHERE id = :id AND removed_at IS NULL`, возврат Int
  (rowcount 0 → NotFound, атомарно — ревью D-5), аналогично
  `softDeleteGroups(ids, removedAt)` и
  `hardDeleteWordGroupsByGroups(ids)` — **list-формы сразу** (Э4-каскад
  работает списком поддерева; Э3 передаёт список из одного — ревью D-4).

### D13.4 Удаление

- **Решение (§3.2):** одна транзакция — liveness-check → soft-delete
  узлов (список) + hard-delete их строк `word_groups`. Форма готова к
  каскаду Э4 (там добавится пересчёт поддерева по БД).

## D14. DisplayTree с группами

### D14.1 buildDisplayTree с группами

- **Решение:** `buildDisplayTree(slice, groups: List<GroupNode> =
  emptyList(), comparator = naturalOrder())` — default'ы СОХРАНЯЮТСЯ
  (коммит фазы 1 зелёный без правок groupstab — ревью T-6); риск «забытый
  Collator» (ревью A-E) закрывается НЕ сигнатурой, а тестом сортировки
  кириллицы + мутационной проверкой (подмена компаратора → тест падает).
  В фазе 3 handler передаёт оба аргумента явно.
- `GroupNode(id, parentGroupId, name)` — domain-тип (приём D7.3).
- **Контракт неизвестного groupId (ревью A-D, combine-skew):** membership
  со groupId, отсутствующим во входе `groups` → строка отбрасывается
  (слово остаётся в «Все»); transient до следующей эмиссии combine. В Э3
  недостижимо, тест пишется сейчас (форма финальная). Fail-soft битого
  parent (А2) — guard-форма, как было.
- `subtreeWordCount` — distinct wordId среза по groupId (Э3: 0).

### D14.2 Компаратор

- **Решение (уточнено при реализации):** `Collator.getInstance()` (default
  locale — единый источник D12.3), обёрнут в `Comparator<String>` и создаётся
  ВНУТРИ handler'а (не прокидывается конструктором через мост — v2-маршрут
  упрощён: А8 требует инжекта параметром в domain-ФУНКЦИЮ, что соблюдено —
  handler передаёт компаратор в `buildDisplayTree` явно; тащить его через
  VM-factory не добавляло ничего, кроме церемонии). Гарантия против
  «забытого Collator» — тест кириллической сортировки + мутация М5.
  Rename-скачок позиции — принятая цена (А8).

## D15. Groupstab: handle, шторка, фильтр, kebab, конфирм

### D15.1 Шторка + фильтр (В1 v3 — правки ручного прогона 2026-08-22)

- **Решение:** `GroupBottomSheetWidget` — форма 1:1 как
  `AddWordBottomSheetWidget` (state-driven `ModalBottomSheet`, рендер по
  `state.sheet != null`, dismiss → Msg), режимы Create/Rename
  (предзаполнено). **Скрим — ДЕФОЛТНЫЙ, как у words** (правка юзера:
  «точно как во вкладке слова» — без затемнения не видно границ шторки;
  прозрачный скрим v2 отменён). Фильтр остаётся виден сквозь затемнение.
- Фильтр: в ОБОИХ режимах (финал ручного прогона: при rename видеть
  занятые имена важнее — T-10-отключение отменено; «схлопнутый до себя»
  список при открытии rename принят); prefix, case-insensitive
  (lowercase c locale D12.3); «Все» вне фильтра; закрытие шторки
  сбрасывает фильтр. `visibleGroups` — явное поле state, пересчитывает
  reducer (SliceLoaded / ввод / сброс).

### D15.2 Ошибки и гонки (v5 — ФИНАЛ ручного прогона 2026-08-22)

- **Решение (итог трёх итераций с юзером):** при ОТКРЫТОЙ шторке ошибка —
  строкой ПОД полем (`sheet.error`), ввод сохраняется, submit
  разблокируется; ввод сбрасывает ошибку. Снекбар host'а под открытой
  M3-шторкой (отдельное окно) и клавиатурой НЕ ВИДЕН — подтверждено
  прогоном (v4-снекбар отменён), потому inline. При ЗАКРЫТОЙ шторке
  (гонка dismiss) ошибка — снекбаром host'а (`errorSnackbar` →
  LaunchedEffect в Content → `Msg.ErrorSnackbarShown`).
- Шторка — общий `InputBottomSheetWidget` (modules/core/ui, параметр
  `errorText`): единая вёрстка words/groups по требованию юзера;
  `AddWordBottomSheetWidget` words переведён на него (осиротевший
  `AddWordWidget` удалён).
  `isSubmitting` дизейблит кнопку (ignore-while-committing).
- **Guard'ы гонок (ревью U-3/T-2):** swipe/back закрывает шторку и при
  `isSubmitting` — `Success` при `sheet == null` → no-op; ОШИБОЧНЫЙ
  outcome при закрытой шторке всё равно показывает снекбар (операция
  завершилась ошибкой — сообщаем). `RenameGroupOutcome.NotFound` →
  шторку закрыть молча (группа исчезла — список обновит живая подписка);
  `DeleteOutcome(Success/NotFound)` → no-op по state, удалённый id
  вычищается из `expandedGroups` на `SliceLoaded`.

### D15.3 Kebab + конфирм

- **Решение (уточнено ревью U-2/F-3):** `IconDropdownWidget` —
  controlled-компонент (`isDropDownOpen/onClickDropDown/onDismissRequest`);
  open-state — ЯВНОЕ поле `openMenuGroupId: Long?` в GroupsTabState +
  `Msg.OpenKebab(id)` / `Msg.DismissKebab` (прецедент —
  `wordcard TopBarState.isMenuOpen`). Пункты: `MenuItem.withIcon(EditIcon,
  «Переименовать»)` / `(DeleteIcon, «Удалить»)`.
- `GroupNodeWidget`: trailing-слот `actions: @Composable (() -> Unit)? =
  null` (у «Все» — null). Порядок в строке: title(weight) → счётчик →
  шеврон → kebab; IconButton 48dp меняет высоту строки — сверить в Preview
  (ревью U-8). Тап по kebab не доходит до onToggle строки (вложенный
  clickable потребляет — подтверждено ревью).
- Конфирм — ВСЕГДА (В2), по образцу `ConfirmDeleteWordWidget`
  (`AlarmDialogWidget`).

### D15.4 State/Msg/Reducer

- **Решение:** расширение Э2-цикла (allNode-механика не трогается):
  - State: + `groups: List<GroupUiItem>`, `visibleGroups`,
    `sheet: GroupSheetState?` (mode, input, error?, isSubmitting),
    `confirmDelete: GroupId?`, `openMenuGroupId: Long?`,
    `expandedGroups: Set<Long>` — все поля явные.
  - Msg: `OpenCreateSheet`, `OpenRenameSheet(id)`, `SheetInputChanged`,
    `SubmitSheet`, `SheetOutcome`, `DismissSheet`, `RequestDelete(id)`,
    `ConfirmDelete`, `DismissDelete`, `DeleteOutcome`, `OpenKebab(id)`,
    `DismissKebab`, `ToggleGroup(id)`.
  - Эффекты: `CreateGroup(dictId, name)`, `RenameGroup(id, name)`,
    `DeleteGroup(id)`; handler → use case → outcome-Msg, `runCatching`
    (T-6-guard Э2).
  - Подписка: slice-ветка v3 расширяется до
    `combine(membershipSlice, groupTree)` → `buildDisplayTree(slice,
    groups, comparator)`; **`distinctUntilChanged()` на каждом источнике
    перед combine** (мутация групп инвалидирует обе подписки — без дедупа
    дерево пересобирается дважды, ревью D-7). Окно «Все» не трогается.
  - Ключи LazyColumn (ревью U-4): Э2 занял `"node:all"`, `"all:{id}"`,
    `"footer:all"`; группы — `"node:group:{id}"`, заглушка —
    `"empty:group:{id}"`. Дубль ключа = runtime-crash.

## D16. Host/мост: rememberGroupsTabHandle

- **Решение (закрыт вопрос v1 — ревью U-1):** groupstab получает
  **handle-паттерн по образцу words** (`rememberWordsTabHandle`):
  `rememberGroupsTabHandle(factory, navigator): GroupsTabHandle` — VM
  создаётся и коллектится ВНУТРИ модуля; наружу:
  - `isFabVisible: State<Boolean>` — `derivedStateOf { sheet == null &&
    confirmDelete == null }` (FAB скрыт под шторкой/конфирмом — как words);
  - `onFabClick` → `Msg.OpenCreateSheet`;
  - `Content(dictionaryId, isDictResolved)` — DictionaryChanged-проводка
    (LaunchedEffect) остаётся внутри Content.
  Мост: `tabs = remember(words, groups)`; groups-`FabSpec(iconRes =
  core_resources.R.drawable.ic_add — drawable живёт в core-resources
  (ревью F-6), visible = { groups.isFabVisible.value }, onClick =
  groups::onFabClick)`. Спеки стабильны (урок D1.3).
- Это рефакторинг Э2-кода: VM переезжает из `GroupsTabScreen`-дефолта в
  handle — отдельный пункт работ в плане (фаза 3.3).
- Reserved-набор имён провайдит app в DI-граф core-db (D12.3), мост его
  больше не касается.

## Отклонённые находки ревью (с аргументами)

- **A-E «убрать default'ы buildDisplayTree»** — конфликтует с зелёной
  коммит-нарезкой (T-6): default'ы остаются, риск «забытый Collator»
  закрыт тестом кириллической сортировки + мутационной проверкой.
- **U-5 вариант «принять затемнённый скрим»** — отклонён в пользу
  прозрачного скрима: затемнение гасит смысл В1 (видеть дубли при вводе).
- **Per-call reservedNames (v1 D13.3)** — снят по A-B: конструкторная
  инъекция (см. D12.3).

## Риски

- Handle-рефакторинг groupstab (D16) — VM-owner меняет точку создания;
  проверить, что `viewModel()` в handle цепляется к тому же
  NavBackStackEntry (прецедент words это уже прошёл).
- Rename-скачок позиции (А8) — в мануал.
- Транзакции: форма по 12 прецедентам, но первая транзакция с
  domain-валидатором групп — сверять по образцу checkAcyclic/planCascade.
- Combine-skew — контракт D14.1 закрывает; тест обязателен.
