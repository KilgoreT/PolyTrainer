package me.apomazkin.polytrainer.logger

import android.util.Log
import me.apomazkin.ui.logger.LogLevel
import me.apomazkin.ui.logger.LogSink

class LogcatSink(override val minLevel: LogLevel) : LogSink {
    override fun write(level: LogLevel, tag: String, message: String) {
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARNING -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
        }
    }
}
