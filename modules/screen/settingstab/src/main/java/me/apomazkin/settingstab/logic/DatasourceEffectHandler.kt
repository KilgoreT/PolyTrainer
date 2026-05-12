package me.apomazkin.settingstab.logic

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateTypedEffectHandler
import me.apomazkin.settingstab.deps.SettingsTabUseCase
import me.apomazkin.mate.LogTags
import me.apomazkin.logger.LexemeLogger
import javax.inject.Inject

sealed interface DatasourceEffect : Effect {
    data class ExportData(val uri: Uri) : DatasourceEffect
    data class ImportData(val uri: Uri) : DatasourceEffect
}

class DatasourceEffectHandler @Inject constructor(
    private val settingsTabUseCase: SettingsTabUseCase,
    private val logger: LexemeLogger,
) : MateTypedEffectHandler<Msg, DatasourceEffect>() {

    override fun filter(effect: Effect): DatasourceEffect? = effect as? DatasourceEffect

    override suspend fun onEffect(effect: DatasourceEffect, consumer: (Msg) -> Unit) {
        logger.d(tag = LogTags.MATE, message = "RunEffect: $effect")
        val msg: Msg = when (effect) {
            is DatasourceEffect.ExportData -> withContext(Dispatchers.IO) {
                val exported = settingsTabUseCase.exportData(uri = effect.uri)
                Msg.ExportFile(uri = exported)
            }

            is DatasourceEffect.ImportData -> withContext(Dispatchers.IO) {
                val ok = settingsTabUseCase.importData(uri = effect.uri)
                if (ok) Msg.ImportSuccess else Msg.ImportFailed
            }
        }
        consumer(msg)
    }
}
