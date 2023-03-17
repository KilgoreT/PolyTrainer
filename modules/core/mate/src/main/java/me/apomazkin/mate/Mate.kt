package me.apomazkin.mate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class Mate<State, Message, Effect>(
    initState: State,
    reducer: MateReducer<State, Message, Effect>,
    initEffects: Set<Effect>,
    private val effectHandlerSet: Set<MateEffectHandler<Message, Effect>>,
    private val coroutineScope: CoroutineScope,
) : MateStateHolder<State, Message>,
    MateReducer<State, Message, Effect> by reducer,
    MateEffectHandler<Message, Effect> {

    private val _state: MutableStateFlow<State> = MutableStateFlow(initState)

    override val state: StateFlow<State>
        get() = _state.asStateFlow()

    init {
        executeEffect(initEffects)
    }

    override fun accept(message: Message) {
        val (newState, effects) = reduce(_state.value, message)
        _state.value = newState
        executeEffect(effects)
    }

    private fun executeEffect(effects: Set<Effect>) {
        effects.forEach { effect: Effect ->
            coroutineScope.launch {
                runEffect(effect, ::accept)
            }
        }
    }

    override suspend fun runEffect(effect: Effect, consumer: (Message) -> Unit) {
        effectHandlerSet.forEach {
            it.runEffect(effect, consumer)
        }
    }
}