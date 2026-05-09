package me.apomazkin.polytrainer.di

import dagger.Component
import me.apomazkin.polytrainer.di.module.LoggerModule
import me.apomazkin.logger.LexemeLogger

@Component(modules = [LoggerModule::class])
interface LoggerComponent {

    fun getLogger(): LexemeLogger

    companion object {
        fun create(): LoggerComponent = DaggerLoggerComponent.create()
    }
}
