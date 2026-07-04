package me.apomazkin.components_manager.mate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.apomazkin.components_manager.LogTags
import me.apomazkin.components_manager.deps.ComponentsManagerUseCase
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateFlowHandler
import javax.inject.Inject

/**
 * Подписка на `useCase.flowAllUserDefinedTypes()`. Стартует через `subscribe()`
 * на init Mate; emit'ы → `Msg.TypesLoaded(snapshot)`. Любая ошибка collect
 * → `Msg.TypesLoadFailed`.
 *
 * Parity pattern: `QuizPickerFlowHandler` (`modules/screen/quiz/chat/.../QuizPickerFlowHandler.kt`).
 */
class AllUserDefinedTypesFlowHandler @Inject constructor(
    private val useCase: ComponentsManagerUseCase,
    private val logger: LexemeLogger,
) : MateFlowHandler<Msg, Effect> {

    override var job: Job? = null

    /** F163: scope сохраняется при первом `subscribe()` для возможности re-подписки на retry. */
    private var scope: CoroutineScope? = null
    private var send: ((Msg) -> Unit)? = null

    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        if (effect is DatasourceEffect.LoadAllUserDefinedTypes) {
            // F163: re-subscribe — отменяем существующую job и стартуем новую.
            val s = scope ?: return
            unsubscribe()
            subscribe(s, send ?: consumer)
        }
    }

    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        this.scope = scope
        this.send = send
        job = scope.launch {
            useCase.flowAllUserDefinedTypes()
                .catch { e ->
                    logger.e(
                        tag = LogTags.ALL_COMPONENTS,
                        message = "flow failed: ${e.message}",
                    )
                    send(Msg.TypesLoadFailed(e))
                }
                .collectLatest { snapshot ->
                    send(Msg.TypesLoaded(snapshot))
                }
        }
    }
}
