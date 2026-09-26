package me.apomazkin.dictionary.form

import io.github.kilgoret.mate.MateSubscriptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.dictionary.DictionaryUseCase
import javax.inject.Inject

/**
 * Исполнитель семейства подписок [DictionaryFormSub]: раннер mate
 * отдаёт сюда подписку, появившуюся в наборе [subscriptions], и
 * коллектит возвращённый Flow в mailbox; отмена коллекта при
 * исчезновении подписки — на раннере.
 */
class DictionaryFormSubHandler @Inject constructor(
    private val dictionaryUseCase: DictionaryUseCase,
) : MateSubscriptionHandler<DictionaryFormMsg, DictionaryFormSub> {

    override val subscriptionFamily = DictionaryFormSub::class

    override fun flow(sub: DictionaryFormSub): Flow<DictionaryFormMsg> = when (sub) {
        DictionaryFormSub.Flags ->
            dictionaryUseCase.flagsFlow()
                .map { flags -> DictionaryFormMsg.FlagsUpdated(flags) }
    }
}
