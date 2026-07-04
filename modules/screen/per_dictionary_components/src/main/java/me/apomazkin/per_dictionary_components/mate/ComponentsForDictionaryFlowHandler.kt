package me.apomazkin.per_dictionary_components.mate

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateFlowHandler
import me.apomazkin.per_dictionary_components.LogTags
import me.apomazkin.per_dictionary_components.deps.PerDictionaryComponentsUseCase

/**
 * Подписка на `useCase.flowComponentsForDictionary(dictionaryId)`. Стартует через
 * `subscribe()` на init Mate; emit'ы → `Msg.ItemsLoaded(snapshot)`. Любая ошибка
 * collect → `Msg.ItemsLoadFailed`.
 *
 * `dictionaryId` приходит через `@Assisted` — ViewModel создаёт handler через factory
 * и передаёт его в `effectHandlerSet`. См. business_design_tree.md #44 финальная форма.
 *
 * Parity pattern: `AllUserDefinedTypesFlowHandler` (CM Mate) + `QuizPickerFlowHandler`.
 */
class ComponentsForDictionaryFlowHandler @AssistedInject constructor(
    @Assisted private val dictionaryId: Long,
    private val useCase: PerDictionaryComponentsUseCase,
    private val logger: LexemeLogger,
) : MateFlowHandler<Msg, Effect> {

    override var job: Job? = null

    /** F163: scope сохраняется при первом `subscribe()` для возможности re-подписки на retry. */
    private var scope: CoroutineScope? = null
    private var send: ((Msg) -> Unit)? = null

    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {
        if (effect is DatasourceEffect.LoadComponentsForDictionary) {
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
            useCase.flowComponentsForDictionary(dictionaryId)
                .catch { e ->
                    logger.e(
                        tag = LogTags.DICT_COMPONENTS,
                        message = "flow failed: ${e.message}",
                    )
                    send(Msg.ItemsLoadFailed(e))
                }
                .collectLatest { snapshot ->
                    send(Msg.ItemsLoaded(snapshot))
                }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(dictionaryId: Long): ComponentsForDictionaryFlowHandler
    }
}
