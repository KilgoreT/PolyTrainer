# EffectHandlers через Dagger

## Идея

Сейчас все `EffectHandler`'ы создаются вручную в `init` блоке ViewModel. ViewModel принимает UseCase'ы и callbacks через конструктор, передаёт их в handlers:

```kotlin
class DictionaryListViewModel @AssistedInject constructor(
    @Assisted onBackPress: () -> Unit,
    @Assisted onExit: () -> Unit,
    @Assisted onEditDictionary: (Long) -> Unit,
    dictionaryUseCase: DictionaryUseCase,
) : ViewModel() {

    private val stateHolder = Mate(
        initState = DictionaryListScreenState(),
        reducer = DictionaryListReducer(),
        effectHandlerSet = setOf(
            DictionaryListEffectHandler(dictionaryUseCase),                         // ← вручную с UseCase
            ListNavigationEffectHandler(onBackPress, onExit, onEditDictionary),     // ← вручную с callbacks
        )
    )
}
```

ViewModel — посредник между Dagger (UseCase) / navigation layer (callbacks) и handlers. Boilerplate.

**Хотим:** handlers приходят готовыми. ViewModel не знает про UseCase/callbacks, только подключает handlers.

---

## DatasourceEffectHandler — через @Inject

DatasourceEffectHandler принимает только UseCase из DI. Делаем `@Inject constructor` — Dagger сам создаёт.

```kotlin
class DictionaryListEffectHandler @Inject constructor(
    private val dictionaryUseCase: DictionaryUseCase,
) : MateEffectHandler<DictionaryListMsg, Effect> { ... }
```

ViewModel получает готовый handler через DI:

```kotlin
class DictionaryListViewModel @AssistedInject constructor(
    datasourceHandler: DictionaryListEffectHandler,
    // ...
) : ViewModel() {
    private val stateHolder = Mate(
        effectHandlerSet = setOf(datasourceHandler, ...)
    )
}
```

UseCase исчез из ViewModel конструктора. Аналогично для UiEffectHandler, FlowHandler — всё что принимает только DI зависимости.

---

## NavigationEffectHandler — наследование от базового

NavigationEffectHandler принимает runtime `NavController` через интерфейс `Navigator`. Базовый handler в `core/mate` обрабатывает `Back`, экранные наследуют и добавляют свои эффекты. `ExitApp` — не базовый эффект, объявляется только в тех экранах где он нужен (root экраны: Splash, DictionaryList).

### Базовая инфраструктура (core/mate)

```kotlin
// core/mate/NavigationEffect.kt
interface NavigationEffect : Effect {
    data object Back : NavigationEffect
}

// core/mate/Navigator.kt
interface Navigator {
    fun back()
}

// core/mate/MateNavigationEffectHandler.kt
abstract class MateNavigationEffectHandler<Msg, N : Navigator, E : NavigationEffect>(
    protected val navigator: N,
) : MateTypedEffectHandler<Msg, NavigationEffect>() {

    final override fun filter(effect: Effect): NavigationEffect? = effect as? NavigationEffect

    // `onEffect` (от MateTypedEffectHandler) замкнут final — обрабатывает базовый Back
    // и диспатчит остальное в `onScreenEffect`. `onScreenEffect` без consumer потому что
    // навигация не эмитит msg в reducer (см. секцию "Контракт NavController" + п.5 в гайде effect-handlers).
    @Suppress("UNCHECKED_CAST")
    final override suspend fun onEffect(effect: NavigationEffect, consumer: (Msg) -> Unit) {
        when (effect) {
            is NavigationEffect.Back -> navigator.back()
            // safe: per-screen reducer эмитит только эффекты своего sealed E + NavigationEffect.Back
            // ClassCastException возможен только при программной ошибке — эмит чужого NavigationEffect
            else -> onScreenEffect(effect as E)
        }
    }

    protected abstract suspend fun onScreenEffect(effect: E)
}
```

