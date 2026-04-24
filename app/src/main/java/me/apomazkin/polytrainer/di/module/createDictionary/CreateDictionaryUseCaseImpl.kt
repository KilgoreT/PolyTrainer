package me.apomazkin.polytrainer.di.module.createDictionary

import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.createdictionary.CreateDictionaryUseCase
import me.apomazkin.flags.FlagProvider
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import javax.inject.Inject

class CreateDictionaryUseCaseImpl @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val flagProvider: FlagProvider,
    private val prefsProvider: PrefsProvider,
) : CreateDictionaryUseCase {
    override suspend fun getFlagRes(numericCode: Int): Int =
        flagProvider.getFlagRes(numericCode)

    override suspend fun addDictionary(numericCode: Int, name: String): Long {
        return dictionaryApi.addDictionary(numericCode, name)
    }

    override suspend fun saveCurrentDictionary(numericCode: Int) {
        prefsProvider.setInt(PrefKey.CURRENT_DICTIONARY_ID_LONG, numericCode)
    }
}