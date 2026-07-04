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
 * IS481 phase 2: подписка на `useCase.flowDictionaries()` для multi-dict scope picker
 * в Create-диалоге. Стартует через `subscribe()` на init Mate; emit'ы →
 * `Msg.DictionariesLoaded(list)`.
 *
 * Error в подписке → `Msg.DictionariesLoaded(emptyList())` (MVP best-guess —
 * chip-list скроется; degrade'им к Global only).
 *
 * Parity pattern: [AllUserDefinedTypesFlowHandler] (F163 re-subscribe через
 * [DatasourceEffect.SubscribeDictionaries]).
 */
class DictionariesFlowHandler @Inject constructor(
    private val useCase: ComponentsManagerUseCase,
    private val logger: LexemeLogger,
) : MateFlowHandler<Msg, Effect> {

    override var job: Job? = null

    private var scope: CoroutineScope? = null
    private var send: ((Msg) -> Unit)? = null

    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        if (effect is DatasourceEffect.SubscribeDictionaries) {
            val s = scope ?: return
            unsubscribe()
            subscribe(s, send ?: consumer)
        }
    }

    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        this.scope = scope
        this.send = send
        job?.cancel()
        job = scope.launch {
            useCase.flowDictionaries()
                .catch { e ->
                    logger.e(
                        tag = LogTags.ALL_COMPONENTS,
                        message = "flowDictionaries failed: ${e.message}",
                    )
                    send(Msg.DictionariesLoaded(emptyList()))
                }
                .collectLatest { list ->
                    send(Msg.DictionariesLoaded(list))
                }
        }
    }
}
