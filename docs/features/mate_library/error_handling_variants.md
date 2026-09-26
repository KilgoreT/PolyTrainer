# Обработка ошибок эффектов: два варианта развития

Статус: РЕШЕНИЕ ПРИНЯТО 2026-09-25 — в работу идёт вариант 3
(`RecoverableEffect`, метод-форма) + переименование `runMateCatching`
→ `runSuspendCatching`. Вариант 1 сохранён как запасной: если 3 не
приживётся, откат на 1. Документ — источник для будущей переработки
docs/mate_manual (глава 07 об ошибках).

Источник — ревью 2026-09-25 (5 агентов: понятность, adversarial,
бойлерплейт, архитектура/прецеденты, тестируемость).

## Проблема

Сегодня широкая обработка ошибок в эффект-хендлерах держится на
дисциплине: помнить, что голый `runCatching` глотает
`CancellationException`, и писать `runMateCatching`. Забыл — сломана
отмена корутин и ложные fail-Msg при закрытии экрана. Плюс имя
`runMateCatching` не выдаёт, что функция делает.

Отклонённый по ревью вариант — хук `onFailure(effect, error): Message?`
в контракте `MateEffectHandler`: без прецедентов (Elm/TCA решают ошибку
у места объявления эффекта, MVIKotlin/Orbit — статус-кво/глобальная
политика), незащищённый вызов хука ломает гарантию «цикл не умирает»,
для безопасности требует suspend, guard, новый MateError, ограничение
«только одношаговые» — API раздувается ради сахара.

Остались два жизнеспособных варианта. Они не исключают друг друга:
1 — минимальный шаг сейчас, 3 — каноничная механика (может идти сразу
вместо 1 или после него).

---

## Вариант 1. Хелпер в библиотеке, контракт не трогаем

Идея: узаконить и обобщить паттерн `guarded`, который wordcard уже
написал руками. Раннер, контракт хендлера, трасса — без изменений.

### Шаг 1. Переименовать `runMateCatching` → `runSuspendCatching`

Имя перестаёт врать: функция не про mate, а про suspend-код вообще.
Прецедент — библиотека kotlin-result (`runSuspendCatching` с той же
семантикой). Правило запоминается грамматически: «в suspend-коде пиши
`runSuspendCatching` вместо `runCatching`».

### Шаг 2. Добавить хелпер `guardEffect` (имя обсуждаемо)

```kotlin
/**
 * Исполняет тело эффекта; исключение (кроме отмены) маппится в
 * Message и уходит consumer'у. Отмена пробрасывается.
 */
public inline fun <Message> guardEffect(
    consumer: (Message) -> Unit,
    failMessage: (Throwable) -> Message,
    block: () -> Unit,
) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        consumer(failMessage(e))
    }
}
```

### Шаг 3. Миграция хендлеров

Было (groupstab, типовая ветка):

```kotlin
is RemoveGroup -> runMateCatching { useCase.remove(effect.id) }
    .onSuccess { consumer(Msg.Removed(effect.id)) }
    .onFailure { consumer(Msg.MutationFailed(effect.id)) }
```

Стало:

```kotlin
is RemoveGroup -> guardEffect(consumer, { Msg.MutationFailed(effect.id) }) {
    useCase.remove(effect.id)
    consumer(Msg.Removed(effect.id))
}
```

Happy-path и fail-ветка в одной конструкции, отмена обработана
хелпером. Throwable доступен в `failMessage` — кейсы вида
`ImpactPreviewFailed(effect.typeId, e)` выразимы.

### Шаг 4. Detekt-правило в PolyTrainer

Запрет голого `runCatching` в файлах `*EffectHandler.kt` — машина
ловит забывчивость вместо ревьюера.

### Оценка

| | |
|---|---|
| Изменения библиотеки | rename + один хелпер, ~20 строк |
| Изменения раннера/трассы | нет |
| Миграция приложения | механическая, по одному хендлеру |
| Футган с отменой | закрыт хелпером, но дисциплина «использовать хелпер» остаётся (страхует detekt) |
| Контракт ошибки | не виден ни в типе эффекта, ни в контракте — живёт внутри тела хендлера |

---

## Вариант 3. Fail-Msg в объявлении эффекта (`RecoverableEffect`)

Идея (Elm-канон): ответ на провал — знание владельца эффекта, а не
исполнителя. Эффект сам объявляет, каким Message ответить на своё
падение. Elm: `Task.attempt (Result -> Msg) task` — команда несёт
конструктор сообщения об исходе, рантайм лишь доставляет.

Форма — МЕТОД эффекта, не поле и не лямбда:
- лямбда-поле (`val onFail: (Throwable) -> Msg`) отвергнуто: лямбды
  не равны друг другу → equality data class'а разваливается → Set
  и дедупликация мертвы (П1);
