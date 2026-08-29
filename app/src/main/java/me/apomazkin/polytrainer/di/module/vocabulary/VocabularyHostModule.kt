package me.apomazkin.polytrainer.di.module.vocabulary

import dagger.Binds
import dagger.Module
import me.apomazkin.vocabulary.deps.VocabularyHostUseCase

@Module
interface VocabularyHostModule {

    @Binds
    fun bindVocabularyHostUseCase(impl: VocabularyHostUseCaseImpl): VocabularyHostUseCase
}
