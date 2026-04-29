package me.apomazkin.polytrainer.di.module.dictionary

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.dictionary.DictionaryUseCase
import me.apomazkin.dictionary.model.CountryFlagItem
import me.apomazkin.dictionary.model.DictionaryItem
import me.apomazkin.dictionary.model.DictionaryListItem
import me.apomazkin.flags.CountryProvider
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import javax.inject.Inject

class DictionaryUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val countryProvider: CountryProvider,
    private val prefsProvider: PrefsProvider,
) : DictionaryUseCase {

    private val allFlags: List<CountryFlagItem> by lazy { loadAllFlags() }
    private val filterQuery = MutableStateFlow("")

    override suspend fun getDictionaryList(): List<DictionaryListItem> {
        return dictionaryApi.getDictionaryList().map { entity ->
            DictionaryListItem(
                id = entity.id,
                name = entity.name,
                flagRes = entity.numericCode?.let { countryProvider.getFlagRes(it) },
            )
        }
    }

    override fun flowDictionaryList(): Flow<List<DictionaryListItem>> {
        return dictionaryApi.flowDictionaryList().map { list ->
            list.map { entity ->
                DictionaryListItem(
                    id = entity.id,
                    name = entity.name,
                    flagRes = entity.numericCode?.let { countryProvider.getFlagRes(it) },
                )
            }
        }
    }

    override suspend fun addDictionary(name: String, numericCode: Int?): Long {
        val id = dictionaryApi.addDictionary(name, numericCode)
        setCurrentDictionary(id)
        return id
    }

    override suspend fun updateDictionary(id: Long, name: String, numericCode: Int?) {
        dictionaryApi.updateDictionary(id, name, numericCode)
    }

    override suspend fun deleteDictionary(id: Long) {
        dictionaryApi.deleteDictionary(id)
        val currentId = prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)
        if (currentId == id) {
            val remaining = dictionaryApi.getDictionaryList().firstOrNull()
            remaining?.let { setCurrentDictionary(it.id) }
        }
    }

    override suspend fun setCurrentDictionary(id: Long) {
        prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, id)
    }

    override fun updateFilter(query: String) {
        filterQuery.value = query
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun flagsFlow(): Flow<List<CountryFlagItem>> {
        return filterQuery
            .transformLatest { query ->
                if (query.isBlank()) emit(query) else { delay(300L); emit(query) }
            }
            .map { query -> filterFlags(allFlags, query) }
    }

    override suspend fun getDictionary(id: Long): DictionaryItem {
        val entity = dictionaryApi.getDictionaryById(id)
            ?: error("Dictionary with id=$id not found")
        return DictionaryItem(
            id = entity.id,
            name = entity.name,
            numericCode = entity.numericCode,
        )
    }

    override fun findFlag(numericCode: Int): CountryFlagItem? {
        return allFlags.firstOrNull { it.numericCode == numericCode }
    }

    private fun loadAllFlags(): List<CountryFlagItem> {
        val deviceLocale = java.util.Locale.getDefault()
        return countryProvider.getAllCountries().map { country ->
            val localized = java.util.Locale("", country.alpha2)
                .getDisplayCountry(deviceLocale)
            CountryFlagItem(
                numericCode = country.numericCode,
                countryName = country.name,
                localizedName = localized,
                flagRes = countryProvider.getFlagRes(country.numericCode),
                languages = countryProvider.getLanguagesForCountry(country.numericCode),
            )
        }
    }

    private fun filterFlags(
        allFlags: List<CountryFlagItem>,
        query: String,
    ): List<CountryFlagItem> {
        if (query.isBlank()) return allFlags
        val q = query.trim().lowercase()
        return allFlags.filter { flag ->
            flag.countryName.lowercase().contains(q) ||
                flag.localizedName.lowercase().contains(q) ||
                flag.languages.any { lang -> lang.lowercase().contains(q) }
        }
    }

}
