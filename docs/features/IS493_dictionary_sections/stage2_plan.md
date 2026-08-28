# IS493 / Э2 — План реализации: группа «Все» — аккордеон со всеми словами

Статус: **v3** (2026-08-11: механика контента «Все» заменена на ЖИВОЕ ОКНО
по итогам ручной проверки юзера — чанк-снапшот не показывал вставки/правки;
см. design_tree D10 v3. Разделы фаз 1.4/5.x ниже описывают ФАКТИЧЕСКУЮ
реализацию: правки внесены по месту. v2 — 2026-08-10, после ревью 5 агентов
и триажа — принятые находки внесены; отклонённые и аргументы — в
[stage2_design_tree.md](stage2_design_tree.md) §«Отклонённые находки»).
Решения пользователя В1–В5 — см. «Решения этапа».
Основа: [rollout_stages.md](rollout_stages.md) Э2, [architecture.md](architecture.md)
§2.2, §2.5, §3, А7/А10/А13/А16; узлы D6–D11 —
[stage2_design_tree.md](stage2_design_tree.md); долги Э1 —
[stage1_design_tree.md](stage1_design_tree.md) D1.1, D1.6.

**TDD-порядок: сначала тесты (red), потом код (green).** Composable-слои — вне
TDD-контура (помечено); их вес несёт ручной чек-лист фазы 6.

**androidTest не покрыт CI** (workflow гоняет только lint + unit + assemble):
прогон миграционных и DAO-тестов — руками на эмуляторе в фазе 1 и повторно в
фазе 6; обязательный шаг перед merge. CI-job — в Backlog (ВекторныйПиздеж).

## Решения этапа (В1–В5, 2026-08-10)

- **В1. Виджет строки слова** → новый модуль `modules/widget/wordrow`:
  вёрстка и entities (`TermUiItem`, `LexemeUiItem`) переезжают из wordstab;
  selection-логика (`Msg`, `WordInfo`) остаётся в wordstab через колбеки
  (D8.1–D8.2). App — тоже потребитель entities (`WordsTabUseCaseImpl`) —
  правится в том же коммите (ревью F-1).
- **В2 (v3, 2026-08-11). Раскрытие «Все»** → ЖИВОЕ ОКНО: предикатный
  live-запрос `flowTermsWindow(dictId, window)` (`LIMIT`, старт
  `CHUNK_SIZE=10` — решение юзера: «50 для групп много»), кнопка «Ещё»
  расширяет `window += 10`. Вставки/правки/удаления переэмичиваются
  Room'ом сами — рассинхронов «счётчик живой / список мёртвый» нет.
  Компенсация вытеснения: рост count под раскрытым узлом → window += дельта.
  Paging 3 НЕ используется; чанк-механика v2 (дифф-нарезка,
  `wordsContent(ids)`) заменена — `getTermsByIds` остался заделом Э3.
- **В3. dictionaryId (долг D1.6)** → host резолвит
  (`flowCurrentDictId(): Flow<Long?>` — узкая сигнатура без dep на
  dictionarypicker, D9.1) и раздаёт вкладкам `DictionarySlot(id, isResolved)`
  параметром content-слота; **words НЕ мигрирует**. Полная миграция words —
  out-of-scope, долг остаётся открытым.
- **В4. Тест миграции** → androidTest `MigrationFrom12to13`: изолированный
  12→13 + **chained 11→13** (боевой путь: деплой-тег на v11) + cascade-кейс;
  **без idempotency** (чистые CREATE IF NOT EXISTS). Ручные кейсы миграции —
  чек-лист фазы 6.
- **В5. `modules/widget/grouptree`** → создаётся в Э2 (по А16), но
  `GroupNodeWidget` — ТОЛЬКО строка узла БЕЗ content-слота (ревью U-1:
  слот убил бы виртуализацию LazyColumn); аккордеон собирает groupstab на
  `LazyListScope`.
