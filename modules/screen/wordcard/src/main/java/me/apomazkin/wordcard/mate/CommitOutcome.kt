package me.apomazkin.wordcard.mate

import me.apomazkin.lexeme.ComponentTemplate

/**
 * Решение на commit одной component-value записи (parity с 4-веточным when перевода).
 * Логика — 03 §3.2 / тесты §2.3.
 *
 * IS491: [Update] несёт пару (text, caption) — caption используется только
 * captioned_text-шаблоном (null у остальных и при пустой подписи).
 */
sealed interface CommitOutcome {
    data object NoOp : CommitOutcome

    data object LocalRemove : CommitOutcome

    data object PessimisticRemove : CommitOutcome

    data class Update(
        val text: String,
        val caption: String? = null,
    ) : CommitOutcome
}

internal fun ComponentValueState.commitDecision(): CommitOutcome {
    if (!isEdit) return CommitOutcome.NoOp
    val trimmed = edited.trim()
    val originTrimmed = origin.trim()
    if (trimmed.isEmpty()) {
        // «Пусто» определяется ТОЛЬКО по text (Д7): значение «одна подпись» не существует.
        // Origin-lossy fix (IS491): сохранённое значение шаблона, чей контент не
        // выражается редактируемым текстом (origin всегда ""), нельзя терять через
        // LocalRemove — оно обязано удаляться через БД.
        return when {
            originTrimmed.isNotEmpty() -> CommitOutcome.PessimisticRemove
            componentValueId != null && !template.hasEditableText -> CommitOutcome.PessimisticRemove
            else -> CommitOutcome.LocalRemove
        }
    }
    if (template == ComponentTemplate.CAPTIONED_TEXT) {
        val captionTrimmed = editedCaption?.trim()?.takeIf { it.isNotEmpty() }
        val originCaptionTrimmed = originCaption?.trim()?.takeIf { it.isNotEmpty() }
        return if (trimmed == originTrimmed && captionTrimmed == originCaptionTrimmed) {
            CommitOutcome.NoOp
        } else {
            CommitOutcome.Update(trimmed, captionTrimmed)
        }
    }
    return if (trimmed == originTrimmed) CommitOutcome.NoOp else CommitOutcome.Update(trimmed)
}

/** Шаблоны, чьё значение выражается редактируемым текстом (origin = текст). */
internal val ComponentTemplate.hasEditableText: Boolean
    get() =
        when (this) {
            ComponentTemplate.TEXT, ComponentTemplate.CAPTIONED_TEXT -> true
            ComponentTemplate.IMAGE, ComponentTemplate.CHOICE -> false
        }
