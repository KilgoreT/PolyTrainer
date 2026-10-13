package me.apomazkin.wordstab.logic

import androidx.annotation.StringRes
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.textrecognition.RecognizedWord
import me.apomazkin.wordrow.entity.TermUiItem
import me.apomazkin.wordstab.deps.PhotoDataKind
import me.apomazkin.wordstab.deps.PhotoFormOutcome
import me.apomazkin.wordstab.deps.PhotoLookupOutcome
import me.apomazkin.wordstab.deps.PhotoTarget
import me.apomazkin.wordstab.entity.WordInfo
import me.apomazkin.wordstab.logic.photo.PhotoAddFailure
import me.apomazkin.wordstab.logic.photo.PhotoCardTarget
import me.apomazkin.wordstab.logic.photo.PhotoOwnGroup
import me.apomazkin.wordstab.logic.photo.PhotoSheet
import me.apomazkin.wordstab.logic.photo.PhotoTextField


sealed interface Msg {

    /**
     * Message to open WordCard screen for given word id.
     */
    data class OpenWordCard(val wordId: Long) : Msg

    /**
     * Message to select and activate a dictionary for word operations.
     * IS476: nullable — null = реактивно получили "нет словаря".
     * @param current The dictionary entity to be selected as active, or null if no dictionaries.
     */
    data class SelectDictionary(val current: DictUiEntity?) : Msg

    /**
     * Message indicating that terms data has been loaded from the repository.
     * @param pattern The search pattern used to filter terms.
     * @param termList Flow of paginated term items to be displayed.
     */
    data class TermsLoaded(
            val pattern: String,
            val termList: Flow<PagingData<TermUiItem>>,
    ) : Msg

    /**
     * Message to open dialog for creating a new word.
     * @param wordValue Optional pre-filled text value for the new word.
     */
    data class OpenAddWordDialog(
            val wordValue: String? = null,
    ) : Msg

    /**
     * Message to close the add word dialog.
     */
    data object CloseAddWordDialog : Msg

    /**
     * Message to open dialog for editing an existing word.
     * @param wordId The unique identifier of the word to be edited.
     * @param wordValue The current text value of the word to be edited.
     */
    data class OpenEditWordDialog(
            val wordId: Long,
            val wordValue: String,
    ) : Msg

    /**
     * Message to update the input field value when user types in word creation/editing dialog.
     * @param value The current text value entered by the user.
     */
    data class UpdateWordInput(val value: String) : Msg

    /**
     * Message to create new word in the dictionary.
     * @param value The text value of the word to be created.
     */
    data class CreateWord(val value: String) : Msg

    /**
     * Message to update existing word in the dictionary.
     * @param wordId The unique identifier of the word to be updated.
     * @param value The new text value for the word.
     */
    data class UpdateWord(val wordId: Long, val value: String) : Msg

    /**
     * Message to open confirmation dialog for word deletion.
     * @param wordIds Set of words selected for deletion.
     */
    data class OpenDeleteConfirmation(val wordIds: Set<WordInfo>) : Msg

    /**
     * Message to close the word deletion confirmation dialog.
     */
    data object CloseDeleteConfirmation : Msg

    /**
     * Message to remove words from the dictionary.
     * @param wordIds Set of words to be removed.
     */
    data class RemoveWords(val wordIds: Set<WordInfo>) : Msg

    /**
     * Message to enter selection mode for multiple word operations.
     * @param targetWord The word that triggered the selection mode.
     */
    data class EnterSelectionMode(val targetWord: WordInfo) : Msg

    /**
     * Message to exit selection mode.
     */
    data object ExitSelectionMode : Msg

    /**
     * Message to toggle word selection in selection mode.
     * @param targetWord The word to toggle selection for.
     */
    data class ToggleSelection(val targetWord: WordInfo) : Msg

    /** IS527: кнопка-камера — открыть выбор источника фото. */
    data object OpenPhotoSourceChooser : Msg

    /**
     * IS527: камера или галерея вернули снимок. Uri строкой — Android-классы
     * в unit-тестах модуля заглушены; в Uri превращает исполнитель эффекта.
     */
    data class PhotoPicked(val uri: String) : Msg

    /** IS527: камера или галерея закрыты без снимка — назад к выбору источника. */
    data object PhotoPickCancelled : Msg

    /**
     * IS529: распознавание завершено, токены подготовлены к отправке (Ш5:
     * без пустых и дублей, не больше 2000). Пустой список — «слов не
     * найдено» без запроса (WFT-60.1). [requestId] — чей это результат.
     */
    data class TokensReady(
            val tokens: List<RecognizedWord>,
            val requestId: Long,
    ) : Msg

    /**
     * IS529: сервер слов ответил или не ответил (Ш10, В3, В5). Чужой
     * [requestId] — поздний ответ отменённого запуска, отбрасывается (WFT-62).
     */
    data class WordsLookedUp(
            val outcome: PhotoLookupOutcome,
            val requestId: Long,
    ) : Msg

    /** IS527: распознавание упало или камера не запустилась — текст ошибки ресурсом. */
    data class RecognitionFailed(
            @StringRes val errorRes: Int,
            val requestId: Long,
    ) : Msg

