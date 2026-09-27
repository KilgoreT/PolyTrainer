package me.apomazkin.polytrainer.di.module.widget

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.dictionaryappbar.deps.DictionaryAppBarUseCase
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.flags.CountryProvider
import me.apomazkin.polytrainer.di.module.dictionary.CurrentDictionaryProvider
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import javax.inject.Inject

class DictionaryAppBarUseCaseImpl @Inject constructor(
        private val dictionaryApi: CoreDbApi.DictionaryApi,
        private val prefsProvider: PrefsProvider,
        private val countryProvider: CountryProvider,
        private val currentDictionaryProvider: CurrentDictionaryProvider,
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

    // IS500: резолв текущего словаря — через единый CurrentDictionaryProvider.
    override fun flowCurrentDict(): Flow<DictUiEntity?> =
        currentDictionaryProvider.flowCurrentDict()
                .map { dict ->
                    // null если словарей нет — валидное доменное состояние (IS476)
                    dict?.let {
                        DictUiEntity(
                                id = it.id,
                                flagRes = it.numericCode?.let { nc -> countryProvider.getFlagRes(nc) } ?: 0,
                                title = it.name,
                                numericCode = it.numericCode ?: 0,
                        )
                    }
                }

    override suspend fun changeDict(id: Long) {
        prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, id)
    }
}