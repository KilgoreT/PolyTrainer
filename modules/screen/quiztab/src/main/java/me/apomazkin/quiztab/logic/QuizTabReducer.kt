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

            is Msg.PickGroup -> reducePickGroup(state, message)

            is Msg.GroupOptionsLoaded ->
                state
                    .applyGroupOptions(
                        dictionaryId = message.dictionaryId,
                        options = message.options,
                    )
                    .resolveSelection(message.selectedGroupId)
                    .setCardEnabled(message.options.hasEligibleOption) to
                    // Фолбэк закрепляется: невалидный персист стирается,
                    // «Все» остаётся выбором и после исцеления группы.
                    if (message.selectionInvalidated && message.dictionaryId != null) {
                        setOf(
                            QuizTabDatasourceEffect.PersistGroupSelection(
                                quizType = message.quizType,
                                dictionaryId = message.dictionaryId,
                                groupId = null,
                            ),
                        )
                    } else {
                        emptySet()
                    }

            is Msg.GroupOptionsFailed -> state.noOp("options failed")

            Msg.Empty -> state to emptySet()
        }
    }

    private fun reducePickGroup(
        state: QuizTabState,
        message: Msg.PickGroup,
    ): ReducerResult<QuizTabState, Effect> {
        // Единственная карточка v1 — chat; чужой тип = гонка/ошибка UI.
        if (message.quizType != QuizTypes.CHAT) return state.noOp("unknown quiz type")
        val dictionaryId = state.dictionaryId ?: return state.noOp("no dictionary")
        if (message.groupId == state.selectedGroupId) return state.noOp("same selection")
        return state.pickGroup(message.groupId) to setOf(
            QuizTabDatasourceEffect.PersistGroupSelection(
                quizType = message.quizType,
                dictionaryId = dictionaryId,
                groupId = message.groupId,
            ),
        )
    }
}
