package me.apomazkin.scancandidates.mate

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.effects
import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.testReduce
import me.apomazkin.candidates.CardLexemeUiItem
import me.apomazkin.candidates.ComponentRecommendationUi
import me.apomazkin.candidates.DictionaryFormUi
import me.apomazkin.candidates.LemmaCardUiItem
import me.apomazkin.candidates.LookupWordUi
import me.apomazkin.lexeme.LookupToken
import me.apomazkin.lexeme.WordsBatch
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel

/** Обвязка message-тестов: редьюсер с пустым логгером и путь до собранного окна. */
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

/** Токены распознаны: индикатор «ищу слова». */
internal fun ScanCandidatesReducer.recognized(
    tokens: List<LookupToken> = Fx.tokens,
): ScanCandidatesState = step(fresh(), Msg.TextRecognized(tokens))

/** Ответ сервера со словами: индикатор «собираю окно». */
internal fun ScanCandidatesReducer.answered(
    vararg words: LookupWordUi,
    recommendations: List<ComponentRecommendationUi> = emptyList(),
): ScanCandidatesState = step(
    recognized(),
    Msg.WordsFound(Fx.DICT, words.toList(), recommendations),
)

/** Собранное окно: ответ и форма. */
internal fun ScanCandidatesReducer.window(
    vararg words: LookupWordUi,
    form: DictionaryFormUi = Fx.form(),
    recommendations: List<ComponentRecommendationUi> = emptyList(),
): ScanCandidatesState = step(
    answered(*words, recommendations = recommendations),
    Msg.FormLoaded(form),
)

/** Пакет, который уйдёт по «да» в вопросе о добавлении. */
internal fun ScanCandidatesReducer.batchOf(state: ScanCandidatesState): WordsBatch {
    val asking = step(state, Msg.RequestAdd)
    val effect = effectsOf(asking, Msg.ConfirmAdd).single() as DatasourceEffect.AddWords
    return effect.batch
}

internal fun ScanCandidatesState.card(lemma: String): LemmaCardUiItem =
    cards.first { it.lemma == lemma }

internal fun ScanCandidatesState.lexeme(
    lemma: String,
    key: String,
): CardLexemeUiItem = card(lemma).lexemes.first { it.key == key }

/** Что видит пользователь у лексемы-кандидата: значения компонентов строками. */
internal fun ScanCandidatesState.shown(
    lemma: String,
    key: String,
): List<String> = lexeme(lemma, key).shown(requireNotNull(form))

/** Тексты компонента у лексемы-кандидата, по умолчанию «Перевод». */
internal fun ScanCandidatesState.texts(
    lemma: String,
    key: String,
    componentId: Long = Fx.TRANSLATION,
): List<String> = lexeme(lemma, key).texts(componentId)

/** Значение компонента у лексемы-кандидата активно: его зависимость выполнена (AWP-17.6). */
internal fun ScanCandidatesState.isActive(
    lemma: String,
    key: String,
    componentId: Long,
): Boolean = lexeme(lemma, key).values.first { it.componentId == componentId }.isActive

/** Опции «Выбора» у лексемы-кандидата. */
internal fun ScanCandidatesState.chosen(
    lemma: String,
    key: String,
    componentId: Long,
): List<Long> = lexeme(lemma, key).chosen(componentId)
