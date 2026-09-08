package me.apomazkin.stattab.mate

import io.github.kilgoret.mate.MateSubscriptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import me.apomazkin.stattab.deps.StatisticUseCase
import javax.inject.Inject

/**
 * Исполнитель семейства подписок [StatSub]: раннер mate отдаёт сюда
 * подписку из набора [subscriptions] и коллектит возвращённый Flow в
 * mailbox; отмена коллекта при исчезновении подписки — на раннере.
 * Счётчики трёх источников склеиваются combine'ом в один
 * [Msg.UpdateStates] — экран всегда видит согласованный снимок.
 */
class StatSubHandler @Inject constructor(
    private val useCase: StatisticUseCase,
) : MateSubscriptionHandler<Msg, StatSub> {

    override val subFamily = StatSub::class

    override fun flow(sub: StatSub): Flow<Msg> = when (sub) {
        // Юзкейсы suspend-фабрики потоков — разворачиваем внутри flow.
        StatSub.Counters -> flow {
            emitAll(
                combine(
                    useCase.flowWordCount(),
                    useCase.flowLexemeCount(),
                    useCase.flowQuizStat(),
                ) { wordCount, lexemeCount, quizStat ->
                    Msg.UpdateStates(
                        wordCount = wordCount,
                        lexemeCount = lexemeCount,
                        quizStat = quizStat,
                    )
                },
            )
        }
    }
}
