package me.apomazkin.polytrainer.di.module.statistictab

import dagger.Binds
import dagger.Module
import me.apomazkin.stattab.deps.StatisticUseCase

@Module
interface StatisticModule {
    
    @Binds
    fun bindStatisticTabUseCase(impl: StatisticUseCaseImpl): StatisticUseCase
}