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
     * Запрос значений слова (AWP Ш6, Ш7, LFL-103): [word] — слово списка,
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
     * «Создать»: запись плашки [key] слова [word] в новое слово одной
     * транзакцией (AWP Ш9, AWP-20.9); [batch] — пакет из одного нового слова
     * с одной лексемой. Сбой эффекта — «не добавилось» у плашки.
     */
    data class CreateWord(
        val word: String,
        val key: String,
        val batch: WordsBatch,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.AddFailed(
            text = word,
            key = key,
            failure = AddFailureUi.Failed,
        )

        override fun toString(): String = "CreateWord(word=$word, key=$key)"
    }

    /**
     * «Перенести»: запись плашки [key] слова [word] в выбранное слово словаря
     * одной транзакцией (AWP Ш9, AWP-20.10); [batch] — пакет из одного
     * существующего слова с одной лексемой. Сбой эффекта — «не добавилось» у
     * плашки.
     */
    data class TransferLexeme(
        val word: String,
        val key: String,
        val batch: WordsBatch,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.AddFailed(
            text = word,
            key = key,
            failure = AddFailureUi.Failed,
        )

        override fun toString(): String =
            "TransferLexeme(word=$word, key=$key, wordId=${batch.words.firstOrNull()?.wordId})"
    }

    /**
     * Леммы словаря снимка с текстом слова [word] заново после записи (AWP
     * Ш9, AWP-63): тот же запрос формы по одному тексту. Сбой эффекта —
     * плашки словаря как были.
     */
    data class ReloadLemmas(
        val word: String,
        val dictionaryId: Long,
    ) : DatasourceEffect,
        RecoverableEffect<Msg> {
        override fun onFail(error: Throwable) = Msg.LemmasReloadFailed(text = word)
    }
}
