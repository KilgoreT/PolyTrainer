# Первый экран

[← Оглавление](README.md)

Сквозной пример — экран списка слов: показывает слова, умеет
создавать новое. Дальше этот же экран обрастёт
[подпиской](05_subscriptions.md), [навигацией](06_navigation.md) и
[сценарием](11_harness.md).

## 1. State — что видит экран

```kotlin
/**
 * Состояние экрана списка слов — иммутабельный слепок всего, что
 * видит UI.
 *
 * @param words слова текущего словаря (обновляет живая подписка).
 * @param isLoading первичная загрузка ещё идёт — показать спиннер.
 * @param draft черновик поля ввода нового слова (то, что юзер набрал,
 *   но ещё не отправил кнопкой «создать»).
 */
data class WordsState(
    val words: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val draft: String = "",
)
```

Все UI-флаги — явные поля state, а не вычисления в composable.

## 2. Msg — что может произойти

```kotlin
/** Всё, что может произойти с экраном, — единственный вход в цикл. */
sealed interface WordsMsg {
    /** Эмиссия живой подписки: актуальный список слов целиком. */
    data class WordsLoaded(val words: List<String>) : WordsMsg

    /** Юзер поменял текст в поле ввода нового слова. */
    data class DraftChanged(val value: String) : WordsMsg

    /** Юзер нажал «создать» — черновик пора отправлять в базу. */
    data object CreateClicked : WordsMsg

    /** Итог эффекта: слово создано (список обновит подписка). */
    data object WordCreated : WordsMsg

    /** Итог эффекта: создание упало; [value] — что не сохранилось. */
    data class CreateFailed(val value: String) : WordsMsg
}
```

Msg — ФАКТЫ, не команды: «кликнули», «создано», «загружено».

## 3. Effect — разовые намерения

```kotlin
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.RecoverableEffect

/** Семейство разовых намерений экрана — исполняет [WordsEffectHandler]. */
sealed interface WordsEffect : Effect {
    /**
     * Записать новое слово [value] в базу. Провал записи отвечает
     * [WordsMsg.CreateFailed] — маппинг объявлен здесь, раннер
     * доставит его сам.
     */
    data class CreateWord(val value: String) :
        WordsEffect, RecoverableEffect<WordsMsg> {
        override fun onFail(error: Throwable) = WordsMsg.CreateFailed(value)
    }
}
```

Эффекта «загрузить список» здесь нет. В нашем примере список должен
обновляться при каждом изменении базы, поэтому его источник —
[подписка](05_subscriptions.md). Подключим её в главе 5; там появится
и `WordsMsg.WordsLoaded`.

Подписка нужна не всегда — это выбор по контексту задачи. Данным,
которые достаточно загрузить один раз (справочник, детали по id),
хватит обычного load-эффекта.

Обратите внимание: эффекты одного reduce собираются в `Set` — это
принцип различимых намерений, [подробнее](04_effects.md).

## 4. Reducer — вся логика экрана

```kotlin
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateReducer
import io.github.kilgoret.mate.ReducerResult

/**
 * Чистая тотальная функция update: Msg × State → (State, Effects).
 * Вся логика экрана здесь; IO и исключений нет.
 */
class WordsReducer : MateReducer<WordsState, WordsMsg, Effect> {
    override fun reduce(
        state: WordsState,
        message: WordsMsg,
    ): ReducerResult<WordsState, Effect> = when (message) {
        is WordsMsg.WordsLoaded ->
            state.copy(words = message.words, isLoading = false) to emptySet()

        is WordsMsg.DraftChanged ->
            state.copy(draft = message.value) to emptySet()

        WordsMsg.CreateClicked ->
            state.copy(draft = "") to setOf(WordsEffect.CreateWord(state.draft))

        WordsMsg.WordCreated ->
            state to emptySet() // список обновит живая подписка

        is WordsMsg.CreateFailed ->
            // Вернуть несохранённое слово в поле ввода.
            state.copy(draft = message.value) to emptySet()
    }
}
```

