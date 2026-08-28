# IS493 / Э2 — Design tree

Статус: **v3** (2026-08-11: D10 переработан на ЖИВОЕ ОКНО по итогам ручной
проверки юзера — см. преамбулу D10; v2 — 2026-08-10, после ревью 5 агентов:
архитектура A-1..7, данные D-1..6, UI U-1..7, факты F-1..4, тесты T-1..9;
триаж оркестратора — принятые находки внесены в узлы, отклонённые перечислены
в конце). Узлы D6–D11 (продолжение нумерации Э1: D1–D5); у каждого — решение
и обоснование. Решения пользователя В1–В5 (2026-08-10).
Факты проверены по коду: `WordDao.kt`, `CoreDbApiImpl.kt`, `RoomModule.kt`,
`DatasourceEffectHandler.kt`, `State.kt` (wordstab), `TermWidget.kt`,
`WordListWidget.kt`, `ComponentTypeDb.kt`, `Database.kt`,
`Migration_011_to_012.kt`, `WordsNavigator.kt`, `WordsTabUseCaseImpl.kt`,
`stage1_design_tree.md`.

```
Э2. Группа «Все» — аккордеон со всеми словами
├── D6. Данные: миграция 12→13 + slice/content API
│   ├── D6.1 Обе таблицы сразу, схема финальная (Э3–Э7 БД не трогают)
│   ├── D6.2 Составной PK word_groups — новая DDL-форма, сверка с 13.json
│   ├── D6.3 membershipSlice: подзапрос живых membership + ORDER BY id DESC
│   ├── D6.4 Контент: flowTermsWindow (живое окно, Э2) + getTermsByIds (задел Э3)
│   └── D6.5 Тест миграции: chained 11→13 + cascade, без idempotency (В4)
├── D7. domain/group урезанный: DisplayNode + buildDisplayTree
│   ├── D7.1 Скоуп: без валидаций/outcomes (приходят в Э3)
│   ├── D7.2 Компаратор — параметром, в Э2 не используется
│   └── D7.3 Slice-тип объявляет domain/group; дедуп wordId в builder
├── D8. Виджеты
│   ├── D8.1 wordrow: вынос вёрстки + entities из wordstab (В1)
│   ├── D8.2 Колбеки вместо Msg: Msg/WordInfo остаются в wordstab
│   └── D8.3 grouptree: GroupNodeWidget — только строка узла, БЕЗ слота
├── D9. Host: владение словарём + Dagger
│   ├── D9.1 dictionaryId + isDictResolved в host-state; flowCurrentDictId
│   ├── D9.2 Words НЕ мигрирует — резолвит сам (В3)
│   ├── D9.3 Dagger-миграция host-VM (закрытие долга D1.1)
│   └── D9.4 TabSpec.content: + DictionarySlot(id, resolved); LaunchedEffect-проводка
├── D10. Groupstab: Mate-цикл + живое окно (v3)
│   ├── D10.1 Живое окно по предикату; «Ещё» расширяет LIMIT (В2 v2)
│   ├── D10.2 State: окно + явные флаги; полный сброс
│   ├── D10.3 Роль slice (счётчик); ошибки эффектов
│   └── D10.4 Навигация: GroupsNavigator.openWordCard по образцу words
└── D11. Мост в app: groups-TabSpec, DI-модули
```

---

## D6. Данные: миграция 12→13 + slice/content API

### D6.1 Обе таблицы сразу, схема финальная

- **Решение:** `Migration_012_to_013` создаёт `dictionary_groups` И
  `word_groups` разом, пустыми; этапы Э3–Э7 БД не трогают (концепт-правка
  2026-08-06: между этапами релизов не будет, миграция одна).
- Только DDL, data-backfill нет. `CREATE TABLE/INDEX IF NOT EXISTS`
  (defensive, образец `Migration_011_to_012`), имена индексов
  `index_<table>_<col>`.
- Регистрация — `RoomModule.kt:57` (`addMigrations`), факт проверен.

### D6.2 Составной PK word_groups

