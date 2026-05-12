package me.apomazkin.dictionary.list

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.apomazkin.dictionary.DictionaryUseCase
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateFlowHandler
import javax.inject.Inject

class DictionaryListFlowHandler @Inject constructor(
    private val dictionaryUseCase: DictionaryUseCase,
) : MateFlowHandler<DictionaryListMsg, Effect> {

    override var job: Job? = null

    override fun subscribe(scope: CoroutineScope, send: (DictionaryListMsg) -> Unit) {
        job = scope.launch {
            dictionaryUseCase.flowDictionaryList()
                .collectLatest { list ->
                    send(DictionaryListMsg.DictionariesLoaded(list))
                }
        }
    }

    override suspend fun runEffect(
        effect: Effect,
        consumer: (DictionaryListMsg) -> Unit,
    ) {
        // Flow handler — effects handled by DictionaryListEffectHandler
    }
}
