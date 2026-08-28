package me.apomazkin.groupstab.logic

import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel

/** No-op логгер для reducer-тестов (образец — WordsTabReducerKtTest). */
internal object NoopLogger : LexemeLogger {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
}
