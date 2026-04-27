package me.apomazkin.polytrainer.di.module.widget

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.dictionaryappbar.deps.DictionaryAppBarUseCase
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.dictionarytab.deps.DictionaryNotFoundException
import me.apomazkin.flags.CountryProvider
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import javax.inject.Inject

class DictionaryAppBarUseCaseImpl @Inject constructor(
        private val dictionaryApi: CoreDbApi.DictionaryApi,
        private val prefsProvider: PrefsProvider,
        private val countryProvider: CountryProvider,
) : DictionaryAppBarUseCase {
    override fun flowAvailableDict(): Flow<List<DictUiEntity>> = dictionaryApi.flowDictionaryList()
            .map {
                it.map { dict ->
                    DictUiEntity(
                            id = dict.id,
                            flagRes = dict.numericCode?.let { countryProvider.getFlagRes(it) } ?: 0,
                            title = dict.name,
                            numericCode = dict.numericCode ?: 0,
                    )
                }
            }

    override fun flowCurrentDict(): Flow<DictUiEntity> {
        return prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)
                .map { id: Long ->
                    val dict = (dictionaryApi
                            .getDictionaryById(id)
                            ?: dictionaryApi.getDictionaryList().firstOrNull())
                            ?.let { dict ->
                                DictUiEntity(
                                        id = dict.id,
                                        flagRes = dict.numericCode?.let { countryProvider.getFlagRes(it) } ?: 0,
                                        title = dict.name,
                                        numericCode = dict.numericCode ?: 0,
                                )
                            }
                    dict ?: throw DictionaryNotFoundException()
                }
    }

    override suspend fun changeDict(id: Long) {
        prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, id)
    }
}