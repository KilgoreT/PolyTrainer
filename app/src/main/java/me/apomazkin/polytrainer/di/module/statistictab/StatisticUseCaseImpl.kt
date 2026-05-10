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
        private val dictionaryApi: CoreDbApi.DictionaryApi,
        private val statisticDbApi: CoreDbApi.StatisticApi,
        private val prefsProvider: PrefsProvider,
) : StatisticUseCase {
    override suspend fun flowWordCount(): Flow<Int> {
        val dictionaryIdFlow: Flow<Int?> = prefsProvider
                .getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)
                .mapLatest { id ->
                    val dictionaryId = id?.let { dictionaryApi.getDictionaryById(it)?.id?.toInt() }
                            ?: dictionaryApi.getDictionaryList().firstOrNull()?.id?.toInt()
                    dictionaryId
                }
        return  dictionaryIdFlow
                .filterNotNull()
                .flatMapLatest { dictionaryId ->
                    statisticDbApi.flowWordCount(dictionaryId)
                }
    }

    override suspend fun flowLexemeCount(): Flow<Int> {
        val dictionaryIdFlow: Flow<Int?> = prefsProvider
                .getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)
                .mapLatest { id ->
                    val dictionaryId = id?.let { dictionaryApi.getDictionaryById(it)?.id?.toInt() }
                            ?: dictionaryApi.getDictionaryList().firstOrNull()?.id?.toInt()
                    dictionaryId
                }
        return  dictionaryIdFlow
                .filterNotNull()
                .flatMapLatest { dictionaryId ->
                    statisticDbApi.flowLexemeCount(dictionaryId)
                }
    }

    override suspend fun flowQuizStat(): Flow<Map<Int, Int>> {
        val dictionaryIdFlow: Flow<Int?> = prefsProvider
            .getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG)
            .mapLatest { id ->
                val dictionaryId = id?.let { dictionaryApi.getDictionaryById(it)?.id?.toInt() }
                    ?: dictionaryApi.getDictionaryList().firstOrNull()?.id?.toInt()
                dictionaryId
            }
        return dictionaryIdFlow
            .filterNotNull()
            .flatMapLatest { dictionaryId ->
                statisticDbApi.flowQuizCount(dictionaryId = dictionaryId, maxGrade = 4)
            }
    }
}