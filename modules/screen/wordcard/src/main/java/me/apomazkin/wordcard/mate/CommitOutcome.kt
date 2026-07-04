package me.apomazkin.wordcard.mate

/**
 * Решение на commit одной component-value записи (parity с 4-веточным when перевода).
 * Логика — 03 §3.2 / тесты §2.3. РЕАЛИЗАЦИЯ — этап 3 (сейчас заглушка → тесты red).
 */
sealed interface CommitOutcome {
    data object NoOp : CommitOutcome
    data object LocalRemove : CommitOutcome
    data object PessimisticRemove : CommitOutcome
    data class Update(val text: String) : CommitOutcome
}

internal fun ComponentValueState.commitDecision(): CommitOutcome {
    if (!isEdit) return CommitOutcome.NoOp
    val trimmed = edited.trim()
    val originTrimmed = origin.trim()
    if (trimmed.isEmpty()) {
        return if (originTrimmed.isEmpty()) CommitOutcome.LocalRemove else CommitOutcome.PessimisticRemove
    }
    return if (trimmed == originTrimmed) CommitOutcome.NoOp else CommitOutcome.Update(trimmed)
}