- Зафиксировано ранее: `modules/domain/group` — урезанный (`DisplayNode` +
  slice-типы + `buildDisplayTree` с дедупом wordId; компаратор параметром —
  задел); валидации/outcomes — Э3. Host-VM → Dagger-factory (D1.1).
  Expand/чанки — явные поля state, не `rememberSaveable`.

## Коммит-нарезка

1. Фаза 1 «Данные» — миграция + DAO + API; build зелёный, androidTest
   прогнаны локально на эмуляторе (критерий зелёности коммита).
2. Фаза 2 «domain/group» — отдельно (чистый Kotlin, тесты); можно слить с
   коммитом фазы 3.2.
3. Фаза 3.1 «wordrow-вынос» — атомарно: модуль + правки wordstab + правки
   app (`WordsTabUseCaseImpl` импорты, `app/build.gradle.kts`) — иначе не
   соберётся (F-1). БЕЗ смысловых правок words. После коммита — короткий
   ручной smoke «Слов» (список/выделение), не откладывая до фазы 6 (T-7).
4. Фаза 3.2 «grouptree».
5. Фаза 4 «host: словарь + Dagger» — отдельно (правит контракт TabSpec).
6. Фаза 5 «groupstab» — финальный, включает мост.

---

## Фаза 1. Данные: миграция 12→13 + slice/content API

### 1.1 Room-entities (`core-db-impl`)

- [ ] `GroupDb` (`dictionary_groups`) — образец стиля: `ComponentTypeDb` (M13):
  - `id` PK autoincrement; `dictionary_id` FK → dictionaries CASCADE,
    NOT NULL, INDEX; `parent_group_id` self-FK CASCADE, NULL, INDEX
    (прецедент — `depends_on_type_id`); `name` TEXT NOT NULL;
    `created_at` / `updated_at` NOT NULL (Date через `DateTimeConverter`);
    `removed_at` NULL (soft-delete).
- [ ] `WordGroupDb` (`word_groups`):
  - `word_id` FK → words CASCADE NOT NULL; `group_id` FK → dictionary_groups
    CASCADE NOT NULL, INDEX; `created_at` NOT NULL;
    **PK составной `@Entity(primaryKeys = ["word_id", "group_id"])`** —
    порядок колонок PK важен для сверки (word_id=1, group_id=2, D6.2);
  - индексов ровно один — `index_word_groups_group_id`; «симметричный» по
    `word_id` НЕ создавать (Room не экспортирует → провал валидации, D6.2).
- [ ] `Database.kt`: entities + `version = 13`; экспорт схемы →
  `schemas/me.apomazkin.core_db_impl.room.Database/13.json`.

### 1.2 Миграция

- [ ] `Migration_012_to_013.kt` (`room/migrations/`):
  - только DDL: `CREATE TABLE IF NOT EXISTS` × 2 + `CREATE INDEX IF NOT
    EXISTS` (defensive, как `Migration_011_to_012`);
  - имена индексов — конвенция Room `index_<table>_<col>`;
  - **DDL выверять против `13.json` руками** (Р12/D6.2): составной PK —
    новая форма; NOT NULL на всех колонках PK.
- [ ] Регистрация: `RoomModule.kt` —
  `.addMigrations(Migration_011_to_012, Migration_012_to_013)`;
  KDoc-шапка RoomModule дополняется строкой M12→M13.

### 1.3 Тест миграции (В4, D6.5)

- [ ] androidTest `MigrationFrom12to13.kt` по образцу `MigrationFrom11to12`
  (MigrationTestHelper Rule, BundledSQLiteDriver):
  - изолированный кейс: `createDatabase(12)` + данные (слово+лексема) →
    `runMigrationsAndValidate(13, ...)` — данные живы, схема совпала;
  - **chained-кейс (боевой путь)**: `createDatabase(11)` + данные →
    `runMigrationsAndValidate(13, listOf(Migration_011_to_012,
    Migration_012_to_013))`;
  - **cascade-кейс** (образец Case F 11→12): вставить группу + membership →
    удалить слово/словарь → строки `word_groups`/`dictionary_groups` ушли;
    явный `PRAGMA foreign_keys=ON` в тестовом соединении.
  - Без idempotency-теста (осознанно, В4).
