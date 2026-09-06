package me.apomazkin.dictionary.form

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.apomazkin.dictionary.DictionaryUseCase
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.MateFlowHandler
import javax.inject.Inject

/** Гибрид: подписка на флаги + исполнитель семейства FlagFilterEffect. */
class FlagFilterFlowHandler @Inject constructor(
    private val dictionaryUseCase: DictionaryUseCase,
) : MateFlowHandler<DictionaryFormMsg>,
    MateEffectHandler<DictionaryFormMsg, FlagFilterEffect> {

    override var job: Job? = null

    override val effectFamily = FlagFilterEffect::class

    override fun subscribe(scope: CoroutineScope, send: (DictionaryFormMsg) -> Unit) {
        job = scope.launch {
            dictionaryUseCase.flagsFlow().collectLatest { flags ->
                send(DictionaryFormMsg.FlagsUpdated(flags))
            }
        }
    }

    override suspend fun runEffect(
        effect: FlagFilterEffect,
        consumer: (DictionaryFormMsg) -> Unit,
    ) {
        when (effect) {
            is FlagFilterEffect.FilterFlags -> dictionaryUseCase.updateFilter(effect.query)
        }
    }
}
