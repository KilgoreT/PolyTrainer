# Навигация

[← Оглавление](README.md)

## Какую проблему решает модуль

Обычный способ навигации в Android-приложении: экрану при создании
передают либо сам NavController, либо колбэк вида
`onOpenWordCard: (Long) -> Unit`, и обработчик клика его вызывает.
У этого способа три хронические проблемы:

1. **Знание «куда ведёт клик» размазано по экранам.** Каждый экран
   держит свой набор колбэков; общей карты переходов приложения не
   существует — она восстанавливается чтением всех экранов подряд.
2. **Навигацию не проверить тестом без UI.** Переход происходит
   внутри вызова NavController — чтобы убедиться, что «клик по слову
   открывает карточку», нужен инструментальный тест или руки.
3. **Вызов в неудачный момент — краш.** NavController запрещает
   `navigate()` когда экран не на переднем плане; переход, случившийся
   через секунду после сворачивания приложения (ответ сети → навигация),
   роняет процесс (`IllegalStateException: State must be at least
   CREATED`).

Модуль mate-navigation решает все три одним ходом: **переход — это
данные**. Экран не вызывает ничего — его reducer возвращает объект
«хочу открыть карточку» (обычный эффект mate), а дальше конвейер
библиотеки сам превращает намерение в реальный вызов NavController:
по общей таблице переходов (п.1), проверяемо в обычных юнит-тестах
(п.2) и только когда приложение готово к переходу (п.3).

## Путь одного нажатия

Полный путь клика «открыть карточку слова» через конвейер:

```
reducer: Msg.WordClick → эффект OpenWordCard(42)
    ↓ (family-роутинг раннера)
MateNavigationHandler.runEffect
    ↓ (резолв по таблице)
NavTable: OpenWordCard → [Push(WordCardScreen(42))]
    ↓ (FIFO-очередь + гейт готовности)
NavigationExecutor.execute(Push(...))
    ↓ (платформа)
NavController.navigate("word_card/42")
```

Кроме трёх проблем из вступления конвейер решает и четвёртую, менее
очевидную: **порядок**. Два перехода, выпущенные подряд или из разных
раннеров, проходят через одну очередь — стек собирается в порядке
намерений при любых гонках.

Дальше — классы конвейера, по одному на шаг, в порядке пути эффекта.

## Шаг 1. Объявляю намерение — навигационный эффект

Пишу навигацию экрана списка слов: клик по слову должен открыть
карточку. Начинаю там же, где любая логика в mate, — с эффекта.
Библиотека даёт базовое семейство `NavigationEffect` (mate-core,
наследник `Effect`; внутри уже есть один готовый эффект —
`NavigationEffect.Back`, «закрыть текущий экран»). Свои переходы
объявляю подгруппой внутри него:

```kotlin
import io.github.kilgoret.mate.NavigationEffect

/** Навигационные намерения экрана списка слов. */
sealed interface WordsNavigationEffect : NavigationEffect {
    /** Открыть карточку слова [wordId]. */
    data class OpenWordCard(val wordId: Long) : WordsNavigationEffect
}
```

И возвращаю из reducer'а как обычный эффект:

```kotlin
is WordsMsg.WordClick ->
    state to setOf(WordsNavigationEffect.OpenWordCard(message.wordId))

WordsMsg.CloseClicked ->
    state to setOf(NavigationEffect.Back)
```

На этом код экрана ЗАКОНЧЕН. Ни NavController, ни колбэков — reducer
вернул данные «хочу на карточку 42» и забыл. Дальше вопрос: кто эти
данные получит?

## Шаг 2. Кто получает эффект — общий handler семейства

Как любой эффект, навигационный доставляется по family-роутингу:
раннер ищет в своей сборке handler, задекларировавший семейство
`NavigationEffect`. Такой handler в библиотеке один —
`MateNavigationHandler`, и у него две особенности:

