package me.apomazkin.ui.logger

interface LogSink {
    val minLevel: LogLevel
    fun write(level: LogLevel, tag: String, message: String)
}
