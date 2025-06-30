package me.apomazkin.mate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

interface MateFlowHandler<Message, Effect> : MateEffectHandler<Message, Effect> {

    var job: Job?

    fun subscribe(scope: CoroutineScope, send: (Message) -> Unit)
    fun unsubscribe() {
        job?.cancel()
        job = null
    }
}