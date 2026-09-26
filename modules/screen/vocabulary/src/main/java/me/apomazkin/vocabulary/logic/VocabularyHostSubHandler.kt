package me.apomazkin.vocabulary.logic

import io.github.kilgoret.mate.MateSubscriptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.vocabulary.deps.VocabularyHostUseCase
import javax.inject.Inject

/**
 * Исполнитель семейства подписок [VocabularyHostSub]: раннер mate
 * отдаёт сюда подписку, появившуюся в наборе [subscriptions], и
 * коллектит возвращённый Flow в mailbox; отмена коллекта при
 * исчезновении подписки — на раннере.
 *
 * Поток текущего словаря маппится в [Msg.DictionaryChanged] — null
 * проходит как валидное доменное состояние «словарей нет».
 */
class VocabularyHostSubHandler
    @Inject
    constructor(
        private val useCase: VocabularyHostUseCase,
    ) : MateSubscriptionHandler<Msg, VocabularyHostSub> {
        override val subscriptionFamily = VocabularyHostSub::class

        override fun flow(sub: VocabularyHostSub): Flow<Msg> =
            when (sub) {
                VocabularyHostSub.CurrentDict ->
                    useCase
                        .flowCurrentDictId()
                        .map { dictId -> Msg.DictionaryChanged(dictionaryId = dictId) }
            }
    }
