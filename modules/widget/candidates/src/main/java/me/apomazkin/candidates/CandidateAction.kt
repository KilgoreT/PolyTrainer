package me.apomazkin.candidates

/*
 * IS533/IS536: действия пользователя в окне снимка и в диалоге карточки
 * слова — одни и те же виджеты шлют их наружу, экран превращает в свой
 * Msg. Порядок — по частоте: карточки и лексемы, листы и правка, запись,
 * закрытие.
 */
sealed interface CandidateAction {
    data class DeleteCard(val lemma: String) : CandidateAction

    data class DeleteLexeme(val lemma: String, val key: String) : CandidateAction

    /** Крестик у значения компонента (AWP-15.2, 15.7). */
    data class DeleteValue(val ref: ValueRefUi) : CandidateAction

    data class OpenSheet(val sheet: CandidatesSheetUi) : CandidateAction

    data object CloseSheet : CandidateAction

    data class UpdateDraft(val text: String) : CandidateAction

    /** «Сохранить» в листе правки: адрес значения и текст из листа — сообщение самодостаточно (AWP-16). */
    data class SubmitText(val ref: ValueRefUi, val text: String) : CandidateAction

    data class ResetText(val ref: ValueRefUi) : CandidateAction

    /** Опция «Выбора» у лексемы-кандидата, null — «без значения» (AWP-17). */
    data class SetOption(val ref: ComponentRefUi, val option: Long?) : CandidateAction

    /** «Куда» плашки нейронки (AWP-18): лемма карточки, ключ лексемы-кандидата, выбор. */
    data class SetCardTarget(val lemma: String, val key: String, val target: CardTargetUi) : CandidateAction

    /** Своя группа плашки при «новое слово» (AWP-08). */
    data class SetCardGroup(val lemma: String, val key: String, val group: CardOwnGroupUi) : CandidateAction

    data class SetCommonGroup(val groupId: Long?) : CandidateAction

    /** Заголовок раздела «Сопоставление» (AWP-31.4). */
    data object ToggleComponentMatches : CandidateAction

    /** Диалог карточки (WCL-15): переименовать слово в лемму-форму или перенести в её слово при записи; повтор снимает. */
    data class ToggleLemmaWord(val choice: LemmaWordChoice) : CandidateAction

    data object RequestAdd : CandidateAction

    data object ConfirmAdd : CandidateAction

    data object CancelAdd : CandidateAction

    data object RetryAdd : CandidateAction

    /** «Повторить» запрос в диалоге карточки: сервер не ответил или недоступен (WCL-11.9). */
    data object RetryLookup : CandidateAction

    data object RequestClose : CandidateAction

    data object ConfirmClose : CandidateAction

    data object Stay : CandidateAction
}

/**
 * Что рисуют общие виджеты: поля окна снимка без его запуска. Окно
 * снимка собирает вид из своего состояния — карточки лемм всех слов для
 * листов правки и «куда класть», — диалог карточки — из своего; поля,
 * которых у диалога нет (группы, вопрос перед записью), остаются в
 * умолчаниях.
 */
data class CandidatesView(
    val form: DictionaryFormUi?,
    val cards: List<LemmaCardUiItem>,
    val summary: AddSummaryUi,
    val sheet: CandidatesSheetUi?,
    val isAdding: Boolean,
    val addFailure: AddFailureUi?,
    val isConfirmingClose: Boolean,
    val remarks: List<RemarkUiItem> = emptyList(),
    val componentMatchLines: List<ComponentMatchLineUiItem> = emptyList(),
    val isComponentMatchesExpanded: Boolean = false,
    val commonGroupId: Long? = null,
    val isConfirmingAdd: Boolean = false,
    /** Диалог карточки (WCL-15): что сделать со словом диалога при записи; null — ничего. */
    val lemmaWordChoice: LemmaWordChoice? = null,
    /** Диалог карточки (WCL-15.4): по лемме-форме — сколько значений её слов совпадает с кандидатами и лексемами слова диалога. */
    val sameValues: Map<String, Int> = emptyMap(),
)
