# Сценарный харнес

[← Оглавление](README.md)

## Что это

`mate-app-test` гоняет бизнес-логику ВСЕГО приложения — все раннеры и
живую навигацию — на JVM, в виртуальном времени, за миллисекунды.
Мокаются только use case'ы. Пишутся только сценарии: вся обвязка
(registry экранов) описывается один раз.

```kotlin
import io.github.kilgoret.mate.apptest.runAppScenario

@Test
fun `open word card for dead word auto-closes back to main`() {
    coEvery { wordCardUseCase.getTermById(42L) } returns null

    runAppScenario(registry, appNavGraph) {   // ПРОДОВАЯ таблица навигации
        launch(AppScreen.Main)

        send(WordsMsg.OpenWordCard(42L))      // тап по слову

        expectScreen(AppScreen.Main)          // карточка открылась и сама
    }                                         // закрылась: слова нет → Back
}
```

## Узловая модель

Элемент стека харнеса — не «экран-раннер», а **узел = набор
co-located раннеров**: на реальном «экране» одновременно живут host,
вкладки, виджеты. Плоский экран — частный случай узла с одним
раннером; иной модели нет, поэтому любая топология приложения
описывается одинаково.

`send(msg)` доставляет сообщение раннеру ВЕРХНЕГО узла по семейству
сообщений (какому корневому Msg-типу принадлежит объект) — та же
табличная идея, что у роутинга эффектов; сирота и двусмысленность —
громкий fail. `expectState<S>` находит раннер узла по типу state.

## Registry: один раз на приложение

Фабрики узлов зовут ТЕ ЖЕ Assembly, что прод, — со стабами вместо
базы:

```kotlin
import io.github.kilgoret.mate.apptest.RunnerSlot
import io.github.kilgoret.mate.apptest.ScreenNode
import io.github.kilgoret.mate.apptest.screenRegistry

/**
 * Registry сценариев: «класс экрана → фабрика узла». Пишется один раз
 * на приложение; use case'ы приходят стабами из конкретного теста.
 */
fun registry(words: WordsUseCase, groups: GroupsUseCase) = screenRegistry {
    on<AppScreen.Main> { screen, context ->
        ScreenNode(
            screen = screen,
            runners = listOf(
                RunnerSlot("words", WordsMsg::class, WordsAssembly.create(
                    useCase = words,
                    navigationHandler = context.navigationHandler,
                    io = context.io,
                    coroutineScope = context.scope,
                    observers = listOf(context.observer),
                )),
                RunnerSlot("groups", GroupsMsg::class, GroupsAssembly.create(/* ... */)),
            ),
        )
    }
    on<AppScreen.WordCard> { screen, context ->
        ScreenNode(screen, listOf(RunnerSlot("wordcard", WordCardMsg::class,
            WordCardAssembly.create(wordId = screen.wordId, /* ... */))))
    }
}
```

`HarnessContext` даёт фабрике всё обязательное: `scope` узла (pop
экрана отменяет его — раннеры и подписки узла умирают),
`observer` (единая лента), `navigationHandler` (харнесный). Тестовый
диспатчер для `io`-параметров достаётся из scope:

```kotlin
import io.github.kilgoret.mate.apptest.HarnessContext
import kotlin.coroutines.ContinuationInterceptor

/** Тестовый диспатчер сценария — для io-параметров Assembly. */
val HarnessContext.io: CoroutineDispatcher
    get() = scope.coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
```

## Навигация — та же таблица

Харнес поднимает ПРОДОВЫЙ `MateNavigationHandler` с ПРОДОВЫМ
`navGraph`; отличается только исполнитель команд: Push создаёт узел через
registry и кладёт на стек, Pop снимает и гасит. Прод и тест не могут
разойтись по построению. `back()` сценария — системная «назад».

## DSL сценария

| Шаг | Что делает |
|---|---|
| `launch(screen)` | стартовый узел на стек |
| `send(msg)` | доставить Msg + доработать весь каскад |
| `back()` | снять верхний узел |
| `advanceTimeBy(5.seconds)` | сдвинуть виртуальное время (тикеры) |
| `awaitIdle()` | доработать готовое (после ручных эмиссий stubFlow) |
| `expectState<S> { ... }` | ассерты над state раннера верхнего узла |
| `expectScreen(screen)` | верх стека экранов |
| `expectEffect(e)` / `expectNoEffect(e)` | лента эффектов, курсор строго вперёд |

Каждый шаг-действие дорабатывает каскад целиком (reduce → эффекты →
outcome-Msg → reduce → дифф подписок) — детерминизм даёт FIFO
mailbox'а. Время НЕ мотается само: спящие тикеры будит только явный
`advanceTimeBy`.

## Живая база: stubFlow

```kotlin
import io.github.kilgoret.mate.apptest.stubFlow

val membership = stubFlow<List<MembershipEntry>>()
every { groupsUseCase.membershipSlice(any()) } returns membership
// ...в сценарии:
membership.emit(listOf(entry1, entry2))   // база изменилась — подписка получила данные
awaitIdle()
expectState<GroupsTabState> { assertEquals(2, it.allNode?.count) }
```

`stubFlow` (replay = 1) ведёт себя как живой запрос к базе: поздний
подписчик получает последнее значение, эмиссии будят подписки экранов.

## Виртуальное время

```kotlin
send(GroupsMsg.ToggleDeleteWords)          // галка запустила тикер
advanceTimeBy(5.seconds)                    // 5 тиков за 0 реальных мс
expectState<GroupsTabState> { assertEquals(0, it.confirmDelete?.countdownLeft) }
advanceTimeBy(10.seconds)                   // тикер погашен диффом —
expectState<GroupsTabState> { assertEquals(0, it.confirmDelete?.countdownLeft) }
```

## Падение = полная трасса

Любой упавший ассерт дополняется пронумерованной лентой ВСЕХ событий
сценария (Msg, state-диффы, эффекты, каузальность, подписки, вехи
навигации) с маркером курсора — падение самодиагностично:

```
java.lang.AssertionError: Effect CreateWord(value=dom) not found after cursor 0

=== harness trace (7 events, cursor=0) ===
>[0] SubscriptionStarted(sub=CurrentDict)
 [1] ScreenPushed(screen=Main)
 [2] EffectStarted(effect=LoadTermFlow(pattern=))
 ...
```

## Что харнес не видит

Склейки, живущие в Compose-композиции (лямбды между раннерами через
UI), для харнеса невидимы. Лечение — не иметь таких склеек: живые
данные каждая часть экрана слушает сама подпиской; тогда узел
описывает экран честно и полностью.

---

[← Оглавление](README.md)
