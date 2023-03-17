package me.apomazkin.mate

import kotlinx.coroutines.flow.StateFlow

interface MateStateHolder<State, Message> {

    val state: StateFlow<State>
    fun accept(message: Message)
}