1. **Он один на всё приложение.** Навигация — глобальный ресурс: стек
   экранов один, значит и очередь переходов должна быть одна. Поэтому
   создаётся один инстанс и кладётся в `effectHandlers` КАЖДОЙ сборки
   экрана.
2. **Он не отвечает сообщениями.** Тип —
   `MateEffectHandler<Nothing, NavigationEffect>`: переход не рождает
   Msg. Благодаря `Nothing` (ковариантность по Message) один и тот же
   инстанс совместим с раннером любого экрана, какие бы Msg-типы у
   них ни были.

Забыл положить handler в сборку — эффект-«сирота», громкий
`MateError.OrphanEffect` при первом же клике. Family-правило одно:
семейством декларируется ТОЛЬКО базовый `NavigationEffect`, подгруппы
вроде `WordsNavigationEffect` — нет (вложенные family запрещены,
[глава об эффектах](04_effects.md)).

Эффект дошёл до handler'а. Что он с ним делает?

## Шаг 3. Эффект превращается в команды — таблица NavTable

Handler сам не знает, куда ведёт `OpenWordCard`, — это знание
приложения. Оно записывается в одном месте, таблице переходов:

```kotlin
import io.github.kilgoret.mate.navigation.navTable

val appNavTable = navTable {
    on<WordsNavigationEffect.OpenWordCard> { push(AppScreen.WordCard(it.wordId)) }
    on<NavigationEffect.Back> { pop() }
}
```

Одна строка — один переход: «такой эффект → такие команды». Внутри
`on<E> { … }` лямбда получает сам эффект (`it` — отсюда аргументы) и
записывает команды `push(экран)`/`pop()`. Обычно команда одна;
несколько — составной переход, исполнится по порядку.

Здесь впервые появляются два новых слова, и оба — просто данные:

**Команда** — язык из двух слов, во что таблица переводит эффект:

```kotlin
sealed interface NavCommand {
    data class Push(val screen: Screen) : NavCommand  // положить экран на стек
    data object Pop : NavCommand                      // снять текущий
}
```

**Экран** (`Screen`) — пункт назначения команды `Push`. Для
библиотеки это пустой маркер «объект-экран, внутри аргументы»;
приложение объявляет свои:

```kotlin
import io.github.kilgoret.mate.navigation.Screen

sealed interface AppScreen : Screen {
    data object Main : AppScreen
    data class WordCard(val wordId: Long) : AppScreen
}
```

Почему маркер пустой — станет ясно на шаге 5: что такое экран
физически (route, фрагмент, узел теста), библиотека не решает.

Правила резолва эффекта в таблице:

- сначала ищется строка ТОЧНОГО класса эффекта;
- нет — единственная строка-СУПЕРТИП (так регистрируют подгруппу
  целиком: `on<WordsNavigationEffect> { … }`);
- два подходящих супертипа — ошибка конфигурации.

Ошибки громкие: дубль строки на один эффект — fail при создании
графа; эффект без маршрута — fail при диспатче (в `MateFailPolicy`
раннера-отправителя). Опечатка в таблице не живёт молча.

### Кто и где создаёт таблицу

Таблицу пишете вы — одну на всё приложение, и живёт она в
**app-модуле** (композиционном корне). Это не соглашение вкуса, а
следствие зависимостей: строка таблицы связывает эффект feature-модуля
с экраном приложения, то есть автор таблицы должен видеть ВСЕ
feature-модули разом — а видит их только app.

Раскладка по модулям в многомодульном приложении:

- **feature-модуль экрана** объявляет свои nav-эффекты у себя
  (`WordsNavigationEffect` лежит в модуле wordstab) — ему для этого
  нужен только mate-core; про чужие экраны и про таблицу он не знает;
- **app-модуль** держит: перечень экранов (`AppScreens.kt` — sealed
  `AppScreen`), таблицу (`AppNavTable.kt` — top-level
  `val appNavTable = navTable { … }`) и исполнителя.

