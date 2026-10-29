package me.apomazkin.scancandidates.deps

import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.FormOutcome
import me.apomazkin.lexeme.LemmaContext
import me.apomazkin.lexeme.LemmasOutcome
import me.apomazkin.lexeme.LexemesOutcome
import me.apomazkin.lexeme.LookupToken
import me.apomazkin.lexeme.RecognitionOutcome
import me.apomazkin.lexeme.WordsBatch

/**
 * Порт экрана снимка. Говорит доменом: исход каждого вызова — типом на случай.
 * Непредвиденный сбой — исключение, в сообщение его превращает `onFail`
 * эффекта. Реализует порт `app`.
 */
interface ScanCandidatesUseCase {
    /**
     * Распознавание текста по файлу снимка (TFP Ш4–Ш5): токены как распознаны, с
     * повторами, пустые прочь, потолок 2000. Текст есть или пусто — исходом.
     */
    suspend fun recognize(uri: String): RecognitionOutcome

    /**
     * Отпустить доступ к файлу галереи, закреплённый вкладкой при выборе снимка
     * (Д52). У файла камеры ничего не делает.
     */
    suspend fun releaseImageAccess(uri: String)

    /**
     * Запрос лемм текста на сервер (TFP Ш6, LFT-02): токены и языки текущего
     * словаря, без дерева компонентов. Исход: леммы текста, нет слов, отказ с
     * причиной, сервер не ответил.
     */
    suspend fun lemmasFromTokens(tokens: List<LookupToken>): LemmasOutcome

    /**
     * Форма словаря снимка (AWP Ш4): включённые живые компоненты всех
     * зависимостей, опции всех «Выборов», группы, леммы словаря с текстами слов
     * списков [lemmaTexts] — у леммы дата добавления и сохранённые лексемы
     * (AWP-18, AWP-46).
     */
    suspend fun loadForm(
        dictionaryId: Long,
        lemmaTexts: List<String>,
    ): FormOutcome

    /**
     * Значения слова списков (AWP Ш6, Ш8, LFL-103): лемма слова с текстом и
     * переводом снимка [context], языки и дерево компонентов словаря снимка
     * [dictionaryId] на сервер одним запросом. Исход: слово с леммой, не
     * найдено, значений нет, отказ с причиной, сервер не ответил, слово
     * «ошибка». Лемма ответа не та, что просили, — с леммами словаря с её
     * текстом (AWP-43.7).
     */
    suspend fun lexemesFromLemma(
        dictionaryId: Long,
        lemma: String,
        context: LemmaContext,
    ): LexemesOutcome

    /**
     * Запись пакета одного слова одной транзакцией (AWP Ш9, AWP-20): записано
     * или отказ до первой вставки с причиной; сбой после — исключение.
     */
    suspend fun addWord(batch: WordsBatch): AddWordsBatchOutcome
}
