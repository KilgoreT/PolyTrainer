package me.apomazkin.dictionary

import kotlinx.coroutines.flow.Flow
import me.apomazkin.dictionary.model.CountryFlagItem
import me.apomazkin.dictionary.model.DictionaryItem
import me.apomazkin.dictionary.model.DictionaryListItem
import me.apomazkin.dictionary.model.LanguageDefaults
import me.apomazkin.dictionary.model.LanguageItem

interface DictionaryUseCase {
    suspend fun getDictionaryList(): List<DictionaryListItem>
    fun flowDictionaryList(): Flow<List<DictionaryListItem>>
    suspend fun addDictionary(
        name: String,
        numericCode: Int?,
        learningLanguage: String,
        translationLanguage: String,
    ): Long
    suspend fun updateDictionary(
        id: Long,
        name: String,
        numericCode: Int?,
        learningLanguage: String,
        translationLanguage: String,
    )
    suspend fun deleteDictionary(id: Long)
    suspend fun setCurrentDictionary(id: Long)
    fun updateFilter(query: String)
    fun flagsFlow(): Flow<List<CountryFlagItem>>
    suspend fun getDictionary(id: Long): DictionaryItem
    fun findFlag(numericCode: Int): CountryFlagItem?

    /** IS525: языки новой формы — словарь без флага (английский) и перевод (язык телефона). */
    fun languageDefaults(): LanguageDefaults

    /** IS525: полный список языков для выбора, названия на языке интерфейса, по алфавиту. */
    fun allLanguages(): List<LanguageItem>
}
