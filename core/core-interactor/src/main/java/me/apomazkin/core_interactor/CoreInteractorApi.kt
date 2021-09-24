package me.apomazkin.core_interactor

import me.apomazkin.core_interactor.scenario.StatisticScenario
import me.apomazkin.core_interactor.scenario.WriteQuizScenario
import me.apomazkin.core_interactor.useCase.definition.*
import me.apomazkin.core_interactor.useCase.sample.AddSampleUseCase
import me.apomazkin.core_interactor.useCase.sample.GetSampleUseCase
import me.apomazkin.core_interactor.useCase.term.GetTermUseCase
import me.apomazkin.core_interactor.useCase.term.SearchTermUseCase
import me.apomazkin.core_interactor.useCase.word.AddWordUseCase
import me.apomazkin.core_interactor.useCase.word.RemoveWordUseCase
import me.apomazkin.core_interactor.useCase.word.UpdateWordUseCase
import me.apomazkin.core_interactor.useCase.writeQuiz.GetWriteQuizByAccessTimeUseCase
import me.apomazkin.core_interactor.useCase.writeQuiz.RemoveWriteQuizUseCase

interface CoreInteractorApi {
    fun writeQuizScenario(): WriteQuizScenario
    fun statisticScenario(): StatisticScenario

    fun getTermUseCase(): GetTermUseCase
    fun searchTermUseCase(): SearchTermUseCase
    fun addWordUseCase(): AddWordUseCase
    fun updateWordUseCase(): UpdateWordUseCase
    fun removeWordUseCase(): RemoveWordUseCase
    fun updateDefinitionUseCase(): UpdateDefinitionUseCase
    fun removeDefinitionUseCase(): RemoveDefinitionUseCase
    fun addDefinitionUseCase(): AddDefinitionUseCase
    fun getDefinitionUseCase(): GetDefinitionUseCase
    fun removeWriteQuizUseCase(): RemoveWriteQuizUseCase
    fun getWriteQuizByAccessTimeUseCase(): GetWriteQuizByAccessTimeUseCase
    fun addSampleUseCase(): AddSampleUseCase
    fun getSampleUseCase(): GetSampleUseCase

    @Deprecated("NotUsed")
    fun getDefinitionListUseCase(): GetDefinitionListUseCase
}