Наследует `MateTypedEffectHandler` — фильтрация чужих эффектов и `return` без consumer уже там. Базовый Navigation handler пишет только `filter` (один cast) и `onEffect` (диспатч на Back/screen-specific). Единый паттерн для всех handlers.

Базовый класс:
- `Back` обрабатывается один раз — нет дублирования
- `if (effect !is NavigationEffect) return` — handler не вызывает `consumer` для чужих эффектов
- `consumer` не вызывается **никогда** — навигация не меняет state, reducer'у нечего обрабатывать. Контракт `MateEffectHandler.runEffect` не требует вызова `consumer`
- Дженерик `N : Navigator` — типизированный доступ к специфичным методам экрана без cast
- Дженерик `E : NavigationEffect` — дочерний параметризует своим sealed подтипом, `when` в `onScreenEffect` exhaustive

**`ExitApp` — НЕ в базовом.** Не все экраны могут закрыть приложение (вложенные не могут). `ExitApp` добавляется в per-screen sealed effect там где нужен:
```kotlin
sealed interface ListNavigationEffect : NavigationEffect {
    data object ExitApp : ListNavigationEffect       // только для root экранов
    data class OpenEdit(val id: Long) : ListNavigationEffect
}
```

Per-screen Navigator добавляет `exit()` в свой контракт. Реализация дёргает `activity.finish()` или подобное.

`NavigationEffect` остаётся обычным `interface` (sealed нельзя — наследники в screen модулях). Per-screen эффект делается `sealed interface` в своём модуле — exhaustive обеспечивается через дженерик `E`.

### Per-screen — наследуют базовое

**Navigator интерфейс:**
DictionaryList — root экран, нужен `exit()`:

```kotlin
interface ListNavigator : Navigator {           // back наследуется
    fun exit()                                  // root экран — может закрыть приложение
    fun openEdit(id: Long)
}
```

**Per-screen NavigationEffect — sealed, добавляет ExitApp если нужен:**
```kotlin
sealed interface ListNavigationEffect : NavigationEffect {
    data object ExitApp : ListNavigationEffect
    data class OpenEdit(val id: Long) : ListNavigationEffect
}
```

**Handler — наследует `MateNavigationEffectHandler`, обрабатывает ExitApp и свои эффекты:**
```kotlin
class ListNavigationEffectHandler @AssistedInject constructor(
    @Assisted navigator: ListNavigator,
) : MateNavigationEffectHandler<DictionaryListMsg, ListNavigator, ListNavigationEffect>(navigator) {

    override suspend fun onScreenEffect(effect: ListNavigationEffect) {
        when (effect) {                                                      // exhaustive по sealed
            is ListNavigationEffect.ExitApp -> navigator.exit()
            is ListNavigationEffect.OpenEdit -> navigator.openEdit(effect.id)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navigator: ListNavigator): ListNavigationEffectHandler
    }
}
```

Handler обрабатывает только свои эффекты. `Back` — в базовом.

Для вложенного экрана (WordCard) — без exit:
```kotlin
interface WordCardNavigator : Navigator                  // только back
sealed interface WordCardNavigationEffect : NavigationEffect      // нет ExitApp
```

### Реализация Navigator (обычный класс, без Dagger)

`NavigatorImpl` — чистая обёртка над `NavController`, других DI зависимостей нет. Dagger избыточен — обычный конструктор:

```kotlin
// Root экран (DictionaryList) — принимает callback на Activity
class ListNavigatorImpl(
    private val navController: NavController,
    private val onExit: () -> Unit,                    // вызывает activity.finish()
) : ListNavigator {
    override fun back() = navController.popBackStack()
    override fun exit() = onExit()
    override fun openEdit(id: Long) = navController.navigate("dictionary_form/$id")
}

// Вложенный экран (WordCard) — только NavController
class WordCardNavigatorImpl(
    private val navController: NavController,
) : WordCardNavigator {
    override fun back() = navController.popBackStack()
}
```

Если внутри Navigator понадобятся DI зависимости (logger, prefs) — переходим на `@AssistedInject` с `NavController` через `@Assisted` + `@AssistedFactory`.

