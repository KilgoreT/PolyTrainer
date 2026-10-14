package me.apomazkin.wordstab.ui.widget.photo

import me.apomazkin.candidates.CandidateAction
import me.apomazkin.candidates.CandidatesView
import me.apomazkin.wordstab.logic.Msg
import me.apomazkin.wordstab.logic.PhotoWordsState

/*
 * IS533: общие виджеты окна живут в модуле candidates и шлют
 * [CandidateAction]; здесь — его перевод в Msg вкладки и вид окна из
 * состояния вкладки.
 */

/** Действие общего виджета → сообщение вкладки «Слова». */
internal fun CandidateAction.toMsg(): Msg = when (this) {
    is CandidateAction.ToggleCard -> Msg.TogglePhotoCard(lemma)
    is CandidateAction.DeleteCard -> Msg.DeletePhotoCard(lemma)
    is CandidateAction.DeleteLexeme -> Msg.DeletePhotoLexeme(lemma, key)
    is CandidateAction.DeleteExample -> Msg.DeletePhotoExample(lemma, key, index)
    is CandidateAction.OpenSheet -> Msg.OpenPhotoSheet(sheet)
    CandidateAction.CloseSheet -> Msg.ClosePhotoSheet
    is CandidateAction.UpdateDraft -> Msg.UpdatePhotoDraft(text)
    CandidateAction.SubmitText -> Msg.SubmitPhotoText
    is CandidateAction.ResetText -> Msg.ResetPhotoText(lemma, key, field)
    is CandidateAction.SetPartOfSpeech -> Msg.SetPhotoPartOfSpeech(lemma, key, optionId)
    is CandidateAction.SetCardTarget -> Msg.SetPhotoCardTarget(lemma, target)
    is CandidateAction.SetCardGroup -> Msg.SetPhotoCardGroup(lemma, group)
    is CandidateAction.SetCommonGroup -> Msg.SetPhotoCommonGroup(groupId)
    is CandidateAction.SetMapping -> Msg.SetPhotoMapping(kind, target)
    CandidateAction.ToggleMapping -> Msg.TogglePhotoMapping
    // Переименование и перенос — только у диалога карточки слова (WCL-15); в окне снимка слова новые.
    is CandidateAction.ToggleLemmaWord -> Msg.NoOperation
    CandidateAction.ToggleNotAdded -> Msg.TogglePhotoNotAdded
    CandidateAction.RequestAdd -> Msg.RequestPhotoAdd
    CandidateAction.ConfirmAdd -> Msg.ConfirmPhotoAdd
    CandidateAction.CancelAdd -> Msg.CancelPhotoAdd
    CandidateAction.RetryAdd -> Msg.RetryPhotoAdd
    CandidateAction.RequestClose -> Msg.RequestClosePhotoWords
    CandidateAction.ConfirmClose -> Msg.ConfirmClosePhotoWords
    CandidateAction.Stay -> Msg.StayInPhotoWords
}

/** Вид окна из состояния диалога «слова с фото». */
internal fun PhotoWordsState.toCandidatesView(): CandidatesView = CandidatesView(
    form = form,
    mapping = mapping,
    cards = cards,
    summary = summary,
    sheet = sheet,
    isAdding = isAdding,
    addFailure = addFailure,
    isConfirmingClose = isConfirmingClose,
    isMappingExpanded = isMappingExpanded,
    notAdded = notAdded,
    isNotAddedExpanded = isNotAddedExpanded,
    commonGroupId = commonGroupId,
    isConfirmingAdd = isConfirmingAdd,
)
