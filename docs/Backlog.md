# Backlog

---

## Продолжение фич

- **IS481 phase 2: feature-scoped tag `###ComponentConstructor###` + логи в Migration_012_to_013 и DAO cascade.**
  `checklist.md § Примечание о логах` декларирует tag `###ComponentConstructor###` для adb logcat фильтрации фича-событий. Реальность: используются module-scoped tags `ComponentsManager` / `PerDictComponents`. Migration_012_to_013 молчит полностью (счётчики rewrite text/image rows, drop индексов, backfill timestamps — не пишутся). DAO cascade (`QuizConfigDao.updateComponentRefs`, prefs reset) тоже молчит.
  
  Impact: manual smoke verify через logcat усложнён; при багах миграции (например `long_text` rows не консолидировались) узнать получится только по результату (пустые компоненты у юзера / crash в parser), не по logs.
  
  Что сделать: добавить `LogTags.COMPONENT_CONSTRUCTOR = "###ComponentConstructor###"` в shared logger, использовать в UseCase impls + Migration_012_to_013 (счётчики per step) + DAO cascade методах. Решить — оставить ли параллельно module-scoped tags или снести.

- **IS481 phase 2: `RenameOutcome.BuiltInProtected` conflation для soft-deleted типов.**
  `renameComponent(typeId)` для **soft-deleted** типа возвращает `RenameOutcome.BuiltInProtected` — misleading: тип не built-in, он удалён. UI показывает «нельзя переименовывать встроенный» вместо «компонент удалён».
  
  Что сделать: добавить variant `RenameOutcome.NotFound` (либо `Removed`) в sealed. UseCase impl различает: `type.systemKey != null → BuiltInProtected`, `type.removedAt != null → NotFound`. Аналогично проверить `DeleteOutcome` / `softDeleteComponent` — там может быть та же проблема.

---

## Срочное

- **[Миграция существующих спек в `docs/handbook/specs/` под новый формат contract_spec].**

  **Контекст.** В рамках декомпозиции шага `contract` Business sub-flow (см. `docs/features/FORGEFLOW_contract_design.md`) выход контрактного блока теперь = спека в `docs/handbook/specs/<feature>.md`. Структура новой спеки:
  ```
  # <Feature Name>

  ## Бизнес-описание
  ## User Stories
  ## State
  ## UI Messages
  ## IO
    ### Effects (Datasource / Navigation / Ui)
    ### Subscribers
  ## UseCase
  ## Тестовые сценарии (если применимо)
  ```

  **Что есть сейчас.** Существующие спеки в `docs/handbook/specs/` (по `README.md` директории — `dictionary-list.md`, `dictionary-create.md`, `dictionary-appbar.md`, `wordcard.md`, и др.) в **старом формате**: «срез текущего состояния, констатация бизнес-логики и кейсов, БЕЗ разбивки по TEA-слоям».

  **Расхождение.** Старый формат описывает фичу единым текстом (бизнес-логика + UI-кейсы вперемешку), без явной структуры State / Msg / Effect / Subscriber / UseCase. Новый формат — структурированный по TEA-слоям, явно показывает контракт каждого слоя.

  **Решение.** Миграция всех существующих спек **единоразово** под новый формат. Не lazy (option b) и не гибрид (option c) — full migration сейчас.

  **Зачем сейчас, а не lazy:**
  - Lazy миграция (по мере касания фич) создаёт **долгий период coexistence** двух форматов в одной директории. Разработчики и агенты не понимают «чему доверять» когда находят спеку в старом формате.
  - Lazy не покрывает фичи которые редко меняются — они навсегда останутся в старом формате.
  - Документация-as-contract работает только если **все** спеки консистентны. Mix — не контракт.

  **Алгоритм миграции (для каждой существующей спеки):**

  1. Прочитать существующую спеку
  2. Прочитать соответствующий код (State.kt, Message.kt, Reducer.kt, EffectHandler.kt, UseCase.kt)
  3. Извлечь из кода:
     - `## State` — текущая структура data class + extensions + computed (если есть)
     - `## UI Messages` — sealed interface Msg + варианты + reducer-логика per Msg
     - `## IO`:
       - `### Effects` — все sealed Effect-интерфейсы (Datasource / Navigation / Ui) + variants
       - `### Subscribers` — все FlowHandler-подписки
     - `## UseCase` — interface методы
  4. Из существующей спеки забрать:
     - `## Бизнес-описание` — найти текст про «что фича делает», переформулировать кратко
     - `## User Stories` — если есть в старой спеке, перенести; если нет — выписать из бизнес-описания
     - `## Тестовые сценарии` — если в старой спеке есть кейсы тестирования, перенести
  5. Если в коде обнаружены **расхождения** с текущей спекой (старая описывает поведение которого уже нет в коде или наоборот) — спека пишется по **коду** (источник истины), расхождение фиксируется в `tech debt` секции миграционного отчёта
  6. Перезаписать файл целиком в новом формате

  **Список спек для миграции** (на момент записи в backlog):
  - `dictionary-list.md`
  - `dictionary-create.md`
  - `dictionary-appbar.md`
  - `wordcard.md`
  - и др. — полный список взять из `docs/handbook/specs/README.md`

  **Когда:**
  - **После** того как создан step file `contract_spec.md` в `docs/forgeflow-overlay/steps/` и первый прогон новой decomposition прошёл на новой фиче (validation формата)
  - **До** запуска адаптивного flow на существующих фичах (иначе первый запуск столкнётся с разнобоем форматов)

  **Подход к выполнению:**
  - Не делать одним PR — миграция спек 1-к-1 за итерацию (один PR на спеку)
  - Каждый PR содержит: миграция спеки + (если обнаружены расхождения кода/спеки) issue в `Tech Debt`
  - В commit message ссылка на это backlog-задание

- **[Quiz config UX: UI редактор `quiz_configs.component_refs`].**

  **Контекст.** После IS481 quiz_configs schema + runtime wire уже есть (auto-INSERT default `[translation]` для новых словарей; миграция existing наполняет по факту имеющихся типов). Пользователь не может **менять** конфиг — нужен UI.

  **Проблема.** В IS481 default `[translation]` для нового словаря зафиксирован, пока без UI пользователь не может включить definition (или другой user-defined компонент) в квиз для нового словаря. Для existing словарей с definition после миграции работает автоматически (миграция включила).

  **Что решить в фиче:**
  - Экран/диалог редактирования `quiz_configs` для словаря — список доступных `component_types` словаря + чекбоксы какие включить в квиз для каждого `quiz_mode`.
  - Multiplicity quiz_mode — один глобальный выбор на словарь или per-mode.
  - Auto-select когда в словаре единственный component type (тупо показывать UI с одним вариантом не надо).
  - Создать `modules/domain/quiz` (новый domain модуль) — вынести `QuizConfig` / `ComponentTypeRef` (sealed) из `modules/domain/lexeme` (см. TODO-комменты на типах из IS481, см. AGG-10).
  - DAO `deleteComponentType` атомарно cleanup'ит `quiz_configs.component_refs` (одна транзакция: SELECT configs с ref на тип → UPDATE через `json_remove` → DELETE component_type). Реализовать F6 invariant (см. `_alignment_decisions.md` MIN-11).
  - **Rename component_type** — атомарная операция: UPDATE `component_types.name` + UPDATE всех `quiz_configs.component_refs` (заменить old name → new name через `json_replace` либо собрать новый JSON в Kotlin). Без cleanup ссылок rename → UX-регрессия: graceful skip в квизе, definition исчезает после переименования. До implementing rename UI — rename операция не поддерживается (component_types в IS481 immutable после миграции).
  - **F1 invariant maintenance:** при добавлении нового `quiz_mode` (`card`, `recall`, ...) — миграция обязана INSERT default config row для **всех existing dictionaries** (`INSERT INTO quiz_configs SELECT id, '<new_mode>', '<default_refs>' FROM dictionaries`). Без этого новый mode получит пустую quiz session для существующих словарей. F1 — процедурный invariant, DDL `UNIQUE(dictionary_id, quiz_mode)` его не покрывает (см. 07.md § Invariants F1).

  **Зависит:** IS481 merged.

---

## Critical Bugs

- **[disableUserInput() инвертирован].**
  `modules/screen/quiz/chat/logic/State.kt:206` — устанавливает `isUserInputEnable = true` вместо `false`.
  Вызывается 6 раз в ChatReducer. Пользователь может вводить текст когда не должен.
  Нужно: заменить `true` на `false`, убрать дублированные вызовы в `Msg.GetAnswer` и `Msg.Skip`.

- **[TermNotLoaded не реализован].**
  `modules/screen/wordcard/mate/WordCardReducer.kt:16` — `TODO("TermNotLoaded is not implemented")`.
  Если слово не загрузится — краш. Нужно: добавить обработку ошибки (snackbar + закрытие экрана или retry).

- **[Нет error handling в эффект-хендлерах].**
  Ни один DatasourceEffectHandler не оборачивает вызовы в try-catch.
  Исключение из Room/UseCase убивает coroutine scope без recovery.
  WordCard явно бросает `IllegalStateException("Lexeme not found")`.
  Нужно: обернуть все `withContext(Dispatchers.IO)` в try-catch, генерировать Error-сообщения.

- **[Race condition в Mate.accept()].**
  Нет синхронизации между чтением `_state.value` и записью.
  Два эффекта вернувшие сообщения одновременно — один прочитает stale state.
  Нужно: Mutex или `_state.update {}` вместо read-then-write.

---

- **[Lint rule: запрет прямого android.util.Log].**
  `android.util.Log` допустим только в `LogcatSink`. Lint check, detekt rule или CI grep-проверка.

- **[ProGuard general keep для native methods].**
  Заменить узкое правило `-keep class androidx.sqlite.** { native <methods>; }` (`app/proguard-rules.pro:21`) на канонический широкий keep: `-keepclasseswithmembernames class * { native <methods>; }`. Текущее правило сохраняет native методы только внутри уже-сохранённых классов в `androidx.sqlite.**`; если в `androidx.sqlite.db.**` или других пакетах добавятся native — под угрозой обфускации. Стандартная рекомендация Android docs для native libs. Источник: IS481 vPrepared global code review (Bugs B6).

- **[DATABASE_NAME: вынести в Database companion + sync с androidTest Schema].**
  Production `RoomModule.kt:95` имеет `private const val DATABASE_NAME = "name"` (плюс TODO с 2021 «поправить имя базы»), androidTest `Schema.kt:16` хардкодит `"TestDatabaseName"`. Нет общей точки — рассинхрон возможен при будущем integration теста на full bootstrap. Решение: вынести `DATABASE_NAME` в `Database` companion object как `internal` (доступен androidTest source set). Источник: IS481 vPrepared global code review (Architecture A4).

- **[Централизованная система ошибок и снекбаров].**
  Сейчас каждый экран по-своему обрабатывает ошибки: кто-то Toast, кто-то Snackbar с boolean флагом, кто-то просто молчит. Нет единого механизма показа ошибок пользователю.
  Нужно: централизованный ErrorHandler / SnackbarManager. Единый API для показа ошибок из любого места (EffectHandler, WebView, навигация). Единый UI компонент (Snackbar/BottomSheet). Типизированные ошибки (network, unknown, validation).

---

- **[DictUiEntity.flagRes: Int → Int?].**
  flagRes = 0 как "нет флага" — магическое значение. Сделать nullable, убрать `?: 0` из маппинга в UseCase. 11 файлов затронуто.

---

## Архитектура

