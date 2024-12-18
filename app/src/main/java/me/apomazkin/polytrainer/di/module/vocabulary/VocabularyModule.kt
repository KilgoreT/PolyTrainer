package me.apomazkin.polytrainer.di.module.vocabulary

import dagger.Binds
import dagger.Module
import me.apomazkin.dictionarytab.deps.VocabularyUseCase

@Module
interface VocabularyModule {

    @Binds
    fun bindVocabularyUseCase(impl: VocabularyUseCaseImpl): VocabularyUseCase
}