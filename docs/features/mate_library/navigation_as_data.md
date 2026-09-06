# Навигация как данные: что сейчас и что будет

Пояснение к решению «navGraph — единственный источник навигации»
([app_scenario_impl.md](app_scenario_impl.md)). Весь код «сейчас» —
РЕАЛЬНЫЙ, из проекта.

## ЧТО СЕЙЧАС: одно знание, размазанное по пяти файлам

Возьмём одно-единственное знание: **«тап по слову открывает карточку»**.
Проследим его путь по коду:

**Звено 1 — reducer выдаёт эффект** (wordstab/logic):

```kotlin
is Msg.OpenWordCard -> state to setOf(
    WordsNavigationEffect.OpenWordCard(message.wordId)
)
```

**Звено 2 — handler экрана разворачивает эффект в вызов интерфейса**
(wordstab/ui/WordsNavigationEffectHandler.kt):

```kotlin
override suspend fun onScreenEffect(effect: NavigationEffect) {
    when (effect) {
        is WordsNavigationEffect.OpenWordCard ->
            vocabularyNavigator.openWordCard(effect.wordId)
    }
}
```

**Звено 3 — интерфейс навигатора экрана** (wordstab/ui/WordsNavigator):

```kotlin
interface WordsNavigator : Navigator {
    fun openWordCard(wordId: Long)
}
```

**Звено 4 — реализация в app превращает вызов в лямбду**
(app/navigator/WordsNavigatorImpl.kt):

```kotlin
class WordsNavigatorImpl(
    private val onOpenWordCard: (Long) -> Unit,
) : WordsNavigator {
    override fun openWordCard(wordId: Long) = onOpenWordCard(wordId)
}
```

**Звено 5 — откуда берётся лямбда: рождается в nav-графе Compose**
(main/Vocabulary.kt:22) и пробрасывается параметром через
CompositionRoot:

```kotlin
// main/Vocabulary.kt — рождение лямбды при сборке nav-графа:
composable(TabPoint.VOCABULARY.route) {
    compositionRoot.VocabularyHostDep(
        openWordCard = { navController.goToWordCard(it) },   // ← вот она
        ...
    )
}

// app/CompositionRootImpl.kt — проброс в конструктор Impl'а:
val vocabularyNavigator = remember(openWordCard) {
    WordsNavigatorImpl(onOpenWordCard = openWordCard)
}
val groupsNavigator = remember(openWordCard) {
    GroupsNavigatorImpl(onOpenWordCard = openWordCard)  // ТА ЖЕ лямбда —
}                                    // знание уже дублируется по экранам
```

Итого: чтобы ответить на вопрос «куда ведёт OpenWordCard?», надо
пройти ШЕСТЬ файлов в ЧЕТЫРЁХ модулях (эффект → when handler'а →
интерфейс → Impl → CompositionRoot-проброс → лямбда в nav-графе).
А чтобы завести новый переход — потрогать их все. При этом сам
ОТВЕТ — одна строчка смысла: «OpenWordCard → экран карточки».
Остальные звенья — транспорт.

И главное для тестов: конец цепочки — NavController. Он существует
только в Android/Compose; в JVM-тесте цепочка обрывается, харнесу
переиспользовать её нельзя.

## ЧТО БУДЕТ: знание — одной строкой в одном месте

Всё то же знание записывается ДАННЫМИ, в единственной таблице на всё
приложение:

```kotlin
val appNavGraph = navGraph {
    on<WordsNavigationEffect.OpenWordCard> { push(WordCardScreen, it.wordId) }
    on<GroupsNavigationEffect.OpenWordCard> { push(WordCardScreen, it.wordId) }
    on<NavigationEffect.Back> { pop() }
}
```

Читаешь таблицу — видишь ВСЮ навигацию приложения. Новый переход —
одна строка здесь (плюс сам эффект в экране, как и сейчас).

Кто исполняет `push`? Таблица сама ничего не делает — её читают двое:

**Прод** — один общий nav-handler на всё приложение (вместо звеньев
2–5). Получил эффект → нашёл строку в таблице → перевёл push в
NavController:

```kotlin
// android-обвязка, ЕДИНСТВЕННОЕ место с NavController
push = { screen, args -> navController.navigate(screen.route(args)) }
pop  = { navController.popBackStack() }
```

**Тестовый харнес** — тот же nav-handler, та же таблица, но push/pop
другие:

```kotlin
// mate-app-test: НИКАКОГО Android
push = { screen, args -> stack += registry.create(screen, args) }  // новый Mate
pop  = { stack.removeLast().dispose() }
```

## Что меняется по файлам

| Сейчас | Будет |
|---|---|
| Звено 1: эффект в reducer'е | **Без изменений** — эффекты остаются |
| Звено 2: when в handler'е каждого экрана | Умирает — один общий nav-handler |
| Звено 3: Navigator-интерфейс каждого экрана | Умирает |
| Звено 4: NavigatorImpl каждого экрана в app | Умирает |
| Звено 5: лямбды с navController по composition-коду | Сжимаются в ОДНО место (android-интерпретатор push/pop) |
| — | +navGraph: одна таблица на приложение |

## Что это даёт

1. **«Куда ведёт эффект» читается в одном месте** — таблица и есть
   документация навигации.
2. **Прод и тест не могут разойтись:** оба исполняют одну таблицу,
   отличается только «мышца» (NavController vs стек Mate).
3. **Сценарные тесты получают навигацию бесплатно** — без единой
   строчки тестового кода про переходы.
4. Роль mate не меняется: он по-прежнему только ДОВОЗИТ nav-эффект до
   nav-handler'а (с гейтом и очередью из О5). Что такое «экран» — знает
   только исполнитель push/pop.

## Связь с роадмапом

Это не отдельная работа «для тестов»: таблица + общий nav-handler —
ровно содержание v1 О5 («навигация как данные»). Харнес (v2) просто
переиспользует готовое.
