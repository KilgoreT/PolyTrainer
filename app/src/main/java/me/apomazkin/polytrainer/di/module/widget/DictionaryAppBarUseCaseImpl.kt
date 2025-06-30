package me.apomazkin.polytrainer.di.module.widget

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.dictionaryappbar.deps.DictionaryAppBarUseCase
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.dictionarytab.deps.LangNotFoundException
import me.apomazkin.flags.FlagProvider
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import javax.inject.Inject

class DictionaryAppBarUseCaseImpl @Inject constructor(
        private val langApi: CoreDbApi.LangApi,
        private val prefsProvider: PrefsProvider,
        private val flagProvider: FlagProvider,
) : DictionaryAppBarUseCase {
    override fun flowAvailableDict(): Flow<List<DictUiEntity>> = langApi.flowLangList()
            .map {
                it.map { lang ->
                    DictUiEntity(
                            flagRes = flagProvider.getFlagRes(lang.numericCode),
                            title = lang.name,
                            numericCode = lang.numericCode,
                    )
                }
            }

    override fun flowCurrentDict(): Flow<DictUiEntity> {
        return prefsProvider.getIntFlow(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT)
                .map { numeric: Int ->
                    val lang = (langApi
                            .getLang(numeric)
                            ?: langApi.getLangList().firstOrNull())
                            ?.let { lang ->
                                DictUiEntity(
                                        flagRes = flagProvider.getFlagRes(lang.numericCode),
                                        title = lang.name,
                                        numericCode = lang.numericCode,
                                )
                            }
                    lang ?: throw LangNotFoundException()
                }
    }

    override suspend fun changeDict(numericCode: Int) {
        prefsProvider.setInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT, numericCode)
    }
}