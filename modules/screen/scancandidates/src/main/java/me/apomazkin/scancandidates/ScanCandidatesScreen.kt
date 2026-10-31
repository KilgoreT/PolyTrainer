package me.apomazkin.scancandidates

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.candidates.CandidateValueUiItem
import me.apomazkin.candidates.CardLexemeUiItem
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.EditableTextUiItem
import me.apomazkin.candidates.FormComponentUi
import me.apomazkin.candidates.FormGroupUi
import me.apomazkin.candidates.LemmaCardUiItem
import me.apomazkin.candidates.SavedWordUi
import me.apomazkin.core_resources.R
import me.apomazkin.di.viewModelFactory
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.scancandidates.mate.CardPhase
import me.apomazkin.scancandidates.mate.Msg
import me.apomazkin.scancandidates.mate.ScanCandidatesState
import me.apomazkin.scancandidates.mate.ScanPhase
import me.apomazkin.scancandidates.mate.ScanWordListsUi
import me.apomazkin.scancandidates.mate.ScanWordUiItem
import me.apomazkin.scancandidates.mate.WordCardUi
import me.apomazkin.scancandidates.ui.MessagePanel
import me.apomazkin.scancandidates.ui.ProgressPanel
import me.apomazkin.scancandidates.ui.ScanWordListsContent
import me.apomazkin.scancandidates.ui.toMsg
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.formBackground
import me.apomazkin.ui.SystemBarsWidget
import me.apomazkin.ui.preview.PreviewScreen

/**
 * Экран снимка: от распознавания до окна со списками словарных форм и
 * плашками под словами. Живёт один снимок, ViewModel — по ключу снимка.
 */
