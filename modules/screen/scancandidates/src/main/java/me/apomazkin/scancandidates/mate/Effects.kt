package me.apomazkin.scancandidates.mate

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.NavigationEffect
import io.github.kilgoret.mate.RecoverableEffect
import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.LookupToken
import me.apomazkin.lexeme.WordsBatch

/**
 * Эффекты данных экрана. Провал каждого объявлен в его типе
 * ([RecoverableEffect]): хендлер ошибок не ловит, стектрейс пишет
 * `ErrorLoggingObserver`, раннер шлёт сообщение из `onFail`. Тяжёлые данные в
 * `toString` — счётчиками.
 */
sealed interface DatasourceEffect : Effect {
    /** Распознать текст по файлу снимка [uri] (WFP Ш4). Первый эффект экрана. */
    data class RecognizeText(
        val uri: String,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.RecognitionFailed(
            errorRes = R.string.photo_words_error_recognition,
        )
    }

    /** Запрос слов на сервер одним списком токенов (WFP Ш6). */
    data class LookupWords(
        val tokens: List<LookupToken>,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.LookupUnavailable(
            errorRes = R.string.photo_words_error_server,
        )

        override fun toString(): String = "LookupWords(tokens=${tokens.size})"
    }

    /** Форма словаря снимка и его леммы с текстами лемм ответа [lemmaTexts] (AWP Ш1). */
    data class LoadForm(
        val dictionaryId: Long,
        val lemmaTexts: List<String>,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.FormFailed(
            errorRes = R.string.photo_add_form_failed,
        )

        override fun toString(): String =
            "LoadForm(dictionaryId=$dictionaryId, lemmas=${lemmaTexts.size})"
    }

    /** Пакет окна одной транзакцией (AWP Ш4). Исключение — сбой записи, транзакция откачена. */
    data class AddWords(
        val batch: WordsBatch,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.AddFailed(failure = AddFailureUi.Failed)

        override fun toString(): String {
            val lexemes = batch.words.sumOf { it.lexemes.size }
            return "AddWords(dictionaryId=${batch.dictionaryId}, " +
                "words=${batch.words.size}, lexemes=$lexemes)"
        }
    }
}

/**
 * Переходы экрана. Закрытие без результата — `NavigationEffect.Back` из
 * библиотеки.
 */
sealed interface ScanNavigationEffect : NavigationEffect {
    /** Назад на вкладку «Слова» со счётом записанного (AWP-23). */
    data class CloseWithResult(
        val lexemeCount: Int,
        val wordCount: Int,
    ) : ScanNavigationEffect
}
