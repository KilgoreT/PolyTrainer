# Backlog

---

## Срочное

- **[Миграция существующих спек в `docs/features-spec/` под новый формат contract_spec].**

  **Контекст.** В рамках декомпозиции шага `contract` Business sub-flow (см. `docs/features/FORGEFLOW_contract_design.md`) выход контрактного блока теперь = спека в `docs/features-spec/<feature>.md`. Структура новой спеки:
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

  **Что есть сейчас.** Существующие спеки в `docs/features-spec/` (по `README.md` директории — `dictionary-list.md`, `dictionary-create.md`, `dictionary-appbar.md`, `wordcard.md`, и др.) в **старом формате**: «срез текущего состояния, констатация бизнес-логики и кейсов, БЕЗ разбивки по TEA-слоям».

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
  - и др. — полный список взять из `docs/features-spec/README.md`

  **Когда:**
  - **После** того как создан step file `contract_spec.md` в `docs/forgeflow-overlay/steps/` и первый прогон новой decomposition прошёл на новой фиче (validation формата)
  - **До** запуска адаптивного flow на существующих фичах (иначе первый запуск столкнётся с разнобоем форматов)

  **Подход к выполнению:**
  - Не делать одним PR — миграция спек 1-к-1 за итерацию (один PR на спеку)
  - Каждый PR содержит: миграция спеки + (если обнаружены расхождения кода/спеки) issue в `Tech Debt`
  - В commit message ссылка на это backlog-задание

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
  Нужно: переписать wordcard mate на generic — `LexemeState.components: List<ComponentValueState>`, generic `Msg.CreateComponent(typeId)` / `Msg.UpdateComponentInput(componentId, value)` etc. Объём ~400+ строк + ~10 reducer-тестов с нуля. Тот же refactor — в quiz/chat и dictionaryTab. После — выпиливаем `@Deprecated` обёртки. Триггер: появление UI для user-defined компонентов.

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
  В IS479 правило зафиксировано в `docs/guides/data-layer.md` (раздел "Нормализация текстового ввода (trim)") и применено в `WordCardUseCaseImpl` (4 точки: updateWord, addLexemeTranslation, addLexemeDefinition, restoreLexeme).
  Нужно: пройтись по остальным `*UseCaseImpl` в `app/src/main/java/me/apomazkin/polytrainer/di/module/` и добавить `.trim()` перед передачей строки в `CoreDbApi` (или в сеть, если появится). Затронуты: `DictionaryUseCaseImpl`, `DictionaryTabUseCaseImpl`, `SettingsTabUseCaseImpl`, `QuizTabUseCaseImpl`, `QuizChatUseCaseImpl`, `SplashUseCaseImpl`, `StatisticUseCaseImpl`, `DictionaryAppBarUseCaseImpl` — пересмотреть write-методы со String на входе.
  Если для конкретного поля trim семантически вреден — задокументировать исключение комментарием у метода.

- **[Прогнать правило "виджеты получают callbacks, не sendMessage" по остальным модулям].**
  В IS479 правило зафиксировано в `docs/guides/ui-patterns.md` (раздел "Виджеты получают callbacks, не `sendMessage`") и применено в `modules/screen/wordcard/`. Виджеты wordcard переписаны на плоские callbacks (5 виджетов, включая декомпозицию `LexemeItemWidget` на 12 параметров).
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
  Сейчас `samples.lexemeId` и `hints.lexemeId` — camelCase (нет `@ColumnInfo`, Room взял Kotlin-имя). `write_quiz.lexeme_id` — snake_case. Из-за этого в `LexemeDbEntity` `@Relation` смешивает `entityColumn = "lexemeId"` (samples) и `entityColumn = "lexeme_id"` (для будущих component_values). Нужно: добавить `@ColumnInfo(name = "lexeme_id")` в SampleDb / HintDb + миграция переименования колонок (в SQLite < 3.25 RENAME COLUMN не работает → recreate-таблицы). Правило R-N-002 в `docs/guides/naming.md`. Отдельная фича — не объединять с IS481.

- **[Некосистентные аннотации @Stable/@Immutable].**
  WordCard — `@Stable`, Chat — `@Immutable`, CreateDictionary — без аннотаций. 9 классов без маркировки.
  Нужно: договориться на один подход (`@Immutable` для всех data class стейтов), пройти по всем модулям.

- **[termListMap растёт без лимита].**
  DictionaryTabState добавляет Flow на каждый поисковый паттерн. Memory leak при активном поиске.
  Нужно: ограничить размер map или очищать при смене паттерна.

- **[MateFlowHandler.job — public var].**
  Можно перезаписать без отмены предыдущего job. Subscription leak.
  Нужно: сделать private set или список job'ов.

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
