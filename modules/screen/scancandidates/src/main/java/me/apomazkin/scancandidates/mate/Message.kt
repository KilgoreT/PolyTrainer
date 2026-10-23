package me.apomazkin.scancandidates.mate

import androidx.annotation.StringRes
import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.candidates.CandidatesSheetUi
import me.apomazkin.candidates.CardOwnGroupUi
import me.apomazkin.candidates.CardTargetUi
import me.apomazkin.candidates.ComponentRecommendationUi
import me.apomazkin.candidates.ComponentRefUi
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.LookupWordUi
import me.apomazkin.candidates.ValueRefUi
import me.apomazkin.lexeme.LookupToken

/**
 * Сообщения экрана лексем-кандидатов. Сначала ответы эффектов: хендлер
 * разбирает исход юзкейса и шлёт сообщение на каждый случай, редьюсер исходов
 * не видит. Затем действия пользователя. Номера запуска нет: экран живёт один
 * снимок, закрыли экран — ответу некуда прийти.
 */
sealed interface Msg {
    // ===== От эффектов: снимок, запрос, форма =====

    /** Исход распознавания `Text`: токены как распознаны, с повторами. */
    data class TextRecognized(
        val tokens: List<LookupToken>,
    ) : Msg

    /** Исход распознавания `Empty`: на снимке ни одного слова. */
    data object TextEmpty : Msg

    /** Распознавание упало: снимок не прочитался, [errorRes] — текст причины. */
    data class RecognitionFailed(
        @param:StringRes val errorRes: Int,
    ) : Msg

    /**
     * Исход запроса `Words`: слова ответа с лексемами-кандидатами и рекомендации
     * ко всему ответу.
     */
    data class WordsFound(
        val dictionaryId: Long,
        val words: List<LookupWordUi>,
        val recommendations: List<ComponentRecommendationUi>,
    ) : Msg

    /** Исход запроса `NoWords`: сервер слов не нашёл. */
    data object WordsEmpty : Msg

    /**
     * Исход запроса `Denied`, кроме недоступной нейронки: отказ по ключу или
     * лимиту, «Повторить» нет.
     */
    data class LookupDenied(
        @param:StringRes val errorRes: Int,
    ) : Msg

    /**
     * Сети нет или сервер недоступен: исход `Unavailable`, отказ 503 или сбой
     * эффекта, «Повторить» есть.
     */
    data class LookupUnavailable(
        @param:StringRes val errorRes: Int,
    ) : Msg

    /** Исход формы `Loaded`: компоненты с опциями и зависимостями, группы, леммы словаря. */
    data class FormLoaded(
        val form: DictionaryFormUi,
    ) : Msg

    /** Форма не прочиталась: словарь удалён или чтение упало, [errorRes] — текст. */
    data class FormFailed(
        @param:StringRes val errorRes: Int,
    ) : Msg

    // ===== От эффектов: запись =====

    /** Исход записи `Added`: сколько значений записано и в сколько слов. */
    data class AddFinished(
        val lexemeCount: Int,
        val wordCount: Int,
    ) : Msg

    /** Отказ записи: группа слова удалена. */
    data class AddGroupGone(
        val failure: AddFailureUi.GroupMissing,
    ) : Msg

    /** Отказ записи: выбранное слово словаря удалено. */
    data class AddWordGone(
        val failure: AddFailureUi.WordMissing,
    ) : Msg

    /** Отказ записи: компонент выключили. */
    data class AddComponentGone(
        val failure: AddFailureUi.TypeUnavailable,
    ) : Msg

    /** Остальные отказы и сбой записи: причина как есть для панели. */
    data class AddFailed(
        val failure: AddFailureUi,
    ) : Msg

    // ===== От экрана: запрос и закрытие =====

    /** «Повторить» на ошибке: запрос заново с теми же токенами (Д53). */
    data object RetryLookup : Msg

    /** Крестик или системный «назад». */
    data object RequestClose : Msg

    /** «Закрыть» в вопросе. */
    data object ConfirmClose : Msg

    /** «Остаться» в вопросе. */
    data object Stay : Msg

    // ===== От экрана: карточки и значения =====

    data class DeleteCard(
        val lemma: String,
    ) : Msg

    data class DeleteLexeme(
        val lemma: String,
        val key: String,
    ) : Msg

    /** Крестик у значения компонента. */
    data class DeleteValue(
        val ref: ValueRefUi,
    ) : Msg

    /** «Сохранить» в листе правки: адрес и текст из листа, сообщение самодостаточно. */
    data class SubmitText(
        val ref: ValueRefUi,
        val text: String,
    ) : Msg

    data class ResetText(
        val ref: ValueRefUi,
    ) : Msg

    /** Опция «Выбора» у лексемы-кандидата, null — «без значения». */
    data class SetOption(
        val ref: ComponentRefUi,
        val option: Long?,
    ) : Msg

    // ===== От экрана: листы, группы, куда класть =====

    data class OpenSheet(
        val sheet: CandidatesSheetUi,
    ) : Msg

    data object CloseSheet : Msg

    /** Ввод в листе правки, каждое изменение. */
    data class UpdateDraft(
        val text: String,
    ) : Msg

    data class SetCardTarget(
        val lemma: String,
        val target: CardTargetUi,
    ) : Msg

    data class SetCardGroup(
        val lemma: String,
        val group: CardOwnGroupUi,
    ) : Msg

    /** Общая группа окна, null — «Все». */
    data class SetCommonGroup(
        val groupId: Long?,
    ) : Msg

    // ===== От экрана: показ =====

    data object ToggleComponentMatches : Msg

    data object ToggleNotAdded : Msg

    // ===== От экрана: запись =====

    /** Кнопка «Добавить». */
    data object RequestAdd : Msg

    /** «Да» в вопросе о добавлении. */
    data object ConfirmAdd : Msg

    /** «Отмена» в вопросе о добавлении. */
    data object CancelAdd : Msg

    /** «Повторить» в «не добавилось». */
    data object RetryAdd : Msg

    /** Действие, которого у экрана снимка нет. */
    data object NoOperation : Msg
}
