package me.apomazkin.quiz.chat.logic

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateFlowHandler
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider

class AppBarFlowHandler(
        private val prefsProvider: PrefsProvider
): MateFlowHandler<Msg, Effect> {

    private var job: Job? = null

    override suspend fun runEffect(effect: Effect, consumer: (Msg) -> Unit) {}

    override fun subscribe(scope: CoroutineScope, send: (Msg) -> Unit) {
        job = scope.launch {
            prefsProvider.getBooleanFlow(PrefKey.CHAT_DEBUG_STATUS_BOOLEAN)
                    .collectLatest { value ->
                        send(Msg.UpdateDebug(isOn = value))
                    }
        }
    }

    override fun unsubscribe() {
        job?.cancel()
        job = null
    }
}