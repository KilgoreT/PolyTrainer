# Архитектура проекта

## Граф модулей

```
app (application)
 |
 +-- modules/core/mate          -- TEA фреймворк состояний
 +-- modules/core/theme         -- Material3 тема (AppTheme, цвета, типографика)
 +-- modules/core/ui            -- Общие composable виджеты (~40+ компонентов)
 +-- modules/core/tools         -- Kotlin утилиты для коллекций
 |
 +-- modules/screen/splash          -- Splash (простой ViewModel, без TEA)
 +-- modules/screen/createdictionary -- Создание словаря (TEA)
 +-- modules/screen/main            -- Контейнер табов (Navigation, без TEA)
 +-- modules/screen/vocabulary      -- Host вкладок словаря «Слова|Группы» (TEA; IS493, владеет текущим словарём)
 +-- modules/screen/wordstab        -- Список слов, вкладка «Слова» (TEA; экс-dictionaryTab)
 +-- modules/screen/groupstab       -- Вкладка «Группы»: «Все» + чанки (TEA; IS493 Э2)
 +-- modules/screen/wordcard        -- Карточка слова (TEA, reference с тестами)
 +-- modules/screen/quiztab         -- Выбор квиза (TEA)
 +-- modules/screen/quiz/chat       -- Чат-квиз (TEA, reference для редьюсера)
 +-- modules/screen/stattab         -- Статистика (TEA)
 +-- modules/screen/settingstab     -- Настройки (TEA)
 |
 +-- modules/widget/dictionaryappbar    -- AppBar с TEA
 +-- modules/widget/dictionarypicker    -- Выбор словаря
 +-- modules/widget/chipPicker          -- Выбор чипов
 +-- modules/widget/iconDropDowned      -- Dropdown иконка
 +-- modules/widget/wordrow             -- Строка слова (вёрстка + UI-entities; IS493 Э2, экс-TermWidget)
 +-- modules/widget/grouptree           -- Строка узла дерева групп (IS493 Э2; без content-слота)
 |
 +-- modules/domain/lexeme      -- Чистый Kotlin: домен лексем/компонентов
 +-- modules/domain/group       -- Чистый Kotlin: DisplayTree групп (IS493 Э2; валидации — Э3)
 |
 +-- modules/datasource/prefs   -- DataStore preferences
 +-- modules/library/flags      -- Флаги стран
 |
 +-- core/core-db               -- DB API интерфейсы (legacy)
 +-- core/core-db-api           -- DB API контракты
 +-- core/core-db-impl          -- Room реализация
 +-- core/core-resources        -- Общие ресурсы
```

## Правила зависимостей

1. **Feature модули зависят от core модулей.** Никогда наоборот.
2. **Core модули не зависят друг от друга** (кроме: `ui` зависит от `theme`).
3. **Screen модули не зависят от других screen модулей.** Связь через навигацию.
4. **Widget модули** могут иметь собственные Mate инстансы (например, `dictionaryappbar`).
5. **Screen модули не зависят от Compose Navigation.** Видят интерфейс `XxxNavigator : Navigator` из `core/mate`, реализация `XxxNavigatorImpl` живёт в `app/.../navigator/`.
6. **Legacy core модули** (`core/core-*`) постепенно заменяются.

## Слои

```
UI Layer (Screen.kt)                                    Navigator (XxxNavigator)
    |                                                       ^
    | collectAsStateWithLifecycle()                         |
    | accept(Msg)                                           |
    v                                                       |
State Management Layer (ViewModel + Mate + Reducer)         |
    |                                                       |
    | Effects (DatasourceEffect / UiEffect /                |
    |          NavigationEffect)                            |
    v                                                       |
Effect Layer (handlers)                                     |
    |  MateTypedEffectHandler / MateFlowHandler  --------> NavigatorImpl (app/.../navigator/)
    |  MateNavigationEffectHandler ----------------------> NavController
    | UseCase вызовы
    v
Domain Layer (UseCase интерфейсы)
    |
    | Реализация
    v
Data Layer (Room, DataStore, Prefs)
```

Базовые классы handlers в `core/mate`:
- `MateTypedEffectHandler<Msg, E>` — автоматическая фильтрация чужих эффектов через `filter()`.
- `MateNavigationEffectHandler<Msg>(navigator)` — наследует typed handler, обрабатывает `NavigationEffect.Back` через `Navigator.back()`, делегирует специфичные в `onScreenEffect()`.
- `MateFlowHandler` — для долгоживущих подписок на Flow.

