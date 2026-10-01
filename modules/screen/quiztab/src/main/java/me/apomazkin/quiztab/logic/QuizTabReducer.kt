package me.apomazkin.quiztab.logic

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateReducer
import io.github.kilgoret.mate.ReducerResult
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.quiz.QuizTypes
import me.apomazkin.quiztab.QuizTabNavigationEffect
import me.apomazkin.quiztab.logic.processor.processUiMessage

internal class QuizTabReducer(
    logger: LexemeLogger,
) : QuizTabStateAtoms(logger),
    MateReducer<QuizTabState, Msg, Effect> {

    override fun reduce(
        state: QuizTabState,
        message: Msg,
    ): ReducerResult<QuizTabState, Effect> {
        logMessage(message::class.simpleName ?: message.toString())
        return when (message) {
            is UiMsg -> processUiMessage(state, message)

            is Msg.OpenChat ->
                if (state.isChatCardEnabled) {
                    state to setOf(QuizTabNavigationEffect.OpenChat(message.quizType))
                } else {
                    // Reducer не доверяет UI: карточка обязана быть
                    // некликабельной, но тап в окно гонки — no-op.
                    state.noOp("card disabled")
                }

            is Msg.GroupOptionsLoaded ->
                state
                    .applyGroupOptions(
                        dictionaryId = message.dictionaryId,
                        options = message.options,
                    )
                    .resolveSelection(message.selectedGroupIds)
                    .applySelectionLabel()
                    .setCardEnabled(message.options.hasEligibleOption) to
                    // Закрепление: очищенный набор пишется в pref, выпавшая
                    // группа сама не вернётся и после исцеления.
                    if (message.selectionInvalidated && message.dictionaryId != null) {
                        setOf(
                            QuizTabDatasourceEffect.PersistGroupSelection(
                                quizType = message.quizType,
                                dictionaryId = message.dictionaryId,
                                groupIds = message.selectedGroupIds,
                            ),
                        )
                    } else {
                        emptySet()
                    }

            is Msg.ToggleGroup -> reduceToggleGroup(state, message)

            is Msg.PickAll -> reducePickAll(state, message)

            is Msg.GroupOptionsFailed -> state.noOp("options failed")

            Msg.Empty -> state to emptySet()
        }
    }

    private fun reduceToggleGroup(
        state: QuizTabState,
        message: Msg.ToggleGroup,
    ): ReducerResult<QuizTabState, Effect> {
        // Единственная карточка v1 — chat; чужой тип = гонка/ошибка UI.
        if (message.quizType != QuizTypes.CHAT) return state.noOp("unknown quiz type")
        val dictionaryId = state.dictionaryId ?: return state.noOp("no dictionary")
        // Reducer не доверяет UI: отметить можно только пригодную группу
        // текущих опций (устаревший тап после эмиссии — no-op).
        val isEligible = state.groupOptions.any { it.id == message.groupId && it.isEligible }
        if (message.checked && !isEligible) return state.noOp("ineligible group")
        val next = if (message.checked) {
            state.selectedGroupIds + message.groupId
        } else {
            state.selectedGroupIds - message.groupId
        }
        return state.persistGroups(message.quizType, dictionaryId, next)
    }

    private fun reducePickAll(
        state: QuizTabState,
        message: Msg.PickAll,
    ): ReducerResult<QuizTabState, Effect> {
        if (message.quizType != QuizTypes.CHAT) return state.noOp("unknown quiz type")
        val dictionaryId = state.dictionaryId ?: return state.noOp("no dictionary")
        return state.persistGroups(message.quizType, dictionaryId, emptySet())
    }

    /** Тот же набор — no-op; иначе оптимистичный state + персист. */
    private fun QuizTabState.persistGroups(
        quizType: String,
        dictionaryId: Long,
        next: Set<Long>,
    ): ReducerResult<QuizTabState, Effect> {
        if (next == selectedGroupIds) return noOp("same selection")
        return pickGroups(next).applySelectionLabel() to setOf(
            QuizTabDatasourceEffect.PersistGroupSelection(
                quizType = quizType,
                dictionaryId = dictionaryId,
                groupIds = next,
            ),
        )
    }
}
