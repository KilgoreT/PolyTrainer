package me.apomazkin.core_interactor.di.module

import dagger.Binds
import dagger.Module
import me.apomazkin.core_interactor.scenario.StatisticScenario
import me.apomazkin.core_interactor.scenario.StatisticScenarioImpl
import me.apomazkin.core_interactor.useCase.definition.*
import me.apomazkin.core_interactor.useCase.sample.AddSampleUseCase
import me.apomazkin.core_interactor.useCase.sample.GetSampleUseCase
import me.apomazkin.core_interactor.useCase.statistic.GetDefinitionCountUseCase
import me.apomazkin.core_interactor.useCase.statistic.GetWordClassCountUseCase
import me.apomazkin.core_interactor.useCase.statistic.GetWordCountUseCase
import me.apomazkin.core_interactor.useCase.statistic.GetWriteQuizCountUseCase
import me.apomazkin.core_interactor.useCase.term.GetTermUseCase
import me.apomazkin.core_interactor.useCase.term.SearchTermUseCase
import me.apomazkin.core_interactor.useCase.word.AddWordUseCase
import me.apomazkin.core_interactor.useCase.word.GetWordUseCase
import me.apomazkin.core_interactor.useCase.word.RemoveWordUseCase
import me.apomazkin.core_interactor.useCase.word.UpdateWordUseCase
import me.apomazkin.core_interactor.useCase.writeQuiz.*

@Module
interface UseCaseModule {

    // TODO: 16.07.2021 rename to bind

    @Binds
    fun provideAddWordUseCase(impl: AddWordUseCase.AddWordUseCaseImpl): AddWordUseCase

    @Binds
    fun provideGetWordUseCase(impl: GetWordUseCase.GetWordUseCaseImpl): GetWordUseCase

    @Binds
    fun provideUpdateWordUseCase(impl: UpdateWordUseCase.UpdateWordUseCaseImpl): UpdateWordUseCase

    @Binds
    fun provideRemoveWordUseCase(impl: RemoveWordUseCase.RemoveWordUseCaseImpl): RemoveWordUseCase

    @Binds
    fun provideGetTermUseCase(impl: GetTermUseCase.GetTermUseCaseImpl): GetTermUseCase

    @Binds
    fun provideSearchTermUseCase(impl: SearchTermUseCase.SearchTermUseCaseImpl): SearchTermUseCase

    @Binds
    fun provideAddDefinitionUseCase(impl: AddDefinitionUseCase.AddDefinitionUseCaseImpl): AddDefinitionUseCase

    @Binds
    fun provideGetDefinitionUseCase(impl: GetDefinitionUseCase.GetDefinitionUseCaseImpl): GetDefinitionUseCase

    @Binds
    fun provideGetDefinitionListUseCase(impl: GetDefinitionListUseCase.Impl): GetDefinitionListUseCase

    @Binds
    fun provideUpdateDefinitionUseCase(impl: UpdateDefinitionUseCase.Impl): UpdateDefinitionUseCase

    @Binds
    fun provideRemoveDefinitionUseCase(impl: RemoveDefinitionUseCase.RemoveDefinitionUseCaseImpl): RemoveDefinitionUseCase

    @Binds
    fun bindGetRandomWriteQuizUseCase(impl: GetRandomWriteQuizUseCase.Impl): GetRandomWriteQuizUseCase

    @Binds
    fun bindGetWriteQuizByAccessTimeUseCase(impl: GetWriteQuizByAccessTimeUseCase.Impl): GetWriteQuizByAccessTimeUseCase

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
    fun provideRemoveWriteQuizUseCase(impl: RemoveWriteQuizUseCase.Impl): RemoveWriteQuizUseCase

    @Binds
    fun provideStatisticScenario(impl: StatisticScenarioImpl): StatisticScenario

    @Binds
    fun bindAddSampleUseCase(impl: AddSampleUseCase.Impl): AddSampleUseCase

    @Binds
    fun bindGetSampleUseCase(impl: GetSampleUseCase.Impl): GetSampleUseCase
}