## Dependency Injection

**Dagger 2** с единственным `AppComponent` (Singleton scope).

### AppComponent

```kotlin
@Singleton
@Component(
    dependencies = [CoreDbProvider::class],
    modules = [AppModule::class],
)
interface AppComponent {
    fun getSplashUseCase(): SplashUseCase
    fun getDictionaryUseCase(): DictionaryUseCase

    // Factory каждой ViewModel — Dagger генерирует из @AssistedFactory
    fun getDictionaryFormViewModelFactory(): DictionaryFormViewModel.Factory
    fun getDictionaryListViewModelFactory(): DictionaryListViewModel.Factory
    fun getWordCardViewModelFactory(): WordCardViewModel.Factory
    fun getChatViewModelFactory(): ChatViewModel.Factory
    fun getDictionaryAppBarViewModelFactory(): DictionaryAppBarViewModel.Factory
    fun getWordsTabViewModelFactory(): WordsTabViewModel.Factory
    fun getQuizTabViewModelFactory(): QuizTabViewModel.Factory
    fun getStatisticViewModelFactory(): StatisticViewModel.Factory
    fun getSettingsTabViewModelFactory(): SettingsTabViewModel.Factory

    fun getLogger(): LexemeLogger
}
```

### Composition root для UI

`CompositionRootImpl` (в app модуле) — единственное место, где Factory из AppComponent встречаются с навигационными callbacks из RootRouter:

```kotlin
class CompositionRootImpl(
    private val wordCardViewModelFactory: WordCardViewModel.Factory,
    // ... остальные Factory + envParams + logger
) : CompositionRoot {
    @Composable
    override fun WordCardScreenDep(wordId: Long, onBackPress: () -> Unit) {
        val navigator = remember(onBackPress) { WordCardNavigatorImpl(onBackPress) }
        WordCardScreen(
            wordId = wordId,
            factory = wordCardViewModelFactory,
            navigator = navigator,
        )
    }
}
```

Composable получает `factory + navigator` параметрами — никогда не лезет в `appComponent` напрямую.

### DI модуль фичи

```kotlin
@Module
interface QuizChatModule {
    @Binds
    fun bindQuizChatUseCase(impl: QuizChatUseCaseImpl): QuizChatUseCase
}
```

ViewModel биндинга в модуле фичи **нет** — `@AssistedInject` + `@AssistedFactory` создаёт Factory автоматически, getter на AppComponent её экспонирует.

### ViewModel — @AssistedInject

```kotlin
class WordCardViewModel @AssistedInject constructor(
    @Assisted wordId: Long,
    @Assisted navigator: WordCardNavigator,
    datasourceHandler: DatasourceEffectHandler,
    uiHandler: UiEffectHandler,
    navHandlerFactory: WordCardNavigationEffectHandler.Factory,
) : ViewModel(), MateStateHolder<WordCardState, Msg> {

    @AssistedFactory
    interface Factory {
        fun create(wordId: Long, navigator: WordCardNavigator): WordCardViewModel
    }
}
```

`@Assisted` для runtime аргументов (Navigator, id), остальные зависимости — обычный constructor injection через Dagger.

**Снятое исключение (IS493 Э2):** `VocabularyHostViewModel` в Э1 жил вне Dagger
(zero deps); с появлением зависимости (подписка на текущий словарь) мигрировал
на `@AssistedInject` по общему образцу — VM вне AppComponent в проекте больше
нет. Нюанс: у host- и groupstab-VM factory БЕЗ assisted-параметров
(`fun create(): Vm`) — навигатора/runtime-аргументов у них нет, Dagger это
поддерживает.

### TabSpec-контракт host-модуля (IS493)

Host внутренних вкладок (`modules/screen/vocabulary`) объявляет контракт
`TabSpec(titleRes, topBarOverride, fab, content)`; наполняет его app-мост
(`CompositionRootImpl`) контентом других screen-модулей — host и вкладки друг о
друге не знают (обобщение приёма `CompositionRoot`/UiDeps-слотов). TabSpec'ы в
мосте собираются в `remember` (лямбды в data class ломают equals — без remember
`@Stable`-skipping не работает).

