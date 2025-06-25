package me.apomazkin.quiz.chat.logic

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateFlowHandler
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider

class AppBarFlowHandler(
        private val prefsProvider: PrefsProvider,
) : MateFlowHandler<Msg, Effect> {

    private var job: Job? = null

    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {}

    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        job = scope.launch {
            combine(
                    prefsProvider.getBooleanFlow(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN),
                    prefsProvider.getBooleanFlow(PrefKey.CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN),
                    prefsProvider.getBooleanFlow(PrefKey.CHAT_DEBUG_STATUS_BOOLEAN)
            ) { earliest, frequentMistakes, debug ->
                Msg.UpdateMenu(
                        isEarliestOn = earliest,
                        isFrequentMistakesOn = frequentMistakes,
                        isDebugOn = debug
                )
            }.collectLatest { msg ->
                send(msg)
            }
        }
    }

    override fun unsubscribe() {
        job?.cancel()
        job = null
    }
}