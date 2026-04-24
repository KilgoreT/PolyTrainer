package me.apomazkin.polytrainer.di.module.widget

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.dictionaryappbar.deps.DictionaryAppBarUseCase
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.dictionarytab.deps.DictionaryNotFoundException
import me.apomazkin.flags.FlagProvider
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import javax.inject.Inject

class DictionaryAppBarUseCaseImpl @Inject constructor(
        private val dictionaryApi: CoreDbApi.DictionaryApi,
        private val prefsProvider: PrefsProvider,
        private val flagProvider: FlagProvider,
) : DictionaryAppBarUseCase {
    override fun flowAvailableDict(): Flow<List<DictUiEntity>> = dictionaryApi.flowDictionaryList()
            .map {
                it.map { dict ->
                    DictUiEntity(
                            flagRes = flagProvider.getFlagRes(dict.numericCode ?: 0),
                            title = dict.name,
                            numericCode = dict.numericCode ?: 0,
                    )
                }
            }

    override fun flowCurrentDict(): Flow<DictUiEntity> {
        return prefsProvider.getIntFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)
                .map { numeric: Int ->
                    val dict = (dictionaryApi
                            .getDictionary(numeric)
                            ?: dictionaryApi.getDictionaryList().firstOrNull())
                            ?.let { dict ->
                                DictUiEntity(
                                        flagRes = flagProvider.getFlagRes(dict.numericCode ?: 0),
                                        title = dict.name,
                                        numericCode = dict.numericCode ?: 0,
                                )
                            }
                    dict ?: throw DictionaryNotFoundException()
                }
    }

    override suspend fun changeDict(numericCode: Int) {
        prefsProvider.setInt(PrefKey.CURRENT_DICTIONARY_ID_LONG, numericCode)
    }
}