Э2: host — единственный резолвер текущего словаря для вкладок; `content`
получает `DictionarySlot(id, isResolved)` **параметром вызова** (не полем
TabSpec — спеки не пересобираются при смене словаря). Два явных поля вместо
одного nullable: `isResolved=false` — prefs ещё не эмитил (loading), `true` +
`id=null` — честное «словарей нет». Мост распаковывает слот в примитивы для
вкладки; вкладка проводит их в VM через
`LaunchedEffect(slot) { accept(Msg.DictionaryChanged(...)) }` и НЕ подписывается
на prefs сама.

### Reducer-конвенция: атомарные state-экстеншны + ReducerChain (IS493 Э3)

**Msg → видимая ЦЕПОЧКА атомарных state-экстеншнов.** Атом = ОДИН
технический шаг изменения (`closeSheet()` — убрать шторку,
`refreshVisibleGroups()` — пересчитать фильтр: это РАЗНЫЕ шаги, каждый
виден в цепочке отдельно). Атомы друг друга не вызывают;
атомов-оркестраторов НЕТ — guard'ы, ветвление (`when`/`?:`) и сами
цепочки живут в ветках reducer'а, чтобы технические шаги читались по
месту. Экстеншны возвращают `ReducerResult` (эффект рождается там, где
изменение — Elm-паттерн), цепочки собираются комбинаторами
`begin/then/withEffect` (`core/mate/ReducerChain.kt`).

Мапперы границы reducer'а — зеркальная пара (образец —
`groupstab/logic/MutationMappers.kt`): на входе intent → effect
(`GroupSheetState.toMutationEffect`), на выходе доменный outcome →
ПЛОСКИЙ Msg (`toMutationMsg`), который handler маппит ДО отправки —
простое сообщение → простое изменение стейта, reducer не разворачивает
доменные sealed'ы (MutationApplied / MutationRejected(error) /
MutationIgnored; исключение эффекта — отдельный GroupMutationFailed).
Какая операция ответила — видно в event-логе handler'а строкой выше.
Сборка эффекта — маппинг данных, не state-шаг: её место в маппере, не
в ветке.

**Ветка не должна быть нагромождённой.** В ветке допустимы только:
guard'ы, ветвление по state/результату маппера и цепочка атомов.
Многострочный inline-`when` по ДАННЫМ (сборка эффекта, выбор конструктора
по mode) внутри `withEffect`/цепочки — признак, что нужен маппер
(прецедент: `Msg.SubmitSheet` — inline-сборка Create/Rename-эффекта
смутила ревью юзера, вынесена в `toMutationEffect`). Дробить Msg ради
упрощения ветки (SubmitCreate/SubmitRename из UI) — НЕ решение: интент
у кнопки один, mode — контекст в state; split дублирует источник истины
и переносит `when` в UI.

**Где рождается эффект:** в атоме — если данные для него есть в state или
параметрах атома (`closeWindow` → `SetWindow(null)`, `openWindow(limit)` →
`SetWindow(limit)`); `withEffect` в ветке — когда эффект собирается из
контекста ветки (Msg, несколько частей state — например эффект мутации
из mode шторки + dictionaryId при submit).

**Порядок в цепочке значим:** атом, читающий «прежнее» значение, идёт до
атома, его перезаписывающего (`widenWindowForGrowth(newCount)` — ДО
`applyAllCount(count)`: дельта считается от старого count); производные
шаги — после источника (`refreshVisibleGroups` после смены groups/шторки).

**KDoc обязателен каждому экстеншну:** что делает (одной фразой — какой
технический шаг), когда зовётся, `@param` для каждого параметра, какие
поля меняет, какие эффекты возвращает (или «эффектов нет»), поведение
no-op-guard'ов; если сопутствующий шаг вынесен в соседний атом цепочки —
сослаться на него.

**Логирование:** logger — конструктор reducer'а (образец words); `reduce()`
логирует каждый входящий Msg через `logMessage` (`Reduce ---message---`);
data-Msg с тяжёлым содержимым (списки слов/дерево) — только именем класса,
их содержимое компактно логирует effect handler (counts). Юзер-события —
полным toString. **Тег — фичевый, один на экран:** `logTag` объявлен в
`ReducerLogging` и задаётся классом `StateAtoms` экрана
(`override val logTag = LogTags.GROUPS`); под ним пишут и шаги, и
сообщения, и effect handler — весь флоу вкладки достаётся одним
`adb logcat | grep ###GROUPS###` (дефолтный `###LEXEME###` разрывал ленту
надвое, прецедент фичевого тега — per_dictionary_components/Reducer).

