package me.apomazkin.quiztab.logic

import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.ReducerLogging
import me.apomazkin.quiz.QuizGroupOptions
import me.apomazkin.quiztab.LogTags

/**
 * Атомарные state-экстеншны таба «Тренировки» (конвенция «Msg →
 * цепочка атомов»). Reducer наследует класс атомов; тесты атомов —
 * его же с Noop-логгером.
 */
internal abstract class QuizTabStateAtoms(
    logger: LexemeLogger,
) : ReducerLogging(logger) {

    override val logTag: String = LogTags.QUIZ

    /** Применить эмиссию опций: словарь, пункты, счётчик «Все». */
    fun QuizTabState.applyGroupOptions(
        dictionaryId: Long?,
        options: QuizGroupOptions,
    ): QuizTabState {
        logStep(
            "applyGroupOptions",
            "dict" to dictionaryId,
            "groups" to options.groups.size,
            "eligible" to options.groups.count { it.isEligible },
            "allCount" to options.allWordCount,
        )
        return copy(
            dictionaryId = dictionaryId,
            groupOptions = options.groups,
            isAllEligible = options.isAllEligible,
            allWordCount = options.allWordCount,
        )
    }

    /** Применить валидированный выбор подписки (null = «Все»). */
    fun QuizTabState.resolveSelection(selectedGroupId: Long?): QuizTabState {
        logStep(
            "resolveSelection",
            "selected" to (selectedGroupId ?: "all"),
            "fallback" to (selectedGroupId == null && this.selectedGroupId != null),
        )
        return copy(selectedGroupId = selectedGroupId)
    }

    /** Явный флаг кликабельности карточки (Д2). */
    fun QuizTabState.setCardEnabled(enabled: Boolean): QuizTabState {
        logStep("setCardEnabled", "enabled" to enabled)
        return copy(isChatCardEnabled = enabled)
    }

    /** Оптимистичный выбор юзера в пикере (персист — эффектом). */
    fun QuizTabState.pickGroup(groupId: Long?): QuizTabState {
        logStep("pickGroup", "group" to (groupId ?: "all"))
        return copy(selectedGroupId = groupId)
    }
}
