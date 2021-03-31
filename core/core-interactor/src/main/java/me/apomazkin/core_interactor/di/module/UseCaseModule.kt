package me.apomazkin.core_interactor.di.module

import dagger.Binds
import dagger.Module
import me.apomazkin.core_interactor.scenario.StatisticScenario
import me.apomazkin.core_interactor.scenario.StatisticScenarioImpl
import me.apomazkin.core_interactor.useCase.definition.GetDefinitionUseCase
import me.apomazkin.core_interactor.useCase.definition.RemoveDefinitionUseCase
import me.apomazkin.core_interactor.useCase.statistic.GetDefinitionCountUseCase
import me.apomazkin.core_interactor.useCase.statistic.GetWordClassCountUseCase
import me.apomazkin.core_interactor.useCase.statistic.GetWordCountUseCase
import me.apomazkin.core_interactor.useCase.statistic.GetWriteQuizCountUseCase
import me.apomazkin.core_interactor.useCase.term.GetTermUseCase
import me.apomazkin.core_interactor.useCase.term.SearchTermUseCase
import me.apomazkin.core_interactor.useCase.word.AddWordUseCase
import me.apomazkin.core_interactor.useCase.word.GetWordUseCase
import me.apomazkin.core_interactor.useCase.word.RemoveWordUseCase
import me.apomazkin.core_interactor.useCase.writeQuiz.GetWriteQuizUseCase
import me.apomazkin.core_interactor.useCase.writeQuiz.UpdateWriteQuizUseCase
import me.apomazkin.core_interactor.useCase.writeQuiz.WriteQuizScenario
import me.apomazkin.core_interactor.useCase.writeQuiz.WriteQuizScenarioImpl

@Module
interface UseCaseModule {

    @Binds
    fun provideAddWordUseCase(impl: AddWordUseCase.AddWordUseCaseImpl): AddWordUseCase

    @Binds
    fun provideGetWordUseCase(impl: GetWordUseCase.GetWordUseCaseImpl): GetWordUseCase

    @Binds
    fun provideRemoveWordUseCase(impl: RemoveWordUseCase.RemoveWordUseCaseImpl): RemoveWordUseCase

    @Binds
    fun provideGetTermUseCase(impl: GetTermUseCase.GetTermUseCaseImpl): GetTermUseCase

    @Binds
    fun provideSearchTermUseCase(impl: SearchTermUseCase.SearchTermUseCaseImpl): SearchTermUseCase

    @Binds
    fun provideGetDefinitionUseCase(impl: GetDefinitionUseCase.GetDefinitionUseCaseImpl): GetDefinitionUseCase

    @Binds
    fun provide(impl: RemoveDefinitionUseCase.RemoveDefinitionUseCaseImpl): RemoveDefinitionUseCase

    @Binds
    fun provideGetWriteQuizUseCase(impl: GetWriteQuizUseCase.GetWriteQuizUseCaseImpl): GetWriteQuizUseCase

    @Binds
    fun provideUpdateWriteQuizUseCase(impl: UpdateWriteQuizUseCase.UpdateWriteQuizUseCaseImpl): UpdateWriteQuizUseCase

    @Binds
    fun provideWriteQuizScenario(impl: WriteQuizScenarioImpl): WriteQuizScenario

    @Binds
    fun provideGetWordCountUseCase(impl: GetWordCountUseCase.GetWordCountUseCaseImpl): GetWordCountUseCase

    @Binds
    fun provideGetDefinitionCountUseCase(impl: GetDefinitionCountUseCase.GetDefinitionCountUseCaseImpl): GetDefinitionCountUseCase

    @Binds
    fun provideGetWordClassCountUseCase(impl: GetWordClassCountUseCase.GetWordClassCountUseCaseImpl): GetWordClassCountUseCase

    @Binds
    fun provideGetWriteQuizCountUseCase(impl: GetWriteQuizCountUseCase.GetWriteQuizCountUseCaseImpl): GetWriteQuizCountUseCase

    @Binds
    fun provideStatisticScenario(impl: StatisticScenarioImpl): StatisticScenario
}