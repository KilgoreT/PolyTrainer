package me.apomazkin.core_interactor

import me.apomazkin.core_interactor.scenario.StatisticScenario
import me.apomazkin.core_interactor.scenario.WriteQuizScenario

interface CoreInteractorApi {
    fun writeQuizScenario(): WriteQuizScenario
    fun statisticScenario(): StatisticScenario

}