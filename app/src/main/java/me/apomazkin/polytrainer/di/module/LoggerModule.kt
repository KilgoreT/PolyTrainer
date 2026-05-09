package me.apomazkin.polytrainer.di.module

import dagger.Module
import dagger.Provides
import me.apomazkin.polytrainer.BuildConfig
import me.apomazkin.polytrainer.logger.CrashlyticsSink
import me.apomazkin.polytrainer.logger.LexemeLoggerImpl
import me.apomazkin.polytrainer.logger.LogcatSink
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import me.apomazkin.logger.LogSink

@Module
object LoggerModule {

    @Provides
    fun provideSinks(): List<@JvmSuppressWildcards LogSink> = buildList {
        val logLevel = BuildConfig.LOG_LEVEL
        if (logLevel != "NONE") {
            add(LogcatSink(LogLevel.valueOf(logLevel)))
        }
        val remoteLogLevel = BuildConfig.REMOTE_LOG_LEVEL
        if (remoteLogLevel != "NONE") {
            add(CrashlyticsSink(LogLevel.valueOf(remoteLogLevel)))
        }
    }

    @Provides
    fun provideLogger(sinks: List<@JvmSuppressWildcards LogSink>): LexemeLogger {
        return LexemeLoggerImpl(sinks)
    }
}
