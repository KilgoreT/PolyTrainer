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

```kotlin
import io.github.kilgoret.mate.runMateCatching

override suspend fun runEffect(effect: WordsEffect, consumer: (WordsMsg) -> Unit) {
    when (effect) {
        is WordsEffect.CreateWord -> {
            val msg = runMateCatching {
                withContext(io) { useCase.addWord(effect.value) }
            }.fold(
                onSuccess = { WordsMsg.WordCreated },
                onFailure = { WordsMsg.CreateFailed(effect.value) },
            )
            consumer(msg)
        }
    }
}
```

`runMateCatching` — библиотечный `runCatching`, который пробрасывает
`CancellationException`: отмена корутины (закрытие экрана) — не
ошибка и не должна превращаться в fail-Msg.

Как решить, нужен ли перехват вообще:

- handler ничего не ловит → про отмену не думать, раннер отличит её
  от сбоя сам;
- handler хочет fail-Msg на любой сбой → `runMateCatching`, никакого
  ручного кода про отмену;
- handler ловит конкретные типы (`IOException`, `SQLiteException`) →
  обычный catch, отмена под него не попадает.

Ручной `catch (e: CancellationException) { throw e }` больше не нужен
нигде: широкий перехват делайте через `runMateCatching`.

Непойманное исключение эффекта раннер отдаёт в политику как
`MateError.EffectFailed`; observer получает `onEffectFailed`.

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
`onEffectFailed(effect, error)` и `onSubscriptionError(sub, error)` —
удобно для трасс и метрик независимо от политики
([глава о наблюдаемости](08_observability.md)).

## Отмена — не ошибка

Отмена приходит со смертью scope, то есть с закрытием экрана: раннер,
state и подписки умирают вместе, возвращать Msg некому. Раннер не
считает это ошибкой ни для эффектов, ни для подписок. Успел ли
оборванный запрос примениться в базе — экран не узнает и не должен:
при следующем открытии подписка прочитает фактическое состояние.

---

Дальше: [Наблюдаемость](08_observability.md)
