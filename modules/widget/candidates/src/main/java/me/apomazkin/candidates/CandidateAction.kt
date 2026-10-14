package me.apomazkin.candidates

/*
 * IS533: действия пользователя в окне снимка и в диалоге карточки слова
 * — одни и те же виджеты шлют их наружу, экран превращает в свой Msg.
 * Порядок — по частоте: карточки и лексемы, листы и правка, соответствия,
 * запись, закрытие.
 */
sealed interface CandidateAction {
    data class ToggleCard(val lemma: String) : CandidateAction

    data class DeleteCard(val lemma: String) : CandidateAction

    data class DeleteLexeme(val lemma: String, val key: String) : CandidateAction

    data class DeleteExample(val lemma: String, val key: String, val index: Int) : CandidateAction

    data class OpenSheet(val sheet: PhotoSheet) : CandidateAction

    data object CloseSheet : CandidateAction

    data class UpdateDraft(val text: String) : CandidateAction

    data object SubmitText : CandidateAction

    data class ResetText(val lemma: String, val key: String, val field: PhotoTextField) : CandidateAction

    data class SetPartOfSpeech(val lemma: String, val key: String, val optionId: Long?) : CandidateAction

    data class SetCardTarget(val lemma: String, val target: PhotoCardTarget) : CandidateAction

    data class SetCardGroup(val lemma: String, val group: PhotoOwnGroup) : CandidateAction

    data class SetCommonGroup(val groupId: Long?) : CandidateAction

    data class SetMapping(val kind: PhotoDataKind, val target: PhotoTarget) : CandidateAction

    data object ToggleMapping : CandidateAction

    /** Диалог карточки (WCL-15): переименовать слово в лемму-форму или перенести в её слово при записи; повтор снимает. */
    data class ToggleLemmaWord(val choice: LemmaWordChoice) : CandidateAction

    data object ToggleNotAdded : CandidateAction

    data object RequestAdd : CandidateAction

    data object ConfirmAdd : CandidateAction

    data object CancelAdd : CandidateAction

    data object RetryAdd : CandidateAction

    data object RequestClose : CandidateAction

    data object ConfirmClose : CandidateAction

    data object Stay : CandidateAction
}

/**
 * Что рисуют общие виджеты: поля окна снимка без его запуска. Окно
 * снимка собирает вид из своего состояния, диалог карточки — из своего;
 * поля, которых у диалога нет (группы, «не добавить», вопрос перед
 * записью), остаются в умолчаниях.
 */
data class CandidatesView(
    val form: PhotoDictionaryForm?,
    val mapping: PhotoMapping,
    val cards: List<PhotoCard>,
    val summary: PhotoAddSummary,
    val sheet: PhotoSheet?,
    val isAdding: Boolean,
    val addFailure: PhotoAddFailure?,
    val isConfirmingClose: Boolean,
    val isMappingExpanded: Boolean,
    val notAdded: List<PhotoNotAdded> = emptyList(),
    val isNotAddedExpanded: Boolean = false,
    val commonGroupId: Long? = null,
    val isConfirmingAdd: Boolean = false,
    /** Диалог карточки (WCL-15): что сделать со словом диалога при записи; null — ничего. */
    val lemmaWordChoice: LemmaWordChoice? = null,
    /** Диалог карточки (WCL-15.4): по лемме-форме — сколько значений её слов совпадает с кандидатами и лексемами слова диалога. */
    val sameValues: Map<String, Int> = emptyMap(),
)
