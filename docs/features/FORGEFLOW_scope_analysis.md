# ForgeFlow — анализ scope для адаптивного flow

Документ для построения единого адаптивного flow «scope analysis → выбор шагов» в ForgeFlow. Основан на реальной структуре PolyTrainer (Lexeme) и истории 13 задач (IS441–IS476).

---

## 1. Архитектурные слои проекта

Lexeme — многомодульный Android-проект (Gradle), Kotlin + Compose, паттерн TEA (через собственный фреймворк `mate`), DI через Dagger 2.

### 1.0. Обобщённые слои (4 группы — основа для scope_analysis)

Рабочая модель для шага `scope_analysis`. Детализация в §1.1.

**1. 🎨 UI**
- Composables — только отображение и реакция на тап, без решений «что делать когда».
- *Где живёт:* `*/ui/` в screen/widget модулях

**2. 🧠 Бизнес-логика**
- TEA-логика (State / Msg / Reducer / Effect / EffectHandler / FlowHandler)
- UseCase (контракты в `deps/` и реализации в `app/di/module/`)
- Доменные сущности
- **Specs** — снимок поведения фичи
- Здесь принимаются все решения: валидации, инварианты, последовательности, переходы.
- *Где живёт:* `*/logic/`, `*/mate/`, `*/deps/`, `app/di/module/*/`, `docs/features-spec/*.md`

**3. 💾 Данные**
- DB (Room: api + impl + миграции + schemas)
- Prefs (DataStore)
- Library-обёртки над third-party источниками
- *Где живёт:* `core/core-db-*`, `modules/datasource/prefs/`, `modules/library/*`

**4. 🏗 Инфраструктура**
- DI (Dagger граф)
- Mate framework
- Logger + sinks
- Navigation infra (routes, Navigator impls)
- Core.ui / Core.theme / Core.tools (общие виджеты, тема, утилиты)
- Build (Gradle, ProGuard, BuildConfig)
- Manifest
- CI/CD
- **Guides** — снимок паттернов и правил подсистем
- *Где живёт:* `modules/core/*`, `app/di/`, `app/route/`, `app/navigator/`, `build-logic/`, `*/build.gradle.kts`, `.github/`, `app/proguard-rules.pro`, `AndroidManifest.xml`, `docs/guides/*.md`

---

**Критерий разделения UI ↔ Бизнес-логика:** содержит ли код **решения**. Composable читает `State` и рисует → UI. Код решает «что произойдёт когда» → бизнес-логика, **даже если живёт в `logic/` папке screen-модуля**.

**Почему Specs внутри Бизнес-логики, а не в отдельной «Документации»:** спека — артефакт поведения фичи. Если затронута спека — это явный сигнал входа в бизнес-логику. «Только-документационных» правок не делаем; если спека отстала — это долг, чинится вместе с кодом, который её рассинхронил. Аналогично Guides — артефакт паттерна/инфраструктуры.

**Связанный backlog-item** (`docs/Backlog.md` → Архитектура): «Извлечь бизнес-логику из Reducer'ов в UseCase». Концепт: Reducer должен отражать преимущественно UI-логику, бизнес-правила — в UseCase. **До разрешения этой задачи Reducer и UseCase лежат в одной группе** — границы размыты на практике.

### 1.1. Детализация слоёв → пути

**1. App / Composition root**
- Точка входа: Application, MainActivity, навигация верхнего уровня
- *Путь:* `app/src/main/java/me/apomazkin/polytrainer/` (`route/`, `navigation/`, `navigator/`, `uiDeps/`, `env/`)

**2. UseCase Impls (composition)**
- Реализации `XxxUseCase` для каждой фичи. Живут в `app`, собирают зависимости из разных слоёв.
- *Путь:* `app/.../polytrainer/di/module/<feature>/`
- *Тесты:* `app/src/test/.../di/module/<feature>/`

**3. DI graph (Dagger)**
- `AppComponent`, `RoomComponent`, `LoggerComponent`, модули, `@AssistedInject`-фабрики.
- *Путь:* `app/.../polytrainer/di/`, `modules/core/di/`

