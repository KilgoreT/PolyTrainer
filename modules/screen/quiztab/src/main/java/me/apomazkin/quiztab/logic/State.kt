package me.apomazkin.quiztab.logic

import androidx.compose.runtime.Immutable
import me.apomazkin.mate.EMPTY_STRING
import me.apomazkin.quiz.QuizGroup
import me.apomazkin.quiz.QuizGroupLabel

/**
 * State
 *
 * Карточка chat-квиза несёт пикер набора групп. Дефолты — рабочее
 * состояние до первой эмиссии подписки: карточка кликабельна, пикер —
 * «Все» без пунктов (disabled не мигает; тап в это окно безопасен —
 * квиз сам читает валидированный выбор на старте сессии).
 *
 * [selectedGroupIds] — выбранные группы, пусто = «Все» (весь словарь).
 * [selectionLabel] — подпись выбора («Быт +2»), явное поле: считается
 * атомом при каждом изменении набора или опций; null = «Все».
 */
@Immutable
data class QuizTabState(
    val snackbarState: SnackbarState = SnackbarState(),
    val dictionaryId: Long? = null,
    val groupOptions: List<QuizGroup> = emptyList(),
    val isAllEligible: Boolean = true,
    val allWordCount: Int = 0,
    val selectedGroupIds: Set<Long> = emptySet(),
    val selectionLabel: QuizGroupLabel? = null,
    val isChatCardEnabled: Boolean = true,
)

@Immutable
data class SnackbarState(
    val title: String = EMPTY_STRING,
    val show: Boolean = false,
)
