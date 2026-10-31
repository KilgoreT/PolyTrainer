package me.apomazkin.scancandidates.mate

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import me.apomazkin.candidates.AddSummaryUi
import me.apomazkin.candidates.CandidatesSheetUi
import me.apomazkin.candidates.CandidatesView
import me.apomazkin.candidates.ComponentMatchLineUiItem
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.LemmaCardUiItem
import me.apomazkin.candidates.RemarkUiItem
import me.apomazkin.candidates.SavedWordUi
import me.apomazkin.lexeme.LookupToken

/**
 * Состояние экрана снимка: от распознавания до окна со списками и плашками
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
 * четыре списка слов с пометками «в словаре» (AWP-39) и плашками под словами
 * (AWP-42).
 *
 * Окно: [form] — форма словаря снимка, [commonGroupId] — общая группа (null —
 * «Все»), [sheet] — открытый лист, [isAdding] — идёт запись хотя бы одной
 * плашки: крестик и «назад» не действуют (AWP-51.12), [isConfirmingClose] —
 * вопрос «Закрыть без добавления?» открыт, [pendingWords] — число слов с
 * неразобранными плашками в вопросе (AWP-51).
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
 * [card] — что под словом, null — не открывали, [isExpanded] — плашки
 * раскрыты (AWP-42). Слово с одним текстом в двух списках несёт одну и ту же
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
 * Карточка под словом — всё, что под ним раскрыто (AWP-42): [phase] — что
 * показано; [lemma] — лемма с текстом слова и плашками нейронки — её
 * лексемами-кандидатами, у каждой своё «Куда», своя группа, запись и сбой
 * записи (AWP-18, AWP-08, AWP-21), у слова «в словаре» до проверки — без
 * лексем; [saved] — плашки словаря: по слову словаря с этим текстом (AWP-46),
 * у нового слова пусто; [remarks] — строки «Замечаний» (AWP-30);
 * [componentMatchLines] — строки «Сопоставления» (AWP-31),
 * [isComponentMatchesExpanded] — раздел раскрыт; [errorRes] — «Не найдено»,
 * «Значений нет» или причина ошибки, [hasRetryButton] — у ошибки есть
 * «Повторить»; [isAdding] — идёт запись одной из плашек, кнопки и правки
 * всех плашек слова неактивны (AWP-02, AWP-47.4).
 */
@Immutable
data class WordCardUi(
    val phase: CardPhase,
    val lemma: LemmaCardUiItem,
    val saved: List<SavedWordUi> = emptyList(),
    val remarks: List<RemarkUiItem> = emptyList(),
    val componentMatchLines: List<ComponentMatchLineUiItem> = emptyList(),
    val isComponentMatchesExpanded: Boolean = false,
    @param:StringRes val errorRes: Int? = null,
    val hasRetryButton: Boolean = false,
    val isAdding: Boolean = false,
)

/**
 * Что показано под словом: плашки словаря и «Проверить по тексту» (AWP-46),
 * запрос значений идёт (AWP-57), плашки нейронки с сервера (AWP-43), «Не
 * найдено» или «Значений нет» (AWP-55), ошибка или отказ на слове (AWP-61).
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
 * слов — для листов правки, «Куда» и своей группы плашек, — запись и вопрос
 * закрытия; сводки кнопки и общего «не добавилось» у экрана нет — они у плашек.
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