- **Решение:** `@Entity(primaryKeys = ["word_id", "group_id"])` +
  `INDEX(group_id)` обязателен (PK покрывает только префикс `word_id`).
- Это первая не-autoincrement PK-форма в проекте (подтверждено ревью F:
  `primaryKeys` нигде не используется) — DDL миграции выверять против
  экспорта `13.json` руками (Р12).
- Ловушки сверки (ревью D-6): MigrationTestHelper сравнивает TableInfo —
  критичны (а) порядок колонок PK (`word_id`=1, `group_id`=2), (б) NOT NULL
  на всех колонках, (в) **точное множество индексов**: ровно один
  `index_word_groups_group_id`; «симметричный» индекс по `word_id` руками НЕ
  создавать — Room его не экспортирует, лишний индекс = провал валидации.
- Экспорт схемы ляжет в
  `schemas/me.apomazkin.core_db_impl.room.Database/13.json`
  (`room { schemaDirectory }` — факт проверен).
- Self-FK `parent_group_id` CASCADE — прецедент `depends_on_type_id`
  (`ComponentTypeDb.kt:42-47`), bundled SQLite. FK enforcement в проде
  подтверждён: `Database_Impl.onOpen` выполняет `PRAGMA foreign_keys = ON`.

### D6.3 membershipSlice: подзапрос живых membership + ORDER BY

- **Решение (уточнено ревью D-1, D-2):** новый `GroupDao`, один `@Query`.
  Наивная цепочка `words LEFT JOIN word_groups LEFT JOIN dictionary_groups`
  ломается на мёртвой группе (слово теряется целиком или возвращается id
  мёртвой группы). Каноническая форма — LEFT JOIN на предварительно
  склеенные ЖИВЫЕ membership:

  ```sql
  SELECT w.id AS wordId, m.group_id AS groupId
  FROM words w
  LEFT JOIN (
      SELECT wg.word_id, wg.group_id
      FROM word_groups wg
      JOIN dictionary_groups dg
        ON dg.id = wg.group_id AND dg.removed_at IS NULL
  ) m ON m.word_id = w.id
  WHERE w.dictionary_id = :dictionaryId
  ORDER BY w.id DESC
  ```

  Слово только в мёртвой группе → ровно одна строка `(wordId, null)`;
  слово в мёртвой + живой → только живая строка, без фантомных null.
- **`ORDER BY w.id DESC` обязателен** (ревью A-1/D-2): глобальный порядок
  «Все» определяется slice'ом — чанки режутся по этому списку; без ORDER BY
  SQLite отдаст произвольный порядок и первый чанк покажет старейшие слова,
  ломая «тот же порядок, что на Словах» (`searchTermsPaging` —
  `ORDER BY id DESC`, `WordDao.kt:97-106`).
- Все три таблицы присутствуют в тексте запроса (включая подзапрос) —
  Room-инвалидация по трём таблицам сохраняется (А13). Nullable `groupId`
  в POJO `@Query`-результата Room поддерживает (проверено ревью).
- В Э2 `word_groups` пуст — slice вернёт все слова с `groupId = null`;
  форма финальная, Э3 переиспользует без правок. Раз форма финальная —
  кейсы Э3 (мёртвая группа, дубли membership) тестируются УЖЕ в Э2
  вставкой строк напрямую в тесте (API групп ещё нет).

### D6.4 Контент: flowTermsWindow (Э2) + getTermsByIds (задел Э3)

- **Решение (v3):** контент «Все» в Э2 читает
  `WordDao.flowTermsWindow(dictionaryId, limit)` — ПРЕДИКАТНЫЙ live-запрос
  `WHERE dictionary_id = :id ORDER BY id DESC LIMIT :limit` (@Transaction +
  relations; Room-инвалидация по words/lexemes/values) — та же механика
  живости, что у `searchTermsPaging` «Слов», но LIMIT-окно вместо Paging.
  Обоснование перехода с чанков — преамбула D10.
