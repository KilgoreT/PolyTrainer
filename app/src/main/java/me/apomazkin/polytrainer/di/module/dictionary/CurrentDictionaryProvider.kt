package me.apomazkin.polytrainer.di.module.dictionary

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import javax.inject.Inject

/**
 * IS500. Единая точка резолва текущего словаря: pref
 * `CURRENT_DICTIONARY_ID_LONG` + fallback на первый словарь при
 * отсутствующем/протухшем id; `null` — валидное «словарей нет» (IS476,
 * инварианты pref'а — спека dictionary-list). До IS500 резолв
 * копировался по UseCaseImpl'ам — потребители обязаны идти через
 * провайдер.
 *
 * Потоки построены на `combine` с живым списком словарей: удаление или
 * переименование текущего словаря переэмичивает без смены pref'а.
 */
class CurrentDictionaryProvider @Inject constructor(
    private val dictionaryApi: CoreDbApi.DictionaryApi,
    private val prefsProvider: PrefsProvider,
) {

    fun flowCurrentDict(): Flow<DictionaryApiEntity?> =
        combine(
            prefsProvider.getLongFlow(PrefKey.CURRENT_DICTIONARY_ID_LONG),
            dictionaryApi.flowDictionaryList(),
        ) { id, list ->
            list.find { it.id == id } ?: list.firstOrNull()
        }.distinctUntilChanged()

    fun flowCurrentDictId(): Flow<Long?> =
        flowCurrentDict()
            .map { it?.id }
            .distinctUntilChanged()

    suspend fun getCurrentDictId(): Long? {
        val id = prefsProvider.getLong(PrefKey.CURRENT_DICTIONARY_ID_LONG)
        return (id?.let { dictionaryApi.getDictionaryById(it) }
            ?: dictionaryApi.getDictionaryList().firstOrNull())
            ?.id
    }
}