- [ ] Прогнать на эмуляторе СЕЙЧАС (фаза 1), не откладывая на фазу 6 — CI
  androidTest не гоняет.

### 1.4 DAO

- [ ] **ТЕСТЫ СНАЧАЛА** (androidTest, по образцу существующих DAO-тестов):
  - `membershipSlice`: пустой `word_groups` → все слова словаря с
    `groupId = null`; чужой словарь не попадает;
  - **порядок**: строки приходят `id DESC` (assert порядка — глобальный
    порядок «Все» держится на этом, A-1/D-2);
  - **слово только в мёртвой группе** (removed_at ≠ null, строки вставить
    напрямую — API групп в Э2 нет) → `(wordId, null)`, слово НЕ потеряно;
    слово в мёртвой + живой → только живая строка (D-1);
  - **переэмит**: добавление слова → Flow переэмитит (А13/Р5). Это ПЕРВЫЙ
    Flow-DAO-тест проекта (прецедента и turbine в deps нет — ревью T-3):
    ручная конструкция — collect в фоне / `take(2).toList()` с конкурентной
    вставкой + timeout; новую библиотеку не заводим (решение триажа).
    Деградация до «дважды `first()`» запрещена — она переэмит не проверяет;
  - `getTermsByIds`: слова с лексемами по ids, `id DESC`; пустой список →
    пустой результат;
  - **(v3) `flowTermsWindow`**: LIMIT-окно соблюдается; вставка нового слова
    → переэмит с новым словом в голове окна.
- [ ] `GroupDao` (новый, `room/dao/`): `membershipSlice(dictionaryId):
  Flow<List<MembershipSliceDb>>` — ОДИН `@Query`, каноническая форма из
  D6.3 (LEFT JOIN на подзапрос живых membership, `ORDER BY w.id DESC`);
  все три таблицы в тексте запроса → трёхтабличная инвалидация Room.
- [ ] `WordDao`: **(v3) `flowTermsWindow(dictionaryId, limit):
  Flow<List<TermDbEntity>>`** — предикатный live-запрос контента «Все»
  (`WHERE dictionary_id ORDER BY id DESC LIMIT`, D6.4 v3);
  `getTermsByIds(ids)` — задел Э3 (bind-лимит 999: ids ≤50).

### 1.5 CoreDbApi (`core-db-api` + impl)

- [ ] Api-entity: `MembershipSliceApiEntity(wordId: Long, groupId: Long?)`.
- [ ] `CoreDbApi`: `membershipSlice(dictionaryId): Flow<List<MembershipSliceApiEntity>>`;
  **(v3) `flowTermsWindow(dictionaryId, limit): Flow<List<TermApiEntity>>`**;
  `getTermsByIds(ids): List<TermApiEntity>` (задел Э3).
- [ ] `CoreDbApiImpl`: делегация в DAO + мапперы. Транзакционных мутаций в
  Э2 НЕТ (группы не создаются) — §2.4-механика приходит в Э3.

## Фаза 2. Domain: `modules/domain/group` (урезанный)

- [ ] Модуль `modules:domain:group` — чистый Kotlin `kotlin("jvm")`, zero
  prod deps (образец — `modules/domain/lexeme`).
- [ ] **ТЕСТЫ СНАЧАЛА**:
  - пустой словарь → `AllWords(words = [], count = 0)`;
  - N слов без групп → `AllWords` со всеми id, count = N, порядок входа
    сохранён (не пересортирован);
  - слова с membership — ВСЁ РАВНО в AllWords («Все» = все слова, А7);
  - **дубли wordId во входе (мультичленство, задел Э3)** → в
    `AllWords.words`/`count` — distinct (D7.3/D-5);
  - группы в Э2 отсутствуют → `groups` пуст.
