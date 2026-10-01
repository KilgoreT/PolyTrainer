package me.apomazkin.quiztab.logic

import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.ReducerLogging
import me.apomazkin.quiz.QuizGroupOptions
import me.apomazkin.quiz.quizGroupLabel
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

    /** Применить валидированный набор подписки (пусто = «Все»). */
    fun QuizTabState.resolveSelection(selectedGroupIds: Set<Long>): QuizTabState {
        logStep(
            "resolveSelection",
            "selected" to selectedGroupIds.describe(),
            "dropped" to (this.selectedGroupIds - selectedGroupIds).describe(),
        )
        return copy(selectedGroupIds = selectedGroupIds)
    }

    /** Явный флаг кликабельности карточки. */
    fun QuizTabState.setCardEnabled(enabled: Boolean): QuizTabState {
        logStep("setCardEnabled", "enabled" to enabled)
        return copy(isChatCardEnabled = enabled)
    }

    /** Оптимистичный выбор юзера в пикере (персист — эффектом). */
    fun QuizTabState.pickGroups(groupIds: Set<Long>): QuizTabState {
        logStep("pickGroups", "groups" to groupIds.describe())
        return copy(selectedGroupIds = groupIds)
    }

    /**
     * Подпись выбора по текущим набору и опциям (порядок опций уже по
     * алфавиту). Звать после любого изменения набора или опций.
     */
    fun QuizTabState.applySelectionLabel(): QuizTabState {
        val label = quizGroupLabel(groupOptions, selectedGroupIds)
        logStep(
            "applySelectionLabel",
            "label" to (label?.let { "${it.first}+${it.more}" } ?: "all"),
        )
        return copy(selectionLabel = label)
    }

    private fun Set<Long>.describe(): String =
        if (isEmpty()) "all" else sorted().joinToString(",")
}
