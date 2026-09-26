package me.apomazkin.dictionary.list

import io.github.kilgoret.mate.MateSubscriptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.dictionary.DictionaryUseCase
import javax.inject.Inject

/**
 * Исполнитель семейства подписок [DictionaryListSub]: раннер mate
 * отдаёт сюда подписку, появившуюся в наборе [subscriptions], и
 * коллектит возвращённый Flow в mailbox; отмена коллекта при
 * исчезновении подписки — на раннере.
 */
class DictionaryListSubHandler @Inject constructor(
    private val dictionaryUseCase: DictionaryUseCase,
) : MateSubscriptionHandler<DictionaryListMsg, DictionaryListSub> {

    override val subscriptionFamily = DictionaryListSub::class

    override fun flow(sub: DictionaryListSub): Flow<DictionaryListMsg> = when (sub) {
        DictionaryListSub.Dictionaries ->
            dictionaryUseCase.flowDictionaryList()
                .map { list -> DictionaryListMsg.DictionariesLoaded(list) }
    }
}