- [ ] Код:
  - slice-тип границы: `MembershipEntry(wordId: Long, groupId: Long?)` —
    объявляет domain/group (D7.3; Api-entity в screen-модуль не протекает);
  - `DisplayNode` ADT по architecture §2.2: `Group(...)` +
    `AllWords(words, count)` (имя — в UI);
  - `buildDisplayTree(slice: List<MembershipEntry>, comparator):
    DisplayTree` — чистая функция; дедуп wordId; корень: `AllWords` первым +
    корневые группы (в Э2 пусто); компаратор — параметром (задел, D7.2);
  - Валидаций/outcomes/normalize НЕТ (Э3).

## Фаза 3. Виджеты

### 3.1 `modules/widget/wordrow` — вынос строки слова (В1, атомарный коммит)

- [ ] Модуль `modules:widget:wordrow` (deps: `theme`, `ui`,
  `modules/domain/lexeme` — прецедент widget→domain: `component_widgets`).
- [ ] Переезд из wordstab БЕЗ правок вёрстки:
  - entities: `TermUiItem`, `LexemeUiItem` (+ `TranslationUiEntity`,
    `DefinitionUiEntity`), маппер `Lexeme.toUiItem()`; поля
    `isSelected`/`isExpand` едут как есть (wordstab-семантика, groupstab
    использует дефолты — D8.1/F-3);
  - виджеты: `TermWidget` → **`WordRowWidget(termItem, onClick:
    (TermUiItem) -> Unit, onLongClick: ((TermUiItem) -> Unit)?)`** —
    именованные параметры; null-propagation обязателен:
    `combinedClickable(onLongClick = cb?.let { { it(termItem) } })` — НЕ
    пустая лямбда (иначе long-press потребляется, D8.2/U-6);
  - `LexemeWidget`, `DefinitionWidget`, `TranslationWidget`;
  - `WordInfo` и `Msg` — ОСТАЮТСЯ в wordstab.
- [ ] wordstab переезжает на wordrow:
  - импорты entities из нового package;
  - `WordListWidget` вызывает `WordRowWidget` с колбеками, маппящими в
    `WordInfo`/`Msg.EnterSelectionMode` (логика 1:1);
  - **регресс-гейт**: reducer/state/effects wordstab НЕ трогаются; unit-тесты
    wordstab — только механические правки импортов.
- [ ] **App в том же коммите** (F-1): `WordsTabUseCaseImpl` (импорты
  `TermUiItem`/`toUiItem`) + `:modules:widget:wordrow` в
  `app/build.gradle.kts`. Других потребителей нет (проверено ревью).
- [ ] `LexemeUiItemTest` переезжает в wordrow тем же коммитом (T-7 — тест
  уезжающей сущности не остаётся в чужом модуле).
- [ ] Preview и `DataHelper`-фикстуры разносятся: wordrow берёт данные
  переносимых entities, wordstab — остальное (State/WordInfo — проверено).
- [ ] После коммита: ручной smoke «Слов» — список, выделение (long-press),
  снятие выделения (T-7).

### 3.2 `modules/widget/grouptree` — строка узла (В5, по U-1)

- [ ] Модуль `modules:widget:grouptree` (deps: `theme`, `ui`).
- [ ] `GroupNodeWidget(title, count, isExpanded, onToggle)` — ТОЛЬКО
  строка-заголовок узла: имя + счётчик + шеврон. **Content-слота НЕТ** —
  строки раскрытого узла обязаны быть items() LazyColumn groupstab, иначе
  чанк склеивается в один item и виртуализация умирает (D8.3).
- [ ] Preview (свёрнут/раскрыт). Логики нет → unit-тестов нет (осознанно).

## Фаза 4. Host: владение словарём + Dagger (D1.1, D1.6, В3)

- [ ] **ТЕСТЫ СНАЧАЛА** — reducer-тесты vocabulary:
  - `Msg.DictionaryChanged(id)` → `dictionaryId` обновлён,
    `isDictResolved = true`;
  - `Msg.DictionaryChanged(null)` → `dictionaryId = null`,
    `isDictResolved = true` (честный «словарей нет»);
  - исходный state: `isDictResolved = false` (loading-фаза);
  - `SelectTab` — прежние тесты не ломаются.
