package me.apomazkin.polytrainer.logger

import com.google.firebase.crashlytics.FirebaseCrashlytics
import me.apomazkin.ui.logger.LogLevel
import me.apomazkin.ui.logger.LogSink

class CrashlyticsSink(override val minLevel: LogLevel) : LogSink {
    override fun write(level: LogLevel, tag: String, message: String) {
        when (level) {
            LogLevel.WARNING -> {
                FirebaseCrashlytics.getInstance().log("$tag: $message")
            }
            LogLevel.ERROR -> {
                FirebaseCrashlytics.getInstance().recordException(RuntimeException("$tag: $message"))
            }
            else -> {
                // CrashlyticsSink only handles WARNING and ERROR
            }
        }
    }
}
