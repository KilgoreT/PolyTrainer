package me.apomazkin.core_interactor.di.module

import dagger.Binds
import dagger.Module
import me.apomazkin.core_interactor.scenario.StatisticScenario
import me.apomazkin.core_interactor.scenario.WriteQuizScenario

@Module
interface ScenarioModule {

    @Binds
    fun bindWriteQuizScenario(impl: WriteQuizScenario.Impl): WriteQuizScenario

    @Binds
    fun bindStatisticScenario(impl: StatisticScenario.Impl): StatisticScenario
}