### ViewModel

```kotlin
class DictionaryListViewModel @AssistedInject constructor(
    @Assisted navigator: ListNavigator,
    datasourceHandler: DictionaryListEffectHandler,
    navHandlerFactory: ListNavigationEffectHandler.Factory,
) : ViewModel() {

    private val stateHolder = Mate(
        effectHandlerSet = setOf(
            datasourceHandler,
            navHandlerFactory.create(navigator),
        )
    )

    @AssistedFactory
    interface Factory {
        fun create(navigator: ListNavigator): DictionaryListViewModel
    }
}
```

ViewModel не знает про `NavController` — только про абстракцию `ListNavigator`. Получает готовый.

### Composable

```kotlin
@Composable
fun DictionaryListScreen(
    factory: DictionaryListViewModel.Factory,
    navigator: ListNavigator,
    viewModel: DictionaryListViewModel = viewModel(
        factory = viewModelFactory { factory.create(navigator) }
    ),
) { ... }
```

Composable принимает готовый `navigator` параметром. Не создаёт `NavigatorImpl` сам — это работа `app`.

### Navigation graph (в app)

```kotlin
composable("dictionary_list") {
    val activity = LocalActivity.current
    val navigator = remember(navController) {
        ListNavigatorImpl(
            navController = navController,
            onExit = { activity?.finish() },
        )
    }
    DictionaryListScreen(
        factory = appComponent.getDictionaryListViewModelFactory(),
        navigator = navigator,
    )
}
```

`LocalActivity` доступен с Compose 1.7+. Для вложенных экранов callback не передаётся:
```kotlin
composable("word_card/{wordId}") {
    val wordId = it.arguments?.getLong("wordId") ?: 0L
    val navigator = remember(navController) { WordCardNavigatorImpl(navController) }
    WordCardScreen(
        factory = appComponent.getWordCardViewModelFactory(),
        wordId = wordId,
        navigator = navigator,
    )
}
```

`app` знает про `NavController` (он в navigation graph), создаёт `Impl` через `remember(navController)` и передаёт в screen.

**Зачем `remember(navController)`:** без него `NavigatorImpl` пересоздаётся при каждой рекомпозиции родителя — Compose видит параметр `navigator` как нестабильный, screen перерисовывается зря. `remember` фиксирует инстанс между рекомпозициями, пересоздаёт только если `navController` изменился.

---

## Где хранить интерфейсы и реализации

**Интерфейсы — в screen модулях, реализации — в `app/navigation/`.**

```
modules/screen/dictionary/list/
  ├── ListNavigator.kt              ← интерфейс (чистый Kotlin)
  └── ListNavigationEffectHandler.kt

app/.../navigation/
  └── ListNavigatorImpl.kt          ← реализация (знает про NavController)
```

### Почему так

- **Интерфейс рядом с handler** — контракт виден в одном месте с тем кто его использует
- **Screen модуль не зависит от Compose Navigation** — чистый Kotlin
- **App = composition root** — знает все routes и навигационные связи. Это его роль
- **Нет gradle модуля для навигации** — без overhead

### Цена

- App пересобирается при изменении любой навигации (некритично для размера Lexeme)
- Реализации размазаны по `app/navigation/` — нет единого "карта навигации" в отдельном модуле

### Что отвергнуто

- **Реализации в отдельном `modules/navigation`** — циклическая зависимость: интерфейс в `screen/X`, реализация в `navigation`, composable в `screen/X` должен видеть реализацию → цикл.
- **API-модули на каждый screen** (`screen/X/api` + `screen/X`) — даёт изоляцию, но +N gradle модулей. Для Lexeme избыточно.
- **Полный навигационный модуль** (всё в `modules/navigation`) — изоляция Compose Navigation, но screen зависит от него — нет переиспользуемости.

---

## Контракт NavController

NavController в Lexeme создаётся через `rememberNavController()` в composable Activity, переживает configuration change через `rememberSaveable` механизм навигации.

