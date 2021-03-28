package me.apomazkin.core_interactor.di.module

import dagger.Binds
import dagger.Module
import me.apomazkin.core_interactor.useCase.definition.GetDefinitionUseCase
import me.apomazkin.core_interactor.useCase.definition.RemoveDefinitionUseCase
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
}