- `getTermsByIds(ids)` (`WHERE id IN (:ids) ORDER BY id DESC`) остаётся в
  DAO/API как задел Э3-узлов (там контент подмножества; предикат станет
  JOIN по word_groups). Room сохраняет порядок родителей при
  relation-подгрузке (проверено ревью D-2). Bind-лимит 999 — контракт
  вызова ids ≤50.

### D6.5 Тест миграции: chained 11→13 + cascade (В4, уточнено D-4)

- **Решение:** androidTest `MigrationFrom12to13` по образцу
  `MigrationFrom11to12` (MigrationTestHelper Rule, `createDatabase` +
  `runMigrationsAndValidate`), БЕЗ idempotency (чистые
  `CREATE IF NOT EXISTS` идемпотентны по построению + Room-транзакция).
- **Chained-кейс обязателен** (ревью D-4): боевой путь пользователя —
  11→12→13 (последний деплой-тег 0.1.5 на v11, KDoc RoomModule):
  `createDatabase(11)` + данные →
  `runMigrationsAndValidate(13, listOf(Migration_011_to_012, Migration_012_to_013))`.
  Изолированный 12→13 — dev-путь, одного его мало.
- **Cascade-кейс** (образец Case F `MigrationFrom11to12`): вставить
  группу + membership → удалить слово/словарь → строки
  `word_groups`/`dictionary_groups` ушли. В тестовом соединении явный
  `PRAGMA foreign_keys=ON` (helper по умолчанию выключен — Case F делает так же).
- **CI не гоняет androidTest** (ревью T-1, факт:
  `on_feature_push.yml` — только lint + unit + assemble): прогон миграционных
  и DAO-тестов — руками на эмуляторе, в фазе 1 сразу и повторно в фазе 6;
  зафиксировано как обязательный шаг перед merge. CI-job — в Backlog
  (раздел ВекторныйПиздеж).

## D7. domain/group урезанный

### D7.1 Скоуп: без валидаций/outcomes

- **Решение:** модуль `modules/domain/group` создаётся в Э2, но несёт ТОЛЬКО
  `DisplayNode` ADT (architecture §2.2), slice-типы (D7.3) и
  `buildDisplayTree` (чистая функция). Валидации, outcomes, normalize,
  `createsCycle` — Э3.
- Почему сейчас: Э2 требует DisplayTree с маркером «Все»; строить его в
  groupstab = переезд в Э3 (модуль, который Э3 и так активно меняет).
  Перенос из Э3 в Э2 — правка `rollout_stages.md` (фаза 6 плана).
- Образец модуля — `modules/domain/lexeme` (kotlin("jvm"), zero prod deps —
  факт проверен ревью).

### D7.2 Компаратор — параметром

- **Решение:** сигнатура `buildDisplayTree(slice, comparator)` закладывается
  сразу (приём А8: locale-aware компаратор инжектится, domain без
  зависимостей), но в Э2 не используется — у «Все» нет сиблингов.
- Слова внутри AllWords — в порядке прихода из slice (гарантирован
  `ORDER BY w.id DESC` в D6.3), domain порядок не пересортировывает.

### D7.3 Slice-тип объявляет domain/group; дедуп wordId

- **Решение (ревью A-7):** тип slice на границе модулей —
  `MembershipEntry(wordId, groupId?)` в `domain/group` (его и потребляет
  `buildDisplayTree`). По конвенции проекта screen-модули не видят
  `core-db-api` (WordsTabUseCase оперирует UI-entity, маппинг Api→X — в
  app-impl) — `MembershipSliceApiEntity` в интерфейс groupstab попасть не
  может. App-impl маппит Api→domain; groupstab и app зависят от
  `domain/group`.
- **Дедуп обязателен в контракте builder'а** (ревью D-5): финальная форма
  slice возвращает строку на membership — в Э3 слово в двух живых группах
  даст два wordId; `AllWords.words`/`count` считаются по **distinct** wordId
  (иначе счётчик «Все» завышен). В Э2 дублей не бывает — поэтому тест со
  строками-дублями пишется УЖЕ сейчас, иначе форма уедет в Э3 непроверенной.

## D8. Виджеты

### D8.1 wordrow: вынос вёрстки + entities (В1)