- [ ] `VocabularyHostState`: + `dictionaryId: Long?` +
  `isDictResolved: Boolean = false` (D9.1 — null не перегружен).
- [ ] `deps/VocabularyHostUseCase`: **`flowCurrentDictId(): Flow<Long?>`** —
  узкая сигнатура; `DictUiEntity`/dep на dictionarypicker НЕ тащить (A-6/F-2).
  Flow-handler в VM (подписка → `Msg.DictionaryChanged`).
- [ ] Dagger-миграция host-VM (цена из D1.1):
  - `dagger` + `ksp` в `build.gradle.kts` vocabulary;
  - `VocabularyHostViewModel` → `@AssistedInject` factory по образцу words
    (паттерн подтверждён: `WordsTabViewModel` + `AppComponent` +
    factory-параметры `CompositionRootImpl`);
  - `VocabularyHostModule` в app (`di/module/vocabulary/`) +
    `VocabularyHostUseCaseImpl` (тонкий, prefs — по образцу
    `flowCurrentDict` words, но маппит в id) + unit-тест в `app/src/test`;
  - `AppComponent`, `MainActivity`-wiring, параметр в `CompositionRootImpl`.
- [ ] Контракт вкладки: `TabSpec.content: @Composable (SnackbarHostState,
  DictionarySlot) -> Unit`; `@Immutable data class DictionarySlot(val id:
  Long?, val isResolved: Boolean)` в контракт-файле vocabulary.
  DictionarySlot — параметр ВЫЗОВА, не поле TabSpec (стабильность — урок
  D1.3). Words параметр игнорирует (В3). Мост и оба TabSpec обновить.

## Фаза 5. Groupstab: «Все» с аккордеоном и живым окном (v3)

### 5.1 Mate-цикл (ТЕСТЫ СНАЧАЛА)

- [ ] Reducer-тесты (v3 — живое окно):
  - init: `isLoading = true` до `DictionaryChanged`;
  - `DictionaryChanged(id)` → `SubscribeSlice(id)` + `SetWindow(null)`;
  - `DictionaryChanged` при раскрытом узле → **полный сброс**
    (`allNode = null`, `isLoading = true`) + resubscribe + гашение окна;
  - `SliceLoaded(tree)` (первый) → узел со счётчиком, `isLoading = false`;
  - **рост count под раскрытым узлом** → окно += дельта + `SetWindow`
    (компенсация вытеснения); под свёрнутым — окно не тронуто;
  - уменьшение count → счётчик/`hasMore` пересчитаны (список подчистит
    живая эмиссия окна);
  - `ToggleAll` (раскрыть) → `window = 50`, `SetWindow(50)`, спиннер;
  - **`ToggleAll` при `count = 0`** → раскрытие без подписки (T-5а);
  - повторный `ToggleAll` → свернуть: `SetWindow(null)`, контент сброшен;
  - `WindowLoaded(words)` → контент ЦЕЛИКОМ (не append), спиннер погашен,
    `hasMore = words.size < count`; после сворачивания — игнор;
  - `LoadMore` → `window += 50`, `SetWindow`; при `isWindowLoading` —
    игнор (спам); всё показано → `hasMore = false` без эффекта;
  - **`WindowLoadFailed`** → `isWindowLoading = false` (кнопка жива);
    **`SliceLoadFailed`** → `isLoading = false` (T-6).
- [ ] `GroupsTabState`: `isLoading`, `hasNoDictionary`, `dictionaryId`,
  `allNode: AllNodeState?`; `AllNodeState(count, isExpanded, window,
  loadedWords: List<TermUiItem>, isWindowLoading, hasMore)` — все флаги
  явные (D10.2 v3); id-срез в state больше не нужен.
