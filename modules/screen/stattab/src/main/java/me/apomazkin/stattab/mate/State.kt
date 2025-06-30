package me.apomazkin.stattab.mate

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import me.apomazkin.theme.statInProcessBg
import me.apomazkin.theme.statInProcessFg
import me.apomazkin.theme.statLearnedBg
import me.apomazkin.theme.statLearnedFg
import me.apomazkin.theme.statNotSTartedFg
import me.apomazkin.theme.statNotStartedBg

/**
 * State
 */
@Immutable
data class StatisticState(
    val isLoading: Boolean = true,
    val wordCount: Int = 0,
    val lexemeCount: Int = 0,
    val quizState: QuizState = QuizState()
)

@Immutable
data class QuizState(
    val quizStat: List<QuizProcessState> = emptyList(),
    val quizGrades: List<QuizGradeState> = emptyList()
)

@Immutable
data class QuizProcessState(
    val processState: ProcessState,
    val value: Int,
) {
    enum class ProcessState(
        index: Int
    ) {
        NOT_STARTED(3),
        IN_PROCESS(2),
        DONE(3);

        override fun toString(): String {
            return when(this) {
                NOT_STARTED -> "Не выучено"
                IN_PROCESS -> "В процессе"
                DONE -> "Выучено"
            }
        }

        fun toFg(): Color {
            return when (this) {
                NOT_STARTED -> statNotSTartedFg
                IN_PROCESS -> statInProcessFg
                DONE -> statLearnedFg
            }
        }

        fun toBg(): Color {
            return when (this) {
                NOT_STARTED -> statNotStartedBg
                IN_PROCESS -> statInProcessBg
                DONE -> statLearnedBg
            }
        }
    }
}

@Immutable
data class QuizGradeState(
    val grade: Int,
    val value: Int,
)

fun StatisticState.showLoading() = this.copy(
    isLoading = true,
)

fun StatisticState.hideLoading() = this.copy(
    isLoading = false,
)

fun StatisticState.updateWordCount(wordCount: Int) = this.copy(
    wordCount = wordCount,
)

fun StatisticState.updateLexemeCount(lexemeCount: Int) = this.copy(
    lexemeCount = lexemeCount,
)

fun StatisticState.updateQuizStat(quizStat: Map<Int, Int>) = this.copy(
    quizState = QuizState(
        quizStat = listOfNotNull(
            quizStat[quizStat.keys.max()]?.let { value ->
                QuizProcessState(
                    processState = QuizProcessState.ProcessState.DONE,
                    value = value
                )
            },
            QuizProcessState(
                processState = QuizProcessState.ProcessState.IN_PROCESS,
                value = quizStat.filterKeys {
                    it != quizStat.keys.minOrNull()
                            && it != quizStat.keys.maxOrNull()
                }.values.sum()
            ),
            quizStat[quizStat.keys.min()]?.let { value ->
                QuizProcessState(
                    processState = QuizProcessState.ProcessState.NOT_STARTED,
                    value = value
                )
            }
        ),
        quizGrades = quizStat
            .filterKeys {
                it != quizStat.keys.minOrNull()
                        && it != quizStat.keys.maxOrNull()
            }
            .keys
            .sortedDescending()
            .mapNotNull { grade ->
                quizStat[grade]?.let { value ->
                    QuizGradeState(
                        grade = grade,
                        value = value
                    )
                }
            }
    )
)