**NavController не пересоздаётся в lifetime ViewModel:**
- Configuration change (rotation, theme) — тот же инстанс
- Process death — VM и NavController пересоздаются вместе, парные
- Вложенные NavHost (Main → tabs) — при пересоздании родителя пересоздаётся и NavBackStackEntry таба → ViewModel таба тоже пересоздаётся → `factory.create(navigator)` вызывается заново с актуальным navigator

ViewModel захватывает Navigator один раз при создании. Это безопасно — пересоздание NavController всегда сопровождается пересозданием ViewModel.

**Что нужно соблюдать:**
- Navigator всегда stateless — обёртка над NavController без собственного состояния
- NavController создавать только через `rememberNavController()` в Activity или вложенных composable, не передавать вверх по дереву

---

## Правила и заметки

### Scoping handlers

`@Inject` handlers — всегда unscoped. Не ставить `@Singleton` или `@Reusable` — handler stateless, но если несколько ViewModel'ей шарят один инстанс — потенциальные race conditions.

### NavigatorImpl — без логики

`NavigatorImpl` строго одна строка на метод — `navController.navigate(...)` или `popBackStack()`. Логика навигации (debounce, проверки) — в reducer перед эмитом effect. Иначе Impl становится untested (Compose Navigation в unit-тестах не поднимается).

### Splash openMain

`SplashNavigator` наследует базовый `Navigator` (получает `back()`). На Splash экране back бессмысленен — переопределить как no-op или `exit()`. ExitApp в Splash sealed effect — обязательный (closing app — единственный реальный выход).

### Process death

`@Assisted` параметры (Navigator) не переживают process death. Если ViewModel пересоздаётся через `SavedStateHandle`, Navigator пересоздаётся в composable — Factory вызывается заново. Это работает корректно (NavController + VM пересоздаются парно, см. секцию **Контракт NavController**).

### Правило для новых экранов во время миграции

После завершения **Этапа 1** (инфраструктура в core/mate) — все НОВЫЕ экраны пишутся по новому паттерну. Старый паттерн запрещён для нового кода. Существующие экраны мигрируются по таблице порядка.

### Boilerplate per-screen

Новый экран = 7 точек изменения: интерфейс Navigator, sealed NavigationEffect, EffectHandler + AssistedFactory, NavigatorImpl, ViewModel, Composable, navigation graph. Это сознательная плата за чистоту. При желании облегчается Live Template / file generator — не блокер.

---

## Cross-graph навигация (MainScreen + табы)

В Lexeme **2 NavController'а:**
- **Root** (создаётся в `MainActivity`): Splash, DictionarySetup, DictionaryCreate, DictionaryList, MainRouter
- **Tabs** (создаётся в `MainScreen`): vocabulary+wordCard, quiz+chat, statistic, settings+aboutApp+webview

Tab экран может навигировать на двух уровнях:
- Внутри своего таба (`wordCard` из vocabulary) — `tabsNavController`
- На root уровень (`openDictionaryCreate`) — `rootNavController` (захвачен в RootRouter)

**Решение:** Tab NavigatorImpl принимает `tabsNavController` + callbacks для root операций. MainScreen не знает про `rootNavController` — только про callbacks (как сейчас).