- [ ] `Msg` / `GroupsTabReducer` / эффекты: `SubscribeSlice(dictionaryId)`,
  **`SetWindow(limit: Int?)`** — эффекты только двигают входы подписок,
  логика размеров окна в reducer (дух A-2/T-4).

### 5.2 Deps + effect handler

- [ ] `deps/GroupsTabUseCase`:
  - `membershipSlice(dictionaryId): Flow<List<MembershipEntry>>` —
    **domain/group-тип** (D7.3, Api-entity не протекает);
  - **(v3) `flowWordsWindow(dictionaryId, limit): Flow<List<TermUiItem>>`**
    (wordrow-entity).
- [ ] `DatasourceEffectHandler` groupstab — ДВЕ живые подписки:
  - slice: `flatMapLatest` по dictionaryId → `buildDisplayTree` →
    `Msg.SliceLoaded`;
  - окно: `combine(dictionaryId, window)` → `flatMapLatest(flowWordsWindow)`
    → `Msg.WindowLoaded`; `window == null` → `emptyFlow()` (подписка гаснет);
  - **`catch` на обеих подписках** → `Msg.WindowLoadFailed` /
    `Msg.SliceLoadFailed` (T-6 — иначе исключение убивает корутину и
    залипает спиннер; первый error-guard в Mate, осознанно минимальный);
  - константа `CHUNK_SIZE = 10` (В2 v3, решение юзера) — используется
    reducer'ом.

### 5.3 UI (вне TDD-контура)

- [ ] `GroupsTabScreen` заменяет `GroupsTabStubScreen`:
  - VM `@AssistedInject` factory (Dagger, по образцу words);
  - **проводка словаря**: `LaunchedEffect(slot.id, slot.isResolved) {
    vm.accept(Msg.DictionaryChanged(...)) }` (D9.4); прямой подписки на
    prefs НЕТ;
  - `LazyColumn` — аккордеон на `LazyListScope` (D8.3): header-item
    `GroupNodeWidget`(«Все», счётчик, шеврон; фон `groupNodeBgColor` —
    отличает узел от карточки слова, решение юзера) → word-items
    `WordRowWidget(termItem, onClick, onLongClick = null)` → footer-item:
    кнопка «Ещё» + «показано X из Y» при `hasMore`, спиннер при
    `isWindowLoading`;
  - **ключи items составные**: `key = "all:${term.id}"` (схема
    nodeId:wordId — задел Э3, U-5);
  - тап по слову → `GroupsNavigator.openWordCard(wordId)`;
  - empty-state «нет словаря» — ТОЛЬКО при `slot.isResolved && slot.id ==
    null`; при `!isResolved` — loading (D9.1);
- [ ] Строки в `core-resources` (`values/` + `values-ru-rRU/`): «Все»,
  «Ещё», «показано %1$d из %2$d». Шаблон БЕЗ существительного — plurals не
  нужны; появится существительное («слов») → перейти на `<plurals>` (U-7,
  пометка чтобы не потерять к Э3).

### 5.4 Мост в app

- [ ] `di/module/groupstab/`: `GroupsTabModule`, `GroupsTabUseCaseImpl`
  (делегация в `CoreDbApi`; маппинг slice Api → `MembershipEntry`
  (domain/group), контент `TermApiEntity → TermUiItem` — как
  `WordsTabUseCaseImpl.kt:116-135`) + unit-тест в `app/src/test`;
  `GroupsNavigatorImpl` (переиспользует wordcard-роут — `openWordCard`
  уже в `VocabularyHostDep`).
- [ ] `AppComponent`: groupstab factory.
- [ ] `CompositionRootImpl`: groups-`TabSpec.content` = `GroupsTabScreen`
  со `slot` от host; FAB у групп по-прежнему `null` (Э3).
- [ ] `app/build.gradle.kts`: `:modules:domain:group` (маппер use case;
  `:modules:widget:wordrow` уже добавлен в фазе 3.1),
  `:modules:screen:groupstab`-deps проверить (grouptree/wordrow/domain-group
  в build.gradle groupstab).