- val-поле (`val onFail: Msg`) отвергнуто в пользу метода: метод
  получает Throwable (кейсы вида `ImpactPreviewFailed(typeId, e)`
  выразимы), а методы в `equals` не участвуют — Set цел. И это
  дословный Elm: `Task.attempt` принимает именно функцию.

### Шаг 1. Маркер-интерфейс в библиотеке

Новый файл `mate-core/src/commonMain/kotlin/io/github/kilgoret/mate/RecoverableEffect.kt`
(рядом с `Effect.kt`):

```kotlin
package io.github.kilgoret.mate

public interface RecoverableEffect<out Message> : Effect {
    /**
     * Построить Message о провале; раннер вызывает при исключении
     * из исполнения эффекта (кроме отмены). Чистый конструктор Msg:
     * никакой работы, никаких обращений вовне.
     */
    public fun onFail(error: Throwable): Message
}
```

Контракт `MateEffectHandler` не меняется.

### Шаг 2. Ветка в раннере

Файл `mate-core/src/commonMain/kotlin/io/github/kilgoret/mate/Mate.kt`,
класс `Mate`, приватный метод `dispatchEffect` (сегодня — строки
~195–220). Метод целиком, как он будет выглядеть; новое помечено
`// НОВОЕ`:

```kotlin
private fun dispatchEffect(effect: E) {
    val handler = resolveEffectHandler(effect) ?: return
    coroutineScope.launch {
        notifyObservers { onEffectStarted(effect) }
        val startMark = TimeSource.Monotonic.markNow()
        try {
            @Suppress("UNCHECKED_CAST")
            (handler as MateEffectHandler<Message, E>).runEffect(effect) { message ->
                notifyObservers { onCausedMessage(effect, message) }
                accept(message)
            }
            notifyObservers { onEffectFinished(effect, startMark.elapsedNow()) }
        } catch (error: CancellationException) {
            // Отмена scope (закрытие экрана) — не ошибка эффекта:
            // пробрасывается, recovery НЕ вызывается.
            throw error
        } catch (error: Throwable) {
            // НОВОЕ: recovery-Msg эффекта, объявившего ответ на провал.
            // null в двух случаях: эффект не Recoverable, либо onFail
            // сам упал (его исключение прикрепляется к исходному).
            @Suppress("UNCHECKED_CAST")
            val recovery: Message? =
                (effect as? RecoverableEffect<Message>)?.let { recoverable ->
                    try {
                        recoverable.onFail(error)
                    } catch (recoveryError: Throwable) {
                        error.addSuppressed(recoveryError)
                        null
                    }
                }
            if (recovery != null) {
                notifyObservers {
                    onEffectRecovered(effect, error, recovery)
                    onCausedMessage(effect, recovery)
                }
                accept(recovery)
            } else {
                // Прежний путь: ошибка уходит в политику.
                notifyObservers { onEffectFailed(effect, error) }
                failPolicy.onError(MateError.EffectFailed(effect, error))
            }
        }
    }
}
```

### Шаг 3. Хук наблюдателя

`MateObserver.onEffectRecovered(effect, error, message)` — «упал, но
восстановился» отличим в трассе от «упал, ушёл в политику».
`onCausedMessage` зовётся дополнительно — каузальный граф не рвётся,
recovery-Msg не «из ниоткуда».

### Шаг 4. Использование в приложении (реальные файлы)

**Кейс 1 — ошибка в Msg не нужна.** Модуль
`modules/screen/groupstab`, файл `logic/Message.kt`. Сегодня эффект
(строка 127) и fail-Msg (строка 87) не связаны — связь живёт в
хендлере через `runMateCatching`:

```kotlin
// logic/Message.kt:127 — сегодня:
data class DeleteGroup(val groupId: Long) : GroupsEffect

// logic/DatasourceEffectHandler.kt:69 — сегодня:
is GroupsEffect.DeleteGroup -> withContext(io) {
    runMateCatching { useCase.deleteGroup(effect.groupId) }
        .onSuccess { /* Msg по результату */ }
        .onFailure { consumer(Msg.GroupMutationFailed) }
}
```

Станет — связь объявлена в самом эффекте, параметр ошибки
игнорируется:

```kotlin
// logic/Message.kt:127:
data class DeleteGroup(val groupId: Long) :
    GroupsEffect, RecoverableEffect<Msg> {
    override fun onFail(error: Throwable) = Msg.GroupMutationFailed
}

// logic/DatasourceEffectHandler.kt — голый happy-path:
is GroupsEffect.DeleteGroup -> withContext(io) {
    useCase.deleteGroup(effect.groupId)
    /* Msg по результату */
}
```