```kotlin
// modules/screen/dictionaryTab — интерфейс
interface VocabularyNavigator : Navigator {
    fun openDictionaryCreate()      // root-уровневая навигация
    fun openWordCard(id: Long)      // внутри таба
}

// app/.../navigation — Impl
class VocabularyNavigatorImpl(
    private val tabsNavController: NavController,
    private val onOpenDictionaryCreate: () -> Unit,   // callback от RootRouter
) : VocabularyNavigator {
    override fun back() = tabsNavController.popBackStack()
    override fun openDictionaryCreate() = onOpenDictionaryCreate()
    override fun openWordCard(id: Long) = tabsNavController.navigate("wordCard/$id")
}

// MainRouter — пробрасывает callbacks (как сейчас)
fun NavGraphBuilder.mainRouter(
    route: String,
    onOpenDictionaryCreate: () -> Unit,
    onOpenDictionaryList: () -> Unit,
) {
    composable(MainPoint.MAIN.route) {
        MainScreen(
            onOpenDictionaryCreate = onOpenDictionaryCreate,
            onOpenDictionaryList = onOpenDictionaryList,
        )
    }
}

// MainScreen — создаёт tabsNavController, передаёт его + callbacks в табы
@Composable
fun MainScreen(
    onOpenDictionaryCreate: () -> Unit,
    onOpenDictionaryList: () -> Unit,
) {
    val tabsNavController = rememberNavController()
    NavHost(tabsNavController, "vocabulary") {
        composable("vocabulary") {
            val navigator = remember(tabsNavController, onOpenDictionaryCreate) {
                VocabularyNavigatorImpl(tabsNavController, onOpenDictionaryCreate)
            }
            VocabularyTabScreen(navigator = navigator, ...)
        }
        // другие табы
    }
}

// RootRouter — захватывает rootNavController в callbacks
mainRouter(
    route = RootPoint.MAIN_ROUTER.route,
    onOpenDictionaryCreate = { rootNavController.navigate("dictionary_create") },
    onOpenDictionaryList = { rootNavController.navigate("dictionary_list") },
)
```

**Почему так:**
- Сохраняется текущий паттерн (callbacks сверху вниз)
- MainScreen инкапсулирован — не знает про rootNavController, только про callbacks
- Таб не может произвольно навигировать по root, только через объявленные callbacks
- NavigatorImpl: для своего таба — tabsNavController напрямую, для cross-graph — callback

**Root экраны** (Splash, DictionaryList, DictionaryForm) — один NavController, NavigatorImpl принимает только `rootNavController`. Проще.

### Shared widget AppBar — один Navigator

`DictionaryAppBar` используется в 3 табах (Vocabulary, Quiz, Statistic) с идентичной навигацией. Один Navigator на всё:

```kotlin
// modules/widget/dictionaryappbar — интерфейс
interface DictionaryAppBarNavigator : Navigator {
    fun openDictionaryCreate()
}

// app/.../navigation — один Impl
class DictionaryAppBarNavigatorImpl(
    private val onOpenDictionaryCreate: () -> Unit,
) : DictionaryAppBarNavigator {
    override fun back() {}                                    // no-op для shared widget
    override fun openDictionaryCreate() = onOpenDictionaryCreate()
}

// MainScreen — один инстанс на 3 таба
@Composable
fun MainScreen(onOpenDictionaryCreate: () -> Unit, ...) {
    val tabsNavController = rememberNavController()
    val appBarNavigator = remember(onOpenDictionaryCreate) {
        DictionaryAppBarNavigatorImpl(onOpenDictionaryCreate)
    }
    NavHost(tabsNavController, "vocabulary") {
        composable("vocabulary") { VocabularyTabScreen(appBarNavigator = appBarNavigator, ...) }
        composable("quiz")       { QuizTabScreen(appBarNavigator = appBarNavigator, ...) }
        composable("statistic")  { StatisticTabScreen(appBarNavigator = appBarNavigator, ...) }
    }
}
```

Если в будущем понадобится контекстная навигация per-таб — разделить на три Impl. Сейчас YAGNI.

**BottomBar** (переключение табов) — Navigator не нужен. Использует `tabsNavController` напрямую через UI events (sealed эффект не требуется — нет cross-graph навигации, просто переключение в одном NavHost).

### Navigator контракты табов

```kotlin
// modules/screen/dictionaryTab — нужен tabsNavController + cross-graph через callback
interface VocabularyNavigator : Navigator {
    fun openWordCard(id: Long)              // internal — tabsNavController
    fun openDictionaryCreate()              // cross-graph — callback от root
}

// modules/screen/quiztab — аналогично
interface QuizTabNavigator : Navigator {
    fun openChat(quizType: String)          // internal
    fun openDictionaryCreate()              // cross-graph
}

// modules/screen/stattab — только AppBar (без своего Navigator beyond Navigator.back)
interface StatisticNavigator : Navigator
// или вообще не нужен — экран не имеет своих навигационных эффектов кроме базовых

// modules/screen/settingstab — internal + cross-graph
interface SettingsNavigator : Navigator {
    fun openAboutApp()                       // internal
    fun openWebView(pageKey: String)         // internal
    fun openDictionaryList()                 // cross-graph — callback от root (openLangManagement)
}
```

