package me.apomazkin.settingstab.logic

import me.apomazkin.mate.Effect
import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.ReducerResult
import me.apomazkin.settingstab.logic.DatasourceEffect.ExportData
import me.apomazkin.settingstab.logic.DatasourceEffect.ImportData
import me.apomazkin.ui.logger.LexemeLogger

internal class SettingsTabReducer(
    private val logger: LexemeLogger,
) : MateReducer<SettingsTabState, Msg, Effect> {
    override fun reduce(
        state: SettingsTabState,
        message: Msg
    ): ReducerResult<SettingsTabState, Effect> {
        logger.log(message = "Reduce --prevState--: $state ")
        logger.log(message = "Reduce ---message---: $message ")
        return when (message) {
            
            is Msg.ExportData -> state
                .showExporting() to setOf(ExportData(uri = message.uri))
            
            is Msg.ExportFile -> state
                .hideExporting()
                .showDbExportDialog(
                    uri = message.uri
                ) to setOf()
            
            is Msg.FileExported -> state
                .hideDbExportDialog() to setOf()
            
            is Msg.ImportData -> state
                .showExporting() to setOf(ImportData(uri = message.uri))
            
            is Msg.ImportFailed -> state
                .hideExporting() to setOf()
            
            is Msg.ImportSuccess -> state
                .hideExporting() to setOf()
            
            is UiMsg -> state to emptySet<Effect>()
            is Msg.Empty -> state to emptySet()
        }.also {
            logger.log(message = "Reduce --newState--: ${it.first} ")
            it.second.forEach { effect ->
                logger.log(message = "Reduce --toEffect--: $effect ")
            }
        }
    }
}