**4. Screen (TEA: UI + Logic + Deps)**
- Каждая «фичевая» экранная единица. *Корень:* `modules/screen/<screen>/src/main/java/me/apomazkin/<screen>/`
  - **4.1 UI (Compose)** — `.../<screen>/ui/` — `*Screen.kt` + UI-виджеты экрана
  - **4.2 Logic (TEA core)** — `.../<screen>/logic/` — `State`, `Message`, `Reducer`, `*Effect`, `*EffectHandler`, `*FlowHandler`
  - **4.3 Deps (контракт наружу)** — `.../<screen>/deps/` — `XxxUseCase` и `XxxNavigator` интерфейсы, доменные сущности. **Публичный контракт модуля.**
  - **4.4 Entity / tools** — `.../<screen>/entity/`, `.../<screen>/tools/` — UI-модели, маперы, утилиты

**5. Widget (TEA-виджет, переиспользуемый)**
- Структура аналогична screen, но не экран — встраиваемый виджет (AppBar, picker и т.п.).
- *Путь:* `modules/widget/<widget>/src/main/java/.../<widget>/` (`mate/`, `ui/`, `deps/`)

**6. Core: mate (TEA-фреймворк)**
- Базовые типы: `Mate`, `MateTypedEffectHandler`, `MateNavigationEffectHandler`, `NavigationEffect`, `MateFlowHandler`. Меняется редко — затрагивает всех.
- *Путь:* `modules/core/mate/`

**7. Core: ui (общие виджеты)**
- Общие Compose-виджеты: кнопки, `FlagPlaceholderWidget`, `ImageFlagWidget`, контейнеры.
- *Путь:* `modules/core/ui/`

**8. Core: theme**
- Цвета, типографика, MaterialTheme.
- *Путь:* `modules/core/theme/`

**9. Core: logger**
- `LexemeLogger` интерфейс + sink-паттерн (`LogcatSink`, `CrashlyticsSink`). Реализация — в `app/.../logger/`.
- *Путь:* `modules/core/logger/`

**10. Core: tools**
- Утилиты, kotlin extensions.
- *Путь:* `modules/core/tools/`

**11. Core: di (шаблоны)**
- Базовые правила DI (qualifier-аннотации, `CompositionRoot`).
- *Путь:* `modules/core/di/`

**12. Library (third-party интеграции)**
- Обёртки над внешними libs. Сейчас: `flags/` (blongho country_data).
- *Путь:* `modules/library/<lib>/`

**13. Datasource: prefs (DataStore)**
- `PrefsProvider` (DataStore + Flow), `PrefKey` enum. **Системный слой** — читают все экраны.
- *Путь:* `modules/datasource/prefs/src/main/java/me/apomazkin/prefs/`

**14. DB API (контракт)**
- Публичный API БД: `CoreDbApi`, доменные сущности (`DictionaryItem`, `Lexeme`, `Word`), Flow-методы.
- *Путь:* `core/core-db-api/src/main/java/`

**15. DB Impl (Room + миграции)**
- Room entities (`*Db`), DAOs, `RoomDatabase`, миграции, мапперы DB↔domain.
- *Путь:* `core/core-db-impl/src/main/java/`
- *Schemas:* `core/core-db-impl/schemas/`

**16. DB module wrapper**
- Промежуточный модуль связки api↔impl.
- *Путь:* `core/core-db/`

**17. Core resources**
- Общие `strings.xml`, drawable.
- *Путь:* `core/core-resources/`

**18. Build / Gradle / ProGuard**
- Convention plugins, версии, ProGuard, BuildConfig-параметры (LOG_LEVEL, REMOTE_LOG_LEVEL).
- *Путь:* `build-logic/`, `build-settings/`, `*/build.gradle.kts`, `app/proguard-rules.pro`, `gradle/libs.versions.toml`

**19. CI/CD**
- Pipelines (Lint → Test → Build APK). Триггеры `IS*`/`MT*`.
- *Путь:* `.github/workflows/`, `scripts/`, `distribution/`

**20. Specs (живая документация фичи)**
- Срез текущего состояния фичи: контракты, user-journey, инварианты. Меняется вместе с кодом.
- *Путь:* `docs/features-spec/*.md`