---

## Единый план реализации

### Этап 1: Инфраструктура в core/mate

**Только создание новых файлов. Ничего не используется, существующий код не трогается.**

1. Создать `core/mate/MateTypedEffectHandler.kt` — abstract класс с `filter`/`onEffect` (описан в `docs/guides/effect-handlers.md` секция "Typed base паттерн")
2. Создать `core/mate/Navigator.kt`:
   ```kotlin
   interface Navigator {
       fun back()
   }
   ```
3. Создать `core/mate/MateNavigationEffectHandler.kt` — abstract класс наследует `MateTypedEffectHandler<Msg, NavigationEffect>`, обрабатывает `NavigationEffect.Back`, делегируя в `Navigator`

**Удаление `NavigationEffect.ExitApp` — НЕ здесь.** Существующий код использует `ExitApp`, удаление сломает сборку. Удаляется в Этапе 4 после миграции всех root экранов.

✅ **Чекпойнт 1 (Этап 1 завершён):** build + lint + tests должны быть зелёные. Приложение работает как раньше — новая инфраструктура добавлена но никем не используется.

### Этап 2: Миграция экранов (один PR на экран)

Для каждого экрана совмещаем три действия в одном PR:
- DatasourceEffectHandler → `@Inject` + наследует `MateTypedEffectHandler<Msg, E>`
- NavigationEffectHandler → `MateNavigationEffectHandler` + `Navigator`
- Тесты обновляются

**Для уже мигрированных AssistedInject ViewModel'ей** (WordCardViewModel из IS471 этап 3) — две независимые правки одного файла, делать в отдельных PR:
- PR 1: убрать UseCase из конструктора ViewModel, handler через `@Inject`
- PR 2: заменить навигационный callback на Navigator

**Шаги для каждого экрана:**

1. **`build.gradle.kts` модуля** — подключить Dagger + KSP processor (если ещё не подключено):
   ```kotlin
   plugins {
       id("com.google.devtools.ksp")
   }
   dependencies {
       implementation(diLibs.dagger)
       ksp(diLibs.daggerCompiler)
   }
   ```

2. **DatasourceEffectHandler** — добавить `@Inject constructor` И унаследовать `MateTypedEffectHandler<Msg, XxxDatasourceEffect>`:
   - `filter(effect: Effect) = effect as? XxxDatasourceEffect`
   - `onEffect(effect: XxxDatasourceEffect, consumer)` — типизированный, без `else -> Empty`

3. **Navigator интерфейс + EffectHandler** (в screen модуле):
   - `XxxNavigator` — интерфейс наследует `Navigator` (получает `back()`). Для root экрана — добавить `fun exit()`
   - `XxxNavigationEffect` — `sealed interface XxxNavigationEffect : NavigationEffect` (sealed обязательно)
   - `XxxNavigationEffectHandler` — наследует `MateNavigationEffectHandler<Msg, XxxNavigator, XxxNavigationEffect>(navigator)`. Реализует только `onScreenEffect`. Конструктор: `@AssistedInject(@Assisted navigator: XxxNavigator)` + `@AssistedFactory`

4. **NavigatorImpl** (в `app/.../navigation/`) — обычный класс с конструктором `(navController: NavController)`. Реализует все методы. Для root экрана — также принимает `onExit: () -> Unit`. Все `navigate()` с `launchSingleTop = true`

5. **Удалить старый `@IntoMap @ViewModelKey(XxxViewModel::class)` биндинг** из соответствующего DI модуля. Делать ДО шага 6 — иначе граф не соберётся

