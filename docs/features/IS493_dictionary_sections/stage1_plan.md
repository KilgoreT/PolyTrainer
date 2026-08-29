# IS493 / Э1 — План реализации: две вкладки в словаре

Статус: **v3** (после ревью конвенций 2026-08-04: flat-размещение подтверждено,
модуль групп — `groupstab`, viewModelFactory-хелпер, remember для TabSpec,
handbook-фиксация паттернов).
Основа: [rollout_stages.md](rollout_stages.md) Э1, [architecture.md](architecture.md)
§4.1 (А9), [stage1_design_tree.md](stage1_design_tree.md) (D1–D5).

**TDD-порядок: сначала тесты (red), потом код (green).** Composable-слои — вне
TDD-контура (помечено); их вес несёт ручной чек-лист фазы 5.

- БД в Э1 не трогается.
- Новых **library**-зависимостей в `app/build.gradle.kts` НЕТ (handle-паттерн,
  D2.6); project-deps новых модулей — добавляются (фаза 3.3).
- Размещение модулей — flat (стиль проекта); package-анатомия — по `naming.md`:
  `logic/` (State, Msg, Reducer), `ui/`, `deps/`. Package'ы:
  `me.apomazkin.vocabulary`, `me.apomazkin.groupstab`, после rename —
  `me.apomazkin.wordstab`.
- Отклонение от А9, принятое ревью: dictionaryId в Э1 host НЕ раздаёт —
  words резолвит сам, как сейчас; host забирает владение словарём в Э2 (D1.6).

## Коммит-нарезка

1. Фазы 1–2 (host + заглушка) — build зелёный, app ещё на старом экране.
2. Фаза 3 «Замена контракта» — атомарно, одним коммитом.
3. Фаза 4 rename — отдельным коммитом, без смысловых правок.

---

## Фаза 1. Host-модуль `modules/screen/vocabulary`

### 1.1 Каркас модуля

