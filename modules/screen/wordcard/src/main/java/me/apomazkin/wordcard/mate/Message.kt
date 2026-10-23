package me.apomazkin.wordcard.mate

import androidx.annotation.StringRes
import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.candidates.CandidateAction
import me.apomazkin.candidates.ComponentRecommendationUi
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.LookupWordUi
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentValue
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.wordcard.deps.AvailableComponents
import me.apomazkin.wordcard.entity.Term

sealed interface Msg {
    // --- Top bar menu ---
    data object OpenTopBarMenu : Msg

    data object CloseTopBarMenu : Msg

    // --- Delete word ---
    data object OpenDeleteWordDialog : Msg

    data object CloseDeleteWordDialog : Msg

    data class RemoveWord(
        val wordId: Long,
    ) : Msg

    // --- Word edit ---
    data object EnterWordEditMode : Msg

    data class UpdateWordInput(
        val value: String,
    ) : Msg

    data object CommitWordChanges : Msg

    // --- Lexeme ---
    data object CreateLexeme : Msg

    data class OpenDeleteLexemeDialog(
        val lexemeId: Long,
    ) : Msg

    data object CloseDeleteLexemeDialog : Msg

    data class RemoveLexeme(
        val lexemeId: Long,
    ) : Msg

    // --- Component value (generic) ---
    data class CreateComponentValue(
        val lexemeId: Long,
        val typeId: ComponentTypeId,
    ) : Msg

    data class UpdateComponentValueInput(
        val lexemeId: Long,
        val key: ComponentValueKey,
        val value: String,
    ) : Msg

    data class EnterComponentValueEditMode(
        val lexemeId: Long,
        val key: ComponentValueKey,
    ) : Msg

    data class CommitComponentValueEdit(
        val lexemeId: Long,
        val key: ComponentValueKey,
    ) : Msg

    data class RemoveComponentValueRequested(
        val lexemeId: Long,
        val key: ComponentValueKey,
    ) : Msg

    // --- IS491: captioned_text ---

    /** Ввод в caption-поле открытого edit-режима captioned-значения. */
    data class UpdateComponentCaptionInput(
        val lexemeId: Long,
        val key: ComponentValueKey,
        val caption: String,
    ) : Msg

    /** Фокус caption-поля → one-shot загрузка подсказок компонента. */
    data class LoadCaptionSuggestions(
        val typeId: ComponentTypeId,
    ) : Msg

    data class CaptionSuggestionsLoaded(
        val typeId: ComponentTypeId,
        val suggestions: List<String>,
    ) : Msg

    /**
     * IS486: выбор опции CHOICE-компонента (пикер-диалог) — коммитит сразу, без edit-режима.
     * Reducer сам решает: существующее значение типа → UpdateValue, нет → AddValue.
     */
    data class SelectComponentOption(
        val lexemeId: Long,
        val typeId: ComponentTypeId,
        val optionId: Long,
    ) : Msg

    // --- Component types stream ---
    data class ComponentTypesLoaded(
        val available: AvailableComponents,
    ) : Msg

    data class ComponentTypesLoadFailed(
        val error: Throwable,
    ) : Msg

    data object RetryLoadComponentTypes : Msg

    // --- Datasource events ---
    data class WordLoaded(
        val word: Term,
    ) : Msg

    data object WordNotFound : Msg

    data class RefreshWord(
        val word: Term,
    ) : Msg

    data class RefreshLexemeComponents(
        val lexemeId: Long,
        val components: List<ComponentValue>,
    ) : Msg

    data class ComponentValueInserted(
        val lexemeId: Long,
        val pristineKey: Long,
        val newCvId: ComponentValueId,
    ) : Msg

    data class LexemeDraftPromoted(
        val newLexeme: Lexeme,
        val anchorPristineKey: Long,
    ) : Msg

    // --- Delete / undo ---
    // IS486 фаза 3: LexemeCascadeRemoved упразднён (деградация в черновик, spec §9.1).
    data class LexemeRemoved(
        val removedLexeme: Lexeme,
    ) : Msg

    data class UndoRestoreLexeme(
        val lexeme: Lexeme,
    ) : Msg

    data class RestoreLexemeFailed(
        val snapshot: Lexeme,
    ) : Msg

    // --- IS493 Э5: группы слова (пикер + чипы, D22) ---