6. **Добавить getter в AppComponent** — `fun getXxxViewModelFactory(): XxxViewModel.Factory`

7. **ViewModel:**
   - Конструктор: `@Assisted navigator: XxxNavigator` + injected datasource handler + injected handler.Factory
   - В Mate: `navHandlerFactory.create(navigator)` + готовый datasourceHandler
   - Старые callbacks и UseCase из конструктора удалить
   - Удалить ручное создание handlers в init блоке Mate

8. **Composable** (в screen модуле):
   - Параметры: `navigator: XxxNavigator` + ViewModel.Factory
   - Для табов с shared widget — добавить `appBarNavigator: DictionaryAppBarNavigator`
   - `BackHandler { viewModel.accept(Msg.RequestBack) }` — системная back через reducer
   - Убрать старые навигационные callbacks из параметров

9. **Conditional навигация → в reducer** (если есть):
   ```kotlin
   is Msg.RequestBack -> if (state.dictionaries.isEmpty()) {
       state to setOf(ListNavigationEffect.ExitApp)
   } else {
       state to setOf(NavigationEffect.Back)
   }
   ```

10. **Navigation graph (в app):**
    - Через `remember(navController) { XxxNavigatorImpl(navController, onExit?) }` создаёт инстанс и передаёт в screen как `navigator`

11. **Тесты:**
    - `*Test.kt` — заменить моки старого `NavigationEffectHandler(callback)` на моки `Navigator`
    - ViewModel-тесты — моки factories
    - Reducer-тесты — новые ветки conditional навигации

**Особые случаи (из аудита handlers):**
- `quiztab/DatasourceEffectHandler` и `stattab/DatasourceEffectHandler` — пустые sealed interface. **Удалить handler целиком**, не мигрировать
- `quiz/chat/DatasourceEffectHandler` — жёсткий cast без `?`. Миграция попутно фиксит crash
- `dictionary/form/FlagFilterFlowHandler` обрабатывает `DictionaryFormEffect.FilterFlags` из чужого sealed — **вынести FilterFlags в отдельный sealed** до миграции, иначе конфликт

✅ **Чекпойнт 2-N (после каждого экрана):**
- `./gradlew assembleDebug` — зелёный
- `./gradlew :app:lintDebug` — зелёный
- `./gradlew testDebugUnitTest` — зелёный
- Ручной smoke test экрана на устройстве/эмуляторе

### Этап 3: Финальный cleanup

**После миграции всех экранов которые эмитили `NavigationEffect.ExitApp`** (минимум: Splash, DictionaryList).

1. **Инвентаризация `NavigationEffect.ExitApp`:**
   - `grep -r 'NavigationEffect.ExitApp' modules/ app/` — должно вернуть только экраны где это уже было заменено на per-screen sealed
   - Если есть остатки — заменить на соответствующий per-screen `XxxNavigationEffect.ExitApp`
2. **Удалить `NavigationEffect.ExitApp`** из `core/mate/NavigationEffect.kt`
3. Удалить старые `NavigationEffectHandler` классы (ручные, без Dagger) — мёртвый код
4. Удалить старые callback параметры из всех затронутых мест
5. Проверить что нет неиспользуемых импортов
6. Обновить `docs/features-spec/navigation.md` — текущая схема с MateNavigationEffectHandler + Navigator
7. **Обновить `docs/guides/effect-handlers.md`** — заменить пример старого `NavigationEffectHandler` на новую схему
8. Перенести секцию **Cross-graph навигация** из этого плана в `docs/guides/navigation.md`

✅ **Чекпойнт финальный:**
- `./gradlew assembleDebug` — зелёный
- `./gradlew :app:lintDebug` — зелёный
- `./gradlew testDebugUnitTest` — зелёный
- Полный smoke test приложения

### Порядок миграции экранов

