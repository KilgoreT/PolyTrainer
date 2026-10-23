package me.apomazkin.polytrainer.navigation

/**
 * Ключ счёта записанного экраном лексем-кандидатов в `SavedStateHandle` записи
 * вкладки «Слова»: массив из двух чисел, значения и слова. Кладёт
 * [AppNavigationExecutor] при исполнении [AppScreen.ScanCandidatesAdded], читает
 * мост вкладки и стирает после чтения.
 */
internal const val SCAN_CANDIDATES_ADDED = "scanCandidatesAdded"

/** Счёт для `SavedStateHandle`: сначала значения, затем слова. */
internal fun AppScreen.ScanCandidatesAdded.toSavedState(): LongArray = longArrayOf(
    lexemeCount.toLong(),
    wordCount.toLong(),
)

/** Счёт обратно из `SavedStateHandle`. */
internal fun LongArray.toScanCandidatesAdded(): AppScreen.ScanCandidatesAdded =
    AppScreen.ScanCandidatesAdded(
        lexemeCount = this[0].toInt(),
        wordCount = this[1].toInt(),
    )
