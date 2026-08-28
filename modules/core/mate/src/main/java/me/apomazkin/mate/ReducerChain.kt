package me.apomazkin.mate

/**
 * IS493 Э3 (конвенция юзера): цепочка атомарных state-экстеншнов, каждый
 * из которых возвращает [ReducerResult] — state прокатывается по шагам,
 * эффекты АККУМУЛИРУЮТСЯ (Elm-паттерн «Update → (Model, Cmd)»).
 *
 * Пример ветки reducer'а:
 * ```
 * is Msg.SliceLoaded -> state.begin<S, Effect>()
 *     .then { it.hideLoading() }
 *     .then { it.applyGroups(items) }
 *     .then { it.applyAllCount(count) }   // может вернуть свой эффект
 * ```
 * Первый потребитель — GroupsTabReducer; words мигрирует позже.
 */

/** Начало цепочки: текущее состояние, эффектов пока нет. */
fun <S, E> S.begin(): ReducerResult<S, E> = this to emptySet()

/**
 * Шаг цепочки: применить [step] к текущему state, добавить его эффекты
 * к уже накопленным.
 */
infix fun <S, E> ReducerResult<S, E>.then(
    step: (S) -> ReducerResult<S, E>,
): ReducerResult<S, E> {
    val (next, stepEffects) = step(state())
    return next to (effects() + stepEffects)
}

/** Добавить эффект, не меняя state (эффект решает ветка reducer'а). */
infix fun <S, E> ReducerResult<S, E>.withEffect(effect: E): ReducerResult<S, E> =
    state() to (effects() + effect)
