# Сценарные тесты: как это реализовать

План реализации [app_scenario_tests.md](app_scenario_tests.md).
Здесь — честный ответ «что писать кроме сценариев», механика прогона
и этапы.

## Что пишется КРОМЕ сценариев (полный список, один раз на приложение)

### 1. ScreenFactory на каждый экран — главный труд

Сейчас Mate собирается ВНУТРИ ViewModel (`GroupsTabViewModel`:
конструктор Mate с reducer'ом и handler'ами). Харнесу нужна сборка без
ViewModel. Рефакторинг прода (полезный и сам по себе):

```kotlin
// модуль экрана; ЕДИНСТВЕННОЕ место сборки экрана
object WordsTabScreen : ScreenSpec<WordsTabState, Msg> {
    override fun create(deps: ScreenDeps, scope: CoroutineScope) = Mate(
        initState = WordsTabState(),
        reducer = WordsTabReducer(deps.logger),
        effectHandlers = listOf(
            DatasourceEffectHandler(deps.get<WordsTabUseCase>()),
            deps.navHandler,          // nav-handler даёт окружение
        ),
        coroutineScope = scope,
    )
}
```

ViewModel прода делегирует сюда же (`Mate = WordsTabScreen.create(...)`)
— дублей сборки нет. Это НЕ новая логика, а переезд существующих
строк из VM/DI в одно именованное место.

### 2. NavGraph — одна таблица на приложение

Навигация у нас УЖЕ данные (nav-эффекты). Харнесу нужен только маппинг
«эффект → переход»:

```kotlin
val appNavGraph = navGraph {
    on<WordsNavigationEffect.OpenWordCard> { push(WordCardScreen, it.wordId) }
    on<GroupsNavigationEffect.OpenWordCard> { push(WordCardScreen, it.wordId) }
    on<NavigationEffect.Back> { pop() }
}
```

Это формализация того, что сейчас разбросано строками по
`*NavigatorImpl` в app. Требование к стилю приложения (у нас
выполняется): навигация выражается ЭФФЕКТАМИ, не прямыми вызовами
navController из UI.

**Решение (юзер, 2026-08-31): navGraph — единственный источник И для
прода.** Не «таблица для тестов» рядом с продовым кодом (разойдутся),
а инверсия: таблица-данные одна, интерпретаторов два —
- прод: базовый nav-handler читает navGraph, `push(WordCardScreen,
  args)` переводится в `navController.navigate(...)` (Android-код в
  app / тонком android-артефакте);
- харнес: тот же navGraph, `push` = создать Mate экрана в стеке.
Разойтись невозможно по построению. Это ровно кусок v1 О5 («навигация
как данные»); `*NavigatorImpl` и per-экранные Navigator-интерфейсы при
этом худеют/умирают.

**Граница ответственности (важно):** mate НЕ занимается навигацией —
он занимается ДОСТАВКОЙ навигационных намерений. Раннер диспатчит
nav-эффект как любой другой; mate-navigation даёт словарь
(NavigationEffect + navGraph DSL) и безопасную доставку (гейт +
FIFO-очередь О5); а что такое «экран» и как его показать — знает
только интерпретатор (NavController в проде, стек Mate в тестах).
Аналогия: mate не ходит в сеть — он доставляет эффект «сходи в сеть»
исполнителю.

### 3. Стабы use case'ов — в сценарии, но по правилам

Use case'ы уже интерфейсы — шов готов. `stub<T>` в DSL: на JVM — mockk
либо рукописные фейки (решить на Э2; дефолт харнеса — незастабленный
вызов = громкий fail). Флоу-стабы делаем УПРАВЛЯЕМЫМИ:

```kotlin
val terms = stubFlow<List<TermApiEntity>>()   // MutableSharedFlow внутри
stub<WordsTabUseCase> { onFlowTerms returns terms }
...
terms.emit(listOf(term("dom")))   // сценарий СИМУЛИРУЕТ живую БД
expectState<WordsTabState> { termList.size == 1 }
```

— то есть «база изменилась под ногами» (наши любимые live-подписки)
тоже сценарируется.

Итого кроме сценариев: N маленьких ScreenSpec (переезд существующего
кода) + 1 navGraph + стабы в сценариях. Новой ЛОГИКИ — ноль.

