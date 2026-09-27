package me.apomazkin.quiztab.logic

import androidx.compose.runtime.Immutable
import me.apomazkin.mate.EMPTY_STRING
import me.apomazkin.quiz.QuizGroup

/**
 * State
 *
 * IS500: карточка chat-квиза несёт пикер группы. Дефолты — рабочее
 * состояние до первой эмиссии подписки: карточка кликабельна, пикер —
 * «Все» без пунктов (сохраняет поведение таба до фичи, disabled не
 * мигает; тап в это окно безопасен — квиз сам читает валидированный
 * выбор на старте сессии).
 *
 * Имя выбранной группы в state не дублируется — оно производное от
 * [selectedGroupId] + [groupOptions] (null = «Все», ресурс UI).
 */
@Immutable
data class QuizTabState(
    val snackbarState: SnackbarState = SnackbarState(),
    val dictionaryId: Long? = null,
    val groupOptions: List<QuizGroup> = emptyList(),
    val isAllEligible: Boolean = true,
    val allWordCount: Int = 0,
    val selectedGroupId: Long? = null,
    val isChatCardEnabled: Boolean = true,
)

@Immutable
data class SnackbarState(
    val title: String = EMPTY_STRING,
    val show: Boolean = false,
)
