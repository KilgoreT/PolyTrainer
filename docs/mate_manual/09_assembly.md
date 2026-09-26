# Assembly: сборка экрана

[← Оглавление](README.md)

## Проблема

Если Mate собирается внутри ViewModel, у сборки два хозяина: прод (VM
+ DI) и сценарный харнес (которому VM не нужна — ему нужен раннер на
тестовом scope со стабами). Дубль сборки неизбежно разъезжается.

## Паттерн

**Единственное место сборки** — объект `XxxAssembly` в модуле экрана:

```kotlin
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.MateObserver
import io.github.kilgoret.mate.NavigationEffect

/**
 * ЕДИНСТВЕННОЕ место сборки раннера экрана слов. Прод (ViewModel) и
 * сценарный харнес зовут одну и ту же фабрику — собранный экран не
 * может разойтись между ними; харнес лишь подставляет стабы
 * use case'ов, свой nav-handler, наблюдателя ленты и тестовый
 * диспатчер.
 */
object WordsAssembly {
    /**
     * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
     *   в тестах можно подставить тестовый.
     */
    fun create(
        useCase: WordsUseCase,
        logger: AppLogger,
        navigationHandler: MateEffectHandler<Nothing, NavigationEffect>,
        io: CoroutineDispatcher = Dispatchers.IO,
        coroutineScope: CoroutineScope,
        observers: List<MateObserver<Any?, Any?, Effect>> = emptyList(),
    ): Mate<WordsState, WordsMsg, Effect> = Mate(
        initState = WordsState(),
        initEffects = emptySet(),
        coroutineScope = coroutineScope,
        reducer = WordsReducer(logger),
        effectHandlers = listOf(
            WordsEffectHandler(useCase, logger, io),
            navigationHandler,
        ),
        subscriptions = { it.subscriptions() },
        subscriptionHandlers = listOf(WordsSubHandler(useCase)),
        observers = observers,
    )
}
```

Конвенция сигнатуры: зависимости-интерфейсы → nav-handler → экранные
параметры (wordId и т.п.) → `io` → `coroutineScope` → `observers`.

ViewModel делегирует и становится тонкой:

```kotlin
import io.github.kilgoret.mate.MateStore
import io.github.kilgoret.mate.navigation.MateNavigationHandler

/**
 * Тонкая обёртка: сборка живёт в [WordsAssembly], VM даёт только
 * viewModelScope и продовые зависимости из DI.
 */
class WordsViewModel @AssistedInject constructor(
    useCase: WordsUseCase,
    navigationHandler: MateNavigationHandler,
    logger: AppLogger,
) : ViewModel(), MateStore<WordsState, WordsMsg> {

    private val mate = WordsAssembly.create(
        useCase = useCase,
        logger = logger,
        navigationHandler = navigationHandler,
        coroutineScope = viewModelScope,
    )

    override val state get() = mate.state
    override fun accept(message: WordsMsg) = mate.accept(message)
}
```

## Правила

- **Handler'ы создаются в Assembly руками** обычными конструкторами;
  DI-фабрики handler'ов не нужны (инжектятся только исходные
  зависимости — use case'ы, логгер, shared nav-handler).
- **Это не новая логика** — переезд существующих строк из VM/DI в одно
  именованное место; поведение бит-в-бит.
- **`observers` и `io` с дефолтами** — прод их не замечает, харнес
  подставляет ленту и тестовый диспатчер.
- **Никаких Android-типов** в сигнатуре create — иначе харнес на JVM
  её не позовёт.

---

Дальше: [Юнит-тесты раннера](10_testing.md)
