package me.apomazkin.scancandidates.mate

import io.github.kilgoret.mate.Effect
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
    /** Распознать текст по файлу снимка [uri] (TFP Ш4). Первый эффект экрана. */
    data class RecognizeText(
        val uri: String,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.RecognitionFailed(
            errorRes = R.string.photo_words_error_recognition,
        )
    }

    /**
     * Один запрос лемм текста на сервер списком токенов как распознаны, с
     * повторами (TFP Ш6, LFT-02). Сбой эффекта — «Сервер не ответил» с
     * «Повторить».
     */
    data class ExtractLemmas(
        val tokens: List<LookupToken>,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.LemmasUnavailable(
            errorRes = R.string.photo_words_error_server,
        )

        override fun toString(): String = "ExtractLemmas(tokens=${tokens.size})"
    }

    /** Форма словаря снимка и его леммы с текстами слов списков [lemmaTexts] (AWP Ш4). */
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

    /**
     * Запрос значений слова (AWP Ш6, Ш8, LFL-103): [word] — слово списка,
     * адрес карточки; [lemma] — что ищет сервер, текст слова как есть;
     * [dictionaryId] — словарь снимка: языки и дерево; [text] и
     * [translation] — исправленный текст снимка и его перевод, контекст.
     * Сбой эффекта — «Сервер не ответил» с «Повторить». В лог — счётчиками:
     * длина текста, не сам текст.
     */
    data class LookupLemma(
        val word: String,
        val lemma: String,
        val dictionaryId: Long,
        val text: String,
        val translation: String,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.LexemesUnavailable(
            text = word,
            errorRes = R.string.photo_words_error_server,
        )

        override fun toString(): String =
            "LookupLemma(word=$word, lemma=$lemma, dictionaryId=$dictionaryId, text=${text.length})"
    }

    /**
     * Запись одного слова одной транзакцией (AWP Ш9): [word] — слово списка,
     * адрес карточки; [batch] — пакет из одного слова. Сбой эффекта — «не
     * добавилось» у слова. В лог — слово и число лексем.
     */
    data class AddWord(
        val word: String,
        val batch: WordsBatch,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.AddFailed(
            text = word,
            failure = AddFailureUi.Failed,
        )

        override fun toString(): String =
            "AddWord(word=$word, lexemes=${batch.words.sumOf { it.lexemes.size }})"
    }
}
