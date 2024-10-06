package me.apomazkin.polytrainer.di.module

import dagger.Binds
import dagger.Module
import me.apomazkin.polytrainer.logger.LexemeLoggerImpl
import me.apomazkin.ui.logger.LexemeLogger

@Module
interface LoggerModule {
    @Binds
    fun bindLoggerImpl(impl: LexemeLoggerImpl): LexemeLogger
}