| # | Экран | Уровень | Сложность | Зависимости | Особые работы |
|---|-------|---------|-----------|-------------|---------------|
| 1 | DictionaryForm | root | MEDIUM | только Back | Вынести `FilterFlags` в отдельный sealed (FlagFilterFlowHandler — конфликт sealed) |
| 2 | DictionaryList | root | LOW | Back + ExitApp + OpenEdit | — |
| 3 | Splash | root | LOW | OpenMain + ExitApp | — |
| 4 | WordCard | tab nested | LOW | только Back | Уже на @AssistedInject — две отдельные правки (этапы 2 и 3) |
| 5 | Chat | tab nested | LOW | только Back | Fix crash от жёсткого `as` cast в DatasourceEffectHandler |
| 6 | DictionaryAppBar | shared widget | MEDIUM | OpenDictionaryCreate (через callback от root) | — |
| 7 | VocabularyTab | tab | MEDIUM | Back + OpenWordCard + OpenDictionaryCreate | — |
| 8 | QuizTab | tab | MEDIUM | Back + OpenChat + OpenDictionaryCreate | **Удалить пустой `DatasourceEffectHandler`** (вместо миграции) |
| 9 | StatisticTab | tab | LOW | только AppBar | **Удалить пустой `DatasourceEffectHandler`** (вместо миграции) |
| 10 | SettingsTab | tab | MEDIUM | Back + OpenAboutApp + OpenWebView + OpenDictionaryList | — |
| 11 | MainScreen | composite | HIGH | tabsNavController + callbacks для root | — |

**Порядок обоснован:**
- root экраны (1-3) — отрабатываем базовый паттерн на простом
- nested экраны (4-5) — внутри табов, не требуют MainScreen
- AppBar (6) — shared widget, нужен для табов
- табы (7-10) — используют AppBar, должны быть мигрированы до MainScreen
- MainScreen (11) — последним, композирует всё что выше

### Риски

- **Несколько NavController'ов в Lexeme** — Root + Main + nested. Каждый screen работает со своим. Нужно явно передавать правильный
- **ExitApp — только root экраны.** Не базовый эффект. Добавляется в per-screen sealed effect (Splash, DictionaryList). Per-screen Navigator получает `fun exit()`. `NavigatorImpl` принимает `onExit: () -> Unit` callback который дёргает `activity.finish()`. Activity извлекается через `LocalActivity.current` в navigation graph
- **AppBar shared widget** — используется в 3 табах с идентичной навигацией (`openDictionaryCreate` через callback от RootRouter). Один `DictionaryAppBarNavigator` + один `DictionaryAppBarNavigatorImpl(onOpenDictionaryCreate)`. В MainScreen создаётся один инстанс через `remember`, передаётся во все три таба

### Что в итоге

- **Единый паттерн handlers:** все наследуют `MateTypedEffectHandler` — фильтрация чужих эффектов в одном месте
- **DatasourceEffectHandler** через `@Inject` — UseCase'ы исчезают из ViewModel
- **NavigationEffectHandler** через `Navigator` интерфейс + `MateNavigationEffectHandler` — Handler чистый Kotlin, тестируемый
- **Back** обрабатывается один раз в базовом классе. **ExitApp** — только в root экранах через свой sealed effect
- **Conditional навигация** — переезжает из composable в reducer (`Msg.RequestBack` → проверка state)
- **Navigator интерфейсы** — в screen модулях рядом с handler. **NavigatorImpl** — в `app/.../navigation/`
- **ViewModel конструктор:** `@Assisted navigator: XxxNavigator` + injected datasource handler + injected handler.Factory
- **Composable конструктор:** `navigator: XxxNavigator` + ViewModel.Factory (для табов также `appBarNavigator`)
- **Tab экраны:** NavigatorImpl принимает `tabsNavController` + callbacks для root операций. MainScreen инкапсулирован, не знает про rootNavController
- **AppBar shared widget:** один `DictionaryAppBarNavigatorImpl`, передаётся во все 3 таба через `remember`
- **`launchSingleTop = true`** в `NavigatorImpl.navigate()` — попутно фикс бага двойного тапа
- **Compose Navigation зависимость** только в `app` (NavigatorImpl) — screen модули чистые от `androidx.navigation`
