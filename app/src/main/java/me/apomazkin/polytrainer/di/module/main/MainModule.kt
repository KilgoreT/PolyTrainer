package me.apomazkin.polytrainer.di.module.main

import dagger.Binds
import dagger.Module
import me.apomazkin.main.widget.top.MainTopBarUseCase

@Module
interface MainModule {

    @Binds
    fun bindMainTopBarUseCase(impl: MainTopBarUseCaseImpl): MainTopBarUseCase
}