**21. Guides (правила архитектуры)**
- Гайды по подсистемам: mate-framework, dagger-di, prefs-datastore, testing-reducers, navigation. Меняются при изменении правил, не точечной фичи.
- *Путь:* `docs/guides/*.md`

**22. ForgeFlow artifacts (per-feature)**
- План, лог, артефакты шагов flow (00_task → 10_check).
- *Путь:* `docs/features/<TICKET>_<name>/`

**23. ForgeFlow framework**
- Сам фреймворк (steps, modules, BOOTSTRAP).
- *Путь:* `docs/forgeflow/`, `forgeflow.yml`

### 1.2. Системные / cross-cutting файлы

- **AndroidManifest.xml** (`app/src/main/AndroidManifest.xml`) — permissions, activities, intents
- **Backlog** (`docs/Backlog.md`) — единственная точка для новых идей
- **CLAUDE.md** — правила для LLM
- **Лог-крашей** (`docs/crashes/`) — артефакты с прод-крашей

---

## 2. Типовые комбинации слоёв по типам задач

Из реальных тикетов IS441–IS476.

### 2.1. Краш-фикс прода (простой)

**Тикеты:** IS461, IS472, IS474

**Примеры:**
- **IS461** quiz empty list — Screen.Logic (`QuizGameImpl` — guard + bounds check)
- **IS472** country_data ProGuard NPE — Build (proguard-rules.pro: keep classes для Gson reflection)
- **IS474** prefs key not found — Datasource.prefs (`PrefsProvider` — nullable Flow контракт) + Specs

**Профиль:** 1–2 слоя, изменение локальное, контракт чаще НЕ двигаем (IS474 — исключение, поправили контракт `getLongFlow → Flow<Long?>`).

### 2.2. Краш-фикс + рефактор контракта

**Тикеты:** IS463, IS476

**Примеры:**
- **IS463** Room @Relation NULL — DB.Impl (DAO query), DB.Api, Screen.Logic, Core.logger (extract module), DI
- **IS476** delete all dicts crash — Screen.deps (×3: dictionaryTab, dictionaryappbar, quiz/chat), App.UseCaseImpls (×3), Screen.Logic (Tab: State+Reducer+EffectHandler+UI), Widget.Logic (AppBar: Msg+State), Datasource.prefs (setLong nullable), Specs (×4)

**Профиль:** 5–8 слоёв, контракт `*UseCase` меняется, рябь по `mate`-логике, правка спек **обязательна**.

### 2.3. Реактивный баг (синхронизация состояния)

**Тикет:** IS445

**Пример:**
- **IS445** stale dict icon в AppBar — Widget (dictionaryappbar) — подписка на Flow вместо одноразовой загрузки; App.UseCaseImpl (источник Flow); возможно Screen (где триггерится изменение)

**Профиль:** 2–3 слоя, фокус на **подписочной модели** (Flow vs suspend). Контракт меняется (snapshot → Flow).

### 2.4. UI/UX полировка

**Тикеты:** IS453, IS443

**Примеры:**
- **IS453** WordCard input UX — Screen.ui (composable вёрстка + икoнка)
- **IS443** flag placeholder widget — Core.ui (вынос виджета), Screen.ui (3 места использования), модульные зависимости (build.gradle.kts)

**Профиль:** только `ui/` + (опц.) `core/ui`. State и Msg не двигаем. Без правок DB/prefs/use case.

### 2.5. Новая мини-фича

**Тикет:** IS449

**Пример:**
- **IS449** privacy policy WebView — Screen (новый: settingstab subscreen) — ui+logic+deps минимальные; App.navigation (route + navigator wiring); Manifest (INTERNET permission); Specs (`webview-screen.md`)

**Профиль:** новый Screen-узел + правка App.navigation + manifest. Без БД, без миграций.

### 2.6. Рефактор бизнес-логики экрана

**Тикеты:** IS441×2

**Пример:**
- **IS441** lang screen refactor + flag-picker — Screen.deps (новый контракт `CountryFlagItem`, `flagsFlow`, `updateFilter`), Screen.Logic (full TEA-rewrite), Screen.ui (новый layout), App.UseCaseImpl, Library.flags (интеграция blongho country_data), Specs (`dictionary-create.md`), Guides (если поменялся паттерн)

