package me.apomazkin.polytrainer.logger

import me.apomazkin.ui.logger.LexemeLogger
import me.apomazkin.ui.logger.LogLevel
import me.apomazkin.ui.logger.LogSink
import javax.inject.Inject

class LexemeLoggerImpl @Inject constructor(
    private val sinks: List<@JvmSuppressWildcards LogSink>
) : LexemeLogger {
    override fun log(level: LogLevel, tag: String, message: String) {
        sinks.forEach { sink ->
            if (level >= sink.minLevel) {
                sink.write(level, tag, message)
            }
        }
    }
}
