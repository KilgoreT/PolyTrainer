package me.apomazkin.polytrainer.logger

import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import me.apomazkin.logger.LogSink
import javax.inject.Inject

class LexemeLoggerImpl @Inject constructor(
    private val sinks: List<@JvmSuppressWildcards LogSink>
) : LexemeLogger {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        sinks.forEach { sink ->
            if (level >= sink.minLevel) {
                sink.write(level, tag, message, throwable)
            }
        }
    }
}
