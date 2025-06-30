@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalCoroutinesApi::class)

package me.apomazkin.polytrainer.di.module.statistictab

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.stattab.deps.StatisticUseCase
import javax.inject.Inject

class StatisticUseCaseImpl @Inject constructor(
        private val langApi: CoreDbApi.LangApi,
        private val statisticDbApi: CoreDbApi.StatisticApi,
        private val prefsProvider: PrefsProvider,
) : StatisticUseCase {
    override suspend fun flowWordCount(): Flow<Int> {
        val langIdFlow: Flow<Int?> = prefsProvider
                .getIntFlow(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT)
                .filterNotNull()
                .mapLatest { numericCode ->
                    val langId = langApi
                            .getLang(numericCode = numericCode)?.id
                            ?: langApi.getLangList().firstOrNull()?.id
                    langId
                }
        return  langIdFlow
                .filterNotNull()
                .flatMapLatest { langId ->
                    statisticDbApi.flowWordCount(langId)
                }
    }

    override suspend fun flowLexemeCount(): Flow<Int> {
        val langIdFlow: Flow<Int?> = prefsProvider
                .getIntFlow(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT)
                .filterNotNull()
                .mapLatest { numericCode ->
                    val langId = langApi
                            .getLang(numericCode = numericCode)?.id
                            ?: langApi.getLangList().firstOrNull()?.id
                    langId
                }
        return  langIdFlow
                .filterNotNull()
                .flatMapLatest { langId ->
                    statisticDbApi.flowLexemeCount(langId)
                }
    }

    override suspend fun flowQuizStat(): Flow<Map<Int, Int>> {
        val langIdFlow: Flow<Int?> = prefsProvider
            .getIntFlow(PrefKey.CURRENT_LANG_NUMERIC_CODE_INT)
            .filterNotNull()
            .mapLatest { numericCode ->
                val langId = langApi
                    .getLang(numericCode = numericCode)?.id
                    ?: langApi.getLangList().firstOrNull()?.id
                langId
            }
        return langIdFlow
            .filterNotNull()
            .flatMapLatest { langId ->
                statisticDbApi.flowQuizCount(langId = langId, maxGrade = 4)
            }
    }
}