- [ ] `settings.gradle.kts` — include `:modules:screen:vocabulary`.
- [ ] `build.gradle.kts` — точный список deps (НЕ «как dictionaryTab»):
  - `mate`, `theme`, `ui`, `core-resources`, `di`
  - `lifecycleViewmodelCompose`, `lifecycleRuntimeCompose`
  - без: paging, activityCompose, dagger (у reducer'а нет зависимостей — D1.1)
- [ ] Контракт вкладки (D1.3):
  - `enum class VocabularyTab { WORDS, GROUPS }`
  - `@Stable data class FabSpec(iconRes, visible, onClick)`
  - `@Stable data class TabSpec(titleRes, topBarOverride, fab, content)`
- [ ] Строки в `core-resources` (`values/` + `values-ru-rRU/`):
  - титулы вкладок «Слова» / «Группы»
  - титул AppBar — переиспользуется существующий `item_title_vocabulary`

### 1.2 Mate-цикл host'а

- [ ] **ТЕСТЫ СНАЧАЛА** — reducer-тесты:
  - исходный state: `selectedTab = WORDS`
  - `Msg.SelectTab(GROUPS)` → `selectedTab = GROUPS`
  - `Msg.SelectTab` на текущую вкладку → state без изменений
- [ ] Код: `VocabularyHostState(selectedTab)`, `Msg.SelectTab`,
  `VocabularyHostReducer` (без эффектов — сброс ActionMode делает мост, D4.2).
- [ ] `VocabularyHostViewModel` — по образцу `DictionaryTabViewModel`
  (ViewModel + Mate + MateStateHolder); создаётся **внутри модуля** через
  существующий хелпер `viewModelFactory { }` из `modules/core/di` (не «голый»
  `viewModel {}`), без Dagger (D1.1); Dagger-factory появится в Э2 —
  цена миграции выписана в D1.1.

### 1.3 Host-экран (вне TDD-контура — composable)

- [ ] `VocabularyHostScreen(tabs, appBar-слот через VocabularyHostUiDeps)`:
  - `Scaffold`: `containerColor = Transparent`,
    `contentWindowInsets = WindowInsets(0)`, контент в `padding(paddingValues)`
    — параметры переносятся с текущего words-Scaffold (D1.2)
  - `topBar` = `topBarOverride` активной вкладки `?:` `AppBar`
  - `TabRow` — первый элемент content (НЕ topBar-слот)
  - FAB — из `FabSpec`, обёрнут в `AnimatedVisibility(scaleIn/scaleOut)` +
    `enabled` (перенос текущей анимации, D1.2)
  - `SnackbarHost` один; `SnackbarHostState` передаётся в content
  - контент вкладки — через `SaveableStateHolder` (D1.5)
  - **статусбар считает host**: `topBarOverride != null` → secondary,
    иначе transparent (D1.7 — гонка words-SystemBars устранена)
- [ ] Preview на обе вкладки.

## Фаза 2. Заглушка `modules/screen/groupstab`

- [ ] Модуль `modules:screen:groupstab` (deps: theme, ui, core-resources) —
  имя во множественном числе, согласовано с `wordstab` (ревью конвенций К2).
- [ ] `GroupsTabStubScreen`: empty-state (иконка + текст).
- [ ] Строки заглушки в `core-resources` (`values/` + `values-ru-rRU/`).
- [ ] Логики нет → unit-тестов нет (осознанно, D3).

## Фаза 3. Замена контракта (АТОМАРНО: words + мост одним коммитом)

### 3.1 Регресс-гейт

- [ ] Существующие unit-тесты words-модуля НЕ меняются в этой фазе и обязаны
  остаться зелёными (reducer/state/effects не трогаются, D2.5).
  Помнить: они покрывают только logic-слой — UI-регресс ловит чек-лист фазы 5.

### 3.2 Words: контент + handle

- [ ] `DictionaryTabScreen` → `WordsTabContent` (D2.1):
  - убрать Scaffold / topBar / FAB / SnackbarHost / SystemBarsWidget
    (статусбар теперь у host'а, D1.7)
  - оставить: Box-контент (loading / empty / `WordListWidget`), диалоги,
    `BackHandler(isActionMode)`, `LifecycleEventHandler` (явно, D2.1)
  - свой `padding` Box убирает — padding применяет host (D1.2)
  - snackbar — через переданный `SnackbarHostState` (D2.4)
- [ ] `rememberWordsTabHandle(factory, navigator)` — **handle-паттерн** (D2.6):
  создаёт VM внутри words-модуля, возвращает handle:
  - `ActionTopBar(): @Composable` (виджет уже public — шага «сделать public» нет)
  - `isActionMode: State<Boolean>`, `isFabVisible: State<Boolean>` — узкие
    derived-State (host не рекомпозится от каждого keystroke, D2.6)
  - `onFabClick`, `onExitSelectionMode`
  - `Content(snackbarHostState)`
- [ ] Preview words переписать под новую сигнатуру (без deps, без Scaffold).
- [ ] `deps/DictionaryTabUiDeps.kt` — удалить; AppBar-слот теперь у host'а
  (`VocabularyHostUiDeps`, D1.4).

### 3.3 Мост в app

- [ ] `CompositionRoot.VocabularyTabDep` → `VocabularyHostDep` (сигнатура та же).
- [ ] `CompositionRootImpl`: чистая склейка (D4.1) — БЕЗ viewModel/collect в app:
  - `val words = rememberWordsTabHandle(factory, navigator)`
  - words `TabSpec`: `topBarOverride` из `words.isActionMode` +
    `words.ActionTopBar`; `FabSpec` из `words.isFabVisible`/`onFabClick`;
    content = `words.Content`
  - groups `TabSpec`: content = `GroupsTabStubScreen`, `fab = null`,
    `topBarOverride = null`
  - **`TabSpec`'ы — в `remember` с корректными ключами** (иначе лямбды
    пересоздаются каждую рекомпозицию и `@Stable`-skipping не работает)
  - реализация `VocabularyHostUiDeps.AppBar` = `DictionaryAppBar` с той же
    проводкой, что нынешний анонимный объект (`openDictionaryCreate`,
    `openPerDictionaryComponents` — оба callback'а в сигнатуре депа есть)
  - `onTabSelect`: **guard** `if (tab != current)` →
    `host.accept(SelectTab)` + `words.onExitSelectionMode()` (D4.2)
- [ ] `Vocabulary.kt` / `MainScreen` — только имя депа; роуты не меняются.
- [ ] `app/build.gradle.kts`: добавить `:modules:screen:vocabulary` и
  `:modules:screen:groupstab` в deps. Lifecycle-библиотеки в app НЕ добавляются.
- [ ] Мост — склейка без логики → без unit-тестов (осознанно).

## Фаза 4. Rename (механика, отдельный коммит)

- [ ] Модуль `dictionaryTab` → `wordstab`: каталог, `settings.gradle.kts`,
  `app/build.gradle.kts` (`:modules:screen:dictionaryTab`), namespace
  (+ проверить использования BuildConfig), package
  `me.apomazkin.dictionarytab` → `me.apomazkin.wordstab`.
- [ ] Классы: `Vocabulary*` → `Words*` И `DictionaryTab*` → `Words*`
  (полный нейминг-фикс: `WordsTabScreen`, `WordsTabReducer`, `WordsTabState`,
  `WordsTabViewModel`, `WordsTabUseCase`, `WordsNavigator`).
- [ ] App-сторона: `di/module/dictionarytab/` (`DictionaryTabModule`,
  `DictionaryTabUseCaseImpl` + его тест в `app/src/test`), `AppComponent`,
  `navigator/VocabularyNavigatorImpl`, `CompositionRootImpl`.
- [ ] Тесты модуля: package/импорты правятся механически (это НЕ нарушение
  регресс-гейта фазы 3 — он ограничен фазой 3).
- [ ] Доки: `docs/handbook/guides/project-architecture.md` — не только rename:
  **зафиксировать новые паттерны** (TabSpec-контракт host-модуля; handle-паттерн
  module-API) и отклонение от «assisted-factory для всех VM» (host-VM без
  Dagger до Э2); `logging.md`;
  `modules/screen/dictionaryTab/dictionary_tab.puml`.
- [ ] Прогнать все тесты + build после rename.

## Фаза 5. Верификация этапа

- [ ] `testDebugUnitTest` (vocabulary + wordstab), `assembleDebug`, `lintDebug`
  — через `./scripts/cc-build.sh`.
- [ ] Ручной чек-лист:
  - [ ] регресс «Слов»: список, поиск, добавление, удаление, ActionMode
  - [ ] вкладки переключаются; на «Группах» — заглушка, FAB скрыт
  - [ ] ActionMode недоступен на «Группах» (нет входа в selection)
  - [ ] ActionMode сбрасывается при переключении вкладки
  - [ ] тап по УЖЕ активной вкладке в ActionMode — выделение НЕ сбрасывается
  - [ ] статусбар: цвет корректен на обеих вкладках, в т.ч. после
    переключения из ActionMode
  - [ ] snackbar работает (после переезда в host): например удаление слова
  - [ ] скролл «Слов» переживает переключение вкладок
  - [ ] поиск активен → переключение вкладки и назад: без мигания списка,
    фильтр/скролл в разумном состоянии
  - [ ] смена словаря; back; поворот экрана (`selectedTab` переживает)
