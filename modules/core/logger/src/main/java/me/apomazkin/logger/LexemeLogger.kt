package me.apomazkin.logger

interface LexemeLogger {
    fun log(level: LogLevel = LogLevel.DEBUG, tag: String = "###LEXEME###", message: String)

    fun d(tag: String = "###LEXEME###", message: String) = log(LogLevel.DEBUG, tag, message)
    fun i(tag: String = "###LEXEME###", message: String) = log(LogLevel.INFO, tag, message)
    fun w(tag: String = "###LEXEME###", message: String) = log(LogLevel.WARNING, tag, message)
    fun e(tag: String = "###LEXEME###", message: String) = log(LogLevel.ERROR, tag, message)
}
