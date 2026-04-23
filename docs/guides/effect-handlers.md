# Эффект-хендлеры

## Обзор

Эффект-хендлеры выполняют сайд-эффекты, объявленные редьюсером. Они конвертируют эффекты в новые сообщения, которые возвращаются в TEA-цикл.

## Две категории эффектов

### DatasourceEffect

Операции с БД, сетью, preferences. Всегда на `Dispatchers.IO`.

```kotlin
internal sealed interface DatasourceEffect : Effect {
    data class LoadWord(val wordId: Long) : DatasourceEffect
    data class DeleteWord(val wordId: Long) : DatasourceEffect
    data class SaveWord(val wordId: Long, val value: String) : DatasourceEffect
    data class AddLexeme(val wordId: Long) : DatasourceEffect
}
```

### UiEffect

UI-эффекты: snackbar, toast, диалоги инициированные системой.

```kotlin
internal sealed interface UiEffect : Effect {
    data class ShowSnackbar(val title: String) : UiEffect
}
```

## Паттерн DatasourceEffectHandler

```kotlin
internal class DatasourceEffectHandler(
    private val useCase: WordCardUseCase,
) : MateEffectHandler<Msg, Effect> {

    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        when (val eff = effect as? DatasourceEffect) {
            is DatasourceEffect.LoadWord -> {
                withContext(Dispatchers.IO) {
                    useCase.getTermById(eff.wordId).let { term ->
                        term?.let { Msg.TermLoaded(it) }
                            ?: Msg.TermNotLoaded
                    }
                }
            }
            is DatasourceEffect.DeleteWord -> {
                withContext(Dispatchers.IO) {
                    useCase.deleteWord(eff.wordId).let {
                        Msg.CloseScreen
                    }
                }
            }
            null -> Msg.Empty   // Эффект не для этого хендлера
        }.let(consumer)         // Отправить результат обратно
    }
}
```

**Ключевые паттерны:**
- `effect as? DatasourceEffect` — safe cast, `null` для не своих эффектов
- `null -> Msg.Empty` — фоллбэк для эффектов предназначенных другим хендлерам
- `.let(consumer)` — отправка результирующего сообщения обратно в Mate
- `withContext(Dispatchers.IO)` — переключение на IO для блокирующих операций

## Паттерн UiEffectHandler

Минимальный хендлер, трансформирующий эффекты в сообщения:

```kotlin
internal class UiEffectHandler :
    MateEffectHandler<Msg, Effect> {

    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        return when (val eff = effect as? UiEffect) {
            is UiEffect.ShowSnackbar -> UiMsg.Snackbar(text = eff.title, show = true)
            null -> Msg.Empty
        }.let(consumer)
    }
}
```

## Паттерн FlowHandler (долгоживущие подписки)

Для наблюдения за реактивными стримами (Flow, DataStore):

```kotlin
internal class AppBarFlowHandler(
    private val prefsProvider: PrefsProvider,
) : MateFlowHandler<Msg, Effect> {

    override var job: Job? = null

    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        job = scope.launch {
            combine(
                prefsProvider.getBooleanFlow(EARLIEST_KEY),
                prefsProvider.getBooleanFlow(FREQUENT_MISTAKES_KEY),
                prefsProvider.getBooleanFlow(DEBUG_KEY),
            ) { earliest, mistakes, debug ->
                Msg.UpdateMenu(
                    isEarliestOn = earliest,
                    isFrequentMistakesOn = mistakes,
                    isDebugOn = debug,
                )
            }.collect { send(it) }
        }
    }

    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        // FlowHandler'ы обычно не обрабатывают отдельные эффекты.
        // Их работа — через subscribe().
    }
}
```

**Ключевые паттерны:**
- Реализует `MateFlowHandler` (не просто `MateEffectHandler`)
- `subscribe()` вызывается автоматически при инициализации Mate
- `unsubscribe()` вызывается при `Mate.dispose()` (через ViewModel.onCleared)
- Комбинирует несколько Flow и эмитит сообщения

## Подключение хендлеров

В инициализации ViewModel:

```kotlin
private val stateHolder = Mate(
    initState = ChatScreenState(),
    initEffects = setOf(DatasourceEffect.PrepareToStart),
    coroutineScope = viewModelScope,
    reducer = ChatReducer(logger, resourceManager),
    effectHandlerSet = setOf(
        DatasourceEffectHandler(quizGame, prefsProvider),   // одноразовые эффекты
        AppBarFlowHandler(prefsProvider)                     // долгоживущие flow
    )
)
```

Все хендлеры в сете получают **каждый эффект**. Каждый хендлер проверяет свой тип через safe cast и игнорирует чужие эффекты.

## Маппинг Effect → Message

```
DatasourceEffect.LoadWord     → Msg.TermLoaded / Msg.TermNotLoaded
DatasourceEffect.DeleteWord   → Msg.CloseScreen
DatasourceEffect.SaveWord     → Msg.TermLoading (запускает перезагрузку)
DatasourceEffect.AddLexeme    → Msg.LexemeUpdate
DatasourceEffect.DeleteLexeme → Msg.TermLoading (запускает перезагрузку)
UiEffect.ShowSnackbar         → UiMsg.Snackbar
```

Паттерн: деструктивные операции часто запускают полную перезагрузку (`Msg.TermLoading`) вместо локального патча стейта.

## Конвенции

1. **Sealed interface** для каждой категории эффектов, расширяющий `Effect`.
2. **Эффект + хендлер в одном файле** для DatasourceEffect и UiEffect соответственно.
3. **`null -> Msg.Empty`** фоллбэк в каждом хендлере.
4. **Всегда `withContext(Dispatchers.IO)`** для операций с данными.
5. **FlowHandler для подписок**, обычный хендлер для одноразовых эффектов.
6. **Consumer отправляет результат сразу** — без пост-обработки, без кеширования.
7. **Internal visibility** — эффекты и хендлеры `internal` для feature-модуля.
8. **Без сложной логики в хендлерах** — они выполняют эффект и конвертируют результат в сообщение. Принятие решений остаётся в редьюсере.
