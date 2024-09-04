package me.apomazkin.polytrainer.di.module.createDictionary

import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.createdictionary.CreateDictionaryUseCase
import me.apomazkin.flags.FlagProvider
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import javax.inject.Inject

class CreateDictionaryUseCaseImpl @Inject constructor(
    private val dbApi: CoreDbApi,
    private val flagProvider: FlagProvider,
    private val prefsProvider: PrefsProvider,
) : CreateDictionaryUseCase {
    override suspend fun getFlagRes(numericCode: Int): Int =
        flagProvider.getFlagRes(numericCode)

    override suspend fun addLang(numericCode: Int, name: String): Long {
        return dbApi.addLangSuspend(numericCode, name)
    }

    override suspend fun saveCurrentLang(numericCode: Int) {
        prefsProvider.setInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT, numericCode)
    }
}