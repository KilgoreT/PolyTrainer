package me.apomazkin.wordcard.mate

import androidx.annotation.StringRes
import androidx.compose.runtime.Stable
import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.candidates.AddSummaryUi
import me.apomazkin.candidates.CandidatesSheetUi
import me.apomazkin.candidates.CandidatesView
import me.apomazkin.candidates.ComponentMatchLineUiItem
import me.apomazkin.candidates.ComponentRecommendationUi
import me.apomazkin.candidates.ComponentRefUi
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.LemmaCardUiItem
import me.apomazkin.candidates.LemmaWordChoice
import me.apomazkin.candidates.LookupWordUi
import me.apomazkin.candidates.RemarkUiItem
import me.apomazkin.candidates.ValueRefUi
import me.apomazkin.candidates.textOf
import me.apomazkin.candidates.valueOf
import me.apomazkin.candidates.withOption

/*
 * IS533/IS536: диалог лексем-кандидатов в карточке слова (WCL) — черновик
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
 * (WCL-11.4), [words] и [recommendations] — ответ сервера до сборки формы,
 * [cards] — разделы лемм (WCL-04), [hasRetryButton] — на ошибке есть
 * «Повторить» (WCL-11.9), остальное — как в окне снимка без групп и «куда
 * класть».
 */
@Stable
data class CandidatesState(
    val wordId: Long,
    val text: String,
    val dictionaryId: Long,
    val requestId: Long,
    val phase: CandidatesPhase = CandidatesPhase.LOADING,
    @StringRes val errorRes: Int? = null,
    val hasRetryButton: Boolean = false,
    val words: List<LookupWordUi> = emptyList(),
    val recommendations: List<ComponentRecommendationUi> = emptyList(),
    val form: DictionaryFormUi? = null,
    val cards: List<LemmaCardUiItem> = emptyList(),
    val summary: AddSummaryUi = AddSummaryUi(),
    val sheet: CandidatesSheetUi? = null,
    val remarks: List<RemarkUiItem> = emptyList(),
    val componentMatchLines: List<ComponentMatchLineUiItem> = emptyList(),
    val isComponentMatchesExpanded: Boolean = false,
    val isDirty: Boolean = false,
    val isAdding: Boolean = false,
    val addFailure: AddFailureUi? = null,
    val isConfirmingClose: Boolean = false,
    /** Что сделать со словом диалога при записи — переименовать в лемму-форму или перенести в её слово (WCL-15); null — ничего. */
    val lemmaWordChoice: LemmaWordChoice? = null,
    /** По лемме-форме — сколько значений её слов совпадает с кандидатами и лексемами слова диалога (WCL-15.4). */
    val sameValues: Map<String, Int> = emptyMap(),
) {
    /** Вид для общих виджетов модуля candidates. */
    fun toView(): CandidatesView = CandidatesView(
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
        lemmaWordChoice = lemmaWordChoice,
        sameValues = sameValues,
    )
}

/**
 * Текст значения по адресу отличается от ввода с обрезанными краями
 * (WCL-12.5); значения нет — false. Условие по стейту: флаг правок ставится,
 * только если текст и правда сменился.
 */
fun CandidatesState.textChanged(
    ref: ValueRefUi,
    draft: String,
): Boolean {
    val current = cards.textOf(ref) ?: return false
    return current.text != draft.trim()
}

/**
 * Текст значения по адресу правлен: отличается от текста ответа (WCL-12.5);
 * значения нет — false.
 */
fun CandidatesState.isTextEdited(ref: ValueRefUi): Boolean = cards.textOf(ref)?.isEdited == true

/**
 * После выбора [option] у «Выбора» по адресу его опции станут другими
 * (WCL-12.5); значения или компонента нет — false. Повтор опции у «несколько»
 * снимает её, поэтому сравнивается результат перехода, а не одна опция.
 */
fun CandidatesState.optionChanged(
    ref: ComponentRefUi,
    option: Long?,
): Boolean {
    val formState = form ?: return false
    val before = cards.valueOf(ref)
    val after = cards.withOption(ref, option, formState)?.valueOf(ref)
    return after != null && after != before
}
