package me.apomazkin.quiz.chat.logic

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateFlowHandler
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.prefs.quizPickerPrefKey
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import javax.inject.Inject

/**
 * IS481 quiz picker. Subscribe на dynamic-key string flow `quiz_picker_dict_<id>`.
 * На каждый emit (initial DataStore value + write) → re-emit
 * `Msg.QuizComponentTypesLoaded` с актуальным types + restored selectedRef.
 *
 * - Initial subscribe резолвит `dictionaryId` через `getCurrentDictionaryId()`.
 *   `null` → handler в terminal state (subscribe не запускает collect, send
 *   никогда не вызывается; повторные pref-writes не доходят до handler'а
 *   до re-init ViewModel'а, F1 спека).
 * - Каждый write в pref → flow emit → re-fetch types + restored selectedRef
 *   → send `Msg.QuizComponentTypesLoaded` (single update path: initial load +
 *   persist update идут через один Msg).
 */
class QuizPickerFlowHandler @Inject constructor(
    private val useCase: QuizChatUseCase,
    private val prefsProvider: PrefsProvider,
) : MateFlowHandler<Msg, Effect> {

    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {}

    override var job: Job? = null

    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        job = scope.launch {
            val dictId = useCase.getCurrentDictionaryId() ?: return@launch
            prefsProvider.getStringFlowByRawKey(quizPickerPrefKey(dictId))
                .collectLatest { _ ->
                    send(
                        Msg.QuizComponentTypesLoaded(
                            types = useCase.getAvailableTypes(dictId),
                            restoredSelectedRef = useCase.getQuizPickerSelection(dictId),
                        )
                    )
                }
        }
    }
}
