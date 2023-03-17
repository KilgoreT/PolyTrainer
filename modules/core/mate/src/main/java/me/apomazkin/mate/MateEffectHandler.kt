package me.apomazkin.mate

interface Effect
interface MateEffectHandler<Message, out Effect> {
    suspend fun runEffect(
        effect: @UnsafeVariance Effect,
        consumer: (Message) -> Unit
    )
}