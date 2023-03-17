package me.apomazkin.mate

typealias ReducerResult<State, Effect> = Pair<State, Set<Effect>>

interface MateReducer<State, Message, Effect> {
    fun reduce(state: State, message: Message): ReducerResult<State, Effect>
}