**Логирование КАЖДОГО атома цепочки** — через member extension functions:
атомы объявляются НЕ top-level, а членами класса `StateAtoms` экрана,
который наследует `mate/ReducerLogging(logger)`. Логгер приходит в атом
дважды-receiver'ным вызовом (dispatch receiver = класс, extension receiver
= state) — без параметра в сигнатуре и без хранения в state. Reducer
наследует `StateAtoms`, ветки не меняются ни на символ; ext-тест наследует
его же с `NoopLogger`. Формат шага — событие + характеристики:
`Reduce ---step---: applyAllCount | count=13`; тяжёлые данные —
счётчиками (`applyGroups | groups=3`, `refreshVisibleGroups | visible=1`).
Чистые внутренние хелперы (`updateNode`, `visibleGroupsFor`) не логируются.
Иерархия: `ReducerLogging` (mate, один на проект) ← `StateAtoms` (атомы
экрана) ← `XxxReducer` / `StateAtomsTest`. Когда context parameters
Kotlin выйдут из experimental — миграция механическая.

Тесты — три слоя: экстеншны (state + эффекты атома), message-тесты
reducer'а, сценарные цепочки Msg (ответы handler'а подаются вручную).
Первый экземпляр конвенции — groupstab; words мигрирует по мере правок.

**Раскладка тест-файлов (решение юзера 2026-08-28): НИКАКИХ
Stage/этапов в именах** — фича сквошится в один коммит, этапы — артефакт
разработки. Message-тесты режутся по ФУНКЦИОНАЛЬНЫМ зонам state
(образец groupstab: `*LifecycleReducerTest` — словарь/slice-структура,
`AllNodeReducerTest`, `GroupWindowsReducerTest`, `GroupSheetReducerTest`,
`GroupDeleteReducerTest`); ВСЕ сценарные — в одном `*ScenarioTest`
экрана, методы с префиксом `` `scenario - …` ``.

### Handle-паттерн module-API (IS493)

Вкладка, чьи куски (topBar-override, FAB, контент) распределяются по чужому
Scaffold, экспортирует не `Screen(...)`, а handle:
`rememberWordsTabHandle(factory, navigator): WordsTabHandle` — VM создаётся и
коллектится ВНУТРИ модуля, наружу — узкие `derivedStateOf`-State и
composable-куски. App-мост остаётся чистой склейкой без `viewModel()`/`collect`
(и без lifecycle-зависимостей в app). Первый экземпляр — wordstab; шаблон для
будущих tab-модулей.

## Сборка

- **Version catalogs** в `deps/*.versions.toml` (не `gradle/libs.versions.toml`)
- **Build варианты:** LOCAL, CI_DEV, CI_PROD с разными keystore
- **CI/CD:** GitHub Actions на ветках `IS*` и `MT*`: Lint → Tests → Build APK
- **Convention plugin** в `build-logic/` (частично используется)

## Ключевые технические решения

| Решение | Обоснование |
|---------|-------------|
| Кастомный TEA (Mate) vs MVVM | Явные эффекты, чистые редьюсеры, трассируемый стейт |
| Dagger 2 vs Hilt | Проект старше Hilt, миграция не приоритетна |
| Модуль на каждый экран | Чёткая изоляция, независимая компиляция |
| @Stable vs @Immutable | Более гибко для вложенных ссылок |
| `@AssistedInject` + `@AssistedFactory` для всех ViewModel | Constructor injection без Hilt, Navigator/runtime аргументы через `@Assisted`; у VM без runtime-аргументов (host, groupstab) — factory с `create()` без параметров |
| Sealed interface для Msg | Exhaustive when, явный каталог сообщений |
| Navigator интерфейс в screen, Impl в app | Screen не зависит от Compose Navigation, легко мокать в тестах |
| `CompositionRoot` / `CompositionRootImpl` | Единая точка связки Dagger → Compose: фабрики ViewModel встречаются с навигационными callbacks |
