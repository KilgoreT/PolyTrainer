package me.apomazkin.scancandidates.mate

import androidx.annotation.StringRes
import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.candidates.CandidatesSheetUi
import me.apomazkin.candidates.CardOwnGroupUi
import me.apomazkin.candidates.CardTargetUi
import me.apomazkin.candidates.ComponentRecommendationUi
import me.apomazkin.candidates.ComponentRefUi
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.ExistingLemmaUi
import me.apomazkin.candidates.LookupWordUi
import me.apomazkin.candidates.ValueRefUi
import me.apomazkin.lexeme.LookupToken

/**
 * Сообщения экрана снимка. Сначала ответы эффектов: хендлер разбирает исход
 * юзкейса и шлёт сообщение на каждый случай, редьюсер исходов не видит. Затем
 * действия пользователя. Адрес слова — его текст в списках; адрес правки —
 * лемма карточки, как у общих виджетов. Номера запуска нет: экран живёт один
 * снимок, закрыли экран — ответу некуда прийти; слово удалено — ответу некуда
 * лечь (AWP-48).
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
     * Исход запроса `Found`: ответ сервера на токены (LFT-14) — [dictionaryId]
     * словаря снимка, исправленный [text], его [translation] и четыре списка
     * [lists] словами.
     */
    data class LemmasFound(
        val dictionaryId: Long,
        val text: String,
        val translation: String,
        val lists: ScanWordListsUi,
    ) : Msg

    /** Исход запроса `NoWords`: в ответе ни словарной формы, ни «не разобрано». */
    data object LemmasEmpty : Msg

    /**
     * Исход запроса `Denied`, кроме недоступной нейронки: отказ по ключу,
     * лимиту, размеру или языку, «Повторить» нет (AWP-53).
     */
    data class LemmasDenied(
        @param:StringRes val errorRes: Int,
    ) : Msg

    /**
     * Сети нет, сервер не ответил, отказ 503 или сбой эффекта: «Повторить»
     * есть (AWP-53.1, AWP-53.5).
     */
    data class LemmasUnavailable(
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

    // ===== От эффектов: значения слова =====

    /**
     * Исход запроса по лемме `Found`: [text] — слово списка, [word] — слово
     * ответа с леммой и замечаниями, [recommendations] — советы завести
     * компонент, [existing] — леммы словаря с текстом леммы ответа, когда она
     * не из списков («children» → «child»); пусто, если лемма та же (AWP-43.7).
     */
    data class LexemesFound(
        val text: String,
        val word: LookupWordUi,
        val recommendations: List<ComponentRecommendationUi>,
        val existing: List<ExistingLemmaUi>,
    ) : Msg

    /**
     * Исходы `NotFound` и `NoLexemes`: словарной формы в базе нет или нейронка
     * ничего не выбрала, [textRes] — «Не найдено» или «Значений нет» (AWP-55).
     */
    data class LexemesNotFound(
        val text: String,
        @param:StringRes val textRes: Int,
    ) : Msg

    /** Исход `Denied`, кроме нейронки: отказ по ключу, лимиту, размеру или языку, «Повторить» нет (AWP-61). */
    data class LexemesDenied(
        val text: String,
        @param:StringRes val errorRes: Int,
    ) : Msg

    /**
     * Сети нет, сервер не ответил, нейронка недоступна или по слову не
     * справилась, сбой эффекта: «Повторить» есть (AWP-61.1, 61.2, 61.5).
     */
    data class LexemesUnavailable(
        val text: String,
        @param:StringRes val errorRes: Int,
    ) : Msg

    // ===== От эффектов: запись =====

    /** Исход записи `Added`: слово [text] записано, строки прочь (AWP-47). */
    data class WordAdded(
        val text: String,
    ) : Msg

    /** Отказ записи: группа слова удалена (AWP-21.3, 21.5). */
    data class AddGroupGone(
        val text: String,
        val failure: AddFailureUi.GroupMissing,
    ) : Msg

    /** Отказ записи: выбранная лемма словаря удалена (AWP-21.4). */
    data class AddWordGone(
        val text: String,
        val failure: AddFailureUi.WordMissing,
    ) : Msg

    /** Отказ записи: компонент выключили (AWP-21.6). */
    data class AddComponentGone(
        val text: String,
        val failure: AddFailureUi.TypeUnavailable,
    ) : Msg

    /** Остальные отказы и сбой записи: причина как есть под словом (AWP-21.1). */
    data class AddFailed(
        val text: String,
        val failure: AddFailureUi,
    ) : Msg

    // ===== От экрана: запрос, перевод, закрытие =====

    /** «Повторить» на ошибке: запрос заново с теми же токенами (TFP-26). */
    data object RetryLemmas : Msg

    /** Тап по блоку перевода (AWP-41). */
    data object ToggleTranslation : Msg

    /** Крестик, системный «назад» или «Отменить» на индикаторе. */
    data object RequestClose : Msg

    /** «Закрыть» в вопросе (AWP-51.4). */
    data object ConfirmClose : Msg

    /** «Остаться» в вопросе (AWP-51.3). */
    data object Stay : Msg

    // ===== От экрана: слово =====

    /**
     * Тап по новому слову без карточки, «Проверить по тексту» у слова «в
     * словаре» или «Повторить» на ошибке слова: запрос значений (AWP-43,
     * AWP-49, AWP-61.7).
     */
    data class LoadLexemes(
        val text: String,
    ) : Msg

    /** Тап по слову «в словаре» без карточки: сохранённые лексемы (AWP-46). */
    data class OpenSavedCard(
        val text: String,
    ) : Msg

    /** Повторный тап по слову с карточкой: свернуть или раскрыть (AWP-42). */
    data class ToggleCard(
        val text: String,
    ) : Msg

    /** «Удалить» у слова (AWP-48). */
    data class DeleteWord(
        val text: String,
    ) : Msg

    /** «Добавить» у слова (AWP-02, AWP-47). */
    data class AddWord(
        val text: String,
    ) : Msg

    /** «Повторить» под словом после «не добавилось» (AWP-26). */
    data class RetryAdd(
        val text: String,
    ) : Msg

    /** Заголовок «Сопоставления» в карточке слова (AWP-31.4). */
    data class ToggleComponentMatches(
        val text: String,
    ) : Msg

    // ===== От экрана: правки карточки =====

    data class DeleteLexeme(
        val lemma: String,
        val key: String,
    ) : Msg

    /** Крестик у значения компонента (AWP-15.2, 15.7). */
    data class DeleteValue(
        val ref: ValueRefUi,
    ) : Msg

    /** «Сохранить» в листе правки: адрес и текст из листа, сообщение самодостаточно (AWP-16). */
    data class SubmitText(
        val ref: ValueRefUi,
        val text: String,
    ) : Msg

    data class ResetText(
        val ref: ValueRefUi,
    ) : Msg

    /** Опция «Выбора» у лексемы-кандидата, null — «без значения» (AWP-17). */
    data class SetOption(
        val ref: ComponentRefUi,
        val option: Long?,
    ) : Msg

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

    // ===== От экрана: общая группа и листы =====

    data class OpenSheet(
        val sheet: CandidatesSheetUi,
    ) : Msg

    data object CloseSheet : Msg

    /** Общая группа окна, null — «Все». */
    data class SetCommonGroup(
        val groupId: Long?,
    ) : Msg

    /** Действие общих виджетов, которого у экрана снимка нет. */
    data object NoOperation : Msg
}
