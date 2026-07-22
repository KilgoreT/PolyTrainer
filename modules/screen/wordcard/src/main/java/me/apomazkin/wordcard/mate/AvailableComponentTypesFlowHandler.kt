package me.apomazkin.wordcard.mate

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateFlowHandler
import me.apomazkin.wordcard.deps.WordCardUseCase
import javax.inject.Inject

private const val TAG = "ComponentTypesFlow"

/**
 * ЭТАП 0: skeleton. РЕАЛИЗАЦИЯ — этап 5 (§7): init-subscribe() no-op; runEffect на
 * LoadAvailableComponentTypes → (re-)subscribe на flowAvailableComponentTypes(dictId);
 * emit → ComponentTypesLoaded / catch → ComponentTypesLoadFailed; resubscribe отменяет job.
 */
class AvailableComponentTypesFlowHandler @Inject constructor(
    private val useCase: WordCardUseCase,
    private val logger: LexemeLogger,
) : MateFlowHandler<Msg, Effect> {

    override var job: Job? = null
    private var scope: CoroutineScope? = null
    private var send: ((Msg) -> Unit)? = null

    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        this.scope = scope
        this.send = send
    }

    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        val load = effect as? DatasourceEffect.LoadAvailableComponentTypes ?: return
        val scope = scope ?: return
        val send = send ?: return
        job?.cancel()
        // UNDISPATCHED: подписка на flow регистрируется синхронно до возврата runEffect
        // (иначе эмиссия до первого dispatch теряется — §9.4 resubscribe).
        job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                useCase.flowAvailableComponentTypes(load.dictionaryId).collect { available ->
                    send(Msg.ComponentTypesLoaded(available))
                }
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                logger.log(LogLevel.ERROR, TAG, "flowAvailableComponentTypes failed", t)
                send(Msg.ComponentTypesLoadFailed(t))
            }
        }
    }
}
