# Навигация как данные

[← Оглавление](README.md)

## Граница ответственности

Mate НЕ занимается навигацией — он занимается ДОСТАВКОЙ навигационных
намерений, как не ходит в сеть, а доставляет эффект «сходи в сеть»
исполнителю. Что такое «экран» и как его показать — знает только
исполнитель команд (NavController в проде, стек раннеров в харнесе).

## Словарь

- **`NavigationEffect`** (mate-core) — базовое семейство навигационных
  эффектов; `NavigationEffect.Back` доступен всем экранам. Экраны
  доопределяют свои подгруппами:

```kotlin
import io.github.kilgoret.mate.NavigationEffect

/** Навигационные намерения экрана слов — подгруппа базового семейства. */
sealed interface WordsNavigationEffect : NavigationEffect {
    /** Тап по слову → карточка слова [wordId]. */
    data class OpenWordCard(val wordId: Long) : WordsNavigationEffect
}
```

- **`Screen`** (mate-navigation) — маркер пункта назначения, только
  данные (аргументы перехода внутри):

```kotlin
import io.github.kilgoret.mate.navigation.Screen

/** Экраны приложения — пункты назначения таблицы, аргументы внутри. */
sealed interface AppScreen : Screen {
    /** Карточка слова [wordId]. */
    data class WordCard(val wordId: Long) : AppScreen

    /** Главный экран с вкладками. */
    data object Main : AppScreen
}
```

- **`NavCommand`** — `Push(screen)` / `Pop`; **`NavigationExecutor`** —
  исполнитель этих команд.

## Таблица: одна на приложение

ВСЯ навигация записывается строками `navGraph` — читается сверху вниз
как документация переходов; новый переход = одна строка:

```kotlin
import io.github.kilgoret.mate.navigation.NavGraph
import io.github.kilgoret.mate.navigation.navGraph

val appNavGraph: NavGraph = navGraph {
    on<NavigationEffect.Back> { pop() }
    on<WordsNavigationEffect.OpenWordCard> { push(AppScreen.WordCard(it.wordId)) }
    on<QuizNavigationEffect.OpenChat> { push(AppScreen.ChatQuiz(it.quizType)) }
}
```

Матч — по конкретному классу эффекта (точный класс побеждает
строку-супертип); дубль строки — fail при создании, эффект без
маршрута — громкий fail при диспатче. Таблицу читают ДВА
интерпретатора — прод и тест-харнес — разойтись они не могут по
построению.

## Единый nav-handler

Один shared-инстанс на всё приложение (навигация — глобальный ресурс:
стек один, значит очередь переходов одна). Кладётся в `effectHandlers`
каждого раннера; типизирован `Nothing` по Message — совместим с любым
экраном.

```kotlin
import io.github.kilgoret.mate.navigation.MateNavigationHandler

val navigationHandler = MateNavigationHandler(
    graph = appNavGraph,
    executor = appExecutor,          // ваш адаптер NavController
    readiness = executor.readiness,  // гейт готовности
)
navigationHandler.attach(applicationScope) // дренаж очереди
```

### Гейт и очередь

Известный класс крашей — `navigate()` после ухода приложения в фон.
Handler решает его контрактом: все nav-эффекты идут через FIFO-очередь
(даже при открытом гейте — очередь и есть сериализация:
`navigate(A); navigate(B)` соберут back stack в порядке намерений);
перед каждым элементом дренаж ждёт открытый гейт `readiness`
(в Android его обновляет lifecycle: STARTED = true). Навигация в закрытый
гейт не исполняется и НЕ теряется — ждёт открытия и исполняется
накопленной пачкой. Гейт перечитывается между элементами: исполненный
переход может сам его захлопнуть.

Очередь без лимита — переходы не теряются. Опциональная
staleness-политика (`staleness: Duration`, `onDropped`) отбрасывает
переходы, прождавшие дольше срока годности (после долгого фона пачка
устаревших переходов может быть нежеланна).

`attach(scope)` запускает дренаж, `detach()` останавливает; очередь и
вычитанное намерение переживают detach — пересоздание хоста ничего
не теряет.

## Исполнитель в проде

Единственное место приложения, знающее про NavController:

```kotlin
import io.github.kilgoret.mate.navigation.NavCommand
import io.github.kilgoret.mate.navigation.NavigationExecutor

/**
 * Исполнитель навигации: переводит NavCommand с данными экрана в
 * вызовы NavController'ов. Больше NavController не знает никто.
 */
class AppNavigationExecutor : NavigationExecutor {
    override fun execute(command: NavCommand) = when (command) {
        is NavCommand.Push -> push(command.screen as AppScreen)
        NavCommand.Pop -> pop()
    }
    // push(screen) — when по экранам: navigate(route), popUpTo,
    // singleTop; pop() — контекстные политики стеков.
}
```

Экранные политики вхождения в стек (какой контроллер, popUpTo,
схлопывание) собраны здесь же — таблица остаётся простой.

## Что умирает

С таблицей и единым handler'ом из проекта уходят: per-экранные
Navigator-интерфейсы, их Impl'ы в app, per-экранные nav-handler'ы и
навигационные лямбды-пропсы через композицию. Эффекты в reducer'ах —
остаются, это их единственный след.

---

Дальше: [Обработка ошибок](07_errors.md)
