# IS493 / Э1 — Design tree

Статус: **v2** (после ревью 2026-08-04). Узлы D1–D5; у каждого — решение и
обоснование. Факты проверены по коду: `CompositionRoot.kt`, `CompositionRootImpl.kt`,
`Vocabulary.kt`, `VocabularyTabScreen.kt`, `DictionaryTabUiDeps.kt`,
`ActionTopBarWidget.kt`, `SystemBarsWidget.kt`, `core-resources/res`.

```
Э1. Две вкладки в словаре
├── D1. Host-модуль vocabulary
│   ├── D1.1 State: Mate-минимум, VM внутри модуля
│   ├── D1.2 Layout: Scaffold у host'а (все параметры + FAB-анимация)
│   ├── D1.3 Контракт вкладки: TabSpec (@Stable data)
│   ├── D1.4 AppBar: deps-слот переезжает в host
│   ├── D1.5 Сохранение state вкладок: SaveableStateHolder
│   ├── D1.6 dictionaryId: отложено до Э2 (явное отклонение А9)
│   └── D1.7 Статусбар считает host
├── D2. Words-рефактор (dictionaryTab)
│   ├── D2.1 DictionaryTabScreen → WordsTabContent
│   ├── D2.4 Snackbar → host
│   ├── D2.5 Reducer/State — не трогаются (скоуп: фаза 3)
│   └── D2.6 Handle-паттерн: rememberWordsTabHandle
├── D3. Grouptab-заглушка
├── D4. Мост в app (CompositionRootImpl)
│   ├── D4.1 Чистая склейка без viewModel/collect в app
│   └── D4.2 Сброс ActionMode при переключении (+ same-tab guard)
└── D5. Rename dictionaryTab → wordstab (полный)
```

---

## D1. Host-модуль `modules/screen/vocabulary`

### D1.1 State: Mate-минимум, VM внутри модуля

- **Решение:** Mate-цикл с одним полем
  `VocabularyHostState(selectedTab: VocabularyTab = WORDS)` + `Msg.SelectTab`.
  `VocabularyHostViewModel` создаётся **внутри модуля** через существующий
  хелпер `viewModelFactory { }` (`modules/core/di/ViewModelHelper.kt`) —
  Dagger-factory не нужен: у reducer'а ноль внешних зависимостей.
- Это ПЕРВЫЙ VM проекта вне Dagger/AppComponent (все 12 существующих —
  @AssistedInject) — осознанное исключение, фиксируется в
  `project-architecture.md`. Цена Э2-миграции на Dagger: dagger+ksp в
  build.gradle модуля, factory-запись в `AppComponent`, параметр в конструктор
  `CompositionRootImpl`, MainActivity-wiring — по образцу words.
- Почему не `rememberSaveable`: правило explicit state flags +
  юнит-тестируемость + консистентность проекта.
- Эффектов у reducer'а НЕТ: сброс ActionMode — мост (D4.2).
- Factory с зависимостями появится в Э2 (use case текущего словаря) по
  образцу words — место заложено.

### D1.2 Layout: Scaffold у host'а

- **Решение:** host владеет единственным `Scaffold` и переносит ВСЕ параметры
  текущего words-Scaffold (факт: `VocabularyTabScreen.kt:103-133`):
  - `containerColor = Color.Transparent`
  - `contentWindowInsets = WindowInsets(0.dp)` (дефолтные инсеты сдвинули бы layout)
  - контент — в `padding(paddingValues)`; words-Box свой padding убирает
  - FAB — `AnimatedVisibility(scaleIn/scaleOut)` + `enabled` (перенос анимации)
  - `topBar` = `topBarOverride` активной вкладки `?:` AppBar (D1.4)
  - `TabRow` — первый элемент content, НЕ в topBar-слоте
  - `SnackbarHost` один, host'а
- Почему TabRow не в topBar: topBar свопится ActionMode-баром, TabRow обязан
  оставаться видимым (А9; факт: `VocabularyTabScreen.kt:105-113`).
- Почему не nested Scaffold: TabRow оказался бы над AppBar.

### D1.3 Контракт вкладки: TabSpec

- **Решение (v3, после бага невидимой шторки):** типы контракта — в
  host-модуле; **TabSpec стабилен навсегда** — мост собирает его ОДИН раз
  (`remember(handle)`), изменчивость только через лямбды-читалки, которые host
  вызывает в своих скоупах:

