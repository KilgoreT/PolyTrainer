package me.apomazkin.logger

interface LogSink {
    val minLevel: LogLevel
    fun write(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)
}
