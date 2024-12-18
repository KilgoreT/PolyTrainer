package me.apomazkin.core_interactor.di.module

import dagger.Binds
import dagger.Module
import me.apomazkin.core_interactor.useCase.dump.GetDumpUseCase
import me.apomazkin.core_interactor.useCase.language.AddLanguageUseCase
import me.apomazkin.core_interactor.useCase.language.GetLanguageUseCase
import me.apomazkin.core_interactor.useCase.statistic.GetDefinitionCountUseCase
import me.apomazkin.core_interactor.useCase.statistic.GetWordClassCountUseCase
import me.apomazkin.core_interactor.useCase.statistic.GetWordCountUseCase
import me.apomazkin.core_interactor.useCase.statistic.GetWriteQuizCountUseCase
import me.apomazkin.core_interactor.useCase.writeQuiz.GetWriteQuizByAccessTimeUseCase
import me.apomazkin.core_interactor.useCase.writeQuiz.GetWriteQuizByRandomUseCase
import me.apomazkin.core_interactor.useCase.writeQuiz.RemoveWriteQuizUseCase
import me.apomazkin.core_interactor.useCase.writeQuiz.UpdateWriteQuizUseCase

@Module
interface UseCaseModule {

    @Binds
    fun bindGetLanguageUseCase(impl: GetLanguageUseCase.Impl): GetLanguageUseCase

    @Binds
    fun bindAddLanguageUseCase(impl: AddLanguageUseCase.Impl): AddLanguageUseCase

    @Binds
    fun bindGetRandomWriteQuizUseCase(impl: GetWriteQuizByRandomUseCase.Impl): GetWriteQuizByRandomUseCase

    @Binds
    fun bindGetWriteQuizByAccessTimeUseCase(impl: GetWriteQuizByAccessTimeUseCase.Impl): GetWriteQuizByAccessTimeUseCase

    @Binds
    fun bindUpdateWriteQuizUseCase(impl: UpdateWriteQuizUseCase.Impl): UpdateWriteQuizUseCase

    @Binds
    fun bindGetWordCountUseCase(impl: GetWordCountUseCase.Impl): GetWordCountUseCase

    @Binds
    fun bindGetDefinitionCountUseCase(impl: GetDefinitionCountUseCase.Impl): GetDefinitionCountUseCase

    @Binds
    fun bindGetWordClassCountUseCase(impl: GetWordClassCountUseCase.Impl): GetWordClassCountUseCase

    @Binds
    fun bindGetWriteQuizCountUseCase(impl: GetWriteQuizCountUseCase.Impl): GetWriteQuizCountUseCase

    @Binds
    fun bindRemoveWriteQuizUseCase(impl: RemoveWriteQuizUseCase.Impl): RemoveWriteQuizUseCase

    @Binds
    fun bindGetDumpUseCase(impl: GetDumpUseCase.Impl): GetDumpUseCase
}