    /** Иконка шапки либо тап по чипу (В4) → пикер групп. */
    data object OpenGroupPicker : Msg

    data object DismissGroupPicker : Msg

    /** Тап галочки пикера: направление (add/remove) reducer берёт из
     * wordGroupIds — факта БД (D22.3). */
    data class ToggleGroupMembership(
        val groupId: Long,
    ) : Msg

    /** Эмиссия подписки групп словаря (Collator handler'а). */
    data class DictGroupsLoaded(
        val groups: List<GroupUi>,
    ) : Msg

    /** Эмиссия подписки членств слова — единственный мутатор wordGroupIds. */
    data class WordGroupsLoaded(
        val ids: Set<Long>,
    ) : Msg

    /** Плоский итог membership-мутации (handler маппит outcome ДО отправки). */
    data class MembershipDone(
        val groupId: Long,
    ) : Msg

    /** Исключение membership-эффекта: снять in-flight. */
    data class MembershipFailed(
        val groupId: Long,
    ) : Msg

    // --- IS533/IS536: лексемы-кандидаты в карточке (WCL) ---
    // Ответы эффектов диалога несут номер запуска: чужой ответ отсекается (WCL-11.4).

    /** Кнопка «Добавить лексемы-кандидаты» → диалог и запрос (WCL-03). */
    data object OpenCandidates : Msg

    /** Хендлер запроса, исход Found: слово ответа с леммами и рекомендации ко всему ответу. */
    data class CandidatesFound(
        val word: LookupWordUi,
        val recommendations: List<ComponentRecommendationUi>,
        val requestId: Long,
    ) : Msg

    /** Хендлер запроса, исход NotFound: ни одной лексемы-кандидата (WCL-04.7, WCL-11.1). */
    data class CandidatesNotFound(
        val requestId: Long,
    ) : Msg

    /** Хендлер запроса: слово удалено или отказ сервера — текст без «Повторить» (WCL-11.2). */
    data class CandidatesLookupFailed(
        @StringRes val errorRes: Int,
        val requestId: Long,
    ) : Msg

    /**
     * Хендлер запроса: сервер не ответил, 503 или слово, по которому сервер не
     * справился, — текст и «Повторить» (WCL-11.7, WCL-11.9).
     */
    data class CandidatesLookupUnavailable(
        @StringRes val errorRes: Int,
        val requestId: Long,
    ) : Msg

    /** Хендлер формы, исход Loaded: форма словаря слова с опциями, зависимостями и словами. */
    data class CandidatesFormLoaded(
        val form: DictionaryFormUi,
        val requestId: Long,
    ) : Msg

    /** Хендлер формы: словарь удалён или чтение упало — текст причины (WCL-11.5). */
    data class CandidatesFormFailed(
        @StringRes val errorRes: Int,
        val requestId: Long,
    ) : Msg

    /** Записано: лексем добавлено (WCL-10). */
    data class CandidatesAdded(
        val lexemeCount: Int,
        val requestId: Long,
    ) : Msg

    /** Перенесено (WCL-15.3): слова диалога больше нет — его лексемы и новые ушли в слово леммы. */
    data class CandidatesTransferred(
        val requestId: Long,
    ) : Msg

    /** Отказ записи: слово диалога или получатель переноса удалены (WCL-13.2, WCL-15.6). */
    data class CandidatesWordGone(
        val failure: AddFailureUi.WordMissing,
        val requestId: Long,
    ) : Msg

    /** Отказ записи: компонент из сопоставления выключен (WCL-13.3, AWP-21.6). */
    data class CandidatesComponentGone(
        val failure: AddFailureUi.TypeUnavailable,
        val requestId: Long,
    ) : Msg

    /** Прочий отказ или ошибка записи — «Не добавилось» с «Повторить» (WCL-13.1). */
    data class CandidatesAddFailed(
        val failure: AddFailureUi,
        val requestId: Long,
    ) : Msg

    /** Есть ли ключ сервера в сборке (WCL-08). */
    data class ServerAccessLoaded(
        val hasAccess: Boolean,
    ) : Msg

    /** Действие пользователя в диалоге — общие виджеты модуля candidates. */
    data class Candidates(
        val action: CandidateAction,
    ) : Msg

    // --- Errors / nav ---
    data class OperationFailed(
        @StringRes val messageRes: Int,
    ) : Msg

    data object NavigateBack : Msg

    data object NoOperation : Msg
}
