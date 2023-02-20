package me.apomazkin.polytrainer.di.module.main

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.flags.FlagProvider
import me.apomazkin.main.entity.LangUiEntity
import me.apomazkin.main.widget.top.MainTopBarUseCase
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import javax.inject.Inject

class MainTopBarUseCaseImpl @Inject constructor(
    private val dbApi: CoreDbApi,
    private val flagProvider: FlagProvider,
    private val prefsProvider: PrefsProvider,
) : MainTopBarUseCase {

    override fun getCurrentLang(): Flow<Int> {
        return prefsProvider.getInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT)
    }

    override fun getAvailableLang(): Flow<List<LangUiEntity>> =
        dbApi.getLangSuspend().map { list ->
            list.map {
                LangUiEntity(
                    iconRes = flagProvider.getFlagRes(it.numericCode),
                    title = it.name ?: "",
                    numericCode = it.numericCode,
                )
            }
        }

    override suspend fun changeLang(numericCode: Int) {
        prefsProvider.setInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT, numericCode)
    }
}