```kotlin
enum class VocabularyTab { WORDS, GROUPS }

@Stable data class FabSpec(val iconRes: Int, val visible: () -> Boolean, val onClick: () -> Unit)

@Stable data class TabSpec(
    val titleRes: Int,
    val isTopBarOverridden: () -> Boolean = { false },
    val topBarOverride: @Composable () -> Unit = {},
    val fab: FabSpec? = null,          // null у groups (Э1)
    val content: @Composable (SnackbarHostState) -> Unit,
)
```

- **Почему НЕ Boolean-значения в полях (урок бага):** первая версия держала
  `visible: Boolean` / `topBarOverride: null?` и пересоздавала tabs с ключами
  `remember(words, isActionMode, isFabVisible)` — map пересобиралась ровно в
  кадр открытия шторки (флип isFabVisible), новая content-лямбда рвала
  show-анимацию ModalBottomSheet: окно есть (клавиатура поднялась, FAB
  спрятался), шторка невидима, «назад» закрывает. Флейк по гонке кадра.

- Связывает контракт **app**: host и вкладки друг о друге не знают.
- Почему не интерфейс в core/ui: контракт специфичен host'у.

### D1.4 AppBar: deps-слот переезжает в host

- **Решение:** `VocabularyHostUiDeps.AppBar` — тот же приём, что нынешний
  `DictionaryTabUiDeps.AppBar`; инъектит app. Титул — существующий
  `item_title_vocabulary` (строка в `core-resources`).
- `DictionaryTabUiDeps` в words-модуле удаляется (шаг фазы 3.2).

### D1.5 Сохранение state вкладок: SaveableStateHolder

- **Решение:** контент активной вкладки — в
  `SaveableStateHolder.SaveableStateProvider(tab)`.
- Почему: `rememberLazyListState` «Слов» иначе теряется при переключении.
- Известный нюанс (ревью F8): paging-flow с активным поиском НЕ кэшируется
  (`cachedIn` только для пустого паттерна) — пере-collect при возврате
  вкладки перезапустит поиск-загрузку. Принято; проверяется чек-листом
  («поиск активен → переключение»), фикс — вне скоупа Э1.

### D1.6 dictionaryId: отложено до Э2 (явное отклонение А9)

- А9 велит host'у резолвить словарь и раздавать вкладкам. В Э1 НЕ делается:
  - words резолвит сам (`getCurrentDict()` вшит в его эффекты) — отучать =
    править words-логику, что запрещено регресс-контрактом D2.5;
  - у host'а нет ни одного потребителя (заглушке словарь не нужен) —
    обвязка была бы мёртвым кодом.
- **Host забирает владение словарём в Э2** (первый потребитель — groupstab):
  `dictionaryId` добавится в сигнатуру `TabSpec.content` + резолв в host-VM
  (factory по образцу words). Миграция words на host-словарь — отдельное
  решение позже (оба читают одни prefs — рассинхрона источников нет).

### D1.7 Статусбар считает host

- **Решение:** `SystemBarsWidget` вызывает host: `topBarOverride != null` →
  secondary + светлые иконки, иначе transparent. Words свой вызов удаляет.
- Почему: `SystemBarsWidget.onDispose` пустой (факт:
  `modules/core/ui/.../SystemBarsWidget.kt:29`) — words-контент, выброшенный
  из композиции при переключении вкладки в ActionMode, не восстановил бы цвет
  (гонка двух Mate-циклов). Один источник правды в host'е убирает гонку
  по построению.

## D2. Words-рефактор (`dictionaryTab`)

### D2.1 `DictionaryTabScreen` → `WordsTabContent`

- Из экрана уходит: Scaffold, topBar, FAB, SnackbarHost, `SystemBarsWidget`
  (→ host, D1.7), `padding` Box (→ host, D1.2).
- Остаётся: Box-контент (loading / empty / `WordListWidget`), диалоги,
  `BackHandler(isActionMode)` (на «Группах» words вне композиции → перехвата
  нет, В5 даром), **`LifecycleEventHandler`** (явно: сегодня no-op в
  `UiMessageProcessor`, но проводка сохраняется — логика не трогается).
- Старые Preview (`PreviewLoading/Empty/Loaded`) переписываются под новую
  сигнатуру — прежние не скомпилируются.

### D2.4 Snackbar → host

- `WordsTabContent` получает `SnackbarHostState` параметром; существующий
  `LaunchedEffect(snackbarState.show)` показывает через него.

### D2.5 Reducer / State / EffectHandlers — не трогаются (скоуп: фаза 3)

- Рефактор строго UI-обвязки; существующие unit-тесты проходят без правок
  **в фазе 3**. В фазе 4 (rename) тесты правятся механически (package/имена) —
  это не нарушение гейта.
