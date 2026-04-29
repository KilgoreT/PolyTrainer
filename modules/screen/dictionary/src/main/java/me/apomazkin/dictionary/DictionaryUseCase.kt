package me.apomazkin.dictionary

import kotlinx.coroutines.flow.Flow
import me.apomazkin.dictionary.model.CountryFlagItem
import me.apomazkin.dictionary.model.DictionaryItem
import me.apomazkin.dictionary.model.DictionaryListItem

interface DictionaryUseCase {
    suspend fun getDictionaryList(): List<DictionaryListItem>
    fun flowDictionaryList(): Flow<List<DictionaryListItem>>
    suspend fun addDictionary(name: String, numericCode: Int?): Long
    suspend fun updateDictionary(id: Long, name: String, numericCode: Int?)
    suspend fun deleteDictionary(id: Long)
    suspend fun setCurrentDictionary(id: Long)
    fun updateFilter(query: String)
    fun flagsFlow(): Flow<List<CountryFlagItem>>
    suspend fun getDictionary(id: Long): DictionaryItem
    fun findFlag(numericCode: Int): CountryFlagItem?
}