Живой фрагмент таблицы PolyTrainer
(`app/src/main/java/…/navigation/AppNavTable.kt`) — эффекты семи
разных модулей сведены в один файл, который читается сверху вниз как
документация всех переходов приложения:

```kotlin
val appNavTable: NavTable = navTable {
    on<NavigationEffect.Back> { pop() }

    // Сплэш: первичный выбор маршрута.
    on<SplashNavigationEffect.OpenDictionarySetup> { push(AppScreen.DictionarySetup) }
    on<SplashNavigationEffect.OpenMainScreen> { push(AppScreen.Main) }

    // Слово → карточка (вкладки «Слова» и «Группы»).
    on<WordsNavigationEffect.OpenWordCard> { push(AppScreen.WordCard(it.wordId)) }
    on<GroupsNavigationEffect.OpenWordCard> { push(AppScreen.WordCard(it.wordId)) }
    // …
}
```

Новый переход в приложении = один эффект в своём модуле + одна строка
здесь. Создаётся таблица один раз (обычный top-level `val`) и
передаётся в handler при сборке (шаг 4) — никакой регистрации в
рантайме, никаких «модуль сам добавляет свои маршруты»: вся картина
переходов намеренно собрана в одном читаемом месте.

Итак, эффект стал списком команд. Когда и кем они исполнятся?

## Шаг 4. Когда исполнять — очередь и гейт

Команды нельзя исполнять сразу: приложение может быть в фоне
(NavController упадёт), а два перехода могли прийти одновременно
(нужен порядок). Поэтому внутри `MateNavigationHandler` между
таблицей и исполнением стоят очередь и гейт:

```kotlin
MateNavigationHandler(
    graph = appNavTable,            // таблица (шаг 3)
    executor = executor,            // исполнитель (шаг 5)
    readiness = executor.readiness, // гейт: StateFlow<Boolean> «мир готов?»
    staleness = 5.seconds,          // необязательный срок годности перехода
    onDropped = { log(it) },        // наблюдаемость отброшенных
)
```

Жизнь эффекта внутри handler'а, по порядку:

1. `runEffect` резолвит эффект по таблице СРАЗУ — маршрут проверяется
   на диспатче, а не когда-нибудь потом;
2. готовые команды встают во внутреннюю FIFO-очередь без лимита. Всегда,
   даже при открытом гейте: очередь и есть сериализация — «navigate(A);
   navigate(B)» соберут стек в порядке намерений при любых гонках;
3. с другого конца очередь разбирает дренаж-цикл — корутина,
   запущенная `attach(scope)`. Перед КАЖДЫМ элементом она ждёт
   открытый гейт (`readiness.first { it }`), затем отдаёт команды
   исполнителю. Гейт перечитывается между элементами не из
   осторожности, а по необходимости: исполненный переход сам меняет
   мир — пошла анимация, старый экран ушёл ниже STARTED, гейт мог
   захлопнуться результатом предыдущей же команды;
4. `staleness` (опционально): элемент, прождавший в очереди дольше
   срока, отбрасывается с вызовом `onDropped` — после часа в фоне
   пачка устаревших переходов юзеру, скорее всего, не нужна. `null` —
   переходы не устаревают.

Свойства, которые из этого следуют:

- переход в закрытый гейт НЕ теряется — ждёт открытия и исполняется;
- `detach()` останавливает дренаж, но очередь и даже вычитанный,
  недоисполненный элемент переживают его — следующий `attach`
  продолжит ровно с него;
- повторный `attach` заменяет предыдущий дренаж (пересоздание хоста).

Остался последний вопрос: кто физически делает переход?

## Шаг 5. Кто исполняет — NavigationExecutor

Единственное место всей цепочки, знающее платформу:

```kotlin
fun interface NavigationExecutor {
    fun execute(command: NavCommand)
}
```

