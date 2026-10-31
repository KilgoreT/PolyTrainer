package me.apomazkin.scancandidates.ui

import me.apomazkin.candidates.CandidateAction
import me.apomazkin.scancandidates.mate.Msg

/**
 * Действие общих виджетов окна → сообщение экрана с теми же данными: правки
 * плашки, листы, «Куда», своя и общая группы, закрытие и вопрос о нём; крестик
 * лексемы у виджета — то же, что «Не нужно» (AWP-64). Крестик карточки, кнопки
 * панели окна первой версии и действия диалога карточки слова у экрана снимка
 * не живут — ничего не меняют. Действия строки слова и плашек — свой тип
 * [WordAction].
 */
internal fun CandidateAction.toMsg(): Msg = when (this) {
    is CandidateAction.DeleteLexeme -> Msg.DismissLexeme(lemma, key)
    is CandidateAction.DeleteValue -> Msg.DeleteValue(ref)
    is CandidateAction.OpenSheet -> Msg.OpenSheet(sheet)
    CandidateAction.CloseSheet -> Msg.CloseSheet
    is CandidateAction.UpdateDraft -> Msg.UpdateDraft(text)
    is CandidateAction.SubmitText -> Msg.SubmitText(ref, text)
    is CandidateAction.ResetText -> Msg.ResetText(ref)
    is CandidateAction.SetOption -> Msg.SetOption(ref, option)
    is CandidateAction.SetCardTarget -> Msg.SetCardTarget(lemma, key, target)
    is CandidateAction.SetCardGroup -> Msg.SetCardGroup(lemma, key, group)
    is CandidateAction.SetCommonGroup -> Msg.SetCommonGroup(groupId)
    CandidateAction.RequestClose -> Msg.RequestClose
    CandidateAction.ConfirmClose -> Msg.ConfirmClose
    CandidateAction.Stay -> Msg.Stay
    is CandidateAction.DeleteCard,
    CandidateAction.ToggleComponentMatches,
    is CandidateAction.ToggleLemmaWord,
    CandidateAction.RequestAdd,
    CandidateAction.ConfirmAdd,
    CandidateAction.CancelAdd,
    CandidateAction.RetryAdd,
    CandidateAction.RetryLookup,
    -> Msg.NoOperation
}
