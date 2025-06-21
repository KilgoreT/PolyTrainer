package me.apomazkin.mate

import kotlinx.coroutines.CoroutineScope

interface MateFlowHandler<Message, Effect> : MateEffectHandler<Message, Effect> {
    fun subscribe(scope: CoroutineScope, send: (Message) -> Unit)
    fun unsubscribe()
}