    /** IS527: закрыть диалог слов с фото на любом шаге без вопросов (кнопка «отмена», ошибка). */
    data object ClosePhotoWords : Msg

    // ===== IS532: окно добавления =====

    /**
     * Ш1: форма словаря снимка загружена или нет (AWP-24). Чужой
     * [requestId] отбрасывается (AWP-24.2).
     */
    data class PhotoFormLoaded(
            val outcome: PhotoFormOutcome,
            val requestId: Long,
    ) : Msg

    /** Ш2: развернуть или свернуть карточку леммы. */
    data class TogglePhotoCard(val lemma: String) : Msg

    /** Ш3: удалить карточку — лемму со всем, что в ней (AWP-15). */
    data class DeletePhotoCard(val lemma: String) : Msg

    /** Ш3: удалить лексему-кандидата с её примерами (AWP-15.1). */
    data class DeletePhotoLexeme(val lemma: String, val key: String) : Msg

    /** Ш3: удалить один пример лексемы-кандидата (AWP-15.2). */
    data class DeletePhotoExample(val lemma: String, val key: String, val index: Int) : Msg

    /** Ш3: открыть лист выбора или правки текста. */
    data class OpenPhotoSheet(val sheet: PhotoSheet) : Msg

    /** Ш3: закрыть лист без выбора. */
    data object ClosePhotoSheet : Msg

    /** Ш3: ввод в листе правки текста — полный текст поля. */
    data class UpdatePhotoDraft(val text: String) : Msg

    /** Ш3: принять правку текста из листа (AWP-16). */
    data object SubmitPhotoText : Msg

    /** Ш3: вернуть тексту лексемы-кандидата текст источника (AWP-16.6). */
    data class ResetPhotoText(val lemma: String, val key: String, val field: PhotoTextField) : Msg

    /** Ш3: часть речи лексемы-кандидата — опция или null «без части речи» (AWP-17). */
    data class SetPhotoPartOfSpeech(val lemma: String, val key: String, val optionId: Long?) : Msg

    /** Ш3: куда класть лексемы карточки (AWP-18). */
    data class SetPhotoCardTarget(val lemma: String, val target: PhotoCardTarget) : Msg

    /** Ш3: своя группа карточки (AWP-08). */
    data class SetPhotoCardGroup(val lemma: String, val group: PhotoOwnGroup) : Msg

    /** Ш3: общая группа — группа словаря или null «Все» (AWP-08). */
    data class SetPhotoCommonGroup(val groupId: Long?) : Msg

    /** Ш3: цель вида данных — компонент или «не добавлять» (AWP-09). */
    data class SetPhotoMapping(val kind: PhotoDataKind, val target: PhotoTarget) : Msg

    /** Ш2: раскрыть или свернуть блок соответствий. */
    data object TogglePhotoMapping : Msg

    /** Ш2: раскрыть или свернуть блок «не добавить». */
    data object TogglePhotoNotAdded : Msg

    /** Ш4: нажата «добавить» — вопрос «Добавить N значений в M слов?» (AWP-13). */
    data object RequestPhotoAdd : Msg

    /** Ш4: «да» в вопросе — запись (AWP-20). */
    data object ConfirmPhotoAdd : Msg

    /** Ш4: «отмена» в вопросе — окно как было (AWP-13.7). */
    data object CancelPhotoAdd : Msg

    /** В3: «повторить» — запись заново без вопроса (AWP-26). */
    data object RetryPhotoAdd : Msg

    /** Ш4 → Ш5: записано; счёт — для сообщения (AWP-23). Чужой [requestId] отбрасывается. */
    data class PhotoAddFinished(
            val lexemeCount: Int,
            val wordCount: Int,
            val requestId: Long,
    ) : Msg

    /** Ш4 → В3: отказ проверки с причиной или сбой записи (AWP-21). Чужой [requestId] отбрасывается. */
    data class PhotoAddRejected(
            val failure: PhotoAddFailure,
            val requestId: Long,
    ) : Msg

    /** Ш2: крестик или системная «назад» — закрыть сразу или спросить (AWP-19). */
    data object RequestClosePhotoWords : Msg

    /** В2: «закрыть» в вопросе (AWP-25). */
    data object ConfirmClosePhotoWords : Msg

    /** В2: «остаться» в вопросе (AWP-25.2). */
    data object StayInPhotoWords : Msg

    /**
     * Message indicating no operation is needed after an effect.
     */
    data object NoOperation : Msg
}

sealed interface UiMsg : Msg {
    /**
     * Message to show or hide notification snackbar.
     * @param message The text content of the notification.
     * @param show Flag to control notification visibility.
     */
    data class ShowNotification(val message: String, val show: Boolean) : UiMsg
    /**
     * Message to handle application lifecycle events.
     * @param lifecycle The current lifecycle state of the application.
     */
    data class LifecycleEvent(val lifecycle: Lifecycle) : UiMsg {
        enum class Lifecycle {
            ON_CREATE,
            ON_START,
            ON_RESUME,
            ON_PAUSE,
            ON_STOP,
            ON_DESTROY,
            ON_ANY,
        }
    }
}