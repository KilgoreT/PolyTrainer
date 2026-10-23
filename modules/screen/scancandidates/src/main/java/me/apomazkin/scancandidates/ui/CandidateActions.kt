package me.apomazkin.scancandidates.ui

import me.apomazkin.candidates.CandidateAction
import me.apomazkin.scancandidates.mate.Msg

/**
 * Действие общих виджетов окна → сообщение экрана. Действия диалога карточки
 * слова — переименование слова в лемму-форму и «Повторить» запрос слова — у
 * экрана снимка не используются: его «Повторить» рисует сам экран и шлёт
 * [Msg.RetryLookup].
 */
internal fun CandidateAction.toMsg(): Msg = when (this) {
    is CandidateAction.DeleteCard -> Msg.DeleteCard(lemma)
    is CandidateAction.DeleteLexeme -> Msg.DeleteLexeme(lemma, key)
    is CandidateAction.DeleteValue -> Msg.DeleteValue(ref)
    is CandidateAction.OpenSheet -> Msg.OpenSheet(sheet)
    CandidateAction.CloseSheet -> Msg.CloseSheet
    is CandidateAction.UpdateDraft -> Msg.UpdateDraft(text)
    is CandidateAction.SubmitText -> Msg.SubmitText(ref, text)
    is CandidateAction.ResetText -> Msg.ResetText(ref)
    is CandidateAction.SetOption -> Msg.SetOption(ref, option)
    is CandidateAction.SetCardTarget -> Msg.SetCardTarget(lemma, target)
    is CandidateAction.SetCardGroup -> Msg.SetCardGroup(lemma, group)
    is CandidateAction.SetCommonGroup -> Msg.SetCommonGroup(groupId)
    CandidateAction.ToggleComponentMatches -> Msg.ToggleComponentMatches
    is CandidateAction.ToggleLemmaWord -> Msg.NoOperation
    CandidateAction.ToggleNotAdded -> Msg.ToggleNotAdded
    CandidateAction.RequestAdd -> Msg.RequestAdd
    CandidateAction.ConfirmAdd -> Msg.ConfirmAdd
    CandidateAction.CancelAdd -> Msg.CancelAdd
    CandidateAction.RetryAdd -> Msg.RetryAdd
    CandidateAction.RetryLookup -> Msg.NoOperation
    CandidateAction.RequestClose -> Msg.RequestClose
    CandidateAction.ConfirmClose -> Msg.ConfirmClose
    CandidateAction.Stay -> Msg.Stay
}
