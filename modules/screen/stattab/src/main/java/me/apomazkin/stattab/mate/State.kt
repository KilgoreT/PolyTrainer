package me.apomazkin.stattab.mate

import androidx.compose.runtime.Immutable

/**
 * State
 */
@Immutable
data class StatisticState(
    val isLoading: Boolean = true,
    val wordCount: Int = 0,
    val lexemeCount: Int = 0,
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