- **Решение:** новый модуль `modules/widget/wordrow`; из wordstab переезжают
  БЕЗ правок вёрстки: `TermWidget` (→ `WordRowWidget`), `LexemeWidget`,
  `DefinitionWidget`, `TranslationWidget`, entities `TermUiItem`,
  `LexemeUiItem` + value-классы, маппер `Lexeme.toUiItem()`.
- Deps модуля: `theme`, `ui`, `modules/domain/lexeme` (маппер принимает
  `Lexeme` — факт: `LexemeUiItem.kt:3,22`; wordstab уже зависит от
  domain/lexeme). Прецедент `widget → domain`: `component_widgets`
  (проверено ревью A). `core-resources` вёрстке не нужен (проверено).
- **App — тоже потребитель entities** (ревью F-1, критично для
  коммит-нарезки): `WordsTabUseCaseImpl.kt:10-11,101,122` импортирует и
  конструирует `TermUiItem`/`toUiItem` — правка импортов app +
  `:modules:widget:wordrow` в `app/build.gradle.kts` входят в ТОТ ЖЕ коммит
  выноса, иначе он не соберётся. Других внешних потребителей нет
  (wordcard/quiz не используют — проверено).
- Уточнение (ревью F-3): `TermUiItem` несёт wordstab-семантику
  `isSelected`/`isExpand` (`TermUiItem.kt:13-14`) — поля уезжают вместе с
  entity; groupstab использует дефолты (false), рамка selection не
  рисуется. Осознанная цена В1 — чистка модели не в скоупе Э2.
- Почему не слот через app (отклонённый вариант): виджет-модуль чище по
  слоям и нужен обоим потребителям надолго (Э5 — слова в группах).
- Почему не дубль вёрстки: нарушает В2 («тот же виджет») и двоит сопровождение.

### D8.2 Колбеки вместо Msg

- **Решение:** `WordRowWidget(termItem, onClick: (TermUiItem) -> Unit,
  onLongClick: ((TermUiItem) -> Unit)?)` — именованные параметры (не
  trailing-лямбда, как у текущего `TermWidget.kt:35-39`). `Msg` и `WordInfo`
  остаются в wordstab: обёртка маппит колбеки в
  `Msg.EnterSelectionMode`/`openWordCard` (логика `TermWidget.kt:44-56` — 1:1).
- `onLongClick` nullable: groupstab передаёт null (read-only, В6 «тап →
  карточка»). **Null обязан доходить до `combinedClickable` null'ом**
  (ревью U-6): `onLongClick = cb?.let { { it(termItem) } }` — НЕ
  `{ cb?.invoke(...) }`, иначе long-press потребляется (haptic/ripple) при
  отсутствующем действии.
- Регресс-гейт: reducer/state/effects wordstab не трогаются; тесты wordstab
  зелёные с механическими правками импортов.

### D8.3 grouptree: GroupNodeWidget — только строка узла, БЕЗ слота

- **Решение (переработано по ревью U-1):** модуль `modules/widget/grouptree`
  создаётся в Э2 (по А16 — отдельная фаза ДО потребителей), но виджет —
  ТОЛЬКО строка-заголовок узла: `GroupNodeWidget(title, count, isExpanded,
  onToggle)`. Слота `content` НЕТ.
- Почему без слота: WordRow-строки чанков обязаны быть items() самой
  LazyColumn groupstab — контент в слоте склеил бы весь чанк (50–150+ строк
  после «Ещё») в ОДИН item, убив виртуализацию (композиция всего списка
  разом, джанк на раскрытии). «Аккордеон» собирается на уровне
  `LazyListScope` в groupstab: header-item (GroupNodeWidget) → word-items →
  footer-item («Ещё»). При желании API — extension
  `LazyListScope.groupNode(...)` в grouptree (опционально, Э3 решит).
- Дерево/отступы вложенности — Э4; строка узла масштабируется на дерево
  без переделки именно потому, что не владеет контентом.

## D9. Host: владение словарём + Dagger

### D9.1 dictionaryId + isDictResolved; flowCurrentDictId (В3, долг D1.6)

