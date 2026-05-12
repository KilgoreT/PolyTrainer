package me.apomazkin.dictionary.form

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.apomazkin.dictionary.DictionaryUseCase
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateFlowHandler
import javax.inject.Inject

class FlagFilterFlowHandler @Inject constructor(
    private val dictionaryUseCase: DictionaryUseCase,
) : MateFlowHandler<DictionaryFormMsg, Effect> {

    override var job: Job? = null

    override fun subscribe(scope: CoroutineScope, send: (DictionaryFormMsg) -> Unit) {
        job = scope.launch {
            dictionaryUseCase.flagsFlow().collectLatest { flags ->
                send(DictionaryFormMsg.FlagsUpdated(flags))
            }
        }
    }

    override suspend fun runEffect(
        effect: Effect,
        consumer: (DictionaryFormMsg) -> Unit,
    ) {
        val filtered = effect as? FlagFilterEffect ?: return
        when (filtered) {
            is FlagFilterEffect.FilterFlags -> dictionaryUseCase.updateFilter(filtered.query)
        }
    }
}
