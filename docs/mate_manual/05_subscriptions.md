# Декларативные подписки

[← Оглавление](README.md)

## Идея

Эффект — разовое действие. Для потоков данных («слушай слова словаря
5, пока вкладка раскрыта») императивные Subscribe/Unsubscribe-эффекты
порождают ручное управление жизнью подписки и подвисшие потоки. Mate
решает это иначе: желаемый набор подписок — чистая функция от State.

```kotlin
import io.github.kilgoret.mate.Sub

/** Семейство подписок экрана — исполняет [WordsSubHandler]. */
sealed interface WordsSub : Sub {
    /** Текущий словарь приложения — жив всё время жизни экрана. */
    data object CurrentDict : WordsSub

    /**
     * Живое окно слов словаря.
     *
     * @param dictionaryId чей список слушаем.
     * @param limit размер окна от головы; «Ещё» = новая подписка
     *   с бо́льшим limit.
     */
    data class Window(val dictionaryId: Long, val limit: Int) : WordsSub
}

/**
 * Декларация «что экран слушает и при каких условиях» — чистая
 * функция от state; раннер диффит набор после каждого reduce.
 */
fun WordsState.subscriptions(): Set<Sub> = buildSet {
    add(WordsSub.CurrentDict)
    dictionaryId?.let { add(WordsSub.Window(it, limit = window)) }
}
```

Раннер после КАЖДОГО reduce диффит набор с активным: новые подписки
стартуют, исчезнувшие гасятся. «Подвисшая подписка» невозможна по
построению; teardown — исчезновение из набора. Initial-дифф идёт от
initState, до init-эффектов.

## Исполнитель семейства

Зеркало effect-handler'а для подписок:

```kotlin
import io.github.kilgoret.mate.MateSubscriptionHandler

/**
 * Исполнитель семейства [WordsSub]: по данным подписки строит поток
 * Msg; чтение потока и его отмену ведёт раннер по диффу.
 */
class WordsSubHandler(
    private val useCase: WordsUseCase,
) : MateSubscriptionHandler<WordsMsg, WordsSub> {

    /** Декларация семейства — как effectFamily, но для подписок. */
    override val subFamily = WordsSub::class

    override fun flow(sub: WordsSub): Flow<WordsMsg> = when (sub) {
        WordsSub.CurrentDict ->
            useCase.flowCurrentDictId().map { WordsMsg.DictionaryChanged(it) }

        is WordsSub.Window ->
            useCase.flowWords(sub.dictionaryId, sub.limit)
                .map { WordsMsg.WordsLoaded(it) }
                .catch { emit(WordsMsg.WordsLoadFailed) }
    }
}
```

Правила роутинга те же и такие же громкие: один handler на семейство,
сирота и двойной матч — fail (`OrphanSubscription` /
`AmbiguousSubscription`).

## Идентичность = equality данных

Подписка — ТОЛЬКО данные (лямбда или Flow-ссылка внутри сломает
equality-дифф). Равная подписка НЕ рестартует; изменился параметр —
это ДРУГАЯ подписка: старая гаснет, новая стартует. Так «Ещё» в
списке — просто `state.copy(window = window + 10)`: дифф сам
перезапустит `Window` с новым лимитом.

### Принудительный рестарт: generation-приём

Рестарт «той же» подписки (retry после ошибки) выражается различающим
полем:

```kotlin
data class Items(val dictionaryId: Long, val generation: Int) : MySub

// reducer на Retry:
state.copy(loadGeneration = state.loadGeneration + 1) to emptySet()
```

Инкремент ломает equality — упавшая подписка гаснет, новая стартует
с теми же параметрами.

## Ошибки потока

Упавший поток гасит ТОЛЬКО свою подписку (без рестарта). Ожидаемые
ошибки ловите на месте — `catch { emit(FailMsg) }` в фабрике потока;
retry — generation-приёмом. Подробно — в главе
[«Обработка ошибок»](07_errors.md).

## Конвенция корреляции

Msg подписки несёт параметры своего Sub (`WindowLoaded(dictionaryId,
words)`): reducer no-op'ит эмиссию, не совпавшую с текущим state, —
защита от гонки на границе рестарта подписки.

## Тикеры

Таймер — тоже подписка: жив, пока условие в state истинно.

```kotlin
is MySub.Countdown -> flow {
    while (true) {
        delay(1_000)
        emit(Msg.Tick)
    }
}
```

Отдельной команды «остановить таймер» нет — снятие условия в state
гасит подписку диффом.

---

Дальше: [Навигация как данные](06_navigation.md)
