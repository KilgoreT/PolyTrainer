# Обработка ошибок

[← Оглавление](README.md)

## Два класса ошибок

- **Ожидаемые** — часть предметной области: «нет сети», «имя занято»,
  «слово удалено». Выражаются значениями: handler ловит исключение и
  возвращает fail-Msg, reducer переводит экран в состояние ошибки.
  До инфраструктуры ошибок раннера они не доходят.
- **Неожиданные** — баги и невалидная конфигурация. Их раннер
  перехватывает сам и отдаёт в политику ошибок; цикл при этом не
  умирает.

## Ошибки в эффектах

Ожидаемая ошибка эффекта объявляется в самом эффекте — маркером
`RecoverableEffect`:

```kotlin
import io.github.kilgoret.mate.RecoverableEffect

sealed interface WordsEffect : Effect {
    data class CreateWord(val value: String) :
        WordsEffect, RecoverableEffect<WordsMsg> {
        override fun onFail(error: Throwable) = WordsMsg.CreateFailed(value)
    }
}
```

Handler при этом пишет только успешный путь, без catch:

```kotlin
override suspend fun runEffect(effect: WordsEffect, consumer: (WordsMsg) -> Unit) {
    when (effect) {
        is WordsEffect.CreateWord -> {
            withContext(io) { useCase.addWord(effect.value) }
            consumer(WordsMsg.WordCreated)
        }
    }
}
```

Исключение из исполнения ловит раннер: он вызывает `onFail(error)`
эффекта и отправляет полученный Msg в цикл. Observer видит это как
`onEffectRecovered`. Отмена корутины (закрытие экрана) в `onFail` не
попадает — раннер пробрасывает её без recovery.

Правила `onFail`:

- чистый конструктор Msg: только поля эффекта и `error`, никакой
  работы и обращений вовне — метод вызывается на диспатчере раннера;
- восстановительные действия выражаются через цикл: fallback —
  reducer на fail-Msg выпускает новый эффект; retry — счётчик попыток
  кладётся полем эффекта (`Msg.Retry(id, attempt + 1)`);
- маркер — только для одношаговых эффектов: Msg уходит consumer'у
  после завершения всей работы. Эффект с промежуточными эмиссиями
  разбивается на цепочку одношаговых через reducer.

Эффект без маркера при исключении уходит в политику ошибок как
`MateError.EffectFailed`; observer получает `onEffectFailed`.

Точечный перехват конкретных типов (`IOException`,
`SQLiteException`) внутри `runEffect` легален — отмена под него не
попадает. Широкий перехват внутри `runEffect` не пишется; если он
всё же нужен (шаг составной работы, fallback-значение) — только
`runSuspendCatching`: библиотечный `runCatching`, пробрасывающий
`CancellationException`. Голый `runCatching` в suspend-коде запрещён:
он глотает отмену.

`withTimeout` внутри `runEffect`: истёкший таймаут — провал эффекта
(уходит в `onFail` или политику), а не отмена.

## Ошибки в подписках

Ожидаемые ошибки потока ловятся в фабрике потока:

```kotlin
is WordsSub.Window ->
    useCase.flowWords(sub.dictionaryId, sub.limit)
        .map { WordsMsg.WordsLoaded(it) }
        .catch { emit(WordsMsg.WordsLoadFailed) }
```

Непойманное исключение гасит ТОЛЬКО эту подписку (без рестарта) и
уходит в политику как `MateError.SubscriptionFailed`; observer
получает `onSubscriptionError`. Повторный запуск — generation-приёмом
([глава о подписках](05_subscriptions.md)).

## Ошибки в reducer'е

Reducer по контракту не бросает исключений. Если всё же бросил — это
баг: раннер перехватит, state останется прежним, ошибка уйдёт в
политику как `MateError.MessageFailed`.

## Политика ошибок: MateFailPolicy

Все неожиданные ошибки стекаются в одну точку:

```kotlin
import io.github.kilgoret.mate.MateFailPolicy

Mate(
    // ...
    failPolicy = MateFailPolicy { error -> crashReporter.record(error) },
)
```

Дефолт — `MateFailPolicy.Strict`: кидает `MateException`. В debug это
осознанный краш («ошибка конфигурации не живёт молча»), в release
обычно ставят логирующую политику. Цикл раннера не умирает ни при
какой политике — умереть может только процесс от кидающей.

## Справочник MateError

| Ошибка | Когда |
|---|---|
| `MessageFailed(message, cause)` | reducer бросил исключение |
| `MessageRejected(message)` | mailbox не принял Msg (закрыт) |
| `EffectFailed(effect, cause)` | handler бросил непойманное |
| `OrphanEffect(effect)` | эффект без handler'а (семейство не зарегистрировано) |
| `AmbiguousEffect(effect, families)` | эффект подходит двум семействам |
| `SubscriptionFailed(sub, cause)` | поток подписки упал непойманным |
| `OrphanSubscription(sub)` | подписка без handler'а |
| `AmbiguousSubscription(sub, families)` | подписка подходит двум семействам |

Orphan/Ambiguous — ошибки конфигурации: чинятся декларацией семейств,
а не обработкой в рантайме.

## Наблюдаемость ошибок

`MateObserver` видит каждую ошибку в момент возникновения:
`onEffectRecovered(effect, error, message)` — эффект упал и
восстановлен своим fail-Msg, `onEffectFailed(effect, error)` — ошибка
ушла в политику, `onSubscriptionError(sub, error)` — упал поток
подписки. Удобно для трасс и метрик независимо от политики
([глава о наблюдаемости](08_observability.md)).

## Отмена — не ошибка

Отмена приходит со смертью scope, то есть с закрытием экрана: раннер,
state и подписки умирают вместе, возвращать Msg некому. Раннер не
считает это ошибкой ни для эффектов, ни для подписок. Успел ли
оборванный запрос примениться в базе — экран не узнает и не должен:
при следующем открытии подписка прочитает фактическое состояние.

---

Дальше: [Наблюдаемость](08_observability.md)
