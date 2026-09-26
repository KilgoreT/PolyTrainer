package me.apomazkin.wordstab.logic

import io.github.kilgoret.mate.MateSubscriptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.wordstab.deps.WordsTabUseCase
import javax.inject.Inject

/**
 * Исполнитель семейства подписок [WordsTabSub]: раннер mate отдаёт
 * сюда подписку, появившуюся в наборе [subscriptions], и коллектит
 * возвращённый Flow в mailbox; отмена коллекта при исчезновении
 * подписки — на раннере.
 *
 * Поток текущего словаря маппится в [Msg.SelectDictionary] — null
 * проходит как валидное доменное состояние «словарей нет».
 */
class WordsTabSubHandler @Inject constructor(
    private val useCase: WordsTabUseCase,
) : MateSubscriptionHandler<Msg, WordsTabSub> {

    override val subscriptionFamily = WordsTabSub::class

    override fun flow(sub: WordsTabSub): Flow<Msg> = when (sub) {
        WordsTabSub.CurrentDict ->
            useCase.flowCurrentDict()
                .map { dict -> Msg.SelectDictionary(current = dict) }
    }
}
