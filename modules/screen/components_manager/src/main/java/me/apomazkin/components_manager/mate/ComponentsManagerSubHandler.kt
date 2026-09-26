package me.apomazkin.components_manager.mate

import io.github.kilgoret.mate.MateSubscriptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.apomazkin.components_manager.LogTags
import me.apomazkin.components_manager.deps.ComponentsManagerUseCase
import me.apomazkin.logger.LexemeLogger
import javax.inject.Inject

/**
 * Исполнитель семейства подписок [ComponentsManagerSub]: раннер mate
 * отдаёт сюда подписку, появившуюся в наборе [subscriptions], и
 * коллектит возвращённый Flow в mailbox; отмена коллекта при
 * исчезновении подписки — на раннере. Одна when-ветка = один вид
 * подписки; ошибки перехватываются на месте (`catch` → fail-Msg
 * либо деградация до пустого списка).
 */
class ComponentsManagerSubHandler
    @Inject
    constructor(
        private val useCase: ComponentsManagerUseCase,
        private val logger: LexemeLogger,
    ) : MateSubscriptionHandler<Msg, ComponentsManagerSub> {
        override val subscriptionFamily = ComponentsManagerSub::class

        override fun flow(sub: ComponentsManagerSub): Flow<Msg> =
            when (sub) {
                is ComponentsManagerSub.AllTypes ->
                    useCase
                        .flowAllUserDefinedTypes()
                        .map<_, Msg> { snapshot -> Msg.TypesLoaded(snapshot) }
                        .catch { e ->
                            logger.e(
                                tag = LogTags.ALL_COMPONENTS,
                                message = "flow failed: ${e.message}",
                            )
                            emit(Msg.TypesLoadFailed(e))
                        }

                ComponentsManagerSub.Dictionaries ->
                    useCase
                        .flowDictionaries()
                        .map<_, Msg> { list -> Msg.DictionariesLoaded(list) }
                        .catch { e ->
                            logger.e(
                                tag = LogTags.ALL_COMPONENTS,
                                message = "flowDictionaries failed: ${e.message}",
                            )
                            emit(Msg.DictionariesLoaded(emptyList()))
                        }
            }
    }