Reducer здесь типизирован корневым `Effect`, а не своим семейством.
Типизировать конкретным (`MateReducer<..., WordsEffect>`) тоже можно,
если у экрана ровно одно семейство эффектов и других не появится.
Но обычно появляется второе — как минимум навигация, — и тогда тип
придётся расширять до корневого с правкой сигнатур и тестов. Корневой
`Effect` с самого начала делает это расширение бесплатным.

## 5. Handler — исполнитель эффектов

```kotlin
import io.github.kilgoret.mate.MateEffectHandler

/**
 * Исполнитель семейства [WordsEffect]: один эффект — один вызов
 * use case, итог возвращается в цикл новым Msg через consumer.
 *
 * @param useCase доступ к данным; интерфейс — шов для стабов в тестах.
 * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
 *   в тестах можно подставить тестовый.
 */
class WordsEffectHandler(
    private val useCase: WordsUseCase,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : MateEffectHandler<WordsMsg, WordsEffect> {

    /**
     * Декларация семейства: по ней раннер строит реестр
     * «семейство → исполнитель» и доставляет сюда каждый эффект,
     * являющийся WordsEffect. Два handler'а на одно семейство —
     * ошибка при создании раннера.
     */
    override val effectFamily = WordsEffect::class

    override suspend fun runEffect(
        effect: WordsEffect,
        consumer: (WordsMsg) -> Unit,
    ) {
        when (effect) {
            is WordsEffect.CreateWord -> {
                withContext(io) { useCase.addWord(effect.value) }
                consumer(WordsMsg.WordCreated)
            }
        }
    }
}
```

Catch в handler'е нет: ожидаемую ошибку эффект объявил сам
(`RecoverableEffect.onFail` в шаге 3) — при исключении раннер
отправит `WordsMsg.CreateFailed` в цикл, и reducer переведёт экран
в состояние ошибки. Отмена корутины (закрытие экрана) ошибкой не
считается. Полная картина — в главе
[«Обработка ошибок»](07_errors.md).

Два приёма, которые окупятся в тестах:
- зависимость — ИНТЕРФЕЙС use case (стабится в сценариях);
- диспатчер блокирующих операций — инъектируемый параметр с дефолтом
  `Dispatchers.IO`: в тестах вместо него подставляется тестовый —
  иначе реальный IO-пул ломает детерминизм виртуального времени.

## 6. Сборка

Сборка живёт в Assembly-объекте ([почему — отдельная глава](09_assembly.md)):

```kotlin
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.Mate
import io.github.kilgoret.mate.MateObserver

/**
 * ЕДИНСТВЕННОЕ место сборки раннера экрана: прод-VM и тест-харнес
 * зовут одну фабрику — сборка не может разойтись.
 */
object WordsAssembly {
    fun create(
        useCase: WordsUseCase,
        io: CoroutineDispatcher = Dispatchers.IO,
        coroutineScope: CoroutineScope,
        observers: List<MateObserver<Any?, Any?, Effect>> = emptyList(),
    ): Mate<WordsState, WordsMsg, Effect> = Mate(
        initState = WordsState(),
        initEffects = emptySet(),
        coroutineScope = coroutineScope,
        reducer = WordsReducer(),
        effectHandlers = listOf(WordsEffectHandler(useCase, io)),
        observers = observers,
    )
}
```

ViewModel — тонкая обёртка:

```kotlin
import io.github.kilgoret.mate.MateStore

/** Тонкая обёртка: даёт раннеру viewModelScope и продовые зависимости. */
class WordsViewModel(useCase: WordsUseCase) :
    ViewModel(), MateStore<WordsState, WordsMsg> {

    private val mate = WordsAssembly.create(
        useCase = useCase,
        coroutineScope = viewModelScope,
    )

    override val state: StateFlow<WordsState> get() = mate.state
    override fun accept(message: WordsMsg) = mate.accept(message)
}
```

## 7. UI

UI читает `state` подпиской и шлёт Msg — больше ему ничего не нужно:

```kotlin
@Composable
fun WordsScreen(holder: MateStore<WordsState, WordsMsg>) {
    val state by holder.state.collectAsStateWithLifecycle()
    // ...
    Button(onClick = { holder.accept(WordsMsg.CreateClicked) }) { /* ... */ }
}
```

---

Дальше: [Эффекты и роутинг](04_effects.md)
