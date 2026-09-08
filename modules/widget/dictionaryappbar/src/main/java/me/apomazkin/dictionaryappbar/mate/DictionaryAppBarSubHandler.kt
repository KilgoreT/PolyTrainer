package me.apomazkin.dictionaryappbar.mate

import io.github.kilgoret.mate.MateSubscriptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.dictionaryappbar.deps.DictionaryAppBarUseCase
import javax.inject.Inject

/**
 * Исполнитель семейства подписок [DictionaryAppBarSub]: раннер mate
 * отдаёт сюда подписку, появившуюся в наборе [subscriptions], и
 * коллектит возвращённый Flow в mailbox; отмена коллекта при
 * исчезновении подписки — на раннере. Одна when-ветка = один вид
 * подписки.
 */
class DictionaryAppBarSubHandler @Inject constructor(
    private val useCase: DictionaryAppBarUseCase,
) : MateSubscriptionHandler<Msg, DictionaryAppBarSub> {

    override val subFamily = DictionaryAppBarSub::class

    override fun flow(sub: DictionaryAppBarSub): Flow<Msg> = when (sub) {
        DictionaryAppBarSub.AvailableDicts ->
            useCase.flowAvailableDict()
                .map { Msg.AvailableDict(list = it) }

        DictionaryAppBarSub.CurrentDict ->
            useCase.flowCurrentDict()
                .map { Msg.CurrentDict(current = it) }
    }
}
