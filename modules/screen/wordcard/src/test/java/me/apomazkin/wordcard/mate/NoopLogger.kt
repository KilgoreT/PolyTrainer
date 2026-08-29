package me.apomazkin.wordcard.mate

import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel

/** IS493 Э5: логгер-заглушка для юнитов reducer'а (образец groupstab). */
object NoopLogger : LexemeLogger {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
}