- **Решение:** `VocabularyHostState` + `dictionaryId: Long?` +
  `isDictResolved: Boolean = false` (оба — явные поля). Первая эмиссия
  prefs-flow (включая null) → `isDictResolved = true`.
- Почему два поля (ревью U-3/A-4): один null перегружен двумя смыслами —
  «ещё не отрезолвлен» (старт, показывать loading) и «словарей нет»
  (честный empty-state). Без признака юзер с существующим словарём ловит
  вспышку «нет словаря» при входе на вкладку — ровно тот класс бага, ради
  которого words держит явный `hasNoDictionary` (правило explicit state flags).
- **Сигнатура use case — `flowCurrentDictId(): Flow<Long?>`**, НЕ
  `Flow<DictUiEntity?>` (ревью A-6/F-2): `DictUiEntity` живёт в
  `modules/widget/dictionarypicker` — копирование words-сигнатуры втащило бы
  в vocabulary незаявленную dep (от которой в wordstab уже висит TODO
  «избавиться»); host'у нужен только id.
- Host — единственный резолвер словаря для вкладок (А9/Р8): groupstab
  получает id параметром слота, свой резолв не заводит.

### D9.2 Words НЕ мигрирует (В3)

- **Решение:** words продолжает резолвить словарь сам
  (`getCurrentDict()`/`flowCurrentDict` в его эффектах — факт:
  `DatasourceEffectHandler.kt:45,60`). Оба читают один prefs-источник —
  рассинхрона нет (подтверждено ревью A: возможен лишь transient-skew без
  функциональных последствий).
- Полная миграция words на host-словарь — отдельное решение позже
  (out-of-scope Э2, долг D1.6 остаётся частично открытым).

### D9.3 Dagger-миграция host-VM (закрытие D1.1)

- **Решение:** у host-VM появляется первая зависимость (use case словаря) —
  исполняется выписанная в D1.1 цена: `dagger`+`ksp` в build.gradle
  vocabulary, `@AssistedInject`-factory по образцу words (паттерн
  подтверждён ревью F: `WordsTabViewModel.kt:19,44-47` +
  `AppComponent.kt:67` + factory-параметры CompositionRootImpl),
  MainActivity-wiring. Текущее создание — `viewModelFactory` из
  `modules/core/di` (`VocabularyHostScreen.kt:59-61`) — заменяется.
- Осознанное исключение Э1 («первый VM вне Dagger») закрывается — отметить
  в `project-architecture.md` (фаза 6).

### D9.4 TabSpec.content: DictionarySlot; LaunchedEffect-проводка

- **Решение:** `content: @Composable (SnackbarHostState, DictionarySlot)
  -> Unit`, где `@Immutable data class DictionarySlot(val id: Long?,
  val isResolved: Boolean)` — два явных поля, не sealed-обёртка (правило
  simple state).
- TabSpec остаётся стабильным (урок D1.3, подтверждено ревью U-2/A: инстансы
  лямбд создаются один раз в `remember(words)` — `CompositionRootImpl.kt:88-106`;
  DictionarySlot — параметр ВЫЗОВА content, не поле спеки — мост ничего не
  пересобирает при смене словаря).
- **Проводка параметр → VM специфицирована** (ревью U-2/A-5):
  `GroupsTabScreen` делает `LaunchedEffect(slot.id, slot.isResolved) {
  vm.accept(Msg.DictionaryChanged(...)) }`. Groupstab НЕ подписывается на
  prefs напрямую (нарушило бы В3/Р8 «host — единственный резолвер»).
- **Известное ограничение (принято):** доставка гейтится композицией —
  `SaveableStateProvider` компонует только активную вкладку
  (`VocabularyHostScreen.kt:156-158`); смена словаря на «Словах» доедет до
  groupstab-VM при возврате на «Группы», первый кадр может показать старый
  state до отработки reset. Митигация — полный reset по `DictionaryChanged`
  (D10.2); UI-guard сравнения параметра со state в composable ОТКЛОНЁН
  (вычисляемый флаг в composable — против правила explicit state flags).
  Кейс — в ручной чек-лист.

