package me.apomazkin.settingstab.logic

import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateEffectHandler
import me.apomazkin.settingstab.deps.SettingsTabUseCase
import me.apomazkin.mate.LogTags
import me.apomazkin.logger.LexemeLogger

sealed interface DatasourceEffect : Effect {
    data class ExportData(val uri: Uri) : DatasourceEffect
    data class ImportData(val uri: Uri) : DatasourceEffect
}

/**
 * @param io диспатчер блокирующих операций; прод — Dispatchers.IO,
 *   в тестах можно подставить тестовый.
 */
class DatasourceEffectHandler(
    private val settingsTabUseCase: SettingsTabUseCase,
    private val logger: LexemeLogger,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : MateEffectHandler<Msg, DatasourceEffect> {

    override val effectFamily = DatasourceEffect::class

    override suspend fun runEffect(effect: DatasourceEffect, consumer: (Msg) -> Unit) {
        logger.d(tag = LogTags.MATE, message = "RunEffect: $effect")
        val msg: Msg = when (effect) {
            is DatasourceEffect.ExportData -> withContext(io) {
                val exported = settingsTabUseCase.exportData(uri = effect.uri)
                Msg.ExportFile(uri = exported)
            }

            is DatasourceEffect.ImportData -> withContext(io) {
                val ok = settingsTabUseCase.importData(uri = effect.uri)
                if (ok) Msg.ImportSuccess else Msg.ImportFailed
            }
        }
        consumer(msg)
    }
}
