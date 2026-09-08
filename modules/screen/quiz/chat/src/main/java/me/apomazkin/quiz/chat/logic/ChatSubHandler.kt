package me.apomazkin.quiz.chat.logic

import io.github.kilgoret.mate.MateSubscriptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.prefs.quizPickerPrefKey
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import javax.inject.Inject

/**
 * Исполнитель семейства подписок [ChatSub]: раннер mate отдаёт сюда
 * подписку, появившуюся в наборе [subscriptions], и коллектит
 * возвращённый Flow в mailbox; отмена коллекта при исчезновении
 * подписки — на раннере. Одна when-ветка = один вид подписки.
 *
 * QuizPicker: dictionaryId резолвится один раз на старте подписки;
 * null («нет текущего словаря») — поток завершается пустым, экран
 * живёт без выбора компонентов до пересоздания раннера.
 */
class ChatSubHandler @Inject constructor(
    private val useCase: QuizChatUseCase,
    private val prefsProvider: PrefsProvider,
) : MateSubscriptionHandler<Msg, ChatSub> {

    override val subFamily = ChatSub::class

    override fun flow(sub: ChatSub): Flow<Msg> = when (sub) {
        ChatSub.AppBarMenu -> combine(
            prefsProvider.getBooleanFlow(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN),
            prefsProvider.getBooleanFlow(PrefKey.CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN),
            prefsProvider.getBooleanFlow(PrefKey.CHAT_DEBUG_STATUS_BOOLEAN),
        ) { earliest, frequentMistakes, debug ->
            Msg.UpdateMenu(
                isEarliestOn = earliest,
                isFrequentMistakesOn = frequentMistakes,
                isDebugOn = debug,
            )
        }

        ChatSub.QuizPicker -> flow {
            val dictId = useCase.getCurrentDictionaryId() ?: return@flow
            emitAll(
                prefsProvider.getStringFlowByRawKey(quizPickerPrefKey(dictId))
                    .map {
                        Msg.QuizComponentTypesLoaded(
                            types = useCase.getAvailableTypes(dictId),
                            restoredSelectedRef = useCase.getQuizPickerSelection(dictId),
                        )
                    },
            )
        }
    }
}