**Профиль:** **весь экран целиком**, плюс возможная новая library-обёртка. БД может затронуться. Specs обязательны.

### 2.7. Архитектурный / инфра рефактор

**Тикеты:** IS471, IS451

**Примеры:**
- **IS471** Dagger DI refactor — DI (`AppComponent`, `@AssistedInject` повсеместно), App.uiDeps (`CompositionRoot` rename), App.navigator (introduce Navigator interface + Impl), все Screen.deps (Navigator контракты), Core.mate (`MateNavigationEffectHandler`), Guides (`dagger-di.md`, `navigation.md`), Specs (sweep)
- **IS451** Logger levels — Core.logger (новый: уровни, sink-pattern), App.logger (`LexemeLoggerImpl` + sinks), DI (LoggerModule), Build (BuildConfig: LOG_LEVEL, REMOTE_LOG_LEVEL), Guides (`logging.md`)

**Профиль:** трогает **все или почти все** Screen-модули (cross-cutting), Core, DI, Build. **Гайды обязательны.** Спеки — выборочно. Самый длинный flow.

---

## 3. Сигналы для классификации

Признаки в тексте задачи / в коде, которые однозначно определяют scope.

### 3.1. «Только UI, без логики»

- В брифе слова: «вёрстка», «отступы», «иконка», «цвет», «шрифт», «ширина поля», «padding»
- В критериях приёмки нет упоминания данных / новых полей / новых сценариев
- Точка изменения — единственный файл `*Screen.kt` или composable-widget
- В коде: правка касается параметров `Modifier`, ресурсов цвета/строк, состава `Row/Column/Box`, но не `State` и не `Msg`
- **Анти-сигнал:** нужно добавить новое поле для условного рендера → State меняется → это уже не «только UI»

### 3.2. «Требует обновления TEA-контракта»