## D10. Groupstab: Mate-цикл + живое окно

> **v3 (2026-08-11, ручная проверка юзера).** Первая реализация (чанки
> `getTermsByIds(ids)` + дифф-нарезка + фильтрация удалённых) заменена на
> ЖИВОЕ ОКНО. Причина: чанк-контент был мёртвым снапшотом — вставка нового
> слова под раскрытым узлом не появлялась в списке (юзер квалифицировал
> багом; плюс вторая дыра — правки лексем из карточки тоже были stale).
> Вкладка «Слова» живая, потому что её запрос ПРЕДИКАТНЫЙ
> (`WHERE dictionary_id`), а не по фиксированным ids — то же решение
> применено здесь. Исторические D10.1–D10.3 v2 (дифф-нарезка) — в git.

### D10.1 Живое окно по предикату; «Ещё» расширяет LIMIT (В2 v2)

- **Решение:** контент раскрытого узла — live-Flow предикатного запроса
  `WordDao.flowTermsWindow`: `WHERE dictionary_id = :id ORDER BY id DESC
  LIMIT :window` (@Transaction + relations — Room инвалидирует по
  words/lexemes/values). `window` — явное поле state, старт
  `CHUNK_SIZE=10` (решение юзера 2026-08-11: «50 для групп много»;
  изначально 50 = pageSize «Слов»), «Ещё» → `window += 10`. Свернул узел /
  сменил словарь → `SetWindow(null)`, подписка гаснет — синхронизируется
  только раскрытое.
- Что живое автоматически: вставка нового слова (голова окна — новые id
  всегда сверху при DESC), правка лексем из карточки, удаление (строка
  уходит, окно дотягивает следующее слово с хвоста).
- Голова и хвост независимы: вставки приходят только в голову (live),
  дозагрузка «Ещё» работает только с хвостом (LIMIT+) — механики не
  конфликтуют.
- **Компенсация вытеснения:** рост count под раскрытым узлом → `window +=
  дельта` (эффект `SetWindow`) — вставка в голову не выталкивает нижние
  загруженные слова из окна.
- Почему не Paging 3 — прежние аргументы в силе (кнопка против авто-скролла;
  paging-поток на узел не ложится в дерево Э3+). Почему не полная подписка
  без LIMIT: В2-требование юзера (контролируемая длина списка) + рефетч
  всего словаря на каждый чих.
- **Ключи items — составные** (ревью U-5): `key = "all:${term.id}"` — задел
  Э3 (одно слово в нескольких раскрытых узлах). Скролл при вставке в голову
  не дёргается: keyed-диффинг + якорение первого видимого элемента LazyColumn.
- `getTermsByIds(ids)` остаётся в DAO/API как задел Э3-узлов (там предикат
  станет JOIN по `word_groups` — тоже живой).

### D10.2 State: окно + явные флаги; полный сброс

- **Решение:** `AllNodeState(count, isExpanded, window, loadedWords,
  isWindowLoading, hasMore)` — все флаги явные (explicit state flags), НЕ
  `rememberSaveable` → поворот переживается через VM. `loadedWords` —
  последняя эмиссия окна ЦЕЛИКОМ (не аккумуляция); дедуп/фильтрация/
  дифф-нарезка не существуют. `hasMore = loadedWords.size < count`
  (count — из slice).
- Handler: две подписки — slice (`flatMapLatest` по dictionaryId) и окно
  (`combine(dictionaryId, window)` → `flatMapLatest(flowWordsWindow)`);
  эффекты `SubscribeSlice(id)` / `SetWindow(limit?)` только двигают
  StateFlow-входы подписок — вся логика размеров окна в reducer (дух A-2/T-4).
- Смена словаря: `Msg.DictionaryChanged` → **полный сброс** (`allNode=null`,
  `isLoading=true`) + `SubscribeSlice(new)` + `SetWindow(null)`. Спам по
  «Ещё»: `LoadMore` при `isWindowLoading` — игнор.

