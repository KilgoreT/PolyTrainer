package me.apomazkin.polytrainer.di.module.dictionary

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.dictionary.DictionaryUseCase
import me.apomazkin.dictionary.model.CountryFlagItem
import me.apomazkin.dictionary.model.DictionaryListItem
import me.apomazkin.dictionary.model.LanguageItem
import me.apomazkin.flags.CountryProvider
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import java.util.Locale
import javax.inject.Inject

class DictionaryUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val countryProvider: CountryProvider,
    private val prefsProvider: PrefsProvider,
) : DictionaryUseCase {

    override suspend fun getDictionaryList(): List<DictionaryListItem> {
        return dictionaryApi.getDictionaryList().map { entity ->
            DictionaryListItem(
                id = entity.id,
                name = entity.name,
                flagRes = entity.numericCode?.let { countryProvider.getFlagRes(it) },
                languageName = entity.numericCode?.let { resolveLanguageName(it) },
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
                    languageName = entity.numericCode?.let { resolveLanguageName(it) },
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

    override fun getAvailableLanguages(): List<LanguageItem> {
        val currentLocale = Locale.getDefault()
        return Locale.getISOLanguages()
            .map { code -> Locale(code) }
            .map { locale ->
                LanguageItem(
                    code = locale.language,
                    displayName = locale.getDisplayLanguage(currentLocale)
                        .replaceFirstChar { it.uppercase() },
                )
            }
            .filter { it.displayName.isNotBlank() }
            .sortedBy { it.displayName }
    }

    override suspend fun getCountriesForLanguage(languageCode: String): List<CountryFlagItem> {
        val languageName = Locale(languageCode)
            .getDisplayLanguage(Locale.ENGLISH)
            .lowercase()

        return countryProvider.getAllCountries()
            .filter { country ->
                countryProvider.getLanguagesForCountry(country.numericCode)
                    .any { it.lowercase().contains(languageName) }
            }
            .map { country ->
                CountryFlagItem(
                    numericCode = country.numericCode,
                    countryName = country.name,
                    flagRes = countryProvider.getFlagRes(country.numericCode),
                )
            }
    }

    private fun resolveLanguageName(numericCode: Int): String? {
        val languages = countryProvider.getLanguagesForCountry(numericCode)
        return languages.firstOrNull()
    }
}