- Меняется набор пользовательских действий (новая кнопка, новый input) → новый `Msg.*`
- Меняется набор внешних эффектов (новая команда в БД, новый Flow) → новый `Effect.*` или `*FlowHandler`
- Меняется визуальное состояние (новая ветка рендера, новый loading/error/empty state) → новое поле в `State`
- Меняется навигация (новый Back-сценарий, переход к другому экрану) → новый `NavigationEffect`
- В стектрейсе/коде дефект в `Flow.combine` или `Flow.map` → почти всегда контракт `Flow<T>` → `Flow<T?>`
- **Сигнал в bug-кейсах:** дефект имеет «близнецов» в других экранах (как IS476 — 3 use case'а) → нужно править контракт **системно**

### 3.3. «Требует миграции БД»

- Меняется доменная модель (новое поле в `DictionaryItem`, `Lexeme`, `Word`) → schema bump
- Меняются CASCADE/FK constraints
- Меняется индекс
- Файлы: что-то под `core/core-db-impl/` (`*Db.kt`, `*Dao.kt`) **И** контракт в `core/core-db-api/` одновременно
- В artifact'ах: должна появиться `schemas/<dbName>/<newVersion>.json` + `migrations/`
- Тесты миграций: `core/core-db-impl/src/androidTest/...MigrationsTest.kt`
- Гайд: `docs/guides/testing-migrations.md`
- **Сигнал в bug-кейсах:** прод-краш с `IllegalStateException` от Room, `NOT NULL constraint failed`, orphaned rows (как IS463)

### 3.4. «Архитектурный рефактор, а не точечный фикс»

- Дефект/задача проявляется в N≥3 модулях одинаково (IS476: AppBar+Tab+QuizChat; IS471: DI везде)
- Меняется базовый класс из `core/mate/` или `core/di/`
- Меняется контракт сразу нескольких use case'ов
- Меняется BuildConfig-параметр, который читают все sinks / handlers
- Тикет в брифе говорит о «принципе», «паттерне», «графе зависимостей»
- Затрагиваются файлы из `docs/guides/` — гайды описывают **правила**, и если правило меняется — это рефактор, а не фикс

---

## 4. Аспекты помимо слоёв

Что ещё важно учитывать в scope-анализе, кроме «какие папки тронем».

### 4.1. Публичный контракт модуля (deps API)

- Файлы в `<module>/deps/` — это **публичный API модуля для остальных**
- Изменения здесь = breaking change для всех потребителей
- Сигнал: нужна правка тестов в потребителях, обновление `UseCaseImpl` в `app/`, апдейт спек

### 4.2. Кросс-модульные подписки (Flow живут долго)

- В `mate`-фреймворке `*FlowHandler` подписывается на Flow и живёт пока жив экран
- При `saveState=true` в табах — подписки **всех табов живы одновременно** (см. IS476)
- Сигнал: краш приходит из бэкграунда / из таба, где пользователя нет → анализировать кросс-таб реактивность

### 4.3. Версионирование БД и совместимость

- Любое изменение `*Db.kt` / DAO / `RoomDatabase.version` = миграция
- Откатывать нельзя — пользователи уже на новой версии
- Сигнал: нужна `*MigrationsTest` + ручная проверка `assembleRelease`

### 4.4. ProGuard / R8 (release-only баги)

- Code shrinking может убить классы для рефлексии (Gson, Moshi, blongho country_data — см. IS472)
- В debug всё ОК → краш только в `assembleRelease`
- Сигнал: краш в продакшне которого нет локально → проверять `app/proguard-rules.pro`

### 4.5. Manifest и permissions

- INTERNET, AD_ID, FOREGROUND_SERVICE — не должны добавляться без явной необходимости (IS459 — убрали AD_ID)
- Любая новая фича с сетью/локацией/уведомлениями = правка манифеста + privacy policy

### 4.6. User journey (сквозной сценарий)

- Поведение фичи может быть размазано по нескольким экранам (IS476: list → delete → exit → next launch → onboarding)
- Сигнал: спека должна явно прошивать journey крест-накрест (cross-ref между `dictionary-list.md` и `dictionary-create.md`)

### 4.7. Документация — два уровня

- **Spec** (`docs/features-spec/*.md`) — снимок состояния **фичи**. Правится при каждом фичевом изменении.
- **Guide** (`docs/guides/*.md`) — снимок состояния **подсистемы/паттерна**. Правится только когда меняется правило.
- Сигнал: если поменялся **how we do X** (а не «что делает фича X») → надо обновить гайд

### 4.8. Тесты — какой уровень

- **Reducer tests** (`*ReducerKtTest`) — чистый TEA, без mock'ов — для всех изменений `State`/`Msg`/`Reducer`
- **UseCaseImpl tests** (`app/src/test/.../di/module/<feature>/`) — для контракта use case
- **Migration tests** (`core/core-db-impl/src/androidTest/`) — только для DB-миграций
- **Lint** — для всего
- Сигнал: какие из этих 4-х нужны определяется тем, **какие слои тронуты**

### 4.9. Релиз-аспекты

- Версионирование: `IS461+ → 0.1.1`, `IS472+ → 0.1.2`, и т.д. Маркер: новая версия = пора в `release notes`
- Crashlytics: каждый прод-краш привязан к версии — фикс должен попасть в следующий релиз
- Сигнал: если задача — крах в проде, scope включает «бамп версии + release notes + push to play»

### 4.10. Backlog hygiene

- Архитектурные предложения, которые не лезут в текущий scope → автоматически в `docs/Backlog.md` секция `## ВекторныйПиздеж` (правило из CLAUDE.md)
- Сигнал: если архитектура сильно мешает текущему фиксу, но фикс возможен «как есть» — финдинг в бэклог, текущая задача завершается

### 4.11. Stakeholder-аспект (кто будет смотреть PR)

- Это сам разработчик (apomazkin) — итерации быстрые, можно делать squash-merge без code review
- Но: `architect` agent в `design_tree` review-шаге — внутренний strict reviewer, замещает peer review

---

## 5. Ловушки («выглядит просто — на деле глубже»)

### 5.1. UI-фикс, который требует менять State

**Кейс:** «надо показать заглушку, когда список пуст» — звучит как UI.

**Реальность:** «пустота» — это **состояние**, а не отсутствие данных. Нужно:
- Добавить поле в `State` (например `hasNoDictionary` в IS476)
- Reducer должен уметь перейти в это состояние из всех релевантных Msg
- EffectHandler должен публиковать соответствующий Msg
- Только тогда — UI рисует заглушку

**Пример из проекта:** IS476 — добавили `State.hasNoDictionary: Boolean` (явное поле, см. memory `feedback_explicit_state_flags.md`). Соблазн был «вычислять inline в composable из `dict == null`», но это не TEA-way.

### 5.2. Простой defensive guard, который скрывает архитектурную проблему

**Кейс:** прод-краш `IndexOutOfBoundsException` на пустом списке (IS461). Решение «в лоб» — `if (list.isEmpty()) return`.

**Реальность:** часто это симптом того, что **контракт источника** некорректен. IS461 разобрали как простой guard. IS474 — похожий по характеру дефект, но решили **сменой контракта** (`Flow<Long>` → `Flow<Long?>`). IS476 — повтор уже на уровне `Flow<Dict>` → `Flow<Dict?>` сразу для трёх use case'ов.

**Урок:** при guard-фиксе всегда задавай вопрос «есть ли близнецы у этого дефекта в других местах?» и «не должен ли контракт быть nullable системно?» Если ответ «да хотя бы на один» — это уже §2.2, а не §2.1.

### 5.3. «Просто добавить флаг в БД» = миграция + миграционный тест + обновление api+impl одновременно

**Кейс:** «добавим в `DictionaryItem` поле `numericCode: Int?`» (IS441 family).

**Реальность:**
- `core-db-impl/.../*Db.kt` — новая колонка
- `RoomDatabase.version++` + миграция в `migrations/`
- Новый JSON в `core-db-impl/schemas/<version>.json`
- `core-db-api/.../DictionaryItem.kt` — поле в доменной сущности
- Маппер DB↔domain в `core-db-impl`
- Тест миграции в `androidTest`
- Использование в UseCase + UI

Один «маленький» флаг = ~8 точек правки. Сигнал в scope analysis: «новое поле в БД» → автоматически включаем шаг `db_migration`.

### 5.4. Реактивный баг ≠ UI баг

**Кейс:** «иконка в AppBar не обновляется после смены флага» (IS445).

**Реальность:** соблазн искать ошибку в composable. На самом деле — `UseCase` отдаёт snapshot (`suspend fun getCurrentDict()`) вместо подписки (`fun flowCurrentDict(): Flow<DictItem>`), а Room не уведомляет UI о смене. Правится **сменой контракта use case** на Flow + правкой EffectHandler (одноразовый → подписочный `FlowHandler`).

**Сигнал ловушки:** «после действия X на экране A экран B показывает старое». Это **не UI**, это **модель подписки**.

### 5.5. ProGuard-ловушка release-only

**Кейс:** «всё работает в debug, падает на проде» (IS472, IS467).

**Реальность:** R8 shrink/obfuscate ломает рефлексию (Gson/Moshi/blongho). Изменения локального кода не помогут — нужен `-keep` в `proguard-rules.pro`. Сигнал: scope включает Build-слой, даже если код «правильный».

### 5.6. Cross-tab подписки выживают табы

**Кейс:** краш приходит из таба, на котором пользователя нет (IS476).

**Реальность:** в Compose Navigation с `saveState=true` `BackStackEntry` всех табов **живут одновременно**, как и их ViewModel'и → как и `mate`-подписки. Фикс «одного» экрана при удалении словаря должен покрыть **все табы, где есть подписка на текущий словарь** (AppBar в Tab, Quiz, Stat) — это поиск по сигнатуре `flowCurrentDict` / `getCurrentDictionaryId`, а не по UI.

**Сигнал ловушки:** «дефект логически касается одного экрана, но подписка на источник есть в нескольких». scope_analysis должен grep'нуть всех потребителей источника.

### 5.7. «Архитектурный гайд устарел» — невидимая часть scope

**Кейс:** после рефактора DI/мейта/логгера старые гайды (`docs/guides/*.md`) уже не описывают реальность.

**Реальность:** следующий разработчик / LLM прочитает устаревший гайд и **повторит старый паттерн**. Регрессия архитектуры через документацию.

**Урок:** scope любой архитектурной задачи включает аудит `docs/guides/` на предмет «что устарело и должно быть переписано или удалено».

---

## 6. Архитектура адаптивного flow

### 6.1. Концепция

**Master flow адаптивный, sub-flows строгие.**

- **Master flow** определяет на основе scope_analysis какие слои затронуты, и запускает соответствующие sub-flow в нужном порядке.
- **Sub-flow** — строгий пайплайн с фиксированным набором шагов, по одному на каждый обобщённый слой (см. §1.0).

**Тип задачи (bug / refactor / new feature) — НЕ отдельная ось классификации.** Это естественное следствие набора затронутых слоёв и характера изменений в них (что выясняется в scope_analysis). Triage как отдельный шаг не нужен — диагностика причины проводится в scope_analysis и распределяется по соответствующим sub-flow.

### 6.2. Состав sub-flow

По одному sub-flow на каждый обобщённый слой:

1. **Infrastructure sub-flow** — изменения в инфраструктуре (DI, mate, logger, navigation infra, core.ui/theme, build/proguard, manifest, guides)
2. **Business logic sub-flow** — контракты, TEA-логика, UseCase, доменные сущности, specs
3. **UI sub-flow** — composables, вёрстка, визуальные виджеты
4. **Data sub-flow** — DB (schemas, миграции), prefs, library-обёртки

### 6.3. Порядок выполнения

```
1. scope_analysis  (в master)
        ↓
2. Infrastructure sub-flow      ← если Инфра затронута. ВСЕГДА первый.
                                  Фундамент, на котором стоит остальное.
        ↓
3. Business sub-flow            ← если Бизнес-логика затронута.
                                  Формирует контракты и решения.
        ↓
4. UI sub-flow  ∥  Data sub-flow ← параллельно после Business.
                                   Принимают на вход выхлоп Business.
        ↓
5. integration / publish  (в master)
```

Sub-flow, чей слой не затронут, **не запускается**.

**Почему Infra первая:** инфраструктурные изменения (новая версия mate, новый DI-паттерн, новый sink логгера) — фундамент. Бизнес/UI/Data будут писаться **поверх** обновлённой инфраструктуры. Если запустить их параллельно — придётся переделывать.

**Почему Business вторая:** контракты, бизнес-сущности и решения по поведению фиксируются ДО реализации. UI и Data получают эти решения на вход и реализуют их.

**Почему UI и Data параллельно:** после фиксации Business-контрактов они независимы. UI рисует то, что описано в State (от Business). Data реализует доступ к источникам, описанным в контракте (от Business). Между собой не зависят.

### 6.4. Передача данных между sub-flow

Sub-flow на вход получает выходные артефакты предыдущих (summary от каждого предыдущего, или конкретные артефакты — **детали договорим в следующем документе**). Sub-flow на выход даёт свои артефакты в master, который доступен всем последующим sub-flow.

### 6.5. Feedback loop (ошибка анализа)

Если sub-flow в процессе работы обнаруживает что анализ был неточен (например UI нашёл, что бизнес-контракт не покрывает реальный кейс) — sub-flow **перенаправляет в master с новыми данными**.

Master:
- Перезапускает **scope_analysis** с расширенным контекстом
- Артефакты предыдущей итерации **не выбрасываются** — архивируются с суффиксом `_iterN` и становятся входом для нового scope_analysis
- Уже выполненные sub-flow **не откатываются автоматически**. Новый scope_analysis на основе артефактов прошлой итерации решает: какие sub-flow перезапустить (с новым input), какие оставить как сделанные

Подробная механика, версионирование артефактов и лимиты итераций — в [`FORGEFLOW_design.md §8`](FORGEFLOW_design.md).

### 6.6. Sub-flow внутри — строгий пайплайн

Sub-flow НЕ адаптивный изнутри. Каждый имеет фиксированный набор шагов под свой слой. Вся адаптивность вынесена в master.

**Конкретный состав шагов каждого sub-flow — отдельный документ.**

---

_Документ создан для проектирования адаптивного flow в ForgeFlow. Источник: анализ структуры PolyTrainer и 13 завершённых тикетов (IS441–IS476)._