Handler отдаёт сюда команды по одной. Что такое `Push(AppScreen
.WordCard(42))` физически — решает реализация: у приложения это
адаптер над NavController, у тестового харнеса — стек узлов с
раннерами. Именно поэтому `Screen` из шага 3 — пустой маркер:
превращением объекта-экрана в route/фрагмент/узел владеет исполнитель,
и только он.

Живой пример — продовый исполнитель PolyTrainer
(`app/src/main/java/…/navigation/AppNavigationExecutor.kt`, сокращён).
Вся платформенная грязь — route-строки, `launchSingleTop`, `popUpTo`,
два NavController'а (root-стек и стек табов), выход из приложения —
собрана здесь и только здесь:

```kotlin
class AppNavigationExecutor(private val logger: LexemeLogger) : NavigationExecutor {

    private var rootNavController: NavHostController? = null
    private var tabsNavController: NavHostController? = null
    private var finishApp: (() -> Unit)? = null

    override fun execute(command: NavCommand) {
        logger.d(tag = LogTags.NAV, message = "execute: $command")
        when (command) {
            is NavCommand.Push -> push(command.screen as AppScreen)
            NavCommand.Pop -> pop()
        }
    }

    private fun push(screen: AppScreen) {
        val root = rootNavController ?: return
        val tabs = tabsNavController ?: return
        when (screen) {
            // ===== root-стек =====
            AppScreen.DictionarySetup ->
                root.navigate(RootPoint.DICTIONARY_SETUP.route) {
                    launchSingleTop = true
                    popUpTo(RootPoint.SPLASH.route) { inclusive = true }
                }

            AppScreen.Main -> openMainScreen(root)

            AppScreen.ExitApp -> finishApp?.invoke()

            // ===== tabs-стек =====
            is AppScreen.WordCard ->
                tabs.navigate(MainRoutes.wordCard(screen.wordId)) {
                    launchSingleTop = true
                }
            // …остальные экраны — по той же схеме
        }
    }
}
```

Что здесь видно:

- `execute` — тупой диспетчер из двух веток; интеллект — в `push`;
- `screen as AppScreen` — легальный каст: исполнитель приложения по
  определению знает перечень экранов приложения;
- один логический `Push` может разворачиваться в сложный вызов
  (`popUpTo`, `singleTop`, выбор из двух контроллеров) — это знание
  платформы, и таблица переходов о нём не подозревает;
- «экран» `ExitApp` исполняется вообще без навигации — `finish()`
  активности: исполнитель волен трактовать Push как угодно;
- контроллеры — nullable и привязываются снаружи (`bind`/`unbind` из
  композиции, шаг 6): умерла композиция — исполнитель безопасно
  «глохнет», а команды копятся в очереди handler'а.

Один `fun interface` — это и есть весь порт на платформу; смена
навигационной библиотеки означает замену одной реализации (раздел
«Другие навигационные библиотеки» ниже).

## Шаг 6. Сборка в приложении

Продовая обвязка из PolyTrainer, по ролям:

**Исполнитель-синглтон** (`AppNavigationExecutor`) держит слабые места
Android при себе: живые `NavController`'ы привязываются/отвязываются
(`bind`/`unbind`) из композиции, lifecycle хоста поднимает/роняет
`hostStarted`; из этого исполнитель сам вычисляет свой
`readiness: StateFlow<Boolean>` — «есть контроллеры И хост STARTED».

**Хендлер-синглтон** создаётся один раз (DI-модуль):

```kotlin
val navigationHandler = MateNavigationHandler(
    graph = appNavTable,
    executor = executor,
    readiness = executor.readiness,
)
```

и кладётся в сборку каждого экрана
(`Assembly.create(navigationHandler = …)`).

**Старт дренажа** — один раз на процесс (`App.onCreate`):

```kotlin
appComponent.getNavigationHandler().attach(processScope)
```

**Привязка целей** — из композиции корневого роутера:

