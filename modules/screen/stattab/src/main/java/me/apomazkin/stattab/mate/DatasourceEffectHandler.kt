package me.apomazkin.stattab.mate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateFlowHandler
import me.apomazkin.stattab.deps.StatisticUseCase
import me.apomazkin.mate.LogTags
import me.apomazkin.ui.logger.LexemeLogger

/**
 * Effect
 */
internal sealed interface DatasourceEffect : Effect {
}

/**
 * EffectHandler for datastore calls.
 */
internal class DatasourceEffectHandler(
    private val useCase: StatisticUseCase,
    private val logger: LexemeLogger,
) : MateFlowHandler<Msg, Effect> {

    override var job: Job? = null

    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        job = scope.launch {
            combine(
                useCase.flowWordCount(),
                useCase.flowLexemeCount(),
                useCase.flowQuizStat()
            ) { wordCount, lexemeCount, quizStat ->
                Msg.UpdateStates(
                    wordCount = wordCount,
                    lexemeCount = lexemeCount,
                    quizStat = quizStat,
                )
            }.collectLatest { msg ->
                send(msg)
            }
        }
    }

    override suspend fun runEffect(
        effect: Effect,
        consumer: (Msg) -> Unit
    ) {
        logger.d(tag = LogTags.MATE, message = "RunEffect: $effect")
        return when (val eff = effect as? DatasourceEffect) {

            null -> Msg.Empty
            else -> Msg.Empty
        }.let(consumer)
    }

}