- **[Repository pattern: mapper API→Domain в `core-db-impl`, `CoreDbApi` возвращает domain].**
  Сейчас (после IS482) mapper `LexemeApiEntity.toDomain(): Lexeme` живёт в `app/.../mapper/LexemeMapper.kt` — соответствует convention `data-layer.md` § «Маппинг сущностей» («API → Domain в UseCase модуле»). По Clean Architecture / Repository pattern правильнее: mapper в **`core-db-impl`** (data adapter), а `CoreDbApi.LexemeApi.getLexemes()` возвращает сразу `Lexeme` (domain), не `LexemeApiEntity`.
  Текущее состояние: UseCase знает про data-API (`LexemeApiEntity`) → нарушение Dependency Rule. UseCaseImpl де-факто работает как mapper.
  Решение: `CoreDbApi.LexemeApi.*` методы меняют тип возврата на `Lexeme` (domain). Mapper переезжает из `app/.../mapper/` в `core-db-impl` (рядом с маппингом `DbEntity → ApiEntity`). `LexemeApiEntity` становится internal DTO в `core-db-impl` (или остаётся в `core-db-api` для transition period с `@Deprecated`). `core-db-api` начинает зависеть от `modules/domain/lexeme` (по Clean — нормально, data → domain).
  Затрагивает: 7 lexeme-методов в `CoreDbApi.LexemeApi`, все 3 `UseCaseImpl` (`WordCardUseCaseImpl` / `QuizChatUseCaseImpl` / `DictionaryTabUseCaseImpl`), `LexemeMapper.kt` (удаляется/переезжает), `data-layer.md` гайд (правка convention).
  Аналогично — Word, Term, WriteQuiz (если унификация распространится).
  Источник: F-A5 IS482 REVIEW — поднят аспект «mapper в app/ + Dependency Rule инверсия». Решение отложено как big refactor.
  Объём: большой (refactor data-API контракта + 3 UseCaseImpl).

- **[Wordcard mate refactor: generic компоненты в Msg / State / Reducer (после IS481)].**
  После IS481 в `CoreDbApi` / `UseCase` появляются generic-методы для компонентов (`addLexemeWithBuiltInComponent` etc.), а старые специфичные методы (`addLexemeWithTranslation` / `addLexemeWithDefinition`) остаются как `@Deprecated` обёртки. Mate-слой wordcard (`LexemeState.translation/definition` поля, `Msg.CreateTranslation/CreateDefinition` и зеркальные 20+ Msg, `DatasourceEffect.UpdateLexemeTranslation/Definition`) продолжает работать через эти обёртки.
  Нужно: переписать wordcard mate на generic — `LexemeState.components: List<ComponentValueState>`, generic `Msg.CreateComponent(typeId)` / `Msg.UpdateComponentInput(componentId, value)` etc. Объём ~400+ строк + ~10 reducer-тестов с нуля. Тот же refactor — в quiz/chat и dictionaryTab. После — выпиливаем `@Deprecated` обёртки.
  **Триггеры на выпиливание shim после рефактора:**
  - shim-поля `Lexeme.translation: Translation?` / `Lexeme.definition: Definition?` (B4 IS481) — удаляются из `Lexeme` data class.
  - value-classes `Translation` / `Definition` в `modules/domain/lexeme` — удаляются полностью (сейчас `@Deprecated`).
  - заполнение shim в `LexemeApiEntity.toDomain()` через built-in lookup — удаляется, маппер сокращается до `components.map { it.toDomain() }`.
  - `@Deprecated` обёртки в `CoreDbApi.LexemeApi` (`addLexemeWithTranslation/Definition`, `updateLexemeTranslation/Definition`) — удаляются.
  - `@Deprecated` обёртки в `WordCardUseCase` (`deleteLexemeTranslation/Definition`) — удаляются.

  Триггер на старт фичи: появление UI для user-defined компонентов (тогда mate всё равно надо рефакторить, заодно выпиливаем shim).

- **[Snackbar queue: undo первого snackbar теряется при быстром втором].**

  **Контекст.** В IS479 (wordcard inline lexeme editing) реализован snackbar+undo для удалений (translation/definition/lexeme/cascade). Канон через `UiHost`/`UiEffect.ShowSnackbarWithUndo`:
  ```
  Msg.TranslationDeleted/DefinitionDeleted/LexemeRemoved/LexemeCascadeRemovedWithUndo
    → reducer эмитит UiEffect.ShowSnackbarWithUndo(messageRes, actionLabelRes, undoMsg)
    → UiEffectHandler делает uiHost.showSnackbarWithAction(...)
    → если пользователь нажал Action → consumer(undoMsg) → reducer обработает undo
  ```
  Реализация `UiHostImpl.showSnackbarWithAction` использует `snackbarHostState.showSnackbar(message, actionLabel, duration = SnackbarDuration.Short)`.

  **Проблема.** Material 3 `SnackbarHostState.showSnackbar` использует `MutatorMutex` — **новый вызов отменяет coroutine текущего snackbar'а**. Отменённая coroutine получает `CancellationException`, suspend возвращается **не** с `SnackbarResult.ActionPerformed` → `showSnackbarWithAction` возвращает `false` → `undoMsg` первого удаления **никогда не отправляется**.

  **Сценарий воспроизведения:**
  1. Пользователь удалил translation у lexeme A → snackbar "Перевод удалён (Отменить)".
  2. До таймаута первого snackbar'а удалил translation у lexeme B → второй snackbar того же типа.
  3. Mutator отменил первый. Первый undoMsg потерян. Пользователь больше **не может отменить** первое удаление, оно стало необратимым после короткого окна.

  **Файлы:**
  - `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/widget/internal/UiHostImpl.kt` — `showSnackbarWithAction` (строки 22-34).
  - `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/UiEffectHandler.kt` — обработка `UiEffect.ShowSnackbarWithUndo` (строки 19-29).
  - `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/deps/UiHost.kt` — interface.

  **Возможные подходы:**

  - **Вариант A — queue в UiEffectHandler.** Внутри handler'а держать `Channel<UiEffect>` или `Mutex` — pop'ать следующий snackbar только после завершения предыдущего. Минусы: пользователь ждёт пока пройдут все накопленные snackbar'ы; при таймауте 4с × N — долго. Очередь нужно ограничивать и/или сливать однотипные сообщения.

  - **Вариант B — auto-apply pending action при cancellation.** Если первый snackbar отменён mutator'ом (а не пользовательским Dismiss / Timeout), всё равно эмитить undoMsg первого. Минусы: меняет UX — пользователь явно не нажал отмену, но undo сработал. Нелогично.

  - **Вариант C — отличать CancellationException от Dismissed.** Сейчас `showSnackbarWithAction` возвращает Boolean (`ActionPerformed`/`Dismissed`). Расширить API: вернуть sealed `SnackbarOutcome { ActionPerformed, Dismissed, Cancelled }`. Над `Cancelled` — выбрать стратегию (по умолчанию игнорировать как сейчас, но логировать).

  - **Вариант D — UX-дизайн: блокировать второе удаление пока активен snackbar первого.** Радикально, но защищает инвариант "одно удаление с undo за раз". Минусы: лишний фрустрейт пользователя.

  **Рекомендация:** Вариант A (queue) с лимитом 3-5 и dedup однотипных сообщений. Это canonical Material design pattern для "transient action".

  **Дополнительный риск.** Та же проблема для будущих error-snackbar'ов после миграции IS479 ошибок на UiHost (см. соседнюю backlog-задачу про legacy snackbar). Решение queue должно покрывать оба пути (snackbar с action и без).

  **Тестируемость.** Юнит-тест: эмулятор `SnackbarHostState`/`UiHost` с быстрой последовательностью `showSnackbarWithAction` → проверить что undoMsg обоих доставляются. Возможно потребуется обёртка над `SnackbarHostState` для контроля времени.

- **[Прогнать trim текстовых полей по остальным UseCaseImpl].**
  В IS479 правило зафиксировано в `docs/handbook/guides/data-layer.md` (раздел "Нормализация текстового ввода (trim)") и применено в `WordCardUseCaseImpl` (4 точки: updateWord, addLexemeTranslation, addLexemeDefinition, restoreLexeme).
  Нужно: пройтись по остальным `*UseCaseImpl` в `app/src/main/java/me/apomazkin/polytrainer/di/module/` и добавить `.trim()` перед передачей строки в `CoreDbApi` (или в сеть, если появится). Затронуты: `DictionaryUseCaseImpl`, `DictionaryTabUseCaseImpl`, `SettingsTabUseCaseImpl`, `QuizTabUseCaseImpl`, `QuizChatUseCaseImpl`, `SplashUseCaseImpl`, `StatisticUseCaseImpl`, `DictionaryAppBarUseCaseImpl` — пересмотреть write-методы со String на входе.
  Если для конкретного поля trim семантически вреден — задокументировать исключение комментарием у метода.

- **[Прогнать правило "виджеты получают callbacks, не sendMessage" по остальным модулям].**
  В IS479 правило зафиксировано в `docs/handbook/guides/ui-patterns.md` (раздел "Виджеты получают callbacks, не `sendMessage`") и применено в `modules/screen/wordcard/`. Виджеты wordcard переписаны на плоские callbacks (5 виджетов, включая декомпозицию `LexemeItemWidget` на 12 параметров).
  Нужно: пройтись по всем `modules/screen/*/widget/` и `modules/widget/*/`, найти composable-функции принимающие `sendMessage: (Msg) -> Unit` / `sendMsg`, переписать на плоские callbacks. Параметры, нужные только для построения Msg (id, loaded), убрать — id уезжает наверх через callback-builder. Если callbacks > 5-7 — декомпозировать виджет.
  Предварительно затронуты: dictionary, dictionarytab, quiztab, settingstab, main, splash, stattab + всё в `modules/widget/`.

- **[Разобраться в слоях entity].**
  Нет доменного слоя. ApiEntity (core-db-api) де-факто выполняет роль доменной сущности. UI модели (UiItem, CountryFlagItem) содержат и бизнес-поля и UI-поля. Суффиксы непоследовательны (ApiEntity, UiItem, UiEntity, Item, Info).
  Нужно: определить конвенцию слоёв entity, решить нужен ли отдельный domain layer или ApiEntity = доменная (с переименованием). Описать в гайде.

- **[DictionaryUseCase.getDictionaryList() — кандидат на удаление].**
  suspend-версия не вызывается — используется только flowDictionaryList(). CoreDbApi.getDictionaryList() нужен другим UseCase'ам, но в DictionaryUseCase — мёртвый код.

- **[Шиммеры при загрузке].**
  Добавить loading-состояние для цепочек эффектов. Пока цепочка не завершена — показывать шиммеры вместо контента. Шиммеров в проекте ещё нет — нужно создать компонент. Начать с формы словаря (загрузка флагов + данных словаря).

- **[Reducer не чистый — ChatReducer].**
  ChatReducer принимает ResourceManager и LexemeLogger. Логирование — сайд-эффект.
  WordCardReducer чистый — несоответствие reference-реализации.
  Нужно: вынести строки в сообщения или предвычислять в эффект-хендлере. Логирование — через обёртку Mate.

- **[Service Locator вместо DI].**
  `Context.appComponent` — service locator anti-pattern. Каждый composable сам достаёт зависимости.
  Нужно: рассмотреть миграцию на Hilt или component-per-feature.

- **[UseCaseImpl в app-модуле].**
  9 реализаций в `app/di/module/`. App знает про ВСЕ реализации, фичи не независимы.
  Нужно: перенести UseCaseImpl в соответствующие feature-модули.

- **[CompositionRoot — god-object].**
  Интерфейс `CompositionRoot` — 8 `@Composable` методов, `CompositionRootImpl` — 9 параметров (7 Factory + envParams + logger). Растёт с каждым экраном.
  Нужно: разбить на per-tab интерфейсы (`VocabularyDeps`, `QuizDeps`, etc.) или вынести AppBar-обёртки в widget-модуль и оставить только screen-фабрики.

- **[Монолитный WordDao — 40+ методов].**
  Languages, Words, Lexemes, Quiz, Statistics — всё в одном DAO.
  Нужно: разбить на `LanguageDao`, `WordDao`, `LexemeDao`, `QuizDao`, `StatisticDao`.

- **[O(n*m) в Mate — эффекты через все хендлеры].**
  Каждый эффект всё ещё прогоняется через ВСЕ хендлеры. После IS471 фильтрация через `MateTypedEffectHandler.filter()` убрала лишние reducer.reduce(state, Empty), но сам прогон через handler-список остался.
  Нужно: dispatch map по типу эффекта на стороне Mate перед вызовом handlers.

