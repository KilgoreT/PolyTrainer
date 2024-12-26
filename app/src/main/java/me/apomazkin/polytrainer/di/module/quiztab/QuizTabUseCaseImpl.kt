package me.apomazkin.polytrainer.di.module.quiztab

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.flags.FlagProvider
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.quiztab.deps.QuizTabUseCase
import javax.inject.Inject

class QuizTabUseCaseImpl @Inject constructor(
    private val dbApi: CoreDbApi,
    private val langApi: CoreDbApi.LangApi,
    private val termApi: CoreDbApi.TermApi,
    private val lexemeApi: CoreDbApi.LexemeApi,
    private val prefsProvider: PrefsProvider,
    private val flagProvider: FlagProvider,
) : QuizTabUseCase {
    
    override suspend fun getCurrentDict(): DictUiEntity {
        val numericCode =
            prefsProvider.getInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT)
        langApi.getLang(numericCode = numericCode)?.let {
            return DictUiEntity(
                flagRes = flagProvider.getFlagRes(it.numericCode),
                title = it.name ?: "",
                numericCode = it.numericCode,
            )
        }
        throw IllegalStateException("Language not found")
    }
    
    override suspend fun getAvailableDict(): List<DictUiEntity> =
        langApi.getLangList().map {
            DictUiEntity(
                flagRes = flagProvider.getFlagRes(it.numericCode),
                title = it.name,
                numericCode = it.numericCode,
            )
        }
    
    override fun flowAvailableDict(): Flow<List<DictUiEntity>> =
        langApi.flowLangList().map {
            it.map { lang ->
                DictUiEntity(
                    flagRes = flagProvider.getFlagRes(lang.numericCode),
                    title = lang.name,
                    numericCode = lang.numericCode,
                )
            }
        }
    
    override suspend fun changeDict(numericCode: Int) {
        prefsProvider.setInt(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT, numericCode)
    }
}