- Калибровка (ревью): тесты покрывают только logic-слой — UI-регресс ловит
  ручной чек-лист фазы 5, он расширен соответствующе.

### D2.6 Handle-паттерн: `rememberWordsTabHandle`

- **Решение:** words-модуль экспортирует
  `rememberWordsTabHandle(factory, navigator): WordsTabHandle` — создаёт VM
  и коллектит state ВНУТРИ модуля; наружу — узкие куски:
  - `ActionTopBar(): @Composable` (`ActionTopBarWidget` уже public — факт)
  - `isActionMode: State<Boolean>`, `isFabVisible: State<Boolean>`
    (derived — host не рекомпозится от каждого keystroke words-диалога)
  - `onFabClick`, `onExitSelectionMode`
  - `Content(snackbarHostState)`
- Почему: app-мост НЕ вызывает `viewModel()`/`collectAsStateWithLifecycle` →
  в `app/build.gradle.kts` не добавляются lifecycle-зависимости (решение
  пользователя: лишних deps в app не заводить). VM-owner не меняется
  (та же NavBackStackEntry — факт подтверждён ревью).
- Уточнение по ревью конвенций: handle НЕ уводит сборку из CompositionRootImpl
  — VM-создание и collect УЖЕ живут внутри модулей (экран сам зовёт
  `viewModel(factory = viewModelFactory {...})`); меняется только форма
  публичного API модуля: один `Screen(...)` → handle с узкими кусками.
  Прецедента в проекте нет — **первый экземпляр**; паттерн фиксируется в
  `project-architecture.md` как шаблон для будущих tab-модулей.

## D3. Grouptab-заглушка

- Отдельный модуль сразу: каркас Э2+ готов, host о groupstab не знает.
- `GroupsTabStubScreen` — stateless composable. VM/Mate НЕТ (появятся в Э2).
  Тестов НЕТ — нет логики.
- Строки заглушки — `core-resources` (`values/` + `values-ru-rRU/`; ru-локаль
  в проекте давно объявлена — «lint-мина MissingTranslation» из ревью
  оказалась false positive: папка называется `values-ru-rRU`).

## D4. Мост в app (`CompositionRootImpl`)

### D4.1 Чистая склейка

- Мост вызывает `rememberWordsTabHandle` + host-экран; сам НЕ создаёт VM и
  НЕ коллектит state (D2.6). Собирает `TabSpec`'ы из кусков handle —
  **обязательно в `remember`** с корректными ключами: без него data class с
  лямбдами пересоздаётся каждую рекомпозицию и `@Stable`-skipping (D1.3)
  не работает.
- ActionMode на «Группах» отсутствует по построению: override живёт только
  в words-`TabSpec`.
- Склейка без логики → без unit-тестов (осознанно).

### D4.2 Сброс ActionMode при переключении

- `onTabSelect = { tab -> if (tab != selectedTab) { host.accept(SelectTab(tab));
  words.onExitSelectionMode() } }` — **guard обязателен**: тап по уже активной
  вкладке не должен сбрасывать выделение пользователя (ревью F5).
- `Msg.ExitSelectionMode` существует (факт: `VocabularyTabReducer.kt:52-55`).

## D5. Rename `dictionaryTab` → `wordstab` (полный)

- Модуль: каталог, `settings.gradle.kts`, `app/build.gradle.kts`
  (`:modules:screen:dictionaryTab`), namespace (+ проверить BuildConfig),
  package → `me.apomazkin.wordstab`.
- Классы — полный нейминг-фикс (ревью F7: иначе зеркальный рассинхрон):
  `Vocabulary*` → `Words*` И `DictionaryTab*` → `Words*`.
- App-сторона: `di/module/dictionarytab/*`, `AppComponent`,
  `VocabularyNavigatorImpl`, `CompositionRootImpl`, тест
  `DictionaryTabUseCaseImpl` в `app/src/test`.
- Доки: `project-architecture.md`, `logging.md`, `dictionary_tab.puml`.
- Отдельный коммит ПОСЛЕ зелёной фазы 3 (ревьюабельность диффа).
- R-классы не страдают: у модуля нет своего `res/` (факт подтверждён ревью).

## Риски

- **Главный:** регрессия «Слов» при выносе Scaffold — митигация: логика не
  трогается (D2.5), контент как есть, регресс-гейт + расширенный чек-лист.
- Статусбар: после D1.7 источник один (host) — руками проверить обе вкладки
  и выход из ActionMode.
- SaveableStateHolder + paging: базовый скролл переживает (`cachedIn`),
  кейс активного поиска — известное ограничение (D1.5), в чек-листе.