**Кейс 2 — ошибка нужна в Msg.** Модуль
`modules/screen/per_dictionary_components`, файлы
`mate/DatasourceEffect.kt` (эффект, строка 32), `mate/Msg.kt`
(fail-Msg с полем `cause`, строка 88), `mate/DatasourceEffectHandler.kt`
(зеркальный when в catch, строки 154–178):

```kotlin
// mate/DatasourceEffect.kt:32 — сегодня:
data class LoadImpact(val typeId: ComponentTypeId) : DatasourceEffect

// mate/DatasourceEffectHandler.kt:158 — сегодня, ветка в catch:
is DatasourceEffect.LoadImpact ->
    Msg.ImpactPreviewFailed(effect.typeId, e)
```

Станет:

```kotlin
// mate/DatasourceEffect.kt:32:
data class LoadImpact(val typeId: ComponentTypeId) :
    DatasourceEffect, RecoverableEffect<Msg> {
    override fun onFail(error: Throwable) =
        Msg.ImpactPreviewFailed(typeId, error)
}
```

Когда так мигрированы все 8 эффектов файла — из
`DatasourceEffectHandler.kt` удаляется весь `catch (e: Exception)` со
вторым when (строки 149–179) и обёртка try; лог `logger.e("Effect
failed…")` заменяет единый лог observer'а на `onEffectRecovered`.
Reducer'ы не меняются нигде.

Тест маппинга — прямой вызов, без раннера и моков:

```kotlin
@Test fun deleteGroupFailsToGroupMutationFailed() {
    assertEquals(
        Msg.GroupMutationFailed,
        GroupsEffect.DeleteGroup(5).onFail(IOException()),
    )
}
```

### Шаг 5. Контракт-тесты библиотеки

Минимум: recoverable-эффект упал → onFail отредьюсен, recovered+caused
в трассе, политика молчит; обычный эффект упал → как сейчас; отмена →
ни recovery, ни политики; `onFail` сам бросил → цикл жив, политика
получает EffectFailed с suppressed-ошибкой конструктора; recovery-Msg
встаёт в хвост FIFO.

### Что даёт

- Контракт ошибки виден в типе эффекта и тестируется прямым вызовом
  `effect.onFail(error)` — чистая функция, без раннера и моков.
- Throwable доступен маппингу — кейсы с ошибкой в Msg выразимы без
  точечных catch.
- Хендлеры — happy-path по построению: ловить нечем и незачем, футган
  с отменой исчезает классом, а не дисциплиной.
- Нет второго when и потери exhaustiveness.
- Set-семантика цела: методы в equality data class'а не участвуют.
- Nothing-хендлеры (навигация): их эффекты не реализуют маркер —
  запрет recovery выражен типом.
- Ретраи получают счётчик естественно: `onFail(e) = Msg.Retry(id,
  attempt + 1)` — эффекты попыток не равны, зацикливание видно в
  данных.

### Ограничения — полный разбор

Вывод разбора: блокеров нет. №1 — принцип TEA, замаскированный под
ограничение; №2 — теоретическая потеря с дешёвыми обходами; №3 —
дисциплина однострочника (страховка на библиотеке); №4 — одно
предложение в мануале; №5 — внутренность раннера, уже присутствующая
в коде.

**1. Только одношаговые эффекты — и это принцип, а не ограничение.**

Определение: эффект одношаговый, если ни один Msg не уходит в
consumer до завершения всей работы, способной упасть. Сделал →
отправил результат → конец. Весь текущий код PolyTrainer такой:
даже хендлеры с несколькими операциями юзкейса внутри (per_dictionary_
components) собирают Msg и зовут consumer один раз в конце.

Почему многошаговому эффекту нельзя маркер — гипотетический
«сохранить и синхронизировать»:

```kotlin
is SaveAndSync -> {
    useCase.save(effect.wordId)
    consumer(Msg.Saved(effect.wordId))   // Msg уже в цикле ← шаг
    useCase.syncRemote(effect.wordId)    // ← упало здесь
}
```

`Msg.Saved` отредьюсен («сохранено ✓»), затем recovery доставит
`Msg.SaveFailed` («ошибка сохранения») — состояние противоречиво.
Откатить ушедший Msg нечем: mailbox не транзакция. `onFail` не знает,
на каком шаге упало — он один на весь эффект.

Лекарство — декомпозиция, и она канонический TEA: два одношаговых
эффекта (`Save` → `Msg.Saved` → reducer выпускает `Sync`), у каждого
свой честный onFail. Каждая развилка «что дальше» живёт в чистом
reducer'е (видна в трассе, тестируется без моков), handler остаётся
тупым исполнителем. Многошаговость внутри эффекта = оркестрация
уползла в грязный код.

Две легальные оговорки: (а) несколько Msg разом В КОНЦЕ — не
многошаговость (wordcard шлёт burst из двух Msg после успешной
вставки: один шаг работы, составной результат); (б) длящийся
источник — это не Effect, это Subscription, граница проведена типами.

