package me.apomazkin.core_interactor

import me.apomazkin.core_interactor.scenario.StatisticScenario
import me.apomazkin.core_interactor.useCase.definition.RemoveDefinitionUseCase
import me.apomazkin.core_interactor.useCase.term.GetTermUseCase
import me.apomazkin.core_interactor.useCase.term.SearchTermUseCase
import me.apomazkin.core_interactor.useCase.word.AddWordUseCase
import me.apomazkin.core_interactor.useCase.word.RemoveWordUseCase
import me.apomazkin.core_interactor.useCase.writeQuiz.WriteQuizScenario

interface CoreInteractorApi {
    fun writeQuizScenario(): WriteQuizScenario
    fun statisticScenario(): StatisticScenario

    fun getTermUseCase(): GetTermUseCase
    fun searchTermUseCase(): SearchTermUseCase
    fun addWordUseCase(): AddWordUseCase
    fun removeWordUseCase(): RemoveWordUseCase
    fun removeDefinitionUseCase(): RemoveDefinitionUseCase
}