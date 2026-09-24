# Наблюдаемость

[← Оглавление](README.md)

## MateObserver — полный цикл одним интерфейсом

С mailbox'ом весь цикл проходит через одну точку — там раннер и даёт
наблюдать:

```kotlin
interface MateObserver<in State, in Message, in Effect> {
    fun onMessage(message: Message, stateBefore: State)
    fun onReduced(message: Message, before: State, after: State, effects: Set<Effect>)
    fun onEffectStarted(effect: Effect)
    fun onEffectFinished(effect: Effect, duration: Duration)
    fun onEffectFailed(effect: Effect, error: Throwable)
    fun onCausedMessage(parent: Effect, message: Message)
    fun onSubscriptionStarted(sub: Sub)
    fun onSubscriptionStopped(sub: Sub)
    fun onSubscriptionError(sub: Sub, error: Throwable)
}
```

Все методы с пустыми дефолтами — переопределяйте нужные.

- **Каузальность.** `consumer` handler'а обёрнут раннером: Msg,
  рождённый эффектом, регистрируется с родителем
  (`onCausedMessage`) — по логу читается дерево
  «Msg → эффект (34ms) → Msg → …».
- **Длительность** — `kotlin.time.Duration` от монотонных часов.
- **Read-only by design.** Наблюдатель не вмешивается в цикл;
  исключение в наблюдателе глотается — кривой логгер не имеет права
  убить mailbox.
- **Контравариантность.** Все три типа `in` — один наблюдатель
  `MateObserver<Any?, Any?, Effect>` вешается на раннер ЛЮБОГО экрана.
  Так сценарный харнес пишет единую ленту всех раннеров приложения.

## Свой логгер

```kotlin
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateObserver

/**
 * Пример прикладного логгера цикла: печатает reduce-шаги и
 * каузальность; какие события и как рендерить — решает проект.
 *
 * @param tag префикс строк (grep-якорь ленты этого раннера).
 * @param log куда писать (Logcat, файл, println).
 */
class LoggingObserver(
    private val tag: String,
    private val log: (String) -> Unit,
) : MateObserver<Any?, Any?, Effect> {
    override fun onReduced(message: Any?, before: Any?, after: Any?, effects: Set<Effect>) {
        log("$tag reduce: $message -> effects=$effects")
    }
    override fun onCausedMessage(parent: Effect, message: Any?) {
        log("$tag caused: $parent -> $message")
    }
}

Mate(/* ... */, observers = listOf(LoggingObserver("###WORDS###", ::println)))
```

Что печатать (полный state, счётчики, без PII) — решают ваши
рендеры; observers опциональны и zero-cost без подписчиков.

## Прикладной сахар внутри reduce

Библиотечный observer видит цикл СНАРУЖИ reduce. Если проект хочет
детализировать шаги ВНУТРИ reducer'а (цепочки атомарных экстеншнов с
логом каждого шага) — это прикладная обвязка поверх, библиотека о ней
не знает; общий тег склеивает обе ленты grep'ом.

---

Дальше: [Assembly: сборка экрана](09_assembly.md)
