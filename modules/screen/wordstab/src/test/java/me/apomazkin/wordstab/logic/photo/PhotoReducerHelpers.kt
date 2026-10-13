package me.apomazkin.wordstab.logic.photo

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.testReduce
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import me.apomazkin.wordstab.deps.PhotoDictionaryForm
import me.apomazkin.wordstab.deps.PhotoFormOutcome
import me.apomazkin.wordstab.deps.PhotoLookupOutcome
import me.apomazkin.wordstab.deps.PhotoMapping
import me.apomazkin.wordstab.deps.PhotoWord
import me.apomazkin.wordstab.logic.Msg
import me.apomazkin.wordstab.logic.PhotoWordsState
import me.apomazkin.wordstab.logic.WordsTabReducer
import me.apomazkin.wordstab.logic.WordsTabState

/** Обвязка message-тестов окна: reducer с пустым логгером и путь до собранного окна. */
internal object NoopLogger : LexemeLogger {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {}
}

internal fun reducer() = WordsTabReducer(logger = NoopLogger)

internal fun idle() = WordsTabState(isLoading = false)

internal fun WordsTabState.photo(): PhotoWordsState = photoWordsState

/** Ответ сервера своего запуска: выбор → снимок → токены → слова; возвращает состояние с индикатором FORM. */
internal fun WordsTabReducer.answered(vararg words: PhotoWord): WordsTabState {
    val chooser = testReduce(idle(), Msg.OpenPhotoSourceChooser).state()
    val picked = testReduce(chooser, Msg.PhotoPicked(uri = "content://photo/1")).state()
    val runId = picked.photo().requestId
    val looking = testReduce(picked, Msg.TokensReady(listOf(me.apomazkin.textrecognition.RecognizedWord("x", 0.9f, "en")), runId)).state()
    return testReduce(looking, Msg.WordsLookedUp(PhotoLookupOutcome.Words(Fx.DICT, words.toList()), runId)).state()
}

/** Собранное окно: ответ + форма своего запуска. */
internal fun WordsTabReducer.window(
    vararg words: PhotoWord,
    form: PhotoDictionaryForm = Fx.form(),
    remembered: PhotoMapping? = null,
): WordsTabState {
    val answered = answered(*words)
    return testReduce(answered, Msg.PhotoFormLoaded(PhotoFormOutcome.Loaded(form, remembered), answered.photo().requestId)).state()
}

internal fun WordsTabReducer.step(state: WordsTabState, message: Msg): WordsTabState = testReduce(state, message).state()

internal fun WordsTabReducer.effectsOf(state: WordsTabState, message: Msg): Set<Effect> = testReduce(state, message).second

internal fun WordsTabState.card(lemma: String): PhotoCard = photo().cards.first { it.lemma == lemma }

internal fun WordsTabState.lexeme(lemma: String, key: String): PhotoCardLexeme = card(lemma).lexemes.first { it.key == key }
