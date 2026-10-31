package me.apomazkin.wordstab.logic

import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel

/** Обвязка message-тестов: reducer с пустым логгером и чистое состояние вкладки. */
internal object NoopLogger : LexemeLogger {
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {}
}

internal fun reducer() = WordsTabReducer(logger = NoopLogger)

/** Вкладка после загрузки, диалоги закрыты. */
internal fun idle() = WordsTabState(isLoading = false)
