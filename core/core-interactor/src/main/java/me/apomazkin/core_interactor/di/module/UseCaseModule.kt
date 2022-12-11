package me.apomazkin.core_interactor.di.module

import dagger.Binds
import dagger.Module
import me.apomazkin.core_interactor.useCase.definition.*
import me.apomazkin.core_interactor.useCase.dump.GetDumpUseCase
import me.apomazkin.core_interactor.useCase.language.AddLanguageUseCase
import me.apomazkin.core_interactor.useCase.language.GetLanguageUseCase
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
    fun bindAddWordUseCase(impl: AddWordUseCase.Impl): AddWordUseCase

    @Binds
    fun bindGetWordUseCase(impl: GetWordUseCase.Impl): GetWordUseCase

    @Binds
    fun bindUpdateWordUseCase(impl: UpdateWordUseCase.Impl): UpdateWordUseCase

    @Binds
    fun bindRemoveWordUseCase(impl: RemoveWordUseCase.Impl): RemoveWordUseCase

    @Binds
    fun bindGetTermUseCase(impl: GetTermUseCase.Impl): GetTermUseCase

    @Binds
    fun bindSearchTermUseCase(impl: SearchTermUseCase.Impl): SearchTermUseCase

    @Binds
    fun bindAddDefinitionUseCase(impl: AddDefinitionUseCase.Impl): AddDefinitionUseCase

    @Binds
    fun bindGetDefinitionUseCase(impl: GetDefinitionUseCase.Impl): GetDefinitionUseCase

    @Binds
    fun bindGetDefinitionListUseCase(impl: GetDefinitionListUseCase.Impl): GetDefinitionListUseCase

    @Binds
    fun bindUpdateDefinitionUseCase(impl: UpdateDefinitionUseCase.Impl): UpdateDefinitionUseCase

    @Binds
    fun bindRemoveDefinitionUseCase(impl: RemoveDefinitionUseCase.Impl): RemoveDefinitionUseCase

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
    fun bindAddSampleUseCase(impl: AddSampleUseCase.Impl): AddSampleUseCase

    @Binds
    fun bindGetSampleUseCase(impl: GetSampleUseCase.Impl): GetSampleUseCase

    @Binds
    fun bindGetDumpUseCase(impl: GetDumpUseCase.Impl): GetDumpUseCase
}