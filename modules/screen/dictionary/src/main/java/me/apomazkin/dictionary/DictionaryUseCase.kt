package me.apomazkin.dictionary

import kotlinx.coroutines.flow.Flow
import me.apomazkin.dictionary.model.CountryFlagItem
import me.apomazkin.dictionary.model.DictionaryListItem
import me.apomazkin.dictionary.model.LanguageItem

interface DictionaryUseCase {
    suspend fun getDictionaryList(): List<DictionaryListItem>
    fun flowDictionaryList(): Flow<List<DictionaryListItem>>
    suspend fun addDictionary(name: String, numericCode: Int?): Long
    suspend fun updateDictionary(id: Long, name: String, numericCode: Int?)
    suspend fun deleteDictionary(id: Long)
    suspend fun setCurrentDictionary(id: Long)
    fun getAvailableLanguages(): List<LanguageItem>
    suspend fun getCountriesForLanguage(languageCode: String): List<CountryFlagItem>
}
