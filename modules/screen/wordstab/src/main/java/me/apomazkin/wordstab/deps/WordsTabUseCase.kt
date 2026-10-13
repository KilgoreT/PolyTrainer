package me.apomazkin.wordstab.deps

import android.net.Uri
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.WordsBatch
import me.apomazkin.textrecognition.RecognizedText
import me.apomazkin.textrecognition.RecognizedWord
import me.apomazkin.wordrow.entity.TermUiItem

interface WordsTabUseCase {
    //TODO kilg 29.06.2025 21:33 завести слой доменных сущностей.
    suspend fun getCurrentDict(): DictUiEntity?
    fun flowCurrentDict(): Flow<DictUiEntity?>
    suspend fun changeDict(id: Long)

    suspend fun getWordList(): List<TermUiItem>
    fun searchTerms(
        pattern: String,
        dictionaryId: Int
    ): Flow<PagingData<TermUiItem>>

    suspend fun addWord(value: String): Long
    suspend fun deleteWord(wordId: Long)
    suspend fun updateWord(id: Long, value: String): Boolean

    /** IS527: текст со снимка (ML Kit на устройстве). */
    suspend fun recognizeText(uri: Uri): RecognizedText

    /**
     * IS529: токены одним запросом на сервер слов с языками текущего словаря
     * (Ш6, WFT-20); [runId] — номер запуска для склейки логов. Исключений не
     * бросает: сеть и отказы приходят вариантами [PhotoLookupOutcome].
     */
    suspend fun lookupWords(tokens: List<RecognizedWord>, runId: Long): PhotoLookupOutcome

    /**
     * IS532 (Ш1): форма словаря снимка по id, леммы словаря с текстами
     * [lemmaTexts] и запомненные соответствия. Исключений не бросает:
     * словаря нет или чтение упало — варианты [PhotoFormOutcome] (AWP-24).
     */
    suspend fun loadPhotoForm(dictionaryId: Long, lemmaTexts: List<String>): PhotoFormOutcome

    /**
     * IS532 (Ш4): пакет слов одной транзакцией (AWP-20). Отказ до первой
     * вставки — типизированный исход; исключение — сбой записи, транзакция
     * откачена (AWP-21).
     */
    suspend fun addPhotoWords(batch: WordsBatch): AddWordsBatchOutcome

    /** IS532 (Ш3): запомнить соответствия для словаря (AWP-06). */
    suspend fun savePhotoMapping(dictionaryId: Long, mapping: PhotoMapping)
}