@Composable
fun ScanCandidatesScreen(
    uri: String,
    factory: ScanCandidatesViewModel.Factory,
) {
    val viewModel: ScanCandidatesViewModel = viewModel(
        key = "scanCandidates_$uri",
        factory = viewModelFactory { factory.create(uri) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScanCandidatesScreen(
        state = state,
        sendMessage = { viewModel.accept(it) },
    )
}

/**
 * Что на экране решает фаза стейта: индикатор с подписью и «Отменить», «слов
 * нет», ошибка с «Закрыть» и, по флагу, «Повторить», окно. Системный «назад» —
 * `RequestClose` в любой фазе: во время записи его гасит редьюсер (AWP-51.12),
 * при недобавленных словах — вопрос (AWP-51), иначе окно закрывается сразу
 * (AWP-60).
 */
@Composable
internal fun ScanCandidatesScreen(
    state: ScanCandidatesState,
    sendMessage: (Msg) -> Unit,
) {
    SystemBarsWidget(color = formBackground)
    BackHandler { sendMessage(Msg.RequestClose) }

    when (state.phase) {
        ScanPhase.RECOGNIZING -> ProgressPanel(
            captionRes = R.string.photo_words_recognizing,
            onCancel = { sendMessage(Msg.RequestClose) },
        )
        ScanPhase.ANALYZING -> ProgressPanel(
            captionRes = R.string.scan_analyzing,
            onCancel = { sendMessage(Msg.RequestClose) },
        )
        ScanPhase.FORM_LOADING -> ProgressPanel(
            captionRes = R.string.photo_add_form_loading,
            onCancel = { sendMessage(Msg.RequestClose) },
        )
        ScanPhase.WINDOW -> ScanWordListsContent(
            state = state,
            onAction = { sendMessage(it.toMsg()) },
            onWordAction = { sendMessage(it.toMsg()) },
            onToggleTranslation = { sendMessage(Msg.ToggleTranslation) },
        )
        ScanPhase.NO_WORDS -> MessagePanel(
            textRes = R.string.photo_words_empty,
            hasRetryButton = false,
            onRetry = { sendMessage(Msg.RetryLemmas) },
            onClose = { sendMessage(Msg.RequestClose) },
        )
        ScanPhase.ERROR -> state.errorRes?.let { errorRes ->
            MessagePanel(
                textRes = errorRes,
                hasRetryButton = state.hasRetryButton,
                onRetry = { sendMessage(Msg.RetryLemmas) },
                onClose = { sendMessage(Msg.RequestClose) },
            )
        }
    }
}

private const val PREVIEW_URI = "content://photo/1"

private val previewForm = DictionaryFormUi(
    dictionaryId = 1L,
    translationLanguage = "ru",
    components = listOf(
        FormComponentUi(
            id = 1L,
            systemKey = BuiltInComponent.TRANSLATION,
            name = null,
            template = ComponentTemplate.TEXT,
            isCore = true,
            isMultiple = false,
            position = 0,
            dependsOn = DependencyTarget.Lexeme,
        ),
    ),
    options = emptyMap(),
    groups = listOf(FormGroupUi(id = 7L, name = "Harry Potter 1")),
    existingLemmas = emptyList(),
)

private fun previewLexeme(
    key: String,
    translation: String,
    isInContext: Boolean,
) = CardLexemeUiItem(
    key = key,
    source = "ruwiktionary",
    values = listOf(
        CandidateValueUiItem.Text(
            componentId = 1L,
            texts = listOf(EditableTextUiItem.of(translation)),
            field = "translations",
        ),
    ),
    isEnglishFallback = false,
    exists = false,
    lacksCore = false,
    isInContext = isInContext,
)

private val previewCard = WordCardUi(
    phase = CardPhase.CANDIDATES,
    lemma = LemmaCardUiItem(
        lemma = "correr",
        sourceWords = listOf("correr"),
        lexemes = listOf(
            previewLexeme("ruwiktionary/1.verb.3", "течь, литься, струиться (о жидкости)", isInContext = true),
            previewLexeme("ruwiktionary/1.verb.1", "бегать; бежать", isInContext = false),
        ),
        existingCount = 1,
    ),
    saved = listOf(
        SavedWordUi(
            wordId = 1L,
            text = "correr",
            groupNames = emptyList(),
            addedAt = 1_760_000_000_000L,
            lexemes = emptyList(),
        ),
    ),
)

private val previewLists = ScanWordListsUi(
    lexical = listOf(
        ScanWordUiItem("nube"),
        ScanWordUiItem("correr", card = previewCard, isExpanded = true),
        ScanWordUiItem("cielo", isInDictionary = true),
    ),
    function = listOf(ScanWordUiItem("el"), ScanWordUiItem("por")),
    unparsed = listOf(ScanWordUiItem("sh")),
)

@PreviewScreen
@Composable
private fun PreviewRecognizing() {
    AppTheme {
        ScanCandidatesScreen(
            state = ScanCandidatesState(uri = PREVIEW_URI),
            sendMessage = {},
        )
    }
}

@PreviewScreen
@Composable
private fun PreviewAnalyzing() {
    AppTheme {
        ScanCandidatesScreen(
            state = ScanCandidatesState(uri = PREVIEW_URI, phase = ScanPhase.ANALYZING),
            sendMessage = {},
        )
    }
}

@PreviewScreen
@Composable
private fun PreviewFormLoading() {
    AppTheme {
        ScanCandidatesScreen(
            state = ScanCandidatesState(uri = PREVIEW_URI, phase = ScanPhase.FORM_LOADING),
            sendMessage = {},
        )
    }
}

@PreviewScreen
@Composable
private fun PreviewNoWords() {
    AppTheme {
        ScanCandidatesScreen(
            state = ScanCandidatesState(uri = PREVIEW_URI, phase = ScanPhase.NO_WORDS),
            sendMessage = {},
        )
    }
}

@PreviewScreen
@Composable
private fun PreviewError() {
    AppTheme {
        ScanCandidatesScreen(
            state = ScanCandidatesState(
                uri = PREVIEW_URI,
                phase = ScanPhase.ERROR,
                errorRes = R.string.photo_words_error_recognition,
            ),
            sendMessage = {},
        )
    }
}

@PreviewScreen
@Composable
private fun PreviewRetryableError() {
    AppTheme {
        ScanCandidatesScreen(
            state = ScanCandidatesState(
                uri = PREVIEW_URI,
                phase = ScanPhase.ERROR,
                errorRes = R.string.scan_analysis_unavailable,
                hasRetryButton = true,
            ),
            sendMessage = {},
        )
    }
}

@PreviewScreen
@Composable
private fun PreviewWindow() {
    AppTheme {
        ScanCandidatesScreen(
            state = ScanCandidatesState(
                uri = PREVIEW_URI,
                phase = ScanPhase.WINDOW,
                text = "Las nubes corrían por el cielo.",
                translation = "Облака бежали по небу.",
                isTranslationExpanded = true,
                lists = previewLists,
                form = previewForm,
            ),
            sendMessage = {},
        )
    }
}
