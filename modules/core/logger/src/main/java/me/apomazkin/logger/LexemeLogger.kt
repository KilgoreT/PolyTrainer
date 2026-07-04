package me.apomazkin.logger

interface LexemeLogger {
    fun log(
        level: LogLevel = LogLevel.DEBUG,
        tag: String = "###LEXEME###",
        message: String,
        throwable: Throwable? = null,
    )

    fun d(tag: String = "###LEXEME###", message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String = "###LEXEME###", message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String = "###LEXEME###", message: String, throwable: Throwable? = null) =
        log(LogLevel.WARNING, tag, message, throwable)
    fun e(tag: String = "###LEXEME###", message: String, throwable: Throwable? = null) =
        log(LogLevel.ERROR, tag, message, throwable)
}