```kotlin
DisposableEffect(navController, tabsNavController) {
    executor.bind(navController, tabsNavController, finishApp)
    onDispose { executor.unbind() }
}
```

Итог: композиция может умирать и пересоздаваться, приложение — уходить
в фон; намерения навигации копятся в очереди и исполняются, когда мир
снова готов.

## Шаг 7. Навигация в тестах

Сценарный харнес использует ТОТ ЖЕ `appNavTable`, а исполнителем
ставит свой стек узлов: `Push` поднимает узел экрана с его раннерами,
`Pop` снимает. Поэтому сценарий проверяет навигацию ассертами:

```kotlin
runAppScenario(registry, appNavTable) {
    launch(AppScreen.Main)
    send(WordsMsg.WordClick(42))
    expectScreen(AppScreen.WordCard(42))
    back()
    expectScreen(AppScreen.Main)
}
```

Разойтись прод и тест не могут: таблица одна ([глава о харнесе](11_harness.md)).

## Другие навигационные библиотеки

Весь платформенный код навигации сосредоточен в двух местах:
`NavigationExecutor.execute(NavCommand)` и источник
`readiness: StateFlow<Boolean>`. Смена навигационной библиотеки — это
реализация одного `fun interface` и выбор, откуда брать готовность;
эффекты, таблица, хендлер, очередь и тесты не меняются.

Статус проверенности:

| Исполнитель | Статус |
|---|---|
| navigation-compose (`NavController`) | проверено в бою (PolyTrainer) |
| Стек узлов харнеса | проверено (сценарные тесты mate-app-test) |
| Voyager / Decompose / FragmentManager | эскизы ниже, НЕ проверялись |

Эскиз для Voyager (`cafe.adriel.voyager`):

```kotlin
class VoyagerExecutor(private val navigator: Navigator) : NavigationExecutor {
    override fun execute(command: NavCommand) = when (command) {
        is NavCommand.Push -> navigator.push((command.screen as VoyagerScreenMap).toVoyager())
        NavCommand.Pop -> { navigator.pop(); Unit }
    }
}
// readiness: у Voyager навигатор живёт в композиции — гейт из
// lifecycle хоста, как в AppNavigationExecutor.
```

Эскиз для Decompose (`StackNavigation<Config>`):

```kotlin
class DecomposeExecutor(
    private val navigation: StackNavigation<Config>,
) : NavigationExecutor {
    override fun execute(command: NavCommand) = when (command) {
        is NavCommand.Push -> navigation.push((command.screen as Config))
        NavCommand.Pop -> navigation.pop()
    }
}
// readiness: Decompose переживает конфигурационные смены сам — гейт
// может быть MutableStateFlow(true) навсегда, очередь остаётся
// полезной для сериализации.
```

Эскиз для FragmentManager:

```kotlin
class FragmentExecutor(private val fm: FragmentManager) : NavigationExecutor {
    override fun execute(command: NavCommand) = when (command) {
        is NavCommand.Push -> fm.commit { replace(R.id.host, command.screen.toFragment()); addToBackStack(null) }
        NavCommand.Pop -> fm.popBackStack()
    }
}
// readiness: fm.isStateSaved == false && хост STARTED — тот же класс
// проблем, что у NavController, гейт обязателен.
```

Общая схема адаптации: (1) свой маппинг `Screen` → сущность библиотеки
(route/воyager-screen/config/fragment); (2) `execute` из двух веток;
(3) источник readiness — либо lifecycle-гейт (библиотеки, падающие от
вызова в неправильном состоянии), либо константа true (библиотеки,
безопасные всегда). Ограничение текущего языка команд: `Push`/`Pop` —
достаточно для стековой навигации; сценарии вроде «replace»/«popUpTo»
потребуют расширения `NavCommand` (осознанно не добавлены, пока нет
потребителя).

---

Дальше: [Обработка ошибок](07_errors.md)
