package me.apomazkin.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class PrefsProvider(
    private val context: Context
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lexemePrefStore")

    fun getIntFlow(prefKey: PrefKey): Flow<Int?> {
        return context.dataStore.data
            .map { it[intPreferencesKey(prefKey.value)] }
    }
    
    suspend fun getInt(prefKey: PrefKey): Int? {
        return context.dataStore.data.firstOrNull()?.get(intPreferencesKey(prefKey.value))
    }

    suspend fun setInt(prefKey: PrefKey, value: Int) {
        context.dataStore.edit {
            it[intPreferencesKey(prefKey.value)] = value
        }
    }

    fun getLongFlow(prefKey: PrefKey): Flow<Long?> {
        return context.dataStore.data
            .map { it[longPreferencesKey(prefKey.value)] }
    }

    suspend fun getLong(prefKey: PrefKey): Long? {
        return context.dataStore.data.firstOrNull()?.get(longPreferencesKey(prefKey.value))
    }

    suspend fun setLong(prefKey: PrefKey, value: Long?) {
        context.dataStore.edit {
            val key = longPreferencesKey(prefKey.value)
            if (value == null) {
                it.remove(key)
            } else {
                it[key] = value
            }
        }
    }

    fun getBooleanFlow(prefKey: PrefKey): Flow<Boolean> {
        return context.dataStore.data
            .map {
                it[booleanPreferencesKey(prefKey.value)]
                    ?: false
            }
    }

    suspend fun getBoolean(prefKey: PrefKey): Boolean? {
        return context.dataStore.data.firstOrNull()?.get(booleanPreferencesKey(prefKey.value))
    }
    suspend fun setBoolean(prefKey: PrefKey, value: Boolean) {
        context.dataStore.edit {
            it[booleanPreferencesKey(prefKey.value)] = value
        }
    }

    /**
     * IS481: raw-string dynamic-key API для per-entity prefs (например,
     * per-dictionary quiz picker selection — ключ `quiz_picker_dict_<id>`).
     * `PrefKey` enum не используется — ключи живут вне enum.
     */
    suspend fun getStringByRawKey(key: String): String? {
        return context.dataStore.data.firstOrNull()?.get(stringPreferencesKey(key))
    }

    fun getStringFlowByRawKey(key: String): Flow<String?> {
        return context.dataStore.data
            .map { it[stringPreferencesKey(key)] }
    }

    suspend fun setStringByRawKey(key: String, value: String?) {
        context.dataStore.edit {
            val prefKey = stringPreferencesKey(key)
            if (value == null) {
                it.remove(prefKey)
            } else {
                it[prefKey] = value
            }
        }
    }
}

enum class PrefKey(val value: String) {
    CURRENT_DICTIONARY_ID_LONG("LONG_currentDictionaryId"),
    CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN("Boolean_chatEarliestReviewedStatus"),
    CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN("Boolean_chatFrequentMistakesStatus"),
    CHAT_DEBUG_STATUS_BOOLEAN("Boolean_chatDebugStatus")
}