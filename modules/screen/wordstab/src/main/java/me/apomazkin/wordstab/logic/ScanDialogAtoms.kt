package me.apomazkin.wordstab.logic

import androidx.annotation.StringRes
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.ReducerResult
import io.github.kilgoret.mate.begin
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.ReducerLogging
import me.apomazkin.wordstab.LogTags

/**
 * ###### Атомарные state-экстеншны диалога выбора источника снимка
 * (конвенция §Reducer-конвенция): один экстеншн = ОДИН технический шаг,
 * каждый возвращает [ReducerResult] и логируется [logStep]. Оркестраторов
 * нет: цепочки — в ветках [WordsTabReducer]. Reducer и тест атомов
 * наследуют этот класс. ######
 */
abstract class ScanDialogAtoms(
    logger: LexemeLogger,
) : ReducerLogging(logger) {
    /** Фичевый тег вкладки: шаги, сообщения и логи handler'а — под одним `###VOCAB###`. */
    override val logTag: String = LogTags.VOCAB

    /**
     * Открыть диалог выбора источника, без ошибки. Диалог один: повторный
     * вызов при открытом диалоге второго не открывает (TFP-06.5), прежний
     * текст ошибки сбрасывается. Меняет [WordsTabState.scanDialogState].
     * Эффектов нет.
     */
    fun WordsTabState.showScanDialog(): ReducerResult<WordsTabState, Effect> {
        logStep("showScanDialog")
        return copy(scanDialogState = ScanDialogState(isOpen = true)).begin()
    }

    /**
     * Закрыть диалог: всё в умолчаниях, ошибка снята. Зовётся по «Отмена» и
     * «Закрыть» и когда снимок получен — в том числе при уже закрытом диалоге
     * (TFP-22). Меняет [WordsTabState.scanDialogState]. Эффектов нет.
     */
    fun WordsTabState.hideScanDialog(): ReducerResult<WordsTabState, Effect> {
        logStep("hideScanDialog")
        return copy(scanDialogState = ScanDialogState()).begin()
    }

    /**
     * Показать в диалоге причину, по которой камера не запустилась (TFP-07):
     * вместо кнопок источника — текст и «Закрыть». Диалог остаётся открытым.
     * Меняет [WordsTabState.scanDialogState]. Эффектов нет.
     * @param errorRes строка причины.
     */
    fun WordsTabState.showScanDialogError(
        @StringRes errorRes: Int,
    ): ReducerResult<WordsTabState, Effect> {
        logStep("showScanDialogError", "errorRes" to errorRes)
        return copy(scanDialogState = ScanDialogState(isOpen = true, errorRes = errorRes)).begin()
    }
}
