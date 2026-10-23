package me.apomazkin.scancandidates.mate

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.candidates.AddSummaryUi
import me.apomazkin.candidates.CandidatesSheetUi
import me.apomazkin.candidates.CandidatesView
import me.apomazkin.candidates.ComponentMatchLineUiItem
import me.apomazkin.candidates.ComponentRecommendationUi
import me.apomazkin.candidates.ComponentRefUi
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.LemmaCardUiItem
import me.apomazkin.candidates.LookupWordUi
import me.apomazkin.candidates.NotAddedWordUiItem
import me.apomazkin.candidates.RemarkUiItem
import me.apomazkin.candidates.ValueRefUi
import me.apomazkin.candidates.textOf
import me.apomazkin.candidates.valueOf
import me.apomazkin.candidates.withOption
import me.apomazkin.lexeme.LookupToken

/**
 * Состояние экрана лексем-кандидатов со снимка: от распознавания до записи в
 * словарь. Экран живёт один снимок. Всё показанное лежит явными полями,
 * composable ничего не толкует. Исход юзкейса и домен сюда не попадают: только
 * UI-типы и значения.
 *
 * Запуск: [uri] — файл снимка, [phase] — что на экране, [tokens] — токены
 * снимка для повтора запроса, [dictionaryId], [words] и [recommendations] —
 * ответ сервера, [errorRes] и [hasRetryButton] — ошибка и кнопка «Повторить».
 *
 * Окно: [form] — форма словаря снимка, [commonGroupId] — общая группа (null —
 * «Все»), [cards], [notAdded], [remarks], [componentMatchLines] — что показано,
 * [summary] — кнопка «добавить», [sheet] — открытый лист.
 *
 * Флаги: [isDirty] — были удаления, правки текста или смена опций (AWP-19),
 * [isAdding] — идёт запись, [addFailure] — запись не удалась,
 * [isConfirmingAdd] и [isConfirmingClose] — открытые вопросы,
 * [isComponentMatchesExpanded] и [isNotAddedExpanded] — раскрытые разделы.
 */
@Immutable
data class ScanCandidatesState(
    val uri: String,
    val phase: ScanPhase = ScanPhase.RECOGNIZING,
    val tokens: List<LookupToken> = emptyList(),
    val dictionaryId: Long? = null,
    val words: List<LookupWordUi> = emptyList(),
    val recommendations: List<ComponentRecommendationUi> = emptyList(),
    @param:StringRes val errorRes: Int? = null,
    val hasRetryButton: Boolean = false,
    val form: DictionaryFormUi? = null,
    val commonGroupId: Long? = null,
    val cards: List<LemmaCardUiItem> = emptyList(),
    val notAdded: List<NotAddedWordUiItem> = emptyList(),
    val remarks: List<RemarkUiItem> = emptyList(),
    val componentMatchLines: List<ComponentMatchLineUiItem> = emptyList(),
    val summary: AddSummaryUi = AddSummaryUi(),
    val isDirty: Boolean = false,
    val isAdding: Boolean = false,
    val addFailure: AddFailureUi? = null,
    val isConfirmingAdd: Boolean = false,
    val isConfirmingClose: Boolean = false,
    val isComponentMatchesExpanded: Boolean = false,
    val isNotAddedExpanded: Boolean = false,
    val sheet: CandidatesSheetUi? = null,
)

/**
 * Что на экране: три фазы с индикатором и подписью (WFP-13, AWP-24), окно,
 * «слов нет» (WFP-19) и ошибка (WFP-20).
 */
enum class ScanPhase {
    RECOGNIZING,
    LOOKING,
    FORM_LOADING,
    WINDOW,
    NO_WORDS,
    ERROR,
}

/**
 * Вид для общих виджетов окна: поля диалога карточки, которых у экрана нет,
 * остаются в умолчаниях.
 */
fun ScanCandidatesState.toView(): CandidatesView = CandidatesView(
    form = form,
    cards = cards,
    summary = summary,
    sheet = sheet,
    isAdding = isAdding,
    addFailure = addFailure,
    isConfirmingClose = isConfirmingClose,
    remarks = remarks,
    componentMatchLines = componentMatchLines,
    isComponentMatchesExpanded = isComponentMatchesExpanded,
    notAdded = notAdded,
    isNotAddedExpanded = isNotAddedExpanded,
    commonGroupId = commonGroupId,
    isConfirmingAdd = isConfirmingAdd,
)

/**
 * Текст значения по адресу отличается от ввода с обрезанными краями; значения
 * нет — false. Условие флага правок при «сохранить» (AWP-19.9).
 */
fun ScanCandidatesState.textChanged(
    ref: ValueRefUi,
    draft: String,
): Boolean {
    val current = cards.textOf(ref) ?: return false
    return current.text != draft.trim()
}

/**
 * Текст значения по адресу правлен руками — отличается от текста ответа;
 * значения нет — false. Условие флага правок при «сбросить» (AWP-19.9).
 */
fun ScanCandidatesState.textEdited(ref: ValueRefUi): Boolean = cards.textOf(ref)?.isEdited == true

/**
 * Выбор [option] поменяет значение «Выбора» по адресу: у компонента с одним
 * значением — другая опция, у «несколько» — набор опций; значения нет — false.
 * Условие флага правок при выборе опции (AWP-19.9).
 */
fun ScanCandidatesState.optionChanged(
    ref: ComponentRefUi,
    option: Long?,
): Boolean {
    val form = form ?: return false
    val changed = cards.withOption(ref, option, form) ?: return false
    return changed.valueOf(ref) != cards.valueOf(ref)
}
