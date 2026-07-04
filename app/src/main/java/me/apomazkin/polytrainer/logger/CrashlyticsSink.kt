package me.apomazkin.polytrainer.logger

import com.google.firebase.crashlytics.FirebaseCrashlytics
import me.apomazkin.logger.LogLevel
import me.apomazkin.logger.LogSink

class CrashlyticsSink(override val minLevel: LogLevel) : LogSink {
    override fun write(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        when (level) {
            LogLevel.WARNING -> {
                FirebaseCrashlytics.getInstance().log("$tag: $message")
                if (throwable != null) {
                    FirebaseCrashlytics.getInstance().recordException(throwable)
                }
            }
            LogLevel.ERROR -> {
                val cause = throwable ?: RuntimeException("$tag: $message")
                FirebaseCrashlytics.getInstance().log("$tag: $message")
                FirebaseCrashlytics.getInstance().recordException(cause)
            }
            else -> {
                // CrashlyticsSink only handles WARNING and ERROR
            }
        }
    }
}
