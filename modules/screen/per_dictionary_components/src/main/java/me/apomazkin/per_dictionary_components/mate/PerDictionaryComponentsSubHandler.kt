package me.apomazkin.per_dictionary_components.mate

import io.github.kilgoret.mate.MateSubscriptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.per_dictionary_components.LogTags
import me.apomazkin.per_dictionary_components.deps.PerDictionaryComponentsUseCase
import javax.inject.Inject

/**
 * Исполнитель семейства подписок [PerDictionaryComponentsSub]: раннер
 * mate отдаёт сюда подписку, появившуюся в наборе [subscriptions], и
 * коллектит возвращённый Flow в mailbox; отмена коллекта при
 * исчезновении подписки — на раннере.
 *
 * Ошибка потока перехватывается на месте (`catch` →
 * [Msg.ItemsLoadFailed]): экран переходит в error state с кнопкой
 * Retry, которая рестартует подписку через generation-поле.
 */
class PerDictionaryComponentsSubHandler @Inject constructor(
    private val useCase: PerDictionaryComponentsUseCase,
    private val logger: LexemeLogger,
) : MateSubscriptionHandler<Msg, PerDictionaryComponentsSub> {

    override val subscriptionFamily = PerDictionaryComponentsSub::class

    override fun flow(sub: PerDictionaryComponentsSub): Flow<Msg> = when (sub) {
        is PerDictionaryComponentsSub.Components ->
            useCase.flowComponentsForDictionary(sub.dictionaryId)
                .map<_, Msg> { snapshot -> Msg.ItemsLoaded(snapshot) }
                .catch { e ->
                    logger.e(
                        tag = LogTags.DICT_COMPONENTS,
                        message = "flow failed: ${e.message}",
                    )
                    emit(Msg.ItemsLoadFailed(e))
                }
    }
}