- [ ] Заглушка `GroupsTabStubScreen` удаляется; строка `groups_tab_stub_text`
  остаётся под empty-state «нет словаря».

## Фаза 6. Верификация этапа

- [ ] Тесты по модулям ПОСЛЕДОВАТЕЛЬНО (`./scripts/cc-build.sh`):
  `domain:group`, `screen:groupstab`, `screen:vocabulary`, `screen:wordstab`,
  `widget:wordrow`, app-тесты; затем `assembleDebug`, `lintDebug`.
- [ ] androidTest (миграция + DAO) — ПОВТОРНЫЙ прогон на эмуляторе
  (`connectedDebugAndroidTest` core-db-impl); CI их не гоняет — ручной шаг
  обязателен перед merge.
- [ ] Синхронизация доков:
  - `rollout_stages.md`: перенос `domain/group` из Э3 в Э2 (урезанный) +
    пометки решений В1–В5 + **правка пункта чек-листа Э2 «новое слово
    сразу появляется в „Все“»** → «счётчик — сразу; в раскрытый список —
    по „Ещё“/пересворачиванию» (T-8 — иначе приёмка по rollout формально
    провалена);
  - `project-architecture.md`: модули wordrow/grouptree/domain-group,
    host-VM на Dagger (снять отклонение D1.1), контракт DictionarySlot;
  - **`testing-migrations.md`**: переписать под фактический паттерн
    `MigrationFrom11to12` (MigrationTestHelper + BundledSQLiteDriver) —
    гайд описывает снесённый в IS481 фреймворк BaseMigration/Schemable/
    AllMigrationTest (T-2).
- [ ] `stage2_manual_test.md` — ручной чек-лист по формату Э1:
  - «Все» видна, счётчик = числу слов; раскрытие/сворачивание;
  - слова — та же вёрстка, что на «Словах» (визуальная сверка), порядок
    тот же (новые сверху);
  - чанки: раскрытие грузит ≤50; «Ещё» догружает; «показано X из Y» честно;
    спам по «Ещё» не дублирует слова;
  - **данные для чанков**: нужен словарь 51+ слов — использовать
    существующий большой словарь; если нет — временно снизить `CHUNK_SIZE`
    до 5–10 в debug-прогоне с ОБЯЗАТЕЛЬНЫМ возвратом константы (T-9 —
    иначе пункт молча пропустят);
  - тап по слову → карточка; назад → «Группы» в прежнем состоянии;
  - **удаление слова из карточки, открытой с «Групп»** → назад: слова нет
    в списке, счётчик уменьшился (D10.3/T-5);
  - новое слово на «Словах» → счётчик «Все» обновился без рестарта
    (в раскрытый список — по «Ещё», известный компромисс);
  - смена словаря на «Словах» → возврат на «Группы»: данные нового словаря
    (допустим кадр loading; чужие слова не показываются — D9.4);
  - смена словаря через AppBar прямо на «Группах» → мгновенный сброс;
  - миграция (В4): апдейт APK на девайсе с данными → слова/лексемы/
    компоненты живы, «Все» показывает всё; свежая установка → v13 с нуля;
  - регресс «Слов» после wordrow-выноса: список, поиск, выделение
    (long-press), удаление, добавление;
  - поворот экрана: expand/чанки переживают (state в VM);
  - вкладки: переключение туда-сюда — скролл и state обеих живы.

## Вне скоупа Э2 (фиксация)

- Миграция words на host-словарь (В3) — долг D1.6 остаётся частично.
- `groupTree`-чтение, мутации групп, транзакции §2.4, валидации имён — Э3.
- Kebab, диалоги, подгруппы, пикер — Э3+.
- Живой merge ВСТАВОК в загруженные чанки (удаления фильтруются — D10.3).
- Общий механизм обработки ошибок эффектов Mate (в Э2 — точечный guard
  groupstab).
- Удаление мёртвого `RoomPaging.kt` — кандидат в бэклог.
- CI-job для androidTest — в Backlog (ВекторныйПиздеж, добавлено 2026-08-10).
