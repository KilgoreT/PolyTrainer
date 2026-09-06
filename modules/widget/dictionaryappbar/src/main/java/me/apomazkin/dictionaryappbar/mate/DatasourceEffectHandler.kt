package me.apomazkin.dictionaryappbar.mate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.apomazkin.dictionaryappbar.deps.DictionaryAppBarUseCase
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.MateFlowHandler
import me.apomazkin.mate.LogTags
import me.apomazkin.logger.LexemeLogger
import javax.inject.Inject

sealed interface DatasourceEffect : Effect {
    data class ChangeDict(val dict: DictUiEntity) : DatasourceEffect
}

class DatasourceEffectHandler @Inject constructor(
        private val useCase: DictionaryAppBarUseCase,
        private val logger: LexemeLogger,
) : MateFlowHandler<Msg>,
        MateEffectHandler<Msg, DatasourceEffect> {

    override val effectFamily = DatasourceEffect::class

    override var job: Job? = null

    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        job = scope.launch {
            launch {
                useCase.flowAvailableDict()
                        .collectLatest { send(Msg.AvailableDict(list = it)) }
            }
            launch {
                useCase.flowCurrentDict()
                        .collectLatest { send(Msg.CurrentDict(current = it)) }
            }
        }
    }

    override suspend fun runEffect(
            effect: DatasourceEffect,
            consumer: (Msg) -> Unit,
    ) {
        logger.d(tag = LogTags.MATE, message = "RunEffect: $effect")
        val msg = when (effect) {
            is DatasourceEffect.ChangeDict -> {
                withContext(Dispatchers.IO) {
                    useCase.changeDict(id = effect.dict.id)
                }
                Msg.Empty
            }
        }
        consumer(msg)
    }
}