Бонус против варианта 1: хелпер многошаговость молча позволяет,
маркер подталкивает к правильной декомпозиции.

**2. Маппинг прибит к типу эффекта — теоретическая потеря.**

Метод объявлен в классе один раз: `DeleteGroup` всегда отвечает
`GroupMutationFailed`, откуда бы его ни выпустили. С val-полем reducer
мог бы задавать fail-Msg в месте выпуска. Гипотетика, где это жмёт:
`LoadSlice` при старте экрана (провал → полноэкранная ошибка) и при
pull-to-refresh (провал → тихий снекбар) — одно намерение, два
желаемых ответа.

Обходы: (а) режим в данные эффекта — `LoadSlice(dictId, trigger)`,
onFail ветвится по trigger: equality цела, трасса видит режим;
(б) два типа эффекта (`InitialLoad`/`RefreshSlice`) — часто честнее:
разные ответы на провал = разные намерения. Практическая цена сегодня
ноль: во всех 28 fail-ветках PolyTrainer маппинг один на тип.

**3. `onFail` обязан быть тупым чистым конструктором Msg.**

Взял поля эффекта, взял error, собрал Msg, вернул. Никакой работы.
Три причины: (а) вызывается в catch раннера на диспатчере scope — для
экранов это Main: тяжёлое/блокирующее вешает UI; (б) не-suspend
НАМЕРЕННО — delay/fallback-запрос физически не написать;
восстановительная работа по определению новый эффект, а не побочка
маппинга; (в) падение onFail страхуется guard'ом библиотеки
(suppressed → политика, цикл жив), но recovery при этом не состоится —
чем меньше кода, тем меньше шансов на баг.

Куда девать «умную» реакцию — через цикл: fallback («сеть упала —
возьми кэш») = onFail → `Msg.LoadFailed` → reducer выпускает
`LoadFromCache`; retry с backoff = `onFail(e) = Msg.RetryLoad(attempt
+ 1)` → reducer проверяет лимит → эффект следующей попытки (пауза —
Delay-эффектом). Ограничение перенаправляет логику в reducer, где ей
место; guard — забота библиотеки, юзер о нём не думает.

**4. Msg-тип в объявлении эффекта — цена: предложение в мануале.**

Философское возражение «намерение не должно знать адресата ответа».
Практика: (а) Msg и Effect физически соседи — в groupstab оба в одном
файле logic/Message.kt; связь между ними и так существует через
`ReducerResult<State, Effect>`; новых модульных зависимостей и циклов
сборки нет; (б) эффекты экранно-локальны — в чужой раннер с чужим Msg
не попадают по архитектуре; (в) Elm-канон именно таков: команда — это
`Cmd Msg`, тип сообщения — параметр команды с рождения языка.
Единственная потеря — формулировка «эффект = намерение без адресата»
в 02_concepts станет менее абсолютной.

**5. Unchecked cast в раннере — внутренняя неизбежность.**

`(effect as? RecoverableEffect<Message>)` с suppress: дженерики
стёрты, JVM проверит только интерфейс, совпадение Message-типов
компилятор гарантировать не может. Безопасно контрактно: эффект
попадает в dispatchEffect единственным путём — из reduce этого же
раннера (или initEffects), где тип E прибит дженериком
`Mate<State, Message, E>`; сборка экранно-локальна (Assembly), эффект
с чужим Msg в раннер не заносится. Прецедент в том же методе:
`(handler as MateEffectHandler<Message, E>)` строкой выше и второй в
diffSubscriptions — приём для family-роутинга со стёртыми дженериками
неизбежен и уже прожит. Наружу suppress не протекает.

### Оценка

| | |
|---|---|
| Изменения библиотеки | маркер + ветка dispatchEffect с guard + хук + тесты, ~80 строк |
| Миграция приложения | opt-in: только эффекты с fail-Msg (28 веток в 4 хендлерах), остальные 6 хендлеров не трогаются |
| Футган с отменой | исчезает по построению (для мигрированных эффектов) |
| Контракт ошибки | в типе эффекта, Throwable доступен, тестируем прямым вызовом |

---

## Сопутствующее (независимо от выбора)

- Баг текущего `dispatchEffect` (нашёл adversarial): `withTimeout`
  внутри runEffect бросает `TimeoutCancellationException` — наследника
  `CancellationException`; он уходит в ветку проброса мимо политики и
  наблюдателей, эффект исчезает бесследно. Чинить: различать
  реальную отмену (`coroutineContext.isActive`) от таймаута, либо
  задокументировать «таймауты ловить внутри runEffect».
- `runMateCatching` → `runSuspendCatching` — уместно при любом
  варианте (при 3 функция остаётся нишевой утилитой для suspend-кода
  вне хендлеров).
