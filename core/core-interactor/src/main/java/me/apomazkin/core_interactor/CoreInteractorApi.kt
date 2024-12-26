package me.apomazkin.core_interactor

import me.apomazkin.core_interactor.scenario.StatisticScenario
import me.apomazkin.core_interactor.scenario.WriteQuizScenario
import me.apomazkin.core_interactor.useCase.dump.GetDumpUseCase
import me.apomazkin.core_interactor.useCase.writeQuiz.GetWriteQuizByAccessTimeUseCase
import me.apomazkin.core_interactor.useCase.writeQuiz.RemoveWriteQuizUseCase

interface CoreInteractorApi {
    fun writeQuizScenario(): WriteQuizScenario
    fun statisticScenario(): StatisticScenario

    fun removeWriteQuizUseCase(): RemoveWriteQuizUseCase
    fun getWriteQuizByAccessTimeUseCase(): GetWriteQuizByAccessTimeUseCase
    fun getDumpUseCase(): GetDumpUseCase
}