- **[@UnsafeVariance в MateEffectHandler].**
  Ломает type safety. Компилятор не проверяет тип эффекта для хендлера.
  Нужно: пересмотреть generic-дизайн интерфейса без `@UnsafeVariance`.

- **[Int/Long мисматч в TermApi и других API].**
  `TermApi.getTermList(dictionaryId: Int)`, `searchTermsPaging(dictionaryId: Int)` принимают Int, но dictionary id теперь Long.
  `.toInt()` в DictionaryTabUseCaseImpl и StatisticUseCaseImpl — потенциальное переполнение.
  Нужно: перевести все API методы на Long для dictionaryId.

- **[Извлечь бизнес-логику из Reducer'ов в UseCase].**
  Концепт: Reducer должен отражать преимущественно UI-логику (что показать, как реагировать на тап), а бизнес-правила (валидации, инварианты домена, последовательности операций) — жить в UseCase.
  Сейчас Reducer часто содержит и то, и другое — границы размыты.
  Нужно: проанализировать все Reducer'ы (dictionaryTab, dictionaryappbar, dictionary/form, quiz/chat, wordcard, settingstab, splash, stattab, quiztab), выделить куски бизнес-логики, вынести в соответствующие UseCase. Описать критерий «UI-логика vs бизнес-логика» в гайде `mate-framework.md` / `reducer-patterns.md`.

- **[UiEffect: убрать круг Effect → Msg → State, показывать snackbar/toast напрямую через UiHost].**

  **Текущее состояние.** `UiEffect` (например `ShowNotification`) обрабатывается через раунд:
  ```
  Reducer возвращает UiEffect.ShowNotification("text")
    → UiEffectHandler.onEffect()
    → consumer(UiMsg.ShowNotification(message, show = true))
    → Reducer обрабатывает UiMsg.ShowNotification
    → State.snackbarState.show = true, .title = "text"
    → Composable наблюдает State, показывает Snackbar
  ```
  То есть Effect → Handler → Msg → State change. State содержит `snackbarState` чисто как канал для one-shot уведомления.

  **Проблема.** State захламлён транзитивным UI-фидбеком (snackbar, future toast/vibration), которые по природе one-shot и не должны быть частью data state. Лишние Msg в шине. `MateTypedEffectHandler` контракт требует return Msg даже когда побочка fire-and-forget — приходится возвращать `Msg.NoOperation`.

  **Концепт.** Симметричный паттерн с Navigation: handler получает абстракцию-host через `@AssistedInject`, реализация живёт на стороне Composable (где есть `SnackbarHostState`, `Context`, `NavController`).

  ```kotlin
  interface UiHost {
      suspend fun showSnackbar(message: String)
      fun showToast(message: String)
      // future: vibration, system UI, etc.
  }

  class UiEffectHandler @AssistedInject constructor(
      @Assisted private val uiHost: UiHost,
  ) : MateTypedEffectHandler<Msg, UiEffect>() {

      override suspend fun onEffect(effect: UiEffect, consumer: (Msg) -> Unit) {
          when (effect) {
              is UiEffect.ShowNotification -> uiHost.showSnackbar(effect.message)
          }
          consumer(Msg.NoOperation)
      }

      @AssistedFactory
      interface Factory {
          fun create(uiHost: UiHost): UiEffectHandler
      }
  }
  ```

  Реализация на стороне Compose:
  ```kotlin
  class UiHostImpl(
      private val snackbarHostState: SnackbarHostState,
      private val context: Context,
  ) : UiHost {
      override suspend fun showSnackbar(message: String) {
          snackbarHostState.showSnackbar(message)
      }
      override fun showToast(message: String) {
          Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      }
  }

  // В Composable экрана:
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current
  val uiHost = remember { UiHostImpl(snackbarHostState, context) }
  val handler = remember { factory.create(uiHost) }
  ```

  Поток после рефакторинга:
  ```
  Reducer возвращает UiEffect.ShowNotification("text")
    → UiEffectHandler.uiHost.showSnackbar("text")
    → snackbarHostState.showSnackbar() напрямую
    → Snackbar появился, State не тронут
  ```

  **Что упрощается:**
  - `State.snackbarState` уходит для one-shot (можно оставить если кто-то хочет долгоживущий)
  - `UiMsg.ShowNotification` уходит — handler не возвращает в шину
  - Reducer остаётся чистым: знает «нужно показать text», не знает про `SnackbarHostState`/Compose
  - Полная симметрия с Navigation: `NavigationEffect.X → Navigator.x()` и `UiEffect.X → UiHost.x()`
  - Решает централизованно «Snackbar pattern fragile» (см. State Management section)

  **Возражение / цена:**
  - Side-channel обходит TEA single-source-of-truth для transient UI-событий. State перестаёт быть полным описанием экрана. Это **осознанная цена** для one-shot фидбека.
  - **Lifecycle:** реализация UiHost держит ссылку на `SnackbarHostState` который recreates на config change / process death. При process death snackbar не восстановится — у Channel/SharedFlow та же проблема. Для «удалено / сохранено» норм. Для критичных уведомлений (например «нельзя удалить — обязательная зависимость») — оставлять в State.
  - AssistedInject уже работает для Navigator, тот же паттерн → инфраструктура есть.

  **Скоуп:**
  - Создать `UiHost` interface в `core/mate`
  - Создать базовую `UiHostImpl` или per-screen реализации
  - Переписать `UiEffectHandler` под `@AssistedInject` (один файл в `dictionaryTab`, проверить другие модули с UiEffect)
  - Удалить `UiMsg.ShowNotification` (если не используется иначе) — проверить все экраны
  - Решить судьбу `State.snackbarState` per-screen (можно удалить если только для one-shot)
  - Обновить Composable экранов: `remember { factory.create(uiHost) }`
  - Описать паттерн в гайде `mate-framework.md`

  **Связано:**
  - State Management → «Snackbar pattern fragile» (закрывается этим)
  - Critical Bugs → «Централизованная система ошибок и снекбаров» (UiHost становится тем самым централизованным API)

---

## State Management

- **[Snackbar pattern fragile].**
  `show: Boolean` + `title: String` — temporal coupling. Может показаться старый title.
  Нужно: sealed class `SnackbarState { Hidden, Showing(id, message) }`.

- **[TextValueState vs EditableTextState — дублирование].**
  Два идентичных паттерна с разными именами (`origin/edited` vs `text/editedText`).
  Нужно: унифицировать в один тип, использовать во всех модулях.

- **[TextValueState.edited зависит от origin].**
  `val edited: String = origin` — default parameter зависит от другого. Неочевидное поведение при copy().
  Нужно: сделать `edited: String = ""` явно.

- **[TextValueState — атомарные extension'ы enableEdit / disableEdit].**
  В IS479 reducer'ы делали ручной `state.copy(translation = translation.copy(edited = origin, isEdit = true))` — забыли копировать в одной ветке (`enableLexemeTranslationEdit` баг). Нужно: добавить extension `TextValueState.enableEdit()` (атомарно `copy(edited = origin, isEdit = true)`) и `disableEdit()`. Reducer'ы вызывают только их, ручной `copy()` запрещён (R-RP-003 в `reducer-patterns.md`). Файл: `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/mate/State.kt`.

- **[SampleDb / HintDb columns — миграция на snake_case].**
  Сейчас `samples.lexemeId` и `hints.lexemeId` — camelCase (нет `@ColumnInfo`, Room взял Kotlin-имя). `write_quiz.lexeme_id` — snake_case. Из-за этого в `LexemeDbEntity` `@Relation` смешивает `entityColumn = "lexemeId"` (samples) и `entityColumn = "lexeme_id"` (для будущих component_values). Нужно: добавить `@ColumnInfo(name = "lexeme_id")` в SampleDb / HintDb + миграция переименования колонок (в SQLite < 3.25 RENAME COLUMN не работает → recreate-таблицы). Правило R-N-002 в `docs/handbook/guides/naming.md`. Отдельная фича — не объединять с IS481.

- **[Некосистентные аннотации @Stable/@Immutable].**
  WordCard — `@Stable`, Chat — `@Immutable`, CreateDictionary — без аннотаций. 9 классов без маркировки.
  Нужно: договориться на один подход (`@Immutable` для всех data class стейтов), пройти по всем модулям.

- **[termListMap растёт без лимита].**
  DictionaryTabState добавляет Flow на каждый поисковый паттерн. Memory leak при активном поиске.
  Нужно: ограничить размер map или очищать при смене паттерна.

- **[MateFlowHandler.job — public var].**
  Можно перезаписать без отмены предыдущего job. Subscription leak.
  Нужно: сделать private set или список job'ов.

- **[Универсальные логи на уровне Mate — редьюсер логируется по дефолту].**
  Идея (2026-07-03, из расследования багов IS481): дебаг рантайм-поведения TEA-цикла сейчас слепой — какие Msg пришли, какой State получился, какие Effect улетели, видно только точечными принтами.
  Нужно: встроить в `Mate` универсальное логирование по дефолту (Msg → до/после State (diff) → Set<Effect>), через уже имеющийся `LexemeLogger`, с возможностью отключения/фильтрации по модулю.

---

## Тестирование — конвенция

- **[Конвенция тестов extension-функций].**
  Один тест-файл = одна extension-функция (`<FunctionName>ExtTest.kt`).
  Doc-комментарий класса: полный список тест-кейсов.
  Каждая тестовая функция: комментарий UI-логики, бизнес-логики (если есть), конкретного тест-кейса.

## Тестирование — каркас миграций

- **[`getFromDatabase()` — копипаста на каждый Schemable].**
  50-80 строк boilerplate на версию. Нужно: generic cursor→map парсер.

- **[`checkMatcher` — нет exhaustiveness].**
  Новое поле — компилятор молчит. Нужно: assertEquals на entity или генерировать matcher.

- **[Schemable.data() использует актуальные entity].**
  При переименовании полей ломаются старые Schemable. Нужно: отдельный data class на каждую версию.

- **[Дублирование интерфейсов в Schemable.kt и Schema.kt].**
  Два набора одинаковых интерфейсов. Нужно: убрать дубли.

- **[Schema.kt — god object, 400+ строк].**
  Часть вынесена в schemable/, часть нет. Нужно: вынести всё.

- **[afterCreateCheck дублирует afterMigrationCheck].**
  Нужно: извлечь helper verifySchemable().

- **[Нет теста на удаление данных].**
  Нужно: негативные проверки — дропнутая колонка не существует.

- **[Date — хак с погрешностью 1000ms].**
  isEqualTo() с погрешностью, даты закомментированы в checkMatcher. Нужно: починить.

## Тестирование

- **[18+ модулей без тестов].**
  Только placeholder `ExampleUnitTest.kt`. Реальные тесты есть у VocabularyTab и DB-миграций.
  Нужно: покрыть reducer'ы всех TEA-модулей (минимум 8 screen-модулей).

- **[Ноль тестов на эффект-хендлеры].**
  8+ реализаций DatasourceEffectHandler полностью не покрыты.
  Нужно: тесты с моками UseCase для каждого хендлера.

- **[Ноль Compose UI тестов].**
  При тяжёлом использовании Compose — ни одного ComposeTestRule теста.
  Нужно: добавить smoke-тесты хотя бы для критических экранов.

---

## Tech Debt

- **[IS486: typed-отказы при создании компонента].**
  Ревью спеки (2026-07-21) указало: спека §5/§7.1 обещает `CreateOutcome.InvalidTarget`
  (мёртвая/чужая цель, core при не-лексеме) и `MultiForbiddenForChoice` при создании —
  в коде веток нет, `checkSetup()` в домене есть, но не подключён к create-пути.
  Почему не сделано сейчас: UI собрать невалидное не даёт, defensive-валидация data-слоя — отдельный скоуп.
  Нужно: подключить `checkSetup()` в `createUserDefinedComponent` + ветки outcome + тесты.

- **[IS486: LabelEmpty для CRUD опций].**
  Ревью спеки (2026-07-21): спека §5 фиксирует `LabelEmpty` в outcome опций —
  валидации пустого лейбла в data нет (UI фильтрует пустые черновики до отправки).
  Почему не сделано сейчас: практический риск нулевой, та же категория defensive-валидации.
  Нужно: trim-валидация в `addComponentOption`/`renameComponentOption` + ветка outcome.

- **[40 TODO в кодовой базе].**
  Включая баг с именем БД (данные теряются), неработающий convention plugin, неоптимальная загрузка Flow (#377).
  Нужно: разобрать каждый TODO — или починить, или создать задачу, или удалить.

- **[@Deprecated("Outdated") код всё ещё используется].**
  `WriteQuizStep`, цвета в theme.
  Нужно: заменить или удалить deprecated код.

- **[@SuppressLint("CheckResult") на 6+ классах].**
  RxJava observable errors молча игнорируются в legacy feature-модулях.
  Нужно: добавить error handling или удалить legacy код (если feature/ мёртв — просто удалить).

- **[java.util.Date вместо java.time].**
  Deprecated API по всей кодовой базе.
  Нужно: мигрировать на `java.time.LocalDate` / `Instant` (minSdk 26+ или desugaring).

---

## После публикации

- **[Восстановить "Оцените нас" в настройках].**
  `RateWidget` скрыт из `SettingsTabScreen.kt`. После публикации в Google Play — вернуть в секцию и добавить intent на страницу приложения в маркете.

- **[Восстановить "Поделитесь приложением" в настройках].**
  `AppShareWidget` скрыт из `SettingsTabScreen.kt`. После публикации — вернуть в секцию, обновить текст share с ссылкой на маркет.

---

## ВекторныйПиздеж

- **[IS486: `ComponentTypeRef.UserDefined(name)` — миграция name-based refs на id-based].**
  Ревью-агент (совместимость IS486) указал: quiz_configs хранят refs по имени компонента, а зависимости/опции IS486 — по числовым id. Два параллельных адресных пространства: rename дешёвый в id-мире требует cascade в name-мире; фильтрация квизов по enabled/degraded вынуждена резолвить имя → тип на каждую сборку квиза.
  Почему не сделано сейчас: out-of-scope IS486 — трогает формат хранения quiz_configs и cascade rename, отдельная миграция.
  Нужно: единая функция резолва name→ComponentType при сборке квиза (в рамках IS486), затем отдельным брифом — миграция refs на id.

- **[IS481: seed built-in типов не выполняется на destructive-fallback пути].**
  Расследование BUG-1 (docs/features/IS481_bugs/bugs.md) показало: seed `translation` висит только на `Callback.onCreate`, а Room после destructive-пересоздания зовёт `onDestructiveMigration`+`onOpen`, но НЕ `onCreate` (Room 2.8.4, `RoomConnectionManager.onMigrate`) → после fallback приложение остаётся без built-in типа навсегда.
  Почему не сделано сейчас: путь недостижим в проде (v13 существовала только на dev-девайсе; fallback рассчитан на pre-0.1.0 internal сборки) — решение юзера: не баг, чинится переустановкой.
  Нужно: перенести seed в `Callback.onOpen` (идемпотентный `INSERT OR IGNORE`, UNIQUE на `system_key` есть) — самовосстановление на любом пути открытия БД; починить лживый комментарий в `RoomModule.onDestructiveMigration`.

- **[IS481 wordcard_components: `origin` lossy для не-текстовых компонентов].**
  Ревью-агент (итоговое ревью IS481) указал: `ComponentValue.toComponentValueState()` берёт `origin = data.asText().orEmpty()` → для любого не-`TextValues` (image и т.п.) origin = `""`. Если такой saved-компонент откроют в edit и закоммитят пустым, `commitDecision` вернёт `LocalRemove` (origin пуст) вместо `PessimisticRemove` → компонент исчезнет из UI без эффекта `RemoveComponentValue`, оставшись в БД.
  Почему не сделано сейчас: out-of-scope — IS481 работает только с TEXT-шаблонами (ChipsRow фильтрует `template == TEXT`), не-текст недостижим.
  Нужно: при вводе не-текстовых компонентов сделать `origin`/`commitDecision` template-aware (не сводить значение к тексту), либо хранить origin как `TemplateValues`, а не `String`.

- **[IS481 wordcard_components: resubscribe-гонка emit в AvailableComponentTypesFlowHandler].**
  Ревью-агент указал: `runEffect` делает `job?.cancel()` без `join` перед relaunch → старый flow (Room) может эмитнуть устаревший `ComponentTypesLoaded` уже после нового. Для одного `dictionaryId` безвредно (идемпотентный set в reducer); при смене dictId старые типы могут на миг перетереть новые.
  Почему не сделано сейчас: dictionaryId на карточке стабилен → реально не воспроизводится; правка требует suspend-cancel-join в runEffect.
  Нужно: при подтверждённой потребности — `job?.cancelAndJoin()` (сделать runEffect честно ждущим отмены) или фильтровать эмиссии по актуальному dictId.

- **[IS481 wordcard_components: пробелы тест-покрытия (reducer-ветки без тестов)].**
  Ревью-агент (тест-аудит) указал на непокрытые ветки/сценарии: `RestoreLexemeFailed` (теперь retry-снек — поведение изменено фиксом, нужен reducer-тест), S-trap-1 (chained сбой промоута NOT_IN_DB), S-trap-4, S16 (независимость лексем при таргетном Msg), позитив `OpenDeleteLexemeDialog`/`CloseDeleteLexemeDialog`, `CreateComponentValue(lexemeId=NOT_IN_DB)` (баг был пойман только ревью, не тестом).
  Почему не сделано сейчас: существующие тесты — неизменяемый контракт; дописывание новых — отдельная задача.
  Нужно: добавить недостающие reducer/scenario-тесты (новые, не трогая существующие) на перечисленные ветки.

- **[IS481 wordcard_components: flush-on-back loader без watchdog/таймаута].**
  Оркестратор (итоговое ревью) указал: сброс `isExiting` завязан только на приход `OperationFailed`/успешный refresh. Если DB-операция зависнет или Msg потеряется — пользователь заперт под блокирующим спиннером, даже «назад» не выйдет.
  Почему не сделано сейчас: out-of-scope — отдельный механизм надёжности.
  Нужно: watchdog-таймаут на flush (N сек → принудительный `OperationFailed`/выход), либо escape-жест из loader-оверлея.

- **[IS481 wordcard_components: гонка `isCommitting` (B1 двойное создание / B3 потеря ввода) — ПРОВЕРЕНО ТРЕЙСОМ (2026-06-29): не воспроизводится, correlation-id не нужен].**

  Изначально adversarial-ревью предлагало correlation-id против двух гонок в окне сохранения. Трейс по **финальному** коду показал, что оба бага уже закрыты реализацией:

  - **B3 (потеря ввода соседа) — недостижим.** Открыть на правку существующее поле = `EnterComponentValueEditMode`, guarded при `isPendingDbOp` (09 A20) → пока A летит, B не открыть. Новое поле = pristine; `reduceRefreshLexemeComponents` закрывает edit **только** у `isCommitting` и сохраняет pristine в `pristineTail` → `edited` соседа цел.
  - **B1 (двойное создание лексемы) — недостижим.** `commitDraftLexeme` первой строкой `if (any { isCommitting }) return lexeme` → повторный `CreateComponentValue` во время летящего `CreateLexeme` второй create не эмитит; anchor всегда первый `Update`.

  **Остаточный нюанс (F4, осознанно оставлен):** `OperationFailed` снимает `isCommitting` у **всех** при batch-реэмите survivor'ов (`LexemeDraftPromoted` → несколько параллельных `AddValue`). Данные НЕ теряются (возвраты матчатся по `componentValueId`/`pristineKey` независимо от маркера), страдает лишь точность flush-трекинга при batch-ошибке (выход отменяется — что при ошибке и нужно).

  **Correlation-id НЕ нужен.** Единственный оставшийся триггер — «несколько одновременно открытых редакторов» — исключён продуктом (однозначно не будет; инвариант «один активный редактор» через `commitAndCloseAllEdits`). **Реанимировать только если этот инвариант изменится** (появится мульти-редактор) — тогда correlation-id + guard обязательны как фундамент.

- **[Глобально: проверить ВСЕ кнопки и нажатия на «дятлинг» (быстрые повторные тапы / double-tap).**

  **Контекст.** Гонка в IS481 wordcard_components (см. соседний пункт про `isCommitting`) показала: быстрые повторные нажатия в окно асинхронной операции способны вызвать дубли / потерю данных. Это не локальная проблема одного экрана — паттерн «тапнул дважды пока летит эффект» применим ко всем actionable-элементам приложения (создание/удаление/сохранение/навигация).

  **Что сделать.** Пройтись по всем экранам и виджетам и проверить кликабельные элементы (кнопки, chips, FAB, пункты списков, диалоговые actions) на устойчивость к «дятлингу»:
  - двойной тап по «создать/добавить» → не создаёт два объекта;
  - быстрый повторный тап по «удалить» → не двойное удаление / не краш;
  - повторный тап по навигации → не открывает экран дважды (double-push);
  - тап по элементу, пока летит его async-операция → игнорируется или блокируется.

  **Подход.** Рассмотреть единый механизм защиты, а не точечные флаги: debounce/throttle на clickable (напр. общий `Modifier.clickableDebounced` / `clickableOnce`), либо disable-on-pending на уровне состояния. Зафиксировать правило в гайде (`ui-patterns.md`). Точечные `isPendingDbOp`-гарды в reducer'ах оставить как defense-in-depth.

  **Почему важно.** Дешёвая системная защита закрывает целый класс гонок (дубли, double-navigation, повторные сетевые/DB-операции) разом, вместо отлова каждого по отдельности.

- **[IS481 WordCard: унифицировать пусто-vs-пробелы в onFocusLost].**
  `ComponentValueField.onFocusLost` проверяет `value.isEmpty()` (без trim) → пусто и пробелы идут РАЗНЫМИ Msg-маршрутами (`RemoveComponentValueRequested` vs `CommitComponentValueEdit`→`commitDecision` с trim), хотя исход один (удаление). Лишняя поверхность для рассинхрона.
  Почему не сейчас: правка UI-слоя, обе ветки рабочие и покрыты тестами врозь.
  Нужно: в `onFocusLost` проверять `value.isBlank()` вместо `isEmpty()` → один маршрут (`onRemove`), `commitDecision` остаётся defense-in-depth.

- **[IS481 phase 2: template-immutability gate в UseCase].**
  В фиче phase 2 редактирование компонента — проверка «нельзя менять шаблон существующего типа» живёт только в БД (`CoreDbApiImpl.editComponentType:582`). По концепту F017 + business contract — должна быть на UseCase уровне (быстрый отказ без обращения к data API) + страховка в БД (defense-in-depth). Сейчас один уровень вместо двух — каждый changed-template submit делает лишний DB round-trip.
  Нужно: добавить в `ComponentsManagerUseCaseImpl.editComponent` lookup current type (через новый `LexemeApi.getComponentTypeById` либо через `flowAllUserDefined` snapshot), сравнить `template != current.template` → вернуть `EditOutcome.TemplateImmutable` без вызова data API. Парный test `whenSubmitEditWithChangedTemplate_thenTemplateImmutable_andDataApiNotCalled` (verify-no-interactions on lexemeApi.editComponentType).

- **[IS481 phase 2: порядок защит в editComponentType inconsistent vs rename/softDelete].**
  В `core/core-db-impl/.../CoreDbApiImpl.kt:573-574` (edit) — порядок `BuiltInProtected → Removed`. В `:532-533` (rename) и `:690-691` (softDelete) — обратный порядок `Removed → BuiltInProtected`. Sibling CRUD-методы на один тип ведут себя по-разному: для built-in + soft-deleted типа edit вернёт «Built-in», rename вернёт «Removed». UI покажет разные snackbar для одной реальности.
  Нужно: swap порядок в `editComponentType` (Removed первым). Исправить ложь в `data_summary.md:13,27` — там утверждается parity, которой нет.

- **[IS481 phase 2: sibling CRUD-методы не унифицированы по политике Removed vs BuiltInProtected].**
  Концепт пункта 4 brief'а — разделить «удалённый» и «встроенный» как отдельные состояния и обрабатывать одинаково во всех CRUD-операциях над компонентом (rename / edit / softDelete). По факту edit-метод даёт другой ответ чем rename/softDelete на одинаковую ситуацию (см. предыдущий пункт). Юзер увидит «Built-in» при попытке edit и «Removed» при попытке rename того же типа — разные snackbar'ы на одну реальность нарушают concept промиса единообразия.
  Нужно: после фикса предыдущего пункта — добавить контракт-тест который для каждой пары `(state ∈ {built-in, removed, normal}) × (operation ∈ {rename, edit, softDelete})` проверяет ожидаемый outcome. Парный тест-кейс выявит будущие регрессии sibling non-uniform.

- **[IS481 phase 2: логи миграции M12→M13 — заглушки «ok» вместо реальных counters].**
  Brief пункт 5 явно требует логи per-step миграции с количеством affected rows (`renameComponentTypesRemoveDate: N rows`, `rewriteTextJson: N rows updated`, и т.д.) — чтобы при manual smoke было видно что именно отработало в каждом из 9 шагов. По факту все 9 шагов (`Migration_012_to_013.kt:55-91`) пишут одну и ту же заглушку `"M12→M13 step N <name>: ok"`. Если миграция сломается или отработает частично — невозможно понять на каком шаге сколько строк прошло. Diagnostic value лога = ноль.
  Нужно: вытаскивать `affected_rows` через `SELECT changes()` после каждого UPDATE/DELETE-шага и подставлять в Log.d. Для DDL-шагов (CREATE INDEX / DROP INDEX / ADD COLUMN) логировать факт выполнения без count'а — но не одинаковым «ok».

- **[IS481 phase 2: добавить TODO/комментарии для 4 dead-code мест под будущие фичи].**
  4 куска зарезервированного "на будущее" кода без production-callers: (1) `LexemeApiImpl.addLexemeWithTranslation` + `updateLexemeTranslation` deprecated shims; (2) `WordCardUseCase.addComponentValue`/`updateComponentValue`/`deleteComponentValue` generic methods; (3) `modules/domain/lexeme/Field.kt` + `PrimitiveType.kt` + `ComponentTemplate.fields` под композитные шаблоны; (4) `Primitive.Color` вариант под "цвет как компонент". Не удалять — оставляем под будущие фичи. Нужно добавить ко всем 4 точкам понятные `// TODO(reserved, IS-XXX): used by <планируемая фича> — composite templates / color components / generic write-path` комментарии чтобы будущие разработчики понимали зачем эти типы здесь и не удаляли по YAGNI.

- **[IS481 phase 2: гонка в `editComponentType` — cardinality downgrade SELECT вне `withTransaction`].**
  Файл: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt:588-622`.
  Сейчас flow метода: (1) на line 589 — `componentValueDao.findLexemesWithMultipleValuesForType(typeId): List<Long>` (SQL SELECT по `component_values` чтобы найти лексемы где для данного типа > 1 значения, для блокировки downgrade `isMulti: true → false`); (2) проверка `if (impacted.isNotEmpty()) return EditComponentOutcome.CardinalityDowngradeBlocked(impacted)` — line ~595; (3) на line 615 — `database.withTransaction { ... }`; (4) на line 616 — `componentTypeDao.update(typeId, name, template, isMulti = false, updatedAt = now)`.
  Шаги 1 и 2 происходят **вне** транзакции. Между моментом «получили пустой impacted list» и моментом «UPDATE компонента на isMulti=false» в другом потоке может произойти `INSERT INTO component_values (..., component_type_id = :typeId, lexeme_id = X)` — добавление второго значения для лексемы X с этим типом. После UPDATE: тип помечен `isMulti=false`, но в БД для лексемы X остаются 2 active row в `component_values` с этим `component_type_id` → инвариант single-cardinality нарушен. Следующий `insertSingleSafe` (для другой операции) бросит `IllegalStateException` → краш в UI.
  Вероятность: окно гонки ~миллисекунды (только время между SELECT и BEGIN TRANSACTION). Реальный trigger — юзер в Manager жмёт «Edit + выключить multi» одновременно с тем что в WordCard у другой лексемы добавляет второе значение этого типа. Low likelihood, но non-zero, и last falling is critical (uncaught exception → краш UI).
  Нужно: перенести `findLexemesWithMultipleValuesForType` **внутрь** `database.withTransaction { ... }` блока. Room транзакция изолирует от concurrent writes на уровне БД lock'ов → SELECT внутри транзакции видит snapshot который не может измениться до commit'а. Структурно: внутри `withTransaction { val impacted = ...; if (impacted.isNotEmpty()) return@editComponentType ...; componentTypeDao.update(...) }`. Парный test: spy на DAO + concurrent insert в другом thread → assert `IllegalStateException` НЕ бросается / outcome = `CardinalityDowngradeBlocked`.

- **[IS481 phase 2: гонка в `softDeleteComponentType` — `previewDeletionImpact` вне `withTransaction`, неполный `affectedPrefs`].**
  Файл: `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt:685-720`.
  Сейчас flow метода: (1) на line 693 — `val impact = previewDeletionImpact(typeId)` (внутри: SELECT по `quiz_configs` чтобы найти конфиги ссылающиеся на тип, агрегация по `dictionaryId` для `affectedPrefs: List<Long>` — список словарей у которых надо сбросить picker prefs); (2) на line 698 — `database.withTransaction { ... }`; (3) внутри транзакции — мягкое удаление типа + cascade очистка `component_refs` в `quiz_configs` + установка `removed_at`.
  Шаги 1 и 2 вне транзакции. Между ними другой поток может выполнить `INSERT INTO quiz_configs (..., component_refs LIKE '%' || :typeRef || '%', dictionary_id = NEW_DICT)` — добавить новый конфиг ссылающийся на удаляемый тип в словаре `NEW_DICT` которого не было в исходном snapshot. После транзакции: cascade очистит ссылку в этом новом конфиге (потому что её `WHERE component_refs LIKE '%' || :typeRef || '%'` тоже срабатывает на новый row), **НО** `affectedPrefs` не содержит `NEW_DICT` → `resetQuizPickerPrefsBestEffort` не сбросит prefs для `NEW_DICT` → у юзера в picker'е `quiz_picker_dict_NEW_DICT` остаётся stale ref на soft-deleted тип. При следующем рендеринге picker'а — ссылка либо отрендерится «компонентом-призраком», либо отфильтруется (не отображается, но remains в prefs) → юзер видит inconsistency между picker selection и реальными доступными компонентами.
  Вероятность: ниже чем в предыдущем пункте (требует одновременной soft-delete + создания quiz_config в новом словаре с этим типом). Не critical (stale pref, не invariant violation), но визуальное отклонение возможно.
  Нужно: перенести `previewDeletionImpact(typeId)` или хотя бы расчёт `affectedConfigs` **внутрь** `database.withTransaction { ... }`. Альтернатива: после транзакции пересобрать `affectedPrefs` из реальных удалённых row в `quiz_configs` (через `SELECT changes()` или возвращаемое значение cascade update). Парный test для concurrent scenario.

- **[IS481 phase 2: `EditDialogState` / `EditNameError` / `ImpactedLexemesPreview` дублируются один-в-один между двумя screen-mate].**
  Те же 3 sealed/data class'а описаны в обоих модулях: `modules/screen/components_manager/.../mate/State.kt:101-145` + `modules/screen/per_dictionary_components/.../mate/State.kt:108-137`. Identical поля, identical sealed-варианты, identical KDoc «Parity с Manager-вариантом — структурно дублируется». Также Reducer Edit-ветки (~160 строк) дублируются — Manager отличается только наличием `Msg.DictionariesLoaded` + `Msg.CreateDictionaryToggle` + clear `selectedDictionaryIds` при Global.
  Параллельно: 14 общих UI-виджетов (диалоги, строки, FAB) вынесены в shared `:modules:widget:component_widgets`, а соответствующее MATE state осталось дублированным per-screen. Логически парные вещи разнесены.
  Нужно: вынести `EditDialogState` / `EditNameError` / `ImpactedLexemesPreview` (плюс возможно `RenameDialogState` / `DeleteConfirmState` / `SnackbarState` — общие для Create/Rename/Delete) в новый shared module — `:modules:domain:lexeme` (domain не зависит от Android, но `@Stable` требует compose runtime → возможно нужен отдельный `:modules:domain:components-mate-types` с compose-runtime dep). Альтернатива — accept дублирование как осознанный choice и зафиксировать в spec. Сейчас — minor, не блокер.

- **[IS481 phase 2: preview лексем в downgrade-блоке показывает технические ID вместо слов].**
  В downgrade preview (когда юзер пытается выключить «multi» у компонента с problematic лексемами) показывается список 3 проблемных лексем. По задумке там должны быть реальные слова которые юзер ввёл (`apple`, `book`, `cat`). По факту — технический placeholder `Lexeme #1, Lexeme #2, Lexeme #3` через ресурс `R.string.components_edit_lexeme_label` = `"Lexeme #%1$d"`. Preview становится бесполезным — юзер не понимает у каких именно слов сломает значение.
  Нужно: добавить UseCase-метод который по `List<Long>` lexemeId возвращает названия. Маппить в Reducer'е или UseCaseImpl'е при формировании `ImpactedLexemesPreview` (либо передавать список display-DTO `{id, label}` через outcome). Заменить string ресурс на просто `%1$s` для слова. Подумать про fallback если слово удалено / null.

- **[IS481 phase 2: feature-tag `###ComponentConstructor###` не дублируется в success-путях public UseCase методов].**
  Brief пункт 5 — double-tag pattern (module-tag для debug + feature-tag для smoke) реализован только в `prefs reset` и в каскадах `cascadeRename`/`cascadeSoftDelete`. Public UseCase методы `createUserDefinedComponent` / `renameComponent` / `editComponent` / `softDeleteComponent` в `ComponentsManagerUseCaseImpl` логируют успех только под `LogTags.COMPONENTS_MANAGER` (module-tag). Через `adb logcat | grep '###ComponentConstructor###'` юзер при smoke НЕ увидит главные user-actions фичи — только их побочные эффекты (prefs reset, cascade). Это убивает основной use-case feature-тега.
  Нужно: в каждом success-пути этих 4 методов добавить параллельный `logger.d(FeatureLogTags.COMPONENT_CONSTRUCTOR, ...)` с тем же message. Аналогично в `PerDictionaryComponentsUseCaseImpl` для его методов.

- **[Room RANDOM + @Relation].**
  После обновления Room с 2.6.1 до 2.8.4 — проверить запросы с RANDOM() + LIMIT + @Relation.
  Ранее Room 2.7.1 давал IllegalStateException (https://issuetracker.google.com/issues/413924560).
  Нужно: прогнать квиз-выборку и убедиться что рандомные запросы работают корректно.

- **[ForgeFlow base: защита от зацикливания в reviewer-механике `reviews:` + `trigger_step_rerun`].**
  В новой reviewer-механике (`reviews: <target_step>` + `changes_requested` → `trigger_step_rerun`) защиты от бесконечного rerun нет. Старый `execute_repeat` имел `max` параметр; `trigger_step_rerun` просто сбрасывает шаг в `pending` без счётчика. Теоретически пара «контракт ↔ reviewer» может крутиться неограниченно если reviewer стабильно ставит `changes_requested`.
  Нужно: добавить в `step.feedback_iteration` верхний лимит (например `max_feedback_iterations: 7` в frontmatter промпта или дефолт в runner), при превышении — `escalate` к пользователю. Затрагивает `~/dev/forgeflow/spec/runner.md → trigger_step_rerun`.

- **[WordCard F074: data-loss `NOT_IN_DB`-буфера при `RemoveLexeme` через menu другой лексемы].**
  Пользователь создал NOT_IN_DB лексему, ввёл text в `translation.edited`, не закоммитил. Открыл menu другой реальной лексемы → Delete. После `RemoveLexemeEffect → RefreshLexemeList` merge выкидывает `NOT_IN_DB` целиком вместе с typed text без подтверждения.
  Нужно: добавить guard «`isCreatingLexeme=true ⇒ блокировать RemoveLexeme других лексем» или ConfirmDialog при `RemoveLexeme(NOT_IN_DB)`. UX-тикет.

- **[WordCard F054: undefined ordering Room `@Relation`].**
  `termApi.getTermById(wordId).lexemeList` возвращает лексемы через Room `@Relation` без `ORDER BY` — порядок Room-determined, нестабилен. После `RemoveLexeme` оставшиеся лексемы могут визуально переупорядочиться.
  Нужно: добавить явный `@Query getLexemesByTermId ORDER BY addDate ASC` в `LexemeDao` либо в API. Data-слой, отдельный тикет.

- **[WordCard F049: diagnostic-бедность snackbar при failure операциях].**
  `Msg.ShowNotification(text)` несёт только локализованный текст («Не удалось сохранить перевод» / «Не удалось создать лексему»), без structured reason — `IllegalStateException("Dictionary not found")` и реальный БД-сбой неразличимы. UI не может предложить retry с правильной семантикой.
  Нужно: переработать `ShowNotification` в sealed result с reason-полем + UX-обогащение (retry, copy stack trace). Отдельная error-handling фича.

- **[WordCard: lens-extract Translation/Definition reducer-веток].**
  10 пар почти зеркальных функций (`CommitTranslationEdit`/`CommitDefinitionEdit`, `RefreshTranslation`/`RefreshDefinition`, `CreateTranslation`/`CreateDefinition`, `EnterTranslationEditMode`/`EnterDefinitionEditMode`, `CancelTranslationEdit`/`CancelDefinitionEdit`, `RemoveTranslation`/`RemoveDefinition`). Различаются только полем `LexemeState.translation` vs `LexemeState.definition`. Зеркальный код дублирован ~50 LOC на пару.
  Нужно: lens-pattern `LexemeLens<TextValueState?>` либо переименовать Msg в `UpsertLexeme*(lexemeId, kind: SubentityKind, ...)`. Большой архитектурный refactor контракта Msg, отдельный тикет.

- **[WordCard UI: cancel-trigger для word edit отсутствует].**
  `Msg.ExitWordEditMode` в контракте жив (UX «тап Отмена / back-кнопка внутри edit-mode»), но `LexemeEditableText` имеет только `onCloseEditMode` → `CommitWordChanges`. Cancel-кнопки нет, back закрывает экран целиком.
  Нужно: добавить cancel-control в `LexemeEditableText` либо обоснованно зафиксировать отсутствие cancel UX. Точечный UX-тикет.

- **[WordCard UI: loading overlay при `isLoading=true`].**
  При cold load (`isLoading=true && wordState is NotLoaded`) UI рендерит пустой Scaffold без `CircularProgressIndicator`. Figma `9154-82509` явно показывает spinner-state. Сейчас пользователь видит чистый экран на ~100-300ms.
  Нужно: добавить `CircularProgressIndicator`-overlay при `state.isLoading && wordState is NotLoaded`. Точечный UX-тикет.

- **[Auto-format Kotlin: vertical multi-параметровые вызовы].**

  **Цель.** При `Cmd+Option+L` (Reformat Code) Android Studio должна приводить multi-параметровые вызовы / функции к vertical-формату:
  ```
  PaddingValues(
      start = 12.dp,
      top = 6.dp,
      end = 6.dp,
      bottom = 6.dp,
  )
  ```
  Inline `PaddingValues(start = 12.dp, top = 6.dp, end = 6.dp, bottom = 6.dp)` не приемлем.

  **Что пробовали (не сработало):**
  1. `.editorconfig` в корне проекта с `ij_kotlin_call_parameters_wrap = on_every_item`, `*_new_line_after_left_paren = true`, `*_right_paren_on_new_line = true`. После рестарта Studio reformat не сработал.
  2. `.idea/codeStyles/Project.xml` — прописаны опции `CALL_PARAMETERS_WRAP = 5` (затем `6`) + `LPAREN_ON_NEXT_LINE = true` + `RPAREN_ON_NEXT_LINE = true` в `JetCodeStyleSettings` и `codeStyleSettings language="kotlin"`. Reformat всё равно не приводит к vertical.
  3. Убран `CODE_STYLE_DEFAULTS = KOTLIN_OFFICIAL` — не помогло.

  **Гипотезы что проверить:**
  - Settings UI → Editor → Code Style → Kotlin → Wrapping and Braces → Function call arguments — посмотреть какое значение реально стоит (если `Wrap if long` — Project.xml не активирован → проверить scheme).
  - Возможно нужен `ktlint` plugin (gradle) — он форматирует через свои правила, IDE через тот же scheme.
  - Возможна проблема с тем что **`Cmd+Option+L`** в Studio не запускает full kotlin formatter, а только мелкие правки. Тогда нужен **right-click → Reformat Code...** с опциями.
  - Проверить — может быть в Studio есть отдельная категория "Compose / Kotlin DSL" где другой override.

  **Артефакты:** `.editorconfig` и `.idea/codeStyles/Project.xml` — текущее состояние правок, не работает. Удалить или дополнить под рабочее решение.

- **[IS481 followup: atomicity rollback androidTest для F013/F015].**

  **Контекст.** IS481 main спланировал unit-тесты F013 (`addLexemeWithBuiltInComponent` FK violation → rollback `lexemes` + `write_quiz`) и F015 (`addDictionary` + `insertDefaultQuizConfig` FK violation / corrupt JSON → rollback `dictionaries` row). Реализованы только mockk unit-тесты на обработку exception, что **не** проверяет реальный rollback SQLite-транзакции.

  **Что сделать:** добавить `core/core-db-impl/src/androidTest/java/.../WordDaoAtomicityTest.kt` с 2 кейсами через `MigrationTestHelper(driver = BundledSQLiteDriver(), ...)` либо in-memory Room на v12:
  - **F013:** `addLexemeWithComponents` с FK violation на `component_type_id` (несуществующий type id) → assert `lexemes` + `write_quiz` rows count до операции == count после.
  - **F015:** `WordDao.addDictionary` с симулированной ошибкой на `insertDefaultQuizConfig` (corrupt JSON или FK violation на dictionary_id) → assert `dictionaries` row не создан, F1 invariant держится.

  **Risk без теста:** регрессия IS479 F1 (атомарность `INSERT lexeme + write_quiz`) на runtime FK violation — высокий impact, любая правка `addLexemeWithComponents` / `addDictionary` может незаметно сломать инвариант.

  **Триггер:** при следующем заходе в `core-db-impl` (next data feature / bugfix), либо если регрессия проявится в проде.

  **Источник:** IS481 global_code_review.md § Major #1.

- **[IS481 followup: `updateComponentValue` / `deleteComponentValue` honest return через DAO lookup].**

  **Контекст.** В `app/.../WordCardUseCaseImpl.kt` методы `updateComponentValue(componentValueId, data): Lexeme?` и `deleteComponentValue(componentValueId): RemoveComponentResult?` на success path **возвращают null** — потому что нет DAO метода `getLexemeIdByComponentValueId(componentValueId)`, а без `lexemeId` загрузить `Lexeme` обратно невозможно. Foot-gun для будущих callers: signature обещает `Lexeme?`, на самом деле null = «успех или провал — не понять».

  **Что сделать:**
  - Добавить в `ComponentValueDao` метод `@Query("SELECT lexeme_id FROM component_values WHERE id = :id") suspend fun getLexemeIdByComponentValueId(id: Long): Long?`.
  - Прокинуть через `LexemeApi`: `suspend fun getLexemeIdByComponentValueId(id: ComponentValueId): Long?`.
  - В `WordCardUseCaseImpl.updateComponentValue` / `deleteComponentValue` после успешного DAO call — lookup `lexemeId` → `lexemeApi.getLexemeById(lexemeId)?.toDomain()` → вернуть честный `Lexeme?` / `RemoveComponentResult.ComponentRemoved`.

  **Сейчас не баг** — никто эти 2 метода не вызывает в IS481 (definition flow через lexemeId-based path, translation через shim).

  **Триггер:** когда появится UI configurator для component values (backlog «Quiz config UX» либо direct component editing) — callers нужны будут honest return для UI feedback (snackbar success/error).

  **Источник:** IS481 global_code_review.md § Major #2.

- **[Migrate `modules/widget/iconDropDowned/` → `modules/core/ui/dropdown/` с `Lexeme*` префиксом (design-system unification)].**

  **Контекст.** В рамках IS481 quiz_component_picker зафиксирована widget naming convention:
  - **Tier 1** — design-system primitives в `core/ui` + `core/theme` с `Lexeme*` префиксом (атомарные блоки, без бизнес-логики).
  - **Tier 2** — специализированные межфичевые widget'ы (один widget = один модуль `modules/widget/<name>/`), внутри композирует Tier 1, имеет бизнес-семантику.
  - **Tier 3** — feature wrappers с domain naming.

  **Расхождение.** `modules/widget/iconDropDowned/` содержит `MenuItem`/`DividerMenuItem`/`IconDropdownWidget`/`Entity`/`StringSource`/`IconSource` — все без бизнес-логики, чистые UI primitives. По convention это **Tier 1**. Но физически живёт в `modules/widget/` (= место Tier 2) и без `Lexeme*` префикса. Mismatch.

  **Что сделать (одной фичей-задачей):**

  1. Создать `modules/core/ui/src/main/java/me/apomazkin/ui/dropdown/` (subdir по аналогии с `btn/` / `dialog/` / `input/`).
  2. Перенести файлы из `modules/widget/iconDropDowned/src/main/java/me/apomazkin/icondropdowned/` в новую location.
  3. Сменить package: `me.apomazkin.icondropdowned` → `me.apomazkin.ui.dropdown` во всех файлах.
  4. Префиксовать public API `Lexeme*`:
     - `IconDropdownWidget` → `LexemeIconDropdownWidget`
     - `DividerMenuItem` → `LexemeDividerMenuItem`
     - `MenuItem` builders (`WithCheckbox` / `WithIcon` / `TextOnly`) — если public composables → `LexemeMenuItemWithCheckbox` / `LexemeMenuItemWithIcon` / `LexemeMenuItemTextOnly`.
     - `Entity` / `StringSource` / `IconSource` — public data types → `LexemeMenuItemEntity` / `LexemeStringSource` / `LexemeIconSource` (либо подходящее имя).
  5. Обновить **все callsite'ы** (на момент записи в backlog — ~31 reference в ~10 файлах: `ActionsWidget.kt`, `MistakesMenuItem.kt`/`DebugMenuItem.kt`/`EarliestReviewedMenuItem.kt` в quiz/chat, `DeleteWordMenuItem.kt` в wordcard, `DictDropDownWidget.kt`/`AddDictMenuWidget.kt` в dictionarypicker + gradle файлы).
  6. Обновить gradle deps:
     - Удалить entry для `modules:widget:iconDropDowned` из `settings.gradle.kts`.
     - Заменить `implementation(project(":modules:widget:iconDropDowned"))` на `implementation(project(":modules:core:ui"))` (если `core:ui` уже подключён — просто удалить iconDropDowned).
  7. Удалить весь `modules/widget/iconDropDowned/` directory.
  8. Build verify: `:modules:core:ui:compileDebugKotlin`, `:modules:screen:quiz:chat:compileDebugKotlin`, `:modules:screen:wordcard:compileDebugKotlin`, `:modules:widget:dictionarypicker:compileDebugKotlin`, `:app:compileDebugKotlin` — все EXIT 0.

  **Когда:** до либо после IS481 quiz_component_picker фичи (она вводит **новые** Tier 1 primitives `LexemeSubmenuMenuItem` / `LexemeRadioMenuItem` сразу в `core/ui/dropdown/`, не трогая `iconDropDowned/`).

  **Триггер:** при следующем заходе в `widget/iconDropDowned/` либо «чисто пора убрать tech debt» — миграция одна задача, обходит весь связанный код.

  **Источник:** IS481 quiz_component_picker design-фаза, разговор про widget naming convention.

- **[Консолидация encoding `ComponentTypeRef`: единый кодек вместо двух (JSON + prefs string)].**

  **Контекст.** Сейчас две независимые serialization одной sealed-иерархии `ComponentTypeRef`:
  - `core/core-db-impl/.../mapper/ComponentTypeRefJson.kt` — JSON discriminator `type` (для `quiz_configs.component_refs` в DB).
  - `app/.../QuizChatUseCaseImpl.encodeRef`/`decodeRef` — custom string `"builtin:<key>"` / `"user:<name>"` (для picker prefs).

  Любое будущее изменение sealed (новый variant, payload) нужно синхронизировать в двух местах. Edge case'ы (unknown key, malformed) тестируются отдельно для каждого формата.

  **Что сделать:** вынести единый кодек в `modules/domain/lexeme/.../ComponentTypeRefCodec.kt` либо подобное. Public API: `fun ComponentTypeRef.encode(): String` + `fun String.decodeComponentTypeRef(): ComponentTypeRef?`. Использовать тот же string format (либо JSON, либо новый). Обновить callsite'ы в `ComponentTypeRefJson` (для лист — `joinToString` over codec) и `QuizChatUseCaseImpl`.

  **Триггер:** новый variant `ComponentTypeRef` (например `BuiltIn` с param'ами) либо «пора убрать duplicate».

  **Источник:** IS481 quiz_component_picker senior review § F2.

- **[`BuiltInComponent.titleResId` + `quizHeaderResId` — display resources на enum-entries].**

  **Контекст.** Сейчас UI'i решают как отрендерить built-in component:
  - `modules/screen/quiz/chat/.../widget/appbar/menu/ComponentChoiceItem.kt` — `when (ref.key) { TRANSLATION -> stringResource(...) }` exhaustive. Compile-fail при новом built-in (loud).
  - `modules/screen/quiz/chat/.../quiz/QuizGameImpl.toQuizItem` — `is ComponentTypeRef.BuiltIn -> R.string.chat_quiz_ask_translation_header` молча покажет неправильный header (silent bug при новом built-in).

  Несимметрия — UI loud-fail, quiz silent-fail. По AGG-1 «codebase добавляет новые built-in (transcription, pronunciation, ...)» — это сценарий не далёкий.

  **Что сделать:** добавить `@StringRes val titleResId: Int` (для menu picker title) + `@StringRes val quizHeaderResId: Int` (для quiz session header) прямо на entries `BuiltInComponent`. Forking точка одна — добавление variant'а enum форсит указание обоих resource'ов.

  **Проблема:** `BuiltInComponent` живёт в `modules/domain/lexeme` (pure-JVM, без Android-deps). `@StringRes` Int — это Android. Solution либо:
  - (a) Просто `Int` без `@StringRes` annotation в domain (потеряем lint, но сохраним pure-JVM).
  - (b) Вынести «display mapping» в отдельный Android-aware модуль (e.g. `core/core-resources/.../BuiltInComponentDisplay.kt`), где declared `fun BuiltInComponent.titleResId(): Int`. Domain остаётся pure. UI use Display extension.
  - **Рекомендую (b)** — domain покрытие, UI extension через ресурсы.

  **Триггер:** добавление второго built-in component (PRONUNCIATION / TRANSCRIPTION / etc).

  **Источник:** IS481 quiz_component_picker senior review § F3.

- **[`LoadQuizComponentTypes` effect — убрать (DataStore Flow initial-emit покрывает)].**

  **Контекст.** В IS481 quiz_component_picker есть два пути загрузки `availableTypes` на entry в chat:
  - One-shot `DatasourceEffect.LoadQuizComponentTypes` через `Msg.PrepareToStart`.
  - `QuizPickerFlowHandler.subscribe` initial DataStore emission.

  Оба пути вызывают `getAvailableTypes` + `getQuizPickerSelection` + emit `Msg.QuizComponentTypesLoaded`. Reducer `resolveSelection` idempotent — state не дрожит, но 2x I/O при каждом cold start.

  **Что сделать:** убрать `DatasourceEffect.LoadQuizComponentTypes` + соответствующий branch в `ChatReducer.PrepareToStart`. Полагаться только на FlowHandler initial-emit. Это упростит code path. Тест `PrepareToStart emits LoadQuizComponentTypes effect` нужно переделать на subscribe assertion в FlowHandler (или удалить, если он лишний).

  **Триггер:** при следующем заходе в quiz/chat module либо «пора убрать дубль».

  **Источник:** IS481 quiz_component_picker senior review § F4.

- **[Per-dictionary disable built-in компонентов].**

  **Контекст.** В IS481 component_constructor built-in компоненты (сейчас `translation`, в будущем — `transcription`/`pronunciation`/etc) — **read-only глобально** и присутствуют в каждом словаре. Юзер не может их «отключить» для конкретного словаря. Сценарий: словарь идиом / афоризмов где нужны только `definition` + `source`, но `translation` всё равно появляется в quiz picker и редакторе лексемы.

  **Что сделать:**
  - Новая таблица `dictionary_builtin_disabled(dictionary_id, built_in_key, disabled_at)` либо колонка / extension таблицы `component_types` для built-in row'ов.
  - DAO фильтр: при запросе available components для словаря — исключать built-in перечисленные в disable-list.
  - UI: в настройках словаря (либо в per-dictionary view конструктора) — список built-in компонентов с toggle «использовать в этом словаре».
  - Side effect для quiz_configs: при отключении built-in — убрать его из `quiz_configs.component_refs` соответствующего словаря.

  **Зависит:** IS481 component_constructor merged. Не блокирует другие фичи (read-only built-in — рабочее поведение по умолчанию).

  **Триггер:** когда юзер захочет словарь без определённого built-in (или появится второй built-in компонент кроме translation).

- **[Recovery UI + background TTL hard-delete для soft-deleted записей].**

  **Контекст.** В IS481 component_constructor реализован только soft-delete механизм (помечает `removed_at`, скрывает из активных queries). Без UI восстановления, без background TTL — записи лежат в БД бесконечно, юзер не может их вернуть и не может ускорить hard-delete. См. `docs/features/IS481_component_constructor/deletion_concept.md`.

  **Что сделать (одной фичей — recovery и TTL связаны):**

  1. **UI корзина / архив** — отдельный экран «Удалённые компоненты» (внутри конструктора, кнопка «Удалённые», или в settings tab). На карточке: имя, словарь, сколько values, дней до hard-delete, кнопка «Восстановить» + кнопка «Удалить навсегда сейчас».
  2. **Recovery flow** — очистить `removed_at` у component_type, связанные values становятся видны через JOIN-фильтр (если на этом этапе уже live). `quiz_configs.component_refs` НЕ восстанавливается автоматически (юзер сам включает компонент обратно в квиз). **Conflict при recovery** (имя занято): диалог переименования с default suggestion типа `name (2)`.
  3. **Background TTL job** (WorkManager / на старте app) — `DELETE FROM <table> WHERE removed_at IS NOT NULL AND removed_at < (now - TTL)`. Cascade на дочерние таблицы. Logging — сколько rows почистили.
  4. **TTL value** — выбрать (30/14/60 дней), хардкод либо параметризуемо через settings UI.
  5. **Manual hard-delete** — кнопка «Удалить навсегда сейчас» в корзине (ускоренный hard-delete без ожидания TTL).
  6. **Convention для всех soft-delete таблиц** (`component_types`, `component_values` сейчас + `dictionaries` / `words` / `lexemes` после соответствующей backlog-фичи).

  **Зависит:** IS481 component_constructor merged. После реализации других soft-delete фич — расширить корзину/TTL на их таблицы.

  **Триггер:** когда юзеру понадобится восстановить случайно удалённое, либо когда БД растёт от мёртвых rows.

- **[Soft-delete + recovery для `dictionaries` / `words` / `lexemes`].**

  **Контекст.** В IS481 component_constructor реализуется soft-delete + TTL recovery (см. `docs/features/IS481_component_constructor/deletion_concept.md`) — но только для `component_types` / `component_values` (это нужно конструктору). Остальные таблицы с накопленными данными остались на hard-delete.

  **Что сделать (одной фичей либо тремя последовательно):**
  - **`dictionaries`** — добавить `removed_at` (Unix ms, nullable), DAO `softDelete` метод, фильтр `WHERE removed_at IS NULL` во все active queries, UI корзина с recovery, TTL background cleanup. Огромная боль при случайном удалении словаря — главный triggering use-case.
  - **`words`** — то же. Сейчас есть короткий Undo snackbar (~4-5 сек); TTL даёт второй уровень защиты для забывчивых.
  - **`lexemes`** — то же. **Особо опасно:** auto-cascade lexeme при удалении последнего component (`remaining == 0` ветка в `WordCardUseCaseImpl.deleteLexemeComponentBy`) — soft-delete защищает от непреднамеренной потери всей лексемы при удалении одного компонента.
  - **Доп. мотивация (IS481 WordCard components):** undo удаления лексемы = повторный INSERT (`restoreLexemeWithComponents`), при сбое INSERT данные теряются безвозвратно — в фиче это обойдено retry-снеком (A17), но soft-delete (`removed_at=null` на undo) делает восстановление idempotent и убирает обход. Также flush-on-back (автосейв при «назад») гарантированно доедет только при живучем scope / soft-delete — иначе best-effort. Оба момента закрываются этим пунктом. Выявлено при проектировании user-сценариев IS481 (A17 — единственный безвозвратный error-путь).

  Каждая таблица — `removed_at` колонка + DAO `softDelete(id, now)` + cascade-через-JOIN для дочерних таблиц (по аналогии с component_types → component_values из M13).

  **Не входит:**
  - `samples` / `hints` — dead branches, отложено до миграции в template компоненты (отдельная задача).
  - `quiz_configs` — derived, defaults регенерируются, soft-delete не нужен.
  - `write_quiz` — append-only log, soft-delete не нужен.

  **Зависит:** IS481 component_constructor (M13 + recovery UI patterns) merged. Эта фича переиспользует уже наработанные паттерны (recovery screen, TTL background job, convention `WHERE removed_at IS NULL` в DAO).

  **Триггер:** после merge IS481 component_constructor.

- **[M14: repository-wide timestamps rename — convention `created_at` / `updated_at`].**

  **Контекст.** В IS481 component_constructor (M13) применена convention timestamps `created_at` / `updated_at` / `removed_at` (Unix ms, см. `docs/handbook/guides/data-layer.md` Конвенции #9) только для таблиц непосредственно затронутых фичей (`component_types` — добавили `created_at` + `updated_at`, переименовали `remove_date` → `removed_at`; `component_values` — добавили все три). Остальные таблицы (`lexemes`, `terms`, `dictionaries`, `samples`, `hints` и т.д.) остались с legacy неконвенциональными именами (`addDate`, `modifiedDate` и подобные).

  **Что сделать в M14:**
  - Аудит всех `*Db.kt` в `core-db-impl/entity/` — составить список таблиц + колонок которые надо переименовать.
  - Для каждой таблицы: rename existing timestamp-колонок (`addDate` → `created_at`, `modifiedDate` → `updated_at`, etc) с сохранением данных. Если колонки нет — добавить со значением даты миграции для existing rows.
  - Каскадные изменения: Kotlin domain (field rename в data classes) + Room `@ColumnInfo` + все `@Query` raw SQL (literal'ы старых имён скрытно не компилируются — runtime crash, нужен аудит вручную) + callsite'ы в reducers/mappers/тестах.
  - Migration test с реальной БД-fixture от M13.

  **Почему отдельной миграцией от M13:** [F-N3 inquisitor finding] — это convention refactor, не блокирующий конструктор. Composite JSON rewrite (M13) — рискованная часть с destructive fallback. Не смешивать с rename операциями в 5+ таблицах которые могут стрельнуть из-за пропущенного `@Query` SQL литерала.

  **Триггер:** когда захочется применить convention repository-wide. Не блокирует другие фичи (legacy имена работают через старые `@Query`).

- **[Component constructor: autocomplete уникальных значений поля компонента].**

  **Контекст.** После реализации конструктора компонентов (`docs/features/IS481_component_constructor/`) пользователь может создавать composite-templates типа `quote_with_source` с полем `source: Text`. Реальный кейс: пользователь хранит несколько цитат из одного источника (учебник, статья, видео) — каждый раз вбивать имя источника заново неудобно и приводит к опечаткам / дубликатам.

  **Что сделать.** При фокусе / вводе в поле компонента (любого `Text` примитива) — показывать выпадающий список уникальных существующих значений этого поля с поиском по подстроке. Тап по подсказке → подставляет в input.

  **Реализация (UI-only, БД-модель не трогается):**
  1. **DAO метод** `getDistinctFieldValues(componentTypeId, fieldName): List<String>` через `SELECT DISTINCT json_extract(value, '$.fields.<fieldName>.value') FROM component_values WHERE component_type_id = ? AND removed_at IS NULL`.
  2. **UseCase** — тонкая обёртка над DAO.
  3. **UI** — Material 3 `ExposedDropdownMenuBox` или custom dropdown с TextField + LazyColumn suggestions, фильтрация по input.

  **Когда:** после реализации конструктора компонентов + хотя бы одного composite-template со свободным текстовым полем (`source`, `author`, etc).

  **Цена производительности.** DISTINCT по `json_extract` на больших объёмах (10k+ values) деградирует. Для текущих объёмов (десятки-сотни цитат per словарь) — пренебрежимо. Если станет проблемой — materialized index таблица либо SQLite JSON-индекс.

  **Не блокер для MVP конструктора.** Можно внедрить любой отдельной фичей после merge IS481.

- **[IS481 followup: `LexemeSubmenuMenuItem` — cascaded popup вместо inline accordion].**

  **Контекст.** В IS481 quiz_component_picker Tier 1 primitive `LexemeSubmenuMenuItem` (`modules/core/ui/src/main/java/me/apomazkin/ui/dropdown/LexemeSubmenuMenuItem.kt`) реализован как **inline accordion**: header `DropdownMenuItem` + при тапе разворачивается `Column` под ним, прямо внутри родительского `DropdownMenu`. Это работает, но не соответствует Material design — пользователь ожидал каскадное подменю (отдельный popup рядом с родительским меню).

  **Что сделать:** переписать Tier 1 primitive на cascaded popup. Два пути:
  - **Saket cascade lib** (`me.saket.cascade:cascade-compose:2.3.0`, [github.com/saket/cascade](https://github.com/saket/cascade)) — drop-in `CascadeDropdownMenu` + `DropdownMenuItem(text, children = { ... })`. Анимация раскрытия и back-кнопка из коробки. Минус — одна dependency.
  - **Manual nested `DropdownMenu`** — два popup + state + offset через `onGloballyPositioned` родительского item'а. Без зависимостей, но позиционирование «pixel-perfect рядом» муторно.

  **Скоуп изменения:** только Tier 1 primitive `LexemeSubmenuMenuItem` (sig остаётся `title` + content slot). Callers (`QuizComponentMenuItem` + future) не меняются. Существующий аккордеон-стиль `Column` под header'ом — удаляется.

  **Триггер:** когда появится второй submenu в `core/ui/dropdown/` (накопится критическая масса nested menus), либо UX-полировка перед публикацией.

  **Источник:** IS481 quiz_component_picker manual smoke 2026-06-11 — user expectation mismatch (accordion vs cascaded popup).

- **[Tooling: `cc-src.sh` — автоматический lookup library исходников по FQN class name].**

  **Контекст.** F11 из IS481 FlowBacklog: sub-agent делает предположения про library API (Compose, Room, DataStore, Material) без чтения исходника. Лечится правилом «любое решение про library API подтверждается ссылкой на конкретный файл», но без автоматизации = постоянный manual поиск sources.jar в `~/.gradle/caches/`.

  **Что сделать (минимум):** скрипт `./scripts/cc-src.sh <fully.qualified.ClassName>` который:
  - Находит matching `*-sources.jar` в `~/.gradle/caches/modules-2/files-2.1/`.
  - Извлекает `.kt` / `.java` файл по path (`<package>/<Class>.{kt,java}`) в `/tmp/cc-src/<artifact>/`.
  - Печатает абсолютный путь к распакованному файлу (один или несколько кандидатов если есть).
  - EXIT=0 если найдено хотя бы одно, EXIT=1 если не найдено.

  **Что сделать (расширение, позже):**
  - Кэширование: распаковывать sources jar один раз, переиспользовать.
  - Резолв по короткому имени (`DropdownMenu` без package) через индекс.
  - Поддержка transitive deps (не только direct project deps).
  - Интеграция с `./gradlew dependencies` для понимания какой artifact owner класса.

  **Триггер:** когда снова случится F11-ситуация в flow (sub-agent сделал предположение про library API → пользователь поправил). Тогда обкатать тулинг прямо в боевой фиче.

  **Зачем:** даёт sub-agent'у автономный путь «не угадывать API, посмотреть». Без вопросов к пользователю «можно ли распаковать jar».

- **[State.disableUserInput() — инвертированный флаг, pre-existing bug].**

  **Контекст.** `modules/screen/quiz/chat/.../logic/State.kt:238-242` — `disableUserInput()` устанавливает `isUserInputEnable = true` с `//TODO` пометкой. Семантически противоположно имени метода. Pre-existing, IS481 quiz_component_picker только подсветил при touch'е файла.

  **Что сделать:** заменить на `isUserInputEnable = false`. Проверить callsite'ы — все 6 вызовов в `ChatReducer` ожидают reset инпута.

  **Триггер:** при следующем заходе в state-логику либо «пора убрать TODO».

  **Источник:** IS481 quiz_component_picker senior review § F5.

---

## Очень блять хуевая пизда

- **[IS481 phase 2: route `per_dict_components/{dictionaryId}` не открывается после delete+create словаря в той же сессии приложения].**

  Полная история бага: `docs/features/IS481_component_constructor_phase2/bugs/nav_stuck_after_dict_recreate.md`.

  **Симптомы:** юзер удаляет все словари → создаёт новый → тапает «Компоненты» в AppBar → окно не открывается. После рестарта приложения работает.

  **Root cause (подтверждено deep dive 10 sub-агентов):** `androidx.navigation 2.9.0` хранит saved state в `NavControllerImpl.backStackStates: Map<String, ArrayDeque<NavBackStackEntryState>>` keyed по `destination.id` (hash от route TEMPLATE, не от concrete args). `BottomBarWidget.kt:77-83` использует tab-switch с `popUpTo(saveState=true) + restoreState=true` — каждое переключение tab'а сохраняет stale entry под ключом template'а `per_dict_components/{dictionaryId}`. При повторном `navigate("per_dict_components/8")` Compose Nav в `NavControllerImpl.kt:1197` видит `shouldRestoreState()=true && backStackMap.containsKey(node.id)=true` → вызывает `restoreStateInternal` который восстанавливает entry со старым `args.dictionaryId=5` ВМЕСТО push'а fresh → composable lambda не invokes → экран не рендерится.

  `clearBackStack(routeTemplate)` крашит NPE в `NavControllerImpl.restoreStateInternal:1319` потому что попытка восстановить state с corrupted bundle — это сам по себе доказывает что saved state Map содержит broken entry.

  **Что НЕ помогло (проверено в коде, см. bug doc):**
  - `launchSingleTop=false`.
  - `popUpTo(startDest) { saveState=false }; restoreState=false` в Vocabulary.kt `goToPerDictionaryComponents`.
  - `popBackStack(route, inclusive=true, saveState=false)` перед navigate.
  - `clearBackStack(route)` — crash.

  **Что помогло частично:** `BottomBarWidget.kt:79 saveState=true → false` починил `goToComponentsManager` (route без template-аргумента), НО ломает UX tab-switch state preservation (юзер хочет сохранять state) → откатил.

  **Решения которые предложили sub-агенты (по приоритету):**
  1. **Bus-bridge "dict changed → clearBackStack"** — singleton `MutableSharedFlow<Unit>`, эмиттит `DictionaryListEffectHandler` после delete/add, `MainScreen` ловит через `LaunchedEffect` и зовёт `runCatching { navController.clearBackStack(route) }`. Targeted, ~4 файла, не ломает UX. **REJECTED юзером** как «хуйня».
  2. **Bump `navigation-compose 2.9.0 → 2.9.8`** — 2.9.1/2.9.2/2.9.3/2.9.5/2.9.6/2.9.8 содержат фиксы saved-state restoration, single-top lifecycle (b/421095236), entry-not-resuming (b/418746335). Может auto-fix целый класс. 1 строка в `deps/compose.versions.toml:7`. Низкий риск миграции внутри 2.9.x.
  3. **Type-safe `@Serializable` routes** (стабильно с 2.8.0) — `@Serializable data class PerDictComponents(val dictionaryId: Long)` — убирает целый класс template-collision bugs без миграции либы. ~2 файла.
  4. **Architectural refactor: убрать `{dictionaryId}` arg** — route становится plain `per_dict_components`, dictionaryId читается из shared `DictionaryAppBarViewModel.currentDict.id`. 10-15 шагов, medium effort, см. bug doc раздел «Что попробовать ещё» #5. Долгосрочно правильно.
  5. **`key(dictionariesVersion) { rememberNavController() }`** — пересоздавать NavController при изменении набора словарей. Side effect: backstack теряется при ЛЮБОМ delete/add dictionary — ломает UX (юзер: «может я в принципе не хочу чтобы стейт сбрасывался»). REJECTED юзером.
  6. **Reflection hack** в `NavControllerImpl.savedStates` Map чтобы удалить запись напрямую. Хрупко, ломается при апгрейде либы. NOT recommended.
  7. **Миграция на Voyager/Decompose** — другая навигационная либа без template collision. Large effort 1-4 недели, меняет один баг на другой. NOT recommended для IS481.

  **Эффект на пользователя сейчас:** нужно закрывать и заново открывать приложение после удаления+создания словаря, чтобы открыть Компоненты на новом словаре. Manager (через Settings) работает, потому что у него route без template — bug проявляется только на route с аргументом.

  **Не блокирует MVP** — workaround через restart есть. Но раздражает в testing/onboarding.

  **Diagnostic-логи остались в коде** после расследования — нужно снести когда будет fix или решить что забить:
  - `app/.../navigator/SettingsNavigatorImpl.kt` — `Log.d` (против гайда `docs/handbook/guides/logging.md`).
  - `app/.../navigator/DictionaryAppBarNavigatorImpl.kt` — `Log.d`.
  - `app/.../uiDeps/CompositionRootImpl.kt` — `logger.log` в ComponentsManager/PerDict ScreenDep (`[diag]` метки).
  - `modules/screen/main/.../Vocabulary.kt` — `Log.d` + `popBackStack` workaround.
  - `modules/screen/main/.../Settings.kt` — `Log.d` + `popBackStack` workaround.
  - `modules/screen/settingstab/.../SettingsNavigationEffectHandler.kt` — `logger: LexemeLogger` ctor + `[diag]` logs.
  - `modules/widget/dictionaryappbar/.../DictionaryAppBarNavigationEffectHandler.kt` — `Log.d`.
  - `modules/screen/components_manager/.../ComponentsManagerViewModel.kt` — `init { logger.log("[diag] VM INIT ...") }` + `.also { ... }`.
  - `modules/screen/per_dictionary_components/.../PerDictionaryComponentsViewModel.kt` — то же.
  - `modules/screen/components_manager/.../mate/AllUserDefinedTypesFlowHandler.kt` — `[diag]` `subscribe()`/`launch{}`/`EMIT` логи.
  - `modules/screen/per_dictionary_components/.../mate/ComponentsForDictionaryFlowHandler.kt` — то же.

  **Trigger:** при возврате к этой фиче (или когда юзер устанет от workaround). Сначала попробовать опцию 2 (bump library) — самый дешёвый shot. Если не сработает — опция 3 (type-safe routes) или 4 (route refactor).
