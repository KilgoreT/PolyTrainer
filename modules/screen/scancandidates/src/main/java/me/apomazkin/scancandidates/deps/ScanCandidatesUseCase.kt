package me.apomazkin.scancandidates.deps

import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.FormOutcome
import me.apomazkin.lexeme.LookupOutcome
import me.apomazkin.lexeme.LookupToken
import me.apomazkin.lexeme.RecognitionOutcome
import me.apomazkin.lexeme.WordsBatch

/**
 * Порт экрана лексем-кандидатов со снимка. Говорит доменом: исход каждого
 * вызова — типом на случай. Непредвиденный сбой — исключение, в сообщение его
 * превращает `onFail` эффекта. Реализует порт `app`.
 */
interface ScanCandidatesUseCase {
    /**
     * Распознавание текста по файлу снимка (WFP Ш4–Ш5): токены как распознаны, с
     * повторами, пустые прочь, потолок 2000. Текст есть или пусто — исходом.
     */
    suspend fun recognize(uri: String): RecognitionOutcome

    /**
     * Отпустить доступ к файлу галереи, закреплённый вкладкой при выборе снимка
     * (Д52). У файла камеры ничего не делает.
     */
    suspend fun releaseImageAccess(uri: String)

    /**
     * Запрос слов на сервер (WFP Ш6): токены, языки и дерево компонентов
     * текущего словаря (WFT-78). Исход: слова с рекомендациями, нет слов, отказ
     * с причиной, сервер не ответил.
     */
    suspend fun lookupWords(tokens: List<LookupToken>): LookupOutcome

    /**
     * Форма словаря снимка (AWP Ш1): включённые живые компоненты всех
     * зависимостей, опции всех «Выборов», группы, леммы словаря с текстами лемм
     * ответа [lemmaTexts].
     */
    suspend fun loadForm(
        dictionaryId: Long,
        lemmaTexts: List<String>,
    ): FormOutcome

    /**
     * Запись пакета одной транзакцией (AWP Ш4, AWP-20): записано с id или отказ
     * проверки до первой вставки. Сбой после первой вставки — исключение,
     * транзакция откатывается.
     */
    suspend fun addWords(batch: WordsBatch): AddWordsBatchOutcome
}
