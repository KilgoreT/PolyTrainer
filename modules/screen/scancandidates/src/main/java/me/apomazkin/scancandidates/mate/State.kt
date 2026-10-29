package me.apomazkin.scancandidates.mate

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.candidates.AddSummaryUi
import me.apomazkin.candidates.CandidatesSheetUi
import me.apomazkin.candidates.CandidatesView
import me.apomazkin.candidates.ComponentMatchLineUiItem
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.LemmaCardUiItem
import me.apomazkin.candidates.RemarkUiItem
import me.apomazkin.candidates.SavedLexemeUi
import me.apomazkin.lexeme.LookupToken

/**
 * Состояние экрана снимка: от распознавания до окна со списками и карточками
 * под словами. Экран живёт один снимок. Всё показанное лежит явными полями,
 * composable ничего не толкует. Исход юзкейса и домен сюда не попадают: только
 * UI-типы и значения.
 *
 * Запуск: [uri] — файл снимка, [phase] — что на экране, [tokens] — токены
 * снимка для повтора запроса, [dictionaryId] — словарь снимка, [errorRes] и
 * [hasRetryButton] — ошибка и кнопка «Повторить».
 *
 * Ответ сервера (LFT-14): [text] — исправленный текст, [translation] — его
 * перевод, [isTranslationExpanded] — блок перевода раскрыт (AWP-41), [lists] —
 * четыре списка слов с пометками «в словаре» (AWP-39) и карточками под словами
 * (AWP-42).
 *
 * Окно: [form] — форма словаря снимка, [commonGroupId] — общая группа (null —
 * «Все»), [sheet] — открытый лист, [isAdding] — идёт запись хотя бы одного
 * слова: крестик и «назад» не действуют (AWP-51.12), [isConfirmingClose] —
 * вопрос «Закрыть без добавления?» открыт, [pendingWords] — число
 * недобавленных слов в вопросе (AWP-51).
 */
@Immutable
data class ScanCandidatesState(
    val uri: String,
    val phase: ScanPhase = ScanPhase.RECOGNIZING,
    val tokens: List<LookupToken> = emptyList(),
    val dictionaryId: Long? = null,
    val text: String = "",
    val translation: String = "",
    val isTranslationExpanded: Boolean = false,
    val lists: ScanWordListsUi = ScanWordListsUi(),
    @param:StringRes val errorRes: Int? = null,
    val hasRetryButton: Boolean = false,
    val form: DictionaryFormUi? = null,
    val commonGroupId: Long? = null,
    val sheet: CandidatesSheetUi? = null,
    val isAdding: Boolean = false,
    val isConfirmingClose: Boolean = false,
    val pendingWords: Int = 0,
)

/**
 * Что на экране: три фазы с индикатором и подписью — «Распознаю текст…»,
 * «Анализирую текст…» (AWP-38), «Собираю окно…» (AWP-24), окно, «слов нет»
 * (TFP-19, AWP-54) и ошибка (AWP-53).
 */
enum class ScanPhase {
    RECOGNIZING,
    ANALYZING,
    FORM_LOADING,
    WINDOW,
    NO_WORDS,
    ERROR,
}

/**
 * Четыре списка окна в порядке показа: [lexical] — смысловые в порядке текста,
 * [proper] — имена собственные, [function] — служебные, [unparsed] — «не
 * разобрано», как прислал сервер. Пустой список на экране не рисуется
 * (AWP-40.7).
 */
@Immutable
data class ScanWordListsUi(
    val lexical: List<ScanWordUiItem> = emptyList(),
    val proper: List<ScanWordUiItem> = emptyList(),
    val function: List<ScanWordUiItem> = emptyList(),
    val unparsed: List<ScanWordUiItem> = emptyList(),
)

/**
 * Слово списка: [text] — словарная форма или токен из «не разобрано»,
 * [isInDictionary] — в словаре снимка есть лемма с тем же текстом (AWP-39),
 * [card] — карточка под словом, null — не открывали, [isExpanded] — карточка
 * раскрыта (AWP-42). Слово с одним текстом в двух списках несёт одну и ту же
 * карточку в обеих строках.
 */
@Immutable
data class ScanWordUiItem(
    val text: String,
    val isInDictionary: Boolean = false,
    val card: WordCardUi? = null,
    val isExpanded: Boolean = false,
)

/**
 * Карточка под словом (AWP-42): [phase] — что в карточке, [lemma] — лемма с
 * лексемами-кандидатами, «куда класть», своей группой и пометкой формы, у
 * слова «в словаре» до проверки — без лексем; [saved] — сохранённые лексемы
 * леммы словаря из «куда класть» (AWP-46), у нового слова пусто; [remarks] —
 * строки «Замечаний» (AWP-30); [componentMatchLines] — строки «Сопоставления»
 * (AWP-31), [isComponentMatchesExpanded] — раздел раскрыт; [canAdd] —
 * «Добавить» активна: есть лексема без «уже в словаре» и ни одной без ядра
 * (AWP-47.2, 47.3); [errorRes] — «Не найдено», «Значений нет» или причина
 * ошибки, [hasRetryButton] — у ошибки есть «Повторить»; [isAdding] — идёт
 * запись этого слова, кнопки неактивны (AWP-02); [addFailure] — запись не
 * удалась, причина (AWP-21).
 */
@Immutable
data class WordCardUi(
    val phase: CardPhase,
    val lemma: LemmaCardUiItem,
    val saved: List<SavedLexemeUi> = emptyList(),
    val remarks: List<RemarkUiItem> = emptyList(),
    val componentMatchLines: List<ComponentMatchLineUiItem> = emptyList(),
    val isComponentMatchesExpanded: Boolean = false,
    val canAdd: Boolean = false,
    @param:StringRes val errorRes: Int? = null,
    val hasRetryButton: Boolean = false,
    val isAdding: Boolean = false,
    val addFailure: AddFailureUi? = null,
)

/**
 * Что показано в карточке: сохранённые лексемы слова «в словаре» и «Проверить
 * по тексту» (AWP-46), запрос значений идёт (AWP-57), лексемы-кандидаты с
 * сервера (AWP-43), «Не найдено» или «Значений нет» (AWP-55), ошибка или отказ
 * на слове (AWP-61).
 */
enum class CardPhase {
    SAVED,
    LOADING,
    CANDIDATES,
    NOT_FOUND,
    ERROR,
}

/**
 * Вид для общих виджетов окна: форма, общая группа, лист, карточки лемм всех
 * слов — для листов правки и «куда класть», — запись и вопрос закрытия; сводки
 * кнопки и общего «не добавилось» у экрана нет — они у карточек слов.
 */
fun ScanCandidatesState.toView(): CandidatesView = CandidatesView(
    form = form,
    cards = lists.allLemmaCards(),
    summary = AddSummaryUi(),
    sheet = sheet,
    isAdding = isAdding,
    addFailure = null,
    isConfirmingClose = isConfirmingClose,
    commonGroupId = commonGroupId,
)
