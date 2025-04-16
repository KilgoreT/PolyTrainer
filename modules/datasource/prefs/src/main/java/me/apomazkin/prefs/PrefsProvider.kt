package me.apomazkin.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class PrefsProvider(
    private val context: Context
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "PolyStore")

    fun getIntFlow(prefKey: PrefKey): Flow<Int> {
        return context.dataStore.data
            .map {
                it[intPreferencesKey(prefKey.value)]
                    ?: throw IllegalStateException("PrefKey $prefKey not found")
            }
    }
    
    suspend fun getInt(prefKey: PrefKey): Int? {
        return context.dataStore.data.firstOrNull()?.get(intPreferencesKey(prefKey.value))
        //            ?: throw IllegalStateException("PrefKey $prefKey not found")
    }

    suspend fun setInt(prefKey: PrefKey, value: Int) {
        context.dataStore.edit {
            it[intPreferencesKey(prefKey.value)] = value
        }
    }
}

enum class PrefKey(val value: String) {
    CURRENT_LANG_NUMERIC_CODE_INT("currentLangNumericCode")
}