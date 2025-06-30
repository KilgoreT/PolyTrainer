package me.apomazkin.dictionaryappbar.mate

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.apomazkin.dictionaryappbar.deps.DictionaryAppBarUseCase
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateFlowHandler

/**
 * Effect
 */
internal sealed interface DatasourceEffect : Effect {
    data class ChangeDict(val dict: DictUiEntity) : DatasourceEffect
}

/**
 * EffectHandler for datastore calls.
 */
internal class DatasourceEffectHandler(
        private val useCase: DictionaryAppBarUseCase,
) : MateFlowHandler<Msg, Effect> {

    override var job: Job? = null


    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        job = scope.launch {
            launch {
                useCase.flowAvailableDict()
                        .debounce { 1000L }
                        .collectLatest { send(Msg.AvailableDict(list = it)) }
            }
            launch {
                useCase.flowCurrentDict()
                        .collectLatest { send(Msg.CurrentDict(current = it)) }
            }
        }
    }

    override suspend fun runEffect(
            effect: Effect,
            consumer: (Msg) -> Unit,
    ) {
        Log.d("##MATE##", "RunEffect: $effect")
        return when (val eff = effect as? DatasourceEffect) {
            is DatasourceEffect.ChangeDict -> {
                withContext(Dispatchers.IO) {
                    useCase
                            .changeDict(numericCode = eff.dict.numericCode)
                    Msg.Empty
                }
            }

            null -> Msg.Empty
        }.let(consumer)
    }
}