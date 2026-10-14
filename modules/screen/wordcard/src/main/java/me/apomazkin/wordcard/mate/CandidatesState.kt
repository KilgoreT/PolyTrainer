package me.apomazkin.wordcard.mate

import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import me.apomazkin.candidates.CandidatesView
import me.apomazkin.candidates.LemmaWordChoice
import me.apomazkin.candidates.PhotoAddFailure
import me.apomazkin.candidates.PhotoAddSummary
import me.apomazkin.candidates.PhotoCard
import me.apomazkin.candidates.PhotoDictionaryForm
import me.apomazkin.candidates.PhotoMapping
import me.apomazkin.candidates.PhotoSheet
import me.apomazkin.candidates.PhotoWord

/*
 * IS533: диалог «значения из словаря» в карточке слова (WCL) — черновик
 * добавления в слово диалога. Все флаги явные, их ставят атомы
 * [CandidatesAtoms]; null в [WordCardState.candidates] — диалога нет.
 */

/** Шаг диалога: запрос и сборка формы, разделы на экране, текст причины. */
enum class CandidatesPhase {
    LOADING,
    READY,
    ERROR,
}

/**
 * Состояние диалога: [wordId], [text] и [dictionaryId] — слово диалога
 * (WCL-01), [requestId] — номер запуска для отсечения чужих ответов
 * (WCL-11.4), [words] — ответ сервера до сборки формы, [cards] — разделы
 * лемм (WCL-04), остальное — как в окне снимка без групп и «куда класть».
 */
@Stable
data class CandidatesState(
    val wordId: Long,
    val text: String,
    val dictionaryId: Long,
    val requestId: Long,
    val phase: CandidatesPhase = CandidatesPhase.LOADING,
    @StringRes val errorRes: Int? = null,
    val words: List<PhotoWord> = emptyList(),
    val form: PhotoDictionaryForm? = null,
    val mapping: PhotoMapping = PhotoMapping.NONE,
    val cards: List<PhotoCard> = emptyList(),
    val summary: PhotoAddSummary = PhotoAddSummary(),
    val sheet: PhotoSheet? = null,
    val isMappingExpanded: Boolean = false,
    val isDirty: Boolean = false,
    val isAdding: Boolean = false,
    val addFailure: PhotoAddFailure? = null,
    val isConfirmingClose: Boolean = false,
    /** Что сделать со словом диалога при записи — переименовать в лемму-форму или перенести в её слово (WCL-15); null — ничего. */
    val lemmaWordChoice: LemmaWordChoice? = null,
    /** По лемме-форме — сколько значений её слов совпадает с кандидатами и лексемами слова диалога (WCL-15.4). */
    val sameValues: Map<String, Int> = emptyMap(),
) {
    /** Вид для общих виджетов модуля candidates. */
    fun toView(): CandidatesView = CandidatesView(
        form = form,
        mapping = mapping,
        cards = cards,
        summary = summary,
        sheet = sheet,
        isAdding = isAdding,
        addFailure = addFailure,
        isConfirmingClose = isConfirmingClose,
        isMappingExpanded = isMappingExpanded,
        lemmaWordChoice = lemmaWordChoice,
        sameValues = sameValues,
    )
}
