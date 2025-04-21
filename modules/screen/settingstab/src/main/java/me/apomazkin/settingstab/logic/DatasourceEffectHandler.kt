package me.apomazkin.settingstab.logic

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateEffectHandler
import me.apomazkin.settingstab.deps.SettingsTabUseCase

/**
 * Effect
 */
internal sealed interface DatasourceEffect : Effect {
    data class ExportData(val uri: Uri) : DatasourceEffect
    data class ImportData(val uri: Uri) : DatasourceEffect
}

/**
 * EffectHandler for datastore calls.
 */
internal class DatasourceEffectHandler(
    private val settingsTabUseCase: SettingsTabUseCase
) : MateEffectHandler<Msg, Effect> {
    
    override suspend fun runEffect(
        effect: Effect,
        consumer: (Msg) -> Unit
    ) {
        Log.d("##MATE##", "RunEffect: $effect")
        return when (val eff = effect as? DatasourceEffect) {
            is DatasourceEffect.ExportData -> {
                withContext(Dispatchers.IO) {
                    val exported = settingsTabUseCase.exportData(uri = eff.uri)
                    Msg.ExportFile(uri = exported)
                }
            }
            
            is DatasourceEffect.ImportData -> {
                withContext(Dispatchers.IO) {
                    settingsTabUseCase.importData(uri = eff.uri).let {
                        if (it) {
                            Msg.ImportSuccess
                        } else {
                            Msg.ImportFailed
                        }
                    }
                }
            }
            
            null -> Msg.Empty
        }.let(consumer)
    }
}