## Механика прогона (что делает харнес шаг за шагом)

```
appScenario(registry, appNavGraph) { ... }
 ├─ создаёт TestScope/TestDispatcher, пустой стек экранов
 │
 ├─ launch(WordsTab)
 │   ├─ registry: WordsTabScreen.create(стабы, testScope)
 │   ├─ в сборку подставлен ХАРНЕСНЫЙ nav-handler (вместо продового)
 │   ├─ на Mate повешен RecordingObserver
 │   └─ advanceUntilIdle() — init-эффекты отработали
 │
 ├─ send(Msg.X)
 │   └─ mate.accept + advanceUntilIdle — ВЕСЬ каскад дошёл
 │      (reduce → эффекты → стабы → outcome-Msg → reduce…) — FIFO
 │      mailbox'а гарантирует детерминизм
 │
 ├─ nav-эффект из reducer'а
 │   └─ харнесный nav-handler → navGraph → push(WordCardScreen, args)
 │      → фабрика создаёт Mate карточки → её init-эффекты бегут;
 │      стек: [WordsTab, WordCard]
 │
 ├─ expectState<T> { ... }   — state ВЕРХНЕГО экрана (или screen(X) {})
 ├─ expectEffect(e)          — лента RecordingObserver, курсор вперёд
 ├─ expectScreen(X, args)    — текущий стек харнеса
 ├─ advanceTimeBy(5.seconds) — виртуальное время (тики счётчика)
 ├─ back()                   — NavigationEffect.Back → pop + dispose Mate
 │
 └─ падение любого assert'а → отчёт: шаг сценария + полная лента
    ВСЕХ экранов (Msg/state-диффы/эффекты/каузальность) до момента
    падения
```

## Состав артефакта `mate-app-test`

- `ScreenSpec` / `ScreenDeps` / `ScreenRegistry` — контракт сборки.
- `AppHarness` — стек экранов, создание/dispose Mate, харнесный
  nav-handler (реализует контракт nav-семейства из О3/О5).
- `navGraph { on<E> { push/pop/replace } }` — DSL таблицы переходов.
- `RecordingObserver` — лента поверх MateObserver (О4) с курсором для
  expectEffect и рендером трассы падения.
- `ScenarioScope` — DSL: `launch / send / screen(X){} / expectState /
  expectEffect / expectNoEffect / expectScreen / back /
  advanceTimeBy / stub / stubFlow`.
- Стаб-движок (mockk-обвязка или фейк-генерация — решение Э2).

## Этапы реализации

- **Э1. Прод-рефакторинг сборки:** ScreenFactory/ScreenSpec для
  экранов, VM делегируют. Механика, по одному экрану, без изменения
  поведения (юниты/смоук как регресс).
- **Э2. Ядро mate-app-test:** харнес + navGraph + RecordingObserver +
  ScenarioScope + стаб-движок. TDD на двух фейковых мини-экранах
  внутри библиотеки.
- **Э3. Пилот на PolyTrainer:** registry для wordstab + wordcard +
  groupstab, 3–5 сценариев: «создать слово → карточка», «создать
  группу → добавить слово из карточки → счётчик вырос», «деструктив
  с галкой: тики 5→0 на виртуальном времени», «live-обновление:
  stubFlow эмитит — список перерисовался».
- **Э4. Разгон:** полный registry, перегонка ручников M-кейсов в
  сценарии (кроме UI-специфики), сетка в CI.

## Открытые вопросы (решить на Э1/Э2)

1. **Host-иерархия:** vocabulary — host с вкладками и DictionarySlot,
   не «route». Вариант для старта: моделировать плоско (WordsTab и
   GroupsTab — самостоятельные экраны харнеса, dictionaryId подаётся
   сценарием как Msg). Честную host-модель (экран-в-экране) — позже.
2. **Стаб-движок:** mockk (JVM-only — сценарные тесты и так на
   jvm-таргете, приемлемо) vs рукописные фейки (KMP-чисто, но больше
   кода). Склоняюсь к mockk для старта.
3. **Args экрана:** как фабрика получает параметры (wordId) — через
   ScreenDeps или отдельным параметром create; согласовать с тем, как
   их получает продовая VM (SavedStateHandle).