### D10.3 Роль slice; ошибки

- Slice (`membershipSlice`) остаётся источником СЧЁТЧИКА (и структуры
  групп в Э3); контент окна приходит собственной подпиской. Рассинхронов
  «счётчик живой / список мёртвый» больше нет — живое и то и другое.
- **Ошибки эффектов** (ревью T-6): `catch` на обеих подписках →
  `Msg.WindowLoadFailed` / `Msg.SliceLoadFailed` → reducer сбрасывает
  `isWindowLoading`/`isLoading` (иначе исключение убивает корутину и
  спиннер залипает; в Mate обработки ошибок нет — guard осознанно
  минимальный). Reducer-тесты обязательны. Общий механизм ошибок Mate —
  вне скоупа.

### D10.4 Навигация: GroupsNavigator

- **Решение:** `GroupsNavigator : Navigator { openWordCard(wordId) }` — по
  образцу `WordsNavigator` (факт: `WordsNavigator.kt:5-6`);
  `GroupsNavigatorImpl` в app переиспользует существующий wordcard-роут
  (`openWordCard`-лямбда уже приходит в VocabularyHostDep —
  `CompositionRootImpl.kt:68`, проверено ревью).

## D11. Мост в app

- **Решение:** по образцу Э1-моста (D4.1 — чистая склейка, без
  viewModel/collect в app): `GroupsTabModule` + `GroupsTabUseCaseImpl`
  (делегация в `CoreDbApi` + маппинг: slice `Api → domain/group`-тип (D7.3),
  контент `TermApiEntity → TermUiItem` — как `WordsTabUseCaseImpl.kt:116-135`)
  + unit-тест, `VocabularyHostModule` + `VocabularyHostUseCaseImpl`
  (`flowCurrentDictId`) + тест; `AppComponent` — две factory-записи.
- groups-`TabSpec`: content = `GroupsTabScreen(slot, snackbar)`,
  `fab = null` (FAB групп — Э3), `topBarOverride = null`.
- Заглушка `GroupsTabStubScreen` удаляется; строка `groups_tab_stub_text`
  (факт: values/strings.xml:85, values-ru-rRU:75) остаётся под empty-state
  «нет словаря» (`isResolved && id == null`).

## Отклонённые находки ревью (с аргументами)

- **UI-guard `if (slot.id != state.dictionaryId) → loading` в composable**
  (U-2в) — вычисляемое состояние в composable против правила explicit
  state flags; вместо него полный reset (D10.2) + кадр стейла принят как
  известное ограничение (D9.4) с пунктом чек-листа.
- **`X = min(loadedWords.size, count)` в футере** (U-4) — не нужен: с
  фильтрацией `loadedWords` по id-set (D10.3) инвариант `X ≤ Y`
  выполняется сам.
- **Turbine/coroutines-test как новая dep для Flow-DAO-теста** (T-3,
  вариант) — новую библиотеку не заводим; Flow-переэмит тестируется ручной
  конструкцией (см. план 1.4). Признано: это первый Flow-DAO-тест проекта.
- **CI-job для androidTest** (T-1, часть) — правильная идея вне скоупа →
  Backlog «ВекторныйПиздеж» (запись добавлена 2026-08-10).

## Риски

- **Главный:** wordrow-вынос заденет words-вёрстку — митигация: перенос
  файлов 1:1 без правок, регресс-гейт тестов + smoke «Слов» сразу после
  коммита выноса (ревью T-7) + полный регресс в чек-листе фазы 6.
- Миграция: составной PK против 13.json — androidTest chained 11→13 (D6.5)
  + ручная проверка апдейта на девайсе с данными.
- Смена словаря под раскрытым узлом: гонка slice/чанков — закрывается
  `flatMapLatest` + полным сбросом state (reducer-тест) + дифф-нарезкой.
- TabSpec-сигнатура меняется — пересобрать мост аккуратно (урок D1.3:
  специфи в remember, изменчивость через параметры вызова).
- androidTest вне CI — дисциплина ручного прогона (фаза 1 и фаза 6),
  системное решение — в Backlog.
