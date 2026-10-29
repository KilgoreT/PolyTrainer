package me.apomazkin.scancandidates.mate

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.effects
import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.testReduce
import me.apomazkin.candidates.ComponentRecommendationUi
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.ExistingLemmaUi
import me.apomazkin.candidates.LookupWordUi
import me.apomazkin.lexeme.LookupToken
import me.apomazkin.lexeme.WordsBatch
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel

/** Обвязка message-тестов: редьюсер с пустым логгером и путь до собранного окна и карточки. */
internal object NoopLogger : LexemeLogger {
    override fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = Unit
}

internal fun reducer() = ScanCandidatesReducer(logger = NoopLogger)

/** Экран только открыт: идёт распознавание снимка. */
internal fun fresh() = ScanCandidatesState(uri = Fx.URI)

internal fun ScanCandidatesReducer.step(
    state: ScanCandidatesState,
    message: Msg,
): ScanCandidatesState = testReduce(state, message).state()

/** Цепочка сообщений подряд: состояние после последнего. */
internal fun ScanCandidatesReducer.steps(
    state: ScanCandidatesState,
    vararg messages: Msg,
): ScanCandidatesState = messages.fold(state) { current, message -> step(current, message) }

internal fun ScanCandidatesReducer.effectsOf(
    state: ScanCandidatesState,
    message: Msg,
): Set<Effect> = testReduce(state, message).effects()

/** Токены распознаны: индикатор «анализирую текст». */
internal fun ScanCandidatesReducer.recognized(
    tokens: List<LookupToken> = Fx.tokens,
): ScanCandidatesState = step(fresh(), Msg.TextRecognized(tokens))

/** Ответ сервера со списками: индикатор «собираю окно». */
internal fun ScanCandidatesReducer.answered(
    found: Msg.LemmasFound = Fx.found(),
): ScanCandidatesState = step(recognized(), found)

/** Собранное окно: ответ и форма. */
internal fun ScanCandidatesReducer.window(
    found: Msg.LemmasFound = Fx.found(),
    form: DictionaryFormUi = Fx.form(),
): ScanCandidatesState = step(answered(found), Msg.FormLoaded(form))

/** Окно со списком из одних смысловых слов [texts]. */
internal fun ScanCandidatesReducer.windowOf(
    vararg texts: String,
    form: DictionaryFormUi = Fx.form(),
): ScanCandidatesState = window(found = Fx.found(lists = Fx.lists(lexical = texts.toList())), form = form)

/**
 * Окно с раскрытой карточкой слова [text] по ответу [word]: тап и ответ
 * сервера; списки — одно это слово, если не заданы.
 */
internal fun ScanCandidatesReducer.withCard(
    text: String,
    word: LookupWordUi,
    form: DictionaryFormUi = Fx.form(),
    lists: ScanWordListsUi = Fx.lists(lexical = listOf(text)),
    recommendations: List<ComponentRecommendationUi> = emptyList(),
    existing: List<ExistingLemmaUi> = emptyList(),
): ScanCandidatesState = steps(
    window(found = Fx.found(lists = lists), form = form),
    Msg.LoadLexemes(text),
    Fx.lexemes(text, word, recommendations, existing),
)

/** Пакет записи слова из эффекта «Добавить». */
internal fun ScanCandidatesReducer.batchOf(
    state: ScanCandidatesState,
    text: String,
): WordsBatch = (effectsOf(state, Msg.AddWord(text)).single() as DatasourceEffect.AddWord).batch
