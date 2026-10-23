package me.apomazkin.wordstab.logic

import me.apomazkin.candidates.addedMessage
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateReducer
import io.github.kilgoret.mate.ReducerResult
import io.github.kilgoret.mate.begin
import io.github.kilgoret.mate.withEffect
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.wordstab.logic.DatasourceEffect.CreateWord
import me.apomazkin.wordstab.logic.DatasourceEffect.LoadTermFlow
import me.apomazkin.wordstab.logic.DatasourceEffect.RemoveWords
import me.apomazkin.wordstab.logic.DatasourceEffect.UpdateWord
import me.apomazkin.wordstab.logic.processor.processUiMessage
import me.apomazkin.wordstab.ui.WordsNavigationEffect

/**
 * Reducer вкладки «Слова». Вход в скан снимка — по §Reducer-конвенция: Msg →
 * цепочка атомов [ScanDialogAtoms]; остальные ветки — прежние экстеншны
 * State.kt (мигрируют по мере правок). Каждый Msg логируется именем или
 * `toString`; State целиком в лог не пишется (R-RP-007).
 */
class WordsTabReducer(
    logger: LexemeLogger,
) : ScanDialogAtoms(logger),
    MateReducer<WordsTabState, Msg, Effect> {
    override fun reduce(
        state: WordsTabState,
        message: Msg,
    ): ReducerResult<WordsTabState, Effect> {
        // Data-Msg — именем класса: их содержимое компактно пишет handler.
        logMessage(
            when (message) {
                is Msg.TermsLoaded -> message::class.simpleName.orEmpty()
                else -> message.toString()
            },
        )
        return reduceMessage(state, message)
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun reduceMessage(
        state: WordsTabState,
        message: Msg,
    ): ReducerResult<WordsTabState, Effect> =
        when (message) {

            // IS496 Р3: тап по слову при открытой панели добавления —
            // намерение «добавить» сменилось на «открыть существующее»:
            // панель закрывается (ввод и фильтр сбрасываются), затем
            // навигация; возврат из карточки — к обычному списку.
            is Msg.OpenWordCard -> if (state.addWordDialogState.isOpen) {
                state
                    .hideAddWordDialog()
                    .toDefaultTermsFlow() to setOf(
                    WordsNavigationEffect.OpenWordCard(message.wordId)
                )
            } else {
                state to setOf(
                    WordsNavigationEffect.OpenWordCard(message.wordId)
                )
            }

            is Msg.SelectDictionary -> if (message.current == null) {
                // IS476: словарь отсутствует — переходим в пустое состояние таба.
                // Не запускаем LoadTermFlow (внутри был бы NPE на .id.toInt()).
                state.markNoDictionary() to emptySet()
            } else {
                // IS476: словарь появился (новый или переход null → non-null).
                // showLoading() перед LoadTermFlow — симметрично hideLoading() в Msg.TermsLoaded,
                // чтобы UI не моргал пустым WordListWidget с устаревшим termList.
                state.markDictionaryPresent().showLoading() to setOf(LoadTermFlow())
            }

            is Msg.TermsLoaded -> state
                .hideLoading()
                .appendTermsFlow(
                    pattern = message.pattern,
                    termsFlow = message.termList,
                ) to emptySet()

            is Msg.EnterSelectionMode -> state
                .modifySelectedSet(message.targetWord)
                .highlightWord(message.targetWord)
                .showActionMode() to setOf()

            is Msg.ExitSelectionMode -> state
                .clearHighlighted()
                .clearSelectedSet()
                .hideActionMode() to setOf()

            is Msg.ToggleSelection -> state
                .modifySelectedSet(message.targetWord)
                .highlightWord(message.targetWord)
                .checkActionMode() to setOf()

            is Msg.OpenAddWordDialog -> state
                .showAddWordDialog(
                    wordValue = message.wordValue,
                    wordId = null,
                ) to setOf()

            is Msg.CloseAddWordDialog -> state
                .hideAddWordDialog()
                .toDefaultTermsFlow() to setOf()

            is Msg.OpenEditWordDialog -> state
                .clearHighlighted()
                .clearSelectedSet()
                .hideActionMode()
                .showAddWordDialog(
                    wordId = message.wordId,
                    wordValue = message.wordValue,
                ) to setOf()

            is Msg.UpdateWordInput -> state.choose(
                check = { it.addWordDialogState.isOpen },
                yes = {
                    //TODO kilg 31.05.2025 01:30 добавить условие,
                    // что если "" уже есть, и паттерн "" -
                    // то не надо блять отправлять эффект создания потока
                    // https://github.com/KilgoreT/PolyTrainer/issues/377
                    it
                        .updateWordValue(message.value)
                        .retainDefaultAndCurrentFlow(message.value) to setOf(LoadTermFlow(message.value))
                },
                no = { it to emptySet() }
            )

            is Msg.CreateWord -> state
                .hideAddWordDialog()
                .toDefaultTermsFlow() to setOf(CreateWord(message.value))

            is Msg.UpdateWord -> state
                .clearHighlighted()
                .clearSelectedSet()
                .hideActionMode()
                .hideAddWordDialog()
                .toDefaultTermsFlow() to setOf(
                UpdateWord(
                    wordId = message.wordId,
                    value = message.value,
                )
            )

            is Msg.OpenDeleteConfirmation -> state
                .showConfirmDeleteDialog(message.wordIds) to setOf()

            is Msg.CloseDeleteConfirmation -> state
                .hideConfirmDeleteDialog() to setOf()

            is Msg.RemoveWords -> state
                .hideConfirmDeleteDialog()
                .clearHighlighted()
                .clearSelectedSet()
                .hideActionMode() to setOf(
                RemoveWords(wordSet = message.wordIds),
                UiEffect.ShowNotification(message = "Слово удалено")
            )

            // ===== Скан снимка: диалог выбора источника, запуск, переход на экран =====

            // Повторное нажатие диалог не дублирует: он один (WFP-06.5).
            Msg.OpenScanDialog -> state.showScanDialog()

            // Диалог остаётся открытым: отмена в системном приложении ничего не
            // шлёт, пользователь снова видит «Камера» и «Галерея» (WFP-18).
            Msg.ChooseCamera -> state
                .begin<WordsTabState, Effect>()
                .withEffect(UiEffect.LaunchCamera)

            Msg.ChooseGallery -> state
                .begin<WordsTabState, Effect>()
                .withEffect(UiEffect.LaunchGallery)

            // Принимается и при закрытом диалоге: система могла убить процесс,
            // пока была открыта камера или галерея — снимок идёт в экран (WFP-22).
            is Msg.ImagePicked -> state
                .hideScanDialog()
                .withEffect(WordsNavigationEffect.OpenScanCandidates(message.uri))

            is Msg.CameraUnavailable -> state.showScanDialogError(message.errorRes)

            Msg.CloseScanDialog -> state.hideScanDialog()

            // Экран закрылся после записи: сообщение со счётом, стейт не меняется (AWP-23).
            is Msg.ScanCandidatesAdded -> state
                .begin<WordsTabState, Effect>()
                .withEffect(
                    UiEffect.ShowNotification(
                        message = addedMessage(message.lexemeCount, message.wordCount),
                    ),
                )

            is UiMsg -> processUiMessage(state, message)
            Msg.NoOperation -> state to emptySet()
        }
}
