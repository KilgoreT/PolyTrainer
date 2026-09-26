# Юнит-тесты раннера

[← Оглавление](README.md)

## Три слоя тестов экрана

1. **Reducer** — чистая функция: обычные юниты без корутин вообще.
   Подаёте state+Msg, ассертите новый state и `Set` эффектов.
2. **`subscriptions(state)`** — тоже чистая функция: state → набор
   подписок. Проверяются условия попадания каждой подписки в набор.
3. **Раннер целиком** (reducer + handler'ы + подписки) — здесь нужен
   `mate-test`.

## runMateTest

Вечный mailbox-цикл должен жить в scope, чьи задачи гонит виртуальное
время, и умереть по концу теста. `runMateTest` даёт готовый
`mateScope`:

```kotlin
import io.github.kilgoret.mate.test.runMateTest

@Test
fun screenLoadsOnInit() = runMateTest { mateScope ->
    val mate = WordsAssembly.create(
        useCase = stubUseCase,
        io = StandardTestDispatcher(testScheduler), // без реального IO!
        coroutineScope = mateScope,
    )

    mate.accept(WordsMsg.CreateClicked)
    testScheduler.advanceUntilIdle()

    assertEquals(listOf("dom"), mate.state.value.words)
}
```

## Ловушки

- **`backgroundScope` не подходит** для раннера: его задачи при
  активном теле теста не исполняются `advanceUntilIdle` — раннер
  молча не работает. Поэтому `runMateTest` и существует.
- **Реальный `Dispatchers.IO` в handler'ах ломает детерминизм**:
  каскад уходит с виртуального времени на реальный пул, ассерты
  гоняются с гонками. Всегда подставляйте тестовый диспатчер через
  `io`-параметр Assembly.
- **Вечные тикеры + `advanceUntilIdle`** — бесконечная прокрутка
  времени. В тестах раннера с тикером двигайте время явно:
  `advanceTimeBy(...)` + `runCurrent()`. (Сценарный харнес решает это
  за вас — его `awaitIdle` времени не мотает.)
- Типизируйте Mate и reducer КОРНЕВЫМ `Effect`, как в проде, — иначе
  variance-ошибки на списках handler'ов.

---

Дальше: [Сценарный харнес](11_harness.md)
